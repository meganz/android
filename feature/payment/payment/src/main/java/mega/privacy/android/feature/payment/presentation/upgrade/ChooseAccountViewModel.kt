package mega.privacy.android.feature.payment.presentation.upgrade

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.billing.Pricing
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.billing.GetMonthlySubscriptionsUseCase
import mega.privacy.android.domain.usecase.billing.GetRecommendedSubscriptionUseCase
import mega.privacy.android.domain.usecase.billing.GetYearlySubscriptionsUseCase
import mega.privacy.android.feature.payment.model.ChooseAccountState
import mega.privacy.android.feature.payment.model.mapper.LocalisedSubscriptionMapper
import timber.log.Timber
import javax.inject.Inject

/**
 * Choose account view model
 *
 * @params getPricing use case to get the pricing list of products
 * @param getMonthlySubscriptionsUseCase use case to get the list of monthly subscriptions available in the app
 * @param getYearlySubscriptionsUseCase use case to get the list of yearly subscriptions available in the app
 * @param localisedSubscriptionMapper mapper to map Subscription class to LocalisedSubscription class
 * @param getRecommendedSubscriptionUseCase use case to get the cheapest subscription available in the app
 *
 * @property state The current UI state
 */
@HiltViewModel
class ChooseAccountViewModel @Inject constructor(
    private val getPricing: GetPricing,
    private val getMonthlySubscriptionsUseCase: GetMonthlySubscriptionsUseCase,
    private val getYearlySubscriptionsUseCase: GetYearlySubscriptionsUseCase,
    private val localisedSubscriptionMapper: LocalisedSubscriptionMapper,
    private val getRecommendedSubscriptionUseCase: GetRecommendedSubscriptionUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(ChooseAccountState())
    val state: StateFlow<ChooseAccountState> = _state

    private val isUpgradeAccountFlow =
        savedStateHandle.get<Boolean>(EXTRA_IS_UPGRADE_ACCOUNT) ?: false

    init {
        viewModelScope.launch {
            val monthlySubscriptionsDeferred = async {
                runCatching { getMonthlySubscriptionsUseCase() }.getOrElse {
                    Timber.Forest.e(it)
                    emptyList()
                }
            }
            val yearlySubscriptionsDeferred = async {
                runCatching { getYearlySubscriptionsUseCase() }.getOrElse {
                    Timber.Forest.e(it)
                    emptyList()
                }
            }
            val monthlySubscriptions = monthlySubscriptionsDeferred.await()
            val yearlySubscriptions = yearlySubscriptionsDeferred.await()
            val localisedSubscriptions = monthlySubscriptions.associateWith { monthlySubscription ->
                yearlySubscriptions.firstOrNull { it.accountType == monthlySubscription.accountType }
            }.mapNotNull { (monthlySubscription, yearlySubscription) ->
                yearlySubscription?.let {
                    localisedSubscriptionMapper(
                        monthlySubscription = monthlySubscription,
                        yearlySubscription = yearlySubscription
                    )
                }
            }
            _state.update { it.copy(localisedSubscriptionsList = localisedSubscriptions) }
        }
        viewModelScope.launch {
            val cheapestSubscriptionAvailable =
                runCatching { getRecommendedSubscriptionUseCase() }.getOrElse {
                    Timber.Forest.e(it)
                    null
                }
            _state.update {
                it.copy(
                    cheapestSubscriptionAvailable = cheapestSubscriptionAvailable?.let { cheapestSubscriptionAvailable ->
                        localisedSubscriptionMapper(
                            cheapestSubscriptionAvailable,
                            cheapestSubscriptionAvailable
                        )
                    }
                )
            }
        }
        if (isUpgradeAccountFlow) {
            loadCurrentSubscriptionPlan()
        }
        refreshPricing()
    }

    /**
     * Load current subscription plan information.
     */
    private fun loadCurrentSubscriptionPlan() {
        viewModelScope.launch {
            monitorAccountDetailUseCase()
                .catch { Timber.Forest.e(it) }
                .mapNotNull { it.levelDetail }
                .distinctUntilChanged()
                .collectLatest { levelDetail ->
                    _state.update {
                        it.copy(
                            subscriptionCycle = levelDetail.accountSubscriptionCycle,
                            currentSubscriptionPlan = levelDetail.accountType
                        )
                    }
                }
        }
    }

    /**
     * Asks for pricing if needed.
     */
    fun refreshPricing() {
        viewModelScope.launch {
            val pricing = runCatching { getPricing(false) }.getOrElse {
                Timber.Forest.w(it, "Returning empty pricing as get pricing failed.")
                Pricing(emptyList())
            }
            _state.update {
                it.copy(product = pricing.products)
            }
        }
    }

    companion object {
        /**
         * Extra key to indicate if the activity is for upgrading an account.
         */
        const val EXTRA_IS_UPGRADE_ACCOUNT = "EXTRA_IS_UPGRADE_ACCOUNT"
    }
}