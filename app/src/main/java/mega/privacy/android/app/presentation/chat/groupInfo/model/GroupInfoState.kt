package mega.privacy.android.app.presentation.chat.groupInfo.model

import mega.privacy.android.domain.entity.chat.ChatCall

/**
 * Group chat info UI state
 *
 * @property chatId                 The chat id.
 * @property error                  String resource id for showing an error.
 * @property call                   [ChatCall]
 * @property resultSetOpenInvite    True if it's enabled, false if not.
 * @property isPushNotificationSettingsUpdatedEvent     Push notification settings updated event
 * @property showForceUpdateDialog  True, shows force update dialog to the user
 */
data class GroupInfoState(
    val chatId: Long = -1L,
    val call: ChatCall? = null,
    val error: Int? = null,
    val resultSetOpenInvite: Boolean? = null,
    val isPushNotificationSettingsUpdatedEvent: Boolean = false,
    val showForceUpdateDialog: Boolean = false,
)
