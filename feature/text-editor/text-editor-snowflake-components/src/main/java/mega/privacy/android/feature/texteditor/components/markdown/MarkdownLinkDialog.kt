package mega.privacy.android.feature.texteditor.components.markdown

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.android.core.ui.components.dialogs.HORIZONTAL
import mega.android.core.ui.components.dialogs.VERTICAL
import mega.android.core.ui.components.inputfields.TextInputField
import mega.privacy.android.shared.resources.R as sharedR

const val MARKDOWN_LINK_DIALOG_TAG = "markdown_link_dialog"

/**
 * Dialog for inserting or editing a Markdown link from the formatting toolbar. Pre-filled when
 * the cursor sits inside an existing link; the Remove link button (shown only then) unwraps the
 * link back to plain text.
 */
@Composable
fun MarkdownLinkDialog(
    initialText: String,
    initialUrl: String,
    showRemove: Boolean,
    onConfirm: (text: String, url: String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var linkText by rememberSaveable { mutableStateOf(initialText) }
    var url by rememberSaveable { mutableStateOf(initialUrl) }
    // The text is usually already filled in from the selection, so the URL is what the user
    // came here to type.
    val urlFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { urlFocusRequester.requestFocus() }
    }

    BasicDialog(
        title = stringResource(sharedR.string.text_editor_link_dialog_title),
        buttons = buildList {
            if (showRemove) {
                add(
                    BasicDialogButton(
                        text = stringResource(sharedR.string.text_editor_link_dialog_remove),
                        onClick = onRemove,
                    ),
                )
            }
            add(
                BasicDialogButton(
                    text = stringResource(sharedR.string.general_dialog_cancel_button),
                    onClick = onDismiss,
                ),
            )
            add(
                BasicDialogButton(
                    text = stringResource(sharedR.string.general_action_save),
                    onClick = { onConfirm(linkText, url) },
                    enabled = url.isNotBlank(),
                ),
            )
        }.toImmutableList(),
        onDismissRequest = onDismiss,
        // Three buttons do not fit one row, and the horizontal flow row places wrapped rows
        // above their predecessors, which would put Save on top of the other two.
        buttonDirection = if (showRemove) VERTICAL else HORIZONTAL,
        modifier = modifier.testTag(MARKDOWN_LINK_DIALOG_TAG),
    ) {
        MarkdownLinkDialogContent(
            linkText = linkText,
            url = url,
            onLinkTextChanged = { linkText = it },
            onUrlChanged = { url = it },
            urlModifier = Modifier.focusRequester(urlFocusRequester),
        )
    }
}

/** Dialog body, extracted so screenshot tests can render it without a dialog window. */
@Composable
internal fun MarkdownLinkDialogContent(
    linkText: String,
    url: String,
    onLinkTextChanged: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    urlModifier: Modifier = Modifier,
) = Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    TextInputField(
        modifier = urlModifier.fillMaxWidth(),
        keyboardType = KeyboardType.Uri,
        imeAction = ImeAction.Next,
        text = url,
        label = stringResource(sharedR.string.text_editor_link_dialog_url_label),
        onValueChanged = onUrlChanged,
    )
    TextInputField(
        modifier = Modifier.fillMaxWidth(),
        keyboardType = KeyboardType.Text,
        text = linkText,
        label = stringResource(sharedR.string.text_editor_link_dialog_text_label),
        onValueChanged = onLinkTextChanged,
    )
}
