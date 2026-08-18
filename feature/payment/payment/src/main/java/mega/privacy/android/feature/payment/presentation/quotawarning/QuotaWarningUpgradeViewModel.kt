package mega.privacy.android.feature.payment.presentation.quotawarning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.payment.Subscriptions
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.billing.GetSubscriptionsUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.login.IsUserLoggedInUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferOverQuotaUseCase
import mega.privacy.android.feature.payment.model.LocalisedSubscription
import mega.privacy.android.feature.payment.model.mapper.LocalisedSubscriptionMapper
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the quota-warning upsell screen. Exposes the current plan, storage/transfer usage,
 * and the recommended plan (the smallest subscription covering the quota the backend flags).
 */
@HiltViewModel
class QuotaWarningUpgradeViewModel @Inject constructor(
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val monitorStorageStateUseCase: MonitorStorageStateUseCase,
    private val monitorTransferOverQuotaUseCase: MonitorTransferOverQuotaUseCase,
    private val getSubscriptionsUseCase: GetSubscriptionsUseCase,
    private val getCurrentUserEmail: GetCurrentUserEmail,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val localisedSubscriptionMapper: LocalisedSubscriptionMapper,
    private val getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase,
    private val isUserLoggedInUseCase: IsUserLoggedInUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(QuotaWarningUpgradeState())

    /**
     * The current UI state.
     */
    val state = _state.asStateFlow()

    /** Plans fetch outcome, null until the first attempt finishes. */
    private val subscriptionsResult = MutableStateFlow<Result<Subscriptions>?>(null)

    /** In-flight retry, so a second tap on "Try again" is ignored rather than starting a fetch. */
    private var retryJob: Job? = null

    init {
        monitorConnectivity()
        loadQuotaData()
    }

    /**
     * Anonymous users have no account to read, so which sources the screen is built from can only
     * be decided once the session is known.
     */
    private fun loadQuotaData() {
        viewModelScope.launch {
            val isLoggedIn = runCatching { isUserLoggedInUseCase() }.getOrElse {
                Timber.e(it)
                false
            }
            _state.update { it.copy(isLoggedIn = isLoggedIn) }
            if (isLoggedIn) {
                fetchEmail()
                monitorAccountDetail()
                fetchLatestUsedInfo()
            } else {
                monitorAnonymousRecommendation()
            }
        }
    }

    private fun fetchLatestUsedInfo() {
        viewModelScope.launch {
            runCatching {
                getSpecificAccountDetailUseCase(
                    storage = true,
                    transfer = true,
                    pro = false
                )
            }.onFailure { Timber.e(it) }
        }
    }

    /**
     * Refetches the screen's data, for the error state's "Try again" action. Ignored while an
     * earlier retry is still running, so repeated taps do not start a fetch each.
     */
    fun onRetry() {
        if (retryJob?.isActive == true) return
        retryJob = viewModelScope.launch {
            fetchSubscriptions()
            if (_state.value.isLoggedIn && _state.value.email == null) {
                loadEmail()
            }
        }
    }

    private fun monitorConnectivity() {
        viewModelScope.launch {
            monitorConnectivityUseCase()
                .distinctUntilChanged()
                .catch { Timber.e(it) }
                .collect { isConnected ->
                    _state.update { it.copy(isConnected = isConnected) }
                }
        }
    }

    private fun fetchEmail() {
        viewModelScope.launch { loadEmail() }
    }

    private suspend fun loadEmail() {
        val email = runCatching { getCurrentUserEmail() }.getOrElse {
            Timber.e(it)
            null
        }
        _state.update { it.copy(email = email) }
    }

    private fun monitorAccountDetail() {
        viewModelScope.launch {
            fetchSubscriptions()
            combine(
                monitorAccountDetailUseCase(),
                subscriptionsResult,
                monitorStorageStateUseCase(),
                // once exceeded the warning stands, even if a later emission clears it
                monitorTransferOverQuotaUseCase().runningFold(false) { wasOverQuota, isOverQuota ->
                    wasOverQuota || isOverQuota
                },
            ) { detail, result, storageState, isTransferOverQuota ->
                QuotaInput(detail, result?.getOrNull(), storageState, isTransferOverQuota)
            }
                .catch { Timber.e(it) }
                .collect(::updateAccountDetail)
        }
    }

    /**
     * Anonymous users reach the screen from public links, where there is no account detail to
     * monitor. The recommendation then comes from the price list alone: the smallest plan on offer,
     * which is what a free account with no usage to cover would be recommended too.
     */
    private fun monitorAnonymousRecommendation() {
        viewModelScope.launch {
            fetchSubscriptions()
            subscriptionsResult.filterNotNull().collect { result ->
                val candidates = upgradeCandidates(
                    currentPlan = AccountType.FREE,
                    totalStorage = null,
                    totalTransfer = null,
                    cycle = AccountSubscriptionCycle.UNKNOWN,
                    subscriptions = result.getOrNull(),
                )
                _state.update {
                    it.copy(
                        recommendedSubscription = recommendedSubscription(
                            storageToCover = null,
                            transferToCover = null,
                            cycle = AccountSubscriptionCycle.UNKNOWN,
                            candidates = candidates,
                        ),
                        isLoading = false,
                    )
                }
            }
        }
    }

    /** Inputs of the recommendation, so a change to any of them recomputes it. */
    private data class QuotaInput(
        val detail: AccountDetail,
        val subscriptions: Subscriptions?,
        val storageState: StorageState,
        val isTransferOverQuota: Boolean,
    )

    private suspend fun fetchSubscriptions() {
        val result = runCatching { getSubscriptionsUseCase() }.onFailure { Timber.e(it) }
        subscriptionsResult.update { result }
        _state.update { it.copy(hasLoadError = result.isFailure) }
    }

    private fun updateAccountDetail(input: QuotaInput) {
        val (detail, subscriptions, storageState, isTransferOverQuota) = input
        val levelDetail = detail.levelDetail
        val storageDetail = detail.storageDetail
        val transferDetail = detail.transferDetail
        val storageUsed = storageDetail?.usedStorage
        val transferUsed = transferDetail?.usedTransfer
        val cycle = levelDetail?.let(::resolveCurrentPlanCycle) ?: AccountSubscriptionCycle.UNKNOWN
        val currentPlan = levelDetail?.accountType
        val candidates = currentPlan?.let {
            upgradeCandidates(
                currentPlan = it,
                totalStorage = storageDetail?.totalStorage,
                totalTransfer = transferDetail?.totalTransfer,
                cycle = cycle,
                subscriptions = subscriptions,
            )
        }.orEmpty()
        val isHighestPlan = currentPlan?.isPaid == true &&
                subscriptions.hasPlans() &&
                candidates.isEmpty()
        // only the metric running out has to be covered, so a healthy one cannot push the
        // recommendation to a bigger plan; with neither flagged, both do
        val storageRunningOut = storageState.isRunningOut()
        val recommended = currentPlan?.let {
            recommendedSubscription(
                storageToCover = storageUsed.takeIf { storageRunningOut || !isTransferOverQuota },
                transferToCover = transferUsed.takeIf { isTransferOverQuota || !storageRunningOut },
                cycle = cycle,
                candidates = candidates,
            )
        }
        _state.update {
            it.copy(
                currentPlan = currentPlan,
                subscriptionCycle = cycle,
                storageUsed = storageUsed,
                storageTotal = storageDetail?.totalStorage,
                storageUsedPercentage = storageDetail?.usedPercentage ?: 0,
                transferUsed = transferUsed,
                transferTotal = transferDetail?.totalTransfer,
                transferUsedPercentage = transferDetail?.usedTransferPercentage ?: 0,
                storageState = storageState,
                isTransferOverQuota = isTransferOverQuota,
                recommendedSubscription = recommended,
                isHighestPlan = isHighestPlan,
                isLoading = it.isLoading && (storageDetail == null || currentPlan == null),
            )
        }
    }

    private fun StorageState.isRunningOut(): Boolean =
        this == StorageState.Orange || this == StorageState.Red || this == StorageState.PayWall

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

    private fun Subscriptions?.hasPlans(): Boolean =
        this != null && (monthlySubscriptions.isNotEmpty() || yearlySubscriptions.isNotEmpty())

    /**
     * The plans worth upgrading to, sorted by storage: those that raise at least one quota of the
     * current plan and lower neither. Every plan qualifies while the account is not on a paid plan.
     */
    private fun upgradeCandidates(
        currentPlan: AccountType,
        totalStorage: Long?,
        totalTransfer: Long?,
        cycle: AccountSubscriptionCycle,
        subscriptions: Subscriptions?,
    ): List<LocalisedSubscription> {
        if (subscriptions == null) return emptyList()
        val plans = subscriptions.toLocalisedPlans()
        if (!currentPlan.isPaid) return plans
        // the account totals include bonus quota, which no plan on the price list can match
        val currentPlanOnPriceList = plans.firstOrNull { it.accountType == currentPlan }
        val currentStorageGb = currentPlanOnPriceList?.storage?.toLong()
            ?: ((totalStorage ?: 0L) / BYTES_IN_GB)
        val currentTransferGb = currentPlanOnPriceList?.transferGbFor(cycle)
            ?: ((totalTransfer ?: 0L) / BYTES_IN_GB)
        return plans.filter { plan ->
            val storageGb = plan.storage.toLong()
            val transferGb = plan.transferGbFor(cycle) ?: currentTransferGb
            plan.accountType != currentPlan &&
                    storageGb >= currentStorageGb && transferGb >= currentTransferGb &&
                    (storageGb > currentStorageGb || transferGb > currentTransferGb)
        }
    }

    private fun Subscriptions.toLocalisedPlans(): List<LocalisedSubscription> =
        (monthlySubscriptions + yearlySubscriptions)
            .map { it.accountType }
            .distinct()
            .map { accountType ->
                localisedSubscriptionMapper(
                    monthlySubscription = monthlySubscriptions
                        .firstOrNull { it.accountType == accountType },
                    yearlySubscription = yearlySubscriptions
                        .firstOrNull { it.accountType == accountType },
                )
            }
            .sortedBy { it.storage }

    /**
     * Smallest candidate covering the usage (largest if none does), so upgrading clears the warning.
     *
     * Special case: a discounted plan that also covers the usage and undercuts that default on
     * post-offer price wins instead, so the user is shown the better-value deal.
     */
    private fun recommendedSubscription(
        storageToCover: Long?,
        transferToCover: Long?,
        cycle: AccountSubscriptionCycle,
        candidates: List<LocalisedSubscription>,
    ): LocalisedSubscription? {
        val default = candidates
            .firstOrNull { it.coversUsage(storageToCover, transferToCover, cycle) }
            ?: candidates.lastOrNull()
            ?: return null
        return cheaperDiscountedAlternative(
            candidates = candidates,
            storageToCover = storageToCover,
            transferToCover = transferToCover,
            cycle = cycle,
            default = default,
        ) ?: default
    }

    /**
     * The cheapest discounted plan that also covers current usage and whose post-offer price
     * undercuts [default], or null when no such better-value offer exists.
     */
    private fun cheaperDiscountedAlternative(
        candidates: List<LocalisedSubscription>,
        storageToCover: Long?,
        transferToCover: Long?,
        cycle: AccountSubscriptionCycle,
        default: LocalisedSubscription,
    ): LocalisedSubscription? {
        val defaultPrice = default.effectiveMonthlyPrice() ?: return null
        return candidates
            .filter { it.hasDiscount && it.coversUsage(storageToCover, transferToCover, cycle) }
            .mapNotNull { plan -> plan.effectiveMonthlyPrice()?.let { plan to it } }
            .filter { (_, price) -> price < defaultPrice }
            .minByOrNull { (_, price) -> price }
            ?.first
    }

    /**
     * Whether this plan's quotas exceed the usage to cover. Usage is in bytes, plan quotas in GB. A
     * null usage is no constraint: that metric is not running out, or never loaded.
     */
    private fun LocalisedSubscription.coversUsage(
        storageToCover: Long?,
        transferToCover: Long?,
        cycle: AccountSubscriptionCycle,
    ): Boolean = (storageToCover == null || storage.toLong() * BYTES_IN_GB > storageToCover) &&
            coversTransferUsage(transferToCover, cycle)

    private fun LocalisedSubscription.coversTransferUsage(
        transferToCover: Long?,
        cycle: AccountSubscriptionCycle,
    ): Boolean {
        val used = transferToCover ?: return true
        val quotaGb = transferGbFor(cycle) ?: return true
        return quotaGb * BYTES_IN_GB > used
    }

    /**
     * Transfer quota in GB the SDK reports for [cycle], so one comparison never mixes cycles. Null
     * when the plan has no option for it — callers treat that as no obstacle rather than converting
     * a figure the price list never gave.
     */
    private fun LocalisedSubscription.transferGbFor(cycle: AccountSubscriptionCycle): Long? {
        val isMonthly = cycle == AccountSubscriptionCycle.MONTHLY
        return getSubscription(isMonthly)?.transfer?.toLong()?.takeIf { it > 0 }
    }

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
