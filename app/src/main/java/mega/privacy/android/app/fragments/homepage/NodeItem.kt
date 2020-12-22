package mega.privacy.android.app.fragments.homepage

import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import nz.mega.sdk.MegaNode
import java.io.File

open class NodeItem(
    open var node: MegaNode? = null,
    open var index: Int = INVALID_POSITION,      // Index of Node including TYPE_TITLE node (RecyclerView Layout position)
    open var isVideo: Boolean = false,
    open var modifiedDate: String = "",
    open var thumbnail: File? = null,
    open var selected: Boolean = false,
    open var uiDirty: Boolean = true   // Force refresh the newly created Node list item
)
