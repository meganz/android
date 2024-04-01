package test.mega.privacy.android.app.presentation.meeting.managechathistory

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.managechathistory.ManageChatHistoryViewModel
import mega.privacy.android.app.presentation.snackbar.MegaSnackbarDuration
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.ClearChatHistoryUseCase
import mega.privacy.android.domain.usecase.chat.GetChatRoomByUserUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatRetentionTimeUpdateUseCase
import mega.privacy.android.domain.usecase.chat.SetChatRetentionTimeUseCase
import mega.privacy.android.domain.usecase.contact.GetContactHandleUseCase
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import java.util.stream.Stream

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ManageChatHistoryViewModelTest {

    private lateinit var underTest: ManageChatHistoryViewModel

    private val monitorChatRetentionTimeUpdateUseCase =
        mock<MonitorChatRetentionTimeUpdateUseCase>()
    private val clearChatHistoryUseCase = mock<ClearChatHistoryUseCase>()
    private val setChatRetentionTimeUseCase = mock<SetChatRetentionTimeUseCase>()
    private val getChatRoomUseCase = mock<GetChatRoomUseCase>()
    private val getContactHandleUseCase = mock<GetContactHandleUseCase>()
    private val getChatRoomByUserUseCase = mock<GetChatRoomByUserUseCase>()
    private val snackBarHandler = mock<SnackBarHandler>()

    private lateinit var savedStateHandle: SavedStateHandle

    private val chatRoomId = 123L
    private val email = "test@test.com"

    @BeforeEach
    fun setUp() {
        savedStateHandle = SavedStateHandle(mapOf("CHAT_ROOM_ID_KEY" to null))
        underTest = ManageChatHistoryViewModel(
            monitorChatRetentionTimeUpdateUseCase = monitorChatRetentionTimeUpdateUseCase,
            clearChatHistoryUseCase = clearChatHistoryUseCase,
            setChatRetentionTimeUseCase = setChatRetentionTimeUseCase,
            getChatRoomUseCase = getChatRoomUseCase,
            getContactHandleUseCase = getContactHandleUseCase,
            getChatRoomByUserUseCase = getChatRoomByUserUseCase,
            savedStateHandle = savedStateHandle,
            snackBarHandler = snackBarHandler
        )
    }

    @AfterEach
    fun resetMocks() {
        wheneverBlocking { monitorChatRetentionTimeUpdateUseCase(chatRoomId) } doReturn emptyFlow()
        reset(
            monitorChatRetentionTimeUpdateUseCase,
            clearChatHistoryUseCase,
            snackBarHandler
        )
    }

    @Test
    fun `test that retention time in state is updated when retention time update is received`() =
        runTest {
            val retentionTime = 100L
            whenever(monitorChatRetentionTimeUpdateUseCase(chatRoomId)) doReturn flowOf(
                retentionTime
            )
            val chatRoom = newChatRoom()
            whenever(getChatRoomUseCase(chatRoomId)) doReturn chatRoom

            underTest.initializeChatRoom(chatId = chatRoomId, email = email)

            underTest.uiState.test {
                assertThat(awaitItem().retentionTimeUpdate).isEqualTo(retentionTime)
            }
        }

    @Test
    fun `test that retention time in state is updated as null when update is consumed`() = runTest {
        val retentionTime = 100L
        whenever(monitorChatRetentionTimeUpdateUseCase(chatRoomId)) doReturn flowOf(retentionTime)
        val chatRoom = newChatRoom()
        whenever(getChatRoomUseCase(chatRoomId)) doReturn chatRoom

        underTest.initializeChatRoom(chatId = chatRoomId, email = email)
        underTest.onRetentionTimeUpdateConsumed()

        underTest.uiState.test {
            assertThat(awaitItem().retentionTimeUpdate).isNull()
        }
    }

    @Test
    fun `test that the chat's history is cleared with the correct chat room ID`() = runTest {
        underTest.clearChatHistory(chatRoomId)

        verify(clearChatHistoryUseCase).invoke(chatRoomId)
    }

    @Test
    fun `test that the clear chat history visibility state is true`() = runTest {
        underTest.showClearChatConfirmation()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().shouldShowClearChatConfirmation).isTrue()
        }
    }

    @Test
    fun `test that the clear chat history visibility state is false when dismissed`() = runTest {
        underTest.showClearChatConfirmation()
        underTest.dismissClearChatConfirmation()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().shouldShowClearChatConfirmation).isFalse()
        }
    }

    @Test
    fun `test that the correct snack bar message is shown after successfully clearing the chat history`() =
        runTest {
            underTest.clearChatHistory(chatRoomId)

            verify(snackBarHandler).postSnackbarMessage(
                resId = R.string.clear_history_success,
                snackbarDuration = MegaSnackbarDuration.Long,
            )
        }

    @Test
    fun `test that the correct snack bar message is shown when clearing the chat history fails`() =
        runTest {
            whenever(clearChatHistoryUseCase(chatRoomId)) doThrow RuntimeException()

            underTest.clearChatHistory(chatRoomId)

            verify(snackBarHandler).postSnackbarMessage(
                resId = R.string.clear_history_error,
                snackbarDuration = MegaSnackbarDuration.Long
            )
        }

    @Test
    fun `test that chat's retention time use case is invoked with the correct parameters`() =
        runTest {
            val period = 321L
            underTest.setChatRetentionTime(chatId = chatRoomId, period = period)

            verify(setChatRetentionTimeUseCase).invoke(chatRoomId, period)
        }

    @ParameterizedTest
    @ValueSource(longs = [123L])
    @NullSource
    fun `test that the chat room id should be set when the chat room is initialized`(chatRoomId: Long?) =
        runTest {
            val newChatRoomId = chatRoomId ?: MEGACHAT_INVALID_HANDLE
            val retentionTime = 100L
            whenever(monitorChatRetentionTimeUpdateUseCase(newChatRoomId)) doReturn flowOf(
                retentionTime
            )
            val chatRoom = newChatRoom()
            whenever(getChatRoomUseCase(newChatRoomId)) doReturn chatRoom

            underTest.initializeChatRoom(chatId = chatRoomId, email = null)

            assertThat(underTest.chatRoomId).isEqualTo(newChatRoomId)
        }

    @Test
    fun `test that the chat room UI state is updated after successfully retrieving the chat room with a valid handle`() =
        runTest {
            val retentionTime = 100L
            whenever(monitorChatRetentionTimeUpdateUseCase(chatRoomId)) doReturn flowOf(
                retentionTime
            )
            val chatRoom = newChatRoom()
            whenever(getChatRoomUseCase(chatRoomId)) doReturn chatRoom

            underTest.initializeChatRoom(chatId = chatRoomId, email = null)

            underTest.chatRoomUiState.test {
                assertThat(expectMostRecentItem()).isEqualTo(chatRoom)
            }
        }

    @ParameterizedTest
    @MethodSource("provideEmailAndContactHandle")
    fun `test that the screen is navigated up`(email: String?, contactHandle: Long?) =
        runTest {
            val retentionTime = 100L
            whenever(monitorChatRetentionTimeUpdateUseCase(chatRoomId)) doReturn flowOf(
                retentionTime
            )
            email?.let {
                whenever(getContactHandleUseCase(it)) doReturn contactHandle
            }

            underTest.initializeChatRoom(chatId = null, email = email)

            underTest.uiState.test {
                assertThat(expectMostRecentItem().shouldNavigateUp).isTrue()
            }
        }

    private fun provideEmailAndContactHandle() = Stream.of(
        Arguments.of(null, 123L),
        Arguments.of("    ", 123L),
        Arguments.of(email, null),
    )

    @Test
    fun `test that the chat room UI state is updated after successfully retrieving the chat room by the user`() =
        runTest {
            val retentionTime = 100L
            whenever(monitorChatRetentionTimeUpdateUseCase(chatRoomId)) doReturn flowOf(
                retentionTime
            )
            val contactHandle = 321L
            whenever(getContactHandleUseCase(email)) doReturn contactHandle
            val chatRoom = newChatRoom()
            whenever(getChatRoomByUserUseCase(contactHandle)) doReturn chatRoom

            underTest.initializeChatRoom(chatId = null, email = email)

            underTest.chatRoomUiState.test {
                assertThat(expectMostRecentItem()).isEqualTo(chatRoom)
            }
        }

    @Test
    fun `test that the navigate up UI state is reset after navigating up`() =
        runTest {
            underTest.initializeChatRoom(chatId = null, email = null)
            underTest.onNavigatedUp()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().shouldNavigateUp).isFalse()
            }
        }

    private fun newChatRoom(
        withChatId: Long = chatRoomId,
        withOwnPrivilege: ChatRoomPermission = ChatRoomPermission.Standard,
        withNumPreviewers: Long = 0L,
        withPeerPrivilegesByHandles: Map<Long, ChatRoomPermission> = mapOf(),
        withPeerCount: Long = 0L,
        withPeerHandlesList: List<Long> = listOf(),
        withPeerPrivilegesList: List<ChatRoomPermission> = listOf(),
        withIsGroup: Boolean = false,
        withIsPublic: Boolean = false,
        withIsPreview: Boolean = false,
        withAuthorizationToken: String? = null,
        withTitle: String = "",
        withHasCustomTitle: Boolean = false,
        withUnreadCount: Int = 0,
        withUserTyping: Long = 0L,
        withUserHandle: Long = 0L,
        withIsActive: Boolean = false,
        withIsArchived: Boolean = false,
        withRetentionTime: Long = 0L,
        withCreationTime: Long = 0L,
        withIsMeeting: Boolean = false,
        withIsWaitingRoom: Boolean = false,
        withIsOpenInvite: Boolean = false,
        withIsSpeakRequest: Boolean = false,
        withChanges: List<ChatRoomChange>? = null,
    ) = ChatRoom(
        chatId = withChatId,
        ownPrivilege = withOwnPrivilege,
        numPreviewers = withNumPreviewers,
        peerPrivilegesByHandles = withPeerPrivilegesByHandles,
        peerCount = withPeerCount,
        peerHandlesList = withPeerHandlesList,
        peerPrivilegesList = withPeerPrivilegesList,
        isGroup = withIsGroup,
        isPublic = withIsPublic,
        isPreview = withIsPreview,
        authorizationToken = withAuthorizationToken,
        title = withTitle,
        hasCustomTitle = withHasCustomTitle,
        unreadCount = withUnreadCount,
        userTyping = withUserTyping,
        userHandle = withUserHandle,
        isActive = withIsActive,
        isArchived = withIsArchived,
        retentionTime = withRetentionTime,
        creationTime = withCreationTime,
        isMeeting = withIsMeeting,
        isWaitingRoom = withIsWaitingRoom,
        isOpenInvite = withIsOpenInvite,
        isSpeakRequest = withIsSpeakRequest,
        changes = withChanges
    )
}
