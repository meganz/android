package mega.privacy.android.app.presentation.videosection

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.videosection.model.VideosFilterOptionEntity
import mega.privacy.android.app.presentation.videosection.view.allvideos.VideosFilterBottomSheet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalMaterialApi::class)
@RunWith(AndroidJUnit4::class)
class VideosFilterBottomSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        modifier: Modifier = Modifier,
        title: String = "",
        options: List<VideosFilterOptionEntity> = emptyList(),
        onItemSelected: (VideosFilterOptionEntity) -> Unit = {},
    ) {
        composeTestRule.setContent {
            val sheetState = ModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Expanded,
                isSkipHalfExpanded = false,
                density = LocalDensity.current,
            )
            val coroutineScope = rememberCoroutineScope()
            VideosFilterBottomSheet(
                modifier = modifier,
                modalSheetState = sheetState,
                coroutineScope = coroutineScope,
                title = title,
                options = options,
                onItemSelected = onItemSelected
            )
        }
    }

    @Test
    fun `test that title is displayed correctly`() {
        val title = "Title"
        setComposeContent(title = title)
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun `test that titles of filter options are displayed correctly`() {
        val option1 = VideosFilterOptionEntity(0, "Option 1", false)
        val option2 = VideosFilterOptionEntity(1, "Option 2", true)
        val options = listOf(option1, option2)
        setComposeContent(options = options)
        composeTestRule.onNodeWithText(option1.title).assertIsDisplayed()
        composeTestRule.onNodeWithText(option1.title).assertIsDisplayed()
    }

    @Test
    fun `test that onItemSelected is invoked when option is clicked`() {
        val onItemSelected: (VideosFilterOptionEntity) -> Unit = mock()
        val option1 = VideosFilterOptionEntity(0, "Option 1", false)
        val option2 = VideosFilterOptionEntity(1, "Option 2", true)
        val options = listOf(option1, option2)
        setComposeContent(options = options, onItemSelected = onItemSelected)

        composeTestRule.onNodeWithText(option1.title).performClick()
        verify(onItemSelected).invoke(option1)
    }
}