package mega.privacy.android.feature.photos.presentation.albums

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.photos.AlbumId
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

    @get:Rule
    val analyticsRule = AnalyticsTestRule()

    private val context = InstrumentationRegistry
        .getInstrumentation()
        .targetContext

    private fun setComposeContent(
        uiState: AlbumsTabUiState,
        addNewAlbum: (String) -> Unit = {},
        deleteAlbums: () -> Unit = {},
        onNavigate: (NavKey) -> Unit = {},
        showNewAlbumDialogEvent: StateEvent = consumed,
        resetNewAlbumDialogEvent: () -> Unit = {},
        resetErrorMessage: () -> Unit = {},
        resetAddNewAlbumSuccess: () -> Unit = {},
        resetNavigationEvent: () -> Unit = {},
        resetDeleteAlbumsConfirmationEvent: () -> Unit = {},
        onAlbumSelectionToggle: (MediaAlbum.User) -> Unit = {}
    ) {
        composeTestRule.setContent {
            AlbumsTabScreen(
                uiState = uiState,
                modifier = Modifier,
                addNewAlbum = addNewAlbum,
                deleteAlbums = deleteAlbums,
                onNavigate = onNavigate,
                showNewAlbumDialogEvent = showNewAlbumDialogEvent,
                resetNewAlbumDialogEvent = resetNewAlbumDialogEvent,
                resetErrorMessage = resetErrorMessage,
                resetAddNewAlbumSuccess = resetAddNewAlbumSuccess,
                resetNavigationEvent = resetNavigationEvent,
                resetDeleteAlbumsConfirmationEvent = resetDeleteAlbumsConfirmationEvent,
                onAlbumSelectionToggle = onAlbumSelectionToggle
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
                .onNodeWithText(album.title.get(context))
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

    @Test
    fun `test that remove album confirmation dialog is visible when deleteAlbumsConfirmationEvent is triggered`() {
        val selectedAlbum = createMockUserAlbum(1L, "Album 1")
        val uiState = AlbumsTabUiState(
            albums = emptyList(),
            selectedUserAlbums = setOf(selectedAlbum),
            deleteAlbumsConfirmationEvent = triggered
        )

        setComposeContent(uiState = uiState)

        composeTestRule
            .onNodeWithTag(ALBUMS_SCREEN_REMOVE_ALBUM_CONFIRMATION_DIALOG)
            .assertIsDisplayed()

        // Verify singular title is displayed for single album
        composeTestRule
            .onNodeWithText(context.getString(sharedResR.string.delete_album_singular_confirmation_dialog_title))
            .assertIsDisplayed()
    }

    @Test
    fun `test that remove album confirmation dialog shows plural title for multiple albums`() {
        val selectedAlbums = setOf(
            createMockUserAlbum(1L, "Album 1"),
            createMockUserAlbum(2L, "Album 2")
        )
        val uiState = AlbumsTabUiState(
            albums = emptyList(),
            selectedUserAlbums = selectedAlbums,
            deleteAlbumsConfirmationEvent = triggered
        )

        setComposeContent(uiState = uiState)

        composeTestRule
            .onNodeWithTag(ALBUMS_SCREEN_REMOVE_ALBUM_CONFIRMATION_DIALOG)
            .assertIsDisplayed()

        // Verify plural title is displayed for multiple albums
        composeTestRule
            .onNodeWithText(context.getString(sharedResR.string.delete_albums_multiple_confirmation_dialog_title))
            .assertIsDisplayed()
    }

    @Test
    fun `test that remove album confirmation dialog is not visible when event is consumed`() {
        val uiState = AlbumsTabUiState(
            albums = emptyList(),
            deleteAlbumsConfirmationEvent = consumed
        )

        setComposeContent(uiState = uiState)

        composeTestRule
            .onNodeWithTag(ALBUMS_SCREEN_REMOVE_ALBUM_CONFIRMATION_DIALOG)
            .assertIsNotDisplayed()
    }

    @Test
    fun `test that on confirm remove album dialog should trigger deleteAlbums and resetDeleteAlbumsConfirmationEvent`() {
        val selectedAlbum = createMockUserAlbum(1L, "Album 1")
        val uiState = AlbumsTabUiState(
            albums = emptyList(),
            selectedUserAlbums = setOf(selectedAlbum),
            deleteAlbumsConfirmationEvent = triggered
        )
        val mockDeleteAlbums: () -> Unit = mock()
        val mockResetEvent: () -> Unit = mock()

        setComposeContent(
            uiState = uiState,
            deleteAlbums = mockDeleteAlbums,
            resetDeleteAlbumsConfirmationEvent = mockResetEvent
        )

        composeTestRule
            .onNodeWithTag(ALBUMS_SCREEN_REMOVE_ALBUM_CONFIRMATION_DIALOG)
            .assertIsDisplayed()

        // Click the positive button (Delete)
        composeTestRule
            .onNodeWithText(context.getString(sharedResR.string.delete_album_confirmation_positive_button_text))
            .performClick()

        verify(mockDeleteAlbums).invoke()
        verify(mockResetEvent).invoke()
    }

    @Test
    fun `test that on dismiss remove album dialog should trigger resetDeleteAlbumsConfirmationEvent`() {
        val selectedAlbum = createMockUserAlbum(1L, "Album 1")
        val uiState = AlbumsTabUiState(
            albums = emptyList(),
            selectedUserAlbums = setOf(selectedAlbum),
            deleteAlbumsConfirmationEvent = triggered
        )
        val mockResetEvent: () -> Unit = mock()

        setComposeContent(
            uiState = uiState,
            resetDeleteAlbumsConfirmationEvent = mockResetEvent
        )

        composeTestRule
            .onNodeWithTag(ALBUMS_SCREEN_REMOVE_ALBUM_CONFIRMATION_DIALOG)
            .assertIsDisplayed()

        // Click the negative button (Cancel)
        composeTestRule
            .onNodeWithText(context.getString(sharedResR.string.general_dialog_cancel_button))
            .performClick()

        verify(mockResetEvent).invoke()
    }

    private fun createMockUserAlbum(id: Long, title: String): MediaAlbum.User {
        return MediaAlbum.User(
            id = AlbumId(id),
            title = title,
            cover = null,
            creationTime = 0L,
            modificationTime = 0L,
            isExported = false
        )
    }

    private fun createMockAlbum(id: Long, title: String): AlbumUiState {
        val mediaAlbum = MediaAlbum.User(
            id = AlbumId(id),
            title = title,
            cover = null,
            creationTime = 0L,
            modificationTime = 0L,
            isExported = false
        )
        return AlbumUiState(
            mediaAlbum = mediaAlbum,
            title = LocalizedText.Literal(title),
            isExported = false,
            cover = null
        )
    }
}
