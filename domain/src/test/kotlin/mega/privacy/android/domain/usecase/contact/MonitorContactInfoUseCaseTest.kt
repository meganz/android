package mega.privacy.android.domain.usecase.contact

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.entity.chat.ChatNotificationMuteState
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.contacts.UserPresence
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.exception.ContactDoesNotExistException
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.GetChatRoomByUserUseCase
import mega.privacy.android.domain.usecase.chat.MonitorCallInChatUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatNotificationMuteStateUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatRetentionTimeUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.shares.MonitorContactInSharesCountUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorContactInfoUseCaseTest {
    private lateinit var underTest: MonitorContactInfoUseCase

    private val getContactFromEmailUseCase = mock<GetContactFromEmailUseCase>()
    private val getContactFromChatUseCase = mock<GetContactFromChatUseCase>()
    private val getChatRoomUseCase = mock<GetChatRoomUseCase>()
    private val getChatRoomByUserUseCase = mock<GetChatRoomByUserUseCase>()
    private val isConnectedToInternetUseCase = mock<IsConnectedToInternetUseCase>()
    private val monitorContactItemUpdatesUseCase = mock<MonitorContactItemUpdatesUseCase>()
    private val monitorUserPresenceUseCase = mock<MonitorUserPresenceUseCase>()
    private val monitorChatNotificationMuteStateUseCase =
        mock<MonitorChatNotificationMuteStateUseCase>()
    private val monitorChatRetentionTimeUseCase = mock<MonitorChatRetentionTimeUseCase>()
    private val monitorContactInSharesCountUseCase = mock<MonitorContactInSharesCountUseCase>()
    private val monitorCallInChatUseCase = mock<MonitorCallInChatUseCase>()
    private val chatRepository = mock<ChatRepository>()

    private val userHandle = 456L
    private val chatId = 789L
    private val email = "contact@mega.nz"
    private val contactItem = ContactItem(
        handle = userHandle,
        email = email,
        contactData = ContactData(
            fullName = "Full Name",
            alias = null,
            avatarUri = null,
            userVisibility = UserVisibility.Visible,
        ),
        defaultAvatarColor = null,
        visibility = UserVisibility.Visible,
        timestamp = 0L,
        areCredentialsVerified = false,
        status = UserChatStatus.Online,
        lastSeen = 5,
        chatroomId = null,
    )

    private lateinit var contactFlow: MutableStateFlow<ContactItem>
    private lateinit var presenceFlow: MutableStateFlow<UserPresence>
    private lateinit var muteStateFlow: MutableStateFlow<ChatNotificationMuteState>
    private lateinit var retentionTimeFlow: MutableStateFlow<Long?>
    private lateinit var callFlow: MutableStateFlow<ChatCall?>
    private lateinit var inSharesCountFlow: MutableStateFlow<Int>
    private lateinit var chatListItemUpdatesFlow: MutableSharedFlow<ChatListItem>

    @BeforeAll
    fun setUp() {
        underTest = MonitorContactInfoUseCase(
            getContactFromEmailUseCase = getContactFromEmailUseCase,
            getContactFromChatUseCase = getContactFromChatUseCase,
            getChatRoomUseCase = getChatRoomUseCase,
            getChatRoomByUserUseCase = getChatRoomByUserUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            monitorContactItemUpdatesUseCase = monitorContactItemUpdatesUseCase,
            monitorUserPresenceUseCase = monitorUserPresenceUseCase,
            monitorChatNotificationMuteStateUseCase = monitorChatNotificationMuteStateUseCase,
            monitorChatRetentionTimeUseCase = monitorChatRetentionTimeUseCase,
            monitorContactInSharesCountUseCase = monitorContactInSharesCountUseCase,
            monitorCallInChatUseCase = monitorCallInChatUseCase,
            chatRepository = chatRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getContactFromEmailUseCase,
            getContactFromChatUseCase,
            getChatRoomUseCase,
            getChatRoomByUserUseCase,
            isConnectedToInternetUseCase,
            monitorContactItemUpdatesUseCase,
            monitorUserPresenceUseCase,
            monitorChatNotificationMuteStateUseCase,
            monitorChatRetentionTimeUseCase,
            monitorContactInSharesCountUseCase,
            monitorCallInChatUseCase,
            chatRepository,
        )
        contactFlow = MutableStateFlow(contactItem)
        presenceFlow = MutableStateFlow(
            UserPresence(status = UserChatStatus.Online, lastGreenMinutes = null)
        )
        muteStateFlow = MutableStateFlow(
            ChatNotificationMuteState(isMuted = false, mutedUntilTimestamp = null)
        )
        retentionTimeFlow = MutableStateFlow(null)
        callFlow = MutableStateFlow(null)
        inSharesCountFlow = MutableStateFlow(0)
        chatListItemUpdatesFlow = MutableSharedFlow()
        whenever(monitorContactItemUpdatesUseCase(contactItem)).thenReturn(contactFlow)
        whenever(monitorUserPresenceUseCase(userHandle)).thenReturn(presenceFlow)
        whenever(monitorChatNotificationMuteStateUseCase(chatId)).thenReturn(muteStateFlow)
        whenever(monitorChatRetentionTimeUseCase(chatId)).thenReturn(retentionTimeFlow)
        whenever(monitorCallInChatUseCase(chatId)).thenReturn(callFlow)
        whenever(monitorContactInSharesCountUseCase(email)).thenReturn(inSharesCountFlow)
        whenever(chatRepository.monitorChatListItemUpdates()).thenReturn(chatListItemUpdatesFlow)
    }

    private suspend fun stubEmailEntry(withChatRoom: Boolean = true) {
        val chatRoom = if (withChatRoom) chatRoom() else null
        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        whenever(getContactFromEmailUseCase(email, true)).thenReturn(contactItem)
        whenever(getChatRoomByUserUseCase(userHandle)).thenReturn(chatRoom)
    }

    private suspend fun stubChatEntry(withContact: Boolean = true) {
        val chatRoom = chatRoom()
        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        whenever(getContactFromChatUseCase(chatId, true))
            .thenReturn(if (withContact) contactItem else null)
    }

    private fun chatRoom(): ChatRoom = mock {
        on { this.chatId } doReturn chatId
        on { title } doReturn "Chat title"
        on { peerHandlesList } doReturn listOf(userHandle)
        on { retentionTime } doReturn 0L
    }

    @Test
    fun `test that invoke resolves the peer from the email entry point`() = runTest {
        stubEmailEntry()

        underTest(email = email, chatId = null).test {
            val state = awaitItem()
            assertThat(state.contactItem).isEqualTo(contactItem)
            assertThat(state.userHandle).isEqualTo(userHandle)
            assertThat(state.chatRoomId).isEqualTo(chatId)
            assertThat(state.chatTitle).isNull()
            assertThat(state.userChatStatus).isEqualTo(UserChatStatus.Online)
        }
    }

    @Test
    fun `test that invoke resolves the peer from the chat entry point`() = runTest {
        stubChatEntry()

        underTest(email = null, chatId = chatId).test {
            val state = awaitItem()
            assertThat(state.contactItem).isEqualTo(contactItem)
            assertThat(state.userHandle).isEqualTo(userHandle)
            assertThat(state.chatRoomId).isEqualTo(chatId)
            assertThat(state.chatTitle).isEqualTo("Chat title")
        }
    }

    @Test
    fun `test that invoke resolves a non-contact peer from the chat room when the contact is not found`() =
        runTest {
            stubChatEntry(withContact = false)

            underTest(email = null, chatId = chatId).test {
                val state = awaitItem()
                assertThat(state.contactItem).isNull()
                assertThat(state.userHandle).isEqualTo(userHandle)
                assertThat(state.chatRoomId).isEqualTo(chatId)
                assertThat(state.chatTitle).isEqualTo("Chat title")
                assertThat(state.inSharesCount).isEqualTo(0)
            }
        }

    @Test
    fun `test that invoke throws ContactDoesNotExistException when no entry point is given`() =
        runTest {
            whenever(isConnectedToInternetUseCase()).thenReturn(true)

            underTest(email = null, chatId = null).test {
                assertThat(awaitError())
                    .isInstanceOf(ContactDoesNotExistException::class.java)
            }
        }

    @Test
    fun `test that invoke throws ContactDoesNotExistException when the email cannot be resolved`() =
        runTest {
            whenever(isConnectedToInternetUseCase()).thenReturn(true)
            whenever(getContactFromEmailUseCase(email, true)).thenReturn(null)

            underTest(email = email, chatId = null).test {
                assertThat(awaitError())
                    .isInstanceOf(ContactDoesNotExistException::class.java)
            }
        }

    @Test
    fun `test that invoke throws ContactDoesNotExistException when the chat has no resolvable peer`() =
        runTest {
            whenever(isConnectedToInternetUseCase()).thenReturn(true)
            whenever(getChatRoomUseCase(chatId)).thenReturn(null)
            whenever(getContactFromChatUseCase(chatId, true)).thenReturn(null)

            underTest(email = null, chatId = chatId).test {
                assertThat(awaitError())
                    .isInstanceOf(ContactDoesNotExistException::class.java)
            }
        }

    @Test
    fun `test that invoke passes the connectivity as skipCache when resolving the peer`() =
        runTest {
            val chatRoom = chatRoom()
            whenever(isConnectedToInternetUseCase()).thenReturn(false)
            whenever(getContactFromEmailUseCase(email, false)).thenReturn(contactItem)
            whenever(getChatRoomByUserUseCase(userHandle)).thenReturn(chatRoom)

            underTest(email = email, chatId = null).test {
                awaitItem()
                verify(getContactFromEmailUseCase)(email, false)
            }
        }

    @Test
    fun `test that invoke emits the updated contact when the contact monitor emits`() = runTest {
        stubEmailEntry()
        val updatedContact = contactItem.copy(
            contactData = contactItem.contactData.copy(alias = "New alias"),
        )

        underTest(email = email, chatId = null).test {
            assertThat(awaitItem().contactItem).isEqualTo(contactItem)
            contactFlow.value = updatedContact
            assertThat(awaitItem().contactItem).isEqualTo(updatedContact)
        }
    }

    @Test
    fun `test that invoke emits the updated presence when the presence monitor emits`() = runTest {
        stubEmailEntry()

        underTest(email = email, chatId = null).test {
            val initial = awaitItem()
            assertThat(initial.userChatStatus).isEqualTo(UserChatStatus.Online)
            assertThat(initial.lastGreenMinutes).isEqualTo(contactItem.lastSeen)
            presenceFlow.value = UserPresence(status = UserChatStatus.Away, lastGreenMinutes = 15)
            val updated = awaitItem()
            assertThat(updated.userChatStatus).isEqualTo(UserChatStatus.Away)
            assertThat(updated.lastGreenMinutes).isEqualTo(15)
        }
    }

    @Test
    fun `test that invoke emits the updated mute state when the mute state monitor emits`() =
        runTest {
            stubEmailEntry()

            underTest(email = email, chatId = null).test {
                val initial = awaitItem()
                assertThat(initial.isNotificationsMuted).isFalse()
                assertThat(initial.notificationsMutedUntilTimestamp).isNull()
                muteStateFlow.value =
                    ChatNotificationMuteState(isMuted = true, mutedUntilTimestamp = 99L)
                val updated = awaitItem()
                assertThat(updated.isNotificationsMuted).isTrue()
                assertThat(updated.notificationsMutedUntilTimestamp).isEqualTo(99L)
            }
        }

    @Test
    fun `test that invoke emits the updated retention time when the retention time monitor emits`() =
        runTest {
            stubEmailEntry()

            underTest(email = email, chatId = null).test {
                assertThat(awaitItem().retentionTimeSeconds).isNull()
                retentionTimeFlow.value = 3600L
                assertThat(awaitItem().retentionTimeSeconds).isEqualTo(3600L)
            }
        }

    @Test
    fun `test that invoke emits the updated in shares count when the in shares monitor emits`() =
        runTest {
            stubEmailEntry()

            underTest(email = email, chatId = null).test {
                assertThat(awaitItem().inSharesCount).isEqualTo(0)
                inSharesCountFlow.value = 3
                assertThat(awaitItem().inSharesCount).isEqualTo(3)
            }
        }

    @Test
    fun `test that invoke emits an ongoing call when the call monitor emits an active call`() =
        runTest {
            stubEmailEntry()
            val call = mock<ChatCall> {
                on { status } doReturn ChatCallStatus.InProgress
            }

            underTest(email = email, chatId = null).test {
                assertThat(awaitItem().hasOngoingCall).isFalse()
                callFlow.value = call
                assertThat(awaitItem().hasOngoingCall).isTrue()
            }
        }

    @Test
    fun `test that invoke does not report an ongoing call when the call is not active`() =
        runTest {
            stubEmailEntry()
            val call = mock<ChatCall> {
                on { status } doReturn ChatCallStatus.UserNoPresent
            }

            underTest(email = email, chatId = null).test {
                assertThat(awaitItem().hasOngoingCall).isFalse()
                callFlow.value = call
                expectNoEvents()
            }
        }

    @Test
    fun `test that invoke attaches the chat scoped monitors when a chat room is created later`() =
        runTest {
            stubEmailEntry(withChatRoom = false)

            underTest(email = email, chatId = null).test {
                val initial = awaitItem()
                assertThat(initial.chatRoomId).isNull()
                assertThat(initial.isNotificationsMuted).isNull()
                assertThat(initial.retentionTimeSeconds).isNull()
                val chatRoom = chatRoom()
                whenever(getChatRoomByUserUseCase(userHandle)).thenReturn(chatRoom)
                muteStateFlow.value =
                    ChatNotificationMuteState(isMuted = true, mutedUntilTimestamp = null)
                chatListItemUpdatesFlow.emit(
                    ChatListItem(chatId = chatId, peerHandle = userHandle, isGroup = false)
                )
                val updated = awaitItem()
                assertThat(updated.chatRoomId).isEqualTo(chatId)
                assertThat(updated.isNotificationsMuted).isTrue()
            }
        }

    @Test
    fun `test that invoke ignores chat list item updates for other peers when waiting for a chat room`() =
        runTest {
            stubEmailEntry(withChatRoom = false)

            underTest(email = email, chatId = null).test {
                assertThat(awaitItem().chatRoomId).isNull()
                chatListItemUpdatesFlow.emit(
                    ChatListItem(chatId = 999L, peerHandle = 111L, isGroup = false)
                )
                expectNoEvents()
            }
        }
}
