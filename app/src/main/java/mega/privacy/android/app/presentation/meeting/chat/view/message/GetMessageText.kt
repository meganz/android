package mega.privacy.android.app.presentation.meeting.chat.view.message

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.text.megaSpanStyle

/**
 * Get message text
 *
 * @param message
 * @param isEdited
 */
@Composable
internal fun getMessageText(
    message: String,
    isEdited: Boolean,
): AnnotatedString =
    buildAnnotatedString {
        append(message)
        if (isEdited) {
            append(" ")
            withStyle(
                style = megaSpanStyle(
                    fontStyle = FontStyle.Italic,
                )
            ) {
                append(stringResource(id = R.string.edited_message_text))
            }
        }
    }