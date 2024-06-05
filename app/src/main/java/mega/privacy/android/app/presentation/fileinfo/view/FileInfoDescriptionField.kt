package mega.privacy.android.app.presentation.fileinfo.view

import mega.privacy.android.shared.resources.R as sharedR
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.controls.textfields.GenericDescriptionTextField
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.mobile.analytics.event.NodeInfoDescriptionCharacterLimitEvent
import mega.privacy.mobile.analytics.event.NodeInfoDescriptionConfirmedEvent
import mega.privacy.mobile.analytics.event.NodeInfoDescriptionEnteredEvent

/**
 * TextField Generic Description
 *
 * @param descriptionText       Description text value
 * @param labelId               Label string resource Id
 * @param placeholder           Placeholder string
 * @param descriptionLimit      Description text character limit
 * @param isEditable            If user can change the description
 * @param onConfirmDescription  Description is confirmed by keyboard interaction
 */
@Composable
fun FileInfoDescriptionField(
    descriptionText: String,
    modifier: Modifier = Modifier,
    @StringRes labelId: Int? = null,
    placeholder: String? = null,
    descriptionLimit: Int = DESCRIPTION_LIMIT,
    isEditable: Boolean = true,
    onConfirmDescription: (String) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    var isFocused by rememberSaveable { mutableStateOf(false) }
    var description by rememberSaveable { mutableStateOf(descriptionText) }

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
            placeholder = placeholder,
            onValueChange = {
                if (it.length > descriptionLimit) {
                    Analytics.tracker.trackEvent(NodeInfoDescriptionCharacterLimitEvent)
                }
                val isOverLimit =
                    description.length >= descriptionLimit && it.length >= description.length
                if (!isOverLimit) {
                    description = it.take(descriptionLimit)
                }
            },
        )

        if (!isEditable) {
            MegaText(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.Start),
                text = stringResource(id = R.string.file_properties_shared_folder_read_only),
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.caption,
            )
        } else {
            if (isFocused) {
                Analytics.tracker.trackEvent(NodeInfoDescriptionEnteredEvent)
                MegaText(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .align(Alignment.End),
                    text = "${description.length}/$descriptionLimit",
                    textColor = TextColor.Primary,
                    style = MaterialTheme.typography.caption,
                )
            }
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
            labelId = sharedR.string.file_info_information_description_label,
            placeholder = stringResource(id = sharedR.string.file_info_information_description_placeholder),
        )
    }
}
