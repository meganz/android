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
import org.mockito.Mockito.never
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

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
        enabled: Boolean = true,
        onPlayAllClicked: () -> Unit = {},
        modifier: Modifier = Modifier
    ) {
        composeTestRule.setContent {
            VideoPlaylistDetailHeaderView(
                thumbnailList = thumbnailList,
                title = title,
                totalDuration = totalDuration,
                numberOfVideos = numberOfVideos,
                enabled = enabled,
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
            numberOfVideos = 5,
            enabled = true
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
            enabled = true,
            onPlayAllClicked = onPlayAllClicked
        )

        VIDEO_PLAYLIST_DETAIL_HEADER_PLAY_ALL_TEST_TAG.assertIsDisplayedWithTag()
        composeTestRule.onNodeWithTag(VIDEO_PLAYLIST_DETAIL_HEADER_PLAY_ALL_TEST_TAG, true)
            .performClick()
        verify(onPlayAllClicked).invoke()
    }

    @Test
    fun `test that play all button is not displayed when numberOfVideos is null`() {
        setComposeContent(
            title = "My Playlist",
            totalDuration = "10:00",
            numberOfVideos = null
        )

        VIDEO_PLAYLIST_DETAIL_HEADER_INFO_TEST_TAG.assertIsDisplayedWithTag()
        VIDEO_PLAYLIST_DETAIL_HEADER_PLAY_ALL_TEST_TAG.assertDoesNotExist()
    }

    @Test
    fun `test that play all button is not displayed when numberOfVideos is zero`() {
        setComposeContent(
            title = "My Playlist",
            totalDuration = "10:00",
            numberOfVideos = 0
        )

        VIDEO_PLAYLIST_DETAIL_HEADER_INFO_TEST_TAG.assertIsDisplayedWithTag()
        VIDEO_PLAYLIST_DETAIL_HEADER_PLAY_ALL_TEST_TAG.assertDoesNotExist()
    }

    @Test
    fun `test that play all button is displayed when numberOfVideos is non-zero`() {
        setComposeContent(
            title = "My Playlist",
            numberOfVideos = 1
        )

        VIDEO_PLAYLIST_DETAIL_HEADER_PLAY_ALL_TEST_TAG.assertIsDisplayedWithTag()
    }

    @Test
    fun `test that onPlayAllClicked is not invoked when enabled is false and play all is clicked`() {
        val onPlayAllClicked = mock<() -> Unit>()
        setComposeContent(
            numberOfVideos = 3,
            enabled = false,
            onPlayAllClicked = onPlayAllClicked
        )

        composeTestRule.onNodeWithTag(VIDEO_PLAYLIST_DETAIL_HEADER_PLAY_ALL_TEST_TAG, true)
            .performClick()
        verify(onPlayAllClicked, never()).invoke()
    }

    private fun String.assertIsDisplayedWithTag() =
        composeTestRule.onNodeWithTag(this, true).assertIsDisplayed()

    private fun String.assertDoesNotExist() =
        composeTestRule.onNodeWithTag(this, true).assertDoesNotExist()
}