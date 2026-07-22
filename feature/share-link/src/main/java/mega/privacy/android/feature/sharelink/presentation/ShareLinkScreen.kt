package mega.privacy.android.feature.sharelink.presentation

import android.content.ClipData
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.banner.InlineInfoBanner
import mega.android.core.ui.components.button.AnchoredButtonGroup
import mega.android.core.ui.components.divider.SubtleDivider
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.Button
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.feature.sharelink.presentation.component.ShareLinkDetails
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Revamped Share link result screen (single node).
 *
 * @param uiState The current [ShareLinkUiState].
 * @param onBack Invoked when the Close action is tapped.
 * @param onOpenSettings Invoked when the settings (gear) action is tapped.
 * @param onShareLink Invoked when the bottom "Share link" button is tapped.
 * @param onCopyLink Invoked when the copy icon on the link is tapped.
 * @param onCopyKey Invoked when the copy icon on the separate key card is tapped.
 * @param modifier Modifier for the scaffold.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareLinkScreen(
    uiState: ShareLinkUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onShareLink: () -> Unit,
    onCopyLink: () -> Unit,
    onCopyKey: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkCount = (uiState as? ShareLinkUiState.Data)?.handles?.size ?: 1

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier,
        topBar = {
            MegaTopAppBar(
                modifier = Modifier.testTag(SHARE_LINK_APP_BAR_TAG),
                title = pluralStringResource(sharedR.plurals.label_share_links, linkCount),
                subtitle = null,
                navigationType = AppBarNavigationType.Close(onBack),
                actions = buildList {
                    if (uiState is ShareLinkUiState.Data && !uiState.isMultiNode) {
                        add(MenuActionWithClick(ShareLinkSettingsAction, onOpenSettings))
                    }
                },
            )
        },
        bottomBar = {
            if (uiState is ShareLinkUiState.Data) {
                val shareText = pluralStringResource(sharedR.plurals.label_share_links, linkCount)
                AnchoredButtonGroup(
                    modifier = Modifier.fillMaxWidth(),
                    buttonGroup = listOf(
                        {
                            Button.PrimaryButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(SHARE_LINK_SHARE_BUTTON_TAG),
                                text = shareText,
                                onClick = onShareLink,
                            )
                        },
                    ),
                )
            }
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            when (uiState) {
                ShareLinkUiState.Loading -> ShareLinkLoading()
                ShareLinkUiState.Error -> ShareLinkError()
                is ShareLinkUiState.Data -> if (uiState.isMultiNode) {
                    MultiNodeContent(
                        uiState = uiState,
                        onCopyLink = onCopyLink,
                    )
                } else {
                    ShareLinkContent(
                        uiState = uiState,
                        onCopyLink = onCopyLink,
                        onCopyKey = onCopyKey,
                    )
                }
            }
        }
    }

}

@Composable
private fun ShareLinkContent(
    uiState: ShareLinkUiState.Data,
    onCopyLink: () -> Unit,
    onCopyKey: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val primary = uiState.primary
    val displayLink = primary.linkWithoutKey?.takeIf { uiState.isKeySeparate } ?: primary.link
    val separateKey = primary.key?.takeIf { uiState.isKeySeparate }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        NodeHeader(node = primary)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            InlineInfoBanner(
                modifier = Modifier.testTag(SHARE_LINK_ACCESS_BANNER_TAG),
                title = stringResource(sharedR.string.share_link_access_banner_title),
                body = pluralStringResource(
                    if (uiState.isKeySeparate) {
                        sharedR.plurals.share_link_access_banner_description_with_key
                    } else {
                        sharedR.plurals.share_link_access_banner_description
                    },
                    uiState.handles.size,
                ),
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
            )
        }
    }
}

@Composable
private fun MultiNodeContent(
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
                )
            }
        }
    }
}

@Composable
private fun NodeHeader(
    node: ShareLinkNodeItem,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        NodeInfoRow(
            node = node,
            modifier = Modifier.testTag(SHARE_LINK_NODE_HEADER_TAG),
        )
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Image(
            modifier = Modifier.size(32.dp),
            painter = painterResource(id = node.iconRes),
            contentDescription = null,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MegaText(
                text = node.name,
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
private fun ShareLinkLoading(modifier: Modifier = Modifier) {
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
private fun ShareLinkError(modifier: Modifier = Modifier) {
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

/**
 * Toolbar action that opens the Link settings editor from the Share link screen.
 */
internal data object ShareLinkSettingsAction : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() =
        rememberVectorPainter(IconPack.Medium.Thin.Outline.GearSix)

    override val testTag = "share_link_screen:action_settings"

    @Composable
    override fun getDescription() = stringResource(sharedR.string.general_settings)
}

private val previewData = ShareLinkUiState.Data(
    nodeLinks = listOf(
        ShareLinkNodeItem(
            handle = 1L,
            name = "Presentation.pdf",
            isFolder = false,
            iconRes = iconPackR.drawable.ic_pdf_medium_solid,
            sizeInBytes = 10L * 1024 * 1024,
            modificationTime = 1_749_000_000L,
            childFolderCount = null,
            childFileCount = null,
            link = "https://mega.nz/file/abc123#decryptionKey",
            linkWithoutKey = "https://mega.nz/file/abc123",
            key = "decryptionKey",
        ),
    ),
    accountType = null,
)

@CombinedThemePreviews
@Composable
private fun ShareLinkScreenDataPreview() {
    AndroidThemeForPreviews {
        ShareLinkScreen(
            uiState = previewData,
            onBack = {},
            onOpenSettings = {},
            onShareLink = {},
            onCopyLink = {},
            onCopyKey = {},
        )
    }
}

private val previewMultiNodeData = ShareLinkUiState.Data(
    nodeLinks = listOf(
        ShareLinkNodeItem(
            handle = 1L,
            name = "Documents",
            isFolder = true,
            iconRes = iconPackR.drawable.ic_folder_medium_solid,
            sizeInBytes = null,
            modificationTime = null,
            childFolderCount = 6,
            childFileCount = 12,
            link = "https://mega.nz/folder/abc123#folderKey",
            linkWithoutKey = "https://mega.nz/folder/abc123",
            key = "folderKey",
        ),
        ShareLinkNodeItem(
            handle = 2L,
            name = "Presentation.pdf",
            isFolder = false,
            iconRes = iconPackR.drawable.ic_pdf_medium_solid,
            sizeInBytes = 10L * 1024 * 1024,
            modificationTime = 1_749_000_000L,
            childFolderCount = null,
            childFileCount = null,
            link = "https://mega.nz/file/def456#fileKey",
            linkWithoutKey = "https://mega.nz/file/def456",
            key = "fileKey",
        ),
    ),
    accountType = null,
)

@CombinedThemePreviews
@Composable
private fun ShareLinkScreenMultiNodePreview() {
    AndroidThemeForPreviews {
        ShareLinkScreen(
            uiState = previewMultiNodeData,
            onBack = {},
            onOpenSettings = {},
            onShareLink = {},
            onCopyLink = {},
            onCopyKey = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ShareLinkScreenLoadingPreview() {
    AndroidThemeForPreviews {
        ShareLinkScreen(
            uiState = ShareLinkUiState.Loading,
            onBack = {},
            onOpenSettings = {},
            onShareLink = {},
            onCopyLink = {},
            onCopyKey = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ShareLinkScreenErrorPreview() {
    AndroidThemeForPreviews {
        ShareLinkScreen(
            uiState = ShareLinkUiState.Error,
            onBack = {},
            onOpenSettings = {},
            onShareLink = {},
            onCopyLink = {},
            onCopyKey = {},
        )
    }
}

internal const val SHARE_LINK_APP_BAR_TAG = "share_link_screen:app_bar"
internal const val SHARE_LINK_SHARE_BUTTON_TAG = "share_link_screen:button_share"
internal const val SHARE_LINK_NODE_HEADER_TAG = "share_link_screen:node_header"
internal const val SHARE_LINK_MULTI_NODE_LIST_TAG = "share_link_screen:multi_node_list"
internal const val SHARE_LINK_ACCESS_BANNER_TAG = "share_link_screen:access_banner"
internal const val SHARE_LINK_LOADING_TAG = "share_link_screen:loading"
internal const val SHARE_LINK_ERROR_TAG = "share_link_screen:error"
private const val COPIED_LINK_LABEL = "Copied Text"
private const val COPIED_KEY_LABEL = "Copied Key"
