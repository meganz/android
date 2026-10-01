package mega.privacy.android.feature.chat.managehistory

import mega.privacy.android.feature.chat.managehistory.model.ChatHistoryRetention
import kotlin.time.Duration.Companion.seconds
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.ClearChatHistoryUseCase
import mega.privacy.android.domain.usecase.chat.GetChatRoomByUserUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatRetentionTimeUpdateUseCase
import mega.privacy.android.domain.usecase.chat.SetChatRetentionTimeUseCase
import mega.privacy.android.domain.usecase.contact.GetContactHandleUseCase
import mega.privacy.android.feature.chat.managehistory.model.ManageChatHistoryUiState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
class ManageChatHistoryViewModelTest {

    private val getChatRoomUseCase = mock<GetChatRoomUseCase>()
    private val getChatRoomByUserUseCase = mock<GetChatRoomByUserUseCase>()
    private val getContactHandleUseCase = mock<GetContactHandleUseCase>()
    private val monitorChatRetentionTimeUpdateUseCase =
        mock<MonitorChatRetentionTimeUpdateUseCase>()
    private val clearChatHistoryUseCase = mock<ClearChatHistoryUseCase>()
    private val setChatRetentionTimeUseCase = mock<SetChatRetentionTimeUseCase>()

    @AfterEach
    fun tearDown() {
        reset(
            getChatRoomUseCase,
            getChatRoomByUserUseCase,
            getContactHandleUseCase,
            monitorChatRetentionTimeUpdateUseCase,
            clearChatHistoryUseCase,
            setChatRetentionTimeUseCase,
        )
    }

    @Test
    fun `test that the initial state is Loading`() = runTest {
        val underTest = createViewModel(ManageChatHistoryViewModel.Args(CHAT_ID, null))

        assertThat(underTest.uiState.value).isEqualTo(ManageChatHistoryUiState.Loading)
    }

    @Test
    fun `test that uiState emits the chat room when the chat id resolves`() = runTest {
        stubChatRoom(chatId = CHAT_ID, isMeeting = true, retentionTime = ONE_DAY)
        stubRetentionTimeUpdates()

        val underTest = createViewModel(ManageChatHistoryViewModel.Args(CHAT_ID, null))

        underTest.uiState.test {
            val actual = awaitDataState()
            assertThat(actual.chatId).isEqualTo(CHAT_ID)
            assertThat(actual.isMeeting).isTrue()
            assertThat(actual.retention).isEqualTo(ChatHistoryRetention.OneDay)
        }
    }

    @Test
    fun `test that uiState emits the chat room of the contact when only the email is given`() =
        runTest {
            val chatRoom = chatRoom(chatId = CHAT_ID, isMeeting = false, retentionTime = 0L)
            getContactHandleUseCase.stub { on { invoke(EMAIL) } doReturn USER_HANDLE }
            getChatRoomByUserUseCase.stub { on { invoke(USER_HANDLE) } doReturn chatRoom }
            stubRetentionTimeUpdates()

            val underTest = createViewModel(ManageChatHistoryViewModel.Args(null, EMAIL))

            underTest.uiState.test {
                val actual = awaitDataState()
                assertThat(actual.chatId).isEqualTo(CHAT_ID)
            }
            verifyNoInteractions(getChatRoomUseCase)
        }

    @Test
    fun `test that uiState emits Unavailable when the chat room cannot be resolved`() = runTest {
        getChatRoomUseCase.stub { on { invoke(CHAT_ID) } doReturn null }

        val underTest = createViewModel(ManageChatHistoryViewModel.Args(CHAT_ID, null))

        underTest.uiState.test {
            assertThat(awaitUnavailableState()).isEqualTo(ManageChatHistoryUiState.Unavailable)
        }
    }

    @Test
    fun `test that uiState emits Unavailable when neither a chat id nor an email is given`() =
        runTest {
            val underTest = createViewModel(ManageChatHistoryViewModel.Args(null, null))

            underTest.uiState.test {
                assertThat(awaitUnavailableState()).isEqualTo(ManageChatHistoryUiState.Unavailable)
            }
            verifyNoInteractions(getChatRoomUseCase, getContactHandleUseCase)
        }

    @Test
    fun `test that uiState emits Unavailable when the contact handle cannot be resolved`() =
        runTest {
            getContactHandleUseCase.stub { on { invoke(EMAIL) } doReturn null }

            val underTest = createViewModel(ManageChatHistoryViewModel.Args(null, EMAIL))

            underTest.uiState.test {
                assertThat(awaitUnavailableState()).isEqualTo(ManageChatHistoryUiState.Unavailable)
            }
            verifyNoInteractions(getChatRoomByUserUseCase)
        }

    @Test
    fun `test that uiState emits Unavailable when getting the chat room fails`() = runTest {
        getChatRoomUseCase.stub { on { invoke(CHAT_ID) } doThrow RuntimeException("boom") }

        val underTest = createViewModel(ManageChatHistoryViewModel.Args(CHAT_ID, null))

        underTest.uiState.test {
            assertThat(awaitUnavailableState()).isEqualTo(ManageChatHistoryUiState.Unavailable)
        }
    }

    @Test
    fun `test that uiState follows the monitored retention time`() = runTest {
        stubChatRoom(chatId = CHAT_ID, isMeeting = false, retentionTime = 0L)
        whenever(monitorChatRetentionTimeUpdateUseCase(CHAT_ID)) doReturn flow {
            emit(ONE_DAY)
            awaitCancellation()
        }

        val underTest = createViewModel(ManageChatHistoryViewModel.Args(CHAT_ID, null))

        underTest.uiState.test {
            var actual = awaitDataState()
            while (actual.retention != ChatHistoryRetention.OneDay) {
                actual = awaitDataState()
            }
            assertThat(actual.retention).isEqualTo(ChatHistoryRetention.OneDay)
        }
    }

    @Test
    fun `test that onSetRetentionTime sets the retention time on the chat room`() = runTest {
        val underTest = createViewModel(ManageChatHistoryViewModel.Args(CHAT_ID, null))

        underTest.onSetRetention(CHAT_ID, ChatHistoryRetention.OneDay)

        verify(setChatRetentionTimeUseCase).invoke(chatId = CHAT_ID, period = ONE_DAY)
    }

    @Test
    fun `test that onClearHistory reports success when the history is cleared`() = runTest {
        stubChatRoom(chatId = CHAT_ID, isMeeting = false, retentionTime = 0L)
        stubRetentionTimeUpdates()

        val underTest = createViewModel(ManageChatHistoryViewModel.Args(CHAT_ID, null))

        underTest.uiState.test {
            awaitDataState()
            underTest.onClearHistory(CHAT_ID)
            val actual = awaitDataState()
            assertThat(actual.historyClearedEvent)
                .isEqualTo(StateEventWithContentTriggered(true))
        }
        verify(clearChatHistoryUseCase).invoke(CHAT_ID)
    }

    @Test
    fun `test that onClearHistory reports failure when clearing the history fails`() = runTest {
        stubChatRoom(chatId = CHAT_ID, isMeeting = false, retentionTime = 0L)
        stubRetentionTimeUpdates()
        clearChatHistoryUseCase.stub {
            on { invoke(CHAT_ID) } doThrow RuntimeException("boom")
        }

        val underTest = createViewModel(ManageChatHistoryViewModel.Args(CHAT_ID, null))

        underTest.uiState.test {
            awaitDataState()
            underTest.onClearHistory(CHAT_ID)
            val actual = awaitDataState()
            assertThat(actual.historyClearedEvent)
                .isEqualTo(StateEventWithContentTriggered(false))
        }
    }

    @Test
    fun `test that onHistoryClearedEventConsumed clears the clear history outcome`() = runTest {
        stubChatRoom(chatId = CHAT_ID, isMeeting = false, retentionTime = 0L)
        stubRetentionTimeUpdates()

        val underTest = createViewModel(ManageChatHistoryViewModel.Args(CHAT_ID, null))

        underTest.uiState.test {
            awaitDataState()
            underTest.onClearHistory(CHAT_ID)
            awaitDataState()
            underTest.onHistoryClearedEventConsumed()
            val actual = awaitDataState()
            assertThat(actual.historyClearedEvent)
                .isInstanceOf(StateEventWithContentConsumed::class.java)
        }
    }

    private fun createViewModel(args: ManageChatHistoryViewModel.Args) =
        ManageChatHistoryViewModel(
            getChatRoomUseCase = getChatRoomUseCase,
            getChatRoomByUserUseCase = getChatRoomByUserUseCase,
            getContactHandleUseCase = getContactHandleUseCase,
            monitorChatRetentionTimeUpdateUseCase = monitorChatRetentionTimeUpdateUseCase,
            clearChatHistoryUseCase = clearChatHistoryUseCase,
            setChatRetentionTimeUseCase = setChatRetentionTimeUseCase,
            args = args,
        )

    private fun stubChatRoom(chatId: Long, isMeeting: Boolean, retentionTime: Long) {
        getChatRoomUseCase.stub {
            on { invoke(chatId) } doReturn chatRoom(chatId, isMeeting, retentionTime)
        }
    }

    private fun stubRetentionTimeUpdates() {
        whenever(monitorChatRetentionTimeUpdateUseCase(any())) doReturn flow { awaitCancellation() }
    }

    private fun chatRoom(chatId: Long, isMeeting: Boolean, retentionTime: Long) = ChatRoom(
        chatId = chatId,
        ownPrivilege = ChatRoomPermission.Standard,
        numPreviewers = 0L,
        peerPrivilegesByHandles = emptyMap(),
        peerCount = 1L,
        peerHandlesList = listOf(USER_HANDLE),
        peerPrivilegesList = listOf(ChatRoomPermission.Standard),
        isGroup = false,
        isPublic = false,
        isPreview = false,
        authorizationToken = null,
        title = "Chat",
        hasCustomTitle = false,
        unreadCount = 0,
        userTyping = 0L,
        userHandle = USER_HANDLE,
        isActive = true,
        isArchived = false,
        retentionTime = retentionTime,
        creationTime = 0L,
        isMeeting = isMeeting,
        isWaitingRoom = false,
        isOpenInvite = false,
        isSpeakRequest = false,
        isNoteToSelf = false,
    )

    private suspend fun ReceiveTurbine<ManageChatHistoryUiState>.awaitDataState(): ManageChatHistoryUiState.Data {
        var item = awaitItem()
        while (item !is ManageChatHistoryUiState.Data) {
            item = awaitItem()
        }
        return item
    }

    private suspend fun ReceiveTurbine<ManageChatHistoryUiState>.awaitUnavailableState(): ManageChatHistoryUiState {
        var item = awaitItem()
        while (item !is ManageChatHistoryUiState.Unavailable) {
            item = awaitItem()
        }
        return item
    }

    private companion object {
        const val CHAT_ID = 123L
        const val USER_HANDLE = 456L
        const val EMAIL = "contact@mega.co.nz"
        const val ONE_DAY = 86400L
    }
}
