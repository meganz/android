package mega.privacy.android.app.presentation.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.search.view.ChipItem
import mega.privacy.android.app.presentation.search.view.DropdownChipToolbar
import mega.privacy.android.app.presentation.search.view.TYPE_DROPDOWN_CHIP_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class DropdownChipToolbarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        isTypeFilterSelected: Boolean = false,
        typeFilterTitle: String = "",
        selectedTypeFilterTitle: String = "",
        onTypeFilterClicked: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            DropdownChipToolbar(
                listOf(
                    ChipItem(
                        isSelected = isTypeFilterSelected,
                        notSelectedTitle = typeFilterTitle,
                        selectedFilterTitle = selectedTypeFilterTitle,
                        onFilterClicked = onTypeFilterClicked,
                        testTag = TYPE_DROPDOWN_CHIP_TEST_TAG
                    )
                )
            )
        }
    }

    @Test
    fun `test that type filter is displayed correctly when filter is not selected`() {
        val typeTitle = "Type"
        val selectedTypeTitle = "Videos"
        setComposeContent(
            typeFilterTitle = typeTitle,
            selectedTypeFilterTitle = selectedTypeTitle,
        )

        composeTestRule.onNodeWithTag(TYPE_DROPDOWN_CHIP_TEST_TAG, true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(typeTitle, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that type filter is displayed correctly when filter is selected`() {
        val typeTitle = "Type"
        val selectedTypeTitle = "Videos"
        setComposeContent(
            isTypeFilterSelected = true,
            typeFilterTitle = typeTitle,
            selectedTypeFilterTitle = selectedTypeTitle,
        )

        composeTestRule.onNodeWithTag(TYPE_DROPDOWN_CHIP_TEST_TAG, true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(selectedTypeTitle, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that onClicked is invoked when filter is clicked`() {
        val onTypeFilterClicked: () -> Unit = Mockito.mock()
        setComposeContent(onTypeFilterClicked = onTypeFilterClicked)

        composeTestRule.onNodeWithTag(TYPE_DROPDOWN_CHIP_TEST_TAG, true).performClick()
        verify(onTypeFilterClicked).invoke()
    }
}
