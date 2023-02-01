package mega.privacy.android.domain.entity.node


/**
 * Folder Node
 */
interface FolderNode : UnTypedNode {
    /**
     * Is folder in the rubbish bin
     */
    val isInRubbishBin: Boolean

    /**
     * Is the folder an outgoing share
     */
    val isShared: Boolean

    /**
     * Is the folder a pending outgoing share
     */
    val isPendingShare: Boolean

    /**
     * Device
     */
    val device: String?

    /**
     * Number of child folders
     */
    val childFolderCount: Int

    /**
     * Number of child files
     */
    val childFileCount: Int
}
