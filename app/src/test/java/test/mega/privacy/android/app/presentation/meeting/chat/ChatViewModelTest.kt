package test.mega.privacy.android.app.presentation.meeting.chat

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.MonitorChatRoomUpdates
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.chat.HasACallInThisChatByChatIdUseCase
import mega.privacy.android.domain.usecase.chat.IsChatNotificationMuteUseCase
import mega.privacy.android.domain.usecase.chat.MonitorUserChatStatusByHandleUseCase
import mega.privacy.android.domain.usecase.contact.GetUserOnlineStatusByHandleUseCase
import mega.privacy.android.domain.usecase.meeting.IsParticipatingInChatCallUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatViewModelTest {
    private lateinit var underTest: ChatViewModel
    private val getChatRoomUseCase: GetChatRoom = mock()
    private val savedStateHandle: SavedStateHandle = mock()
    private val isChatNotificationMuteUseCase: IsChatNotificationMuteUseCase = mock()
    private val monitorChatRoomUpdates: MonitorChatRoomUpdates = mock {
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
    private val isParticipatingInChatCallUseCase: IsParticipatingInChatCallUseCase = mock()
    private val hasACallInThisChatByChatIdUseCase: HasACallInThisChatByChatIdUseCase = mock()
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase = mock {
        onBlocking { invoke() } doReturn MutableStateFlow(
            StorageStateEvent(
                handle = 1L,
                eventString = "",
                number = 0L,
                text = "",
                type = EventType.Storage,
                storageState = StorageState.Unknown
            )
        )
    }

    private val chatId = 123L
    private val userHandle = 321L

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
            getUserOnlineStatusByHandleUseCase,
            isParticipatingInChatCallUseCase,
            hasACallInThisChatByChatIdUseCase,
        )
        wheneverBlocking { monitorChatRoomUpdates(any()) } doReturn emptyFlow()
        wheneverBlocking { monitorUpdatePushNotificationSettingsUseCase() } doReturn emptyFlow()
        wheneverBlocking { monitorUserChatStatusByHandleUseCase(any()) } doReturn emptyFlow()
        wheneverBlocking { monitorStorageStateEventUseCase() } doReturn MutableStateFlow(
            StorageStateEvent(
                handle = 1L,
                eventString = "",
                number = 0L,
                text = "",
                type = EventType.Storage,
                storageState = StorageState.Unknown
            )
        )
    }

    private fun initTestClass() {
        underTest = ChatViewModel(
            getChatRoomUseCase = getChatRoomUseCase,
            isChatNotificationMuteUseCase = isChatNotificationMuteUseCase,
            monitorChatRoomUpdates = monitorChatRoomUpdates,
            monitorUpdatePushNotificationSettingsUseCase = monitorUpdatePushNotificationSettingsUseCase,
            getUserOnlineStatusByHandleUseCase = getUserOnlineStatusByHandleUseCase,
            monitorUserChatStatusByHandleUseCase = monitorUserChatStatusByHandleUseCase,
            isParticipatingInChatCallUseCase = isParticipatingInChatCallUseCase,
            hasACallInThisChatByChatIdUseCase = hasACallInThisChatByChatIdUseCase,
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            savedStateHandle = savedStateHandle
        )
    }

    @Test
    fun `test that title update when we passing the chatId`() = runTest {
        val chatRoom = mock<ChatRoom> {
            on { title } doReturn "title"
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
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
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
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
        val chatRoom = mock<ChatRoom> {
            on { isGroup } doReturn false
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
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
    fun `test that user chat status is not updated if chat is group`() =
        runTest {
            val chatRoom = mock<ChatRoom> {
                on { isGroup } doReturn true
            }
            whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
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
        whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
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
        whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
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
            on { ownPrivilege } doReturn ChatRoomPermission.Moderator
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
        whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        whenever(monitorChatRoomUpdates(chatId)).thenReturn(updateFlow)
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
    @ValueSource(booleans = [true, false])
    fun `test that is participating in a call has correct state if use case`(
        isParticipatingInChatCall: Boolean,
    ) = runTest {
        whenever(isParticipatingInChatCallUseCase()).thenReturn(isParticipatingInChatCall)
        initTestClass()
        testScheduler.advanceUntilIdle()
        underTest.state.test {
            assertThat(awaitItem().isParticipatingInACall).isEqualTo(isParticipatingInChatCall)
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
        whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
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
        whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
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
        whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().isGroup).isEqualTo(isGroupResult)
        }
    }

    @ParameterizedTest(name = " returns {0}")
    @ValueSource(booleans = [true, false])
    fun `test that has a call in this chat has correct state if use case`(
        hasACallInThisChat: Boolean,
    ) = runTest {
        whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
        whenever(hasACallInThisChatByChatIdUseCase(chatId)).thenReturn(hasACallInThisChat)
        initTestClass()
        testScheduler.advanceUntilIdle()
        underTest.state.test {
            assertThat(awaitItem().hasACallInThisChat).isEqualTo(hasACallInThisChat)
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
        whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        whenever(monitorChatRoomUpdates(chatId)).thenReturn(updateFlow)
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
                eventString = "",
                number = 0L,
                text = "",
                type = EventType.Storage,
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
                eventString = "",
                number = 0L,
                text = "",
                type = EventType.Storage,
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
        whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
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
        whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
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
        whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        whenever(monitorChatRoomUpdates(chatId)).thenReturn(updateFlow)
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
            whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
            whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
            whenever(monitorChatRoomUpdates(chatId)).thenReturn(updateFlow)
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
        whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        whenever(monitorChatRoomUpdates(chatId)).thenReturn(updateFlow)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().isArchived).isFalse()
        }
        val newChatRoom = mock<ChatRoom> {
            on { isArchived } doReturn expectedArchived
            on { changes } doReturn listOf(ChatRoomChange.Archive)
        }
        updateFlow.emit(newChatRoom)
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
        whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initTestClass()
        underTest.state.test {
            assertThat(awaitItem().isArchived).isEqualTo(isArchived)
        }
    }

    private fun provideUserChatStatusParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(UserChatStatus.Offline, UserChatStatus.Away),
        Arguments.of(UserChatStatus.Away, UserChatStatus.Online),
        Arguments.of(UserChatStatus.Online, UserChatStatus.Busy),
        Arguments.of(UserChatStatus.Busy, UserChatStatus.Invalid),
        Arguments.of(UserChatStatus.Invalid, UserChatStatus.Offline),
    )
}