package mega.privacy.android.app.components.dragger

/**
 * Class holding scroll event info.
 *
 * @property viewerFrom an identifier that shows where the viewer is opened from
 * @property handle current thumbnail node handle
 */
data class ScrollEvent(
    val viewerFrom: Int,
    val handle: Long
)
