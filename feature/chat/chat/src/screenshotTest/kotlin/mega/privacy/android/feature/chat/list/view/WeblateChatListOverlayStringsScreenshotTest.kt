package mega.privacy.android.feature.chat.list.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.test.weblate.WeblateContentDescriptionScreenshot
import mega.privacy.android.domain.entity.chat.ChatStatus
import mega.privacy.android.domain.entity.chat.MeetingTooltipItem
import mega.privacy.android.feature.chat.list.model.ChatListTabState
import mega.privacy.android.feature.chat.list.model.ChatListUiState
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Weblate reference renders for the two chat list strings that only ever appear in something the
 * screenshot engine cannot capture: the do-not-disturb label, which lives inside the top app bar's
 * overflow dropdown, and the meetings onboarding tooltip body, which lives inside a
 * [androidx.compose.ui.window.Popup].
 *
 * [WeblateContentDescriptionScreenshot] assembles a MEGA tooltip bubble directly in the hierarchy
 * and points it at the control the string belongs to, so translators see the text together with
 * where it comes from.
 */
class WeblateChatListOverlayStringsScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun DoNotDisturbOverflowAction() {
        AndroidThemeForPreviews {
            WeblateContentDescriptionScreenshot(
                description = stringResource(sharedR.string.title_dialog_mute_chat_notifications),
                tooltipAlignment = Alignment.TopEnd,
                tooltipOffset = DpOffset(x = (-8).dp, y = 64.dp),
            ) {
                Column {
                    ChatListTopAppBar(
                        chatStatus = ChatStatus.Online,
                        isSearchAvailable = true,
                        hasArchivedChats = true,
                        navigationType = AppBarNavigationType.None,
                        onPresenceClick = {},
                        onSearchClick = {},
                        onArchivedClick = {},
                        onOpenLinkClick = {},
                        onDoNotDisturbClick = {},
                    )
                    Spacer(modifier = Modifier.height(160.dp))
                }
            }
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun CreateMeetingTooltipBody() {
        AndroidThemeForPreviews {
            WeblateContentDescriptionScreenshot(
                description = stringResource(sharedR.string.meeting_list_tooltip_fab_description),
                tooltipAlignment = Alignment.BottomEnd,
                tooltipOffset = DpOffset(x = (-16).dp, y = (-120).dp),
            ) {
                ChatListScreen(
                    uiState = ChatListUiState.Data(
                        chats = ChatListTabState.Empty,
                        meetings = ChatListTabState.Results(
                            persistentListOf(
                                meetingUiItem(1L, "Weekly stand-up"),
                                meetingUiItem(2L, "Design review"),
                            )
                        ),
                        chatsHaveUnread = false,
                        meetingsHaveUnread = false,
                        hasAnyContact = true,
                        hasArchivedChats = true,
                        isSearchAvailable = true,
                        chatStatus = ChatStatus.Online,
                        isDoNotDisturbEnabled = false,
                        meetingTooltip = MeetingTooltipItem.NONE,
                    ),
                    showMeetingTab = true,
                    onBackClick = {},
                    onItemClick = {},
                    onNewChatClick = {},
                    onInviteFriendClick = {},
                    onMeetingActionClick = {},
                    onArchivedClick = {},
                    onOpenLinkClick = {},
                    onSearchQueryChange = {},
                )
            }
        }
    }

    private fun meetingUiItem(chatId: Long, title: String) = ChatRoomUiItem(
        chatId = chatId,
        title = title,
        lastMessage = "Anna: See you all soon!",
        lastTimestampFormatted = "Today 14:25",
        header = null,
        pendingMeeting = null,
        showRecurringIcon = false,
        unreadCount = 0,
        isMuted = false,
        highlight = false,
        isNoteToSelf = false,
        avatar = ChatRoomUiItem.ChatRoomUiAvatar.Meeting,
        status = ContactItemStatus.Unknown,
        hasOngoingCall = false,
    )
}
