package com.example.domain.permissions

import android.Manifest
import android.os.Build

enum class PermissionType {
    RUNTIME,
    SYSTEM_ALERT,
    BATTERY_OPTIMIZATION
}

data class PermissionItem(
    val id: String,
    val name: String,
    val permissionString: String?,
    val purpose: String,
    val explanation: String,
    val isRequired: Boolean,
    val type: PermissionType
)

object PermissionProvider {
    fun getPermissions(): List<PermissionItem> {
        val permissions = mutableListOf(
            PermissionItem(
                id = "microphone",
                name = "Microphone Access",
                permissionString = Manifest.permission.RECORD_AUDIO,
                purpose = "Required to capture and transcribe voice commands",
                explanation = "Since Jaxon is fully offline, voice recording is processed directly on your device and is never sent to remote servers.",
                isRequired = true,
                type = PermissionType.RUNTIME
            ),
            PermissionItem(
                id = "contacts",
                name = "Contacts",
                permissionString = Manifest.permission.READ_CONTACTS,
                purpose = "Allows calling contacts by name",
                explanation = "Required if you want to say 'Call Rahul' or search for contact phone numbers stored locally.",
                isRequired = false,
                type = PermissionType.RUNTIME
            ),
            PermissionItem(
                id = "phone",
                name = "Phone Control",
                permissionString = Manifest.permission.CALL_PHONE,
                purpose = "Allows initiating phone calls directly",
                explanation = "Allows Jaxon to place calls when you ask. If denied, Jaxon will open the Dialer prefilled instead.",
                isRequired = false,
                type = PermissionType.RUNTIME
            ),
            PermissionItem(
                id = "camera",
                name = "Camera (Flashlight)",
                permissionString = Manifest.permission.CAMERA,
                purpose = "Allows toggling flashlight via torch mode",
                explanation = "Android requires Camera permissions to toggle the camera flash when you say 'turn on flashlight'.",
                isRequired = false,
                type = PermissionType.RUNTIME
            ),
            PermissionItem(
                id = "location",
                name = "Location",
                permissionString = Manifest.permission.ACCESS_FINE_LOCATION,
                purpose = "Enables location-aware directions in maps",
                explanation = "Required if you want Jaxon to calculate routes from your current position or find local directories.",
                isRequired = false,
                type = PermissionType.RUNTIME
            )
        )

        // Android 13+ Notification Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(
                PermissionItem(
                    id = "notifications",
                    name = "Notifications",
                    permissionString = Manifest.permission.POST_NOTIFICATIONS,
                    purpose = "Displays the persistent foreground assistant controls",
                    explanation = "Allows Jaxon to post an ongoing notification with quick actions like 'Start Listening' and 'Stop Assistant'.",
                    isRequired = false,
                    type = PermissionType.RUNTIME
                )
            )
        }

        // Bluetooth Connect on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(
                PermissionItem(
                    id = "bluetooth",
                    name = "Bluetooth Connect",
                    permissionString = Manifest.permission.BLUETOOTH_CONNECT,
                    purpose = "Allows displaying and navigating bluetooth devices",
                    explanation = "Required when directing the device to connect or view local settings for external sound systems.",
                    isRequired = false,
                    type = PermissionType.RUNTIME
                )
            )
        }

        // System Alert Overlay & Battery Optimizations
        permissions.add(
            PermissionItem(
                id = "overlay",
                name = "Draw Over Other Apps",
                permissionString = null,
                purpose = "Allows launching listening UI from background services",
                explanation = "Enables the voice assistant panel to slide up over other applications when triggered from the background notification.",
                isRequired = false,
                type = PermissionType.SYSTEM_ALERT
            )
        )

        permissions.add(
            PermissionItem(
                id = "battery",
                name = "Ignore Battery Optimizations",
                permissionString = null,
                purpose = "Prevents system from shutting down Jaxon background service",
                explanation = "Allows Jaxon to remain active and respond quickly without being killed by system battery optimization policies.",
                isRequired = false,
                type = PermissionType.BATTERY_OPTIMIZATION
            )
        )

        return permissions
    }
}
