package mega.privacy.android.domain.entity.node

import kotlinx.serialization.Polymorphic


/**
 * Node
 */
@Polymorphic
interface Node {

    /**
     * Id
     */
    val id: NodeId

    /**
     * Name
     */
    val name: String

    /**
     * Parent id
     */
    val parentId: NodeId

    /**
     * Base64id
     */
    val base64Id: String

    /**
     * Restore id
     */
    val restoreId: NodeId?

    /**
     * Label
     */
    val label: Int

    /**
     * Is favourite
     */
    val isFavourite: Boolean

    /**
     * Is sensitive
     */
    val isMarkedSensitive: Boolean

    /**
     * Is sensitive inherited from parent node
     */
    val isSensitiveInherited: Boolean

    /**
     * Exported data if the node is exported (shared with public link), null otherwise
     */
    val exportedData: ExportedData?

    /**
     * Is taken down
     */
    val isTakenDown: Boolean

    /**
     * Is the node an incoming share
     */
    val isIncomingShare: Boolean

    /**
     * Is node key decrypted by verification from owner
     */
    val isNodeKeyDecrypted: Boolean

    /**
     * Creation time
     */
    val creationTime: Long

    /**
     * Serialized string of the node
     * This value should be set only if required and not always for all the nodes
     */
    val serializedData: String?

    /**
     * offline available
     */
    val isAvailableOffline: Boolean

    /**
     * version count exclude current version
     */
    val versionCount: Int

    /**
     * Description of the node
     */
    val description: String?

    /**
     * Tags associated with the node
     */
    val tags: List<String>?

    /**
     * Has version
     */
    val hasVersion: Boolean
        get() = versionCount > 0
}

