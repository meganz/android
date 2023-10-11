package mega.privacy.android.core.ui.controls.dialogs

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.controls.lists.SettingsItemWithRadioButton
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * Confirmation dialog with radio button
 * @param radioOptions the options to be shown, can be of any type [T],
 * T.toString() will be used by default to set the text button, but can be defined with [optionDescriptionMapper]
 * @param optionDescriptionMapper can be used to map each option to the text that represents it, toString() will be used by default
 *
 */
@Composable
fun <T> ConfirmationDialogWithRadioButtons(
    radioOptions: List<T>?,
    onOptionSelected: (T) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    cancelButtonText: String? = null,
    confirmButtonText: String? = null,
    onConfirmRequest: () -> Unit = {},
    titleText: String = "",
    subTitleText: String = "",
    initialSelectedOption: T? = null,
    optionDescriptionMapper: @Composable (T) -> String = { it.toString() },
    properties: DialogProperties = DialogProperties(),
) {

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        Surface(
            elevation = 24.dp,
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
            ) {
                if (titleText.isNotEmpty()) {
                    Text(
                        modifier = Modifier
                            .padding(top = 20.dp, bottom = 16.dp, start = 24.dp, end = 24.dp),
                        text = titleText,
                        color = MegaTheme.colors.text.primary,
                        style = MaterialTheme.typography.h6,
                        textAlign = TextAlign.Start,
                    )
                }
                if (subTitleText.isNotEmpty()) {
                    Text(
                        modifier = Modifier
                            .padding(bottom = 16.dp, start = 24.dp, end = 24.dp),
                        text = subTitleText,
                        style = MaterialTheme.typography.subtitle1.copy(
                            color = MegaTheme.colors.text.secondary,
                            textAlign = TextAlign.Start,
                        )
                    )
                }

                Column(modifier = modifier.selectableGroup()) {
                    radioOptions?.forEach { item ->
                        SettingsItemWithRadioButton(
                            modifier = modifier.fillMaxWidth(),
                            title = optionDescriptionMapper(item),
                            selected = item == initialSelectedOption,
                            onClick = { onOptionSelected(item) })
                    }
                }

                if (!cancelButtonText.isNullOrEmpty() || !confirmButtonText.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.End),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (!cancelButtonText.isNullOrEmpty()) {
                            TextMegaButton(
                                text = cancelButtonText,
                                onClick = onDismissRequest,
                            )
                        }
                        if (!confirmButtonText.isNullOrEmpty()) {
                            TextMegaButton(
                                text = confirmButtonText,
                                onClick = onConfirmRequest,
                            )
                        }
                    }
                } else {
                    Spacer(modifier = modifier.height(8.dp))
                }
            }
        }
    }
}


@CombinedThemePreviews
@Composable
private fun PreviewConfirmationWithRadioButtonsDialog() {
    val options = listOf("Light", "Dark", "Busy", "System default")
    var selected by remember { mutableStateOf(options[0]) }
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ConfirmationDialogWithRadioButtons(
            titleText = "Dialog title",
            cancelButtonText = "Cancel",
            initialSelectedOption = selected,
            radioOptions = options,
            onOptionSelected = { selected = it },
            onDismissRequest = { }
        )
    }
}


@CombinedThemePreviews
@Composable
private fun PreviewConfirmationWithRadioButtonsDialogWithoutActionButton() {
    val options = listOf(
        "Light",
        "Dark",
        "Busy",
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit",
        "System default"
    )
    var selected by remember { mutableStateOf(options[0]) }
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ConfirmationDialogWithRadioButtons(
            titleText = "Dialog title",
            subTitleText = "Subtitle",
            cancelButtonText = "Cancel",
            confirmButtonText = "OK",
            initialSelectedOption = selected,
            radioOptions = options,
            onOptionSelected = { selected = it },
            onDismissRequest = { }
        )
    }
}