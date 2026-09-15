package mega.privacy.android.feature.contact.attachment

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.contacts.AttachedContact
import mega.privacy.android.domain.usecase.chat.StartConversationUseCase
import mega.privacy.android.domain.usecase.contact.GetContactsAttachedToChatMessageUseCase
import mega.privacy.android.domain.usecase.contact.InviteContactWithEmailsUseCase
import mega.privacy.android.feature.contact.attachment.model.ContactAttachmentMessage
import mega.privacy.android.feature.contact.attachment.model.ContactAttachmentPrimaryAction
import mega.privacy.android.feature.contact.attachment.model.ContactAttachmentUiState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
class ContactAttachmentViewModelTest {

    private lateinit var underTest: ContactAttachmentViewModel

    private val getContactsAttachedToChatMessageUseCase =
        mock<GetContactsAttachedToChatMessageUseCase>()
    private val inviteContactWithEmailsUseCase = mock<InviteContactWithEmailsUseCase>()
    private val startConversationUseCase = mock<StartConversationUseCase>()

    @BeforeEach
    fun resetMocks() {
        reset(
            getContactsAttachedToChatMessageUseCase,
            inviteContactWithEmailsUseCase,
            startConversationUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            getContactsAttachedToChatMessageUseCase,
            inviteContactWithEmailsUseCase,
            startConversationUseCase,
        )
    }

    private suspend fun initUnderTest(contacts: List<AttachedContact>) {
        whenever(getContactsAttachedToChatMessageUseCase(CHAT_ID, MESSAGE_ID))
            .thenReturn(contacts)
        underTest = ContactAttachmentViewModel(
            chatId = CHAT_ID,
            messageId = MESSAGE_ID,
            getContactsAttachedToChatMessageUseCase = getContactsAttachedToChatMessageUseCase,
            inviteContactWithEmailsUseCase = inviteContactWithEmailsUseCase,
            startConversationUseCase = startConversationUseCase,
        )
    }

    @Test
    fun `test that uiState emits Data with the mapped attached contacts`() = runTest {
        initUnderTest(listOf(contact(avatarColor = 0xFF123456.toInt())))

        underTest.uiState.test {
            val state = awaitItem() as ContactAttachmentUiState.Data
            assertThat(state.contacts).hasSize(1)
            assertThat(state.contacts.first().email).isEqualTo("alice@example.com")
            assertThat(state.contacts.first().avatarColor).isEqualTo(0xFF123456.toInt())
        }
    }

    @Test
    fun `test that primaryAction is Invite when an attached user is not a contact`() = runTest {
        initUnderTest(listOf(contact(isContact = true), contact(isContact = false)))

        underTest.uiState.test {
            val state = awaitItem() as ContactAttachmentUiState.Data
            assertThat(state.primaryAction).isEqualTo(ContactAttachmentPrimaryAction.Invite)
        }
    }

    @Test
    fun `test that primaryAction is StartConversation when all attached users are contacts`() =
        runTest {
            initUnderTest(listOf(contact(isContact = true), contact(isContact = true)))

            underTest.uiState.test {
                val state = awaitItem() as ContactAttachmentUiState.Data
                assertThat(state.primaryAction)
                    .isEqualTo(ContactAttachmentPrimaryAction.StartConversation)
            }
        }

    @Test
    fun `test that primaryAction is null when there are no attached contacts`() = runTest {
        initUnderTest(emptyList())

        underTest.uiState.test {
            val state = awaitItem() as ContactAttachmentUiState.Data
            assertThat(state.primaryAction).isNull()
        }
    }

    @Test
    fun `test that onPrimaryActionClick invites the non-contacts when the action is Invite`() =
        runTest {
            initUnderTest(
                listOf(
                    contact(email = "me@example.com", isMe = true, isContact = false),
                    contact(email = "bob@example.com", isContact = false),
                )
            )
            underTest.uiState.test { awaitItem() }

            underTest.onPrimaryActionClick()

            verify(inviteContactWithEmailsUseCase).invoke(listOf("bob@example.com"))
            verifyNoInteractions(startConversationUseCase)
        }

    @Test
    fun `test that onPrimaryActionClick starts a group conversation when the action is StartConversation`() =
        runTest {
            whenever(startConversationUseCase(any(), any())).thenReturn(CHAT_ROOM_ID)
            initUnderTest(
                listOf(
                    contact(handle = 10L, isContact = true),
                    contact(handle = 20L, isContact = true),
                )
            )
            underTest.uiState.test { awaitItem() }

            underTest.onPrimaryActionClick()

            verify(startConversationUseCase).invoke(isGroup = true, userHandles = listOf(10L, 20L))
        }

    @Test
    fun `test that onPrimaryActionClick emits the open chat event when the conversation is created`() =
        runTest {
            whenever(startConversationUseCase(any(), any())).thenReturn(CHAT_ROOM_ID)
            initUnderTest(listOf(contact(handle = 10L, isContact = true)))

            underTest.uiState.test {
                awaitItem()
                underTest.onPrimaryActionClick()
                val state = awaitItem() as ContactAttachmentUiState.Data
                assertThat(state.openChatEvent).isInstanceOf(
                    StateEventWithContentTriggered::class.java
                )
                assertThat((state.openChatEvent as StateEventWithContentTriggered).content)
                    .isEqualTo(CHAT_ROOM_ID)
            }
        }

    @Test
    fun `test that onStartConversation starts a one to one conversation`() = runTest {
        whenever(startConversationUseCase(any(), any())).thenReturn(CHAT_ROOM_ID)
        initUnderTest(listOf(contact(handle = 10L, isContact = true)))
        underTest.uiState.test { awaitItem() }

        underTest.onStartConversation(handle = 10L)

        verify(startConversationUseCase).invoke(isGroup = false, userHandles = listOf(10L))
    }

    @Test
    fun `test that onInviteContact invites the given email`() = runTest {
        initUnderTest(listOf(contact(isContact = false)))
        underTest.uiState.test { awaitItem() }

        underTest.onInviteContact("bob@example.com")

        verify(inviteContactWithEmailsUseCase).invoke(listOf("bob@example.com"))
    }

    @Test
    fun `test that onInviteContact emits the ContactsInvited message`() = runTest {
        initUnderTest(listOf(contact(isContact = false)))

        underTest.uiState.test {
            awaitItem()
            underTest.onInviteContact("bob@example.com")
            val state = awaitItem() as ContactAttachmentUiState.Data
            assertThat((state.messageEvent as StateEventWithContentTriggered).content)
                .isEqualTo(ContactAttachmentMessage.ContactsInvited)
        }
    }

    @Test
    fun `test that onContactClicked emits UserNotAContact when the tapped user is not a contact`() =
        runTest {
            initUnderTest(listOf(contact(handle = 10L, isContact = false)))

            underTest.uiState.test {
                awaitItem()
                underTest.onContactClicked(handle = 10L)
                val state = awaitItem() as ContactAttachmentUiState.Data
                assertThat((state.messageEvent as StateEventWithContentTriggered).content)
                    .isEqualTo(ContactAttachmentMessage.UserNotAContact)
            }
        }

    @Test
    fun `test that onContactClicked emits the open contact info event when the tapped user is a contact`() =
        runTest {
            initUnderTest(
                listOf(contact(handle = 10L, email = "alice@example.com", isContact = true))
            )

            underTest.uiState.test {
                awaitItem()
                underTest.onContactClicked(handle = 10L)
                val state = awaitItem() as ContactAttachmentUiState.Data
                assertThat((state.openContactInfoEvent as StateEventWithContentTriggered).content)
                    .isEqualTo("alice@example.com")
            }
        }

    private fun contact(
        handle: Long = 1L,
        email: String = "alice@example.com",
        name: String = "Alice",
        isMe: Boolean = false,
        isContact: Boolean = true,
        isVerified: Boolean = false,
        avatarColor: Int = 0xFF2E7D32.toInt(),
    ) = AttachedContact(
        handle = handle,
        email = email,
        name = name,
        isMe = isMe,
        isContact = isContact,
        isVerified = isVerified,
        avatarColor = avatarColor,
    )

    private companion object {
        const val CHAT_ID = 100L
        const val MESSAGE_ID = 200L
        const val CHAT_ROOM_ID = 300L
    }
}
