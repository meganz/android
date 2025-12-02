package mega.privacy.android.feature.photos.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoPhotosNodeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that the thumbnail placeholder image is displayed when the thumbnail data is for placeholder`() {
        composeRuleScope {
            val thumbnailData = PhotosNodeThumbnailData.Placeholder(
                mega.privacy.android.icon.pack.R.drawable.ic_usp_2
            )

            setNode(thumbnailData = thumbnailData)

            onNodeWithTag(BASIC_PHOTOS_NODE_IMAGE_THUMBNAIL_PLACEHOLDER_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the thumbnail with file path is displayed when the thumbnail data is for file`() {
        composeRuleScope {
            val thumbnailData = PhotosNodeThumbnailData.File(
                path = "path",
                isSensitive = false
            )

            setNode(thumbnailData = thumbnailData)

            onNodeWithTag(BASIC_PHOTOS_NODE_IMAGE_THUMBNAIL_FILE_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the favourite icon is displayed when the node is added to favourite`() {
        composeRuleScope {
            setNode(shouldShowFavourite = true)

            onNodeWithTag(BASIC_PHOTOS_NODE_FAVOURITE_ICON_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the selected checkbox is displayed when the node is selected`() {
        composeRuleScope {
            setNode(isSelected = true)

            onNodeWithContentDescription("check icon").assertIsDisplayed()
        }
    }

    @Test
    fun `test that the video duration is displayed`() {
        val duration = "2:50"
        composeRuleScope {
            setNode(duration = duration, isSelected = true)

            onNodeWithText(duration).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the video duration is not displayed when duration is empty`() {
        composeRuleScope {
            setNode()

            onNodeWithTag(VIDEO_PHOTOS_NODE_DURATION_TEXT_TAG).assertIsNotDisplayed()
        }
    }

    private fun composeRuleScope(block: ComposeContentTestRule.() -> Unit) {
        with(composeRule) {
            block()
        }
    }

    private fun ComposeContentTestRule.setNode(
        duration: String = "",
        thumbnailData: PhotosNodeThumbnailData = PhotosNodeThumbnailData.Placeholder(
            mega.privacy.android.icon.pack.R.drawable.ic_usp_2
        ),
        isSelected: Boolean = false,
        shouldShowFavourite: Boolean = false,
    ) {
        setContent {
            VideoPhotosNode(
                duration = duration,
                thumbnailData = thumbnailData,
                isSelected = isSelected,
                shouldShowFavourite = shouldShowFavourite
            )
        }
    }
}
