package mega.privacy.android.app.presentation.meeting.view.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.controls.textfields.GenericTextField
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

@Composable
internal fun PasteMeetingLinkGuestDialog(
    meetingLink: String,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = onCancel,
    errorText: String? = null,
) {
    val focusRequester = remember { FocusRequester() }

    MegaAlertDialog(
        modifier = modifier,
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                MegaText(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.paste_meeting_link_guest_dialog_title),
                    textColor = TextColor.Primary,
                    style = MaterialTheme.typography.h6,
                )

                MegaText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    text = stringResource(R.string.paste_meeting_link_guest_instruction),
                    textColor = TextColor.Secondary,
                    style = MaterialTheme.typography.subtitle1,
                )

                GenericTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 30.dp)
                        .focusRequester(focusRequester),
                    text = meetingLink,
                    errorText = errorText,
                    placeholder = stringResource(id = R.string.meeting_link),
                    onTextChange = onTextChange
                )
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        },
        confirmButtonText = stringResource(id = R.string.general_ok),
        cancelButtonText = stringResource(id = R.string.general_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        onCancel = onCancel,
        dismissOnClickOutside = false
    )
}

@CombinedTextAndThemePreviews
@Composable
private fun PasteMeetingLinkGuestDialogWithoutErrorPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        PasteMeetingLinkGuestDialog(
            meetingLink = "",
            onTextChange = {},
            onConfirm = {},
            onCancel = {},
            onDismiss = {}
        )
    }
}

@CombinedTextAndThemePreviews
@Composable
private fun PasteMeetingLinkGuestDialogWithErrorPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        PasteMeetingLinkGuestDialog(
            meetingLink = "",
            errorText = "error",
            onTextChange = {},
            onConfirm = {},
            onCancel = {},
            onDismiss = {}
        )
    }
}

internal const val ACTION_JOIN_AS_GUEST = "action_join_as_guest"
