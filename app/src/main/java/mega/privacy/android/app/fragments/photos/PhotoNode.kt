package mega.privacy.android.app.fragments.photos

import nz.mega.sdk.MegaNode
import java.io.File

data class PhotoNode(
    val type: Int,
    override val node: MegaNode?,
    override val index: Int,
    override val modifiedDate: String,
    override var thumbnail: File?,
    override var selected: Boolean,
    override var uiDirty: Boolean = false
) : SelectableNode {
    companion object {
        const val TYPE_TITLE = 0   // The datetime header
        const val TYPE_PHOTO = 1
    }
}