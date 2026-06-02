package mega.privacy.android.feature.clouddrive.presentation.filelink

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.CircularLightIconButton
import mega.android.core.ui.components.button.InlineAnchoredButtonGroup
import mega.android.core.ui.components.button.SecondaryFilledButtonM3
import mega.android.core.ui.components.divider.SubtleDivider
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.modifiers.shimmerEffect
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
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData
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
import mega.privacy.android.shared.nodes.components.NodeThumbnailView
import mega.privacy.android.shared.nodes.components.ThumbnailLayoutType
import mega.privacy.android.shared.resources.R as sharedR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FileLinkScreen(
    uiState: FileLinkUiState,
    onProcessAction: (FileLinkAction) -> Unit,
    singleNodeActionHandler: SingleNodeActionHandler,
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    val context = LocalContext.current
    var openedFileNode by remember { mutableStateOf<TypedFileNode?>(null) }
    val coroutineScope = rememberCoroutineScope()

    FileLinkScreenContent(
        uiState = uiState,
        formattedFileSize = uiState.fileNode?.let { formatFileSize(it.size, context) }.orEmpty(),
        onSaveToMegaClicked = {
            uiState.fileNode?.let { singleNodeActionHandler(SaveToMegaMenuAction(), it) }
        },
        onDownloadClicked = {
            uiState.fileNode?.let { singleNodeActionHandler(DownloadMenuAction(), it) }
        },
        onOpenClicked = { uiState.fileNode?.let { openedFileNode = it } },
        onTransfersClicked = { onNavigate(TransfersNavKey()) },
        onAdsNavigate = onNavigate,
        onAction = onProcessAction,
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
            coroutineScope = coroutineScope,
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
                title = uiState.title.text,
                subtitle = uiState.subTitle?.text,
                navigationType = AppBarNavigationType.Close(onBack),
                trailingIcons = {
                    TransfersToolbarWidget(onClick = onTransfersClicked)
                },
                actions = buildList {
                    if (uiState.contentState is FileLinkContentState.Loaded) {
                        add(MenuActionWithClick(PublicLinkShareAction) {
                            context.startPublicLinkShareIntent(
                                link = uiState.url,
                                title = uiState.title.get(context)
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
                        showAdsForScreen = uiState.shouldShowAdsForLink,
                    ) { adsContentModifier ->
                        Column(
                            modifier = adsContentModifier
                                .fillMaxWidth()
                                .testTag(FILE_LINK_BOTTOM_BAR_TAG)
                        ) {
                            SubtleDivider()
                            InlineAnchoredButtonGroup(
                                modifier = Modifier
                                    .fillMaxWidth(),
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
            FileLinkContentState.Loading -> LoadingFileLinkContent(
                modifier = Modifier.testTag(FILE_LINK_LOADING_TAG),
            )

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
                    sharedR.string.file_link_unavaible_ToS_violation,
                ),
                modifier = Modifier.testTag(FILE_LINK_UNAVAILABLE_TAG),
            )

            is FileLinkContentState.Loaded -> {
                val fileNode = uiState.fileNode
                if (fileNode != null) {
                    LoadedFileLinkContent(
                        fileName = fileNode.name,
                        fileSize = formattedFileSize,
                        duration = state.formattedDuration,
                        iconRes = state.iconRes,
                        thumbnailData = state.thumbnailData,
                        isVideo = state.isVideo,
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
    duration: String?,
    iconRes: Int,
    thumbnailData: ThumbnailData?,
    isVideo: Boolean,
    onOpenClicked: () -> Unit,
) {
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top,
        ) {
            FileLinkThumbnail(
                fileName = fileName,
                duration = duration,
                iconRes = iconRes,
                thumbnailData = thumbnailData,
                isVideo = isVideo,
                onOpenClicked = onOpenClicked,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .aspectRatio(5f / 4f),
            )
            FileLinkInfoRow(
                fileName = fileName,
                fileSize = fileSize,
                duration = duration,
                onOpenClicked = onOpenClicked,
                modifier = Modifier
                    .weight(1f)
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            FileLinkThumbnail(
                fileName = fileName,
                duration = duration,
                iconRes = iconRes,
                thumbnailData = thumbnailData,
                isVideo = isVideo,
                onOpenClicked = onOpenClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(5f / 4f),
            )
            FileLinkInfoRow(
                fileName = fileName,
                fileSize = fileSize,
                duration = duration,
                onOpenClicked = onOpenClicked,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FileLinkThumbnail(
    fileName: String,
    duration: String?,
    iconRes: Int,
    thumbnailData: ThumbnailData?,
    isVideo: Boolean,
    onOpenClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxSurface(
        modifier = modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(6.dp)),
        surfaceColor = SurfaceColor.Surface1,
    ) {
        NodeThumbnailView(
            data = thumbnailData,
            defaultImage = iconRes,
            contentDescription = fileName,
            contentScale = ContentScale.Crop,
            layoutType = ThumbnailLayoutType.FullSize,
            modifier = Modifier
                .align(Alignment.Center)
                .testTag(FILE_LINK_THUMBNAIL_TAG),
        )

        if (isVideo) {
            CircularLightIconButton(
                modifier = Modifier
                    .align(Alignment.Center)
                    .shadow(
                        elevation = 8.dp,
                        shape = MaterialTheme.shapes.large,
                        clip = false
                    )
                    .testTag(FILE_LINK_PLAY_BUTTON_TAG),
                icon = rememberVectorPainter(IconPack.Medium.Regular.Solid.Play),
                onClick = onOpenClicked
            )
        }

        if (!duration.isNullOrEmpty()) {
            DurationBadge(
                text = duration,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .testTag(FILE_LINK_DURATION_BADGE_TAG),
            )
        }
    }
}

@Composable
private fun FileLinkInfoRow(
    fileName: String,
    fileSize: String,
    duration: String?,
    onOpenClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(16.dp),
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
                text = if (!duration.isNullOrEmpty()) {
                    stringResource(sharedR.string.file_info_subtitle_format, duration, fileSize)
                } else {
                    fileSize
                },
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

@Composable
private fun DurationBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    BoxSurface(
        surfaceColor = SurfaceColor.SurfaceTransparent,
        modifier = modifier
            .clip(RoundedCornerShape(3.dp)),
    ) {
        MegaText(
            text = text,
            textColor = TextColor.OnColor,
            style = AppTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun LoadingFileLinkContent(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(5f / 4f)
                .padding(16.dp)
                .shimmerEffect(shape = RoundedCornerShape(6.dp)),
        )

        Spacer(Modifier.height(2.dp))

        Column(
            modifier = Modifier
                .padding(16.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Spacer(
                modifier = Modifier
                    .width(177.dp)
                    .height(22.dp)
                    .shimmerEffect(),
            )

            Spacer(Modifier.height(2.dp))

            Spacer(
                modifier = Modifier
                    .width(96.dp)
                    .height(14.dp)
                    .shimmerEffect(),
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun LoadingFileLinkContentPreview() {
    AndroidThemeForPreviews {
        LoadingFileLinkContent()
    }
}

@CombinedThemePreviews
@Composable
private fun LoadedFileLinkContentPreview() {
    AndroidThemeForPreviews {
        LoadedFileLinkContent(
            fileName = "Document.pdf",
            fileSize = "12.3 MB",
            duration = null,
            iconRes = iconPackR.drawable.ic_generic_medium_solid,
            thumbnailData = null,
            isVideo = false,
            onOpenClicked = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun LoadedFileLinkContentVideoPreview() {
    AndroidThemeForPreviews {
        LoadedFileLinkContent(
            fileName = "Hobbiton.mp4",
            fileSize = "647 MB",
            duration = "2:50",
            iconRes = iconPackR.drawable.ic_video_medium_solid,
            thumbnailData = null,
            isVideo = true,
            onOpenClicked = {},
        )
    }
}

@Preview(
    name = "Landscape - Light",
    device = "spec:width=917dp,height=412dp",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "Landscape - Dark",
    device = "spec:width=917dp,height=412dp",
    showBackground = true,
    backgroundColor = 0xFF121212,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LoadedFileLinkContentLandscapePreview() {
    AndroidThemeForPreviews {
        LoadedFileLinkContent(
            fileName = "Marketing Plan 2026.pdf",
            fileSize = "10 MB",
            duration = null,
            iconRes = iconPackR.drawable.ic_pdf_medium_solid,
            thumbnailData = null,
            isVideo = false,
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
internal const val FILE_LINK_LOADING_TAG = "file_link_screen:loading"
internal const val FILE_LINK_EXPIRED_TAG = "file_link_screen:expired"
internal const val FILE_LINK_UNAVAILABLE_TAG = "file_link_screen:unavailable"
internal const val FILE_LINK_THUMBNAIL_TAG = "file_link_screen:thumbnail"
internal const val FILE_LINK_FILE_NAME_TAG = "file_link_screen:file_name"
internal const val FILE_LINK_FILE_SIZE_TAG = "file_link_screen:file_size"
internal const val FILE_LINK_OPEN_BUTTON_TAG = "file_link_screen:open_button"
internal const val FILE_LINK_DURATION_BADGE_TAG = "file_link_screen:duration_badge"
internal const val FILE_LINK_PLAY_BUTTON_TAG = "file_link_screen:play_button"
