package mega.privacy.android.feature.cloudexplorer.presentation.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer.ChatExplorerList
import mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer.ChatExplorerSelectionState
import mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer.ChatExplorerUiState
import mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer.ChatExplorerViewModel
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Chat search results, reusing the browse [ChatExplorerViewModel]. Opts out of recent searches.
 */
@Composable
internal fun ChatExplorerSearchContent(
    query: String?,
    onQueryChanged: (String) -> Unit,
    chatExplorerSelectionState: ChatExplorerSelectionState,
    isProcessingAction: Boolean,
    modifier: Modifier = Modifier,
) = ExplorerSearchContent(
    query = query,
    onQueryChanged = onQueryChanged,
    modifier = modifier,
    recentSearchesEnabled = false,
    landingDescription = sharedR.string.search_landing_chat_explorer_subtitle,
) { debouncedQuery ->
    val viewModel = hiltViewModel<ChatExplorerViewModel>()
    val chatState by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    LaunchedEffect(debouncedQuery) {
        viewModel.onSearchQuery(
            debouncedQuery?.let { searchQuery ->
                ChatExplorerViewModel.ChatSearchInput(
                    query = searchQuery,
                    noteToSelfTitle = resources.getString(sharedR.string.chat_note_to_self_chat_title),
                )
            }
        )
    }
    val results = (chatState as? ChatExplorerUiState.Data)?.searchResults
        ?: ChatExplorerUiState.Items.Empty
    when {
        chatState is ChatExplorerUiState.Loading -> SearchLoadingState(modifier = modifier)
        results.noteToSelf == null && results.isEmpty -> SearchResultsEmptyView(modifier = modifier)
        else -> ChatExplorerList(
            items = results,
            isProcessingAction = isProcessingAction,
            selectedChatIds = chatExplorerSelectionState.selectedChatIds,
            onChatToggled = chatExplorerSelectionState::toggleSelection,
            modifier = modifier,
        )
    }
}
