package test.mega.privacy.android.app.presentation.advertisements.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.advertisements.model.AdsLoadState
import mega.privacy.android.app.presentation.advertisements.view.AdsWebView
import mega.privacy.android.app.presentation.advertisements.view.CLOSE_BUTTON_TEST_TAG
import mega.privacy.android.app.presentation.advertisements.view.WEB_VIEW_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config


@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
class AdsWebViewTest() {
    private val url = "https://megaad.nz/?z=44919&w=320&h=50&id=wphr&r=16910391477453148"

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that web view exists if uiState is Loaded with url`() {
        composeRule.setContent {
            AdsWebView(uiState = AdsLoadState.Loaded(url), {}, {})
        }
        composeRule.onNodeWithTag(WEB_VIEW_TEST_TAG).assertExists()
    }

    @Test
    fun `test that web view does not exist if uiState is Empty`() {
        composeRule.setContent {
            AdsWebView(uiState = AdsLoadState.Empty, {}, {})
        }
        composeRule.onNodeWithTag(WEB_VIEW_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that close button is showing`() {
        composeRule.setContent {
            AdsWebView(uiState = AdsLoadState.Loaded(url), {}, {})
        }
        composeRule.onNodeWithTag(CLOSE_BUTTON_TEST_TAG).assertIsDisplayed()
    }
}