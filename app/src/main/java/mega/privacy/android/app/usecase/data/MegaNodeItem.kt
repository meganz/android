package mega.privacy.android.app.usecase.data

import nz.mega.sdk.MegaNode

/**
 * Data object that encapsulates an item representing a Mega Node with extra information.
 *
 * @property node               MegaNode itself
 * @property hasReadAccess      Flag indicating if current user can read the node.
 * @property hasReadWriteAccess Flag indicating if current user can read and write the node.
 * @property hasFullAccess      Flag indicating if current user has full permissions over the node.
 * @property hasOwnerAccess     Flag indicating if current user is the owner of the node.
 * @property isFromIncoming     Flag indicating if node is an Incoming node.
 * @property isFromRubbishBin   Flag indicating if node is a child of Rubbish bin node.
 * @property isFromInbox        Flag indicating if node is a child of Inbox node.
 * @property isFromRoot         Flag indicating if node is a child of Root node.
 * @property isExternalNode     Flag indicating if node is external.
 * @property isAvailableOffline Flag indicating if node is available offline.
 * @property hasVersions        Flag indicating if node has versions.
 */
data class MegaNodeItem constructor(
    val name: String,
    val handle: Long,
    val hasReadAccess: Boolean = false,
    val hasReadWriteAccess: Boolean = false,
    val hasFullAccess: Boolean = false,
    val hasOwnerAccess: Boolean = false,
    val isFromIncoming: Boolean = false,
    val isFromRubbishBin: Boolean = false,
    val isFromInbox: Boolean = false,
    val isFromRoot: Boolean = false,
    val isExternalNode: Boolean = false,
    val hasVersions: Boolean = false,
    val isAvailableOffline: Boolean = false,
    val node: MegaNode? = null
)
