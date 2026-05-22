package mega.privacy.mobile.home.presentation.home.widget.viewedlinks

import androidx.core.util.lruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.RecentlyViewedLinkType
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.ViewedLink
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.viewedlinks.ViewedLinksSortField
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeUseCase
import mega.privacy.android.domain.usecase.viewedlinks.ClearViewedLinksUseCase
import mega.privacy.android.domain.usecase.viewedlinks.MonitorViewedLinksSortPreferenceUseCase
import mega.privacy.android.domain.usecase.viewedlinks.MonitorViewedLinksUseCase
import mega.privacy.android.domain.usecase.viewedlinks.RemoveViewedLinksUseCase
import mega.privacy.android.domain.usecase.viewedlinks.SetViewedLinksSortUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import mega.privacy.android.shared.nodes.extension.getIcon
import mega.privacy.android.shared.nodes.mapper.FileTypeIconMapper
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.home.presentation.home.widget.viewedlinks.mapper.ViewedLinksSortMapper
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the Viewed Links widget and full-screen list.
 *
 * Exposes viewed links as paginated UI items so consumers (the home widget and the
 * full-screen list) only resolve the rows they actually render. For file links,
 * [GetPublicNodeUseCase] is invoked per item — with paging in place, this only fires
 * for rows in loaded pages.
 *
 * Folder links are displayed with a static folder icon and no thumbnail.
 *
 * @property pagedItems The paginated stream of [ViewedLinkUiItem]s, cached for the
 *   ViewModel scope so configuration changes do not refetch. The flow re-emits a fresh
 *   [PagingData] whenever the persisted sort preference changes.
 * @param monitorViewedLinksUseCase
 * @param monitorViewedLinksSortPreferenceUseCase
 * @param setViewedLinksSortUseCase
 * @param viewedLinksSortMapper
 * @param getPublicNodeUseCase
 * @param fileTypeIconMapper
 * @param clearViewedLinksUseCase
 * @param snackbarEventQueue
 * @param monitorViewTypeUseCase
 * @param setViewTypeUseCase
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class ViewedLinksViewModel @Inject constructor(
    monitorViewedLinksUseCase: MonitorViewedLinksUseCase,
    private val monitorViewedLinksSortPreferenceUseCase: MonitorViewedLinksSortPreferenceUseCase,
    private val setViewedLinksSortUseCase: SetViewedLinksSortUseCase,
    private val viewedLinksSortMapper: ViewedLinksSortMapper,
    private val getPublicNodeUseCase: GetPublicNodeUseCase,
    private val fileTypeIconMapper: FileTypeIconMapper,
    private val clearViewedLinksUseCase: ClearViewedLinksUseCase,
    private val removeViewedLinksUseCase: RemoveViewedLinksUseCase,
    private val snackbarEventQueue: SnackbarEventQueue,
    private val monitorViewTypeUseCase: MonitorViewType,
    private val setViewTypeUseCase: SetViewType,
) : ViewModel() {
    private val resolvedLinkCache = lruCache<String, ViewedLinkUiItem>(maxSize = PAGE_SIZE * 100)
    private val clearAllLinksEvent = MutableStateFlow<StateEvent>(consumed)
    private val selectedNodeHandles = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<ViewedLinksUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            monitorViewedLinksSortPreferenceUseCase()
                .map { (field, direction) -> viewedLinksSortMapper(field, direction) },
            clearAllLinksEvent,
            monitorViewTypeUseCase(),
            selectedNodeHandles,
        ) { sortConfiguration, clearAllLinksEvent, viewType, selectedHandles ->
            ViewedLinksUiState(
                clearAllLinksEvent = clearAllLinksEvent,
                sortConfiguration = sortConfiguration,
                currentViewType = viewType,
                selectedNodeHandles = selectedHandles,
            )
        }.catch { e ->
            Timber.e(e, "Failed to build ViewedLinks UI state")
        }.asUiStateFlow(
            viewModelScope,
            ViewedLinksUiState(),
        )
    }

    val pagedItems: Flow<PagingData<ViewedLinkUiItem>> by lazy(LazyThreadSafetyMode.NONE) {
        monitorViewedLinksSortPreferenceUseCase()
            .flatMapLatest { (field, direction) ->
                Pager(
                    config = PagingConfig(
                        pageSize = PAGE_SIZE,
                        initialLoadSize = PAGE_SIZE,
                        enablePlaceholders = false,
                    ),
                    pagingSourceFactory = { monitorViewedLinksUseCase(field, direction) },
                ).flow
            }
            .map { pagingData -> pagingData.map { it.toUiItem() } }
            .cachedIn(viewModelScope)
    }

    val previewItems: Flow<PagingData<ViewedLinkUiItem>> by lazy(LazyThreadSafetyMode.NONE) {
        // The widget displays at most MAX_PREVIEW_COUNT items, but we fetch one extra so we
        // can tell whether more items exist and decide if the "View more" icon should be shown.
        // Loading just one additional item keeps this as efficient as possible.
        val pageSize = MAX_PREVIEW_COUNT + 1

        Pager(
            config = PagingConfig(
                pageSize = pageSize,
                initialLoadSize = pageSize,
                maxSize = pageSize * 3,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                monitorViewedLinksUseCase(
                    ViewedLinksSortField.LastAccessed,
                    SortDirection.Descending
                )
            }
        ).flow
            .mapLatest { pagingData -> pagingData.map { it.toUiItem() } }
            .cachedIn(viewModelScope)
    }

    /**
     * Resolves a single [ViewedLink] to its UI item.
     * File links go through the public-node API to fetch icon + preview path;
     * folder links use a static icon.
     */
    private suspend fun ViewedLink.toUiItem(): ViewedLinkUiItem = when (type) {
        RecentlyViewedLinkType.FileLink -> resolveFileLink(this)
        RecentlyViewedLinkType.FolderLink -> ViewedLinkUiItem(
            viewedLink = this,
            iconRes = iconPackR.drawable.ic_folder_users_small_solid,
            previewPath = null,
        )
    }

    /**
     * Resolves a file link to a [ViewedLinkUiItem] by fetching the public node.
     * Falls back to extension-based icon if the node cannot be resolved.
     */
    private suspend fun resolveFileLink(link: ViewedLink): ViewedLinkUiItem {
        resolvedLinkCache[link.linkUrl]?.let {
            return it.copy(viewedLink = link)
        }
        val node = runCatching { getPublicNodeUseCase(link.linkUrl) }.getOrNull()
        return ViewedLinkUiItem(
            viewedLink = link,
            iconRes = node?.getIcon(fileTypeIconMapper)
                ?: fileTypeIconMapper(link.name.substringAfterLast('.', "")),
            previewPath = node?.previewPath,
        ).also { resolvedLinkCache.put(link.linkUrl, it) }
    }

    internal fun clearAllLinks() {
        viewModelScope.launch {
            runCatching { clearViewedLinksUseCase() }
                .onFailure { Timber.e(it, "Failed to clear viewed links history") }
                .onSuccess {
                    snackbarEventQueue.queueMessage(sharedR.string.home_widget_viewed_links_clear_history_success_message)
                    clearAllLinksEvent.update { triggered }
                }
        }
    }

    internal fun onClearAllLinksEventConsumed() {
        clearAllLinksEvent.update { consumed }
    }

    internal fun changeViewType() {
        viewModelScope.launch {
            runCatching {
                val toViewType = if (uiState.value.currentViewType == ViewType.LIST) {
                    ViewType.GRID
                } else {
                    ViewType.LIST
                }
                setViewTypeUseCase(toViewType)
            }
        }
    }

    internal fun toggleSelection(nodeHandle: Long) {
        selectedNodeHandles.update { current ->
            if (current.contains(nodeHandle)) {
                current - nodeHandle
            } else {
                current + nodeHandle
            }
        }
    }

    internal fun clearSelection() {
        selectedNodeHandles.update { emptySet() }
    }

    internal fun deleteSelectedLinks() {
        val handlesToDelete = selectedNodeHandles.value
        if (handlesToDelete.isEmpty()) return

        viewModelScope.launch {
            runCatching { removeViewedLinksUseCase(handlesToDelete) }
                .onFailure {
                    Timber.e(it, "Failed to remove viewed links $handlesToDelete")
                }.onSuccess {
                    clearSelection()
                }
        }
    }

    internal fun updateSortConfiguration(configuration: NodeSortConfiguration) {
        viewModelScope.launch {
            runCatching {
                setViewedLinksSortUseCase(
                    sortField = viewedLinksSortMapper(configuration.sortOption),
                    sortDirection = configuration.sortDirection,
                )
            }.onFailure {
                Timber.e(
                    it,
                    "Failed to persist viewed-links sort preference"
                )
            }
        }
    }

    companion object {
        private const val PAGE_SIZE = 10
        internal const val MAX_PREVIEW_COUNT = 4
    }
}
