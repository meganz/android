package mega.privacy.android.app.presentation.verifytwofactor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.domain.usecase.login.LogoutUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Verify two factor view model
 *
 * @param logoutUseCase
 */
class VerifyTwoFactorViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    /**
     * Logout
     *
     * logs out the user from mega application and navigates to login activity
     * logic is handled at [MegaChatRequestHandler] onRequestFinished callback
     */
    fun logout() = viewModelScope.launch {
        runCatching {
            logoutUseCase()
        }.onFailure {
            Timber.d("Error on logout $it")
        }
    }
}