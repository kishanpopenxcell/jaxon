package com.example.domain.executor

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.Settings
import android.view.KeyEvent
import com.example.domain.parser.ParsedIntent
import com.example.domain.parser.IntentType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ActionExecutor(private val context: Context) {

    /**
     * Executes the parsed voice command and returns a natural descriptive result string.
     */
    suspend fun execute(parsed: ParsedIntent): String {
        return try {
            when (parsed.intentType) {
                IntentType.OPEN_APP -> openAppAction(parsed.parameters["app"])
                IntentType.CALL_CONTACT -> callContactAction(parsed.parameters["contact"] ?: parsed.parameters["number"])
                IntentType.SHOW_TIME -> showTimeAction()
                IntentType.BATTERY_INFO -> batteryInfoAction()
                IntentType.DEVICE_STORAGE -> deviceStorageAction()
                IntentType.OPEN_SETTINGS -> openSettingsAction(parsed.parameters["setting"])
                IntentType.FLASHLIGHT_TOGGLE -> flashlightToggleAction(parsed.parameters["state"])
                IntentType.VOLUME_CONTROL -> volumeControlAction(parsed.parameters["action"], parsed.parameters["state"])
                IntentType.SET_ALARM -> setAlarmAction(parsed.parameters["time"], parsed.parameters["hour"], parsed.parameters["minute"], parsed.parameters["ampm"])
                IntentType.SET_TIMER -> setTimerAction(parsed.parameters["duration"], parsed.parameters["unit"])
                IntentType.MAP_NAVIGATION -> mapNavigationAction(parsed.parameters["location"])
                IntentType.LAUNCH_SEARCH -> launchSearchAction(parsed.parameters["query"])
                IntentType.MEDIA_CONTROL -> mediaControlAction(parsed.parameters["action"])
                IntentType.UNKNOWN -> "I am not completely sure how to execute that. Try asking to set an alarm, check the battery, or open settings."
            }
        } catch (e: Exception) {
            "An error occurred while executing that action: ${e.localizedMessage}"
        }
    }

    /**
     * Tries to find and launch an application by its user-friendly name.
     */
    private fun openAppAction(appName: String?): String {
        if (appName.isNullOrBlank()) {
            return "Please specify which application you would like to open."
        }

        val pm = context.packageManager
        val cleanAppName = appName.lowercase(Locale.getDefault()).trim()

        // Dictionary of standard package names for quick lookups
        val popularApps = mapOf(
            "camera" to listOf("com.android.camera", "com.google.android.GoogleCamera", "com.sec.android.app.camera"),
            "gallery" to listOf("com.google.android.apps.photos", "com.android.gallery3d", "com.sec.android.gallery3d"),
            "chrome" to listOf("com.android.chrome"),
            "gmail" to listOf("com.google.android.gm"),
            "maps" to listOf("com.google.android.apps.maps"),
            "youtube" to listOf("com.google.android.youtube"),
            "whatsapp" to listOf("com.whatsapp"),
            "telegram" to listOf("org.telegram.messenger"),
            "facebook" to listOf("com.facebook.katana"),
            "instagram" to listOf("com.instagram.android"),
            "clock" to listOf("com.google.android.deskclock", "com.android.deskclock", "com.sec.android.app.clockpackage"),
            "calculator" to listOf("com.google.android.calculator", "com.android.calculator2", "com.sec.android.app.popupcalculator"),
            "contacts" to listOf("com.android.contacts", "com.google.android.contacts"),
            "phone" to listOf("com.android.dialer", "com.google.android.dialer", "com.sec.android.app.dialertab"),
            "messages" to listOf("com.google.android.apps.messaging", "com.android.mms")
        )

        // 1. Try launching with popular packages dictionary
        val possiblePackages = popularApps[cleanAppName]
        if (possiblePackages != null) {
            for (pkg in possiblePackages) {
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return "Opening $appName."
                }
            }
        }

        // 2. Scan all installed packages and look for app name match
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        var bestPkg: String? = null
        var bestName: String? = null

        for (appInfo in packages) {
            val label = pm.getApplicationLabel(appInfo).toString().lowercase(Locale.getDefault())
            if (label == cleanAppName || label.contains(cleanAppName)) {
                val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
                if (launchIntent != null) {
                    bestPkg = appInfo.packageName
                    bestName = pm.getApplicationLabel(appInfo).toString()
                    if (label == cleanAppName) break // Exact match
                }
            }
        }

        if (bestPkg != null) {
            val launchIntent = pm.getLaunchIntentForPackage(bestPkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return "Opening $bestName."
            }
        }

        // 3. Fallback to settings or explain we couldn't find it
        return "I couldn't find the application '$appName' installed on your device."
    }

    /**
     * Resolves a contact name into a phone number and makes or dials the call.
     */
    @SuppressLint("Range")
    private fun callContactAction(contact: String?): String {
        if (contact.isNullOrBlank()) {
            return "Please tell me who you want to call."
        }

        val cleanContact = contact.trim()

        // Check if contact is already a direct phone number
        if (cleanContact.matches(Regex("^[+]?[0-9\\s-]{3,15}$"))) {
            return dialOrCallNumber(cleanContact)
        }

        // Search contacts ContentProvider (Real Android SDK integration!)
        val contentResolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$cleanContact%")

        var cursor: Cursor? = null
        val foundContacts = mutableListOf<Pair<String, String>>()

        try {
            cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    val number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    foundContacts.add(Pair(name, number))
                }
            }
        } catch (e: Exception) {
            return "Unable to access your contacts. Please ensure you have granted the Contacts permission. Proceeding to open your Contacts app instead."
        } finally {
            cursor?.close()
        }

        if (foundContacts.isEmpty()) {
            // Fallback: Open dialer with prefilled search
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            return "I couldn't find a contact named '$cleanContact'. Opening dialer."
        }

        if (foundContacts.size > 1) {
            // Ambiguity found - dial the first one but list choices
            val bestChoice = foundContacts.first()
            dialOrCallNumber(bestChoice.second)
            return "I found multiple contacts matching '$cleanContact'. Calling ${bestChoice.first}."
        }

        val exactContact = foundContacts.first()
        dialOrCallNumber(exactContact.second)
        return "Calling ${exactContact.first}."
    }

    /**
     * Helper to dial/call a specific phone number using action dial
     */
    private fun dialOrCallNumber(number: String): String {
        val cleanNumber = number.replace(" ", "")
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Opening dialer with $number."
    }

    /**
     * Reads and speaks the current time and date.
     */
    private fun showTimeAction(): String {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val now = Date()
        val timeString = timeFormat.format(now)
        val dateString = dateFormat.format(now)
        return "The time is $timeString on $dateString."
    }

    /**
     * Fetches current battery percentage.
     */
    private fun batteryInfoAction(): String {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return "Your device is at $pct% battery capacity."
    }

    /**
     * Fetches current storage info.
     */
    private fun deviceStorageAction(): String {
        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        val totalBytes = stat.blockSizeLong * stat.blockCountLong
        val freeGb = bytesAvailable / (1024 * 1024 * 1024)
        val totalGb = totalBytes / (1024 * 1024 * 1024)
        return "You have $freeGb GB free out of $totalGb GB total storage."
    }

    /**
     * Opens system settings or specific settings page.
     */
    private fun openSettingsAction(settingName: String?): String {
        val intent = when (settingName?.lowercase(Locale.getDefault())?.trim()) {
            "wifi", "internet", "wi-fi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
            "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            "display", "brightness", "screen" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            "battery", "power" -> Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
            "location", "gps" -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            "apps", "applications" -> Intent(Settings.ACTION_APPLICATION_SETTINGS)
            "sound", "volume", "audio" -> Intent(Settings.ACTION_SOUND_SETTINGS)
            else -> Intent(Settings.ACTION_SETTINGS)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
        return if (settingName != null) {
            "Opening $settingName settings."
        } else {
            "Opening system settings."
        }
    }

    /**
     * Toggle flashlight camera flashlight.
     */
    private fun flashlightToggleAction(state: String?): String {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull()
            if (cameraId == null) {
                return "Flashlight is not available on this device."
            }
            val turnOn = state?.lowercase(Locale.getDefault()) == "on" || state == "enable"
            cameraManager.setTorchMode(cameraId, turnOn)
            if (turnOn) "Flashlight is now on." else "Flashlight is now off."
        } catch (e: Exception) {
            // System restriction - redirect to settings or show message
            "Unable to access the flashlight. Please check if camera permissions are active."
        }
    }

    /**
     * Adjust music volume.
     */
    private fun volumeControlAction(action: String?, state: String?): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return when {
            state == "mute" -> {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
                "Media volume muted."
            }
            state == "unmute" -> {
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume / 2, AudioManager.FLAG_SHOW_UI)
                "Media volume unmuted."
            }
            action == "increase" || action == "up" -> {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                "Increasing media volume."
            }
            action == "decrease" || action == "down" -> {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                "Decreasing media volume."
            }
            else -> "Managing volume settings."
        }
    }

    /**
     * Sets an alarm with implicit Alarm intent.
     */
    private fun setAlarmAction(timeStr: String?, hourStr: String?, minuteStr: String?, ampmStr: String?): String {
        var hour = 7
        var minute = 0
        var ampm = ""

        try {
            if (!hourStr.isNullOrEmpty()) {
                hour = hourStr.toInt()
                minute = minuteStr?.toIntOrNull() ?: 0
                ampm = ampmStr?.lowercase(Locale.getDefault()) ?: ""
                if (ampm == "pm" && hour < 12) hour += 12
                if (ampm == "am" && hour == 12) hour = 0
            } else if (!timeStr.isNullOrEmpty()) {
                // Parse strings like "8:30" or "8 am"
                val cleanTime = timeStr.lowercase(Locale.getDefault()).replace(" ", "")
                val parts = cleanTime.split(":")
                if (parts.size >= 2) {
                    hour = parts[0].filter { it.isDigit() }.toInt()
                    val minutePart = parts[1]
                    minute = minutePart.filter { it.isDigit() }.toInt()
                    if (minutePart.contains("pm") && hour < 12) hour += 12
                    if (minutePart.contains("am") && hour == 12) hour = 0
                } else {
                    val digits = cleanTime.filter { it.isDigit() }
                    if (digits.isNotEmpty()) {
                        hour = digits.toInt()
                        if (cleanTime.contains("pm") && hour < 12) hour += 12
                        if (cleanTime.contains("am") && hour == 12) hour = 0
                    }
                }
            } else {
                return "I couldn't parse the alarm time. Try saying 'set alarm for 8:30 am'."
            }

            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, "Jaxon Alarm")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            val displayHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
            val displayMin = String.format(Locale.getDefault(), "%02d", minute)
            val displayAmPm = if (hour >= 12) "PM" else "AM"

            return "Setting alarm for $displayHour:$displayMin $displayAmPm."
        } catch (e: Exception) {
            return "Unable to set alarm automatically. Try checking the alarm clock settings."
        }
    }

    /**
     * Set a countdown timer using Alarm Intents.
     */
    private fun setTimerAction(durationStr: String?, unitStr: String?): String {
        if (durationStr.isNullOrBlank()) {
            return "How long would you like to set the timer for?"
        }

        return try {
            val durationValue = durationStr.toInt()
            val unit = unitStr?.lowercase(Locale.getDefault()) ?: "minute"

            val totalSeconds = when {
                unit.startsWith("second") || unit.startsWith("sec") -> durationValue
                unit.startsWith("hour") || unit.startsWith("hr") -> durationValue * 3600
                else -> durationValue * 60 // defaults to minutes
            }

            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, totalSeconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, "Jaxon Timer")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Starting timer for $durationValue $unit."
        } catch (e: Exception) {
            "Unable to set timer automatically."
        }
    }

    /**
     * Map navigation in Google Maps app.
     */
    private fun mapNavigationAction(location: String?): String {
        if (location.isNullOrBlank()) {
            return "Please specify a location for navigation."
        }

        return try {
            val gmmIntentUri = Uri.parse("google.navigation:q=" + Uri.encode(location))
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
                "Navigating to $location."
            } else {
                // Fallback to web browser maps
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(location))).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                "Navigating to $location in web browser."
            }
        } catch (e: Exception) {
            "Unable to launch navigation."
        }
    }

    /**
     * Google web search query.
     */
    private fun launchSearchAction(query: String?): String {
        if (query.isNullOrBlank()) {
            return "What would you like to search for?"
        }

        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Searching Google for '$query'."
        } catch (e: Exception) {
            // Browser fallback
            val searchUri = Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))
            val browserIntent = Intent(Intent.ACTION_VIEW, searchUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            "Searching the web for '$query'."
        }
    }

    /**
     * Local media playback key codes.
     */
    private fun mediaControlAction(action: String?): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val keyCode = when (action?.lowercase(Locale.getDefault())?.trim()) {
            "play" -> KeyEvent.KEYCODE_MEDIA_PLAY
            "pause" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "stop" -> KeyEvent.KEYCODE_MEDIA_STOP
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "resume" -> KeyEvent.KEYCODE_MEDIA_PLAY
            else -> null
        }

        if (keyCode != null) {
            val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            audioManager.dispatchMediaKeyEvent(eventDown)
            audioManager.dispatchMediaKeyEvent(eventUp)
            return "Executing media ${action} command."
        }

        return "Unsupported media command."
    }
}
