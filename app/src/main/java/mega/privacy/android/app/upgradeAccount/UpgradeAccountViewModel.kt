package mega.privacy.android.app.upgradeAccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.upgradeAccount.model.UpgradeAccountState
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.usecase.GetCurrentSubscriptionPlan
import mega.privacy.android.domain.usecase.GetSubscriptions

/**
 * Upgrade account view model
 *
 * @param getSubscriptions use case to get the list of available subscriptions in the app
 * @param getCurrentSubscriptionPlan use case to get the current subscribed plan
 *
 * @property state The current UI state
 */
class UpgradeAccountViewModel(
    getSubscriptions: GetSubscriptions,
    getCurrentSubscriptionPlan: GetCurrentSubscriptionPlan,
) : ViewModel() {
    private val _state = MutableStateFlow(UpgradeAccountState(listOf(), AccountType.FREE))
    val state: StateFlow<UpgradeAccountState> = _state

    init {
        viewModelScope.launch {
            val subscriptions = getSubscriptions()
            _state.update { it.copy(subscriptionsList = subscriptions) }
        }
        viewModelScope.launch {
            val currentSubscriptionPlan = getCurrentSubscriptionPlan()
            _state.update { it.copy(currentSubscriptionPlan = currentSubscriptionPlan) }
        }
    }
}