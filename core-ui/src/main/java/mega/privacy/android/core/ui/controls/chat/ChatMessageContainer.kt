package mega.privacy.android.core.ui.controls.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.buttons.MegaCheckbox
import mega.privacy.android.core.ui.controls.chat.messages.ChatBubble
import mega.privacy.android.core.ui.controls.chat.messages.reaction.ReactionsView
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.core.ui.controls.chat.messages.reaction.reactionsList
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.extensions.body4
import mega.privacy.android.core.ui.theme.extensions.conditional
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
    isSendError: Boolean = false,
    isSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onSendErrorClick: () -> Unit = {},
    onSelectionChanged: (Boolean) -> Unit = {},
    avatarOrIcon: @Composable (RowScope.(modifier: Modifier) -> Unit)? = null,
    content: @Composable RowScope.(interactionEnabled: Boolean) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .conditional(
                condition = isSelectMode
            ) {
                toggleable(
                    value = isSelected,
                    enabled = true,
                    role = Role.Checkbox,
                    onValueChange = onSelectionChanged
                )
            },
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(if (isMine && !isSelectMode) 48.dp else 16.dp, end = 16.dp)
                .alpha(if (isSendError) 0.5f else 1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val indicatorSizeModifier = Modifier.size(24.dp)
            if (isMine) {
                if (isSelectMode) {
                    CheckBox(isSelected, onSelectionChanged, indicatorSizeModifier)
                    Spacer(modifier = Modifier.weight(1F))
                }
                if (shouldDisplayForwardIcon(showForwardIcon && !isSelectMode, isSendError)) {
                    ForwardIcon(onForwardClicked)
                }
                content(!isSelectMode)
            } else {
                if (isSelectMode) {
                    CheckBox(isSelected, onSelectionChanged, indicatorSizeModifier)
                } else {
                    avatarOrIcon?.let { it(indicatorSizeModifier) }
                }
                content(!isSelectMode)
                if (shouldDisplayForwardIcon(showForwardIcon, isSelectMode)) {
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
                interactionEnabled = !isSelectMode,
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
private fun RowScope.CheckBox(
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit,
    modifier: Modifier,
) {
    MegaCheckbox(
        checked = isSelected,
        onCheckedChange = onSelectionChanged,
        modifier = modifier
            .align(Alignment.Bottom)
    )
}

@Composable
private fun shouldDisplayForwardIcon(showForwardIcon: Boolean, isSelectMode: Boolean) =
    showForwardIcon && !isSelectMode

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
private fun Preview(
    @PreviewParameter(Provider::class) parameter: ChatMessageContainerPreviewParameter,
) {
    var isSelected by remember {
        mutableStateOf(parameter.checked)
    }
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatMessageContainer(
            modifier = Modifier,
            isMine = parameter.isMe,
            reactions = parameter.reactions,
            onMoreReactionsClick = { },
            onReactionClick = { },
            onReactionLongClick = {},
            onForwardClicked = {},
            onSelectionChanged = { isSelected = !isSelected },
            isSelectMode = parameter.inSelectMode,
            isSelected = isSelected,
            isSendError = parameter.isMe && parameter.hasSendError,
            avatarOrIcon = {
                Icon(
                    modifier = it
                        .align(Alignment.Bottom),
                    painter = painterResource(id = R.drawable.ic_emoji_smile_medium_regular),
                    contentDescription = "Avatar",
                    tint = MegaTheme.colors.icon.secondary
                )
            },
            showForwardIcon = true,
            content = {
                ChatBubble(isMe = parameter.isMe) {
                    Text(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        text = parameter.text
                    )
                }
            },
        )
    }
}

private data class ChatMessageContainerPreviewParameter(
    val checked: Boolean = false,
    val isMe: Boolean = false,
    val inSelectMode: Boolean = false,
    val hasSendError: Boolean = false,
    val showForward: Boolean = true,
    val text: String = "Short string",
    val reactions: List<UIReaction> = reactionsList.take(3),
)

private class Provider : PreviewParameterProvider<ChatMessageContainerPreviewParameter> {
    override val values: Sequence<ChatMessageContainerPreviewParameter> =
        sequenceOf(
            ChatMessageContainerPreviewParameter(isMe = true),
            ChatMessageContainerPreviewParameter(isMe = false),
            ChatMessageContainerPreviewParameter(isMe = true, inSelectMode = true, checked = true),
            ChatMessageContainerPreviewParameter(isMe = false, inSelectMode = true, checked = true),
            ChatMessageContainerPreviewParameter(isMe = true, inSelectMode = true, checked = false),
            ChatMessageContainerPreviewParameter(
                isMe = false,
                inSelectMode = true,
                checked = false,
            ),
            ChatMessageContainerPreviewParameter(isMe = true, showForward = false),
            ChatMessageContainerPreviewParameter(isMe = false, showForward = false),
            ChatMessageContainerPreviewParameter(isMe = true, hasSendError = true),
            ChatMessageContainerPreviewParameter(
                isMe = true,
                text = "is a caldera in the Sunda Strait between the islands of Java and Sumatra in the Indonesian province of...",
            ),
            ChatMessageContainerPreviewParameter(
                isMe = false,
                text = "is a caldera in the Sunda Strait between the islands of Java and Sumatra in the Indonesian province of...",
            ),
            ChatMessageContainerPreviewParameter(isMe = true, reactions = reactionsList),
            ChatMessageContainerPreviewParameter(isMe = false, reactions = reactionsList),
        )
}

