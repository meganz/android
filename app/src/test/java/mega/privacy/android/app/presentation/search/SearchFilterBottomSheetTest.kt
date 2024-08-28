package mega.privacy.android.app.presentation.search

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.search.model.FilterOptionEntity
import mega.privacy.android.app.presentation.search.view.BottomSheetContentLayout
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import mega.privacy.android.app.fromId

@RunWith(AndroidJUnit4::class)
class SearchFilterBottomSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        modifier: Modifier = Modifier,
        title: String = "",
        options: List<FilterOptionEntity> = emptyList(),
        onItemSelected: (FilterOptionEntity) -> Unit = {},
    ) {
        composeTestRule.setContent {
            BottomSheetContentLayout(
                modifier = modifier,
                title = title,
                options = options,
                onItemSelected = onItemSelected
            )
        }
    }

    @Test
    fun `test that title is displayed correctly`() {
        val title = "Types"
        setComposeContent(title = title)
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun `test that filter options are displayed correctly`() {
        val docs = FilterOptionEntity(
            0,
            R.string.search_dropdown_chip_filter_type_file_type_documents,
            false
        )
        val videos =
            FilterOptionEntity(1, R.string.search_dropdown_chip_filter_type_file_type_video, true)
        val photos =
            FilterOptionEntity(2, R.string.search_dropdown_chip_filter_type_file_type_images, false)
        val options = listOf(docs, videos, photos)
        setComposeContent(options = options)
        composeTestRule.onNodeWithText(fromId(docs.title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(fromId(videos.title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(fromId(photos.title)).assertIsDisplayed()
    }

    @Test
    fun `test that onItemSelected is invoked when filter option is clicked`() {
        val onItemSelected: (FilterOptionEntity) -> Unit = mock()
        val docs = FilterOptionEntity(
            0,
            R.string.search_dropdown_chip_filter_type_file_type_documents,
            false
        )
        val videos =
            FilterOptionEntity(1, R.string.search_dropdown_chip_filter_type_file_type_video, true)
        val photos =
            FilterOptionEntity(2, R.string.search_dropdown_chip_filter_type_file_type_images, false)
        val options = listOf(docs, videos, photos)
        setComposeContent(options = options, onItemSelected = onItemSelected)

        composeTestRule.onNodeWithText(fromId(docs.title)).performClick()
        verify(onItemSelected).invoke(docs)
    }
}
