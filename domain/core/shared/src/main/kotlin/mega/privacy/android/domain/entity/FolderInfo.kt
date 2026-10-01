package mega.privacy.android.domain.entity

import mega.privacy.android.domain.entity.node.NodeId

/**
 * Folder Information
 *
 * @property id NodeId of the folder
 * @property currentSize Total size of files inside the folder
 * @property numVersions  Number of file versions inside the folder
 * @property numFiles Number of files inside the folder
 * @property numFolders Number of folders inside the folder
 * @property versionsSize Total size of file versions inside the folder
 * @property folderName Name of the folder
 */
data class FolderInfo(
    val id: NodeId,
    val currentSize: Long,
    val numVersions: Int,
    val numFiles: Int,
    val numFolders: Int,
    val versionsSize: Long,
    val folderName: String,
)
