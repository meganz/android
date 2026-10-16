package mega.privacy.android.feature.photos.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
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
        modifier: Modifier = Modifier,
        fontScale: Float = 1f,
    ) {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale)
            ) {
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

    @Test
    fun `test that the header is taller than its minimum height when the text does not fit in it`() {
        setComposeContent(
            title = "My Playlist",
            totalDuration = "10:00",
            numberOfVideos = 5
        )

        assertHeaderIsTallerThanItsMinimumHeight()
    }

    @Test
    fun `test that the header is taller than its minimum height when the font scale is doubled`() {
        setComposeContent(
            title = "My Playlist",
            totalDuration = "10:00",
            numberOfVideos = 5,
            fontScale = 2f
        )

        assertHeaderIsTallerThanItsMinimumHeight()
    }

    @Test
    fun `test that the header keeps its minimum height when there is no text to show`() {
        setComposeContent()

        composeTestRule.onNodeWithTag(VIDEO_PLAYLIST_DETAIL_HEADER_INFO_TEST_TAG, true)
            .assertHeightIsEqualTo(VIDEO_PLAYLIST_DETAIL_HEADER_INFO_MIN_HEIGHT)
    }

    @Test
    fun `test that the play all button keeps its minimum height when the font scale is doubled`() {
        setComposeContent(
            title = "My Playlist",
            totalDuration = "10:00",
            numberOfVideos = 5,
            fontScale = 2f
        )

        composeTestRule.onNodeWithTag(VIDEO_PLAYLIST_DETAIL_HEADER_PLAY_ALL_TEST_TAG, true)
            .assertHeightIsAtLeast(VIDEO_PLAYLIST_DETAIL_HEADER_PLAY_ALL_MIN_HEIGHT)
    }

    private fun assertHeaderIsTallerThanItsMinimumHeight() {
        val headerHeight = composeTestRule
            .onNodeWithTag(VIDEO_PLAYLIST_DETAIL_HEADER_INFO_TEST_TAG, useUnmergedTree = true)
            .getUnclippedBoundsInRoot().height

        assertThat(headerHeight).isGreaterThan(VIDEO_PLAYLIST_DETAIL_HEADER_INFO_MIN_HEIGHT)
    }

    private fun String.assertIsDisplayedWithTag() =
        composeTestRule.onNodeWithTag(this, true).assertIsDisplayed()

    private fun String.assertDoesNotExist() =
        composeTestRule.onNodeWithTag(this, true).assertDoesNotExist()
}