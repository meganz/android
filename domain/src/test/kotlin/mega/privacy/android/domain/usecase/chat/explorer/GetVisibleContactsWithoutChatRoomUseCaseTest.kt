package mega.privacy.android.domain.usecase.chat.explorer

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.contacts.UserContact
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.usecase.chat.GetChatRoomByUserUseCase
import mega.privacy.android.domain.usecase.contact.GetContactFromCacheByHandleUseCase
import mega.privacy.android.domain.usecase.contact.GetUserOnlineStatusByHandleUseCase
import mega.privacy.android.domain.usecase.contact.RequestUserLastGreenUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetVisibleContactsWithoutChatRoomUseCaseTest {

    private lateinit var underTest: GetVisibleContactsWithoutChatRoomUseCase

    private val contactsRepository: ContactsRepository = mock()
    private val getChatRoomByUserUseCase: GetChatRoomByUserUseCase = mock()
    private val getUserOnlineStatusByHandleUseCase: GetUserOnlineStatusByHandleUseCase = mock()
    private val requestUserLastGreenUseCase: RequestUserLastGreenUseCase = mock()
    private val getContactFromCacheByHandleUseCase: GetContactFromCacheByHandleUseCase = mock()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val defaultDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        underTest = GetVisibleContactsWithoutChatRoomUseCase(
            contactsRepository = contactsRepository,
            getChatRoomByUserUseCase = getChatRoomByUserUseCase,
            getUserOnlineStatusByHandleUseCase = getUserOnlineStatusByHandleUseCase,
            requestUserLastGreenUseCase = requestUserLastGreenUseCase,
            getContactFromCacheByHandleUseCase = getContactFromCacheByHandleUseCase,
            defaultDispatcher = defaultDispatcher
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            contactsRepository,
            getChatRoomByUserUseCase,
            getUserOnlineStatusByHandleUseCase,
            requestUserLastGreenUseCase
        )
    }

    @Test
    fun `test that an empty list is returned when the available contacts is not visible`() =
        runTest {
            val availableContacts = listOf(newUser())
            whenever(contactsRepository.getAvailableContacts()) doReturn availableContacts

            val actual = underTest()

            assertThat(actual).isEmpty()
        }

    @Test
    fun `test that an empty list is returned when there is a chat room associated with the contact`() =
        runTest {
            val userHandle = 123L
            val availableContacts = listOf(
                newUser(
                    withHandle = userHandle,
                    withVisibility = UserVisibility.Visible
                )
            )
            whenever(contactsRepository.getAvailableContacts()) doReturn availableContacts
            whenever(getChatRoomByUserUseCase(userHandle)) doReturn newChatRoom()

            val actual = underTest()

            assertThat(actual).isEmpty()
        }

    @Test
    fun `test that the user's online status is not requested when the handle is invalid`() =
        runTest {
            val userHandle = -1L
            val availableContacts = listOf(
                newUser(
                    withHandle = userHandle,
                    withVisibility = UserVisibility.Visible
                )
            )
            whenever(contactsRepository.getAvailableContacts()) doReturn availableContacts
            whenever(getChatRoomByUserUseCase(userHandle)) doReturn null

            underTest()

            verify(getUserOnlineStatusByHandleUseCase, never()).invoke(userHandle)
        }

    @Test
    fun `test that the user's online status is requested when the handle is valid`() =
        runTest {
            val userHandle = 123L
            val availableContacts = listOf(
                newUser(
                    withHandle = userHandle,
                    withVisibility = UserVisibility.Visible
                )
            )
            whenever(contactsRepository.getAvailableContacts()) doReturn availableContacts
            whenever(getChatRoomByUserUseCase(userHandle)) doReturn null

            underTest()

            verify(getUserOnlineStatusByHandleUseCase).invoke(userHandle)
        }

    @ParameterizedTest(name = "when the user's status is {0}")
    @EnumSource(value = UserChatStatus::class, names = ["Offline", "Away"])
    fun `test that the user's last green status is requested`(
        userStatus: UserChatStatus,
    ) = runTest {
        val userHandle = 123L
        val availableContacts = listOf(
            newUser(
                withHandle = userHandle,
                withVisibility = UserVisibility.Visible
            )
        )
        whenever(contactsRepository.getAvailableContacts()) doReturn availableContacts
        whenever(getChatRoomByUserUseCase(userHandle)) doReturn null
        whenever(getUserOnlineStatusByHandleUseCase(userHandle)) doReturn userStatus

        underTest()

        verify(requestUserLastGreenUseCase).invoke(userHandle)
    }

    @ParameterizedTest(name = "when the user's status is {0}")
    @EnumSource(value = UserChatStatus::class, names = ["Online", "Busy", "Invalid"])
    fun `test that the user's last green status is not requested`(
        userStatus: UserChatStatus,
    ) = runTest {
        val userHandle = 123L
        val availableContacts = listOf(
            newUser(
                withHandle = userHandle,
                withVisibility = UserVisibility.Visible
            )
        )
        whenever(contactsRepository.getAvailableContacts()) doReturn availableContacts
        whenever(getChatRoomByUserUseCase(userHandle)) doReturn null
        whenever(getUserOnlineStatusByHandleUseCase(userHandle)) doReturn userStatus

        underTest()

        verify(requestUserLastGreenUseCase, never()).invoke(userHandle)
    }

    @Test
    fun `test that the cached contact's email is returned when it is not NULL`() = runTest {
        val userHandle = 123L
        val user = newUser(
            withHandle = userHandle,
            withVisibility = UserVisibility.Visible
        )
        val availableContacts = listOf(user)
        whenever(contactsRepository.getAvailableContacts()) doReturn availableContacts
        whenever(getChatRoomByUserUseCase(userHandle)) doReturn null
        val contactEmail = "test@test.com"
        val contact = Contact(
            userId = userHandle,
            email = contactEmail
        )
        whenever(getContactFromCacheByHandleUseCase(userHandle)) doReturn contact

        val actual = underTest()

        val expected = listOf(
            UserContact(
                contact = contact.copy(email = contactEmail),
                user = user
            )
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that the contact's email is returned when the cached contact's email is NULL`() =
        runTest {
            val userHandle = 123L
            val userEmail = "test@test.com"
            val user = newUser(
                withHandle = userHandle,
                withEmail = userEmail,
                withVisibility = UserVisibility.Visible
            )
            val availableContacts = listOf(user)
            whenever(contactsRepository.getAvailableContacts()) doReturn availableContacts
            whenever(getChatRoomByUserUseCase(userHandle)) doReturn null
            val contact = Contact(
                userId = userHandle,
                email = null
            )
            whenever(getContactFromCacheByHandleUseCase(userHandle)) doReturn contact

            val actual = underTest()

            val expected = listOf(
                UserContact(
                    contact = contact.copy(email = userEmail),
                    user = user
                )
            )
            assertThat(actual).isEqualTo(expected)
        }

    private fun newUser(
        withHandle: Long = -1L,
        withEmail: String = "",
        withVisibility: UserVisibility = UserVisibility.Unknown,
        withTimestamp: Long = 0L,
        withUserChanges: List<UserChanges> = emptyList(),
    ) = User(
        handle = withHandle,
        email = withEmail,
        visibility = withVisibility,
        timestamp = withTimestamp,
        userChanges = withUserChanges
    )

    private fun newChatRoom(
        withChatId: Long = -1L,
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
