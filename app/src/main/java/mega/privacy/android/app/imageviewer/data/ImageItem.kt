package mega.privacy.android.app.imageviewer.data

import android.content.Context
import android.net.Uri
import mega.privacy.android.app.usecase.data.MegaNodeItem
import mega.privacy.android.app.utils.MegaNodeUtil.getInfoText
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.photos.Album

/**
 * Data object that encapsulates an item representing an Image.
 */
sealed class ImageItem {

    abstract val id: Long
    abstract val name: String
    abstract val infoText: String
    abstract val nodeItem: MegaNodeItem?
    abstract val imageResult: ImageResult?

    /**
     * Data object that encapsulates an item representing an Image from a MegaNode.
     *
     * @property handle         MegaNode handle
     * @property id             Image item unique Id
     * @property name           Image name
     * @property infoText       Image information preformatted text
     * @property nodeItem       Image node item
     * @property imageResult    Image result containing each Image Uri
     */
    data class Node constructor(
        val handle: Long,
        override val id: Long,
        override val name: String,
        override val infoText: String = "",
        override val nodeItem: MegaNodeItem? = null,
        override val imageResult: ImageResult? = null,
    ) : ImageItem()

    /**
     * Data object that encapsulates an item representing an Image from a MegaOffline.
     *
     * @property handle         MegaOffline handle
     * @property fileUri        MegaOffline file uri
     * @property id             Image item unique Id
     * @property name           Image name
     * @property infoText       Image information preformatted text
     * @property nodeItem       Image node item
     * @property imageResult    Image result containing each Image Uri
     */
    data class OfflineNode constructor(
        val handle: Long,
        val fileUri: Uri,
        override val id: Long,
        override val name: String,
        override val infoText: String,
        override val nodeItem: MegaNodeItem? = null,
        override val imageResult: ImageResult? = null,
    ) : ImageItem()

    /**
     * Data object that encapsulates an item representing an Image from a Public MegaNode.
     *
     * @property handle         MegaNode handle
     * @property nodePublicLink MegaNode public link
     * @property id             Image item unique Id
     * @property name           Image name
     * @property infoText       Image information preformatted text
     * @property nodeItem       Image node item
     * @property imageResult    Image result containing each Image Uri
     */
    data class PublicNode constructor(
        val handle: Long,
        val nodePublicLink: String,
        override val id: Long,
        override val name: String,
        override val infoText: String,
        override val nodeItem: MegaNodeItem? = null,
        override val imageResult: ImageResult? = null,
    ) : ImageItem()

    /**
     * Data object that encapsulates an item representing an Image from a Chat MegaNode.
     *
     * @property handle         MegaNode handle
     * @property chatMessageId  Chat Message Id
     * @property chatRoomId     Chat Room Id
     * @property isDeletable    Flag to check if chat node is deletable
     * @property id             Image item unique Id
     * @property name           Image name
     * @property infoText       Image information preformatted text
     * @property nodeItem       Image node item
     * @property imageResult    Image result containing each Image Uri
     */
    data class ChatNode constructor(
        val handle: Long,
        val chatMessageId: Long,
        val chatRoomId: Long,
        val isDeletable: Boolean = false,
        override val id: Long,
        override val name: String,
        override val infoText: String,
        override val nodeItem: MegaNodeItem? = null,
        override val imageResult: ImageResult? = null,
    ) : ImageItem()

    /**
     * Data object that encapsulates an item representing an Image from a File.
     *
     * @property fileUri        File uri
     * @property id             Image item unique Id
     * @property name           Image name
     * @property infoText       Image information preformatted text
     * @property nodeItem       Image node item
     * @property imageResult    Image result containing each Image Uri
     */
    data class File constructor(
        val fileUri: Uri,
        override val id: Long,
        override val name: String,
        override val infoText: String,
        override val nodeItem: MegaNodeItem? = null,
        override val imageResult: ImageResult? = null,
    ) : ImageItem()

    /**
     * Data object that encapsulates an item representing an Image from a MegaNode for Album Sharing.
     *
     * @property handle         MegaNode handle
     * @property id             Image item unique Id
     * @property name           Image name
     * @property infoText       Image information preformatted text
     * @property nodeItem       Image node item
     * @property imageResult    Image result containing each Image Uri
     */
    data class AlbumImportNode constructor(
        val handle: Long,
        override val id: Long,
        override val name: String,
        override val infoText: String = "",
        override val nodeItem: MegaNodeItem? = null,
        override val imageResult: ImageResult? = null,
    ) : ImageItem()

    /**
     * Get MegaNode handle when ImageItem has one.
     *
     * @return  MegaNode handle
     */
    fun getNodeHandle(): Long? =
        when (this) {
            is Node -> handle
            is ChatNode -> handle
            is PublicNode -> handle
            is OfflineNode -> handle
            is AlbumImportNode -> handle
            else -> null
        }

    override fun hashCode(): Int =
        super.hashCode() + (nodeItem?.hashCode() ?: 0) + (imageResult?.hashCode() ?: 0)

    override fun equals(other: Any?): Boolean =
        when (other) {
            is ImageItem -> {
                super.equals(other)
                        && this.nodeItem?.hashCode() == other.nodeItem?.hashCode()
                        && this.imageResult?.hashCode() == other.imageResult?.hashCode()
            }

            else -> false
        }

    /**
     * Get a copy of an existing ImageItem by replacing nodeItem or imageResult fields.
     *
     * @param nodeItem      NodeItem to be replaced
     * @param imageResult   ImageResult to be replaced
     * @return              ImageItem with updated fields
     */
    fun copy(
        nodeItem: MegaNodeItem? = null,
        imageResult: ImageResult? = null,
        context: Context,
    ): ImageItem =
        when (this) {
            is Node -> copy(
                handle,
                id,
                nodeItem?.name ?: name,
                nodeItem?.node?.getInfoText(context) ?: infoText,
                nodeItem ?: this.nodeItem,
                imageResult ?: this.imageResult
            )

            is OfflineNode -> copy(
                handle,
                fileUri,
                id,
                nodeItem?.name ?: name,
                nodeItem?.node?.getInfoText(context) ?: infoText,
                nodeItem ?: this.nodeItem,
                imageResult ?: this.imageResult
            )

            is ChatNode -> copy(
                handle,
                chatMessageId,
                chatRoomId,
                isDeletable,
                id,
                nodeItem?.name ?: name,
                nodeItem?.node?.getInfoText(context) ?: infoText,
                nodeItem ?: this.nodeItem,
                imageResult ?: this.imageResult
            )

            is PublicNode -> copy(
                handle,
                nodePublicLink,
                id,
                nodeItem?.name ?: name,
                nodeItem?.node?.getInfoText(context) ?: infoText,
                nodeItem ?: this.nodeItem,
                imageResult ?: this.imageResult
            )

            is File -> copy(
                fileUri,
                id,
                name,
                infoText,
                nodeItem ?: this.nodeItem,
                imageResult ?: this.imageResult
            )

            is AlbumImportNode -> copy(
                handle,
                id,
                nodeItem?.name ?: name,
                nodeItem?.node?.getInfoText(context) ?: infoText,
                nodeItem ?: this.nodeItem,
                imageResult ?: this.imageResult
            )
        }
}
