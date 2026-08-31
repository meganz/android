package mega.privacy.android.a11ytest.sweep

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.R as CoreUiR
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.button.SecondaryFilledButton
import mega.android.core.ui.components.prompt.ErrorPrompt
import mega.android.core.ui.components.prompt.SuccessPrompt
import mega.android.core.ui.components.prompt.TransparentPrompt
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.text.SpannableText
import mega.privacy.android.a11ytest.AccessibilityCheckReporter
import mega.privacy.android.a11ytest.megaAccessibilityValidator
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility baseline sweep of the design-system empty state view and prompt banners.
 */
@RunWith(AndroidJUnit4::class)
class EmptyStatesAndPromptsAccessibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val reporter =
        AccessibilityCheckReporter(reportName = "EmptyStatesAndPromptsAccessibilityTest")

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks(megaAccessibilityValidator(reporter))
    }

    @Test
    fun test_that_empty_state_view_with_actions_passes_accessibility_checks() {
        composeTestRule.setSweepContent {
            EmptyStateView(
                imagePainter = painterResource(CoreUiR.drawable.ic_close_medium_thin_outline),
                title = "No contacts yet",
                description = SpannableText("Invite people to start sharing files."),
                primaryAction = {
                    PrimaryFilledButton(
                        modifier = Modifier,
                        text = "Invite contacts",
                        onClick = {},
                    )
                },
                secondaryAction = {
                    SecondaryFilledButton(
                        modifier = Modifier,
                        text = "Scan QR code",
                        onClick = {},
                    )
                },
            )
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun test_that_prompts_pass_accessibility_checks() {
        composeTestRule.setSweepContent {
            SuccessPrompt(message = "Link copied")
            ErrorPrompt(message = "No internet connection")
            TransparentPrompt(message = "Reconnecting")
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }
}
