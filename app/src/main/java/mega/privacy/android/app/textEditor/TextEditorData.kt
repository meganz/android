package mega.privacy.android.app.textEditor

import android.net.Uri
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.domain.entity.node.ViewerNode
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaNode

/**
 * Data class containing required info for the Text Editor.
 *
 * @property api                MegaApiFolder if node comes from a folder link, MegaApi otherwise.
 * @property node               [MegaNode]
 * @property fileUri            Uri of the file if any.
 * @property fileSize           Size of the file if any.
 * @property adapterType        Type of adapter from where the node comes.
 * @property editableAdapter    True if can edit, false otherwise.
 * @property msgChat            [MegaChatMessage]
 * @property chatRoom           [MegaChatRoom]
 * @property needStopHttpServer True if the sever was initialized, false otherwise.
 * @property viewerNode         [ViewerNode]
 */
data class TextEditorData(
    var api: MegaApiAndroid? = null,
    var node: MegaNode? = null,
    var fileUri: Uri? = null,
    var fileSize: Long? = null,
    var adapterType: Int = INVALID_VALUE,
    var editableAdapter: Boolean = false,
    var msgChat: MegaChatMessage? = null,
    var chatRoom: MegaChatRoom? = null,
    var needStopHttpServer: Boolean = false,
    var viewerNode: ViewerNode? = null
)
