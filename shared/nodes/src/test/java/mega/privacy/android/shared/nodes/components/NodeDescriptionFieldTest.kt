package mega.privacy.android.shared.nodes.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.AndroidThemeForPreviews
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NodeDescriptionFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(
        description: String = "",
        isEditable: Boolean = true,
        label: String = "Description",
        placeholder: String = "Add description",
        charLimit: Int = DEFAULT_NODE_DESCRIPTION_CHAR_LIMIT,
        onDescriptionChange: (String) -> Unit = {},
        onFocused: () -> Unit = {},
        onConfirmed: () -> Unit = {},
        onCharLimitReached: () -> Unit = {},
    ) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                NodeDescriptionField(
                    description = description,
                    isEditable = isEditable,
                    label = label,
                    placeholder = placeholder,
                    onDescriptionChange = onDescriptionChange,
                    charLimit = charLimit,
                    onFocused = onFocused,
                    onConfirmed = onConfirmed,
                    onCharLimitReached = onCharLimitReached,
                )
            }
        }
    }

    @Test
    fun `test that the label is displayed`() {
        setContent(label = "Description")

        composeRule.onNodeWithText("Description").assertIsDisplayed()
    }

    @Test
    fun `test that the text field shows the current description when editable`() {
        setContent(description = "My description", isEditable = true)

        composeRule.onNodeWithTag(NODE_DESCRIPTION_TEXT_FIELD_TAG)
            .assertIsDisplayed()
            .assert(hasText("My description"))
        composeRule.onNodeWithTag(NODE_DESCRIPTION_READ_ONLY_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the placeholder is displayed when editable and description is empty`() {
        setContent(description = "", isEditable = true, placeholder = "Add description")

        composeRule.onNodeWithText("Add description").assertIsDisplayed()
    }

    @Test
    fun `test that the read-only text is displayed instead of the text field when not editable`() {
        setContent(description = "My description", isEditable = false)

        composeRule.onNodeWithTag(NODE_DESCRIPTION_READ_ONLY_TAG)
            .assertIsDisplayed()
            .assert(hasText("My description"))
        composeRule.onNodeWithTag(NODE_DESCRIPTION_TEXT_FIELD_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the counter is displayed when the field is focused`() {
        setContent(description = "Hello", charLimit = 100)

        composeRule.onNodeWithTag(NODE_DESCRIPTION_COUNTER_TAG, useUnmergedTree = true)
            .assertDoesNotExist()

        composeRule.onNodeWithTag(NODE_DESCRIPTION_TEXT_FIELD_TAG).performClick()

        composeRule.onNodeWithTag(NODE_DESCRIPTION_COUNTER_TAG, useUnmergedTree = true)
            .assertExists()
            .assert(hasText("5/100"))
    }

    @Test
    fun `test that input is truncated to the char limit`() {
        setContent(charLimit = 5)

        composeRule.onNodeWithTag(NODE_DESCRIPTION_TEXT_FIELD_TAG)
            .performTextInput("1234567890")

        composeRule.onNodeWithTag(NODE_DESCRIPTION_TEXT_FIELD_TAG)
            .assert(hasText("12345"))
    }

    @Test
    fun `test that onDescriptionChange is invoked with the new text when Done is pressed`() {
        var newDescription: String? = null
        setContent(description = "", onDescriptionChange = { newDescription = it })

        composeRule.onNodeWithTag(NODE_DESCRIPTION_TEXT_FIELD_TAG)
            .performTextInput("New description")
        composeRule.onNodeWithTag(NODE_DESCRIPTION_TEXT_FIELD_TAG).performImeAction()

        assertThat(newDescription).isEqualTo("New description")
    }

    @Test
    fun `test that onDescriptionChange is not invoked when Done is pressed and the text is unchanged`() {
        var invoked = false
        setContent(description = "Same text", onDescriptionChange = { invoked = true })

        composeRule.onNodeWithTag(NODE_DESCRIPTION_TEXT_FIELD_TAG).performClick()
        composeRule.onNodeWithTag(NODE_DESCRIPTION_TEXT_FIELD_TAG).performImeAction()

        assertThat(invoked).isFalse()
    }

    @Test
    fun `test that onFocused is invoked once when the field gains focus`() {
        var focusedCount = 0
        setContent(description = "Hello", onFocused = { focusedCount++ })

        composeRule.onNodeWithTag(NODE_DESCRIPTION_TEXT_FIELD_TAG).performClick()

        assertThat(focusedCount).isEqualTo(1)
    }

    @Test
    fun `test that onConfirmed is invoked when Done is pressed even if the text is unchanged`() {
        var confirmed = false
        setContent(description = "Same text", onConfirmed = { confirmed = true })

        composeRule.onNodeWithTag(NODE_DESCRIPTION_TEXT_FIELD_TAG).performClick()
        composeRule.onNodeWithTag(NODE_DESCRIPTION_TEXT_FIELD_TAG).performImeAction()

        assertThat(confirmed).isTrue()
    }

    @Test
    fun `test that onCharLimitReached is invoked when input exceeds the char limit`() {
        var limitReached = false
        setContent(charLimit = 5, onCharLimitReached = { limitReached = true })

        composeRule.onNodeWithTag(NODE_DESCRIPTION_TEXT_FIELD_TAG)
            .performTextInput("1234567890")

        assertThat(limitReached).isTrue()
    }

    @Test
    fun `test that onCharLimitReached is not invoked when input stays within the char limit`() {
        var limitReached = false
        setContent(charLimit = 5, onCharLimitReached = { limitReached = true })

        composeRule.onNodeWithTag(NODE_DESCRIPTION_TEXT_FIELD_TAG)
            .performTextInput("12345")

        assertThat(limitReached).isFalse()
    }

    @Test
    fun `test that the field text updates when the persisted description changes`() {
        var description by mutableStateOf("Initial")
        composeRule.setContent {
            AndroidThemeForPreviews {
                NodeDescriptionField(
                    description = description,
                    isEditable = true,
                    label = "Description",
                    placeholder = "Add description",
                    onDescriptionChange = {},
                )
            }
        }

        composeRule.onNodeWithTag(NODE_DESCRIPTION_TEXT_FIELD_TAG).assert(hasText("Initial"))

        description = "Updated"

        composeRule.onNodeWithTag(NODE_DESCRIPTION_TEXT_FIELD_TAG).assert(hasText("Updated"))
    }
}
