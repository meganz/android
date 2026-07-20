package mega.privacy.android.feature.payment.presentation.quotawarning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.billing.GetRecommendedSubscriptionUseCase
import mega.privacy.android.domain.usecase.billing.GetSubscriptionsUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferOverQuotaUseCase
import mega.privacy.android.feature.payment.model.mapper.LocalisedSubscriptionMapper
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the quota-warning upsell screen. Exposes the current plan and storage/transfer
 * usage (from [MonitorAccountDetailUseCase]) plus the recommended next-tier plan (from
 * [GetRecommendedSubscriptionUseCase]). The screen selects storage or transfer figures based on
 * the [mega.privacy.android.navigation.payment.QuotaWarningType] it was opened with.
 */
@HiltViewModel
class QuotaWarningUpgradeViewModel @Inject constructor(
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val monitorStorageStateUseCase: MonitorStorageStateUseCase,
    private val monitorTransferOverQuotaUseCase: MonitorTransferOverQuotaUseCase,
    private val getRecommendedSubscriptionUseCase: GetRecommendedSubscriptionUseCase,
    private val getSubscriptionsUseCase: GetSubscriptionsUseCase,
    private val localisedSubscriptionMapper: LocalisedSubscriptionMapper,
) : ViewModel() {

    private val _state = MutableStateFlow(QuotaWarningUpgradeState())

    /**
     * The current UI state.
     */
    val state = _state.asStateFlow()

    init {
        monitorAccountDetail()
        monitorQuotaState()
        loadRecommendedSubscription()
    }

    private fun monitorQuotaState() {
        viewModelScope.launch {
            combine(
                monitorStorageStateUseCase(),
                monitorTransferOverQuotaUseCase(),
            ) { storageState, isTransferOverQuota -> storageState to isTransferOverQuota }
                .catch { Timber.e(it) }
                .collect { (storageState, isTransferOverQuota) ->
                    _state.update {
                        it.copy(
                            storageState = storageState,
                            isTransferOverQuota = isTransferOverQuota,
                        )
                    }
                }
        }
    }

    private fun monitorAccountDetail() {
        viewModelScope.launch {
            monitorAccountDetailUseCase()
                .catch { Timber.e(it) }
                .collect { detail ->
                    _state.update {
                        it.copy(
                            currentPlan = detail.levelDetail?.accountType,
                            storageUsed = detail.storageDetail?.usedStorage,
                            storageTotal = detail.storageDetail?.totalStorage,
                            storageUsedPercentage = detail.storageDetail?.usedPercentage ?: 0,
                            transferUsed = detail.transferDetail?.usedTransfer,
                            transferTotal = detail.transferDetail?.totalTransfer,
                            transferUsedPercentage = detail.transferDetail?.usedTransferPercentage
                                ?: 0,
                        )
                    }
                }
        }
    }

    private fun loadRecommendedSubscription() {
        viewModelScope.launch {
            val recommended = runCatching { getRecommendedSubscriptionUseCase() }.getOrElse {
                Timber.e(it)
                null
            }
            val localised = recommended?.let { buildLocalisedSubscription(it) }
            _state.update {
                it.copy(recommendedSubscription = localised, isLoading = false)
            }
        }
    }

    /**
     * Builds a [mega.privacy.android.feature.payment.model.LocalisedSubscription] for the
     * recommended plan, preferring the full monthly + yearly options so the card can show yearly
     * pricing, and falling back to the recommended subscription alone.
     */
    private suspend fun buildLocalisedSubscription(recommended: Subscription) =
        runCatching { getSubscriptionsUseCase() }.getOrNull()?.let { subscriptions ->
            val monthly = subscriptions.monthlySubscriptions
                .firstOrNull { it.accountType == recommended.accountType }
            val yearly = subscriptions.yearlySubscriptions
                .firstOrNull { it.accountType == recommended.accountType }
            if (monthly != null || yearly != null) {
                localisedSubscriptionMapper(monthlySubscription = monthly, yearlySubscription = yearly)
            } else {
                null
            }
        } ?: localisedSubscriptionMapper(
            monthlySubscription = recommended,
            yearlySubscription = recommended,
        )
}
