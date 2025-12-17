package mega.privacy.android.feature.photos.components

import androidx.annotation.DrawableRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.icon.pack.R as iconPackR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config
import kotlin.intArrayOf

@Config(sdk = [34])
@RunWith(AndroidJUnit4::class)
class ThumbnailListViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val emptyIcon = iconPackR.drawable.ic_video_playlist_default_thumbnail
    private val noThumbnailIcon = iconPackR.drawable.ic_video_playlist_no_thumbnail

    private fun setComposeContent(
        @DrawableRes emptyPlaylistIcon: Int = emptyIcon,
        @DrawableRes noThumbnailIcon: Int = this.noThumbnailIcon,
        thumbnailList: List<Any?>? = null,
        modifier: Modifier = Modifier,
    ) {
        composeTestRule.setContent {
            ThumbnailListView(
                emptyPlaylistIcon = emptyPlaylistIcon,
                noThumbnailIcon = noThumbnailIcon,
                thumbnailList = thumbnailList,
                modifier = modifier
            )
        }
    }

    @Test
    fun `test that the empty playlist view is displayed as expected when thumbnailList is null`() {
        setComposeContent()

        THUMBNAIL_LIST_VIEW_EMPTY_VIEW_TEST_TAG.assertIsDisplayedWithTag()
        THUMBNAIL_LIST_VIEW_NO_THUMBNAIL_TEST_TAG.assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that the no thumbnail view is displayed as expected when thumbnailList is empty`() {
        setComposeContent(
            thumbnailList = emptyList()
        )

        THUMBNAIL_LIST_VIEW_NO_THUMBNAIL_TEST_TAG.assertIsDisplayedWithTag()
        THUMBNAIL_LIST_VIEW_EMPTY_VIEW_TEST_TAG.assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that the info text is displayed as expected when thumbnail is not empty`() {
        setComposeContent(
            thumbnailList = listOf(mock(), mock(), mock(), mock())
        )
        listOf(
            "${THUMBNAIL_LIST_VIEW_THUMBNAIL_TEST_TAG}0",
            "${THUMBNAIL_LIST_VIEW_THUMBNAIL_TEST_TAG}1",
            "${THUMBNAIL_LIST_VIEW_THUMBNAIL_TEST_TAG}2",
            "${THUMBNAIL_LIST_VIEW_THUMBNAIL_TEST_TAG}3",
        ).assertIsDisplayedWithTag()

        listOf(
            THUMBNAIL_LIST_VIEW_EMPTY_VIEW_TEST_TAG,
            THUMBNAIL_LIST_VIEW_NO_THUMBNAIL_TEST_TAG
        ).assertIsNotDisplayedWithTag()
    }

    private fun String.assertIsDisplayedWithTag() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsDisplayed()

    private fun String.assertIsNotDisplayedWithTag() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsNotDisplayed()

    private fun List<String>.assertIsDisplayedWithTag() =
        onEach {
            it.assertIsDisplayedWithTag()
        }

    private fun List<String>.assertIsNotDisplayedWithTag() =
        onEach {
            it.assertIsNotDisplayedWithTag()
        }
}