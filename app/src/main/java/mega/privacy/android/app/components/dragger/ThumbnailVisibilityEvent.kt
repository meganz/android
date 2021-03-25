package mega.privacy.android.app.components.dragger

import nz.mega.sdk.MegaApiJava.INVALID_HANDLE

/**
 * Class holding thumbnail visibility event info.
 *
 * @property viewerFrom an identifier that shows where the viewer is opened from
 * @property handle current thumbnail node handle
 * @property visible whether current thumbnail should be visible
 * @property previousHiddenHandle previously hidden thumbnail node handle
 */
data class ThumbnailVisibilityEvent(
    val viewerFrom: Int,
    val handle: Long,
    val visible: Boolean,
    val previousHiddenHandle: Long = INVALID_HANDLE
)
