package mega.privacy.android.app.presentation.meeting.chat.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.controls.buttons.RaisedProgressMegaButton
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Button to join a chat, only available in preview mode.
 */
@Composable
fun JoinChatButton(
    isPreviewMode: Boolean,
    isJoining: Boolean,
    onClick: () -> Unit,
) {
    if (!isPreviewMode) return

    val modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)

    if (isJoining) {
        RaisedProgressMegaButton(modifier = modifier.testTag(TEST_TAG_JOIN_PROGRESS_BUTTON))
    } else {
        RaisedDefaultMegaButton(
            textId = R.string.action_join,
            onClick = onClick,
            modifier = modifier.testTag(TEST_TAG_JOIN_CHAT_BUTTON),
        )
    }
}

@CombinedTextAndThemePreviews
@Composable
private fun JoinChatButtonPreview(
    @PreviewParameter(BooleanProvider::class) isJoining: Boolean,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        JoinChatButton(
            isPreviewMode = true,
            isJoining = isJoining,
            onClick = {},
        )
    }
}

internal const val TEST_TAG_JOIN_CHAT_BUTTON = "chat_view:join_chat_button"
internal const val TEST_TAG_JOIN_PROGRESS_BUTTON = "chat_view:join_progress_button"