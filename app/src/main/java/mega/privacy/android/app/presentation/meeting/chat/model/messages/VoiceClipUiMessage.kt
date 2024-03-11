package mega.privacy.android.app.presentation.meeting.chat.model.messages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.message.voiceclip.VoiceClipMessageViewModel
import mega.privacy.android.core.ui.controls.chat.messages.CoreVoiceClipMessageView
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.core.ui.controls.layouts.LocalSnackBarHostState
import mega.privacy.android.core.ui.theme.extensions.conditional
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage

/**
 * UI message for voice clip
 *
 * @property message [VoiceClipMessage]
 */
@OptIn(ExperimentalFoundationApi::class)
class VoiceClipUiMessage(
    override val message: VoiceClipMessage,
    override val reactions: List<UIReaction>,
) : AvatarMessage() {

    @Composable
    override fun ContentComposable(
        onLongClick: (TypedMessage) -> Unit,
        interactionEnabled: Boolean,
    ) {
        val viewModel: VoiceClipMessageViewModel = hiltViewModel()

        with(message) {
            val uiState by viewModel.getUiStateFlow(msgId).collectAsStateWithLifecycle()

            LaunchedEffect(msgId) {
                viewModel.addVoiceClip(message = message)
            }

            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current
            val snackbarHostState = LocalSnackBarHostState.current

            val onClick: () -> Unit = {
                if (exists) {
                    viewModel.onPlayOrPauseClicked(msgId)
                } else {
                    coroutineScope.launch {
                        snackbarHostState?.showSnackbar(context.getString(R.string.error_message_voice_clip))
                    }
                }
            }
            CoreVoiceClipMessageView(
                isMe = isMine,
                timestamp = uiState.timestamp,
                exists = uiState.voiceClipMessage?.exists ?: exists,
                loadProgress = uiState.loadProgress?.floatValue,
                playProgress = uiState.playProgress?.floatValue,
                isPlaying = uiState.isPlaying,
                onPlayClicked = onClick,
                interactionEnabled = interactionEnabled,
                modifier = Modifier.conditional(interactionEnabled) {
                    combinedClickable(
                        onClick = onClick,
                        onLongClick = { onLongClick(message) },
                    )
                }
            )
        }
    }

    override val showAvatar = message.shouldShowAvatar
    override val displayAsMine = message.isMine
    override val shouldDisplayForwardIcon = message.exists
    override val timeSent = message.time
    override val userHandle = message.userHandle
    override val id = message.msgId
}