package mega.privacy.android.feature.photos.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.domain.entity.photos.thumbnail.MediaThumbnailRequest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImagePhotosNodeTest {

    @get:Rule
    val composeRule = createComposeRule()

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

    private fun composeRuleScope(block: ComposeContentTestRule.() -> Unit) {
        with(composeRule) {
            block()
        }
    }

    private fun ComposeContentTestRule.setNode(
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
    ) {
        setContent {
            ImagePhotosNode(
                thumbnailRequest = thumbnailRequest,
                isSensitive = isSensitive,
                isSelected = isSelected,
                shouldShowFavourite = shouldShowFavourite
            )
        }
    }
}
