package mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.chats.model.ChatExplorerUiItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ChatExplorerContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the loading state is shown when the ui state is Loading`() {
        setContent(uiState = ChatExplorerUiState.Loading)

        composeTestRule.onNodeWithTag(CHAT_EXPLORER_LOADING_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the empty state is shown when there are no chats`() {
        setContent(uiState = dataWith(recents = emptyList(), others = emptyList()))

        composeTestRule.onNodeWithTag(CHAT_EXPLORER_EMPTY_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CHAT_EXPLORER_NEW_GROUP_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the chat list and its rows are shown when there are chats`() {
        setContent(uiState = dataWith(recents = listOf(groupChat(RECENT_CHAT_ID))))

        composeTestRule.onNodeWithTag(CHAT_EXPLORER_LIST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CHAT_EXPLORER_ROW_TAG + RECENT_CHAT_ID).assertIsDisplayed()
    }

    @Test
    fun `test that clicking a chat row toggles that chat`() {
        var toggledChatId: Long? = null
        setContent(
            uiState = dataWith(recents = listOf(groupChat(RECENT_CHAT_ID))),
            onChatToggled = { toggledChatId = it },
        )

        composeTestRule.onNodeWithTag(CHAT_EXPLORER_ROW_TAG + RECENT_CHAT_ID).performClick()

        assertThat(toggledChatId).isEqualTo(RECENT_CHAT_ID)
    }

    @Test
    fun `test that the note to self recents and others sections all render`() {
        setContent(
            uiState = ChatExplorerUiState.Data(
                items = ChatExplorerUiState.Items(
                    noteToSelf = noteToSelf(NOTE_TO_SELF_ID),
                    recents = listOf(groupChat(RECENT_CHAT_ID)),
                    others = listOf(groupChat(OTHER_CHAT_ID)),
                ),
                newChatCreatedEvent = consumed(),
                chatsReadyToShareEvent = consumed(),
                searchResults = ChatExplorerUiState.Items.Empty,
                isConnected = true,
            ),
        )

        composeTestRule.onNodeWithTag(CHAT_EXPLORER_ROW_TAG + NOTE_TO_SELF_ID).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CHAT_EXPLORER_ROW_TAG + RECENT_CHAT_ID).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CHAT_EXPLORER_ROW_TAG + OTHER_CHAT_ID).assertIsDisplayed()
    }

    @Test
    fun `test that clicking a disabled chat does not toggle it`() {
        var toggledChatId: Long? = null
        setContent(
            uiState = dataWith(recents = listOf(groupChat(RECENT_CHAT_ID, isEnabled = false))),
            onChatToggled = { toggledChatId = it },
        )

        composeTestRule.onNodeWithTag(CHAT_EXPLORER_ROW_TAG + RECENT_CHAT_ID).performClick()

        assertThat(toggledChatId).isNull()
    }

    @Test
    fun `test that clicking a chat does not toggle it while an action is processing`() {
        var toggledChatId: Long? = null
        setContent(
            uiState = dataWith(recents = listOf(groupChat(RECENT_CHAT_ID))),
            isProcessingAction = true,
            onChatToggled = { toggledChatId = it },
        )

        composeTestRule.onNodeWithTag(CHAT_EXPLORER_ROW_TAG + RECENT_CHAT_ID).performClick()

        assertThat(toggledChatId).isNull()
    }

    @Test
    fun `test that clicking new group chat invokes the callback`() {
        var newGroupChatClicked = false
        setContent(
            uiState = dataWith(recents = listOf(groupChat(RECENT_CHAT_ID))),
            onNewGroupChatClick = { newGroupChatClicked = true },
        )

        composeTestRule.onNodeWithTag(CHAT_EXPLORER_NEW_GROUP_TAG).performClick()

        assertThat(newGroupChatClicked).isTrue()
    }

    private fun setContent(
        uiState: ChatExplorerUiState,
        isProcessingAction: Boolean = false,
        onNewGroupChatClick: () -> Unit = {},
        onChatToggled: (Long) -> Unit = {},
    ) {
        composeTestRule.setContent {
            AndroidThemeForPreviews {
                ChatExplorerContent(
                    uiState = uiState,
                    isProcessingAction = isProcessingAction,
                    selectedChatIds = emptySet(),
                    onNewGroupChatClick = onNewGroupChatClick,
                    onChatToggled = onChatToggled,
                )
            }
        }
    }

    private fun dataWith(
        recents: List<ChatExplorerUiItem> = emptyList(),
        others: List<ChatExplorerUiItem> = emptyList(),
    ) = ChatExplorerUiState.Data(
        items = ChatExplorerUiState.Items(
            noteToSelf = null,
            recents = recents,
            others = others,
        ),
        newChatCreatedEvent = consumed(),
        chatsReadyToShareEvent = consumed(),
        searchResults = ChatExplorerUiState.Items.Empty,
        isConnected = true,
    )

    private fun groupChat(id: Long, isEnabled: Boolean = true) = ChatExplorerUiItem.GroupChat(
        id = id,
        title = "Group $id",
        participants = 2,
        isSelected = false,
        isEnabled = isEnabled,
        isArchived = false,
        lastTimestamp = 0L,
    )

    private fun noteToSelf(id: Long) = ChatExplorerUiItem.NoteToSelf(
        id = id,
        isHint = false,
        isSelected = false,
        isEnabled = true,
        isArchived = false,
        lastTimestamp = 0L,
    )

    private companion object {
        const val NOTE_TO_SELF_ID = 10L
        const val RECENT_CHAT_ID = 1L
        const val OTHER_CHAT_ID = 2L
    }
}
