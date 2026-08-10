package mega.privacy.android.feature.photos.presentation.playlists.videoselect

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.model.SelectVideoItemUiEntity
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.model.SelectVideosMenuAction
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.view.SELECT_VIDEOS_SEARCH_BOTTOM_VIEW_ROW_TEST_TAG
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.view.SELECT_VIDEOS_SEARCH_EMPTY_VIEW_TEST_TAG
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.view.SELECT_VIDEOS_SEARCH_GRID_VIEW_TAG
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.view.SELECT_VIDEOS_SEARCH_LIST_VIEW_TAG
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.view.SELECT_VIDEOS_SEARCH_LOADING_VIEW_TEST_TAG
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.view.SELECT_VIDEOS_SEARCH_SELECTION_TOP_APP_BAR_TAG
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.view.SELECT_VIDEOS_SEARCH_TOP_APP_BAR_TAG
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.view.SelectVideosSearchScreen
import mega.privacy.android.feature.photos.presentation.videos.VIDEO_TAB_SORT_BOTTOM_SHEET_TEST_TAG
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(AndroidJUnit4::class)
class SelectVideosSearchScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        uiState: SelectVideoSearchUiState = SelectVideoSearchUiState.Data(),
        updateSearchQuery: (String?) -> Unit = {},
        onSortNodes: (NodeSortConfiguration) -> Unit = {},
        onChangeViewTypeClick: () -> Unit = {},
        onItemClicked: (SelectVideoItemUiEntity) -> Unit = {},
        confirmAddVideos: () -> Unit = {},
        onBackPressed: () -> Unit = {},
        selectAll: () -> Unit = {},
        modifier: Modifier = Modifier,
    ) {
        composeTestRule.setContent {
            SelectVideosSearchScreen(
                uiState = uiState,
                updateSearchQuery = updateSearchQuery,
                onSortNodes = onSortNodes,
                onChangeViewTypeClick = onChangeViewTypeClick,
                modifier = modifier,
                onItemClicked = onItemClicked,
                confirmAddVideos = confirmAddVideos,
                onBackPressed = onBackPressed,
                selectAll = selectAll,
            )
        }
    }

    @Test
    fun `test that loading view is displayed when uiState is Loading`() {
        setComposeContent(uiState = SelectVideoSearchUiState.Loading)

        SELECT_VIDEOS_SEARCH_TOP_APP_BAR_TAG.assertIsDisplayedWithTag()
        composeTestRule.onAllNodesWithTag(SELECT_VIDEOS_SEARCH_LOADING_VIEW_TEST_TAG, true)
            .onFirst().assertIsDisplayed()

        listOf(
            SELECT_VIDEOS_SEARCH_EMPTY_VIEW_TEST_TAG,
        ).assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that empty view is displayed when Data state has no items`() {
        setComposeContent(
            uiState = SelectVideoSearchUiState.Data(
                title = LocalizedText.Literal("Title"),
                items = emptyList(),
            )
        )

        listOf(
            SELECT_VIDEOS_SEARCH_TOP_APP_BAR_TAG,
            SELECT_VIDEOS_SEARCH_EMPTY_VIEW_TEST_TAG,
        ).assertIsDisplayedWithTag()

        SELECT_VIDEOS_SEARCH_LOADING_VIEW_TEST_TAG.assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that list view is displayed when Data has items and currentViewType is LIST`() {
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
            uiState = SelectVideoSearchUiState.Data(
                title = LocalizedText.Literal("Folder"),
                items = items,
                currentViewType = ViewType.LIST,
            )
        )

        listOf(
            SELECT_VIDEOS_SEARCH_TOP_APP_BAR_TAG,
            SELECT_VIDEOS_SEARCH_LIST_VIEW_TAG,
            SELECT_VIDEOS_SEARCH_BOTTOM_VIEW_ROW_TEST_TAG,
        ).assertIsDisplayedWithTag()

        listOf(
            SELECT_VIDEOS_SEARCH_SELECTION_TOP_APP_BAR_TAG,
            SELECT_VIDEOS_SEARCH_GRID_VIEW_TAG,
            SELECT_VIDEOS_SEARCH_LOADING_VIEW_TEST_TAG,
            SELECT_VIDEOS_SEARCH_EMPTY_VIEW_TEST_TAG,
        ).assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that grid view is displayed when Data has items and currentViewType is GRID`() {
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
            uiState = SelectVideoSearchUiState.Data(
                title = LocalizedText.Literal("Folder"),
                items = items,
                currentViewType = ViewType.GRID,
            )
        )

        listOf(
            SELECT_VIDEOS_SEARCH_TOP_APP_BAR_TAG,
            SELECT_VIDEOS_SEARCH_GRID_VIEW_TAG,
            SELECT_VIDEOS_SEARCH_BOTTOM_VIEW_ROW_TEST_TAG,
        ).assertIsDisplayedWithTag()

        listOf(
            SELECT_VIDEOS_SEARCH_SELECTION_TOP_APP_BAR_TAG,
            SELECT_VIDEOS_SEARCH_LIST_VIEW_TAG,
            SELECT_VIDEOS_SEARCH_EMPTY_VIEW_TEST_TAG,
        ).assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that selection top bar is displayed when selectItemHandles is not empty`() {
        setComposeContent(
            uiState = SelectVideoSearchUiState.Data(
                title = LocalizedText.Literal("Cloud Drive"),
                isCloudDriveRoot = true,
                items = emptyList(),
                selectItemHandles = setOf(1L, 2L),
            )
        )
        SELECT_VIDEOS_SEARCH_SELECTION_TOP_APP_BAR_TAG.assertIsDisplayedWithTag()
        SELECT_VIDEOS_SEARCH_TOP_APP_BAR_TAG.assertIsNotDisplayedWithTag()
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
            uiState = SelectVideoSearchUiState.Data(
                title = LocalizedText.Literal("Folder"),
                items = items,
                currentViewType = ViewType.LIST,
            )
        )

        composeTestRule.onNodeWithTag(VIDEO_TAB_SORT_BOTTOM_SHEET_TEST_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that bottom view is displayed when Data state has items`() {
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
            uiState = SelectVideoSearchUiState.Data(
                title = LocalizedText.Literal("Folder"),
                items = items,
                currentViewType = ViewType.LIST,
            )
        )

        SELECT_VIDEOS_SEARCH_BOTTOM_VIEW_ROW_TEST_TAG.assertIsDisplayedWithTag()
    }

    @Test
    fun `test that selection top app bar is displayed for Data state`() {
        setComposeContent(
            uiState = SelectVideoSearchUiState.Data(
                selectItemHandles = setOf(1L, 2L)
            )
        )
        SELECT_VIDEOS_SEARCH_SELECTION_TOP_APP_BAR_TAG.assertIsDisplayedWithTag()
    }

    @Test
    fun `test that selectAll is invoked when Select All action is clicked`() {
        val selectAll = mock<() -> Unit>()
        val items = listOf(
            SelectVideoItemUiEntity(
                id = NodeId(1L),
                name = "a.mp4",
                title = LocalizedText.Literal("a.mp4"),
                isFolder = false,
                isVideo = true,
                iconRes = iconPackR.drawable.ic_folder_outgoing_medium_solid,
            ),
            SelectVideoItemUiEntity(
                id = NodeId(2L),
                name = "b.mp4",
                title = LocalizedText.Literal("b.mp4"),
                isFolder = false,
                isVideo = true,
                iconRes = iconPackR.drawable.ic_folder_outgoing_medium_solid,
            ),
        )
        setComposeContent(
            uiState = SelectVideoSearchUiState.Data(
                title = LocalizedText.Literal("Folder"),
                items = items,
                currentViewType = ViewType.LIST,
                selectItemHandles = setOf(1L),
            ),
            selectAll = selectAll
        )

        composeTestRule.onNodeWithTag(CommonMenuAction.SelectAll.testTag, useUnmergedTree = true)
            .performClick()

        verify(selectAll).invoke()
    }

    @Test
    fun `test that Select All action is not displayed when all selectable items are selected`() {
        val items = listOf(
            SelectVideoItemUiEntity(
                id = NodeId(1L),
                name = "a.mp4",
                title = LocalizedText.Literal("a.mp4"),
                isFolder = false,
                isVideo = true,
                iconRes = iconPackR.drawable.ic_folder_outgoing_medium_solid,
            ),
            SelectVideoItemUiEntity(
                id = NodeId(2L),
                name = "b.mp4",
                title = LocalizedText.Literal("b.mp4"),
                isFolder = false,
                isVideo = true,
                iconRes = iconPackR.drawable.ic_folder_outgoing_medium_solid,
            ),
        )
        setComposeContent(
            uiState = SelectVideoSearchUiState.Data(
                title = LocalizedText.Literal("Folder"),
                items = items,
                currentViewType = ViewType.LIST,
                selectItemHandles = setOf(1L, 2L),
            )
        )

        composeTestRule.onNodeWithTag(CommonMenuAction.SelectAll.testTag, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that Search action is not displayed when there is a selection`() {
        val items = listOf(
            SelectVideoItemUiEntity(
                id = NodeId(1L),
                name = "video.mp4",
                title = LocalizedText.Literal("video.mp4"),
                isFolder = false,
                isVideo = true,
                iconRes = iconPackR.drawable.ic_folder_outgoing_medium_solid,
            ),
        )
        setComposeContent(
            uiState = SelectVideoSearchUiState.Data(
                title = LocalizedText.Literal("Folder"),
                items = items,
                currentViewType = ViewType.LIST,
                selectItemHandles = setOf(1L),
            )
        )

        composeTestRule.onNodeWithTag(SelectVideosMenuAction.Search.testTag, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that searchText is shown in search top bar when Data has searchText`() {
        setComposeContent(
            uiState = SelectVideoSearchUiState.Data(
                items = emptyList(),
                searchText = "my query",
                searchedQuery = "my query",
            )
        )
        composeTestRule.onNodeWithTag(
            SELECT_VIDEOS_SEARCH_TOP_APP_BAR_TAG,
            useUnmergedTree = true
        ).assertIsDisplayed()
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
