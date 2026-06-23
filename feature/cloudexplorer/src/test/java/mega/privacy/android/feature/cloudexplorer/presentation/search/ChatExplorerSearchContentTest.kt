package mega.privacy.android.feature.cloudexplorer.presentation.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.palm.composestateevents.consumed
import kotlinx.coroutines.flow.MutableStateFlow
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer.CHAT_EXPLORER_LIST_TAG
import mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer.ChatExplorerUiState
import mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer.ChatExplorerViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer.rememberChatExplorerSelectionState
import mega.privacy.android.shared.chats.model.ChatExplorerUiItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
internal class ChatExplorerSearchContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the loading state is shown while the chat search is loading`() {
        setContent(chatState = ChatExplorerUiState.Loading)

        composeTestRule.onNodeWithTag(EXPLORER_SEARCH_LOADING_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the empty results view is shown when the chat search returns nothing`() {
        setContent(chatState = dataWith(ChatExplorerUiState.Items.Empty))

        composeTestRule.onNodeWithTag(EXPLORER_SEARCH_EMPTY_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the chat search results list is shown`() {
        setContent(
            chatState = dataWith(
                ChatExplorerUiState.Items(
                    noteToSelf = null,
                    recents = listOf(groupChat(1L)),
                    others = emptyList(),
                )
            )
        )

        composeTestRule.onNodeWithTag(CHAT_EXPLORER_LIST_TAG).assertIsDisplayed()
    }

    private fun setContent(chatState: ChatExplorerUiState) {
        val searchViewModel = mock<ExplorerSearchViewModel> {
            on { uiState } doReturn MutableStateFlow(
                ExplorerSearchUiState.Data(debouncedQuery = QUERY, recentSearches = emptyList())
            )
        }
        val chatViewModel = mock<ChatExplorerViewModel> {
            on { uiState } doReturn MutableStateFlow(chatState)
        }
        composeTestRule.setContent {
            Content(
                viewModelStoreOwnerOf(
                    ExplorerSearchViewModel::class.java to searchViewModel,
                    ChatExplorerViewModel::class.java to chatViewModel,
                )
            )
        }
    }

    @Composable
    private fun Content(owner: ViewModelStoreOwner) {
        AndroidThemeForPreviews {
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                ChatExplorerSearchContent(
                    query = QUERY,
                    onQueryChanged = {},
                    chatExplorerSelectionState = rememberChatExplorerSelectionState(),
                    isProcessingAction = false,
                )
            }
        }
    }

    private fun dataWith(searchResults: ChatExplorerUiState.Items) = ChatExplorerUiState.Data(
        items = ChatExplorerUiState.Items.Empty,
        newChatCreatedEvent = consumed(),
        chatsReadyToShareEvent = consumed(),
        searchResults = searchResults,
        isConnected = true,
    )

    private fun groupChat(id: Long) = ChatExplorerUiItem.GroupChat(
        id = id,
        title = "Group $id",
        participants = 2,
        isSelected = false,
        isEnabled = true,
        isArchived = false,
        lastTimestamp = 0L,
    )

    private companion object {
        const val QUERY = "report"
    }
}
