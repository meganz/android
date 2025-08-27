package mega.privacy.android.app.presentation.transfers.transferoverquota

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.transfers.transferoverquota.model.TransferOverQuotaViewState
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorBandwidthOverQuotaDelayUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.UpdateTransferOverQuotaTimestampUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TransferOverQuotaViewModel @Inject constructor(
    private val hasCredentialsUseCase: HasCredentialsUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val monitorBandwidthOverQuotaDelayUseCase: MonitorBandwidthOverQuotaDelayUseCase,
    private val updateTransferOverQuotaTimestampUseCase: UpdateTransferOverQuotaTimestampUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferOverQuotaViewState())

    /**
     * the state of the view
     */
    internal val uiState = _uiState.asStateFlow()

    init {
        checkLoggedIn()
        checkFreeAccount()
        monitorBandwidthOverQuotaDealy()
    }

    private fun checkLoggedIn() {
        viewModelScope.launch {
            val isLoggedIn = hasCredentialsUseCase()
            _uiState.update { state -> state.copy(isLoggedIn = isLoggedIn) }
        }
    }

    private fun checkFreeAccount() {
        viewModelScope.launch {
            monitorAccountDetailUseCase()
                .catch { Timber.w("Exception monitoring account details: $it") }
                .collectLatest { accountDetail ->
                    val isFreeAccount = (accountDetail.levelDetail?.accountType)
                        ?.let { it == AccountType.FREE } ?: true

                    _uiState.update { state -> state.copy(isFreeAccount = isFreeAccount) }
                }
        }
    }

    private fun monitorBandwidthOverQuotaDealy() {
        viewModelScope.launch {
            monitorBandwidthOverQuotaDelayUseCase().conflate().collect { bandwidthOverQuotaDelay ->
                _uiState.update { state -> state.copy(bandwidthOverQuotaDelay = bandwidthOverQuotaDelay) }
            }
        }
    }

    fun bandwidthOverQuotaDelayConsumed() {
        uiState.value.bandwidthOverQuotaDelay?.inWholeSeconds?.let { bandwidthOverQuotaDelay ->
            updateTransferOverQuotaTimestampUseCase()
            _uiState.update { state -> state.copy(bandwidthOverQuotaDelay = null) }
        }
    }
}