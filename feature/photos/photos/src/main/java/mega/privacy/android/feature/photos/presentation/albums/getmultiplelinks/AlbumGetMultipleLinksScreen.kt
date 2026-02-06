package mega.privacy.android.feature.photos.presentation.albums.getmultiplelinks

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.MegaOutlinedButton
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.divider.SubtleDivider
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.sharedcomponents.extension.isDarkMode
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.feature.photos.R
import mega.privacy.android.feature.photos.presentation.albums.copyright.CopyRightScreen
import mega.privacy.android.feature.photos.presentation.albums.getlink.ALBUM_GET_LINK_SHARE_ACTION_TAG
import mega.privacy.android.feature.photos.presentation.albums.getlink.AlbumSummary
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.MultipleAlbumLinksScreenEvent

private typealias ImageDownloader = (photo: Photo, callback: (Boolean) -> Unit) -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumGetMultipleLinksScreen(
    onBack: () -> Unit,
    onShareLinks: (List<AlbumLink>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumGetMultipleLinksViewModel =
        hiltViewModel<AlbumGetMultipleLinksViewModel, AlbumGetMultipleLinksViewModel.Factory> {
            it.create(null)
        },
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        Analytics.tracker.trackEvent(MultipleAlbumLinksScreenEvent)
        onPauseOrDispose {}
    }

    LaunchedEffect(state.exitScreen) {
        if (state.exitScreen) {
            onBack()
        }
    }

    LaunchedEffect(state.copyrightAgreed) {
        if (state.copyrightAgreed) {
            viewModel.hideCopyright()

            if (!state.showSharingSensitiveWarning) {
                viewModel.fetchAlbums()
                viewModel.fetchLinks()
            }
        }
    }

    val albumLinks = state.albumLinks
    val linksValuesList = albumLinks.values.toList()
    val albumSummaries = state.albumsSummaries

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier.systemBarsPadding(),
        topBar = {
            MegaTopAppBar(
                title = pluralStringResource(
                    id = sharedR.plurals.album_get_link_screen_title,
                    count = linksValuesList.size
                ),
                navigationType = AppBarNavigationType.Back(onBack),
                actions = listOf(
                    MenuActionWithClick(
                        menuAction = object : MenuActionWithIcon {
                            @Composable
                            override fun getIconPainter(): Painter =
                                rememberVectorPainter(IconPack.Medium.Thin.Outline.ShareNetwork)

                            override val testTag: String = ALBUM_GET_LINK_SHARE_ACTION_TAG

                            @Composable
                            override fun getDescription(): String = "Share"

                        },
                        onClick = { onShareLinks(linksValuesList) }
                    ),
                )
            )
        },
        content = { contentPadding ->
            AlbumGetMultipleLinksContent(
                modifier = Modifier
                    .padding(paddingValues = contentPadding)
                    .fillMaxSize(),
                albumSummaries = albumSummaries,
                links = albumLinks,
                albumLinksList = state.albumLinksList,
                onDownloadImage = viewModel::downloadImage,
                isSystemInDarkTheme = state.themeMode.isDarkMode(),
                snackbarEventQueue = rememberSnackBarQueue(),
            )
        },
    )

    if (!state.showCopyright && state.showSharingSensitiveWarning) {
        BasicDialog(
            title = stringResource(id = sharedR.string.hidden_items),
            description = stringResource(id = sharedR.string.hidden_nodes_sharing_albums),
            positiveButtonText = stringResource(id = sharedR.string.button_continue),
            negativeButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
            dismissOnClickOutside = false,
            dismissOnBackPress = false,
            onPositiveButtonClicked = {
                viewModel.hideSharingSensitiveWarning()
                viewModel.fetchAlbums()
                viewModel.fetchLinks()
            },
            onDismiss = {},
            onNegativeButtonClicked = onBack,
        )
    }

    if (state.showCopyright) {
        CopyRightScreen(
            modifier = Modifier
                .systemBarsPadding()
                .fillMaxSize(),
            onAgree = {
                viewModel.updateShowCopyRight(false)
                viewModel.agreeCopyrightTerms()
            },
            onDisagree = {
                viewModel.updateShowCopyRight(true)
                onBack()
            }
        )
    }
}

@SuppressLint("ComposeUnstableCollections")
@Composable
internal fun AlbumGetMultipleLinksContent(
    albumSummaries: Map<AlbumId, AlbumSummary>,
    links: Map<AlbumId, AlbumLink>,
    albumLinksList: List<String>,
    onDownloadImage: ImageDownloader,
    isSystemInDarkTheme: Boolean,
    snackbarEventQueue: SnackbarEventQueue,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp),
        ) {
            item {
                MegaText(
                    modifier = Modifier.padding(vertical = 14.dp),
                    text = stringResource(id = sharedR.string.album_get_multiple_links_list_title),
                    style = AppTheme.typography.titleSmall
                )
            }

            if (albumSummaries.isEmpty()) {
                items(count = 2) { index ->
                    AlbumGetLinkRowItemPlaceholder(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, end = 16.dp, bottom = 16.dp)
                            .testTag("$ALBUM_GET_MULTIPLE_LINKS_PLACEHOLDER_ITEM_TAG:$index"),
                    )

                    SubtleDivider()
                }
            } else {
                items(
                    items = albumSummaries.keys.toList(),
                    key = { it.id }
                ) { albumId ->
                    val link = links[albumId]?.link ?: ""
                    val summary = albumSummaries[albumId]

                    AlbumGetLinkRowItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, end = 16.dp, bottom = 16.dp)
                            .testTag("$ALBUM_GET_MULTIPLE_LINKS_ROW_ITEM_TAG:${albumId.id}"),
                        albumSummary = summary,
                        albumLink = link,
                        onDownloadImage = onDownloadImage,
                        isSystemInDarkTheme = isSystemInDarkTheme,
                    )

                    SubtleDivider()
                }
            }
        }

        if (links.isNotEmpty()) {
            val message = pluralStringResource(
                sharedR.plurals.album_link_copied_success_message,
                links.size,
            )

            MegaOutlinedButton(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(16.dp)
                    .testTag(ALBUM_GET_MULTIPLE_LINKS_COPY_ALL_BUTTON_TAG),
                text = stringResource(id = sharedR.string.album_get_multiple_links_copy_all_action),
                onClick = {
                    val links = albumLinksList.joinToString(System.lineSeparator())
                    clipboardManager.setText(AnnotatedString(links))

                    coroutineScope.launch {
                        snackbarEventQueue.queueMessage(message)
                    }
                }
            )
        }
    }
}


@Composable
private fun AlbumGetLinkRowItem(
    albumSummary: AlbumSummary?,
    albumLink: String,
    onDownloadImage: ImageDownloader,
    isSystemInDarkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val album = albumSummary?.album
    val numPhotos = albumSummary?.numPhotos ?: 0

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumCoverImage(
            cover = album?.cover,
            onDownloadImage = onDownloadImage,
            isSystemInDarkTheme = isSystemInDarkTheme,
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MegaText(
                overflow = TextOverflow.MiddleEllipsis,
                text = album?.title.orEmpty(),
                textColor = TextColor.Primary,
                style = AppTheme.typography.titleMedium
            )

            MegaText(
                text = albumLink.ifEmpty {
                    stringResource(id = sharedR.string.album_get_link_requesting_link_placeholder)
                },
                textColor = TextColor.Secondary,
                style = AppTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (albumLink.isNotEmpty()) {
                MegaText(
                    text = pluralStringResource(
                        id = sharedR.plurals.general_num_items_template,
                        count = numPhotos,
                        numPhotos,
                    ),
                    textColor = TextColor.Secondary,
                    style = AppTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun AlbumGetLinkRowItemPlaceholder(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            modifier = Modifier
                .size(48.dp)
                .clip(shape = RoundedCornerShape(4.dp))
                .shimmerEffect(RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(20.dp)
                    .shimmerEffect(RoundedCornerShape(4.dp))
            )

            Spacer(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(16.dp)
                    .shimmerEffect(RoundedCornerShape(4.dp))
            )

            Spacer(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(16.dp)
                    .shimmerEffect(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun AlbumCoverImage(
    cover: Photo?,
    onDownloadImage: ImageDownloader,
    isSystemInDarkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val placeholder = if (isSystemInDarkTheme) {
        painterResource(R.drawable.ic_album_cover_d)
    } else {
        painterResource(R.drawable.ic_album_cover)
    }
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
        placeholder = placeholder,
        error = placeholder,
    )
}

/**
 * Test tag for placeholder items in AlbumGetMultipleLinksScreen
 */
internal const val ALBUM_GET_MULTIPLE_LINKS_PLACEHOLDER_ITEM_TAG =
    "album_get_multiple_links:placeholder_item"

/**
 * Test tag for row items in AlbumGetMultipleLinksScreen
 */
internal const val ALBUM_GET_MULTIPLE_LINKS_ROW_ITEM_TAG =
    "album_get_multiple_links:row_item"

/**
 * Test tag for copy all button in AlbumGetMultipleLinksScreen
 */
internal const val ALBUM_GET_MULTIPLE_LINKS_COPY_ALL_BUTTON_TAG =
    "album_get_multiple_links:copy_all_button"
