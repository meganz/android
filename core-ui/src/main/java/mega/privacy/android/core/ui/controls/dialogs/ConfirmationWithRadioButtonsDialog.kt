package mega.privacy.android.core.ui.controls.dialogs

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme

/**
 * Confirmation dialog with radio button
 * @param radioOptions the options to be shown, can be of any type [T],
 * T.toString() will be used by default to set the text button, but can be defined with [optionDescriptionMapper]
 * @param optionDescriptionMapper can be used to map each option to the text that represents it, toString() will be used by default
 *
 */
@Composable
fun <T> ConfirmationWithRadioButtonsDialog(
    modifier: Modifier = Modifier,
    radioOptions: List<T>?,
    onOptionSelected: (T) -> Unit,
    onDismissRequest: () -> Unit,
    buttonText: String? = null,
    titleText: String = "",
    initialSelectedOption: T? = null,
    optionDescriptionMapper: @Composable (T) -> String = { it.toString() },
    properties: DialogProperties = DialogProperties(),
) {

    val radioButtonColors = RadioButtonDefaults.colors(
        selectedColor = MaterialTheme.colors.secondary,
        unselectedColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        disabledColor = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
    )

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
                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 16.dp, start = 24.dp, end = 24.dp)
                ) {
                    if (titleText.isNotEmpty()) {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.h6.copy(
                                color = MaterialTheme.colors.onPrimary,
                                textAlign = TextAlign.Start,
                            )
                        )
                    }
                }

                Column(modifier = modifier.selectableGroup()) {
                    radioOptions?.forEach { item ->
                        Row(
                            modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .selectable(
                                    selected = (item == initialSelectedOption),
                                    onClick = {
                                        onOptionSelected(item)
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (item == initialSelectedOption),
                                colors = radioButtonColors,
                                onClick = { onOptionSelected(item) }
                            )
                            Text(
                                modifier = modifier
                                    .padding(start = 20.dp),
                                text = optionDescriptionMapper(item),
                                style = MaterialTheme.typography.subtitle1.copy(
                                    color = MaterialTheme.colors.onPrimary,
                                    textAlign = TextAlign.Start
                                )
                            )
                        }
                    }
                }

                if (!buttonText.isNullOrEmpty()) {
                    TextButton(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.End),
                        onClick = onDismissRequest,
                    ) {
                        Text(
                            text = buttonText,
                            style = MaterialTheme.typography.button.copy(
                                color = MaterialTheme.colors.secondary,
                                textAlign = TextAlign.End
                            )
                        )
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
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ConfirmationWithRadioButtonsDialog(
            titleText = "Dialog title",
            buttonText = "Cancel",
            initialSelectedOption = "Light",
            radioOptions = listOf("Light", "Dark", "Busy", "System default"),
            onOptionSelected = { },
            onDismissRequest = { }
        )
    }
}


@CombinedThemePreviews
@Composable
private fun PreviewConfirmationWithRadioButtonsDialogWithoutActionButton() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ConfirmationWithRadioButtonsDialog(
            titleText = "Dialog title",
            initialSelectedOption = "Light",
            radioOptions = listOf("Light", "Dark", "Busy", "System default"),
            onOptionSelected = { },
            onDismissRequest = { }
        )
    }
}