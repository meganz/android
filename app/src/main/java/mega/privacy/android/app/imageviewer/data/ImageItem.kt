package mega.privacy.android.app.imageviewer.data

import android.net.Uri
import mega.privacy.android.app.usecase.data.MegaNodeItem

/**
 * Data object that encapsulates an item representing an Image.
 */
sealed class ImageItem {

    abstract val id: Long
    abstract val name: String
    abstract val infoText: String
    abstract val nodeItem: MegaNodeItem?
    abstract val imageResult: ImageResult?

    data class Node constructor(
        val handle: Long,
        override val id: Long,
        override val name: String,
        override val infoText: String,
        override val nodeItem: MegaNodeItem? = null,
        override val imageResult: ImageResult? = null
    ) : ImageItem()

    data class OfflineNode constructor(
        val handle: Long,
        val fileUri: Uri,
        override val id: Long,
        override val name: String,
        override val infoText: String,
        override val nodeItem: MegaNodeItem? = null,
        override val imageResult: ImageResult? = null,
    ) : ImageItem()

    data class PublicNode constructor(
        val handle: Long,
        val nodePublicLink: String,
        override val id: Long,
        override val name: String,
        override val infoText: String,
        override val nodeItem: MegaNodeItem? = null,
        override val imageResult: ImageResult? = null
    ) : ImageItem()

    data class ChatNode constructor(
        val handle: Long,
        val chatMessageId: Long,
        val chatRoomId: Long,
        val isDeletable: Boolean = false,
        override val id: Long,
        override val name: String,
        override val infoText: String,
        override val nodeItem: MegaNodeItem? = null,
        override val imageResult: ImageResult? = null
    ) : ImageItem()

    data class File constructor(
        val fileUri: Uri,
        override val id: Long,
        override val name: String,
        override val infoText: String,
        override val nodeItem: MegaNodeItem? = null,
        override val imageResult: ImageResult? = null
    ) : ImageItem()

    fun getNodeHandle(): Long? =
        when (this) {
            is Node -> handle
            is ChatNode -> handle
            is PublicNode -> handle
            else -> null
        }

    fun copy(nodeItem: MegaNodeItem? = null, imageResult: ImageResult? = null): ImageItem =
        when (this) {
            is Node -> copy(handle, id, name, infoText, nodeItem, imageResult)
            is OfflineNode -> copy(handle, fileUri, id, name, infoText, nodeItem, imageResult)
            is ChatNode -> copy(handle, chatMessageId, chatRoomId, isDeletable, id, name, infoText, nodeItem, imageResult)
            is PublicNode -> copy(handle, nodePublicLink, id, name, infoText, nodeItem, imageResult)
            is File -> copy(fileUri, id, name, infoText, nodeItem, imageResult)
        }
}
