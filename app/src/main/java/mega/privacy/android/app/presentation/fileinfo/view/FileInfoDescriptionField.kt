package mega.privacy.android.app.presentation.fileinfo.view

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.controls.textfields.GenericDescriptionTextField
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.android.shared.resources.R
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.mobile.analytics.event.NodeInfoDescriptionConfirmedEvent
import mega.privacy.mobile.analytics.event.NodeInfoDescriptionEnteredEvent

/**
 * TextField Generic Description
 *
 * @param descriptionText       Description text value
 * @param labelId               Label string resource Id
 * @param placeholderId         Placeholder string resource Id
 * @param descriptionLimit      Description text character limit
 * @param isEditable            If user can change the description
 * @param onConfirmDescription  Description is confirmed by keyboard interaction
 */
@Composable
fun FileInfoDescriptionField(
    modifier: Modifier = Modifier,
    descriptionText: String,
    @StringRes labelId: Int? = null,
    @StringRes placeholderId: Int? = null,
    descriptionLimit: Int = DESCRIPTION_LIMIT,
    isEditable: Boolean = true,
    onConfirmDescription: (String) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf(descriptionText) }

    LaunchedEffect(descriptionText) {
        description = descriptionText
    }

    Column(modifier = modifier) {
        labelId?.let {
            MegaText(
                text = stringResource(id = labelId),
                textColor = if (!isFocused) TextColor.Secondary else TextColor.Accent,
                style = MaterialTheme.typography.caption,
            )
        }

        GenericDescriptionTextField(
            modifier = Modifier
                .onFocusChanged {
                    if (isFocused != it.isFocused) {
                        isFocused = it.isFocused
                    }
                },
            value = description,
            charLimit = descriptionLimit,
            initiallyFocused = false,
            isEnabled = isEditable,
            maxLines = 5,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = {
                    onConfirmDescription(description)
                    Analytics.tracker.trackEvent(NodeInfoDescriptionConfirmedEvent)
                    focusManager.clearFocus()
                }
            ),
            showUnderline = true,
            placeholderId = placeholderId,
            onValueChange = {
                description = it.take(descriptionLimit)
            },
        )

        if (isFocused) {
            Analytics.tracker.trackEvent(NodeInfoDescriptionEnteredEvent)
            MegaText(
                modifier = Modifier.align(Alignment.End),
                text = "${description.length}/$descriptionLimit",
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.caption,
            )
        }
    }
}

private const val DESCRIPTION_LIMIT = 300

@CombinedTextAndThemePreviews
@Composable
private fun FileInfoDescriptionFieldPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        FileInfoDescriptionField(
            descriptionText = "This is a description",
            labelId = R.string.file_info_information_description_label,
            placeholderId = R.string.file_info_information_description_placeholder,
        )
    }
}
