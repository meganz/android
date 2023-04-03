package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetChatRoomByUser
import mega.privacy.android.domain.usecase.meeting.HangChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.IsParticipatingInChatCallUseCase
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class RemoveContactByEmailUseCaseTest {
    private val nodeRepository: NodeRepository = mock()
    private val hangChatCallUseCase: HangChatCallUseCase = mock()
    private val callRepository: CallRepository = mock()
    private val getChatRoomByUser: GetChatRoomByUser = mock()
    private val getContactFromEmail: GetContactFromEmail = mock()
    private val contactsRepository: ContactsRepository = mock()
    private val isParticipatingInChatCallUseCase: IsParticipatingInChatCallUseCase = mock()
    private val contactItem: ContactItem = mock {
        on { handle }.thenReturn(123456)
    }
    private val chatRoom: ChatRoom = mock {
        on { chatId }.thenReturn(123456789)
    }
    private val chatCall: ChatCall = mock {
        on { callId }.thenReturn(1234567)
    }
    private val testEmail = "test@mega.nz"
    private val underTest = RemoveContactByEmailUseCase(
        nodeRepository,
        hangChatCallUseCase,
        callRepository,
        getChatRoomByUser,
        getContactFromEmail,
        contactsRepository,
        isParticipatingInChatCallUseCase
    )

    @Test
    fun `test that hang chat call is executed if user is participating in any call`() = runTest {
        whenever(isParticipatingInChatCallUseCase()).thenReturn(true)
        whenever(nodeRepository.removedInSharedNodesByEmail(testEmail)).thenAnswer { }
        whenever(getContactFromEmail(testEmail, false)).thenReturn(contactItem)
        whenever(getChatRoomByUser(contactItem.handle)).thenReturn(chatRoom)
        whenever(callRepository.getChatCall(chatRoom.chatId)).thenReturn(chatCall)
        whenever(contactsRepository.removeContact(testEmail)).thenAnswer { }
        underTest(testEmail)
        verify(hangChatCallUseCase, times(1)).invoke(chatCall.callId)
    }

    @Test
    fun `test that hang chat call is not executed if user is not participating in any call`() =
        runTest {
            whenever(isParticipatingInChatCallUseCase()).thenReturn(false)
            whenever(nodeRepository.removedInSharedNodesByEmail(testEmail)).thenAnswer { }
            whenever(getContactFromEmail(testEmail, false)).thenReturn(contactItem)
            whenever(getChatRoomByUser(contactItem.handle)).thenReturn(chatRoom)
            whenever(callRepository.getChatCall(chatRoom.chatId)).thenReturn(chatCall)
            whenever(contactsRepository.removeContact(testEmail)).thenAnswer { }
            underTest(testEmail)
            verifyNoInteractions(hangChatCallUseCase)
        }
}