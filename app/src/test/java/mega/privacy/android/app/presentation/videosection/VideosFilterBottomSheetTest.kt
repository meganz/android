package mega.privacy.android.app.presentation.videosection

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import mega.privacy.android.app.presentation.videosection.model.VideosFilterOptionEntity
import mega.privacy.android.app.presentation.videosection.view.allvideos.VideosFilterBottomSheet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
class VideosFilterBottomSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        modifier: Modifier = Modifier,
        title: String = "",
        options: List<VideosFilterOptionEntity> = emptyList(),
        onItemSelected: (VideosFilterOptionEntity) -> Unit = {},
        onDismissRequest: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            val sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { true }
            )

            LaunchedEffect(Unit) {
                delay(500)
                sheetState.show()
            }

            VideosFilterBottomSheet(
                modifier = modifier,
                sheetState = sheetState,
                title = title,
                options = options,
                onItemSelected = onItemSelected,
                onDismissRequest = onDismissRequest
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

        val textNode = composeTestRule.onNodeWithText(option1.title, useUnmergedTree = true)
        textNode.assertIsDisplayed()

        textNode.onParent()
            .assert(hasClickAction())
            .performSemanticsAction(SemanticsActions.OnClick)

        composeTestRule.waitForIdle()

        verify(onItemSelected).invoke(option1)
    }
}