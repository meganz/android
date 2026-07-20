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
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.payment.Subscriptions
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.billing.GetSubscriptionsUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferOverQuotaUseCase
import mega.privacy.android.feature.payment.model.LocalisedSubscription
import mega.privacy.android.feature.payment.model.mapper.LocalisedSubscriptionMapper
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the quota-warning upsell screen. Exposes the current plan, storage/transfer usage,
 * and the recommended plan (the smallest subscription whose storage covers current usage).
 */
@HiltViewModel
class QuotaWarningUpgradeViewModel @Inject constructor(
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val monitorStorageStateUseCase: MonitorStorageStateUseCase,
    private val monitorTransferOverQuotaUseCase: MonitorTransferOverQuotaUseCase,
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
            val subscriptions = runCatching { getSubscriptionsUseCase() }.getOrElse {
                Timber.e(it)
                null
            }
            monitorAccountDetailUseCase()
                .catch { Timber.e(it) }
                .collect { detail ->
                    val levelDetail = detail.levelDetail
                    val storageDetail = detail.storageDetail
                    val storageUsed = storageDetail?.usedStorage
                    _state.update {
                        it.copy(
                            currentPlan = levelDetail?.accountType,
                            subscriptionCycle = levelDetail
                                ?.let(::resolveCurrentPlanCycle)
                                ?: AccountSubscriptionCycle.UNKNOWN,
                            storageUsed = storageUsed,
                            storageTotal = storageDetail?.totalStorage,
                            storageUsedPercentage = storageDetail?.usedPercentage ?: 0,
                            transferUsed = detail.transferDetail?.usedTransfer,
                            transferTotal = detail.transferDetail?.totalTransfer,
                            transferUsedPercentage = detail.transferDetail?.usedTransferPercentage
                                ?: 0,
                            recommendedSubscription = subscriptions
                                ?.let { subs -> recommendedSubscription(storageUsed, subs) },
                            isLoading = it.isLoading && storageDetail == null,
                        )
                    }
                }
        }
    }

    /**
     * Cycle of the current plan's own subscription (matched by id, then by level), falling back to
     * the account-level cycle, which can be wrong when the account holds multiple subscriptions.
     */
    private fun resolveCurrentPlanCycle(levelDetail: AccountLevelDetail): AccountSubscriptionCycle {
        val subscriptions = levelDetail.accountSubscriptionDetailList
        val planSubscriptionId = levelDetail.accountPlanDetail?.subscriptionId
        val matchingSubscription = planSubscriptionId?.let { id ->
            subscriptions.firstOrNull { it.subscriptionId == id }
        } ?: subscriptions.firstOrNull { it.subscriptionLevel == levelDetail.accountType }
        return matchingSubscription?.subscriptionCycle
            ?.takeIf { it != AccountSubscriptionCycle.UNKNOWN }
            ?: levelDetail.accountSubscriptionCycle
    }

    /**
     * Smallest plan whose storage covers current usage (largest if none does), so upgrading clears
     * the over-quota state regardless of the current tier. Plans are merged across the monthly and
     * yearly lists so a plan offered in only one cycle is still considered.
     */
    private fun recommendedSubscription(
        storageUsed: Long?,
        subscriptions: Subscriptions,
    ): LocalisedSubscription? {
        val plansBySize = (subscriptions.monthlySubscriptions + subscriptions.yearlySubscriptions)
            .map { it.accountType }
            .distinct()
            .map { accountType ->
                localisedSubscriptionMapper(
                    monthlySubscription = subscriptions.monthlySubscriptions
                        .firstOrNull { it.accountType == accountType },
                    yearlySubscription = subscriptions.yearlySubscriptions
                        .firstOrNull { it.accountType == accountType },
                )
            }
            .sortedBy { it.storage }
        val usedBytes = storageUsed ?: 0L
        return plansBySize.firstOrNull { it.storage.toLong() * BYTES_IN_GB > usedBytes }
            ?: plansBySize.lastOrNull()
    }

    private companion object {
        private const val BYTES_IN_GB = 1024L * 1024L * 1024L
    }
}
