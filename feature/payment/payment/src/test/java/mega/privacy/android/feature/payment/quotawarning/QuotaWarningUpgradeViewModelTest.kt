package mega.privacy.android.feature.payment.quotawarning

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
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
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.AccountPlanDetail
import mega.privacy.android.domain.entity.account.AccountStorageDetail
import mega.privacy.android.domain.entity.account.AccountSubscriptionDetail
import mega.privacy.android.domain.entity.account.AccountTransferDetail
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.domain.entity.payment.Subscriptions
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.billing.GetSubscriptionsUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.login.IsUserLoggedInUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferOverQuotaUseCase
import mega.privacy.android.feature.payment.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.feature.payment.model.mapper.LocalisedSubscriptionMapper
import mega.privacy.android.feature.payment.presentation.quotawarning.QuotaMetric
import mega.privacy.android.feature.payment.presentation.quotawarning.QuotaWarningUpgradeViewModel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.verifyNoInteractions
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
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val getSpecificAccountDetailUseCase = mock<GetSpecificAccountDetailUseCase>()
    private val isUserLoggedInUseCase = mock<IsUserLoggedInUseCase>()
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
            monitorConnectivityUseCase,
            getSpecificAccountDetailUseCase,
            isUserLoggedInUseCase,
            localisedPriceCurrencyCodeStringMapper,
            formattedSizeMapper,
        )
        wheneverBlocking { isUserLoggedInUseCase() }.thenReturn(true)
        whenever(monitorAccountDetailUseCase()).thenReturn(emptyFlow())
        // both monitors emit their current value on collection
        whenever(monitorStorageStateUseCase()).thenReturn(flowOf(StorageState.Unknown))
        whenever(monitorTransferOverQuotaUseCase()).thenReturn(flowOf(false))
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(true))
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
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            getSpecificAccountDetailUseCase = getSpecificAccountDetailUseCase,
            isUserLoggedInUseCase = isUserLoggedInUseCase,
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
    fun `test that init recommends the smallest plan when the user is not logged in`() = runTest {
        wheneverBlocking { isUserLoggedInUseCase() }.thenReturn(false)
        wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(
            Subscriptions(
                monthlySubscriptions = listOf(
                    subscription(AccountType.ESSENTIAL, storage = 200, transfer = 2400),
                    subscription(AccountType.PRO_LITE, storage = 750, transfer = 12288),
                ),
                yearlySubscriptions = emptyList(),
            )
        )

        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.isLoggedIn).isFalse()
            assertThat(state.isLoading).isFalse()
            assertThat(state.currentPlan).isNull()
            assertThat(state.showQuotaDetails(QuotaMetric.Storage)).isFalse()
            assertThat(state.showQuotaDetails(QuotaMetric.Transfer)).isFalse()
            assertThat(state.recommendedSubscription?.accountType)
                .isEqualTo(AccountType.ESSENTIAL)
        }
    }

    @Test
    fun `test that init does not read account data when the user is not logged in`() = runTest {
        wheneverBlocking { isUserLoggedInUseCase() }.thenReturn(false)

        initViewModel()
        advanceUntilIdle()

        verifyBlocking(getSpecificAccountDetailUseCase, never()) { invoke(any(), any(), any()) }
        verifyBlocking(getCurrentUserEmail, never()) { invoke() }
        verifyNoInteractions(monitorAccountDetailUseCase)
    }

    @Test
    fun `test that init reports a load error when the user is not logged in and plans fail to load`() =
        runTest {
            wheneverBlocking { isUserLoggedInUseCase() }.thenReturn(false)
            wheneverBlocking { getSubscriptionsUseCase() }
                .thenThrow(RuntimeException("offline"))

            initViewModel()
            advanceUntilIdle()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.hasLoadError).isTrue()
                assertThat(state.recommendedSubscription).isNull()
            }
        }

    @Test
    fun `test that init treats a failing login check as logged out`() = runTest {
        wheneverBlocking { isUserLoggedInUseCase() }.thenThrow(RuntimeException("no session"))

        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            assertThat(awaitItem().isLoggedIn).isFalse()
        }
    }

    @Test
    fun `test that init requests both storage and transfer account details`() = runTest {
        initViewModel()
        advanceUntilIdle()

        verifyBlocking(getSpecificAccountDetailUseCase) {
            invoke(storage = true, transfer = true, pro = false)
        }
    }

    @Test
    fun `test that init still emits state when fetching account details fails`() = runTest {
        wheneverBlocking { getSpecificAccountDetailUseCase(any(), any(), any()) }
            .thenThrow(RuntimeException("offline"))

        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            assertThat(awaitItem()).isNotNull()
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
    fun `test that a plan with a smaller quota than the current plan is not recommended`() =
        runTest {
            val essential = subscription(AccountType.ESSENTIAL, storage = 200, transfer = 2048)
            val proII = subscription(AccountType.PRO_II, storage = 8192, transfer = 8192)
            val detail = accountDetail(
                storageUsed = 20 * BYTES_IN_GB,
                accountType = AccountType.PRO_I,
                totalStorage = 3072 * BYTES_IN_GB,
                totalTransfer = 3072 * BYTES_IN_GB,
                transferUsed = 3072 * BYTES_IN_GB,
            )
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
    fun `test that a yearly account is compared against the yearly transfer quota`() = runTest {
        val detail = accountDetail(
            storageUsed = 1 * BYTES_IN_GB,
            accountType = AccountType.PRO_I,
            totalStorage = 3072 * BYTES_IN_GB,
            totalTransfer = 36864 * BYTES_IN_GB,
            transferUsed = 36864 * BYTES_IN_GB,
            accountSubscriptionCycle = AccountSubscriptionCycle.YEARLY,
        )
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
        wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(proPlanSubscriptions())
        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.recommendedSubscription?.accountType).isEqualTo(AccountType.PRO_II)
            assertThat(state.isHighestPlan).isFalse()
        }
    }

    @Test
    fun `test that an account with no billing cycle is compared against the yearly transfer quota`() =
        runTest {
            // a one-off yearly plan carries no subscription cycle
            val detail = accountDetail(
                storageUsed = 1 * BYTES_IN_GB,
                accountType = AccountType.PRO_I,
                totalStorage = 3072 * BYTES_IN_GB,
                totalTransfer = 36864 * BYTES_IN_GB,
                transferUsed = 36864 * BYTES_IN_GB,
            )
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
            wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(proPlanSubscriptions())
            initViewModel()
            advanceUntilIdle()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.recommendedSubscription?.accountType).isEqualTo(AccountType.PRO_II)
                assertThat(state.isHighestPlan).isFalse()
            }
        }

    @Test
    fun `test that a monthly account is compared against the monthly transfer quota`() = runTest {
        val detail = accountDetail(
            storageUsed = 1 * BYTES_IN_GB,
            accountType = AccountType.PRO_I,
            totalStorage = 3072 * BYTES_IN_GB,
            totalTransfer = 3072 * BYTES_IN_GB,
            transferUsed = 3072 * BYTES_IN_GB,
            accountSubscriptionCycle = AccountSubscriptionCycle.MONTHLY,
        )
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
        wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(proPlanSubscriptions())
        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            // Essential is excluded on its monthly 2 TB transfer, which its yearly 24 TB would pass
            assertThat(state.recommendedSubscription?.accountType).isEqualTo(AccountType.PRO_II)
            assertThat(state.isHighestPlan).isFalse()
        }
    }

    /** Plans offered in both cycles, where the yearly option carries twelve times the transfer. */
    private fun proPlanSubscriptions() = Subscriptions(
        monthlySubscriptions = listOf(
            subscription(AccountType.ESSENTIAL, storage = 4096, transfer = 2048),
            subscription(AccountType.PRO_II, storage = 10240, transfer = 10240),
        ),
        yearlySubscriptions = listOf(
            subscription(AccountType.ESSENTIAL, storage = 4096, transfer = 2048 * 12),
            subscription(AccountType.PRO_II, storage = 10240, transfer = 10240 * 12),
        ),
    )

    @Test
    fun `test that the plan the account is already on is not recommended`() = runTest {
        val essential = subscription(AccountType.ESSENTIAL, storage = 200, transfer = 2048)
        val proI = subscription(AccountType.PRO_I, storage = 3072, transfer = 3072)
        val proII = subscription(AccountType.PRO_II, storage = 10240, transfer = 10240)
        val detail = accountDetail(
            storageUsed = 20 * BYTES_IN_GB,
            accountType = AccountType.PRO_I,
            totalStorage = 3072 * BYTES_IN_GB,
            totalTransfer = 3072 * BYTES_IN_GB,
            transferUsed = 3072 * BYTES_IN_GB,
        )
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
        wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(
            Subscriptions(
                monthlySubscriptions = listOf(essential, proI, proII),
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
    fun `test that recommended subscription is the smallest plan whose transfer quota exceeds usage`() =
        runTest {
            val essential = subscription(AccountType.ESSENTIAL, storage = 200, transfer = 2048)
            val proII = subscription(AccountType.PRO_II, storage = 8192, transfer = 8192)
            val detail = accountDetail(
                storageUsed = 10 * BYTES_IN_GB,
                totalStorage = 20 * BYTES_IN_GB,
                transferUsed = 3072 * BYTES_IN_GB,
            )
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
            wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(
                // a free account has no cycle, so plans are compared on their yearly option
                Subscriptions(
                    monthlySubscriptions = emptyList(),
                    yearlySubscriptions = listOf(essential, proII),
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
    fun `test that a cheaper discounted plan is ignored when it does not cover transfer usage`() =
        runTest {
            val essential = subscription(
                AccountType.ESSENTIAL,
                storage = 8192,
                transfer = 2048,
                amount = 4.99f,
                discountedAmountMonthly = 1.99f,
                discountedPercentage = 60,
            )
            val proII = subscription(
                AccountType.PRO_II,
                storage = 8192,
                transfer = 8192,
                amount = 9.99f,
            )
            val detail = accountDetail(
                storageUsed = 10 * BYTES_IN_GB,
                totalStorage = 20 * BYTES_IN_GB,
                transferUsed = 3072 * BYTES_IN_GB,
            )
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
            wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(
                // a free account has no cycle, so plans are compared on their yearly option
                Subscriptions(
                    monthlySubscriptions = emptyList(),
                    yearlySubscriptions = listOf(essential, proII),
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
    fun `test that a plan adding storage but lowering transfer is not an upgrade`() = runTest {
        val proII = subscription(AccountType.PRO_II, storage = 8192, transfer = 8192)
        val moreStorageLessTransfer =
            subscription(AccountType.PRO_III, storage = 16384, transfer = 4096)
        val detail = accountDetail(
            storageUsed = 1000 * BYTES_IN_GB,
            accountType = AccountType.PRO_II,
            totalStorage = 8192 * BYTES_IN_GB,
            totalTransfer = 8192 * BYTES_IN_GB,
            transferUsed = 8192 * BYTES_IN_GB,
            accountSubscriptionCycle = AccountSubscriptionCycle.MONTHLY,
        )
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
        wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(
            Subscriptions(
                monthlySubscriptions = listOf(proII, moreStorageLessTransfer),
                yearlySubscriptions = emptyList(),
            )
        )
        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.recommendedSubscription).isNull()
            assertThat(state.isHighestPlan).isTrue()
        }
    }

    @Test
    fun `test that a plan raising only the transfer quota is an upgrade`() = runTest {
        val proII = subscription(AccountType.PRO_II, storage = 8192, transfer = 8192)
        val moreTransfer = subscription(AccountType.PRO_III, storage = 8192, transfer = 16384)
        val detail = accountDetail(
            storageUsed = 1000 * BYTES_IN_GB,
            accountType = AccountType.PRO_II,
            totalStorage = 8192 * BYTES_IN_GB,
            totalTransfer = 8192 * BYTES_IN_GB,
            transferUsed = 8192 * BYTES_IN_GB,
            accountSubscriptionCycle = AccountSubscriptionCycle.MONTHLY,
        )
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
        wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(
            Subscriptions(
                monthlySubscriptions = listOf(proII, moreTransfer),
                yearlySubscriptions = emptyList(),
            )
        )
        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.recommendedSubscription?.accountType).isEqualTo(AccountType.PRO_III)
            assertThat(state.isHighestPlan).isFalse()
        }
    }

    @Test
    fun `test that bonus quota on the account does not rule out every upgrade`() = runTest {
        val proII = subscription(AccountType.PRO_II, storage = 10240, transfer = 10240)
        val proIII = subscription(AccountType.PRO_III, storage = 20480, transfer = 20480)
        // the account holds more transfer than the plan sells: plan quota plus bonus
        val detail = accountDetail(
            storageUsed = 10240 * BYTES_IN_GB,
            accountType = AccountType.PRO_II,
            totalStorage = 10240 * BYTES_IN_GB,
            totalTransfer = 25600 * BYTES_IN_GB,
            transferUsed = 12288 * BYTES_IN_GB,
            accountSubscriptionCycle = AccountSubscriptionCycle.MONTHLY,
        )
        whenever(monitorStorageStateUseCase()).thenReturn(flowOf(StorageState.Red))
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
            assertThat(state.recommendedSubscription?.accountType).isEqualTo(AccountType.PRO_III)
            assertThat(state.isHighestPlan).isFalse()
        }
    }

    @Test
    fun `test that transfer usage does not raise the recommendation when storage is running out`() =
        runTest {
            val proI = subscription(AccountType.PRO_I, storage = 3072, transfer = 3072)
            val proII = subscription(AccountType.PRO_II, storage = 10240, transfer = 10240)
            val proIII = subscription(AccountType.PRO_III, storage = 20480, transfer = 20480)
            val detail = accountDetail(
                storageUsed = 2940 * BYTES_IN_GB,
                accountType = AccountType.PRO_I,
                totalStorage = 3072 * BYTES_IN_GB,
                totalTransfer = 3072 * BYTES_IN_GB,
                transferUsed = 12288 * BYTES_IN_GB,
                accountSubscriptionCycle = AccountSubscriptionCycle.MONTHLY,
            )
            whenever(monitorStorageStateUseCase()).thenReturn(flowOf(StorageState.Red))
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
            wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(
                Subscriptions(
                    monthlySubscriptions = listOf(proI, proII, proIII),
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
    fun `test that storage usage does not raise the recommendation when transfer is over quota`() =
        runTest {
            val proII = subscription(AccountType.PRO_II, storage = 10240, transfer = 4096)
            val proIII = subscription(AccountType.PRO_III, storage = 20480, transfer = 20480)
            val detail = accountDetail(
                storageUsed = 12288 * BYTES_IN_GB,
                accountType = AccountType.PRO_I,
                totalStorage = 3072 * BYTES_IN_GB,
                totalTransfer = 3072 * BYTES_IN_GB,
                transferUsed = 3072 * BYTES_IN_GB,
                accountSubscriptionCycle = AccountSubscriptionCycle.MONTHLY,
            )
            whenever(monitorTransferOverQuotaUseCase()).thenReturn(flowOf(true))
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
                assertThat(awaitItem().recommendedSubscription?.accountType)
                    .isEqualTo(AccountType.PRO_II)
            }
        }

    @Test
    fun `test that no plan is recommended until the current plan is known`() = runTest {
        val storageDetail = AccountStorageDetail(
            usedCloudDrive = 0,
            usedRubbish = 0,
            usedIncoming = 0,
            totalStorage = 20 * BYTES_IN_GB,
            usedStorage = 19 * BYTES_IN_GB,
        )
        val detail = mock<AccountDetail> {
            on { this.storageDetail }.thenReturn(storageDetail)
            on { this.transferDetail }.thenReturn(null)
            on { this.levelDetail }.thenReturn(null)
        }
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
        wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(
            Subscriptions(
                monthlySubscriptions = listOf(subscription(AccountType.PRO_III, storage = 20480)),
                yearlySubscriptions = emptyList(),
            )
        )
        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.currentPlan).isNull()
            assertThat(state.recommendedSubscription).isNull()
            assertThat(state.isLoading).isTrue()
        }
    }

    @Test
    fun `test that a plan offered only yearly is not compared against a monthly transfer quota`() =
        runTest {
            val proI = subscription(AccountType.PRO_I, storage = 3072, transfer = 3072)
            val proII = subscription(AccountType.PRO_II, storage = 10240, transfer = 10240)
            val yearlyOnly = subscription(AccountType.ESSENTIAL, storage = 4096, transfer = 2048)
            val detail = accountDetail(
                storageUsed = 3000 * BYTES_IN_GB,
                accountType = AccountType.PRO_I,
                totalStorage = 3072 * BYTES_IN_GB,
                totalTransfer = 3072 * BYTES_IN_GB,
                transferUsed = 1000 * BYTES_IN_GB,
                accountSubscriptionCycle = AccountSubscriptionCycle.MONTHLY,
            )
            whenever(monitorStorageStateUseCase()).thenReturn(flowOf(StorageState.Red))
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
            wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(
                Subscriptions(
                    monthlySubscriptions = listOf(proI, proII),
                    yearlySubscriptions = listOf(yearlyOnly),
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

    @Test
    fun `test that isConnected is false when there is no connection`() = runTest {
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(false))
        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            assertThat(awaitItem().isConnected).isFalse()
        }
    }

    @Test
    fun `test that isConnected follows connectivity changes`() = runTest {
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(false, true))
        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            assertThat(awaitItem().isConnected).isTrue()
        }
    }

    @Test
    fun `test that hasLoadError is true when the subscriptions fetch fails`() = runTest {
        wheneverBlocking { getSubscriptionsUseCase() }.thenThrow(RuntimeException("failed"))
        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            assertThat(awaitItem().hasLoadError).isTrue()
        }
    }

    @Test
    fun `test that hasLoadError is false when the subscriptions fetch succeeds`() = runTest {
        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            assertThat(awaitItem().hasLoadError).isFalse()
        }
    }

    @Test
    fun `test that a second onRetry while the first is running does not fetch again`() = runTest {
        initViewModel()
        advanceUntilIdle()
        reset(getSubscriptionsUseCase)
        // Hold the fetch suspended so the second tap lands while the first retry is still active.
        val gate = CompletableDeferred<Subscriptions>()
        wheneverBlocking { getSubscriptionsUseCase() }.doSuspendableAnswer { gate.await() }

        underTest.onRetry()
        underTest.onRetry()
        gate.complete(Subscriptions(emptyList(), emptyList()))
        advanceUntilIdle()

        verifyBlocking(getSubscriptionsUseCase, times(1)) { invoke() }
    }

    @Test
    fun `test that onRetry recommends a plan when the initial subscriptions fetch failed`() =
        runTest {
            val proI = subscription(AccountType.PRO_I, storage = 400)
            val detail = accountDetail(storageUsed = 250 * BYTES_IN_GB)
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
            wheneverBlocking { getSubscriptionsUseCase() }.thenThrow(RuntimeException("offline"))
            initViewModel()
            advanceUntilIdle()
            assertThat(underTest.state.value.recommendedSubscription).isNull()

            reset(getSubscriptionsUseCase)
            wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(
                Subscriptions(monthlySubscriptions = listOf(proI), yearlySubscriptions = emptyList())
            )
            underTest.onRetry()
            advanceUntilIdle()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.recommendedSubscription?.accountType).isEqualTo(AccountType.PRO_I)
                assertThat(state.hasLoadError).isFalse()
            }
        }

    @Test
    fun `test that a failed onRetry replaces the loaded subscriptions with the error state`() =
        runTest {
            val proI = subscription(AccountType.PRO_I, storage = 400)
            val detail = accountDetail(storageUsed = 250 * BYTES_IN_GB)
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(detail))
            wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(
                Subscriptions(monthlySubscriptions = listOf(proI), yearlySubscriptions = emptyList())
            )
            initViewModel()
            advanceUntilIdle()
            assertThat(underTest.state.value.recommendedSubscription).isNotNull()

            reset(getSubscriptionsUseCase)
            wheneverBlocking { getSubscriptionsUseCase() }.thenThrow(RuntimeException("offline"))
            underTest.onRetry()
            advanceUntilIdle()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.hasLoadError).isTrue()
                assertThat(state.recommendedSubscription).isNull()
            }
        }

    private fun accountDetail(
        storageUsed: Long,
        accountType: AccountType = AccountType.FREE,
        totalStorage: Long = 0,
        transferUsed: Long? = null,
        totalTransfer: Long = 0,
        accountSubscriptionCycle: AccountSubscriptionCycle = AccountSubscriptionCycle.UNKNOWN,
    ): AccountDetail {
        val storageDetail = AccountStorageDetail(
            usedCloudDrive = 0,
            usedRubbish = 0,
            usedIncoming = 0,
            totalStorage = totalStorage,
            usedStorage = storageUsed,
        )
        val transferDetail = transferUsed?.let {
            AccountTransferDetail(
                totalTransfer = totalTransfer,
                usedTransfer = it,
                usedTransferPercentage = 0,
            )
        }
        val levelDetail = mock<AccountLevelDetail> {
            on { this.accountType }.thenReturn(accountType)
            on { this.accountSubscriptionCycle }.thenReturn(accountSubscriptionCycle)
        }
        return mock {
            on { this.storageDetail }.thenReturn(storageDetail)
            on { this.transferDetail }.thenReturn(transferDetail)
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
        transfer: Int = 0,
        amount: Float = 4.99f,
        discountedAmountMonthly: Float? = null,
        discountedPercentage: Int? = null,
    ) = Subscription(
        sku = "sku_${accountType.name}_$storage",
        accountType = accountType,
        handle = 1L,
        storage = storage,
        transfer = transfer,
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
