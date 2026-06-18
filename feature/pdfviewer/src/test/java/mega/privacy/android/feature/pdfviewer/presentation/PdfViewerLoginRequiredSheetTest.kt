package mega.privacy.android.feature.pdfviewer.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

/**
 * Compose UI tests for [PdfViewerLoginRequiredSheet].
 *
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class PdfViewerLoginRequiredSheetTest {

    private val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    private val onSignupClicked = mock<() -> Unit>()
    private val onLoginClicked = mock<() -> Unit>()
    private val onDismissSheet = mock<() -> Unit>()

    private fun setContent(isFileLink: Boolean) {
        composeTestRule.setContent {
            PdfViewerLoginRequiredSheet(
                isFileLink = isFileLink,
                onSignupClicked = onSignupClicked,
                onLoginClicked = onLoginClicked,
                onDismissSheet = onDismissSheet,
            )
        }
    }

    @Test
    fun `test that the file-link variant shows the default description`() {
        setContent(isFileLink = true)

        val description = composeTestRule.activity.getString(
            sharedR.string.public_link_auth_alert_description_default
        )
        composeTestRule.onNodeWithText(description).assertIsDisplayed()
    }

    @Test
    fun `test that the external-file variant shows the external file description`() {
        setContent(isFileLink = false)

        val description = composeTestRule.activity.getString(
            sharedR.string.public_link_auth_alert_description_external_file
        )
        composeTestRule.onNodeWithText(description).assertIsDisplayed()
    }

    @Test
    fun `test that onSignupClicked is invoked when create account button is clicked`() {
        setContent(isFileLink = true)

        val signupLabel =
            composeTestRule.activity.getString(sharedR.string.general_label_create_account)
        composeTestRule.onNodeWithText(signupLabel).performClick()

        verify(onSignupClicked).invoke()
    }

    @Test
    fun `test that onLoginClicked is invoked when log in button is clicked`() {
        setContent(isFileLink = true)

        val loginLabel = composeTestRule.activity.getString(sharedR.string.login_text)
        composeTestRule.onNodeWithText(loginLabel).performClick()

        verify(onLoginClicked).invoke()
    }
}
