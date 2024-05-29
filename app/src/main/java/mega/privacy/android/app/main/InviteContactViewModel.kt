package mega.privacy.android.app.main

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_PHONE_CONTACT
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_PHONE_CONTACT_HEADER
import mega.privacy.android.app.main.model.InviteContactFilterUiState
import mega.privacy.android.app.main.model.InviteContactUiState
import mega.privacy.android.app.utils.contacts.ContactsFilter
import mega.privacy.android.domain.entity.contacts.LocalContact
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.contact.FilterLocalContactsByEmailUseCase
import mega.privacy.android.domain.usecase.contact.FilterPendingOrAcceptedLocalContactsByEmailUseCase
import mega.privacy.android.domain.usecase.contact.GetLocalContactsUseCase
import mega.privacy.android.domain.usecase.qrcode.CreateContactLinkUseCase
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * InviteContact ViewModel
 */
@HiltViewModel
class InviteContactViewModel @Inject constructor(
    @ApplicationContext applicationContext: Context,
    private val getLocalContactsUseCase: GetLocalContactsUseCase,
    private val filterLocalContactsByEmailUseCase: FilterLocalContactsByEmailUseCase,
    private val filterPendingOrAcceptedLocalContactsByEmailUseCase: FilterPendingOrAcceptedLocalContactsByEmailUseCase,
    private val createContactLinkUseCase: CreateContactLinkUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val context = WeakReference(applicationContext)

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
        runCatching {
            localContactToInvitationContactInfo(getLocalContactsUseCase())
        }.onSuccess { localContacts ->
            initializeFilteredContacts(localContacts)
            initializeAllContacts(_filterUiState.value.filteredContacts)
            _uiState.update { it.copy(onContactsInitialized = true) }
        }.onFailure {
            Timber.e("Failed to get local contacts", it)
        }
    }

    private suspend fun localContactToInvitationContactInfo(localContact: List<LocalContact>) =
        buildList {
            Timber.d("megaContactToContactInfo %s", localContact.size)

            if (localContact.isEmpty()) return@buildList

            add(
                InvitationContactInfo(
                    id = ID_PHONE_CONTACTS_HEADER,
                    name = context.get()?.getString(R.string.contacts_phone).orEmpty(),
                    type = TYPE_PHONE_CONTACT_HEADER
                )
            )

            // Filter contacts if the emails exist in the MEGA contact
            val filteredMEGAContactList = filterLocalContactsByEmailUseCase(localContact)

            // Filter out pending contacts by email
            val filteredPendingMEGAContactList =
                filterPendingOrAcceptedLocalContactsByEmailUseCase(filteredMEGAContactList)

            filteredPendingMEGAContactList.forEach {
                val phoneNumberList = it.phoneNumbers + it.emails
                if (phoneNumberList.isNotEmpty()) {
                    add(
                        InvitationContactInfo(
                            id = it.id,
                            name = it.name,
                            type = TYPE_PHONE_CONTACT,
                            filteredContactInfos = phoneNumberList,
                            displayInfo = phoneNumberList[0],
                            avatarColorResId = R.color.grey_500_grey_400
                        )
                    )
                }
            }
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
                if (ContactsFilter.isTheSameContact(contact, contactInfo)) {
                    contact.copy(isHighlighted = value)
                } else {
                    contact
                }
            }

        _filterUiState.update {
            it.copy(
                filteredContacts = it.filteredContacts.toMutableList()
                    .map { contact ->
                        if (ContactsFilter.isTheSameContact(contact, contactInfo)) {
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
                            ID_PHONE_CONTACTS_HEADER,
                            context.get()?.getString(R.string.contacts_phone).orEmpty(),
                            TYPE_PHONE_CONTACT_HEADER
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

    companion object {
        /**
         * View's ID for the header
         */
        const val ID_PHONE_CONTACTS_HEADER = -1L

        private const val CONTACT_SEARCH_QUERY = "CONTACT_SEARCH_QUERY"
    }
}
