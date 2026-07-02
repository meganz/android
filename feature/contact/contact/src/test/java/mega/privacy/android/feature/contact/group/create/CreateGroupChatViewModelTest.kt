package mega.privacy.android.feature.contact.group.create

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.usecase.contact.GetContactsUseCase
import mega.privacy.android.feature.contact.group.create.model.CreateGroupChatUiState
import mega.privacy.android.shared.contact.mapper.ContactItemAvatarMapper
import mega.privacy.android.shared.contact.mapper.ContactItemStatusMapper
import mega.privacy.android.shared.contact.mapper.ContactItemUiStateMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import java.time.Instant

@ExtendWith(CoroutineMainDispatcherExtension::class)
class CreateGroupChatViewModelTest {

    private lateinit var underTest: CreateGroupChatViewModel

    private val getContactsUseCase = mock<GetContactsUseCase>()
    private val contactItemUiStateMapper = ContactItemUiStateMapper(
        contactItemStatusMapper = ContactItemStatusMapper(),
        contactItemAvatarMapper = ContactItemAvatarMapper(),
    )

    @BeforeEach
    fun setUp() {
        underTest = createViewModel()
    }

    @AfterEach
    fun tearDown() {
        reset(getContactsUseCase)
    }

    private fun createViewModel() = CreateGroupChatViewModel(
        getContactsUseCase = getContactsUseCase,
        contactItemUiStateMapper = contactItemUiStateMapper,
    )

    @Test
    fun `test that initial state is Loading`() = runTest {
        stubContactsFlow(emptyList())
        assertThat(underTest.uiState.value).isEqualTo(CreateGroupChatUiState.Loading)
    }

    @Test
    fun `test that state is Data when contacts use case emits`() = runTest {
        stubContactsFlow(listOf(createContactItem(handle = 1L, email = "a@test.com")))

        underTest.uiState.test {
            assertThat(awaitDataState().contacts).isNotEmpty()
        }
    }

    @Test
    fun `test that Data is empty when contacts use case emits empty list`() = runTest {
        stubContactsFlow(emptyList())

        underTest.uiState.test {
            assertThat(awaitDataState().isEmpty).isTrue()
        }
    }

    @Test
    fun `test that contacts are sorted alphabetically`() = runTest {
        stubContactsFlow(
            listOf(
                createContactItem(handle = 1L, email = "z@test.com", alias = "Zara"),
                createContactItem(handle = 2L, email = "a@test.com", alias = "Alice"),
                createContactItem(handle = 3L, email = "m@test.com", alias = "Mike"),
            )
        )

        underTest.uiState.test {
            assertThat(awaitDataState().contacts.map { it.displayName })
                .isEqualTo(listOf("Alice", "Mike", "Zara"))
        }
    }

    @Test
    fun `test that contacts are filtered when query is set`() = runTest {
        stubContactsFlow(
            listOf(
                createContactItem(handle = 1L, email = "a@test.com", alias = "Alice"),
                createContactItem(handle = 2L, email = "b@test.com", alias = "Bob"),
            )
        )

        underTest.setQuery("ali")
        underTest.uiState.test {
            val state = awaitDataState()
            assertThat(state.contacts).hasSize(1)
            assertThat(state.contacts.first().displayName).contains("Ali")
        }
    }

    @Test
    fun `test that all contacts are displayed when query is cleared`() = runTest {
        stubContactsFlow(
            listOf(
                createContactItem(handle = 1L, email = "a@test.com", alias = "Alice"),
                createContactItem(handle = 2L, email = "b@test.com", alias = "Bob"),
            )
        )

        underTest.setQuery("exclude")
        underTest.uiState.test {
            assertThat(awaitDataState().contacts).isEmpty()

            underTest.setQuery(null)
            assertThat(awaitDataState().contacts).hasSize(2)
        }
    }

    @Test
    fun `test that emailsForSelected returns the emails of the selected handles`() = runTest {
        stubContactsFlow(
            listOf(
                createContactItem(handle = 1L, email = "a@test.com", alias = "Alice"),
                createContactItem(handle = 2L, email = "b@test.com", alias = "Bob"),
            )
        )

        underTest.uiState.test {
            awaitDataState()
            assertThat(underTest.emailsForSelected(setOf(1L)))
                .containsExactly("a@test.com")
        }
    }

    @Test
    fun `test that emailsForSelected resolves email when the selected contact is filtered out`() =
        runTest {
            stubContactsFlow(
                listOf(
                    createContactItem(handle = 1L, email = "a@test.com", alias = "Alice"),
                    createContactItem(handle = 2L, email = "b@test.com", alias = "Bob"),
                )
            )

            underTest.setQuery("bob")
            underTest.uiState.test {
                val state = awaitDataState()
                assertThat(state.contacts.map { it.displayName }).containsExactly("Bob")
                assertThat(underTest.emailsForSelected(setOf(1L)))
                    .containsExactly("a@test.com")
            }
        }

    @Test
    fun `test that error in contacts flow is caught`() = runTest {
        getContactsUseCase.stub {
            on { invoke() } doReturn flow<List<ContactItem>> { throw RuntimeException("error") }
        }

        underTest.uiState.test {
            assertThat(awaitItem()).isInstanceOf(CreateGroupChatUiState::class.java)
        }
    }

    private suspend fun ReceiveTurbine<CreateGroupChatUiState>.awaitDataState(): CreateGroupChatUiState.Data {
        var item = awaitItem()
        while (item !is CreateGroupChatUiState.Data) {
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
