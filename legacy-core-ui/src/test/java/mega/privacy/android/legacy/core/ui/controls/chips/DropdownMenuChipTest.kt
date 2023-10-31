package mega.privacy.android.legacy.core.ui.controls.chips

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DropdownMenuChipTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `test that text field chip is disabled`() {
        composeTestRule.setContent {
            DropdownMenuChip(
                options = listOf("day", "week", "month"),
                initialSelectedOption = "day",
                onOptionSelected = { },
                iconId = null,
                isDisabled = true,
                onDropdownExpanded = { }
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_DROPDOWN_MENU_CHIP).assertExists()
    }

    @Test
    fun `test that text field chip is enabled with option day selected`() {
        composeTestRule.setContent {
            DropdownMenuChip(
                options = listOf("day", "week", "month"),
                initialSelectedOption = "day",
                onOptionSelected = { },
                iconId = null,
                isDisabled = false,
                onDropdownExpanded = { }
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_DROPDOWN_MENU_CHIP).assertExists()
    }

    @Test
    fun `test that text field chip has week option selected`() {
        composeTestRule.setContent {
            DropdownMenuChip(
                options = listOf("day", "week", "month"),
                initialSelectedOption = "week",
                onOptionSelected = { },
                iconId = null,
                isDisabled = false,
                onDropdownExpanded = { }
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_DROPDOWN_MENU_CHIP).assertExists()
    }

    @Test
    fun `test that text field chip has month option selected`() {
        composeTestRule.setContent {
            DropdownMenuChip(
                options = listOf("day", "week", "month"),
                initialSelectedOption = "month",
                onOptionSelected = { },
                iconId = null,
                isDisabled = false,
                onDropdownExpanded = { }
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_DROPDOWN_MENU_CHIP).assertExists()
    }
}