package mega.privacy.android.core.ui.controls.textfields

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.semantics.SemanticsProperties.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextInputService
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class PasscodeFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that default keyboard is numeric password`() {
        val platformTextInputService = mock<PlatformTextInputService>()
        val textInputService = TextInputService(platformTextInputService)
        composeRule.setContent {
            CompositionLocalProvider(
                LocalTextInputService provides textInputService
            ) {
                PasscodeField(onComplete = {})
            }
        }

        composeRule.onNodeWithTag(PASSCODE_FIELD_TAG).performClick()

        composeRule.runOnIdle {
            verify(platformTextInputService, atLeastOnce()).startInput(
                value = any(),
                imeOptions = eq(
                    ImeOptions(
                        keyboardType = KeyboardType.NumberPassword,
                    )
                ),
                onEditCommand = any(),
                onImeActionPerformed = any()
            )
        }
    }

    @Test
    fun `test that visible text is masked`() {
        val maxCharacters = 6
        val mask = '\u264C'
        composeRule.setContent {
            PasscodeField(
                onComplete = {},
                numberOfCharacters = maxCharacters,
                maskCharacter = mask,
            )
        }

        val textField = composeRule.onNodeWithTag(PASSCODE_FIELD_TAG)
        repeat(maxCharacters - 1) {
            textField
                .performTextInput("x")
        }

        val actual = textField.fetchSemanticsNode().config[Text]

        actual.take(maxCharacters - 1)
            .forEach { assertThat(it.toString()).isEqualTo(mask.toString()) }
    }

    @Test
    fun `test that visible text is not masked if mask character is null`() {
        val maxCharacters = 6
        val mask = null
        composeRule.setContent {
            PasscodeField(
                onComplete = {},
                numberOfCharacters = maxCharacters,
                maskCharacter = mask,
            )
        }

        val textField = composeRule.onNodeWithTag(PASSCODE_FIELD_TAG)
        val expected = "x"
        repeat(maxCharacters - 1) {
            textField
                .performTextInput(expected)
        }

        val actual = textField.fetchSemanticsNode().config[Text]

        actual.take(maxCharacters - 1).forEach { assertThat(it.toString()).isEqualTo(expected) }
    }

    @Test
    fun `test that passcode is returned once all characters are filled`() {
        val maxCharacters = 4
        var actual: String? = null
        composeRule.setContent {
            PasscodeField(
                onComplete = { actual = it },
                numberOfCharacters = maxCharacters,
            )
        }

        val textField = composeRule.onNodeWithTag(PASSCODE_FIELD_TAG)
        repeat(maxCharacters) {
            textField
                .performTextInput("x")
        }

        val expected = buildString { repeat(maxCharacters) { append("x") } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that passcode value is cleared after submitting`() {
        val maxCharacters = 4
        val onComplete = mock<(String) -> Unit>()
        composeRule.setContent {
            PasscodeField(
                onComplete = onComplete,
                numberOfCharacters = maxCharacters,
                maskCharacter = null,
            )
        }

        val textField = composeRule.onNodeWithTag(PASSCODE_FIELD_TAG)
        val input = "x"
        repeat(maxCharacters) {
            textField
                .performTextInput(input)
        }

        verify(onComplete).invoke(any())

        val actual = textField.fetchSemanticsNode().config[Text]

        actual.forEach { assertThat(it.toString()).isEmpty() }

    }
}