package mega.privacy.android.app.presentation.photos.albums.model

import mega.privacy.android.domain.entity.photos.Photo

/**
 * Different Album Photo Item types
 */
sealed interface AlbumPhotoItem {
    val key: String

    data class BigSmall2Item(
        val photos: List<Photo>,
    ) : AlbumPhotoItem {
        override val key: String
            get() = photos.first().toString()
    }


    data class Small3Item(
        val photos: List<Photo>,
    ) : AlbumPhotoItem {
        override val key: String
            get() = photos.first().toString()
    }


    data class Small2BigItem(
        val photos: List<Photo>,
    ) : AlbumPhotoItem {
        override val key: String
            get() = photos.first().toString()
    }

}
