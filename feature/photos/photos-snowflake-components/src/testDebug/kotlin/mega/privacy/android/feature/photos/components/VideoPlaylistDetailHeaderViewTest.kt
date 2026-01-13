package mega.privacy.android.feature.photos.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config
import kotlin.intArrayOf

@Config(sdk = [34])
@RunWith(AndroidJUnit4::class)
class VideoPlaylistDetailHeaderViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        thumbnailList: List<Any?>? = null,
        title: String? = null,
        totalDuration: String? = null,
        numberOfVideos: Int? = null,
        onPlayAllClicked: () -> Unit = {},
        modifier: Modifier = Modifier
    ) {
        composeTestRule.setContent {
            VideoPlaylistDetailHeaderView(
                thumbnailList = thumbnailList,
                title = title,
                totalDuration = totalDuration,
                numberOfVideos = numberOfVideos,
                onPlayAllClicked = onPlayAllClicked,
                modifier = modifier
            )
        }
    }

    @Test
    fun `test that views are displayed correctly when all data is provided`() {
        setComposeContent(
            thumbnailList = null,
            title = "My Playlist",
            totalDuration = "10:00",
            numberOfVideos = 5
        )

        listOf(
            VIDEO_PLAYLIST_ITEM_THUMBNAIL_VIEW_TEST_TAG,
            VIDEO_PLAYLIST_DETAIL_HEADER_INFO_TEST_TAG,
            VIDEO_PLAYLIST_DETAIL_HEADER_PLAY_ALL_TEST_TAG,
            VIDEO_PLAYLIST_ITEM_TITLE_TEST_TAG,
            VIDEO_PLAYLIST_ITEM_INFO_TEST_TAG
        ).forEach { it.assertIsDisplayedWithTag() }
    }

    @Test
    fun `test that views are displayed correctly when all data is default value`() {
        setComposeContent()

        listOf(
            VIDEO_PLAYLIST_ITEM_THUMBNAIL_VIEW_TEST_TAG,
            VIDEO_PLAYLIST_DETAIL_HEADER_INFO_TEST_TAG,
            VIDEO_PLAYLIST_ITEM_TITLE_TEST_TAG,
            VIDEO_PLAYLIST_ITEM_INFO_TEST_TAG
        ).forEach { it.assertIsDisplayedWithTag() }
    }

    @Test
    fun `test that onPlayAllClicked is invoked when play all button is clicked`() {
        val onPlayAllClicked = mock<() -> Unit>()
        setComposeContent(
            totalDuration = "10:00",
            numberOfVideos = 3,
            onPlayAllClicked = onPlayAllClicked
        )

        VIDEO_PLAYLIST_DETAIL_HEADER_PLAY_ALL_TEST_TAG
            .assertIsDisplayedWithTag()
            .performClick()
        verify(onPlayAllClicked).invoke()
    }

    private fun String.assertIsDisplayedWithTag() =
        composeTestRule.onNodeWithTag(this, true).assertIsDisplayed()
}