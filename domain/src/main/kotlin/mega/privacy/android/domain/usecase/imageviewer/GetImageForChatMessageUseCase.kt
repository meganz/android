package mega.privacy.android.domain.usecase.imageviewer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.repository.ImageRepository
import mega.privacy.android.domain.usecase.node.AddImageTypeUseCase
import javax.inject.Inject

/**
 * The use case to get an ImageResult given Chat Room Id and Chat Message Id.
 */
class GetImageForChatMessageUseCase @Inject constructor(
    private val addImageTypeUseCase: AddImageTypeUseCase,
    private val getImageUseCase: GetImageUseCase,
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
    ): Flow<ImageResult> = flow {
        val node = addImageTypeUseCase(
            imageRepository.getImageNodeForChatMessage(
                chatRoomId,
                chatMessageId
            )
        )
        emitAll(getImageUseCase(node, fullSize, highPriority, resetDownloads))
    }
}
