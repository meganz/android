package mega.privacy.android.domain.entity.settings

import mega.privacy.android.domain.entity.VideoQuality

/**
 * Data class for storing chat settings.
 *
 * @property notificationsSound
 * @property vibrationEnabled
 * @property videoQuality
 */
data class ChatSettings(
    val notificationsSound: String = "",
    val vibrationEnabled: String = VIBRATION_ON,
    val videoQuality: String = VideoQuality.MEDIUM.value.toString(),
) {
    companion object {
        /**
         * Notification vibration on.
         */
        const val VIBRATION_ON = true.toString()

        /**
         * Notification vibration off.
         */
        const val VIBRATION_OFF = false.toString()
    }
}