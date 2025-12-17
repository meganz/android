package mega.privacy.android.feature.photos.components

import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.icon.pack.R
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(AndroidJUnit4::class)
class VideoPlaylistItemViewTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val emptyIcon = R.drawable.ic_video_playlist_default_thumbnail
    private val noThumbnailIcon = R.drawable.ic_video_playlist_no_thumbnail
    private val title = "My Playlist"

    private fun setComposeContent(
        @DrawableRes emptyPlaylistIcon: Int = emptyIcon,
        @DrawableRes noThumbnailIcon: Int = this.noThumbnailIcon,
        title: String = this.title,
        numberOfVideos: Int = 0,
        totalDuration: String? = null,
        isSelected: Boolean = false,
        onClick: () -> Unit = {},
        thumbnailList: List<Any?>? = null,
        modifier: Modifier = Modifier,
        isSystemVideoPlaylist: Boolean = false,
        onLongClick: (() -> Unit)? = null,
        onMenuClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            VideoPlaylistItemView(
                emptyPlaylistIcon = emptyPlaylistIcon,
                noThumbnailIcon = noThumbnailIcon,
                title = title,
                numberOfVideos = numberOfVideos,
                thumbnailList = thumbnailList,
                totalDuration = totalDuration,
                isSelected = isSelected,
                isSystemVideoPlaylist = isSystemVideoPlaylist,
                onClick = onClick,
                onLongClick = onLongClick,
                onMenuClick = onMenuClick,
                modifier = modifier
            )
        }
    }

    @Test
    fun `test that the empty playlist view is displayed as expected when thumbnailList is null`() {
        val context = composeTestRule.activity
        setComposeContent()

        listOf(
            VIDEO_PLAYLIST_ITEM_VIEW_TEST_TAG,
            VIDEO_PLAYLIST_ITEM_STACK_ICON_TEST_TAG,
            VIDEO_PLAYLIST_ITEM_THUMBNAIL_VIEW_TEST_TAG,
            VIDEO_PLAYLIST_ITEM_TITLE_TEST_TAG,
            VIDEO_PLAYLIST_ITEM_INFO_TEST_TAG,
            VIDEO_PLAYLIST_ITEM_TAILING_ICON_TEST_TAG
        ).assertIsDisplayedWithTag()
        VIDEO_PLAYLIST_ITEM_TITLE_TEST_TAG.getNodeWithTag().assertTextEquals(title)
        val infoText = context.getString(sharedR.string.video_section_playlists_video_empty)
        VIDEO_PLAYLIST_ITEM_INFO_TEST_TAG.getNodeWithTag().assertTextEquals(infoText)
    }

    @Test
    fun `test that the no thumbnail view is displayed as expected when thumbnailList is empty`() {
        val context = composeTestRule.activity
        setComposeContent(
            thumbnailList = emptyList()
        )

        listOf(
            VIDEO_PLAYLIST_ITEM_VIEW_TEST_TAG,
            VIDEO_PLAYLIST_ITEM_STACK_ICON_TEST_TAG,
            VIDEO_PLAYLIST_ITEM_THUMBNAIL_VIEW_TEST_TAG,
            VIDEO_PLAYLIST_ITEM_TITLE_TEST_TAG,
            VIDEO_PLAYLIST_ITEM_INFO_TEST_TAG,
            VIDEO_PLAYLIST_ITEM_TAILING_ICON_TEST_TAG
        ).assertIsDisplayedWithTag()
        VIDEO_PLAYLIST_ITEM_TITLE_TEST_TAG.getNodeWithTag().assertTextEquals(title)
        val infoText = context.getString(sharedR.string.video_section_playlists_video_empty)
        VIDEO_PLAYLIST_ITEM_INFO_TEST_TAG.getNodeWithTag().assertTextEquals(infoText)
    }

    @Test
    fun `test that the info text is displayed as expected when numberOfVideos is greater than zero`() {
        val context = composeTestRule.activity
        val numberOfVideos = 5
        val totalDuration = "15:30"
        setComposeContent(
            numberOfVideos = numberOfVideos,
            totalDuration = totalDuration,
            thumbnailList = listOf(mock(), mock(), mock(), mock())
        )
        listOf(
            VIDEO_PLAYLIST_ITEM_VIEW_TEST_TAG,
            VIDEO_PLAYLIST_ITEM_STACK_ICON_TEST_TAG,
            VIDEO_PLAYLIST_ITEM_TITLE_TEST_TAG,
            VIDEO_PLAYLIST_ITEM_THUMBNAIL_VIEW_TEST_TAG,
            VIDEO_PLAYLIST_ITEM_INFO_TEST_TAG,
            VIDEO_PLAYLIST_ITEM_TAILING_ICON_TEST_TAG,
        ).assertIsDisplayedWithTag()

        VIDEO_PLAYLIST_ITEM_TITLE_TEST_TAG.getNodeWithTag().assertTextEquals(title)
        val numberOfVideosText = context.resources.getQuantityString(
            sharedR.plurals.video_section_playlists_video_count,
            numberOfVideos,
            numberOfVideos
        )
        val infoText = "$numberOfVideosText • $totalDuration"
        VIDEO_PLAYLIST_ITEM_INFO_TEST_TAG.getNodeWithTag().assertTextEquals(infoText)
    }

    @Test
    fun `test that tailing icon is not displayed when playlist is system playlist`() {
        setComposeContent(
            isSystemVideoPlaylist = true
        )

        VIDEO_PLAYLIST_ITEM_TAILING_ICON_TEST_TAG.assertIsNotDisplayedWithTag()
    }

    private fun String.assertIsDisplayedWithTag() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsDisplayed()

    private fun String.assertIsNotDisplayedWithTag() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsNotDisplayed()

    private fun String.getNodeWithTag() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true)

    private fun List<String>.assertIsDisplayedWithTag() =
        onEach {
            it.assertIsDisplayedWithTag()
        }
}