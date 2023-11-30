package mega.privacy.android.core.ui.controls.chat

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.chat.messages.ChatBubble
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.extensions.body4

internal const val TEST_TAG_FORWARD_ICON = "chat_message_container:forward_icon"

/**
 * Message container
 *
 * @param modifier
 * @param isMine
 * @param showForwardIcon
 * @param avatarOrIcon
 * @param time
 * @param content
 */
@Composable
fun ChatMessageContainer(
    isMine: Boolean,
    showForwardIcon: Boolean,
    modifier: Modifier = Modifier,
    time: String? = null,
    avatarOrIcon: @Composable RowScope.() -> Unit = {},
    content: @Composable RowScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .padding(start = if (isMine) 48.dp else 16.dp, end = 16.dp)
            .fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        time?.let {
            Text(
                modifier = if (isMine) Modifier.padding(0.dp) else Modifier.padding(start = 32.dp),
                text = time,
                style = MaterialTheme.typography.body4,
                color = MegaTheme.colors.text.secondary
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isMine) {
                if (showForwardIcon) {
                    ForwardIcon()
                }
                content()
            } else {
                avatarOrIcon()
                content()
                if (showForwardIcon) {
                    ForwardIcon()
                }
            }
        }
    }
}

@Composable
private fun RowScope.ForwardIcon() {
    Icon(
        modifier = Modifier
            .size(24.dp)
            .align(Alignment.CenterVertically)
            .testTag(TEST_TAG_FORWARD_ICON),
        painter = painterResource(id = R.drawable.ic_forward_circle),
        contentDescription = "Icon Forward",
        tint = MegaTheme.colors.icon.secondary
    )
}

@CombinedThemePreviews
@Composable
private fun TextMessageContainerPreview(
    @PreviewParameter(BooleanProvider::class) isMe: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatMessageContainer(
            modifier = Modifier,
            isMine = isMe,
            avatarOrIcon = {
                Icon(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Bottom),
                    painter = painterResource(id = R.drawable.ic_emoji_smile),
                    contentDescription = "Avatar",
                    tint = MegaTheme.colors.icon.secondary
                )
            },
            time = "12:00",
            showForwardIcon = isMe,
            content = {
                ChatBubble(isMe = isMe) {
                    Text(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        text = "is a caldera in the Sunda Strait between the islands of Java and Sumatra in the Indonesian province of..."
                    )
                }
            },
        )
    }
}