package mega.privacy.android.feature.clouddrive.presentation.shares.links

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.filelink.DecryptPasswordProtectedLinkUseCase
import mega.privacy.android.domain.usecase.link.GetDecodedUrlRegexPatternTypeUseCase
import javax.inject.Inject

/**
 * Open password link view model
 */
@HiltViewModel
class OpenPasswordLinkViewModel @Inject constructor(
    private val decryptPasswordProtectedLinkUseCase: DecryptPasswordProtectedLinkUseCase,
    private val getDecodedUrlRegexPatternTypeUseCase: GetDecodedUrlRegexPatternTypeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OpenPasswordLinkUiState())

    /**
     * UI state flow
     */
    val uiState = _uiState.asStateFlow()


    /**
     * Decrypt Password Protected Link
     */
    fun decryptPasswordProtectedLink(passwordProtectedLink: String, password: String) {
        viewModelScope.launch {
            val decryptedLinkEvent = runCatching {
                decryptPasswordProtectedLinkUseCase(
                    passwordProtectedLink,
                    password
                )?.let { decryptedLink ->
                    when (getDecodedUrlRegexPatternTypeUseCase(decryptedLink)) {
                        RegexPatternType.FILE_LINK -> DecryptedLink.FileLink(decryptedLink)
                        RegexPatternType.FOLDER_LINK -> DecryptedLink.FolderLink(decryptedLink)
                        else -> null
                    }
                }
            }.getOrNull()
            if (decryptedLinkEvent == null) {
                _uiState.update {
                    it.copy(errorMessage = true)
                }
            } else {
                _uiState.update {
                    it.copy(decryptedLinkEvent = triggered(decryptedLinkEvent))
                }
            }
        }
    }

    fun consumeDecryptedLinkEvent() {
        _uiState.update {
            it.copy(decryptedLinkEvent = consumed())
        }
    }

    fun resetError() {
        if (_uiState.value.errorMessage) {
            _uiState.update {
                it.copy(errorMessage = false)
            }
        }
    }
}