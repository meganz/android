package mega.privacy.android.app.presentation.filelink.view

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.R
import mega.privacy.android.app.components.transferWidget.TransfersWidgetView
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.main.dialog.storagestatus.StorageStatusDialogView
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.presentation.advertisements.model.AdsUIState
import mega.privacy.android.app.presentation.advertisements.view.AdsBannerView
import mega.privacy.android.app.presentation.extensions.errorDialogContentId
import mega.privacy.android.app.presentation.extensions.errorDialogTitleId
import mega.privacy.android.app.presentation.fileinfo.view.FileInfoHeader
import mega.privacy.android.app.presentation.fileinfo.view.PreviewWithShadow
import mega.privacy.android.app.presentation.filelink.model.FileLinkState
import mega.privacy.android.app.presentation.transfers.TransferManagementUiState
import mega.privacy.android.app.upgradeAccount.UpgradeAccountActivity
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.controls.layouts.ScaffoldWithCollapsibleHeader
import mega.privacy.android.core.ui.controls.snackbars.MegaSnackbar
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_020_grey_700
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.legacy.core.ui.controls.dialogs.LoadingDialog

/**
 * View to render the File Link Screen, including toolbar, content, etc.
 */

internal const val IMPORT_BUTTON_TAG = "file_link_view:button_import"
internal const val SAVE_BUTTON_TAG = "file_link_view:button_save"

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
    onConfirmErrorDialogClick: () -> Unit,
    onErrorMessageConsumed: () -> Unit,
    onOverQuotaErrorConsumed: () -> Unit,
    onForeignNodeErrorConsumed: () -> Unit,
    adsUiState: AdsUIState,
    onAdClicked: (uri: Uri?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val snackBarHostState = remember { SnackbarHostState() }
    val showQuotaExceededDialog = remember { mutableStateOf<StorageState?>(null) }
    val showForeignNodeErrorDialog = remember { mutableStateOf(false) }
    val isAdDismissed = remember { mutableStateOf(false) }

    EventEffect(
        event = viewState.errorMessage,
        onConsumed = onErrorMessageConsumed
    ) {
        snackBarHostState.showSnackbar(context.resources.getString(it))
    }

    EventEffect(event = viewState.overQuotaError, onConsumed = onOverQuotaErrorConsumed) {
        showQuotaExceededDialog.value = it
    }

    EventEffect(event = viewState.foreignNodeError, onConsumed = onForeignNodeErrorConsumed) {
        showForeignNodeErrorDialog.value = true
    }

    Box(modifier = modifier.navigationBarsPadding().fillMaxSize()) {
        ScaffoldWithCollapsibleHeader(
            topBar = {
                FileLinkTopBar(
                    title = viewState.title,
                    onBackPressed = onBackPressed,
                    onShareClicked = onShareClicked,
                )
            },
            header = {
                FileInfoHeader(
                    title = viewState.title,
                    iconResource = viewState.iconResource,
                    accessPermissionDescription = null,
                )
            },
            headerIncludingSystemBar = viewState.previewPath
                ?.let { previewUri ->
                    {
                        PreviewWithShadow(
                            previewUri = previewUri,
                        )
                    }
                },
            snackbarHost = {
                SnackbarHost(hostState = snackBarHostState) { data ->
                    MegaSnackbar(snackbarData = data)
                }
            },
        ) {
            FileLinkContent(
                viewState = viewState,
                onPreviewClick = onPreviewClick,
            )
        }

        Column(modifier = Modifier.align(Alignment.BottomEnd)) {
            ImportDownloadView(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(MaterialTheme.colors.grey_020_grey_700),
                hasDbCredentials = viewState.hasDbCredentials,
                onImportClicked = onImportClicked,
                onSaveToDeviceClicked = onSaveToDeviceClicked
            )
            if (isAdDismissed.value.not()) {
                AdsBannerView(
                    uiState = adsUiState,
                    onAdClicked = onAdClicked,
                    onAdsWebpageLoaded = {},
                    onAdDismissed = { isAdDismissed.value = true }
                )
            }
        }

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
    viewState.jobInProgressState?.progressMessage?.let {
        LoadingDialog(text = stringResource(id = it))
    }

    viewState.fetchPublicNodeError?.let {
        MegaAlertDialog(
            title = stringResource(id = it.errorDialogTitleId),
            text = stringResource(id = it.errorDialogContentId),
            confirmButtonText = stringResource(id = android.R.string.ok),
            cancelButtonText = null,
            onConfirm = onConfirmErrorDialogClick,
            onDismiss = {},
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    }

    showQuotaExceededDialog.value?.let {
        StorageStatusDialogView(
            modifier = Modifier.padding(horizontal = 24.dp),
            usePlatformDefaultWidth = false,
            storageState = it,
            preWarning = it != StorageState.Red,
            overQuotaAlert = true,
            onUpgradeClick = {
                context.startActivity(Intent(context, UpgradeAccountActivity::class.java))
            },
            onCustomizedPlanClick = { email, accountType ->
                AlertsAndWarnings.askForCustomizedPlan(context, email, accountType)
            },
            onAchievementsClick = {
                context.startActivity(
                    Intent(context, MyAccountActivity::class.java)
                        .setAction(IntentConstants.ACTION_OPEN_ACHIEVEMENTS)
                )
            },
            onClose = { showQuotaExceededDialog.value = null }
        )
    }

    if (showForeignNodeErrorDialog.value) {
        MegaAlertDialog(
            text = stringResource(id = R.string.warning_share_owner_storage_quota),
            confirmButtonText = stringResource(id = R.string.general_ok),
            cancelButtonText = null,
            onConfirm = { showForeignNodeErrorDialog.value = false },
            onDismiss = {},
            dismissOnClickOutside = false
        )
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
            onTransferWidgetClick = {},
            onConfirmErrorDialogClick = {},
            onErrorMessageConsumed = {},
            onOverQuotaErrorConsumed = {},
            onForeignNodeErrorConsumed = {},
            adsUiState = AdsUIState(),
            onAdClicked = {},
        )
    }
}

internal const val animationDuration = 300
internal const val animationScale = 0.2f
internal val animationSpecs = TweenSpec<Float>(durationMillis = animationDuration)
