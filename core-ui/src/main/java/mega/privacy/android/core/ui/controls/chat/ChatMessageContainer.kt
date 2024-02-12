package mega.privacy.android.core.ui.controls.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.chat.messages.ChatBubble
import mega.privacy.android.core.ui.controls.chat.messages.reaction.ReactionsView
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.core.ui.controls.chat.messages.reaction.reactionsList
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.extensions.body4
import mega.privacy.android.core.ui.theme.tokens.TextColor

internal const val TEST_TAG_FORWARD_ICON = "chat_message_container:forward_icon"

/**
 * Message container
 *
 * @param modifier
 * @param isMine
 * @param showForwardIcon
 * @param reactions
 * @param avatarOrIcon
 * @param time
 * @param content
 */
@Composable
fun ChatMessageContainer(
    isMine: Boolean,
    showForwardIcon: Boolean,
    reactions: List<UIReaction>,
    onMoreReactionsClick: () -> Unit,
    onReactionClick: (String) -> Unit,
    onReactionLongClick: (String) -> Unit,
    onForwardClicked: () -> Unit,
    modifier: Modifier = Modifier,
    time: String? = null,
    isSendError: Boolean = false,
    onSendErrorClick: () -> Unit = {},
    avatarOrIcon: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        time?.let {
            Text(
                modifier = if (isMine) Modifier.padding(end = 16.dp) else Modifier.padding(start = 48.dp),
                text = time,
                style = MaterialTheme.typography.body4,
                color = MegaTheme.colors.text.secondary
            )
        }
        Row(
            modifier = Modifier
                .padding(if (isMine) 48.dp else 16.dp, end = 16.dp)
                .alpha(if (isSendError) 0.5f else 1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isMine) {
                if (showForwardIcon && !isSendError) {
                    ForwardIcon(onForwardClicked)
                }
                content()
            } else {
                avatarOrIcon?.let { it() }
                content()
                if (showForwardIcon) {
                    ForwardIcon(onForwardClicked)
                }
            }
        }
        if (reactions.isNotEmpty()) {
            ReactionsView(
                modifier = Modifier.padding(
                    start = if (isMine) 16.dp else 48.dp,
                    end = if (isMine) 48.dp else 16.dp
                ),
                reactions = reactions,
                isMine = isMine,
                onMoreReactionsClick = onMoreReactionsClick,
                onReactionClick = onReactionClick,
                onReactionLongClick = onReactionLongClick,
            )
        }
        if (isSendError) {
            MegaText(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .padding(start = 48.dp, end = 16.dp)
                    .clickable(enabled = true, onClick = onSendErrorClick),
                text = stringResource(id = R.string.manual_retry_alert),
                style = MaterialTheme.typography.body4,
                textColor = TextColor.Error
            )
        }
    }
}

@Composable
private fun RowScope.ForwardIcon(
    onForwardClicked: () -> Unit,
) {
    Icon(
        modifier = Modifier
            .size(24.dp)
            .align(Alignment.CenterVertically)
            .testTag(TEST_TAG_FORWARD_ICON)
            .clip(CircleShape)
            .clickable { onForwardClicked() },
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
            reactions = reactionsList,
            onMoreReactionsClick = { },
            onReactionClick = { },
            onReactionLongClick = {},
            onForwardClicked = {},
            avatarOrIcon = {
                Icon(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Bottom),
                    painter = painterResource(id = R.drawable.ic_emoji_smile_medium_regular),
                    contentDescription = "Avatar",
                    tint = MegaTheme.colors.icon.secondary
                )
            },
            time = "12:00",
            showForwardIcon = true,
            content = {
                ChatBubble(isMe = isMe, modifier = Modifier.weight(1f)) {
                    Text(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        text = "is a caldera in the Sunda Strait between the islands of Java and Sumatra in the Indonesian province of..."
                    )
                }
            },
        )
    }
}

@CombinedThemePreviews
@Composable
private fun TextMessageContainerSendErrorPreview(
    @PreviewParameter(BooleanProvider::class) isMe: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatMessageContainer(
            modifier = Modifier,
            isMine = isMe,
            reactions = emptyList(),
            onMoreReactionsClick = { },
            onReactionClick = { },
            onReactionLongClick = {},
            onForwardClicked = {},
            avatarOrIcon = {
                Icon(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Bottom),
                    painter = painterResource(id = R.drawable.ic_emoji_smile_medium_regular),
                    contentDescription = "Avatar",
                    tint = MegaTheme.colors.icon.secondary
                )
            },
            time = "12:00",
            showForwardIcon = true,
            isSendError = isMe,
            content = {
                ChatBubble(isMe = isMe, modifier = Modifier.weight(1f)) {
                    Text(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        text = "is a caldera in the Sunda Strait between the islands of Java and Sumatra in the Indonesian province of..."
                    )
                }
            },
        )
    }
}