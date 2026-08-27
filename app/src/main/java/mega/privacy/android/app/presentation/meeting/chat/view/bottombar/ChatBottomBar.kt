package mega.privacy.android.app.presentation.meeting.chat.view.bottombar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import mega.privacy.android.app.R
import mega.privacy.android.app.extensions.navigateToAppSettings
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.view.UserTypingView
import mega.privacy.android.shared.original.core.ui.controls.chat.ChatInputTextToolbar
import mega.privacy.android.shared.original.core.ui.controls.chat.VoiceClipRecordEvent
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.utils.ComposableLifecycle
import mega.privacy.android.shared.resources.R as sharedR


/**
 * Chat bottom bar
 *
 * @param uiState chat ui state
 * @param showEmojiPicker show emoji picker
 * @param onSendClick send click
 * @param onAttachmentClick attachment click
 * @param onEmojiClick emoji click
 * @param interactionSourceTextInput interaction source text input
 */
@Composable
internal fun ChatBottomBar(
    uiState: ChatUiState,
    onSendClick: (String) -> Unit,
    onAttachmentClick: () -> Unit,
    onCloseEditing: () -> Unit,
    onVoiceClipEvent: (VoiceClipRecordEvent) -> Unit = {},
    viewModel: ChatBottomBarViewModel = hiltViewModel(),
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val textFieldState = rememberTextFieldState()
    // The draft already written into the field. Survives configuration changes so that
    // recreating the composition re-applies nothing and leaves unsaved typing intact.
    var appliedSendingText by rememberSaveable { mutableStateOf<String?>(null) }
    var appliedEditingContent by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.sendingText, uiState.editingMessageContent) {
        if (appliedSendingText != uiState.sendingText
            || appliedEditingContent != uiState.editingMessageContent
        ) {
            appliedSendingText = uiState.sendingText
            appliedEditingContent = uiState.editingMessageContent
            textFieldState.setTextAndPlaceCursorAtEnd(uiState.sendingText)
        }
        if (uiState.sendingText.isNotEmpty()) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val latestEditingMessageId by rememberUpdatedState(uiState.editingMessageId)
    ComposableLifecycle {
        if (it == Lifecycle.Event.ON_PAUSE) {
            viewModel.saveDraftMessage(textFieldState.text.toString(), latestEditingMessageId)
        }
    }

    var showEmojiPicker by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = showEmojiPicker) {
        showEmojiPicker = false
    }
    val onEmojiClick: () -> Unit = {
        showEmojiPicker = !showEmojiPicker

        if (showEmojiPicker) {
            keyboardController?.hide()
        } else {
            keyboardController?.show()
        }
    }
    val interactionSourceTextInput = remember { MutableInteractionSource() }
    val isTextInputPressed by interactionSourceTextInput.collectIsPressedAsState()
    LaunchedEffect(isTextInputPressed) {
        if (isTextInputPressed) {
            showEmojiPicker = false
        }
    }


    ChatBottomBarContent(
        uiState = uiState,
        textFieldState = textFieldState,
        // Runs only for user input: a programmatic edit goes through TextFieldState.edit,
        // which never applies the input transformation. Writing a draft into the field
        // therefore cannot report the user as typing to the other participants.
        inputTransformation = InputTransformation {
            if (length > 0) viewModel.onUserTyping()
        },
        showEmojiPicker = showEmojiPicker,
        onSendClick = {
            onSendClick(it)
            viewModel.onExitTypingContext()
        },
        onAttachmentClick = {
            showEmojiPicker = false
            onAttachmentClick()
        },
        onEmojiClick = onEmojiClick,
        interactionSourceTextInput = interactionSourceTextInput,
        onCloseEditing = {
            onCloseEditing()
            viewModel.onExitTypingContext()
        },
        onVoiceClipEvent = onVoiceClipEvent,
        focusRequester = focusRequester,
    )
}

/**
 * Chat bottom bar content
 *
 * @param uiState
 * @param textFieldState
 * @param showEmojiPicker
 * @param onSendClick
 * @param onAttachmentClick
 * @param onEmojiClick
 * @param interactionSourceTextInput
 */
@Composable
fun ChatBottomBarContent(
    uiState: ChatUiState,
    textFieldState: TextFieldState,
    showEmojiPicker: Boolean,
    onSendClick: (String) -> Unit,
    onAttachmentClick: () -> Unit,
    onEmojiClick: () -> Unit,
    interactionSourceTextInput: MutableInteractionSource,
    inputTransformation: InputTransformation? = null,
    onCloseEditing: () -> Unit = {},
    onVoiceClipEvent: (VoiceClipRecordEvent) -> Unit = {},
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val context = LocalContext.current
    Column {
        UserTypingView(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            usersTyping = uiState.usersTyping,
        )
        ChatInputTextToolbar(
            onAttachmentClick = onAttachmentClick,
            placeholder = if (uiState.isNoteToSelf) stringResource(sharedR.string.chat_note_to_self_chat_message_hint) else stringResource(
                R.string.type_message_hint_with_customized_title,
                uiState.title.orEmpty()
            ),
            showEmojiPicker = showEmojiPicker,
            onSendClick = {
                onSendClick(it)
                textFieldState.clearText()
            },
            onEmojiClick = onEmojiClick,
            interactionSource = interactionSourceTextInput,
            state = textFieldState,
            inputTransformation = inputTransformation,
            editingMessageId = uiState.editingMessageId,
            editMessageContent = uiState.editingMessageContent,
            onCloseEditing = onCloseEditing,
            onVoiceClipEvent = onVoiceClipEvent,
            onNavigateToAppSettings = { context.navigateToAppSettings() },
            focusRequester = focusRequester,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ChatBottomBarPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ChatBottomBarContent(
            uiState = ChatUiState(
                sendingText = "Sending text",
                usersTyping = listOf("User 1", "User 2"),
            ),
            textFieldState = rememberTextFieldState(),
            showEmojiPicker = false,
            onSendClick = {},
            onAttachmentClick = {},
            onEmojiClick = {},
            interactionSourceTextInput = remember { MutableInteractionSource() },
        )
    }
}