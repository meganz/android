package mega.privacy.android.app.presentation.chat.list.toolbar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.chat.list.model.ChatsTabState
import mega.privacy.android.app.presentation.meeting.model.NoteToSelfChatUIState
import mega.privacy.android.legacy.core.ui.controls.appbar.SEARCH_TOOLBAR_BACK_BUTTON_TEST_TAG
import mega.privacy.android.legacy.core.ui.controls.appbar.SEARCH_TOOLBAR_SEARCH_BUTTON_TEST_TAG
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ChatListToolBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val mockOnNavigationClick = mock<() -> Unit>()
    private val mockOnChangeUserStatus = mock<() -> Unit>()
    private val mockOnSearchTextChange = mock<(String) -> Unit>()
    private val mockOnSearchCloseClicked = mock<() -> Unit>()
    private val mockOnOpenLinkActionClick = mock<() -> Unit>()
    private val mockOnDoNotDisturbActionClick = mock<() -> Unit>()
    private val mockOnArchivedActionClick = mock<() -> Unit>()

    @Test
    fun `test that search button is displayed when conditions are met`() {
        composeRule.setContent {
            OriginalTheme(isDark = false) {
                ChatListToolBar(
                    state = ChatsTabState(
                        areChatsOrMeetingLoading = false,
                        isEmptyChatsOrMeetings = false,
                        onlyNoteToSelfChat = false
                    ),
                    noteToSelfChatState = NoteToSelfChatUIState(isNoteToSelfChatEmpty = false),
                    onNavigationClick = mockOnNavigationClick,
                    onChangeUserStatus = mockOnChangeUserStatus,
                    onSearchTextChange = mockOnSearchTextChange,
                    onSearchCloseClicked = mockOnSearchCloseClicked,
                    onOpenLinkActionClick = mockOnOpenLinkActionClick,
                    onDoNotDisturbActionClick = mockOnDoNotDisturbActionClick,
                    onArchivedActionClick = mockOnArchivedActionClick
                )
            }
        }

        composeRule.onNodeWithTag(SEARCH_TOOLBAR_SEARCH_BUTTON_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that search button is not displayed when loading`() {
        composeRule.setContent {
            OriginalTheme(isDark = false) {
                ChatListToolBar(
                    state = ChatsTabState(
                        areChatsOrMeetingLoading = true,
                        isEmptyChatsOrMeetings = false,
                        onlyNoteToSelfChat = false
                    ),
                    noteToSelfChatState = NoteToSelfChatUIState(),
                    onNavigationClick = mockOnNavigationClick,
                    onChangeUserStatus = mockOnChangeUserStatus,
                    onSearchTextChange = mockOnSearchTextChange,
                    onSearchCloseClicked = mockOnSearchCloseClicked,
                    onOpenLinkActionClick = mockOnOpenLinkActionClick,
                    onDoNotDisturbActionClick = mockOnDoNotDisturbActionClick,
                    onArchivedActionClick = mockOnArchivedActionClick
                )
            }
        }

        composeRule.onNodeWithTag(SEARCH_TOOLBAR_SEARCH_BUTTON_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that archived menu action is not displayed when hasArchivedChats is false`() {
        composeRule.setContent {
            OriginalTheme(isDark = false) {
                ChatListToolBar(
                    state = ChatsTabState(
                        hasArchivedChats = false,
                        areChatsOrMeetingLoading = false,
                        isEmptyChatsOrMeetings = false
                    ),
                    noteToSelfChatState = NoteToSelfChatUIState(isNoteToSelfChatEmpty = false),
                    onNavigationClick = mockOnNavigationClick,
                    onChangeUserStatus = mockOnChangeUserStatus,
                    onSearchTextChange = mockOnSearchTextChange,
                    onSearchCloseClicked = mockOnSearchCloseClicked,
                    onOpenLinkActionClick = mockOnOpenLinkActionClick,
                    onDoNotDisturbActionClick = mockOnDoNotDisturbActionClick,
                    onArchivedActionClick = mockOnArchivedActionClick
                )
            }
        }

        composeRule.onNodeWithTag(ChatListMenuAction.TEST_TAG_CHAT_LIST_ARCHIVED_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that navigation button click triggers callback`() {
        composeRule.setContent {
            OriginalTheme(isDark = false) {
                ChatListToolBar(
                    state = ChatsTabState(),
                    noteToSelfChatState = NoteToSelfChatUIState(),
                    onNavigationClick = mockOnNavigationClick,
                    onChangeUserStatus = mockOnChangeUserStatus,
                    onSearchTextChange = mockOnSearchTextChange,
                    onSearchCloseClicked = mockOnSearchCloseClicked,
                    onOpenLinkActionClick = mockOnOpenLinkActionClick,
                    onDoNotDisturbActionClick = mockOnDoNotDisturbActionClick,
                    onArchivedActionClick = mockOnArchivedActionClick
                )
            }
        }

        composeRule.onNodeWithTag(SEARCH_TOOLBAR_BACK_BUTTON_TEST_TAG).performClick()
        verify(mockOnNavigationClick).invoke()
    }

    @Test
    fun `test that search button visibility logic works for note to self chat`() {
        composeRule.setContent {
            OriginalTheme(isDark = false) {
                ChatListToolBar(
                    state = ChatsTabState(
                        areChatsOrMeetingLoading = false,
                        isEmptyChatsOrMeetings = false,
                        onlyNoteToSelfChat = true
                    ),
                    noteToSelfChatState = NoteToSelfChatUIState(isNoteToSelfChatEmpty = true),
                    onNavigationClick = mockOnNavigationClick,
                    onChangeUserStatus = mockOnChangeUserStatus,
                    onSearchTextChange = mockOnSearchTextChange,
                    onSearchCloseClicked = mockOnSearchCloseClicked,
                    onOpenLinkActionClick = mockOnOpenLinkActionClick,
                    onDoNotDisturbActionClick = mockOnDoNotDisturbActionClick,
                    onArchivedActionClick = mockOnArchivedActionClick
                )
            }
        }

        // When only note to self chat exists and it's empty, search should not be shown
        composeRule.onNodeWithTag(SEARCH_TOOLBAR_SEARCH_BUTTON_TEST_TAG).assertDoesNotExist()
    }
}
