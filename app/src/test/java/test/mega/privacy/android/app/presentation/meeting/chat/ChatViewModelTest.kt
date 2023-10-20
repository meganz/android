package test.mega.privacy.android.app.presentation.meeting.chat

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.meeting.chat.ChatViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.MonitorChatRoomUpdates
import mega.privacy.android.domain.usecase.chat.GetUserChatStatusByChatUseCase
import mega.privacy.android.domain.usecase.chat.IsChatNotificationMuteUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatViewModelTest {
    private lateinit var underTest: ChatViewModel
    private val getChatRoomUseCase: GetChatRoom = mock()
    private val getUserChatStatusByChatUseCase: GetUserChatStatusByChatUseCase = mock()
    private val savedStateHandle: SavedStateHandle = mock()
    private val isChatNotificationMuteUseCase: IsChatNotificationMuteUseCase = mock()
    private val monitorChatRoomUpdates: MonitorChatRoomUpdates = mock()
    private val monitorUpdatePushNotificationSettingsUseCase
            : MonitorUpdatePushNotificationSettingsUseCase = mock()

    private val chatId = 123L

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initTestClass()
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getChatRoomUseCase,
            savedStateHandle,
            isChatNotificationMuteUseCase,
            monitorChatRoomUpdates,
            monitorUpdatePushNotificationSettingsUseCase,
            getUserChatStatusByChatUseCase
        )
    }

    private fun initTestClass() {
        underTest = ChatViewModel(
            getChatRoomUseCase = getChatRoomUseCase,
            isChatNotificationMuteUseCase = isChatNotificationMuteUseCase,
            monitorChatRoomUpdates = monitorChatRoomUpdates,
            monitorUpdatePushNotificationSettingsUseCase = monitorUpdatePushNotificationSettingsUseCase,
            getUserChatStatusByChatUseCase = getUserChatStatusByChatUseCase,
            savedStateHandle = savedStateHandle
        )
    }

    @Test
    fun `test that title update when we passing the chatId`() = runTest {
        val chatRoom = mock<ChatRoom> {
            on { title } doReturn "title"
        }
        whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
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
        }
        whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().isPrivateChat).isEqualTo(!isPublic)
        }
    }

    @Test
    fun `test that is private chat when call sdk returns chat room is not group chat`() = runTest {
        val chatId = 123L
        val chatRoom = mock<ChatRoom> {
            on { isGroup } doReturn false
        }
        whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().isPrivateChat).isTrue()
        }
    }


    @Test
    fun `test that title not update when chatId is not passed`() = runTest {
        whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(null)
        initTestClass()
        verifyNoInteractions(getChatRoomUseCase)
    }

    @Test
    fun `test that user chat status is not updated if get user chat status by chat returns null`() =
        runTest {
            val chatRoom = mock<ChatRoom> {
                on { isGroup } doReturn true
            }
            whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
            whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
            whenever(getUserChatStatusByChatUseCase(any())).thenReturn(null)
            initTestClass()
            underTest.state.test {
                assertThat(awaitItem().userChatStatus).isEqualTo(null)
            }
        }

    @ParameterizedTest(name = " is {0}")
    @MethodSource("provideUserChatStatusParameters")
    fun `test that user chat status is updated if get user chat status by chat`(
        expectedUserChatStatus: UserChatStatus,
    ) = runTest {
        val chatRoom = mock<ChatRoom> {
            on { isGroup } doReturn false
        }
        whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        whenever(getUserChatStatusByChatUseCase(chatRoom)).thenReturn(expectedUserChatStatus)
        initTestClass()
        testScheduler.advanceUntilIdle()
        underTest.state.map { it.userChatStatus }.distinctUntilChanged().test {
            assertThat(awaitItem()).isEqualTo(expectedUserChatStatus)
        }
    }

    @Test
    fun `test that notification mute icon is not updated when chatId is not passed`() =
        runTest {
            whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(null)
            initTestClass()
            verifyNoInteractions(isChatNotificationMuteUseCase)
        }

    @Test
    fun `test that notification mute icon is shown when mute is enabled`() = runTest {
        whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
        whenever(isChatNotificationMuteUseCase(chatId)).thenReturn(true)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().isChatNotificationMute).isTrue()
        }
    }

    @Test
    fun `test that notification mute icon is hidden when mute is disabled`() = runTest {
        whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
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
        }
        val updateFlow = MutableSharedFlow<ChatRoom>()
        whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        whenever(monitorChatRoomUpdates(chatId)).thenReturn(updateFlow)
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
            whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
            whenever(isChatNotificationMuteUseCase(chatId)).thenReturn(true)
            whenever(monitorUpdatePushNotificationSettingsUseCase()).thenReturn(
                pushNotificationSettingFlow
            )
            whenever(monitorChatRoomUpdates(chatId)).thenReturn(chatRoomUpdate)

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

    private fun provideUserChatStatusParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(UserChatStatus.Offline),
        Arguments.of(UserChatStatus.Away),
        Arguments.of(UserChatStatus.Online),
        Arguments.of(UserChatStatus.Busy),
        Arguments.of(UserChatStatus.Invalid),
    )
}