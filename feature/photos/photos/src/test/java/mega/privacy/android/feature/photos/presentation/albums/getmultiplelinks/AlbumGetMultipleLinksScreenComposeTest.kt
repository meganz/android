package mega.privacy.android.feature.photos.presentation.albums.getmultiplelinks

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.feature.photos.presentation.albums.getlink.AlbumSummary
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.annotation.Config

/**
 * Compose test class for [AlbumGetMultipleLinksScreen]
 */
@Config(sdk = [33])
@RunWith(AndroidJUnit4::class)
class AlbumGetMultipleLinksScreenComposeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val snackbarQueue = mock<SnackbarEventQueue>()

    @Test
    fun `test that placeholder items are displayed when album summaries are empty`() {
        composeTestRule.setContent {
            AlbumGetMultipleLinksContent(
                albumSummaries = emptyMap(),
                links = emptyMap(),
                albumLinksList = emptyList(),
                onDownloadImage = { _, _ -> },
                isSystemInDarkTheme = false,
                snackbarEventQueue = snackbarQueue,
            )
        }

        composeTestRule.onNodeWithTag("$ALBUM_GET_MULTIPLE_LINKS_PLACEHOLDER_ITEM_TAG:0")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("$ALBUM_GET_MULTIPLE_LINKS_PLACEHOLDER_ITEM_TAG:1")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("$ALBUM_GET_MULTIPLE_LINKS_ROW_ITEM_TAG:1")
            .assertDoesNotExist()
    }

    @Test
    fun `test that row items are displayed when album summaries are not empty`() {
        val albumId1 = AlbumId(1L)
        val albumId2 = AlbumId(2L)
        val album1 = Album.UserAlbum(
            id = albumId1,
            title = "Album 1",
            cover = null,
            creationTime = 0L,
            modificationTime = 0L,
            isExported = false,
        )
        val album2 = Album.UserAlbum(
            id = albumId2,
            title = "Album 2",
            cover = null,
            creationTime = 0L,
            modificationTime = 0L,
            isExported = false,
        )
        val albumSummaries = mapOf(
            albumId1 to AlbumSummary(album = album1, numPhotos = 5),
            albumId2 to AlbumSummary(album = album2, numPhotos = 10),
        )
        val links = mapOf(
            albumId1 to AlbumLink("https://mega.nz/album1"),
            albumId2 to AlbumLink("https://mega.nz/album2"),
        )

        composeTestRule.setContent {
            AlbumGetMultipleLinksContent(
                albumSummaries = albumSummaries,
                links = links,
                albumLinksList = listOf("https://mega.nz/album1", "https://mega.nz/album2"),
                onDownloadImage = { _, _ -> },
                isSystemInDarkTheme = false,
                snackbarEventQueue = snackbarQueue,
            )
        }

        composeTestRule.onNodeWithTag("$ALBUM_GET_MULTIPLE_LINKS_ROW_ITEM_TAG:1")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("$ALBUM_GET_MULTIPLE_LINKS_ROW_ITEM_TAG:2")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("$ALBUM_GET_MULTIPLE_LINKS_PLACEHOLDER_ITEM_TAG:0")
            .assertDoesNotExist()
    }

    @Test
    fun `test that copy all button is displayed when links are not empty`() {
        val albumId = AlbumId(1L)
        val album = Album.UserAlbum(
            id = albumId,
            title = "Album 1",
            cover = null,
            creationTime = 0L,
            modificationTime = 0L,
            isExported = false,
        )
        val albumSummaries = mapOf(
            albumId to AlbumSummary(album = album, numPhotos = 5),
        )
        val links = mapOf(
            albumId to AlbumLink("https://mega.nz/album1"),
        )

        composeTestRule.setContent {
            AlbumGetMultipleLinksContent(
                albumSummaries = albumSummaries,
                links = links,
                albumLinksList = listOf("https://mega.nz/album1"),
                onDownloadImage = { _, _ -> },
                isSystemInDarkTheme = false,
                snackbarEventQueue = snackbarQueue,
            )
        }

        composeTestRule.onNodeWithTag(ALBUM_GET_MULTIPLE_LINKS_COPY_ALL_BUTTON_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that copy all button is not displayed when links are empty`() {
        composeTestRule.setContent {
            AlbumGetMultipleLinksContent(
                albumSummaries = emptyMap(),
                links = emptyMap(),
                albumLinksList = emptyList(),
                onDownloadImage = { _, _ -> },
                isSystemInDarkTheme = false,
                snackbarEventQueue = snackbarQueue,
            )
        }

        composeTestRule.onNodeWithTag(ALBUM_GET_MULTIPLE_LINKS_COPY_ALL_BUTTON_TAG)
            .assertDoesNotExist()
    }
}
