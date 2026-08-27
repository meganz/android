package mega.privacy.android.shared.original.core.ui.controls.chat

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.core.R
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.conditional

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
 * Record voice clip tag
 */
const val TEST_TAG_RECORD_VOICE_CLIP_ICON = "chat_input_text_toolbar:record_voice_clip"

/**
 * Chat input text toolbar
 *
 * @param state holds the text and the selection of the input field
 * @param modifier modifier
 * @param onAttachmentClick click listener for attachment icon
 * @param onSendClick click listener for send icon
 */
@Composable
fun ChatInputTextToolbar(
    state: TextFieldState,
    placeholder: String,
    showEmojiPicker: Boolean,
    onAttachmentClick: () -> Unit,
    onSendClick: (String) -> Unit,
    onEmojiClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    inputTransformation: InputTransformation? = null,
    editingMessageId: Long? = null,
    editMessageContent: String? = null,
    onCloseEditing: () -> Unit = {},
    onVoiceClipEvent: (VoiceClipRecordEvent) -> Unit = {},
    onNavigateToAppSettings: () -> Unit = {},
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    var isInputExpanded by rememberSaveable { mutableStateOf(false) }
    var showExpandButton by remember { mutableStateOf(false) }
    var isRoundedShape by remember { mutableStateOf(false) }
    val isEditing = editingMessageId != null
    // Derived so that typing only recomposes the toolbar when the field crosses
    // between empty and non-empty, not on every keystroke.
    val isTextEmpty by remember(state) { derivedStateOf { state.text.isEmpty() } }
    val voiceClipRecorderState =
        remember { mutableStateOf(VoiceClipRecorderState()) }
    LaunchedEffect(isTextEmpty) {
        if (isTextEmpty) {
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
            .background(color = DSTokens.colors.background.pageBackground)
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
                painter = rememberVectorPainter(
                    if (isInputExpanded) IconPack.Medium.Thin.Outline.Minimize02 else IconPack.Medium.Thin.Outline.Maximize02
                ),
                contentDescription = "Attachment icon",
                tint = DSTokens.colors.icon.secondary,
            )
        }
        VoiceClipRecorderView(
            voiceClipRecorderState = voiceClipRecorderState,
            onVoiceClipEvent = onVoiceClipEvent,
            onNavigateToAppSettings = onNavigateToAppSettings,
        )
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
                    tint = DSTokens.colors.icon.secondary,
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .conditional(isInputExpanded) {
                            fillMaxHeight()
                        }
                        .conditional(!isInputExpanded) {
                            background(
                                color = DSTokens.colors.background.surface2,
                                shape = shape,
                            )
                        },
                ) {
                    if (editingMessageId != null) {
                        ChatEditMessageView(
                            modifier = Modifier
                                .padding(
                                    start = 12.dp,
                                    end = 10.dp,
                                    top = 8.dp,
                                    bottom = 4.dp,
                                )
                                .fillMaxWidth(),
                            content = editMessageContent.orEmpty(),
                            onCloseEditing = onCloseEditing,
                        )
                    }
                    ChatTextField(
                        state = state,
                        inputTransformation = inputTransformation,
                        placeholder = placeholder,
                        onEmojiClick = onEmojiClick,
                        isEmojiPickerShown = showEmojiPicker,
                        isExpanded = isInputExpanded,
                        onTextLayout = {
                            showExpandButton = it.lineCount >= 4
                            isRoundedShape = it.lineCount == 1
                        },
                        interactionSource = interactionSource,
                        focusRequester = focusRequester,
                    )
                }

                AnimatedVisibility(
                    modifier = Modifier
                        .align(Alignment.Bottom)
                        .padding(vertical = 6.dp),
                    visible = !isTextEmpty ||
                            voiceClipRecorderState.value.event == VoiceClipRecordEvent.Lock,
                ) {
                    Icon(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                            .testTag(TEST_TAG_SEND_ICON)
                            .clickable(onClick = {
                                if (voiceClipRecorderState.value.show) {
                                    voiceClipRecorderState.value =
                                        VoiceClipRecorderState(show = false)
                                    onVoiceClipEvent(VoiceClipRecordEvent.Finish)
                                } else {
                                    onSendClick(state.text.toString())
                                }
                                isInputExpanded = false
                            }),
                        painter = painterResource(id = R.drawable.ic_send_horizontal),
                        contentDescription = "Send icon",
                        tint = DSTokens.colors.icon.accent
                    )
                }
                AnimatedVisibility(
                    modifier = Modifier
                        .align(Alignment.Bottom)
                        .padding(vertical = 6.dp),
                    visible = isTextEmpty &&
                            voiceClipRecorderState.value.event != VoiceClipRecordEvent.Lock,
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(mega.privacy.android.icon.pack.R.drawable.ic_mic_medium_thin_outline),
                        contentDescription = "mic icon",
                        tint = DSTokens.colors.icon.secondary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                            .testTag(TEST_TAG_RECORD_VOICE_CLIP_ICON)
                            .pointerInput(null) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.type == PointerEventType.Press) {
                                            voiceClipRecorderState.value =
                                                VoiceClipRecorderState(show = true)
                                        }
                                        voiceClipRecorderState.value =
                                            voiceClipRecorderState.value.copy(
                                                offsetX = event.changes.first().position.x,
                                                offsetY = event.changes.first().position.y,
                                                type = event.type
                                            )
                                    }
                                }
                            },
                    )
                }
            }

            AnimatedVisibility(visible = showEmojiPicker) {
                val isPortrait =
                    LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
                val defaultHeight = if (isPortrait) 300.dp else 150.dp
                var keyboardHeight by remember {
                    mutableStateOf(0.dp)
                }
                keyboardHeight =
                    maxOf(
                        keyboardHeight,
                        WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                                - WindowInsets.navigationBars.asPaddingValues()
                            .calculateBottomPadding()
                    )
                MegaEmojiPickerView(
                    onEmojiPicked = { state.insertAtSelection(it.emoji) },
                    showEmojiPicker = showEmojiPicker,
                    modifier = Modifier
                        .height(if (keyboardHeight > 0.dp) keyboardHeight else defaultHeight),
                )
            }
        }
    }
}

/**
 * Replaces the current selection with [text] and leaves the cursor after it.
 *
 * A selection dragged right-to-left is reversed, so its offsets are normalised —
 * [TextFieldBuffer.replace] requires `start <= end` and throws otherwise.
 */
internal fun TextFieldState.insertAtSelection(text: String) {
    edit {
        val start = selection.min
        replace(start, selection.max, text)
        selection = TextRange(start + text.length)
    }
}

@CombinedThemePreviews
@Composable
private fun ChatInputTextToolbarPlaceholderPreview(
    @PreviewParameter(BooleanProvider::class) showEmojiPicker: Boolean,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ChatInputTextToolbar(
            state = rememberTextFieldState(),
            placeholder = "Very long long long long long long long long long long long long long long long long long long long long long long long long long long long ",
            showEmojiPicker = showEmojiPicker,
            onAttachmentClick = {},
            onSendClick = {},
            onEmojiClick = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ChatInputTextToolbarLongTextPreview(
    @PreviewParameter(BooleanProvider::class) showEmojiPicker: Boolean,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ChatInputTextToolbar(
            state = rememberTextFieldState("abc ".repeat(30)),
            placeholder = "",
            showEmojiPicker = showEmojiPicker,
            onAttachmentClick = {},
            onSendClick = {},
            onEmojiClick = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ChatInputTextToolbarEditingMessageTextPreview(
    @PreviewParameter(BooleanProvider::class) showEmojiPicker: Boolean,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ChatInputTextToolbar(
            state = rememberTextFieldState("Hello world"),
            placeholder = "",
            showEmojiPicker = false,
            onAttachmentClick = {},
            onSendClick = {},
            onEmojiClick = {},
            editingMessageId = 1L,
            editMessageContent = "This is a message"
        )
    }
}