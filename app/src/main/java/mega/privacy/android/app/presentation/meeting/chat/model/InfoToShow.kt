package mega.privacy.android.app.presentation.meeting.chat.model

import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption

/**
 * Data class storing all the required data for showing some info to the user.
 *
 * @property stringId String id.
 * @property args List of strings in case the string admits some params.
 * @property inviteContactToChatResult [InviteContactToChatResult]
 * @property chatPushNotificationMuteOption [ChatPushNotificationMuteOption]
 */
data class InfoToShow(
    @StringRes val stringId: Int? = null,
    val args: List<String> = emptyList(),
    val inviteContactToChatResult: InviteContactToChatResult? = null,
    val chatPushNotificationMuteOption: ChatPushNotificationMuteOption? = null,
)
