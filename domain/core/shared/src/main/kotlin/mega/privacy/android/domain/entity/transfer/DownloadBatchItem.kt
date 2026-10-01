package mega.privacy.android.domain.entity.transfer

import mega.privacy.android.domain.entity.node.TypedNode

/**
 * One root node of a batch download, with the settings that apply to it alone.
 *
 * @property node             The node to download, it can be a folder.
 * @property destinationPath  Full path to the destination folder of [node].
 * @property fileName         Custom name for the node in the local destination.
 * @property appData          Custom app data to save in the MegaTransfer object of this item.
 * @property isHighPriority   Puts the transfer on top of the download queue.
 */
data class DownloadBatchItem(
    val node: TypedNode,
    val destinationPath: String,
    val fileName: String?,
    val appData: List<TransferAppData>?,
    val isHighPriority: Boolean,
)
