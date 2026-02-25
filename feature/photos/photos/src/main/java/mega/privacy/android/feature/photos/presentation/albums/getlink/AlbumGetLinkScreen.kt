package mega.privacy.android.feature.photos.presentation.albums.getlink

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
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
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.MegaOutlinedButton
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.divider.SubtleDivider
import mega.android.core.ui.components.toggle.Toggle
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.formatter.LinkFormatter
import mega.privacy.android.core.sharedcomponents.extension.isDarkMode
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.feature.photos.R
import mega.privacy.android.feature.photos.presentation.albums.copyright.CopyRightScreen
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.SingleAlbumLinkScreenEvent

private typealias ImageDownloader = (photo: Photo, callback: (Boolean) -> Unit) -> Unit

@Composable
fun AlbumGetLinkScreen(
    onBack: () -> Unit,
    onLearnMore: () -> Unit,
    onShareLink: (Album.UserAlbum?, String) -> Unit,
    modifier: Modifier = Modifier,
    albumGetLinkViewModel: AlbumGetLinkViewModel =
        hiltViewModel<AlbumGetLinkViewModel, AlbumGetLinkViewModel.Factory> {
            it.create(null)
        },
) {
    val state by albumGetLinkViewModel.stateFlow.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var showShareKeyConfirmation by rememberSaveable { mutableStateOf(false) }
    val snackbarQueue = rememberSnackBarQueue()

    LifecycleResumeEffect(Unit) {
        Analytics.tracker.trackEvent(SingleAlbumLinkScreenEvent)
        onPauseOrDispose {}
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

    LaunchedEffect(state.copyrightAgreed) {
        if (state.copyrightAgreed) {
            albumGetLinkViewModel.hideCopyright()

            if (!state.showSharingSensitiveWarning) {
                albumGetLinkViewModel.fetchAlbum()
            }
        }
    }

    if (showShareKeyConfirmation) {
        val keylessLink = LinkFormatter.extractLinkWithoutKey(state.link)
        val key = LinkFormatter.extractDecryptionKey(state.link)
        val album = state.albumSummary?.album

        BasicDialog(
            modifier = Modifier,
            title = stringResource(id = sharedR.string.album_get_link_share_link_dialog_title),
            description = stringResource(id = sharedR.string.album_get_link_share_link_dialog_description),
            positiveButtonText = stringResource(id = sharedR.string.album_get_link_share_link_dialog_share_action_only_link),
            negativeButtonText = stringResource(id = sharedR.string.album_get_link_share_link_dialog_share_action_link_key),
            onPositiveButtonClicked = {
                if (keylessLink != null) {
                    onShareLink(album, keylessLink)
                }

                showShareKeyConfirmation = false
            },
            onNegativeButtonClicked = {
                val link =
                    resources.getString(
                        sharedR.string.album_get_link_share_link_with_key,
                        keylessLink,
                        key
                    )
                onShareLink(album, link)

                showShareKeyConfirmation = false
            },
        )
    }

    if (!state.showCopyright && state.showSharingSensitiveWarning) {
        BasicDialog(
            title = stringResource(id = sharedR.string.hidden_items),
            description = stringResource(id = sharedR.string.album_has_hidden_nodes_dialog_description),
            positiveButtonText = stringResource(id = sharedR.string.button_continue),
            negativeButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
            dismissOnClickOutside = false,
            dismissOnBackPress = false,
            onPositiveButtonClicked = {
                albumGetLinkViewModel.hideSharingSensitiveWarning()
                albumGetLinkViewModel.fetchAlbum()
            },
            onDismiss = {},
            onNegativeButtonClicked = onBack,
        )
    }

    MegaScaffold(
        modifier = modifier.systemBarsPadding(),
        topBar = {
            MegaTopAppBar(
                title = pluralStringResource(
                    id = sharedR.plurals.album_get_link_screen_title,
                    count = 1
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
                        onClick = {
                            if (state.isSeparateKeyEnabled) {
                                showShareKeyConfirmation = true
                            } else {
                                onShareLink(state.albumSummary?.album, state.link)
                            }
                        }
                    ),
                )
            )
        },
        content = { innerPaddings ->
            AlbumGetLinkContent(
                modifier = Modifier.padding(innerPaddings),
                albumSummary = state.albumSummary,
                isSeparateKeyEnabled = state.isSeparateKeyEnabled,
                isSystemInDarkTheme = state.themeMode.isDarkMode(),
                link = state.link,
                onDownloadImage = albumGetLinkViewModel::downloadImage,
                onLearnMore = onLearnMore,
                onSeparateKeyChecked = albumGetLinkViewModel::toggleSeparateKeyEnabled,
                onCopyLink = { link ->
                    clipboardManager.setText(AnnotatedString(link))

                    coroutineScope.launch {
                        snackbarQueue.queueMessage(
                            resources.getQuantityString(
                                sharedR.plurals.album_link_copied_success_message,
                                1,
                            )
                        )
                    }
                },
                onCopyKey = { key ->
                    clipboardManager.setText(AnnotatedString(key))

                    coroutineScope.launch {
                        snackbarQueue.queueMessage(resources.getString(sharedR.string.album_get_link_copy_key_success_message))
                    }
                },
            )
        },
    )

    if (state.showCopyright) {
        CopyRightScreen(
            modifier = Modifier
                .systemBarsPadding()
                .fillMaxSize(),
            onAgree = {
                albumGetLinkViewModel.updateShowCopyRight(false)
                albumGetLinkViewModel.agreeCopyrightTerms()
            },
            onDisagree = {
                albumGetLinkViewModel.updateShowCopyRight(true)
                onBack()
            }
        )
    }
}

@Composable
private fun AlbumGetLinkContent(
    albumSummary: AlbumSummary?,
    isSeparateKeyEnabled: Boolean,
    isSystemInDarkTheme: Boolean,
    link: String,
    onDownloadImage: ImageDownloader,
    onLearnMore: () -> Unit,
    onSeparateKeyChecked: (Boolean) -> Unit,
    onCopyLink: (String) -> Unit,
    onCopyKey: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                isSystemInDarkTheme = isSystemInDarkTheme,
            )

            SubtleDivider(modifier = Modifier.fillMaxWidth())

            ExportSeparateKeySection(
                modifier = Modifier.padding(top = 16.dp, end = 16.dp, bottom = 16.dp),
                isSeparateKeyEnabled = isSeparateKeyEnabled,
                link = link,
                onSeparateKeyChecked = onSeparateKeyChecked,
                onLearnMore = onLearnMore,
            )

            SubtleDivider(modifier = Modifier.fillMaxWidth())

            LinkSection(
                modifier = Modifier.padding(top = 16.dp, end = 16.dp, bottom = 16.dp),
                isSeparateKeyEnabled = isSeparateKeyEnabled,
                link = link,
            )

            SubtleDivider(modifier = Modifier.fillMaxWidth())

            AnimatedVisibility(
                modifier = Modifier.padding(end = 16.dp),
                visible = isSeparateKeyEnabled,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                DecryptionKeySection(
                    modifier = Modifier.padding(top = 16.dp),
                    link = link
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            ActionsSection(
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
    albumSummary: AlbumSummary?,
    onDownloadImage: ImageDownloader,
    isSystemInDarkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val album = albumSummary?.album
    val numPhotos = albumSummary?.numPhotos ?: 0

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        content = {
            AlbumCoverImage(
                cover = album?.cover,
                onDownloadImage = onDownloadImage,
                isSystemInDarkTheme = isSystemInDarkTheme,
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                content = {
                    MegaText(
                        overflow = TextOverflow.MiddleEllipsis,
                        text = album?.title.orEmpty(),
                        textColor = TextColor.Primary,
                        style = AppTheme.typography.titleMedium
                    )

                    MegaText(
                        text = pluralStringResource(
                            id = sharedR.plurals.general_num_items_template,
                            count = numPhotos,
                            numPhotos,
                        ),
                        textColor = TextColor.Secondary,
                        style = AppTheme.typography.bodyMedium
                    )
                },
            )
        },
    )
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

@Composable
private fun ExportSeparateKeySection(
    isSeparateKeyEnabled: Boolean,
    link: String,
    onSeparateKeyChecked: (Boolean) -> Unit,
    onLearnMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .alpha(1f.takeIf { link.isNotBlank() } ?: 0.4f),
        verticalAlignment = Alignment.CenterVertically,
        content = {
            Column(
                modifier = Modifier.weight(1f),
                content = {
                    MegaText(
                        text = stringResource(id = sharedR.string.album_send_decryption_key_separately_title),
                        textColor = TextColor.Primary,
                        style = AppTheme.typography.titleSmall
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    MegaText(
                        text = stringResource(id = sharedR.string.album_send_decryption_key_separately_description),
                        textColor = TextColor.Secondary,
                        style = AppTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    MegaText(
                        modifier = Modifier
                            .clickable { onLearnMore() },
                        text = stringResource(id = sharedR.string.general_learn_more),
                        textColor = TextColor.Primary,
                        style = AppTheme.typography.titleSmall
                    )
                },
            )

            Spacer(modifier = Modifier.width(16.dp))

            Toggle(
                isChecked = isSeparateKeyEnabled,
                onCheckedChange = onSeparateKeyChecked,
                isEnabled = link.isNotBlank(),
            )
        },
    )
}

@Composable
private fun LinkSection(
    isSeparateKeyEnabled: Boolean,
    link: String,
    modifier: Modifier = Modifier,
) {
    val processedLink =
        link.takeIf { !isSeparateKeyEnabled } ?: LinkFormatter.extractLinkWithoutKey(link)

    Column(
        modifier = modifier.fillMaxWidth(),
        content = {
            MegaText(
                text = stringResource(id = sharedR.string.album_get_link_link_section_title),
                textColor = TextColor.Primary,
                style = AppTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(2.dp))

            MegaText(
                text = stringResource(id = sharedR.string.album_get_link_requesting_link_placeholder).takeIf {
                    link.isBlank()
                } ?: processedLink.orEmpty(),
                textColor = TextColor.Secondary,
                style = AppTheme.typography.bodyMedium
            )
        },
    )
}

@Composable
private fun DecryptionKeySection(
    link: String,
    modifier: Modifier = Modifier,
) {
    val key = LinkFormatter.extractDecryptionKey(link)

    Column(
        modifier = modifier.fillMaxWidth(),
        content = {
            MegaText(
                text = stringResource(id = sharedR.string.album_get_link_decryption_key_section_title),
                textColor = TextColor.Primary,
                style = AppTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(2.dp))

            MegaText(
                text = key.orEmpty(),
                textColor = TextColor.Secondary,
                style = AppTheme.typography.titleSmall
            )
        },
    )
}

@Composable
private fun ActionsSection(
    isSeparateKeyEnabled: Boolean,
    link: String,
    onCopyLink: (String) -> Unit,
    onCopyKey: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (link.isNotBlank()) {
        val processedLink = link.takeIf {
            !isSeparateKeyEnabled
        } ?: LinkFormatter.extractLinkWithoutKey(link)
        val key = LinkFormatter.extractDecryptionKey(link)

        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            content = {
                MegaOutlinedButton(
                    text = stringResource(id = sharedR.string.album_get_link_copy_link_action),
                    onClick = { onCopyLink(processedLink.orEmpty()) },
                    modifier = Modifier.wrapContentSize()
                )

                if (isSeparateKeyEnabled) {
                    MegaOutlinedButton(
                        text = stringResource(id = sharedR.string.album_get_link_copy_key_action),
                        onClick = { onCopyKey(key.orEmpty()) },
                        modifier = Modifier.wrapContentSize()
                    )
                }
            },
        )
    }
}

internal const val ALBUM_GET_LINK_SHARE_ACTION_TAG = "album_get_link_screen:share_action"