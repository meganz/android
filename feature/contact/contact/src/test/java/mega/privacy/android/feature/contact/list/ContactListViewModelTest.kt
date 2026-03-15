package mega.privacy.android.feature.contact.list

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.contacts.ContactRequestLists
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.usecase.account.contactrequest.MonitorContactRequestsUseCase
import mega.privacy.android.domain.usecase.call.GetChatCallUseCase
import mega.privacy.android.domain.usecase.call.StartCallUseCase
import mega.privacy.android.domain.usecase.chat.Get1On1ChatIdUseCase
import mega.privacy.android.domain.usecase.contact.GetContactsUseCase
import mega.privacy.android.domain.usecase.contact.RemoveContactByEmailUseCase
import mega.privacy.android.feature.contact.list.mapper.ContactItemUiModelMapper
import mega.privacy.android.feature.contact.list.model.ContactListUiState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

@ExtendWith(CoroutineMainDispatcherExtension::class)
class ContactListViewModelTest {

    private lateinit var underTest: ContactListViewModel

    private val getContactsUseCase = mock<GetContactsUseCase>()
    private val get1On1ChatIdUseCase = mock<Get1On1ChatIdUseCase>()
    private val removeContactByEmailUseCase = mock<RemoveContactByEmailUseCase>()
    private val startCallUseCase = mock<StartCallUseCase>()
    private val getChatCallUseCase = mock<GetChatCallUseCase>()
    private val monitorContactRequestsUseCase = mock<MonitorContactRequestsUseCase>()
    private val contactItemUiModelMapper = ContactItemUiModelMapper()

    @BeforeEach
    fun setUp() {
        underTest = ContactListViewModel(
            getContactsUseCase = getContactsUseCase,
            get1On1ChatIdUseCase = get1On1ChatIdUseCase,
            removeContactByEmailUseCase = removeContactByEmailUseCase,
            startCallUseCase = startCallUseCase,
            getChatCallUseCase = getChatCallUseCase,
            monitorContactRequestsUseCase = monitorContactRequestsUseCase,
            contactItemUiModelMapper = contactItemUiModelMapper,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            getContactsUseCase,
            get1On1ChatIdUseCase,
            removeContactByEmailUseCase,
            startCallUseCase,
            getChatCallUseCase,
            monitorContactRequestsUseCase,
        )
    }

    @Test
    fun `test that initial state is Loading`() = runTest {
        assertThat(underTest.uiState.value).isEqualTo(ContactListUiState.Loading)
    }

    @Test
    fun `test that state is Data when contacts use case emits`() = runTest {
        val contacts = listOf(createContactItem(handle = 1L, email = "a@test.com"))
        stubContactsFlow(contacts)

        underTest.uiState.test {
            val state = awaitDataState()
            assertThat(state.contacts).isNotEmpty()
        }
    }

    @Test
    fun `test that contacts are sorted alphabetically`() = runTest {
        val contacts = listOf(
            createContactItem(handle = 1L, email = "z@test.com", alias = "Zara"),
            createContactItem(handle = 2L, email = "a@test.com", alias = "Alice"),
            createContactItem(handle = 3L, email = "m@test.com", alias = "Mike"),
        )
        stubContactsFlow(contacts)

        underTest.uiState.test {
            val state = awaitDataState()
            val allContacts = state.contacts.values.flatten()
            assertThat(allContacts.map { it.displayName })
                .isEqualTo(listOf("Alice", "Mike", "Zara"))
        }
    }

    @Test
    fun `test that contacts are grouped by first character`() = runTest {
        val contacts = listOf(
            createContactItem(handle = 1L, email = "a1@test.com", alias = "Alice"),
            createContactItem(handle = 2L, email = "a2@test.com", alias = "Adam"),
            createContactItem(handle = 3L, email = "b@test.com", alias = "Bob"),
        )
        stubContactsFlow(contacts)

        underTest.uiState.test {
            val state = awaitDataState()
            assertThat(state.contacts.keys).containsExactly("A", "B")
            assertThat(state.contacts["A"]).hasSize(2)
            assertThat(state.contacts["B"]).hasSize(1)
        }
    }

    @Test
    fun `test that contacts are filtered when query is set`() = runTest {
        val contacts = listOf(
            createContactItem(handle = 1L, email = "a@test.com", alias = "Alice"),
            createContactItem(
                handle = 2L,
                email = "a2@test.com",
                fullName = "Alison Brent",
                alias = "null"
            ),
            createContactItem(handle = 3L, email = "b@test.com", alias = "Bob"),
        )
        stubContactsFlow(contacts)

        underTest.setQuery("ali")
        underTest.uiState.test {
            val state = awaitDataState()
            val allContacts = state.contacts.values.flatten()
            assertThat(allContacts).hasSize(2)
            assertThat(allContacts.first().displayName).contains("Ali")
        }
    }

    @Test
    fun `test that recently added contacts include only new contacts`() = runTest {
        val recentTimestamp = Instant.now().epochSecond - 60
        val oldTimestamp = Instant.now().epochSecond - (4 * 24 * 60 * 60)
        val contacts = listOf(
            createContactItem(
                handle = 1L,
                email = "new@test.com",
                alias = "New",
                timestamp = recentTimestamp,
                chatroomId = null
            ),
            createContactItem(
                handle = 2L,
                email = "old@test.com",
                alias = "Old",
                timestamp = oldTimestamp,
                chatroomId = null
            ),
        )
        stubContactsFlow(contacts)

        underTest.uiState.test {
            val state = awaitDataState()
            assertThat(state.recentlyAddedContacts).hasSize(1)
            assertThat(state.recentlyAddedContacts.first().email).isEqualTo("new@test.com")
        }
    }

    @Test
    fun `test that recently added contacts are empty when query is set`() = runTest {
        val recentTimestamp = Instant.now().epochSecond - 60
        val contacts = listOf(
            createContactItem(
                handle = 1L,
                email = "new@test.com",
                alias = "New",
                timestamp = recentTimestamp,
                chatroomId = null
            ),
        )
        stubContactsFlow(contacts)

        underTest.setQuery("new")
        underTest.uiState.test {
            val state = awaitDataState()
            assertThat(state.recentlyAddedContacts).isEmpty()
        }
    }

    @Test
    fun `test that all contacts are displayed when query is cleared`() = runTest {
        val recentTimestamp = Instant.now().epochSecond - 60
        val contacts = listOf(
            createContactItem(
                handle = 1L,
                email = "new@test.com",
                alias = "Include me",
                timestamp = recentTimestamp,
                chatroomId = null
            ),
            createContactItem(handle = 2L, email = "old@old.com", alias = "include me too")
        )
        stubContactsFlow(contacts)

        underTest.setQuery("exclude")
        underTest.uiState.test {
            val state = awaitDataState()
            val noContacts = state.contacts.values.flatten()
            assertThat(noContacts).hasSize(0)
            assertThat(state.recentlyAddedContacts).isEmpty()

            underTest.setQuery(null)
            val newState = awaitDataState()
            val allContacts = newState.contacts.values.flatten()
            assertThat(allContacts).hasSize(2)
            assertThat(newState.recentlyAddedContacts).hasSize(1)
        }
    }

    @Test
    fun `test that incoming request count matches monitor use case`() = runTest {
        val requests =
            listOf(mock<ContactRequest>(), mock<ContactRequest>(), mock<ContactRequest>())
        monitorContactRequestsUseCase.stub {
            on { invoke() } doReturn flowOf(
                ContactRequestLists(
                    incomingContactRequests = requests,
                    outgoingContactRequests = emptyList(),
                )
            )
        }
        stubContactsFlow(emptyList())

        underTest.uiState.test {
            val state = awaitDataState()
            assertThat(state.incomingRequestCount).isEqualTo(3)
        }
    }

    @Test
    fun `test that getChatRoomId triggers open chat event with chat id`() = runTest {
        stubContactsFlow(emptyList())
        whenever(get1On1ChatIdUseCase(any())).thenReturn(456L)

        underTest.uiState.test {
            awaitDataState() // initial data
            underTest.getChatRoomId(123L)
            val state = awaitDataState()
            assertThat(state.openChatEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat((state.openChatEvent as StateEventWithContentTriggered).content).isEqualTo(
                456L
            )
        }
    }

    @Test
    fun `test that onChatEventConsumed clears open chat event`() = runTest {
        stubContactsFlow(emptyList())
        whenever(get1On1ChatIdUseCase(any())).thenReturn(456L)

        underTest.uiState.test {
            awaitDataState()
            underTest.getChatRoomId(123L)
            awaitDataState() // triggered
            underTest.onChatEventConsumed()
            val state = awaitDataState()
            assertThat(state.openChatEvent).isInstanceOf(StateEventWithContentConsumed::class.java)
        }
    }

    @Test
    fun `test that removeContact calls use case with correct email`() = runTest {
        stubContactsFlow(emptyList())

        underTest.removeContact("test@example.com")

        verify(removeContactByEmailUseCase).invoke("test@example.com")
    }

    @Test
    fun `test that onCallTap triggers call event for existing call`() = runTest {
        stubContactsFlow(emptyList())
        whenever(get1On1ChatIdUseCase(any())).thenReturn(789L)
        val existingCall = mock<mega.privacy.android.domain.entity.call.ChatCall>()
        whenever(getChatCallUseCase(789L)).thenReturn(existingCall)

        underTest.uiState.test {
            awaitDataState()
            underTest.onCallTap(userHandle = 1L, video = false, audio = true)
            val state = awaitDataState()
            assertThat(state.startCallEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
            val data = (state.startCallEvent as StateEventWithContentTriggered).content
            assertThat(data.chatId).isEqualTo(789L)
            assertThat(data.isExistingCall).isTrue()
        }
    }

    @Test
    fun `test that onCallTap triggers call event for new call`() = runTest {
        stubContactsFlow(emptyList())
        whenever(get1On1ChatIdUseCase(any())).thenReturn(789L)
        whenever(getChatCallUseCase(789L)).thenReturn(null)
        val chatCall = mock<mega.privacy.android.domain.entity.call.ChatCall> {
            on { hasLocalAudio } doReturn true
            on { hasLocalVideo } doReturn false
        }
        whenever(startCallUseCase(chatId = 789L, audio = true, video = false)).thenReturn(chatCall)

        underTest.uiState.test {
            awaitDataState()
            underTest.onCallTap(userHandle = 1L, video = false, audio = true)
            val state = awaitDataState()
            assertThat(state.startCallEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
            val data = (state.startCallEvent as StateEventWithContentTriggered).content
            assertThat(data.chatId).isEqualTo(789L)
            assertThat(data.isExistingCall).isFalse()
            assertThat(data.hasLocalAudio).isTrue()
            assertThat(data.hasLocalVideo).isFalse()
        }
    }

    @Test
    fun `test that onCallTap handles failure gracefully`() = runTest {
        stubContactsFlow(emptyList())
        whenever(get1On1ChatIdUseCase(any())).thenThrow(RuntimeException("error"))

        underTest.uiState.test {
            awaitDataState()
            underTest.onCallTap(userHandle = 1L, video = false, audio = true)
            // Should not crash, event should remain consumed
            expectNoEvents()
        }
    }

    @Test
    fun `test that onCallEventConsumed clears call event`() = runTest {
        stubContactsFlow(emptyList())
        whenever(get1On1ChatIdUseCase(any())).thenReturn(789L)
        whenever(getChatCallUseCase(789L)).thenReturn(mock())

        underTest.uiState.test {
            awaitDataState()
            underTest.onCallTap(userHandle = 1L, video = false, audio = true)
            awaitDataState() // triggered
            underTest.onCallEventConsumed()
            val state = awaitDataState()
            assertThat(state.startCallEvent).isInstanceOf(StateEventWithContentConsumed::class.java)
        }
    }

    @Test
    fun `test that error in contacts flow is caught`() = runTest {
        getContactsUseCase.stub {
            on { invoke() } doReturn flow { throw RuntimeException("contacts error") }
        }

        underTest.uiState.test {
            // Should not crash - state stays Loading or catches gracefully
            val item = awaitItem()
            assertThat(item).isInstanceOf(ContactListUiState::class.java)
        }
    }

    @Test
    fun `test that state transitions to Data without explicit setQuery call`() = runTest {
        val contacts = listOf(createContactItem(handle = 1L, email = "a@test.com"))
        stubContactsFlow(contacts)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(ContactListUiState.Data::class.java)
        }
    }

    @Test
    fun `test that error in contact requests flow is caught`() = runTest {
        stubContactsFlow(emptyList())
        monitorContactRequestsUseCase.stub {
            on { invoke() } doReturn flow { throw RuntimeException("requests error") }
        }

        underTest.uiState.test {
            val state = awaitDataState()
            // Should have 0 request count since the flow errored
            assertThat(state.incomingRequestCount).isEqualTo(0)
        }
    }

    @Test
    fun `test that contacts with empty display name are grouped under hash when filtered`() =
        runTest {
            val contacts = listOf(
                createContactItem(handle = 1L, email = "", alias = "", fullName = ""),
                createContactItem(handle = 2L, email = "", alias = "", fullName = ""),
            )
            stubContactsFlow(contacts)

            underTest.uiState.test {
                val state = awaitDataState()
                assertThat(state.contacts.keys).containsExactly("#")
            }
        }

    @Test
    fun `test that contacts with empty display name are grouped under hash when filtered and searched`() =
        runTest {
            val contacts = listOf(
                createContactItem(handle = 1L, email = " ", alias = "", fullName = ""),
                createContactItem(handle = 2L, email = " ", alias = "", fullName = ""),
            )
            stubContactsFlow(contacts)

            underTest.setQuery(" ")
            underTest.uiState.test {
                val state = awaitDataState()
                assertThat(state.contacts.keys).containsExactly("#")
            }
        }

    private suspend fun ReceiveTurbine<ContactListUiState>.awaitDataState(): ContactListUiState.Data {
        var item = awaitItem()
        while (item !is ContactListUiState.Data) {
            item = awaitItem()
        }
        return item
    }

    private fun stubContactsFlow(contacts: List<ContactItem>) {
        getContactsUseCase.stub {
            on { invoke() } doReturn flow {
                emit(contacts)
                awaitCancellation()
            }
        }
    }

    private fun createContactItem(
        handle: Long = 1L,
        email: String = "test@example.com",
        alias: String? = "Alias",
        fullName: String? = "Full Name",
        timestamp: Long = Instant.now().epochSecond,
        chatroomId: Long? = 100L,
        status: UserChatStatus = UserChatStatus.Offline,
    ) = ContactItem(
        handle = handle,
        email = email,
        contactData = ContactData(
            fullName = fullName,
            alias = alias,
            avatarUri = null,
            userVisibility = UserVisibility.Visible,
        ),
        defaultAvatarColor = "#FF0000",
        visibility = UserVisibility.Visible,
        timestamp = timestamp,
        areCredentialsVerified = false,
        status = status,
        lastSeen = null,
        chatroomId = chatroomId,
    )
}
