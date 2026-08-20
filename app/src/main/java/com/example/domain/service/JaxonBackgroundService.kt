package com.example.domain.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.util.Log
import com.example.JaxonApplication
import com.example.MainActivity
import com.example.data.di.AppContainer
import com.example.domain.speech.MicOwner
import com.example.domain.speech.MicOwnership
import com.example.domain.speech.WakeWordEngine
import com.example.domain.speech.WakeWordState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** One-shot events the ViewModel reacts to; a SharedFlow avoids the stale-flag bugs of a boolean. */
sealed class WakeEvent {
    object StartListening : WakeEvent()
    data class WakeDetected(val trailingCommand: String?) : WakeEvent()
}

class JaxonBackgroundService : Service() {

    companion object {
        private const val TAG = "JaxonBgService"
        const val CHANNEL_ID = "jaxon_voice_service_channel"
        const val NOTIFICATION_ID = 4040

        const val ACTION_START_SERVICE = "com.example.jaxon.action.START"
        const val ACTION_STOP_SERVICE = "com.example.jaxon.action.STOP"
        const val ACTION_START_LISTENING_TRIGGER = "com.example.jaxon.action.START_LISTENING"
        const val ACTION_STOP_LISTENING_TRIGGER = "com.example.jaxon.action.STOP_LISTENING"
        const val ACTION_WAKE_COMMAND_FINISHED = "com.example.jaxon.action.WAKE_COMMAND_FINISHED"

        private const val WAKE_COMMAND_TIMEOUT_MS = 20_000L

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _wakeWordState = MutableStateFlow(WakeWordState.DISABLED)
        val wakeWordState: StateFlow<WakeWordState> = _wakeWordState.asStateFlow()

        private val _wakeEvents = MutableSharedFlow<WakeEvent>(extraBufferCapacity = 4)
        val wakeEvents: SharedFlow<WakeEvent> = _wakeEvents.asSharedFlow()

        /** True once onTaskRemoved has fired, so a sticky restart doesn't resume listening. */
        @Volatile
        private var taskRemoved = false

        /**
         * Called by JaxonViewModel right before it starts UI-driven listening, so the wake loop
         * releases the mic first. Safe to call even if the service isn't running.
         */
        fun requestPauseWakeLoop(context: Context) {
            val intent = Intent(context, JaxonBackgroundService::class.java).apply {
                action = ACTION_STOP_LISTENING_TRIGGER
            }
            runCatching { context.startService(intent) }
        }

        /** Called by JaxonViewModel once a UI-driven command cycle finishes, to resume the wake loop. */
        fun notifyWakeCommandFinished(context: Context) {
            val intent = Intent(context, JaxonBackgroundService::class.java).apply {
                action = ACTION_WAKE_COMMAND_FINISHED
            }
            runCatching { context.startService(intent) }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wakeEngine: WakeWordEngine? = null
    private var wakeWatchdogJob: kotlinx.coroutines.Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP_SERVICE) {
            stopEverything()
            stopSelf()
            return START_NOT_STICKY
        }

        if (action == ACTION_START_SERVICE) {
            // A fresh, explicit user action (toggling Background Mode on) always overrides a
            // prior force-close - otherwise the feature could never be re-enabled in-process.
            taskRemoved = false
        } else if (taskRemoved) {
            // The app was force-closed from Recent Apps; a sticky restart must not silently
            // resume background listening.
            stopEverything()
            stopSelf()
            return START_NOT_STICKY
        }

        _isRunning.value = true
        if (!tryStartForeground()) {
            stopSelf()
            return START_NOT_STICKY
        }

        val container = (application as JaxonApplication).container

        when (action) {
            ACTION_START_SERVICE, null -> {
                // null == system-triggered restart after the service was killed. Never trust
                // in-memory state here - re-read the user's actual setting from DataStore.
                serviceScope.launch {
                    val enabled = container.settingsRepository.isBackgroundServiceEnabled.first()
                    val micGranted = ContextCompat.checkSelfPermission(
                        this@JaxonBackgroundService, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    when {
                        !enabled -> {
                            stopEverything()
                            stopSelf()
                        }
                        !micGranted -> {
                            _wakeWordState.value = WakeWordState.ERROR_PERMISSION
                            updateNotification("Microphone permission required for wake word")
                        }
                        else -> startWakeLoop()
                    }
                }
            }
            ACTION_START_LISTENING_TRIGGER -> {
                wakeEngine?.pause()
                _wakeEvents.tryEmit(WakeEvent.StartListening)
                val openIntent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(openIntent)
            }
            ACTION_STOP_LISTENING_TRIGGER -> {
                // UI is about to start its own listening session - release the wake loop's claim.
                wakeEngine?.pause()
            }
            ACTION_WAKE_COMMAND_FINISHED -> {
                Log.d(TAG, "ACTION_WAKE_COMMAND_FINISHED received, resuming wake loop")
                wakeWatchdogJob?.cancel()
                MicOwnership.release(MicOwner.UI)
                resumeWakeLoopIfEligible(container)
            }
        }
        return START_STICKY
    }

    private fun startWakeLoop() {
        val container = (application as JaxonApplication).container
        if (wakeEngine == null) {
            wakeEngine = WakeWordEngine(applicationContext) { trailingCommand ->
                onWakeDetected(trailingCommand)
            }
            serviceScope.launch {
                wakeEngine?.state?.collectLatest { state ->
                    _wakeWordState.value = state
                    updateNotification(notificationTextFor(state))
                }
            }
        }
        serviceScope.launch {
            val locale = container.settingsRepository.recognitionLocale.first()
            wakeEngine?.start(locale)
        }
    }

    private fun onWakeDetected(trailingCommand: String?) {
        Log.d(TAG, "onWakeDetected: trailingCommand=$trailingCommand")
        val container = (application as JaxonApplication).container
        serviceScope.launch {
            MicOwnership.forceClaim(MicOwner.UI)

            if (container.settingsRepository.isTtsEnabled.first()) {
                val rate = container.settingsRepository.ttsRate.first()
                val pitch = container.settingsRepository.ttsPitch.first()
                container.ttsManager.speakAndAwait("Listening, Sir", rate, pitch)
            }

            ensureActivityForeground()
            _wakeEvents.emit(WakeEvent.WakeDetected(trailingCommand))

            // Safety watchdog: if the UI never reports the command as finished (e.g. no Activity
            // ever consumed the event, or it crashed), force the mic back to the wake loop so it
            // can't get stuck forever. notifyWakeCommandFinished() cancels this job on success,
            // which is what stops it from ever reaching the delay's end.
            wakeWatchdogJob?.cancel()
            wakeWatchdogJob = serviceScope.launch {
                kotlinx.coroutines.delay(WAKE_COMMAND_TIMEOUT_MS)
                Log.d(TAG, "wake watchdog fired - ViewModel never signalled command finished")
                MicOwnership.release(MicOwner.UI)
                resumeWakeLoopIfEligible(container)
            }
        }
    }

    private fun ensureActivityForeground() {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        runCatching { startActivity(openIntent) }
    }

    private fun resumeWakeLoopIfEligible(container: AppContainer) {
        serviceScope.launch {
            val enabled = container.settingsRepository.isBackgroundServiceEnabled.first()
            Log.d(TAG, "resumeWakeLoopIfEligible: enabled=$enabled taskRemoved=$taskRemoved wakeEngine=${wakeEngine != null}")
            if (enabled && !taskRemoved) {
                wakeEngine?.resume()
            } else {
                wakeEngine?.pause()
            }
        }
    }

    private fun stopEverything() {
        wakeWatchdogJob?.cancel()
        wakeEngine?.shutdown()
        wakeEngine = null
        MicOwnership.release(MicOwner.WAKE_WORD)
        _wakeWordState.value = WakeWordState.DISABLED
        _isRunning.value = false
    }

    /** @return false if the foreground service could not be started (caller should stopSelf()). */
    private fun tryStartForeground(): Boolean {
        return try {
            startForegroundServiceWithNotification(notificationTextFor(_wakeWordState.value))
            true
        } catch (e: Exception) {
            // Covers ForegroundServiceStartNotAllowedException (API 31+), SecurityException, and
            // MissingForegroundServiceTypeException - all fatal for this attempt, never crash-loop.
            false
        }
    }

    private fun notificationTextFor(state: WakeWordState): String = when (state) {
        WakeWordState.DISABLED -> "Jaxon is running and ready for commands."
        WakeWordState.WAKE_LISTENING -> "Listening for \"Hey Jaxon\"…"
        WakeWordState.TRIGGERED -> "Jaxon is listening…"
        WakeWordState.PAUSED_BY_UI -> "Jaxon is busy…"
        WakeWordState.ERROR_PERMISSION -> "Microphone permission required for wake word"
        WakeWordState.ERROR_UNAVAILABLE -> "Speech recognition unavailable on this device"
    }

    private fun startForegroundServiceWithNotification(contentText: String) {
        startForeground(NOTIFICATION_ID, buildNotification(contentText))
    }

    private fun updateNotification(contentText: String) {
        if (!hasNotificationPermission()) return
        val manager = NotificationManagerCompat.from(this)
        runCatching { manager.notify(NOTIFICATION_ID, buildNotification(contentText)) }
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun buildNotification(contentText: String): android.app.Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val exitIntent = Intent(this, JaxonBackgroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val exitPendingIntent = PendingIntent.getService(
            this, 2, exitIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Jaxon Voice Assistant")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Exit Service", exitPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Jaxon Voice Assistant Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keep Jaxon active in background to execute offline commands"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        taskRemoved = true
        stopEverything()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        stopEverything()
        serviceScope.cancel()
        super.onDestroy()
    }
}
