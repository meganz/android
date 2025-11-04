package mega.privacy.android.feature.photos.presentation.albums

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumUiState
import mega.privacy.android.shared.resources.R as sharedResR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

/**
 * Compose test class for [AlbumsTabScreen]
 */
@Config(sdk = [33])
@RunWith(AndroidJUnit4::class)
class AlbumsTabScreenComposeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry
        .getInstrumentation()
        .targetContext

    private fun setComposeContent(
        uiState: AlbumsTabUiState,
        addNewAlbum: (String) -> Unit = {},
        showNewAlbumDialogEvent: StateEvent = consumed,
        resetNewAlbumDialogEvent: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            AlbumsTabScreen(
                uiState = uiState,
                modifier = Modifier,
                addNewAlbum = addNewAlbum,
                showNewAlbumDialogEvent = showNewAlbumDialogEvent,
                resetNewAlbumDialogEvent = resetNewAlbumDialogEvent
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

    @Test
    fun `test that new album dialog is visible when event triggered`() {
        val uiState = AlbumsTabUiState(albums = emptyList())

        setComposeContent(uiState, showNewAlbumDialogEvent = triggered)

        composeTestRule
            .onNodeWithTag(ALBUMS_SCREEN_ADD_NEW_ALBUM_DIALOG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that on confirm dialog should trigger add new album callback`() {
        val uiState = AlbumsTabUiState(albums = emptyList())
        val mockCallback: (String) -> Unit = mock()

        setComposeContent(
            uiState = uiState,
            showNewAlbumDialogEvent = triggered,
            addNewAlbum = mockCallback
        )

        composeTestRule
            .onNodeWithTag(ALBUMS_SCREEN_ADD_NEW_ALBUM_DIALOG)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(context.getString(sharedResR.string.media_add_new_album_dialog_positive_button))
            .performClick()

        verify(mockCallback).invoke(any())
    }

    private fun createMockAlbum(id: Long, title: String): AlbumUiState {
        return AlbumUiState(
            id = id,
            title = title,
            cover = null
        )
    }
}
