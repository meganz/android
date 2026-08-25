package mega.privacy.mobile.home.presentation.home.widget.banner.view

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class OfferExpiryFlowTest {

    @Test
    fun `test that offerExpiryFlow emits false while the offer is still running`() = runTest {
        offerExpiryFlow(
            validUntil = 60L,
            currentTimeMillis = { 0L },
        ).test {
            assertThat(awaitItem()).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that offerExpiryFlow emits true once the offer has elapsed`() = runTest {
        offerExpiryFlow(
            validUntil = 60L,
            currentTimeMillis = { testScheduler.currentTime },
        ).test {
            assertThat(awaitItem()).isFalse()
            assertThat(awaitItem()).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `test that offerExpiryFlow emits true on its next poll when the clock jumps past the expiry`() =
        runTest {
            var now = 0L
            offerExpiryFlow(
                validUntil = 3600L,
                currentTimeMillis = { now },
            ).test {
                assertThat(awaitItem()).isFalse()
                now = 7_200_000L
                assertThat(awaitItem()).isTrue()
                awaitComplete()
            }
        }

    @Test
    fun `test that offerExpiryFlow emits true immediately when the offer is already elapsed`() =
        runTest {
            offerExpiryFlow(
                validUntil = 10L,
                currentTimeMillis = { 20_000L },
            ).test {
                assertThat(awaitItem()).isTrue()
                awaitComplete()
            }
        }
}
