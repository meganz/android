package mega.privacy.mobile.home.presentation.home.widget.viewedlinks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.RecentlyViewedLinkType
import mega.privacy.android.domain.entity.node.ViewedLink
import mega.privacy.android.domain.extension.mapAsync
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeUseCase
import mega.privacy.android.domain.usecase.viewedlinks.ClearViewedLinksUseCase
import mega.privacy.android.domain.usecase.viewedlinks.MonitorViewedLinksUseCase
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import mega.privacy.android.shared.nodes.extension.getIcon
import mega.privacy.android.shared.nodes.mapper.FileTypeIconMapper
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the Viewed Links widget on the Home page.
 *
 * Monitors viewed links (file and folder links the user has previously opened) and maps
 * them to UI items with resolved icons and preview thumbnails.
 *
 * For file links, [GetPublicNodeUseCase] is called to fetch the public node, which provides
 * the correct file type icon and a local preview path for thumbnail display.
 * If resolution fails, the icon falls back to [FileTypeIconMapper] based on file extension.
 *
 * Folder links are displayed with a static folder icon and no thumbnail.
 *
 * @property uiState The UI state containing the list of [ViewedLinkUiItem]s.
 * @param monitorViewedLinksUseCase
 * @param getPublicNodeUseCase
 * @param fileTypeIconMapper
 * @param clearViewedLinksUseCase
 * @param snackbarEventQueue
 */
@HiltViewModel
internal class ViewedLinksViewModel @Inject constructor(
    monitorViewedLinksUseCase: MonitorViewedLinksUseCase,
    private val getPublicNodeUseCase: GetPublicNodeUseCase,
    private val fileTypeIconMapper: FileTypeIconMapper,
    private val clearViewedLinksUseCase: ClearViewedLinksUseCase,
    private val snackbarEventQueue: SnackbarEventQueue,
) : ViewModel() {

    private val clearAllLinksEvent = MutableStateFlow<StateEvent>(consumed)
    private val resolvedLinkCache = mutableMapOf<String, ViewedLinkUiItem>()

    val uiState: StateFlow<ViewedLinksUiState> by lazy {
        combine(
            monitorViewedLinksUseCase(),
            clearAllLinksEvent,
        ) { viewedLinks, clearEvent ->
            ViewedLinksUiState.Ready(
                items = viewedLinks.toUiItems(),
                clearAllLinksEvent = clearEvent,
            )
        }.catch { Timber.e(it) }
            .asUiStateFlow(
                scope = viewModelScope,
                initialValue = ViewedLinksUiState.Loading,
            )
    }

    /**
     * Maps a list of [ViewedLink]s to [ViewedLinkUiItem]s in parallel.
     * File links are resolved via the public API; folder links use a static icon.
     */
    private suspend fun List<ViewedLink>.toUiItems() =
        mapAsync { link ->
            when (link.type) {
                RecentlyViewedLinkType.FileLink -> resolveFileLink(link)
                RecentlyViewedLinkType.FolderLink -> ViewedLinkUiItem(
                    viewedLink = link,
                    iconRes = iconPackR.drawable.ic_folder_users_small_solid,
                    previewPath = null
                )
            }
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
        ).also { resolvedLinkCache[link.linkUrl] = it }
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
}
