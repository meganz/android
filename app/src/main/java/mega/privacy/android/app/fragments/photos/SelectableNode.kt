package mega.privacy.android.app.fragments.photos

import nz.mega.sdk.MegaNode
import java.io.File

interface SelectableNode {
    val node: MegaNode?
    val index: Int
    val modifiedDate: String
    var thumbnail: File?
    var selected: Boolean
}