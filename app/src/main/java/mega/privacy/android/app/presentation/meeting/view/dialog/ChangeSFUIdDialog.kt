package mega.privacy.android.app.presentation.meeting.view.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.ChangeSFUIdViewModel
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.controls.textfields.GenericTextField
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

internal const val SFU_TITLE_TAG = "change_sfu_id_dialog:title"
internal const val SFU_SUBTITLE_TAG = "change_sfu_id_dialog:subtitle"
internal const val SFU_TEXT_FIELD_TAG = "change_sfu_id_dialog:text_field"

@Composable
internal fun ChangeSFUIdDialog(
    modifier: Modifier = Modifier,
    viewModel: ChangeSFUIdViewModel = hiltViewModel(),
    onDismiss: () -> Unit = {},
) {
    ChangeSFUIdDialog(
        modifier = modifier,
        onChange = viewModel::changeSFUId,
        onDismiss = onDismiss
    )
}

@Composable
internal fun ChangeSFUIdDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    onChange: (Int) -> Unit,
) {
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = "",
            )
        )
    }
    MegaAlertDialog(
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MegaText(
                    modifier = Modifier
                        .padding(
                            top = 20.dp, bottom = 16.dp, start = 24.dp, end = 24.dp
                        )
                        .testTag(SFU_TITLE_TAG),
                    text = stringResource(R.string.meetings_change_sfu_dialog_title),
                    textColor = TextColor.Primary,
                    style = MaterialTheme.typography.h6,
                )
                MegaText(
                    modifier = Modifier
                        .padding(bottom = 16.dp, start = 24.dp, end = 24.dp)
                        .testTag(SFU_SUBTITLE_TAG),
                    text = stringResource(R.string.meetings_change_sfu_dialog_subtitle),
                    textColor = TextColor.Secondary,
                    style = MaterialTheme.typography.subtitle1,
                )

                GenericTextField(
                    modifier = Modifier
                        .padding(bottom = 16.dp, start = 24.dp, end = 24.dp)
                        .testTag(SFU_TEXT_FIELD_TAG),
                    placeholder = stringResource(R.string.meetings_change_sfu_dialog_hint),
                    onTextChange = { textFieldValue = it },
                    imeAction = ImeAction.Default,
                    keyboardActions = KeyboardActions(),
                    textFieldValue = textFieldValue,
                )
            }
        },
        confirmButtonText = stringResource(R.string.meetings_change_sfu_dialog_action_button),
        cancelButtonText = stringResource(R.string.general_cancel),
        onConfirm = {
            val value = runCatching { textFieldValue.text.toInt() }.getOrNull()
            value?.let(onChange)
            onDismiss()
        },
        onDismiss = onDismiss,
        modifier = modifier,
        dismissOnClickOutside = true,
        dismissOnBackPress = true,
    )
}

@CombinedThemePreviews
@Composable
private fun ChangeSFUIdDialogPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ChangeSFUIdDialog(onChange = {}, onDismiss = {})
    }
}
