package mega.privacy.android.data.model.chat

import android.net.Uri
import nz.mega.sdk.MegaNode

/**
 * Android mega rich link message
 *
 * @property url
 * @property server
 * @property folderName
 * @property folderContent
 * @property node
 * @property isFile
 * @property isChat
 * @property title
 * @property numParticipants
 */
data class AndroidMegaRichLinkMessage(
    var url: String,
    var server: String?,
    var folderName: String? = null,
    var folderContent: String? = null,
    var node: MegaNode? = null,
    var isFile: Boolean = false,
    var isChat: Boolean = false,
    var title: String? = null,
    var numParticipants: Long = 0,
) {
    constructor(url: String, folderContent: String?, folderName: String?)
            : this(url = url, server = Uri.parse(url).authority) {
        this.folderContent = folderContent
        this.folderName = folderName
    }

    constructor(url: String, node: MegaNode?) : this(url = url, server = Uri.parse(url).authority) {
        this.node = node
        isFile = true
    }

    constructor(url: String, title: String?, participants: Long)
            : this(url = url, server = Uri.parse(url).authority) {
        this.title = title
        numParticipants = participants
        isChat = true
    }
}