package mega.privacy.android.app.presentation.filecontact.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
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
    MegaModalBottomSheet(
        sheetState = sheetState,
        bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
        onDismissRequest = onDismissSheet,
        modifier = modifier,
    ) {
        Spacer(Modifier.Companion.height(24.dp))
        MegaText(
            modifier = Modifier.Companion.padding(16.dp),
            text = stringResource(R.string.file_properties_shared_folder_permissions),
            textColor = TextColor.Primary,
            style = TextStyle(fontWeight = FontWeight.Companion.Bold)
        )
        Spacer(Modifier.Companion.height(24.dp))
        MegaText(
            modifier = Modifier.Companion
                .clickable {
                    shareWithPermission(AccessPermission.READ)
                    coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            onDismissSheet()
                        }
                    }
                }
                .padding(16.dp),
            text = stringResource(R.string.file_properties_shared_folder_read_only),
            textColor = TextColor.Primary
        )
        Spacer(Modifier.Companion.height(24.dp))
        MegaText(
            modifier = Modifier.Companion
                .clickable {
                    shareWithPermission(AccessPermission.READWRITE)
                    coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            onDismissSheet()
                        }
                    }
                }
                .padding(16.dp),
            text = stringResource(R.string.file_properties_shared_folder_read_write),
            textColor = TextColor.Primary
        )
        Spacer(Modifier.Companion.height(24.dp))
        MegaText(
            modifier = Modifier.Companion
                .clickable {
                    shareWithPermission(AccessPermission.FULL)
                    coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            onDismissSheet()
                        }
                    }
                }
                .padding(16.dp),
            text = stringResource(R.string.file_properties_shared_folder_full_access),
            textColor = TextColor.Primary
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