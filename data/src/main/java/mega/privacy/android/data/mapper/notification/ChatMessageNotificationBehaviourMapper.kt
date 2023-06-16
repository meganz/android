package mega.privacy.android.data.mapper.notification

import mega.privacy.android.domain.entity.NotificationBehaviour
import mega.privacy.android.domain.entity.settings.ChatSettings
import mega.privacy.android.domain.entity.settings.ChatSettings.Companion.VIBRATION_OFF
import mega.privacy.android.domain.entity.settings.ChatSettings.Companion.VIBRATION_ON
import javax.inject.Inject

/**
 * Mapper for converting data into [NotificationBehaviour].
 */
class ChatMessageNotificationBehaviourMapper @Inject constructor() {

    /**
     * Invoke.
     *
     * @param chatSettings [ChatSettings].
     * @param beep  Push notification flag indicating if the notification should beep or not.
     * @param defaultSound Default device sound.
     */
    operator fun invoke(
        chatSettings: ChatSettings?,
        beep: Boolean,
        defaultSound: String,
    ) = NotificationBehaviour(
        sound = when {
            beep && chatSettings != null -> chatSettings.notificationsSound.ifEmpty { defaultSound }
            beep -> defaultSound
            else -> null
        }, vibration = when {
            beep && chatSettings != null -> chatSettings.vibrationEnabled
            beep -> VIBRATION_ON
            else -> VIBRATION_OFF
        })
}