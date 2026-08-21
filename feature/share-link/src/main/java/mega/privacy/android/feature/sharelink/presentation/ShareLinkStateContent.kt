package mega.privacy.android.feature.sharelink.presentation

import android.content.ClipData
import android.content.ClipDescription
import android.os.Build
import android.os.PersistableBundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.banner.InlineInfoBanner
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.divider.SubtleDivider
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.feature.sharelink.presentation.component.ShareLinkDetails
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
import java.io.File

@Composable
internal fun SensitiveItemsWarningDialog(
    type: SensitiveWarningType,
    nodeCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val description = when (type) {
        SensitiveWarningType.Items -> if (nodeCount > 1) {
            sharedR.string.share_hidden_item_links_description
        } else {
            sharedR.string.share_hidden_item_link_description
        }

        SensitiveWarningType.Folder -> if (nodeCount > 1) {
            sharedR.string.share_hidden_folders_description
        } else {
            sharedR.string.share_hidden_folder_description
        }
    }
    BasicDialog(
        modifier = Modifier.testTag(SHARE_LINK_SENSITIVE_WARNING_TAG),
        title = stringResource(sharedR.string.hidden_items),
        description = stringResource(description),
        positiveButtonText = stringResource(sharedR.string.button_continue),
        onPositiveButtonClicked = onConfirm,
        negativeButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
        onNegativeButtonClicked = onDismiss,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun ShareLinkContent(
    uiState: ShareLinkUiState.Data,
    onCopyLink: () -> Unit,
    onCopyKey: () -> Unit,
    modifier: Modifier = Modifier,
    onCopyPassword: () -> Unit = {},
) {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val primary = uiState.primary
    val password = uiState.password?.takeIf { uiState.isPasswordSet }
    val displayLink = uiState.resolvedSingleLink()
    val separateKey = primary.key?.takeIf { uiState.isKeySeparate }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SubjectHeader(uiState = uiState)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            InlineInfoBanner(
                modifier = Modifier.testTag(SHARE_LINK_ACCESS_BANNER_TAG),
                title = stringResource(sharedR.string.share_link_access_banner_title),
                body = if (uiState.isPasswordSet) {
                    stringResource(sharedR.string.share_link_access_password_description)
                } else {
                    pluralStringResource(
                        if (uiState.isKeySeparate) {
                            sharedR.plurals.share_link_access_banner_description_with_key
                        } else {
                            sharedR.plurals.share_link_access_banner_description
                        },
                        uiState.handles.size,
                    )
                },
                showCancelButton = false,
            )

            ShareLinkDetails(
                link = displayLink,
                onCopyLink = {
                    coroutineScope.launch {
                        clipboard.setClipEntry(
                            ClipData.newPlainText(COPIED_LINK_LABEL, displayLink).toClipEntry(),
                        )
                    }
                    onCopyLink()
                },
                key = separateKey,
                onCopyKey = {
                    separateKey?.let {
                        coroutineScope.launch {
                            clipboard.setClipEntry(
                                ClipData.newPlainText(COPIED_KEY_LABEL, it).toClipEntry(),
                            )
                        }
                    }
                    onCopyKey()
                },
                passwordProtected = uiState.isPasswordSet,
                maskedPassword = password?.let { "•".repeat(it.length) },
                onCopyPassword = {
                    password?.let {
                        coroutineScope.launch {
                            clipboard.setClipEntry(sensitiveClip(COPIED_PASSWORD_LABEL, it))
                        }
                    }
                    onCopyPassword()
                },
                expirationTime = uiState.primary.expirationTime,
                isExpired = uiState.primary.isExpired,
            )
        }
    }
}

/**
 * Copies the subject's link(s) to the clipboard once, the first time the screen opens, and reports
 * it through [onCopied] so the caller can confirm with a snackbar.
 *
 * A newly created link is copied whatever the subject, matching the design's "Link created and
 * copied to your clipboard" confirmation. A link that already existed is only copied for a
 * multi-node selection, where copying the whole set on open is what the legacy several-links screen
 * did; a single existing link is left alone, as the design shows no confirmation for merely opening
 * one to manage it.
 *
 * The rememberSaveable guard keeps it to one copy per screen open, surviving recomposition, a
 * configuration change and returning to this screen.
 */
@Composable
internal fun CopyLinksOnFirstOpen(
    uiState: ShareLinkUiState.Data,
    onCopied: () -> Unit,
) {
    val clipboard = LocalClipboard.current
    val linksCopied = rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (linksCopied.value || !(uiState.hasNewLinks || uiState.isMultiNode)) {
            return@LaunchedEffect
        }
        linksCopied.value = true
        clipboard.setClipEntry(
            ClipData.newPlainText(COPIED_LINK_LABEL, uiState.linksToCopyOnOpen()).toClipEntry()
        )
        onCopied()
    }
}

/**
 * The full link, or every link for a multi-node selection.
 *
 * Deliberately the raw link rather than [resolvedSingleLink]: this only runs for a link that was
 * just created or for a multi-node selection, neither of which can carry a session password or a
 * separated key yet.
 */
private fun ShareLinkUiState.Data.linksToCopyOnOpen(): String =
    if (isMultiNode) nodeLinks.joinToString(separator = "\n") { it.link } else primary.link

@Composable
internal fun MultiNodeContent(
    uiState: ShareLinkUiState.Data,
    onCopyLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag(SHARE_LINK_MULTI_NODE_LIST_TAG)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        InlineInfoBanner(
            modifier = Modifier.testTag(SHARE_LINK_ACCESS_BANNER_TAG),
            title = stringResource(sharedR.string.share_link_access_banner_title),
            body = pluralStringResource(
                sharedR.plurals.share_link_access_banner_description,
                uiState.nodeLinks.size,
            ),
            showCancelButton = false,
        )

        uiState.nodeLinks.forEach { node ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NodeInfoRow(node = node)
                ShareLinkDetails(
                    link = node.link,
                    onCopyLink = {
                        coroutineScope.launch {
                            clipboard.setClipEntry(
                                ClipData.newPlainText(COPIED_LINK_LABEL, node.link).toClipEntry(),
                            )
                        }
                        onCopyLink()
                    },
                    expirationTime = node.expirationTime,
                    isExpired = node.isExpired,
                )
            }
        }
    }
}

/**
 * The header above the link card: the album's cover, title and photo count when an album is being
 * shared, otherwise the node row.
 */
@Composable
private fun SubjectHeader(
    uiState: ShareLinkUiState.Data,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val album = uiState.album
        if (album != null) {
            AlbumInfoRow(
                title = uiState.primary.name,
                album = album,
                modifier = Modifier.testTag(SHARE_LINK_ALBUM_HEADER_TAG),
            )
        } else {
            NodeInfoRow(
                node = uiState.primary,
                modifier = Modifier.testTag(SHARE_LINK_NODE_HEADER_TAG),
            )
        }
        SubtleDivider()
    }
}

@Composable
private fun NodeInfoRow(
    node: ShareLinkNodeItem,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    val subtitle = when {
        node.isFolder && node.childFolderCount != null && node.childFileCount != null ->
            pluralStringResource(
                sharedR.plurals.info_num_folders_and_files,
                node.childFolderCount,
                node.childFolderCount,
            ) + pluralStringResource(
                sharedR.plurals.info_num_files,
                node.childFileCount,
                node.childFileCount,
            )

        else -> remember(node.sizeInBytes, node.modificationTime) {
            buildList {
                node.sizeInBytes?.let { add(formatFileSize(it, context)) }
                node.modificationTime?.let { add(formatModifiedDate(locale, it)) }
            }.joinToString(separator = " • ")
        }
    }

    SubjectInfoRow(
        title = node.name,
        subtitle = subtitle,
        modifier = modifier,
        leading = {
            node.iconRes?.let { iconRes ->
                Image(
                    modifier = Modifier.size(32.dp),
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
private fun AlbumInfoRow(
    title: String,
    album: ShareLinkAlbumInfo,
    modifier: Modifier = Modifier,
) {
    SubjectInfoRow(
        title = title,
        subtitle = pluralStringResource(
            sharedR.plurals.general_num_items_template,
            album.photoCount,
            album.photoCount,
        ),
        modifier = modifier,
        leading = { AlbumCover(thumbnailPath = album.coverThumbnailPath) },
    )
}

/**
 * The album cover thumbnail, falling back to a placeholder while it loads and when the album is
 * empty or the thumbnail could not be fetched.
 */
@Composable
private fun AlbumCover(
    thumbnailPath: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbnailPath == null) {
            MegaIcon(
                modifier = Modifier.fillMaxSize(),
                painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Image01),
                tint = IconColor.Secondary,
                contentDescription = null,
            )
        } else {
            AsyncImage(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(SHARE_LINK_ALBUM_COVER_TAG),
                model = File(thumbnailPath),
                contentScale = ContentScale.Crop,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun SubjectInfoRow(
    title: String,
    subtitle: String,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        leading()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MegaText(
                text = title,
                textColor = TextColor.Primary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = AppTheme.typography.titleMedium,
            )
            if (subtitle.isNotEmpty()) {
                MegaText(
                    text = subtitle,
                    textColor = TextColor.Secondary,
                    style = AppTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
internal fun ShareLinkLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(SHARE_LINK_LOADING_TAG)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(
            modifier = Modifier
                .width(200.dp)
                .height(24.dp)
                .shimmerEffect(),
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .shimmerEffect(shape = RoundedCornerShape(8.dp)),
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shimmerEffect(shape = RoundedCornerShape(8.dp)),
        )
    }
}

@Composable
internal fun ShareLinkError(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(SHARE_LINK_ERROR_TAG)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        MegaText(
            text = stringResource(sharedR.string.general_request_failed_message),
            textColor = TextColor.Secondary,
            style = AppTheme.typography.bodyMedium,
        )
    }
}

@Composable
internal fun CopyrightConsent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(SHARE_LINK_COPYRIGHT_TAG)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        MegaIcon(
            modifier = Modifier.size(80.dp),
            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Copyright),
            tint = IconColor.Primary,
            contentDescription = null,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MegaText(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(sharedR.string.copyright_screen_title),
                textColor = TextColor.Primary,
                style = AppTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            MegaText(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(sharedR.string.copyright_screen_description_first_paragraph) +
                        "\n\n" +
                        stringResource(sharedR.string.copyright_screen_description_second_paragraph),
                textColor = TextColor.Secondary,
                style = AppTheme.typography.bodyLarge,
            )
        }
    }
}

/**
 * The link text placed on the system share sheet: for multiple nodes every link joined by
 * newlines; for a single node its [resolvedSingleLink].
 */
internal fun ShareLinkUiState.Data.shareableLinksText(): String =
    if (isMultiNode) {
        nodeLinks.joinToString(separator = "\n") { it.link }
    } else {
        resolvedSingleLink()
    }

/**
 * The password to offer alongside the link when sharing, or null when there is none to offer.
 * Password protection is single-node only, so the multi-node flow never asks.
 */
internal val ShareLinkUiState.Data.sharePassword: String?
    get() = password?.takeIf { isPasswordSet && !isMultiNode }

/**
 * The decryption key to offer alongside the link when sharing, or null when there is none to offer.
 * Sharing the key separately is single-node only, so the multi-node flow never asks.
 */
internal val ShareLinkUiState.Data.shareKey: String?
    get() = primary.key?.takeIf { isKeySeparate && !isMultiNode }

/**
 * The link shown and shared for a single node: the password-protected link when a password is set,
 * the key-less link when the key is shared separately, otherwise the full link.
 */
private fun ShareLinkUiState.Data.resolvedSingleLink(): String = when {
    isPasswordSet -> linkWithPassword ?: primary.link
    isKeySeparate -> primary.linkWithoutKey ?: primary.link
    else -> primary.link
}

/**
 * A plain-text clip flagged sensitive on API 33+, so the OS keeps it out of the clipboard preview
 * (used for the copied password).
 */
private fun sensitiveClip(label: String, text: String): ClipEntry {
    val clip = ClipData.newPlainText(label, text)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        clip.description.extras = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
    }
    return clip.toClipEntry()
}

internal const val COPIED_LINK_LABEL = "Copied Text"
private const val COPIED_KEY_LABEL = "Copied Key"
private const val COPIED_PASSWORD_LABEL = "Copied Password"
