package mega.privacy.android.app.presentation.clouddrive

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.app.MimeTypeList
import nz.mega.sdk.MegaNode
import javax.inject.Inject

@HiltViewModel
class FileBrowserViewModel @Inject constructor() : ViewModel() {

    /**
     * If a folder only contains images or videos, then go to MD mode directly
     */
    fun shouldEnterMDMode(nodes: List<MegaNode>): Boolean {
        if (nodes.isEmpty())
            return false
        for (node: MegaNode in nodes) {
            if (node.isFolder ||
                !MimeTypeList.typeForName(node.name).isImage &&
                !MimeTypeList.typeForName(node.name).isVideoReproducible
            ) {
                return false
            }
        }
        return true
    }
}