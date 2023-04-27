package mega.privacy.android.domain.usecase.slideshow

import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Get photo by message id use case
 */
class GetChatPhotoByMessageIdUseCase @Inject constructor(
    private val repository: PhotosRepository,
) {
    /**
     * Get Photo by message id use case
     * @return photo
     */
    suspend operator fun invoke(chatRoomId: Long, messageId: Long): Photo? =
        repository.getChatPhotoByMessageId(chatId = chatRoomId, messageId = messageId)
}