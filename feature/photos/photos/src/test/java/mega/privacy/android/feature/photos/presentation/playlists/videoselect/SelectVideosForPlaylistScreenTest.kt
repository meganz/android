package mega.privacy.android.feature.photos.presentation.playlists.videoselect

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.model.SelectVideoItemUiEntity
import mega.privacy.android.feature.photos.presentation.videos.VIDEO_TAB_SORT_BOTTOM_SHEET_TEST_TAG
import mega.privacy.android.icon.pack.R as iconPackR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(AndroidJUnit4::class)
class SelectVideosForPlaylistScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        uiState: SelectVideosForPlaylistUiState = SelectVideosForPlaylistUiState.Data(),
        searchQuery: String? = null,
        updateSearchQuery: (String?) -> Unit = {},
        onSortNodes: (NodeSortConfiguration) -> Unit = {},
        onChangeViewTypeClick: () -> Unit = {},
        onItemClicked: (SelectVideoItemUiEntity) -> Unit = {},
        onBackPressed: () -> Unit = {},
        modifier: Modifier = Modifier,
    ) {
        composeTestRule.setContent {
            SelectVideosForPlaylistScreen(
                uiState = uiState,
                searchQuery = searchQuery,
                updateSearchQuery = updateSearchQuery,
                onSortNodes = onSortNodes,
                onChangeViewTypeClick = onChangeViewTypeClick,
                modifier = modifier,
                onItemClicked = onItemClicked,
                onBackPressed = onBackPressed,
            )
        }
    }

    @Test
    fun `test that loading view is displayed when uiState is Loading`() {
        setComposeContent(uiState = SelectVideosForPlaylistUiState.Loading)

        listOf(
            SELECT_VIDEOS_SEARCH_TOP_APP_BAR_TAG,
            SELECT_VIDEOS_LOADING_VIEW_TEST_TAG,
        ).assertIsDisplayedWithTag()

        listOf(
            SELECT_VIDEOS_EMPTY_VIEW_TEST_TAG,
        ).assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that empty view is displayed when Data state has no items`() {
        setComposeContent(
            uiState = SelectVideosForPlaylistUiState.Data(
                title = LocalizedText.Literal("Title"),
                items = emptyList(),
            )
        )

        listOf(
            SELECT_VIDEOS_SEARCH_TOP_APP_BAR_TAG,
            SELECT_VIDEOS_EMPTY_VIEW_TEST_TAG,
        ).assertIsDisplayedWithTag()

        SELECT_VIDEOS_LOADING_VIEW_TEST_TAG.assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that list view is displayed when Data state has items and currentViewType is LIST`() {
        val items = listOf(
            SelectVideoItemUiEntity(
                id = NodeId(1L),
                name = "video.mp4",
                title = LocalizedText.Literal("video.mp4"),
                isFolder = true,
                iconRes = iconPackR.drawable.ic_folder_outgoing_medium_solid,
            ),
        )
        setComposeContent(
            uiState = SelectVideosForPlaylistUiState.Data(
                title = LocalizedText.Literal("Folder"),
                items = items,
                currentViewType = ViewType.LIST,
            )
        )

        SELECT_VIDEOS_SEARCH_TOP_APP_BAR_TAG.assertIsDisplayedWithTag()
        SELECT_VIDEOS_LIST_VIEW_TAG.assertIsDisplayedWithTag()

        SELECT_VIDEOS_GRID_VIEW_TAG.assertIsNotDisplayedWithTag()
        SELECT_VIDEOS_LOADING_VIEW_TEST_TAG.assertIsNotDisplayedWithTag()
        SELECT_VIDEOS_EMPTY_VIEW_TEST_TAG.assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that list view is displayed when Data state has items and currentViewType is GRID`() {
        val items = listOf(
            SelectVideoItemUiEntity(
                id = NodeId(1L),
                name = "video.mp4",
                title = LocalizedText.Literal("video.mp4"),
                isFolder = true,
                iconRes = iconPackR.drawable.ic_folder_outgoing_medium_solid,
            ),
        )
        setComposeContent(
            uiState = SelectVideosForPlaylistUiState.Data(
                title = LocalizedText.Literal("Folder"),
                items = items,
                currentViewType = ViewType.GRID,
            )
        )

        SELECT_VIDEOS_SEARCH_TOP_APP_BAR_TAG.assertIsDisplayedWithTag()
        SELECT_VIDEOS_GRID_VIEW_TAG.assertIsDisplayedWithTag()

        SELECT_VIDEOS_LIST_VIEW_TAG.assertIsNotDisplayedWithTag()
        SELECT_VIDEOS_LOADING_VIEW_TEST_TAG.assertIsNotDisplayedWithTag()
        SELECT_VIDEOS_EMPTY_VIEW_TEST_TAG.assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that app bar is displayed for Data state`() {
        setComposeContent(
            uiState = SelectVideosForPlaylistUiState.Data(
                title = LocalizedText.Literal("Cloud Drive"),
                isCloudDriveRoot = true,
                items = emptyList(),
            )
        )

        SELECT_VIDEOS_SEARCH_TOP_APP_BAR_TAG.assertIsDisplayedWithTag()
    }

    @Test
    fun `test that sort bottom sheet is not displayed by default when Data has items`() {
        val items = listOf(
            SelectVideoItemUiEntity(
                id = NodeId(1L),
                name = "video.mp4",
                title = LocalizedText.Literal("video.mp4"),
                isFolder = true,
                iconRes = iconPackR.drawable.ic_folder_outgoing_medium_solid,
            ),
        )
        setComposeContent(
            uiState = SelectVideosForPlaylistUiState.Data(
                title = LocalizedText.Literal("Folder"),
                items = items,
                currentViewType = ViewType.LIST,
            )
        )

        composeTestRule.onNodeWithTag(VIDEO_TAB_SORT_BOTTOM_SHEET_TEST_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    private fun String.assertIsDisplayedWithTag() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsDisplayed()

    private fun List<String>.assertIsDisplayedWithTag() =
        forEach { it.assertIsDisplayedWithTag() }

    private fun String.assertIsNotDisplayedWithTag() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsNotDisplayed()

    private fun List<String>.assertIsNotDisplayedWithTag() =
        forEach { it.assertIsNotDisplayedWithTag() }
}
