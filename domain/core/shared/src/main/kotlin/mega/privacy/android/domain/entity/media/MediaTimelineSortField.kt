package mega.privacy.android.domain.entity.media

/**
 * The timestamp the media timeline orders and buckets its media by.
 */
enum class MediaTimelineSortField {
    /** The media capture timestamp — when the photo or video was taken. */
    DateTaken,

    /** The node modification timestamp. */
    DateAdded,
}
