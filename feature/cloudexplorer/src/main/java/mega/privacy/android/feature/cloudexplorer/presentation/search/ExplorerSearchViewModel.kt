package mega.privacy.android.feature.cloudexplorer.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.usecase.search.ClearRecentSearchesUseCase
import mega.privacy.android.domain.usecase.search.MonitorRecentSearchesUseCase
import mega.privacy.android.domain.usecase.search.SaveRecentSearchUseCase
import timber.log.Timber

/**
 * Screen-level ViewModel for the explorer search surface. Exposes a single [uiState] holding the
 * raw query, the debounced query that drives the per-tab searches, and the global recent searches.
 * The query is kept here (rather than in the UI) so it — and the debounced value driving the
 * search — survive configuration changes.
 *
 * [Args.recentSearchesEnabled] is `false` for chat search, which neither shows nor saves recent
 * searches into the global (node) history.
 */
@OptIn(FlowPreview::class)
@HiltViewModel(assistedFactory = ExplorerSearchViewModel.Factory::class)
internal class ExplorerSearchViewModel @AssistedInject constructor(
    private val monitorRecentSearchesUseCase: MonitorRecentSearchesUseCase,
    private val saveRecentSearchUseCase: SaveRecentSearchUseCase,
    private val clearRecentSearchesUseCase: ClearRecentSearchesUseCase,
    @Assisted private val args: Args,
) : ViewModel() {

    private val queryChannel = Channel<String?>(Channel.CONFLATED)

    private val queryFlow = queryChannel.receiveAsFlow()
        .onStart { emit(null) }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), replay = 1)

    // Blank/closed queries settle immediately; non-blank ones debounce. Settled, non-blank queries
    // are persisted as a side effect (the DAO de-duplicates typed prefixes); chat search opts out.
    private val debouncedQueryFlow = queryFlow
        .debounce { if (it.isNullOrBlank()) 0L else SEARCH_DEBOUNCE_MS }
        .onEach { query ->
            if (args.recentSearchesEnabled && !query.isNullOrBlank()) {
                runCatching { saveRecentSearchUseCase(query) }.onFailure { Timber.e(it) }
            }
        }

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

    fun clearRecentSearches() {
        viewModelScope.launch {
            runCatching { clearRecentSearchesUseCase() }.onFailure { Timber.e(it) }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(args: Args): ExplorerSearchViewModel
    }

    /**
     * @property recentSearchesEnabled whether the active context participates in recent searches.
     */
    data class Args(
        val recentSearchesEnabled: Boolean,
    )

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
