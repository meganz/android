package mega.privacy.android.feature.photos.presentation.playlists.view

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.nodecomponents.model.NodeSelectionAction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(AndroidJUnit4::class)
class VideoPlaylistsTabAppBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        count: Int = 0,
        isAllSelected: Boolean = false,
        onSelectAllClicked: () -> Unit = {},
        onCancelSelectionClicked: () -> Unit = {},
        removePlaylist: () -> Unit = {},
        modifier: Modifier = Modifier,
    ) {
        composeTestRule.setContent {
            VideoPlaylistsTabAppBar(
                count = count,
                isAllSelected = isAllSelected,
                onSelectAllClicked = onSelectAllClicked,
                onCancelSelectionClicked = onCancelSelectionClicked,
                removePlaylist = removePlaylist,
                modifier = modifier
            )
        }
    }

    @Test
    fun `test that selection top bar is displayed as expected`() {
        setComposeContent()

        composeTestRule
            .onNodeWithTag(VIDEO_PLAYLISTS_TAB_SELECTION_TOP_APP_BAR_TAG)
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `test that selectAll option is displayed and onSelectAllClicked is invoked`() {
        val mockOnSelectAllClicked = mock<() -> Unit>()
        setComposeContent(isAllSelected = false, onSelectAllClicked = mockOnSelectAllClicked)
        composeTestRule
            .onNodeWithTag(NodeSelectionAction.SelectAll.testTag)
            .assertExists()
            .assertIsDisplayed()
            .performClick()

        verify(mockOnSelectAllClicked).invoke()
    }

    @Test
    fun `test that selectAll option is not displayed`() {
        setComposeContent(isAllSelected = true)
        composeTestRule
            .onNodeWithTag(NodeSelectionAction.SelectAll.testTag)
            .assertIsNotDisplayed()
    }

    @Test
    fun `test that VideoPlaylistsTrashMenuAction option is displayed and removePlaylist is invoked`() {
        val mockRemovePlaylist = mock<() -> Unit>()
        setComposeContent(isAllSelected = false, removePlaylist = mockRemovePlaylist)
        composeTestRule
            .onNodeWithTag(VideoPlaylistsTrashMenuAction().testTag)
            .assertExists()
            .assertIsDisplayed()
            .performClick()

        verify(mockRemovePlaylist).invoke()
    }
}