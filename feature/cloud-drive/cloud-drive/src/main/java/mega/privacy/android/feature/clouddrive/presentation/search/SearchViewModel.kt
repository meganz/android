package mega.privacy.android.feature.clouddrive.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.search.SearchParameters
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.search.SearchUseCase
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.NodesLoadingState
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchUiAction
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchUiState
import mega.privacy.android.navigation.destination.SearchNavKey
import kotlin.coroutines.cancellation.CancellationException

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = SearchViewModel.Factory::class)
class SearchViewModel @AssistedInject constructor(
    @Assisted private val navKey: SearchNavKey,
    private val searchUseCase: SearchUseCase,
    private val cancelCancelTokenUseCase: CancelCancelTokenUseCase,
    private val nodeUiItemMapper: NodeUiItemMapper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            searchQueryFlow
                .debounce(SEARCH_DEBOUNCE_MS)
                .collectLatest { query -> performSearch(query) }
        }
    }

    fun processAction(action: SearchUiAction) {
        when (action) {
            is SearchUiAction.UpdateSearchText -> updateSearchText(action.text)
            is SearchUiAction.ItemClicked -> {} // TODO
            is SearchUiAction.ItemLongClicked -> {} // TODO
            is SearchUiAction.ChangeViewTypeClicked -> {} // TODO
            is SearchUiAction.OpenedFileNodeHandled -> {} // TODO
            is SearchUiAction.SelectAllItems -> {} // TODO
            is SearchUiAction.DeselectAllItems -> {} // TODO
            is SearchUiAction.NavigateToFolderEventConsumed -> {} // TODO
            is SearchUiAction.NavigateBackEventConsumed -> {} // TODO
            is SearchUiAction.OverQuotaConsumptionWarning -> {} // TODO
        }
    }

    private fun updateSearchText(text: String) {
        // Instantly show loading for initial search
        val nodesLoadingState = if (uiState.value.searchText.isEmpty()) {
            NodesLoadingState.Loading
        } else {
            uiState.value.nodesLoadingState
        }
        _uiState.update {
            it.copy(
                searchText = text
            )
        }
        searchQueryFlow.value = text
    }

    private suspend fun performSearch(query: String) {
        if (query.isEmpty()) {
            _uiState.update { state ->
                state.copy(
                    items = emptyList(),
                    searchedQuery = query,
                    nodesLoadingState = NodesLoadingState.Idle,
                )
            }
            return
        }

        _uiState.update { it.copy(nodesLoadingState = NodesLoadingState.Loading) }

        runCatching {
            cancelCancelTokenUseCase()
            val nodes = searchUseCase(
                parentHandle = NodeId(navKey.parentHandle),
                nodeSourceType = navKey.nodeSourceType,
                searchParameters = SearchParameters(
                    query = query
                ),
                isSingleActivityEnabled = true
            )
            val nodeUiItems = nodeUiItemMapper(
                nodeList = nodes,
                nodeSourceType = navKey.nodeSourceType,
                existingItems = _uiState.value.items,
            )
            _uiState.update { state ->
                state.copy(
                    items = nodeUiItems,
                    searchedQuery = query,
                    nodesLoadingState = NodesLoadingState.FullyLoaded,
                    isHiddenNodeSettingsLoading = false
                )
            }
        }.onFailure { throwable ->
            if (throwable !is CancellationException) {
                _uiState.update { state ->
                    state.copy(
                        items = emptyList(),
                        nodesLoadingState = NodesLoadingState.FullyLoaded,
                    )
                }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: SearchNavKey): SearchViewModel
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}

