package mega.privacy.android.app.presentation.meeting.chat.view.message

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import mega.privacy.android.app.R

/**
 * Get message text
 *
 * @param message
 * @param isEdited
 * @param spansStyle
 */
@Composable
internal fun getMessageText(
    message: String,
    isEdited: Boolean,
    spansStyle: SpanStyle,
): AnnotatedString =
    buildAnnotatedString {
        append(message)
        if (isEdited) {
            append(" ")
            withStyle(
                style = spansStyle
            ) {
                append(stringResource(id = R.string.edited_message_text))
            }
        }
    }