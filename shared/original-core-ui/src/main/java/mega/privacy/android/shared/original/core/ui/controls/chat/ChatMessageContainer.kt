package mega.privacy.android.shared.original.core.ui.controls.chat

import mega.privacy.android.icon.pack.R as IconPackR
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.controls.buttons.MegaCheckbox
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.ChatBubble
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.LocationMessageView
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.ReactionsView
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.reactionsList
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.body4
import mega.privacy.android.shared.original.core.ui.theme.extensions.conditional
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

internal const val TEST_TAG_FORWARD_ICON = "chat_message_container:forward_icon"

/**
 * Message container
 *
 * @param modifier
 * @param isMine
 * @param showForwardIcon
 * @param reactions
 * @param avatarOrIcon
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
    onSelectionChanged: (Boolean) -> Unit = {},
    avatarOrIcon: @Composable ((modifier: Modifier) -> Unit)? = null,
    avatarAlignment: Alignment.Vertical = Alignment.Bottom,
    content: @Composable (interactionEnabled: Boolean) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp)
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
    ) {
        if (avatarOrIcon != null) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(24.dp)
                    .align(if (isSelectMode) Alignment.Top else avatarAlignment),
            ) {
                if (isSelectMode) {
                    MegaCheckbox(
                        checked = isSelected,
                        onCheckedChange = onSelectionChanged,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    avatarOrIcon(Modifier.align(Alignment.Center))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp),
            ) {
                val forward: @Composable () -> Unit = {
                    if (shouldDisplayForwardIcon(
                            showForwardIcon,
                            isSelectMode,
                            isSendError
                        )
                    ) {
                        ForwardIcon(
                            onForwardClicked,
                            Modifier
                                .align(Alignment.CenterVertically)
                                .padding(start = 8.dp, end = 8.dp)
                        )
                    }
                }

                if (isMine) {
                    forward()
                    content(!isSelectMode)
                } else {
                    Box(modifier = Modifier.weight(1f, false)) {
                        content(!isSelectMode)
                    }
                    forward()
                }
            }

            if (isSendError || reactions.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp),
                ) {
                    if (isSendError) {
                        MegaText(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 2.dp),
                            text = stringResource(id = R.string.manual_retry_alert),
                            style = MaterialTheme.typography.body4,
                            textColor = TextColor.Error
                        )
                    } else {
                        ReactionsView(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .align(if (isMine) Alignment.TopEnd else Alignment.TopStart),
                            reactions = reactions,
                            isMine = isMine,
                            onMoreReactionsClick = onMoreReactionsClick,
                            onReactionClick = onReactionClick,
                            onReactionLongClick = onReactionLongClick,
                            interactionEnabled = !isSelectMode,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun shouldDisplayForwardIcon(
    showForwardIcon: Boolean,
    isSelectMode: Boolean,
    hasError: Boolean,
) = showForwardIcon && !isSelectMode && !hasError

@Composable
private fun ForwardIcon(
    onForwardClicked: () -> Unit,
    modifier: Modifier,
) {
    Icon(
        modifier = modifier
            .size(24.dp)
            .testTag(TEST_TAG_FORWARD_ICON)
            .clip(CircleShape)
            .clickable { onForwardClicked() },
        painter = painterResource(id = R.drawable.ic_forward_circle),
        contentDescription = "Icon Forward",
        tint = MegaOriginalTheme.colors.icon.secondary
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        LazyColumn() {
            item {
                ChatMessageContainer(
                    isMine = parameter.isMe,
                    showForwardIcon = true,
                    reactions = parameter.reactions,
                    onMoreReactionsClick = { },
                    onReactionClick = { },
                    onReactionLongClick = {},
                    onForwardClicked = {},
                    modifier = Modifier,
                    isSendError = parameter.isMe && parameter.hasSendError,
                    isSelectMode = parameter.inSelectMode,
                    isSelected = isSelected,
                    onSelectionChanged = { isSelected = !isSelected },
                    avatarOrIcon = {
                        Icon(
                            modifier = it,
                            painter = painterResource(id = R.drawable.ic_emoji_smile_medium_regular),
                            contentDescription = "Avatar",
                            tint = MegaOriginalTheme.colors.icon.secondary
                        )
                    },
                    content = parameter.content,
                )
            }
        }
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
    val content: @Composable (interactionEnabled: Boolean) -> Unit =
        {
            ChatBubble(isMe = isMe) {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    text = text
                )
            }
        },
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
            ChatMessageContainerPreviewParameter(
                isMe = false,
                inSelectMode = true,
                text = "is a caldera in the Sunda Strait between the islands of Java and Sumatra in the Indonesian province of...",
            ),
            ChatMessageContainerPreviewParameter(isMe = true, reactions = reactionsList),
            ChatMessageContainerPreviewParameter(isMe = false, reactions = reactionsList),
            ChatMessageContainerPreviewParameter(isMe = true,
                content = {
                    LocationMessageView(
                        isMe = true,
                        title = buildAnnotatedString { append("Pinned location") },
                        geolocation = "41.1472° N, 8.6179° W",
                        map = ImageBitmap.imageResource(IconPackR.drawable.ic_folder_incoming_medium_solid),
                    )
                }
            ),
            ChatMessageContainerPreviewParameter(isMe = true,
                inSelectMode = true,
                content = {
                    LocationMessageView(
                        isMe = true,
                        title = buildAnnotatedString { append("Pinned location") },
                        geolocation = "41.1472° N, 8.6179° W",
                        map = ImageBitmap.imageResource(IconPackR.drawable.ic_folder_incoming_medium_solid),
                    )
                }
            ),
            ChatMessageContainerPreviewParameter(isMe = false,
                inSelectMode = true,
                content = {
                    LocationMessageView(
                        isMe = false,
                        title = buildAnnotatedString { append("Pinned location") },
                        geolocation = "41.1472° N, 8.6179° W",
                        map = ImageBitmap.imageResource(IconPackR.drawable.ic_folder_incoming_medium_solid),
                    )
                }
            ),
        )
}

