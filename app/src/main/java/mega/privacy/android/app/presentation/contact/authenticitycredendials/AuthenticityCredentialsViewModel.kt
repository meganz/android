package mega.privacy.android.app.presentation.contact.authenticitycredendials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contact.authenticitycredendials.model.AuthenticityCredentialsState
import mega.privacy.android.domain.usecase.AreCredentialsVerified
import mega.privacy.android.domain.usecase.GetContactCredentials
import mega.privacy.android.domain.usecase.GetMyCredentials
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.ResetCredentials
import mega.privacy.android.domain.usecase.VerifyCredentials
import javax.inject.Inject

/**
 * View model for [AuthenticityCredentialsActivity].
 *
 * @property getContactCredentials  [GetContactCredentials]
 * @property areCredentialsVerified [AreCredentialsVerified]
 * @property getMyCredentials       [GetMyCredentials]
 * @property verifyCredentials      [VerifyCredentials]
 * @property resetCredentials       [ResetCredentials]
 * @property state                  Current view state as [AuthenticityCredentialsState]
 */
@HiltViewModel
class AuthenticityCredentialsViewModel @Inject constructor(
    private val getContactCredentials: GetContactCredentials,
    private val areCredentialsVerified: AreCredentialsVerified,
    private val getMyCredentials: GetMyCredentials,
    private val verifyCredentials: VerifyCredentials,
    private val resetCredentials: ResetCredentials,
    monitorConnectivity: MonitorConnectivity,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthenticityCredentialsState())
    val state: StateFlow<AuthenticityCredentialsState> = _state

    private val isConnected =
        monitorConnectivity().stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            _state.update { it.copy(myAccountCredentials = getMyCredentials()) }
        }
    }

    /**
     * Request data related to a contact.
     *
     * @param userEmail User's email.
     */
    fun requestData(userEmail: String) {
        viewModelScope.launch {
            _state.update { it.copy(contactCredentials = getContactCredentials(userEmail)) }
        }
        viewModelScope.launch {
            _state.update { it.copy(areCredentialsVerified = areCredentialsVerified(userEmail)) }
        }
    }

    /**
     * Resets credentials if already verified, verifies them if not.
     */
    fun actionClicked() {
        when {
            !isConnected.value -> {
                _state.update { it.copy(error = R.string.check_internet_connection_error) }
            }
            state.value.areCredentialsVerified -> {
                resetContactCredentials()
            }
            else -> {
                verifyContactCredentials()
            }
        }
    }

    private fun verifyContactCredentials() =
        viewModelScope.launch {
            state.value.contactCredentials?.let {
                kotlin.runCatching {
                    verifyCredentials(it.email)
                }.onSuccess {
                    _state.update {
                        it.copy(
                            areCredentialsVerified = true,
                            error = R.string.label_verified)
                    }
                }.onFailure {
                    _state.update { it.copy(error = R.string.general_text_error) }
                }
            }
        }

    private fun resetContactCredentials() = viewModelScope.launch {
        state.value.contactCredentials?.let {
            kotlin.runCatching {
                resetCredentials(it.email)
            }.onSuccess {
                _state.update { it.copy(areCredentialsVerified = false) }
            }.onFailure {
                _state.update { it.copy(error = R.string.general_text_error) }
            }
        }
    }
}