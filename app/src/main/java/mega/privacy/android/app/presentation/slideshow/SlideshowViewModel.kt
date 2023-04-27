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
import mega.privacy.android.app.presentation.slideshow.model.SlideshowItem
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
import mega.privacy.android.domain.usecase.imageviewer.GetImageForChatMessageUseCase
import mega.privacy.android.domain.usecase.slideshow.GetChatPhotoByMessageIdUseCase
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
    private val getImageForChatMessageUseCase: GetImageForChatMessageUseCase,
    private val getChatPhotoByMessageIdUseCase: GetChatPhotoByMessageIdUseCase,
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
        if (_state.value.slideshowItems.isNotEmpty())
            return
        viewModelScope.launch {
            val slideshowItems = if (imageItems.first() is ImageItem.ChatNode) {
                imageItems.mapNotNull {
                    val chatMessageId = (it as ImageItem.ChatNode).chatMessageId
                    val chatRoomId = (imageItems.first() as ImageItem.ChatNode).chatRoomId
                    val photo = getChatPhotoByMessageIdUseCase(
                        chatRoomId,
                        chatMessageId
                    )
                    if (photo != null && photo is Photo.Image) {
                        SlideshowItem.ChatItem(
                            photo = photo,
                            chatRoomId = chatRoomId,
                            messageId = chatMessageId,
                        )
                    } else {
                        null
                    }
                }
            } else {
                val ids = imageItems.map {
                    it.getNodeHandle() ?: it.id
                }
                getPhotosByIds(ids = ids.map { id -> NodeId(id) }).filterIsInstance<Photo.Image>()
                    .map { SlideshowItem.DefaultItem(it) }
            }
            val order = _state.value.order ?: SlideshowOrder.Shuffle
            val sortedItems = sortItems(slideshowItems, order)
            _state.update {
                it.copy(
                    slideshowItems = sortedItems,
                    isPlaying = true
                )
            }
        }
    }

    private fun sortItems(
        slideshowItems: List<SlideshowItem>,
        order: SlideshowOrder,
    ): List<SlideshowItem> {
        return when (order) {
            SlideshowOrder.Shuffle -> slideshowItems.shuffled()
            SlideshowOrder.Newest -> slideshowItems.sortedByDescending { it.photo.modificationTime }
            SlideshowOrder.Oldest -> slideshowItems.sortedBy { it.photo.modificationTime }
        }
    }

    suspend fun downloadFullSizeImage(
        slideshowItem: SlideshowItem,
    ) = when (slideshowItem) {
        is SlideshowItem.ChatItem -> getChatItemImage(
            chatRoomId = slideshowItem.chatRoomId,
            chatMessageId = slideshowItem.messageId
        )

        is SlideshowItem.DefaultItem -> getDefaultItemImage(
            nodeHandle = slideshowItem.photo.id
        )
    }

    private suspend fun getDefaultItemImage(
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

    private suspend fun getChatItemImage(
        chatRoomId: Long,
        chatMessageId: Long,
        fullSize: Boolean = true,
        highPriority: Boolean = false,
        resetDownloads: () -> Unit = {},
    ) = getImageForChatMessageUseCase(
        chatRoomId = chatRoomId,
        chatMessageId = chatMessageId,
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
                    val slideshowItems = _state.value.slideshowItems
                    val settingOrder = order ?: SlideshowOrder.Shuffle
                    val sortedItems = sortItems(slideshowItems, settingOrder)
                    _state.update {
                        it.copy(
                            order = settingOrder,
                            shouldPlayFromFirst = true,
                            slideshowItems = sortedItems
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
