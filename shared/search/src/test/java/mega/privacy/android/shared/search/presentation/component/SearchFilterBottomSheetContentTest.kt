package mega.privacy.android.shared.search.presentation.component

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.model.LocalizedText
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.search.presentation.model.SearchFilterOption
import mega.privacy.android.shared.search.presentation.model.SearchFilterOptions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class SearchFilterBottomSheetContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun options(selectedOptionId: String? = null) = SearchFilterOptions(
        id = FILTER_ID,
        title = LocalizedText.Literal("File type"),
        options = OPTION_IDS.map { SearchFilterOption(it, LocalizedText.Literal(it)) },
        selectedOptionId = selectedOptionId,
    )

    @Test
    fun `test that the title is displayed`() {
        setupComposeContent(filterOptions = options())

        composeRule.onNodeWithTag(SEARCH_FILTER_BOTTOM_SHEET_TITLE_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that every option is displayed`() {
        setupComposeContent(filterOptions = options())
        composeRule.waitForIdle()

        OPTION_IDS.forEach { id ->
            composeRule.onNodeWithTag("${SEARCH_FILTER_OPTION_TAG}_$id", useUnmergedTree = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun `test that selecting an unselected option emits the filter id and option id`() {
        val onOptionSelected = mock<(String, String?) -> Unit>()
        setupComposeContent(filterOptions = options(), onOptionSelected = onOptionSelected)

        composeRule.onNodeWithTag(
            "${SEARCH_FILTER_OPTION_TAG}_$UNSELECTED_OPTION_ID",
            useUnmergedTree = true
        )
            .performClick()
        composeRule.waitForIdle()

        verify(onOptionSelected).invoke(FILTER_ID, UNSELECTED_OPTION_ID)
    }

    @Test
    fun `test that tapping the selected option clears it by emitting a null option id`() {
        val onOptionSelected = mock<(String, String?) -> Unit>()
        setupComposeContent(
            filterOptions = options(selectedOptionId = SELECTED_OPTION_ID),
            onOptionSelected = onOptionSelected,
        )

        composeRule.onNodeWithTag(
            "${SEARCH_FILTER_OPTION_TAG}_$SELECTED_OPTION_ID",
            useUnmergedTree = true
        )
            .performClick()
        composeRule.waitForIdle()

        verify(onOptionSelected).invoke(FILTER_ID, null)
    }

    private fun setupComposeContent(
        filterOptions: SearchFilterOptions,
        onOptionSelected: (String, String?) -> Unit = { _, _ -> },
    ) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                SearchFilterBottomSheetContent(
                    filterOptions = filterOptions,
                    onOptionSelected = onOptionSelected,
                )
            }
        }
    }

    private companion object {
        const val FILTER_ID = "type"
        const val UNSELECTED_OPTION_ID = "images"
        const val SELECTED_OPTION_ID = "documents"
        val OPTION_IDS = listOf("images", "documents", "audio")
    }
}
