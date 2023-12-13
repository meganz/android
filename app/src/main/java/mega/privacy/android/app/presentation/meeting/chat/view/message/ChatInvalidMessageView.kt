package mega.privacy.android.app.presentation.meeting.chat.view.message

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.chat.messages.ChatErrorBubble
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.chat.messages.InvalidMessageType
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Text view for chat message
 *
 * @param invalidType [InvalidMessageType]
 * @param modifier Modifier
 */
@Composable
fun ChatInvalidMessageView(
    invalidType: InvalidMessageType,
    modifier: Modifier = Modifier,
) {
    ChatErrorBubble(errorText = getInvalidMessageTextId(invalidType), modifier = modifier)
}

@Composable
private fun getInvalidMessageTextId(invalidType: InvalidMessageType) =
    stringResource(
        id =
        when (invalidType) {
            InvalidMessageType.Format -> R.string.error_message_invalid_format
            InvalidMessageType.Signature -> R.string.error_message_invalid_signature
            InvalidMessageType.Unrecognizable -> R.string.error_message_unrecognizable
        }
    )

@CombinedThemePreviews
@Composable
private fun ChatInvalidMessagePreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ChatInvalidMessageView(invalidType = InvalidMessageType.Format)
    }
}