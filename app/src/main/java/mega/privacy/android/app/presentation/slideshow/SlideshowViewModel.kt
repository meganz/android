package mega.privacy.android.app.presentation.slideshow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.presentation.slideshow.model.SlideshowItem
import mega.privacy.android.app.presentation.slideshow.model.SlideshowViewState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.slideshow.SlideshowOrder
import mega.privacy.android.domain.entity.slideshow.SlideshowSpeed
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetPhotosByIdsUseCase
import mega.privacy.android.domain.usecase.MonitorSlideshowOrderSettingUseCase
import mega.privacy.android.domain.usecase.MonitorSlideshowRepeatSettingUseCase
import mega.privacy.android.domain.usecase.MonitorSlideshowSpeedSettingUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageByAlbumImportNodeUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageByNodeHandleUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageByNodePublicLinkUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageForChatMessageUseCase
import mega.privacy.android.domain.usecase.slideshow.GetChatPhotoByMessageIdUseCase
import mega.privacy.android.domain.usecase.slideshow.GetPhotoByAlbumImportNodeUseCase
import mega.privacy.android.domain.usecase.slideshow.GetPhotoByPublicLinkUseCase
import timber.log.Timber
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.exists

/**
 * ViewModel for slideshow
 *
 */
@HiltViewModel
class SlideshowViewModel @Inject constructor(
    private val getPhotosByIdsUseCase: GetPhotosByIdsUseCase,
    private val monitorSlideshowOrderSettingUseCase: MonitorSlideshowOrderSettingUseCase,
    private val monitorSlideshowSpeedSettingUseCase: MonitorSlideshowSpeedSettingUseCase,
    private val monitorSlideshowRepeatSettingUseCase: MonitorSlideshowRepeatSettingUseCase,
    private val getImageByNodeHandleUseCase: GetImageByNodeHandleUseCase,
    private val getImageForChatMessageUseCase: GetImageForChatMessageUseCase,
    private val getChatPhotoByMessageIdUseCase: GetChatPhotoByMessageIdUseCase,
    private val getImageByNodePublicLinkUseCase: GetImageByNodePublicLinkUseCase,
    private val getPhotoByPublicLinkUseCase: GetPhotoByPublicLinkUseCase,
    private val getImageByAlbumImportNodeUseCase: GetImageByAlbumImportNodeUseCase,
    private val getPhotoByAlbumImportNodeUseCase: GetPhotoByAlbumImportNodeUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
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
        viewModelScope.launch(ioDispatcher) {
            val slideshowItems = when (imageItems.first()) {
                is ImageItem.ChatNode -> {
                    imageItems.mapNotNull {
                        val chatNode = it as ImageItem.ChatNode
                        createChatItem(
                            chatMessageId = chatNode.chatMessageId,
                            chatRoomId = chatNode.chatRoomId
                        )
                    }
                }

                is ImageItem.PublicNode -> {
                    imageItems.mapNotNull {
                        val link = (it as ImageItem.PublicNode).nodePublicLink
                        createLinkItem(link)
                    }
                }

                is ImageItem.AlbumImportNode -> {
                    imageItems.map {
                        getPhotoByAlbumImportNodeUseCase(NodeId(it.getNodeHandle() ?: it.id))
                    }.filterIsInstance<Photo.Image>()
                        .map { SlideshowItem.AlbumSharingItem(it) }
                }

                else -> {
                    val ids = imageItems.map {
                        it.getNodeHandle() ?: it.id
                    }
                    getPhotosByIdsUseCase(ids = ids.map { id -> NodeId(id) }).filterIsInstance<Photo.Image>()
                        .map { SlideshowItem.DefaultItem(it) }
                }
            }
            val order = _state.value.order ?: SlideshowOrder.Shuffle
            val filteredSlideshowItems = slideshowItems.filter {
                with(it.photo) {
                    Path(previewFilePath ?: "").exists() || Path(thumbnailFilePath ?: "").exists()
                }
            }

            val sortedItems = sortItems(filteredSlideshowItems, order)
            _state.update {
                it.copy(
                    slideshowItems = sortedItems,
                    isPlaying = true
                )

            }
        }
    }

    private suspend fun createLinkItem(
        link: String,
    ): SlideshowItem.PublicLinkItem? {
        val photo = getPhotoByPublicLinkUseCase(link)
        return if (photo != null && photo is Photo.Image) {
            SlideshowItem.PublicLinkItem(
                photo = photo,
                link = link,
            )
        } else {
            null
        }
    }

    private suspend fun createChatItem(
        chatMessageId: Long,
        chatRoomId: Long,
    ): SlideshowItem.ChatItem? {
        val photo = getChatPhotoByMessageIdUseCase(
            chatRoomId,
            chatMessageId
        )
        return if (photo != null && photo is Photo.Image) {
            SlideshowItem.ChatItem(
                photo = photo,
                chatRoomId = chatRoomId,
                messageId = chatMessageId,
            )
        } else {
            null
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

        is SlideshowItem.PublicLinkItem -> getPublicLinkItemImage(
            link = slideshowItem.link
        )

        is SlideshowItem.AlbumSharingItem ->
            getAlbumSharingItemImage(
                nodeHandle = slideshowItem.photo.id
            )

        is SlideshowItem.DefaultItem ->
            getDefaultItemImage(
                nodeHandle = slideshowItem.photo.id
            )
    }

    private suspend fun getDefaultItemImage(
        nodeHandle: Long,
        fullSize: Boolean = true,
        highPriority: Boolean = false,
        resetDownloads: () -> Unit = {},
    ) = getImageByNodeHandleUseCase(
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

    private suspend fun getPublicLinkItemImage(
        link: String,
        fullSize: Boolean = true,
        highPriority: Boolean = false,
        resetDownloads: () -> Unit = {},
    ) = getImageByNodePublicLinkUseCase(
        nodeFileLink = link,
        fullSize = fullSize,
        highPriority = highPriority,
        resetDownloads = resetDownloads,
    )

    private suspend fun getAlbumSharingItemImage(
        nodeHandle: Long,
        fullSize: Boolean = true,
        highPriority: Boolean = false,
        resetDownloads: () -> Unit = {},
    ) = getImageByAlbumImportNodeUseCase(
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
