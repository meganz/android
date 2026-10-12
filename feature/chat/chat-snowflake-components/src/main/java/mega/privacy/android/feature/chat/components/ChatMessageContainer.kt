package mega.privacy.android.feature.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.checkbox.Checkbox
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.modifiers.conditional
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.R as IconPackR

/**
 * Layout for one chat message row: an avatar column, the message content, an optional forward
 * button and an optional row below the content for reactions or a send error.
 *
 * Own messages align to the end and other messages align to the start. In select mode the whole
 * row toggles the selection, a checkbox replaces the avatar and the forward button is hidden.
 *
 * @param isMine True when the message was sent by the current user.
 * @param modifier [Modifier]
 * @param onForwardClick Invoked when the forward button is tapped. The forward button shows only
 * when this is not null, the row is not in select mode and [sendErrorText] is null.
 * @param forwardContentDescription Accessibility label of the forward button.
 * @param sendErrorText Text that tells the user that the message was not sent, or null when there
 * is no send error. When not null, it replaces [reactions].
 * @param isSelectMode True when the chat room is in multi-select mode.
 * @param isSelected True when this message is selected.
 * @param onSelectionChanged Invoked with the new selection state when the row is toggled.
 * @param avatar Optional avatar slot. The modifier centres the avatar in the avatar column.
 * @param avatarAlignment Vertical alignment of the avatar relative to the content.
 * @param reactions Optional reactions slot shown below the content. The argument is false in
 * select mode, when the reactions must not react to taps.
 * @param content Message content, usually a [ChatBubble]. The argument is false in select mode,
 * when the content must not react to taps.
 */
@Composable
fun ChatMessageContainer(
    isMine: Boolean,
    modifier: Modifier = Modifier,
    onForwardClick: (() -> Unit)? = null,
    forwardContentDescription: String? = null,
    sendErrorText: String? = null,
    isSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChanged: (Boolean) -> Unit = {},
    avatar: (@Composable (modifier: Modifier) -> Unit)? = null,
    avatarAlignment: Alignment.Vertical = Alignment.Bottom,
    reactions: (@Composable (interactionEnabled: Boolean) -> Unit)? = null,
    content: @Composable (interactionEnabled: Boolean) -> Unit,
) {
    val interactionEnabled = !isSelectMode
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp)
            .conditional(isSelectMode) {
                toggleable(
                    value = isSelected,
                    role = Role.Checkbox,
                    onValueChange = onSelectionChanged,
                )
            },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (avatar != null || isSelectMode) {
                Box(
                    modifier = Modifier
                        .width(avatarWidth)
                        .height(24.dp)
                        .align(if (isSelectMode) Alignment.Top else avatarAlignment),
                ) {
                    if (isSelectMode) {
                        // The row owns the toggle, so the checkbox only shows the state.
                        Checkbox(
                            checked = isSelected,
                            onCheckStateChanged = onSelectionChanged,
                            clickable = false,
                            tapTargetArea = false,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .testTag(CHAT_MESSAGE_CONTAINER_CHECKBOX_TAG),
                        )
                    } else {
                        avatar?.invoke(Modifier.align(Alignment.Center))
                    }
                }
            }
            Row(
                horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp),
            ) {
                val forward: @Composable () -> Unit = {
                    if (onForwardClick != null && !isSelectMode && sendErrorText == null) {
                        ForwardButton(
                            onClick = onForwardClick,
                            contentDescription = forwardContentDescription,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }
                if (isMine) {
                    forward()
                    content(interactionEnabled)
                } else {
                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        content(interactionEnabled)
                    }
                    forward()
                }
            }
        }

        if (sendErrorText != null || reactions != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = avatarWidth, end = 16.dp),
            ) {
                if (sendErrorText != null) {
                    MegaText(
                        text = sendErrorText,
                        textColor = TextColor.Error,
                        style = AppTheme.typography.bodySmall,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 2.dp)
                            .testTag(CHAT_MESSAGE_CONTAINER_SEND_ERROR_TAG),
                    )
                } else if (reactions != null) {
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .align(if (isMine) Alignment.TopEnd else Alignment.TopStart)
                            .testTag(CHAT_MESSAGE_CONTAINER_REACTIONS_TAG),
                    ) {
                        reactions(interactionEnabled)
                    }
                }
            }
        }
    }
}

@Composable
private fun ForwardButton(
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .border(1.5.dp, DSTokens.colors.icon.secondary, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(CHAT_MESSAGE_CONTAINER_FORWARD_TAG),
        contentAlignment = Alignment.Center,
    ) {
        MegaIcon(
            painter = painterResource(IconPackR.drawable.ic_corner_up_right_medium_thin_outline),
            tint = IconColor.Secondary,
            contentDescription = contentDescription,
            modifier = Modifier.size(14.dp),
        )
    }
}

/**
 * Width of the avatar column, including its padding.
 */
private val avatarWidth = 40.dp

@CombinedThemePreviews
@Composable
private fun ChatMessageContainerPreview(
    @PreviewParameter(ChatMessageContainerPreviewProvider::class)
    parameter: ChatMessageContainerPreviewParameter,
) {
    AndroidThemeForPreviews {
        ChatMessageContainerSample(parameter)
    }
}

/**
 * Sample [ChatMessageContainer] with a text bubble, used by the previews and screenshot tests.
 */
@Composable
internal fun ChatMessageContainerSample(parameter: ChatMessageContainerPreviewParameter) {
    ChatMessageContainer(
        isMine = parameter.isMine,
        onForwardClick = if (parameter.showForward) ({}) else null,
        sendErrorText = if (parameter.hasSendError) "Message not sent" else null,
        isSelectMode = parameter.isSelectMode,
        isSelected = parameter.isSelected,
        avatar = if (parameter.isMine) null else { avatarModifier ->
            Box(
                modifier = avatarModifier
                    .size(24.dp)
                    .background(DSTokens.colors.background.surface3, CircleShape),
            )
        },
        reactions = if (parameter.hasReactions) {
            { SampleReactions() }
        } else {
            null
        },
    ) {
        ChatBubble(isMine = parameter.isMine) {
            MegaText(
                text = parameter.text,
                textColor = if (parameter.isMine) TextColor.Inverse else TextColor.Primary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun SampleReactions() {
    MegaText(
        text = "👍 2",
        style = AppTheme.typography.bodySmall,
        modifier = Modifier
            .border(1.dp, DSTokens.colors.border.subtle, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/**
 * Preview parameters for [ChatMessageContainerSample].
 */
internal data class ChatMessageContainerPreviewParameter(
    val isMine: Boolean,
    val showForward: Boolean = true,
    val hasSendError: Boolean = false,
    val isSelectMode: Boolean = false,
    val isSelected: Boolean = false,
    val hasReactions: Boolean = false,
    val text: String = "Short message",
)

private const val LONG_TEXT =
    "Krakatoa is a caldera in the Sunda Strait between the islands of Java and Sumatra in Indonesia"

/**
 * Key variants of [ChatMessageContainer], for own and other messages.
 */
internal class ChatMessageContainerPreviewProvider :
    PreviewParameterProvider<ChatMessageContainerPreviewParameter> {
    override val values: Sequence<ChatMessageContainerPreviewParameter> = sequenceOf(
        ChatMessageContainerPreviewParameter(isMine = true),
        ChatMessageContainerPreviewParameter(isMine = false),
        ChatMessageContainerPreviewParameter(isMine = true, showForward = false),
        ChatMessageContainerPreviewParameter(isMine = true, text = LONG_TEXT),
        ChatMessageContainerPreviewParameter(isMine = false, text = LONG_TEXT),
        ChatMessageContainerPreviewParameter(isMine = true, hasSendError = true),
        ChatMessageContainerPreviewParameter(isMine = true, hasReactions = true),
        ChatMessageContainerPreviewParameter(isMine = false, hasReactions = true),
        ChatMessageContainerPreviewParameter(isMine = true, isSelectMode = true, isSelected = true),
        ChatMessageContainerPreviewParameter(isMine = false, isSelectMode = true),
    )
}

/**
 * Test tag for the forward button of a chat message.
 */
const val CHAT_MESSAGE_CONTAINER_FORWARD_TAG = "chat_message_container:forward"

/**
 * Test tag for the selection checkbox of a chat message.
 */
const val CHAT_MESSAGE_CONTAINER_CHECKBOX_TAG = "chat_message_container:checkbox"

/**
 * Test tag for the send error text of a chat message.
 */
const val CHAT_MESSAGE_CONTAINER_SEND_ERROR_TAG = "chat_message_container:send_error"

/**
 * Test tag for the reactions slot of a chat message.
 */
const val CHAT_MESSAGE_CONTAINER_REACTIONS_TAG = "chat_message_container:reactions"
