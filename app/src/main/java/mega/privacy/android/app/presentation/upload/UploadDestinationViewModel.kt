package mega.privacy.android.app.presentation.upload

import android.net.Uri
import android.webkit.URLUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.transfers.GetFileNameFromContentUri
import mega.privacy.android.icon.pack.R
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for [UploadDestinationActivity]
 */
@HiltViewModel
class UploadDestinationViewModel @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getFileNameFromContentUri: GetFileNameFromContentUri,
    private val fileTypeIconMapper: FileTypeIconMapper,
    private val importFilesErrorMessageMapper: ImportFilesErrorMessageMapper,
    private val importFileErrorMessageMapper: ImportFileErrorMessageMapper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadDestinationUiState())

    /**
     * UI state for [UploadDestinationActivity]
     */
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(AppFeatures.NewUploadDestinationActivity)
            }.onSuccess { result ->
                _uiState.update { it.copy(isNewUploadScreenEnabled = result) }
            }.onFailure { error ->
                Timber.e(error)
                _uiState.update { it.copy(isNewUploadScreenEnabled = false) }
            }
        }
    }

    /**
     * Update the list of [Uri] of the files to upload
     */
    fun updateUri(fileUriList: List<Uri>) = viewModelScope.launch {
        val importableItems = fileUriList.map { uri ->
            val fileName = runCatching { getFileNameFromContentUri(uri.toString()) }.getOrNull().orEmpty()
            ImportUiItem(
                filePath = uri.toString(),
                originalFileName = fileName,
                fileName = fileName,
                fileIcon = fileTypeIconMapper(fileName.substringAfterLast('.')),
                error = importFileErrorMessageMapper(fileName)
            )
        }
        _uiState.update { it.copy(importUiItems = importableItems) }
    }

    /**
     * Update the text content
     *
     * @param text Text of the content
     * @param subject Subject of the content
     */
    fun updateContent(text: String, subject: String) {
        val isUrl = URLUtil.isHttpUrl(text) || URLUtil.isHttpsUrl(text)
        val importTextItem = ImportUiItem(
            filePath = null,
            originalFileName = subject,
            fileName = subject,
            fileIcon = if (isUrl) R.drawable.ic_url_medium_solid else R.drawable.ic_generic_medium_solid,
        )
        _uiState.update { it.copy(importUiItems = listOf(importTextItem)) }
    }

    /**
     * Confirm the import
     */
    fun isValidNameForUpload() : Boolean {
        Timber.d("Import confirmed")
        val emptyNames = uiState.value.importUiItems.count { it.fileName.isBlank() }
        val hasWrongNames = uiState.value.importUiItems.any {
            Constants.NODE_NAME_REGEX.matcher(it.fileName).find()
        }

        val hasError = hasWrongNames || emptyNames > 0
        if (hasError) {
            val message = importFilesErrorMessageMapper(hasWrongNames, emptyNames)
            _uiState.update { it.copy(nameValidationError = triggered(message)) }
        }
        return !hasError
    }

    /**
     * Consume the error message
     */
    fun consumeNameValidationError() {
        _uiState.update { it.copy(nameValidationError = consumed()) }
    }

    /**
     * Edit the file name
     */
    fun editFileName(selectedFile: ImportUiItem?) {
        _uiState.update { it.copy(editableFile = selectedFile) }
    }

    /**
     * Update the file name
     */
    fun updateFileName(fileName: String) {
        uiState.value.editableFile?.let { editableFile ->
            uiState.value.importUiItems.indexOf(editableFile).takeIf { it != -1 }?.let { index ->
                val updatedItem = editableFile.copy(
                    fileName = fileName,
                    error = importFileErrorMessageMapper(fileName)
                )
                val updatedItems = uiState.value.importUiItems.toMutableList().apply {
                    set(index, updatedItem)
                }
                _uiState.update {
                    it.copy(
                        importUiItems = updatedItems,
                        editableFile = updatedItem
                    )
                }
            }
        }
    }
}