package mega.privacy.android.feature.cloudexplorer.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.usecase.search.ClearRecentSearchesUseCase
import mega.privacy.android.domain.usecase.search.MonitorRecentSearchesUseCase
import mega.privacy.android.domain.usecase.search.SaveRecentSearchUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Screen-level ViewModel for the explorer search surface. Owns the query (so it and its debounced
 * value survive configuration changes), exposing it plus the global recent searches via [uiState].
 * Recording a query into recent searches is an explicit [saveRecentSearch] call, so node contexts
 * opt in while chat does not.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
internal class ExplorerSearchViewModel @Inject constructor(
    private val monitorRecentSearchesUseCase: MonitorRecentSearchesUseCase,
    private val saveRecentSearchUseCase: SaveRecentSearchUseCase,
    private val clearRecentSearchesUseCase: ClearRecentSearchesUseCase,
) : ViewModel() {

    private val queryChannel = Channel<String?>(Channel.CONFLATED)

    private val queryFlow = queryChannel.receiveAsFlow()
        .onStart { emit(null) }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), replay = 1)

    // Blank/closed queries settle immediately; non-blank ones debounce.
    private val debouncedQueryFlow = queryFlow
        .debounce { if (it.isNullOrBlank()) 0L else SEARCH_DEBOUNCE_MS }

    val uiState: StateFlow<ExplorerSearchUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            debouncedQueryFlow,
            monitorRecentSearchesUseCase()
                .map { it to false }
                .catch { Timber.e(it); emit(emptyList<String>() to false) }
                .onStart { emit(emptyList<String>() to true) },
        ) { debouncedQuery, (recentSearches, isLoading) ->
            // Stay Loading only until recent searches arrive and while nothing is being searched;
            // once a query is entered we show results regardless of recent-searches loading.
            if (isLoading && debouncedQuery.isNullOrBlank()) {
                ExplorerSearchUiState.Loading
            } else {
                ExplorerSearchUiState.Data(
                    debouncedQuery = debouncedQuery,
                    recentSearches = recentSearches,
                )
            }
        }.asUiStateFlow(viewModelScope, ExplorerSearchUiState.Loading)
    }

    /**
     * Updates the query. Pass `null` when the search is closed.
     */
    fun onQueryChanged(value: String?) {
        viewModelScope.launch { queryChannel.send(value) }
    }

    /**
     * Records [query] into the global recent searches; blank queries are ignored. Only node contexts
     * call this — chat opts out.
     */
    fun saveRecentSearch(query: String?) {
        if (query.isNullOrBlank()) return
        viewModelScope.launch {
            runCatching { saveRecentSearchUseCase(query) }.onFailure { Timber.e(it) }
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            runCatching { clearRecentSearchesUseCase() }.onFailure { Timber.e(it) }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
