package mega.privacy.android.app.components.dragger

/**
 * Class holding thumbnail location event info.
 *
 * @property viewerFrom an identifier that shows where the viewer is opened from
 * @property location thumbnail location
 */
class ThumbnailLocationEvent(
    val viewerFrom: Int,
    val location: IntArray
)
