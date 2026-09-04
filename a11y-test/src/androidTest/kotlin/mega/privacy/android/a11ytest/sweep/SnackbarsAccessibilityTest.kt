package mega.privacy.android.a11ytest.sweep

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.components.snackbar.MegaSnackbar
import mega.privacy.android.a11ytest.AccessibilityCheckReporter
import mega.privacy.android.a11ytest.megaAccessibilityValidator
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility baseline sweep of the design-system snackbar. Snackbars are fleeting, so each
 * test shows one with an indefinite duration to keep it mounted while checks run.
 */
@RunWith(AndroidJUnit4::class)
class SnackbarsAccessibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val reporter = AccessibilityCheckReporter(reportName = "SnackbarsAccessibilityTest")

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks(megaAccessibilityValidator(reporter))
    }

    @Test
    fun test_that_snackbar_with_message_passes_accessibility_checks() {
        composeTestRule.setSweepContent {
            val hostState = remember { SnackbarHostState() }
            LaunchedEffect(hostState) {
                hostState.showSnackbar(
                    message = "File uploaded",
                    duration = SnackbarDuration.Indefinite,
                )
            }
            MegaSnackbar(snackBarHostState = hostState)
        }

        composeTestRule.onNodeWithText("File uploaded").tryPerformAccessibilityChecks()
    }

    @Test
    fun test_that_snackbar_with_action_passes_accessibility_checks() {
        composeTestRule.setSweepContent {
            val hostState = remember { SnackbarHostState() }
            LaunchedEffect(hostState) {
                hostState.showSnackbar(
                    message = "File moved to Rubbish bin",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Indefinite,
                )
            }
            MegaSnackbar(snackBarHostState = hostState)
        }

        composeTestRule.onNodeWithText("Undo").tryPerformAccessibilityChecks()
    }
}
