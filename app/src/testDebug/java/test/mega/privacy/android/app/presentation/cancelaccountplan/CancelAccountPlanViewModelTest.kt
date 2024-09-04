package mega.privacy.android.app.presentation.cancelaccountplan

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.cancelaccountplan.model.CancellationInstructionsType
import mega.privacy.android.app.presentation.cancelaccountplan.model.UICancellationSurveyAnswer
import mega.privacy.android.app.presentation.cancelaccountplan.model.mapper.CancellationInstructionsTypeMapper
import mega.privacy.android.app.presentation.myaccount.mapper.AccountNameMapper
import mega.privacy.android.app.upgradeAccount.model.FormattedSize
import mega.privacy.android.app.upgradeAccount.model.mapper.FormattedSizeMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.PaymentMethod
import mega.privacy.android.domain.entity.SubscriptionOption
import mega.privacy.android.domain.entity.SubscriptionStatus
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.AccountPlanDetail
import mega.privacy.android.domain.entity.account.CurrencyPoint
import mega.privacy.android.domain.usecase.account.CancelSubscriptionWithSurveyAnswersUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.billing.GetAppSubscriptionOptionsUseCase
import mega.privacy.android.domain.usecase.billing.GetCurrentPaymentUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
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
import org.mockito.kotlin.verify
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
    private val getAppSubscriptionOptionsUseCase = mock<GetAppSubscriptionOptionsUseCase>()
    private val formattedSizeMapper = mock<FormattedSizeMapper>()
    private val accountNameMapper = mock<AccountNameMapper>()
    private val accountDetailFlow = MutableStateFlow(AccountDetail())
    private val cancelSubscriptionWithSurveyAnswersUseCase =
        mock<CancelSubscriptionWithSurveyAnswersUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

    private val expectedSubscriptionId = "testSubscriptionId"

    private fun getAccountDetails(
        accountType: AccountType,
    ): AccountDetail {
        return AccountDetail(
            storageDetail = null,
            sessionDetail = null,
            transferDetail = null,
            levelDetail = AccountLevelDetail(
                accountType = accountType,
                subscriptionStatus = SubscriptionStatus.VALID,
                subscriptionRenewTime = 0L,
                accountSubscriptionCycle = AccountSubscriptionCycle.MONTHLY,
                proExpirationTime = 0L,
                accountPlanDetail = AccountPlanDetail(
                    accountType = accountType,
                    isProPlan = true,
                    expirationTime = 0L,
                    subscriptionId = expectedSubscriptionId,
                    featuresList = listOf(),
                    isFreeTrial = false,
                ),
                accountSubscriptionDetailList = listOf(),
            )
        )
    }

    private fun getSubscriptionOption(
        accountType: AccountType,
        storage: Int = 0,
        transfer: Int = 0,
    ) =
        SubscriptionOption(
            accountType = accountType,
            months = 12,
            handle = 0,
            storage = storage,
            transfer = transfer,
            amount = CurrencyPoint.SystemCurrencyPoint(0L),
            currency = Currency(code = "USD")
        )

    private fun initViewModel() {
        underTest = CancelAccountPlanViewModel(
            getCurrentPaymentUseCase = getCurrentPaymentUseCase,
            cancellationInstructionsTypeMapper = cancellationInstructionsTypeMapper,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getAppSubscriptionOptionsUseCase = getAppSubscriptionOptionsUseCase,
            formattedSizeMapper = formattedSizeMapper,
            accountNameMapper = accountNameMapper,
            cancelSubscriptionWithSurveyAnswersUseCase = cancelSubscriptionWithSurveyAnswersUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        )
    }

    @BeforeEach
    fun setUp() {
        reset(
            getCurrentPaymentUseCase,
            monitorAccountDetailUseCase,
            cancellationInstructionsTypeMapper,
            getAppSubscriptionOptionsUseCase,
            formattedSizeMapper,
            accountNameMapper,
            cancelSubscriptionWithSurveyAnswersUseCase,
            getFeatureFlagValueUseCase
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
            whenever(getAppSubscriptionOptionsUseCase(any())).thenReturn(
                listOf(
                    getSubscriptionOption(AccountType.PRO_LITE)
                )
            )
            whenever(getCurrentPaymentUseCase()).thenReturn(PaymentMethod.STRIPE)
            whenever(cancellationInstructionsTypeMapper(any())).thenReturn(
                CancellationInstructionsType.WebClient
            )
            whenever(formattedSizeMapper(size = 15)).thenReturn(
                FormattedSize(
                    unit = 12,
                    size = "50"
                )
            )
            whenever(accountNameMapper(any())).thenReturn(45)
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
            accountPlanDetail = null,
            accountSubscriptionDetailList = listOf(),
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
            accountPlanDetail = null,
            accountSubscriptionDetailList = listOf(),
        )
    )

    @ParameterizedTest(name = "test that rewindDaysQuota is {1} when account type is {0}")
    @MethodSource("provideAccountDetails")
    fun `test that rewindDaysQuota is updated correctly based on account type`(
        accountType: AccountType,
        expectedRewindDaysQuota: String,
    ) = runTest {
        whenever(getAppSubscriptionOptionsUseCase(any())).thenReturn(
            listOf(
                getSubscriptionOption(
                    accountType
                )
            )
        )
        whenever(getCurrentPaymentUseCase()).thenReturn(PaymentMethod.STRIPE)
        whenever(cancellationInstructionsTypeMapper(any())).thenReturn(
            CancellationInstructionsType.WebClient
        )
        whenever(formattedSizeMapper(size = 15)).thenReturn(
            FormattedSize(
                unit = 12,
                size = "50"
            )
        )
        whenever(accountNameMapper(any())).thenReturn(45)

        whenever(monitorAccountDetailUseCase()).thenReturn(accountDetailFlow)

        initViewModel()

        accountDetailFlow.emit(getAccountDetails(accountType))

        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.rewindDaysQuota).isEqualTo(expectedRewindDaysQuota)
        }
    }

    private fun provideAccountDetails(): Stream<Arguments> = Stream.of(
        Arguments.of(AccountType.PRO_LITE, "90"),
        Arguments.of(AccountType.PRO_I, "180"),
    )


    @Test
    fun `test that plan storage is updated correctly`() = runTest {
        val storage = 15000
        val planStorage = FormattedSize(
            unit = 12,
            size = "50"
        )
        val accountType = AccountType.PRO_LITE

        whenever(getAppSubscriptionOptionsUseCase(any())).thenReturn(
            listOf(getSubscriptionOption(accountType, storage = storage))
        )
        whenever(getCurrentPaymentUseCase()).thenReturn(PaymentMethod.STRIPE)
        whenever(cancellationInstructionsTypeMapper(any())).thenReturn(
            CancellationInstructionsType.WebClient
        )
        whenever(formattedSizeMapper(size = storage)).thenReturn(planStorage)
        whenever(accountNameMapper(any())).thenReturn(45)

        whenever(monitorAccountDetailUseCase()).thenReturn(accountDetailFlow)

        initViewModel()

        accountDetailFlow.emit(getAccountDetails(accountType))

        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.formattedPlanStorage?.size).isEqualTo(planStorage.size)
            assertThat(state.formattedPlanStorage?.unit).isEqualTo(planStorage.unit)

        }
    }

    @Test
    fun `test that plan transfer is updated correctly`() = runTest {
        val transfer = 15000
        val planTransfer = FormattedSize(
            unit = 12,
            size = "50"
        )
        val accountType = AccountType.PRO_LITE

        whenever(getAppSubscriptionOptionsUseCase(any())).thenReturn(
            listOf(getSubscriptionOption(accountType, transfer = transfer))
        )
        whenever(getCurrentPaymentUseCase()).thenReturn(PaymentMethod.STRIPE)
        whenever(cancellationInstructionsTypeMapper(any())).thenReturn(
            CancellationInstructionsType.WebClient
        )
        whenever(formattedSizeMapper(size = transfer)).thenReturn(planTransfer)
        whenever(accountNameMapper(any())).thenReturn(45)

        whenever(monitorAccountDetailUseCase()).thenReturn(accountDetailFlow)

        initViewModel()

        accountDetailFlow.emit(getAccountDetails(accountType))

        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.formattedPlanTransfer?.size).isEqualTo(planTransfer.size)
            assertThat(state.formattedPlanTransfer?.unit).isEqualTo(planTransfer.unit)
        }
    }

    @Test
    fun `test that uiState is updated correctly`() = runTest {

        val accountType = AccountType.PRO_LITE

        whenever(getAppSubscriptionOptionsUseCase(any())).thenReturn(
            listOf(getSubscriptionOption(accountType))
        )
        whenever(getCurrentPaymentUseCase()).thenReturn(PaymentMethod.STRIPE)
        whenever(cancellationInstructionsTypeMapper(any())).thenReturn(
            CancellationInstructionsType.WebClient
        )
        whenever(formattedSizeMapper(size = 15)).thenReturn(
            FormattedSize(
                unit = 12,
                size = "50"
            )
        )
        whenever(accountNameMapper(any())).thenReturn(45)

        whenever(monitorAccountDetailUseCase()).thenReturn(accountDetailFlow)

        initViewModel()

        accountDetailFlow.emit(getAccountDetails(accountType))

        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.freePlanStorageQuota).isEqualTo("20")
            assertThat(state.accountNameRes).isEqualTo(45)
            assertThat(state.isLoading).isEqualTo(false)
            assertThat(state.accountType).isEqualTo(accountType)
            assertThat(state.cancellationReasons.size).isEqualTo(10)
            assertThat(state.cancellationReasons.last()).isEqualTo(
                UICancellationSurveyAnswer.Answer8
            )
            assertThat(state.subscriptionId).isEqualTo(expectedSubscriptionId)
        }
    }

    @Test
    fun `test that isLoading is true when monitorAccountDetailUseCase is not called`() = runTest {
        initViewModel()
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isEqualTo(true)
        }
    }

    @Test
    fun `test that invoke calls cancelSubscriptionWithSurveyAnswersUseCase`() = runTest {
        val reason = "reason"
        val canContact = 1
        val accountType = AccountType.PRO_LITE

        whenever(getAppSubscriptionOptionsUseCase(any())).thenReturn(
            listOf(getSubscriptionOption(accountType))
        )
        whenever(monitorAccountDetailUseCase()).thenReturn(accountDetailFlow)

        initViewModel()

        accountDetailFlow.emit(getAccountDetails(accountType))

        advanceUntilIdle()
        underTest.cancelSubscription(reason, canContact)
        verify(cancelSubscriptionWithSurveyAnswersUseCase).invoke(
            reason,
            expectedSubscriptionId,
            canContact
        )
    }
}