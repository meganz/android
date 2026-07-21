package mega.privacy.android.feature.photos.presentation.albums

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.triggered
import mega.android.core.ui.model.HighlightedText
import mega.android.core.ui.modifiers.calculateSafeBottomPadding
import mega.android.core.ui.modifiers.excludingBottomPadding
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.sharedcomponents.extension.isDarkMode
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.photos.thumbnail.MediaThumbnailRequest
import mega.privacy.android.feature.photos.R
import mega.privacy.android.feature.photos.components.AlbumGridItem
import mega.privacy.android.feature.photos.presentation.albums.content.toAlbumContentNavKey
import mega.privacy.android.feature.photos.presentation.albums.dialog.RemoveAlbumConfirmationDialog
import mega.privacy.mobile.analytics.event.DeleteAlbumCancelButtonPressedEvent
import mega.privacy.mobile.analytics.event.DeleteAlbumsConfirmationDialogEvent

@Composable
fun AlbumsTabRoute(
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumsTabViewModel = hiltViewModel(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AlbumsTabScreen(
        uiState = uiState,
        onNavigate = onNavigate,
        deleteAlbums = viewModel::deleteAlbums,
        modifier = modifier,
        resetNavigationEvent = viewModel::resetNavigationEvent,
        resetDeleteAlbumsConfirmationEvent = viewModel::resetDeleteAlbumsConfirmationEvent,
        onAlbumSelectionToggle = viewModel::toggleAlbumSelection,
        contentPadding = contentPadding,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AlbumsTabScreen(
    uiState: AlbumsTabUiState,
    deleteAlbums: () -> Unit,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    resetNavigationEvent: () -> Unit = {},
    resetDeleteAlbumsConfirmationEvent: () -> Unit = {},
    onAlbumSelectionToggle: (MediaAlbum.User) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
) {
    val placeholder = if (uiState.themeMode.isDarkMode()) {
        painterResource(R.drawable.ic_album_cover_d)
    } else {
        painterResource(R.drawable.ic_album_cover)
    }

    EventEffect(
        event = uiState.navigationEvent,
        onConsumed = resetNavigationEvent,
        action = onNavigate
    )

    Box(
        modifier = modifier
            .padding(contentPadding.excludingBottomPadding())
    ) {
        val contentPadding = PaddingValues(
            top = 8.dp,
            start = 8.dp,
            end = 8.dp,
            bottom = contentPadding.calculateSafeBottomPadding(),
        )
        if (uiState.isLoading) {
            AlbumsTabSkeletonView(contentPadding = contentPadding)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = contentPadding
            ) {
                items(
                    count = uiState.albums.size,
                    key = { uiState.albums[it].mediaAlbum.hashCode() },
                    contentType = { index ->
                        uiState.albums[index]::class
                    }
                ) { index ->
                    val album = uiState.albums[index]
                    val userAlbum = album.mediaAlbum as? MediaAlbum.User
                    val isSelected =
                        userAlbum?.let { uiState.selectedUserAlbums.contains(it) } ?: false
                    val isSensitive =
                        uiState.showHiddenItems && (album.cover?.isSensitive == true || album.cover?.isSensitiveInherited == true)
                    val isEnabled = !uiState.isInSelectionMode || userAlbum != null

                    AlbumGridItem(
                        modifier = Modifier
                            .testTag("$ALBUMS_SCREEN_ALBUM_GRID_ITEM:${index}")
                            .combinedClickable(
                                enabled = isEnabled,
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
                        coverImage = album.cover?.let {
                            MediaThumbnailRequest(
                                id = it.id,
                                isPreview = false,
                                thumbnailFilePath = it.thumbnailFilePath,
                                previewFilePath = it.previewFilePath,
                                isPublicNode = false,
                                fileExtension = it.fileTypeInfo.extension
                            )
                        },
                        title = HighlightedText(album.title.text),
                        placeholder = placeholder,
                        errorPlaceholder = placeholder,
                        isExported = album.isExported,
                        isSelected = isSelected,
                        isSensitive = isSensitive,
                        enabled = isEnabled
                    )
                }
            }
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

@Composable
private fun AlbumsTabSkeletonView(
    contentPadding: PaddingValues = PaddingValues(),
) {
    val shimmerShape = RoundedCornerShape(4.dp)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(100.dp),
        modifier = Modifier.testTag(ALBUMS_SCREEN_SKELETON),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = contentPadding,
    ) {
        items(count = 12) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .shimmerEffect(shape = shimmerShape),
                )
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .shimmerEffect(shape = shimmerShape),
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun AlbumsTabSkeletonViewPreview() {
    AndroidThemeForPreviews {
        AlbumsTabScreen(
            uiState = AlbumsTabUiState(isLoading = true),
            deleteAlbums = {},
            onNavigate = {},
        )
    }
}

const val ALBUMS_SCREEN_SKELETON = "albums_tab_screen:skeleton"
const val ALBUMS_SCREEN_ALBUM_GRID_ITEM = "albums_tab_screen:album_grid_item"
const val ALBUMS_SCREEN_REMOVE_ALBUM_CONFIRMATION_DIALOG =
    "albums_tab_screen:remove_album_confirmation_dialog"