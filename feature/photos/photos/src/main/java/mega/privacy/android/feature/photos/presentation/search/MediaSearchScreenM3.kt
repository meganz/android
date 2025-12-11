package mega.privacy.android.feature.photos.presentation.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.OneLineListItem
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.model.HighlightedText
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.feature.photos.R
import mega.privacy.android.feature.photos.components.AlbumGridItem
import mega.privacy.android.feature.photos.downloader.PhotoDownloaderViewModel
import mega.privacy.android.feature.photos.presentation.albums.model.UIAlbum
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import kotlin.time.Duration.Companion.milliseconds

private typealias PhotoDownload =
        suspend (isPreview: Boolean, photo: Photo, callback: (success: Boolean) -> Unit) -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaSearchScreenM3(
    state: PhotosSearchState,
    photoDownloaderViewModel: PhotoDownloaderViewModel,
    onOpenAlbum: (Album) -> Unit,
    onOpenImagePreviewScreen: (Photo) -> Unit,
    onShowMoreMenu: (NodeId) -> Unit,
    onCloseScreen: () -> Unit,
    updateQuery: (String) -> Unit,
    updateSelectedQuery: (String?) -> Unit,
    updateRecentQueries: (String) -> Unit,
    searchPhotos: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier,
        topBar = {
            PhotosSearchTopBar(
                query = state.query,
                selectedQuery = state.selectedQuery,
                onUpdateQuery = updateQuery,
                onSelectedQueryRead = { updateSelectedQuery(null) },
                onSaveQuery = updateRecentQueries,
                onSearch = searchPhotos,
                onCloseScreen = onCloseScreen,
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
                            title = stringResource(id = sharedR.string.photos_search_welcome_title),
                            description = stringResource(id = sharedR.string.photos_search_welcome_description),
                        )
                    } else {
                        val recentQueries = remember(state.recentQueries) {
                            state.recentQueries.toImmutableList()
                        }
                        PhotosSearchRecentQuery(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            recentQueries = recentQueries,
                            onSelectQuery = { query ->
                                updateSelectedQuery(query)
                                searchPhotos(query)
                            },
                        )
                    }
                } else if (state.albums.isEmpty() && !state.isSearchingAlbums && state.photos.isEmpty() && !state.isSearchingPhotos) {
                    PhotosSearchEmptyState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        title = stringResource(id = sharedR.string.photos_search_empty_state_title),
                        description = stringResource(id = sharedR.string.photos_search_empty_state_description),
                    )
                } else {
                    val photos = remember(state.photos) {
                        state.photos.toImmutableList()
                    }
                    val albums = remember(state.albums) {
                        state.albums.toImmutableList()
                    }
                    PhotosSearchContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        query = state.query,
                        photos = photos,
                        albums = albums,
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
    query: String,
    selectedQuery: String?,
    onUpdateQuery: (String) -> Unit,
    onSelectedQueryRead: () -> Unit,
    onSaveQuery: (String) -> Unit,
    onSearch: (String) -> Unit,
    onCloseScreen: () -> Unit,
    modifier: Modifier = Modifier,
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

    MegaSearchTopAppBar(
        modifier = modifier,
        query = query,
        title = "",
        navigationType = AppBarNavigationType.Back(onCloseScreen),
        onSearchingModeChanged = { isSearching ->
            if (!isSearching) {
                onCloseScreen()
            }
        },
        searchPlaceholder = stringResource(sharedR.string.search_bar_placeholder_text),
        onSearchAction = onSaveQuery,
        onQueryChanged = {
            text = it
            onUpdateQuery(it)
        },
        isSearchingMode = true
    )
}

@Composable
private fun PhotosSearchEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    EmptyStateView(
        modifier = modifier,
        title = title,
        description = SpannableText(text = description),
        illustration = iconPackR.drawable.ic_search_02
    )
}

@Composable
private fun PhotosSearchRecentQuery(
    recentQueries: ImmutableList<String>,
    onSelectQuery: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 16.dp),
        content = {
            item {
                MegaText(
                    text = stringResource(id = sharedR.string.photos_search_recent_search),
                    textColor = TextColor.Primary,
                    modifier = Modifier.padding(start = 16.dp),
                    style = AppTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            items(
                count = recentQueries.size,
                key = { index -> recentQueries[index] },
                itemContent = { index ->
                    val query = recentQueries[index]
                    MegaText(
                        text = query,
                        textColor = TextColor.Primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectQuery(query) }
                            .padding(16.dp),
                        style = AppTheme.typography.titleMedium,
                    )
                },
            )
        },
    )
}

@Composable
private fun PhotosSearchContent(
    query: String,
    photos: ImmutableList<Photo>,
    albums: ImmutableList<UIAlbum>,
    accountType: AccountType?,
    isBusinessAccountExpired: Boolean,
    onDownloadPhoto: PhotoDownload,
    onClickPhoto: (Photo) -> Unit,
    onClickAlbum: (Album) -> Unit,
    onClickMenu: (NodeId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val placeholder = painterResource(
        if (isDarkTheme) R.drawable.ic_album_cover_d else R.drawable.ic_album_cover
    )

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 16.dp),
        content = {
            if (albums.isNotEmpty()) {
                item {
                    MegaText(
                        text = stringResource(id = sharedR.string.media_albums_tab_title),
                        textColor = TextColor.Primary,
                        modifier = Modifier.padding(start = 16.dp),
                        style = AppTheme.typography.titleSmall,
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
                                    val albumCover = album.coverPhoto ?: album.defaultCover
                                    val albumCoverData by produceState<String?>(
                                        initialValue = null,
                                        key1 = albumCover?.id,
                                    ) {
                                        albumCover?.let { photo ->
                                            onDownloadPhoto(false, photo) { isSuccess ->
                                                if (isSuccess) {
                                                    value = photo.thumbnailFilePath
                                                }
                                            }
                                        }
                                    }

                                    AlbumGridItem(
                                        modifier = Modifier
                                            .width(104.dp)
                                            .clickable {
                                                onClickAlbum(album.id)
                                            },
                                        coverImage = albumCoverData,
                                        title = HighlightedText(
                                            full = album.title.getTitleString(context),
                                            highlighted = query,
                                        ),
                                        placeholder = placeholder,
                                        errorPlaceholder = placeholder,
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
                        text = stringResource(id = sharedR.string.media_feature_title),
                        textColor = TextColor.Primary,
                        modifier = Modifier.padding(start = 16.dp),
                        style = AppTheme.typography.titleSmall,
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
private fun PhotoCard(
    query: String,
    photo: Photo,
    accountType: AccountType?,
    isBusinessAccountExpired: Boolean,
    onDownloadPhoto: PhotoDownload,
    onClick: (Photo) -> Unit,
    onClickMenu: (NodeId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val photoData by produceState<String?>(initialValue = null) {
        onDownloadPhoto(false, photo) { isSuccess ->
            if (isSuccess) {
                value = photo.thumbnailFilePath
            }
        }
    }
    val placeholder = if (isSystemInDarkTheme()) {
        painterResource(R.drawable.ic_album_cover_d)
    } else {
        painterResource(R.drawable.ic_album_cover)
    }
    val isPhotoSensitive = accountType?.isPaid == true
            && !isBusinessAccountExpired
            && (photo.isSensitive || photo.isSensitiveInherited)

    OneLineListItem(
        modifier = modifier
            .fillMaxWidth()
            .alpha(0.5f.takeIf { isPhotoSensitive } ?: 1f)
            .clickable { onClick(photo) },
        text = HighlightedText(
            full = photo.name,
            highlighted = query,
        ),
        leadingElement = {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photoData)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .aspectRatio(1f)
                    .alpha(0.5f.takeIf { isPhotoSensitive } ?: 1f)
                    .blur(16.dp.takeIf { isPhotoSensitive } ?: 0.dp),
                placeholder = placeholder,
                error = placeholder,
                contentScale = ContentScale.FillWidth,
            )
        },
        trailingElement = {
            MegaIcon(
                painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.MoreVertical),
                contentDescription = null,
                modifier = Modifier.clickable { onClickMenu(NodeId(photo.id)) },
            )
        }
    )
}