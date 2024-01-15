package mega.privacy.android.app.presentation.meeting.chat.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.extension.isJoined
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.startMeetingActivity
import mega.privacy.android.core.ui.controls.chat.ReturnToCallBanner

@Composable
internal fun ReturnToCallBanner(
    uiState: ChatUiState,
    isAudioPermissionGranted: Boolean,
    onAnswerCall: () -> Unit,
) = with(uiState) {
    if (!isConnected || (callsInOtherChats.isEmpty() && callInThisChat == null)) return@with null

    val context = LocalContext.current

    val callInOtherChatAnswered =
        callsInOtherChats.find { it.status?.isJoined == true && !it.isOnHold }
            ?: callsInOtherChats.find { it.status?.isJoined == true }
    val isCallInOtherChatAnswered = callInOtherChatAnswered != null
    val isCallInThisChatAnswered = callInThisChat?.status?.isJoined == true

    when {
        callInThisChat != null && callsInOtherChats.isNotEmpty() && (isCallInThisChatAnswered || isCallInOtherChatAnswered) -> {
            // At least one call in which I am participating
            val bannerCall =
                if (isCallInThisChatAnswered && ((callInOtherChatAnswered != null && callInOtherChatAnswered.isOnHold)
                            || !isCallInOtherChatAnswered)
                ) {
                    callInThisChat
                } else {
                    callInOtherChatAnswered
                } ?: return@with null

            ReturnToCallBanner(
                text = stringResource(id = R.string.call_in_progress_layout),
                onBannerClicked = { startMeetingActivity(context, bannerCall.chatId) },
                duration = bannerCall.duration
            )
        }

        callInThisChat != null -> {
            val answered = callInThisChat.status?.isJoined == true

            if (schedIsPending && !answered) return@with null

            ReturnToCallBanner(
                text = stringResource(
                    id = when {
                        answered || (!isGroup && (callInThisChat.isRinging || !isChatNotificationMute)) -> R.string.call_in_progress_layout
                        isMeeting -> R.string.join_meeting_layout_in_group_call
                        isGroup -> R.string.join_call_layout_in_group_call
                        else -> R.string.join_call_layout
                    }
                ),
                onBannerClicked = {
                    when {
                        !answered && isAudioPermissionGranted -> onAnswerCall()
                        !isAudioPermissionGranted -> startMeetingActivity(
                            context,
                            chatId,
                            enableAudio = false
                        )

                        else -> startMeetingActivity(context, chatId)
                    }
                },
                duration = callInThisChat.duration?.takeIf { answered && it > 0 }
            )
        }

        isCallInOtherChatAnswered -> {
            if (callInOtherChatAnswered == null) return@with null

            ReturnToCallBanner(
                text = stringResource(id = R.string.call_in_progress_layout),
                onBannerClicked = { startMeetingActivity(context, callInOtherChatAnswered.chatId) },
                duration = callInOtherChatAnswered.duration
            )
        }

        else -> {
            null
        }
    }
}