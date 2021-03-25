package mega.privacy.android.app.utils

import nz.mega.sdk.MegaApiJava

/**
 * This class hold location info of a node.
 *
 * @property location the human readable location of this node
 * @property offlineParentPath parent path, only used for offline adapter
 * @property parentHandle parent handle, only used for non-offline adapter
 * @property fragmentHandle fragment handle, only used for non-offline adapter
 */
data class LocationInfo(
    val location: String,
    val offlineParentPath: String? = null,
    val parentHandle: Long = MegaApiJava.INVALID_HANDLE,
    val fragmentHandle: Long = MegaApiJava.INVALID_HANDLE,
)
