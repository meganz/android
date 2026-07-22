package mega.privacy.android.feature.payment.quotawarning

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.formatter.mapper.FormattedSizeMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.AccountPlanDetail
import mega.privacy.android.domain.entity.account.AccountStorageDetail
import mega.privacy.android.domain.entity.account.AccountSubscriptionDetail
import mega.privacy.android.domain.entity.account.AccountTransferDetail
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.domain.entity.payment.Subscriptions
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.billing.GetSubscriptionsUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferOverQuotaUseCase
import mega.privacy.android.feature.payment.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.feature.payment.model.mapper.LocalisedSubscriptionMapper
import mega.privacy.android.feature.payment.presentation.quotawarning.QuotaWarningUpgradeViewModel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QuotaWarningUpgradeViewModelTest {

    private lateinit var underTest: QuotaWarningUpgradeViewModel

    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val monitorStorageStateUseCase = mock<MonitorStorageStateUseCase>()
    private val monitorTransferOverQuotaUseCase = mock<MonitorTransferOverQuotaUseCase>()
    private val getSubscriptionsUseCase = mock<GetSubscriptionsUseCase>()
    private val getCurrentUserEmail = mock<GetCurrentUserEmail>()
    private val localisedPriceCurrencyCodeStringMapper =
        mock<LocalisedPriceCurrencyCodeStringMapper>()
    private val formattedSizeMapper = mock<FormattedSizeMapper>()
    private val localisedSubscriptionMapper =
        LocalisedSubscriptionMapper(localisedPriceCurrencyCodeStringMapper, formattedSizeMapper)

    @BeforeEach
    fun setUp() {
        reset(
            monitorAccountDetailUseCase,
            monitorStorageStateUseCase,
            monitorTransferOverQuotaUseCase,
            getSubscriptionsUseCase,
            getCurrentUserEmail,
            localisedPriceCurrencyCodeStringMapper,
            formattedSizeMapper,
        )
        whenever(monitorAccountDetailUseCase()).thenReturn(emptyFlow())
        whenever(monitorStorageStateUseCase()).thenReturn(emptyFlow())
        whenever(monitorTransferOverQuotaUseCase()).thenReturn(emptyFlow())
        wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(Subscriptions(emptyList(), emptyList()))
        wheneverBlocking { getCurrentUserEmail() }.thenReturn(null)
    }

    private fun initViewModel() {
        underTest = QuotaWarningUpgradeViewModel(
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            monitorStorageStateUseCase = monitorStorageStateUseCase,
            monitorTransferOverQuotaUseCase = monitorTransferOverQuotaUseCase,
            getSubscriptionsUseCase = getSubscriptionsUseCase,
            getCurrentUserEmail = getCurrentUserEmail,
            localisedSubscriptionMapper = localisedSubscriptionMapper,
        )
    }

    @Test
    fun `test that account detail populates current plan and usage`() = runTest {
        val storageDetail = AccountStorageDetail(
            usedCloudDrive = 0,
            usedRubbish = 0,
            usedIncoming = 0,
            totalStorage = 20 * BYTES_IN_GB,
            usedStorage = 19 * BYTES_IN_GB,
        )
        val transferDetail = AccountTransferDetail(
            totalTransfer = 5 * BYTES_IN_GB,
            usedTransfer = 1 * BYTES_IN_GB,
            usedTransferPercentage = 20,
        )
        val levelDetail = mock<AccountLevelDetail> {
            on { accountType }.thenReturn(AccountType.FREE)
        }
        val accountDetail = mock<AccountDetail> {
            on { this.storageDetail }.thenReturn(storageDetail)
            on { this.transferDetail }.thenReturn(transferDetail)
            on { this.levelDetail }.thenReturn(levelDetail)
        }
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.currentPlan).isEqualTo(AccountType.FREE)
            assertThat(state.storageUsed).isEqualTo(19 * BYTES_IN_GB)
            assertThat(state.storageTotal).isEqualTo(20 * BYTES_IN_GB)
            assertThat(state.storageUsedPercentage).isEqualTo(95)
            assertThat(state.transferUsed).isEqualTo(1 * BYTES_IN_GB)
            assertThat(state.transferUsedPercentage).isEqualTo(20)
        }
    }

    @Test
    fun `test that recommended subscription is the smallest plan whose storage exceeds usage`() =
        runTest {
            val essential = subscription(AccountType.ESSENTIAL, storage = 100)
            val proI = subscription(AccountType.PRO_I, storage = 400)
            val proII = subscription(AccountType.PRO_II, storage = 2048)
            val proIYearly = subscription(AccountType.PRO_I, storage = 400)
            val detail = accountDetail(storageUsed = 250 * BYTES_IN_GB)
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
            wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(
                Subscriptions(
                    monthlySubscriptions = listOf(essential, proI, proII),
                    yearlySubscriptions = listOf(proIYearly),
                )
            )
            initViewModel()
            advanceUntilIdle()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.isLoading).isFalse()
                assertThat(state.recommendedSubscription?.accountType).isEqualTo(AccountType.PRO_I)
                assertThat(state.recommendedSubscription?.monthlySubscription).isEqualTo(proI)
                assertThat(state.recommendedSubscription?.yearlySubscription).isEqualTo(proIYearly)
            }
        }

    @Test
    fun `test that largest plan is recommended when usage exceeds every plan`() = runTest {
        val essential = subscription(AccountType.ESSENTIAL, storage = 100)
        val proII = subscription(AccountType.PRO_II, storage = 2048)
        val detail = accountDetail(storageUsed = 5000 * BYTES_IN_GB)
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
        wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(
            Subscriptions(
                monthlySubscriptions = listOf(essential, proII),
                yearlySubscriptions = emptyList(),
            )
        )
        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.recommendedSubscription?.accountType).isEqualTo(AccountType.PRO_II)
        }
    }

    @Test
    fun `test that a plan offered only yearly is still considered`() = runTest {
        val essentialMonthly = subscription(AccountType.ESSENTIAL, storage = 100)
        val proIYearly = subscription(AccountType.PRO_I, storage = 400)
        val detail = accountDetail(storageUsed = 200 * BYTES_IN_GB)
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
        wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(
            Subscriptions(
                monthlySubscriptions = listOf(essentialMonthly),
                yearlySubscriptions = listOf(proIYearly),
            )
        )
        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.recommendedSubscription?.accountType).isEqualTo(AccountType.PRO_I)
            assertThat(state.recommendedSubscription?.monthlySubscription).isNull()
            assertThat(state.recommendedSubscription?.yearlySubscription).isEqualTo(proIYearly)
        }
    }

    @Test
    fun `test that a discounted plan covering usage is preferred when it is cheaper than the default`() =
        runTest {
            val essential = subscription(AccountType.ESSENTIAL, storage = 100, amount = 4.99f)
            val proII = subscription(
                AccountType.PRO_II,
                storage = 2048,
                amount = 9.99f,
                discountedAmountMonthly = 2.99f,
                discountedPercentage = 70,
            )
            val detail = accountDetail(storageUsed = 50 * BYTES_IN_GB)
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
            wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(
                Subscriptions(
                    monthlySubscriptions = listOf(essential, proII),
                    yearlySubscriptions = emptyList(),
                )
            )
            initViewModel()
            advanceUntilIdle()

            underTest.state.test {
                assertThat(awaitItem().recommendedSubscription?.accountType)
                    .isEqualTo(AccountType.PRO_II)
            }
        }

    @Test
    fun `test that a cheaper discounted plan is ignored when it does not cover usage`() = runTest {
        val essential = subscription(
            AccountType.ESSENTIAL,
            storage = 100,
            amount = 4.99f,
            discountedAmountMonthly = 1.99f,
            discountedPercentage = 60,
        )
        val proI = subscription(AccountType.PRO_I, storage = 400, amount = 4.99f)
        val detail = accountDetail(storageUsed = 200 * BYTES_IN_GB)
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
        wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(
            Subscriptions(
                monthlySubscriptions = listOf(essential, proI),
                yearlySubscriptions = emptyList(),
            )
        )
        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            assertThat(awaitItem().recommendedSubscription?.accountType)
                .isEqualTo(AccountType.PRO_I)
        }
    }

    @Test
    fun `test that a discounted plan is ignored when its post-offer price is not cheaper than the default`() =
        runTest {
            val essential = subscription(AccountType.ESSENTIAL, storage = 100, amount = 4.99f)
            val proII = subscription(
                AccountType.PRO_II,
                storage = 2048,
                amount = 9.99f,
                discountedAmountMonthly = 6.99f,
                discountedPercentage = 30,
            )
            val detail = accountDetail(storageUsed = 50 * BYTES_IN_GB)
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
            wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(
                Subscriptions(
                    monthlySubscriptions = listOf(essential, proII),
                    yearlySubscriptions = emptyList(),
                )
            )
            initViewModel()
            advanceUntilIdle()

            underTest.state.test {
                assertThat(awaitItem().recommendedSubscription?.accountType)
                    .isEqualTo(AccountType.ESSENTIAL)
            }
        }

    @Test
    fun `test that recommended subscription is null when no plans are available`() = runTest {
        val detail = accountDetail(storageUsed = 10 * BYTES_IN_GB)
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
        wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(Subscriptions(emptyList(), emptyList()))
        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.recommendedSubscription).isNull()
            assertThat(state.isLoading).isFalse()
        }
    }

    @Test
    fun `test that highest plan is detected when a paid user has no larger plan to upgrade to`() =
        runTest {
            val essential = subscription(AccountType.ESSENTIAL, storage = 100)
            val proIII = subscription(AccountType.PRO_III, storage = 10240)
            val detail = accountDetail(
                storageUsed = 9000 * BYTES_IN_GB,
                accountType = AccountType.PRO_III,
                totalStorage = 10240 * BYTES_IN_GB,
            )
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
            wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(
                Subscriptions(
                    monthlySubscriptions = listOf(essential, proIII),
                    yearlySubscriptions = emptyList(),
                )
            )
            initViewModel()
            advanceUntilIdle()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.isHighestPlan).isTrue()
                assertThat(state.recommendedSubscription).isNull()
            }
        }

    @Test
    fun `test that highest plan is false when a larger plan is available`() = runTest {
        val proII = subscription(AccountType.PRO_II, storage = 2048)
        val proIII = subscription(AccountType.PRO_III, storage = 10240)
        val detail = accountDetail(
            storageUsed = 1000 * BYTES_IN_GB,
            accountType = AccountType.PRO_II,
            totalStorage = 2048 * BYTES_IN_GB,
        )
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
        wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(
            Subscriptions(
                monthlySubscriptions = listOf(proII, proIII),
                yearlySubscriptions = emptyList(),
            )
        )
        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.isHighestPlan).isFalse()
            assertThat(state.recommendedSubscription).isNotNull()
        }
    }

    @Test
    fun `test that highest plan is false for a free user`() = runTest {
        val essential = subscription(AccountType.ESSENTIAL, storage = 100)
        val detail = accountDetail(
            storageUsed = 19 * BYTES_IN_GB,
            accountType = AccountType.FREE,
            totalStorage = 20 * BYTES_IN_GB,
        )
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
        wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(
            Subscriptions(
                monthlySubscriptions = listOf(essential),
                yearlySubscriptions = emptyList(),
            )
        )
        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            assertThat(awaitItem().isHighestPlan).isFalse()
        }
    }

    @Test
    fun `test that email is populated from getCurrentUserEmail`() = runTest {
        wheneverBlocking { getCurrentUserEmail() }.thenReturn("user@mega.co.nz")
        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            assertThat(awaitItem().email).isEqualTo("user@mega.co.nz")
        }
    }

    @Test
    fun `test that loading stays active until storage detail is available`() = runTest {
        val partial = accountDetailWithoutStorage()
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(partial))
        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.isLoading).isTrue()
            assertThat(state.storageUsedPercentage).isEqualTo(0)
        }
    }

    @Test
    fun `test that loading completes once storage detail arrives`() = runTest {
        val partial = accountDetailWithoutStorage()
        val full = accountDetail(storageUsed = 19 * BYTES_IN_GB)
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(partial, full))
        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.storageUsed).isEqualTo(19 * BYTES_IN_GB)
        }
    }

    private fun accountDetail(
        storageUsed: Long,
        accountType: AccountType = AccountType.FREE,
        totalStorage: Long = 0,
    ): AccountDetail {
        val storageDetail = AccountStorageDetail(
            usedCloudDrive = 0,
            usedRubbish = 0,
            usedIncoming = 0,
            totalStorage = totalStorage,
            usedStorage = storageUsed,
        )
        val levelDetail = mock<AccountLevelDetail> {
            on { this.accountType }.thenReturn(accountType)
        }
        return mock {
            on { this.storageDetail }.thenReturn(storageDetail)
            on { this.levelDetail }.thenReturn(levelDetail)
        }
    }

    @Test
    fun `test that subscription cycle is resolved from the plan subscription matched by id`() =
        runTest {
            val detail = accountDetailWithCycle(
                accountType = AccountType.PRO_I,
                accountSubscriptionCycle = AccountSubscriptionCycle.MONTHLY,
                planSubscriptionId = "sub-1",
                subscriptions = listOf(
                    subscriptionDetail("sub-1", AccountSubscriptionCycle.YEARLY, AccountType.PRO_I),
                    subscriptionDetail("sub-2", AccountSubscriptionCycle.MONTHLY, AccountType.PRO_II),
                ),
            )
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
            initViewModel()
            advanceUntilIdle()

            underTest.state.test {
                assertThat(awaitItem().subscriptionCycle)
                    .isEqualTo(AccountSubscriptionCycle.YEARLY)
            }
        }

    @Test
    fun `test that subscription cycle falls back to the matching plan level when no id matches`() =
        runTest {
            val detail = accountDetailWithCycle(
                accountType = AccountType.PRO_I,
                accountSubscriptionCycle = AccountSubscriptionCycle.YEARLY,
                planSubscriptionId = null,
                subscriptions = listOf(
                    subscriptionDetail("sub-1", AccountSubscriptionCycle.MONTHLY, AccountType.PRO_I),
                ),
            )
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
            initViewModel()
            advanceUntilIdle()

            underTest.state.test {
                assertThat(awaitItem().subscriptionCycle)
                    .isEqualTo(AccountSubscriptionCycle.MONTHLY)
            }
        }

    @Test
    fun `test that subscription cycle falls back to the account-level cycle when no subscription matches`() =
        runTest {
            val detail = accountDetailWithCycle(
                accountType = AccountType.PRO_I,
                accountSubscriptionCycle = AccountSubscriptionCycle.YEARLY,
                planSubscriptionId = null,
                subscriptions = emptyList(),
            )
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
            initViewModel()
            advanceUntilIdle()

            underTest.state.test {
                assertThat(awaitItem().subscriptionCycle)
                    .isEqualTo(AccountSubscriptionCycle.YEARLY)
            }
        }

    private fun accountDetailWithCycle(
        accountType: AccountType,
        accountSubscriptionCycle: AccountSubscriptionCycle,
        planSubscriptionId: String?,
        subscriptions: List<AccountSubscriptionDetail>,
    ): AccountDetail {
        val levelDetail = AccountLevelDetail(
            accountType = accountType,
            subscriptionStatus = null,
            subscriptionRenewTime = 0,
            accountSubscriptionCycle = accountSubscriptionCycle,
            proExpirationTime = 0,
            accountPlanDetail = planSubscriptionId?.let {
                AccountPlanDetail(
                    accountType = accountType,
                    isProPlan = true,
                    expirationTime = null,
                    subscriptionId = it,
                    featuresList = emptyList(),
                    isFreeTrial = false,
                )
            },
            accountSubscriptionDetailList = subscriptions,
        )
        val storageDetail = AccountStorageDetail(
            usedCloudDrive = 0,
            usedRubbish = 0,
            usedIncoming = 0,
            totalStorage = 0,
            usedStorage = 0,
        )
        return mock {
            on { this.levelDetail }.thenReturn(levelDetail)
            on { this.storageDetail }.thenReturn(storageDetail)
        }
    }

    private fun subscriptionDetail(
        subscriptionId: String,
        cycle: AccountSubscriptionCycle,
        level: AccountType,
    ) = AccountSubscriptionDetail(
        subscriptionId = subscriptionId,
        subscriptionStatus = null,
        subscriptionCycle = cycle,
        paymentMethodType = null,
        renewalTime = 0,
        subscriptionLevel = level,
        featuresList = emptyList(),
        isFreeTrial = false,
    )

    private fun accountDetailWithoutStorage(): AccountDetail {
        val levelDetail = mock<AccountLevelDetail> {
            on { this.accountType }.thenReturn(AccountType.FREE)
        }
        return mock {
            on { this.storageDetail }.thenReturn(null)
            on { this.levelDetail }.thenReturn(levelDetail)
        }
    }

    private fun subscription(
        accountType: AccountType,
        storage: Int,
        amount: Float = 4.99f,
        discountedAmountMonthly: Float? = null,
        discountedPercentage: Int? = null,
    ) = Subscription(
        sku = "sku_${accountType.name}_$storage",
        accountType = accountType,
        handle = 1L,
        storage = storage,
        transfer = 0,
        amount = CurrencyAmount(amount, Currency("EUR")),
        discountedAmountMonthly = discountedAmountMonthly?.let {
            CurrencyAmount(it, Currency("EUR"))
        },
        discountedPercentage = discountedPercentage,
    )

    companion object {
        private const val BYTES_IN_GB = 1024L * 1024L * 1024L
    }
}
