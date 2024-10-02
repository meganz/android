package mega.privacy.android.app.presentation.chat.groupInfo.model

import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.chat.ChatRoom

/**
 * Group chat info UI state
 *
 * @property chatId                                     The chat id.
 * @property error                                      String resource id for showing an error.
 * @property shouldShowUserLimitsWarning                True if it should show user limits warning, false if not.
 * @property resultSetOpenInvite                        True if it's enabled, false if not.
 * @property isPushNotificationSettingsUpdatedEvent     Push notification settings updated event
 * @property showForceUpdateDialog                      True, shows force update dialog to the user
 * @property isCallUnlimitedProPlanFeatureFlagEnabled   True if the call unlimited pro plan feature flag is enabled, false if not.
 * @property retentionTime                              The retention time.
 * @property call                                       The chat call.
 * @property chatRoom                                   The chat room.
 */
data class GroupInfoState(
    val chatId: Long = -1L,
    val shouldShowUserLimitsWarning: Boolean = false,
    val call: ChatCall? = null,
    val chatRoom: ChatRoom? = null,
    val error: Int? = null,
    val resultSetOpenInvite: Boolean? = null,
    val isPushNotificationSettingsUpdatedEvent: Boolean = false,
    val showForceUpdateDialog: Boolean = false,
    val isCallUnlimitedProPlanFeatureFlagEnabled: Boolean = false,
    val retentionTime: Long? = null,
)
