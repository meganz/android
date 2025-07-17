package mega.privacy.android.core.nodecomponents.sheet.sort

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SortBottomSheetViewTest {
    @get:Rule
    var composeRule = createComposeRule()

    private enum class FakeSortOption(
        override val key: String,
        override val displayName: String,
        override val testTag: String = "sort_option:$key"
    ) : SortOptionItem {
        Name("name", "Name"),
        Date("date", "Date"),
        Size("size", "Size"),
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun setContent(
        title: String = "",
        options: List<FakeSortOption> = listOf(),
        selectedSort: SortBottomSheetResult<FakeSortOption>? = null,
        onSortOptionSelected: (SortBottomSheetResult<FakeSortOption>?) -> Unit = {},
    ) {
        composeRule.setContent {
            val sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { true }
            )

            LaunchedEffect(Unit) {
                sheetState.show()
            }

            SortBottomSheet(
                sheetState = sheetState,
                title = title,
                options = options,
                selectedSort = selectedSort,
                onSortOptionSelected = onSortOptionSelected
            )
        }
    }

    @Test
    fun `test that title is displayed correctly`() {
        val title = "Sort Options"

        setContent(title = title)

        composeRule.onNodeWithTag(SORT_BOTTOM_SHEET_TITLE_TAG)
            .assertIsDisplayed()
            .assert(hasText(title))
    }

    @Test
    fun `test that sort options are displayed correctly`() {
        val options = FakeSortOption.entries

        setContent(options = options)

        options.forEach { option ->
            composeRule.onNodeWithText(option.displayName)
                .assertIsDisplayed()
        }
    }
}