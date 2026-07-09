package mega.privacy.android.shared.nodes.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * A reusable node description input field.
 *
 * Multi line text area with a counter
 *
 * @param description the persisted description
 * @param isEditable whether the current user can change the description
 * @param label the field/section label (e.g. "Description")
 * @param placeholder the hint shown in the editable field while it is empty
 * @param onDescriptionChange invoked with the new value when the keyboard "Done" action is pressed
 * @param modifier modifier for the field/section
 * @param charLimit the maximum number of characters allowed
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NodeDescriptionField(
    description: String,
    isEditable: Boolean,
    label: String,
    placeholder: String,
    onDescriptionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    charLimit: Int = DEFAULT_NODE_DESCRIPTION_CHAR_LIMIT,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MegaText(
            text = label,
            textColor = TextColor.Primary,
            style = AppTheme.typography.bodyLarge,
        )

        if (isEditable) {
            var text by rememberSaveable { mutableStateOf(description) }
            var isFocused by rememberSaveable { mutableStateOf(false) }
            // Keep the field in sync when the persisted description changes (e.g. after a save/update).
            LaunchedEffect(description) { text = description }

            val focusManager = LocalFocusManager.current
            val bringIntoViewRequester = remember { BringIntoViewRequester() }
            val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
            // A multiline field scrolls its own cursor, so ask the parent scroll to lift the whole
            // field above the keyboard; re-request as the IME animates in so it clears the final height.
            LaunchedEffect(isFocused, imeBottom) {
                if (isFocused) {
                    bringIntoViewRequester.bringIntoView()
                }
            }

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .onFocusChanged { isFocused = it.isFocused }
                    .testTag(NODE_DESCRIPTION_TEXT_FIELD_TAG),
                value = text,
                onValueChange = { text = it.take(charLimit) },
                placeholder = {
                    MegaText(
                        text = placeholder,
                        textColor = TextColor.Placeholder,
                        style = AppTheme.typography.bodyLarge,
                    )
                },
                textStyle = AppTheme.typography.bodyLarge,
                shape = RoundedCornerShape(8.dp),
                minLines = 1,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (text != description) {
                            onDescriptionChange(text)
                        }
                        focusManager.clearFocus()
                    },
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DSTokens.colors.border.strongSelected,
                    unfocusedBorderColor = DSTokens.colors.border.strong,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = DSTokens.colors.text.primary,
                    focusedTextColor = DSTokens.colors.text.primary,
                    unfocusedTextColor = DSTokens.colors.text.primary,
                ),
                supportingText = if (isFocused) {
                    {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            MegaText(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .testTag(NODE_DESCRIPTION_COUNTER_TAG),
                                text = "${text.length}/$charLimit",
                                textColor = TextColor.Secondary,
                                style = AppTheme.typography.bodySmall,
                            )
                        }
                    }
                } else {
                    null
                },
            )
        } else {
            MegaText(
                modifier = Modifier.testTag(NODE_DESCRIPTION_READ_ONLY_TAG),
                text = description,
                textColor = TextColor.Secondary,
                style = AppTheme.typography.bodyMedium,
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun NodeDescriptionFieldEmptyPreview() {
    AndroidThemeForPreviews {
        NodeDescriptionField(
            description = "",
            isEditable = true,
            label = "Description",
            placeholder = "Add description",
            onDescriptionChange = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun NodeDescriptionFieldEditablePreview() {
    AndroidThemeForPreviews {
        NodeDescriptionField(
            description = "Slides for the Q3 planning meeting",
            isEditable = true,
            label = "Description",
            placeholder = "Add description",
            onDescriptionChange = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun NodeDescriptionFieldReadOnlyPreview() {
    AndroidThemeForPreviews {
        NodeDescriptionField(
            description = "Slides for the Q3 planning meeting",
            isEditable = false,
            label = "Description",
            placeholder = "Add description",
            onDescriptionChange = {},
        )
    }
}

/**
 * Default maximum length for a node description.
 */
const val DEFAULT_NODE_DESCRIPTION_CHAR_LIMIT = 300

/**
 * Test tag for the character counter shown while the editable description field is focused.
 */
const val NODE_DESCRIPTION_COUNTER_TAG = "node_description_field:counter"

/**
 * Test tag for the editable description text field.
 */
const val NODE_DESCRIPTION_TEXT_FIELD_TAG = "node_description_field:text_field"

/**
 * Test tag for the read-only description text shown when the field is not editable.
 */
const val NODE_DESCRIPTION_READ_ONLY_TAG = "node_description_field:read_only_text"
