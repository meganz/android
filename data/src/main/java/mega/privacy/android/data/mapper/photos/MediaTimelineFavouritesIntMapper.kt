package mega.privacy.android.data.mapper.photos

import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Favourites
import nz.mega.sdk.MegaSearchFilter.BOOL_FILTER_DISABLED
import nz.mega.sdk.MegaSearchFilter.BOOL_FILTER_ONLY_FALSE
import nz.mega.sdk.MegaSearchFilter.BOOL_FILTER_ONLY_TRUE
import javax.inject.Inject

/**
 * Two-way mapper between [Favourites] and the matching SDK favourite bool-filter Int value
 * used by `byFavourite`.
 */
internal class MediaTimelineFavouritesIntMapper @Inject constructor() {

    /**
     * Maps a [Favourites] into the SDK favourite bool-filter Int value.
     */
    operator fun invoke(favourites: Favourites): Int = when (favourites) {
        Favourites.All -> BOOL_FILTER_DISABLED
        Favourites.Favourites -> BOOL_FILTER_ONLY_TRUE
        Favourites.NonFavourites -> BOOL_FILTER_ONLY_FALSE
    }

    /**
     * Maps an SDK favourite bool-filter Int value back into a [Favourites].
     */
    operator fun invoke(value: Int): Favourites = when (value) {
        BOOL_FILTER_DISABLED -> Favourites.All
        BOOL_FILTER_ONLY_TRUE -> Favourites.Favourites
        BOOL_FILTER_ONLY_FALSE -> Favourites.NonFavourites
        else -> throw IllegalArgumentException("Unknown favourite filter value: $value")
    }
}
