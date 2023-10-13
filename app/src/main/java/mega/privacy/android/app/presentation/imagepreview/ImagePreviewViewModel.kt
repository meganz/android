package mega.privacy.android.app.presentation.imagepreview

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
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
import mega.privacy.android.app.utils.MegaNodeUtil.getInfoText
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.imageviewer.GetImageUseCase
import mega.privacy.android.domain.usecase.node.AddImageTypeUseCase
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ImagePreviewViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val imageNodeFetchers: Map<@JvmSuppressWildcards ImagePreviewFetcherSource, @JvmSuppressWildcards ImageNodeFetcher>,
    private val imagePreviewMenuOptionsMap: Map<@JvmSuppressWildcards ImagePreviewMenuSource, @JvmSuppressWildcards ImagePreviewMenuOptions>,
    private val addImageTypeUseCase: AddImageTypeUseCase,
    private val getImageUseCase: GetImageUseCase,
) : ViewModel() {
    private val imagePreviewFetcherSource: ImagePreviewFetcherSource
        get() = savedStateHandle[IMAGE_NODE_FETCHER_SOURCE] ?: ImagePreviewFetcherSource.TIMELINE

    private val params: Bundle
        get() = savedStateHandle[FETCHER_PARAMS] ?: Bundle()

    private val currentImageNodeId: Long
        get() = savedStateHandle[PARAMS_CURRENT_IMAGE_NODE_ID] ?: 0L

    private val imagePreviewMenuSource: ImagePreviewMenuSource
        get() = savedStateHandle[IMAGE_PREVIEW_MENU_OPTIONS] ?: ImagePreviewMenuSource.TIMELINE

    private val _state = MutableStateFlow(
        ImagePreviewState(
            currentImageNodeId = NodeId(currentImageNodeId),
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

    suspend fun monitorImageResult(imageNode: ImageNode): Flow<ImageResult> {
        val typedNode = addImageTypeUseCase(imageNode)
        return getImageUseCase(
            node = typedNode,
            fullSize = true,
            highPriority = true,
            resetDownloads = {},
        ).catch { Timber.e("Failed to load image: $it") }
    }

    fun switchFullScreenMode() {
        val inFullScreenMode = _state.value.inFullScreenMode
        _state.update {
            it.copy(
                inFullScreenMode = !inFullScreenMode
            )
        }
    }

    fun setCurrentImageNodeId(nodeId: NodeId) {
        _state.update {
            it.copy(
                currentImageNodeId = nodeId
            )
        }
    }

    fun getInfoText(
        imageNode: ImageNode,
        context: Context,
    ): String {
        val megaNode = MegaNode.unserialize(imageNode.serializedData)
        return megaNode.getInfoText(context)
    }

    companion object {
        const val IMAGE_NODE_FETCHER_SOURCE = "image_node_fetcher_source"
        const val IMAGE_PREVIEW_MENU_OPTIONS = "image_preview_menu_options"
        const val FETCHER_PARAMS = "fetcher_params"
        const val PARAMS_CURRENT_IMAGE_NODE_ID = "currentImageNodeId"
    }
}
