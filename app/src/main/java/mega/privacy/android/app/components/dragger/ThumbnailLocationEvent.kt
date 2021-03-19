package mega.privacy.android.app.components.dragger

/**
 * Class holding thumbnail location event info.
 *
 * @property viewerFrom an identifier that shows where the viewer is opened from
 * @property location thumbnail location
 */
data class ThumbnailLocationEvent(
    val viewerFrom: Int,
    val location: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ThumbnailLocationEvent

        if (viewerFrom != other.viewerFrom) return false
        if (!location.contentEquals(other.location)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = viewerFrom
        result = 31 * result + location.contentHashCode()
        return result
    }
}
