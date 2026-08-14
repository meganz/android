package mega.privacy.android.app.presentation.filecontact.view

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.list.OneLineListItem
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.shares.AccessPermission

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun SetNewSharePermissionBottomSheet(
    onDismissSheet: () -> Unit,
    shareWithPermission: (AccessPermission) -> Unit,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    val onPermissionClick = { permission: AccessPermission ->
        shareWithPermission(permission)
        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onDismissSheet()
            }
        }
    }

    MegaModalBottomSheet(
        sheetState = sheetState,
        bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
        onDismissRequest = onDismissSheet,
        modifier = modifier,
    ) {
        MegaText(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            text = stringResource(R.string.file_properties_shared_folder_permissions),
            textColor = TextColor.Primary,
            style = AppTheme.typography.titleMedium,
        )
        OneLineListItem(
            text = stringResource(R.string.file_properties_shared_folder_read_only),
            onClickListener = { onPermissionClick(AccessPermission.READ) },
        )
        OneLineListItem(
            text = stringResource(R.string.file_properties_shared_folder_read_write),
            onClickListener = { onPermissionClick(AccessPermission.READWRITE) },
        )
        OneLineListItem(
            text = stringResource(R.string.file_properties_shared_folder_full_access),
            onClickListener = { onPermissionClick(AccessPermission.FULL) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@CombinedThemePreviews
@Composable
private fun SetNewSharePermissionBottomSheetPreview() {
    AndroidThemeForPreviews {
        val sheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Expanded
        )
        SetNewSharePermissionBottomSheet(
            onDismissSheet = { },
            shareWithPermission = { },
            coroutineScope = rememberCoroutineScope(),
            sheetState = sheetState,
        )
    }
}
