package mega.privacy.android.app.presentation.photos.albums.getlink

import android.text.TextUtils.TruncateAt.MIDDLE
import android.view.View
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.getLink.CopyrightFragment
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.core.ui.theme.grey_alpha_012
import mega.privacy.android.core.ui.theme.grey_alpha_054
import mega.privacy.android.core.ui.theme.grey_alpha_087
import mega.privacy.android.core.ui.theme.teal_300
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.core.ui.theme.white_alpha_012
import mega.privacy.android.core.ui.theme.white_alpha_054
import mega.privacy.android.core.ui.theme.white_alpha_087
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.legacy.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.mobile.analytics.event.SingleAlbumLinkScreenEvent

private typealias ImageDownloader = (photo: Photo, callback: (Boolean) -> Unit) -> Unit

@Composable
internal fun AlbumGetLinkScreen(
    albumGetLinkViewModel: AlbumGetLinkViewModel = viewModel(),
    createView: (Fragment) -> View,
    onBack: () -> Unit,
    onLearnMore: () -> Unit,
    onShareLink: (Album.UserAlbum?, String) -> Unit,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {
    val isLight = MaterialTheme.colors.isLight
    val state by albumGetLinkViewModel.stateFlow.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    var showShareKeyConfirmation by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Analytics.tracker.trackEvent(SingleAlbumLinkScreenEvent)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(state.isInitialized) {
        if (!state.isInitialized) {
            albumGetLinkViewModel.initialize()
        }
    }

    LaunchedEffect(state.exitScreen) {
        if (state.exitScreen) {
            onBack()
        }
    }

    BackHandler {
        if (state.showCopyright) {
            albumGetLinkViewModel.hideCopyright()
        } else {
            onBack()
        }
    }

    if (showShareKeyConfirmation) {
        val keylessLink = LinksUtil.getLinkWithoutKey(state.link)
        val key = LinksUtil.getKeyLink(state.link)
        val album = state.albumSummary?.album

        ShareKeyConfirmationDialog(
            onDismiss = {
                val link = context.getString(R.string.share_link_with_key, keylessLink, key)
                onShareLink(album, link)

                showShareKeyConfirmation = false
            },
            onShareKey = {
                val link = keylessLink
                onShareLink(album, link)

                showShareKeyConfirmation = false
            },
        )
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            AlbumGetLinkTopBar(
                link = state.link,
                onBack = onBack,
                onShareLink = {
                    if (state.isSeparateKeyEnabled) {
                        showShareKeyConfirmation = true
                    } else {
                        onShareLink(state.albumSummary?.album, state.link)
                    }
                },
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
                },
            )
        },
        content = { innerPaddings ->
            AlbumGetLinkContent(
                modifier = Modifier.padding(innerPaddings),
                albumSummary = state.albumSummary,
                isSeparateKeyEnabled = state.isSeparateKeyEnabled,
                link = state.link,
                onDownloadImage = albumGetLinkViewModel::downloadImage,
                onLearnMore = onLearnMore,
                onSeparateKeyChecked = albumGetLinkViewModel::toggleSeparateKeyEnabled,
                onCopyLink = { link ->
                    clipboardManager.setText(AnnotatedString(link))

                    coroutineScope.launch {
                        scaffoldState.snackbarHostState.showSnackbar(
                            message = context.resources.getQuantityString(
                                R.plurals.album_share_links_copied,
                                1,
                            ),
                        )
                    }
                },
                onCopyKey = { key ->
                    clipboardManager.setText(AnnotatedString(key))

                    coroutineScope.launch {
                        scaffoldState.snackbarHostState.showSnackbar(
                            message = context.getString(R.string.key_copied_clipboard),
                        )
                    }
                },
            )
        },
    )

    if (state.showCopyright) {
        Surface {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    val fragment = CopyrightFragment()
                    createView(fragment)
                },
            )
        }
    }
}

@Composable
private fun AlbumGetLinkTopBar(
    modifier: Modifier = Modifier,
    link: String,
    onBack: () -> Unit,
    onShareLink: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight

    TopAppBar(
        title = {
            Text(
                text = pluralStringResource(id = R.plurals.album_share_get_links, count = 1),
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
                modifier = Modifier.alpha(0.4f.takeIf { link.isBlank() } ?: 1f),
                enabled = link.isNotBlank(),
                content = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_social_share_white),
                        contentDescription = null,
                        tint = grey_alpha_087.takeIf { isLight } ?: white_alpha_087,
                    )
                },
            )
        },
        elevation = 0.dp,
    )
}

@Composable
private fun AlbumGetLinkContent(
    modifier: Modifier = Modifier,
    albumSummary: AlbumSummary?,
    isSeparateKeyEnabled: Boolean,
    link: String,
    onDownloadImage: ImageDownloader,
    onLearnMore: () -> Unit,
    onSeparateKeyChecked: (Boolean) -> Unit,
    onCopyLink: (String) -> Unit,
    onCopyKey: (String) -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState())
            .padding(start = 16.dp, top = 16.dp),
        content = {
            AlbumSummarySection(
                modifier = Modifier.padding(end = 16.dp, bottom = 16.dp),
                albumSummary = albumSummary,
                onDownloadImage = onDownloadImage,
            )

            Divider(
                color = grey_alpha_012.takeIf { isLight } ?: white_alpha_012,
                thickness = 1.dp,
            )

            ExportSeparateKeySection(
                modifier = Modifier.padding(top = 16.dp, end = 16.dp, bottom = 16.dp),
                isSeparateKeyEnabled = isSeparateKeyEnabled,
                link = link,
                onSeparateKeyChecked = onSeparateKeyChecked,
                onLearnMore = onLearnMore,
            )

            Divider(
                color = grey_alpha_012.takeIf { isLight } ?: white_alpha_012,
                thickness = 1.dp,
            )

            LinkSection(
                modifier = Modifier.padding(top = 16.dp, end = 16.dp, bottom = 16.dp),
                isSeparateKeyEnabled = isSeparateKeyEnabled,
                link = link,
            )

            Divider(
                color = grey_alpha_012.takeIf { isLight } ?: white_alpha_012,
                thickness = 1.dp,
            )

            if (isSeparateKeyEnabled) {
                DecryptionKeySection(
                    modifier = Modifier.padding(top = 16.dp, end = 16.dp, bottom = 16.dp),
                    link = link,
                )
            }

            ActionsSection(
                modifier = Modifier.padding(vertical = 16.dp),
                isSeparateKeyEnabled = isSeparateKeyEnabled,
                link = link,
                onCopyLink = onCopyLink,
                onCopyKey = onCopyKey,
            )
        },
    )
}

@Composable
private fun AlbumSummarySection(
    modifier: Modifier = Modifier,
    albumSummary: AlbumSummary?,
    onDownloadImage: ImageDownloader,
) {
    val isLight = MaterialTheme.colors.isLight

    val album = albumSummary?.album
    val numPhotos = albumSummary?.numPhotos ?: 0

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        content = {
            AlbumCoverImage(
                cover = album?.cover,
                onDownloadImage = onDownloadImage,
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                content = {
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
                        text = pluralStringResource(
                            id = R.plurals.general_num_items,
                            count = numPhotos,
                            numPhotos,
                        ),
                        color = grey_alpha_054.takeIf { isLight } ?: white_alpha_054,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W400,
                        style = MaterialTheme.typography.subtitle2,
                    )
                },
            )
        },
    )
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

@Composable
private fun ExportSeparateKeySection(
    modifier: Modifier = Modifier,
    isSeparateKeyEnabled: Boolean,
    link: String,
    onSeparateKeyChecked: (Boolean) -> Unit,
    onLearnMore: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight

    Row(
        modifier = modifier
            .alpha(1f.takeIf { link.isNotBlank() } ?: 0.4f),
        verticalAlignment = Alignment.CenterVertically,
        content = {
            Column(
                modifier = Modifier.weight(1f),
                content = {
                    Text(
                        text = stringResource(id = R.string.option_send_decryption_key_separately),
                        color = grey_alpha_087.takeIf { isLight } ?: white_alpha_087,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                        style = MaterialTheme.typography.subtitle2,
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = stringResource(id = R.string.explanation_send_decryption_key_separately),
                        color = grey_alpha_054.takeIf { isLight } ?: white_alpha_054,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W400,
                        style = MaterialTheme.typography.subtitle2,
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = stringResource(id = R.string.learn_more_option),
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = link.isNotBlank(),
                                onClick = onLearnMore,
                            ),
                        color = teal_300,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                        style = MaterialTheme.typography.body2,
                    )
                },
            )

            Spacer(modifier = Modifier.width(16.dp))

            MegaSwitch(
                checked = isSeparateKeyEnabled,
                onCheckedChange = onSeparateKeyChecked,
                enabled = link.isNotBlank(),
            )
        },
    )
}

@Composable
private fun LinkSection(
    modifier: Modifier = Modifier,
    isSeparateKeyEnabled: Boolean,
    link: String,
) {
    val isLight = MaterialTheme.colors.isLight
    val processedLink = link.takeIf { !isSeparateKeyEnabled } ?: LinksUtil.getLinkWithoutKey(link)

    Column(
        modifier = modifier.fillMaxWidth(),
        content = {
            Text(
                text = stringResource(id = R.string.file_properties_shared_folder_public_link_name),
                color = grey_alpha_087.takeIf { isLight } ?: white_alpha_087,
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
                style = MaterialTheme.typography.subtitle2,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = stringResource(id = R.string.link_request_status).takeIf {
                    link.isBlank()
                } ?: processedLink,
                color = grey_alpha_054.takeIf { isLight } ?: white_alpha_054,
                fontSize = 14.sp,
                fontWeight = FontWeight.W400,
                style = MaterialTheme.typography.subtitle2,
            )
        },
    )
}

@Composable
private fun DecryptionKeySection(
    modifier: Modifier = Modifier,
    link: String,
) {
    val isLight = MaterialTheme.colors.isLight
    val key = LinksUtil.getKeyLink(link)

    Column(
        modifier = modifier.fillMaxWidth(),
        content = {
            Text(
                text = stringResource(id = R.string.key_label),
                color = grey_alpha_087.takeIf { isLight } ?: white_alpha_087,
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
                style = MaterialTheme.typography.subtitle2,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = key,
                color = grey_alpha_054.takeIf { isLight } ?: white_alpha_054,
                fontSize = 14.sp,
                fontWeight = FontWeight.W400,
                style = MaterialTheme.typography.subtitle2,
            )
        },
    )
}

@Composable
private fun ActionsSection(
    modifier: Modifier = Modifier,
    isSeparateKeyEnabled: Boolean,
    link: String,
    onCopyLink: (String) -> Unit,
    onCopyKey: (String) -> Unit,
) {
    if (link.isNotBlank()) {
        val processedLink = link.takeIf {
            !isSeparateKeyEnabled
        } ?: LinksUtil.getLinkWithoutKey(link)
        val key = LinksUtil.getKeyLink(link)

        Row(
            modifier = modifier,
            content = {
                OutlinedButton(
                    onClick = { onCopyLink(processedLink) },
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, teal_300),
                    content = {
                        Text(
                            text = stringResource(id = R.string.button_copy_link),
                            color = teal_300,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W500,
                            style = MaterialTheme.typography.button,
                        )
                    },
                )

                Spacer(modifier = Modifier.width(16.dp))

                if (isSeparateKeyEnabled) {
                    OutlinedButton(
                        onClick = { onCopyKey(key) },
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, teal_300),
                        content = {
                            Text(
                                text = stringResource(id = R.string.button_copy_key),
                                color = teal_300,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.W500,
                                style = MaterialTheme.typography.button,
                            )
                        },
                    )
                }
            },
        )
    }
}

@Composable
private fun ShareKeyConfirmationDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onShareKey: () -> Unit,
) {
    ConfirmationDialog(
        modifier = modifier,
        title = stringResource(id = R.string.album_share_share_link_dialog_title),
        text = stringResource(id = R.string.album_share_share_link_dialog_body),
        confirmButtonText = stringResource(id = R.string.album_share_share_link_dialog_button_link_only),
        cancelButtonText = stringResource(id = R.string.album_share_share_link_dialog_button_link_and_key),
        onConfirm = onShareKey,
        onDismiss = onDismiss,
    )
}
