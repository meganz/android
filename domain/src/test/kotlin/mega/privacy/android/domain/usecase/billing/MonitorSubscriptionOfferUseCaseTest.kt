package mega.privacy.android.domain.usecase.billing

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.AccountStorageDetail
import mega.privacy.android.domain.entity.billing.RecommendedSubscriptionOffer
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorSubscriptionOfferUseCaseTest {

    private lateinit var underTest: MonitorSubscriptionOfferUseCase

    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val getRecommendedSubscriptionWithOfferUseCase =
        mock<GetRecommendedSubscriptionWithOfferUseCase>()

    private val accountDetail = MutableStateFlow(AccountDetail())

    @BeforeEach
    fun resetMocks() {
        reset(monitorAccountDetailUseCase, getRecommendedSubscriptionWithOfferUseCase)
        accountDetail.value = accountDetail(AccountType.FREE)
        whenever(monitorAccountDetailUseCase()).thenReturn(accountDetail)
    }

    private fun createUseCase(scope: CoroutineScope) = MonitorSubscriptionOfferUseCase(
        monitorAccountDetailUseCase = monitorAccountDetailUseCase,
        getRecommendedSubscriptionWithOfferUseCase = getRecommendedSubscriptionWithOfferUseCase,
        scope = scope,
    )

    private fun accountDetail(accountType: AccountType) = AccountDetail(
        levelDetail = mock<AccountLevelDetail> {
            on { this.accountType } doReturn accountType
        },
    )

    @Test
    fun `test that invoke emits the recommended offer`() = runTest {
        val offer = mock<RecommendedSubscriptionOffer>()
        whenever(getRecommendedSubscriptionWithOfferUseCase()).thenReturn(offer)
        underTest = createUseCase(backgroundScope)

        underTest().test {
            assertThat(awaitItem().getOrNull()).isEqualTo(offer)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke emits null when no plan carries an offer`() = runTest {
        whenever(getRecommendedSubscriptionWithOfferUseCase()).thenReturn(null)
        underTest = createUseCase(backgroundScope)

        underTest().test {
            val actual = awaitItem()
            assertThat(actual.isSuccess).isTrue()
            assertThat(actual.getOrNull()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke emits a failure when the lookup fails`() = runTest {
        whenever(getRecommendedSubscriptionWithOfferUseCase())
            .thenAnswer { throw RuntimeException("Billing unavailable") }
        underTest = createUseCase(backgroundScope)

        underTest().test {
            assertThat(awaitItem().isFailure).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke looks the offer up before the account plan is known`() = runTest {
        val offer = mock<RecommendedSubscriptionOffer>()
        whenever(getRecommendedSubscriptionWithOfferUseCase()).thenReturn(offer)
        accountDetail.value = AccountDetail()
        underTest = createUseCase(backgroundScope)

        underTest().test {
            assertThat(awaitItem().getOrNull()).isEqualTo(offer)
            cancelAndIgnoreRemainingEvents()
        }
        verify(getRecommendedSubscriptionWithOfferUseCase).invoke()
    }

    @Test
    fun `test that invoke re-evaluates the offer against the real tier once the account details arrive`() =
        runTest {
            val offer = mock<RecommendedSubscriptionOffer>()
            whenever(getRecommendedSubscriptionWithOfferUseCase()).thenReturn(offer, null)
            accountDetail.value = AccountDetail()
            underTest = createUseCase(backgroundScope)

            underTest().test {
                assertThat(awaitItem().getOrNull()).isEqualTo(offer)

                accountDetail.value = accountDetail(AccountType.PRO_III)

                assertThat(awaitItem().getOrNull()).isNull()
                cancelAndIgnoreRemainingEvents()
            }
            verify(getRecommendedSubscriptionWithOfferUseCase, times(2)).invoke()
        }

    @Test
    fun `test that invoke re-evaluates the offer when the account is switched for one on the same plan`() =
        runTest {
            val offer = mock<RecommendedSubscriptionOffer>()
            whenever(getRecommendedSubscriptionWithOfferUseCase()).thenReturn(offer)
            underTest = createUseCase(backgroundScope)

            underTest().test {
                assertThat(awaitItem().getOrNull()).isEqualTo(offer)

                accountDetail.value = AccountDetail()
                advanceTimeBy(1.seconds)
                awaitItem()

                accountDetail.value = accountDetail(AccountType.FREE)
                advanceTimeBy(1.seconds)
                assertThat(awaitItem().getOrNull()).isEqualTo(offer)

                cancelAndIgnoreRemainingEvents()
            }
            verify(getRecommendedSubscriptionWithOfferUseCase, times(3)).invoke()
        }

    @Test
    fun `test that invoke re-evaluates the offer when the account type changes`() = runTest {
        val offer = mock<RecommendedSubscriptionOffer>()
        whenever(getRecommendedSubscriptionWithOfferUseCase()).thenReturn(offer, null)
        underTest = createUseCase(backgroundScope)

        underTest().test {
            assertThat(awaitItem().getOrNull()).isEqualTo(offer)

            accountDetail.value = accountDetail(AccountType.PRO_I)

            assertThat(awaitItem().getOrNull()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke does not re-evaluate the offer when the account type is unchanged`() =
        runTest {
            whenever(getRecommendedSubscriptionWithOfferUseCase()).thenReturn(null)
            underTest = createUseCase(backgroundScope)

            underTest().test {
                awaitItem()

                accountDetail.value = accountDetail(AccountType.FREE).copy(
                    storageDetail = mock<AccountStorageDetail>(),
                )

                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
            verify(getRecommendedSubscriptionWithOfferUseCase).invoke()
        }

    @Test
    fun `test that invoke keeps monitoring after a failed lookup`() = runTest {
        val offer = mock<RecommendedSubscriptionOffer>()
        whenever(getRecommendedSubscriptionWithOfferUseCase())
            .thenAnswer { throw RuntimeException("Billing unavailable") }
            .thenReturn(offer)
        underTest = createUseCase(backgroundScope)

        underTest().test {
            assertThat(awaitItem().isFailure).isTrue()

            accountDetail.value = accountDetail(AccountType.PRO_I)

            assertThat(awaitItem().getOrNull()).isEqualTo(offer)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke looks the offer up once when two surfaces collect it at the same time`() =
        runTest {
            val offer = mock<RecommendedSubscriptionOffer>()
            whenever(getRecommendedSubscriptionWithOfferUseCase()).thenReturn(offer)
            underTest = createUseCase(backgroundScope)

            underTest().test {
                assertThat(awaitItem().getOrNull()).isEqualTo(offer)

                underTest().test {
                    assertThat(awaitItem().getOrNull()).isEqualTo(offer)
                    cancelAndIgnoreRemainingEvents()
                }

                cancelAndIgnoreRemainingEvents()
            }
            verify(getRecommendedSubscriptionWithOfferUseCase).invoke()
        }

    @Test
    fun `test that invoke does not look the offer up again when a surface resubscribes within the keep alive window`() =
        runTest {
            val offer = mock<RecommendedSubscriptionOffer>()
            whenever(getRecommendedSubscriptionWithOfferUseCase()).thenReturn(offer)
            underTest = createUseCase(backgroundScope)

            underTest().test {
                assertThat(awaitItem().getOrNull()).isEqualTo(offer)
                cancelAndIgnoreRemainingEvents()
            }

            underTest().test {
                assertThat(awaitItem().getOrNull()).isEqualTo(offer)
                cancelAndIgnoreRemainingEvents()
            }
            verify(getRecommendedSubscriptionWithOfferUseCase).invoke()
        }

    @Test
    fun `test that invoke replays the cached offer and refreshes it when a surface resubscribes after the keep alive window`() =
        runTest {
            val offer = mock<RecommendedSubscriptionOffer>()
            whenever(getRecommendedSubscriptionWithOfferUseCase()).thenReturn(offer)
            underTest = createUseCase(backgroundScope)

            underTest().test {
                assertThat(awaitItem().getOrNull()).isEqualTo(offer)
                cancelAndIgnoreRemainingEvents()
            }

            advanceTimeBy(ELAPSED_KEEP_ALIVE_WINDOW)

            underTest().test {
                assertThat(awaitItem().getOrNull()).isEqualTo(offer)
                assertThat(awaitItem().getOrNull()).isEqualTo(offer)
                cancelAndIgnoreRemainingEvents()
            }
            verify(getRecommendedSubscriptionWithOfferUseCase, times(2)).invoke()
        }
}

/**
 * Comfortably past the use case's keep-alive window, so the shared lookup has stopped and the next
 * subscriber is served from the replay cache before the refresh lands.
 */
private val ELAPSED_KEEP_ALIVE_WINDOW = 10.seconds
