package mega.privacy.android.app.presentation.filestorage.model

import mega.privacy.android.domain.entity.file.FileStorageType

/**
 * UI state for FileStorageActivity
 * @property storageType Storage type of current file
 */
data class FileStorageUiState(
    val storageType: FileStorageType = FileStorageType.Unknown,
)