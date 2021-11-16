package mega.privacy.android.app.usecase.data

import nz.mega.sdk.MegaNode

/**
 * Data object that encapsulates an item representing a Mega Node with extra information.
 *
 * @property node               MegaNode itself
 * @property infoText           Node information preformatted text
 * @property hasFullAccess      Flag to check if current user has full access to the node.
 * @property isFromRubbishBin   Flag to check if this node is a child of Rubbish bin node.
 * @property isFromInbox        Flag to check if this node is a child of Inbox node.
 * @property isFromRoot         Flag to check if this node is a child of Root node.
 * @property isAvailableOffline Flag to check if this node is available offline.
 * @property hasVersions        Flag to check if this node has versions.
 */
data class MegaNodeItem constructor(
    val node: MegaNode,
    val infoText: String,
    val hasFullAccess: Boolean,
    val isFromRubbishBin: Boolean,
    val isFromInbox: Boolean,
    val isFromRoot: Boolean,
    val isAvailableOffline: Boolean,
    val hasVersions: Boolean
)
