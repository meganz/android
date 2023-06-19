package mega.privacy.android.app.presentation.openlink

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.psa.PsaManager
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.ClearPsa
import mega.privacy.android.domain.usecase.login.ClearEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.login.LocalLogoutAppUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * open link view model
 */
@HiltViewModel
class OpenLinkViewModel @Inject constructor(
    private val localLogoutAppUseCase: LocalLogoutAppUseCase,
    private val clearEphemeralCredentialsUseCase: ClearEphemeralCredentialsUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {


    private val _state = MutableStateFlow(OpenLinkState())

    /**
     * UI State OpenLinkActivity
     * Flow of [OpenLinkState]
     */
    val state = _state.asStateFlow()

    /**
     * logout confirmed
     * once logout is confirmed methods clears user related app data
     */
    fun logoutConfirmed() {
        Timber.d("END logout sdk request - wait chat logout")
        MegaApplication.urlConfirmationLink?.let {
            Timber.d("Confirmation link - show confirmation screen")
            applicationScope.launch {
                runCatching {
                    clearEphemeralCredentialsUseCase()
                    localLogoutAppUseCase(ClearPsa { PsaManager::clearPsa })
                }.onSuccess {
                    MegaApplication.urlConfirmationLink = null
                    _state.update { it.copy(isLoggedOut = true) }
                }.onFailure {
                    Timber.d("Logout confirmation failed : ${it.message}")
                }
            }
        }
    }

}