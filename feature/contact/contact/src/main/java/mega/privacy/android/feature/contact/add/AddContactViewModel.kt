package mega.privacy.android.feature.contact.add

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.core.nodecomponents.scanner.BarcodeScanResult
import mega.privacy.android.core.nodecomponents.scanner.BarcodeScannerModuleIsNotInstalled
import mega.privacy.android.core.nodecomponents.scanner.ScannerHandler
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.contacts.LocalContact
import mega.privacy.android.domain.entity.qrcode.QRCodeQueryResults
import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.call.MonitorParticipantsLimitWarningUseCase
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
import mega.privacy.android.shared.contact.mapper.ContactItemUiStateMapper
import mega.privacy.android.shared.contact.mapper.ScannedContactAvatarMapper
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import timber.log.Timber

/**
 * Add contact view model. Backs the MEGA-contacts multi-select picker: exposes the
 * searchable contact list and resolves the selected handles to emails on confirm.
 * Selection itself is owned by the Compose layer, not this ViewModel.
 *
 * When [chatId] is provided the contacts already participating in that chat are excluded,
 * which backs the "add chat participants" flow; otherwise the full visible contact list is shown.
 * When [monitorCallLimit] is set the active call is monitored to surface the user-limit warning,
 * which backs the "add meeting participants" flow.
 *
 * When [showPhoneContacts] is set, a "Phone contacts" section is surfaced. On devices below
 * [ANDROID_PICKER_MIN_SDK] the phone contacts are bulk-loaded once READ_CONTACTS is granted; on newer
 * devices the OS multi-select contact picker is used instead and picked contacts are appended to a
 * session-scoped list.
 *
 * @property chatId optional chat whose existing participants should be excluded.
 * @property monitorCallLimit whether to monitor the active call for the user-limit warning.
 * @property showPhoneContacts whether to surface the phone-contacts section.
 * @property getContactsUseCase
 * @property getContactsToAddToChatUseCase
 * @property monitorParticipantsLimitWarningUseCase
 * @property getDeviceSdkVersionUseCase
 * @property getLocalContactsUseCase
 * @property getLocalContactsFromUriUseCase
 * @property contactItemUiStateMapper
 * @property scannerHandler
 * @property parseScannedContactLinkHandleUseCase
 * @property queryScannedContactLinkUseCase
 * @property inviteContactWithHandleUseCase
 * @property scannedContactAvatarMapper
 * @property isEmailValidUseCase
 */
@HiltViewModel(assistedFactory = AddContactViewModel.Factory::class)
class AddContactViewModel @AssistedInject constructor(
    @Assisted private val chatId: Long?,
    @Assisted("monitorCallLimit") private val monitorCallLimit: Boolean,
    @Assisted("showPhoneContacts") private val showPhoneContacts: Boolean,
    private val getContactsUseCase: GetContactsUseCase,
    private val getContactsToAddToChatUseCase: GetContactsToAddToChatUseCase,
    private val monitorParticipantsLimitWarningUseCase: MonitorParticipantsLimitWarningUseCase,
    private val getDeviceSdkVersionUseCase: GetDeviceSdkVersionUseCase,
    private val getLocalContactsUseCase: GetLocalContactsUseCase,
    private val getLocalContactsFromUriUseCase: GetLocalContactsFromUriUseCase,
    private val contactItemUiStateMapper: ContactItemUiStateMapper,
    private val scannerHandler: ScannerHandler,
    private val parseScannedContactLinkHandleUseCase: ParseScannedContactLinkHandleUseCase,
    private val queryScannedContactLinkUseCase: QueryScannedContactLinkUseCase,
    private val inviteContactWithHandleUseCase: InviteContactWithHandleUseCase,
    private val scannedContactAvatarMapper: ScannedContactAvatarMapper,
    private val isEmailValidUseCase: IsEmailValidUseCase,
) : ViewModel() {

    /**
     * Factory for assisted creation, supplying the optional [chatId], [monitorCallLimit] and
     * [showPhoneContacts] flags from the navigation key.
     */
    @AssistedFactory
    interface Factory {
        /**
         * @param chatId the chat whose existing participants to exclude, or null for the full list.
         * @param monitorCallLimit whether to monitor the active call for the user-limit warning.
         * @param showPhoneContacts whether to surface the phone-contacts section.
         */
        fun create(
            chatId: Long?,
            @Assisted("monitorCallLimit") monitorCallLimit: Boolean,
            @Assisted("showPhoneContacts") showPhoneContacts: Boolean,
        ): AddContactViewModel
    }

    private val queryChannel = Channel<String?>(Channel.CONFLATED)

    /**
     * Emits whenever READ_CONTACTS is granted so the pre-picker phone-contacts path can (re)load.
     */
    private val readContactsGranted = MutableStateFlow(false)

    /**
     * Session-scoped list of contacts picked via the OS picker (post-picker path). Keyed by email
     * for de-duplication.
     */
    private val pickedPhoneContacts = MutableStateFlow<List<ContactItemUiState>>(emptyList())

    private val pickedEvents =
        MutableStateFlow<StateEventWithContent<List<String>>>(consumed())

    private val scanState = MutableStateFlow(
        ScanState(
            dialog = null,
            selectContactEvent = consumed(),
            inviteEvent = consumed(),
        )
    )

    private fun contactsSource(): Flow<List<ContactItem>> =
        chatId?.let { getContactsToAddToChatUseCase(it) } ?: getContactsUseCase()

    private fun userLimitWarningSource(): Flow<Boolean> =
        if (monitorCallLimit && chatId != null) {
            monitorParticipantsLimitWarningUseCase(chatId)
        } else {
            flowOf(false)
        }

    /**
     * Resolves a selected MEGA contact handle back to its email. Retained from the full
     * (unfiltered) contact list so a selected contact still resolves even when it has
     * been filtered out of the visible list by the current search query.
     */
    private var handleToEmail: Map<Long, String> = emptyMap()

    /**
     * Ui state
     */
    val uiState: StateFlow<AddContactUiState> by lazy {
        combine(
            queryChannel.receiveAsFlow().onStart { emit(null) },
            contactsSource().map { domainList ->
                domainList
                    .map { item ->
                        IndexedContact(
                            data = item,
                            ui = contactItemUiStateMapper(item)
                        )
                    }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.ui.displayName })
                    .also { indexed -> handleToEmail = indexed.associate { it.ui.handle to it.data.email } }
            },
            userLimitWarningSource(),
            phoneContactsSource(),
            combine(pickedEvents, scanState, ::Pair),
        ) { query, indexed: List<IndexedContact>, showUserLimitWarning, phoneSection, (pickedEvent, scan) ->
            val visible =
                if (query.isNullOrBlank()) indexed else indexed.filter { it.matches(query) }
            AddContactUiState.Data(
                contacts = visible.map { it.ui }.toImmutableList(),
                query = query,
                showUserLimitWarning = showUserLimitWarning,
                phoneContactsSection = phoneSection.filteredBy(query),
                phoneContactsPickedEvent = pickedEvent,
                scannedContactDialog = scan.dialog,
                scannedContactSelectEvent = scan.selectContactEvent,
                scannedContactInviteEvent = scan.inviteEvent,
            )
        }.catch { Timber.e(it) }
            .asUiStateFlow(viewModelScope, AddContactUiState.Loading)
    }

    private fun phoneContactsSource(): Flow<PhoneContactsSection> {
        if (!showPhoneContacts) return flowOf(PhoneContactsSection.Hidden)
        return if (getDeviceSdkVersionUseCase() >= ANDROID_PICKER_MIN_SDK) {
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
     * Resolve the [uriPath] returned by the OS contact picker into phone contacts, append the newly
     * added ones (de-duplicated by email) to the session list, and fire a one-shot event carrying the
     * added emails so the screen can auto-select them.
     *
     * @param uriPath the session Uri returned by the picker.
     */
    fun onContactsPicked(uriPath: UriPath) {
        viewModelScope.launch {
            val resolved = runCatching { getLocalContactsFromUriUseCase(uriPath) }
                .onFailure { Timber.e(it) }
                .getOrDefault(emptyList())
                .mapNotNull { it.toEmailableUiState() }
            if (resolved.isEmpty()) return@launch

            val existingEmails = pickedPhoneContacts.value.map { it.email }.toSet()
            val newContacts = resolved.filter { it.email !in existingEmails }
            if (newContacts.isEmpty()) return@launch

            pickedPhoneContacts.value = pickedPhoneContacts.value + newContacts
            pickedEvents.value = triggered(newContacts.map { it.email })
        }
    }

    /**
     * Consume the picked-contacts event once the UI has auto-selected the new emails.
     */
    fun onPhoneContactsPickedConsumed() {
        pickedEvents.value = consumed()
    }

    /**
     * Launch the barcode scanner and resolve the scanned QR code into a picker outcome: auto-select
     * the scanned user when they are already a selectable contact, or surface the appropriate
     * dialog otherwise.
     */
    fun onScanQrClicked() {
        viewModelScope.launch {
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
    }

    private suspend fun handleScannedCode(rawValue: String?) {
        val scannedHandle = rawValue?.let { parseScannedContactLinkHandleUseCase(it) }
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
        when {
            !result.isContact -> showScannedContactDialog(
                ScannedContactDialog.Found(
                    contactName = result.contactName,
                    email = result.email,
                    handle = result.handle,
                    avatar = scannedContactAvatarMapper(result),
                )
            )

            result.handle in handleToEmail ->
                scanState.update { it.copy(selectContactEvent = triggered(result.handle)) }

            // Already a contact but not selectable here (e.g. already a chat participant), so a
            // silent auto-select is impossible; give feedback instead.
            else -> showScannedContactDialog(ScannedContactDialog.AlreadyAdded(result.email))
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

    /**
     * Resolve the selected MEGA contact handles to their emails.
     *
     * @param handles the handles currently selected in the UI.
     * @return the emails of the selected contacts, in no particular order.
     */
    fun emailsForSelected(handles: Set<Long>): List<String> =
        handles.mapNotNull { handleToEmail[it] }

    /**
     * Combine the selected MEGA contact emails with the selected phone-contact emails.
     *
     * @param handles the MEGA contact handles currently selected.
     * @param phoneEmails the phone-contact emails currently selected.
     * @return the merged, de-duplicated list of emails to publish as the picker result.
     */
    fun emailsForSelected(handles: Set<Long>, phoneEmails: Set<String>): List<String> =
        (emailsForSelected(handles) + phoneEmails).distinct()

    /**
     * Check whether [email] is a syntactically valid email address.
     */
    fun isEmailValid(email: String): Boolean = isEmailValidUseCase(email)

    /**
     * Resolve [email] to the handle of a loaded MEGA contact, comparing case-insensitively against
     * the full (unfiltered) contact list.
     *
     * @return the matching contact's handle, or null when no loaded contact has that email.
     */
    fun handleForEmail(email: String): Long? =
        handleToEmail.entries.firstOrNull { it.value.equals(email, ignoreCase = true) }?.key

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

    private fun IndexedContact.matches(query: String): Boolean {
        val q = query.lowercase()
        return ui.displayName.lowercase().contains(q)
                || data.email.lowercase().contains(q)
                || data.contactData.fullName?.lowercase()?.contains(q) == true
                || data.contactData.alias?.lowercase()?.contains(q) == true
    }

    /**
     * Indexed contact
     *
     * @property data
     * @property ui
     */
    private data class IndexedContact(
        val data: ContactItem,
        val ui: ContactItemUiState,
    )

    private data class ScanState(
        val dialog: ScannedContactDialog?,
        val selectContactEvent: StateEventWithContent<Long>,
        val inviteEvent: StateEventWithContent<ScannedContactInviteFeedback>,
    )

    companion object {
        /**
         * Handle assigned to phone contacts, which have no MEGA handle. They are keyed by email in
         * the UI, not by handle.
         */
        private const val PHONE_CONTACT_HANDLE = -1L

        /**
         * Neutral avatar background for phone contacts, which have no MEGA-assigned avatar color.
         */
        private val PHONE_CONTACT_AVATAR_COLOR = Color.Gray

        /**
         * Minimum device SDK version that exposes the OS multi-select contact picker
         * (`ACTION_PICK_CONTACTS`). Below this, phone contacts are bulk-loaded after the
         * READ_CONTACTS permission is granted.
         */
        internal const val ANDROID_PICKER_MIN_SDK = 37
    }
}
