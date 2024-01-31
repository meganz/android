package mega.privacy.android.core.ui.controls.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.extensions.conditional

/**
 * Attachment icon test tag.
 */
const val TEST_TAG_ATTACHMENT_ICON = "chat_input_text_toolbar:attachment_icon"

/**
 * Expand icon test tag.
 */
const val TEST_TAG_EXPAND_ICON = "chat_input_text_toolbar:expand_icon"

/**
 * Send icon test tag.
 */
const val TEST_TAG_SEND_ICON = "chat_input_text_toolbar:send_icon"

/**
 * Chat input text toolbar
 *
 * @param modifier modifier
 * @param onAttachmentClick click listener for attachment icon
 * @param onSendClick click listener for send icon
 */
@Composable
fun ChatInputTextToolbar(
    text: String,
    placeholder: String,
    showEmojiPicker: Boolean,
    onAttachmentClick: () -> Unit,
    onSendClick: (String) -> Unit,
    onEmojiClick: () -> Unit,
    onTextChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    textFieldValue: TextFieldValue = TextFieldValue(text),
    editingMessageId: Long? = null,
    editMessageContent: String? = null,
    onCloseEditing: () -> Unit = {},
) {
    var isInputExpanded by rememberSaveable { mutableStateOf(false) }
    var showExpandButton by remember { mutableStateOf(false) }
    var isRoundedShape by remember { mutableStateOf(false) }
    val isEditing = editingMessageId != null
    LaunchedEffect(textFieldValue.text) {
        if (textFieldValue.text.isEmpty()) {
            isInputExpanded = false
        }
    }
    BackHandler(enabled = isInputExpanded) {
        isInputExpanded = false
    }
    val shape = if (isRoundedShape && !isEditing) {
        CircleShape
    } else {
        RoundedCornerShape(12.dp)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color = MegaTheme.colors.background.pageBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        if (showExpandButton || isInputExpanded) {
            Icon(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = if (isEditing) 48.dp else 6.dp)
                    .testTag(TEST_TAG_EXPAND_ICON)
                    .clickable {
                        isInputExpanded = !isInputExpanded
                    },
                painter = painterResource(id = if (isInputExpanded) R.drawable.ic_collapse_text_input else R.drawable.ic_expand_text_input),
                contentDescription = "Attachment icon",
                tint = MegaTheme.colors.icon.secondary,
            )
        }
        Column {
            Row {
                Icon(
                    modifier = Modifier
                        .align(Alignment.Bottom)
                        .padding(end = 8.dp, top = 6.dp, bottom = 6.dp)
                        .testTag(TEST_TAG_ATTACHMENT_ICON)
                        .clickable(onClick = onAttachmentClick),
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = "Attachment icon",
                    tint = MegaTheme.colors.icon.secondary,
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .conditional(isInputExpanded) {
                            fillMaxHeight()
                        }
                        .conditional(!isInputExpanded) {
                            background(
                                color = MegaTheme.colors.background.surface2,
                                shape = shape,
                            )
                        },
                ) {
                    if (editingMessageId != null) {
                        ChatEditMessageView(
                            modifier = Modifier.padding(
                                start = 12.dp,
                                end = 10.dp,
                                top = 8.dp,
                                bottom = 4.dp,
                            ).fillMaxWidth(),
                            content = editMessageContent.orEmpty(),
                            onCloseEditing = onCloseEditing,
                        )
                    }
                    ChatTextField(
                        textFieldValue = textFieldValue,
                        placeholder = placeholder,
                        onTextChange = onTextChange,
                        onEmojiClick = onEmojiClick,
                        isEmojiPickerShown = showEmojiPicker,
                        isExpanded = isInputExpanded,
                        onTextLayout = {
                            showExpandButton = it.lineCount >= 4
                            isRoundedShape = it.lineCount == 1
                        },
                        interactionSource = interactionSource
                    )
                }

                AnimatedVisibility(
                    modifier = Modifier
                        .align(Alignment.Bottom)
                        .padding(vertical = 6.dp),
                    visible = textFieldValue.text.isNotEmpty()
                ) {
                    Icon(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                            .testTag(TEST_TAG_SEND_ICON)
                            .clickable(onClick = {
                                onSendClick(textFieldValue.text)
                                isInputExpanded = false
                            }),
                        painter = painterResource(id = R.drawable.ic_send),
                        contentDescription = "Send icon",
                        tint = MegaTheme.colors.icon.accent
                    )
                }
            }
            AnimatedVisibility(visible = showEmojiPicker) {
                MegaEmojiPickerView(
                    onEmojiPicked = {
                        onTextChange(addPickedEmojiToInput(it.emoji, textFieldValue))
                    },
                    showEmojiPicker = showEmojiPicker,
                )
            }
        }
    }
}

private fun addPickedEmojiToInput(
    emoji: String,
    textFieldValue: TextFieldValue,
): TextFieldValue = with(textFieldValue) {
    val start = selection.start
    val end = selection.end

    TextFieldValue(
        text = if (start == end && end == text.length - 1) {
            text + emoji
        } else {
            text.substring(0, start) + emoji + text.substring(end)
        },
        selection = TextRange(start + emoji.length)
    )
}

@CombinedThemePreviews
@Composable
private fun ChatInputTextToolbarPlaceholderPreview(
    @PreviewParameter(BooleanProvider::class) showEmojiPicker: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatInputTextToolbar(
            text = "",
            placeholder = "Very long long long long long long long long long long long long long long long long long long long long long long long long long long long ",
            showEmojiPicker = showEmojiPicker,
            onAttachmentClick = {},
            onSendClick = {},
            onEmojiClick = {},
            onTextChange = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ChatInputTextToolbarLongTextPreview(
    @PreviewParameter(BooleanProvider::class) showEmojiPicker: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatInputTextToolbar(
            text = "abc ".repeat(30),
            placeholder = "",
            showEmojiPicker = showEmojiPicker,
            onAttachmentClick = {},
            onSendClick = {},
            onEmojiClick = {},
            onTextChange = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ChatInputTextToolbarEditingMessageTextPreview(
    @PreviewParameter(BooleanProvider::class) showEmojiPicker: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatInputTextToolbar(
            text = "Hello world",
            placeholder = "",
            showEmojiPicker = false,
            onAttachmentClick = {},
            onSendClick = {},
            onEmojiClick = {},
            onTextChange = {},
            editingMessageId = 1L,
            editMessageContent = "This is a message"
        )
    }
}