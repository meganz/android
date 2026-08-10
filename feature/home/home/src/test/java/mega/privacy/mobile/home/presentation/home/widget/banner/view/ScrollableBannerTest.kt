package mega.privacy.mobile.home.presentation.home.widget.banner.view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.model.LocalizedText
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.domain.entity.banner.PromotionalBanner
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.home.presentation.home.widget.banner.mapper.SubscriptionOfferBannerMapper.Companion.SUBSCRIPTION_OFFER_BANNER_ID
import mega.privacy.mobile.home.presentation.home.widget.banner.mapper.SubscriptionOfferBannerMapper.Companion.SUBSCRIPTION_OFFER_BANNER_URL
import mega.privacy.mobile.home.presentation.home.widget.banner.model.SubscriptionOfferBannerUiModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScrollableBannerTest {

    @get:Rule
    var composeRule = createComposeRule()

    @get:Rule
    val analyticsRule = AnalyticsTestRule()

    private val banner1 = PromotionalBanner(
        id = 1,
        title = "Get 5 GB extra with our password manager",
        buttonText = "Try it now",
        image = "https://example.com/image1.png",
        backgroundImage = "https://example.com/bg1.png",
        url = "https://mega.nz/password-manager",
        imageLocation = "right"
    )

    private val banner2 = PromotionalBanner(
        id = 2,
        title = "MEGA VPN is included in your plan",
        buttonText = "Learn more",
        image = "https://example.com/image2.png",
        backgroundImage = "https://example.com/bg2.png",
        url = "https://mega.nz/vpn",
        imageLocation = "right"
    )

    private val offerBanner = SubscriptionOfferBannerUiModel(
        campaignName = LocalizedText.Literal("Black Friday"),
        discountPercentage = 50,
        formattedPrice = "€4.99",
        planNameRes = sharedR.string.pro1_account,
        validUntil = 1_785_000_000L,
    )

    @Test
    fun `test that banner title is displayed`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                ScrollableBanner(
                    offerBanner = null,
                    banners =listOf(banner1),
                    onDismiss = { _, _ -> },
                    onClick = {},
                )
            }
        }

        composeRule.onNodeWithText(banner1.title).assertIsDisplayed()
    }

    @Test
    fun `test that banner button text is displayed`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                ScrollableBanner(
                    offerBanner = null,
                    banners =listOf(banner1),
                    onDismiss = { _, _ -> },
                    onClick = {},
                )
            }
        }

        composeRule.onNodeWithText(banner1.buttonText).assertIsDisplayed()
    }

    @Test
    fun `test that multiple banners are displayed`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                ScrollableBanner(
                    offerBanner = null,
                    banners =listOf(banner1, banner2),
                    onDismiss = { _, _ -> },
                    onClick = {},
                )
            }
        }

        composeRule.onNodeWithText(banner1.title).assertIsDisplayed()
        composeRule.onNodeWithText(banner2.title).assertIsDisplayed()
    }

    @Test
    fun `test that onClick is called when banner button is clicked`() {
        var clickedUrl: String? = null

        composeRule.setContent {
            AndroidThemeForPreviews {
                ScrollableBanner(
                    offerBanner = null,
                    banners =listOf(banner1),
                    onDismiss = { _, _ -> },
                    onClick = { url -> clickedUrl = url },
                )
            }
        }

        composeRule.onNodeWithText(banner1.buttonText).performClick()

        assertThat(clickedUrl).isEqualTo(banner1.url)
    }

    @Test
    fun `test that clickable modifier on HomeBanner works correctly`() {
        var clickCount = 0
        var clickedUrl: String? = null

        composeRule.setContent {
            AndroidThemeForPreviews {
                ScrollableBanner(
                    offerBanner = null,
                    banners =listOf(banner1),
                    onDismiss = { _, _ -> },
                    onClick = { url ->
                        clickCount++
                        clickedUrl = url
                    },
                )
            }
        }
        composeRule.onNodeWithText(banner1.title).performClick()

        assertThat(clickedUrl).isEqualTo(banner1.url)
        assertThat(clickCount).isEqualTo(1) // This might reveal the double-click issue if it exists
    }

    @Test
    fun `test that dismiss button triggers onDismiss instead of onClick`() {
        var dismissedBannerId: Int? = null
        var dismissedBannerUrl: String? = null
        var clickedUrl: String? = null

        composeRule.setContent {
            AndroidThemeForPreviews {
                ScrollableBanner(
                    offerBanner = null,
                    banners =listOf(banner1),
                    onDismiss = { id, url ->
                        dismissedBannerId = id
                        dismissedBannerUrl = url
                    },
                    onClick = { url -> clickedUrl = url },
                )
            }
        }

        // Click on the dismiss button using content description
        composeRule.onNodeWithContentDescription("Dismiss").performClick()

        assertThat(dismissedBannerId).isEqualTo(banner1.id)
        assertThat(dismissedBannerUrl).isEqualTo(banner1.url)

        // Verify onClick was NOT called
        assertThat(clickedUrl).isNull()
    }

    @Test
    fun `test that empty list does not display any banners`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                ScrollableBanner(
                    offerBanner = null,
                    banners =emptyList(),
                    onDismiss = { _, _ -> },
                    onClick = {},
                )
            }
        }

        composeRule.onNodeWithText(banner1.title).assertDoesNotExist()
        composeRule.onNodeWithText(banner2.title).assertDoesNotExist()
    }

    @Test
    fun `test that the offer banner displays its resolved headline and button`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                ScrollableBanner(
                    offerBanner = offerBanner,
                    banners = emptyList(),
                    onDismiss = { _, _ -> },
                    onClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Black Friday · Get 50% off", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("€4.99/month for Pro I", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Grab deal").assertIsDisplayed()
    }

    /**
     * The offer loads after the promo banners, so it is prepended to a list the LazyRow has already
     * anchored to the first promo banner's key, which would leave the offer off-screen to the left.
     */
    @Test
    fun `test that the offer banner is scrolled into view when it loads after the promo banners`() {
        var offer by mutableStateOf<SubscriptionOfferBannerUiModel?>(null)

        composeRule.setContent {
            AndroidThemeForPreviews {
                ScrollableBanner(
                    offerBanner = offer,
                    banners = listOf(banner1, banner2),
                    onDismiss = { _, _ -> },
                    onClick = {},
                )
            }
        }
        composeRule.onNodeWithText(banner1.title).assertIsDisplayed()

        offer = offerBanner
        composeRule.waitForIdle()

        val offerBounds = composeRule
            .onNodeWithText("Black Friday · Get 50% off", substring = true)
            .getUnclippedBoundsInRoot()
        assertThat(offerBounds.left.value).isAtLeast(0f)
    }

    @Test
    fun `test that dismissing the offer banner reports the offer id and url`() {
        var dismissedBannerId: Int? = null
        var dismissedBannerUrl: String? = null

        composeRule.setContent {
            AndroidThemeForPreviews {
                ScrollableBanner(
                    offerBanner = offerBanner,
                    banners = emptyList(),
                    onDismiss = { id, url ->
                        dismissedBannerId = id
                        dismissedBannerUrl = url
                    },
                    onClick = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Dismiss offer").performClick()

        assertThat(dismissedBannerId).isEqualTo(SUBSCRIPTION_OFFER_BANNER_ID)
        assertThat(dismissedBannerUrl).isEqualTo(SUBSCRIPTION_OFFER_BANNER_URL)
    }
}
