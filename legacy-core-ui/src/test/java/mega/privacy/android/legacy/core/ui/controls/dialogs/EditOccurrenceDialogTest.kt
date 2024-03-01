package mega.privacy.android.legacy.core.ui.controls.dialogs

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class EditOccurrenceDialogTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val onEditOccurrence = mock<() -> Unit>()
    private val onDismissDialog = mock<() -> Unit>()
    private val onDateTap = mock<() -> Unit>()
    private val onStartTimeTap = mock<() -> Unit>()
    private val onEndTimeTap = mock<() -> Unit>()
    private val onUpgradeNowClicked = mock<() -> Unit>()

    @Test
    fun `test that call limitation warning is shown`() {
        initComposeRule(
            shouldShowFreePlanLimitWarning = true
        )
        composeRule.onNodeWithTag(EDIT_OCCURRENCE_DIALOG_WARNING_TAG).assertExists()
    }

    @Test
    fun `test that call limitation warning is hidden`() {
        initComposeRule(
            shouldShowFreePlanLimitWarning = false
        )
        composeRule.onNodeWithTag(EDIT_OCCURRENCE_DIALOG_WARNING_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that call limitation warning is clicked`() {
        initComposeRule(
            shouldShowFreePlanLimitWarning = true
        )
        composeRule.onNodeWithTag(EDIT_OCCURRENCE_DIALOG_WARNING_TAG).performClick()
    }

    private fun initComposeRule(
        shouldShowFreePlanLimitWarning: Boolean = false,
    ) {
        composeRule.setContent {
            EditOccurrenceDialog(
                title = "Title",
                confirmButtonText = "Confirm",
                cancelButtonText = "Cancel",
                dateTitleText = "Date title",
                dateText = "1 March",
                startTimeTitleText = "Start time",
                endTimeTitleText = "End time",
                startTimeText = "11:40",
                endTimeText = "12:40",
                freePlanLimitationWarningText = "Free plan limitations",
                shouldShowFreePlanLimitWarning = shouldShowFreePlanLimitWarning,
                onConfirm = onEditOccurrence,
                onDismiss = onDismissDialog,
                isConfirmButtonEnabled = true,
                isDateEdited = true,
                isStartTimeEdited = true,
                isEndTimeEdited = true,
                onDateTap = onDateTap,
                onStartTimeTap = onStartTimeTap,
                onEndTimeTap = onEndTimeTap,
                onUpgradeNowClicked = onUpgradeNowClicked,
            )
        }
    }
}