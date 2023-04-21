package mega.privacy.android.app.presentation.slideshow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.presentation.slideshow.model.SlideshowViewState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.slideshow.SlideshowOrder
import mega.privacy.android.domain.entity.slideshow.SlideshowSpeed
import mega.privacy.android.domain.usecase.GetPhotosByIds
import mega.privacy.android.domain.usecase.MonitorSlideshowOrderSettingUseCase
import mega.privacy.android.domain.usecase.MonitorSlideshowRepeatSettingUseCase
import mega.privacy.android.domain.usecase.MonitorSlideshowSpeedSettingUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageByNodeHandle
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for slideshow
 *
 */
@HiltViewModel
class SlideshowViewModel @Inject constructor(
    private val getPhotosByIds: GetPhotosByIds,
    private val monitorSlideshowOrderSettingUseCase: MonitorSlideshowOrderSettingUseCase,
    private val monitorSlideshowSpeedSettingUseCase: MonitorSlideshowSpeedSettingUseCase,
    private val monitorSlideshowRepeatSettingUseCase: MonitorSlideshowRepeatSettingUseCase,
    private val getImageByNodeHandle: GetImageByNodeHandle,
) : ViewModel() {

    /**
     * Slideshow ViewState
     */
    private val _state = MutableStateFlow(SlideshowViewState())
    val state = _state.asStateFlow()

    init {
        monitorOrderSetting()
        monitorSpeedSetting()
        monitorRepeatSetting()
    }

    /**
     * Monitor ImageViewer source images
     */
    fun setData(
        items: List<ImageItem>,
    ) {
        playSlideshow(imageItems = items)
    }

    private fun playSlideshow(imageItems: List<ImageItem>) {
        if (_state.value.items.isNotEmpty())
            return
        viewModelScope.launch {
            val ids = imageItems.map { it.getNodeHandle() ?: it.id }
            val slideshowItems = getPhotosByIds(ids = ids.map { id -> NodeId(id) })
                .filterIsInstance<Photo.Image>()
            val order = _state.value.order ?: SlideshowOrder.Shuffle
            val sortedItems = sortItems(slideshowItems, order)
            _state.update {
                it.copy(
                    items = sortedItems,
                    isPlaying = true
                )
            }
        }
    }

    private fun sortItems(slideshowItems: List<Photo>, order: SlideshowOrder): List<Photo> {
        return when (order) {
            SlideshowOrder.Shuffle -> slideshowItems.shuffled()
            SlideshowOrder.Newest -> slideshowItems.sortedByDescending { it.modificationTime }
            SlideshowOrder.Oldest -> slideshowItems.sortedBy { it.modificationTime }
        }
    }

    suspend fun downloadFullSizeImage(
        nodeHandle: Long,
        fullSize: Boolean = true,
        highPriority: Boolean = false,
        resetDownloads: () -> Unit = {},
    ) = getImageByNodeHandle(
        nodeHandle = nodeHandle,
        fullSize = fullSize,
        highPriority = highPriority,
        resetDownloads = resetDownloads,
    )

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
                    val slideshowItems = _state.value.items
                    val settingOrder = order ?: SlideshowOrder.Shuffle
                    val sortedItems = sortItems(slideshowItems, settingOrder)
                    _state.update {
                        it.copy(
                            order = settingOrder,
                            shouldPlayFromFirst = true,
                            items = sortedItems
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
