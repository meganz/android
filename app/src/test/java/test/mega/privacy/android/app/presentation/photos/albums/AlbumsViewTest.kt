package test.mega.privacy.android.app.presentation.photos.albums

import androidx.compose.runtime.mutableStateOf
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
import mega.privacy.android.core.ui.test.AnalyticsTestRule
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import test.mega.privacy.android.app.fromId
import test.mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class AlbumsViewTest {

    val composeRule = createComposeRule()

    @get:Rule
    var ruleChain: RuleChain = RuleChain.outerRule(AnalyticsTestRule()).around(composeRule)

    private val isUserAlbumsEnabled = { true }

    @Test
    fun `test that clicking create album fab opens the dialog`() {
        val onDialogPositiveButtonClicked = mock<(String) -> Unit>()
        val setShowCreateAlbumDialog = mock<(Boolean) -> Unit>()

        val showCreateAlbumDialog = mutableStateOf(false)

        composeRule.setContent {
            AlbumsView(
                albumsViewState = AlbumsViewState(showCreateAlbumDialog = showCreateAlbumDialog.value),
                openAlbum = {},
                downloadPhoto = mock(),
                setShowCreateAlbumDialog = setShowCreateAlbumDialog,
                onDialogPositiveButtonClicked = onDialogPositiveButtonClicked,
                isUserAlbumsEnabled = isUserAlbumsEnabled,
            )
        }

        composeRule.onNodeWithContentDescription("Create new album").performClick()

        verify(setShowCreateAlbumDialog).invoke(true)
        showCreateAlbumDialog.value = true
        composeRule.onNodeWithText(R.string.photos_album_creation_dialog_title)
            .onParent()
            .onParent()
            .assertIsDisplayed()
    }

    @Test
    fun `test that clicking the fab will provide the input placeholder text`() {
        val setDialogInputPlaceholder = mock<(String) -> Unit>()
        val defaultText = fromId(R.string.photos_album_creation_dialog_input_placeholder)
        val onDialogPositiveButtonClicked = mock<(String) -> Unit>()

        composeRule.setContent {
            AlbumsView(
                albumsViewState = AlbumsViewState(),
                openAlbum = {},
                downloadPhoto = mock(),
                onDialogPositiveButtonClicked = onDialogPositiveButtonClicked,
                setDialogInputPlaceholder = setDialogInputPlaceholder,
                isUserAlbumsEnabled = isUserAlbumsEnabled,
            )
        }

        composeRule.onNodeWithContentDescription("Create new album").performClick()

        verify(setDialogInputPlaceholder).invoke(defaultText)
    }

    @Test
    fun `test that successful album creation and empty media will not trigger selection activity open`() {
        val setDialogInputPlaceholder = mock<(String) -> Unit>()
        val onDialogPositiveButtonClicked = mock<(String) -> Unit>()
        val albumsViewState = AlbumsViewState(isAlbumCreatedSuccessfully = true)
        val openPhotosSelectionActivity = mock<(AlbumId) -> Unit>()
        composeRule.setContent {
            AlbumsView(
                albumsViewState = albumsViewState,
                openAlbum = {},
                downloadPhoto = mock(),
                onDialogPositiveButtonClicked = onDialogPositiveButtonClicked,
                setDialogInputPlaceholder = setDialogInputPlaceholder,
                isUserAlbumsEnabled = isUserAlbumsEnabled,
                allPhotos = emptyList(),
                openPhotosSelectionActivity = openPhotosSelectionActivity,
            )
        }

        verifyNoInteractions(openPhotosSelectionActivity)
    }

    @Test
    fun `test that successful album creation and non empty media will trigger selection activity open`() {
        val setDialogInputPlaceholder = mock<(String) -> Unit>()
        val onDialogPositiveButtonClicked = mock<(String) -> Unit>()
        val currentAlbumId = AlbumId(1)
        val albumsViewState = AlbumsViewState(
            isAlbumCreatedSuccessfully = true,
            currentAlbum = createUserAlbum(id = currentAlbumId)
        )
        val openPhotosSelectionActivity = mock<(AlbumId) -> Unit>()
        composeRule.setContent {
            AlbumsView(
                albumsViewState = albumsViewState,
                openAlbum = {},
                downloadPhoto = mock(),
                onDialogPositiveButtonClicked = onDialogPositiveButtonClicked,
                setDialogInputPlaceholder = setDialogInputPlaceholder,
                isUserAlbumsEnabled = isUserAlbumsEnabled,
                allPhotos = listOf<Photo.Image>(mock()),
                openPhotosSelectionActivity = openPhotosSelectionActivity,
            )
        }

        verify(openPhotosSelectionActivity).invoke(currentAlbumId)
    }

    @Test
    fun `test that clicking negative button on dialog calls the correct function`() {
        val onDismissRequest = mock<() -> Unit>()
        val onDialogPositiveButtonClicked = mock<(String) -> Unit>()
        composeRule.setContent {
            CreateNewAlbumDialog(
                titleResID = R.string.photos_album_creation_dialog_title,
                positiveButtonTextResID = R.string.general_create,
                onDismissRequest = onDismissRequest,
                onDialogPositiveButtonClicked = onDialogPositiveButtonClicked
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
                titleResID = R.string.photos_album_creation_dialog_title,
                positiveButtonTextResID = R.string.general_create,
                onDialogPositiveButtonClicked = onDialogPositiveButtonClicked,
                onDismissRequest = onDismissRequest,
            )
        }

        composeRule.onNodeWithText(R.string.general_create).performClick()

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
                titleResID = R.string.photos_album_creation_dialog_title,
                positiveButtonTextResID = R.string.general_create,
                onDialogPositiveButtonClicked = onDialogPositiveButtonClicked,
                onDismissRequest = onDismissRequest,
                inputPlaceHolderText = { fromId(inputPlaceholderText) }
            )
        }

        composeRule.onNodeWithText(inputPlaceholderText)
            .performTextInput(expectedAlbumName)
        composeRule.onNodeWithText(R.string.general_create).performClick()

        verify(onDialogPositiveButtonClicked).invoke(expectedAlbumName)
    }

    private fun createUserAlbum(
        id: AlbumId = AlbumId(0L),
        title: String = "",
        cover: Photo? = null,
        creationTime: Long = 0L,
        modificationTime: Long = 0L,
        isExported: Boolean = false,
    ): Album.UserAlbum =
        Album.UserAlbum(id, title, cover, creationTime, modificationTime, isExported)
}