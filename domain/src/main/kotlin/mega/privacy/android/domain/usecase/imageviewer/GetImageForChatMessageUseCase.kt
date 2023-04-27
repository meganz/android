package mega.privacy.android.domain.usecase.imageviewer

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.repository.ImageRepository
import mega.privacy.android.domain.repository.NetworkRepository
import javax.inject.Inject

/**
 * The use case class to get an ImageResult given a Node Chat Room Id and Chat Message Id.
 */
class GetImageForChatMessageUseCase @Inject constructor(
    private val networkRepository: NetworkRepository,
    private val imageRepository: ImageRepository,
) {

    /**
     * Get an ImageResult given a Node Chat Room Id and Chat Message Id.
     *
     * @param chatRoomId        Chat Message Room Id
     * @param chatMessageId     Chat Message Id
     * @param fullSize          Flag to request full size image despite data/size requirements
     * @param highPriority      Flag to request image with high priority
     * @param resetDownloads    Callback to reset downloads
     *
     * @return Flow<ImageResult>
     */
    suspend operator fun invoke(
        chatRoomId: Long,
        chatMessageId: Long,
        fullSize: Boolean,
        highPriority: Boolean,
        resetDownloads: () -> Unit,
    ): Flow<ImageResult> {
        return imageRepository.getImageForChatMessage(
            chatRoomId,
            chatMessageId,
            fullSize,
            highPriority,
            networkRepository.isMeteredConnection() ?: false,
            resetDownloads
        )
    }
}