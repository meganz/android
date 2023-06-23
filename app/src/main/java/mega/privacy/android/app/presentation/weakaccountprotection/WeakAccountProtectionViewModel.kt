package mega.privacy.android.app.presentation.weakaccountprotection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.domain.usecase.login.LogoutUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Weak account protection view model
 *
 * @param logoutUseCase
 */
@HiltViewModel
class WeakAccountProtectionViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    /**
     * Logout
     *
     * logs out the user from mega application and navigates to login activity
     * logic is handled at [MegaChatRequestHandler] onRequestFinished callback
     */
    fun logout() = viewModelScope.launch {
        MegaApplication.isLoggingOut = true
        runCatching {
            logoutUseCase()
        }.onFailure {
            MegaApplication.isLoggingOut = false
            Timber.d("Error on logout $it")
        }
    }
}