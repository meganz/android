package mega.privacy.android.feature.clouddrive.presentation.filelink

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.InlineAnchoredButtonGroup
import mega.android.core.ui.components.button.SecondaryFilledButtonM3
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.nodecomponents.action.HandleNodeAction3
import mega.privacy.android.core.nodecomponents.action.NodeSourceData
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionHandler
import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.SaveToMegaMenuAction
import mega.privacy.android.core.transfers.widget.TransfersToolbarWidget
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.presentation.filelink.model.FileLinkAction
import mega.privacy.android.feature.clouddrive.presentation.filelink.model.FileLinkContentState
import mega.privacy.android.feature.clouddrive.presentation.filelink.model.FileLinkUiState
import mega.privacy.android.feature.clouddrive.presentation.publiclink.model.PublicLinkShareAction
import mega.privacy.android.feature.clouddrive.presentation.publiclink.model.startPublicLinkShareIntent
import mega.privacy.android.feature.clouddrive.presentation.publiclink.view.DecryptionKeyDialog
import mega.privacy.android.feature.clouddrive.presentation.publiclink.view.ExpiredLinkView
import mega.privacy.android.feature.clouddrive.presentation.publiclink.view.UnavailableLinkView
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.destination.TransfersNavKey
import mega.privacy.android.shared.ads.NewAdsContainer
import mega.privacy.android.shared.resources.R as sharedR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FileLinkScreen(
    viewModel: FileLinkViewModel,
    singleNodeActionHandler: SingleNodeActionHandler,
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var openedFileNode by remember { mutableStateOf<TypedFileNode?>(null) }

    FileLinkScreenContent(
        uiState = uiState,
        formattedFileSize = uiState.fileNode?.let { formatFileSize(it.size, context) }.orEmpty(),
        onSaveToMegaClicked = {
            // TODO
            uiState.fileNode?.let { singleNodeActionHandler(SaveToMegaMenuAction(), it) }
        },
        onDownloadClicked = {
            // TODO
            uiState.fileNode?.let { singleNodeActionHandler(DownloadMenuAction(), it) }
        },
        onOpenClicked = { uiState.fileNode?.let { openedFileNode = it } },
        onTransfersClicked = { onNavigate(TransfersNavKey()) },
        onAdsNavigate = onNavigate,
        onAction = viewModel::processAction,
        onBack = onBack,
    )

    openedFileNode?.let { fileNode ->
        val sourceUrl = uiState.url ?: return@let
        HandleNodeAction3(
            typedFileNode = fileNode,
            nodeSourceData = NodeSourceData.FileLink(url = sourceUrl),
            onNavigate = onNavigate,
            onActionHandled = { openedFileNode = null },
            onDownloadEvent = onTransfer,
        )
    }

    BackHandler(onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FileLinkScreenContent(
    uiState: FileLinkUiState,
    formattedFileSize: String,
    onSaveToMegaClicked: () -> Unit,
    onDownloadClicked: () -> Unit,
    onOpenClicked: () -> Unit,
    onTransfersClicked: () -> Unit,
    onAdsNavigate: (NavKey) -> Unit,
    onAction: (FileLinkAction) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    MegaScaffoldWithTopAppBarScrollBehavior(
        topBar = {
            MegaTopAppBar(
                modifier = Modifier.testTag(FILE_LINK_APP_BAR_TAG),
                title = uiState.title,
                subtitle = stringResource(sharedR.string.file_link_subtitle),
                navigationType = AppBarNavigationType.Close(onBack),
                trailingIcons = {
                    TransfersToolbarWidget(onClick = onTransfersClicked)
                },
                actions = buildList {
                    if (uiState.contentState is FileLinkContentState.Loaded) {
                        add(MenuActionWithClick(PublicLinkShareAction) {
                            context.startPublicLinkShareIntent(
                                link = uiState.url,
                                title = uiState.title
                            )
                        })
                    }
                },
            )
        },
        bottomBar = {
            if (uiState.contentState is FileLinkContentState.Loaded) {
                BoxSurface(surfaceColor = SurfaceColor.Surface1) {
                    NewAdsContainer(
                        modifier = Modifier.fillMaxWidth(),
                        onNavigate = onAdsNavigate,
                    ) { adsContentModifier ->
                        InlineAnchoredButtonGroup(
                            modifier = adsContentModifier
                                .fillMaxWidth()
                                .testTag(FILE_LINK_BOTTOM_BAR_TAG),
                            primaryButtonText = stringResource(sharedR.string.node_option_save_to_mega),
                            primaryButtonLeadingIcon = rememberVectorPainter(
                                IconPack.Medium.Thin.Outline.CloudUpload,
                            ),
                            onPrimaryButtonClick = onSaveToMegaClicked,
                            textOnlyButtonText = stringResource(sharedR.string.general_save_to_device),
                            onTextOnlyButtonClick = onDownloadClicked,
                            applyInsets = true,
                        )
                    }
                }
            }
        },
    ) { contentPadding ->
        FileLinkContent(
            uiState = uiState,
            formattedFileSize = formattedFileSize,
            onOpenClicked = onOpenClicked,
            onAction = onAction,
            onBack = onBack,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        )
    }
}

@Composable
internal fun FileLinkContent(
    uiState: FileLinkUiState,
    formattedFileSize: String,
    onOpenClicked: () -> Unit,
    onAction: (FileLinkAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        when (val state = uiState.contentState) {
            FileLinkContentState.Loading -> Unit // TODO shimmer
            is FileLinkContentState.DecryptionKeyRequired -> DecryptionKeyDialog(
                isKeyIncorrect = state.isKeyIncorrect,
                onDecryptionKeyEntered = { key ->
                    onAction(FileLinkAction.DecryptionKeyEntered(key))
                },
                onDismiss = {
                    onAction(FileLinkAction.DecryptionKeyDialogDismissed)
                    onBack()
                },
            )

            FileLinkContentState.Expired -> ExpiredLinkView(
                title = sharedR.string.file_link_expired_title,
                modifier = Modifier.testTag(FILE_LINK_EXPIRED_TAG),
            )

            FileLinkContentState.Unavailable -> UnavailableLinkView(
                title = sharedR.string.file_link_unavailable_title,
                subtitle = sharedR.string.general_link_unavailable_subtitle,
                bulletPoints = listOf(
                    sharedR.string.file_link_unavailable_deleted,
                    sharedR.string.file_link_unavailable_disabled,
                    sharedR.string.general_link_unavailable_invalid_url,
                ),
                modifier = Modifier.testTag(FILE_LINK_UNAVAILABLE_TAG),
            )

            FileLinkContentState.Loaded -> {
                val fileNode = uiState.fileNode
                val iconRes = uiState.iconRes
                if (fileNode != null && iconRes != null) {
                    LoadedFileLinkContent(
                        fileName = fileNode.name,
                        fileSize = formattedFileSize,
                        iconRes = iconRes,
                        onOpenClicked = onOpenClicked,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadedFileLinkContent(
    fileName: String,
    fileSize: String,
    iconRes: Int,
    onOpenClicked: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BoxSurface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(5f / 4f)
                .padding(16.dp)
                .clip(RoundedCornerShape(6.dp)),
            surfaceColor = SurfaceColor.Surface1,
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(120.dp)
                    .testTag(FILE_LINK_THUMBNAIL_TAG),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                MegaText(
                    text = fileName,
                    textColor = TextColor.Primary,
                    style = AppTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(FILE_LINK_FILE_NAME_TAG),
                )
                MegaText(
                    text = fileSize,
                    textColor = TextColor.Secondary,
                    style = AppTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(FILE_LINK_FILE_SIZE_TAG),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            SecondaryFilledButtonM3(
                modifier = Modifier.testTag(FILE_LINK_OPEN_BUTTON_TAG),
                text = stringResource(sharedR.string.general_open_button),
                onClick = onOpenClicked,
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun LoadedFileLinkContentPreview() {
    AndroidThemeForPreviews {
        LoadedFileLinkContent(
            fileName = "Document.pdf",
            fileSize = "12.3 MB",
            iconRes = iconPackR.drawable.ic_generic_medium_solid,
            onOpenClicked = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun FileLinkScreenContentExpiredPreview() {
    AndroidThemeForPreviews {
        FileLinkScreenContent(
            uiState = FileLinkUiState(contentState = FileLinkContentState.Expired),
            formattedFileSize = "",
            onSaveToMegaClicked = {},
            onDownloadClicked = {},
            onOpenClicked = {},
            onTransfersClicked = {},
            onAdsNavigate = {},
            onAction = {},
            onBack = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun FileLinkScreenContentUnavailablePreview() {
    AndroidThemeForPreviews {
        FileLinkScreenContent(
            uiState = FileLinkUiState(contentState = FileLinkContentState.Unavailable),
            formattedFileSize = "",
            onSaveToMegaClicked = {},
            onDownloadClicked = {},
            onOpenClicked = {},
            onTransfersClicked = {},
            onAdsNavigate = {},
            onAction = {},
            onBack = {},
        )
    }
}

internal const val FILE_LINK_APP_BAR_TAG = "file_link_screen:main_app_bar"
internal const val FILE_LINK_BOTTOM_BAR_TAG = "file_link_screen:bottom_bar"
internal const val FILE_LINK_EXPIRED_TAG = "file_link_screen:expired"
internal const val FILE_LINK_UNAVAILABLE_TAG = "file_link_screen:unavailable"
internal const val FILE_LINK_THUMBNAIL_TAG = "file_link_screen:thumbnail"
internal const val FILE_LINK_FILE_NAME_TAG = "file_link_screen:file_name"
internal const val FILE_LINK_FILE_SIZE_TAG = "file_link_screen:file_size"
internal const val FILE_LINK_OPEN_BUTTON_TAG = "file_link_screen:open_button"
