package mega.privacy.android.app.presentation.cancelaccountplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.cancelaccountplan.model.CancelAccountPlanUiState
import mega.privacy.android.app.presentation.cancelaccountplan.model.mapper.CancellationInstructionsTypeMapper
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.billing.GetCurrentPaymentUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Cancel Account Plan view model
 *
 * @param getCurrentPaymentUseCase use case to get the current payment method
 * @param cancellationInstructionsTypeMapper mapper to map cancellation instructions type to payment method
 * @param monitorAccountDetailUseCase use case to monitor account detail
 *
 * @property uiState current UI state
 */
@HiltViewModel
class CancelAccountPlanViewModel @Inject constructor(
    private val getCurrentPaymentUseCase: GetCurrentPaymentUseCase,
    private val cancellationInstructionsTypeMapper: CancellationInstructionsTypeMapper,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(CancelAccountPlanUiState())

    val uiState = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val currentPayment = getCurrentPaymentUseCase()
            val cancellationInstructionsType = currentPayment?.let {
                cancellationInstructionsTypeMapper(
                    it
                )
            }
            currentPayment?.let {
                _state.update {
                    it.copy(
                        cancellationInstructionsType = cancellationInstructionsType
                    )
                }
            }
        }
        viewModelScope.launch {
            monitorAccountDetailUseCase().catch { Timber.e(it) }
                .collectLatest { accountDetail ->
                    val isMonthly =
                        when (accountDetail.levelDetail?.accountSubscriptionCycle) {
                            AccountSubscriptionCycle.MONTHLY -> true
                            AccountSubscriptionCycle.YEARLY -> false
                            else -> null
                        }
                    _state.update {
                        it.copy(
                            isMonthlySubscription = isMonthly
                        )
                    }
                }
        }
    }
}