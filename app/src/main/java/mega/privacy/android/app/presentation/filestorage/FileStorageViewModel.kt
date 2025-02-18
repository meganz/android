package mega.privacy.android.app.presentation.filestorage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.filestorage.model.FileStorageUiState
import mega.privacy.android.domain.usecase.file.GetFileStorageTypeNameUseCase
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for FileStorageActivity
 */
@HiltViewModel
class FileStorageViewModel @Inject constructor(
    private val getFileStorageTypeNameUseCase: GetFileStorageTypeNameUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileStorageUiState())

    /**
     * UI state
     */
    val uiState = _uiState.asStateFlow()

    /**
     * Update title based on file location
     */
    fun updateTitle(file: File?) {
        viewModelScope.launch {
            runCatching {
                requireNotNull(file)
                getFileStorageTypeNameUseCase(file)
            }.onSuccess { type ->
                _uiState.update {
                    it.copy(storageType = type)
                }
            }
        }
    }
}