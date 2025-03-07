package mega.privacy.android.app.presentation.filestorage

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.FileDocument
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.presentation.filestorage.model.FileStorageUiState
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.extensions.isFile
import mega.privacy.android.data.extensions.toUri
import mega.privacy.android.domain.entity.file.FileStorageType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.file.GetDocumentEntityUseCase
import mega.privacy.android.domain.usecase.file.GetExternalPathByContentUriUseCase
import mega.privacy.android.domain.usecase.file.GetFileStorageTypeNameUseCase
import mega.privacy.android.domain.usecase.file.GetFileUriUseCase
import mega.privacy.android.domain.usecase.file.GetFilesInDocumentFolderUseCase
import mega.privacy.android.domain.usecase.file.GetPathByDocumentContentUriUseCase
import mega.privacy.android.domain.usecase.file.IsUriPathInCacheUseCase
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for FileStorageActivity
 */
@HiltViewModel
class FileStorageViewModel @Inject constructor(
    private val getFileStorageTypeNameUseCase: GetFileStorageTypeNameUseCase,
    private val getPathByDocumentContentUriUseCase: GetPathByDocumentContentUriUseCase,
    private val getFilesInDocumentFolderUseCase: GetFilesInDocumentFolderUseCase,
    private val isUriPathInCacheUseCase: IsUriPathInCacheUseCase,
    private val getDocumentEntityUseCase: GetDocumentEntityUseCase,
    private val getExternalPathByContentUriUseCase: GetExternalPathByContentUriUseCase,
    private val getFileUriUseCase: GetFileUriUseCase,
) : ViewModel() {

    /**
     * UI state
     */
    val uiState: StateFlow<FileStorageUiState>
        field: MutableStateFlow<FileStorageUiState> = MutableStateFlow(FileStorageUiState.Loading)

    /**
     * UI state for loaded state only
     */
    val uiLoadedState = uiState.filterIsInstance<FileStorageUiState.Loaded>()

    /**
     * Sets the root [UriPath] to browse and sets it to current folder
     * @param uriPath
     * @param updateStorageType if true, the storage type will be checked, storage type not changed otherwise
     * @param highlightFileName if not null, the name of the file to be highlighted
     */
    fun setRootPath(
        uriPath: UriPath,
        updateStorageType: Boolean = false,
        highlightFileName: String? = null,
    ) {
        viewModelScope.launch {
            updateUiState(
                uriPath,
                storageType = if (updateStorageType) getStorageType(uriPath) else null,
                isInCacheDirectory = isUriPathInCacheUseCase(uriPath),
                highlightFileName = highlightFileName,
            )
        }
    }

    /**
     * Changes the current path to [uriPath] and the current path as its parent
     */
    fun goToChild(uriPath: UriPath) {
        (uiState.value as? FileStorageUiState.Loaded)?.currentFolder?.let { currentFolder ->
            viewModelScope.launch {
                updateUiState(uriPath, currentFolder)
            }
        }
    }

    /**
     * Changes the current path to the current path parent
     */
    fun goToParent(): Boolean {
        return (uiState.value as? FileStorageUiState.Loaded)?.currentFolder?.parent?.also { parent ->
            viewModelScope.launch {
                updateUiState(parent.uriPath, parent.parent)
            }
        } != null
    }

    /**
     * @return true if the current path is in cache directory
     */
    fun isInCacheDirectory() =
        (uiState.value as? FileStorageUiState.Loaded)?.isInCacheDirectory == true

    /**
     * @return current path
     */
    fun getCurrentUriPath() = (uiState.value as? FileStorageUiState.Loaded)?.currentFolder?.uriPath

    /**
     * Get "content://" Uri of the current path if possible
     */
    suspend fun getCurrentPathContentUri(): Uri? = getCurrentUriPath()?.let {
        getContentUriForUriPath(it)
    }

    private suspend fun getContentUriForUriPath(it: UriPath): Uri? {
        val uri = it.toUri()
        when {
            it.isPath() -> File(it.value)
            it.isFile() -> File(uri.path ?: return null)
            uri.scheme == "content" -> return uri
            else -> return null
        }.let { file ->
            return getFileUriUseCase(file, Constants.AUTHORITY_STRING_FILE_PROVIDER).toUri()
        }
    }

    /**
     * Get file path of the current uriPath, if possible
     */
    fun getCurrentFilePath(): String? = getCurrentUriPath()?.let {
        when {
            it.isPath() -> it.value
            it.isFile() -> it.toUri().path
            else -> null
        }
    }


    /**
     * Folder has been picked with the system picker
     */
    fun folderPicked(uriString: String) {
        viewModelScope.launch {
            // This is used in legacy code expecting path instead of content Uri
            getExternalPathByContentUriUseCase(uriString)?.let { path ->
                uiState.update {
                    it.getOrCreateLoaded().copy(folderPickedEvent = triggered(UriPath(path)))
                }
            }
        }
    }

    /**
     * Consume folder picked event
     */
    fun consumeFolderPickedEvent() {
        uiState.update { it.getOrCreateLoaded().copy(folderPickedEvent = consumed()) }
    }

    /**
     * Document clicked
     */
    fun documentClicked(fileDocument: FileDocument) {
        viewModelScope.launch {
            getContentUriForUriPath(fileDocument.uriPath)?.let { contentUri ->
                val type = typeForName(fileDocument.name).type
                uiState.update {
                    it.getOrCreateLoaded()
                        .copy(openWithThirdPartyAppEvent = triggered(Pair(contentUri, type)))
                }
            }
        }
    }

    /**
     * Consume document click event
     */
    fun consumeDocumentClickedEvent() {
        uiState.update { it.getOrCreateLoaded().copy(openWithThirdPartyAppEvent = consumed()) }
    }

    /**
     * Updates current path and other uiState related values
     * @param uriPath the new current [UriPath] folder
     * @param parent the parent of the current folder, null if it's the root folder
     * @param storageType the new value for storage type, if null it is not changed
     * @param isInCacheDirectory the new value for isInCacheDirectory, if null, it is not changed
     * @param highlightFileName if not null, the name of the file to be highlighted
     */
    private suspend fun updateUiState(
        uriPath: UriPath,
        parent: FileDocument? = null,
        storageType: FileStorageType? = null,
        isInCacheDirectory: Boolean? = null,
        highlightFileName: String? = null,
    ) {
        val currentDocument = FileDocument(
            documentEntity = getDocumentEntityUseCase(uriPath) ?: return,
            parent = parent,
        )
        val children = getChildren(currentDocument, highlightFileName)
        uiState.update {
            val loaded = it.getOrCreateLoaded()
            loaded.getOrCreateLoaded().copy(
                currentFolderPath = getPathByDocumentContentUriUseCase(uriPath.value)
                    ?: uriPath.value,
                currentFolder = currentDocument,
                children = children,
                storageType = storageType ?: loaded.storageType,
                isInCacheDirectory = isInCacheDirectory ?: loaded.isInCacheDirectory,
            )
        }
    }

    private fun FileStorageUiState.getOrCreateLoaded() = when (this) {
        is FileStorageUiState.Loaded -> this
        FileStorageUiState.Loading -> FileStorageUiState.Loaded()
    }

    private suspend fun getStorageType(uriPath: UriPath) =
        runCatching {
            getFileStorageTypeNameUseCase(uriPath)
        }.getOrNull()

    /*
     * Update file list for new folder
     */
    private suspend fun getChildren(
        parent: FileDocument,
        highlightFileName: String?,
    ): List<FileDocument> =
        getFilesInDocumentFolderUseCase(parent.uriPath).files
            .mapNotNull { documentEntity ->
                val isHighlighted = highlightFileName == documentEntity.name
                FileDocument(documentEntity, parent, isHighlighted).takeIf { !it.isHidden }
            }.sortedWith(compareByDescending<FileDocument> { it.isFolder }.thenBy { it.name })
}