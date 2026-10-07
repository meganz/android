package mega.privacy.android.feature.chat.giphy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.chat.Giphy
import mega.privacy.android.domain.usecase.chat.giphy.GetTrendingGiphysUseCase
import mega.privacy.android.domain.usecase.chat.giphy.SearchGiphysUseCase
import mega.privacy.android.feature.chat.giphy.model.GiphyEmptyState
import mega.privacy.android.feature.chat.giphy.model.GiphyPickerUiState
import mega.privacy.android.feature.chat.runCatchingCancellable
import timber.log.Timber
import javax.inject.Inject

/**
 * Loads trending GIFs, or the GIFs matching the search query, a page at a time.
 *
 * Pages already loaded for a query are kept for the lifetime of the screen, so going back to a
 * previous query (or to trending) shows its GIFs without requesting them again.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class GiphyPickerViewModel @Inject constructor(
    private val getTrendingGiphysUseCase: GetTrendingGiphysUseCase,
    private val searchGiphysUseCase: SearchGiphysUseCase,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val loadMoreRequests = Channel<Unit>(Channel.CONFLATED)
    private val loadedPages = mutableMapOf<String?, GiphyPages>()

    val uiState: StateFlow<GiphyPickerUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            query,
            query.map { it.takeUnless(String::isBlank) }
                .debounce { if (it == null) 0L else SEARCH_DEBOUNCE_MILLIS }
                .distinctUntilChanged()
                .flatMapLatest(::monitorPages),
        ) { query, pages ->
            GiphyPickerUiState(
                query = query,
                gifs = pages.gifs,
                isLoading = pages.isLoading,
                isEndOfList = pages.isEndOfList,
                emptyState = pages.emptyState,
            )
        }.asUiStateFlow(viewModelScope, GiphyPickerUiState())
    }

    /**
     * Updates the search query. A blank query shows trending GIFs.
     */
    fun onQueryChanged(newQuery: String) {
        query.value = newQuery
    }

    /**
     * Requests the next page of GIFs for the current query.
     */
    fun onLoadMore() {
        loadMoreRequests.trySend(Unit)
    }

    private fun monitorPages(searchQuery: String?): Flow<GiphyPages> = flow {
        discardPendingLoadMoreRequests()
        var pages = loadedPages[searchQuery] ?: run {
            emit(GiphyPages(isLoading = true))
            loadNextPage(searchQuery, GiphyPages())
        }
        emit(pages)

        loadMoreRequests.receiveAsFlow().collect {
            if (pages.isEndOfList) return@collect
            emit(pages.copy(isLoading = true))
            pages = loadNextPage(searchQuery, pages)
            emit(pages)
            discardPendingLoadMoreRequests()
        }
    }

    private suspend fun loadNextPage(searchQuery: String?, current: GiphyPages): GiphyPages {
        val offset = current.gifs.size
        return runCatchingCancellable {
            if (searchQuery == null) {
                getTrendingGiphysUseCase(PAGE_SIZE, offset)
            } else {
                searchGiphysUseCase(searchQuery, PAGE_SIZE, offset)
            }
        }.fold(
            onSuccess = { newGifs ->
                GiphyPages(
                    gifs = (current.gifs + newGifs).toImmutableList(),
                    isEndOfList = newGifs.size < PAGE_SIZE,
                ).also { loadedPages[searchQuery] = it }
            },
            onFailure = {
                Timber.e(it, "Failed to load GIFs")
                current.copy(isLoading = false, hasFailed = true)
            },
        )
    }

    private fun discardPendingLoadMoreRequests() {
        while (loadMoreRequests.tryReceive().isSuccess) Unit
    }

    private data class GiphyPages(
        val gifs: ImmutableList<Giphy> = persistentListOf(),
        val isLoading: Boolean = false,
        val isEndOfList: Boolean = false,
        val hasFailed: Boolean = false,
    ) {
        val emptyState: GiphyEmptyState?
            get() = when {
                isLoading || gifs.isNotEmpty() -> null
                hasFailed -> GiphyEmptyState.ServerDown
                else -> GiphyEmptyState.NoResults
            }
    }

    companion object {
        /**
         * Number of GIFs requested per page.
         */
        const val PAGE_SIZE = 25

        /**
         * Time the query has to stay unchanged before it is searched.
         */
        const val SEARCH_DEBOUNCE_MILLIS = 300L
    }
}
