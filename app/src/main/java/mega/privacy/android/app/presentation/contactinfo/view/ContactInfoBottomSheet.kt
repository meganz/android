package mega.privacy.android.app.presentation.contactinfo.view

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.foundation.layout.Column
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.legacy.core.ui.controls.lists.MenuActionHeader

/**
 * Contact info bottom sheet
 *
 * @param modalSheetState
 * @param coroutineScope
 * @param updateNickName
 * @param updateNickNameDialogVisibility
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun ContactInfoBottomSheet(
    modalSheetState: ModalBottomSheetState,
    coroutineScope: CoroutineScope,
    updateNickName: (String?) -> Unit,
    updateNickNameDialogVisibility: (Boolean) -> Unit
) {
    BottomSheet(
        modalSheetState = modalSheetState,
        sheetHeader = { MenuActionHeader(text = stringResource(id = R.string.nickname_title)) },
        sheetBody = {
            Column {
                MenuActionListTile(
                    text = stringResource(id = R.string.title_edit_profile_info),
                    icon = painterResource(id = R.drawable.ic_pen_2_medium_regular_outline),
                    onActionClicked = {
                        coroutineScope.launch { modalSheetState.hide() }
                        updateNickNameDialogVisibility(true)
                    }
                )
                MenuActionListTile(
                    text = stringResource(id = R.string.general_remove),
                    icon = painterResource(id = iconPackR.drawable.ic_x_medium_regular_outline),
                    dividerType = null,
                    onActionClicked = {
                        coroutineScope.launch { modalSheetState.hide() }
                        updateNickName(null)
                    }
                )
            }
        },
    )
}
