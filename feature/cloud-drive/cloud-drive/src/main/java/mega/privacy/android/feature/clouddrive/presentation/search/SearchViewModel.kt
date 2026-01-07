package mega.privacy.android.feature.clouddrive.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.search.SearchParameters
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.search.SearchUseCase
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.NodesLoadingState
import mega.privacy.android.feature.clouddrive.presentation.search.mapper.TypeFilterToSearchMapper
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchFilterResult
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchUiAction
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchUiState
import kotlin.coroutines.cancellation.CancellationException

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = SearchViewModel.Factory::class)
class SearchViewModel @AssistedInject constructor(
    @Assisted private val args: Args,
    private val searchUseCase: SearchUseCase,
    private val cancelCancelTokenUseCase: CancelCancelTokenUseCase,
    private val nodeUiItemMapper: NodeUiItemMapper,
    private val typeFilterToSearchMapper: TypeFilterToSearchMapper,
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
            is SearchUiAction.SelectFilter -> updateFilter(action.result)
            is SearchUiAction.ItemClicked -> onItemClicked(action.nodeUiItem)
            is SearchUiAction.ItemLongClicked -> {} // TODO
            is SearchUiAction.ChangeViewTypeClicked -> {} // TODO
            is SearchUiAction.OpenedFileNodeHandled -> onOpenedFileNodeHandled()
            is SearchUiAction.SelectAllItems -> {} // TODO
            is SearchUiAction.DeselectAllItems -> {} // TODO
            is SearchUiAction.NavigateToFolderEventConsumed -> onNavigateToFolderEventConsumed()
            is SearchUiAction.NavigateBackEventConsumed -> {} // TODO
            is SearchUiAction.OverQuotaConsumptionWarning -> {} // TODO
        }
    }

    private fun updateFilter(result: SearchFilterResult) {
        _uiState.update { state ->
            when (result) {
                is SearchFilterResult.Type -> state.copy(typeFilterOption = result.option)
                is SearchFilterResult.DateModified -> state.copy(dateModifiedFilterOption = result.option)
                is SearchFilterResult.DateAdded -> state.copy(dateAddedFilterOption = result.option)
            }
        }
        viewModelScope.launch { performSearch(_uiState.value.searchText) }
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
                parentHandle = NodeId(args.parentHandle),
                nodeSourceType = args.nodeSourceType,
                searchParameters = SearchParameters(
                    query = query,
                    searchCategory = typeFilterToSearchMapper(
                        typeFilterOption = uiState.value.typeFilterOption,
                        nodeSourceType = args.nodeSourceType
                    ),
                    modificationDate = uiState.value.dateModifiedFilterOption,
                    creationDate = uiState.value.dateAddedFilterOption,
                ),
                isSingleActivityEnabled = true
            )
            val nodeUiItems = nodeUiItemMapper(
                nodeList = nodes,
                nodeSourceType = args.nodeSourceType,
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

    /**
     * Handle item click - navigate to folder if it's a folder
     */
    private fun onItemClicked(nodeUiItem: NodeUiItem<TypedNode>) {
        // TODO handle selection mode

        when (val node = nodeUiItem.node) {
            is TypedFolderNode -> {
                _uiState.update { state ->
                    state.copy(
                        navigateToFolderEvent = triggered(node)
                    )
                }
            }

            is PublicLinkFile -> {
                _uiState.update { state ->
                    state.copy(
                        openedFileNode = node
                    )
                }
            }

            is TypedFileNode -> {
                _uiState.update { state ->
                    state.copy(
                        openedFileNode = node
                    )
                }
            }
        }
    }

    /**
     * Consume navigation event
     */
    private fun onNavigateToFolderEventConsumed() {
        _uiState.update { state ->
            state.copy(navigateToFolderEvent = consumed())
        }
    }

    /**
     * Handle the event when a file node is opened
     */
    private fun onOpenedFileNodeHandled() {
        _uiState.update { state ->
            state.copy(openedFileNode = null)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(args: Args): SearchViewModel
    }

    data class Args(
        val parentHandle: Long,
        val nodeSourceType: NodeSourceType,
    )

    companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}

