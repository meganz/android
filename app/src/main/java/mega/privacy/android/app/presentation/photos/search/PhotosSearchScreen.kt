package mega.privacy.android.app.presentation.photos.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.delay
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.PhotoDownloaderViewModel
import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.app.presentation.photos.model.PhotoDownload
import mega.privacy.android.app.presentation.transfers.TransfersManagementViewModel
import mega.privacy.android.core.R as coreR
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.legacy.core.ui.controls.appbar.ExpandedSearchAppBar
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.text.HighlightedText
import mega.privacy.android.shared.original.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.controls.widgets.TransfersWidgetViewAnimated
import mega.privacy.android.shared.original.core.ui.theme.teal_200_alpha_038
import mega.privacy.android.shared.resources.R as resourcesR
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun PhotosSearchScreen(
    photosSearchViewModel: PhotosSearchViewModel,
    photoDownloaderViewModel: PhotoDownloaderViewModel,
    transfersManagementViewModel: TransfersManagementViewModel,
    scaffoldState: ScaffoldState,
    onOpenAlbum: (Album) -> Unit,
    onOpenImagePreviewScreen: (Photo) -> Unit,
    onOpenTransfersScreen: () -> Unit,
    onShowMoreMenu: (NodeId) -> Unit,
    onCloseScreen: () -> Unit,
) {
    val state by photosSearchViewModel.state.collectAsStateWithLifecycle()
    val transfersState by transfersManagementViewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            PhotosSearchTopBar(
                query = state.query,
                selectedQuery = state.selectedQuery,
                onUpdateQuery = photosSearchViewModel::updateQuery,
                onSelectedQueryRead = { photosSearchViewModel.updateSelectedQuery(null) },
                onSaveQuery = photosSearchViewModel::updateRecentQueries,
                onSearch = photosSearchViewModel::search,
                onCloseScreen = onCloseScreen,
            )
        },
        floatingActionButton = {
            TransfersWidgetViewAnimated(
                transfersInfo = transfersState.transfersInfo,
                onClick = onOpenTransfersScreen,
                modifier = Modifier.navigationBarsPadding(),
            )
        },
        content = { paddingValues ->
            if (!state.isInitializing) {
                if (state.query.isBlank()) {
                    if (state.recentQueries.isEmpty()) {
                        PhotosSearchEmptyState(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            title = stringResource(id = resourcesR.string.photos_search_welcome_title),
                            description = stringResource(id = resourcesR.string.photos_search_welcome_description),
                        )
                    } else {
                        PhotosSearchRecentQuery(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            recentQueries = state.recentQueries,
                            onSelectQuery = { query ->
                                photosSearchViewModel.updateSelectedQuery(query)
                                photosSearchViewModel.search(query)
                            },
                        )
                    }
                } else if (state.albums.isEmpty() && !state.isSearchingAlbums && state.photos.isEmpty() && !state.isSearchingPhotos) {
                    PhotosSearchEmptyState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        title = stringResource(id = resourcesR.string.photos_search_empty_state_title),
                        description = stringResource(id = resourcesR.string.photos_search_empty_state_description),
                    )
                } else {
                    PhotosSearchContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        query = state.query,
                        photos = state.photos,
                        albums = state.albums,
                        accountType = state.accountType,
                        isBusinessAccountExpired = state.isBusinessAccountExpired,
                        onDownloadPhoto = photoDownloaderViewModel::downloadPhoto,
                        onClickPhoto = onOpenImagePreviewScreen,
                        onClickAlbum = onOpenAlbum,
                        onClickMenu = onShowMoreMenu,
                    )
                }
            }
        },
    )
}

@Composable
private fun PhotosSearchTopBar(
    modifier: Modifier = Modifier,
    query: String,
    selectedQuery: String?,
    onUpdateQuery: (String) -> Unit,
    onSelectedQueryRead: () -> Unit,
    onSaveQuery: (String) -> Unit,
    onSearch: (String) -> Unit,
    onCloseScreen: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(text) {
        if (text.isNotBlank()) {
            delay(300.milliseconds)
        }
        onSearch(text)
    }

    LaunchedEffect(selectedQuery) {
        selectedQuery?.let { text = it }
        onSelectedQueryRead()
    }

    ExpandedSearchAppBar(
        text = query,
        hintId = R.string.hint_action_search,
        onSearchTextChange = {
            text = it
            onUpdateQuery(it)
        },
        onCloseClicked = onCloseScreen,
        onSearchClicked = onSaveQuery,
        elevation = false,
        modifier = modifier,
        isHideAfterSearch = true,
        overwriteText = true,
    )
}

@Composable
private fun PhotosSearchEmptyState(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            Image(
                painter = painterResource(id = iconPackR.drawable.ic_search_02),
                contentDescription = null,
                modifier = Modifier.size(128.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            MegaText(
                text = title,
                textColor = TextColor.Secondary,
                style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.W500),
            )

            Spacer(modifier = Modifier.height(8.dp))

            MegaText(
                text = description,
                textColor = TextColor.Secondary,
                style = MaterialTheme.typography.subtitle2,
            )
        },
    )
}

@Composable
private fun PhotosSearchRecentQuery(
    modifier: Modifier = Modifier,
    recentQueries: List<String>,
    onSelectQuery: (String) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 16.dp),
        content = {
            item {
                MegaText(
                    text = stringResource(id = resourcesR.string.photos_search_recent_search),
                    textColor = TextColor.Primary,
                    modifier = Modifier.padding(start = 16.dp),
                    style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.W500),
                )

                Spacer(modifier = Modifier.height(16.dp))

                MegaDivider(dividerType = DividerType.SmallStartPadding)
            }

            items(
                count = recentQueries.size,
                key = { index ->
                    val query = recentQueries[index]
                    query
                },
                itemContent = { index ->
                    val query = recentQueries[index]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectQuery(query) },
                        content = {
                            MegaText(
                                text = query,
                                textColor = TextColor.Primary,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.subtitle1,
                            )

                            if (index < recentQueries.lastIndex) {
                                MegaDivider(dividerType = DividerType.SmallStartPadding)
                            }
                        },
                    )
                },
            )
        },
    )
}

@Composable
private fun PhotosSearchContent(
    modifier: Modifier = Modifier,
    query: String,
    photos: List<Photo>,
    albums: List<UIAlbum>,
    accountType: AccountType?,
    isBusinessAccountExpired: Boolean,
    onDownloadPhoto: PhotoDownload,
    onClickPhoto: (Photo) -> Unit,
    onClickAlbum: (Album) -> Unit,
    onClickMenu: (NodeId) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 16.dp),
        content = {
            if (albums.isNotEmpty()) {
                item {
                    MegaText(
                        text = stringResource(id = R.string.tab_title_album),
                        textColor = TextColor.Primary,
                        modifier = Modifier.padding(start = 16.dp),
                        style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.W500),
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        content = {
                            item {
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            items(
                                count = albums.size,
                                key = { index ->
                                    val album = albums[index]
                                    "${album.id}-${album.coverPhoto?.id}-${album.defaultCover?.id}"
                                },
                                itemContent = { index ->
                                    val album = albums[index]
                                    AlbumCard(
                                        modifier = Modifier.width(140.dp),
                                        query = query,
                                        album = album,
                                        accountType = accountType,
                                        isBusinessAccountExpired = isBusinessAccountExpired,
                                        onDownloadPhoto = onDownloadPhoto,
                                        onClick = onClickAlbum,
                                    )
                                },
                            )

                            item {
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        },
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            if (photos.isNotEmpty()) {
                item {
                    MegaText(
                        text = stringResource(id = R.string.settings_media),
                        textColor = TextColor.Primary,
                        modifier = Modifier.padding(start = 16.dp),
                        style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.W500),
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(
                    count = photos.size,
                    key = { index ->
                        val photo = photos[index]
                        "${photo.id}"
                    },
                    itemContent = { index ->
                        val photo = photos[index]
                        PhotoCard(
                            modifier = Modifier.fillMaxWidth(),
                            query = query,
                            photo = photo,
                            accountType = accountType,
                            isBusinessAccountExpired = isBusinessAccountExpired,
                            onDownloadPhoto = onDownloadPhoto,
                            onClick = onClickPhoto,
                            onClickMenu = onClickMenu,
                        )
                    },
                )
            }
        },
    )
}

@Composable
private fun AlbumCard(
    modifier: Modifier = Modifier,
    query: String,
    album: UIAlbum,
    accountType: AccountType?,
    isBusinessAccountExpired: Boolean,
    onDownloadPhoto: PhotoDownload,
    onClick: (Album) -> Unit,
) {
    val context = LocalContext.current
    val isLight = MaterialTheme.colors.isLight

    val albumCover = album.coverPhoto ?: album.defaultCover
    val albumCoverData by produceState<String?>(initialValue = null) {
        albumCover?.let { photo ->
            onDownloadPhoto(false, photo) { isSuccess ->
                if (isSuccess) {
                    value = photo.thumbnailFilePath
                }
            }
        }
    }
    val isCoverSensitive = accountType?.isPaid == true
            && !isBusinessAccountExpired
            && (albumCover?.isSensitive == true || albumCover?.isSensitiveInherited == true)

    Column(
        modifier = modifier.clickable { onClick(album.id) },
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(albumCoverData)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .aspectRatio(1f)
                    .alpha(0.5f.takeIf { isCoverSensitive } ?: 1f)
                    .blur(16.dp.takeIf { isCoverSensitive } ?: 0.dp),
                placeholder = painterResource(
                    id = R.drawable.ic_album_cover_d.takeIf { !isLight }
                        ?: R.drawable.ic_album_cover,
                ),
                error = painterResource(
                    id = R.drawable.ic_album_cover_d.takeIf { !isLight }
                        ?: R.drawable.ic_album_cover,
                ),
                contentScale = ContentScale.FillWidth,
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (query.isNotBlank()) {
                HighlightedText(
                    text = album.title.getTitleString(context),
                    highlightText = query,
                    textColor = TextColor.Primary,
                    highlightColor = teal_200_alpha_038,
                    applyMarqueEffect = false,
                    style = MaterialTheme.typography.subtitle2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            MegaText(
                text = "${album.count}",
                textColor = TextColor.Secondary,
                overflow = LongTextBehaviour.MiddleEllipsis,
                style = MaterialTheme.typography.caption,
            )
        },
    )
}

@Composable
private fun PhotoCard(
    modifier: Modifier = Modifier,
    query: String,
    photo: Photo,
    accountType: AccountType?,
    isBusinessAccountExpired: Boolean,
    onDownloadPhoto: PhotoDownload,
    onClick: (Photo) -> Unit,
    onClickMenu: (NodeId) -> Unit,
) {
    val context = LocalContext.current

    val photoData by produceState<String?>(initialValue = null) {
        onDownloadPhoto(false, photo) { isSuccess ->
            if (isSuccess) {
                value = photo.thumbnailFilePath
            }
        }
    }
    val isPhotoSensitive = accountType?.isPaid == true
            && !isBusinessAccountExpired
            && (photo.isSensitive || photo.isSensitiveInherited)

    Column(
        modifier = modifier
            .alpha(0.5f.takeIf { isPhotoSensitive } ?: 1f)
            .clickable { onClick(photo) },
        content = {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = {
                    Box(
                        contentAlignment = Alignment.Center,
                        content = {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(photoData)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .aspectRatio(1f)
                                    .alpha(0.5f.takeIf { isPhotoSensitive } ?: 1f)
                                    .blur(16.dp.takeIf { isPhotoSensitive } ?: 0.dp),
                                placeholder = painterResource(id = R.drawable.ic_album_cover),
                                error = painterResource(id = R.drawable.ic_album_cover),
                                contentScale = ContentScale.FillWidth,
                            )

                            if (photo.fileTypeInfo is VideoFileTypeInfo) {
                                Icon(
                                    painter = painterResource(id = coreR.drawable.ic_play_voice_clip),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(14.dp),
                                    tint = Color.Unspecified,
                                )
                            }
                        },
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    if (query.isNotBlank()) {
                        HighlightedText(
                            text = photo.name,
                            highlightText = query,
                            textColor = TextColor.Primary,
                            modifier = Modifier.weight(1f),
                            highlightColor = teal_200_alpha_038,
                            applyMarqueEffect = false,
                            style = MaterialTheme.typography.subtitle1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.MoreVertical),
                        contentDescription = null,
                        modifier = Modifier.clickable { onClickMenu(NodeId(photo.id)) },
                    )
                },
            )

            MegaDivider(dividerType = DividerType.BigStartPadding)
        },
    )
}
