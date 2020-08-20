package mega.privacy.android.app.fragments.photos

import nz.mega.sdk.MegaNode
import java.io.File

interface SelectableNode {
    var node: MegaNode?
    var index: Int
    var modifiedDate: String
    var thumbnail: File?
    var selected: Boolean
    var uiDirty: Boolean
}