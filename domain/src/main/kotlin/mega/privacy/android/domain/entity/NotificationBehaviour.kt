package mega.privacy.android.domain.entity

import mega.privacy.android.domain.entity.settings.ChatSettings.Companion.VIBRATION_OFF

/**
 * Data class for storing all the required notification behaviour for pushes.
 *
 * @property sound Uri of the sound if any.
 * @property vibration Vibration behaviour.
 */
data class NotificationBehaviour(
    val sound: String? = null,
    val vibration: String = VIBRATION_OFF,
)
