package mega.privacy.android.app.upgradeAccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.upgradeAccount.model.ChooseAccountState
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedSubscriptionMapper
import mega.privacy.android.domain.entity.billing.Pricing
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.billing.GetCheapestSubscriptionUseCase
import mega.privacy.android.domain.usecase.billing.GetMonthlySubscriptionsUseCase
import mega.privacy.android.domain.usecase.billing.GetYearlySubscriptionsUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ChooseAccountViewModel @Inject constructor(
    private val getPricing: GetPricing,
    private val getMonthlySubscriptionsUseCase: GetMonthlySubscriptionsUseCase,
    private val getYearlySubscriptionsUseCase: GetYearlySubscriptionsUseCase,
    private val localisedSubscriptionMapper: LocalisedSubscriptionMapper,
    private val getCheapestSubscriptionUseCase: GetCheapestSubscriptionUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ChooseAccountState())
    val state: StateFlow<ChooseAccountState> = _state

    init {
        viewModelScope.launch {
            val monthlySubscriptions = runCatching { getMonthlySubscriptionsUseCase() }.getOrElse {
                Timber.e(it)
                emptyList()
            }
            val yearlySubscriptions = runCatching { getYearlySubscriptionsUseCase() }.getOrElse {
                Timber.e(it)
                emptyList()
            }
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
                runCatching { getCheapestSubscriptionUseCase() }.getOrElse {
                    Timber.e(it)
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
        viewModelScope.launch {
            val getFeatureFlag = getFeatureFlagValueUseCase(AppFeatures.ChooseAccountScreenVariantA)
            _state.update {
                it.copy(enableVariantAUI = getFeatureFlag)
            }
        }
        refreshPricing()
    }

    /**
     * Asks for pricing if needed.
     */
    fun refreshPricing() {
        viewModelScope.launch {
            val pricing = runCatching { getPricing(false) }.getOrElse {
                Timber.w("Returning empty pricing as get pricing failed.", it)
                Pricing(emptyList())
            }
            _state.update {
                it.copy(product = pricing.products)
            }
        }
    }
}