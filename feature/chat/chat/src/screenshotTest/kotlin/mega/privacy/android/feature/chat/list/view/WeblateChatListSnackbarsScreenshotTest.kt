package mega.privacy.android.feature.chat.list.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.android.tools.screenshot.PreviewTest
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.test.weblate.WeblateSnackbarScreenshot
import mega.privacy.android.domain.entity.chat.ChatStatus
import mega.privacy.android.feature.chat.list.archived.model.ArchivedChatsUiState
import mega.privacy.android.feature.chat.list.archived.view.ArchivedChatsScreen
import mega.privacy.android.feature.chat.list.model.ChatListTabState
import mega.privacy.android.feature.chat.list.model.ChatListUiState
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Weblate reference renders for the snackbars the chat list and the archived chats list report
 * their actions with.
 *
 * A snackbar animates in from the activity-scoped snackbar host, so it never appears in a static
 * render of the screen itself — [WeblateSnackbarScreenshot] pins a MEGA-styled one over the screen
 * content instead, giving translators the message in the context it actually shows up in.
 */
class WeblateChatListSnackbarsScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ArchiveChatSuccessSnackbar() {
        AndroidThemeForPreviews {
            WeblateSnackbarScreenshot(
                snackbarText = stringResource(sharedR.string.success_archive_chat, CHAT_TITLE),
            ) {
                ChatListForPreview()
            }
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ArchiveChatErrorSnackbar() {
        AndroidThemeForPreviews {
            WeblateSnackbarScreenshot(
                snackbarText = stringResource(sharedR.string.error_archive_chat, CHAT_TITLE),
            ) {
                ChatListForPreview()
            }
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ArchiveSeveralChatsSnackbar() {
        AndroidThemeForPreviews {
            WeblateSnackbarScreenshot(
                snackbarText = pluralStringResource(
                    sharedR.plurals.chat_list_chats_archived_message,
                    SEVERAL_CHATS,
                    SEVERAL_CHATS,
                ),
            ) {
                ChatListForPreview()
            }
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ClearChatHistorySuccessSnackbar() {
        AndroidThemeForPreviews {
            WeblateSnackbarScreenshot(
                snackbarText = stringResource(sharedR.string.clear_history_success),
            ) {
                ChatListForPreview()
            }
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ClearChatHistoryErrorSnackbar() {
        AndroidThemeForPreviews {
            WeblateSnackbarScreenshot(
                snackbarText = stringResource(sharedR.string.clear_history_error),
            ) {
                ChatListForPreview()
            }
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChangeStatusErrorSnackbar() {
        AndroidThemeForPreviews {
            WeblateSnackbarScreenshot(
                snackbarText = stringResource(sharedR.string.changing_status_error),
            ) {
                ChatListForPreview()
            }
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun UnarchiveChatSuccessSnackbar() {
        AndroidThemeForPreviews {
            WeblateSnackbarScreenshot(
                snackbarText = stringResource(
                    sharedR.string.success_unarchive_chat,
                    ARCHIVED_CHAT_TITLE,
                ),
            ) {
                ArchivedChatsForPreview()
            }
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun UnarchiveChatErrorSnackbar() {
        AndroidThemeForPreviews {
            WeblateSnackbarScreenshot(
                snackbarText = stringResource(
                    sharedR.string.error_unarchive_chat,
                    ARCHIVED_CHAT_TITLE,
                ),
            ) {
                ArchivedChatsForPreview()
            }
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun UnarchiveSeveralChatsSnackbar() {
        AndroidThemeForPreviews {
            WeblateSnackbarScreenshot(
                snackbarText = pluralStringResource(
                    sharedR.plurals.chat_list_chats_unarchived_message,
                    SEVERAL_CHATS,
                    SEVERAL_CHATS,
                ),
            ) {
                ArchivedChatsForPreview()
            }
        }
    }

    @Composable
    private fun ChatListForPreview() {
        ChatListScreen(
            uiState = ChatListUiState.Data(
                chats = ChatListTabState.Results(
                    persistentListOf(
                        chatRoomUiItem(1L, CHAT_TITLE, "Anna: See you all soon!"),
                        chatRoomUiItem(2L, "Bob Gonzalez", "Sounds good"),
                        chatRoomUiItem(3L, "Product sync", "Carla: Pushed the update"),
                    )
                ),
                meetings = ChatListTabState.Empty,
                chatsHaveUnread = false,
                meetingsHaveUnread = false,
                hasAnyContact = true,
                hasArchivedChats = true,
                isSearchAvailable = true,
                chatStatus = ChatStatus.Online,
            ),
            showMeetingTab = false,
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

    @Composable
    private fun ArchivedChatsForPreview() {
        ArchivedChatsScreen(
            uiState = ArchivedChatsUiState.Data(
                persistentListOf(
                    chatRoomUiItem(11L, ARCHIVED_CHAT_TITLE, "Dan: Wrapping this up"),
                    chatRoomUiItem(12L, "Summer offsite", "Eve: Thanks everyone!"),
                    chatRoomUiItem(13L, "Beta testers", "Frank: Filed the last report"),
                )
            ),
            onBackClick = {},
            onItemClick = {},
        )
    }

    private fun chatRoomUiItem(chatId: Long, title: String, lastMessage: String) = ChatRoomUiItem(
        chatId = chatId,
        title = title,
        lastMessage = lastMessage,
        lastTimestampFormatted = "Today 14:25",
        header = null,
        pendingMeeting = null,
        showRecurringIcon = false,
        unreadCount = 0,
        isMuted = false,
        highlight = false,
        isNoteToSelf = false,
        avatar = ChatRoomUiItem.ChatRoomUiAvatar.Group,
        status = ContactItemStatus.Unknown,
        hasOngoingCall = false,
    )

    private companion object {
        const val CHAT_TITLE = "Design Team"
        const val ARCHIVED_CHAT_TITLE = "Old Project"
        const val SEVERAL_CHATS = 3
    }
}
