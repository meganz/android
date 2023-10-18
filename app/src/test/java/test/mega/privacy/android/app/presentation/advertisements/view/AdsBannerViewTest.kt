package test.mega.privacy.android.app.presentation.advertisements.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.advertisements.model.AdsUIState
import mega.privacy.android.app.presentation.advertisements.view.AdsBannerView
import mega.privacy.android.app.presentation.advertisements.view.CLOSE_BUTTON_TEST_TAG
import mega.privacy.android.app.presentation.advertisements.view.WEB_VIEW_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import test.mega.privacy.android.app.hasDrawable


@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
class AdsBannerViewTest {
    private val url = "https://megaad.nz/?z=44919&w=320&h=50&id=wphr&r=16910391477453148"

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that ads banner view items showing when showAdsView is true`() {
        composeRule.setContent {
            AdsBannerView(
                uiState = AdsUIState(adsBannerUrl = url, showAdsView = true),
                onAdClicked = {},
                onAdsWebpageLoaded = {},
                onAdDismissed = {})
        }
        composeRule.onNodeWithTag(WEB_VIEW_TEST_TAG).assertExists()
        composeRule.onNodeWithTag(CLOSE_BUTTON_TEST_TAG).assertIsDisplayed()
        composeRule.onNode(hasDrawable(R.drawable.ic_ads_close)).assertIsDisplayed()
    }

    @Test
    fun `test that ads banner view does not exist if showAdsView is false`() {
        composeRule.setContent {
            AdsBannerView(
                uiState = AdsUIState(adsBannerUrl = "", showAdsView = false),
                onAdClicked = {},
                onAdsWebpageLoaded = {},
                onAdDismissed = {})
        }
        composeRule.onNodeWithTag(WEB_VIEW_TEST_TAG).assertDoesNotExist()
    }
}