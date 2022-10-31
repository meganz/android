package mega.privacy.android.domain.entity.set

import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo

/**
 * This interface defines the MegaSet entity from SDK
 */
interface UserSet {

    /**
     * The Set ID
     */
    val id: AlbumId

    /**
     * The Set name
     */
    val name: String

    /**
     * The Set cover
     */
    val cover: Photo?
}