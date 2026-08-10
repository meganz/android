package mega.privacy.android.feature.contact.add

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.scanner.BarcodeScanResult
import mega.privacy.android.core.nodecomponents.scanner.BarcodeScannerModuleIsNotInstalled
import mega.privacy.android.core.nodecomponents.scanner.ScannerHandler
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.qrcode.QRCodeQueryResults
import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.entity.contacts.LocalContact
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.call.MonitorParticipantsLimitWarningUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.contact.GetContactsToAddToChatUseCase
import mega.privacy.android.domain.usecase.contact.GetContactsUseCase
import mega.privacy.android.domain.usecase.contact.GetLocalContactsFromUriUseCase
import mega.privacy.android.domain.usecase.contact.GetLocalContactsUseCase
import mega.privacy.android.domain.usecase.contact.InviteContactWithHandleUseCase
import mega.privacy.android.domain.usecase.IsEmailValidUseCase
import mega.privacy.android.domain.usecase.environment.GetDeviceSdkVersionUseCase
import mega.privacy.android.domain.usecase.qrcode.ParseScannedContactLinkHandleUseCase
import mega.privacy.android.domain.usecase.qrcode.QueryScannedContactLinkUseCase
import mega.privacy.android.feature.contact.add.model.AddContactUiState
import mega.privacy.android.feature.contact.add.model.PhoneContactsSection
import mega.privacy.android.feature.contact.add.model.ScannedContactDialog
import mega.privacy.android.feature.contact.add.model.ScannedContactInviteFeedback
import mega.privacy.android.shared.contact.mapper.ContactItemAvatarMapper
import mega.privacy.android.shared.contact.mapper.ContactItemStatusMapper
import mega.privacy.android.shared.contact.mapper.ContactItemUiStateMapper
import mega.privacy.android.shared.contact.mapper.ScannedContactAvatarMapper
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
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.triggered
import java.time.Instant

@ExtendWith(CoroutineMainDispatcherExtension::class)
class AddContactViewModelTest {

    private lateinit var underTest: AddContactViewModel

    private val getContactsUseCase = mock<GetContactsUseCase>()
    private val getContactsToAddToChatUseCase = mock<GetContactsToAddToChatUseCase>()
    private val monitorParticipantsLimitWarningUseCase = mock<MonitorParticipantsLimitWarningUseCase>()
    private val getContactVerificationWarningUseCase = mock<GetContactVerificationWarningUseCase>()
    private val getDeviceSdkVersionUseCase = mock<GetDeviceSdkVersionUseCase>()
    private val getLocalContactsUseCase = mock<GetLocalContactsUseCase>()
    private val getLocalContactsFromUriUseCase = mock<GetLocalContactsFromUriUseCase>()
    private val contactItemUiStateMapper = ContactItemUiStateMapper(
        contactItemStatusMapper = ContactItemStatusMapper(),
        contactItemAvatarMapper = ContactItemAvatarMapper(),
    )
    private val scannerHandler = mock<ScannerHandler>()
    private val parseScannedContactLinkHandleUseCase = mock<ParseScannedContactLinkHandleUseCase>()
    private val queryScannedContactLinkUseCase = mock<QueryScannedContactLinkUseCase>()
    private val inviteContactWithHandleUseCase = mock<InviteContactWithHandleUseCase>()
    private val scannedContactAvatarMapper = ScannedContactAvatarMapper()

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
            getContactVerificationWarningUseCase,
            getDeviceSdkVersionUseCase,
            getLocalContactsUseCase,
            getLocalContactsFromUriUseCase,
            scannerHandler,
            parseScannedContactLinkHandleUseCase,
            queryScannedContactLinkUseCase,
            inviteContactWithHandleUseCase,
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
        getContactVerificationWarningUseCase = getContactVerificationWarningUseCase,
        getDeviceSdkVersionUseCase = getDeviceSdkVersionUseCase,
        getLocalContactsUseCase = getLocalContactsUseCase,
        getLocalContactsFromUriUseCase = getLocalContactsFromUriUseCase,
        contactItemUiStateMapper = contactItemUiStateMapper,
        scannerHandler = scannerHandler,
        parseScannedContactLinkHandleUseCase = parseScannedContactLinkHandleUseCase,
        queryScannedContactLinkUseCase = queryScannedContactLinkUseCase,
        inviteContactWithHandleUseCase = inviteContactWithHandleUseCase,
        scannedContactAvatarMapper = scannedContactAvatarMapper,
        isEmailValidUseCase = IsEmailValidUseCase(),
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
    fun `test that contact verification warning flag is exposed when the use case returns true`() =
        runTest {
            stubContactsFlow(listOf(createContactItem(handle = 1L, email = "a@test.com")))
            whenever(getContactVerificationWarningUseCase()).thenReturn(true)

            underTest.uiState.test {
                assertThat(awaitDataState().isContactVerificationWarningEnabled).isTrue()
            }
        }

    @Test
    fun `test that contact verification warning flag is false when the use case returns false`() =
        runTest {
            stubContactsFlow(listOf(createContactItem(handle = 1L, email = "a@test.com")))
            whenever(getContactVerificationWarningUseCase()).thenReturn(false)

            underTest.uiState.test {
                assertThat(awaitDataState().isContactVerificationWarningEnabled).isFalse()
            }
        }

    @Test
    fun `test that contact verification warning flag is false when the use case fails`() = runTest {
        stubContactsFlow(listOf(createContactItem(handle = 1L, email = "a@test.com")))
        whenever(getContactVerificationWarningUseCase())
            .thenAnswer { throw RuntimeException("failed") }

        underTest.uiState.test {
            assertThat(awaitDataState().isContactVerificationWarningEnabled).isFalse()
        }
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

    @Test
    fun `test that handleForEmail returns the handle when the email matches case-insensitively`() =
        runTest {
            stubContactsFlow(
                listOf(
                    createContactItem(handle = 1L, email = "alice@test.com", alias = "Alice"),
                    createContactItem(handle = 2L, email = "bob@test.com", alias = "Bob"),
                )
            )

            underTest.uiState.test {
                awaitDataState()
                assertThat(underTest.handleForEmail("ALICE@Test.com")).isEqualTo(1L)
            }
        }

    @Test
    fun `test that handleForEmail returns null when no loaded contact has the email`() = runTest {
        stubContactsFlow(listOf(createContactItem(handle = 1L, email = "alice@test.com")))

        underTest.uiState.test {
            awaitDataState()
            assertThat(underTest.handleForEmail("stranger@test.com")).isNull()
        }
    }

    @Test
    fun `test that handleForEmail resolves a contact that is filtered out by the query`() =
        runTest {
            stubContactsFlow(
                listOf(
                    createContactItem(handle = 1L, email = "alice@test.com", alias = "Alice"),
                    createContactItem(handle = 2L, email = "bob@test.com", alias = "Bob"),
                )
            )

            underTest.setQuery("bob")
            underTest.uiState.test {
                val state = awaitDataState()
                assertThat(state.contacts.map { it.displayName }).containsExactly("Bob")
                assertThat(underTest.handleForEmail("alice@test.com")).isEqualTo(1L)
            }
        }

    @Test
    fun `test that isEmailValid returns true when the email is well formed`() {
        assertThat(underTest.isEmailValid("user@example.com")).isTrue()
    }

    @Test
    fun `test that isEmailValid returns false when the email is malformed`() {
        assertThat(underTest.isEmailValid("not-an-email")).isFalse()
    }

    @Test
    fun `test that onScanQrClicked does nothing when the scan is cancelled`() = runTest {
        stubContactsFlow(emptyList())
        whenever(scannerHandler.scanBarcode()).thenReturn(BarcodeScanResult.Cancelled)

        underTest.uiState.test {
            awaitDataState()
            underTest.onScanQrClicked()
            expectNoEvents()
        }
        verifyNoInteractions(parseScannedContactLinkHandleUseCase, queryScannedContactLinkUseCase)
    }

    @Test
    fun `test that scanner not installed dialog is shown when the scanner module is not installed`() =
        runTest {
            stubContactsFlow(emptyList())
            whenever(scannerHandler.scanBarcode()).thenAnswer {
                throw BarcodeScannerModuleIsNotInstalled()
            }

            underTest.uiState.test {
                awaitDataState()
                underTest.onScanQrClicked()
                assertThat(awaitDialog()).isEqualTo(ScannedContactDialog.ScannerNotInstalled)
            }
        }

    @Test
    fun `test that invalid code dialog is shown when the scanned code is not a contact link`() =
        runTest {
            stubContactsFlow(emptyList())
            whenever(scannerHandler.scanBarcode()).thenReturn(BarcodeScanResult.Success("not a link"))
            whenever(parseScannedContactLinkHandleUseCase("not a link")).thenReturn(null)

            underTest.uiState.test {
                awaitDataState()
                underTest.onScanQrClicked()
                assertThat(awaitDialog()).isEqualTo(ScannedContactDialog.InvalidCode)
            }
            verifyNoInteractions(queryScannedContactLinkUseCase)
        }

    @Test
    fun `test that invalid code dialog is shown when the scanned value is null`() = runTest {
        stubContactsFlow(emptyList())
        whenever(scannerHandler.scanBarcode()).thenReturn(BarcodeScanResult.Success(null))

        underTest.uiState.test {
            awaitDataState()
            underTest.onScanQrClicked()
            assertThat(awaitDialog()).isEqualTo(ScannedContactDialog.InvalidCode)
        }
        verifyNoInteractions(parseScannedContactLinkHandleUseCase, queryScannedContactLinkUseCase)
    }

    @Test
    fun `test that invalid code dialog is shown when the contact link query fails`() = runTest {
        stubContactsFlow(emptyList())
        stubScannedCode(SCANNED_CODE, SCANNED_HANDLE_B64)
        whenever(queryScannedContactLinkUseCase(SCANNED_HANDLE_B64))
            .thenAnswer { throw RuntimeException("query failed") }

        underTest.uiState.test {
            awaitDataState()
            underTest.onScanQrClicked()
            assertThat(awaitDialog()).isEqualTo(ScannedContactDialog.InvalidCode)
        }
    }

    @Test
    fun `test that invalid code dialog is shown when the query result is default`() = runTest {
        stubContactsFlow(emptyList())
        stubScannedCode(SCANNED_CODE, SCANNED_HANDLE_B64)
        whenever(queryScannedContactLinkUseCase(SCANNED_HANDLE_B64)).thenReturn(
            scannedResult(queryResult = QRCodeQueryResults.CONTACT_QUERY_DEFAULT)
        )

        underTest.uiState.test {
            awaitDataState()
            underTest.onScanQrClicked()
            assertThat(awaitDialog()).isEqualTo(ScannedContactDialog.InvalidCode)
        }
    }

    @Test
    fun `test that already added dialog is shown when the query result is EEXIST`() = runTest {
        stubContactsFlow(emptyList())
        stubScannedCode(SCANNED_CODE, SCANNED_HANDLE_B64)
        whenever(queryScannedContactLinkUseCase(SCANNED_HANDLE_B64)).thenReturn(
            scannedResult(queryResult = QRCodeQueryResults.CONTACT_QUERY_EEXIST)
        )

        underTest.uiState.test {
            awaitDataState()
            underTest.onScanQrClicked()
            assertThat(awaitDialog())
                .isEqualTo(ScannedContactDialog.AlreadyAdded("scanned@test.com"))
        }
    }

    @Test
    fun `test that the scanned contact is auto-selected when they are already in the loaded list`() =
        runTest {
            stubContactsFlow(listOf(createContactItem(handle = 42L, email = "scanned@test.com")))
            stubScannedCode(SCANNED_CODE, SCANNED_HANDLE_B64)
            whenever(queryScannedContactLinkUseCase(SCANNED_HANDLE_B64)).thenReturn(
                scannedResult(handle = 42L, isContact = true)
            )

            underTest.uiState.test {
                awaitDataState()
                underTest.onScanQrClicked()
                var state = awaitDataState()
                while (state.scannedContactSelectEvent !is StateEventWithContentTriggered) {
                    state = awaitDataState()
                }
                val event = state.scannedContactSelectEvent
                check(event is StateEventWithContentTriggered)
                assertThat(event.content).isEqualTo(42L)
                assertThat(state.scannedContactDialog).isNull()

                underTest.onScannedContactSelectConsumed()
                assertThat(awaitDataState().scannedContactSelectEvent)
                    .isInstanceOf(StateEventWithContentConsumed::class.java)
            }
        }

    @Test
    fun `test that already added dialog is shown when the scanned contact is not in the loaded list`() =
        runTest {
            stubContactsFlow(listOf(createContactItem(handle = 1L, email = "a@test.com")))
            stubScannedCode(SCANNED_CODE, SCANNED_HANDLE_B64)
            whenever(queryScannedContactLinkUseCase(SCANNED_HANDLE_B64)).thenReturn(
                scannedResult(handle = 42L, isContact = true)
            )

            underTest.uiState.test {
                awaitDataState()
                underTest.onScanQrClicked()
                assertThat(awaitDialog())
                    .isEqualTo(ScannedContactDialog.AlreadyAdded("scanned@test.com"))
            }
        }

    @Test
    fun `test that found dialog is shown when the scanned user is not a contact`() = runTest {
        stubContactsFlow(emptyList())
        val result = scannedResult(handle = 42L, isContact = false)
        stubScannedCode(SCANNED_CODE, SCANNED_HANDLE_B64)
        whenever(queryScannedContactLinkUseCase(SCANNED_HANDLE_B64)).thenReturn(result)

        underTest.uiState.test {
            awaitDataState()
            underTest.onScanQrClicked()
            assertThat(awaitDialog()).isEqualTo(
                ScannedContactDialog.Found(
                    contactName = "Scanned Contact",
                    email = "scanned@test.com",
                    handle = 42L,
                    avatar = scannedContactAvatarMapper(result),
                )
            )
        }
    }

    @Test
    fun `test that sent feedback is fired when the invitation of the scanned contact is sent`() =
        runTest {
            stubContactsFlow(emptyList())
            stubFoundDialog()
            whenever(inviteContactWithHandleUseCase("scanned@test.com", 42L, null))
                .thenReturn(InviteContactRequest.Sent)

            underTest.uiState.test {
                awaitDataState()
                underTest.onScanQrClicked()
                awaitDialog()
                underTest.onInviteScannedContactConfirmed()
                var state = awaitDataState()
                while (state.scannedContactInviteEvent !is StateEventWithContentTriggered) {
                    state = awaitDataState()
                }
                assertThat(state.scannedContactInviteEvent)
                    .isEqualTo(triggered(ScannedContactInviteFeedback.Sent))
                assertThat(state.scannedContactDialog).isNull()

                underTest.onScannedContactInviteConsumed()
                assertThat(awaitDataState().scannedContactInviteEvent)
                    .isInstanceOf(StateEventWithContentConsumed::class.java)
            }
            verify(inviteContactWithHandleUseCase).invoke("scanned@test.com", 42L, null)
        }

    @Test
    fun `test that already added dialog is shown when the invitation reports an existing contact`() =
        runTest {
            stubContactsFlow(emptyList())
            stubFoundDialog()
            whenever(inviteContactWithHandleUseCase("scanned@test.com", 42L, null))
                .thenReturn(InviteContactRequest.AlreadyContact)

            underTest.uiState.test {
                awaitDataState()
                underTest.onScanQrClicked()
                awaitDialog()
                underTest.onInviteScannedContactConfirmed()
                var dialog = awaitDataState().scannedContactDialog
                while (dialog !is ScannedContactDialog.AlreadyAdded) {
                    dialog = awaitDataState().scannedContactDialog
                }
                assertThat(dialog)
                    .isEqualTo(ScannedContactDialog.AlreadyAdded("scanned@test.com"))
            }
        }

    @Test
    fun `test that failed feedback is fired when the invitation fails`() = runTest {
        stubContactsFlow(emptyList())
        stubFoundDialog()
        whenever(inviteContactWithHandleUseCase("scanned@test.com", 42L, null))
            .thenAnswer { throw RuntimeException("invite failed") }

        underTest.uiState.test {
            awaitDataState()
            underTest.onScanQrClicked()
            awaitDialog()
            underTest.onInviteScannedContactConfirmed()
            var state = awaitDataState()
            while (state.scannedContactInviteEvent !is StateEventWithContentTriggered) {
                state = awaitDataState()
            }
            assertThat(state.scannedContactInviteEvent)
                .isEqualTo(triggered(ScannedContactInviteFeedback.Failed))
        }
    }

    @Test
    fun `test that onInviteScannedContactConfirmed does nothing when no found dialog is shown`() =
        runTest {
            stubContactsFlow(emptyList())

            underTest.uiState.test {
                awaitDataState()
                underTest.onInviteScannedContactConfirmed()
                expectNoEvents()
            }
            verifyNoInteractions(inviteContactWithHandleUseCase)
        }

    @Test
    fun `test that onScannedContactDialogDismissed clears the dialog`() = runTest {
        stubContactsFlow(emptyList())
        whenever(scannerHandler.scanBarcode()).thenReturn(BarcodeScanResult.Success(null))

        underTest.uiState.test {
            awaitDataState()
            underTest.onScanQrClicked()
            awaitDialog()
            underTest.onScannedContactDialogDismissed()
            assertThat(awaitDataState().scannedContactDialog).isNull()
        }
    }

    private suspend fun stubScannedCode(code: String, handle: String) {
        whenever(scannerHandler.scanBarcode()).thenReturn(BarcodeScanResult.Success(code))
        whenever(parseScannedContactLinkHandleUseCase(code)).thenReturn(handle)
    }

    private suspend fun stubFoundDialog() {
        stubScannedCode(SCANNED_CODE, SCANNED_HANDLE_B64)
        whenever(queryScannedContactLinkUseCase(SCANNED_HANDLE_B64)).thenReturn(
            scannedResult(handle = 42L, isContact = false)
        )
    }

    private fun scannedResult(
        handle: Long = 42L,
        isContact: Boolean = false,
        queryResult: QRCodeQueryResults = QRCodeQueryResults.CONTACT_QUERY_OK,
    ) = ScannedContactLinkResult(
        contactName = "Scanned Contact",
        email = "scanned@test.com",
        handle = handle,
        isContact = isContact,
        qrCodeQueryResult = queryResult,
    )

    private suspend fun ReceiveTurbine<AddContactUiState>.awaitDialog(): ScannedContactDialog {
        var dialog = awaitDataState().scannedContactDialog
        while (dialog == null) {
            dialog = awaitDataState().scannedContactDialog
        }
        return dialog
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
        const val SCANNED_CODE = "https://mega.nz/C!scannedHandle"
        const val SCANNED_HANDLE_B64 = "scannedHandle"
    }
}
