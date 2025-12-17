package mega.privacy.android.core.nodecomponents.list

import MEDIA_GRID_VIEW_CHECKBOX_TEST_TAG
import MEDIA_GRID_VIEW_DURATION_TEST_TAG
import MEDIA_GRID_VIEW_FAVOURITE_ICON_TEST_TAG
import MediaGridViewItem
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.icon.pack.R as IconPackR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class MediaGridViewItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        thumbnailData: mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData? = null,
        defaultImage: Int = IconPackR.drawable.ic_ic_choose_photo_medium_regular_solid,
        duration: String? = null,
        isSelected: Boolean = false,
        isSensitive: Boolean = false,
        showBlurEffect: Boolean = false,
        showFavourite: Boolean = false,
        onClick: () -> Unit = {},
        onLongClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            MediaGridViewItem(
                thumbnailData = thumbnailData,
                defaultImage = defaultImage,
                duration = duration,
                isSelected = isSelected,
                isSensitive = isSensitive,
                showBlurEffect = showBlurEffect,
                showFavourite = showFavourite,
                onClick = onClick,
                onLongClick = onLongClick,
            )
        }
    }

    @Test
    fun `test that favourite icon is displayed when showFavourite is true`() {
        setContent(showFavourite = true)

        composeTestRule.onNodeWithTag(
            MEDIA_GRID_VIEW_FAVOURITE_ICON_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
    }

    @Test
    fun `test that favourite icon is not displayed when showFavourite is false`() {
        setContent(showFavourite = false)

        composeTestRule.onNodeWithTag(
            MEDIA_GRID_VIEW_FAVOURITE_ICON_TEST_TAG,
            useUnmergedTree = true
        )
            .assertDoesNotExist()
    }

    @Test
    fun `test that checkbox is displayed when isSelected is true`() {
        setContent(isSelected = true)

        composeTestRule.onAllNodesWithTag(MEDIA_GRID_VIEW_CHECKBOX_TEST_TAG, useUnmergedTree = true)
            .assertAny(hasTestTag(MEDIA_GRID_VIEW_CHECKBOX_TEST_TAG))
    }

    @Test
    fun `test that checkbox is not displayed when isSelected is false`() {
        setContent(isSelected = false)

        composeTestRule.onNodeWithTag(MEDIA_GRID_VIEW_CHECKBOX_TEST_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that duration is displayed when duration is provided`() {
        val durationText = "03:45"
        setContent(duration = durationText)

        composeTestRule.onNodeWithText(durationText).assertIsDisplayed()
        composeTestRule.onNodeWithTag(MEDIA_GRID_VIEW_DURATION_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that duration is not displayed when duration is null`() {
        setContent(duration = null)

        composeTestRule.onNodeWithTag(MEDIA_GRID_VIEW_DURATION_TEST_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that duration is not displayed when duration is empty`() {
        setContent(duration = "")

        composeTestRule.onNodeWithTag(MEDIA_GRID_VIEW_DURATION_TEST_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }
}
