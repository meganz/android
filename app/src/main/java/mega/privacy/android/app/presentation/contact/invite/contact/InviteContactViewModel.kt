package mega.privacy.android.app.presentation.contact.invite.contact

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.main.InvitationContactInfo
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_PHONE_CONTACT
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_PHONE_CONTACT_HEADER
import mega.privacy.android.app.main.model.InvitationStatusUiState
import mega.privacy.android.app.main.model.InviteContactFilterUiState
import mega.privacy.android.app.main.model.InviteContactUiState
import mega.privacy.android.app.presentation.contact.invite.contact.mapper.InvitationContactInfoUiMapper
import mega.privacy.android.domain.entity.contacts.InviteContactRequest.Sent
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.contact.FilterLocalContactsByEmailUseCase
import mega.privacy.android.domain.usecase.contact.FilterPendingOrAcceptedLocalContactsByEmailUseCase
import mega.privacy.android.domain.usecase.contact.GetLocalContactsUseCase
import mega.privacy.android.domain.usecase.contact.InviteContactWithEmailsUseCase
import mega.privacy.android.domain.usecase.meeting.AreThereOngoingVideoCallsUseCase
import mega.privacy.android.domain.usecase.qrcode.CreateContactLinkUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * InviteContact ViewModel
 */
@HiltViewModel
class InviteContactViewModel @Inject constructor(
    private val getLocalContactsUseCase: GetLocalContactsUseCase,
    private val filterLocalContactsByEmailUseCase: FilterLocalContactsByEmailUseCase,
    private val filterPendingOrAcceptedLocalContactsByEmailUseCase: FilterPendingOrAcceptedLocalContactsByEmailUseCase,
    private val createContactLinkUseCase: CreateContactLinkUseCase,
    private val inviteContactWithEmailsUseCase: InviteContactWithEmailsUseCase,
    private val areThereOngoingVideoCallsUseCase: AreThereOngoingVideoCallsUseCase,
    private val invitationContactInfoUiMapper: InvitationContactInfoUiMapper,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InviteContactUiState())

    /**
     * Invite contact UI state
     */
    val uiState = _uiState.asStateFlow()

    private val _filterUiState = MutableStateFlow(InviteContactFilterUiState())

    /**
     * Filter specific UI state
     */
    val filterUiState = _filterUiState.asStateFlow()

    private var filterContactsJob: Job? = null

    /**
     * List of all available contacts
     */
    var allContacts: List<InvitationContactInfo> = emptyList()
        private set

    internal val isFromAchievement = savedStateHandle.get<Boolean>(KEY_FROM) ?: false

    private lateinit var currentSearchQuery: String

    init {
        updateCurrentSearchQuery()
        createContactLink()
    }

    private fun createContactLink() {
        viewModelScope.launch {
            Timber.d("Creating contact link")
            runCatching { createContactLinkUseCase(renew = false) }
                .onSuccess { contactLink -> _uiState.update { it.copy(contactLink = contactLink) } }
                .onFailure { Timber.e("Failed to generate a contact link", it) }
        }
    }

    /**
     * Initialize the list of contacts
     */
    fun initializeContacts() = viewModelScope.launch {
        runCatching { getInvitationContactInfo() }
            .onSuccess { invitationContactInfo ->
                initializeFilteredContacts(invitationContactInfo)
                initializeAllContacts(_filterUiState.value.filteredContacts)
                _uiState.update { it.copy(onContactsInitialized = true) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(onContactsInitialized = true) }
                Timber.e("Failed to get local contacts", throwable)
            }
    }

    private suspend fun getInvitationContactInfo(): List<InvitationContactInfo> {
        val localContacts = getLocalContactsUseCase()
        if (localContacts.isEmpty()) return emptyList()

        // Filter contacts if the emails exist in the MEGA contact
        val filteredMEGAContactList = filterLocalContactsByEmailUseCase(localContacts)

        // Filter out pending contacts by email
        val filteredPendingMEGAContactList =
            filterPendingOrAcceptedLocalContactsByEmailUseCase(filteredMEGAContactList)

        return invitationContactInfoUiMapper(localContacts = filteredPendingMEGAContactList)
    }

    /**
     * Initialize all available contacts. Keeping all contacts for records.
     *
     * @param contacts
     */
    fun initializeAllContacts(contacts: List<InvitationContactInfo>) {
        allContacts = contacts
    }

    /**
     * Initialize the filtered contacts
     *
     * @param contacts
     */
    fun initializeFilteredContacts(contacts: List<InvitationContactInfo>) {
        _filterUiState.update { it.copy(filteredContacts = contacts) }
    }

    /**
     * Update contact's highlighted or selected value
     *
     * @param contactInfo The selected contact info
     */
    fun toggleContactHighlightedInfo(contactInfo: InvitationContactInfo) {
        toggleContactHighlightedInfo(contactInfo, !contactInfo.isHighlighted)
    }

    /**
     * Update contact's highlighted or selected value
     *
     * @param contactInfo The selected contact info
     * @param value The expected new value of the highlighted boolean property
     */
    fun toggleContactHighlightedInfo(contactInfo: InvitationContactInfo, value: Boolean) {
        // Since stateflow will not re-emit the value if we use object reference
        // therefore we have to update both
        allContacts = allContacts.toMutableList()
            .map { contact ->
                if (isTheSameContact(contact, contactInfo)) {
                    contact.copy(isHighlighted = value)
                } else {
                    contact
                }
            }

        _filterUiState.update {
            it.copy(
                filteredContacts = it.filteredContacts.toMutableList()
                    .map { contact ->
                        if (isTheSameContact(contact, contactInfo)) {
                            contact.copy(isHighlighted = value)
                        } else {
                            contact
                        }
                    }
            )
        }
    }

    /**
     * Update current search query
     */
    fun onSearchQueryChange(query: String?) {
        if (query == currentSearchQuery) return

        savedStateHandle[CONTACT_SEARCH_QUERY] = query.orEmpty()
        updateCurrentSearchQuery()
        filterContacts(query.orEmpty())
    }

    private fun updateCurrentSearchQuery() {
        currentSearchQuery = savedStateHandle.get<String>(key = CONTACT_SEARCH_QUERY).orEmpty()
    }

    /**
     * Filter contacts based on the given query
     *
     * @param query The user's input
     */
    fun filterContacts(query: String?) {
        filterContactsJob?.cancel()
        filterContactsJob = viewModelScope.launch(defaultDispatcher) {
            Timber.d("Filtering contact")

            if (query.isNullOrBlank()) {
                // Reset the contact list
                _filterUiState.update { it.copy(filteredContacts = allContacts) }
                return@launch
            }

            val phoneContacts: MutableList<InvitationContactInfo> = mutableListOf()
            allContacts.forEach { invitationContactInfo ->
                val type = invitationContactInfo.type
                val name = invitationContactInfo.getContactName().lowercase()
                val nameWithoutSpace = name.replace(regex = "\\s".toRegex(), replacement = "")
                val displayLabel = invitationContactInfo
                    .displayInfo
                    .lowercase()
                    .replace(
                        regex = "\\s".toRegex(),
                        replacement = ""
                    )

                if (
                    isAPhoneContact(query.lowercase(), type, name, displayLabel, nameWithoutSpace)
                ) {
                    phoneContacts.add(invitationContactInfo)
                }
            }

            _filterUiState.update {
                val newFilteredContacts = if (phoneContacts.isNotEmpty()) {
                    listOf(
                        // Header
                        InvitationContactInfo(
                            id = ID_PHONE_CONTACTS_HEADER,
                            type = TYPE_PHONE_CONTACT_HEADER
                        )
                    ).plus(phoneContacts)
                } else emptyList()
                it.copy(filteredContacts = newFilteredContacts)
            }
        }
    }

    private fun isAPhoneContact(
        query: String,
        type: Int,
        name: String,
        displayLabel: String,
        nameWithoutSpace: String,
    ): Boolean {
        if (type != TYPE_PHONE_CONTACT) return false

        return query.isValid(name, displayLabel, nameWithoutSpace)
    }

    private fun String.isValid(
        name: String,
        displayLabel: String,
        nameWithoutSpace: String,
    ) = name.contains(this) || displayLabel.contains(this) || nameWithoutSpace.contains(this)

    /**
     * Reset the onContactsInitialized state
     */
    fun resetOnContactsInitializedState() {
        _uiState.update { it.copy(onContactsInitialized = false) }
    }

    internal fun inviteContactsByEmail(emails: List<String>) {
        viewModelScope.launch {
            Timber.d("Inviting contacts by emails. Total email: ${emails.size}")
            runCatching { inviteContactWithEmailsUseCase(emails) }
                .onSuccess {
                    _uiState.update { uiState ->
                        uiState.copy(
                            invitationStatus = InvitationStatusUiState(
                                emails = emails,
                                totalInvitationSent = it.count { it == Sent }
                            )
                        )
                    }
                }
                .onFailure { Timber.e("Failed to invite contacts by email.") }
        }
    }

    internal fun updateSelectedContactInfo(contactInfo: List<InvitationContactInfo>) {
        val selectedContactInfo = mutableListOf<InvitationContactInfo>().apply {
            addAll(_uiState.value.selectedContactInformation)
        }
        // Remove the unselected contact info
        _uiState.value.selectedContactInformation.forEachIndexed { index, info ->
            if (!contactInfo.contains(info)) {
                selectedContactInfo.removeAt(index)
            }
        }

        // Add the new selected contact info
        contactInfo.forEach { selected ->
            val isContactAdded = selectedContactInfo.any { isTheSameContact(selected, it) }
            if (!isContactAdded) {
                // Update the highlighted value as the contact is selected
                selectedContactInfo.add(selected.copy(isHighlighted = true))
            }
        }

        _uiState.update { it.copy(selectedContactInformation = selectedContactInfo) }
    }

    internal fun removeSelectedContactInformationAt(index: Int) {
        _uiState.update {
            it.copy(
                selectedContactInformation = it.selectedContactInformation.toMutableList().apply {
                    removeAt(index)
                }
            )
        }
    }

    internal fun removeSelectedContactInformationByContact(contact: InvitationContactInfo) {
        _uiState.update { uiState ->
            uiState.copy(
                selectedContactInformation = uiState.selectedContactInformation.filterNot {
                    isTheSameContact(it, contact)
                }
            )
        }
    }

    internal fun isTheSameContact(
        first: InvitationContactInfo,
        second: InvitationContactInfo,
    ): Boolean = first.id == second.id && first.displayInfo.equals(second.displayInfo, true)

    internal fun addSelectedContactInformation(contact: InvitationContactInfo) {
        _uiState.update { it.copy(selectedContactInformation = it.selectedContactInformation + contact) }
    }

    internal fun validateCameraAvailability() {
        viewModelScope.launch {
            Timber.d("Checking if there are ongoing video calls")
            runCatching { areThereOngoingVideoCallsUseCase() }
                .onSuccess {
                    if (it) {
                        showOpenCameraConfirmation()
                    } else {
                        initializeQRScanner()
                    }
                }
                .onFailure { Timber.e("Failed to check ongoing video calls", it) }
        }
    }

    private fun showOpenCameraConfirmation() {
        _uiState.update { it.copy(showOpenCameraConfirmation = true) }
    }

    internal fun onOpenCameraConfirmationShown() {
        _uiState.update { it.copy(showOpenCameraConfirmation = false) }
    }

    private fun initializeQRScanner() {
        _uiState.update { it.copy(shouldInitializeQR = true) }
    }

    internal fun onQRScannerInitialized() {
        _uiState.update { it.copy(shouldInitializeQR = false) }
    }

    companion object {
        /**
         * View's ID for the header
         */
        const val ID_PHONE_CONTACTS_HEADER = -1L

        internal const val KEY_FROM = "fromAchievement"

        private const val CONTACT_SEARCH_QUERY = "CONTACT_SEARCH_QUERY"
    }
}
