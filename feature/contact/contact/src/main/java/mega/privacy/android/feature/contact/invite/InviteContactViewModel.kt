package mega.privacy.android.feature.contact.invite

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.core.sharedcomponents.scanner.BarcodeScanResult
import mega.privacy.android.core.sharedcomponents.scanner.BarcodeScanner
import mega.privacy.android.core.sharedcomponents.scanner.BarcodeScannerModuleIsNotInstalled
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
import mega.privacy.android.feature.contact.invite.model.PickedContactData
import mega.privacy.android.feature.contact.invite.model.SmsInvite
import mega.privacy.android.feature.contact.picker.PhoneContactsSection
import mega.privacy.android.feature.contact.picker.ScannedContactDialog
import mega.privacy.android.feature.contact.picker.ScannedContactInviteFeedback
import mega.privacy.android.shared.contact.mapper.ScannedContactAvatarMapper
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.android.core.ui.components.contact.state.ContactItemStatus
import timber.log.Timber

/**
 * Invite contact view model. Backs the redesigned invite flow: it resolves and validates recipients
 * from picked device contacts, manually typed emails/phone numbers and scanned QR codes, then sends
 * the invitations. Which recipients are selected is owned by the Compose layer, not this ViewModel;
 * the ViewModel only surfaces the picked device contacts to render and emits one-shot events the
 * screen turns into selections.
 *
 * Device contacts come from the OS multi-select contact picker on devices at or above
 * [ANDROID_PICKER_MIN_SDK]; below that they are bulk-loaded once READ_CONTACTS is granted. Picked
 * contacts carry both their emails and phone numbers through [InviteContactUiState.Data.phoneContactsPickedEvent]
 * so phone-only contacts can still be invited by SMS.
 *
 * Sending is sequenced here: email invitations are sent first, and only once they resolve are any
 * SMS invitations requested (after a short delay), mirroring the legacy behaviour.
 *
 * @property isFromAchievement Whether the flow was launched from the achievements screen, in which
 * case the result is returned as an [InvitationResult.Achievement] count instead of a snackbar.
 */
@HiltViewModel(assistedFactory = InviteContactViewModel.Factory::class)
class InviteContactViewModel @AssistedInject constructor(
    @Assisted private val isFromAchievement: Boolean,
    private val getDeviceSdkVersionUseCase: GetDeviceSdkVersionUseCase,
    private val getLocalContactsUseCase: GetLocalContactsUseCase,
    private val getLocalContactsFromUriUseCase: GetLocalContactsFromUriUseCase,
    private val isEmailValidUseCase: IsEmailValidUseCase,
    private val validateEmailInputForInvitationUseCase: ValidateEmailInputForInvitationUseCase,
    private val inviteContactWithEmailsUseCase: InviteContactWithEmailsUseCase,
    private val createContactLinkUseCase: CreateContactLinkUseCase,
    private val areThereOngoingVideoCallsUseCase: AreThereOngoingVideoCallsUseCase,
    private val scannerHandler: BarcodeScanner,
    private val parseScannedContactLinkHandleUseCase: ParseScannedContactLinkHandleUseCase,
    private val queryScannedContactLinkUseCase: QueryScannedContactLinkUseCase,
    private val inviteContactWithHandleUseCase: InviteContactWithHandleUseCase,
    private val scannedContactAvatarMapper: ScannedContactAvatarMapper,
    private val invitationMessageMapper: InvitationMessageMapper,
) : ViewModel() {

    /**
     * Factory for assisted creation, supplying the [isFromAchievement] flag from the navigation key.
     */
    @AssistedFactory
    interface Factory {
        /**
         * @param isFromAchievement whether the flow was launched from the achievements screen.
         */
        fun create(isFromAchievement: Boolean): InviteContactViewModel
    }

    private val queryChannel = Channel<String?>(Channel.CONFLATED)

    private val readContactsGranted = MutableStateFlow(false)

    private val pickedPhoneContacts = MutableStateFlow<List<ContactItemUiState>>(emptyList())

    private val contactLink = MutableStateFlow<String?>(null)

    private val invitationResultEvent =
        MutableStateFlow<StateEventWithContent<InvitationResult>>(consumed())

    private val emailValidationEvent =
        MutableStateFlow<StateEventWithContent<EmailValidationMessage>>(consumed())

    private val sendSmsEvent = MutableStateFlow<StateEventWithContent<SmsInvite>>(consumed())

    private val ongoingCallConfirmEvent = MutableStateFlow<StateEvent>(consumed)

    private val phoneContactsPickedEvent =
        MutableStateFlow<StateEventWithContent<PickedContactData>>(consumed())

    private val manualEmailAcceptedEvent =
        MutableStateFlow<StateEventWithContent<String>>(consumed())

    private val manualPhoneAcceptedEvent =
        MutableStateFlow<StateEventWithContent<String>>(consumed())

    private val scanState = MutableStateFlow(
        ScanState(
            dialog = null,
            selectContactEvent = consumed(),
            inviteEvent = consumed(),
        )
    )

    /**
     * Ui state
     */
    val uiState: StateFlow<InviteContactUiState> by lazy {
        combine(
            queryChannel.receiveAsFlow().onStart { emit(null) },
            phoneContactsSource(),
            contactLinkSource(),
            resultEventsSource(),
            pickerEventsSource(),
        ) { query, phoneSection, contactLink, resultEvents, pickerEvents ->
            InviteContactUiState.Data(
                phoneContactsSection = phoneSection.filteredBy(query),
                query = query,
                contactLink = contactLink,
                invitationResultEvent = resultEvents.invitationResult,
                emailValidationEvent = resultEvents.emailValidation,
                sendSmsEvent = resultEvents.sendSms,
                ongoingCallConfirmEvent = resultEvents.ongoingCallConfirm,
                phoneContactsPickedEvent = pickerEvents.phoneContactsPicked,
                manualEmailAcceptedEvent = pickerEvents.manualEmailAccepted,
                manualPhoneAcceptedEvent = pickerEvents.manualPhoneAccepted,
                scannedContactDialog = pickerEvents.scan.dialog,
                scannedContactSelectEvent = pickerEvents.scan.selectContactEvent,
                scannedContactInviteEvent = pickerEvents.scan.inviteEvent,
            )
        }.catch { Timber.e(it) }
            .asUiStateFlow(viewModelScope, InviteContactUiState.Loading)
    }

    private fun contactLinkSource(): Flow<String?> =
        contactLink.onStart { viewModelScope.launch { loadContactLink() } }

    private suspend fun loadContactLink() {
        if (contactLink.value != null) return
        runCatching { createContactLinkUseCase(renew = false) }
            .onSuccess { contactLink.value = it }
            .onFailure { Timber.e(it, "Failed to generate a contact link") }
    }

    private fun resultEventsSource(): Flow<ResultEvents> =
        combine(
            invitationResultEvent,
            emailValidationEvent,
            sendSmsEvent,
            ongoingCallConfirmEvent,
        ) { invitationResult, emailValidation, sendSms, ongoingCallConfirm ->
            ResultEvents(
                invitationResult = invitationResult,
                emailValidation = emailValidation,
                sendSms = sendSms,
                ongoingCallConfirm = ongoingCallConfirm,
            )
        }

    private fun pickerEventsSource(): Flow<PickerEvents> =
        combine(
            phoneContactsPickedEvent,
            manualEmailAcceptedEvent,
            manualPhoneAcceptedEvent,
            scanState,
        ) { phoneContactsPicked, manualEmailAccepted, manualPhoneAccepted, scan ->
            PickerEvents(
                phoneContactsPicked = phoneContactsPicked,
                manualEmailAccepted = manualEmailAccepted,
                manualPhoneAccepted = manualPhoneAccepted,
                scan = scan,
            )
        }

    private fun phoneContactsSource(): Flow<PhoneContactsSection> =
        if (getDeviceSdkVersionUseCase() >= ANDROID_PICKER_MIN_SDK) {
            pickedPhoneContacts.map { picked ->
                PhoneContactsSection.PickerAvailable(picked.toImmutableList())
            }
        } else {
            readContactsGranted.map { granted ->
                if (!granted) {
                    PhoneContactsSection.PermissionRequired
                } else {
                    PhoneContactsSection.Loaded(loadBulkPhoneContacts().toImmutableList())
                }
            }
        }

    private suspend fun loadBulkPhoneContacts(): List<ContactItemUiState> =
        runCatching { getLocalContactsUseCase() }
            .onFailure { Timber.e(it) }
            .getOrDefault(emptyList())
            .mapNotNull { it.toEmailableUiState() }

    /**
     * Notify that READ_CONTACTS has been granted, triggering the pre-picker bulk load.
     */
    fun onReadContactsPermissionGranted() {
        readContactsGranted.value = true
    }

    /**
     * Resolve the [uriPath] returned by the OS contact picker into device contacts, append the newly
     * added ones (keeping phone-only contacts) to the session list for rendering, and fire a one-shot
     * event carrying the added emails and phone numbers so the screen can auto-select them.
     *
     * @param uriPath the session Uri returned by the picker.
     */
    fun onContactsPicked(uriPath: UriPath) {
        viewModelScope.launch {
            val resolved = runCatching {
                getLocalContactsFromUriUseCase(uriPath, includePhoneNumbers = true)
            }.onFailure { Timber.e(it) }
                .getOrDefault(emptyList())
                .mapNotNull { it.toPickedContact() }
            if (resolved.isEmpty()) return@launch

            val existing = pickedPhoneContacts.value
            val newContacts = resolved.filter { it.ui !in existing }
            if (newContacts.isEmpty()) return@launch

            pickedPhoneContacts.value = existing + newContacts.map { it.ui }
            phoneContactsPickedEvent.value = triggered(
                PickedContactData(
                    emails = newContacts.flatMap { it.emails }.distinct().toImmutableList(),
                    phoneNumbers = newContacts.flatMap { it.phoneNumbers }.distinct().toImmutableList(),
                )
            )
        }
    }

    /**
     * Validate the manually typed [input]: fire an accepted event when it is a valid phone number or
     * an invitable email so the screen can add it to its selection, otherwise surface an
     * [EmailValidationMessage].
     *
     * @param input the raw text typed by the user.
     */
    fun validateManualInput(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                when {
                    PHONE_NUMBER_REGEX.matches(trimmed) ->
                        manualPhoneAcceptedEvent.value = triggered(trimmed)

                    isEmailValidUseCase(trimmed) -> when (validateEmailInputForInvitationUseCase(trimmed)) {
                        EmailInvitationsInputValidity.Valid ->
                            manualEmailAcceptedEvent.value = triggered(trimmed)

                        EmailInvitationsInputValidity.MyOwnEmail ->
                            emailValidationEvent.value = triggered(EmailValidationMessage.MyOwnEmail)

                        EmailInvitationsInputValidity.AlreadyInContacts ->
                            emailValidationEvent.value =
                                triggered(EmailValidationMessage.AlreadyInContacts)

                        EmailInvitationsInputValidity.Pending ->
                            emailValidationEvent.value = triggered(EmailValidationMessage.Pending)
                    }

                    else -> emailValidationEvent.value =
                        triggered(EmailValidationMessage.InvalidInput)
                }
            }.onFailure { Timber.e(it, "Failed to validate manual input") }
        }
    }

    /**
     * Send the invitations. Email invitations are sent first; once they resolve, any SMS invitations
     * are requested after [SMS_INVITE_DELAY_MS]. When only phone numbers are supplied the SMS request
     * is fired immediately.
     *
     * @param emails the email addresses to invite.
     * @param phones the phone numbers to invite by SMS.
     */
    fun inviteContacts(emails: Set<String>, phones: Set<String>) {
        viewModelScope.launch {
            runCatching {
                if (emails.isNotEmpty()) {
                    val requests = inviteContactWithEmailsUseCase(emails.toList())
                    invitationResultEvent.value = triggered(
                        invitationMessageMapper(isFromAchievement, requests, emails.toList())
                    )
                    if (phones.isNotEmpty()) {
                        delay(SMS_INVITE_DELAY_MS)
                        sendSmsEvent.value = triggered(smsInvite(phones))
                    }
                } else if (phones.isNotEmpty()) {
                    sendSmsEvent.value = triggered(smsInvite(phones))
                }
            }.onFailure { Timber.e(it, "Failed to send invitations") }
        }
    }

    private fun smsInvite(phones: Set<String>) = SmsInvite(
        phoneNumbers = phones.toList().toImmutableList(),
        contactLink = contactLink.value.orEmpty(),
    )

    /**
     * Start the QR scan flow. When there is an ongoing video call, confirmation is requested first
     * via [InviteContactUiState.Data.ongoingCallConfirmEvent] before the scanner is launched.
     */
    fun onScanClicked() {
        viewModelScope.launch {
            runCatching { areThereOngoingVideoCallsUseCase() }
                .onSuccess { ongoing ->
                    if (ongoing) {
                        ongoingCallConfirmEvent.value = triggered
                    } else {
                        scan()
                    }
                }
                .onFailure { Timber.e(it, "Failed to check ongoing video calls") }
        }
    }

    /**
     * Proceed with the QR scan after the user confirmed interrupting the ongoing call.
     */
    fun onScanConfirmed() {
        viewModelScope.launch { scan() }
    }

    private suspend fun scan() {
        runCatching { scannerHandler.scanBarcode() }
            .onSuccess { result ->
                when (result) {
                    is BarcodeScanResult.Success -> handleScannedCode(result.rawValue)
                    BarcodeScanResult.Cancelled -> Unit
                }
            }
            .onFailure { error ->
                Timber.e(error, "Failed to scan QR code")
                if (error is BarcodeScannerModuleIsNotInstalled) {
                    showScannedContactDialog(ScannedContactDialog.ScannerNotInstalled)
                }
            }
    }

    private suspend fun handleScannedCode(rawValue: String?) {
        val scannedHandle = rawValue?.let {
            runCatching { parseScannedContactLinkHandleUseCase(it) }.onFailure { error ->
                Timber.e(error)
            }.getOrNull()
        }
        if (scannedHandle == null) {
            showScannedContactDialog(ScannedContactDialog.InvalidCode)
            return
        }
        runCatching { queryScannedContactLinkUseCase(scannedHandle) }
            .onSuccess { result ->
                when (result.qrCodeQueryResult) {
                    QRCodeQueryResults.CONTACT_QUERY_OK -> handleQueriedContact(result)

                    QRCodeQueryResults.CONTACT_QUERY_EEXIST ->
                        showScannedContactDialog(ScannedContactDialog.AlreadyAdded(result.email))

                    QRCodeQueryResults.CONTACT_QUERY_DEFAULT ->
                        showScannedContactDialog(ScannedContactDialog.InvalidCode)
                }
            }
            .onFailure {
                Timber.e(it, "Failed to query scanned contact link")
                showScannedContactDialog(ScannedContactDialog.InvalidCode)
            }
    }

    private fun handleQueriedContact(result: ScannedContactLinkResult) {
        if (!result.isContact) {
            showScannedContactDialog(
                ScannedContactDialog.Found(
                    contactName = result.contactName,
                    email = result.email,
                    handle = result.handle,
                    avatar = scannedContactAvatarMapper(result),
                )
            )
        } else {
            showScannedContactDialog(ScannedContactDialog.AlreadyAdded(result.email))
        }
    }

    /**
     * Invite the scanned contact currently shown in the [ScannedContactDialog.Found] dialog.
     */
    fun onInviteScannedContactConfirmed() {
        val found = scanState.value.dialog as? ScannedContactDialog.Found ?: return
        scanState.update { it.copy(dialog = null) }
        viewModelScope.launch {
            runCatching { inviteContactWithHandleUseCase(found.email, found.handle, null) }
                .onSuccess { request ->
                    when (request) {
                        InviteContactRequest.Sent,
                        InviteContactRequest.Resent,
                            -> scanState.update {
                            it.copy(inviteEvent = triggered(ScannedContactInviteFeedback.Sent))
                        }

                        InviteContactRequest.AlreadyContact ->
                            showScannedContactDialog(ScannedContactDialog.AlreadyAdded(found.email))

                        else -> scanState.update {
                            it.copy(inviteEvent = triggered(ScannedContactInviteFeedback.Failed))
                        }
                    }
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to invite scanned contact")
                    scanState.update {
                        it.copy(inviteEvent = triggered(ScannedContactInviteFeedback.Failed))
                    }
                }
        }
    }

    /**
     * Dismiss the currently shown scanned-contact dialog.
     */
    fun onScannedContactDialogDismissed() {
        scanState.update { it.copy(dialog = null) }
    }

    /**
     * Consume the select-scanned-contact event once the UI has auto-selected the contact.
     */
    fun onScannedContactSelectConsumed() {
        scanState.update { it.copy(selectContactEvent = consumed()) }
    }

    /**
     * Consume the invite-feedback event once the UI has surfaced it.
     */
    fun onScannedContactInviteConsumed() {
        scanState.update { it.copy(inviteEvent = consumed()) }
    }

    /**
     * Consume the invitation-result event once the UI has surfaced it.
     */
    fun onInvitationResultConsumed() {
        invitationResultEvent.value = consumed()
    }

    /**
     * Consume the email-validation event once the UI has surfaced it.
     */
    fun onEmailValidationConsumed() {
        emailValidationEvent.value = consumed()
    }

    /**
     * Consume the send-SMS event once the UI has launched the SMS flow.
     */
    fun onSendSmsConsumed() {
        sendSmsEvent.value = consumed()
    }

    /**
     * Consume the ongoing-call-confirmation event once the UI has surfaced it.
     */
    fun onOngoingCallConfirmConsumed() {
        ongoingCallConfirmEvent.value = consumed
    }

    /**
     * Consume the picked-contacts event once the UI has auto-selected the new emails and phones.
     */
    fun onPhoneContactsPickedConsumed() {
        phoneContactsPickedEvent.value = consumed()
    }

    /**
     * Consume the manual-email-accepted event once the UI has added it to its selection.
     */
    fun onManualEmailAcceptedConsumed() {
        manualEmailAcceptedEvent.value = consumed()
    }

    /**
     * Consume the manual-phone-accepted event once the UI has added it to its selection.
     */
    fun onManualPhoneAcceptedConsumed() {
        manualPhoneAcceptedEvent.value = consumed()
    }

    private fun showScannedContactDialog(dialog: ScannedContactDialog) {
        scanState.update { it.copy(dialog = dialog) }
    }

    /**
     * Set the current search query.
     *
     * @param query the query text, or null to clear the search.
     */
    fun setQuery(query: String?) {
        viewModelScope.launch { queryChannel.send(query) }
    }

    private fun LocalContact.toPickedContact(): PickedContact? {
        val validEmails = emails.filter { it.isNotBlank() }
        val validPhones = phoneNumbers.filter { it.isNotBlank() }
        if (validEmails.isEmpty() && validPhones.isEmpty()) return null
        val label = name.ifBlank { validEmails.firstOrNull() ?: validPhones.first() }
        return PickedContact(
            ui = ContactItemUiState(
                handle = PHONE_CONTACT_HANDLE,
                displayName = label,
                status = ContactItemStatus.Unknown,
                lastSeen = null,
                avatar = AvatarData.Initials(
                    initials = label.trim().take(1).uppercase(),
                    avatarColor = PHONE_CONTACT_AVATAR_COLOR,
                ),
                isVerified = false,
                email = validEmails.firstOrNull().orEmpty(),
            ),
            emails = validEmails,
            phoneNumbers = validPhones,
        )
    }

    private fun LocalContact.toEmailableUiState(): ContactItemUiState? {
        val email = emails.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
        return ContactItemUiState(
            handle = PHONE_CONTACT_HANDLE,
            displayName = name.ifBlank { email },
            status = ContactItemStatus.Unknown,
            lastSeen = null,
            avatar = AvatarData.Initials(
                initials = (name.firstOrNull() ?: email.first()).uppercaseChar().toString(),
                avatarColor = PHONE_CONTACT_AVATAR_COLOR,
            ),
            isVerified = false,
            email = email,
        )
    }

    private fun PhoneContactsSection.filteredBy(query: String?): PhoneContactsSection {
        if (query.isNullOrBlank()) return this
        val q = query.lowercase()
        fun ContactItemUiState.matches() =
            displayName.lowercase().contains(q) || email.lowercase().contains(q)
        return when (this) {
            is PhoneContactsSection.Loaded ->
                PhoneContactsSection.Loaded(contacts.filter { it.matches() }.toImmutableList())

            is PhoneContactsSection.PickerAvailable ->
                PhoneContactsSection.PickerAvailable(picked.filter { it.matches() }.toImmutableList())

            PhoneContactsSection.Hidden,
            PhoneContactsSection.PermissionRequired,
                -> this
        }
    }

    private data class PickedContact(
        val ui: ContactItemUiState,
        val emails: List<String>,
        val phoneNumbers: List<String>,
    )

    private data class ScanState(
        val dialog: ScannedContactDialog?,
        val selectContactEvent: StateEventWithContent<Long>,
        val inviteEvent: StateEventWithContent<ScannedContactInviteFeedback>,
    )

    private data class ResultEvents(
        val invitationResult: StateEventWithContent<InvitationResult>,
        val emailValidation: StateEventWithContent<EmailValidationMessage>,
        val sendSms: StateEventWithContent<SmsInvite>,
        val ongoingCallConfirm: StateEvent,
    )

    private data class PickerEvents(
        val phoneContactsPicked: StateEventWithContent<PickedContactData>,
        val manualEmailAccepted: StateEventWithContent<String>,
        val manualPhoneAccepted: StateEventWithContent<String>,
        val scan: ScanState,
    )

    companion object {
        private const val PHONE_CONTACT_HANDLE = -1L

        private val PHONE_CONTACT_AVATAR_COLOR = Color.Gray

        private val PHONE_NUMBER_REGEX = Regex("^[+]?[0-9]{5,22}$")

        /**
         * Delay before requesting SMS invitations, giving the just-sent email invitations time to
         * resolve. Mirrors the legacy invite flow.
         */
        internal const val SMS_INVITE_DELAY_MS = 2_000L

        /**
         * Minimum device SDK version that exposes the OS multi-select contact picker
         * (`ACTION_PICK_CONTACTS`). Below this, phone contacts are bulk-loaded after the
         * READ_CONTACTS permission is granted.
         */
        internal const val ANDROID_PICKER_MIN_SDK = 37
    }
}
