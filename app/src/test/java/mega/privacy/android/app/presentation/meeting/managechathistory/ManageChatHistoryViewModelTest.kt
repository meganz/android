package mega.privacy.android.app.presentation.meeting.managechathistory

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.chat.mapper.ChatRoomUiMapper
import mega.privacy.android.app.presentation.meeting.managechathistory.ManageChatHistoryViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
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
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import mega.privacy.android.app.presentation.meeting.model.newChatRoom
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

    private lateinit var chatRoomUiMapper: ChatRoomUiMapper

    private lateinit var savedStateHandle: SavedStateHandle

    private val chatRoomId = 123L
    private val email = "test@test.com"

    @BeforeEach
    fun setUp() {
        savedStateHandle = SavedStateHandle()
        initializeViewModel()
    }

    private fun initializeViewModel() {
        chatRoomUiMapper = ChatRoomUiMapper()
        underTest = ManageChatHistoryViewModel(
            monitorChatRetentionTimeUpdateUseCase = monitorChatRetentionTimeUpdateUseCase,
            clearChatHistoryUseCase = clearChatHistoryUseCase,
            setChatRetentionTimeUseCase = setChatRetentionTimeUseCase,
            getChatRoomUseCase = getChatRoomUseCase,
            getContactHandleUseCase = getContactHandleUseCase,
            getChatRoomByUserUseCase = getChatRoomByUserUseCase,
            savedStateHandle = savedStateHandle,
            chatRoomUiMapper = chatRoomUiMapper
        )
    }

    @AfterEach
    fun resetMocks() {
        wheneverBlocking { monitorChatRetentionTimeUpdateUseCase(chatRoomId) } doReturn emptyFlow()
        reset(
            monitorChatRetentionTimeUpdateUseCase,
            clearChatHistoryUseCase,
            setChatRetentionTimeUseCase,
            getChatRoomUseCase,
            getContactHandleUseCase,
            getChatRoomByUserUseCase
        )
    }

    @Test
    fun `test that retention time in state is updated when retention time update is received`() =
        runTest {
            val retentionTime = 100L
            setRetentionTime(
                retentionTime = retentionTime,
                chatRoomId = chatRoomId,
                email = email
            )

            underTest.uiState.test {
                assertThat(expectMostRecentItem().retentionTime).isEqualTo(retentionTime)
            }
        }

    @Test
    fun `test that the chat's history is cleared with the correct chat room ID`() = runTest {
        underTest.clearChatHistory(chatRoomId)

        verify(clearChatHistoryUseCase).invoke(chatRoomId)
    }

    @Test
    fun `test that the correct snack bar message is shown after successfully clearing the chat history`() =
        runTest {
            underTest.clearChatHistory(chatRoomId)

            underTest.uiState.test {
                assertThat(expectMostRecentItem().statusMessageResId).isEqualTo(R.string.clear_history_success)
            }
        }

    @Test
    fun `test that the correct snack bar message is shown when clearing the chat history fails`() =
        runTest {
            whenever(clearChatHistoryUseCase(chatRoomId)) doThrow RuntimeException()

            underTest.clearChatHistory(chatRoomId)

            underTest.uiState.test {
                assertThat(expectMostRecentItem().statusMessageResId).isEqualTo(R.string.clear_history_error)
            }
        }

    @Test
    fun `test that the snack bar message is reset successfully`() =
        runTest {
            whenever(clearChatHistoryUseCase(chatRoomId)) doThrow RuntimeException()

            underTest.clearChatHistory(chatRoomId)
            underTest.onStatusMessageDisplayed()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().statusMessageResId).isNull()
            }
        }

    @Test
    fun `test that chat's retention time updated with the correct parameters`() =
        runTest {
            val period = 321L
            underTest.setChatRetentionTime(period = period)

            verify(setChatRetentionTimeUseCase).invoke(any(), eq(period))
        }

    @ParameterizedTest
    @ValueSource(longs = [123L])
    @NullSource
    fun `test that the chat room id should be set when the chat room is initialized`(chatRoomId: Long?) =
        runTest {
            val newChatRoomId = chatRoomId ?: MEGACHAT_INVALID_HANDLE
            val retentionTime = 100L
            setRetentionTime(chatRoomId = newChatRoomId, retentionTime = retentionTime)

            val actual =
                savedStateHandle.get<Long>(Constants.CHAT_ID) ?: MEGACHAT_INVALID_HANDLE
            assertThat(actual).isEqualTo(newChatRoomId)
        }

    @Test
    fun `test that the chat room UI state is updated after successfully retrieving the chat room`() =
        runTest {
            val chatRoom = newChatRoom(withChatId = chatRoomId)
            val retentionTime = 100L
            whenever(monitorChatRetentionTimeUpdateUseCase(chatRoomId)) doReturn flowOf(
                retentionTime
            )
            whenever(getChatRoomUseCase(chatRoomId)) doReturn chatRoom

            reinitializeViewModelWithProperty(chatRoomId = chatRoomId)

            underTest.uiState.test {
                assertThat(expectMostRecentItem().chatRoom).isEqualTo(chatRoomUiMapper(chatRoom))
            }
        }

    @Test
    fun `test that the chat room UI state is updated to NULL when the returned chat room is NULL`() =
        runTest {
            val chatRoom = null
            val retentionTime = 100L
            whenever(monitorChatRetentionTimeUpdateUseCase(chatRoomId)) doReturn flowOf(
                retentionTime
            )
            whenever(getChatRoomUseCase(chatRoomId)) doReturn chatRoom

            reinitializeViewModelWithProperty(chatRoomId = chatRoomId)

            underTest.uiState.test {
                assertThat(expectMostRecentItem().chatRoom).isNull()
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

            reinitializeViewModelWithProperty(email = email)

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
            val chatRoom = newChatRoom(withChatId = chatRoomId)
            whenever(getChatRoomByUserUseCase(contactHandle)) doReturn chatRoom

            reinitializeViewModelWithProperty(email = email)

            underTest.uiState.test {
                assertThat(expectMostRecentItem().chatRoom).isEqualTo(chatRoomUiMapper(chatRoom))
            }
        }

    @Test
    fun `test that the navigate up UI state is reset after navigating up`() =
        runTest {
            reinitializeViewModelWithProperty()
            underTest.onNavigatedUp()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().shouldNavigateUp).isFalse()
            }
        }

    @Test
    fun `test that the retention time UI state is updated after successfully retrieving the chat room`() =
        runTest {
            val retentionTime = 100L
            setRetentionTime(retentionTime)

            initializeViewModel()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().retentionTime).isEqualTo(retentionTime)
            }
        }

    private suspend fun setRetentionTime(
        retentionTime: Long,
        chatRoomId: Long = this.chatRoomId,
        email: String? = null,
    ) {
        whenever(monitorChatRetentionTimeUpdateUseCase(chatRoomId)) doReturn flowOf(
            retentionTime
        )
        val chatRoom = newChatRoom(withChatId = chatRoomId, withRetentionTime = retentionTime)
        whenever(getChatRoomUseCase(chatRoomId)) doReturn chatRoom

        reinitializeViewModelWithProperty(chatRoomId = chatRoomId, email = email)
    }

    private fun reinitializeViewModelWithProperty(
        chatRoomId: Long? = null,
        email: String? = null,
    ) {
        savedStateHandle = SavedStateHandle(
            mapOf(
                Constants.CHAT_ID to chatRoomId,
                Constants.EMAIL to email
            )
        )
        initializeViewModel()
    }
}
