package mega.privacy.android.domain.entity.media

/**
 * Interface for system album
 */
interface SystemAlbum {
    /**
     * Name of the album
     */
    val albumNameResId: Int

    /**
     * Flag to determine if System album should be hidden when empty
     */
    val hideWhenEmpty: Boolean

    /**
     * The [MediaTimelineFilter] describing which media nodes belong to this album.
     *
     * The album owns everything except sensitivity, which the caller overrides per emission based
     * on the current hidden-items setting and account level.
     */
    val mediaTimelineFilter: MediaTimelineFilter
}
