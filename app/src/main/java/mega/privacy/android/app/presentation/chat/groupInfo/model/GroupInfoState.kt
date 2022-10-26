package mega.privacy.android.app.presentation.chat.groupInfo.model

/**
 * Group chat info UI state
 *
 * @property chatId                 The chat id.
 * @property error                  String resource id for showing an error.
 * @property resultSetOpenInvite    True if it's enabled, false if not.
 */
data class GroupInfoState(
    val chatId: Long = -1L,
    val error: Int? = null,
    val resultSetOpenInvite: Boolean? = null,
)