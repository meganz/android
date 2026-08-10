package mega.privacy.mobile.home.presentation.home.widget.banner

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.domain.entity.banner.PromotionalBanner
import mega.privacy.android.domain.entity.billing.RecommendedSubscriptionOffer
import mega.privacy.android.domain.usecase.banner.DismissBannerUseCase
import mega.privacy.android.domain.usecase.banner.GetPromoBannersUseCase
import mega.privacy.android.domain.usecase.billing.MonitorSubscriptionOfferBannerClosedUseCase
import mega.privacy.android.domain.usecase.billing.MonitorSubscriptionOfferUseCase
import mega.privacy.android.domain.usecase.billing.SetSubscriptionOfferBannerClosedUseCase
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.home.presentation.home.widget.banner.mapper.SubscriptionOfferBannerMapper
import mega.privacy.mobile.home.presentation.home.widget.banner.mapper.SubscriptionOfferBannerMapper.Companion.SUBSCRIPTION_OFFER_BANNER_ID
import mega.privacy.mobile.home.presentation.home.widget.banner.model.SubscriptionOfferBannerUiModel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BannerWidgetViewModelTest {

    private lateinit var underTest: BannerWidgetViewModel
    private val getPromoBannersUseCase = mock<GetPromoBannersUseCase>()
    private val dismissBannerUseCase = mock<DismissBannerUseCase>()
    private val monitorSubscriptionOfferUseCase = mock<MonitorSubscriptionOfferUseCase>()
    private val monitorSubscriptionOfferBannerClosedUseCase =
        mock<MonitorSubscriptionOfferBannerClosedUseCase>()
    private val setSubscriptionOfferBannerClosedUseCase =
        mock<SetSubscriptionOfferBannerClosedUseCase>()
    private val subscriptionOfferBannerMapper = mock<SubscriptionOfferBannerMapper>()

    private val banner1 = PromotionalBanner(
        id = 1,
        title = "Test Banner 1",
        image = "image1.png",
        backgroundImage = "bg1.png",
        url = "https://test.com/1",
        imageLocation = "https://cdn.test.com/",
        buttonText = "Lear More"
    )

    private val banner2 = PromotionalBanner(
        id = 2,
        title = "Test Banner 2",
        image = "image2.png",
        backgroundImage = "bg2.png",
        url = "https://test.com/2",
        imageLocation = "https://cdn.test.com/",
        buttonText = "Lear More"
    )

    private val offerBanner = SubscriptionOfferBannerUiModel(
        campaignName = LocalizedText.Literal("Black Friday"),
        discountPercentage = 50,
        formattedPrice = "€4.99",
        planNameRes = sharedR.string.pro1_account,
        validUntil = 1_785_000_000L,
    )

    private val offerSubscription = Subscription(
        sku = "mega.android.pro1.onemonth",
        accountType = AccountType.PRO_I,
        handle = 1L,
        storage = 2048,
        transfer = 2048,
        amount = CurrencyAmount(9.99f, Currency("EUR")),
        discountedAmountMonthly = CurrencyAmount(4.99f, Currency("EUR")),
        discountedPercentage = 50,
        discountName = "Black Friday",
    )

    private val recommendedOffer = mock<RecommendedSubscriptionOffer> {
        on { subscription } doReturn offerSubscription
    }

    @BeforeEach
    fun setUp() {
        reset(
            dismissBannerUseCase,
            getPromoBannersUseCase,
            monitorSubscriptionOfferUseCase,
            monitorSubscriptionOfferBannerClosedUseCase,
            setSubscriptionOfferBannerClosedUseCase,
            subscriptionOfferBannerMapper,
        )
        stubOfferBannerNotClosed()
    }

    private fun initViewModel() {
        underTest = BannerWidgetViewModel(
            dismissBannerUseCase = dismissBannerUseCase,
            getPromoBannersUseCase = getPromoBannersUseCase,
            monitorSubscriptionOfferUseCase = monitorSubscriptionOfferUseCase,
            monitorSubscriptionOfferBannerClosedUseCase = monitorSubscriptionOfferBannerClosedUseCase,
            setSubscriptionOfferBannerClosedUseCase = setSubscriptionOfferBannerClosedUseCase,
            subscriptionOfferBannerMapper = subscriptionOfferBannerMapper,
        )
    }

    private fun stubNoOffer() {
        whenever(monitorSubscriptionOfferUseCase()).thenReturn(flowOf(Result.success(null)))
    }

    private fun stubOffer() {
        whenever(monitorSubscriptionOfferUseCase())
            .thenReturn(flowOf(Result.success(recommendedOffer)))
        whenever(subscriptionOfferBannerMapper(any(), any())).thenReturn(offerBanner)
    }

    private fun stubOfferFailure() {
        whenever(monitorSubscriptionOfferUseCase())
            .thenReturn(flowOf(Result.failure(RuntimeException("Billing error"))))
    }

    private fun stubOfferBannerNotClosed() {
        whenever(monitorSubscriptionOfferBannerClosedUseCase()).thenReturn(flowOf(false))
    }

    private fun stubOfferBannerClosed() {
        whenever(monitorSubscriptionOfferBannerClosedUseCase()).thenReturn(flowOf(true))
    }

    @Test
    fun `test that empty list is returned when no banners available`() = runTest {
        whenever(getPromoBannersUseCase()).thenReturn(emptyList())
        stubNoOffer()

        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.banners).isEmpty()
            assertThat(state.isLoading).isFalse()
        }
    }

    @Test
    fun `test that banners are loaded successfully`() = runTest {
        whenever(getPromoBannersUseCase()).thenReturn(listOf(banner1, banner2))
        stubNoOffer()

        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.banners).containsExactly(banner1, banner2).inOrder()
            assertThat(state.isLoading).isFalse()
        }
    }

    @Test
    fun `test that empty list is returned when loading fails`() = runTest {
        whenever(getPromoBannersUseCase()).thenThrow(RuntimeException("Network error"))
        stubNoOffer()

        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.banners).isEmpty()
            assertThat(state.isLoading).isFalse()
        }
    }

    @Test
    fun `test that subscription offer banner is shown ahead of promo banners when an offer is active`() =
        runTest {
            whenever(getPromoBannersUseCase()).thenReturn(listOf(banner1))
            stubOffer()

            initViewModel()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.offerBanner).isEqualTo(offerBanner)
                assertThat(state.banners).containsExactly(banner1)
            }
        }

    @Test
    fun `test that recommended discounted subscription backs the offer banner`() = runTest {
        whenever(getPromoBannersUseCase()).thenReturn(emptyList())
        stubOffer()

        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.offerBanner).isEqualTo(offerBanner)
            assertThat(state.banners).isEmpty()
        }
        verify(subscriptionOfferBannerMapper).invoke(eq(offerSubscription), any())
    }

    @Test
    fun `test that offer banner is not shown when there is no recommended subscription`() = runTest {
        whenever(getPromoBannersUseCase()).thenReturn(listOf(banner1))
        stubNoOffer()

        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.offerBanner).isNull()
            assertThat(state.banners).containsExactly(banner1)
        }
        verifyNoInteractions(subscriptionOfferBannerMapper)
    }

    @Test
    fun `test that promo banners are still shown when loading the subscription offer fails`() =
        runTest {
            whenever(getPromoBannersUseCase()).thenReturn(listOf(banner1))
            stubOfferFailure()

            initViewModel()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.banners).containsExactly(banner1)
                assertThat(state.isLoading).isFalse()
            }
        }

    @Test
    fun `test that dismissBanner calls use case`() = runTest {
        whenever(getPromoBannersUseCase()).thenReturn(listOf(banner1, banner2))
        stubNoOffer()
        whenever(dismissBannerUseCase(1)).thenReturn(Unit)

        initViewModel()

        underTest.dismissBanner(1)

        verify(dismissBannerUseCase).invoke(1)
    }

    @Test
    fun `test that dismissBanner removes banner from list`() = runTest {
        whenever(getPromoBannersUseCase()).thenReturn(listOf(banner1, banner2))
        stubNoOffer()
        whenever(dismissBannerUseCase(1)).thenReturn(Unit)

        initViewModel()

        underTest.uiState.test {
            // Skip initial state
            awaitItem()

            underTest.dismissBanner(1)

            val state = awaitItem()
            assertThat(state.banners).containsExactly(banner2)
            assertThat(state.isLoading).isFalse()
        }
    }

    @Test
    fun `test that dismissing the offer banner removes it locally without calling the banners API`() =
        runTest {
            whenever(getPromoBannersUseCase()).thenReturn(listOf(banner1))
            stubOffer()

            initViewModel()

            underTest.uiState.test {
                // Skip initial state
                awaitItem()

                underTest.dismissBanner(SUBSCRIPTION_OFFER_BANNER_ID)

                val state = awaitItem()
                assertThat(state.offerBanner).isNull()
                assertThat(state.banners).containsExactly(banner1)
            }
            verifyNoInteractions(dismissBannerUseCase)
        }

    @Test
    fun `test that dismissing the offer banner persists that it is closed`() = runTest {
        whenever(getPromoBannersUseCase()).thenReturn(emptyList())
        stubOffer()

        initViewModel()

        underTest.dismissBanner(SUBSCRIPTION_OFFER_BANNER_ID)

        verify(setSubscriptionOfferBannerClosedUseCase).invoke()
    }

    @Test
    fun `test that the offer banner is removed even when persisting it fails`() = runTest {
        whenever(getPromoBannersUseCase()).thenReturn(listOf(banner1))
        stubOffer()
        whenever(setSubscriptionOfferBannerClosedUseCase())
            .thenThrow(RuntimeException("Datastore error"))

        initViewModel()

        underTest.uiState.test {
            // Skip initial state
            awaitItem()

            underTest.dismissBanner(SUBSCRIPTION_OFFER_BANNER_ID)

            val state = awaitItem()
            assertThat(state.offerBanner).isNull()
            assertThat(state.banners).containsExactly(banner1)
        }
    }

    @Test
    fun `test that offer banner is not shown when the user has already closed it`() = runTest {
        whenever(getPromoBannersUseCase()).thenReturn(listOf(banner1))
        stubOfferBannerClosed()

        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.offerBanner).isNull()
            assertThat(state.banners).containsExactly(banner1)
        }
        verifyNoInteractions(monitorSubscriptionOfferUseCase)
        verifyNoInteractions(subscriptionOfferBannerMapper)
    }

    @Test
    fun `test that the offer banner is removed when the offer goes away after an upgrade`() =
        runTest {
            whenever(getPromoBannersUseCase()).thenReturn(listOf(banner1))
            val offers = MutableStateFlow<Result<RecommendedSubscriptionOffer?>>(
                Result.success(recommendedOffer)
            )
            whenever(monitorSubscriptionOfferUseCase()).thenReturn(offers)
            whenever(subscriptionOfferBannerMapper(any(), any())).thenReturn(offerBanner)

            initViewModel()

            underTest.uiState.test {
                assertThat(awaitItem().offerBanner).isEqualTo(offerBanner)

                offers.value = Result.success(null)

                val state = awaitItem()
                assertThat(state.offerBanner).isNull()
                assertThat(state.banners).containsExactly(banner1)
            }
        }

    @Test
    fun `test that dismissing last banner results in empty list`() = runTest {
        whenever(getPromoBannersUseCase()).thenReturn(listOf(banner1))
        stubNoOffer()
        whenever(dismissBannerUseCase(1)).thenReturn(Unit)

        initViewModel()

        underTest.uiState.test {
            // Skip initial state
            awaitItem()

            underTest.dismissBanner(1)

            val state = awaitItem()
            assertThat(state.banners).isEmpty()
            assertThat(state.isLoading).isFalse()
        }
    }

    @Test
    fun `test that dismissBanner failure keeps current list`() = runTest {
        whenever(getPromoBannersUseCase()).thenReturn(listOf(banner1, banner2))
        stubNoOffer()
        whenever(dismissBannerUseCase(1)).thenThrow(RuntimeException("Dismiss failed"))

        initViewModel()

        underTest.uiState.test {
            val initialState = awaitItem()

            underTest.dismissBanner(1)

            // State should remain unchanged
            expectNoEvents()
            assertThat(underTest.uiState.value).isEqualTo(initialState)
        }
    }
}
