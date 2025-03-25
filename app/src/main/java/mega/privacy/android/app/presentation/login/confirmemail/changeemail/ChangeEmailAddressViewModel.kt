package mega.privacy.android.app.presentation.login.confirmemail.changeemail

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.exception.account.AccountExistedException
import mega.privacy.android.domain.usecase.IsEmailValidUseCase
import mega.privacy.android.domain.usecase.login.confirmemail.ResendSignUpLinkUseCase
import javax.inject.Inject

@HiltViewModel
internal class ChangeEmailAddressViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val isEmailValidUseCase: IsEmailValidUseCase,
    private val resendSignUpLinkUseCase: ResendSignUpLinkUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangeEmailAddressUIState())

    /**
     * UI State for [ChangeEmailAddressScreen]
     * Flow of [ChangeEmailAddressUIState]
     */
    val uiState = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(email = savedStateHandle[EMAIL] ?: "")
        }
    }

    fun resetGeneralErrorEvent() = viewModelScope.launch {
        _uiState.update { it.copy(generalErrorEvent = consumed) }
    }

    fun resetChangeEmailAddressSuccessEvent() = viewModelScope.launch {
        _uiState.update { it.copy(changeEmailAddressSuccessEvent = consumed) }
    }

    /**
     * save email state to savedStateHandle
     */
    fun onEmailInputChanged(email: String?) = viewModelScope.launch {
        savedStateHandle[EMAIL] = email
        _uiState.update { it.copy(isEmailValid = null) }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun validateEmail(email: String?): Boolean {
        val isEmailValid = runCatching {
            isEmailValidUseCase(email.orEmpty())
        }.getOrElse { false }
        _uiState.update { it.copy(isEmailValid = isEmailValid) }
        return isEmailValid
    }

    fun changeEmailAddress() = viewModelScope.launch {
        val email = savedStateHandle[EMAIL] ?: ""
        val fullName = savedStateHandle[FULL_NAME] ?: ""
        if (validateEmail(email).not()) {
            return@launch
        }
        _uiState.update {
            it.copy(isLoading = true)
        }
        runCatching {
            resendSignUpLinkUseCase(email = email, fullName = fullName)
        }.onSuccess {
            _uiState.update {
                it.copy(changeEmailAddressSuccessEvent = triggered, isLoading = false)
            }
        }.onFailure { e ->
            if (e is AccountExistedException) {
                _uiState.update {
                    it.copy(accountExistEvent = triggered, isLoading = false)
                }
            } else {
                _uiState.update {
                    it.copy(generalErrorEvent = triggered, isLoading = false)
                }
            }
        }
    }

    fun resetAccountExistEvent() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(accountExistEvent = consumed)
            }
        }
    }
}
