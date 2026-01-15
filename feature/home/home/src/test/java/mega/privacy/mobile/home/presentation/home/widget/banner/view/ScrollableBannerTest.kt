package mega.privacy.mobile.home.presentation.home.widget.banner.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.banner.PromotionalBanner
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScrollableBannerTest {

    @get:Rule
    var composeRule = createComposeRule()

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

    @Test
    fun `test that banner title is displayed`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                ScrollableBanner(
                    banners = listOf(banner1),
                    onDismiss = {},
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
                    banners = listOf(banner1),
                    onDismiss = {},
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
                    banners = listOf(banner1, banner2),
                    onDismiss = {},
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
                    banners = listOf(banner1),
                    onDismiss = {},
                    onClick = { url -> clickedUrl = url },
                )
            }
        }

        composeRule.onNodeWithText(banner1.buttonText).performClick()

        assertThat(clickedUrl).isEqualTo(banner1.url)
    }

    @Test
    fun `test that empty list does not display any banners`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                ScrollableBanner(
                    banners = emptyList(),
                    onDismiss = {},
                    onClick = {},
                )
            }
        }

        composeRule.onNodeWithText(banner1.title).assertDoesNotExist()
        composeRule.onNodeWithText(banner2.title).assertDoesNotExist()
    }
}
