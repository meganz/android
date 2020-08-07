package mega.privacy.android.app.fragments.photos

import nz.mega.sdk.MegaNode
import java.io.File

data class PhotoNode(
    val node: MegaNode?,
    val index: Int,
    var thumbnail: File?,
    val type: Int,
    val modifiedDate: String,
    var selected: Boolean
) {
    companion object {
        const val TYPE_TITLE = 0   // The datetime header
        const val TYPE_PHOTO = 1
    }
}