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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel
import mega.privacy.android.app.presentation.imagepreview.fetcher.ImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.slideshow.model.SlideshowState
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.slideshow.SlideshowOrder
import mega.privacy.android.domain.entity.slideshow.SlideshowSpeed
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.MonitorSlideshowOrderSettingUseCase
import mega.privacy.android.domain.usecase.MonitorSlideshowRepeatSettingUseCase
import mega.privacy.android.domain.usecase.MonitorSlideshowSpeedSettingUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageUseCase
import mega.privacy.android.domain.usecase.node.AddImageTypeUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SlideshowViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val imageNodeFetchers: Map<@JvmSuppressWildcards ImagePreviewFetcherSource, @JvmSuppressWildcards ImageNodeFetcher>,
    private val addImageTypeUseCase: AddImageTypeUseCase,
    private val getImageUseCase: GetImageUseCase,
    private val monitorSlideshowOrderSettingUseCase: MonitorSlideshowOrderSettingUseCase,
    private val monitorSlideshowSpeedSettingUseCase: MonitorSlideshowSpeedSettingUseCase,
    private val monitorSlideshowRepeatSettingUseCase: MonitorSlideshowRepeatSettingUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val imagePreviewFetcherSource: ImagePreviewFetcherSource
        get() = savedStateHandle[ImagePreviewViewModel.IMAGE_NODE_FETCHER_SOURCE]
            ?: ImagePreviewFetcherSource.TIMELINE

    private val params: Bundle
        get() = savedStateHandle[ImagePreviewViewModel.FETCHER_PARAMS] ?: Bundle()

    /**
     * Slideshow ViewState
     */
    private val _state = MutableStateFlow(SlideshowState())
    val state = _state.asStateFlow()

    init {
        monitorOrderSetting()
        monitorSpeedSetting()
        monitorRepeatSetting()
        monitorImageNodes()
    }

    private fun monitorImageNodes() {
        val imageFetcher = imageNodeFetchers[imagePreviewFetcherSource] ?: return
        imageFetcher.monitorImageNodes(params)
            .catch { Timber.e(it) }
            .mapLatest { playSlideshow(it) }
            .launchIn(viewModelScope)
    }

    private fun playSlideshow(imageNodes: List<ImageNode>) {
        viewModelScope.launch(ioDispatcher) {
            val order = _state.value.order ?: SlideshowOrder.Shuffle
            val filteredImageNodes = imageNodes.filter {
                it.hasThumbnail || it.hasPreview
                //TODO We don't have those path in ImageNode, and mapTo TypedImageNode will cost more time.
//                with(it.photo) {
//                    Path(previewFilePath ?: "").exists() || Path(thumbnailFilePath ?: "").exists()
//                }
            }

            val sortedItems = sortItems(filteredImageNodes, order)
            _state.update {
                it.copy(
                    imageNodes = sortedItems,
                    isPlaying = true
                )

            }
        }
    }

    private fun sortItems(
        imageNodes: List<ImageNode>,
        order: SlideshowOrder,
    ): List<ImageNode> {
        return when (order) {
            SlideshowOrder.Shuffle -> imageNodes.shuffled()
            SlideshowOrder.Newest -> imageNodes.sortedByDescending { it.modificationTime }
            SlideshowOrder.Oldest -> imageNodes.sortedBy { it.modificationTime }
        }
    }

    suspend fun monitorImageResult(imageNode: ImageNode): Flow<ImageResult> {
        val typedNode = addImageTypeUseCase(imageNode)
        return getImageUseCase(
            node = typedNode,
            fullSize = true,
            highPriority = true,
            resetDownloads = {},
        ).catch { Timber.e("Failed to load image: $it") }
    }

    /**
     * Update Playing status
     */
    fun updateIsPlaying(isPlaying: Boolean) {
        Timber.d("Slideshow updateIsPlaying isPlaying+$isPlaying")
        _state.update {
            it.copy(isPlaying = isPlaying)
        }
    }

    /**
     * Should play slideshow from the first item
     */
    fun shouldPlayFromFirst(shouldPlayFromFirst: Boolean) {
        _state.update {
            it.copy(shouldPlayFromFirst = shouldPlayFromFirst)
        }
    }

    private fun monitorOrderSetting() =
        monitorSlideshowOrderSettingUseCase()
            .distinctUntilChanged().onEach { order ->
                Timber.d("Slideshow monitorOrderSetting order+$order")
                val isFirstInSlideshow = _state.value.isFirstInSlideshow
                Timber.d("Slideshow monitorOrderSetting shouldPlayFromFirst+${!isFirstInSlideshow}")
                if (isFirstInSlideshow) {
                    _state.update {
                        it.copy(
                            order = order ?: SlideshowOrder.Shuffle,
                            shouldPlayFromFirst = false,
                            isFirstInSlideshow = false,
                        )
                    }
                } else {
                    val imageNodes = _state.value.imageNodes
                    val settingOrder = order ?: SlideshowOrder.Shuffle
                    val sortedItems = sortItems(imageNodes, settingOrder)
                    _state.update {
                        it.copy(
                            order = settingOrder,
                            shouldPlayFromFirst = true,
                            imageNodes = sortedItems
                        )
                    }
                }
            }.launchIn(viewModelScope)

    private fun monitorSpeedSetting() = monitorSlideshowSpeedSettingUseCase()
        .distinctUntilChanged().onEach { speed ->
            Timber.d("Slideshow monitorSpeedSetting speed+$speed")
            _state.update {
                it.copy(speed = speed ?: SlideshowSpeed.Normal)
            }
        }.launchIn(viewModelScope)

    private fun monitorRepeatSetting() = monitorSlideshowRepeatSettingUseCase()
        .distinctUntilChanged().onEach { isRepeat ->
            Timber.d("Slideshow monitorRepeatSetting isRepeat+$isRepeat")
            _state.update {
                it.copy(repeat = isRepeat ?: false)
            }
        }.launchIn(viewModelScope)
}