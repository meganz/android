package mega.privacy.android.app.presentation.videosection.view.playlist

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.StringsConstants
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.black
import mega.privacy.android.shared.original.core.ui.theme.caption
import mega.privacy.android.shared.original.core.ui.theme.grey_300
import mega.privacy.android.shared.original.core.ui.theme.red_400
import mega.privacy.android.shared.original.core.ui.theme.red_900
import mega.privacy.android.shared.original.core.ui.theme.teal_200
import mega.privacy.android.shared.original.core.ui.theme.teal_300
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.theme.white
import mega.privacy.android.legacy.core.ui.controls.dialogs.MegaDialog
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun CreateVideoPlaylistDialog(
    title: String,
    positiveButtonText: String,
    onDialogPositiveButtonClicked: (title: String) -> Unit,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {},
    onDialogInputChange: (Boolean) -> Unit = {},
    initialInputText: () -> String = { "" },
    inputPlaceHolderText: () -> String = { "" },
    errorMessage: Int? = null,
    isInputValid: () -> Boolean = { true },
) {
    var textState by rememberSaveable { mutableStateOf(initialInputText()) }
    val isEnabled by rememberSaveable { mutableStateOf(true) }
    val isError by rememberSaveable { mutableStateOf(false) }
    val singleLine = true
    val maxCharacter = 250

    val inputColor = if (isInputValid()) {
        if (MaterialTheme.colors.isLight) {
            teal_300
        } else {
            teal_200
        }
    } else {
        if (MaterialTheme.colors.isLight) {
            red_900
        } else {
            red_400
        }
    }

    val textFieldColors = TextFieldDefaults.textFieldColors(
        backgroundColor = Color.Transparent,
        cursorColor = inputColor,
        focusedIndicatorColor = inputColor,
        unfocusedIndicatorColor = inputColor,
    )

    val interactionSource = remember { MutableInteractionSource() }

    val textColor = if (isInputValid()) {
        if (MaterialTheme.colors.isLight) {
            black
        } else {
            white
        }
    } else {
        if (MaterialTheme.colors.isLight) {
            red_900
        } else {
            red_400
        }
    }
    val mergedTextStyle = LocalTextStyle.current.merge(
        TextStyle(
            color = textColor,
            fontSize = 16.sp,
        )
    )

    MegaDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = modifier
            .padding(horizontal = 40.dp),
        onDismissRequest = onDismissRequest,
        titleString = title,
        fontWeight = FontWeight.W500,
        body = {
            Column(modifier = Modifier.fillMaxWidth()) {
                BasicTextField(
                    value = textState,
                    onValueChange = {
                        onDialogInputChange(true)
                        if (it.length <= maxCharacter) {
                            textState = it
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Color.Transparent)
                        .indicatorLine(
                            enabled = isEnabled,
                            isError = isError,
                            interactionSource = interactionSource,
                            colors = textFieldColors
                        ),
                    cursorBrush = SolidColor(textFieldColors.cursorColor(isError).value),
                    textStyle = mergedTextStyle,
                    maxLines = 1,
                    singleLine = singleLine,
                    decorationBox = @Composable { innerTextField ->
                        TextFieldDefaults.TextFieldDecorationBox(
                            enabled = isEnabled,
                            interactionSource = interactionSource,
                            singleLine = singleLine,
                            visualTransformation = VisualTransformation.None,
                            value = textState,
                            innerTextField = innerTextField,
                            placeholder = {
                                Text(
                                    text = inputPlaceHolderText(),
                                    color = grey_300,
                                    fontSize = 16.sp,
                                )
                            },
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 0.dp),
                            colors = textFieldColors,
                        )
                    }
                )
                if (!isInputValid()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        errorMessage?.let {
                            MegaText(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                                    .testTag(ERROR_MESSAGE_TEST_TAG),
                                text = when (it) {
                                    R.string.invalid_characters_defined -> stringResource(id = it).replace(
                                        "%1\$s",
                                        StringsConstants.INVALID_CHARACTERS
                                    )

                                    R.string.invalid_string -> stringResource(id = it)

                                    else -> stringResource(id = sharedR.string.video_section_playlists_error_message_playlist_name_exists)
                                },
                                textColor = TextColor.Error,
                                style = caption
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onDialogPositiveButtonClicked(textState) },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                ),
                modifier = Modifier
                    .padding(all = 0.dp)
                    .testTag(POSITIVE_BUTTON_TEST_TAG),
                elevation = ButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    disabledElevation = 0.dp,
                    hoveredElevation = 0.dp,
                    focusedElevation = 0.dp
                ),
            ) {
                Text(
                    text = positiveButtonText,
                    color = teal_300
                )
            }
        },
        dismissButton = {
            Button(
                onClick = onDismissRequest,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                ),
                modifier = Modifier.padding(all = 0.dp),
                elevation = ButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    disabledElevation = 0.dp,
                    hoveredElevation = 0.dp,
                    focusedElevation = 0.dp
                ),
            ) {
                Text(
                    stringResource(id = R.string.general_cancel),
                    color = teal_300
                )
            }
        }
    )
}

@CombinedThemePreviews
@Composable
private fun CreateVideoPlaylistDialogPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        CreateVideoPlaylistDialog(
            title = "Enter playlist name",
            positiveButtonText = stringResource(id = R.string.general_create),
            onDismissRequest = {},
            onDialogPositiveButtonClicked = {},
            onDialogInputChange = {},
            inputPlaceHolderText = { "New playlist(x)" },
            errorMessage = -1
        ) {
            true
        }
    }
}

@CombinedThemePreviews
@Composable
private fun CreateVideoPlaylistDialogWithErrorPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        CreateVideoPlaylistDialog(
            title = "Enter playlist name",
            positiveButtonText = stringResource(id = R.string.general_create),
            onDismissRequest = {},
            onDialogPositiveButtonClicked = {},
            onDialogInputChange = {},
            inputPlaceHolderText = { "New playlist(x)" },
            errorMessage = R.string.invalid_characters_defined
        ) {
            false
        }
    }
}

@CombinedThemePreviews
@Composable
private fun CreateVideoPlaylistDialogWithSameNamePreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        CreateVideoPlaylistDialog(
            title = "Enter playlist name",
            positiveButtonText = stringResource(id = R.string.general_create),
            onDismissRequest = {},
            onDialogPositiveButtonClicked = {},
            onDialogInputChange = {},
            inputPlaceHolderText = { "New playlist(x)" },
            errorMessage = -1
        ) {
            false
        }
    }
}

/**
 * Test tag for error message
 */
const val ERROR_MESSAGE_TEST_TAG = "video_playlist_create_dialog:text_error_message"

/**
 * Test tag for error message
 */
const val POSITIVE_BUTTON_TEST_TAG = "video_playlist_create_dialog:button_positive"

