package mega.privacy.android.domain.usecase.billing

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.AccountStorageDetail
import mega.privacy.android.domain.entity.billing.RecommendedSubscriptionOffer
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorSubscriptionOfferUseCaseTest {

    private lateinit var underTest: MonitorSubscriptionOfferUseCase

    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val getRecommendedSubscriptionWithOfferUseCase =
        mock<GetRecommendedSubscriptionWithOfferUseCase>()

    private val accountDetail = MutableStateFlow(AccountDetail())

    @BeforeAll
    fun setUp() {
        underTest = MonitorSubscriptionOfferUseCase(
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getRecommendedSubscriptionWithOfferUseCase = getRecommendedSubscriptionWithOfferUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(monitorAccountDetailUseCase, getRecommendedSubscriptionWithOfferUseCase)
        accountDetail.value = AccountDetail()
        whenever(monitorAccountDetailUseCase()).thenReturn(accountDetail)
    }

    private fun accountDetail(accountType: AccountType) = AccountDetail(
        levelDetail = mock<AccountLevelDetail> {
            on { this.accountType } doReturn accountType
        },
    )

    @Test
    fun `test that invoke emits the recommended offer`() = runTest {
        val offer = mock<RecommendedSubscriptionOffer>()
        whenever(getRecommendedSubscriptionWithOfferUseCase()).thenReturn(offer)

        underTest().test {
            assertThat(awaitItem().getOrNull()).isEqualTo(offer)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke emits null when no plan carries an offer`() = runTest {
        whenever(getRecommendedSubscriptionWithOfferUseCase()).thenReturn(null)

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

        underTest().test {
            assertThat(awaitItem().isFailure).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke re-evaluates the offer when the account type changes`() = runTest {
        val offer = mock<RecommendedSubscriptionOffer>()
        whenever(getRecommendedSubscriptionWithOfferUseCase()).thenReturn(offer, null)

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
            accountDetail.value = accountDetail(AccountType.FREE)

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

        underTest().test {
            assertThat(awaitItem().isFailure).isTrue()

            accountDetail.value = accountDetail(AccountType.PRO_I)

            assertThat(awaitItem().getOrNull()).isEqualTo(offer)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
