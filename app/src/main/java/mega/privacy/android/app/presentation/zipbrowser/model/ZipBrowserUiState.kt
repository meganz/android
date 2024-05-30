package mega.privacy.android.app.presentation.zipbrowser.model

import mega.privacy.android.domain.entity.zipbrowser.ZipTreeNode

/**
 * Zip browser ui state
 *
 * @property items current displayed items
 * @property folderDepth the current folder depth level
 * @property parentFolderName parent folder name
 * @property currentZipTreeNode the current ZipTreeNode
 */
data class ZipBrowserUiState(
    val items: List<ZipInfoUiEntity> = emptyList(),
    val folderDepth: Int = 0,
    val parentFolderName: String = "",
    val currentZipTreeNode: ZipTreeNode? = null
)