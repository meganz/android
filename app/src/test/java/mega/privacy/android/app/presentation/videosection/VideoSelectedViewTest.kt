package mega.privacy.android.app.presentation.videosection

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.test.FakeImageLoaderEngine
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoMenuAction
import mega.privacy.android.app.presentation.videosection.model.VideoSelectedState
import mega.privacy.android.app.presentation.videosection.view.VIDEO_SECTION_LOADING_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.videoselected.VIDEO_SELECTED_EMPTY_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.videoselected.VIDEO_SELECTED_FAB_BUTTON_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.videoselected.VIDEO_SELECTED_GRID_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.videoselected.VIDEO_SELECTED_LIST_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.videoselected.VideoSelectedView
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoilApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class VideoSelectedViewTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val fileTypeIconMapper: FileTypeIconMapper = mock()

    @Before
    fun setUp() {
        val engine = FakeImageLoaderEngine.Builder().build()
        val imageLoader = ImageLoader.Builder(composeTestRule.activity)
            .components { add(engine) }
            .build()
        Coil.setImageLoader(imageLoader)
    }

    private fun setComposeContent(
        uiState: VideoSelectedState = VideoSelectedState(),
        onItemClicked: (NodeUIItem<TypedNode>) -> Unit = {},
        onSearchTextChange: (String) -> Unit = {},
        onCloseClicked: () -> Unit = {},
        onSearchClicked: () -> Unit = {},
        onSortOrderClick: () -> Unit = {},
        onChangeViewTypeClick: () -> Unit = {},
        onVideoSelected: (List<Long>) -> Unit = {},
        onBackPressed: () -> Unit = {},
        onMenuActionClick: (FileInfoMenuAction) -> Unit = {},
    ) {
        composeTestRule.setContent {
            VideoSelectedView(
                uiState = uiState,
                onItemClicked = onItemClicked,
                onSearchTextChange = onSearchTextChange,
                onCloseClicked = onCloseClicked,
                onSearchClicked = onSearchClicked,
                onSortOrderClick = onSortOrderClick,
                onChangeViewTypeClick = onChangeViewTypeClick,
                onVideoSelected = onVideoSelected,
                onBackPressed = onBackPressed,
                onMenuActionClick = onMenuActionClick,
                fileTypeIconMapper = fileTypeIconMapper
            )
        }
    }

    @Test
    fun `test that ui is displayed correctly when isLoading is true`() {
        setComposeContent()

        composeTestRule.onNodeWithTag(VIDEO_SECTION_LOADING_VIEW_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that ui is displayed correctly when nodesList is empty`() {
        setComposeContent(
            VideoSelectedState(
                isLoading = false,
                nodesList = emptyList()
            )
        )

        composeTestRule.onNodeWithTag(VIDEO_SELECTED_EMPTY_VIEW_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that ui is displayed correctly when selectedNodeHandles is not empty`() {
        setComposeContent(
            VideoSelectedState(
                isLoading = false,
                selectedNodeHandles = listOf(1L, 2L)
            )
        )

        composeTestRule.onNodeWithTag(VIDEO_SELECTED_FAB_BUTTON_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that ui is displayed correctly when nodesList is not empty and viewType is list`() {
        val testNodeName = "node name"
        val testNode = mock<TypedNode> {
            on { name }.thenReturn(testNodeName)
        }

        val item = mock<NodeUIItem<TypedNode>> {
            on { node }.thenReturn(testNode)
            on { name }.thenReturn(testNodeName)
        }
        setComposeContent(
            VideoSelectedState(
                currentViewType = ViewType.LIST,
                isLoading = false,
                nodesList = listOf(item)
            )
        )

        composeTestRule.onNodeWithTag(VIDEO_SELECTED_LIST_VIEW_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that ui is displayed correctly when nodesList is not empty and viewType is grid`() {
        val testNodeName = "node name"
        val testNode = mock<TypedNode> {
            on { name }.thenReturn(testNodeName)
        }

        val item = mock<NodeUIItem<TypedNode>> {
            on { node }.thenReturn(testNode)
            on { name }.thenReturn(testNodeName)
        }
        setComposeContent(
            VideoSelectedState(
                currentViewType = ViewType.GRID,
                isLoading = false,
                nodesList = listOf(item)
            )
        )

        composeTestRule.onAllNodesWithTag(VIDEO_SELECTED_GRID_VIEW_TEST_TAG)[0].assertIsDisplayed()
    }

    @Test
    fun `test that video selected button calls the correct function`() {
        val onVideoSelected = mock<(List<Long>) -> Unit>()

        val testSelectedHandles = listOf(1L, 2L)

        setComposeContent(
            VideoSelectedState(
                isLoading = false,
                selectedNodeHandles = testSelectedHandles,
            ),
            onVideoSelected = onVideoSelected
        )

        composeTestRule.onNodeWithTag(VIDEO_SELECTED_FAB_BUTTON_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(VIDEO_SELECTED_FAB_BUTTON_TEST_TAG).performClick()
        verify(onVideoSelected).invoke(testSelectedHandles)
    }
}