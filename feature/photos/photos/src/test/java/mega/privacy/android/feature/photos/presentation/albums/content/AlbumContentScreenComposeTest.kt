package mega.privacy.android.feature.photos.presentation.albums.content

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.palm.composestateevents.triggered
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.model.NodeActionState
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.DownloadPhotoResult
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.photos.extensions.LocalDownloadPhotoResultMock
import mega.privacy.android.feature.photos.model.AlbumSortConfiguration
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.presentation.albums.content.model.AlbumContentSelectionAction
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumTitle
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumUiState
import mega.privacy.android.feature.photos.presentation.albums.model.UIAlbum
import mega.privacy.android.feature.photos.presentation.albums.view.ALBUM_DYNAMIC_CONTENT_GRID_SORT_ITEM
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.AlbumContentPreviewNavKey
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.robolectric.annotation.Config

/**
 * Compose test class for [AlbumContentScreen]
 */
@Config(sdk = [33])
@RunWith(AndroidJUnit4::class)
class AlbumContentScreenComposeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val snackbarEventQueue: SnackbarEventQueue = mock()

    @After
    fun resetMocks() {
        reset(snackbarEventQueue)
    }

    private fun setComposeContent(
        uiState: AlbumContentUiState = AlbumContentUiState(),
        actionState: NodeActionState = NodeActionState(),
        actionHandler: (MenuAction, List<TypedNode>) -> Unit = { _, _ -> },
        onBack: () -> Unit = {},
        togglePhotoSelection: (PhotoUiState) -> Unit = {},
        selectAll: () -> Unit = {},
        deselectAll: () -> Unit = {},
        savePhotosToDevice: () -> Unit = {},
        resetSavePhotosToDeviceEvent: () -> Unit = {},
        sharePhotos: () -> Unit = {},
        resetSharePhotosEvent: () -> Unit = {},
        sendPhotosToChatEvent: () -> Unit = {},
        resetSendPhotosToChatEvent: () -> Unit = {},
        hidePhotosEvent: () -> Unit = {},
        resetHidePhotosEvent: () -> Unit = {},
        removePhotos: () -> Unit = {},
        deleteAlbum: () -> Unit = {},
        resetDeleteAlbumSuccessEvent: () -> Unit = {},
        hideDeleteConfirmation: () -> Unit = {},
        renameAlbum: (String) -> Unit = {},
        resetUpdateAlbumNameErrorMessage: () -> Unit = {},
        resetShowUpdateAlbumName: () -> Unit = {},
        selectAlbumCover: (AlbumId) -> Unit = {},
        resetSelectAlbumCoverEvent: () -> Unit = {},
        resetManageLink: () -> Unit = {},
        hideRemoveLinkConfirmation: () -> Unit = {},
        removeLink: () -> Unit = {},
        resetLinkRemovedSuccessEvent: () -> Unit = {},
        openGetLink: (AlbumId, Boolean) -> Unit = { _, _ -> },
        handleBottomSheetAction: (AlbumContentSelectionAction) -> Unit = {},
        navigateToPaywall: () -> Unit = {},
        resetPaywallEvent: () -> Unit = {},
        sortPhotos: (AlbumSortConfiguration) -> Unit = {},
        previewPhoto: (PhotoUiState) -> Unit = {},
        resetPreviewPhoto: () -> Unit = {},
        navigateToPhotoPreview: (AlbumContentPreviewNavKey) -> Unit = {},
        onTransfer: (TransferTriggerEvent) -> Unit = {},
        consumeDownloadEvent: () -> Unit = {},
        consumeInfoToShowEvent: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDownloadPhotoResultMock provides DownloadPhotoResult.Idle) {
                AlbumContentScreen(
                    uiState = uiState,
                    actionState = actionState,
                    actionHandler = actionHandler,
                    onBack = onBack,
                    togglePhotoSelection = togglePhotoSelection,
                    selectAll = selectAll,
                    deselectAll = deselectAll,
                    savePhotosToDevice = savePhotosToDevice,
                    resetSavePhotosToDeviceEvent = resetSavePhotosToDeviceEvent,
                    sharePhotos = sharePhotos,
                    resetSharePhotosEvent = resetSharePhotosEvent,
                    sendPhotosToChatEvent = sendPhotosToChatEvent,
                    resetSendPhotosToChatEvent = resetSendPhotosToChatEvent,
                    hidePhotosEvent = hidePhotosEvent,
                    resetHidePhotosEvent = resetHidePhotosEvent,
                    removePhotos = removePhotos,
                    deleteAlbum = deleteAlbum,
                    hideDeleteConfirmation = hideDeleteConfirmation,
                    renameAlbum = renameAlbum,
                    resetUpdateAlbumNameErrorMessage = resetUpdateAlbumNameErrorMessage,
                    resetShowUpdateAlbumName = resetShowUpdateAlbumName,
                    resetDeleteAlbumSuccessEvent = resetDeleteAlbumSuccessEvent,
                    selectAlbumCover = selectAlbumCover,
                    resetSelectAlbumCoverEvent = resetSelectAlbumCoverEvent,
                    resetManageLink = resetManageLink,
                    hideRemoveLinkConfirmation = hideRemoveLinkConfirmation,
                    removeLink = removeLink,
                    resetLinkRemovedSuccessEvent = resetLinkRemovedSuccessEvent,
                    openGetLink = openGetLink,
                    handleBottomSheetAction = handleBottomSheetAction,
                    navigateToPaywall = navigateToPaywall,
                    resetPaywallEvent = resetPaywallEvent,
                    sortPhotos = sortPhotos,
                    previewPhoto = previewPhoto,
                    resetPreviewPhoto = resetPreviewPhoto,
                    navigateToPhotoPreview = navigateToPhotoPreview,
                    onTransfer = onTransfer,
                    consumeDownloadEvent = consumeDownloadEvent,
                    consumeInfoToShowEvent = consumeInfoToShowEvent,
                    snackbarQueue = snackbarEventQueue,
                )
            }
        }
    }

    @Test
    fun `test that default toolbar is displayed when no photos are selected`() {
        val albumTitle = "My Album"
        val albumUiState = createMockAlbumUiState(title = albumTitle)
        val uiState = AlbumContentUiState(
            uiAlbum = albumUiState,
            selectedPhotos = persistentSetOf()
        )

        setComposeContent(uiState)

        composeTestRule
            .onNodeWithTag(ALBUM_CONTENT_SCREEN_DEFAULT_TOOLBAR)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(albumTitle)
            .assertIsDisplayed()
    }

    @Test
    fun `test that selection toolbar is displayed when photos are selected`() {
        val photos = listOf(
            createMockPhoto(id = 1L),
            createMockPhoto(id = 2L),
            createMockPhoto(id = 3L)
        )
        val selectedPhotos = setOf(photos[0], photos[1])
        val uiState = AlbumContentUiState(
            photos = photos.toImmutableList(),
            selectedPhotos = selectedPhotos.toImmutableSet()
        )

        setComposeContent(uiState)

        composeTestRule
            .onNodeWithTag(ALBUM_CONTENT_SCREEN_SELECTION_TOOLBAR)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("2")
            .assertIsDisplayed()
    }

    @Test
    fun `test that default toolbar is hidden when photos are selected`() {
        val photos = listOf(createMockPhoto(id = 1L))
        val uiState = AlbumContentUiState(
            uiAlbum = createMockAlbumUiState(),
            photos = photos.toImmutableList(),
            selectedPhotos = setOf(photos[0]).toImmutableSet()
        )

        setComposeContent(uiState)

        composeTestRule
            .onNodeWithTag(ALBUM_CONTENT_SCREEN_DEFAULT_TOOLBAR)
            .assertDoesNotExist()
    }

    @Test
    fun `test that selection bottom bar is displayed when photos are selected`() {
        val photos = listOf(
            createMockPhoto(id = 1L),
            createMockPhoto(id = 2L)
        )
        val selectedPhotos = setOf(photos[0])
        val uiState = AlbumContentUiState(
            photos = photos.toImmutableList(),
            selectedPhotos = selectedPhotos.toImmutableSet()
        )

        setComposeContent(uiState)

        composeTestRule
            .onNodeWithTag(ALBUM_CONTENT_SCREEN_SELECTION_BOTTOM_BAR)
            .assertIsDisplayed()
    }

    @Test
    fun `test that selection bottom bar is not visible when no photos are selected`() {
        val photos = listOf(
            createMockPhoto(id = 1L),
            createMockPhoto(id = 2L)
        )
        val uiState = AlbumContentUiState(
            photos = photos.toImmutableList(),
            selectedPhotos = persistentSetOf()
        )

        setComposeContent(uiState)

        composeTestRule
            .onNodeWithTag(ALBUM_CONTENT_SCREEN_SELECTION_BOTTOM_BAR)
            .assertIsNotDisplayed()
    }

    @Test
    fun `test that selection toolbar displays correct count of selected photos`() {
        val photos = listOf(
            createMockPhoto(id = 1L),
            createMockPhoto(id = 2L),
            createMockPhoto(id = 3L),
            createMockPhoto(id = 4L)
        )
        val selectedCount = 3
        val selectedPhotos = photos.take(selectedCount).toSet()
        val uiState = AlbumContentUiState(
            photos = photos.toImmutableList(),
            selectedPhotos = selectedPhotos.toImmutableSet()
        )

        setComposeContent(uiState)

        composeTestRule
            .onNodeWithTag(ALBUM_CONTENT_SCREEN_SELECTION_TOOLBAR)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(selectedCount.toString())
            .assertIsDisplayed()
    }

    @Test
    fun `test that album title is displayed correctly in default toolbar`() {
        val albumTitle = "Vacation 2024"
        val albumUiState = createMockAlbumUiState(title = albumTitle)
        val uiState = AlbumContentUiState(
            uiAlbum = albumUiState,
            selectedPhotos = persistentSetOf()
        )

        setComposeContent(uiState)

        composeTestRule
            .onNodeWithTag(ALBUM_CONTENT_SCREEN_DEFAULT_TOOLBAR)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(albumTitle)
            .assertIsDisplayed()
    }

    @Test
    fun `test that both toolbar and bottom bar are shown in selection mode`() {
        val photos = listOf(createMockPhoto(id = 1L))
        val uiState = AlbumContentUiState(
            photos = photos.toImmutableList(),
            selectedPhotos = setOf(photos[0]).toImmutableSet()
        )

        setComposeContent(uiState)

        // Both should be displayed in selection mode
        composeTestRule
            .onNodeWithTag(ALBUM_CONTENT_SCREEN_SELECTION_TOOLBAR)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(ALBUM_CONTENT_SCREEN_SELECTION_BOTTOM_BAR)
            .assertIsDisplayed()
    }

    @Test
    fun `test that only default toolbar is shown when no photos are selected`() {
        val uiState = AlbumContentUiState(
            uiAlbum = createMockAlbumUiState(),
            photos = persistentListOf(),
            selectedPhotos = persistentSetOf()
        )

        setComposeContent(uiState)

        // Default toolbar should be visible
        composeTestRule
            .onNodeWithTag(ALBUM_CONTENT_SCREEN_DEFAULT_TOOLBAR)
            .assertIsDisplayed()

        // Selection toolbar should not exist
        composeTestRule
            .onNodeWithTag(ALBUM_CONTENT_SCREEN_SELECTION_TOOLBAR)
            .assertDoesNotExist()

        // Bottom bar should not be visible
        composeTestRule
            .onNodeWithTag(ALBUM_CONTENT_SCREEN_SELECTION_BOTTOM_BAR)
            .assertIsNotDisplayed()
    }

    @Test
    fun `test that delete photos dialog is displayed when triggered`() {
        composeTestRule.setContent {
            RemovePhotosConfirmationDialog(
                isVisible = true,
                onConfirm = {},
                onDismiss = {}
            )
        }

        composeTestRule
            .onNodeWithTag(ALBUM_CONTENT_SCREEN_DELETE_PHOTOS_DIALOG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that delete photos dialog is not displayed when not visible`() {
        composeTestRule.setContent {
            RemovePhotosConfirmationDialog(
                isVisible = false,
                onConfirm = {},
                onDismiss = {}
            )
        }

        composeTestRule
            .onNodeWithTag(ALBUM_CONTENT_SCREEN_DELETE_PHOTOS_DIALOG)
            .assertDoesNotExist()
    }

    @Test
    fun `test that on more options clicked triggers bottom sheet`() {
        val uiState = AlbumContentUiState(
            uiAlbum = createMockAlbumUiState(),
            photos = persistentListOf(),
            selectedPhotos = persistentSetOf()
        )

        setComposeContent(uiState)

        composeTestRule
            .onNodeWithTag(AlbumContentSelectionAction.More.testTag)
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(ALBUM_CONTENT_SCREEN_MORE_OPTIONS_BOTTOM_SHEET)
            .assertIsDisplayed()
    }

    @Test
    fun `test that album options bottom sheet is not displayed when not visible`() {
        val albumUiState = createMockAlbumUiState()

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDownloadPhotoResultMock provides DownloadPhotoResult.Idle) {
                AlbumOptionsBottomSheet(
                    isVisible = false,
                    onDismiss = {},
                    albumUiState = albumUiState,
                    onAction = {}
                )
            }
        }

        composeTestRule
            .onNodeWithTag(ALBUM_CONTENT_SCREEN_MORE_OPTIONS_BOTTOM_SHEET)
            .assertDoesNotExist()
    }

    @Test
    fun `test that album options bottom sheet displays album title`() {
        val albumTitle = "My Vacation Album"
        val albumUiState = createMockAlbumUiState(title = albumTitle)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDownloadPhotoResultMock provides DownloadPhotoResult.Idle) {
                AlbumOptionsBottomSheet(
                    isVisible = true,
                    onDismiss = {},
                    albumUiState = albumUiState,
                    onAction = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(albumTitle)
            .assertIsDisplayed()
    }

    @Test
    fun `test that delete album confirmation dialog is visible when showDeleteConfirmation is triggered`() {
        val mockPhotos = listOf(
            createMockPhoto(1),
            createMockPhoto(2),
            createMockPhoto(3)
        )
        val uiState = AlbumContentUiState(
            uiAlbum = createMockAlbumUiState(),
            photos = mockPhotos.toImmutableList(),
            selectedPhotos = persistentSetOf(),
            showDeleteAlbumConfirmation = triggered
        )

        setComposeContent(uiState = uiState)

        // Verify delete album confirmation dialog is displayed
        composeTestRule
            .onNodeWithTag(ALBUM_CONTENT_SCREEN_DELETE_ALBUM_DIALOG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that update album dialog is visible when state triggered`() {
        val uiState = AlbumContentUiState(
            uiAlbum = createMockAlbumUiState(),
            photos = persistentListOf(),
            selectedPhotos = persistentSetOf(),
            showUpdateAlbumName = triggered
        )

        setComposeContent(uiState)

        composeTestRule
            .onNodeWithTag(ALBUM_CONTENT_SCREEN_UPDATE_ALBUM_DIALOG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that remove links confirmation dialog is visible when showRemoveLinkConfirmation is triggered`() {
        val mockPhotos = listOf(
            createMockPhoto(1),
            createMockPhoto(2),
            createMockPhoto(3)
        )
        val uiState = AlbumContentUiState(
            uiAlbum = createMockAlbumUiState(),
            photos = mockPhotos.toImmutableList(),
            selectedPhotos = persistentSetOf(),
            showRemoveLinkConfirmation = triggered
        )

        setComposeContent(uiState = uiState)

        composeTestRule
            .onNodeWithTag(ALBUM_CONTENT_SCREEN_REMOVE_LINKS_DIALOG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that sort bottom sheet is visible when sort option clicked`() {
        val photos = listOf(createMockPhoto(id = 1L))
        val uiState = AlbumContentUiState(
            uiAlbum = createMockAlbumUiState(),
            photos = photos.toImmutableList(),
            selectedPhotos = persistentSetOf(),
        )

        setComposeContent(uiState)

        composeTestRule
            .onNodeWithTag(ALBUM_DYNAMIC_CONTENT_GRID_SORT_ITEM)
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(ALBUM_CONTENT_SCREEN_SORT_BOTTOM_SHEET)
            .assertIsDisplayed()
    }

    @Test
    fun `test that loading indicator is displayed when adding photos`() {
        val photos = listOf(createMockPhoto(id = 1L))
        val uiState = AlbumContentUiState(
            uiAlbum = createMockAlbumUiState(),
            photos = photos.toImmutableList(),
            selectedPhotos = persistentSetOf(),
            isAddingPhotos = true
        )

        setComposeContent(uiState)

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(ALBUM_CONTENT_SCREEN_LOADING_PROGRESS)
            .assertIsDisplayed()
    }

    @Test
    fun `test that loading indicator is displayed when removing photos`() {
        val photos = listOf(createMockPhoto(id = 1L))
        val uiState = AlbumContentUiState(
            uiAlbum = createMockAlbumUiState(),
            photos = photos.toImmutableList(),
            selectedPhotos = persistentSetOf(),
            isRemovingPhotos = true
        )

        setComposeContent(uiState)

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(ALBUM_CONTENT_SCREEN_LOADING_PROGRESS)
            .assertIsDisplayed()
    }

    @Test
    fun `test that skeleton is visible when photos are empty and loading`() {
        val uiState = AlbumContentUiState(
            uiAlbum = createMockAlbumUiState(),
            photos = persistentListOf(),
            selectedPhotos = persistentSetOf(),
            isLoading = true
        )

        setComposeContent(uiState)

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(ALBUM_CONTENT_SCREEN_SKELETON)
            .assertIsDisplayed()
    }

    @Test
    fun `test that skeleton is visible when photos are empty and adding photos`() {
        val uiState = AlbumContentUiState(
            uiAlbum = createMockAlbumUiState(),
            photos = persistentListOf(),
            selectedPhotos = persistentSetOf(),
            isAddingPhotos = true
        )

        setComposeContent(uiState)

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(ALBUM_CONTENT_SCREEN_SKELETON)
            .assertIsDisplayed()
    }

    private fun createMockPhoto(id: Long): PhotoUiState.Image {
        return PhotoUiState.Image(
            id = id,
            albumPhotoId = null,
            parentId = 0L,
            name = "photo_$id.jpg",
            isFavourite = false,
            creationTime = java.time.LocalDateTime.now(),
            modificationTime = java.time.LocalDateTime.now(),
            thumbnailFilePath = null,
            previewFilePath = null,
            fileTypeInfo = UnknownFileTypeInfo(
                mimeType = "*/*",
                extension = "jpg"
            ),
            base64Id = null,
            size = 0L,
            isTakenDown = false,
            isSensitive = false,
            isSensitiveInherited = false
        )
    }

    private fun createMockUIAlbum(
        title: String = "Test Album",
        id: Album = Album.UserAlbum(
            id = AlbumId(1L),
            title = "Test Album",
            cover = null,
            creationTime = 0L,
            modificationTime = 0L,
            isExported = false
        ),
    ): UIAlbum {
        return UIAlbum(
            id = id,
            title = AlbumTitle.StringTitle(title),
            count = 0,
            imageCount = 0,
            videoCount = 0,
            coverPhoto = null,
            defaultCover = null
        )
    }

    @Suppress("unused")
    private fun createMockMediaAlbum(
        title: String = "Test Album",
        albumId: Long = 1L,
    ): MediaAlbum.User {
        return MediaAlbum.User(
            id = AlbumId(albumId),
            title = title,
            creationTime = 0L,
            modificationTime = 0L,
            isExported = false,
            cover = null
        )
    }

    private fun createMockAlbumUiState(
        title: String = "Test Album",
        albumId: Long = 1L,
    ): AlbumUiState {
        val mediaAlbum = MediaAlbum.User(
            id = AlbumId(albumId),
            title = title,
            creationTime = 0L,
            modificationTime = 0L,
            isExported = false,
            cover = null
        )
        return AlbumUiState(
            mediaAlbum = mediaAlbum,
            title = title,
            cover = null
        )
    }
}

