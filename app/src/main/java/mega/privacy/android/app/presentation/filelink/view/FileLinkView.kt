package mega.privacy.android.app.presentation.filelink.view

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.util.shimmerEffect
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.main.ads.AdsContainer
import mega.privacy.android.app.main.dialog.storagestatus.StorageStatusDialogView
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.nav.megaNavigator
import mega.privacy.android.app.presentation.fileinfo.view.FileInfoHeader
import mega.privacy.android.app.presentation.fileinfo.view.PreviewWithShadow
import mega.privacy.android.app.presentation.filelink.model.FileLinkJobInProgressState
import mega.privacy.android.app.presentation.filelink.model.FileLinkState
import mega.privacy.android.app.presentation.folderlink.model.LinkErrorState
import mega.privacy.android.app.presentation.folderlink.view.ExpiredLinkView
import mega.privacy.android.app.presentation.folderlink.view.ImportDownloadView
import mega.privacy.android.app.presentation.folderlink.view.UnavailableLinkView
import mega.privacy.android.app.presentation.transfers.TransferManagementUiState
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.legacy.core.ui.controls.dialogs.LoadingDialog
import mega.privacy.android.shared.original.core.ui.controls.buttons.DebouncedButtonContainer
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.controls.layouts.ScaffoldWithCollapsibleHeader
import mega.privacy.android.shared.original.core.ui.controls.snackbars.MegaSnackbar
import mega.privacy.android.shared.original.core.ui.controls.widgets.TransfersWidgetViewAnimated
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_020_grey_700
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.android.shared.resources.R as sharedR


/**
 * View to render the File Link Screen, including toolbar, content, etc.
 */

internal const val IMPORT_BUTTON_TAG = "file_link_view:button_import"
internal const val SAVE_BUTTON_TAG = "file_link_view:button_save"

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
internal fun FileLinkView(
    viewState: FileLinkState,
    snackBarHostState: SnackbarHostState,
    transferState: TransferManagementUiState,
    onBackPressed: () -> Unit,
    onShareClicked: () -> Unit,
    onPreviewClick: () -> Unit,
    onSaveToDeviceClicked: () -> Unit,
    onImportClicked: () -> Unit,
    onTransferWidgetClick: () -> Unit,
    onErrorMessageConsumed: () -> Unit,
    onOverQuotaErrorConsumed: () -> Unit,
    onForeignNodeErrorConsumed: () -> Unit,
    request: AdManagerAdRequest?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val showQuotaExceededDialog = remember { mutableStateOf<StorageState?>(null) }
    val showForeignNodeErrorDialog = remember { mutableStateOf(false) }

    EventEffect(
        event = viewState.errorMessage,
        onConsumed = onErrorMessageConsumed
    ) {
        snackBarHostState.showAutoDurationSnackbar(context.resources.getString(it))
    }

    EventEffect(event = viewState.overQuotaError, onConsumed = onOverQuotaErrorConsumed) {
        showQuotaExceededDialog.value = it
    }

    EventEffect(event = viewState.foreignNodeError, onConsumed = onForeignNodeErrorConsumed) {
        showForeignNodeErrorDialog.value = true
    }

    ScaffoldWithCollapsibleHeader(
        topBar = {
            FileLinkTopBar(
                title = viewState.title,
                shouldShowMenuActions = viewState.showContentActions,
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
        bottomBar = {
            Column {
                if (viewState.showContentActions) {
                    ImportDownloadView(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(MaterialTheme.colors.grey_020_grey_700),
                        hasDbCredentials = viewState.hasDbCredentials,
                        onImportClicked = onImportClicked,
                        onSaveToDeviceClicked = onSaveToDeviceClicked
                    )
                }
                AdsContainer(
                    request = request,
                    isLoggedInUser = viewState.hasDbCredentials,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        floatingActionButton = {
            if (!transferState.hideTransfersWidget) {
                TransfersWidgetViewAnimated(
                    transfersInfo = transferState.transfersInfo,
                    onClick = onTransferWidgetClick,
                )
            }
        },
        headerSpacerHeight = if (viewState.errorState == LinkErrorState.NoError && !viewState.isLoading) {
            if (viewState.iconResource != null) (MAX_HEADER_HEIGHT + APP_BAR_HEIGHT).dp else MAX_HEADER_HEIGHT.dp
        } else {
            0.dp
        },
        modifier = modifier,
    ) {
        val screenHeight = (LocalConfiguration.current.screenHeightDp - (APP_BAR_HEIGHT * 2)).dp
        when {
            viewState.isLoading -> {
                FileLinksLoadingView()
            }

            viewState.errorState == LinkErrorState.Expired -> {
                ExpiredLinkView(
                    title = sharedR.string.file_link_expired_title,
                    modifier = Modifier.height(screenHeight)
                )
            }

            viewState.errorState == LinkErrorState.Unavailable -> {
                UnavailableLinkView(
                    title = sharedR.string.file_link_unavailable_title,
                    subtitle = sharedR.string.general_link_unavailable_subtitle,
                    bulletPoints = listOf(
                        sharedR.string.file_link_unavailable_deleted,
                        sharedR.string.file_link_unavailable_disabled,
                        sharedR.string.general_link_unavailable_invalid_url,
                        R.string.file_link_unavaible_ToS_violation
                    ),
                    modifier = Modifier.height(screenHeight)
                )
            }

            else -> FileLinkContent(
                viewState = viewState,
                onPreviewClick = onPreviewClick,
            )
        }
    }

    viewState.jobInProgressState?.takeIf {
        it != FileLinkJobInProgressState.InitialLoading
    }?.progressMessage?.let { message ->
        LoadingDialog(text = stringResource(id = message))
    }

    showQuotaExceededDialog.value?.let {
        StorageStatusDialogView(
            modifier = Modifier.padding(horizontal = 24.dp),
            usePlatformDefaultWidth = false,
            storageState = it,
            preWarning = it != StorageState.Red,
            overQuotaAlert = true,
            onUpgradeClick = {
                context.megaNavigator.openUpgradeAccount(
                    context = context,
                )
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
internal fun FileLinksLoadingView() {
    Column {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 72.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .shimmerEffect()
        )

        Spacer(Modifier.height(28.dp))

        Spacer(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .shimmerEffect()
                .align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(48.dp))

        Spacer(
            modifier = Modifier
                .padding(horizontal = 72.dp)
                .width(66.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .shimmerEffect()
        )

        Spacer(Modifier.height(14.dp))

        Spacer(
            modifier = Modifier
                .padding(horizontal = 72.dp)
                .width(166.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .shimmerEffect()
        )

        Spacer(Modifier.height(50.dp))

        Spacer(
            modifier = Modifier
                .padding(horizontal = 72.dp)
                .width(80.dp)
                .height(45.dp)
                .clip(RoundedCornerShape(6.dp))
                .shimmerEffect()
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
        DebouncedButtonContainer(onSaveToDeviceClicked) { isClickAllowed, debouncedOnClick ->
            TextMegaButton(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .testTag(SAVE_BUTTON_TAG),
                textId = R.string.general_save_to_device,
                onClick = debouncedOnClick,
                enabled = isClickAllowed,
            )
        }
        if (hasDbCredentials) {
            TextMegaButton(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .testTag(IMPORT_BUTTON_TAG),
                textId = R.string.add_to_cloud,
                onClick = onImportClicked,
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewFileLinksLoadingView() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        FileLinksLoadingView()
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewImportDownloadView() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
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
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        val viewState =
            FileLinkState(hasDbCredentials = true, title = "Title", sizeInBytes = 10000L)
        FileLinkView(
            viewState = viewState,
            snackBarHostState = remember { SnackbarHostState() },
            transferState = TransferManagementUiState(),
            onBackPressed = {},
            onShareClicked = {},
            onPreviewClick = {},
            onSaveToDeviceClicked = {},
            onImportClicked = {},
            onTransferWidgetClick = {},
            onErrorMessageConsumed = {},
            onOverQuotaErrorConsumed = {},
            onForeignNodeErrorConsumed = {},
            request = null
        )
    }
}

private const val MAX_HEADER_HEIGHT = 96
private const val APP_BAR_HEIGHT = 56
internal const val DEBOUNCE_ACTION_MILLISECONDS = 800
