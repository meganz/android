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
    if (!isConnected || (callInOtherChat == null && callInThisChat == null)) return@with null

    val context = LocalContext.current

    val callInOtherChatAnswered = callInOtherChat?.status?.isJoined == true
    val callInThisChatAnswered = callInThisChat?.status?.isJoined == true

    when {
        callInThisChat != null && callInOtherChat != null && (callInThisChatAnswered || callInOtherChatAnswered) -> {
            // At least one call in which I am participating
            val bannerCall =
                if ((callInThisChatAnswered && callInOtherChatAnswered && callInOtherChat.isOnHold)
                    || (callInThisChatAnswered && !callInOtherChatAnswered)
                ) {
                    callInThisChat
                } else {
                    callInOtherChat
                }

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

        callInOtherChat != null -> {
            val answered = callInOtherChat.status?.isJoined == true

            if (!answered) return@with null

            ReturnToCallBanner(
                text = stringResource(id = R.string.call_in_progress_layout),
                onBannerClicked = { startMeetingActivity(context, callInOtherChat.chatId) },
                duration = callInOtherChat.duration
            )
        }

        else -> {
            null
        }
    }
}