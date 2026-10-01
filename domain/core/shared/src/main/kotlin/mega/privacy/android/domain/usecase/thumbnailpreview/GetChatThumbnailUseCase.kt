package mega.privacy.android.domain.usecase.thumbnailpreview

import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import javax.inject.Inject

/**
 * The use case implementation class to get chat thumbnail
 */
class GetChatThumbnailUseCase @Inject constructor(
    private val getChatFileUseCase: GetChatFileUseCase,
    private val getPreviewUseCase: GetPreviewUseCase,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(chatId: Long, messageId: Long) =
        getChatFileUseCase(chatId, messageId)?.let {
            getPreviewUseCase(it)
        }
}