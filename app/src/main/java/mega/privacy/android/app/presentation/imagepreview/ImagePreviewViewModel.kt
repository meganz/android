package mega.privacy.android.app.presentation.imagepreview

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import mega.privacy.android.app.presentation.imagepreview.fetcher.ImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.menu.ImagePreviewMenuOptions
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewState
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ImagePreviewViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val imageNodeFetchers: Map<@JvmSuppressWildcards ImagePreviewFetcherSource, @JvmSuppressWildcards ImageNodeFetcher>,
    private val imagePreviewMenuOptionsMap: Map<@JvmSuppressWildcards ImagePreviewMenuSource, @JvmSuppressWildcards ImagePreviewMenuOptions>,
) : ViewModel() {
    private val imagePreviewFetcherSource: ImagePreviewFetcherSource
        get() = savedStateHandle[IMAGE_NODE_FETCHER_SOURCE] ?: ImagePreviewFetcherSource.TIMELINE

    private val params: Bundle
        get() = savedStateHandle[FETCHER_PARAMS] ?: Bundle()

    private val imagePreviewMenuSource: ImagePreviewMenuSource
        get() = savedStateHandle[IMAGE_PREVIEW_MENU_OPTIONS] ?: ImagePreviewMenuSource.TIMELINE

    private val _state = MutableStateFlow(
        ImagePreviewState(
            currentPreviewPhotoId = NodeId(savedStateHandle[PARAMS_CURRENT_PREVIEW_ITEM_ID] ?: 0L),
        )
    )

    val state: StateFlow<ImagePreviewState> = _state

    init {
        monitorImageNodes()
    }

    private fun monitorImageNodes() {
        val imageFetcher = imageNodeFetchers[imagePreviewFetcherSource] ?: return
        imageFetcher.monitorImageNodes(params)
            .catch { Timber.e(it) }
            .mapLatest { imageNodes ->
                _state.update {
                    it.copy(
                        imageNodes = imageNodes
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    //will call in compose snapshotFlow { pagerState.currentPage }.collect { page -> monitorCurrentImageNodeChange }
    suspend fun monitorCurrentImageNodeChange(imageNode: ImageNode) {
        val shouldShowSlideshowOption = isSlideshowOptionVisible(imageNode)
        val shouldShowLinkOption = isLinkOptionVisible(imageNode)
        val shouldShowDownloadOption = isDownloadOptionVisible(imageNode)
        val shouldShowForwardOption = isForwardOptionVisible(imageNode)
        _state.update {
            it.copy(
                shouldShowSlideshowOption = shouldShowSlideshowOption,
                shouldShowLinkOption = shouldShowLinkOption,
                shouldShowDownloadOption = shouldShowDownloadOption,
                shouldShowForwardOption = shouldShowForwardOption
            )
        }
    }

    private fun isSlideshowOptionVisible(imageNode: ImageNode): Boolean =
        imagePreviewMenuOptionsMap[imagePreviewMenuSource]
            ?.isSlideshowOptionVisible(imageNode)
            ?: false


    private fun isLinkOptionVisible(imageNode: ImageNode): Boolean =
        imagePreviewMenuOptionsMap[imagePreviewMenuSource]
            ?.isLinkOptionVisible(imageNode)
            ?: false


    private fun isDownloadOptionVisible(imageNode: ImageNode): Boolean =
        imagePreviewMenuOptionsMap[imagePreviewMenuSource]
            ?.isDownloadOptionVisible(imageNode)
            ?: false


    private fun isForwardOptionVisible(imageNode: ImageNode): Boolean =
        imagePreviewMenuOptionsMap[imagePreviewMenuSource]
            ?.isForwardOptionVisible(imageNode)
            ?: false


    companion object {
        const val IMAGE_NODE_FETCHER_SOURCE = "image_node_fetcher_source"
        const val IMAGE_PREVIEW_MENU_OPTIONS = "image_preview_menu_options"
        const val FETCHER_PARAMS = "fetcher_params"
        const val PARAMS_CURRENT_PREVIEW_ITEM_ID = "currentPreviewItemId"
    }
}
