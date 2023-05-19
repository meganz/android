package mega.privacy.android.app.presentation.photos.albums.getmultiplelinks

import android.text.TextUtils.TruncateAt.MIDDLE
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.albums.getlink.AlbumSummary
import mega.privacy.android.core.ui.theme.grey_alpha_012
import mega.privacy.android.core.ui.theme.grey_alpha_054
import mega.privacy.android.core.ui.theme.grey_alpha_087
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.core.ui.theme.white_alpha_012
import mega.privacy.android.core.ui.theme.white_alpha_054
import mega.privacy.android.core.ui.theme.white_alpha_087
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.entity.photos.Photo

private typealias ImageDownloader = (photo: Photo, callback: (Boolean) -> Unit) -> Unit

@Composable
fun AlbumGetMultipleLinksScreen(
    viewModel: AlbumGetMultipleLinksViewModel = viewModel(),
    onBack: () -> Unit,
    onShareLinks: (List<AlbumLink>) -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    val scaffoldState = rememberScaffoldState()

    LaunchedEffect(state.exitScreen) {
        if (state.exitScreen) {
            onBack()
        }
    }

    val albumLinks = state.albumLinks
    val linksValuesList = albumLinks.values.toList()
    val albumSummaries = state.albumsSummaries

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            AlbumGetMultipleLinksTopBar(
                links = linksValuesList,
                onBack = onBack,
                onShareLink = { onShareLinks(linksValuesList) },
            )
        },
        snackbarHost = { snackbarHostState ->
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { snackbarData ->
                    Snackbar(
                        snackbarData = snackbarData,
                        backgroundColor = grey_alpha_087.takeIf { isLight } ?: white,
                    )
                }
            )
        },
        content = { contentPadding ->
            AlbumGetMultipleLinksContent(
                modifier = Modifier.padding(paddingValues = contentPadding).fillMaxSize(),
                albumSummaries = albumSummaries,
                links = albumLinks,
                albumLinksList = state.albumLinksList,
                onDownloadImage = viewModel::downloadImage,
                scaffoldState = scaffoldState,
            )
        },
    )
}

@Composable
private fun AlbumGetMultipleLinksTopBar(
    modifier: Modifier = Modifier,
    links: List<AlbumLink> = emptyList(),
    onBack: () -> Unit = {},
    onShareLink: () -> Unit = {},
) {
    val isLight = MaterialTheme.colors.isLight

    TopAppBar(
        title = {
            Text(
                text = pluralStringResource(
                    id = R.plurals.album_share_get_links,
                    count = links.size
                ),
                color = grey_alpha_087.takeIf { isLight } ?: white_alpha_087,
                fontSize = 16.sp,
                fontWeight = FontWeight.W500,
                style = MaterialTheme.typography.subtitle1,
            )
        },
        modifier = modifier,
        navigationIcon = {
            IconButton(
                onClick = onBack,
                content = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back_white),
                        contentDescription = null,
                        tint = grey_alpha_087.takeIf { isLight } ?: white_alpha_087,
                    )
                },
            )
        },
        actions = {
            IconButton(
                onClick = onShareLink,
                modifier = Modifier.alpha(0.4f.takeIf { links.isEmpty() } ?: 1f),
                enabled = links.isNotEmpty(),
                content = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_social_share_white),
                        contentDescription = null,
                        tint = grey_alpha_087.takeIf { isLight } ?: white_alpha_087,
                    )
                },
            )
        },
        elevation = 10.dp,
    )
}

@Composable
private fun AlbumGetMultipleLinksContent(
    modifier: Modifier = Modifier,
    albumSummaries: Map<AlbumId, AlbumSummary>,
    links: Map<AlbumId, AlbumLink>,
    albumLinksList: List<String>,
    onDownloadImage: ImageDownloader,
    scaffoldState: ScaffoldState,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val isLight = MaterialTheme.colors.isLight

    Column(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp),
        ) {
            item {
                Text(
                    modifier = Modifier.padding(vertical = 14.dp),
                    text = stringResource(id = R.string.tab_links_shares).uppercase(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            items(
                items = albumSummaries.keys.toList(),
                key = {
                    it.id
                }
            ) { albumId ->
                val link = links[albumId]?.link ?: ""
                val summary = albumSummaries[albumId]

                AlbumGetLinkRowItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, end = 16.dp, bottom = 16.dp),
                    albumSummary = summary,
                    albumLink = link,
                    onDownloadImage = onDownloadImage,
                )

                Divider(
                    color = grey_alpha_012.takeIf { isLight } ?: white_alpha_012,
                    thickness = 1.dp,
                )
            }
        }

        if (links.isNotEmpty()) {
            AlbumGetMultipleLinksBottomBar(
                albumLinksList = albumLinksList,
                onButtonClick = { albumsLinks ->
                    clipboardManager.setText(AnnotatedString(albumsLinks))

                    coroutineScope.launch {
                        scaffoldState.snackbarHostState.showSnackbar(
                            message = context.resources.getQuantityString(
                                R.plurals.album_share_links_copied,
                                links.size,
                            ),
                        )
                    }
                },
            )
        }
    }
}


@Composable
private fun AlbumGetMultipleLinksBottomBar(
    modifier: Modifier = Modifier,
    albumLinksList: List<String>,
    onButtonClick: (String) -> Unit,
) {
    Surface(modifier = modifier) {
        OutlinedButton(
            modifier = Modifier.padding(all = 16.dp),
            onClick = { onButtonClick(albumLinksList.joinToString(System.lineSeparator())) },
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colors.secondary),
        ) {
            Text(
                text = stringResource(id = R.string.action_copy_all),
                color = MaterialTheme.colors.secondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
                style = MaterialTheme.typography.button,
            )
        }
    }
}

@Composable
private fun AlbumGetLinkRowItem(
    modifier: Modifier = Modifier,
    albumSummary: AlbumSummary?,
    albumLink: String,
    onDownloadImage: ImageDownloader,
) {
    val isLight = MaterialTheme.colors.isLight

    val album = albumSummary?.album
    val numPhotos = albumSummary?.numPhotos ?: 0

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumCoverImage(
            cover = album?.cover,
            onDownloadImage = onDownloadImage,
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AndroidView(
                factory = { context ->
                    TextView(context).apply {
                        maxLines = 1
                        ellipsize = MIDDLE
                        textSize = 16f

                        setTextAppearance(R.style.TextAppearance_Mega_Subtitle1)
                        setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.grey_alpha_087.takeIf {
                                    isLight
                                } ?: R.color.white_alpha_087,
                            )
                        )
                    }
                },
                update = { view ->
                    view.text = album?.title.orEmpty()
                },
            )

            Text(
                text = stringResource(id = R.string.link_request_status).takeIf {
                    albumLink.isEmpty()
                } ?: albumLink,
                color = grey_alpha_054.takeIf { isLight } ?: white_alpha_054,
                fontSize = 14.sp,
                fontWeight = FontWeight.W400,
                style = MaterialTheme.typography.subtitle2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (albumLink.isNotEmpty()) {
                Text(
                    text = pluralStringResource(
                        id = R.plurals.general_num_items,
                        count = numPhotos,
                        numPhotos,
                    ),
                    color = grey_alpha_054.takeIf { isLight } ?: white_alpha_054,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.W400,
                    style = MaterialTheme.typography.subtitle2,
                )
            }
        }
    }
}

@Composable
private fun AlbumCoverImage(
    modifier: Modifier = Modifier,
    cover: Photo?,
    onDownloadImage: ImageDownloader,
) {
    val isLight = MaterialTheme.colors.isLight
    val context = LocalContext.current

    val imageState = produceState<String?>(
        initialValue = null,
        key1 = cover,
        producer = {
            cover?.also { photo ->
                onDownloadImage(cover) { isSuccess ->
                    if (isSuccess) {
                        value = photo.thumbnailFilePath
                    }
                }
            }
        },
    )

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageState.value)
            .build(),
        contentDescription = null,
        modifier = modifier
            .size(48.dp)
            .clip(shape = RoundedCornerShape(4.dp)),
        placeholder = painterResource(
            id = R.drawable.ic_album_cover.takeIf { isLight } ?: R.drawable.ic_album_cover_d,
        ),
        error = painterResource(
            id = R.drawable.ic_album_cover.takeIf { isLight } ?: R.drawable.ic_album_cover_d,
        ),
    )
}
