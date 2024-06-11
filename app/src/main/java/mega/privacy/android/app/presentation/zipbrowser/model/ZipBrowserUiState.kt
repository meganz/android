package mega.privacy.android.app.presentation.zipbrowser.model

import mega.privacy.android.domain.entity.zipbrowser.ZipTreeNode

/**
 * Zip browser ui state
 *
 * @property items current displayed items
 * @property folderDepth the current folder depth level
 * @property parentFolderName parent folder name
 * @property currentZipTreeNode the current ZipTreeNode
 * @property showUnzipProgressBar whether should show the unzip progress bar
 * @property showAlertDialog whether should show the file cannot open alert dialog
 * @property showSnackBar whether should show the snack bar
 * @property openedFile opened file
 */
data class ZipBrowserUiState(
    val items: List<ZipInfoUiEntity> = emptyList(),
    val folderDepth: Int = 0,
    val parentFolderName: String = "",
    val currentZipTreeNode: ZipTreeNode? = null,
    val showUnzipProgressBar: Boolean = false,
    val showAlertDialog: Boolean = false,
    val showSnackBar: Boolean = false,
    val openedFile: ZipInfoUiEntity? = null,
)