package mega.privacy.android.app.meeting.activity

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.presentation.meeting.WaitingRoomActivity
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.LegacyMeetingNavKey
import mega.privacy.android.navigation.destination.LegacyWaitingRoomNavKey
import mega.privacy.android.navigation.destination.MeetingNavKeyInfo
import mega.privacy.android.navigation.destination.WaitingRoomNavKeyInfo


fun EntryProviderScope<NavKey>.legacyMeetingScreen(
    removeDestination: () -> Unit,
    megaChatRequestHandler: MegaChatRequestHandler,
    chatManagement: ChatManagement,
    setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase,
    rtcAudioManagerGateway: RTCAudioManagerGateway,
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
                        initGuestMeeting(key.chatId, megaChatRequestHandler, chatManagement)

                        setAction(MeetingActivity.MEETING_ACTION_GUEST)
                        putExtra(MeetingActivity.MEETING_IS_GUEST, true)
                        putExtra(MeetingActivity.MEETING_NAME, meetingInfo.meetingName)
                        setData(meetingInfo.link.toUri())
                        setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    is MeetingNavKeyInfo.RejoinInProgressCall -> {
                        chatManagement.setOpeningMeetingLink(key.chatId, true)

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
                        chatManagement.setOpeningMeetingLink(key.chatId, true)

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
                        if (meetingInfo.answer) {
                            setChatVideoInDeviceUseCase()
                            chatManagement.removeJoiningCallChatId(key.chatId)
                            rtcAudioManagerGateway.removeRTCAudioManagerRingIn()
                            CallUtil.clearIncomingCallNotification(meetingInfo.callId)
                        }
                        chatManagement.setSpeakerStatus(key.chatId, meetingInfo.hasLocalVideo)
                        chatManagement.setRequestSentCall(
                            meetingInfo.callId,
                            meetingInfo.isOutgoing
                        )
                        MegaApplication.getInstance().openCallService(key.chatId)

                        setAction(MeetingActivity.MEETING_ACTION_IN)
                        putExtra(MeetingActivity.MEETING_IS_GUEST, meetingInfo.isGuest)
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
    chatRequestHandler: MegaChatRequestHandler,
    chatManagement: ChatManagement,
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
                        chatManagement.setOpeningMeetingLink(key.chatId, true)
                        putExtra(WaitingRoomActivity.EXTRA_CHAT_LINK, waitingRoomInfo.link)
                        setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    is WaitingRoomNavKeyInfo.JoinAsGuest -> {
                        initGuestMeeting(key.chatId, chatRequestHandler, chatManagement)
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

private fun initGuestMeeting(
    chatId: Long,
    megaChatRequestHandler: MegaChatRequestHandler,
    chatManagement: ChatManagement,
) {
    chatManagement.setOpeningMeetingLink(chatId, true)
    megaChatRequestHandler.setIsLoginRunning(true)
}

