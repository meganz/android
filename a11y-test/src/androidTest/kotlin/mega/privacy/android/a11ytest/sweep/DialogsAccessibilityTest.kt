package mega.privacy.android.a11ytest.sweep

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.android.core.ui.components.dialogs.BasicDialogRadioOption
import mega.android.core.ui.components.dialogs.BasicInputDialog
import mega.android.core.ui.components.dialogs.BasicRadioDialog
import mega.android.core.ui.components.text.SpannableText
import mega.privacy.android.a11ytest.AccessibilityCheckReporter
import mega.privacy.android.a11ytest.megaAccessibilityValidator
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility baseline sweep of the design-system dialog family, covering basic, input and
 * radio dialogs. Dialogs render in their own window, so checks are triggered from a node
 * inside the dialog rather than the host activity root.
 */
@RunWith(AndroidJUnit4::class)
class DialogsAccessibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val reporter = AccessibilityCheckReporter(reportName = "DialogsAccessibilityTest")

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks(megaAccessibilityValidator(reporter))
    }

    @Test
    fun test_that_basic_dialog_passes_accessibility_checks() {
        composeTestRule.setSweepContent {
            BasicDialog(
                title = "Discard changes?",
                positiveButtonText = "Discard",
                onPositiveButtonClicked = {},
                description = "Your unsaved edits will be lost.",
                negativeButtonText = "Cancel",
                onNegativeButtonClicked = {},
            )
        }

        composeTestRule.onNodeWithText("Discard changes?").tryPerformAccessibilityChecks()
    }

    @Test
    fun test_that_input_dialog_passes_accessibility_checks() {
        composeTestRule.setSweepContent {
            BasicInputDialog(
                title = "Rename file",
                onValueChange = {},
                positiveButtonText = "Rename",
                onPositiveButtonClicked = {},
                inputValue = "Report.pdf",
                inputLabel = "File name",
                negativeButtonText = "Cancel",
                onNegativeButtonClicked = {},
                isAutoShowKeyboard = false,
            )
        }

        composeTestRule.onNodeWithText("Rename file").tryPerformAccessibilityChecks()
    }

    @Test
    fun test_that_radio_dialog_passes_accessibility_checks() {
        val options = persistentListOf(
            BasicDialogRadioOption(ordinal = 0, text = "Name"),
            BasicDialogRadioOption(ordinal = 1, text = "Size"),
            BasicDialogRadioOption(ordinal = 2, text = "Date", enabled = false),
        )
        composeTestRule.setSweepContent {
            BasicRadioDialog(
                title = SpannableText("Sort by"),
                options = options,
                buttons = persistentListOf(
                    BasicDialogButton(text = "Cancel", onClick = {}),
                    BasicDialogButton(text = "Apply", onClick = {}),
                ),
                onDismissRequest = {},
                onOptionSelected = {},
                selectedOption = options.first(),
            )
        }

        composeTestRule.onNodeWithText("Sort by").tryPerformAccessibilityChecks()
    }
}
