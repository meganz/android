package mega.privacy.android.feature.photos.presentation.albums

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import mega.android.core.ui.model.HighlightedText
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.photos.DownloadPhotoResult
import mega.privacy.android.feature.photos.R
import mega.privacy.android.feature.photos.components.AlbumGridItem
import mega.privacy.android.feature.photos.extensions.downloadAsStateWithLifecycle
import mega.privacy.android.feature.photos.model.AlbumFlow
import mega.privacy.android.feature.photos.presentation.albums.content.toAlbumContentNavKey
import mega.privacy.android.feature.photos.presentation.albums.dialog.EnterAlbumNameDialog
import mega.privacy.android.feature.photos.presentation.albums.dialog.RemoveAlbumConfirmationDialog
import mega.privacy.android.navigation.destination.LegacyPhotoSelectionNavKey
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.DeleteAlbumCancelButtonPressedEvent
import mega.privacy.mobile.analytics.event.DeleteAlbumsConfirmationDialogEvent

@Composable
fun AlbumsTabRoute(
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumsTabViewModel = hiltViewModel(),
    showNewAlbumDialogEvent: StateEvent = consumed,
    resetNewAlbumDialogEvent: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AlbumsTabScreen(
        uiState = uiState,
        onNavigate = onNavigate,
        addNewAlbum = viewModel::addNewAlbum,
        deleteAlbums = viewModel::deleteAlbums,
        modifier = modifier,
        showNewAlbumDialogEvent = showNewAlbumDialogEvent,
        resetNewAlbumDialogEvent = resetNewAlbumDialogEvent,
        resetErrorMessage = viewModel::resetErrorMessage,
        resetAddNewAlbumSuccess = viewModel::resetAddNewAlbumSuccess,
        resetNavigationEvent = viewModel::resetNavigationEvent,
        resetDeleteAlbumsConfirmationEvent = viewModel::resetDeleteAlbumsConfirmationEvent,
        onAlbumSelectionToggle = viewModel::toggleAlbumSelection
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AlbumsTabScreen(
    uiState: AlbumsTabUiState,
    addNewAlbum: (String) -> Unit,
    deleteAlbums: () -> Unit,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    showNewAlbumDialogEvent: StateEvent = consumed,
    resetNewAlbumDialogEvent: () -> Unit = {},
    resetErrorMessage: () -> Unit = {},
    resetAddNewAlbumSuccess: () -> Unit = {},
    resetNavigationEvent: () -> Unit = {},
    resetDeleteAlbumsConfirmationEvent: () -> Unit = {},
    onAlbumSelectionToggle: (MediaAlbum.User) -> Unit = {},
) {
    val placeholder = if (isSystemInDarkTheme()) {
        painterResource(R.drawable.ic_album_cover_d)
    } else {
        painterResource(R.drawable.ic_album_cover)
    }

    EventEffect(
        event = uiState.addNewAlbumSuccessEvent,
        onConsumed = resetAddNewAlbumSuccess
    ) { albumId ->
        resetNewAlbumDialogEvent()
        onNavigate(
            LegacyPhotoSelectionNavKey(
                albumId = albumId.id,
                selectionMode = AlbumFlow.Creation.ordinal
            )
        )
    }

    EventEffect(
        event = uiState.navigationEvent,
        onConsumed = resetNavigationEvent,
        action = onNavigate
    )

    Box(modifier = modifier) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(120.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(
                count = uiState.albums.size,
                key = { uiState.albums[it].mediaAlbum.hashCode() },
                contentType = { index ->
                    uiState.albums[index]::class
                }
            ) { index ->
                val album = uiState.albums[index]
                val downloadResult = album.cover?.downloadAsStateWithLifecycle(isPreview = false)
                val userAlbum = album.mediaAlbum as? MediaAlbum.User
                val isSelected = userAlbum?.let { uiState.selectedUserAlbums.contains(it) } ?: false

                AlbumGridItem(
                    modifier = Modifier
                        .testTag("$ALBUMS_SCREEN_ALBUM_GRID_ITEM:${index}")
                        .combinedClickable(
                            onClick = {
                                if (uiState.isInSelectionMode) {
                                    userAlbum?.let(onAlbumSelectionToggle)
                                } else {
                                    onNavigate(album.mediaAlbum.toAlbumContentNavKey())
                                }
                            },
                            onLongClick = {
                                userAlbum?.let(onAlbumSelectionToggle)
                            }
                        ),
                    coverImage = when (val result = downloadResult?.value) {
                        is DownloadPhotoResult.Success -> result.thumbnailFilePath
                        else -> null
                    },
                    title = HighlightedText(album.title.text),
                    placeholder = placeholder,
                    errorPlaceholder = placeholder,
                    isExported = album.isExported,
                    isSelected = isSelected
                )
            }
        }

        if (showNewAlbumDialogEvent == triggered) {
            EnterAlbumNameDialog(
                modifier = Modifier.testTag(ALBUMS_SCREEN_ADD_NEW_ALBUM_DIALOG),
                onDismiss = resetNewAlbumDialogEvent,
                onConfirm = addNewAlbum,
                resetErrorMessage = resetErrorMessage,
                errorText = (uiState.addNewAlbumErrorMessage as? StateEventWithContentTriggered)?.content,
                positiveButtonText = stringResource(sharedR.string.media_add_new_album_dialog_positive_button)
            )
        }

        RemoveAlbumConfirmationDialog(
            modifier = Modifier.testTag(ALBUMS_SCREEN_REMOVE_ALBUM_CONFIRMATION_DIALOG),
            size = uiState.selectedUserAlbums.size,
            isVisible = uiState.deleteAlbumsConfirmationEvent == triggered,
            onConfirm = {
                Analytics.tracker.trackEvent(DeleteAlbumsConfirmationDialogEvent)
                deleteAlbums()
                resetDeleteAlbumsConfirmationEvent()
            },
            onDismiss = {
                Analytics.tracker.trackEvent(DeleteAlbumCancelButtonPressedEvent)
                resetDeleteAlbumsConfirmationEvent()
            }
        )
    }
}

const val ALBUMS_SCREEN_ALBUM_GRID_ITEM = "albums_tab_screen:album_grid_item"
const val ALBUMS_SCREEN_ADD_NEW_ALBUM_DIALOG = "albums_tab_screen:add_new_album_dialog"
const val ALBUMS_SCREEN_REMOVE_ALBUM_CONFIRMATION_DIALOG =
    "albums_tab_screen:remove_album_confirmation_dialog"