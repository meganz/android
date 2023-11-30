package mega.privacy.android.app.presentation.qrcode

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.legacy.core.ui.controls.lists.MenuActionHeader
import mega.privacy.android.legacy.core.ui.controls.lists.MenuActionListTile

/**
 * QR code save bottom sheet
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun QRCodeSaveBottomSheetView(
    modalSheetState: ModalBottomSheetState,
    coroutineScope: CoroutineScope,
    onCloudDriveClicked: () -> Unit,
    onFileSystemClicked: () -> Unit,
) {
    BottomSheet(
        modalSheetState = modalSheetState,
        scrimColor = black.copy(alpha = 0.32f),
        sheetBody = {
            BottomSheetContent(
                onCloudDriveClicked = {
                    coroutineScope.launch { modalSheetState.hide() }
                    onCloudDriveClicked()
                },
                onFileSystemClicked = {
                    coroutineScope.launch { modalSheetState.hide() }
                    onFileSystemClicked()
                }
            )
        }
    )
}

@Composable
private fun BottomSheetContent(
    onCloudDriveClicked: () -> Unit,
    onFileSystemClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        MenuActionHeader(text = stringResource(id = R.string.save_action))
        MenuActionListTile(
            text = stringResource(id = R.string.save_cloud_drive),
            icon = painterResource(id = R.drawable.ic_pick_cloud_drive),
            addSeparator = true,
            onActionClicked = onCloudDriveClicked
        )
        MenuActionListTile(
            text = stringResource(id = R.string.save_file_system),
            icon = painterResource(id = R.drawable.ic_save_to_file_system),
            addSeparator = false,
            onActionClicked = onFileSystemClicked
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewBottomSheetContent() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        BottomSheetContent(
            onCloudDriveClicked = { },
            onFileSystemClicked = { }
        )
    }
}
