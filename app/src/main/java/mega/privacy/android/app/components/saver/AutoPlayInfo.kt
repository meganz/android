package mega.privacy.android.app.components.saver

import nz.mega.sdk.MegaApiJava.INVALID_HANDLE

/**
 * Holder class for auto play info.
 *
 * @property nodeName name of the node
 * @property nodeHandle handle of the node
 * @property localPath local path of the node file
 * @property couldAutoPlay whether could auto play
 */
data class AutoPlayInfo(
    val nodeName: String,
    val nodeHandle: Long,
    val localPath: String,
    val couldAutoPlay: Boolean = true,
) {
    companion object {
        val NO_AUTO_PLAY = AutoPlayInfo("", INVALID_HANDLE, "", false)
    }
}
