package mega.privacy.android.app.upgradeAccount

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.upgradeAccount.model.LocalisedSubscription
import mega.privacy.android.app.upgradeAccount.model.UpgradeAccountState
import mega.privacy.android.app.upgradeAccount.model.UpgradePayment
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedSubscriptionMapper
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.Skus
import mega.privacy.android.domain.usecase.billing.GetCurrentPaymentUseCase
import mega.privacy.android.domain.usecase.account.GetCurrentSubscriptionPlanUseCase
import mega.privacy.android.domain.usecase.billing.GetMonthlySubscriptionsUseCase
import mega.privacy.android.domain.usecase.billing.GetYearlySubscriptionsUseCase
import mega.privacy.android.domain.usecase.billing.IsBillingAvailableUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Upgrade account view model
 *
 * @param getMonthlySubscriptionsUseCase use case to get the list of monthly subscriptions available in the app
 * @param getYearlySubscriptionsUseCase use case to get the list of yearly subscriptions available in the app
 * @param getCurrentSubscriptionPlanUseCase use case to get the current subscribed plan
 * @param getCurrentPaymentUseCase use case to get the current payment option
 * @param isBillingAvailableUseCase use case to check if billing is available
 * @param localisedSubscriptionMapper mapper to map Subscription class to LocalisedSubscription class
 *
 * @property state The current UI state
 */
@HiltViewModel
class UpgradeAccountViewModel @Inject constructor(
    private val getMonthlySubscriptionsUseCase: GetMonthlySubscriptionsUseCase,
    private val getYearlySubscriptionsUseCase: GetYearlySubscriptionsUseCase,
    private val getCurrentSubscriptionPlanUseCase: GetCurrentSubscriptionPlanUseCase,
    private val getCurrentPaymentUseCase: GetCurrentPaymentUseCase,
    private val isBillingAvailableUseCase: IsBillingAvailableUseCase,
    private val localisedSubscriptionMapper: LocalisedSubscriptionMapper,
) : ViewModel() {
    private val _state = MutableStateFlow(
        UpgradeAccountState(
            listOf(),
            AccountType.FREE,
            showBillingWarning = false,
            currentPayment = UpgradePayment()
        )
    )
    val state: StateFlow<UpgradeAccountState> = _state

    private val upgradeClick = SingleLiveEvent<Int>()

    fun onUpgradeClick(): LiveData<Int> = upgradeClick

    init {
        viewModelScope.launch {
            var monthlySubscriptions = listOf<Subscription>()
            var yearlySubscriptions = listOf<Subscription>()
            val localisedSubscriptions = mutableListOf<LocalisedSubscription>()
            runCatching { getMonthlySubscriptionsUseCase() }
                .onSuccess { subscriptions ->
                    monthlySubscriptions = subscriptions
                }.onFailure {
                    Timber.e(it)
                }
            runCatching { getYearlySubscriptionsUseCase() }
                .onSuccess { subscriptions ->
                    yearlySubscriptions = subscriptions
                }.onFailure {
                    Timber.e(it)
                }
            monthlySubscriptions.map { monthlySubscription ->
                val yearlySubscription =
                    yearlySubscriptions.first { it.accountType == monthlySubscription.accountType }
                localisedSubscriptions += localisedSubscriptionMapper(
                    monthlySubscription = monthlySubscription,
                    yearlySubscription = yearlySubscription
                )
            }
            _state.update { it.copy(localisedSubscriptionsList = localisedSubscriptions) }
        }
        viewModelScope.launch {
            val currentSubscriptionPlan = getCurrentSubscriptionPlanUseCase()
            _state.update { it.copy(currentSubscriptionPlan = currentSubscriptionPlan) }
        }
        viewModelScope.launch {
            val currentPayment = getCurrentPaymentUseCase()
            currentPayment?.let {
                _state.update {
                    it.copy(
                        showBuyNewSubscriptionDialog = false,
                        currentPayment = UpgradePayment(
                            upgradeType = Constants.INVALID_VALUE,
                            currentPayment = currentPayment
                        )
                    )
                }
            }
        }
    }

    /**
     * Check the current payment
     * @param upgradeType upgrade type
     */
    fun currentPaymentCheck(upgradeType: Int) {
        viewModelScope.launch {
            val currentPayment = getCurrentPaymentUseCase()
            currentPayment?.let {
                _state.update {
                    it.copy(
                        showBuyNewSubscriptionDialog = upgradeType != Constants.INVALID_VALUE,
                        currentPayment = UpgradePayment(
                            upgradeType = upgradeType,
                            currentPayment = currentPayment
                        )
                    )
                }
            } ?: run {
                upgradeClick.value = upgradeType
            }
        }
    }

    fun isBillingAvailable(): Boolean = isBillingAvailableUseCase.invoke()

    fun setBillingWarningVisibility(isVisible: Boolean) {
        _state.update { it.copy(showBillingWarning = isVisible) }
    }

    fun setShowBuyNewSubscriptionDialog(showBuyNewSubscriptionDialog: Boolean) {
        _state.update { it.copy(showBuyNewSubscriptionDialog = showBuyNewSubscriptionDialog) }
    }

    /**
     * On selecting monthly or yearly plan
     *
     * @param isMonthly
     */
    fun onSelectingMonthlyPlan(isMonthly: Boolean) =
        _state.update {
            it.copy(isMonthlySelected = isMonthly)
        }

    /**
     * On selecting monthly or yearly plan
     *
     * @param chosenPlan
     */
    fun onSelectingPlanType(chosenPlan: AccountType) =
        _state.update {
            it.copy(chosenPlan = chosenPlan)
        }

    /**
     * Get product id for payment
     *
     */
    fun getProductId(isMonthly: Boolean, upgradeType: Int): String {
        val skus = getSkus(upgradeType)
        return if (isMonthly) skus.first else skus.second
    }

    private fun getSkus(upgradeType: Int) = when (upgradeType) {
        Constants.PRO_I -> Skus.SKU_PRO_I_MONTH to Skus.SKU_PRO_I_YEAR
        Constants.PRO_II -> Skus.SKU_PRO_II_MONTH to Skus.SKU_PRO_II_YEAR
        Constants.PRO_III -> Skus.SKU_PRO_III_MONTH to Skus.SKU_PRO_III_YEAR
        Constants.PRO_LITE -> Skus.SKU_PRO_LITE_MONTH to Skus.SKU_PRO_LITE_YEAR
        else -> "" to ""
    }
}