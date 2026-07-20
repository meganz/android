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
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.AccountStorageDetail
import mega.privacy.android.domain.entity.account.AccountTransferDetail
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.domain.entity.payment.Subscriptions
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.billing.GetRecommendedSubscriptionUseCase
import mega.privacy.android.domain.usecase.billing.GetSubscriptionsUseCase
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
    private val getRecommendedSubscriptionUseCase = mock<GetRecommendedSubscriptionUseCase>()
    private val getSubscriptionsUseCase = mock<GetSubscriptionsUseCase>()
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
            getRecommendedSubscriptionUseCase,
            getSubscriptionsUseCase,
            localisedPriceCurrencyCodeStringMapper,
            formattedSizeMapper,
        )
        whenever(monitorAccountDetailUseCase()).thenReturn(emptyFlow())
        whenever(monitorStorageStateUseCase()).thenReturn(emptyFlow())
        whenever(monitorTransferOverQuotaUseCase()).thenReturn(emptyFlow())
        wheneverBlocking { getRecommendedSubscriptionUseCase() }.thenReturn(null)
        wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(Subscriptions(emptyList(), emptyList()))
    }

    private fun initViewModel() {
        underTest = QuotaWarningUpgradeViewModel(
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            monitorStorageStateUseCase = monitorStorageStateUseCase,
            monitorTransferOverQuotaUseCase = monitorTransferOverQuotaUseCase,
            getRecommendedSubscriptionUseCase = getRecommendedSubscriptionUseCase,
            getSubscriptionsUseCase = getSubscriptionsUseCase,
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
    fun `test that recommended subscription is built from the subscriptions list`() = runTest {
        val recommended = subscription(AccountType.ESSENTIAL, storage = 200, transfer = 2400)
        val yearly = subscription(AccountType.ESSENTIAL, storage = 200, transfer = 2400)
        wheneverBlocking { getRecommendedSubscriptionUseCase() }.thenReturn(recommended)
        wheneverBlocking { getSubscriptionsUseCase() }.thenReturn(
            Subscriptions(listOf(recommended), listOf(yearly))
        )
        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.recommendedSubscription?.accountType).isEqualTo(AccountType.ESSENTIAL)
            assertThat(state.recommendedSubscription?.monthlySubscription).isEqualTo(recommended)
            assertThat(state.recommendedSubscription?.yearlySubscription).isEqualTo(yearly)
        }
    }

    @Test
    fun `test that recommended subscription is null when no next tier is available`() = runTest {
        wheneverBlocking { getRecommendedSubscriptionUseCase() }.thenReturn(null)
        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.recommendedSubscription).isNull()
            assertThat(state.isLoading).isFalse()
        }
    }

    private fun subscription(accountType: AccountType, storage: Int, transfer: Int) = Subscription(
        sku = "sku_${accountType.name}",
        accountType = accountType,
        handle = 1L,
        storage = storage,
        transfer = transfer,
        amount = CurrencyAmount(4.99f, Currency("EUR")),
    )

    companion object {
        private const val BYTES_IN_GB = 1024L * 1024L * 1024L
    }
}
