package mega.privacy.android.feature.photos.presentation.albums

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.photos.AlbumId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Compose test class for [AlbumsTabScreen]
 */
@Config(sdk = [33])
@RunWith(AndroidJUnit4::class)
class AlbumsTabScreenComposeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(uiState: AlbumsTabUiState) {
        composeTestRule.setContent {
            AlbumsTabScreen(
                uiState = uiState,
                modifier = Modifier
            )
        }
    }

    @Test
    fun `test that albums are displayed correctly`() {
        val albums = listOf(
            createMockAlbum(id = 1L, title = "Album 1"),
            createMockAlbum(id = 2L, title = "Album 2")
        )
        val uiState = AlbumsTabUiState(albums = albums)
        setComposeContent(uiState)

        // Verify all album grid items are displayed
        albums.forEachIndexed { index, album ->
            composeTestRule
                .onNodeWithTag("$ALBUMS_SCREEN_ALBUM_GRID_ITEM:$index")
                .assertIsDisplayed()

            composeTestRule
                .onNodeWithText(album.title)
                .assertIsDisplayed()
        }
    }

    private fun createMockAlbum(id: Long, title: String): MediaAlbum.User {
        return MediaAlbum.User(
            id = AlbumId(id),
            title = title,
            cover = null,
            creationTime = 0,
            modificationTime = 0,
            isExported = false
        )
    }
}
