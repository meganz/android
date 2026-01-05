package mega.privacy.android.feature.photos.presentation.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.DownloadPhotoResult
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.feature.photos.R
import mega.privacy.android.feature.photos.components.AlbumGridItem
import mega.privacy.android.feature.photos.downloader.PhotoDownloaderViewModel
import mega.privacy.android.feature.photos.extensions.downloadAsStateWithLifecycle
import mega.privacy.android.feature.photos.presentation.albums.content.toAlbumContentNavKey
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumUiState
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.destination.AlbumContentNavKey
import mega.privacy.android.shared.resources.R as sharedR
import kotlin.time.Duration.Companion.milliseconds

private typealias PhotoDownload =
        suspend (isPreview: Boolean, photo: Photo, callback: (success: Boolean) -> Unit) -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaSearchScreenM3(
    state: PhotosSearchState,
    photoDownloaderViewModel: PhotoDownloaderViewModel,
    onOpenAlbum: (AlbumContentNavKey) -> Unit,
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
            MediaSearchTopAppBar(
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
            MediaSearchContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                state = state,
                onDownloadPhoto = photoDownloaderViewModel::downloadPhoto,
                onClickPhoto = onOpenImagePreviewScreen,
                onClickAlbum = {
                    onOpenAlbum(it.toAlbumContentNavKey())
                },
                onClickMenu = onShowMoreMenu,
                onSelectQuery = { query ->
                    updateSelectedQuery(query)
                    searchPhotos(query)
                },
            )
        },
    )
}

@Composable
private fun MediaSearchTopAppBar(
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
private fun MediaSearchContent(
    state: PhotosSearchState,
    onDownloadPhoto: PhotoDownload,
    onClickPhoto: (Photo) -> Unit,
    onClickAlbum: (MediaAlbum) -> Unit,
    onClickMenu: (NodeId) -> Unit,
    onSelectQuery: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state.contentState) {
        MediaContentState.Loading -> MediaSearchLoading()
        MediaContentState.WelcomeEmpty -> {
            MediaSearchEmptyState(
                modifier = modifier
                    .testTag(MEDIA_SEARCH_SCREEN_WELCOME_EMPTY),
                title = stringResource(id = sharedR.string.photos_search_welcome_title),
                description = stringResource(id = sharedR.string.photos_search_welcome_description),
            )
        }

        MediaContentState.RecentQueries -> {
            val recentQueries = remember(state.recentQueries) {
                state.recentQueries.toImmutableList()
            }
            MediaSearchRecentQueries(
                modifier = modifier
                    .testTag(MEDIA_SEARCH_SCREEN_RECENT_QUERIES),
                recentQueries = recentQueries,
                onSelectQuery = onSelectQuery,
            )
        }

        MediaContentState.NoResults -> {
            MediaSearchEmptyState(
                modifier = modifier
                    .testTag(MEDIA_SEARCH_SCREEN_NO_RESULTS),
                title = stringResource(id = sharedR.string.photos_search_empty_state_title),
                description = stringResource(id = sharedR.string.photos_search_empty_state_description),
            )
        }

        MediaContentState.SearchResults -> {
            val photos = remember(state.photos) {
                state.photos.toImmutableList()
            }
            val albums = remember(state.albums) {
                state.albums.toImmutableList()
            }
            MediaSearchResults(
                modifier = modifier
                    .testTag(MEDIA_SEARCH_SCREEN_RESULTS),
                query = state.query,
                photos = photos,
                albums = albums,
                accountType = state.accountType,
                isBusinessAccountExpired = state.isBusinessAccountExpired,
                onDownloadPhoto = onDownloadPhoto,
                onClickPhoto = onClickPhoto,
                onClickAlbum = onClickAlbum,
                onClickMenu = onClickMenu,
            )
        }
    }
}

@Composable
private fun MediaSearchEmptyState(
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
private fun MediaSearchRecentQueries(
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
                            .testTag("$MEDIA_SEARCH_SCREEN_RECENT_QUERY_ITEM:$index")
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
private fun MediaSearchResults(
    query: String,
    photos: ImmutableList<Photo>,
    albums: ImmutableList<AlbumUiState>,
    accountType: AccountType?,
    isBusinessAccountExpired: Boolean,
    onDownloadPhoto: PhotoDownload,
    onClickPhoto: (Photo) -> Unit,
    onClickAlbum: (MediaAlbum) -> Unit,
    onClickMenu: (NodeId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val placeholder = remember(isDarkTheme) {
        if (isDarkTheme) R.drawable.ic_album_cover_d else R.drawable.ic_album_cover
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        if (albums.isNotEmpty()) {
            item(key = "albums_header") {
                MegaText(
                    text = stringResource(id = sharedR.string.media_albums_tab_title),
                    textColor = TextColor.Primary,
                    modifier = Modifier.padding(start = 16.dp),
                    style = AppTheme.typography.titleSmall,
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            item(key = "albums_row") {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    items(
                        count = albums.size,
                        key = { albums[it].mediaAlbum.hashCode() },
                    ) { index ->
                        val album = albums[index]
                        AlbumItem(
                            modifier = Modifier.testTag("$MEDIA_SEARCH_SCREEN_ALBUM_ITEM:$index"),
                            album = album,
                            query = query,
                            placeholder = placeholder,
                            onClickAlbum = onClickAlbum,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (photos.isNotEmpty()) {
            item(key = "photos_header") {
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
                key = { index -> photos[index].id },
            ) { index ->
                PhotoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("$MEDIA_SEARCH_SCREEN_PHOTO_ITEM:$index"),
                    query = query,
                    photo = photos[index],
                    accountType = accountType,
                    isBusinessAccountExpired = isBusinessAccountExpired,
                    placeholder = placeholder,
                    onDownloadPhoto = onDownloadPhoto,
                    onClick = onClickPhoto,
                    onClickMenu = onClickMenu,
                )
            }
        }
    }
}

@Composable
private fun AlbumItem(
    album: AlbumUiState,
    query: String,
    placeholder: Int,
    onClickAlbum: (MediaAlbum) -> Unit,
    modifier: Modifier = Modifier,
) {
    val downloadResult = album.cover?.downloadAsStateWithLifecycle(isPreview = false)
    val placeholderPainter = painterResource(placeholder)
    val context = LocalContext.current
    val title = remember(album.title, query) {
        HighlightedText(
            full = album.title.get(context = context),
            highlighted = query,
        )
    }
    val isSensitive =
        album.cover?.isSensitive == true || album.cover?.isSensitiveInherited == true

    AlbumGridItem(
        modifier = modifier
            .width(104.dp)
            .clickable { onClickAlbum(album.mediaAlbum) },
        coverImage = when (val result = downloadResult?.value) {
            is DownloadPhotoResult.Success -> result.thumbnailFilePath
            else -> null
        },
        title = title,
        placeholder = placeholderPainter,
        errorPlaceholder = placeholderPainter,
        isExported = album.isExported,
        isSensitive = isSensitive
    )
}

@Composable
private fun PhotoCard(
    query: String,
    photo: Photo,
    accountType: AccountType?,
    isBusinessAccountExpired: Boolean,
    placeholder: Int,
    onDownloadPhoto: PhotoDownload,
    onClick: (Photo) -> Unit,
    onClickMenu: (NodeId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val photoData by produceState<String?>(
        initialValue = null,
        key1 = photo.id,
    ) {
        onDownloadPhoto(false, photo) { isSuccess ->
            if (isSuccess) {
                value = photo.thumbnailFilePath
            }
        }
    }
    val placeholderPainter = painterResource(placeholder)
    val isPhotoSensitive = remember(
        accountType,
        isBusinessAccountExpired,
        photo.isSensitive,
        photo.isSensitiveInherited
    ) {
        accountType?.isPaid == true
                && !isBusinessAccountExpired
                && (photo.isSensitive || photo.isSensitiveInherited)
    }
    val alpha = if (isPhotoSensitive) 0.5f else 1f
    val blurRadius = if (isPhotoSensitive) 16.dp else 0.dp
    val highlightedText = remember(photo.name, query) {
        HighlightedText(
            full = photo.name,
            highlighted = query,
        )
    }
    val nodeId = remember(photo.id) { NodeId(photo.id) }

    OneLineListItem(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha),
        text = highlightedText,
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
                    .alpha(alpha)
                    .blur(blurRadius),
                placeholder = placeholderPainter,
                error = placeholderPainter,
                contentScale = ContentScale.FillWidth,
            )
        },
        trailingElement = {
            MegaIcon(
                painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.MoreVertical),
                contentDescription = null,
                modifier = Modifier.clickable { onClickMenu(nodeId) },
            )
        },
        enableClick = true,
        onClickListener = {
            onClick(photo)
        }
    )
}

@Composable
private fun MediaSearchLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .height(16.dp)
                .width(50.dp)
                .shimmerEffect()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(3) {
                Column {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(16.dp)
                            .align(Alignment.CenterHorizontally)
                            .shimmerEffect()
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .height(16.dp)
                .width(50.dp)
                .shimmerEffect()
        )

        repeat(5) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .shimmerEffect()
                )

                Box(
                    modifier = Modifier
                        .height(16.dp)
                        .fillMaxWidth()
                        .align(Alignment.CenterVertically)
                        .shimmerEffect()
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun MediaSearchLoadingPreview() {
    AndroidThemeForPreviews {
        MediaSearchLoading()
    }
}

internal const val MEDIA_SEARCH_SCREEN_WELCOME_EMPTY = "media_search_screen:welcome_empty"
internal const val MEDIA_SEARCH_SCREEN_RECENT_QUERIES = "media_search_screen:recent_queries"
internal const val MEDIA_SEARCH_SCREEN_RECENT_QUERY_ITEM = "media_search_screen:recent_query_item"
internal const val MEDIA_SEARCH_SCREEN_NO_RESULTS = "media_search_screen:no_results"
internal const val MEDIA_SEARCH_SCREEN_RESULTS = "media_search_screen:results"
internal const val MEDIA_SEARCH_SCREEN_ALBUM_ITEM = "media_search_screen:album_item"
internal const val MEDIA_SEARCH_SCREEN_PHOTO_ITEM = "media_search_screen:photo_item"