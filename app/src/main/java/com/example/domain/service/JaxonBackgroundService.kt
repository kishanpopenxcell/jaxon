package com.example.domain.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class JaxonBackgroundService : Service() {

    companion object {
        const val CHANNEL_ID = "jaxon_voice_service_channel"
        const val NOTIFICATION_ID = 4040

        const val ACTION_START_SERVICE = "com.example.jaxon.action.START"
        const val ACTION_STOP_SERVICE = "com.example.jaxon.action.STOP"
        const val ACTION_START_LISTENING_TRIGGER = "com.example.jaxon.action.START_LISTENING"
        const val ACTION_STOP_LISTENING_TRIGGER = "com.example.jaxon.action.STOP_LISTENING"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _triggerListenFlow = MutableStateFlow(false)
        val triggerListenFlow: StateFlow<Boolean> = _triggerListenFlow.asStateFlow()

        fun clearTriggerListen() {
            _triggerListenFlow.value = false
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START_SERVICE -> {
                _isRunning.value = true
                startForegroundServiceWithNotification()
            }
            ACTION_STOP_SERVICE -> {
                _isRunning.value = false
                stopSelf()
            }
            ACTION_START_LISTENING_TRIGGER -> {
                // Emits to the UI to open/start listening immediately
                _triggerListenFlow.value = true
                // Relaunch main activity to show UI
                val openIntent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(openIntent)
            }
            ACTION_STOP_LISTENING_TRIGGER -> {
                _triggerListenFlow.value = false
            }
        }
        return START_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val startListenIntent = Intent(this, JaxonBackgroundService::class.java).apply {
            action = ACTION_START_LISTENING_TRIGGER
        }
        val startListenPendingIntent = PendingIntent.getService(
            this, 1, startListenIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val exitIntent = Intent(this, JaxonBackgroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val exitPendingIntent = PendingIntent.getService(
            this, 2, exitIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Native check to avoid crash if resources or icons are loading
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now) // fallback system icon
            .setContentTitle("Jaxon Voice Assistant")
            .setContentText("Jaxon is running and ready for commands.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_media_play, "Start Listening", startListenPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Exit Service", exitPendingIntent)

        startForeground(NOTIFICATION_ID, builder.build())
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

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        _isRunning.value = false
        super.onDestroy()
    }
}
