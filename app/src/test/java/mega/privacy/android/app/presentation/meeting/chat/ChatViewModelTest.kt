package mega.privacy.android.app.presentation.meeting.chat

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.analytics.test.AnalyticsTestExtension
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.middlelayer.scanner.ScannerHandler
import mega.privacy.android.app.objects.GifData
import mega.privacy.android.app.presentation.documentscanner.model.DocumentScanningError
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.presentation.meeting.chat.mapper.ForwardMessagesResultMapper
import mega.privacy.android.app.presentation.meeting.chat.mapper.InviteParticipantResultMapper
import mega.privacy.android.app.presentation.meeting.chat.mapper.ParticipantNameMapper
import mega.privacy.android.app.presentation.meeting.chat.model.ActionToManage
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.model.ForwardMessagesToChatsResult
import mega.privacy.android.app.presentation.meeting.chat.model.InfoToShow
import mega.privacy.android.app.presentation.meeting.chat.model.InviteContactToChatResult
import mega.privacy.android.app.service.scanner.InsufficientRAMToLaunchDocumentScanner
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ChatCallTermCodeType
import mega.privacy.android.domain.entity.chat.ChatConnectionState
import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatPendingChanges
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.ForwardResult
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.meeting.UsersCallLimitReminders
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.chat.CreateChatException
import mega.privacy.android.domain.exception.chat.ParticipantAlreadyExistsException
import mega.privacy.android.domain.exception.chat.ResourceDoesNotExistChatException
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.call.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.call.HangChatCallUseCase
import mega.privacy.android.domain.usecase.call.IsChatStatusConnectedForCallUseCase
import mega.privacy.android.domain.usecase.call.StartCallUseCase
import mega.privacy.android.domain.usecase.call.StartChatCallNoRingingUseCase
import mega.privacy.android.domain.usecase.chat.ArchiveChatUseCase
import mega.privacy.android.domain.usecase.chat.BroadcastChatArchivedUseCase
import mega.privacy.android.domain.usecase.chat.ClearChatHistoryUseCase
import mega.privacy.android.domain.usecase.chat.CloseChatPreviewUseCase
import mega.privacy.android.domain.usecase.chat.EnableGeolocationUseCase
import mega.privacy.android.domain.usecase.chat.EndCallUseCase
import mega.privacy.android.domain.usecase.chat.GetChatMessageUseCase
import mega.privacy.android.domain.usecase.chat.GetChatParticipantEmailUseCase
import mega.privacy.android.domain.usecase.chat.GetCustomSubtitleListUseCase
import mega.privacy.android.domain.usecase.chat.HoldChatCallUseCase
import mega.privacy.android.domain.usecase.chat.InviteToChatUseCase
import mega.privacy.android.domain.usecase.chat.IsAnonymousModeUseCase
import mega.privacy.android.domain.usecase.chat.IsChatNotificationMuteUseCase
import mega.privacy.android.domain.usecase.chat.IsGeolocationEnabledUseCase
import mega.privacy.android.domain.usecase.chat.LeaveChatUseCase
import mega.privacy.android.domain.usecase.chat.MonitorCallInChatUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatConnectionStateUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatPendingChangesUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatRoomUpdatesUseCase
import mega.privacy.android.domain.usecase.chat.MonitorLeaveChatUseCase
import mega.privacy.android.domain.usecase.chat.MonitorLeavingChatUseCase
import mega.privacy.android.domain.usecase.chat.MonitorParticipatingInACallInOtherChatsUseCase
import mega.privacy.android.domain.usecase.chat.MonitorUserChatStatusByHandleUseCase
import mega.privacy.android.domain.usecase.chat.MuteChatNotificationForChatRoomsUseCase
import mega.privacy.android.domain.usecase.chat.OpenChatLinkUseCase
import mega.privacy.android.domain.usecase.chat.RecordAudioUseCase
import mega.privacy.android.domain.usecase.chat.UnmuteChatNotificationUseCase
import mega.privacy.android.domain.usecase.chat.link.JoinPublicChatUseCase
import mega.privacy.android.domain.usecase.chat.link.MonitorJoiningChatUseCase
import mega.privacy.android.domain.usecase.chat.message.AttachContactsUseCase
import mega.privacy.android.domain.usecase.chat.message.AttachNodeUseCase
import mega.privacy.android.domain.usecase.chat.message.GetChatFromContactMessagesUseCase
import mega.privacy.android.domain.usecase.chat.message.SendGiphyMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.SendLocationMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.SendTextMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.delete.DeleteMessagesUseCase
import mega.privacy.android.domain.usecase.chat.message.edit.EditLocationMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.edit.EditMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.forward.ForwardMessagesUseCase
import mega.privacy.android.domain.usecase.chat.message.reactions.AddReactionUseCase
import mega.privacy.android.domain.usecase.chat.message.reactions.DeleteReactionUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.domain.usecase.contact.GetParticipantFirstNameUseCase
import mega.privacy.android.domain.usecase.contact.GetParticipantFullNameUseCase
import mega.privacy.android.domain.usecase.contact.GetUserOnlineStatusByHandleUseCase
import mega.privacy.android.domain.usecase.contact.GetUserUseCase
import mega.privacy.android.domain.usecase.contact.MonitorAllContactParticipantsInChatUseCase
import mega.privacy.android.domain.usecase.contact.MonitorHasAnyContactUseCase
import mega.privacy.android.domain.usecase.contact.MonitorUserLastGreenUpdatesUseCase
import mega.privacy.android.domain.usecase.contact.RequestUserLastGreenUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.CreateNewImageUriUseCase
import mega.privacy.android.domain.usecase.file.DeleteFileUseCase
import mega.privacy.android.domain.usecase.meeting.BroadcastUpgradeDialogClosedUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChatUseCase
import mega.privacy.android.domain.usecase.meeting.GetUsersCallLimitRemindersUseCase
import mega.privacy.android.domain.usecase.meeting.LoadMessagesUseCase
import mega.privacy.android.domain.usecase.meeting.SendStatisticsMeetingsUseCase
import mega.privacy.android.domain.usecase.meeting.SetUsersCallLimitRemindersUseCase
import mega.privacy.android.domain.usecase.meeting.StartMeetingInWaitingRoomChatUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import mega.privacy.android.domain.usecase.transfers.paused.PauseTransfersQueueUseCase
import mega.privacy.android.shared.original.core.ui.controls.chat.VoiceClipRecordEvent
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReactionUser
import mega.privacy.mobile.analytics.event.ChatConversationUnmuteMenuToolbarEvent
import nz.mega.sdk.MegaChatError
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import java.io.File
import java.util.stream.Stream

/**
 * Test class for [ChatViewModel]
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatViewModelTest {

    private val chatId = 123L
    private val userHandle = 321L
    private val callId = 456L

    private lateinit var underTest: ChatViewModel

    private val getChatRoomUseCase: GetChatRoomUseCase = mock()
    private val isChatNotificationMuteUseCase: IsChatNotificationMuteUseCase = mock()
    private val getParticipantFirstNameUseCase: GetParticipantFirstNameUseCase = mock()
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase = mock()
    private val getStringFromStringResMapper: GetStringFromStringResMapper = mock()
    private val monitorChatRoomUpdatesUseCase: MonitorChatRoomUpdatesUseCase = mock {
        onBlocking { invoke(any()) } doReturn emptyFlow()
    }
    private val monitorUpdatePushNotificationSettingsUseCase
            : MonitorUpdatePushNotificationSettingsUseCase = mock {
        onBlocking { invoke() } doReturn emptyFlow()
    }
    private val getUserOnlineStatusByHandleUseCase: GetUserOnlineStatusByHandleUseCase = mock()
    private val monitorUserChatStatusByHandleUseCase: MonitorUserChatStatusByHandleUseCase = mock {
        onBlocking { invoke(any()) } doReturn emptyFlow()
    }
    private val monitorParticipatingInACallInOtherChatsUseCase: MonitorParticipatingInACallInOtherChatsUseCase =
        mock {
            onBlocking { invoke(any()) } doReturn emptyFlow()
        }
    private val monitorCallInChatUseCase: MonitorCallInChatUseCase = mock {
        onBlocking { invoke(any()) } doReturn emptyFlow()
    }
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase = mock {
        onBlocking { invoke() } doReturn MutableStateFlow(
            StorageStateEvent(
                handle = 1L,
                storageState = StorageState.Unknown
            )
        )
    }
    private val monitorChatConnectionStateUseCase: MonitorChatConnectionStateUseCase = mock {
        onBlocking { invoke() } doReturn emptyFlow()
    }
    private val isChatStatusConnectedForCallUseCase: IsChatStatusConnectedForCallUseCase = mock()
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock {
        onBlocking { invoke() } doReturn emptyFlow()
    }

    private val requestUserLastGreenUseCase = mock<RequestUserLastGreenUseCase>()
    private val monitorUserLastGreenUpdatesUseCase =
        mock<MonitorUserLastGreenUpdatesUseCase> {
            on { invoke(any()) } doReturn emptyFlow()
        }
    private val getScheduledMeetingByChatUseCase = mock<GetScheduledMeetingByChatUseCase>()
    private val monitorHasAnyContactUseCase = mock<MonitorHasAnyContactUseCase> {
        onBlocking { invoke() } doReturn emptyFlow()
    }
    private val getCustomSubtitleListUseCase = mock<GetCustomSubtitleListUseCase>()

    private val monitorAllContactParticipantsInChatUseCase: MonitorAllContactParticipantsInChatUseCase =
        mock {
            on { invoke(any()) } doReturn emptyFlow()
        }
    private val inviteToChatUseCase: InviteToChatUseCase = mock()

    private val inviteParticipantResultMapper: InviteParticipantResultMapper = mock()
    private val unmuteChatNotificationUseCase: UnmuteChatNotificationUseCase = mock()
    private val clearChatHistoryUseCase = mock<ClearChatHistoryUseCase>()
    private val endCallUseCase = mock<EndCallUseCase>()
    private val sendStatisticsMeetingsUseCase = mock<SendStatisticsMeetingsUseCase>()
    private val archiveChatUseCase = mock<ArchiveChatUseCase>()
    private val startCallUseCase = mock<StartCallUseCase>()
    private val chatManagement = mock<ChatManagement>()
    private val loadMessagesUseCase = mock<LoadMessagesUseCase>()
    private val muteChatNotificationForChatRoomsUseCase =
        mock<MuteChatNotificationForChatRoomsUseCase>()
    private val startChatCallNoRingingUseCase = mock<StartChatCallNoRingingUseCase>()
    private val answerChatCallUseCase = mock<AnswerChatCallUseCase>()
    private val rtcAudioManagerGateway = mock<RTCAudioManagerGateway>()
    private val startMeetingInWaitingRoomChatUseCase = mock<StartMeetingInWaitingRoomChatUseCase>()
    private val isGeolocationEnabledUseCase = mock<IsGeolocationEnabledUseCase>()
    private val enableGeolocationUseCase = mock<EnableGeolocationUseCase>()
    private val sendTextMessageUseCase = mock<SendTextMessageUseCase>()
    private val openChatLinkUseCase = mock<OpenChatLinkUseCase>()
    private val holdChatCallUseCase = mock<HoldChatCallUseCase>()
    private val hangChatCallUseCase = mock<HangChatCallUseCase>()
    private val joinPublicChatUseCase = mock<JoinPublicChatUseCase>()
    private val isAnonymousModeUseCase = mock<IsAnonymousModeUseCase>()
    private val closeChatPreviewUseCase: CloseChatPreviewUseCase = mock()
    private val createNewImageUriUseCase: CreateNewImageUriUseCase = mock()
    private val monitorJoiningChatUseCase = mock<MonitorJoiningChatUseCase>()
    private val monitorLeavingChatUseCase = mock<MonitorLeavingChatUseCase>()
    private val sendLocationMessageUseCase = mock<SendLocationMessageUseCase>()
    private val monitorChatPendingChangesUseCase = mock<MonitorChatPendingChangesUseCase> {
        on { invoke(any()) } doReturn emptyFlow()
    }
    private val addReactionUseCase = mock<AddReactionUseCase>()
    private val getChatMessageUseCase = mock<GetChatMessageUseCase>()
    private val deleteReactionUseCase = mock<DeleteReactionUseCase>()
    private val sendGiphyMessageUseCase = mock<SendGiphyMessageUseCase>()
    private val attachContactsUseCase = mock<AttachContactsUseCase>()
    private val getChatParticipantEmailUseCase = mock<GetChatParticipantEmailUseCase>()
    private val getParticipantFullNameUseCase = mock<GetParticipantFullNameUseCase>()
    private val participantNameMapper = mock<ParticipantNameMapper>()
    private val getUserUseCase = mock<GetUserUseCase>()
    private val forwardMessagesUseCase = mock<ForwardMessagesUseCase>()
    private val forwardMessagesResultMapper = mock<ForwardMessagesResultMapper>()
    private val attachNodeUseCase = mock<AttachNodeUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val addNodeType = mock<AddNodeType>()
    private val deleteMessagesUseCase = mock<DeleteMessagesUseCase>()
    private val editMessageUseCase = mock<EditMessageUseCase>()
    private val editLocationMessageUseCase = mock<EditLocationMessageUseCase>()
    private val getChatFromContactMessagesUseCase = mock<GetChatFromContactMessagesUseCase>()
    private val getCacheFileUseCase = mock<GetCacheFileUseCase>()
    private val setUsersCallLimitRemindersUseCase = mock<SetUsersCallLimitRemindersUseCase>()
    private val recordAudioUseCase = mock<RecordAudioUseCase>()
    private val deleteFileUseCase = mock<DeleteFileUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val getUsersCallLimitRemindersUseCase = mock<GetUsersCallLimitRemindersUseCase> {
        on { invoke() }.thenReturn(emptyFlow())
    }
    private val monitorLeaveChatUseCase = mock<MonitorLeaveChatUseCase> {
        on { invoke() } doReturn emptyFlow()
    }
    private val leaveChatUseCase = mock<LeaveChatUseCase>()
    private val broadcastChatArchivedUseCase = mock<BroadcastChatArchivedUseCase>()
    private val broadcastUpgradeDialogClosedUseCase = mock<BroadcastUpgradeDialogClosedUseCase>()
    private val areTransfersPausedUseCase = mock<AreTransfersPausedUseCase>()
    private val pauseTransfersQueueUseCase = mock<PauseTransfersQueueUseCase>()
    private val scannerHandler = mock<ScannerHandler>()

    @BeforeEach
    fun resetMocks() {
        reset(
            getChatRoomUseCase,
            isChatNotificationMuteUseCase,
            getUserOnlineStatusByHandleUseCase,
            isChatStatusConnectedForCallUseCase,
            requestUserLastGreenUseCase,
            getMyUserHandleUseCase,
            getParticipantFirstNameUseCase,
            getScheduledMeetingByChatUseCase,
            getCustomSubtitleListUseCase,
            inviteToChatUseCase,
            inviteParticipantResultMapper,
            unmuteChatNotificationUseCase,
            clearChatHistoryUseCase,
            endCallUseCase,
            sendStatisticsMeetingsUseCase,
            archiveChatUseCase,
            startCallUseCase,
            chatManagement,
            loadMessagesUseCase,
            muteChatNotificationForChatRoomsUseCase,
            startChatCallNoRingingUseCase,
            answerChatCallUseCase,
            rtcAudioManagerGateway,
            startMeetingInWaitingRoomChatUseCase,
            isGeolocationEnabledUseCase,
            enableGeolocationUseCase,
            sendTextMessageUseCase,
            openChatLinkUseCase,
            holdChatCallUseCase,
            hangChatCallUseCase,
            joinPublicChatUseCase,
            isAnonymousModeUseCase,
            closeChatPreviewUseCase,
            createNewImageUriUseCase,
            sendLocationMessageUseCase,
            addReactionUseCase,
            getChatMessageUseCase,
            deleteReactionUseCase,
            sendGiphyMessageUseCase,
            attachContactsUseCase,
            getChatParticipantEmailUseCase,
            getParticipantFullNameUseCase,
            participantNameMapper,
            getUserUseCase,
            forwardMessagesUseCase,
            forwardMessagesResultMapper,
            addNodeType,
            deleteMessagesUseCase,
            editMessageUseCase,
            editLocationMessageUseCase,
            getChatFromContactMessagesUseCase,
            getCacheFileUseCase,
            recordAudioUseCase,
            deleteFileUseCase,
            getFeatureFlagValueUseCase,
            leaveChatUseCase,
            broadcastChatArchivedUseCase,
            setUsersCallLimitRemindersUseCase,
            getUsersCallLimitRemindersUseCase,
            broadcastUpgradeDialogClosedUseCase,
            areTransfersPausedUseCase,
            pauseTransfersQueueUseCase,
            scannerHandler,
            getStringFromStringResMapper
        )
        whenever(getUsersCallLimitRemindersUseCase()).thenReturn(emptyFlow())
        wheneverBlocking { isAnonymousModeUseCase() } doReturn false
        wheneverBlocking { monitorChatRoomUpdatesUseCase(any()) } doReturn emptyFlow()
        wheneverBlocking { monitorUpdatePushNotificationSettingsUseCase() } doReturn emptyFlow()
        wheneverBlocking { monitorUserChatStatusByHandleUseCase(any()) } doReturn emptyFlow()
        wheneverBlocking { monitorStorageStateEventUseCase() } doReturn MutableStateFlow(
            StorageStateEvent(
                handle = 1L,
                storageState = StorageState.Unknown
            )
        )
        wheneverBlocking { monitorChatConnectionStateUseCase() } doReturn emptyFlow()
        wheneverBlocking { monitorConnectivityUseCase() } doReturn emptyFlow()
        wheneverBlocking { monitorUserLastGreenUpdatesUseCase(userHandle) } doReturn emptyFlow()
        wheneverBlocking { monitorHasAnyContactUseCase() } doReturn emptyFlow()
        wheneverBlocking { monitorCallInChatUseCase(any()) } doReturn emptyFlow()
        wheneverBlocking { monitorParticipatingInACallInOtherChatsUseCase(any()) } doReturn emptyFlow()
        whenever(monitorAllContactParticipantsInChatUseCase(any())) doReturn emptyFlow()
        wheneverBlocking { monitorJoiningChatUseCase(any()) } doReturn emptyFlow()
        wheneverBlocking { monitorLeavingChatUseCase(any()) } doReturn emptyFlow()
        whenever(monitorChatPendingChangesUseCase(any())) doReturn emptyFlow()
        whenever(monitorLeaveChatUseCase()) doReturn emptyFlow()
        wheneverBlocking { getFeatureFlagValueUseCase(ApiFeatures.CallUnlimitedProPlan) } doReturn false
    }

    private fun initTestClass(
        action: String = Constants.ACTION_CHAT_SHOW_MESSAGES,
        link: String? = null,
    ) {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "chatId" to chatId.toString(),
                "chatAction" to action,
                "link" to link,
            )
        )
        underTest = ChatViewModel(
            getChatRoomUseCase = getChatRoomUseCase,
            isChatNotificationMuteUseCase = isChatNotificationMuteUseCase,
            monitorChatRoomUpdatesUseCase = monitorChatRoomUpdatesUseCase,
            monitorUpdatePushNotificationSettingsUseCase = monitorUpdatePushNotificationSettingsUseCase,
            getUserOnlineStatusByHandleUseCase = getUserOnlineStatusByHandleUseCase,
            monitorUserChatStatusByHandleUseCase = monitorUserChatStatusByHandleUseCase,
            monitorParticipatingInACallInOtherChatsUseCase = monitorParticipatingInACallInOtherChatsUseCase,
            monitorCallInChatUseCase = monitorCallInChatUseCase,
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            monitorChatConnectionStateUseCase = monitorChatConnectionStateUseCase,
            isChatStatusConnectedForCallUseCase = isChatStatusConnectedForCallUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            requestUserLastGreenUseCase = requestUserLastGreenUseCase,
            monitorUserLastGreenUpdatesUseCase = monitorUserLastGreenUpdatesUseCase,
            getParticipantFirstNameUseCase = getParticipantFirstNameUseCase,
            getMyUserHandleUseCase = getMyUserHandleUseCase,
            getScheduledMeetingByChatUseCase = getScheduledMeetingByChatUseCase,
            monitorHasAnyContactUseCase = monitorHasAnyContactUseCase,
            getCustomSubtitleListUseCase = getCustomSubtitleListUseCase,
            savedStateHandle = savedStateHandle,
            monitorAllContactParticipantsInChatUseCase = monitorAllContactParticipantsInChatUseCase,
            inviteToChatUseCase = inviteToChatUseCase,
            unmuteChatNotificationUseCase = unmuteChatNotificationUseCase,
            inviteParticipantResultMapper = inviteParticipantResultMapper,
            clearChatHistoryUseCase = clearChatHistoryUseCase,
            endCallUseCase = endCallUseCase,
            sendStatisticsMeetingsUseCase = sendStatisticsMeetingsUseCase,
            archiveChatUseCase = archiveChatUseCase,
            startCallUseCase = startCallUseCase,
            startChatCallNoRingingUseCase = startChatCallNoRingingUseCase,
            chatManagement = chatManagement,
            muteChatNotificationForChatRoomsUseCase = muteChatNotificationForChatRoomsUseCase,
            answerChatCallUseCase = answerChatCallUseCase,
            rtcAudioManagerGateway = rtcAudioManagerGateway,
            startMeetingInWaitingRoomChatUseCase = startMeetingInWaitingRoomChatUseCase,
            isGeolocationEnabledUseCase = isGeolocationEnabledUseCase,
            enableGeolocationUseCase = enableGeolocationUseCase,
            sendTextMessageUseCase = sendTextMessageUseCase,
            openChatLinkUseCase = openChatLinkUseCase,
            holdChatCallUseCase = holdChatCallUseCase,
            hangChatCallUseCase = hangChatCallUseCase,
            joinPublicChatUseCase = joinPublicChatUseCase,
            isAnonymousModeUseCase = isAnonymousModeUseCase,
            closeChatPreviewUseCase = closeChatPreviewUseCase,
            createNewImageUriUseCase = createNewImageUriUseCase,
            monitorJoiningChatUseCase = monitorJoiningChatUseCase,
            monitorLeavingChatUseCase = monitorLeavingChatUseCase,
            applicationScope = CoroutineScope(testDispatcher),
            sendLocationMessageUseCase = sendLocationMessageUseCase,
            monitorChatPendingChangesUseCase = monitorChatPendingChangesUseCase,
            addReactionUseCase = addReactionUseCase,
            getChatMessageUseCase = getChatMessageUseCase,
            deleteReactionUseCase = deleteReactionUseCase,
            sendGiphyMessageUseCase = sendGiphyMessageUseCase,
            attachContactsUseCase = attachContactsUseCase,
            getParticipantFullNameUseCase = getParticipantFullNameUseCase,
            participantNameMapper = participantNameMapper,
            getUserUseCase = getUserUseCase,
            forwardMessagesUseCase = forwardMessagesUseCase,
            forwardMessagesResultMapper = forwardMessagesResultMapper,
            attachNodeUseCase = attachNodeUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            addNodeType = addNodeType,
            deleteMessagesUseCase = deleteMessagesUseCase,
            editMessageUseCase = editMessageUseCase,
            editLocationMessageUseCase = editLocationMessageUseCase,
            getChatFromContactMessagesUseCase = getChatFromContactMessagesUseCase,
            getCacheFileUseCase = getCacheFileUseCase,
            recordAudioUseCase = recordAudioUseCase,
            deleteFileUseCase = deleteFileUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            leaveChatUseCase = leaveChatUseCase,
            monitorLeaveChatUseCase = monitorLeaveChatUseCase,
            broadcastChatArchivedUseCase = broadcastChatArchivedUseCase,
            setUsersCallLimitRemindersUseCase = setUsersCallLimitRemindersUseCase,
            getUsersCallLimitRemindersUseCase = getUsersCallLimitRemindersUseCase,
            broadcastUpgradeDialogClosedUseCase = broadcastUpgradeDialogClosedUseCase,
            areTransfersPausedUseCase = areTransfersPausedUseCase,
            pauseTransfersQueueUseCase = pauseTransfersQueueUseCase,
            actionFactories = setOf(),
            scannerHandler = scannerHandler,
            getStringFromStringResMapper = getStringFromStringResMapper,
        )
    }

    @Test
    fun `test that the option returned by getUsersCallLimitRemindersUseCase is set as enabled`() =
        runTest {
            initTestClass()
            whenever(getUsersCallLimitRemindersUseCase()).thenReturn(flowOf(UsersCallLimitReminders.Enabled))
            underTest.state.map { it.usersCallLimitReminders }.distinctUntilChanged().test {
                testScheduler.advanceUntilIdle()
                assertThat(awaitItem()).isEqualTo(UsersCallLimitReminders.Enabled)
            }
        }

    @Test
    fun `test that setUsersCallLimitRemindersUseCase calls the set use case with the correct value`() =
        runTest {
            initTestClass()
            underTest.setUsersCallLimitReminder(false)
            testScheduler.advanceUntilIdle()
            verify(setUsersCallLimitRemindersUseCase).invoke(UsersCallLimitReminders.Disabled)
        }

    @Test
    fun `test that title update when we passing the chatId`() = runTest {
        val chatRoom = mock<ChatRoom> {
            on { title } doReturn "title"
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
        }
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().title).isEqualTo("title")
        }
    }

    @ParameterizedTest(name = "with with isPublic {0}")
    @ValueSource(booleans = [true, false])
    fun `test that is private chat updates correctly when call sdk returns chat room  and room is group chat`(
        isPublic: Boolean,
    ) = runTest {
        val chatRoom = mock<ChatRoom> {
            on { this.isPublic } doReturn isPublic
            on { isGroup } doReturn true
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
        }
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().isPrivateChat).isEqualTo(!isPublic)
        }
    }

    @Test
    fun `test that is private chat when call sdk returns chat room is not group chat`() = runTest {
        val chatRoom = mock<ChatRoom> {
            on { isGroup } doReturn false
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
        }
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().isPrivateChat).isTrue()
        }
    }

    @Test
    fun `test that user chat status is not updated if chat is group`() =
        runTest {
            val chatRoom = mock<ChatRoom> {
                on { isGroup } doReturn true
                on { ownPrivilege } doReturn ChatRoomPermission.Moderator
            }
            whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
            initTestClass()
            verifyNoInteractions(getUserOnlineStatusByHandleUseCase)
        }

    @ParameterizedTest(name = " is {0}")
    @MethodSource("provideUserChatStatusParameters")
    fun `test that user chat status is updated if get user chat status by chat`(
        expectedUserChatStatus: UserChatStatus,
    ) = runTest {
        val chatRoom = mock<ChatRoom> {
            on { isGroup } doReturn false
            on { peerHandlesList } doReturn listOf(userHandle)
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
        }
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        whenever(getUserOnlineStatusByHandleUseCase(userHandle)).thenReturn(expectedUserChatStatus)
        initTestClass()
        testScheduler.advanceUntilIdle()
        underTest.state.test {
            assertThat(awaitItem().userChatStatus).isEqualTo(expectedUserChatStatus)
        }
    }

    @ParameterizedTest(name = " is {0} and chat room update with state {1}")
    @MethodSource("provideUserChatStatusParameters")
    fun `test that user chat status updates when status`(
        firstUserChatStatus: UserChatStatus,
        updatedUserChatStatus: UserChatStatus,
    ) = runTest {
        val chatRoom = mock<ChatRoom> {
            on { isGroup } doReturn false
            on { peerHandlesList } doReturn listOf(userHandle)
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
        }
        val updateFlow = MutableSharedFlow<UserChatStatus>()
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        whenever(isChatNotificationMuteUseCase(chatId)).thenReturn(false)
        whenever(getUserOnlineStatusByHandleUseCase(userHandle)).thenReturn(firstUserChatStatus)
        whenever(monitorUserChatStatusByHandleUseCase(userHandle)).thenReturn(updateFlow)
        initTestClass()
        testScheduler.advanceUntilIdle()
        underTest.state.test {
            assertThat(awaitItem().userChatStatus).isEqualTo(firstUserChatStatus)
        }
        updateFlow.emit(updatedUserChatStatus)
        testScheduler.advanceUntilIdle()
        underTest.state.test {
            assertThat(awaitItem().userChatStatus).isEqualTo(updatedUserChatStatus)
        }
    }

    @Test
    fun `test that notification mute icon is shown when mute is enabled`() = runTest {
        whenever(isChatNotificationMuteUseCase(chatId)).thenReturn(true)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().isChatNotificationMute).isTrue()
        }
    }

    @Test
    fun `test that notification mute icon is hidden when mute is disabled`() = runTest {
        whenever(isChatNotificationMuteUseCase(chatId)).thenReturn(false)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().isChatNotificationMute).isFalse()
        }
    }

    @Test
    fun `test that title update when chat room update with title change`() = runTest {
        val chatRoom = mock<ChatRoom> {
            on { title } doReturn "title"
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
        }
        val updateFlow = MutableSharedFlow<ChatRoom>()
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        whenever(monitorChatRoomUpdatesUseCase(chatId)).thenReturn(updateFlow)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().title).isEqualTo("title")
        }
        val newTitle = "new title"
        val newChatRoom = mock<ChatRoom> {
            on { title } doReturn newTitle
            on { changes } doReturn listOf(ChatRoomChange.Title)
        }
        updateFlow.emit(newChatRoom)
        underTest.state.test {
            assertThat(awaitItem().title).isEqualTo(newTitle)
        }
    }

    @Test
    fun `test that mute icon visibility updates when push notification setting updates`() =
        runTest {
            val pushNotificationSettingFlow = MutableSharedFlow<Boolean>()
            val chatRoomUpdate = MutableSharedFlow<ChatRoom>()
            whenever(isChatNotificationMuteUseCase(chatId)).thenReturn(true)
            whenever(monitorUpdatePushNotificationSettingsUseCase()).thenReturn(
                pushNotificationSettingFlow
            )
            whenever(monitorChatRoomUpdatesUseCase(chatId)).thenReturn(chatRoomUpdate)

            initTestClass()
            underTest.state.test {
                assertThat(awaitItem().isChatNotificationMute).isTrue()
            }

            whenever(isChatNotificationMuteUseCase(chatId)).thenReturn(false)
            pushNotificationSettingFlow.emit(true)
            underTest.state.test {
                assertThat(awaitItem().isChatNotificationMute).isFalse()
            }
        }

    @Test
    fun `test that initial mute icon visibility is always false when it fails to fetch the chat notification mute status`() =
        runTest {
            whenever(isChatNotificationMuteUseCase(chatId))
                .thenAnswer { throw Exception("failure to get chat notification mute status") }

            initTestClass()

            underTest.state.test {
                assertThat(awaitItem().isChatNotificationMute).isFalse()
            }
        }

    @Test
    fun `test that private room update when chat room update with mode change`() = runTest {
        val chatRoom = mock<ChatRoom> {
            on { isGroup } doReturn true
            on { isPublic } doReturn true
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
        }
        val updateFlow = MutableSharedFlow<ChatRoom>()
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        whenever(monitorChatRoomUpdatesUseCase(chatId)).thenReturn(updateFlow)
        whenever(isChatNotificationMuteUseCase(chatId)).thenReturn(false)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().isPrivateChat).isFalse()
        }
        val newChatRoom = mock<ChatRoom> {
            on { isPublic } doReturn false
            on { changes } doReturn listOf(ChatRoomChange.ChatMode)
        }
        updateFlow.emit(newChatRoom)
        underTest.state.test {
            assertThat(awaitItem().isPrivateChat).isTrue()
        }
    }

    @ParameterizedTest(name = " returns {0}")
    @NullSource
    @ValueSource(longs = [123L])
    fun `test that is participating in a call has correct state if use case`(
        isParticipatingInChatCall: Long?,
    ) = runTest {
        val flow = MutableSharedFlow<List<ChatCall>>()
        whenever(monitorParticipatingInACallInOtherChatsUseCase(any())).thenReturn(flow)
        initTestClass()
        val expected = isParticipatingInChatCall?.let {
            mock<ChatCall> {
                on { chatId } doReturn isParticipatingInChatCall
            }
        }?.let { listOf(it) } ?: emptyList()
        flow.emit(expected)
        underTest.state.test {
            assertThat(awaitItem().callsInOtherChats).isEqualTo(expected)
        }
    }

    @ParameterizedTest(name = " if chat is group property returns {0}")
    @EnumSource(ChatRoomPermission::class)
    fun `test that my permission is updated correctly in state`(
        permission: ChatRoomPermission,
    ) = runTest {
        val chatRoom = mock<ChatRoom> {
            on { peerHandlesList } doReturn listOf(userHandle)
            on { ownPrivilege } doReturn permission
        }
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().myPermission).isEqualTo(permission)
        }
    }

    @ParameterizedTest(name = " if chat is preview property returns {0}")
    @ValueSource(booleans = [true, false])
    fun `test that is preview has correct state`(
        isPreviewResult: Boolean,
    ) = runTest {
        val chatRoom = mock<ChatRoom> {
            on { peerHandlesList } doReturn listOf(userHandle)
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
            on { isPreview } doReturn isPreviewResult
        }
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().isPreviewMode).isEqualTo(isPreviewResult)
        }
    }

    @ParameterizedTest(name = " if chat is group property returns {0}")
    @ValueSource(booleans = [true, false])
    fun `test that is group has correct state`(
        isGroupResult: Boolean,
    ) = runTest {
        val chatRoom = mock<ChatRoom> {
            on { isGroup } doReturn isGroupResult
            on { peerHandlesList } doReturn listOf(userHandle)
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
        }
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().isGroup).isEqualTo(isGroupResult)
        }
    }

    @ParameterizedTest(name = " emits {0}")
    @ValueSource(booleans = [true, false])
    fun `test that has a call in this chat has correct state if the flow`(
        hasACallInThisChat: Boolean,
    ) = runTest {
        val flow = MutableSharedFlow<ChatCall?>()
        val call = if (hasACallInThisChat) mock<ChatCall>() else null
        whenever(monitorCallInChatUseCase(chatId)).thenReturn(flow)
        initTestClass()
        flow.emit(call)
        underTest.state.test {
            assertThat(awaitItem().callInThisChat).isEqualTo(call)
        }
    }

    @ParameterizedTest(name = " with own privilege change {0}")
    @EnumSource(ChatRoomPermission::class)
    fun `test that my permission and is active is updated when chat room updates`(
        newPermission: ChatRoomPermission,
    ) = runTest {
        val chatRoom = mock<ChatRoom> {
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
            on { isActive } doReturn true
        }
        val updateFlow = MutableSharedFlow<ChatRoom>()
        val expectedIsActive = newPermission in listOf(
            ChatRoomPermission.Moderator,
            ChatRoomPermission.Standard,
            ChatRoomPermission.ReadOnly,
        )
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        whenever(monitorChatRoomUpdatesUseCase(chatId)).thenReturn(updateFlow)
        initTestClass()
        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.myPermission).isEqualTo(ChatRoomPermission.Moderator)
            assertThat(actual.isActive).isTrue()
        }
        val newChatRoom = mock<ChatRoom> {
            on { ownPrivilege } doReturn newPermission
            on { changes } doReturn listOf(ChatRoomChange.OwnPrivilege)
            on { isActive } doReturn expectedIsActive
        }
        updateFlow.emit(newChatRoom)
        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.myPermission).isEqualTo(newPermission)
            assertThat(actual.isActive).isEqualTo(expectedIsActive)
        }
    }

    @ParameterizedTest(name = " with storage state {0}")
    @EnumSource(StorageState::class)
    fun `test that storage state is updated when getting new storage state event`(
        state: StorageState,
    ) = runTest {
        val updateFlow = MutableStateFlow(
            StorageStateEvent(
                handle = 1L,
                storageState = StorageState.Unknown  // initial state is [StorageState.Unknown]
            )
        )
        whenever(monitorStorageStateEventUseCase()).thenReturn(updateFlow)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().storageState).isEqualTo(StorageState.Unknown)
        }
        updateFlow.emit(
            StorageStateEvent(
                handle = 1L,
                storageState = state
            )
        )
        underTest.state.test {
            assertThat(awaitItem().storageState).isEqualTo(state)
        }
    }

    @ParameterizedTest(name = " with value {0}")
    @ValueSource(booleans = [true, false])
    fun `test that is open invite is updated when there is a valid chat room`(
        expectedOpenInvite: Boolean,
    ) = runTest {
        val chatRoom = mock<ChatRoom> {
            on { isOpenInvite } doReturn expectedOpenInvite
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
        }
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().isOpenInvite).isEqualTo(expectedOpenInvite)
        }
    }

    @ParameterizedTest(name = " with value {0}")
    @ValueSource(booleans = [true, false])
    fun `test that is active is updated when there is a valid chat room`(
        expectedIsActive: Boolean,
    ) = runTest {
        val chatRoom = mock<ChatRoom> {
            on { isActive } doReturn expectedIsActive
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
        }
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().isActive).isEqualTo(expectedIsActive)
        }
    }

    @ParameterizedTest(name = " with value {0}")
    @ValueSource(booleans = [true, false])
    fun `test that is open invite update when chat room update with open invite change`(
        expectedOpenInvite: Boolean,
    ) = runTest {
        val chatRoom = mock<ChatRoom> {
            on { isOpenInvite } doReturn true
            on { ownPrivilege } doReturn ChatRoomPermission.Standard
        }
        val updateFlow = MutableSharedFlow<ChatRoom>()
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        whenever(monitorChatRoomUpdatesUseCase(chatId)).thenReturn(updateFlow)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().isOpenInvite).isTrue()
        }
        val newChatRoom = mock<ChatRoom> {
            on { isOpenInvite } doReturn expectedOpenInvite
            on { changes } doReturn listOf(ChatRoomChange.OpenInvite)
        }
        updateFlow.emit(newChatRoom)
        underTest.state.test {
            assertThat(awaitItem().isOpenInvite).isEqualTo(expectedOpenInvite)
        }
    }

    @Test
    fun `test that my permission and is active update when chat room update with closed change`() =
        runTest {
            val chatRoom = mock<ChatRoom> {
                on { isActive } doReturn true
                on { ownPrivilege } doReturn ChatRoomPermission.Standard
            }
            val updateFlow = MutableSharedFlow<ChatRoom>()
            whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
            whenever(monitorChatRoomUpdatesUseCase(chatId)).thenReturn(updateFlow)
            initTestClass()
            underTest.state.test {
                val actual = awaitItem()
                assertThat(actual.isActive).isTrue()
                assertThat(actual.myPermission).isEqualTo(ChatRoomPermission.Standard)
            }
            val newChatRoom = mock<ChatRoom> {
                on { isActive } doReturn false
                on { ownPrivilege } doReturn ChatRoomPermission.Removed
                on { changes } doReturn listOf(ChatRoomChange.Closed)
            }
            updateFlow.emit(newChatRoom)
            underTest.state.test {
                val actual = awaitItem()
                assertThat(actual.isActive).isFalse()
                assertThat(actual.myPermission).isEqualTo(ChatRoomPermission.Removed)
            }
        }

    @ParameterizedTest(name = " with value {0}")
    @ValueSource(booleans = [true, false])
    fun `test that is archived update when chat room update with archived change`(
        expectedArchived: Boolean,
    ) = runTest {
        val chatRoom = mock<ChatRoom> {
            on { isArchived } doReturn false
            on { ownPrivilege } doReturn ChatRoomPermission.Standard
        }
        val updateFlow = MutableSharedFlow<ChatRoom>()
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        whenever(monitorChatRoomUpdatesUseCase(chatId)).thenReturn(updateFlow)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().isArchived).isFalse()
        }
        val newChatRoom = mock<ChatRoom> {
            on { isArchived } doReturn expectedArchived
            on { changes } doReturn listOf(ChatRoomChange.Archive)
        }
        updateFlow.emit(newChatRoom)
        advanceUntilIdle()
        underTest.state.test {
            assertThat(awaitItem().isArchived).isEqualTo(expectedArchived)
        }
    }

    @ParameterizedTest(name = " with isArchived {0}")
    @ValueSource(booleans = [true, false])
    fun `test that archive update when we passing the chatId`(
        isArchived: Boolean,
    ) = runTest {
        val chatRoom = mock<ChatRoom> {
            on { this.isArchived } doReturn isArchived
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
        }
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().isArchived).isEqualTo(isArchived)
        }
    }

    @Test
    fun `test that last green is requested if the chat is 1to1 and the contact status is not online`() =
        runTest {
            val chatRoom = mock<ChatRoom> {
                on { isGroup } doReturn false
                on { peerHandlesList } doReturn listOf(userHandle)
                on { ownPrivilege } doReturn ChatRoomPermission.Moderator
            }
            whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
            whenever(getUserOnlineStatusByHandleUseCase(userHandle)).thenReturn(UserChatStatus.Away)
            initTestClass()
            testScheduler.advanceUntilIdle()
            underTest.state.test {
                assertThat(awaitItem().userChatStatus).isEqualTo(UserChatStatus.Away)
                verify(requestUserLastGreenUseCase).invoke(userHandle)
            }
        }

    @Test
    fun `test that last green is not requested and null if the chat is 1to1 and the contact status is online`() =
        runTest {
            val chatRoom = mock<ChatRoom> {
                on { isGroup } doReturn false
                on { peerHandlesList } doReturn listOf(userHandle)
                on { ownPrivilege } doReturn ChatRoomPermission.Moderator
            }
            whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
            whenever(getUserOnlineStatusByHandleUseCase(userHandle)).thenReturn(UserChatStatus.Online)
            initTestClass()
            testScheduler.advanceUntilIdle()
            underTest.state.test {
                val actual = awaitItem()
                assertThat(actual.userChatStatus).isEqualTo(UserChatStatus.Online)
                assertThat(actual.userLastGreen).isNull()
                verifyNoInteractions(requestUserLastGreenUseCase)
            }
        }

    @Test
    fun `test that contact last green is updated if new update is received and user chat status is not online`() =
        runTest {
            val chatRoom = mock<ChatRoom> {
                on { isGroup } doReturn false
                on { peerHandlesList } doReturn listOf(userHandle)
                on { ownPrivilege } doReturn ChatRoomPermission.Moderator
            }
            val updateFlow = MutableSharedFlow<Int>()
            whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
            whenever(getUserOnlineStatusByHandleUseCase(userHandle)).thenReturn(UserChatStatus.Away)
            whenever(monitorUserLastGreenUpdatesUseCase(userHandle)).thenReturn(updateFlow)
            initTestClass()
            testScheduler.advanceUntilIdle()
            underTest.state.test {
                val actual = awaitItem()
                assertThat(actual.userChatStatus).isEqualTo(UserChatStatus.Away)
                assertThat(actual.userLastGreen).isNull()
            }
            val lastGreen = 5
            updateFlow.emit(lastGreen)
            underTest.state.test {
                assertThat(awaitItem().userLastGreen).isEqualTo(lastGreen)
            }
            val newLastGreen = 10
            updateFlow.emit(newLastGreen)
            underTest.state.test {
                assertThat(awaitItem().userLastGreen).isEqualTo(newLastGreen)
            }
        }

    @Test
    fun `test that contact last green is not updated if new update is received and user chat status is online`() =
        runTest {
            val chatRoom = mock<ChatRoom> {
                on { isGroup } doReturn false
                on { peerHandlesList } doReturn listOf(userHandle)
                on { ownPrivilege } doReturn ChatRoomPermission.Moderator
            }
            val lastGreenFlow = MutableSharedFlow<Int>()
            val statusFlow = MutableSharedFlow<UserChatStatus>()
            whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
            whenever(getUserOnlineStatusByHandleUseCase(userHandle)).thenReturn(UserChatStatus.Away)
            whenever(monitorUserChatStatusByHandleUseCase(userHandle)).thenReturn(statusFlow)
            whenever(monitorUserLastGreenUpdatesUseCase(userHandle)).thenReturn(lastGreenFlow)
            initTestClass()
            testScheduler.advanceUntilIdle()
            underTest.state.test {
                val actual = awaitItem()
                assertThat(actual.userChatStatus).isEqualTo(UserChatStatus.Away)
                assertThat(actual.userLastGreen).isNull()
            }
            val newStatus = UserChatStatus.Online
            statusFlow.emit(newStatus)
            underTest.state.test {
                val actual = awaitItem()
                assertThat(actual.userChatStatus).isEqualTo(newStatus)
                assertThat(actual.userLastGreen).isNull()
            }
            val lastGreen = 5
            lastGreenFlow.emit(lastGreen)
            underTest.state.test {
                assertThat(awaitItem().userLastGreen).isNull()
            }
        }

    @Test
    fun `test that users typing update correctly when chat room update with user typing change`() =
        runTest {
            val myUserHandle = 1L
            val userHandle = 123L
            val expectedFirstName = "firstName"
            val chatRoom = mock<ChatRoom> {
                on { changes } doReturn listOf(ChatRoomChange.UserTyping)
                on { ownPrivilege } doReturn ChatRoomPermission.Moderator
                on { userTyping } doReturn userHandle
            }
            whenever(getParticipantFirstNameUseCase(userHandle)).thenReturn(expectedFirstName)
            whenever(getMyUserHandleUseCase()).thenReturn(myUserHandle)
            val updateFlow = MutableSharedFlow<ChatRoom>()
            whenever(monitorChatRoomUpdatesUseCase(chatId)).thenReturn(updateFlow)
            initTestClass()
            underTest.state.test {
                assertThat(awaitItem().usersTyping).isEmpty()
            }
            updateFlow.emit(chatRoom)
            underTest.state.test {
                assertThat(awaitItem().usersTyping).isEqualTo(listOf(expectedFirstName))
            }
            // test reset after 5s
            advanceTimeBy(6000L)
            underTest.state.test {
                assertThat(awaitItem().usersTyping).isEmpty()
            }
        }

    @Test
    fun `test that users typing update correctly when chat room update with user stop typing`() =
        runTest {
            val myUserHandle = 1L
            val userHandle = 123L
            val expectedFirstName = "firstName"
            val chatRoom = mock<ChatRoom> {
                on { changes } doReturn listOf(ChatRoomChange.UserTyping)
                on { ownPrivilege } doReturn ChatRoomPermission.Moderator
                on { userTyping } doReturn userHandle
            }
            whenever(getParticipantFirstNameUseCase(userHandle)).thenReturn(expectedFirstName)
            whenever(getMyUserHandleUseCase()).thenReturn(myUserHandle)
            val updateFlow = MutableSharedFlow<ChatRoom>()
            whenever(monitorChatRoomUpdatesUseCase(chatId)).thenReturn(updateFlow)
            initTestClass()
            underTest.state.test {
                assertThat(awaitItem().usersTyping).isEmpty()
            }
            val stopTypingChatRoom = mock<ChatRoom> {
                on { changes } doReturn listOf(ChatRoomChange.UserStopTyping)
                on { ownPrivilege } doReturn ChatRoomPermission.Moderator
                on { userTyping } doReturn userHandle
            }
            updateFlow.emit(chatRoom)
            underTest.state.test {
                assertThat(awaitItem().usersTyping).isEqualTo(listOf(expectedFirstName))
            }
            updateFlow.emit(stopTypingChatRoom)
            underTest.state.test {
                assertThat(awaitItem().usersTyping).isEmpty()
            }
        }

    private fun provideUserChatStatusParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(UserChatStatus.Offline, UserChatStatus.Away),
        Arguments.of(UserChatStatus.Away, UserChatStatus.Online),
        Arguments.of(UserChatStatus.Online, UserChatStatus.Busy),
        Arguments.of(UserChatStatus.Busy, UserChatStatus.Invalid),
        Arguments.of(UserChatStatus.Invalid, UserChatStatus.Offline),
    )

    @ParameterizedTest(name = " {0} when isChatStatusConnectedForCallUseCase is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that chat connected state is `(connected: Boolean) = runTest {
        whenever(isChatStatusConnectedForCallUseCase(chatId)).thenReturn(connected)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().isConnected).isEqualTo(connected)
        }
    }

    @ParameterizedTest(name = " {0}")
    @MethodSource("provideChatConnectionStatusParameters")
    fun `test that chat connected state is false when monitor chat connection status returns`(
        chatConnectionStatus: ChatConnectionStatus,
        isChatConnected: Boolean,
    ) =
        runTest {
            val updateFlow = MutableStateFlow(
                ChatConnectionState(
                    chatId = chatId,
                    chatConnectionStatus = chatConnectionStatus
                )
            )
            whenever(monitorChatConnectionStateUseCase()).thenReturn(updateFlow)
            whenever(isChatStatusConnectedForCallUseCase(chatId)).thenReturn(true)
            initTestClass()
            updateFlow.emit(
                ChatConnectionState(
                    chatId = chatId,
                    chatConnectionStatus = chatConnectionStatus
                )
            )
            underTest.state.test {
                assertThat(awaitItem().isConnected).isEqualTo(isChatConnected)
            }
        }


    private fun provideChatConnectionStatusParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(ChatConnectionStatus.Offline, false),
        Arguments.of(ChatConnectionStatus.InProgress, false),
        Arguments.of(ChatConnectionStatus.Logging, false),
        Arguments.of(ChatConnectionStatus.Unknown, false),
        Arguments.of(ChatConnectionStatus.Online, true),
    )

    @Test
    fun `test that chat connected state is false when network connectivity is false`() = runTest {
        val updateFlow = MutableStateFlow(true)
        whenever(monitorConnectivityUseCase()).thenReturn(updateFlow)
        whenever(isChatStatusConnectedForCallUseCase(chatId)).thenReturn(true)

        initTestClass()
        updateFlow.emit(false)
        underTest.state.test {
            assertThat(awaitItem().isConnected).isFalse()
        }
    }

    @Test
    fun `test that chat connected state is true when network connectivity is false and isChatStatusConnectedForCall is true`() =
        runTest {
            val updateFlow = MutableStateFlow(true)
            whenever(monitorConnectivityUseCase()).thenReturn(updateFlow)
            whenever(isChatStatusConnectedForCallUseCase(chatId)).thenReturn(true)

            initTestClass()
            updateFlow.emit(true)
            underTest.state.test {
                assertThat(awaitItem().isConnected).isTrue()
            }
        }

    @Test
    fun `test that ui state is updated when get scheduled meeting by chatId succeeds`() = runTest {
        val invalidHandle = -1L
        val expectedScheduledMeeting = ChatScheduledMeeting(parentSchedId = invalidHandle)
        whenever(getScheduledMeetingByChatUseCase(chatId)).thenReturn(
            listOf(
                expectedScheduledMeeting
            )
        )
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().scheduledMeeting).isEqualTo(expectedScheduledMeeting)
        }
    }

    @Test
    fun `test that scheduled meeting in ui state is null when get scheduled meeting by chatId fails`() =
        runTest {
            whenever(getScheduledMeetingByChatUseCase(any())).thenAnswer {
                throw Exception("get scheduled meeting by chat failed")
            }

            initTestClass()
            underTest.state.test {
                assertThat(awaitItem().scheduledMeeting).isNull()
            }
        }

    @Test
    fun `test that hasAnyContact updates when a new flag is received`() = runTest {
        val updateFlow = MutableSharedFlow<Boolean>()
        whenever(monitorHasAnyContactUseCase()).thenReturn(updateFlow)
        val chatRoom = mock<ChatRoom> {
            on { isGroup } doReturn true
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
            on { peerHandlesList } doReturn listOf(1L, 2L, 3L)
        }
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().hasAnyContact).isFalse()
        }
        updateFlow.emit(true)
        underTest.state.test {
            assertThat(awaitItem().hasAnyContact).isTrue()
        }
        updateFlow.emit(true)
        underTest.state.test {
            assertThat(awaitItem().hasAnyContact).isTrue()
        }
        updateFlow.emit(false)
        underTest.state.test {
            assertThat(awaitItem().hasAnyContact).isFalse()
        }
    }

    @Test
    fun `test that custom subtitle list is not updated if chat is not a group`() = runTest {
        val chatRoom = mock<ChatRoom> {
            on { isGroup } doReturn false
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
        }
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initTestClass()
        testScheduler.advanceUntilIdle()
        underTest.state.test {
            assertThat(awaitItem().customSubtitleList).isNull()
            verifyNoInteractions(getCustomSubtitleListUseCase)
        }
    }

    @ParameterizedTest(name = " is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that custom subtitle list is updated when getting the chat and has custom title`(
        custom: Boolean,
    ) = runTest {
        val userHandles = listOf(1L, 2L, 3L)
        val customSubtitleList = listOf("A", "B", "C")
        val chatRoom = mock<ChatRoom> {
            on { it.chatId } doReturn chatId
            on { isGroup } doReturn true
            on { hasCustomTitle } doReturn custom
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
            on { peerHandlesList } doReturn userHandles
        }
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        whenever(getCustomSubtitleListUseCase(chatId, userHandles))
            .thenReturn(customSubtitleList)
        initTestClass()
        testScheduler.advanceUntilIdle()
        underTest.state.test {
            if (custom) {
                assertThat(awaitItem().customSubtitleList).isEqualTo(customSubtitleList)
            } else {
                assertThat(awaitItem().customSubtitleList).isNull()
                verifyNoInteractions(getCustomSubtitleListUseCase)
            }
        }
    }

    @ParameterizedTest(name = " change {0}")
    @EnumSource(value = ChatRoomChange::class, names = ["Title", "Participants"])
    fun `test that custom subtitle is updated when chat room is a group and updates with`(
        change: ChatRoomChange,
    ) = runTest {
        val userHandles = listOf(1L, 2L, 3L)
        val customSubtitleList = listOf("A", "B", "C")
        val updatedCustomSubtitleList = listOf("X", "B", "C")
        val chatRoom = mock<ChatRoom> {
            on { it.chatId } doReturn chatId
            on { isGroup } doReturn true
            on { hasCustomTitle } doReturn true
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
            on { peerHandlesList } doReturn userHandles
            on { isPreview } doReturn false
        }
        val updateFlow = MutableSharedFlow<ChatRoom>()
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        whenever(monitorChatRoomUpdatesUseCase(chatId)).thenReturn(updateFlow)
        whenever(getCustomSubtitleListUseCase(chatId, userHandles))
            .thenReturn(customSubtitleList)
            .thenReturn(updatedCustomSubtitleList)
        initTestClass()
        testScheduler.advanceUntilIdle()
        underTest.state.test {
            assertThat(awaitItem().customSubtitleList).isEqualTo(customSubtitleList)
        }
        val newChatRoom = mock<ChatRoom> {
            on { it.chatId } doReturn chatId
            on { isGroup } doReturn true
            on { hasCustomTitle } doReturn true
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
            on { changes } doReturn listOf(change)
            on { peerHandlesList } doReturn userHandles
            on { isPreview } doReturn false
        }
        updateFlow.emit(newChatRoom)
        testScheduler.advanceUntilIdle()
        underTest.state.test {
            assertThat(awaitItem().customSubtitleList).isEqualTo(updatedCustomSubtitleList)
        }
    }

    @ParameterizedTest(name = " my permission is {0}, is group is {1}, and peer count is {2}")
    @MethodSource("providePeerCountParameters")
    fun `test that participants count is updated when getting the chat and`(
        myPermission: ChatRoomPermission,
        group: Boolean,
        participantsCount: Long,
    ) = runTest {
        val chatRoom = mock<ChatRoom> {
            on { ownPrivilege } doReturn myPermission
            on { isGroup } doReturn group
            on { peerCount } doReturn participantsCount
        }
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initTestClass()
        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.myPermission).isEqualTo(myPermission)
            assertThat(actual.isGroup).isEqualTo(group)
            assertThat(actual.participantsCount).isEqualTo(chatRoom.getNumberParticipants())
        }
    }

    @ParameterizedTest(name = " with change {0}, is group is {1}")
    @MethodSource("providePeerCountUpdateParameters")
    fun `test that participants count is updated when chat room updates`(
        change: ChatRoomChange,
        group: Boolean,
    ) = runTest {
        val count = if (group) 10L else 0L
        val chatRoom = mock<ChatRoom> {
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
            on { isGroup } doReturn group
            on { peerCount } doReturn count
        }
        val updateFlow = MutableSharedFlow<ChatRoom>()
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        whenever(monitorChatRoomUpdatesUseCase(chatId)).thenReturn(updateFlow)
        initTestClass()
        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.myPermission).isEqualTo(ChatRoomPermission.Moderator)
            assertThat(actual.isGroup).isEqualTo(group)
            assertThat(actual.participantsCount).isEqualTo(chatRoom.getNumberParticipants())
        }
        val permission =
            if (change == ChatRoomChange.OwnPrivilege || change == ChatRoomChange.Closed) ChatRoomPermission.Removed
            else chatRoom.ownPrivilege
        val newCount =
            if (change == ChatRoomChange.Participants) count - 1
            else count
        val newChatRoom = mock<ChatRoom> {
            on { ownPrivilege } doReturn permission
            on { changes } doReturn listOf(change)
            on { isGroup } doReturn group
            on { peerCount } doReturn newCount
        }
        updateFlow.emit(newChatRoom)
        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.myPermission).isEqualTo(permission)
            assertThat(actual.isGroup).isEqualTo(group)
            assertThat(actual.participantsCount).isEqualTo(newChatRoom.getNumberParticipants())
        }
    }

    @Test
    fun `test that monitor all contacts participant in the chat call when monitor chat room updates with participant change`() =
        runTest {
            val flow = MutableSharedFlow<ChatRoom>()
            whenever(monitorChatRoomUpdatesUseCase(chatId)).thenReturn(flow)
            val newChatRoom = mock<ChatRoom> {
                on { ownPrivilege } doReturn ChatRoomPermission.Moderator
                on { peerHandlesList } doReturn listOf(1L, 2L, 3L)
                on { changes } doReturn listOf(ChatRoomChange.Participants)
            }
            initTestClass()
            flow.emit(newChatRoom)
            verify(monitorAllContactParticipantsInChatUseCase).invoke(newChatRoom.peerHandlesList)
        }

    @ParameterizedTest(name = "emits {0}")
    @ValueSource(booleans = [true, false])
    fun `test that all contacts participant in the chat update correctly when monitor new contact`(
        areAllContactParticipantsInChat: Boolean,
    ) = runTest {
        val chatRoom = mock<ChatRoom> {
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
            on { peerHandlesList } doReturn listOf(1L, 2L)
            on { isGroup } doReturn true
        }
        val flow = MutableSharedFlow<Boolean>()
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        whenever(monitorAllContactParticipantsInChatUseCase(any())).thenReturn(flow)
        initTestClass()
        flow.emit(areAllContactParticipantsInChat)
        underTest.state.test {
            assertThat(awaitItem().allContactsParticipateInChat).isEqualTo(
                areAllContactParticipantsInChat
            )
        }
    }

    @Test
    fun `test that multiple contacts are added to chat room`() = runTest {
        val contactList = listOf("user1", "user2")
        val chatRequest: ChatRequest = mock()
        val resultList = listOf(
            Result.success(chatRequest),
            Result.success(chatRequest),
        )

        whenever(inviteToChatUseCase(chatId = chatId, contactList = contactList)).thenReturn(
            resultList
        )
        whenever(inviteParticipantResultMapper(resultList)).thenReturn(
            InviteContactToChatResult.MultipleContactsAdded(
                success = 2
            )
        )

        initTestClass()
        underTest.inviteContactsToChat(chatId, contactList)
        underTest.state.test {
            val result = ((awaitItem().infoToShowEvent as StateEventWithContentTriggered)
                .content as InfoToShow.InviteContactResult).result
            assertThat(result).isInstanceOf(InviteContactToChatResult.MultipleContactsAdded::class.java)
        }
    }

    @Test
    fun `test that one contact is added to chat room`() = runTest {
        val contactList = listOf("user1")
        val chatRequest: ChatRequest = mock()
        val resultList = listOf(Result.success(chatRequest))

        whenever(inviteToChatUseCase(chatId = chatId, contactList = contactList)).thenReturn(
            resultList
        )
        whenever(inviteParticipantResultMapper(resultList)).thenReturn(InviteContactToChatResult.OnlyOneContactAdded)

        initTestClass()
        underTest.inviteContactsToChat(chatId, contactList)
        underTest.state.test {
            val result = ((awaitItem().infoToShowEvent as StateEventWithContentTriggered)
                .content as InfoToShow.InviteContactResult).result
            assertThat(result).isInstanceOf(InviteContactToChatResult.OnlyOneContactAdded::class.java)
        }
    }

    @Test
    fun `test that add one contact fails to chat room due to already exists`() = runTest {
        val contactList = listOf("myself")
        val chatRequest: ChatRequest = mock()
        val resultList = listOf(
            Result.success(chatRequest),
            Result.failure(
                ParticipantAlreadyExistsException()
            )
        )

        whenever(inviteToChatUseCase(chatId, contactList)).thenReturn(resultList)
        whenever(inviteParticipantResultMapper(resultList)).thenReturn(
            InviteContactToChatResult.AlreadyExistsError
        )

        initTestClass()
        underTest.inviteContactsToChat(chatId, contactList)
        underTest.state.test {
            val result = ((awaitItem().infoToShowEvent as StateEventWithContentTriggered)
                .content as InfoToShow.InviteContactResult).result
            assertThat(result).isInstanceOf(InviteContactToChatResult.AlreadyExistsError::class.java)
        }
    }

    @Test
    fun `test that general error is shown when add one contact fails to chat room due to general error`() =
        runTest {
            val contactList = listOf("user1", "user2", "user3")
            val chatRequest: ChatRequest = mock()
            val resultList = listOf(
                Result.success(chatRequest),
                Result.failure(
                    MegaException(
                        errorCode = MegaChatError.ERROR_ACCESS,
                        errorString = "general error"
                    )
                )
            )

            whenever(inviteToChatUseCase(chatId, contactList)).thenReturn(resultList)
            whenever(inviteParticipantResultMapper(resultList)).thenReturn(InviteContactToChatResult.GeneralError)

            initTestClass()
            underTest.inviteContactsToChat(chatId, contactList)
            underTest.state.test {
                val result = ((awaitItem().infoToShowEvent as StateEventWithContentTriggered)
                    .content as InfoToShow.InviteContactResult).result
                assertThat(result).isInstanceOf(InviteContactToChatResult.GeneralError::class.java)
            }
        }

    @Test
    fun `test that some contacts are added and some contacts fail to be added`() = runTest {
        val contactList =
            listOf("user1_added_ok", "user2_added_ok", "user3_add_failure", "user4_add_failure")
        val chatRequest: ChatRequest = mock()
        val resultList = listOf(
            Result.success(chatRequest),
            Result.success(chatRequest),
            Result.failure(
                ParticipantAlreadyExistsException()
            ), Result.failure(
                MegaException(
                    errorCode = MegaChatError.ERROR_ACCESS,
                    errorString = "access error"
                )
            )
        )

        whenever(inviteToChatUseCase(chatId, contactList)).thenReturn(resultList)
        whenever(inviteParticipantResultMapper(resultList)).thenReturn(
            InviteContactToChatResult.SomeAddedSomeNot(success = 2, error = 2)
        )

        initTestClass()
        underTest.inviteContactsToChat(chatId, contactList)
        underTest.state.test {
            val result = ((awaitItem().infoToShowEvent as StateEventWithContentTriggered)
                .content as InfoToShow.InviteContactResult).result
            assertThat(result).isInstanceOf(InviteContactToChatResult.SomeAddedSomeNot::class.java)
            (result as InviteContactToChatResult.SomeAddedSomeNot).let {
                assertThat(it.success).isEqualTo(2)
                assertThat(it.error).isEqualTo(2)
            }
        }
    }

    @ParameterizedTest(name = " and value is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that is waiting room is updated when chat is get`(
        expectedIsWaitingRoom: Boolean,
    ) = runTest {
        val chatRoom = mock<ChatRoom> {
            on { isWaitingRoom } doReturn expectedIsWaitingRoom
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
        }
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().isWaitingRoom).isEqualTo(expectedIsWaitingRoom)
        }
    }

    @ParameterizedTest(name = " and new value is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that is waiting room is updated when a chat room update is received`(
        expectedIsWaitingRoom: Boolean,
    ) = runTest {
        val chatRoom = mock<ChatRoom> {
            on { isWaitingRoom } doReturn expectedIsWaitingRoom
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
        }
        val updateFlow = MutableSharedFlow<ChatRoom>()
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        whenever(monitorChatRoomUpdatesUseCase(chatId)).thenReturn(updateFlow)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().isWaitingRoom).isEqualTo(expectedIsWaitingRoom)
        }

        val newChatRoom = mock<ChatRoom> {
            on { isWaitingRoom } doReturn !expectedIsWaitingRoom
            on { changes } doReturn listOf(ChatRoomChange.WaitingRoom)
        }
        updateFlow.emit(newChatRoom)
        underTest.state.test {
            assertThat(awaitItem().isWaitingRoom).isEqualTo(!expectedIsWaitingRoom)
        }
    }

    @Test
    fun `test that push notification unmute event is not triggered when unmute push notification succeeds`() =
        runTest {
            initTestClass()
            underTest.handleActionPress(ChatRoomMenuAction.Unmute)
            underTest.state.test {
                val result = ((awaitItem().infoToShowEvent as StateEventWithContentTriggered)
                    .content as InfoToShow.MuteOptionResult).result
                assertThat(result).isInstanceOf(ChatPushNotificationMuteOption.Unmute::class.java)
                assertThat(analyticsTestExtension.events).contains(
                    ChatConversationUnmuteMenuToolbarEvent
                )
            }
        }

    @Test
    fun `test that push notification unmute event is triggered when unmute push notification fails`() =
        runTest {
            whenever(unmuteChatNotificationUseCase(chatId)).thenAnswer {
                throw Exception("unmute chat failed")
            }
            initTestClass()
            underTest.handleActionPress(ChatRoomMenuAction.Unmute)
            underTest.state.test {
                assertThat(awaitItem().infoToShowEvent)
                    .isInstanceOf(StateEventWithContentConsumed::class.java)
                assertThat(analyticsTestExtension.events).contains(
                    ChatConversationUnmuteMenuToolbarEvent
                )
            }
        }

    @ParameterizedTest(name = " with success {0}")
    @ValueSource(booleans = [true, false])
    fun `test that chat history finish`(
        success: Boolean,
    ) = runTest {
        if (success) {
            whenever(clearChatHistoryUseCase(chatId = chatId)).thenReturn(Unit)
        } else {
            whenever(clearChatHistoryUseCase(chatId = chatId)).thenThrow(RuntimeException())
        }

        underTest.clearChatHistory()
        underTest.state.test {
            val result = ((awaitItem().infoToShowEvent as StateEventWithContentTriggered)
                .content as InfoToShow.SimpleString).stringId
            assertThat(result).isEqualTo(
                if (success) R.string.clear_history_success
                else R.string.clear_history_error
            )
        }
    }

    @Test
    fun `test that endCall invokes correct use cases when call this function`() = runTest {
        underTest.endCall()
        verify(endCallUseCase).invoke(chatId)
        verify(sendStatisticsMeetingsUseCase).invoke(any())
    }

    @Test
    fun `test that archive finish with error and shows it`() = runTest {
        whenever(archiveChatUseCase(chatId = chatId, true)).thenThrow(RuntimeException())
        underTest.archiveChat()
        verifyNoInteractions(broadcastChatArchivedUseCase)
        underTest.state.test {
            val result = ((awaitItem().infoToShowEvent as StateEventWithContentTriggered).content
                    as InfoToShow.StringWithParams).stringId
            assertThat(result).isEqualTo(R.string.error_archive_chat)
        }
    }

    @Test
    fun `test that archive finish with success`() = runTest {
        whenever(archiveChatUseCase(chatId = chatId, true)).thenReturn(Unit)
        underTest.archiveChat()
        verify(broadcastChatArchivedUseCase).invoke(any())
        underTest.state.test {
            val item = awaitItem()
            assertThat((item.actionToManageEvent as StateEventWithContentTriggered).content)
                .isInstanceOf(ActionToManage.CloseChat::class.java)
            assertThat(item.infoToShowEvent)
                .isInstanceOf(StateEventWithContentConsumed::class.java)
        }
    }

    @ParameterizedTest(name = " when request success {0} and starts with audio {1} and with video {2}")
    @ArgumentsSource(StartCallArgumentsProvider::class)
    fun `test that start call finish with success`(
        success: Boolean,
        audio: Boolean,
        video: Boolean,
    ) = runTest {
        val call = mock<ChatCall> {
            on { this.chatId } doReturn chatId
            on { this.callId } doReturn callId
            on { hasLocalAudio } doReturn audio
            on { hasLocalVideo } doReturn video
            on { isOutgoing } doReturn true
        }

        if (success) {
            whenever(startCallUseCase(chatId, audio, video)).thenReturn(call)
        } else {
            whenever(startCallUseCase(chatId, audio, video)).thenThrow(RuntimeException())
        }

        underTest.startCall(video)

        if (success) {
            verify(chatManagement).setSpeakerStatus(chatId, video)
            verify(chatManagement).setRequestSentCall(callId, true)
            verifyNoMoreInteractions(chatManagement)
            underTest.state.test {
                val actual = awaitItem()
                assertThat(actual.callInThisChat).isEqualTo(call)
                assertThat(actual.isStartingCall).isTrue()
            }
        } else {
            verifyNoInteractions(chatManagement)
        }
    }

    @Test
    fun `test that on call started updates is starting call`() = runTest {
        whenever(startCallUseCase(chatId = chatId, audio = true, video = false)).thenReturn(mock())
        underTest.startCall(false)
        underTest.state.test {
            assertThat(awaitItem().isStartingCall).isTrue()
        }
        underTest.onCallStarted()
        underTest.state.test {
            assertThat(awaitItem().isStartingCall).isFalse()
        }
    }

    @Test
    fun `test that start non waiting schedule meeting failed`() = runTest {
        val invalidHandle = -1L
        val schedId = 123L
        val expectedScheduledMeeting =
            ChatScheduledMeeting(parentSchedId = invalidHandle, schedId = schedId)
        val chatRoom = mock<ChatRoom> {
            on { isGroup } doReturn true
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
            on { isWaitingRoom } doReturn false
        }
        whenever(getScheduledMeetingByChatUseCase(chatId)).thenReturn(
            listOf(
                expectedScheduledMeeting
            )
        )
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().scheduledMeeting).isEqualTo(expectedScheduledMeeting)
        }
        whenever(
            startChatCallNoRingingUseCase(
                chatId = chatId,
                schedId = schedId,
                enabledVideo = false,
                enabledAudio = true
            )
        ).thenThrow(RuntimeException())
        underTest.onStartOrJoinMeeting(false)
        underTest.state.test {
            assertThat(awaitItem().isStartingCall).isFalse()
        }
        verifyNoInteractions(chatManagement)
    }

    @Test
    fun `test that start non waiting schedule meeting successfully`() = runTest {
        val invalidHandle = -1L
        val schedId = 123L
        val expectedScheduledMeeting =
            ChatScheduledMeeting(parentSchedId = invalidHandle, schedId = schedId)
        val chatRoom = mock<ChatRoom> {
            on { isGroup } doReturn true
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
            on { isWaitingRoom } doReturn false
        }
        whenever(getScheduledMeetingByChatUseCase(chatId)).thenReturn(
            listOf(
                expectedScheduledMeeting
            )
        )
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().scheduledMeeting).isEqualTo(expectedScheduledMeeting)
        }
        val call = mock<ChatCall> {
            on { this.chatId } doReturn chatId
            on { this.callId } doReturn callId
            on { isOutgoing } doReturn true
            on { hasLocalVideo } doReturn false
        }
        whenever(
            startChatCallNoRingingUseCase(
                chatId = chatId,
                schedId = schedId,
                enabledVideo = false,
                enabledAudio = true
            )
        ).thenReturn(call)
        underTest.onStartOrJoinMeeting(false)
        underTest.state.test {
            assertThat(awaitItem().isStartingCall).isTrue()
        }
        verify(chatManagement).setSpeakerStatus(chatId, call.hasLocalVideo)
        verify(chatManagement).setRequestSentCall(callId, true)
        verifyNoMoreInteractions(chatManagement)
    }

    @Test
    fun `test that host starts waiting schedule meeting failed`() = runTest {
        val invalidHandle = -1L
        val schedId = 123L
        val expectedScheduledMeeting =
            ChatScheduledMeeting(parentSchedId = invalidHandle, schedId = schedId)
        val chatRoom = mock<ChatRoom> {
            on { isGroup } doReturn true
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
            on { isWaitingRoom } doReturn true
        }
        whenever(getScheduledMeetingByChatUseCase(chatId)).thenReturn(
            listOf(
                expectedScheduledMeeting
            )
        )
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().scheduledMeeting).isEqualTo(expectedScheduledMeeting)
        }
        whenever(
            startMeetingInWaitingRoomChatUseCase(
                chatId = chatId,
                schedIdWr = schedId,
                enabledVideo = false,
                enabledAudio = true,
            )
        ).thenThrow(RuntimeException())
        underTest.onStartOrJoinMeeting(false)
        underTest.state.test {
            assertThat(awaitItem().isStartingCall).isFalse()
        }
        verifyNoInteractions(chatManagement)
    }

    @Test
    fun `test that host start waiting schedule meeting successfully`() = runTest {
        val invalidHandle = -1L
        val schedId = 123L
        val expectedScheduledMeeting =
            ChatScheduledMeeting(parentSchedId = invalidHandle, schedId = schedId)
        val chatRoom = mock<ChatRoom> {
            on { isGroup } doReturn true
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
            on { isWaitingRoom } doReturn true
        }
        whenever(getScheduledMeetingByChatUseCase(chatId)).thenReturn(
            listOf(
                expectedScheduledMeeting
            )
        )
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().scheduledMeeting).isEqualTo(expectedScheduledMeeting)
        }
        val call = mock<ChatCall> {
            on { this.chatId } doReturn chatId
            on { this.callId } doReturn callId
            on { isOutgoing } doReturn true
            on { hasLocalVideo } doReturn false
        }
        whenever(
            startMeetingInWaitingRoomChatUseCase(
                chatId = chatId,
                schedIdWr = schedId,
                enabledVideo = false,
                enabledAudio = true,
            )
        ).thenReturn(call)
        underTest.onStartOrJoinMeeting(false)
        underTest.state.test {
            assertThat(awaitItem().isStartingCall).isTrue()
        }
        verify(chatManagement).setSpeakerStatus(chatId, call.hasLocalVideo)
        verify(chatManagement).setRequestSentCall(callId, true)
        verifyNoMoreInteractions(chatManagement)
    }

    @Test
    fun `test that non-host open waiting screen when starting a meeting`() = runTest {
        val invalidHandle = -1L
        val schedId = 123L
        val expectedScheduledMeeting =
            ChatScheduledMeeting(parentSchedId = invalidHandle, schedId = schedId)
        val chatRoom = mock<ChatRoom> {
            on { isGroup } doReturn true
            on { ownPrivilege } doReturn ChatRoomPermission.Standard
            on { isWaitingRoom } doReturn true
        }
        whenever(getScheduledMeetingByChatUseCase(chatId)).thenReturn(
            listOf(
                expectedScheduledMeeting
            )
        )
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initTestClass()
        underTest.state.test {
            val item = awaitItem()
            assertThat(item.scheduledMeeting).isEqualTo(expectedScheduledMeeting)
            assertThat(item.openWaitingRoomScreen).isFalse()
        }
        underTest.onStartOrJoinMeeting(false)
        underTest.state.test {
            assertThat(awaitItem().openWaitingRoomScreen).isTrue()
        }
    }

    @Test
    fun `test that non-host open waiting screen when joining a meeting`() = runTest {
        val invalidHandle = -1L
        val schedId = 123L
        val expectedScheduledMeeting =
            ChatScheduledMeeting(parentSchedId = invalidHandle, schedId = schedId)
        val chatRoom = mock<ChatRoom> {
            on { isGroup } doReturn true
            on { ownPrivilege } doReturn ChatRoomPermission.Standard
            on { isWaitingRoom } doReturn true
        }
        whenever(getScheduledMeetingByChatUseCase(chatId)).thenReturn(
            listOf(
                expectedScheduledMeeting
            )
        )
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initTestClass()
        underTest.state.test {
            val item = awaitItem()
            assertThat(item.scheduledMeeting).isEqualTo(expectedScheduledMeeting)
            assertThat(item.openWaitingRoomScreen).isFalse()
        }
        underTest.onStartOrJoinMeeting(true)
        underTest.state.test {
            assertThat(awaitItem().openWaitingRoomScreen).isTrue()
        }
    }

    @Test
    fun `test that host answers a call when starting a waiting room meeting`() = runTest {
        val invalidHandle = -1L
        val schedId = 123L
        val expectedScheduledMeeting =
            ChatScheduledMeeting(parentSchedId = invalidHandle, schedId = schedId)
        val chatRoom = mock<ChatRoom> {
            on { isGroup } doReturn true
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
            on { isWaitingRoom } doReturn true
        }
        whenever(getScheduledMeetingByChatUseCase(chatId)).thenReturn(
            listOf(
                expectedScheduledMeeting
            )
        )
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initTestClass()
        underTest.onStartOrJoinMeeting(true)
        verify(answerChatCallUseCase).invoke(chatId = chatId, video = false, audio = true)
    }

    @Test
    fun `test that answer a call when starting a non-waiting room meeting`() = runTest {
        val invalidHandle = -1L
        val schedId = 123L
        val expectedScheduledMeeting =
            ChatScheduledMeeting(parentSchedId = invalidHandle, schedId = schedId)
        val chatRoom = mock<ChatRoom> {
            on { isGroup } doReturn true
            on { ownPrivilege } doReturn ChatRoomPermission.Standard
            on { isWaitingRoom } doReturn false
        }
        whenever(getScheduledMeetingByChatUseCase(chatId)).thenReturn(
            listOf(
                expectedScheduledMeeting
            )
        )
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initTestClass()
        underTest.onStartOrJoinMeeting(true)
        verify(answerChatCallUseCase).invoke(chatId = chatId, video = false, audio = true)
    }

    @ParameterizedTest(name = " {0} is selected")
    @EnumSource(ChatPushNotificationMuteOption::class)
    fun `test that info to show is passed to UI when mute chat push notification option`(
        muteOption: ChatPushNotificationMuteOption,
    ) = runTest {

        initTestClass()
        underTest.mutePushNotification(muteOption)

        underTest.state.test {
            val result = ((awaitItem().infoToShowEvent as StateEventWithContentTriggered)
                .content as InfoToShow.MuteOptionResult).result
            assertThat(result).isEqualTo(muteOption)
        }
    }

    @ParameterizedTest(name = " with success {0}")
    @ValueSource(booleans = [true, false])
    fun `test that answer call invokes chat call use case and finishes`(
        success: Boolean,
    ) = runTest {
        if (success) {
            val call = mock<ChatCall> {
                on { this.chatId } doReturn chatId
                on { this.callId } doReturn callId
            }
            whenever(answerChatCallUseCase(chatId, video = false, audio = true)).thenReturn(call)
        } else {
            whenever(answerChatCallUseCase(chatId, video = false, audio = true))
                .thenThrow(RuntimeException())
        }

        underTest.onAnswerCall()
        verify(chatManagement).addJoiningCallChatId(chatId)
        verify(chatManagement).removeJoiningCallChatId(chatId)
        verifyNoMoreInteractions(chatManagement)

        if (success) {
            verify(rtcAudioManagerGateway).removeRTCAudioManagerRingIn()
            verifyNoMoreInteractions(rtcAudioManagerGateway)
            underTest.state.test {
                assertThat(awaitItem().isStartingCall).isTrue()
            }
        } else {
            verifyNoInteractions(rtcAudioManagerGateway)
        }
    }

    @ParameterizedTest(name = " when use case returns {0}")
    @ValueSource(booleans = [true, false])
    fun `test that is geolocation enabled updates correctly`(
        enabled: Boolean,
    ) = runTest {
        whenever(isGeolocationEnabledUseCase()).thenReturn(enabled)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().isGeolocationEnabled).isEqualTo(enabled)
        }
    }

    @ParameterizedTest(name = " when use case finish with success {0}")
    @ValueSource(booleans = [false, true])
    fun `test that enable geolocation updates the state correctly`(
        success: Boolean,
    ) = runTest {
        whenever(enableGeolocationUseCase()).apply {
            if (success) {
                thenReturn(Unit)
            } else {
                thenThrow(RuntimeException())
            }
        }
        underTest.onEnableGeolocation()
        underTest.state.test {
            assertThat(awaitItem().isGeolocationEnabled).isEqualTo(success)
        }
    }

    @Test
    fun `test that join chat successfully when open by chat link`() = runTest {
        val chatLink = "https://mega.nz/chat/123456789"
        val action = "action"
        val chat = mock<ChatRoom> { on { this.chatId } doReturn chatId }
        whenever(openChatLinkUseCase(chatLink, chatId, false)).thenReturn(chatId)
        whenever(getChatRoomUseCase(chatId)).thenReturn(chat)
        initTestClass(
            action = action,
            link = chatLink,
        )
        underTest.state.test {
            assertThat(awaitItem().chatId).isEqualTo(chatId)
        }
    }

    @Test
    fun `test that join chat failed with general exception when open by chat link`() = runTest {
        val chatLink = "https://mega.nz/chat/123456789"
        val action = "action"
        whenever(openChatLinkUseCase(chatLink, null, false)).thenThrow(RuntimeException())
        initTestClass(
            action = action,
            link = chatLink,
        )
        underTest.state.test {
            val result = ((awaitItem().infoToShowEvent as StateEventWithContentTriggered)
                .content as InfoToShow.SimpleString).stringId
            assertThat(result).isEqualTo(R.string.error_general_nodes)
        }
    }

    @Test
    fun `test that join chat failed with resource not found exception when open by chat link`() =
        runTest {
            val chatLink = "https://mega.nz/chat/123456789"
            val action = "action"
            whenever(openChatLinkUseCase(chatLink, chatId, false)).thenThrow(
                ResourceDoesNotExistChatException()
            )
            initTestClass(
                action = action,
                link = chatLink,
            )
            underTest.state.test {
                val result = ((awaitItem().infoToShowEvent as StateEventWithContentTriggered)
                    .content as InfoToShow.SimpleString).stringId
                assertThat(result).isEqualTo(R.string.invalid_chat_link)
            }
        }

    @Test
    fun `test that hold and answer call invokes use cases if call in other chat exists and hold successes`() =
        runTest {
            val otherChatId = 567L
            val initialCall = mock<ChatCall> {
                on { chatId } doReturn otherChatId
                on { status } doReturn ChatCallStatus.InProgress
            }
            val flow = MutableSharedFlow<List<ChatCall>>()
            whenever(monitorParticipatingInACallInOtherChatsUseCase(any())).thenReturn(flow)
            whenever(holdChatCallUseCase(otherChatId, true)).thenReturn(mock())
            whenever(answerChatCallUseCase(chatId, video = false, audio = true)).thenReturn(mock())
            initTestClass()
            val list = listOf(initialCall)
            flow.emit(list)
            underTest.state.test {
                assertThat(awaitItem().callsInOtherChats).isEqualTo(list)
            }
            underTest.onHoldAndAnswerCall()
            verify(holdChatCallUseCase).invoke(otherChatId, true)
            verify(answerChatCallUseCase).invoke(chatId, video = false, audio = true)
        }

    @Test
    fun `test that hold and answer call invokes answer if call in other chat does not exist`() =
        runTest {
            whenever(answerChatCallUseCase(chatId, video = false, audio = true)).thenReturn(mock())
            underTest.onHoldAndAnswerCall()
            verifyNoInteractions(holdChatCallUseCase)
            verify(answerChatCallUseCase).invoke(chatId, video = false, audio = true)
        }

    @Test
    fun `test that hold and answer call invokes hold but not answer if call in other chat exist and hold fails`() =
        runTest {
            val otherChatId = 567L
            val initialCall = mock<ChatCall> {
                on { chatId } doReturn otherChatId
                on { status } doReturn ChatCallStatus.InProgress
            }
            val flow = MutableSharedFlow<List<ChatCall>>()
            whenever(monitorParticipatingInACallInOtherChatsUseCase(any())).thenReturn(flow)
            whenever(holdChatCallUseCase(otherChatId, true)).thenAnswer {
                throw Exception("hold call failed")
            }
            initTestClass()
            val list = listOf(initialCall)
            flow.emit(list)
            underTest.state.test {
                assertThat(awaitItem().callsInOtherChats).isEqualTo(list)
            }
            underTest.onHoldAndAnswerCall()
            verify(holdChatCallUseCase).invoke(otherChatId, true)
            verifyNoInteractions(answerChatCallUseCase)
        }

    @Test
    fun `test that hold and answer call does not invoke hold but answer if call in other chat exist in hold status`() =
        runTest {
            val otherChatId = 567L
            val initialCall = mock<ChatCall> {
                on { chatId } doReturn otherChatId
                on { status } doReturn ChatCallStatus.InProgress
                on { isOnHold } doReturn true
            }
            val flow = MutableSharedFlow<List<ChatCall>>()
            whenever(monitorParticipatingInACallInOtherChatsUseCase(any())).thenReturn(flow)
            whenever(answerChatCallUseCase(chatId, video = false, audio = true)).thenReturn(mock())
            initTestClass()
            val list = listOf(initialCall)
            flow.emit(list)
            underTest.state.test {
                assertThat(awaitItem().callsInOtherChats).isEqualTo(list)
            }
            underTest.onHoldAndAnswerCall()
            verifyNoInteractions(holdChatCallUseCase)
            verify(answerChatCallUseCase).invoke(chatId, video = false, audio = true)
        }

    @Test
    fun `test that end and answer call invokes use cases if call in other chat exists and end successes`() =
        runTest {
            val callId = 567L
            val initialCall = mock<ChatCall> {
                on { this.callId } doReturn callId
                on { status } doReturn ChatCallStatus.InProgress
            }
            val flow = MutableSharedFlow<List<ChatCall>>()
            whenever(monitorParticipatingInACallInOtherChatsUseCase(any())).thenReturn(flow)
            whenever(hangChatCallUseCase(callId)).thenReturn(mock())
            whenever(answerChatCallUseCase(chatId, video = false, audio = true)).thenReturn(mock())
            initTestClass()
            val list = listOf(initialCall)
            flow.emit(list)
            underTest.state.test {
                assertThat(awaitItem().callsInOtherChats).isEqualTo(list)
            }
            underTest.onEndAndAnswerCall()
            verify(hangChatCallUseCase).invoke(callId)
            verify(answerChatCallUseCase).invoke(chatId, video = false, audio = true)
        }

    @Test
    fun `test that end and answer call invokes answer if call in other chat does not exist`() =
        runTest {
            whenever(answerChatCallUseCase(chatId, video = false, audio = true)).thenReturn(mock())
            underTest.onEndAndAnswerCall()
            verifyNoInteractions(hangChatCallUseCase)
            verify(answerChatCallUseCase).invoke(chatId, video = false, audio = true)
        }

    @Test
    fun `test that end and answer call invokes end but not answer if call in other chat exist and end fails`() =
        runTest {
            val callId = 567L
            val initialCall = mock<ChatCall> {
                on { this.callId } doReturn callId
                on { status } doReturn ChatCallStatus.InProgress
            }
            val flow = MutableSharedFlow<List<ChatCall>>()
            whenever(monitorParticipatingInACallInOtherChatsUseCase(any())).thenReturn(flow)
            whenever(hangChatCallUseCase(callId)).thenAnswer {
                throw Exception("end call failed")
            }
            initTestClass()
            val list = listOf(initialCall)
            flow.emit(list)
            underTest.state.test {
                assertThat(awaitItem().callsInOtherChats).isEqualTo(list)
            }
            underTest.onEndAndAnswerCall()
            verify(hangChatCallUseCase).invoke(callId)
            verifyNoInteractions(answerChatCallUseCase)
        }

    @Test
    fun `test that isAnonymousMode is updated`() = runTest {
        whenever(isAnonymousModeUseCase()).thenReturn(true)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().isAnonymousMode).isTrue()
        }
    }

    @Test
    fun `test that joinChatLinkUseCase is invoked if onJoinChat is`() = runTest {
        whenever(joinPublicChatUseCase(chatId)).thenReturn(Unit)
        underTest.onJoinChat()
        verify(joinPublicChatUseCase).invoke(chatId)
    }

    @Test
    fun `test that chat management is invoked if onSetPendingJoinLink is`() = runTest {
        val link = "link"
        initTestClass(
            link = link,
        )
        underTest.onSetPendingJoinLink()
        verify(chatManagement).pendingJoinLink = link
        verifyNoMoreInteractions(chatManagement)
    }

    @Test
    fun `test that isJoining is updated`() = runTest {
        val updatedFlow = MutableSharedFlow<Boolean>()
        whenever(monitorJoiningChatUseCase(chatId)).thenReturn(updatedFlow)
        initTestClass()
        updatedFlow.emit(true)
        advanceUntilIdle()
        underTest.state.test {
            assertThat(awaitItem().isJoining).isTrue()
        }
        updatedFlow.emit(false)
        underTest.state.test {
            assertThat(awaitItem().isJoining).isTrue()
        }
    }

    @Test
    fun `test that isLeaving is updated`() = runTest {
        val updatedFlow = MutableSharedFlow<Boolean>()
        whenever(monitorLeavingChatUseCase(chatId)).thenReturn(updatedFlow)
        initTestClass()
        updatedFlow.emit(true)
        advanceUntilIdle()
        underTest.state.test {
            assertThat(awaitItem().isLeaving).isTrue()
        }
        updatedFlow.emit(false)
        underTest.state.test {
            assertThat(awaitItem().isLeaving).isFalse()
        }
    }

    @Test
    fun `test that correct state event is set when on attach files is invoked`() =
        runTest {
            initTestClass()
            val files = List(5) { index ->
                UriPath("file$index")
            }
            underTest.state.test {
                awaitItem() // Initial state doesn't matter
                underTest.onAttachFiles(files)
                val actual = awaitItem().downloadEvent
                assertThat(actual).isInstanceOf(StateEventWithContentTriggered::class.java)
                val content = (actual as StateEventWithContentTriggered).content
                assertThat(content)
                    .isInstanceOf(TransferTriggerEvent.StartChatUpload.Files::class.java)
                assertThat((content as TransferTriggerEvent.StartChatUpload.Files).uris)
                    .isEqualTo(files)
            }
        }

    @Test
    fun `test that sending text updates state correctly`() = runTest {
        whenever(monitorChatPendingChangesUseCase(chatId))
            .thenReturn(flowOf(ChatPendingChanges(1L, "draft message")))
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().sendingText).isEqualTo("draft message")
        }
    }

    @Test
    fun `test that close editing message updates state correctly`() = runTest {
        underTest.onCloseEditing()
        underTest.state.test {
            assertThat(awaitItem().editingMessageId).isNull()
        }
    }

    @Test
    fun `test that add reaction invokes use case`() = runTest {
        val msgId = 1234L
        val reaction = "reaction"
        whenever(addReactionUseCase(chatId, msgId, reaction)).thenReturn(Unit)
        underTest.onAddReaction(msgId, reaction)
        verify(addReactionUseCase).invoke(chatId, msgId, reaction)
    }

    @Test
    fun `test that editing message updates state correctly when a chat message exists`() = runTest {
        whenever(monitorChatPendingChangesUseCase(chatId))
            .thenReturn(
                flowOf(
                    ChatPendingChanges(
                        chatId = chatId,
                        draftMessage = "draft message",
                        editingMessageId = 1234L
                    )
                )
            )
        val message = mock<ChatMessage> {
            on { content } doReturn "editing message"
            on { messageId } doReturn 1234L
            on { isEditable } doReturn true
        }
        whenever(getChatMessageUseCase(chatId, 1234L)).thenReturn(message)
        initTestClass()
        underTest.state.test {
            val item = awaitItem()
            assertThat(item.sendingText).isEqualTo("draft message")
            assertThat(item.editingMessageId).isEqualTo(1234L)
            assertThat(item.editingMessageContent).isEqualTo("editing message")
        }
    }

    @Test
    fun `test that editing message updates state correctly when a chat message doesn't exist`() =
        runTest {
            whenever(monitorChatPendingChangesUseCase(chatId))
                .thenReturn(
                    flowOf(
                        ChatPendingChanges(
                            chatId = chatId,
                            draftMessage = "draft message",
                            editingMessageId = 1234L
                        )
                    )
                )
            whenever(getChatMessageUseCase(chatId, 1234L)).thenReturn(null)
            initTestClass()
            underTest.state.test {
                val item = awaitItem()
                assertThat(item.sendingText).isEqualTo("draft message")
                assertThat(item.editingMessageId).isNull()
                assertThat(item.editingMessageContent).isNull()
            }
        }

    @Test
    fun `test that on send giphy message invokes use case`() = runTest {
        val srcMp4 = "srcMp4"
        val srcWebp = "srcWebp"
        val sizeMp4 = 350L
        val sizeWebp = 250L
        val width = 250
        val height = 500
        val title = "title"
        val gifData = GifData(srcMp4, srcWebp, sizeMp4, sizeWebp, width, height, title)
        whenever(
            sendGiphyMessageUseCase(
                chatId,
                srcMp4,
                srcWebp,
                sizeMp4,
                sizeWebp,
                width,
                height,
                title
            )
        ).thenReturn(Unit)
        underTest.onSendGiphyMessage(gifData)
        verify(sendGiphyMessageUseCase).invoke(
            chatId,
            srcMp4,
            srcWebp,
            sizeMp4,
            sizeWebp,
            width,
            height,
            title
        )
    }

    @Test
    fun `test that delete reaction invokes use case`() = runTest {
        val msgId = 1234L
        val reaction = "reaction"
        whenever(deleteReactionUseCase(chatId, msgId, reaction)).thenReturn(Unit)
        underTest.onDeleteReaction(msgId, reaction)
        verify(deleteReactionUseCase).invoke(chatId, msgId, reaction)
    }

    @Test
    fun `test that attach contacts invokes use case`() = runTest {
        val contactList = listOf("contact1, contact2")
        whenever(attachContactsUseCase(chatId, contactList)).thenReturn(Unit)
        underTest.onAttachContacts(contactList)
        verify(attachContactsUseCase).invoke(chatId, contactList)
    }

    @Test
    fun `test that getUser invokes use case`() = runTest {
        val userId = UserId(1234L)
        initTestClass()
        underTest.getUser(userId)
        verify(getUserUseCase).invoke(userId)
    }

    @Test
    fun `test that get user info into UIReaction list can return proper result`() = runTest {
        val fakeMyUserHandle = 1L
        whenever(getMyUserHandleUseCase()).thenReturn(fakeMyUserHandle)
        val expectedFullNameForMe = "MyName(Me)"
        val expectedFullNameForUser1 = "username 1"
        val userHandle1 = 1L
        val expectedFullNameForUser2 = "username 2"
        val userHandle2 = 2L
        whenever(getParticipantFullNameUseCase(userHandle1)).thenReturn(expectedFullNameForUser1)
        whenever(getParticipantFullNameUseCase(userHandle2)).thenReturn(expectedFullNameForUser2)
        whenever(participantNameMapper(true, expectedFullNameForUser1)).thenReturn(
            expectedFullNameForMe
        )
        whenever(participantNameMapper(false, expectedFullNameForUser1)).thenReturn(
            expectedFullNameForUser1
        )
        whenever(participantNameMapper(false, expectedFullNameForUser2)).thenReturn(
            expectedFullNameForUser2
        )

        val input = listOf(
            UIReaction(
                reaction = "reaction1",
                shortCode = ":shortcode1:",
                count = 1,
                hasMe = true,
                userList = listOf(
                    UIReactionUser(
                        userHandle = userHandle1,
                    ),
                    UIReactionUser(
                        userHandle = userHandle2,
                    )
                )
            ),
            UIReaction(
                reaction = "reaction2",
                shortCode = ":shortcode2:",
                count = 2,
                hasMe = false,
                userList = listOf(
                    UIReactionUser(
                        userHandle = userHandle1,
                    ),
                    UIReactionUser(
                        userHandle = userHandle2,
                    )
                )
            )
        )
        initTestClass()
        val result = underTest.getUserInfoIntoReactionList(input)
        with(result[0]) {
            assertThat(userList[0].name).isEqualTo(expectedFullNameForMe)
            assertThat(userList[1].name).isEqualTo(expectedFullNameForUser2)
        }
        with(result[1]) {
            assertThat(userList[0].name).isEqualTo(expectedFullNameForMe)
            assertThat(userList[1].name).isEqualTo(expectedFullNameForUser2)
        }
    }


    @Test
    fun `test that forward messages invokes use case and updates state`() = runTest {
        val message = mock<TypedMessage>()
        val messages = setOf(message)
        val chatId1 = 123L
        val chatId2 = 456L
        val contactId = 789L
        val chatHandles = listOf(chatId1, chatId2)
        val contactHandles = listOf(contactId)
        val result1 = ForwardResult.Success(chatId1)
        val result2 = ForwardResult.Success(chatId2)
        val result3 = ForwardResult.Success(contactId)
        val results = listOf(result1, result2, result3)
        val forwardResult = ForwardMessagesToChatsResult.AllSucceeded(null, messages.size)
        whenever(forwardMessagesUseCase(messages.toList(), chatHandles, contactHandles))
            .thenReturn(results)
        whenever(forwardMessagesResultMapper(results, messages.size)).thenReturn(forwardResult)
        underTest.onForwardMessages(messages, chatHandles, contactHandles)
        verify(forwardMessagesUseCase).invoke(messages.toList(), chatHandles, contactHandles)
        underTest.state.test {
            val result = ((awaitItem().infoToShowEvent as StateEventWithContentTriggered)
                .content as InfoToShow.ForwardMessagesResult).result
            assertThat(result).isEqualTo(forwardResult)
        }
    }

    @Test
    fun `test that forward messages updates state if CreateChatException is thrown`() = runTest {
        val message = mock<TypedMessage>()
        val messages = setOf(message)
        val chatId1 = 123L
        val chatId2 = 456L
        val contactId = 789L
        val chatHandles = listOf(chatId1, chatId2)
        val contactHandles = listOf(contactId)
        whenever(forwardMessagesUseCase(messages.toList(), chatHandles, contactHandles))
            .thenThrow(CreateChatException::class.java)
        underTest.onForwardMessages(messages, chatHandles, contactHandles)
        underTest.state.test {
            val result = ((awaitItem().infoToShowEvent as StateEventWithContentTriggered)
                .content as InfoToShow.QuantityString).stringId
            assertThat(result).isEqualTo(R.plurals.num_messages_not_send)
        }
    }

    @Test
    fun `test that on attach nodes calls attachNodeUseCase with the correct nodes`() = runTest {
        val nodeIds = (1L..5L).map { NodeId(it) }
        val files = nodeIds.map { mock<TypedFileNode>() }
        nodeIds.forEachIndexed { index, nodeId ->
            val file = files[index]
            whenever(getNodeByIdUseCase(nodeId)).thenReturn(file)
            whenever(addNodeType(file)).thenReturn(file)
        }
        underTest.onAttachNodes(nodeIds)
        files.forEach {
            verify(attachNodeUseCase)(chatId, it)
        }
    }

    @Test
    fun `test that error message is sent when on attach nodes fails`() = runTest {
        val nodeIds = (1L..5L).map { NodeId(it) }
        val files = nodeIds.map { mock<TypedFileNode>() }
        val indexWithError = 1
        nodeIds.forEachIndexed { index, nodeId ->
            val file = files[index]
            whenever(getNodeByIdUseCase(nodeId)).thenReturn(file)
            whenever(addNodeType(file)).thenReturn(file)
            if (index == indexWithError) {
                whenever(attachNodeUseCase(chatId, file)).thenThrow(RuntimeException())
            } else {
                whenever(attachNodeUseCase(chatId, file)).thenReturn(Unit)
            }
        }
        underTest.state.test {
            awaitItem() //initial
            underTest.onAttachNodes(nodeIds)
            val actual = awaitItem().infoToShowEvent
            assertThat(actual).isInstanceOf(StateEventWithContentTriggered::class.java)
            val content = (actual as StateEventWithContentTriggered).content
            assertThat(content).isInstanceOf(InfoToShow.SimpleString::class.java)
            val simpleString = (content as InfoToShow.SimpleString)
            assertThat(simpleString.stringId).isEqualTo(R.string.files_send_to_chat_error)
        }
        files.filterIndexed { index, _ -> index != indexWithError }.forEach {
            verify(attachNodeUseCase)(chatId, it)
        }
    }

    @Test
    fun `test that consumeDownloadEvent update state correctly`() = runTest {
        initTestClass()
        underTest.onActionToManageEventConsumed()
        underTest.state.test {
            assertThat(awaitItem().downloadEvent)
                .isInstanceOf(StateEventWithContentConsumed::class.java)
        }
    }

    @Test
    fun `test that on download node for preview updates state correctly`() = runTest {
        val node = mock<ChatDefaultFile>()
        initTestClass()
        underTest.onDownloadForPreviewChatNode(node, false)
        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.downloadEvent)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
            val content = (actual.downloadEvent as StateEventWithContentTriggered).content
            assertThat(content).isInstanceOf(TransferTriggerEvent.StartDownloadForPreview::class.java)
        }
    }

    @Test
    fun `test that on download node for offline updates state correctly`() = runTest {
        val node = mock<ChatDefaultFile>()
        initTestClass()
        underTest.onDownloadForOfflineChatNode(node)
        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.downloadEvent)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
            val content = (actual.downloadEvent as StateEventWithContentTriggered).content
            assertThat(content).isInstanceOf(TransferTriggerEvent.StartDownloadForOffline::class.java)
        }
    }

    @Test
    fun `test that on download node updates state correctly`() = runTest {
        val node = mock<ChatDefaultFile>()
        initTestClass()
        underTest.onDownloadNode(listOf(node))
        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.downloadEvent)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
            val content = (actual.downloadEvent as StateEventWithContentTriggered).content
            assertThat(content).isInstanceOf(TransferTriggerEvent.StartDownloadNode::class.java)
        }
    }

    @Test
    fun `test that delete messages invokes use case`() = runTest {
        val message = mock<TypedMessage>()
        val messages = setOf(message)
        whenever(deleteMessagesUseCase(messages.toList())).thenReturn(Unit)
        initTestClass()
        underTest.onDeletedMessages(messages)
        verify(deleteMessagesUseCase).invoke(messages.toList())
    }

    @Test
    fun `test that on edit message invokes and updates state correctly`() = runTest {
        val content = "content"
        val msgId = 1234L
        val message = mock<NormalMessage> {
            on { this.msgId } doReturn msgId
            on { this.content } doReturn content
        }
        with(underTest) {
            onEditMessage(message)
            state.test {
                val actual = awaitItem()
                assertThat(actual.editingMessageId).isEqualTo(msgId)
                assertThat(actual.editingMessageContent).isEqualTo(content)
                assertThat(actual.sendingText).isEqualTo(content)
            }
        }
    }

    @Test
    fun `test that on edit message shows error if cannot be edited`() = runTest {
        val content = ""
        val msgId = 1234L
        val message = mock<NormalMessage> {
            on { this.msgId } doReturn msgId
            on { this.content } doReturn content
        }
        with(underTest) {
            onEditMessage(message)
            state.test {
                val result = ((awaitItem().infoToShowEvent as StateEventWithContentTriggered)
                    .content as InfoToShow.SimpleString).stringId
                assertThat(result).isEqualTo(R.string.error_editing_message)
            }
        }
    }

    @Test
    fun `test that send text message invokes and updates correctly if it is editing`() = runTest {
        val content = "content"
        val newContent = "newContent"
        val msgId = 1234L
        val message = mock<NormalMessage> {
            on { this.msgId } doReturn msgId
            on { this.content } doReturn content
        }
        whenever(editMessageUseCase(chatId, msgId, newContent)).thenReturn(mock())
        with(underTest) {
            onEditMessage(message)
            sendTextMessage(newContent)
            state.test {
                val actual = awaitItem()
                assertThat(actual.editingMessageId).isNull()
                assertThat(actual.editingMessageContent).isNull()
            }
        }
    }

    @Test
    fun `test that send text message invokes and updates correctly if it is editing and the edition fails`() =
        runTest {
            val content = "content"
            val newContent = "newContent"
            val msgId = 1234L
            val message = mock<NormalMessage> {
                on { this.msgId } doReturn msgId
                on { this.content } doReturn content
            }
            whenever(editMessageUseCase(chatId, msgId, newContent)).thenReturn(null)
            with(underTest) {
                onEditMessage(message)
                sendTextMessage(newContent)
                state.test {
                    val result = ((awaitItem().infoToShowEvent as StateEventWithContentTriggered)
                        .content as InfoToShow.SimpleString).stringId
                    assertThat(result).isEqualTo(R.string.error_editing_message)
                }
            }
        }

    @Test
    fun `test that open chat with invokes and updates correctly`() = runTest {
        val messages = listOf(mock<ContactAttachmentMessage>())
        whenever(getChatFromContactMessagesUseCase(messages)).thenReturn(chatId)
        with(underTest) {
            onOpenChatWith(messages)
            state.test {
                val result =
                    ((awaitItem().actionToManageEvent as StateEventWithContentTriggered)
                        .content as ActionToManage.OpenChat).chatId
                assertThat(result).isEqualTo(chatId)
            }
        }
    }

    @Test
    fun `test that onActionToManageEventConsumed updates state correctly`() = runTest {
        initTestClass()
        underTest.onActionToManageEventConsumed()
        underTest.state.test {
            assertThat(awaitItem().actionToManageEvent)
                .isInstanceOf(StateEventWithContentConsumed::class.java)
        }
    }

    @Test
    fun `test that onEnableSelectMode updates correctly`() = runTest {
        with(underTest) {
            onEnableSelectMode()
            state.test {
                val result =
                    (awaitItem().actionToManageEvent as StateEventWithContentTriggered).content
                assertThat(result).isInstanceOf(ActionToManage.EnableSelectMode::class.java)
            }
        }
    }

    @Test
    fun `test that onOpenContactInfo updates correctly`() = runTest {
        val email = "email"
        with(underTest) {
            onOpenContactInfo(email)
            state.test {
                val result =
                    ((awaitItem().actionToManageEvent as StateEventWithContentTriggered)
                        .content as ActionToManage.OpenContactInfo).email
                assertThat(result).isEqualTo(email)
            }
        }
    }

    @Test
    fun `test that leave chat call when monitorLeaveChatUseCase emit equals chat id`() = runTest {
        val flow = MutableSharedFlow<Long>()
        whenever(monitorLeaveChatUseCase()).thenReturn(flow)
        initTestClass()
        flow.emit(chatId)
        verify(leaveChatUseCase).invoke(chatId)
    }

    @Test
    fun `test that leave chat doesn't call when monitorLeaveChatUseCase emit differ chat id`() =
        runTest {
            val flow = MutableSharedFlow<Long>()
            whenever(monitorLeaveChatUseCase()).thenReturn(flow)
            initTestClass()
            flow.emit(345L)
            verifyNoInteractions(leaveChatUseCase)
        }

    @ParameterizedTest(name = " when call status is {0}")
    @MethodSource("provideChatCallStatusParameters")
    fun `test that has call is ended when user limit is reached`(chatCallStatus: ChatCallStatus) =
        runTest {
            val flow = MutableSharedFlow<ChatCall>()
            val call = mock<ChatCall> {
                on { this.chatId } doReturn chatId
                on { status } doReturn chatCallStatus
                on { termCode } doReturn ChatCallTermCodeType.CallUsersLimit
            }
            whenever(monitorCallInChatUseCase(chatId)).thenReturn(flow)
            whenever(getFeatureFlagValueUseCase(ApiFeatures.CallUnlimitedProPlan)).thenReturn(true)
            initTestClass()
            flow.emit(call)
            advanceUntilIdle()
            underTest.state.test {
                assertThat(awaitItem().callEndedDueToFreePlanLimits).isTrue()
            }
        }

    @ParameterizedTest(name = " when call status is {0}")
    @MethodSource("provideChatCallStatusParameters")
    fun `test that has call is ended when call duration limit is reached`(chatCallStatus: ChatCallStatus) =
        runTest {
            val flow = MutableSharedFlow<ChatCall>()
            val call = mock<ChatCall> {
                on { this.chatId } doReturn chatId
                on { status } doReturn chatCallStatus
                on { termCode } doReturn ChatCallTermCodeType.CallDurationLimit
                on { isOwnClientCaller } doReturn true
            }
            whenever(monitorCallInChatUseCase(chatId)).thenReturn(flow)
            whenever(getFeatureFlagValueUseCase(ApiFeatures.CallUnlimitedProPlan)).thenReturn(true)
            initTestClass()
            flow.emit(call)
            advanceUntilIdle()
            underTest.state.test {
                assertThat(awaitItem().shouldUpgradeToProPlan).isTrue()
            }
        }

    @Test
    fun `test that when consumeShowFreePlanParticipantsLimitDialogEvent is invoked it updates the state`() =
        runTest {
            initTestClass()
            underTest.consumeShowFreePlanParticipantsLimitDialogEvent()
            underTest.state.test {
                assertThat(awaitItem().callEndedDueToFreePlanLimits).isFalse()
                verify(setUsersCallLimitRemindersUseCase).invoke(UsersCallLimitReminders.Disabled)
            }
        }

    @Test
    fun `test that when onConsumeShouldUpgradeToProPlan is invoked it updates the state`() =
        runTest {
            initTestClass()
            underTest.onConsumeShouldUpgradeToProPlan()
            underTest.state.test {
                assertThat(awaitItem().shouldUpgradeToProPlan).isFalse()
                verify(broadcastUpgradeDialogClosedUseCase).invoke()
            }
        }

    private fun provideChatCallStatusParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(ChatCallStatus.TerminatingUserParticipation),
        Arguments.of(ChatCallStatus.GenericNotification),
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class VoiceClipTests {
        private val path = "/cache/example.m4a"
        private val destination = File(path)

        @BeforeEach
        fun setup() {
            whenever(
                getCacheFileUseCase(eq(CacheFolderManager.VOICE_CLIP_FOLDER), any())
            ) doReturn destination
            whenever(recordAudioUseCase(destination)) doReturn flow {
                awaitCancellation()
            }
            initTestClass()
        }

        @AfterEach
        fun cancel() {
            underTest.onVoiceClipRecordEvent(VoiceClipRecordEvent.Cancel) //to cancel the recording job and don't affect other tests
        }

        @Test
        fun `test that audio is recorded to cache file when VoiceClipRecordEvent Start event is received`() =
            runTest {
                underTest.onVoiceClipRecordEvent(VoiceClipRecordEvent.Start)
                verify(recordAudioUseCase).invoke(destination)
            }

        @Test
        fun `test that recorded audio is deleted when VoiceClipRecordEvent Cancel event is received`() =
            runTest {
                underTest.onVoiceClipRecordEvent(VoiceClipRecordEvent.Start)
                underTest.onVoiceClipRecordEvent(VoiceClipRecordEvent.Cancel)
                verify(deleteFileUseCase).invoke(path)
            }

        @Test
        fun `test that correct state event is set when VoiceClipRecordEvent Finish event is received`() =
            runTest {
                underTest.onVoiceClipRecordEvent(VoiceClipRecordEvent.Start)
                underTest.state.test {
                    awaitItem() // Initial state doesn't matter
                    underTest.onVoiceClipRecordEvent(VoiceClipRecordEvent.Finish)
                    val actual = awaitItem().downloadEvent
                    assertThat(actual).isInstanceOf(StateEventWithContentTriggered::class.java)
                    val content = (actual as StateEventWithContentTriggered).content
                    assertThat(content)
                        .isInstanceOf(TransferTriggerEvent.StartChatUpload.VoiceClip::class.java)
                    assertThat((content as TransferTriggerEvent.StartChatUpload.VoiceClip).file)
                        .isEqualTo(destination)
                }
            }
    }

    @ParameterizedTest(name = " when use case returns {0}")
    @ValueSource(booleans = [true, false])
    fun `test that are transfers paused returns correctly`(
        areTransfersPaused: Boolean,
    ) = runTest {
        whenever(areTransfersPausedUseCase()).thenReturn(areTransfersPaused)

        assertThat(underTest.areTransfersPaused()).isEqualTo(areTransfersPaused)
    }

    @Test
    fun `test that resume transfers invokes correct use case`() = runTest {
        underTest.resumeTransfers()

        verify(pauseTransfersQueueUseCase).invoke(false)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class DocumentScannerTests {

        @BeforeEach
        fun setUp() {
            initTestClass()
        }

        @Test
        fun `test that the ML Kit Document Scanner is initialized and ready to scan documents`() =
            runTest {
                val gmsDocumentScanner = mock<GmsDocumentScanner>()
                whenever(scannerHandler.prepareDocumentScanner()).thenReturn(gmsDocumentScanner)

                underTest.onAttachScan()

                underTest.state.test {
                    assertThat(awaitItem().gmsDocumentScanner).isEqualTo(
                        triggered(gmsDocumentScanner)
                    )
                }
            }

        @Test
        fun `test that an insufficient RAM to launch error is returned when initializing the ML Kit Document Scanner with low device RAM`() =
            runTest {
                whenever(scannerHandler.prepareDocumentScanner()).thenAnswer {
                    throw InsufficientRAMToLaunchDocumentScanner()
                }

                assertDoesNotThrow { underTest.onAttachScan() }

                underTest.state.test {
                    assertThat(awaitItem().documentScanningError).isEqualTo(DocumentScanningError.InsufficientRAM)
                }
            }

        @Test
        fun `test that a generic error is returned when initializing the ML Kit Document Scanner results in an error`() =
            runTest {
                whenever(scannerHandler.prepareDocumentScanner()).thenThrow(RuntimeException())

                assertDoesNotThrow { underTest.onAttachScan() }

                underTest.state.test {
                    assertThat(awaitItem().documentScanningError).isEqualTo(DocumentScanningError.GenericError)
                }
            }

        @Test
        fun `test that a generic error is returned when opening the ML Kit Document Scanner results in an error`() =
            runTest {
                underTest.onDocumentScannerFailedToOpen()

                underTest.state.test {
                    assertThat(awaitItem().documentScanningError).isEqualTo(DocumentScanningError.GenericError)
                }
            }

        @Test
        fun `test that the gms document scanner is reset`() = runTest {
            underTest.onGmsDocumentScannerConsumed()

            underTest.state.test {
                assertThat(awaitItem().gmsDocumentScanner).isEqualTo(consumed())
            }
        }

        @Test
        fun `test that the document scanning error is reset`() = runTest {
            underTest.onDocumentScanningErrorConsumed()

            underTest.state.test {
                assertThat(awaitItem().documentScanningError).isNull()
            }
        }
    }

    private fun ChatRoom.getNumberParticipants() =
        (peerCount + if (ownPrivilege != ChatRoomPermission.Unknown
            && ownPrivilege != ChatRoomPermission.Removed
        ) 1 else 0).takeIf { isGroup }

    private fun providePeerCountParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(ChatRoomPermission.Unknown, false, 0L),
        Arguments.of(ChatRoomPermission.Removed, true, 1L),
        Arguments.of(ChatRoomPermission.Removed, false, 0L),
        Arguments.of(ChatRoomPermission.Standard, true, 300L),
        Arguments.of(ChatRoomPermission.ReadOnly, true, 7L),
        Arguments.of(ChatRoomPermission.Moderator, true, 15L),
        Arguments.of(ChatRoomPermission.Moderator, true, 0L),
    )

    private fun providePeerCountUpdateParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(ChatRoomChange.OwnPrivilege, false),
        Arguments.of(ChatRoomChange.OwnPrivilege, true),
        Arguments.of(ChatRoomChange.Closed, false),
        Arguments.of(ChatRoomChange.Closed, true),
        Arguments.of(ChatRoomChange.Participants, true),
    )

    companion object {
        private val testDispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)

        @JvmField
        @RegisterExtension
        val analyticsTestExtension = AnalyticsTestExtension()
    }
}

internal class StartCallArgumentsProvider : ArgumentsProvider {

    override fun provideArguments(context: ExtensionContext): Stream<out Arguments>? {
        return Stream.of(
            Arguments.of(false, false, false),
            Arguments.of(false, true, false),
            Arguments.of(false, false, true),
            Arguments.of(false, true, true),
            Arguments.of(true, true, false),
            Arguments.of(true, true, true),
        )
    }
}
