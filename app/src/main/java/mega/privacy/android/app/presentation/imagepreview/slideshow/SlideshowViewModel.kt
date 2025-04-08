package mega.privacy.android.app.presentation.imagepreview.slideshow

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.PARAMS_CURRENT_IMAGE_NODE_ID_VALUE
import mega.privacy.android.app.presentation.imagepreview.fetcher.ImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.slideshow.model.SlideshowState
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.slideshow.SlideshowOrder
import mega.privacy.android.domain.entity.slideshow.SlideshowSpeed
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.MonitorSlideshowOrderSettingUseCase
import mega.privacy.android.domain.usecase.MonitorSlideshowRepeatSettingUseCase
import mega.privacy.android.domain.usecase.MonitorSlideshowSpeedSettingUseCase
import mega.privacy.android.domain.usecase.file.CheckFileUriUseCase
import mega.privacy.android.domain.usecase.imagepreview.ClearImageResultUseCase
import mega.privacy.android.domain.usecase.imagepreview.GetImageFromFileUseCase
import mega.privacy.android.domain.usecase.imagepreview.GetImageUseCase
import mega.privacy.android.domain.usecase.node.AddImageTypeUseCase
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SlideshowViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val imageNodeFetchers: Map<@JvmSuppressWildcards ImagePreviewFetcherSource, @JvmSuppressWildcards ImageNodeFetcher>,
    private val addImageTypeUseCase: AddImageTypeUseCase,
    private val getImageUseCase: GetImageUseCase,
    private val getImageFromFileUseCase: GetImageFromFileUseCase,
    private val monitorSlideshowOrderSettingUseCase: MonitorSlideshowOrderSettingUseCase,
    private val monitorSlideshowSpeedSettingUseCase: MonitorSlideshowSpeedSettingUseCase,
    private val monitorSlideshowRepeatSettingUseCase: MonitorSlideshowRepeatSettingUseCase,
    private val checkUri: CheckFileUriUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val clearImageResultUseCase: ClearImageResultUseCase,
) : ViewModel() {
    private val imagePreviewFetcherSource: ImagePreviewFetcherSource
        get() = savedStateHandle[ImagePreviewViewModel.IMAGE_NODE_FETCHER_SOURCE]
            ?: ImagePreviewFetcherSource.TIMELINE

    private val params: Bundle
        get() = savedStateHandle[ImagePreviewViewModel.FETCHER_PARAMS] ?: Bundle()

    private val currentImageNodeIdValue: Long
        get() = savedStateHandle[PARAMS_CURRENT_IMAGE_NODE_ID_VALUE] ?: 0L

    /**
     * Slideshow ViewState
     */
    private val _state = MutableStateFlow(SlideshowState())
    val state = _state.asStateFlow()

    init {
        monitorSpeedSetting()
        monitorRepeatSetting()
        monitorSlideshowSettings()
    }

    private fun monitorSlideshowSettings() {
        val orderFlow = monitorSlideshowOrderSettingUseCase().distinctUntilChanged()
        val imageNodesFlow = imageNodeFetchers[imagePreviewFetcherSource]?.monitorImageNodes(params)
            ?.catch { Timber.e(it) } ?: flowOf(emptyList())

        combine(orderFlow, imageNodesFlow) { order, imageNodes ->
            val filteredImageNodes = imageNodes.filter {
                it.type !is VideoFileTypeInfo && (it.hasThumbnail || it.hasPreview)
            }

            val settingOrder = order ?: SlideshowOrder.Shuffle
            val sortedItems = sortItems(
                imageNodes = filteredImageNodes,
                order = settingOrder
            )
            val (currentImageNodeIndex, currentImageNode) = findCurrentImageNode(
                sortedItems
            )
            val finalItems = if (settingOrder == SlideshowOrder.Shuffle) {
                sortedItems.shuffleStartingWithIndex(currentImageNodeIndex)
            } else {
                sortedItems
            }
            val isPlaying = if (!_state.value.isInitialized) {
                true
            } else {
                _state.value.isPlaying
            }

            _state.update {
                it.copy(
                    isInitialized = true,
                    order = settingOrder,
                    imageNodes = finalItems,
                    isPlaying = isPlaying,
                    currentImageNode = currentImageNode,
                    currentImageNodeIndex = if (settingOrder == SlideshowOrder.Shuffle) 0 else currentImageNodeIndex,
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun sortItems(
        imageNodes: List<ImageNode>,
        order: SlideshowOrder,
    ): List<ImageNode> {
        return when (order) {
            SlideshowOrder.Shuffle -> imageNodes
            SlideshowOrder.Newest -> imageNodes.sortedByDescending { it.modificationTime }
            SlideshowOrder.Oldest -> imageNodes.sortedBy { it.modificationTime }
        }
    }

    fun List<ImageNode>.shuffleStartingWithIndex(index: Int): List<ImageNode> {
        if (index !in indices) return this.shuffled()

        val item = this[index]

        val remainingItems = this.filterIndexed { i, _ -> i != index }.shuffled()

        return listOf(item) + remainingItems
    }


    private fun findCurrentImageNode(imageNodes: List<ImageNode>): Pair<Int, ImageNode?> {
        val currentImageNodeIdValue = if (_state.value.isInitialized) {
            _state.value.currentImageNode?.id?.longValue ?: currentImageNodeIdValue
        } else {
            currentImageNodeIdValue
        }
        val index = imageNodes.indexOfFirst { currentImageNodeIdValue == it.id.longValue }

        if (index != -1) {
            return index to imageNodes[index]
        }

        // If the image node is not found, calculate the target index based on the current state
        val currentImageNodeIndex = _state.value.currentImageNodeIndex
        val targetImageNodeIndex =
            if (currentImageNodeIndex > imageNodes.lastIndex) imageNodes.lastIndex else currentImageNodeIndex

        return targetImageNodeIndex to imageNodes.getOrNull(targetImageNodeIndex)
    }

    suspend fun monitorImageResult(imageNode: ImageNode): Flow<ImageResult> {
        return if (imageNode.serializedData?.contains("local") == true) {
            flow {
                val file = File(imageNode.previewPath ?: return@flow)
                emit(getImageFromFileUseCase(file))
            }
        } else {
            val typedNode = addImageTypeUseCase(imageNode)
            getImageUseCase(
                node = typedNode,
                fullSize = true,
                highPriority = true,
                resetDownloads = {},
            )
        }.catch { Timber.e("Failed to load image: $it") }
    }

    /**
     * Update Playing status
     */
    fun updateIsPlaying(isPlaying: Boolean) {
        _state.update {
            it.copy(isPlaying = isPlaying)
        }
    }

    suspend fun getFallbackImagePath(imageResult: ImageResult?): String? {
        return imageResult?.run {
            checkUri(previewUri) ?: checkUri(thumbnailUri)
        }
    }

    suspend fun getHighestResolutionImagePath(imageResult: ImageResult?): String? {
        return imageResult?.run {
            checkUri(fullSizeUri)
        }
    }

    private fun monitorSpeedSetting() = monitorSlideshowSpeedSettingUseCase()
        .distinctUntilChanged().onEach { speed ->
            _state.update {
                it.copy(speed = speed ?: SlideshowSpeed.Normal)
            }
        }.launchIn(viewModelScope)

    private fun monitorRepeatSetting() = monitorSlideshowRepeatSettingUseCase()
        .distinctUntilChanged().onEach { isRepeat ->
            _state.update {
                it.copy(repeat = isRepeat ?: false)
            }
        }.launchIn(viewModelScope)

    fun clearImageResultCache() = clearImageResultUseCase(true)

    fun setCurrentImageNodeIndex(currentImageNodeIndex: Int) {
        _state.update {
            it.copy(
                currentImageNodeIndex = currentImageNodeIndex,
            )
        }
    }

    fun setCurrentImageNode(currentImageNode: ImageNode) {
        _state.update {
            it.copy(
                currentImageNode = currentImageNode,
            )
        }
    }

    // We can simply use beyondViewportPageCount for the preload, but somehow it breaks
    // the cross-fade effect which indicating a bug in Coil lib.
    //
    // This manual preloading is the alternative to simulate same behavior.
    fun preloadImageNode(page: Int, callback: (NodeId, Boolean) -> Unit) = viewModelScope.launch {
        val node = _state.value.imageNodes.getOrNull(page) ?: return@launch
        monitorImageResult(node).collect {
            callback(node.id, it.isFullyLoaded)
        }
    }
}