package mega.privacy.android.feature.sharelink.presentation.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class MegaDatePickerDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that the dialog is displayed`() {
        setContent()

        composeRule.onNodeWithTag(MEGA_DATE_PICKER_DIALOG_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that confirm is disabled when no date is selected`() {
        setContent(initialSelectedTimeMillis = null)

        composeRule.onNodeWithTag(MEGA_DATE_PICKER_CONFIRM_TAG).assertIsNotEnabled()
    }

    @Test
    fun `test that confirm is enabled when a date is pre-selected`() {
        setContent(initialSelectedTimeMillis = FUTURE_UTC_MIDNIGHT)

        composeRule.onNodeWithTag(MEGA_DATE_PICKER_CONFIRM_TAG).assertIsEnabled()
    }

    @Test
    fun `test that confirming a pre-selected date invokes onDateSelected with its millis`() {
        var selected: Long? = null
        setContent(initialSelectedTimeMillis = FUTURE_UTC_MIDNIGHT, onDateSelected = { selected = it })

        composeRule.onNodeWithTag(MEGA_DATE_PICKER_CONFIRM_TAG).performClick()

        assertThat(selected).isEqualTo(FUTURE_UTC_MIDNIGHT)
    }

    @Test
    fun `test that tapping cancel invokes onDismiss`() {
        var dismissed = false
        setContent(onDismiss = { dismissed = true })

        composeRule.onNodeWithTag(MEGA_DATE_PICKER_DISMISS_TAG).performClick()

        assertThat(dismissed).isTrue()
    }

    private fun setContent(
        initialSelectedTimeMillis: Long? = null,
        onDateSelected: (Long) -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        composeRule.setContent {
            MegaDatePickerDialog(
                onDateSelected = onDateSelected,
                onDismiss = onDismiss,
                initialSelectedTimeMillis = initialSelectedTimeMillis,
            )
        }
    }

    private companion object {
        val FUTURE_UTC_MIDNIGHT: Long =
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(2035, Calendar.JANUARY, 1)
            }.timeInMillis
    }
}
