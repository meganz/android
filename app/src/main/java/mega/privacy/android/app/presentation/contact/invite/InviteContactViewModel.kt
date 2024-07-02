package mega.privacy.android.app.presentation.contact.invite

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.main.InvitationContactInfo
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_MANUAL_INPUT_EMAIL
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_PHONE_CONTACT
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_PHONE_CONTACT_HEADER
import mega.privacy.android.app.main.model.InviteContactUiState
import mega.privacy.android.app.presentation.contact.invite.mapper.EmailValidationResultMapper
import mega.privacy.android.app.presentation.contact.invite.mapper.InvitationContactInfoUiMapper
import mega.privacy.android.app.presentation.contact.invite.mapper.InvitationStatusMessageUiMapper
import mega.privacy.android.app.presentation.contact.invite.model.EmailValidationResult.InvalidResult
import mega.privacy.android.app.presentation.contact.invite.model.EmailValidationResult.ValidResult
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.contact.FilterLocalContactsByEmailUseCase
import mega.privacy.android.domain.usecase.contact.FilterPendingOrAcceptedLocalContactsByEmailUseCase
import mega.privacy.android.domain.usecase.contact.GetLocalContactsUseCase
import mega.privacy.android.domain.usecase.contact.InviteContactWithEmailsUseCase
import mega.privacy.android.domain.usecase.contact.ValidateEmailInputForInvitationUseCase
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
    private val validateEmailInputForInvitationUseCase: ValidateEmailInputForInvitationUseCase,
    private val invitationContactInfoUiMapper: InvitationContactInfoUiMapper,
    private val invitationStatusMessageUiMapper: InvitationStatusMessageUiMapper,
    private val emailValidationResultMapper: EmailValidationResultMapper,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InviteContactUiState())

    /**
     * Invite contact UI state
     */
    val uiState = _uiState.asStateFlow()

    private var filterContactsJob: Job? = null

    /**
     * List of all available contacts
     */
    var allContacts: List<InvitationContactInfo> = emptyList()
        private set

    private val isFromAchievement = savedStateHandle.get<Boolean>(KEY_FROM) ?: false

    init {
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
    internal fun initializeContacts() = viewModelScope.launch {
        if (_uiState.value.areContactsInitialized) return@launch

        _uiState.update { it.copy(isLoading = true) }
        runCatching { getInvitationContactInfo() }
            .onSuccess { invitationContactInfo ->
                allContacts = invitationContactInfo
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        areContactsInitialized = true,
                        filteredContacts = invitationContactInfo
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        areContactsInitialized = true
                    )
                }
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
     * Update current search query
     */
    internal fun onSearchQueryChange(query: String) {
        viewModelScope.launch {
            updateCurrentSearchQuery(query)
            // Debounce
            delay(SEARCH_QUERY_DEBOUNCE_DURATION)
            filterContacts(query)
        }
    }

    /**
     * Filter contacts based on the given query
     *
     * @param query The user's input
     */
    internal fun filterContacts(query: String?) {
        filterContactsJob?.cancel()
        filterContactsJob = viewModelScope.launch(defaultDispatcher) {
            Timber.d("Filtering contact")

            if (query.isNullOrBlank()) {
                // Reset the contact list
                _uiState.update { it.copy(filteredContacts = allContacts) }
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

            _uiState.update {
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
    internal fun resetOnContactsInitializedState() {
        _uiState.update { it.copy(areContactsInitialized = false) }
    }

    internal fun onOpenCameraConfirmationShown() {
        _uiState.update { it.copy(showOpenCameraConfirmation = false) }
    }

    internal fun onQRScannerInitialized() {
        _uiState.update { it.copy(shouldInitializeQR = false) }
    }

    internal fun onDismissContactListContactInfo() {
        _uiState.update { it.copy(invitationContactInfoWithMultipleContacts = null) }
    }

    internal fun validateEmailInput(email: String) {
        viewModelScope.launch {
            Timber.d("Validating the inputted email", email)
            runCatching { validateEmailInputForInvitationUseCase(email) }
                .onSuccess { validity ->
                    when (val validationResult = emailValidationResultMapper(email, validity)) {
                        ValidResult -> {
                            addContactInfo(email, TYPE_MANUAL_INPUT_EMAIL)
                            filterContacts(_uiState.value.query)
                        }

                        else -> {
                            _uiState.update {
                                it.copy(
                                    emailValidationMessage = (validationResult as InvalidResult).message
                                )
                            }
                        }
                    }
                }
                .onFailure { Timber.e("Failed to validate input email", it) }
        }
    }

    internal fun addContactInfo(displayInfo: String, type: Int) {
        val existingContactInfo = allContacts.firstOrNull { availableContact ->
            availableContact.displayInfo.equals(other = displayInfo, ignoreCase = true)
        }
        val info = InvitationContactInfo(
            id = displayInfo.hashCode().toLong(),
            type = type,
            displayInfo = displayInfo
        )
        if (existingContactInfo != null) {
            validateContactListItemClick(existingContactInfo)
        } else if (!isContactAdded(info)) {
            addSelectedContactInformation(info)
        }
    }

    internal fun validateContactListItemClick(contactInfo: InvitationContactInfo) {
        if (contactInfo.hasMultipleContactInfos()) {
            _uiState.update { it.copy(invitationContactInfoWithMultipleContacts = contactInfo) }
        } else {
            updateContactListItemSelected(contactInfo)
        }
    }

    private fun updateContactListItemSelected(contactInfo: InvitationContactInfo) {
        toggleContactHighlightedInfo(contactInfo)
        if (isContactAdded(contactInfo)) {
            removeSelectedContactInformation(contactInfo)
        } else {
            addSelectedContactInformation(contactInfo)
        }
    }

    /**
     * Update contact's highlighted or selected value
     *
     * @param contactInfo The selected contact info
     */
    fun toggleContactHighlightedInfo(contactInfo: InvitationContactInfo) {
        toggleContactHighlightedInfo(contactInfo, !contactInfo.isHighlighted)
    }

    private fun isContactAdded(contactInfo: InvitationContactInfo): Boolean =
        _uiState.value.selectedContactInformation.any { isTheSameContact(it, contactInfo) }

    internal fun addSelectedContactInformation(contact: InvitationContactInfo) {
        _uiState.update {
            it.copy(
                selectedContactInformation = it.selectedContactInformation + contact.copy(
                    isHighlighted = true
                )
            )
        }
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

    private fun initializeQRScanner() {
        _uiState.update { it.copy(shouldInitializeQR = true) }
    }

    /**
     * Rules:
     * - If the list of selected contacts contains both emails and phone numbers,
     *   then we should invite the emails first then the phone number 2 seconds after the emails
     *   are successfully invited.
     * - If the list of selected contacts contains only phone numbers, only invite the phone numbers.
     */
    internal fun inviteContacts() {
        val addedEmails = mutableListOf<String>()
        val addedPhoneNumbers = mutableListOf<String>()
        _uiState.value.selectedContactInformation.forEach { contact ->
            if (contact.isEmailContact()) {
                addedEmails.add(contact.displayInfo)
            } else {
                addedPhoneNumbers.add(contact.displayInfo)
            }
        }
        if (addedEmails.isNotEmpty()) {
            inviteEmailsAndPendingPhoneNumber(
                emails = addedEmails,
                pendingPhoneNumbers = addedPhoneNumbers
            )
        } else if (addedPhoneNumbers.isNotEmpty()) {
            _uiState.update { it.copy(pendingPhoneNumberInvitations = addedPhoneNumbers) }
        }
    }

    private fun inviteEmailsAndPendingPhoneNumber(
        emails: List<String>,
        pendingPhoneNumbers: List<String>,
    ) {
        viewModelScope.launch {
            Timber.d("Inviting contacts by emails. Total email: ${emails.size}")
            runCatching { inviteContactWithEmailsUseCase(emails) }
                .onSuccess { requests ->
                    _uiState.update { uiState ->
                        uiState.copy(
                            invitationStatusResult = invitationStatusMessageUiMapper(
                                isFromAchievement = isFromAchievement,
                                requests = requests,
                                emails = emails
                            ),
                            pendingPhoneNumberInvitations = pendingPhoneNumbers
                        )
                    }
                }
                .onFailure { Timber.e("Failed to invite contacts by email.", it) }
        }
    }

    /**
     * This method will update the list of selected contact info
     * after updating the list from the contact info list dialog.
     *
     * Note: The contact info list dialog shows multiple contact info from the same contact.
     */
    internal fun updateSelectedContactInfoByInfoWithMultipleContacts(
        contactInfo: InvitationContactInfo,
        newListOfSelectedContact: List<InvitationContactInfo>,
    ) {
        val selectedContactInfo = mutableListOf<InvitationContactInfo>().apply {
            addAll(_uiState.value.selectedContactInformation)
        }
        // Remove the unselected contact info
        _uiState.value.selectedContactInformation.forEach { info ->
            if (!newListOfSelectedContact.contains(info)) {
                selectedContactInfo.remove(info)
            }
        }

        // Add the new selected contact info
        newListOfSelectedContact.forEach { selected ->
            val isContactAdded = selectedContactInfo.any { isTheSameContact(selected, it) }
            if (!isContactAdded) {
                selectedContactInfo.add(selected.copy(isHighlighted = true))
            }
        }

        _uiState.update { it.copy(selectedContactInformation = selectedContactInfo) }

        // Update the highlighted value of the contact
        toggleContactHighlightedInfo(
            contactInfo = contactInfo,
            value = selectedContactInfo.any { it.id == contactInfo.id }
        )

        // Reset query
        updateCurrentSearchQuery(query = "")
    }

    private fun updateCurrentSearchQuery(query: String) {
        savedStateHandle[CONTACT_SEARCH_QUERY] = query
        _uiState.update {
            it.copy(
                query = savedStateHandle.get<String>(key = CONTACT_SEARCH_QUERY).orEmpty()
            )
        }
    }

    internal fun onContactChipClick(contactInfo: InvitationContactInfo) {
        removeSelectedContactInformation(contactInfo)
        toggleContactHighlightedInfo(
            contactInfo = contactInfo,
            value = if (contactInfo.hasMultipleContactInfos()) {
                _uiState.value.selectedContactInformation.any { it.id == contactInfo.id }
            } else false
        )
    }

    internal fun removeSelectedContactInformation(contact: InvitationContactInfo) {
        _uiState.update { uiState ->
            uiState.copy(
                selectedContactInformation = uiState.selectedContactInformation.filterNot {
                    isTheSameContact(it, contact)
                }
            )
        }
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
                if (contact.id == contactInfo.id) {
                    contact.copy(isHighlighted = value)
                } else {
                    contact
                }
            }

        _uiState.update {
            it.copy(
                filteredContacts = it.filteredContacts.toMutableList()
                    .map { contact ->
                        if (contact.id == contactInfo.id) {
                            contact.copy(isHighlighted = value)
                        } else {
                            contact
                        }
                    }
            )
        }
    }

    private fun isTheSameContact(
        first: InvitationContactInfo,
        second: InvitationContactInfo,
    ): Boolean = first.id == second.id && first.displayInfo.equals(second.displayInfo, true)

    companion object {
        /**
         * View's ID for the header
         */
        const val ID_PHONE_CONTACTS_HEADER = -1L

        internal const val KEY_FROM = "fromAchievement"

        private const val CONTACT_SEARCH_QUERY = "CONTACT_SEARCH_QUERY"

        private const val SEARCH_QUERY_DEBOUNCE_DURATION = 300L
    }
}
