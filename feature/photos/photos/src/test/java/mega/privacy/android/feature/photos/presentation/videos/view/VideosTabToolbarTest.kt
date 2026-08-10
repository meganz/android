package mega.privacy.android.feature.photos.presentation.videos.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.intArrayOf

@Config(sdk = [34])
@RunWith(AndroidJUnit4::class)
class VideosTabToolbarTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()


    private fun setComposeContent(
        count: Int = 0,
        isAllSelected: Boolean = false,
        isSelectionMode: Boolean = false,
        onSelectAllClicked: () -> Unit = {},
        onCancelSelectionClicked: () -> Unit = {},
        searchQuery: String? = null,
        updateSearchQuery: (String) -> Unit = {},
        modifier: Modifier = Modifier,
        title: String = "",
        isSearchMode: Boolean = true,
        onBackPressed: () -> Unit = {},
        onSearchingModeChanged: ((Boolean) -> Unit)? = null,
    ) {
        composeTestRule.setContent {
            VideosTabToolbar(
                count = count,
                isAllSelected = isAllSelected,
                isSelectionMode = isSelectionMode,
                onSelectAllClicked = onSelectAllClicked,
                onCancelSelectionClicked = onCancelSelectionClicked,
                searchQuery = searchQuery,
                updateSearchQuery = updateSearchQuery,
                modifier = modifier,
                title = title,
                isSearchMode = isSearchMode,
                onBackPressed = onBackPressed,
                onSearchingModeChanged = onSearchingModeChanged
            )
        }
    }

    @Test
    fun `test that search top bar is displayed as expected`() {
        setComposeContent(
            isSelectionMode = false
        )

        composeTestRule
            .onNodeWithTag(VIDEOS_TAB_SEARCH_TOP_APP_BAR_TAG)
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `test that selection top bar is displayed as expected`() {
        setComposeContent(
            isSelectionMode = true
        )

        composeTestRule
            .onNodeWithTag(VIDEOS_TAB_SELECTION_TOP_APP_BAR_TAG)
            .assertExists()
            .assertIsDisplayed()
    }
}