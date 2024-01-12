package test.mega.privacy.android.app.upgradeAccount

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.upgradeAccount.UpgradeAccountActivity.Companion.IS_CROSS_ACCOUNT_MATCH
import mega.privacy.android.app.upgradeAccount.UpgradeAccountViewModel
import mega.privacy.android.app.upgradeAccount.model.LocalisedSubscription
import mega.privacy.android.app.upgradeAccount.model.UpgradePayment
import mega.privacy.android.app.upgradeAccount.model.UserSubscription
import mega.privacy.android.app.upgradeAccount.model.mapper.FormattedSizeMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceStringMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedSubscriptionMapper
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.PaymentMethod
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.SubscriptionStatus
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.domain.entity.billing.PaymentMethodFlags
import mega.privacy.android.domain.usecase.account.GetCurrentSubscriptionPlanUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.billing.GetCurrentPaymentUseCase
import mega.privacy.android.domain.usecase.billing.GetMonthlySubscriptionsUseCase
import mega.privacy.android.domain.usecase.billing.GetPaymentMethodUseCase
import mega.privacy.android.domain.usecase.billing.GetYearlySubscriptionsUseCase
import mega.privacy.android.domain.usecase.billing.IsBillingAvailableUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import nz.mega.sdk.MegaApiJava
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream


@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpgradeAccountViewModelTest {
    private lateinit var underTest: UpgradeAccountViewModel

    private val savedStateHandle = SavedStateHandle()
    private val accountDetailFlow = MutableStateFlow(AccountDetail())
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val getMonthlySubscriptionsUseCase = mock<GetMonthlySubscriptionsUseCase>()
    private val getYearlySubscriptionsUseCase = mock<GetYearlySubscriptionsUseCase>()
    private val getCurrentSubscriptionPlanUseCase = mock<GetCurrentSubscriptionPlanUseCase>()
    private val getCurrentPaymentUseCase = mock<GetCurrentPaymentUseCase>()
    private val isBillingAvailableUseCase = mock<IsBillingAvailableUseCase>()
    private val localisedPriceStringMapper = mock<LocalisedPriceStringMapper>()
    private val localisedPriceCurrencyCodeStringMapper =
        mock<LocalisedPriceCurrencyCodeStringMapper>()
    private val formattedSizeMapper = mock<FormattedSizeMapper>()
    private val localisedSubscriptionMapper =
        LocalisedSubscriptionMapper(
            localisedPriceStringMapper,
            localisedPriceCurrencyCodeStringMapper,
            formattedSizeMapper,
        )
    private val getPaymentMethodUseCase = mock<GetPaymentMethodUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

    @BeforeAll
    fun initialise() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeEach
    fun setUp() {
        reset(
            getMonthlySubscriptionsUseCase,
            getYearlySubscriptionsUseCase,
            getCurrentSubscriptionPlanUseCase,
            getCurrentPaymentUseCase,
            isBillingAvailableUseCase,
            getPaymentMethodUseCase,
            localisedPriceStringMapper,
            localisedPriceCurrencyCodeStringMapper,
            formattedSizeMapper,
            getPaymentMethodUseCase,
            monitorAccountDetailUseCase,
            getFeatureFlagValueUseCase
        )
    }

    private fun initViewModel() {
        underTest = UpgradeAccountViewModel(
            savedStateHandle = savedStateHandle,
            getMonthlySubscriptionsUseCase = getMonthlySubscriptionsUseCase,
            getYearlySubscriptionsUseCase = getYearlySubscriptionsUseCase,
            getCurrentSubscriptionPlanUseCase = getCurrentSubscriptionPlanUseCase,
            getCurrentPaymentUseCase = getCurrentPaymentUseCase,
            isBillingAvailableUseCase = isBillingAvailableUseCase,
            localisedSubscriptionMapper = localisedSubscriptionMapper,
            getPaymentMethodUseCase = getPaymentMethodUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that isCrossAccountMatch should be set based on savedStateHandle value`() = runTest {
        savedStateHandle[IS_CROSS_ACCOUNT_MATCH] = false
        whenever(getMonthlySubscriptionsUseCase()).thenReturn(expectedMonthlySubscriptionsList)
        whenever(getYearlySubscriptionsUseCase()).thenReturn(expectedYearlySubscriptionsList)

        initViewModel()

        underTest.state.test {
            assertThat(awaitItem().isCrossAccountMatch).isFalse()
        }
    }

    @Test
    fun `test that initial state has all Pro plans listed`() = runTest {
        whenever(getMonthlySubscriptionsUseCase()).thenReturn(expectedMonthlySubscriptionsList)
        whenever(getYearlySubscriptionsUseCase()).thenReturn(expectedYearlySubscriptionsList)
        initViewModel()
        underTest.state.map { it.localisedSubscriptionsList }.test {
            assertThat(awaitItem()).isEqualTo(expectedLocalisedSubscriptionsList)
        }
    }

    private fun provideShowNoAdsFeatureParameters() = listOf(
        Arguments.of(true, true, true, true),
        Arguments.of(false, true, true, false),
        Arguments.of(true, false, true, false),
        Arguments.of(true, true, false, false),
    )

    @ParameterizedTest(name = "The showNoAdsFeature should be: {3} when in-app ads feature is: {0}, ads are: {1} and external ads are: {2}")
    @MethodSource("provideShowNoAdsFeatureParameters")
    fun `test that showNoAdsFeature is updated correctly when all required fields are provided`(
        inAppAdvertisementFeature: Boolean,
        isAdsEnabledFeature: Boolean,
        isExternalAdsEnabledFeature: Boolean,
        expected: Boolean,
    ) =
        runTest {
            whenever(getMonthlySubscriptionsUseCase()).thenReturn(expectedMonthlySubscriptionsList)
            whenever(getYearlySubscriptionsUseCase()).thenReturn(expectedYearlySubscriptionsList)
            whenever(getCurrentSubscriptionPlanUseCase()).thenReturn(expectedCurrentPlan)
            whenever(getFeatureFlagValueUseCase.invoke(any())).thenReturn(
                inAppAdvertisementFeature,
                isAdsEnabledFeature,
                isExternalAdsEnabledFeature
            )

            initViewModel()
            underTest.state.map { it.showNoAdsFeature }.test {
                assertThat(awaitItem()).isEqualTo(expected)
            }
        }

    @Test
    fun `test that current subscribed plan is listed`() =
        runTest {
            whenever(getMonthlySubscriptionsUseCase()).thenReturn(expectedMonthlySubscriptionsList)
            whenever(getYearlySubscriptionsUseCase()).thenReturn(expectedYearlySubscriptionsList)
            whenever(getCurrentSubscriptionPlanUseCase()).thenReturn(expectedCurrentPlan)
            initViewModel()
            underTest.state.map { it.currentSubscriptionPlan }.distinctUntilChanged().test {
                assertThat(awaitItem()).isEqualTo(expectedCurrentPlan)
            }
        }

    @Test
    fun `test that initial state has current payment listed if current payment is available`() =
        runTest {
            whenever(getMonthlySubscriptionsUseCase()).thenReturn(expectedMonthlySubscriptionsList)
            whenever(getYearlySubscriptionsUseCase()).thenReturn(expectedYearlySubscriptionsList)
            whenever(getCurrentPaymentUseCase()).thenReturn(expectedCurrentPayment.currentPayment)
            initViewModel()
            underTest.state.map { it.currentPayment }.distinctUntilChanged().test {
                assertThat(awaitItem()).isEqualTo(expectedCurrentPayment)
            }
        }

    @Test
    fun `test that state is updated when current payment is available and current payment check is called`() =
        runTest {
            whenever(getMonthlySubscriptionsUseCase()).thenReturn(expectedMonthlySubscriptionsList)
            whenever(getYearlySubscriptionsUseCase()).thenReturn(expectedYearlySubscriptionsList)
            whenever(getCurrentSubscriptionPlanUseCase()).thenReturn(expectedCurrentPlan)
            whenever(getCurrentPaymentUseCase()).thenReturn(expectedCurrentPayment.currentPayment)
            initViewModel()
            underTest.currentPaymentCheck(Constants.PRO_II)

            underTest.state.map { it.currentPayment }.distinctUntilChanged().test {
                assertThat(awaitItem()).isEqualTo(expectedCurrentPaymentUpdated)
            }
            underTest.state.map { it.showBuyNewSubscriptionDialog }.distinctUntilChanged().test {
                assertThat(awaitItem()).isEqualTo(expectedShowBuyNewSubscriptionDialog)
            }
        }

    @ParameterizedTest(name = "test that showBillingWarning state is set to {0}")
    @ValueSource(booleans = [true, false])
    fun `test that showBillingWarning state is set correctly`(boolean: Boolean) =
        runTest {
            whenever(getMonthlySubscriptionsUseCase()).thenReturn(expectedMonthlySubscriptionsList)
            whenever(getYearlySubscriptionsUseCase()).thenReturn(expectedYearlySubscriptionsList)
            whenever(getCurrentSubscriptionPlanUseCase()).thenReturn(expectedCurrentPlan)
            whenever(getCurrentPaymentUseCase()).thenReturn(expectedCurrentPayment.currentPayment)

            initViewModel()

            underTest.setBillingWarningVisibility(boolean)

            underTest.state.test {
                val showBillingWarning = awaitItem().showBillingWarning
                assertThat(showBillingWarning).isEqualTo(boolean)
            }
        }

    @Test
    fun `test that showBuyNewSubscriptionDialog state is updated if setShowBuyNewSubscriptionDialog is called`() =
        runTest {
            whenever(getMonthlySubscriptionsUseCase()).thenReturn(expectedMonthlySubscriptionsList)
            whenever(getYearlySubscriptionsUseCase()).thenReturn(expectedYearlySubscriptionsList)
            whenever(getCurrentSubscriptionPlanUseCase()).thenReturn(expectedCurrentPlan)
            whenever(getCurrentPaymentUseCase()).thenReturn(expectedCurrentPayment.currentPayment)

            initViewModel()

            underTest.setShowBuyNewSubscriptionDialog(expectedShowBuyNewSubscriptionDialog)

            underTest.state.test {
                val showBuyNewSubscriptionDialog = awaitItem().showBuyNewSubscriptionDialog
                assertThat(showBuyNewSubscriptionDialog).isEqualTo(
                    expectedShowBuyNewSubscriptionDialog
                )
            }
        }

    @ParameterizedTest(name = "test that isMonthlySelected state is set to {0}")
    @ValueSource(booleans = [true, false])
    fun `test that isMonthlySelected state is set correctly`(boolean: Boolean) =
        runTest {
            whenever(getMonthlySubscriptionsUseCase()).thenReturn(expectedMonthlySubscriptionsList)
            whenever(getYearlySubscriptionsUseCase()).thenReturn(expectedYearlySubscriptionsList)
            whenever(getCurrentSubscriptionPlanUseCase()).thenReturn(expectedCurrentPlan)
            whenever(getCurrentPaymentUseCase()).thenReturn(expectedCurrentPayment.currentPayment)

            initViewModel()

            underTest.onSelectingMonthlyPlan(boolean)

            underTest.state.test {
                val isMonthlySelected = awaitItem().isMonthlySelected
                assertThat(isMonthlySelected).isEqualTo(boolean)
            }
        }

    @Test
    fun `test that chosenPlan state is set to the plan selected by user`() =
        runTest {
            whenever(getMonthlySubscriptionsUseCase()).thenReturn(expectedMonthlySubscriptionsList)
            whenever(getYearlySubscriptionsUseCase()).thenReturn(expectedYearlySubscriptionsList)
            whenever(getCurrentSubscriptionPlanUseCase()).thenReturn(expectedCurrentPlan)
            whenever(getCurrentPaymentUseCase()).thenReturn(expectedCurrentPayment.currentPayment)

            initViewModel()

            underTest.onSelectingPlanType(AccountType.PRO_II)

            underTest.state.test {
                val chosenPlan = awaitItem().chosenPlan
                assertThat(chosenPlan).isEqualTo(AccountType.PRO_II)
            }
        }

    @Test
    fun `test that isPaymentMethodAvailable returns true when isBillingAvailableUseCase returns true and getPaymentMethodUseCase contains PAYMENT_METHOD_GOOGLE_WALLET`() =
        runTest {
            whenever(getMonthlySubscriptionsUseCase()).thenReturn(expectedMonthlySubscriptionsList)
            whenever(getYearlySubscriptionsUseCase()).thenReturn(expectedYearlySubscriptionsList)
            whenever(isBillingAvailableUseCase()).thenReturn(true)
            whenever(getPaymentMethodUseCase(false)).thenReturn(PaymentMethodFlags(1L shl MegaApiJava.PAYMENT_METHOD_GOOGLE_WALLET))
            initViewModel()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.isPaymentMethodAvailable).isTrue()
            }
        }

    @ParameterizedTest(name = "test that userSubscription return {1} if monitorAccountDetailUseCase return {0}")
    @MethodSource("provideParameters")
    fun `test that userSubscription return correct value`(
        accountDetail: AccountDetail,
        expectedUserSubscription: UserSubscription,
    ) =
        runTest {
            whenever(getMonthlySubscriptionsUseCase()).thenReturn(expectedMonthlySubscriptionsList)
            whenever(getYearlySubscriptionsUseCase()).thenReturn(expectedYearlySubscriptionsList)
            whenever(getCurrentSubscriptionPlanUseCase()).thenReturn(expectedCurrentPlan)
            whenever(getCurrentPaymentUseCase()).thenReturn(expectedCurrentPayment.currentPayment)
            whenever(monitorAccountDetailUseCase()).thenReturn(accountDetailFlow)

            initViewModel()

            accountDetailFlow.emit(accountDetail)

            advanceUntilIdle()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.userSubscription).isEqualTo(expectedUserSubscription)
            }

        }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(
            expectedAccountDetailWithMonthlySubscription,
            UserSubscription.MONTHLY_SUBSCRIBED
        ),
        Arguments.of(
            expectedAccountDetailWithYearlySubscription,
            UserSubscription.YEARLY_SUBSCRIBED
        ),
        Arguments.of(
            AccountDetail(),
            UserSubscription.NOT_SUBSCRIBED
        ),
    )

    private val expectedCurrentPlan = AccountType.PRO_I
    private val expectedShowBuyNewSubscriptionDialog = true
    private val expectedCurrentPayment =
        UpgradePayment(Constants.INVALID_VALUE, PaymentMethod.GOOGLE_WALLET)
    private val expectedCurrentPaymentUpdated =
        UpgradePayment(Constants.PRO_II, PaymentMethod.GOOGLE_WALLET)
    private val expectedAccountDetailWithMonthlySubscription = AccountDetail(
        storageDetail = null,
        sessionDetail = null,
        transferDetail = null,
        levelDetail = AccountLevelDetail(
            accountType = AccountType.PRO_I,
            subscriptionStatus = SubscriptionStatus.VALID,
            subscriptionRenewTime = 1873874783274L,
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
            subscriptionRenewTime = 1873874783274L,
            accountSubscriptionCycle = AccountSubscriptionCycle.YEARLY,
            proExpirationTime = 378672463728467L,
        )
    )

    private val subscriptionProIMonthly = Subscription(
        accountType = AccountType.PRO_I,
        handle = 1560943707714440503,
        storage = 2048,
        transfer = 2048,
        amount = CurrencyAmount(9.99.toFloat(), Currency("EUR"))
    )

    private val subscriptionProIIMonthly = Subscription(
        accountType = AccountType.PRO_II,
        handle = 7974113413762509455,
        storage = 8192,
        transfer = 8192,
        amount = CurrencyAmount(19.99.toFloat(), Currency("EUR"))
    )

    private val subscriptionProIIIMonthly = Subscription(
        accountType = AccountType.PRO_III,
        handle = -2499193043825823892,
        storage = 16384,
        transfer = 16384,
        amount = CurrencyAmount(29.99.toFloat(), Currency("EUR"))
    )

    private val subscriptionProLiteMonthly = Subscription(
        accountType = AccountType.PRO_LITE,
        handle = -4226692769210777158,
        storage = 400,
        transfer = 1024,
        amount = CurrencyAmount(4.99.toFloat(), Currency("EUR"))
    )

    private val expectedMonthlySubscriptionsList = listOf(
        subscriptionProLiteMonthly,
        subscriptionProIMonthly,
        subscriptionProIIMonthly,
        subscriptionProIIIMonthly
    )

    private val subscriptionProIYearly = Subscription(
        accountType = AccountType.PRO_I,
        handle = 7472683699866478542,
        storage = 2048,
        transfer = 24576,
        amount = CurrencyAmount(99.99.toFloat(), Currency("EUR"))
    )

    private val subscriptionProIIYearly = Subscription(
        accountType = AccountType.PRO_II,
        handle = 370834413380951543,
        storage = 8192,
        transfer = 98304,
        amount = CurrencyAmount(199.99.toFloat(), Currency("EUR"))
    )

    private val subscriptionProIIIYearly = Subscription(
        accountType = AccountType.PRO_III,
        handle = 7225413476571973499,
        storage = 16384,
        transfer = 196608,
        amount = CurrencyAmount(299.99.toFloat(), Currency("EUR"))
    )

    private val subscriptionProLiteYearly = Subscription(
        accountType = AccountType.PRO_LITE,
        handle = -5517769810977460898,
        storage = 400,
        transfer = 12288,
        amount = CurrencyAmount(49.99.toFloat(), Currency("EUR"))
    )

    private val expectedYearlySubscriptionsList = listOf(
        subscriptionProLiteYearly,
        subscriptionProIYearly,
        subscriptionProIIYearly,
        subscriptionProIIIYearly
    )

    private val localisedSubscriptionProI = LocalisedSubscription(
        accountType = AccountType.PRO_I,
        storage = 2048,
        monthlyTransfer = 2048,
        yearlyTransfer = 24576,
        monthlyAmount = CurrencyAmount(9.99.toFloat(), Currency("EUR")),
        yearlyAmount = CurrencyAmount(
            99.99.toFloat(),
            Currency("EUR")
        ),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    private val localisedSubscriptionProII = LocalisedSubscription(
        accountType = AccountType.PRO_II,
        storage = 8192,
        monthlyTransfer = 8192,
        yearlyTransfer = 98304,
        monthlyAmount = CurrencyAmount(19.99.toFloat(), Currency("EUR")),
        yearlyAmount = CurrencyAmount(
            199.99.toFloat(),
            Currency("EUR")
        ),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    private val localisedSubscriptionProIII = LocalisedSubscription(
        accountType = AccountType.PRO_III,
        storage = 16384,
        monthlyTransfer = 16384,
        yearlyTransfer = 196608,
        monthlyAmount = CurrencyAmount(29.99.toFloat(), Currency("EUR")),
        yearlyAmount = CurrencyAmount(
            299.99.toFloat(),
            Currency("EUR")
        ),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    private val localisedSubscriptionProLite = LocalisedSubscription(
        accountType = AccountType.PRO_LITE,
        storage = 400,
        monthlyTransfer = 1024,
        yearlyTransfer = 12288,
        monthlyAmount = CurrencyAmount(4.99.toFloat(), Currency("EUR")),
        yearlyAmount = CurrencyAmount(
            49.99.toFloat(),
            Currency("EUR")
        ),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    private val expectedLocalisedSubscriptionsList = listOf(
        localisedSubscriptionProLite,
        localisedSubscriptionProI,
        localisedSubscriptionProII,
        localisedSubscriptionProIII
    )
}