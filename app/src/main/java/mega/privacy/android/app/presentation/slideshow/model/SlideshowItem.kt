package mega.privacy.android.app.presentation.slideshow.model

import mega.privacy.android.domain.entity.photos.Photo

/**
 * @property photo current photo
 */
sealed interface SlideshowItem {
    val photo: Photo

    data class DefaultItem(
        override val photo: Photo,
    ) : SlideshowItem

    data class ChatItem(
        override val photo: Photo,
        val chatRoomId: Long,
        val messageId: Long,
    ) : SlideshowItem

    data class PublicLinkItem(
        override val photo: Photo,
        val link: String,
    ) : SlideshowItem
}




