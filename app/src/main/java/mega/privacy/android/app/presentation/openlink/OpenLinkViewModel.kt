package mega.privacy.android.app.presentation.openlink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.psa.PsaManager
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.ClearPsa
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.ClearEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.login.LocalLogoutAppUseCase
import mega.privacy.android.domain.usecase.login.LogoutUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * open link view model
 */
@HiltViewModel
class OpenLinkViewModel @Inject constructor(
    private val localLogoutAppUseCase: LocalLogoutAppUseCase,
    private val clearEphemeralCredentialsUseCase: ClearEphemeralCredentialsUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
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
    private fun logoutConfirmed() {
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

    /**
     * Logout
     *
     * logs out the user from mega application and navigates to login activity
     * logic is handled at [MegaChatRequestHandler] onRequestFinished callback
     */
    fun logout() = viewModelScope.launch {
        runCatching {
            logoutUseCase()
        }.onSuccess {
            logoutConfirmed()
        }.onFailure {
            Timber.d("Error on logout $it")
        }
    }

    /**
     * Check if given feature flag is enabled or not
     */
    fun isFeatureEnabled(feature: Feature) = state.value.enabledFeatureFlags?.contains(feature)

}