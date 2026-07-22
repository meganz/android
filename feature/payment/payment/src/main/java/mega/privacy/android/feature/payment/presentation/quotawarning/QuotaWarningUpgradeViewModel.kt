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
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.payment.Subscriptions
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.billing.GetSubscriptionsUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
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
    private val getCurrentUserEmail: GetCurrentUserEmail,
    private val localisedSubscriptionMapper: LocalisedSubscriptionMapper,
) : ViewModel() {

    private val _state = MutableStateFlow(QuotaWarningUpgradeState())

    /**
     * The current UI state.
     */
    val state = _state.asStateFlow()

    init {
        fetchEmail()
        monitorAccountDetail()
        monitorQuotaState()
    }

    private fun fetchEmail() {
        viewModelScope.launch {
            val email = runCatching { getCurrentUserEmail() }.getOrElse {
                Timber.e(it)
                null
            }
            _state.update { it.copy(email = email) }
        }
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
                    val isHighestPlan = isHighestPlan(
                        currentPlan = levelDetail?.accountType,
                        totalStorage = storageDetail?.totalStorage,
                        subscriptions = subscriptions,
                    )
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
                            recommendedSubscription = if (isHighestPlan) {
                                null
                            } else {
                                subscriptions?.let { subs ->
                                    recommendedSubscription(storageUsed, subs)
                                }
                            },
                            isHighestPlan = isHighestPlan,
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
     * True when the user is on a paid plan and no available subscription offers more storage than
     * their current plan, i.e. there is nothing left to upgrade to. In that case the screen shows a
     * "Contact support" action instead of a purchase card.
     */
    private fun isHighestPlan(
        currentPlan: AccountType?,
        totalStorage: Long?,
        subscriptions: Subscriptions?,
    ): Boolean {
        if (currentPlan?.isPaid != true) return false
        val plans = subscriptions
            ?.let { it.monthlySubscriptions + it.yearlySubscriptions }
            ?.takeIf { it.isNotEmpty() }
            ?: return false
        // subscription.storage is expressed in GB, so compare against the plan quota in GB
        val currentStorageGb = (totalStorage ?: 0L) / BYTES_IN_GB
        return plans.none { it.storage.toLong() > currentStorageGb }
    }

    /**
     * Smallest plan whose storage covers current usage (largest if none does), so upgrading clears
     * the over-quota state regardless of the current tier. Plans are merged across the monthly and
     * yearly lists so a plan offered in only one cycle is still considered.
     *
     * Special case: when a discounted plan also covers current usage and its post-offer price
     * undercuts that default recommendation, the discounted plan is recommended instead (the
     * cheapest such offer wins), so the user is shown the better-value deal.
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
        val default = plansBySize.firstOrNull { it.coversUsage(usedBytes) }
            ?: plansBySize.lastOrNull()
            ?: return null
        return cheaperDiscountedAlternative(plansBySize, usedBytes, default) ?: default
    }

    /**
     * The cheapest discounted plan that also covers current usage and whose post-offer price
     * undercuts [default], or null when no such better-value offer exists.
     */
    private fun cheaperDiscountedAlternative(
        plansBySize: List<LocalisedSubscription>,
        usedBytes: Long,
        default: LocalisedSubscription,
    ): LocalisedSubscription? {
        val defaultPrice = default.effectiveMonthlyPrice() ?: return null
        return plansBySize
            .filter { it.hasDiscount && it.coversUsage(usedBytes) }
            .mapNotNull { plan -> plan.effectiveMonthlyPrice()?.let { plan to it } }
            .filter { (_, price) -> price < defaultPrice }
            .minByOrNull { (_, price) -> price }
            ?.first
    }

    /**
     * Whether this plan's storage quota exceeds current usage, so upgrading to it clears the
     * over-quota state. [usedBytes] is in bytes; plan storage is expressed in GB.
     */
    private fun LocalisedSubscription.coversUsage(usedBytes: Long): Boolean =
        storage.toLong() * BYTES_IN_GB > usedBytes

    /**
     * The lowest monthly-equivalent price of the plan across its available billing cycles, using the
     * discounted amount where present. Null when no price is available. Amounts share the account
     * currency and plan prices differ by whole currency units, so the raw Float value is safe to compare.
     */
    private fun LocalisedSubscription.effectiveMonthlyPrice(): Float? = listOfNotNull(
        monthlySubscription?.let { (it.discountedAmountMonthly ?: it.amount).value },
        yearlySubscription?.let { it.discountedAmountMonthly?.value ?: (it.amount.value / 12) },
    ).minOrNull()

    private companion object {
        private const val BYTES_IN_GB = 1024L * 1024L * 1024L
    }
}
