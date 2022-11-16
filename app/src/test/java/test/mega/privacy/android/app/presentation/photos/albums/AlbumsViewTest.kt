package test.mega.privacy.android.app.presentation.photos.albums

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.albums.model.AlbumsViewState
import mega.privacy.android.app.presentation.photos.albums.view.AlbumsView
import mega.privacy.android.app.presentation.photos.albums.view.CreateNewAlbumDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import test.mega.privacy.android.app.fromId
import test.mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class AlbumsViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val isUserAlbumsEnabled = { true }

    @Test
    fun `test that clicking create album fab opens the dialog`() {
        composeRule.setContent {
            AlbumsView(
                albumsViewState = AlbumsViewState(),
                openAlbum = {},
                downloadPhoto = mock(),
                onDialogPositiveButtonClicked = {},
                isUserAlbumsEnabled = isUserAlbumsEnabled,
            )
        }

        composeRule.onNodeWithContentDescription("Create new album").performClick()

        composeRule.onNodeWithText(R.string.photos_album_creation_dialog_title)
            .onParent()
            .onParent()
            .assertIsDisplayed()
    }

    @Test
    fun `test that clicking the fab will provide the input placeholder text`() {
        val setDialogInputPlaceholder = mock<(String) -> Unit>()
        val defaultText = "abc"
        composeRule.setContent {
            AlbumsView(
                albumsViewState = AlbumsViewState(),
                openAlbum = {},
                downloadPhoto = mock(),
                onDialogPositiveButtonClicked = {},
                setDialogInputPlaceholder = setDialogInputPlaceholder,
                isUserAlbumsEnabled = isUserAlbumsEnabled,
            )
        }

        composeRule.onNodeWithContentDescription("Create new album").performClick()

        verify(setDialogInputPlaceholder).invoke(defaultText)
    }

    @Test
    fun `test that clicking negative button on dialog calls the correct function`() {
        val onDismissRequest = mock<() -> Unit>()
        composeRule.setContent {
            CreateNewAlbumDialog(
                onDismissRequest = onDismissRequest,
                onDialogPositiveButtonClicked = {}
            )
        }

        composeRule.onNodeWithText(R.string.general_cancel).performClick()

        verify(onDismissRequest).invoke()
    }

    @Test
    fun `test that without input clicking the positive dialog button calls the correct function`() {
        val onDialogPositiveButtonClicked = mock<(String) -> Unit>()
        val onDismissRequest = mock<() -> Unit>()

        composeRule.setContent {
            CreateNewAlbumDialog(
                onDialogPositiveButtonClicked = onDialogPositiveButtonClicked,
                onDismissRequest = onDismissRequest,
            )
        }

        composeRule.onNodeWithText(R.string.general_create).performClick()

        verify(onDismissRequest).invoke()
        verify(onDialogPositiveButtonClicked).invoke("")
    }

    @Test
    fun `test that with input clicking the positive dialog button calls the correct function`() {
        val expectedAlbumName = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val inputPlaceholderText = R.string.photos_album_creation_dialog_input_placeholder
        val onDialogPositiveButtonClicked = mock<(String) -> Unit>()
        val onDismissRequest = mock<() -> Unit>()
        composeRule.setContent {
            CreateNewAlbumDialog(
                onDialogPositiveButtonClicked = onDialogPositiveButtonClicked,
                onDismissRequest = onDismissRequest,
                inputPlaceHolderText = { fromId(inputPlaceholderText) }
            )
        }

        composeRule.onNodeWithText(inputPlaceholderText)
            .performTextInput(expectedAlbumName)
        composeRule.onNodeWithText(R.string.general_create).performClick()

        verify(onDismissRequest).invoke()
        verify(onDialogPositiveButtonClicked).invoke(expectedAlbumName)
    }
}