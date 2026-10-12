package mega.privacy.android.app.meeting

import android.content.pm.ServiceInfo
import android.os.Build

/**
 * Selects the foreground service type that [CallService] passes to `startForeground`.
 *
 * From API 34 a type may only be requested when its prerequisite permissions are granted,
 * otherwise `startForeground` throws a `SecurityException`, so each type is added only
 * when its permissions are held.
 *
 * @param sdkInt the device API level
 * @param hasPhoneCallPermissions whether FOREGROUND_SERVICE_PHONE_CALL and MANAGE_OWN_CALLS are granted
 * @param hasMicrophonePermissions whether FOREGROUND_SERVICE_MICROPHONE and RECORD_AUDIO are granted
 * @param hasCameraPermissions whether FOREGROUND_SERVICE_CAMERA and CAMERA are granted
 * @return the type flags, or null when the API level does not support foreground service types
 */
internal fun callServiceForegroundType(
    sdkInt: Int,
    hasPhoneCallPermissions: Boolean,
    hasMicrophonePermissions: Boolean,
    hasCameraPermissions: Boolean,
): Int? = when {
    sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
        val phoneCall =
            if (hasPhoneCallPermissions) ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL else 0
        val microphone =
            if (hasMicrophonePermissions) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
        val camera =
            if (hasCameraPermissions) ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA else 0
        phoneCall or microphone or camera
    }

    sdkInt >= Build.VERSION_CODES.R ->
        ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE

    sdkInt >= Build.VERSION_CODES.Q -> ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL

    else -> null
}
