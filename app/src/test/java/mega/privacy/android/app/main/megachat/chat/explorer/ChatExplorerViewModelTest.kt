package mega.privacy.android.app.main.megachat.chat.explorer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.contact.mapper.UserContactMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.contacts.UserContact
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.usecase.chat.GetActiveChatListItemsUseCase
import mega.privacy.android.domain.usecase.chat.GetArchivedChatListItemsUseCase
import mega.privacy.android.domain.usecase.chat.explorer.GetVisibleContactsWithoutChatRoomUseCase
import mega.privacy.android.domain.usecase.contact.GetContactFromCacheByHandleUseCase
import mega.privacy.android.domain.usecase.contact.GetUserOnlineStatusByHandleUseCase
import mega.privacy.android.domain.usecase.contact.GetUserUseCase
import mega.privacy.android.domain.usecase.contact.RequestUserLastGreenUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatExplorerViewModelTest {

    private lateinit var underTest: ChatExplorerViewModel

    private val getActiveChatListItemsUseCase: GetActiveChatListItemsUseCase = mock()
    private val getArchivedChatListItemsUseCase: GetArchivedChatListItemsUseCase = mock()
    private val getUserUseCase: GetUserUseCase = mock()
    private val getContactFromCacheByHandleUseCase: GetContactFromCacheByHandleUseCase = mock()
    private val getUserOnlineStatusByHandleUseCase: GetUserOnlineStatusByHandleUseCase = mock()
    private val requestUserLastGreenUseCase: RequestUserLastGreenUseCase = mock()
    private val getVisibleContactsWithoutChatRoomUseCase: GetVisibleContactsWithoutChatRoomUseCase =
        mock()
    private lateinit var userContactMapper: UserContactMapper

    @BeforeEach
    fun setUp() {
        userContactMapper = UserContactMapper()
        commonStub()
        underTest = ChatExplorerViewModel(
            getActiveChatListItemsUseCase = getActiveChatListItemsUseCase,
            getArchivedChatListItemsUseCase = getArchivedChatListItemsUseCase,
            getUserUseCase = getUserUseCase,
            getContactFromCacheByHandleUseCase = getContactFromCacheByHandleUseCase,
            getUserOnlineStatusByHandleUseCase = getUserOnlineStatusByHandleUseCase,
            requestUserLastGreenUseCase = requestUserLastGreenUseCase,
            getVisibleContactsWithoutChatRoomUseCase = getVisibleContactsWithoutChatRoomUseCase,
            userContactMapper = userContactMapper
        )
    }

    private fun commonStub() = runTest {
        whenever(getActiveChatListItemsUseCase()) doReturn emptyList()
        whenever(getArchivedChatListItemsUseCase()) doReturn emptyList()
        whenever(getVisibleContactsWithoutChatRoomUseCase()) doReturn emptyList()
        whenever(getUserUseCase(UserId(any()))) doReturn null
    }

    @AfterEach
    fun tearDown() {
        reset(
            getActiveChatListItemsUseCase,
            getArchivedChatListItemsUseCase,
            getUserUseCase,
            getContactFromCacheByHandleUseCase,
            getUserOnlineStatusByHandleUseCase,
            requestUserLastGreenUseCase,
            getVisibleContactsWithoutChatRoomUseCase,
        )
    }

    @Test
    fun `test that the correct active chat list items are added to the chat room list`() = runTest {
        val firstPeerHandle = 98L
        val secondPeerHandle = 9L
        val currentTime = System.currentTimeMillis()
        val firstItem = newChatListItem(
            withChatId = 123L,
            withTitle = "firstItem",
            withLastTimestamp = currentTime,
            withIsGroup = true
        )
        val secondItem = newChatListItem(
            withChatId = 456L,
            withTitle = "secondItem",
            withLastTimestamp = currentTime + 123L,
            withIsGroup = false,
            withPeerHandle = firstPeerHandle
        )
        val thirdItem = newChatListItem(
            withChatId = 789L,
            withTitle = "thirdItem",
            withLastTimestamp = currentTime + 456L,
            withOwnPrivilege = ChatRoomPermission.Moderator,
            withIsGroup = false,
            withPeerHandle = secondPeerHandle
        )
        val fourthItem = newChatListItem(
            withChatId = 12L,
            withLastTimestamp = System.currentTimeMillis(),
            withOwnPrivilege = ChatRoomPermission.Unknown
        )
        val activeChatListItems = listOf(firstItem, secondItem, thirdItem, fourthItem)
        whenever(getActiveChatListItemsUseCase()) doReturn activeChatListItems
        whenever(getUserUseCase(UserId(firstPeerHandle))) doReturn null
        whenever(getContactFromCacheByHandleUseCase(firstPeerHandle)) doReturn null
        val user = newUser()
        whenever(getUserUseCase(UserId(secondPeerHandle))) doReturn user
        val cachedContact = newContact()
        whenever(getContactFromCacheByHandleUseCase(secondPeerHandle)) doReturn cachedContact

        underTest.getChats()

        underTest.uiState.test {
            val expected = listOf(
                ChatExplorerListItem(
                    isRecent = true,
                    isHeader = true
                ),
                ChatExplorerListItem(
                    contactItem = ContactItemUiState(
                        contact = cachedContact,
                        user = user
                    ),
                    chat = thirdItem,
                    title = thirdItem.title,
                    id = thirdItem.chatId.toString(),
                    isRecent = true
                ),
                ChatExplorerListItem(
                    contactItem = null,
                    chat = secondItem,
                    title = secondItem.title,
                    id = secondItem.chatId.toString(),
                    isRecent = true
                ),
                ChatExplorerListItem(
                    contactItem = null,
                    chat = firstItem,
                    title = firstItem.title,
                    id = firstItem.chatId.toString(),
                    isRecent = true
                )
            )
            assertThat(expectMostRecentItem().items).isEqualTo(expected)
        }
    }

    @Test
    fun `test that the total recent items are six`() = runTest {
        val chatIds = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L)
        val activeChats = listOf(
            newChatListItem(withChatId = chatIds[0]),
            newChatListItem(withChatId = chatIds[1]),
            newChatListItem(withChatId = chatIds[2]),
            newChatListItem(withChatId = chatIds[3]),
            newChatListItem(withChatId = chatIds[4]),
            newChatListItem(withChatId = chatIds[5]),
            newChatListItem(withChatId = chatIds[6]),
        )
        whenever(getActiveChatListItemsUseCase()) doReturn activeChats

        underTest.getChats()

        underTest.uiState.test {
            val expected = listOf(
                ChatExplorerListItem(
                    isRecent = true,
                    isHeader = true
                ),
                ChatExplorerListItem(
                    id = chatIds[0].toString(),
                    chat = activeChats[0],
                    title = activeChats[0].title,
                    isRecent = true
                ),
                ChatExplorerListItem(
                    id = chatIds[1].toString(),
                    chat = activeChats[1],
                    title = activeChats[1].title,
                    isRecent = true
                ),
                ChatExplorerListItem(
                    id = chatIds[2].toString(),
                    chat = activeChats[2],
                    title = activeChats[2].title,
                    isRecent = true
                ),
                ChatExplorerListItem(
                    id = chatIds[3].toString(),
                    chat = activeChats[3],
                    title = activeChats[3].title,
                    isRecent = true
                ),
                ChatExplorerListItem(
                    id = chatIds[4].toString(),
                    chat = activeChats[4],
                    title = activeChats[4].title,
                    isRecent = true
                ),
                ChatExplorerListItem(
                    id = chatIds[5].toString(),
                    chat = activeChats[5],
                    title = activeChats[5].title,
                    isRecent = true
                ),
                ChatExplorerListItem(
                    id = chatIds[6].toString(),
                    chat = activeChats[6],
                    title = activeChats[6].title,
                    isRecent = false
                )
            )
            assertThat(expectMostRecentItem().items).isEqualTo(expected)
        }
    }

    @Test
    fun `test that the list of non-active chat rooms only contains archived chat rooms when there are no contacts without chat rooms`() =
        runTest {
            val firstItem = newChatListItem(
                withChatId = 123L,
                withTitle = "firstItem",
                withLastTimestamp = System.currentTimeMillis(),
                withOwnPrivilege = ChatRoomPermission.Moderator,
                withIsGroup = true
            )
            val secondItem = newChatListItem(
                withChatId = 456L,
                withTitle = "secondItem",
                withLastTimestamp = System.currentTimeMillis(),
                withOwnPrivilege = ChatRoomPermission.Moderator,
                withIsGroup = true
            )
            val thirdItem = newChatListItem(
                withChatId = 12L,
                withLastTimestamp = System.currentTimeMillis(),
                withOwnPrivilege = ChatRoomPermission.Unknown
            )
            val archivedChats = listOf(firstItem, secondItem, thirdItem)
            whenever(getArchivedChatListItemsUseCase()) doReturn archivedChats

            underTest.getChats()

            underTest.uiState.test {
                val expected = listOf(
                    ChatExplorerListItem(isHeader = true),
                    ChatExplorerListItem(
                        contactItem = null,
                        chat = firstItem,
                        title = firstItem.title,
                        id = firstItem.chatId.toString()
                    ),
                    ChatExplorerListItem(
                        contactItem = null,
                        chat = secondItem,
                        title = secondItem.title,
                        id = secondItem.chatId.toString()
                    )
                )
                assertThat(expectMostRecentItem().items).isEqualTo(expected)
            }
        }

    @Test
    fun `test that the list of non-active chat rooms only contains contacts without chat rooms when there are no archived chat rooms`() =
        runTest {
            val userContact = UserContact(contact = newContact(), user = newUser())
            whenever(getVisibleContactsWithoutChatRoomUseCase()) doReturn listOf(userContact)

            underTest.getChats()

            underTest.uiState.test {
                val mappedContact = userContactMapper(userContact)
                val expected = listOf(
                    ChatExplorerListItem(isHeader = true),
                    ChatExplorerListItem(
                        contactItem = mappedContact,
                        title = mappedContact.contact?.fullName,
                        id = mappedContact.user?.handle?.toString()
                    )
                )
                assertThat(expectMostRecentItem().items).isEqualTo(expected)
            }
        }

    @Test
    fun `test that the chat room list is empty when it fails to retrieve chat list items`() =
        runTest {
            whenever(getActiveChatListItemsUseCase()) doThrow RuntimeException()
            whenever(getArchivedChatListItemsUseCase()) doThrow RuntimeException()
            whenever(getVisibleContactsWithoutChatRoomUseCase()) doThrow RuntimeException()
            whenever(getUserUseCase(UserId(any()))) doThrow RuntimeException()

            underTest.getChats()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().items).isEmpty()
            }
        }

    @Test
    fun `test that a new selected item is added to the list`() = runTest {
        val item = ChatExplorerListItem(title = "title")

        underTest.addSelectedItem(item)

        underTest.uiState.test {
            assertThat(expectMostRecentItem().selectedItems).isEqualTo(listOf(item))
        }
    }

    @Test
    fun `test that a selected item is removed from the list`() = runTest {
        val item = ChatExplorerListItem(title = "title")

        underTest.addSelectedItem(item)
        underTest.removeSelectedItem(item)

        underTest.uiState.test {
            assertThat(expectMostRecentItem().selectedItems).isEmpty()
        }
    }

    @Test
    fun `test that the selected item list is cleared correctly`() = runTest {
        val item = ChatExplorerListItem(title = "title")

        underTest.addSelectedItem(item)
        underTest.clearSelections()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().selectedItems).isEmpty()
        }
    }

    @Test
    fun `test that last green date is updated correctly for both items and selected items`() =
        runTest {
            val contact = newContact(withUserId = 123L)
            val user = newUser()
            val userContacts = listOf(
                UserContact(contact = contact, user = user),
                UserContact(contact = newContact(), user = newUser())
            )
            whenever(getVisibleContactsWithoutChatRoomUseCase()) doReturn userContacts

            underTest.getChats()
            val contactItem = ContactItemUiState(
                contact = contact,
                user = user
            )
            underTest.addSelectedItem(
                ChatExplorerListItem(
                    contactItem = contactItem,
                    title = "title"
                )
            )
            underTest.addSelectedItem(ChatExplorerListItem(title = "title2"))
            val date = "Last seen Apr 29 15.49"
            underTest.updateItemLastGreenDateByContact(contactItem, date)

            underTest.uiState.test {
                val item = expectMostRecentItem()
                val firstMappedContact = userContactMapper(userContacts[0])
                val secondMappedContact = userContactMapper(userContacts[1])
                val expectedItems = listOf(
                    ChatExplorerListItem(isHeader = true),
                    ChatExplorerListItem(
                        contactItem = firstMappedContact.copy(lastGreen = date),
                        title = firstMappedContact.contact?.fullName,
                        id = firstMappedContact.user?.handle?.toString(),
                    ),
                    ChatExplorerListItem(
                        contactItem = secondMappedContact,
                        title = secondMappedContact.contact?.fullName,
                        id = secondMappedContact.user?.handle?.toString()
                    )
                )
                assertThat(item.items).isEqualTo(expectedItems)

                val expectedSelectedItems = listOf(
                    ChatExplorerListItem(
                        contactItem = contactItem.copy(lastGreen = date),
                        title = "title"
                    ),
                    ChatExplorerListItem(title = "title2")
                )
                assertThat(item.selectedItems).isEqualTo(expectedSelectedItems)
            }
        }

    @Test
    fun `test that the user's online status is requested when getting a contact with valid handle`() =
        runTest {
            val peerHandle = 456L
            val item = newChatListItem(
                withChatId = 123L,
                withTitle = "item",
                withLastTimestamp = System.currentTimeMillis(),
                withOwnPrivilege = ChatRoomPermission.Moderator,
                withPeerHandle = peerHandle
            )
            whenever(getArchivedChatListItemsUseCase()) doReturn listOf(item)
            whenever(getUserUseCase(UserId(peerHandle))) doReturn newUser(withHandle = 2L)
            whenever(getContactFromCacheByHandleUseCase(peerHandle)) doReturn null

            underTest.getChats()

            verify(getUserOnlineStatusByHandleUseCase).invoke(peerHandle)
        }

    @ParameterizedTest(name = "when the current user''s online status is {0}")
    @EnumSource(value = UserChatStatus::class, names = ["Offline", "Away"])
    fun `test that the user's last green time is requested correctly`(userStatus: UserChatStatus) =
        runTest {
            val peerHandle = 456L
            val item = newChatListItem(
                withChatId = 123L,
                withTitle = "item",
                withLastTimestamp = System.currentTimeMillis(),
                withOwnPrivilege = ChatRoomPermission.Moderator,
                withPeerHandle = peerHandle
            )
            whenever(getArchivedChatListItemsUseCase()) doReturn listOf(item)
            val userHandle = 2L
            whenever(getUserUseCase(UserId(peerHandle))) doReturn newUser(withHandle = userHandle)
            whenever(getUserOnlineStatusByHandleUseCase(peerHandle)) doReturn userStatus
            whenever(getContactFromCacheByHandleUseCase(peerHandle)) doReturn null

            underTest.getChats()

            verify(requestUserLastGreenUseCase).invoke(userHandle)
        }

    @ParameterizedTest(name = "when the current user''s online status is {0}")
    @EnumSource(value = UserChatStatus::class, names = ["Online", "Busy", "Invalid"])
    fun `test that the user's last green time is not requested`(userStatus: UserChatStatus) =
        runTest {
            val peerHandle = 456L
            val item = newChatListItem(
                withChatId = 123L,
                withTitle = "item",
                withLastTimestamp = System.currentTimeMillis(),
                withOwnPrivilege = ChatRoomPermission.Moderator,
                withPeerHandle = peerHandle
            )
            whenever(getArchivedChatListItemsUseCase()) doReturn listOf(item)
            val userHandle = 2L
            whenever(getUserUseCase(UserId(peerHandle))) doReturn newUser(withHandle = userHandle)
            whenever(getUserOnlineStatusByHandleUseCase(peerHandle)) doReturn userStatus
            whenever(getContactFromCacheByHandleUseCase(peerHandle)) doReturn null

            underTest.getChats()

            verify(requestUserLastGreenUseCase, never()).invoke(userHandle)
        }

    private fun newChatListItem(
        withChatId: Long = -1L,
        withChanges: ChatListItemChanges? = null,
        withTitle: String = "",
        withOwnPrivilege: ChatRoomPermission = ChatRoomPermission.Standard,
        withUnreadCount: Int = 0,
        withLastMessage: String = "",
        withLastMessageId: Long = -1,
        withLastMessageType: ChatRoomLastMessage = ChatRoomLastMessage.Unknown,
        withLastMessageSender: Long = -1,
        withLastTimestamp: Long = -1,
        withIsGroup: Boolean = false,
        withIsPublic: Boolean = false,
        withIsPreview: Boolean = false,
        withIsActive: Boolean = false,
        withIsArchived: Boolean = false,
        withIsDeleted: Boolean = false,
        withIsCallInProgress: Boolean = false,
        withPeerHandle: Long = -1,
        withLastMessagePriv: Int = 0,
        withLastMessageHandle: Long = -1,
        withNumPreviewers: Long = -1,
    ) = ChatListItem(
        chatId = withChatId,
        changes = withChanges,
        title = withTitle,
        ownPrivilege = withOwnPrivilege,
        unreadCount = withUnreadCount,
        lastMessage = withLastMessage,
        lastMessageId = withLastMessageId,
        lastMessageType = withLastMessageType,
        lastMessageSender = withLastMessageSender,
        lastTimestamp = withLastTimestamp,
        isGroup = withIsGroup,
        isPublic = withIsPublic,
        isPreview = withIsPreview,
        isActive = withIsActive,
        isArchived = withIsArchived,
        isDeleted = withIsDeleted,
        isCallInProgress = withIsCallInProgress,
        peerHandle = withPeerHandle,
        lastMessagePriv = withLastMessagePriv,
        lastMessageHandle = withLastMessageHandle,
        numPreviewers = withNumPreviewers
    )

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

    private fun newContact(
        withUserId: Long = -1L,
        withEmail: String = "",
        withNickname: String? = null,
        withFirstName: String? = null,
        withLastName: String? = null,
        withHasPendingRequest: Boolean = false,
        withIsVisible: Boolean = false,
    ) = Contact(
        userId = withUserId,
        email = withEmail,
        nickname = withNickname,
        firstName = withFirstName,
        lastName = withLastName,
        hasPendingRequest = withHasPendingRequest,
        isVisible = withIsVisible
    )
}
