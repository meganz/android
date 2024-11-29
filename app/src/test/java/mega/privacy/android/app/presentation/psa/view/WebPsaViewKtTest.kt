package mega.privacy.android.app.presentation.psa.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.psa.model.PsaJavascriptInterface
import mega.privacy.android.app.presentation.psa.model.PsaState
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class WebPsaViewKtTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that web view is not displayed if show psa has not been called`() {
        val contentTag = "content"
        composeTestRule.setContent {
            WebPsaView(
                psa = PsaState.WebPsa(
                    id = 0,
                    url = "https://megaad.nz/psa/test1.html"
                ),
                content = {
                    MegaText(
                        "Content goes here",
                        textColor = TextColor.Primary,
                        modifier = Modifier.testTag(contentTag)
                    )
                },
                markAsSeen = {},
            )
        }

        composeTestRule.onNodeWithTag(contentTag).assertIsDisplayed()
        composeTestRule.onNodeWithTag(WebPsaTag).assertIsDisplayed()
        composeTestRule.onNodeWithTag(WebPsaWebViewTag).assertIsNotDisplayed()
    }

    @Test
    fun `test that web view is displayed after show psa is called`() {
        val contentTag = "content"
        var jsInterface: PsaJavascriptInterface? = null
        composeTestRule.setContent {
            WebPsaView(
                psa = PsaState.WebPsa(
                    id = 0,
                    url = "https://megaad.nz/psa/test1.html"
                ),
                content = {
                    MegaText(
                        "Content goes here",
                        textColor = TextColor.Primary,
                        modifier = Modifier.testTag(contentTag)
                    )
                },
                markAsSeen = {},
                javascriptInterfaceFactory = { show, hide ->
                    jsInterface = PsaJavascriptInterface(
                        onShowPsa = show,
                        onHidePsa = hide
                    )
                    jsInterface!!
                }
            )
        }
        jsInterface?.showPsa()

        composeTestRule.onNodeWithTag(WebPsaWebViewTag).assertIsDisplayed()
    }

    @Test
    fun `test that back handler calls mark as seen if web view is visible`() {
        val contentTag = "content"
        var jsInterface: PsaJavascriptInterface? = null
        val markAsSeen = mock<() -> Unit>()
        composeTestRule.setContent {
            WebPsaView(
                psa = PsaState.WebPsa(
                    id = 0,
                    url = "https://megaad.nz/psa/test1.html"
                ),
                content = {
                    MegaText(
                        "Content goes here",
                        textColor = TextColor.Primary,
                        modifier = Modifier.testTag(contentTag)
                    )
                },
                markAsSeen = markAsSeen,
                javascriptInterfaceFactory = { show, hide ->
                    jsInterface = PsaJavascriptInterface(
                        onShowPsa = show,
                        onHidePsa = hide
                    )
                    jsInterface!!
                }
            )
        }
        jsInterface?.showPsa()

        composeTestRule.onNodeWithTag(WebPsaWebViewTag).assertIsDisplayed()

        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }

        verify(markAsSeen).invoke()
    }

    @Test
    fun `tet that back handler does not call mark as seen if web view is not visible`() {
        val contentTag = "content"
        val markAsSeen = mock<() -> Unit>()
        composeTestRule.setContent {
            WebPsaView(
                psa = PsaState.WebPsa(
                    id = 0,
                    url = "https://megaad.nz/psa/test1.html"
                ),
                content = {
                    MegaText(
                        "Content goes here",
                        textColor = TextColor.Primary,
                        modifier = Modifier.testTag(contentTag)
                    )
                },
                markAsSeen = markAsSeen,
            )
        }

        composeTestRule.onNodeWithTag(WebPsaWebViewTag).assertIsNotDisplayed()

        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }

        verifyNoInteractions(markAsSeen)
    }
}