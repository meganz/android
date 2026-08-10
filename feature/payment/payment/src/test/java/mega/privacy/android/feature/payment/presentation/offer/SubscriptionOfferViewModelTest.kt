package mega.privacy.android.feature.payment.presentation.offer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.Skus
import mega.privacy.android.domain.entity.billing.RecommendedSubscriptionOffer
import mega.privacy.android.domain.usecase.billing.MonitorSubscriptionOfferUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.feature.payment.model.LocalisedSubscription
import mega.privacy.android.feature.payment.model.mapper.LocalisedSubscriptionMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SubscriptionOfferViewModelTest {

    private lateinit var underTest: SubscriptionOfferViewModel

    private val monitorSubscriptionOfferUseCase = mock<MonitorSubscriptionOfferUseCase>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val localisedSubscriptionMapper = mock<LocalisedSubscriptionMapper>()
    private val localisedSubscription = mock<LocalisedSubscription>()

    @BeforeEach
    fun setUp() {
        reset(
            monitorSubscriptionOfferUseCase,
            monitorConnectivityUseCase,
            localisedSubscriptionMapper,
        )
        whenever(localisedSubscriptionMapper(anyOrNull(), anyOrNull()))
            .thenReturn(localisedSubscription)
        stubConnectivity(true)
    }

    private fun stubConnectivity(isConnected: Boolean) {
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(isConnected))
    }

    private fun initViewModel() {
        underTest = SubscriptionOfferViewModel(
            monitorSubscriptionOfferUseCase = monitorSubscriptionOfferUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            localisedSubscriptionMapper = localisedSubscriptionMapper,
        )
    }

    /**
     * Stubs the monitored offer with a subscription on [sku], returning the promoted [Subscription]
     * so the test can assert which billing period it was mapped on.
     */
    private fun stubOffer(sku: String, hasMultipleOffers: Boolean = false): Subscription {
        val subscription = subscription(sku)
        val offer = offer(subscription, hasMultipleOffers)
        whenever(monitorSubscriptionOfferUseCase()).thenReturn(flowOf(Result.success(offer)))
        return subscription
    }

    private fun stubNoOffer() {
        whenever(monitorSubscriptionOfferUseCase())
            .thenReturn(flowOf(Result.success(null)))
    }

    private fun stubOfferFailure() {
        whenever(monitorSubscriptionOfferUseCase())
            .thenReturn(flowOf(Result.failure(RuntimeException("boom"))))
    }

    private fun subscription(sku: String) = mock<Subscription> {
        on { this.sku } doReturn sku
        on { offerValidUntil } doReturn OFFER_VALID_UNTIL
    }

    private fun offer(subscription: Subscription, hasMultipleOffers: Boolean = false) =
        mock<RecommendedSubscriptionOffer> {
            on { this.subscription } doReturn subscription
            on { this.hasMultipleOffers } doReturn hasMultipleOffers
        }

    @Test
    fun `test that init exposes a monthly offer on the monthly billing period`() = runTest {
        val subscription = stubOffer(Skus.SKU_PRO_I_MONTH)
        initViewModel()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.isMonthly).isTrue()
            assertThat(state.offerSubscription).isEqualTo(localisedSubscription)
            assertThat(state.offerValidUntil).isEqualTo(OFFER_VALID_UNTIL)
            assertThat(state.hasMultipleOffers).isFalse()
        }
        verify(localisedSubscriptionMapper).invoke(
            monthlySubscription = subscription,
            yearlySubscription = null,
        )
    }

    @Test
    fun `test that init exposes a yearly offer on the yearly billing period`() = runTest {
        val subscription = stubOffer(Skus.SKU_PRO_I_YEAR)
        initViewModel()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.isMonthly).isFalse()
            assertThat(state.offerSubscription).isEqualTo(localisedSubscription)
        }
        verify(localisedSubscriptionMapper).invoke(
            monthlySubscription = null,
            yearlySubscription = subscription,
        )
    }

    @Test
    fun `test that init flags multiple offers when the campaign discounts several plans`() =
        runTest {
            stubOffer(Skus.SKU_PRO_I_MONTH, hasMultipleOffers = true)
            initViewModel()

            underTest.state.test {
                assertThat(awaitItem().hasMultipleOffers).isTrue()
            }
        }

    @Test
    fun `test that init exposes no offer when there is no recommended subscription`() = runTest {
        stubNoOffer()
        initViewModel()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.offerSubscription).isNull()
            assertThat(state.hasMultipleOffers).isFalse()
        }
    }

    @Test
    fun `test that init does not report a load error when there is no recommended subscription`() =
        runTest {
            stubNoOffer()
            initViewModel()

            underTest.state.test {
                assertThat(awaitItem().hasLoadError).isFalse()
            }
        }

    @Test
    fun `test that init exposes no offer when loading fails`() = runTest {
        stubOfferFailure()
        initViewModel()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.offerSubscription).isNull()
        }
    }

    @Test
    fun `test that init reports a load error when loading fails`() = runTest {
        stubOfferFailure()
        initViewModel()

        underTest.state.test {
            assertThat(awaitItem().hasLoadError).isTrue()
        }
    }

    @Test
    fun `test that init exposes the connectivity state when the device is offline`() = runTest {
        stubConnectivity(false)
        stubOffer(Skus.SKU_PRO_I_MONTH)
        initViewModel()

        underTest.state.test {
            assertThat(awaitItem().isConnected).isFalse()
        }
    }

    @Test
    fun `test that the offer is cleared when the monitored offer goes away`() = runTest {
        val offer = offer(subscription(Skus.SKU_PRO_I_MONTH))
        val offers = MutableStateFlow<Result<RecommendedSubscriptionOffer?>>(Result.success(offer))
        whenever(monitorSubscriptionOfferUseCase()).thenReturn(offers)
        initViewModel()
        advanceUntilIdle()
        assertThat(underTest.state.value.offerSubscription).isEqualTo(localisedSubscription)

        offers.value = Result.success(null)
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.offerSubscription).isNull()
            assertThat(state.offerValidUntil).isNull()
            assertThat(state.hasMultipleOffers).isFalse()
            assertThat(state.hasLoadError).isFalse()
        }
    }

    @Test
    fun `test that onRetry exposes the offer when loading succeeds`() = runTest {
        stubOfferFailure()
        initViewModel()
        advanceUntilIdle()
        assertThat(underTest.state.value.hasLoadError).isTrue()

        reset(monitorSubscriptionOfferUseCase)
        stubOffer(Skus.SKU_PRO_I_MONTH)
        underTest.onRetry()
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.hasLoadError).isFalse()
            assertThat(state.isLoading).isFalse()
            assertThat(state.offerSubscription).isEqualTo(localisedSubscription)
        }
    }

    @Test
    fun `test that onRetry keeps the load error when loading fails again`() = runTest {
        stubOfferFailure()
        initViewModel()
        advanceUntilIdle()

        underTest.onRetry()
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.hasLoadError).isTrue()
            assertThat(state.isLoading).isFalse()
        }
    }

    private companion object {
        const val OFFER_VALID_UNTIL = 1_800_000_000L
    }
}
