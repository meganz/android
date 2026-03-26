package mega.privacy.android.feature.photos.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.domain.entity.photos.thumbnail.MediaThumbnailRequest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class VideoPhotosNodeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that the favourite icon is displayed when the node is added to favourite`() {
        composeRuleScope {
            setNode(shouldShowFavourite = true)

            onNodeWithTag(
                BASIC_PHOTOS_NODE_FAVOURITE_ICON_TAG,
                useUnmergedTree = true
            ).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the selected checkbox is displayed when the node is selected`() {
        composeRuleScope {
            setNode(isSelected = true)

            onNodeWithContentDescription("check icon", useUnmergedTree = true).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the video duration is displayed`() {
        val duration = "2:50"
        composeRuleScope {
            setNode(duration = duration, isSelected = true)

            onNodeWithText(duration, useUnmergedTree = true).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the video duration is not displayed when duration is empty`() {
        composeRuleScope {
            setNode()

            onNodeWithTag(
                VIDEO_PHOTOS_NODE_DURATION_TEXT_TAG,
                useUnmergedTree = true
            ).assertIsNotDisplayed()
        }
    }

    @Test
    fun `test that onClick is invoked when the node is clicked`() {
        val onClick = mock<() -> Unit>()
        composeRuleScope {
            setNode(onClick = onClick)

            onNodeWithTag(BASIC_PHOTOS_NODE_IMAGE_THUMBNAIL_FILE_TAG, useUnmergedTree = true)
                .performTouchInput { click() }

            verify(onClick).invoke()
        }
    }

    @Test
    fun `test that onLongClick is invoked when the node is long clicked`() {
        val onLongClick = mock<() -> Unit>()
        composeRuleScope {
            setNode(onClick = {}, onLongClick = onLongClick)

            onNodeWithTag(BASIC_PHOTOS_NODE_IMAGE_THUMBNAIL_FILE_TAG, useUnmergedTree = true)
                .performTouchInput { longClick() }

            verify(onLongClick).invoke()
        }
    }

    @Test
    fun `test that onClick is not invoked when the node is disabled`() {
        val onClick = mock<() -> Unit>()
        composeRuleScope {
            setNode(enabled = false, onClick = onClick)

            onNodeWithTag(BASIC_PHOTOS_NODE_IMAGE_THUMBNAIL_FILE_TAG, useUnmergedTree = true)
                .performTouchInput { click() }

            verifyNoInteractions(onClick)
        }
    }

    private fun composeRuleScope(block: ComposeContentTestRule.() -> Unit) {
        with(composeRule) {
            block()
        }
    }

    private fun ComposeContentTestRule.setNode(
        duration: String = "",
        thumbnailRequest: MediaThumbnailRequest = MediaThumbnailRequest(
            id = 1L,
            isPreview = false,
            thumbnailFilePath = null,
            previewFilePath = null,
            isPublicNode = false,
            fileExtension = "",
        ),
        isSensitive: Boolean = false,
        isSelected: Boolean = false,
        shouldShowFavourite: Boolean = false,
        enabled: Boolean = true,
        onClick: (() -> Unit)? = null,
        onLongClick: (() -> Unit)? = null,
    ) {
        setContent {
            VideoPhotosNode(
                duration = duration,
                thumbnailRequest = thumbnailRequest,
                isSensitive = isSensitive,
                isSelected = isSelected,
                shouldShowFavourite = shouldShowFavourite,
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick,
            )
        }
    }
}
