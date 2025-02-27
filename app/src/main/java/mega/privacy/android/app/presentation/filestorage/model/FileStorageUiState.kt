package mega.privacy.android.app.presentation.filestorage.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.FileDocument
import mega.privacy.android.domain.entity.file.FileStorageType
import mega.privacy.android.domain.entity.uri.UriPath

sealed interface FileStorageUiState {

    data object Loading : FileStorageUiState

    /**
     * UI state for FileStorageActivity
     * @property currentFolderPath Current folder path as string for UI text
     * @property currentFolder Current folder document
     * @property children Children of current folder
     * @property storageType Storage type of current folder
     * @property isInCacheDirectory If the current folder is in cache directory
     * @property folderPickedEvent event triggered when a folder is picked with the system folder picker
     */
    data class Loaded(
        val currentFolderPath: String = "",
        val currentFolder: FileDocument? = null,
        val children: List<FileDocument> = emptyList(),
        val storageType: FileStorageType = FileStorageType.Unknown,
        val isInCacheDirectory: Boolean = false,
        val folderPickedEvent: StateEventWithContent<UriPath> = consumed(),
    ) : FileStorageUiState {

        /**
         * @return current highlight position, if any
         */
        fun getHighlightFilePosition() =
            children.indexOfFirst { it.isHighlighted }.takeIf { it >= 0 }
    }
}
