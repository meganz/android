package mega.privacy.android.shared.original.core.ui.controls.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.controls.dialogs.internal.AlertDialogFlowRow
import mega.privacy.android.shared.original.core.ui.controls.lists.SettingsItemWithRadioButton
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalThemeForPreviews

/**
 * Confirmation dialog with radio button
 * @param radioOptions the options to be shown, can be of any type [T],
 * T.toString() will be used by default to set the text button, but can be defined with [optionDescriptionMapper]
 * @param optionDescriptionMapper can be used to map each option to the text that represents it, toString() will be used by default
 *
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T> ConfirmationDialogWithRadioButtons(
    radioOptions: List<T>?,
    onOptionSelected: (T) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    cancelButtonText: String? = null,
    confirmButtonText: String? = null,
    isConfirmButtonEnable: (() -> Boolean)? = null,
    onConfirmRequest: (T) -> Unit = {},
    titleText: String = "",
    subTitleText: String = "",
    initialSelectedOption: T? = null,
    optionDescriptionMapper: @Composable (T) -> String = { it.toString() },
    properties: DialogProperties = DialogProperties(),
) {
    var selectedOption by rememberSaveable(initialSelectedOption) {
        mutableStateOf(
            initialSelectedOption
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        Surface(
            modifier = Modifier.semantics { testTagsAsResourceId = true },
            elevation = 24.dp,
            color = DSTokens.colors.background.surface2,
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
            ) {
                if (titleText.isNotEmpty()) {
                    MegaText(
                        modifier = Modifier
                            .padding(top = 20.dp, bottom = 16.dp, start = 24.dp, end = 24.dp)
                            .testTag(CONFIRMATION_DIALOG_TITLE_TAG),
                        text = titleText,
                        textColor = TextColor.Primary,
                        style = MaterialTheme.typography.h6,
                    )
                }
                if (subTitleText.isNotEmpty()) {
                    MegaText(
                        modifier = Modifier
                            .padding(bottom = 16.dp, start = 24.dp, end = 24.dp)
                            .testTag(CONFIRMATION_DIALOG_SUBTITLE_TAG),
                        text = subTitleText,
                        textColor = TextColor.Secondary,
                        style = MaterialTheme.typography.subtitle1,
                    )
                }

                val optionsScrollState = rememberScrollState()
                val showScrollSeparatorTop by remember {
                    derivedStateOf {
                        optionsScrollState.value > 0
                    }
                }
                val showScrollSeparatorBottom by remember {
                    derivedStateOf {
                        optionsScrollState.value < optionsScrollState.maxValue
                    }
                }
                if (showScrollSeparatorTop) {
                    ScrollSeparator()
                }
                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .weight(1f, fill = false)
                        .verticalScroll(state = optionsScrollState)
                ) {
                    radioOptions?.forEach { item ->
                        SettingsItemWithRadioButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("${CONFIRMATION_DIALOG_OPTIONS_TAG}_$item"),
                            title = optionDescriptionMapper(item),
                            selected = item == selectedOption,
                            onClick = {
                                selectedOption = item
                                onOptionSelected(item)
                            }
                        )
                    }
                }
                if (showScrollSeparatorBottom) {
                    ScrollSeparator()
                }


                if (!cancelButtonText.isNullOrEmpty() || !confirmButtonText.isNullOrEmpty()) {
                    AlertDialogFlowRow {
                        if (!cancelButtonText.isNullOrEmpty()) {
                            TextMegaButton(
                                modifier = Modifier.testTag(CONFIRMATION_DIALOG_CANCEL_BUTTON_TAG),
                                text = cancelButtonText,
                                onClick = onDismissRequest,
                            )
                        }
                        if (!confirmButtonText.isNullOrEmpty()) {
                            TextMegaButton(
                                modifier = Modifier.testTag(CONFIRMATION_DIALOG_CONFIRM_BUTTON_TAG),
                                enabled = isConfirmButtonEnable?.invoke()
                                    ?: (selectedOption != null),
                                text = confirmButtonText,
                                onClick = { selectedOption?.let(onConfirmRequest) },
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ScrollSeparator() = Spacer(
    Modifier
        .height(1.dp)
        .fillMaxWidth()
        .background(DSTokens.colors.border.strong)
)


@CombinedThemePreviews
@Composable
private fun ConfirmationDialogWithRadioButtonsPreview() {
    val options = listOf("Light", "Dark", "Busy", "System default")
    var selected by remember { mutableStateOf(options[0]) }
    OriginalThemeForPreviews {
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
private fun ConfirmationDialogWithRadioButtonsWithCancelPreview() {
    val options = listOf(
        "Light",
        "Dark",
        "Busy",
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit",
        "System default",
    )
    var selected by remember { mutableStateOf(options[0]) }
    OriginalThemeForPreviews {
        ConfirmationDialogWithRadioButtons(
            titleText = "Dialog title",
            subTitleText = "Subtitle",
            cancelButtonText = "Cancel very very long",
            confirmButtonText = "OK very very really long",
            initialSelectedOption = selected,
            radioOptions = options,
            onOptionSelected = { selected = it },
            onDismissRequest = { }
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ConfirmationDialogWithRadioButtonsWithScrollPreview() {
    val options = listOf(
        "Light",
        "Dark",
        "Busy",
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit",
        "System default",
        "Yet another one",
    )
    var selected by remember { mutableStateOf(options[0]) }
    OriginalThemeForPreviews {
        ConfirmationDialogWithRadioButtons(
            modifier = Modifier.height(350.dp),
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

internal const val CONFIRMATION_DIALOG_TITLE_TAG =
    "confirmation_dialog_with_radio_buttons:text_title"
internal const val CONFIRMATION_DIALOG_SUBTITLE_TAG =
    "confirmation_dialog_with_radio_buttons:text_subtitle"
internal const val CONFIRMATION_DIALOG_OPTIONS_TAG =
    "confirmation_dialog_with_radio_buttons:text_option"

/**
 * Test tag for the cancel button
 */
const val CONFIRMATION_DIALOG_CANCEL_BUTTON_TAG =
    "confirmation_dialog_with_radio_buttons:button_cancel"

/**
 * Test tag for the confirm button
 */
const val CONFIRMATION_DIALOG_CONFIRM_BUTTON_TAG =
    "confirmation_dialog_with_radio_buttons:button_confirm"
