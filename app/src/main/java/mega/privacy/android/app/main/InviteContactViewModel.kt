package mega.privacy.android.app.main

import android.content.Context
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
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_PHONE_CONTACT_HEADER
import mega.privacy.android.app.main.model.InviteContactFilterUiState
import mega.privacy.android.app.main.model.InviteContactUiState
import mega.privacy.android.app.utils.contacts.ContactsFilter
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * InviteContact ViewModel
 */
@HiltViewModel
class InviteContactViewModel @Inject constructor(
    @ApplicationContext applicationContext: Context,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
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

    init {
        getEnabledFeatures()
    }

    private fun getEnabledFeatures() {
        viewModelScope.launch {
            val enabledFeatures = setOfNotNull(
                AppFeatures.QRCodeCompose.takeIf { getFeatureFlagValueUseCase(it) }
            )
            _uiState.update { it.copy(enabledFeatureFlags = enabledFeatures) }
        }
    }

    /**
     * Check if given feature flag is enabled or not
     */
    fun isFeatureEnabled(feature: Feature) = uiState.value.enabledFeatureFlags.contains(feature)

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
                it.copy(
                    filteredContacts = listOf(
                        // Header
                        InvitationContactInfo(
                            ID_PHONE_CONTACTS_HEADER,
                            context.get()?.getString(R.string.contacts_phone).orEmpty(),
                            TYPE_PHONE_CONTACT_HEADER
                        )
                    ).plus(phoneContacts)
                )
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
        if (type != InvitationContactInfo.TYPE_PHONE_CONTACT) return false

        return query.isValid(name, displayLabel, nameWithoutSpace)
    }

    private fun String.isValid(
        name: String,
        displayLabel: String,
        nameWithoutSpace: String,
    ) = name.contains(this) || displayLabel.contains(this) || nameWithoutSpace.contains(this)

    companion object {
        /**
         * View's ID for the header
         */
        const val ID_PHONE_CONTACTS_HEADER = -1L
    }
}
