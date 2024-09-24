package mega.privacy.android.domain.usecase.meeting

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.call.CallCompositionChanges
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.domain.usecase.meeting.GetParticipantsChangesUseCase.ParticipantsChangesResult
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetParticipantsChangesUseCaseTest {

    private lateinit var underTest: GetParticipantsChangesUseCase

    private val getChatRoomUseCase = mock<GetChatRoomUseCase>()
    private val monitorChatCallUpdatesUseCase = mock<MonitorChatCallUpdatesUseCase>()
    private val getMyUserHandleUseCase = mock<GetMyUserHandleUseCase>()

    private val chatId = 123L
    private val callId = 999L
    private val peerId = 456L
    private val myHandle = 789L

    private val chat = ChatRoom(
        chatId = chatId,
        ownPrivilege = ChatRoomPermission.Standard,
        numPreviewers = 0,
        peerPrivilegesByHandles = emptyMap(),
        peerCount = 0,
        peerHandlesList = emptyList(),
        peerPrivilegesList = emptyList(),
        isGroup = true,
        isPublic = false,
        isPreview = false,
        authorizationToken = null,
        title = "",
        hasCustomTitle = false,
        unreadCount = 0,
        userTyping = 0,
        userHandle = 0,
        isActive = false,
        isArchived = false,
        retentionTime = 0,
        creationTime = 0,
        isMeeting = false,
        isWaitingRoom = false,
        isOpenInvite = false,
        isSpeakRequest = false
    )

    @BeforeAll
    fun setUp() {
        underTest = GetParticipantsChangesUseCase(
            getChatRoomUseCase = getChatRoomUseCase,
            monitorChatCallUpdatesUseCase = monitorChatCallUpdatesUseCase,
            getMyUserHandleUseCase = getMyUserHandleUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getChatRoomUseCase, monitorChatCallUpdatesUseCase, getMyUserHandleUseCase)
    }

    @Test
    fun `test that participant join changes are emitted`() = runTest {
        val chatCall = ChatCall(
            chatId = chatId,
            callId = callId,
            status = ChatCallStatus.InProgress,
            peerIdCallCompositionChange = peerId,
            callCompositionChange = CallCompositionChanges.Added,
            changes = listOf(ChatCallChanges.CallComposition)
        )

        whenever(monitorChatCallUpdatesUseCase()).thenReturn(flowOf(chatCall))
        whenever(getChatRoomUseCase(chatId)).thenReturn(chat)
        whenever(getMyUserHandleUseCase()).thenReturn(myHandle)

        underTest().test {
            val result = awaitItem()
            assertThat(result).isEqualTo(
                ParticipantsChangesResult(
                    chatId = chatId,
                    typeChange = GetParticipantsChangesUseCase.TYPE_JOIN,
                    peers = arrayListOf(peerId)
                )
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that participant leave changes are emitted`() = runTest {

        val chatCall = ChatCall(
            chatId = chatId,
            callId = callId,
            status = ChatCallStatus.InProgress,
            peerIdCallCompositionChange = peerId,
            callCompositionChange = CallCompositionChanges.Removed,
            changes = listOf(ChatCallChanges.CallComposition)
        )

        whenever(monitorChatCallUpdatesUseCase()).thenReturn(flowOf(chatCall))
        whenever(getChatRoomUseCase(chatId)).thenReturn(chat)
        whenever(getMyUserHandleUseCase()).thenReturn(myHandle)

        underTest().test {
            val result = awaitItem()
            assertThat(result).isEqualTo(
                ParticipantsChangesResult(
                    chatId = chatId,
                    typeChange = GetParticipantsChangesUseCase.TYPE_LEFT,
                    peers = arrayListOf(peerId)
                )
            )
            cancelAndIgnoreRemainingEvents()
        }
    }
}
