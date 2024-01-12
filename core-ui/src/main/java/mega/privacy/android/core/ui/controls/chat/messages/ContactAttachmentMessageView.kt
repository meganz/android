package mega.privacy.android.core.ui.controls.chat.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.controls.chat.ChatStatusIcon
import mega.privacy.android.core.ui.controls.chat.UiChatStatus
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * Contact attachment message view
 *
 * @param isMe Whether the message is sent by me
 * @param userName User name
 * @param email Email
 * @param avatar Avatar
 * @param status chat status
 * @param modifier Modifier
 */
@Composable
fun ContactAttachmentMessageView(
    isMe: Boolean,
    userName: String,
    email: String,
    status: UiChatStatus?,
    avatar: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    ChatBubble(isMe = isMe, modifier = modifier) {
        CompositionLocalProvider(
            LocalContentColor provides if (isMe) MegaTheme.colors.text.inverse else MegaTheme.colors.text.primary,
        ) {
            ContactMessageContentView(avatar, status, userName, email)
        }
    }
}

/**
 * Contact message content view
 *
 * @param avatar Avatar composable
 * @param status chat status
 * @param userName User name
 * @param email
 */
@Composable
fun ContactMessageContentView(
    avatar: @Composable BoxScope.() -> Unit,
    status: UiChatStatus?,
    userName: String,
    email: String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(horizontal = 12.dp, vertical = 16.dp),
    ) {
        Box(modifier = Modifier.size(40.dp)) {
            Box(
                modifier = Modifier.border(
                    width = 1.dp,
                    color = MegaTheme.colors.background.pageBackground,
                    shape = CircleShape
                )
            ) {
                avatar()
            }
            status?.let {
                ChatStatusIcon(
                    modifier = Modifier.align(Alignment.TopEnd),
                    status = status
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = userName,
                style = MaterialTheme.typography.subtitle1,
            )
            Text(
                text = email,
                style = MaterialTheme.typography.subtitle2,
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ContactAttachmentMessageViewPreview(
    @PreviewParameter(BooleanProvider::class) isMe: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ContactAttachmentMessageView(
            isMe = isMe,
            userName = "User Name",
            email = "lh@mega.co.nz",
            avatar = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MegaTheme.colors.background.inverse,
                            shape = CircleShape
                        ),
                )
            },
            status = UiChatStatus.Online,
        )
    }
}