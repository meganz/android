package mega.privacy.android.app.presentation.cancelaccountplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.cancelaccountplan.model.CancelAccountPlanUiState
import mega.privacy.android.app.presentation.cancelaccountplan.model.UICancellationSurveyAnswer
import mega.privacy.android.app.presentation.cancelaccountplan.model.mapper.CancellationInstructionsTypeMapper
import mega.privacy.android.app.presentation.myaccount.mapper.AccountNameMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.FormattedSizeMapper
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SubscriptionOption
import mega.privacy.android.domain.usecase.account.CancelSubscriptionWithSurveyAnswersUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.billing.GetAppSubscriptionOptionsUseCase
import mega.privacy.android.domain.usecase.billing.GetCurrentPaymentUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Cancel Account Plan view model
 *
 * @param accountNameMapper mapper to map account type to account name
 * @param formattedSizeMapper mapper to format size
 * @param getCurrentPaymentUseCase use case to get the current payment method
 * @param cancellationInstructionsTypeMapper mapper to map cancellation instructions type to payment method
 * @param getAppSubscriptionOptionsUseCase use case to get the app subscription options
 * @param monitorAccountDetailUseCase use case to monitor account detail
 * @param cancelSubscriptionWithSurveyAnswersUseCase use case to send Cancellation survey answers to API
 *
 * @property uiState current UI state
 */
@HiltViewModel
class CancelAccountPlanViewModel @Inject constructor(
    private val accountNameMapper: AccountNameMapper,
    private val formattedSizeMapper: FormattedSizeMapper,
    private val getCurrentPaymentUseCase: GetCurrentPaymentUseCase,
    private val cancellationInstructionsTypeMapper: CancellationInstructionsTypeMapper,
    private val getAppSubscriptionOptionsUseCase: GetAppSubscriptionOptionsUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val cancelSubscriptionWithSurveyAnswersUseCase: CancelSubscriptionWithSurveyAnswersUseCase,
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
            runCatching {
                monitorAccountDetailUseCase()
                    .collectLatest { accountDetail ->
                        val isMonthly =
                            when (accountDetail.levelDetail?.accountSubscriptionCycle) {
                                AccountSubscriptionCycle.MONTHLY -> true
                                AccountSubscriptionCycle.YEARLY -> false
                                else -> null
                            }
                        accountDetail.levelDetail?.accountType?.let { accountType ->
                            val planRewindDaysQuota = if (accountType == AccountType.PRO_LITE) {
                                "90"
                            } else {
                                "180"
                            }

                            val planDetails = getActiveAccountPlanDetails(
                                accountType = accountType,
                                isMonthly = isMonthly ?: false
                            )
                            val planStorage = formattedSizeMapper(size = planDetails?.storage ?: 0)
                            val planTransfer =
                                formattedSizeMapper(size = planDetails?.transfer ?: 0)

                            _state.update {
                                it.copy(
                                    isMonthlySubscription = isMonthly,
                                    formattedPlanStorage = planStorage,
                                    formattedPlanTransfer = planTransfer,
                                    accountType = accountType,
                                    rewindDaysQuota = planRewindDaysQuota,
                                    accountNameRes = accountNameMapper(accountType),
                                    isLoading = false,
                                    cancellationReasons = getShuffleCancellationReasons(it.cancellationReasons)
                                )
                            }
                        }
                        accountDetail.levelDetail?.accountPlanDetail?.subscriptionId?.let { subscriptionId ->
                            _state.update {
                                it.copy(subscriptionId = subscriptionId)
                            }
                        }
                    }
            }.onFailure {
                Timber.e(it)
            }
        }
    }


    /**
     * Get the cancellation reasons list in shuffled order
     */
    private fun getShuffleCancellationReasons(reasons: List<UICancellationSurveyAnswer>): List<UICancellationSurveyAnswer> {
        val lastReason = reasons.last()
        val shuffledReasons = reasons.subList(0, reasons.size - 1).shuffled()
        return shuffledReasons + lastReason
    }


    /**
     * Get the active account plan details
     * @param accountType the account type
     * @param isMonthly true if the account is monthly
     */
    private suspend fun getActiveAccountPlanDetails(
        accountType: AccountType,
        isMonthly: Boolean,
    ): SubscriptionOption? =
        getAppSubscriptionOptionsUseCase(if (isMonthly) 1 else 12).firstOrNull { plan ->
            plan.accountType == accountType
        }

    /**
     * Cancel the subscription and send reason to API
     * @param reason the reason for cancellation
     * @param canContact whether the user can be contacted
     */
    fun cancelSubscription(reason: String, canContact: Int) {
        val subscriptionId = uiState.value.subscriptionId
        viewModelScope.launch {
            runCatching {
                subscriptionId?.let {
                    cancelSubscriptionWithSurveyAnswersUseCase(
                        reason,
                        subscriptionId,
                        canContact
                    )
                }
            }.onFailure {
                Timber.e(it, "Failed to cancel subscription and send reason to API")
            }
        }
    }
}