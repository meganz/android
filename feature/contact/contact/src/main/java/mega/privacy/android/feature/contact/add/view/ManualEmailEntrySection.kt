package mega.privacy.android.feature.contact.add.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.chip.MegaChip
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.inputfields.TextInputField
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Outcome of submitting a typed email in the [ManualEmailEntrySection]. On [Accepted] the input is
 * cleared; otherwise the matching inline error is shown and the input is kept so it can be fixed.
 */
internal enum class ManualEmailSubmitResult {
    /**
     * The email was added to the selection (as a manual entry or by auto-selecting a contact).
     */
    Accepted,

    /**
     * The email is not a syntactically valid email address.
     */
    InvalidEmail,

    /**
     * The email is already part of the current selection.
     */
    AlreadyAdded,
}

/**
 * Free-text email entry for the share picker: an email input with an add affordance, plus the
 * already-added manual emails rendered as removable chips.
 *
 * @param manualEmails the manually entered emails currently selected, rendered as chips.
 * @param onSubmitEmail invoked with the trimmed typed email when the add affordance is used;
 * returns the outcome deciding whether the input is cleared or an inline error is shown.
 * @param onRemoveEmail invoked with the email of a clicked chip to remove it from the selection.
 * @param modifier
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ManualEmailEntrySection(
    manualEmails: Set<String>,
    onSubmitEmail: (String) -> ManualEmailSubmitResult,
    onRemoveEmail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var textValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var submitError by rememberSaveable { mutableStateOf<ManualEmailSubmitResult?>(null) }

    fun submit() {
        val email = textValue.text.trim()
        if (email.isEmpty()) return
        when (val result = onSubmitEmail(email)) {
            ManualEmailSubmitResult.Accepted -> {
                textValue = TextFieldValue("")
                submitError = null
            }

            else -> submitError = result
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(MANUAL_EMAIL_SECTION_TAG),
    ) {
        TextInputField(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(MANUAL_EMAIL_INPUT_TAG),
            keyboardType = KeyboardType.Email,
            textFieldValue = textValue,
            capitalization = KeyboardCapitalization.None,
            placeholder = stringResource(sharedR.string.add_contacts_manual_email_placeholder),
            errorText = when (submitError) {
                ManualEmailSubmitResult.InvalidEmail ->
                    stringResource(sharedR.string.login_invalid_email_error_message)

                ManualEmailSubmitResult.AlreadyAdded ->
                    stringResource(sharedR.string.add_contacts_manual_email_already_added_error)

                else -> null
            },
            onValueChanged = { newValue ->
                if (newValue.text != textValue.text) submitError = null
                textValue = newValue
            },
            trailingView = {
                IconButton(
                    modifier = Modifier.testTag(MANUAL_EMAIL_ADD_TAG),
                    onClick = ::submit,
                    enabled = textValue.text.isNotBlank(),
                ) {
                    MegaIcon(
                        modifier = Modifier.size(24.dp),
                        painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Plus),
                        contentDescription = stringResource(sharedR.string.add_contacts_manual_email_add_action),
                        tint = IconColor.Accent,
                    )
                }
            },
        )
        if (manualEmails.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag(MANUAL_EMAIL_CHIPS_TAG),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                manualEmails.forEach { email ->
                    MegaChip(
                        selected = false,
                        content = email,
                        trailingPainter = rememberVectorPainter(IconPack.Medium.Thin.Outline.X),
                        onClick = { onRemoveEmail(email) },
                    )
                }
            }
        }
    }
}

internal const val MANUAL_EMAIL_SECTION_TAG = "manual_email_entry_section"
internal const val MANUAL_EMAIL_INPUT_TAG = "manual_email_entry_section:input"
internal const val MANUAL_EMAIL_ADD_TAG = "manual_email_entry_section:add"
internal const val MANUAL_EMAIL_CHIPS_TAG = "manual_email_entry_section:chips"

private class ManualEmailsProvider : PreviewParameterProvider<Set<String>> {
    override val values: Sequence<Set<String>> = sequenceOf(
        emptySet(),
        setOf("guest@example.com", "partner@example.org"),
    )
}

@CombinedThemePreviews
@Composable
private fun ManualEmailEntrySectionPreview(
    @PreviewParameter(ManualEmailsProvider::class) manualEmails: Set<String>,
) {
    AndroidThemeForPreviews {
        ManualEmailEntrySection(
            manualEmails = manualEmails,
            onSubmitEmail = { ManualEmailSubmitResult.Accepted },
            onRemoveEmail = {},
        )
    }
}
