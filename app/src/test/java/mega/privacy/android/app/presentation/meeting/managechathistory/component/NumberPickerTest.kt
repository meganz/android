package mega.privacy.android.app.presentation.meeting.managechathistory.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NumberPickerTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that component is displayed`() {
        composeRule.apply {
            setComponent()

            onNodeWithTag(NUMBER_PICKER_TAG).assertIsDisplayed()
        }
    }

    private fun ComposeContentTestRule.setComponent(
        minimumValue: Int = 0,
        maximumValue: Int = 0,
        currentValue: Int = 0,
        displayValues: List<String>? = null,
        isWrapSelectorWheel: Boolean = true,
        selectionDividerHeight: Dp = 1.dp,
        onScrollChange: ((scrollState: NumberPickerScrollState) -> Unit)? = null,
        onValueChange: ((oldValue: Int, newValue: Int) -> Unit)? = null,
    ) {
        setContent {
            NumberPicker(
                minimumValue = minimumValue,
                maximumValue = maximumValue,
                currentValue = currentValue,
                displayValues = displayValues,
                isWrapSelectorWheel = isWrapSelectorWheel,
                selectionDividerHeight = selectionDividerHeight,
                onScrollChange = onScrollChange,
                onValueChange = onValueChange
            )
        }
    }
}
