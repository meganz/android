package test.mega.privacy.android.app.presentation.cancelaccountplan

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.cancelaccountplan.CancelAccountPlanViewModel
import mega.privacy.android.app.presentation.cancelaccountplan.model.CancellationInstructionsType
import mega.privacy.android.app.presentation.cancelaccountplan.model.mapper.CancellationInstructionsTypeMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.PaymentMethod
import mega.privacy.android.domain.entity.SubscriptionStatus
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.billing.GetCurrentPaymentUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CancelAccountPlanViewModelTest {
    private lateinit var underTest: CancelAccountPlanViewModel

    private val getCurrentPaymentUseCase = mock<GetCurrentPaymentUseCase>()
    private val cancellationInstructionsTypeMapper = mock<CancellationInstructionsTypeMapper>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val accountDetailFlow = MutableStateFlow(AccountDetail())

    private fun initViewModel() {
        underTest = CancelAccountPlanViewModel(
            getCurrentPaymentUseCase = getCurrentPaymentUseCase,
            cancellationInstructionsTypeMapper = cancellationInstructionsTypeMapper,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
        )
    }

    @BeforeEach
    fun setUp() {
        reset(
            getCurrentPaymentUseCase,
            cancellationInstructionsTypeMapper,
            monitorAccountDetailUseCase,
        )
    }

    @Test
    fun `test that cancellation instruction type is updated if current payment method is available`() =
        runTest {
            whenever(getCurrentPaymentUseCase()).thenReturn(PaymentMethod.STRIPE)
            whenever(cancellationInstructionsTypeMapper(any())).thenReturn(
                CancellationInstructionsType.WebClient
            )

            initViewModel()

            underTest.uiState.test {
                assertThat(awaitItem().cancellationInstructionsType).isEqualTo(
                    CancellationInstructionsType.WebClient
                )
            }
        }

    @ParameterizedTest(name = "test that isMonthlySubscription return {1} if monitorAccountDetailUseCase return {0}")
    @MethodSource("provideParameters")
    fun `test that isMonthlySubscription return correct value`(
        accountDetail: AccountDetail,
        isMonthlySubscription: Boolean,
    ) =
        runTest {
            whenever(getCurrentPaymentUseCase()).thenReturn(PaymentMethod.STRIPE)
            whenever(cancellationInstructionsTypeMapper(any())).thenReturn(
                CancellationInstructionsType.WebClient
            )
            whenever(monitorAccountDetailUseCase()).thenReturn(accountDetailFlow)

            initViewModel()

            accountDetailFlow.emit(accountDetail)

            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isMonthlySubscription).isEqualTo(isMonthlySubscription)
            }

        }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(
            expectedAccountDetailWithMonthlySubscription,
            true
        ),
        Arguments.of(
            expectedAccountDetailWithYearlySubscription,
            false
        ),
    )

    private val expectedAccountDetailWithMonthlySubscription = AccountDetail(
        storageDetail = null,
        sessionDetail = null,
        transferDetail = null,
        levelDetail = AccountLevelDetail(
            accountType = AccountType.PRO_I,
            subscriptionStatus = SubscriptionStatus.VALID,
            subscriptionRenewTime = 0L,
            accountSubscriptionCycle = AccountSubscriptionCycle.MONTHLY,
            proExpirationTime = 378672463728467L,
        )
    )
    private val expectedAccountDetailWithYearlySubscription = AccountDetail(
        storageDetail = null,
        sessionDetail = null,
        transferDetail = null,
        levelDetail = AccountLevelDetail(
            accountType = AccountType.PRO_II,
            subscriptionStatus = SubscriptionStatus.VALID,
            subscriptionRenewTime = 0L,
            accountSubscriptionCycle = AccountSubscriptionCycle.YEARLY,
            proExpirationTime = 0L,
        )
    )
}