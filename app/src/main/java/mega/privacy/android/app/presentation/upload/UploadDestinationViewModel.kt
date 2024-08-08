package mega.privacy.android.app.presentation.upload

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.transfers.GetFileForUploadUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for [UploadDestinationActivity]
 */
@HiltViewModel
class UploadDestinationViewModel @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getFileForUploadUseCase: GetFileForUploadUseCase,
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
        val importableItems = fileUriList.mapNotNull {
            runCatching { getFileForUploadUseCase(it.toString(), false) }
                .onFailure { error -> Timber.e(error) }
                .getOrNull()?.let { file ->
                    ImportUiItem(filePath = file.path, fileName = file.name)
                }
        }
        _uiState.update { it.copy(fileUriList = fileUriList, importUiItems = importableItems) }
    }

    /**
     * Update the text content
     */
    fun updateTextContent(text: String, email: String, subject: String) {
        Timber.d("Text content updated $text $email $subject")
    }
}