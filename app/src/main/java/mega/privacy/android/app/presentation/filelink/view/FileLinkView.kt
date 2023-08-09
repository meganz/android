package mega.privacy.android.app.presentation.filelink.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.components.transferWidget.TransfersWidgetView
import mega.privacy.android.app.presentation.fileinfo.view.FileInfoHeader
import mega.privacy.android.app.presentation.filelink.model.FileLinkState
import mega.privacy.android.app.presentation.transfers.TransferManagementUiState
import mega.privacy.android.app.utils.Util
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.controls.dialogs.LoadingDialog
import mega.privacy.android.core.ui.controls.snackbars.MegaSnackbar
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_020_grey_700
import mega.privacy.android.core.ui.theme.white

/**
 * View to render the File Link Screen, including toolbar, content, etc.
 */

internal const val IMPORT_BUTTON_TAG = "file_link_view:button_import"
internal const val SAVE_BUTTON_TAG = "file_link_view:button_save"

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun FileLinkView(
    viewState: FileLinkState,
    transferState: TransferManagementUiState,
    onBackPressed: () -> Unit,
    onShareClicked: () -> Unit,
    onPreviewClick: () -> Unit,
    onSaveToDeviceClicked: () -> Unit,
    onImportClicked: () -> Unit,
    onTransferWidgetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val snackBarHostState = remember { SnackbarHostState() }

    Box(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current.density
        val statusBarHeight = Util.getStatusBarHeight().toFloat() / density
        val tintColorBase = MaterialTheme.colors.onSurface

        val headerHeight by remember {
            derivedStateOf {
                (headerMaxHeight(statusBarHeight) - (scrollState.value / density))
                    .coerceAtLeast(headerMinHeight(statusBarHeight))
            }
        }
        val titleDisplacement by remember {
            derivedStateOf {
                ((headerHeight - headerGoneHeight(statusBarHeight)) * 0.85f).coerceAtLeast(0f)
            }
        }
        val longTitleAlpha by remember {
            derivedStateOf {
                (titleDisplacement / (appBarHeight / 2)).coerceIn(0f, 1f)
            }
        }

        val headerBackgroundAlpha by remember {
            derivedStateOf {
                ((headerHeight - headerGoneHeight(statusBarHeight))
                        / (headerStartGoneHeight(statusBarHeight) - headerGoneHeight(statusBarHeight)))
                    .coerceIn(0f, 1f)
            }
        }
        val topBarOpacityTransitionDelta by remember {
            derivedStateOf {
                1 - ((headerHeight - headerMinHeight(statusBarHeight))
                        / (headerGoneHeight(statusBarHeight) - headerMinHeight(statusBarHeight)))
                    .coerceIn(0f, 1f)
            }
        }
        val tintColor by remember(viewState.previewPath != null) {
            derivedStateOf {
                if (viewState.previewPath != null) {
                    lerp(tintColorBase, white, headerBackgroundAlpha)
                } else {
                    tintColorBase
                }
            }
        }

        FileInfoHeader(
            title = viewState.title,
            titleAlpha = longTitleAlpha,
            titleDisplacement = titleDisplacement.dp,
            tintColor = tintColor,
            backgroundAlpha = headerBackgroundAlpha,
            previewUri = viewState.previewPath,
            iconResource = viewState.iconResource,
            accessPermissionDescription = null,
            modifier = Modifier
                .height(headerHeight.dp),
            statusBarHeight = statusBarHeight.dp,
        )

        Scaffold(
            modifier = Modifier.systemBarsPadding(),
            backgroundColor = Color.Transparent,
            topBar = {
                FileLinkTopBar(
                    title = viewState.title,
                    onBackPressed = onBackPressed,
                    onShareClicked = onShareClicked,
                    opacityTransitionDelta = topBarOpacityTransitionDelta,
                    tintColor = tintColor,
                    titleDisplacement = titleDisplacement.dp,
                    titleAlpha = 1 - longTitleAlpha,
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackBarHostState) { data ->
                    MegaSnackbar(snackbarData = data)
                }
            },
        ) { innerPadding ->
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                //to set the minimum height of the colum so it's always possible to collapse the header
                val boxWithConstraintsScope = this
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(innerPadding)
                ) {
                    Spacer(Modifier.height(spacerHeight(statusBarHeight).dp)) //to give space for the header (that it's outside this column)
                    FileLinkContent(
                        viewState = viewState,
                        onPreviewClick = onPreviewClick,
                        modifier = Modifier.heightIn(
                            min = boxWithConstraintsScope.maxHeight
                        )
                    )
                }
                ImportDownloadView(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.BottomEnd)
                        .background(MaterialTheme.colors.grey_020_grey_700),
                    hasDbCredentials = viewState.hasDbCredentials,
                    onImportClicked = onImportClicked,
                    onSaveToDeviceClicked = onSaveToDeviceClicked
                )

                AnimatedVisibility(
                    visible = transferState.widgetVisible,
                    enter = scaleIn(animationSpecs, initialScale = animationScale) +
                            fadeIn(animationSpecs),
                    exit = scaleOut(animationSpecs, targetScale = animationScale) +
                            fadeOut(animationSpecs),
                    modifier = Modifier
                        .padding(bottom = 48.dp, end = 8.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    TransfersWidgetView(
                        transfersData = transferState.transfersInfo,
                        onClick = onTransferWidgetClick,
                    )
                }
            }
        }
        viewState.jobInProgressState?.progressMessage?.let {
            LoadingDialog(text = stringResource(id = it))
        }
    }
}

@Composable
internal fun ImportDownloadView(
    modifier: Modifier,
    hasDbCredentials: Boolean,
    onImportClicked: () -> Unit,
    onSaveToDeviceClicked: () -> Unit,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.End) {
        TextMegaButton(
            modifier = Modifier
                .padding(end = 16.dp)
                .testTag(SAVE_BUTTON_TAG),
            textId = R.string.general_save_to_device,
            onClick = onSaveToDeviceClicked
        )
        if (hasDbCredentials) {
            TextMegaButton(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .testTag(IMPORT_BUTTON_TAG),
                textId = R.string.add_to_cloud,
                onClick = onImportClicked
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewImportDownloadView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ImportDownloadView(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(MaterialTheme.colors.grey_020_grey_700),
            hasDbCredentials = true,
            onImportClicked = {},
            onSaveToDeviceClicked = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewFileLinkView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        val viewState =
            FileLinkState(hasDbCredentials = true, title = "Title", sizeInBytes = 10000L)
        FileLinkView(
            viewState = viewState,
            transferState = TransferManagementUiState(),
            onBackPressed = {},
            onShareClicked = {},
            onPreviewClick = {},
            onSaveToDeviceClicked = {},
            onImportClicked = {},
            onTransferWidgetClick = {}
        )
    }
}

internal const val animationDuration = 300
internal const val animationScale = 0.2f
internal val animationSpecs = TweenSpec<Float>(durationMillis = animationDuration)
internal const val appBarHeight = 56f
private fun headerMinHeight(statusBarHeight: Float) = appBarHeight + statusBarHeight
private fun headerMaxHeight(statusBarHeight: Float) = headerMinHeight(statusBarHeight) + 128f
private fun headerStartGoneHeight(statusBarHeight: Float) = headerMinHeight(statusBarHeight) + 76f
private fun headerGoneHeight(statusBarHeight: Float) = headerMinHeight(statusBarHeight) + 18f
private fun spacerHeight(statusBarHeight: Float) =
    headerMaxHeight(statusBarHeight) - appBarHeight - statusBarHeight
