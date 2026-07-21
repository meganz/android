package mega.privacy.android.app.meeting.activity

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dagger.Lazy
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.presentation.meeting.CreateScheduledMeetingActivity
import mega.privacy.android.app.presentation.meeting.WaitingRoomActivity
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.CreateScheduledMeetingNavKey
import mega.privacy.android.navigation.destination.LegacyMeetingNavKey
import mega.privacy.android.navigation.destination.LegacyWaitingRoomNavKey
import mega.privacy.android.navigation.destination.MeetingNavKeyInfo
import mega.privacy.android.navigation.destination.WaitingRoomNavKeyInfo


fun EntryProviderScope<NavKey>.legacyMeetingScreen(
    removeDestination: () -> Unit,
    megaChatRequestHandler: Lazy<MegaChatRequestHandler>,
    chatManagement: Lazy<ChatManagement>,
    setChatVideoInDeviceUseCase: Lazy<SetChatVideoInDeviceUseCase>,
    rtcAudioManagerGateway: Lazy<RTCAudioManagerGateway>,
) {
    entry<LegacyMeetingNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = Intent(context, MeetingActivity::class.java).apply {
                putExtra(MeetingActivity.MEETING_CHAT_ID, key.chatId)
                when (val meetingInfo = key.meetingInfo) {
                    is MeetingNavKeyInfo.JoinAsGuest -> {
                        initGuestMeeting(
                            key.chatId,
                            megaChatRequestHandler.get(),
                            chatManagement.get()
                        )

                        setAction(MeetingActivity.MEETING_ACTION_GUEST)
                        putExtra(MeetingActivity.MEETING_IS_GUEST, true)
                        putExtra(MeetingActivity.MEETING_NAME, meetingInfo.meetingName)
                        setData(meetingInfo.link.toUri())
                        setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    is MeetingNavKeyInfo.RejoinInProgressCall -> {
                        chatManagement.get().setOpeningMeetingLink(key.chatId, true)

                        setAction(MeetingActivity.MEETING_ACTION_JOIN)
                        putExtra(
                            MeetingActivity.MEETING_PUBLIC_CHAT_HANDLE,
                            meetingInfo.publicChatHandle
                        )
                        putExtra(MeetingActivity.MEETING_NAME, meetingInfo.meetingName)
                        setData(meetingInfo.link.toUri())
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    is MeetingNavKeyInfo.JoinInProgressCall -> {
                        chatManagement.get().setOpeningMeetingLink(key.chatId, true)

                        setAction(MeetingActivity.MEETING_ACTION_JOIN)
                        putExtra(MeetingActivity.MEETING_NAME, meetingInfo.meetingName)
                        setData(meetingInfo.link.toUri())
                        setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    is MeetingNavKeyInfo.ReturnToInProgressCall -> {
                        setAction(MeetingActivity.MEETING_ACTION_IN)
                        putExtra(MeetingActivity.MEETING_IS_GUEST, meetingInfo.isGuest)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }

                    is MeetingNavKeyInfo.OpenCall -> {
                        val chatMgmt = chatManagement.get()
                        if (meetingInfo.answer) {
                            setChatVideoInDeviceUseCase.get().invoke()
                            chatMgmt.removeJoiningCallChatId(key.chatId)
                            rtcAudioManagerGateway.get().removeRTCAudioManagerRingIn()
                            CallUtil.clearIncomingCallNotification(meetingInfo.callId)
                        }
                        chatMgmt.setSpeakerStatus(key.chatId, meetingInfo.hasLocalVideo)
                        chatMgmt.setRequestSentCall(
                            meetingInfo.callId,
                            meetingInfo.isOutgoing
                        )
                        MegaApplication.getInstance().openCallService(key.chatId)

                        setAction(MeetingActivity.MEETING_ACTION_IN)
                        putExtra(MeetingActivity.MEETING_IS_GUEST, meetingInfo.isGuest)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    is MeetingNavKeyInfo.StartOutgoingCall -> {
                        MegaApplication.getInstance().openCallService(key.chatId)

                        setAction(MeetingActivity.MEETING_ACTION_IN)
                        putExtra(MeetingActivity.MEETING_AUDIO_ENABLE, meetingInfo.isAudioEnable)
                        putExtra(MeetingActivity.MEETING_VIDEO_ENABLE, meetingInfo.isVideoEnable)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
            }
            context.startActivity(intent)

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}

fun EntryProviderScope<NavKey>.legacyWaitingRoomScreen(
    removeDestination: () -> Unit,
    chatRequestHandler: Lazy<MegaChatRequestHandler>,
    chatManagement: Lazy<ChatManagement>,
) {
    entry<LegacyWaitingRoomNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = Intent(context, WaitingRoomActivity::class.java).apply {
                putExtra(WaitingRoomActivity.EXTRA_CHAT_ID, key.chatId)
                when (val waitingRoomInfo = key.waitingRoomInfo) {
                    is WaitingRoomNavKeyInfo.JoinWaitingRoom -> {
                        chatManagement.get().setOpeningMeetingLink(key.chatId, true)
                        putExtra(WaitingRoomActivity.EXTRA_CHAT_LINK, waitingRoomInfo.link)
                        setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    is WaitingRoomNavKeyInfo.JoinAsGuest -> {
                        initGuestMeeting(
                            key.chatId,
                            chatRequestHandler.get(),
                            chatManagement.get()
                        )
                        putExtra(WaitingRoomActivity.EXTRA_CHAT_LINK, waitingRoomInfo.link)
                        setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
            }
            context.startActivity(intent)

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}

fun EntryProviderScope<NavKey>.createScheduledMeetingScreen(
    removeDestination: () -> Unit,
    navigateToMeetingsTab: () -> Unit,
) {
    entry<CreateScheduledMeetingNavKey>(
        metadata = transparentMetadata()
    ) {
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                navigateToMeetingsTab()
            } else {
                removeDestination()
            }
        }
        LaunchedEffect(Unit) {
            launcher.launch(Intent(context, CreateScheduledMeetingActivity::class.java))
        }
    }
}

private fun initGuestMeeting(
    chatId: Long,
    megaChatRequestHandler: MegaChatRequestHandler,
    chatManagement: ChatManagement,
) {
    chatManagement.setOpeningMeetingLink(chatId, true)
    megaChatRequestHandler.setIsLoginRunning(true)
}

