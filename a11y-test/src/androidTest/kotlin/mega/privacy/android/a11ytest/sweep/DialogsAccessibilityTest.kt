package mega.privacy.android.a11ytest.sweep

import androidx.activity.ComponentActivity
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
import mega.privacy.android.a11ytest.ThemeReporters
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

    private val reporters = ThemeReporters("DialogsAccessibilityTest")

    @Test
    fun test_that_basic_dialog_passes_accessibility_checks() {
        val sweep = composeTestRule.setSweepContent(reporters) {
            BasicDialog(
                title = "Discard changes?",
                positiveButtonText = "Discard",
                onPositiveButtonClicked = {},
                description = "Your unsaved edits will be lost.",
                negativeButtonText = "Cancel",
                onNegativeButtonClicked = {},
            )
        }

        sweep.checkInEachTheme {
            onNodeWithText("Discard changes?").tryPerformAccessibilityChecks()
        }
    }

    @Test
    fun test_that_input_dialog_passes_accessibility_checks() {
        val sweep = composeTestRule.setSweepContent(reporters) {
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

        sweep.checkInEachTheme { onNodeWithText("Rename file").tryPerformAccessibilityChecks() }
    }

    @Test
    fun test_that_radio_dialog_passes_accessibility_checks() {
        val options = persistentListOf(
            BasicDialogRadioOption(ordinal = 0, text = "Name"),
            BasicDialogRadioOption(ordinal = 1, text = "Size"),
            BasicDialogRadioOption(ordinal = 2, text = "Date", enabled = false),
        )
        val sweep = composeTestRule.setSweepContent(reporters) {
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

        sweep.checkInEachTheme { onNodeWithText("Sort by").tryPerformAccessibilityChecks() }
    }
}
