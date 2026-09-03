package mega.privacy.android.feature.contact.invite

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.sharedcomponents.scanner.BarcodeScanResult
import mega.privacy.android.core.sharedcomponents.scanner.BarcodeScanner
import mega.privacy.android.core.sharedcomponents.scanner.BarcodeScannerModuleIsNotInstalled
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.contacts.EmailInvitationsInputValidity
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.contacts.LocalContact
import mega.privacy.android.domain.entity.qrcode.QRCodeQueryResults
import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.IsEmailValidUseCase
import mega.privacy.android.domain.usecase.call.AreThereOngoingVideoCallsUseCase
import mega.privacy.android.domain.usecase.contact.GetLocalContactsFromUriUseCase
import mega.privacy.android.domain.usecase.contact.GetLocalContactsUseCase
import mega.privacy.android.domain.usecase.contact.InviteContactWithEmailsUseCase
import mega.privacy.android.domain.usecase.contact.InviteContactWithHandleUseCase
import mega.privacy.android.domain.usecase.contact.ValidateEmailInputForInvitationUseCase
import mega.privacy.android.domain.usecase.environment.GetDeviceSdkVersionUseCase
import mega.privacy.android.domain.usecase.qrcode.CreateContactLinkUseCase
import mega.privacy.android.domain.usecase.qrcode.ParseScannedContactLinkHandleUseCase
import mega.privacy.android.domain.usecase.qrcode.QueryScannedContactLinkUseCase
import mega.privacy.android.feature.contact.invite.mapper.InvitationMessageMapper
import mega.privacy.android.feature.contact.invite.model.EmailValidationMessage
import mega.privacy.android.feature.contact.invite.model.InvitationResult
import mega.privacy.android.feature.contact.invite.model.InviteContactUiState
import mega.privacy.android.feature.contact.invite.model.InviteMessage
import mega.privacy.android.feature.contact.picker.PhoneContactsSection
import mega.privacy.android.feature.contact.picker.ScannedContactDialog
import mega.privacy.android.feature.contact.picker.ScannedContactInviteFeedback
import mega.privacy.android.shared.contact.mapper.ScannedContactAvatarMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class InviteContactViewModelTest {

    private lateinit var underTest: InviteContactViewModel

    private val getDeviceSdkVersionUseCase = mock<GetDeviceSdkVersionUseCase>()
    private val getLocalContactsUseCase = mock<GetLocalContactsUseCase>()
    private val getLocalContactsFromUriUseCase = mock<GetLocalContactsFromUriUseCase>()
    private val validateEmailInputForInvitationUseCase =
        mock<ValidateEmailInputForInvitationUseCase>()
    private val inviteContactWithEmailsUseCase = mock<InviteContactWithEmailsUseCase>()
    private val createContactLinkUseCase = mock<CreateContactLinkUseCase>()
    private val areThereOngoingVideoCallsUseCase = mock<AreThereOngoingVideoCallsUseCase>()
    private val scannerHandler = mock<BarcodeScanner>()
    private val parseScannedContactLinkHandleUseCase = mock<ParseScannedContactLinkHandleUseCase>()
    private val queryScannedContactLinkUseCase = mock<QueryScannedContactLinkUseCase>()
    private val inviteContactWithHandleUseCase = mock<InviteContactWithHandleUseCase>()
    private val scannedContactAvatarMapper = ScannedContactAvatarMapper()
    private val invitationMessageMapper = InvitationMessageMapper()

    @BeforeEach
    fun setUp() {
        underTest = createViewModel(isFromAchievement = false)
    }

    @AfterEach
    fun tearDown() {
        reset(
            getDeviceSdkVersionUseCase,
            getLocalContactsUseCase,
            getLocalContactsFromUriUseCase,
            validateEmailInputForInvitationUseCase,
            inviteContactWithEmailsUseCase,
            createContactLinkUseCase,
            areThereOngoingVideoCallsUseCase,
            scannerHandler,
            parseScannedContactLinkHandleUseCase,
            queryScannedContactLinkUseCase,
            inviteContactWithHandleUseCase,
        )
    }

    private fun createViewModel(isFromAchievement: Boolean) = InviteContactViewModel(
        isFromAchievement = isFromAchievement,
        getDeviceSdkVersionUseCase = getDeviceSdkVersionUseCase,
        getLocalContactsUseCase = getLocalContactsUseCase,
        getLocalContactsFromUriUseCase = getLocalContactsFromUriUseCase,
        isEmailValidUseCase = IsEmailValidUseCase(),
        validateEmailInputForInvitationUseCase = validateEmailInputForInvitationUseCase,
        inviteContactWithEmailsUseCase = inviteContactWithEmailsUseCase,
        createContactLinkUseCase = createContactLinkUseCase,
        areThereOngoingVideoCallsUseCase = areThereOngoingVideoCallsUseCase,
        scannerHandler = scannerHandler,
        parseScannedContactLinkHandleUseCase = parseScannedContactLinkHandleUseCase,
        queryScannedContactLinkUseCase = queryScannedContactLinkUseCase,
        inviteContactWithHandleUseCase = inviteContactWithHandleUseCase,
        scannedContactAvatarMapper = scannedContactAvatarMapper,
        invitationMessageMapper = invitationMessageMapper,
    )

    @Test
    fun `test that initial state is Loading`() = runTest(extension.testDispatcher) {
        assertThat(underTest.uiState.value).isEqualTo(InviteContactUiState.Loading)
    }

    @Test
    fun `test that state becomes Data once collected`() = runTest(extension.testDispatcher) {
        underTest.uiState.test {
            assertThat(awaitDataState()).isInstanceOf(InviteContactUiState.Data::class.java)
        }
    }

    @Test
    fun `test that the contact link is loaded into the state`() = runTest(extension.testDispatcher) {
        whenever(createContactLinkUseCase(false)).thenReturn(CONTACT_LINK)

        underTest.uiState.test {
            var state = awaitDataState()
            while (state.contactLink == null) {
                state = awaitDataState()
            }
            assertThat(state.contactLink).isEqualTo(CONTACT_LINK)
        }
    }

    @Test
    fun `test that phone contacts section is PickerAvailable on picker devices`() =
        runTest(extension.testDispatcher) {
            whenever(getDeviceSdkVersionUseCase()).thenReturn(PICKER_SDK)

            underTest.uiState.test {
                assertThat(awaitDataState().phoneContactsSection)
                    .isInstanceOf(PhoneContactsSection.PickerAvailable::class.java)
            }
        }

    @Test
    fun `test that phone contacts section is PermissionRequired on pre-picker devices`() =
        runTest(extension.testDispatcher) {
            whenever(getDeviceSdkVersionUseCase()).thenReturn(PRE_PICKER_SDK)

            underTest.uiState.test {
                assertThat(awaitDataState().phoneContactsSection)
                    .isEqualTo(PhoneContactsSection.PermissionRequired)
            }
        }

    @Test
    fun `test that onContactsPicked renders the picked contacts including a phone-only one`() =
        runTest(extension.testDispatcher) {
            whenever(getDeviceSdkVersionUseCase()).thenReturn(PICKER_SDK)
            whenever(getLocalContactsFromUriUseCase(any(), any())).thenReturn(pickedLocalContacts())

            underTest.uiState.test {
                awaitDataState()
                underTest.onContactsPicked(UriPath("content://picked"))
                var section = awaitDataState().phoneContactsSection
                while (section !is PhoneContactsSection.PickerAvailable || section.picked.size < 2) {
                    section = awaitDataState().phoneContactsSection
                }
                assertThat(section.picked.map { it.displayName })
                    .containsExactly("Emailed", "Phone Only")
                cancelAndIgnoreRemainingEvents()
            }
            verify(getLocalContactsFromUriUseCase).invoke(UriPath("content://picked"), true)
        }

    @Test
    fun `test that onContactsPicked fires an event carrying picked emails and phone numbers`() =
        runTest(extension.testDispatcher) {
            whenever(getDeviceSdkVersionUseCase()).thenReturn(PICKER_SDK)
            whenever(getLocalContactsFromUriUseCase(any(), any())).thenReturn(pickedLocalContacts())

            underTest.uiState.test {
                awaitDataState()
                underTest.onContactsPicked(UriPath("content://picked"))
                var state = awaitDataState()
                while (state.phoneContactsPickedEvent !is StateEventWithContentTriggered) {
                    state = awaitDataState()
                }
                val event = state.phoneContactsPickedEvent
                check(event is StateEventWithContentTriggered)
                assertThat(event.content.phoneNumbers).containsExactly("+441234567")
                assertThat(event.content.emails).containsExactly("e@test.com")
            }
        }

    @Test
    fun `test that a valid manual email fires a manualEmailAcceptedEvent`() =
        runTest(extension.testDispatcher) {
            whenever(validateEmailInputForInvitationUseCase("user@test.com"))
                .thenReturn(EmailInvitationsInputValidity.Valid)

            underTest.uiState.test {
                awaitDataState()
                underTest.validateManualInput("user@test.com")
                var state = awaitDataState()
                while (state.manualEmailAcceptedEvent !is StateEventWithContentTriggered) {
                    state = awaitDataState()
                }
                val event = state.manualEmailAcceptedEvent
                check(event is StateEventWithContentTriggered)
                assertThat(event.content).isEqualTo("user@test.com")
            }
        }

    @Test
    fun `test that a valid manual phone number fires a manualPhoneAcceptedEvent`() =
        runTest(extension.testDispatcher) {
            underTest.uiState.test {
                awaitDataState()
                underTest.validateManualInput("+441234567")
                var state = awaitDataState()
                while (state.manualPhoneAcceptedEvent !is StateEventWithContentTriggered) {
                    state = awaitDataState()
                }
                val event = state.manualPhoneAcceptedEvent
                check(event is StateEventWithContentTriggered)
                assertThat(event.content).isEqualTo("+441234567")
            }
            verifyNoInteractions(validateEmailInputForInvitationUseCase)
        }

    @Test
    fun `test that a malformed manual input triggers an InvalidInput validation event`() =
        runTest(extension.testDispatcher) {
            underTest.uiState.test {
                awaitDataState()
                underTest.validateManualInput("not-an-email")
                assertThat(awaitEmailValidationMessage())
                    .isEqualTo(EmailValidationMessage.InvalidInput)
            }
            verifyNoInteractions(validateEmailInputForInvitationUseCase)
        }

    @Test
    fun `test that own email triggers a MyOwnEmail validation event`() =
        runTest(extension.testDispatcher) {
            whenever(validateEmailInputForInvitationUseCase("me@test.com"))
                .thenReturn(EmailInvitationsInputValidity.MyOwnEmail)

            underTest.uiState.test {
                awaitDataState()
                underTest.validateManualInput("me@test.com")
                assertThat(awaitEmailValidationMessage())
                    .isEqualTo(EmailValidationMessage.MyOwnEmail)
            }
        }

    @Test
    fun `test that an already-invited email triggers a Pending validation event`() =
        runTest(extension.testDispatcher) {
            whenever(validateEmailInputForInvitationUseCase("pending@test.com"))
                .thenReturn(EmailInvitationsInputValidity.Pending)

            underTest.uiState.test {
                awaitDataState()
                underTest.validateManualInput("pending@test.com")
                assertThat(awaitEmailValidationMessage()).isEqualTo(EmailValidationMessage.Pending)
            }
        }

    @Test
    fun `test that emails are invited before SMS is requested after the delay`() =
        runTest(extension.testDispatcher) {
            whenever(createContactLinkUseCase(false)).thenReturn(CONTACT_LINK)
            whenever(inviteContactWithEmailsUseCase(listOf("a@test.com")))
                .thenReturn(listOf(InviteContactRequest.Sent))
            val collector = launch { underTest.uiState.collect {} }
            advanceUntilIdle()

            underTest.inviteContacts(emails = setOf("a@test.com"), phones = setOf("+441234567"))
            runCurrent()

            val afterEmail = underTest.uiState.value as InviteContactUiState.Data
            assertThat(afterEmail.invitationResultEvent)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat(afterEmail.sendSmsEvent)
                .isInstanceOf(StateEventWithContentConsumed::class.java)

            advanceUntilIdle()

            val afterDelay = underTest.uiState.value as InviteContactUiState.Data
            val sms = afterDelay.sendSmsEvent
            check(sms is StateEventWithContentTriggered)
            assertThat(sms.content.phoneNumbers).containsExactly("+441234567")
            assertThat(sms.content.contactLink).isEqualTo(CONTACT_LINK)
            collector.cancel()
        }

    @Test
    fun `test that phones-only invitations request SMS immediately without inviting emails`() =
        runTest(extension.testDispatcher) {
            whenever(createContactLinkUseCase(false)).thenReturn(CONTACT_LINK)
            val collector = launch { underTest.uiState.collect {} }
            advanceUntilIdle()

            underTest.inviteContacts(emails = emptySet(), phones = setOf("+441234567"))
            runCurrent()

            val state = underTest.uiState.value as InviteContactUiState.Data
            val sms = state.sendSmsEvent
            check(sms is StateEventWithContentTriggered)
            assertThat(sms.content.phoneNumbers).containsExactly("+441234567")
            collector.cancel()
            verifyNoInteractions(inviteContactWithEmailsUseCase)
        }

    @Test
    fun `test that an Achievement result carries the sent count when launched from achievements`() =
        runTest(extension.testDispatcher) {
            underTest = createViewModel(isFromAchievement = true)
            whenever(inviteContactWithEmailsUseCase(listOf("a@test.com", "b@test.com")))
                .thenReturn(listOf(InviteContactRequest.Sent, InviteContactRequest.Sent))
            val collector = launch { underTest.uiState.collect {} }
            advanceUntilIdle()

            underTest.inviteContacts(
                emails = setOf("a@test.com", "b@test.com"),
                phones = emptySet(),
            )
            advanceUntilIdle()

            val state = underTest.uiState.value as InviteContactUiState.Data
            val event = state.invitationResultEvent
            check(event is StateEventWithContentTriggered)
            assertThat(event.content).isEqualTo(InvitationResult.Achievement(sentNumber = 2))
            collector.cancel()
        }

    @Test
    fun `test that a snackbar result is produced for a non-achievement invite`() =
        runTest(extension.testDispatcher) {
            whenever(inviteContactWithEmailsUseCase(listOf("a@test.com")))
                .thenReturn(listOf(InviteContactRequest.Sent))
            val collector = launch { underTest.uiState.collect {} }
            advanceUntilIdle()

            underTest.inviteContacts(emails = setOf("a@test.com"), phones = emptySet())
            advanceUntilIdle()

            val state = underTest.uiState.value as InviteContactUiState.Data
            val event = state.invitationResultEvent
            check(event is StateEventWithContentTriggered)
            val snackbar = event.content as InvitationResult.Snackbar
            assertThat(snackbar.messages).containsExactly(InviteMessage.Sent(count = 1))
            collector.cancel()
        }

    @Test
    fun `test that a failure inviting by email is caught`() = runTest(extension.testDispatcher) {
        whenever(inviteContactWithEmailsUseCase(any()))
            .thenAnswer { throw RuntimeException("invite failed") }
        val collector = launch { underTest.uiState.collect {} }
        advanceUntilIdle()

        underTest.inviteContacts(emails = setOf("a@test.com"), phones = emptySet())
        advanceUntilIdle()

        val state = underTest.uiState.value as InviteContactUiState.Data
        assertThat(state.invitationResultEvent)
            .isInstanceOf(StateEventWithContentConsumed::class.java)
        collector.cancel()
    }

    @Test
    fun `test that scanning requests confirmation when there is an ongoing video call`() =
        runTest(extension.testDispatcher) {
            whenever(areThereOngoingVideoCallsUseCase()).thenReturn(true)

            underTest.uiState.test {
                awaitDataState()
                underTest.onScanClicked()
                var state = awaitDataState()
                while (state.ongoingCallConfirmEvent !is de.palm.composestateevents.StateEvent.Triggered) {
                    state = awaitDataState()
                }
                assertThat(state.ongoingCallConfirmEvent)
                    .isInstanceOf(de.palm.composestateevents.StateEvent.Triggered::class.java)
            }
            verifyNoInteractions(scannerHandler)
        }

    @Test
    fun `test that scanning starts immediately when there is no ongoing call`() =
        runTest(extension.testDispatcher) {
            whenever(areThereOngoingVideoCallsUseCase()).thenReturn(false)
            whenever(scannerHandler.scanBarcode()).thenReturn(BarcodeScanResult.Cancelled)

            underTest.uiState.test {
                awaitDataState()
                underTest.onScanClicked()
                advanceUntilIdle()
            }
            verify(scannerHandler).scanBarcode()
        }

    @Test
    fun `test that a found dialog is shown when the scanned user is not a contact`() =
        runTest(extension.testDispatcher) {
            whenever(areThereOngoingVideoCallsUseCase()).thenReturn(false)
            val result = scannedResult(isContact = false)
            stubScannedCode(SCANNED_CODE, SCANNED_HANDLE)
            whenever(queryScannedContactLinkUseCase(SCANNED_HANDLE)).thenReturn(result)

            underTest.uiState.test {
                awaitDataState()
                underTest.onScanClicked()
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
    fun `test that an already added dialog is shown when the query result is EEXIST`() =
        runTest(extension.testDispatcher) {
            whenever(areThereOngoingVideoCallsUseCase()).thenReturn(false)
            stubScannedCode(SCANNED_CODE, SCANNED_HANDLE)
            whenever(queryScannedContactLinkUseCase(SCANNED_HANDLE)).thenReturn(
                scannedResult(queryResult = QRCodeQueryResults.CONTACT_QUERY_EEXIST)
            )

            underTest.uiState.test {
                awaitDataState()
                underTest.onScanClicked()
                assertThat(awaitDialog())
                    .isEqualTo(ScannedContactDialog.AlreadyAdded("scanned@test.com"))
            }
        }

    @Test
    fun `test that an invalid code dialog is shown when parsing fails`() =
        runTest(extension.testDispatcher) {
            whenever(areThereOngoingVideoCallsUseCase()).thenReturn(false)
            whenever(scannerHandler.scanBarcode())
                .thenReturn(BarcodeScanResult.Success(SCANNED_CODE))
            whenever(parseScannedContactLinkHandleUseCase(SCANNED_CODE)).thenReturn(null)

            underTest.uiState.test {
                awaitDataState()
                underTest.onScanClicked()
                assertThat(awaitDialog()).isEqualTo(ScannedContactDialog.InvalidCode)
            }
            verifyNoInteractions(queryScannedContactLinkUseCase)
        }

    @Test
    fun `test that the scanner not installed dialog is shown when the module is missing`() =
        runTest(extension.testDispatcher) {
            whenever(areThereOngoingVideoCallsUseCase()).thenReturn(false)
            whenever(scannerHandler.scanBarcode()).thenAnswer {
                throw BarcodeScannerModuleIsNotInstalled()
            }

            underTest.uiState.test {
                awaitDataState()
                underTest.onScanClicked()
                assertThat(awaitDialog()).isEqualTo(ScannedContactDialog.ScannerNotInstalled)
            }
        }

    @Test
    fun `test that sent feedback is fired when the scanned contact invitation is sent`() =
        runTest(extension.testDispatcher) {
            whenever(areThereOngoingVideoCallsUseCase()).thenReturn(false)
            stubFoundDialog()
            whenever(inviteContactWithHandleUseCase("scanned@test.com", 42L, null))
                .thenReturn(InviteContactRequest.Sent)

            underTest.uiState.test {
                awaitDataState()
                underTest.onScanClicked()
                awaitDialog()
                underTest.onInviteScannedContactConfirmed()
                var state = awaitDataState()
                while (state.scannedContactInviteEvent !is StateEventWithContentTriggered) {
                    state = awaitDataState()
                }
                assertThat(state.scannedContactInviteEvent)
                    .isEqualTo(triggered(ScannedContactInviteFeedback.Sent))
                assertThat(state.scannedContactDialog).isNull()
            }
            verify(inviteContactWithHandleUseCase).invoke("scanned@test.com", 42L, null)
        }

    @Test
    fun `test that failed feedback is fired when the scanned contact invitation fails`() =
        runTest(extension.testDispatcher) {
            whenever(areThereOngoingVideoCallsUseCase()).thenReturn(false)
            stubFoundDialog()
            whenever(inviteContactWithHandleUseCase("scanned@test.com", 42L, null))
                .thenAnswer { throw RuntimeException("invite failed") }

            underTest.uiState.test {
                awaitDataState()
                underTest.onScanClicked()
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

    private suspend fun stubScannedCode(code: String, handle: String) {
        whenever(scannerHandler.scanBarcode()).thenReturn(BarcodeScanResult.Success(code))
        whenever(parseScannedContactLinkHandleUseCase(code)).thenReturn(handle)
    }

    private suspend fun stubFoundDialog() {
        stubScannedCode(SCANNED_CODE, SCANNED_HANDLE)
        whenever(queryScannedContactLinkUseCase(SCANNED_HANDLE)).thenReturn(
            scannedResult(isContact = false)
        )
    }

    private fun pickedLocalContacts() = listOf(
        LocalContact(
            id = 1L,
            name = "Emailed",
            emails = listOf("e@test.com"),
            phoneNumbers = emptyList(),
        ),
        LocalContact(
            id = 2L,
            name = "Phone Only",
            emails = emptyList(),
            phoneNumbers = listOf("+441234567"),
        ),
    )

    private fun scannedResult(
        isContact: Boolean = false,
        queryResult: QRCodeQueryResults = QRCodeQueryResults.CONTACT_QUERY_OK,
    ) = ScannedContactLinkResult(
        contactName = "Scanned Contact",
        email = "scanned@test.com",
        handle = 42L,
        isContact = isContact,
        qrCodeQueryResult = queryResult,
    )

    private suspend fun ReceiveTurbine<InviteContactUiState>.awaitDialog(): ScannedContactDialog {
        var dialog = awaitDataState().scannedContactDialog
        while (dialog == null) {
            dialog = awaitDataState().scannedContactDialog
        }
        return dialog
    }

    private suspend fun ReceiveTurbine<InviteContactUiState>.awaitEmailValidationMessage(): EmailValidationMessage {
        var state = awaitDataState()
        while (state.emailValidationEvent !is StateEventWithContentTriggered) {
            state = awaitDataState()
        }
        val event = state.emailValidationEvent
        check(event is StateEventWithContentTriggered)
        return event.content
    }

    private suspend fun ReceiveTurbine<InviteContactUiState>.awaitDataState(): InviteContactUiState.Data {
        var item = awaitItem()
        while (item !is InviteContactUiState.Data) {
            item = awaitItem()
        }
        return item
    }

    private companion object {
        const val PRE_PICKER_SDK = 34
        const val PICKER_SDK = 37
        const val CONTACT_LINK = "https://mega.nz/C!contactLink"
        const val SCANNED_CODE = "https://mega.nz/C!scannedHandle"
        const val SCANNED_HANDLE = "scannedHandle"

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}
