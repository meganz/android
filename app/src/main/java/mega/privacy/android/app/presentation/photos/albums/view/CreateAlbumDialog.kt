package mega.privacy.android.app.presentation.photos.albums.view

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.StringsConstants
import mega.privacy.android.legacy.core.ui.controls.dialogs.MegaDialog
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.theme.accent_050
import mega.privacy.android.shared.original.core.ui.theme.accent_900
import mega.privacy.android.shared.original.core.ui.theme.black
import mega.privacy.android.shared.original.core.ui.theme.caption
import mega.privacy.android.shared.original.core.ui.theme.grey_300
import mega.privacy.android.shared.original.core.ui.theme.red_400
import mega.privacy.android.shared.original.core.ui.theme.red_900
import mega.privacy.android.shared.original.core.ui.theme.white
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.CreateNewAlbumDialogEvent

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun CreateNewAlbumDialog(
    titleResID: Int,
    positiveButtonTextResID: Int,
    onDismissRequest: () -> Unit = {},
    onDialogPositiveButtonClicked: (name: String) -> Unit,
    onDialogInputChange: (Boolean) -> Unit = {},
    initialInputText: () -> String = { "" },
    inputPlaceHolderText: () -> String = { "" },
    errorMessage: Int? = null,
    isInputValid: () -> Boolean = { true },
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        // delay needed before requesting focus to account for transition
        delay(300L)
        focusRequester.requestFocus()
    }

    LaunchedEffect(Unit) {
        Analytics.tracker.trackEvent(CreateNewAlbumDialogEvent)
    }

    var textState by rememberSaveable { mutableStateOf(initialInputText()) }
    val isEnabled by remember { mutableStateOf(true) }
    val isError by remember { mutableStateOf(false) }
    val singleLine = true
    val maxCharacter = 250

    val inputColor = if (isInputValid()) {
        if (MaterialTheme.colors.isLight) {
            accent_900
        } else {
            accent_050
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
        modifier = Modifier
            .imePadding()
            .padding(horizontal = 40.dp),
        onDismissRequest = onDismissRequest,
        titleString = stringResource(id = titleResID),
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
                        .focusRequester(focusRequester)
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
                        // places leading icon, text field with label and placeholder, trailing icon
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
                            Text(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                text = if (it == R.string.invalid_characters_defined) {
                                    stringResource(id = it).replace(
                                        "%1\$s",
                                        StringsConstants.INVALID_CHARACTERS
                                    )
                                } else {
                                    stringResource(id = it)
                                },
                                color = if (MaterialTheme.colors.isLight) {
                                    red_900
                                } else {
                                    red_400
                                },
                                style = caption
                            )

                            Icon(
                                modifier = Modifier
                                    .size(24.dp)
                                    .fillMaxHeight(),
                                painter = painterResource(
                                    id = R.drawable.ic_sent_error_warning
                                ),
                                contentDescription = "Input error icon",
                                tint = if (MaterialTheme.colors.isLight) {
                                    red_900
                                } else {
                                    red_400
                                }
                            )

                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {

                    onDialogPositiveButtonClicked(textState)
                },
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
                MegaText(
                    text = stringResource(id = positiveButtonTextResID),
                    textColor = TextColor.Primary
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
                MegaText(
                    text = stringResource(id = sharedR.string.general_dialog_cancel_button),
                    textColor = TextColor.Primary
                )
            }
        }
    )
}
