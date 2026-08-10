package mega.privacy.android.feature.photos.presentation.videos.view

import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import mega.privacy.android.feature.photos.presentation.videos.model.FilterOption
import mega.privacy.android.feature.photos.presentation.videos.model.LocationFilterOption
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalMaterial3Api::class)
@Config(sdk = [34])
@RunWith(AndroidJUnit4::class)
class VideosFilterBottomSheetTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()


    private fun setComposeContent(
        modifier: Modifier = Modifier,
        title: String = "",
        selectedFilterOption: FilterOption = LocationFilterOption.AllLocations,
        options: List<FilterOption> = emptyList(),
        onItemSelected: (FilterOption) -> Unit = {},
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
                selectedFilterOption = selectedFilterOption,
                options = options,
                onItemSelected = onItemSelected,
                onDismissRequest = onDismissRequest
            )
        }
    }

    @Test
    fun `test that UIs are displayed correctly`() {
        val title = "Title"
        val option1 = LocationFilterOption.AllLocations
        val option2 = LocationFilterOption.CameraUploads
        val options = listOf(option1, option2)
        setComposeContent(title = title, options = options)
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithTag(FILTER_OPTION_ITEM_TEST_TAG + option1).assertIsDisplayed()
        composeTestRule.onNodeWithTag(FILTER_OPTION_ITEM_TEST_TAG + option2).assertIsDisplayed()
    }
}