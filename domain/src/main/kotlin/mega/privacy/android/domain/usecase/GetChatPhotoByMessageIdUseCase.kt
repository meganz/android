package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.photos.Photo

/**
 * Get photo by message id use case
 */
fun interface GetChatPhotoByMessageIdUseCase {
    /**
     * Get Photo by message id use case
     * @return photo
     */
    suspend operator fun invoke(chatRoomId: Long, messageId: Long): Photo?
}