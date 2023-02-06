package mega.privacy.android.app.upgradeAccount

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.upgradeAccount.model.UpgradeAccountState
import mega.privacy.android.app.upgradeAccount.model.UpgradePayment
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.usecase.GetCurrentPayment
import mega.privacy.android.domain.usecase.GetCurrentSubscriptionPlan
import mega.privacy.android.domain.usecase.GetSubscriptions
import mega.privacy.android.domain.usecase.billing.IsBillingAvailable
import javax.inject.Inject

/**
 * Upgrade account view model
 *
 * @param getSubscriptions use case to get the list of available subscriptions in the app
 * @param getCurrentSubscriptionPlan use case to get the current subscribed plan
 *
 * @property state The current UI state
 */
@HiltViewModel
class UpgradeAccountViewModel @Inject constructor(
    private val getSubscriptions: GetSubscriptions,
    private val getCurrentSubscriptionPlan: GetCurrentSubscriptionPlan,
    private val getCurrentPayment: GetCurrentPayment,
    private val isBillingAvailable: IsBillingAvailable,
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
            val subscriptions = getSubscriptions()
            _state.update { it.copy(subscriptionsList = subscriptions) }
        }
        viewModelScope.launch {
            val currentSubscriptionPlan = getCurrentSubscriptionPlan()
            _state.update { it.copy(currentSubscriptionPlan = currentSubscriptionPlan) }
        }
        viewModelScope.launch {
            val currentPayment = getCurrentPayment()
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
            val currentPayment = getCurrentPayment()
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

    fun isBillingAvailable(): Boolean = isBillingAvailable.invoke()

    fun setBillingWarningVisibility(isVisible: Boolean) {
        _state.update { it.copy(showBillingWarning = isVisible) }
    }

    fun setShowBuyNewSubscriptionDialog(showBuyNewSubscriptionDialog: Boolean) {
        _state.update { it.copy(showBuyNewSubscriptionDialog = showBuyNewSubscriptionDialog) }
    }
}