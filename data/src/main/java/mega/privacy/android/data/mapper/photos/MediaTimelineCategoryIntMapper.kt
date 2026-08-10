package mega.privacy.android.data.mapper.photos

import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Category
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

/**
 * Two-way mapper between [Category] and the matching
 * [MegaApiJava] file type Int value.
 */
internal class MediaTimelineCategoryIntMapper @Inject constructor() {

    /**
     * Maps a [Category] into the SDK file type Int value.
     */
    operator fun invoke(category: Category): Int = when (category) {
        Category.Photos -> MegaApiJava.FILE_TYPE_PHOTO
        Category.Videos -> MegaApiJava.FILE_TYPE_VIDEO
        Category.All -> MegaApiJava.FILE_TYPE_ALL_VISUAL_MEDIA
    }

    /**
     * Maps an SDK file type Int value back into a [Category].
     */
    operator fun invoke(value: Int): Category = when (value) {
        MegaApiJava.FILE_TYPE_PHOTO -> Category.Photos
        MegaApiJava.FILE_TYPE_VIDEO -> Category.Videos
        MegaApiJava.FILE_TYPE_ALL_VISUAL_MEDIA -> Category.All
        else -> throw IllegalArgumentException("Unknown file type value: $value")
    }
}
