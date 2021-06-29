package mega.privacy.android.app.textEditor

import android.net.Uri
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaNode

data class TextEditorData(
    var api: MegaApiAndroid? = null,
    var node: MegaNode? = null,
    var fileUri: Uri? = null,
    var fileSize: Long? = null,
    var adapterType: Int = INVALID_VALUE,
    var editableAdapter: Boolean = false,
    var msgChat: MegaChatMessage? = null,
    var chatRoom: MegaChatRoom? = null,
    var needStopHttpServer: Boolean = false
)
