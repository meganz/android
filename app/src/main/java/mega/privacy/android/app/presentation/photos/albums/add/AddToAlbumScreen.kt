package mega.privacy.android.app.presentation.photos.albums.add

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.PhotoDownloaderViewModel
import mega.privacy.android.app.presentation.photos.model.PhotoDownload
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.domain.entity.photos.Album.UserAlbum
import mega.privacy.android.legacy.core.ui.controls.appbar.CollapsedSearchAppBar
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AddToAlbumScreen(
    addToAlbumViewModel: AddToAlbumViewModel,
    photoDownloaderViewModel: PhotoDownloaderViewModel,
    onClose: (String) -> Unit,
) {
    val context = LocalContext.current
    val state by addToAlbumViewModel.stateFlow.collectAsStateWithLifecycle()

    val tabNames = if (state.viewType == 0) {
        listOf(stringResource(R.string.tab_title_album))
    } else {
        listOf(
            stringResource(R.string.tab_title_album),
            stringResource(sharedR.string.video_section_tab_title_playlists),
        )
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f,
        pageCount = { tabNames.size },
    )

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(selectedTabIndex) {
        pagerState.scrollToPage(selectedTabIndex)
    }

    LaunchedEffect(pagerState.currentPage) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            selectedTabIndex = page
        }
    }

    LaunchedEffect(state.completionType) {
        if (state.completionType > 0) {
            val message = context.resources.getQuantityString(
                sharedR.plurals.album_add_to_message,
                state.completionType,
                state.mediaHolderName,
                state.numAddedItems,
            )
            onClose(message)
        }
    }

    MegaScaffold(
        topBar = {
            AddToAlbumTopBar(
                viewType = state.viewType,
                onClose = { onClose("") },
            )
        },
        bottomBar = {
            AddToAlbumBottomBar(
                selectedTabIndex = selectedTabIndex,
                selectedAlbum = state.selectedAlbum,
                selectedPlaylist = state.selectedPlaylist,
                onClickCancel = { onClose("") },
                onClickAddToAlbum = addToAlbumViewModel::addPhotosToAlbum,
                onClickAddToPlaylist = addToAlbumViewModel::addVideosToPlaylist,
            )
        },
        content = { paddingValues ->
            AddToAlbumContent(
                modifier = Modifier.padding(paddingValues),
                pagerState = pagerState,
                tabNames = tabNames,
                selectedTabIndex = selectedTabIndex,
                state = state,
                onClickTab = { selectedTabIndex = it },
                onDownloadPhoto = photoDownloaderViewModel::downloadPhoto,
                onSelectAlbum = addToAlbumViewModel::selectAlbum,
                onSetupNewAlbum = addToAlbumViewModel::setupNewAlbum,
                onCancelAlbumCreation = addToAlbumViewModel::cancelAlbumCreation,
                onClearAlbumNameErrorMessage = addToAlbumViewModel::clearAlbumNameErrorMessage,
                onCreateAlbum = addToAlbumViewModel::createAlbum,
                onSelectPlaylist = addToAlbumViewModel::selectPlaylist,
                onSetupNewPlaylist = addToAlbumViewModel::setupNewPlaylist,
                onCancelPlaylistCreation = addToAlbumViewModel::cancelPlaylistCreation,
                onClearPlaylistNameErrorMessage = addToAlbumViewModel::clearPlaylistNameErrorMessage,
                onCreatePlaylist = addToAlbumViewModel::createPlaylist,
            )
        },
    )
}

@Composable
private fun AddToAlbumTopBar(
    modifier: Modifier = Modifier,
    viewType: Int,
    onClose: () -> Unit,
) {
    CollapsedSearchAppBar(
        onBackPressed = onClose,
        elevation = false,
        title = stringResource(sharedR.string.album_add_to_image).takeIf {
            viewType == 0
        } ?: stringResource(sharedR.string.album_add_to_media),
        modifier = modifier,
        showSearchButton = false,
    )
}

@Composable
private fun AddToAlbumBottomBar(
    modifier: Modifier = Modifier,
    selectedTabIndex: Int,
    selectedAlbum: UserAlbum?,
    selectedPlaylist: VideoPlaylistUIEntity?,
    onClickCancel: () -> Unit,
    onClickAddToAlbum: () -> Unit,
    onClickAddToPlaylist: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        content = {
            TextMegaButton(
                text = stringResource(id = R.string.button_cancel),
                onClick = onClickCancel,
            )

            Spacer(modifier = Modifier.size(16.dp))

            RaisedDefaultMegaButton(
                text = stringResource(id = R.string.general_add),
                onClick = {
                    if (selectedTabIndex == 0) onClickAddToAlbum()
                    else if (selectedTabIndex == 1) onClickAddToPlaylist()
                },
                enabled = selectedTabIndex == 0 && selectedAlbum != null || selectedTabIndex == 1 && selectedPlaylist != null,
            )
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddToAlbumContent(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    tabNames: List<String>,
    selectedTabIndex: Int,
    state: AddToAlbumState,
    onClickTab: (Int) -> Unit,
    onDownloadPhoto: PhotoDownload,
    onSelectAlbum: (UserAlbum) -> Unit,
    onSetupNewAlbum: (String) -> Unit,
    onCancelAlbumCreation: () -> Unit,
    onClearAlbumNameErrorMessage: () -> Unit,
    onCreateAlbum: (String) -> Unit,
    onSelectPlaylist: (VideoPlaylistUIEntity) -> Unit,
    onSetupNewPlaylist: (String) -> Unit,
    onCancelPlaylistCreation: () -> Unit,
    onClearPlaylistNameErrorMessage: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
) {
    Column(
        modifier = modifier,
        content = {
            if (tabNames.size > 1) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    indicator = {
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(it[selectedTabIndex]),
                            color = colorResource(id = R.color.red_600_red_300),
                        )
                    },
                    backgroundColor = Color.Transparent,
                    tabs = {
                        tabNames.forEachIndexed { index, tabName ->
                            Tab(
                                selected = index == selectedTabIndex,
                                onClick = { onClickTab(index) },
                                text = {
                                    Text(
                                        text = tabName,
                                        fontWeight = FontWeight.Medium,
                                    )
                                },
                                selectedContentColor = colorResource(id = R.color.red_600_red_300),
                                unselectedContentColor = colorResource(id = R.color.grey_054_white_054),
                            )
                        }
                    },
                )
            }

            HorizontalPager(
                state = pagerState,
                pageContent = { page ->
                    when (page) {
                        0 -> AddToAlbumPage(
                            accountType = state.accountType,
                            isBusinessAccountExpired = state.isBusinessAccountExpired,
                            isLoading = state.isLoadingAlbums,
                            albums = state.albums,
                            selectedAlbum = state.selectedAlbum,
                            isCreatingAlbum = state.isCreatingAlbum,
                            albumNameSuggestion = state.albumNameSuggestion,
                            albumNameErrorMessageRes = state.albumNameErrorMessageRes,
                            onDownloadPhoto = onDownloadPhoto,
                            onSelectAlbum = onSelectAlbum,
                            onSetupNewAlbum = onSetupNewAlbum,
                            onCancelAlbumCreation = onCancelAlbumCreation,
                            onClearAlbumNameErrorMessage = onClearAlbumNameErrorMessage,
                            onCreateAlbum = onCreateAlbum,
                        )

                        1 -> AddToPlaylistPage(
                            isLoading = state.isLoadingPlaylists,
                            playlists = state.playlists,
                            selectedPlaylist = state.selectedPlaylist,
                            isCreatingPlaylist = state.isCreatingPlaylist,
                            playlistNameSuggestion = state.playlistNameSuggestion,
                            playlistNameErrorMessageRes = state.playlistNameErrorMessageRes,
                            onSelectPlaylist = onSelectPlaylist,
                            onSetupNewPlaylist = onSetupNewPlaylist,
                            onCancelPlaylistCreation = onCancelPlaylistCreation,
                            onClearPlaylistNameErrorMessage = onClearPlaylistNameErrorMessage,
                            onCreatePlaylist = onCreatePlaylist,
                        )
                    }
                },
            )
        },
    )
}
