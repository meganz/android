package mega.privacy.android.domain.usecase.billing

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.billing.RecommendedSubscriptionOffer
import mega.privacy.android.domain.repository.BillingRepository
import mega.privacy.android.domain.usecase.environment.GetCurrentTimeInMillisUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShouldShowSubscriptionOfferUseCaseTest {
    private lateinit var underTest: ShouldShowSubscriptionOfferUseCase

    private val getRecommendedSubscriptionWithOfferUseCase =
        mock<GetRecommendedSubscriptionWithOfferUseCase>()
    private val getCurrentTimeInMillisUseCase = mock<GetCurrentTimeInMillisUseCase>()
    private val billingRepository = mock<BillingRepository>()

    @BeforeAll
    fun setUp() {
        underTest = ShouldShowSubscriptionOfferUseCase(
            getRecommendedSubscriptionWithOfferUseCase = getRecommendedSubscriptionWithOfferUseCase,
            getCurrentTimeInMillisUseCase = getCurrentTimeInMillisUseCase,
            billingRepository = billingRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getRecommendedSubscriptionWithOfferUseCase,
            getCurrentTimeInMillisUseCase,
            billingRepository,
        )
    }

    @Test
    fun `test that invoke returns false when no plan has an offer`() = runTest {
        whenever(getRecommendedSubscriptionWithOfferUseCase()).thenReturn(null)

        assertThat(underTest()).isFalse()
    }

    @Test
    fun `test that invoke returns true when the offer has never been shown`() = runTest {
        stub(reshowInterval = RESHOW_INTERVAL_SECONDS, lastShownTime = null)

        assertThat(underTest()).isTrue()
    }

    @Test
    fun `test that invoke returns false when the reshow interval has not elapsed`() = runTest {
        stub(reshowInterval = RESHOW_INTERVAL_SECONDS, lastShownTime = NOW)
        whenever(getCurrentTimeInMillisUseCase()).thenReturn(
            NOW + RESHOW_INTERVAL_SECONDS.seconds.inWholeMilliseconds - 1L
        )

        assertThat(underTest()).isFalse()
    }

    @Test
    fun `test that invoke returns true when the reshow interval has elapsed`() = runTest {
        stub(reshowInterval = RESHOW_INTERVAL_SECONDS, lastShownTime = NOW)
        whenever(getCurrentTimeInMillisUseCase()).thenReturn(
            NOW + RESHOW_INTERVAL_SECONDS.seconds.inWholeMilliseconds
        )

        assertThat(underTest()).isTrue()
    }

    @Test
    fun `test that invoke returns false when the offer has already been shown and has no reshow interval`() =
        runTest {
            stub(reshowInterval = null, lastShownTime = NOW)
            whenever(getCurrentTimeInMillisUseCase()).thenReturn(Long.MAX_VALUE)

            assertThat(underTest()).isFalse()
        }

    private suspend fun stub(reshowInterval: Long?, lastShownTime: Long?) {
        val subscription = mock<Subscription> {
            on { offerReshowInterval } doReturn reshowInterval
        }
        val offer = mock<RecommendedSubscriptionOffer> {
            on { this.subscription } doReturn subscription
        }
        whenever(getRecommendedSubscriptionWithOfferUseCase()).thenReturn(offer)
        whenever(billingRepository.getSubscriptionOfferLastShownTime()).thenReturn(lastShownTime)
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val RESHOW_INTERVAL_SECONDS = 86_400L
    }
}
