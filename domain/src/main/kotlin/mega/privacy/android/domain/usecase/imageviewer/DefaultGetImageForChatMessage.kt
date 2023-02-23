package mega.privacy.android.domain.usecase.imageviewer

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.repository.ImageRepository
import mega.privacy.android.domain.repository.NetworkRepository

/**
 * Default Implementation of [GetImageForChatMessage]
 */
class DefaultGetImageForChatMessage(
    private val networkRepository: NetworkRepository,
    private val imageRepository: ImageRepository,
) : GetImageForChatMessage {
    override suspend fun invoke(
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