package mega.privacy.android.shared.original.core.ui.controls.lists

import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.ImageLoader
import mega.privacy.android.icon.pack.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NodeGridViewItemTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        val imageLoader = ImageLoader.Builder(composeTestRule.activity)
            .build()
        Coil.setImageLoader(imageLoader)
    }

    private fun setComposeContent(
        isSelected: Boolean = false,
        @DrawableRes icon: Int = R.drawable.ic_text_medium_solid,
        name: String = "name",
        thumbnailData: Any? = null,
        isTakenDown: Boolean = false,
        isFolderNode: Boolean = false,
        isVideoNode: Boolean = false,
        duration: String? = null,
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {},
        onMenuClick: (() -> Unit)? = {},
        onLongClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            NodeGridViewItem(
                isSelected = isSelected,
                iconRes = icon,
                name = name,
                thumbnailData = thumbnailData,
                isTakenDown = isTakenDown,
                modifier = modifier,
                onClick = onClick,
                onMenuClick = onMenuClick,
                onLongClick = onLongClick,
                isFolderNode = isFolderNode,
                duration = duration,
                isVideoNode = isVideoNode
            )
        }
    }

    @Test
    fun `test that thumbnail view with footer is displayed when selected node is file node`() {
        setComposeContent()
        composeTestRule.onNodeWithTag(NODE_TITLE_TEXT_TEST_TAG, useUnmergedTree = true)
            .assertExists()
        composeTestRule.onNodeWithTag(GRID_VIEW_CHECK_SELECTION_TEST_TAG, useUnmergedTree = true)
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(THUMBNAIL_FILE_TEST_TAG, useUnmergedTree = true)
            .assertExists()
        composeTestRule.onNodeWithTag(GRID_VIEW_TAKEN_TEST_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(GRID_VIEW_MORE_ICON_TEST_TAG, useUnmergedTree = true)
            .assertExists()
        composeTestRule.onNodeWithTag(VIDEO_PLAY_ICON_TEST_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(VIDEO_DURATION_TEST_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(FOLDER_VIEW_ICON_TEST_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that only footer is displayed when selected node is folder node`() {
        setComposeContent(isFolderNode = true)
        composeTestRule.onNodeWithTag(NODE_TITLE_TEXT_TEST_TAG, useUnmergedTree = true)
            .assertExists()
        composeTestRule.onNodeWithTag(GRID_VIEW_CHECK_SELECTION_TEST_TAG, useUnmergedTree = true)
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(THUMBNAIL_FILE_TEST_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(GRID_VIEW_TAKEN_TEST_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(GRID_VIEW_MORE_ICON_TEST_TAG, useUnmergedTree = true)
            .assertExists()
        composeTestRule.onNodeWithTag(VIDEO_PLAY_ICON_TEST_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(VIDEO_DURATION_TEST_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(FOLDER_VIEW_ICON_TEST_TAG, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `test that radio selection button is visible when more click option is null`() {
        setComposeContent(onMenuClick = null, isSelected = true)
        composeTestRule.onNodeWithTag(GRID_VIEW_CHECK_SELECTION_TEST_TAG, useUnmergedTree = true)
            .assertExists()
        composeTestRule.onNodeWithTag(GRID_VIEW_MORE_ICON_TEST_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that play button is visible when duration is not null`() {
        setComposeContent(duration = "1:00", isVideoNode = true)
        composeTestRule.onNodeWithTag(VIDEO_PLAY_ICON_TEST_TAG, useUnmergedTree = true)
            .assertExists()
        composeTestRule.onNodeWithTag(VIDEO_DURATION_TEST_TAG, useUnmergedTree = true)
            .assertExists()
    }
}