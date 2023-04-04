package mega.privacy.android.app.presentation.testpassword.view

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.testpassword.view.Constants.BOTTOM_SHEET_COPY
import mega.privacy.android.app.presentation.testpassword.view.Constants.BOTTOM_SHEET_PRINT
import mega.privacy.android.app.presentation.testpassword.view.Constants.BOTTOM_SHEET_SAVE
import mega.privacy.android.app.presentation.testpassword.view.Constants.BOTTOM_SHEET_TITLE
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.extensions.grey_012_white_012
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun RecoveryKeyBottomSheet(
    modalSheetState: ModalBottomSheetState,
    onPrint: () -> Unit,
    onCopy: () -> Unit,
    onSave: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheetLayout(
        sheetState = modalSheetState,
        scrimColor = black.copy(alpha = 0.32f),
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, start = 18.dp, end = 18.dp)
                    .wrapContentHeight()
            ) {
                Text(
                    modifier = Modifier
                        .testTag(BOTTOM_SHEET_TITLE)
                        .padding(start = 5.dp),
                    text = stringResource(id = R.string.recovery_key_bottom_sheet),
                    color = MaterialTheme.colors.textColorSecondary,
                    style = MaterialTheme.typography.subtitle1.copy(letterSpacing = 0.03125.sp)
                )
                MenuItem(
                    modifier = Modifier
                        .testTag(BOTTOM_SHEET_PRINT)
                        .padding(top = 12.dp),
                    res = R.drawable.ic_print,
                    text = R.string.context_option_print,
                    description = "Print",
                    onClick = {
                        onPrint()
                        coroutineScope.launch { modalSheetState.hide() }
                    }
                )
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 72.dp),
                    color = MaterialTheme.colors.grey_012_white_012,
                    thickness = 1.dp
                )
                MenuItem(
                    modifier = Modifier.testTag(BOTTOM_SHEET_COPY),
                    res = R.drawable.ic_clipboard,
                    text = R.string.option_copy_to_clipboard,
                    description = "Copy",
                    onClick = {
                        onCopy()
                        coroutineScope.launch { modalSheetState.hide() }
                    }
                )
                MenuItem(
                    modifier = Modifier.testTag(BOTTOM_SHEET_SAVE),
                    res = R.drawable.ic_pick_file_system,
                    text = R.string.option_save_on_filesystem,
                    description = "Save",
                    onClick = {
                        onSave()
                        coroutineScope.launch { modalSheetState.hide() }
                    }
                )
            }
        }
    ) {}
}

@Composable
private fun MenuItem(
    modifier: Modifier,
    @DrawableRes res: Int,
    @StringRes text: Int,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clickable(onClick = onClick)
    ) {
        Icon(
            modifier = Modifier
                .size(36.dp)
                .padding(start = 5.dp)
                .align(Alignment.CenterVertically),
            painter = painterResource(id = res),
            contentDescription = description,
            tint = MaterialTheme.colors.textColorSecondary
        )
        Text(
            modifier = Modifier
                .padding(horizontal = 36.dp, vertical = 2.dp)
                .align(Alignment.CenterVertically),
            text = stringResource(id = text),
            style = MaterialTheme.typography.subtitle1
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkMode")
@Composable
private fun PreviewRecoveryKeyBottomSheet() {
    RecoveryKeyBottomSheet(
        modalSheetState = rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Expanded,
            skipHalfExpanded = true
        ),
        onPrint = {},
        onCopy = {},
        onSave = {}
    )
}