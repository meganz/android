package mega.privacy.android.core.ui.controls.textfields

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary


internal const val PASSCODE_FIELD_TAG = "passcode_field:text_field"

/**
 * Passcode field
 *
 * @param onComplete Function to call once full passcode has been entered
 * @param modifier
 * @param keyboardOptions Default is NumberPassword, make sure to match the expected input type
 * @param numberOfCharacters Number of expected characters
 * @param maskCharacter Character with which to mask the input, null to not mask
 * @param cellComposable A composable to display each of the passcode characters
 */
@Composable
fun PasscodeField(
    onComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.NumberPassword
    ),
    numberOfCharacters: Int = 4,
    maskCharacter: Char? = '\u2022',
    cellComposable: @Composable (String, Boolean) -> Unit = { character, isFocussed ->
        PasscodeCell(
            char = character,
            isFocussed = isFocussed,
            modifier = modifier,
        )
    },
) {
    var passcodeValue by remember {
        mutableStateOf("")
    }

    BasicTextField(
        value = passcodeValue,
        onValueChange = {
            if (it.length <= numberOfCharacters) {
                passcodeValue = it
            }
            if (passcodeValue.length == numberOfCharacters) {
                onComplete(passcodeValue)
            }
        },
        keyboardOptions = keyboardOptions,
        modifier = Modifier.testTag(PASSCODE_FIELD_TAG),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.Center) {
                repeat(numberOfCharacters) { index ->
                    val char = when {
                        index >= passcodeValue.length -> ""
                        else -> maskCharacter?.toString() ?: passcodeValue[index].toString()
                    }
                    val isFocussed = passcodeValue.length == index
                    cellComposable(char, isFocussed)
                }
            }
        }
    )
}

@Composable
private fun PasscodeCell(
    char: String,
    isFocussed: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .width(40.dp)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(color = MaterialTheme.colors.textColorSecondary)
        ) {
            Text(
                text = char,
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                modifier = modifier,
            )
            Divider(
                color = if (isFocussed) MaterialTheme.colors.secondary else LocalTextStyle.current.color,
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PasscodeFieldPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        PasscodeField(onComplete = {})
    }
}
