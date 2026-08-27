package mega.privacy.android.feature.chat.list.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.test.weblate.WeblateContentDescriptionScreenshot
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Weblate translator-reference screenshot for the ongoing-call icon content description
 * ([sharedR.string.chat_list_ongoing_call_content_description]).
 *
 * The string has no visible text, so [WeblateContentDescriptionScreenshot] renders the chat list
 * row with a tooltip bubble whose caret points at the ongoing-call icon.
 */
class WeblateChatListOngoingCallScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun OngoingCallContentDescription() {
        AndroidThemeForPreviews {
            WeblateContentDescriptionScreenshot(
                description = stringResource(
                    sharedR.string.chat_list_ongoing_call_content_description
                ),
                tooltipAlignment = Alignment.TopStart,
                tooltipOffset = DpOffset(x = 140.dp, y = 22.dp),
            ) {
                ChatRoomItemView(
                    item = ChatRoomUiItem(
                        chatId = 1L,
                        title = "Team call",
                        lastMessage = "Anna: Seeya all soon!",
                        lastTimestampFormatted = "Today 14:25",
                        scheduledTimestampFormatted = null,
                        unreadCount = 0,
                        isMuted = false,
                        highlight = false,
                        isNoteToSelf = false,
                        avatar = ChatRoomUiItem.ChatRoomUiAvatar.Group,
                        status = ContactItemStatus.Unknown,
                        hasOngoingCall = true,
                    ),
                    onItemClick = {},
                )
            }
        }
    }
}
