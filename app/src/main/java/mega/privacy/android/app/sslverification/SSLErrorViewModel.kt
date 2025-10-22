package mega.privacy.android.app.sslverification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import mega.privacy.android.app.sslverification.model.SSLDialogState
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.apiserver.ResetConnectionUseCase
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * Ssl error view model
 * This VM is responsible for handling the ssl error after it has been emitted, and not for monitoring ssl errors
 *
 * @property getDomainNameUseCase
 * @property resetConnectionUseCase
 * @constructor Create empty S s l error view model
 */
@HiltViewModel
class SSLErrorViewModel @Inject constructor(
    private val getDomainNameUseCase: GetDomainNameUseCase,
    private val resetConnectionUseCase: ResetConnectionUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    val state by lazy {
        flow<SSLDialogState> {
            emit(SSLDialogState.Ready(webUrl = getDomainNameUseCase()))
        }.catch {
            Timber.e(it, "Error fetching domain name for SSL verification")
        }.asUiStateFlow(viewModelScope, SSLDialogState.Loading)
    }

    /**
     * Retry the SSL verification
     */
    fun onRetry() {
        Timber.d("Retrying SSL verification")
        resetConnection(disablePinning = false)
    }

    /**
     * Dismiss the SSL verification dialog
     */
    fun onDismiss() {
        Timber.d("SSL verification dismissed")
        resetConnection(disablePinning = true)
    }

    private fun resetConnection(disablePinning: Boolean) {
        applicationScope.launch {
            try {
                resetConnectionUseCase(disablePinning = disablePinning)
                Timber.d("SSL verification reset connection successful. Disabling pinning = $disablePinning")
            } catch (e: Exception) {
                Timber.e(
                    e,
                    "Failed to reset connection for SSL verification. Disabling pinning = $disablePinning"
                )
            }
        }
    }

}