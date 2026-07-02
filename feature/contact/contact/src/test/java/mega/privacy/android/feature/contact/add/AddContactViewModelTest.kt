package mega.privacy.android.feature.contact.add

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.entity.contacts.LocalContact
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.call.MonitorParticipantsLimitWarningUseCase
import mega.privacy.android.domain.usecase.contact.GetContactsToAddToChatUseCase
import mega.privacy.android.domain.usecase.contact.GetContactsUseCase
import mega.privacy.android.domain.usecase.contact.GetLocalContactsFromUriUseCase
import mega.privacy.android.domain.usecase.contact.GetLocalContactsUseCase
import mega.privacy.android.domain.usecase.environment.GetDeviceSdkVersionUseCase
import mega.privacy.android.feature.contact.add.model.AddContactUiState
import mega.privacy.android.feature.contact.add.model.PhoneContactsSection
import mega.privacy.android.shared.contact.mapper.ContactItemAvatarMapper
import mega.privacy.android.shared.contact.mapper.ContactItemStatusMapper
import mega.privacy.android.shared.contact.mapper.ContactItemUiStateMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import de.palm.composestateevents.StateEventWithContentTriggered
import java.time.Instant

@ExtendWith(CoroutineMainDispatcherExtension::class)
class AddContactViewModelTest {

    private lateinit var underTest: AddContactViewModel

    private val getContactsUseCase = mock<GetContactsUseCase>()
    private val getContactsToAddToChatUseCase = mock<GetContactsToAddToChatUseCase>()
    private val monitorParticipantsLimitWarningUseCase = mock<MonitorParticipantsLimitWarningUseCase>()
    private val getDeviceSdkVersionUseCase = mock<GetDeviceSdkVersionUseCase>()
    private val getLocalContactsUseCase = mock<GetLocalContactsUseCase>()
    private val getLocalContactsFromUriUseCase = mock<GetLocalContactsFromUriUseCase>()
    private val contactItemUiStateMapper = ContactItemUiStateMapper(
        contactItemStatusMapper = ContactItemStatusMapper(),
        contactItemAvatarMapper = ContactItemAvatarMapper(),
    )

    @BeforeEach
    fun setUp() {
        underTest = createViewModel(chatId = null)
    }

    @AfterEach
    fun tearDown() {
        reset(
            getContactsUseCase,
            getContactsToAddToChatUseCase,
            monitorParticipantsLimitWarningUseCase,
            getDeviceSdkVersionUseCase,
            getLocalContactsUseCase,
            getLocalContactsFromUriUseCase,
        )
    }

    private fun createViewModel(
        chatId: Long?,
        monitorCallLimit: Boolean = false,
        showPhoneContacts: Boolean = false,
    ) = AddContactViewModel(
        chatId = chatId,
        monitorCallLimit = monitorCallLimit,
        showPhoneContacts = showPhoneContacts,
        getContactsUseCase = getContactsUseCase,
        getContactsToAddToChatUseCase = getContactsToAddToChatUseCase,
        monitorParticipantsLimitWarningUseCase = monitorParticipantsLimitWarningUseCase,
        getDeviceSdkVersionUseCase = getDeviceSdkVersionUseCase,
        getLocalContactsUseCase = getLocalContactsUseCase,
        getLocalContactsFromUriUseCase = getLocalContactsFromUriUseCase,
        contactItemUiStateMapper = contactItemUiStateMapper,
    )

    @Test
    fun `test that initial state is Loading`() = runTest {
        stubContactsFlow(emptyList())
        assertThat(underTest.uiState.value).isEqualTo(AddContactUiState.Loading)
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
    fun `test that emailsForSelected returns empty list when nothing is selected`() = runTest {
        stubContactsFlow(listOf(createContactItem(handle = 1L, email = "a@test.com")))

        underTest.uiState.test {
            awaitDataState()
            assertThat(underTest.emailsForSelected(emptySet())).isEmpty()
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
                // Alice is filtered out of the visible list...
                assertThat(state.contacts.map { it.displayName }).containsExactly("Bob")
                // ...but her email still resolves from the retained full list.
                assertThat(underTest.emailsForSelected(setOf(1L)))
                    .containsExactly("a@test.com")
            }
        }

    @Test
    fun `test that contacts come from getContactsToAddToChatUseCase when chatId is set`() = runTest {
        val chatId = 55L
        getContactsToAddToChatUseCase.stub {
            on { invoke(chatId) } doReturn flow {
                emit(listOf(createContactItem(handle = 7L, email = "p@test.com", alias = "Pam")))
                awaitCancellation()
            }
        }
        underTest = createViewModel(chatId = chatId)

        underTest.uiState.test {
            assertThat(awaitDataState().contacts.map { it.displayName }).containsExactly("Pam")
        }
        verifyNoInteractions(getContactsUseCase)
    }

    @Test
    fun `test that user limit warning is shown when monitoring the call limit and the use case emits true`() =
        runTest {
            val chatId = 55L
            getContactsToAddToChatUseCase.stub {
                on { invoke(chatId) } doReturn flow {
                    emit(listOf(createContactItem(handle = 1L, email = "a@test.com")))
                    awaitCancellation()
                }
            }
            monitorParticipantsLimitWarningUseCase.stub {
                on { invoke(chatId) } doReturn flowOf(true)
            }
            underTest = createViewModel(chatId = chatId, monitorCallLimit = true)

            underTest.uiState.test {
                assertThat(awaitDataState().showUserLimitWarning).isTrue()
            }
        }

    @Test
    fun `test that user limit warning is not shown when not monitoring the call limit`() = runTest {
        stubContactsFlow(listOf(createContactItem(handle = 1L, email = "a@test.com")))

        underTest.uiState.test {
            assertThat(awaitDataState().showUserLimitWarning).isFalse()
        }
        verifyNoInteractions(monitorParticipantsLimitWarningUseCase)
    }

    @Test
    fun `test that error in contacts flow is caught`() = runTest {
        getContactsUseCase.stub {
            on { invoke() } doReturn flow<List<ContactItem>> { throw RuntimeException("error") }
        }

        underTest.uiState.test {
            assertThat(awaitItem()).isInstanceOf(AddContactUiState::class.java)
        }
    }

    @Test
    fun `test that phone contacts section is Hidden when showPhoneContacts is false`() = runTest {
        stubContactsFlow(listOf(createContactItem(handle = 1L, email = "a@test.com")))

        underTest.uiState.test {
            assertThat(awaitDataState().phoneContactsSection)
                .isEqualTo(PhoneContactsSection.Hidden)
        }
        verifyNoInteractions(getDeviceSdkVersionUseCase)
    }

    @Test
    fun `test that phone contacts section is PermissionRequired on pre-picker device when permission not granted`() =
        runTest {
            stubContactsFlow(listOf(createContactItem(handle = 1L, email = "a@test.com")))
            whenever(getDeviceSdkVersionUseCase()).thenReturn(PRE_PICKER_SDK)
            underTest = createViewModel(chatId = null, showPhoneContacts = true)

            underTest.uiState.test {
                assertThat(awaitDataState().phoneContactsSection)
                    .isEqualTo(PhoneContactsSection.PermissionRequired)
            }
            verifyNoInteractions(getLocalContactsUseCase)
        }

    @Test
    fun `test that phone contacts section is Loaded on pre-picker device once permission is granted`() =
        runTest {
            stubContactsFlow(listOf(createContactItem(handle = 1L, email = "a@test.com")))
            whenever(getDeviceSdkVersionUseCase()).thenReturn(PRE_PICKER_SDK)
            whenever(getLocalContactsUseCase()).thenReturn(
                listOf(
                    LocalContact(id = 1L, name = "Phone Alice", emails = listOf("pa@test.com")),
                    LocalContact(id = 2L, name = "No Email", emails = emptyList()),
                )
            )
            underTest = createViewModel(chatId = null, showPhoneContacts = true)

            underTest.uiState.test {
                awaitDataState()
                underTest.onReadContactsPermissionGranted()
                val section = awaitDataStateSection()
                assertThat(section).isInstanceOf(PhoneContactsSection.Loaded::class.java)
                val loaded = section as PhoneContactsSection.Loaded
                // Contacts with no email are skipped.
                assertThat(loaded.contacts.map { it.email }).containsExactly("pa@test.com")
            }
        }

    @Test
    fun `test that phone contacts section is PickerAvailable and empty on picker device`() = runTest {
        stubContactsFlow(listOf(createContactItem(handle = 1L, email = "a@test.com")))
        whenever(getDeviceSdkVersionUseCase()).thenReturn(PICKER_SDK)
        underTest = createViewModel(chatId = null, showPhoneContacts = true)

        underTest.uiState.test {
            val section = awaitDataState().phoneContactsSection
            assertThat(section).isInstanceOf(PhoneContactsSection.PickerAvailable::class.java)
            assertThat((section as PhoneContactsSection.PickerAvailable).picked).isEmpty()
        }
    }

    @Test
    fun `test that onContactsPicked appends resolved contacts and triggers the picked event`() =
        runTest {
            stubContactsFlow(listOf(createContactItem(handle = 1L, email = "a@test.com")))
            whenever(getDeviceSdkVersionUseCase()).thenReturn(PICKER_SDK)
            whenever(getLocalContactsFromUriUseCase(any())).thenReturn(
                listOf(LocalContact(id = 1L, name = "Picked", emails = listOf("picked@test.com")))
            )
            underTest = createViewModel(chatId = null, showPhoneContacts = true)

            underTest.uiState.test {
                awaitDataState()
                underTest.onContactsPicked(UriPath("content://picked"))
                var state = awaitDataState()
                while (state.phoneContactsPickedEvent !is StateEventWithContentTriggered) {
                    state = awaitDataState()
                }
                val picked = state.phoneContactsSection as PhoneContactsSection.PickerAvailable
                assertThat(picked.picked.map { it.email }).containsExactly("picked@test.com")
                val event = state.phoneContactsPickedEvent
                check(event is StateEventWithContentTriggered)
                assertThat(event.content).containsExactly("picked@test.com")
            }
        }

    @Test
    fun `test that onContactsPicked de-duplicates by email`() = runTest {
        stubContactsFlow(listOf(createContactItem(handle = 1L, email = "a@test.com")))
        whenever(getDeviceSdkVersionUseCase()).thenReturn(PICKER_SDK)
        whenever(getLocalContactsFromUriUseCase(any())).thenReturn(
            listOf(LocalContact(id = 1L, name = "Picked", emails = listOf("dup@test.com")))
        )
        underTest = createViewModel(chatId = null, showPhoneContacts = true)

        underTest.uiState.test {
            awaitDataState()
            underTest.onContactsPicked(UriPath("content://picked"))
            var state = awaitDataState()
            while ((state.phoneContactsSection as PhoneContactsSection.PickerAvailable).picked.isEmpty()) {
                state = awaitDataState()
            }
            underTest.onPhoneContactsPickedConsumed()
            underTest.onContactsPicked(UriPath("content://picked-again"))

            // Give the second pick a chance to be processed; the list must stay de-duplicated.
            cancelAndConsumeRemainingEvents()
            val picked = (underTest.uiState.value as AddContactUiState.Data)
                .phoneContactsSection as PhoneContactsSection.PickerAvailable
            assertThat(picked.picked.map { it.email }).containsExactly("dup@test.com")
        }
    }

    @Test
    fun `test that emailsForSelected merges mega and phone emails de-duplicated`() = runTest {
        stubContactsFlow(
            listOf(
                createContactItem(handle = 1L, email = "a@test.com", alias = "Alice"),
                createContactItem(handle = 2L, email = "shared@test.com", alias = "Bob"),
            )
        )

        underTest.uiState.test {
            awaitDataState()
            val merged = underTest.emailsForSelected(
                handles = setOf(1L, 2L),
                phoneEmails = setOf("phone@test.com", "shared@test.com"),
            )
            assertThat(merged)
                .containsExactly("a@test.com", "shared@test.com", "phone@test.com")
        }
    }

    private suspend fun ReceiveTurbine<AddContactUiState>.awaitDataStateSection(): PhoneContactsSection {
        return awaitDataState().phoneContactsSection
    }

    private suspend fun ReceiveTurbine<AddContactUiState>.awaitDataState(): AddContactUiState.Data {
        var item = awaitItem()
        while (item !is AddContactUiState.Data) {
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

    private companion object {
        const val PRE_PICKER_SDK = 34
        const val PICKER_SDK = 37
    }
}
