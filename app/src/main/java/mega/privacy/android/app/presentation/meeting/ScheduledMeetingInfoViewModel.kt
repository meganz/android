package mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.data.extensions.findItemByHandle
import mega.privacy.android.app.data.extensions.replaceIfExists
import mega.privacy.android.app.data.extensions.sortList
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoState
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.usecase.AddNewContacts
import mega.privacy.android.domain.usecase.ApplyContactUpdates
import mega.privacy.android.domain.usecase.GetContactData
import mega.privacy.android.domain.usecase.GetVisibleContacts
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.MonitorContactRequestUpdates
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import mega.privacy.android.domain.usecase.MonitorLastGreenUpdates
import mega.privacy.android.domain.usecase.MonitorOnlineStatusUpdates
import mega.privacy.android.domain.usecase.RequestLastGreen
import timber.log.Timber
import javax.inject.Inject


/**
 * StartConversationFragment view model.
 *
 * @property getVisibleContacts           [GetVisibleContacts]
 * @property getContactData               [GetContactData]
 * @property monitorContactUpdates        [MonitorContactUpdates]
 * @property applyContactUpdates          [ApplyContactUpdates]
 * @property monitorLastGreenUpdates      [MonitorLastGreenUpdates]
 * @property monitorOnlineStatusUpdates   [MonitorOnlineStatusUpdates]
 * @property monitorContactRequestUpdates [MonitorContactRequestUpdates]
 * @property addNewContacts               [AddNewContacts]
 * @property requestLastGreen             [RequestLastGreen]
 * @property state                        Current view state as [ScheduledMeetingInfoState]
 */
@HiltViewModel
class ScheduledMeetingInfoViewModel @Inject constructor(
    private val getVisibleContacts: GetVisibleContacts,
    private val getContactData: GetContactData,
    private val monitorContactUpdates: MonitorContactUpdates,
    private val applyContactUpdates: ApplyContactUpdates,
    private val monitorLastGreenUpdates: MonitorLastGreenUpdates,
    private val monitorOnlineStatusUpdates: MonitorOnlineStatusUpdates,
    private val monitorContactRequestUpdates: MonitorContactRequestUpdates,
    private val addNewContacts: AddNewContacts,
    private val requestLastGreen: RequestLastGreen,
    monitorConnectivity: MonitorConnectivity,
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduledMeetingInfoState())
    val state: StateFlow<ScheduledMeetingInfoState> = _state

    private val isConnected =
        monitorConnectivity().stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        observeStateChanges()
        getContacts()
        observeContactUpdates()
        observeLastGreenUpdates()
        observeOnlineStatusUpdates()
        observeNewContacts()
    }

    private fun observeStateChanges() {

    }

    private fun getContacts() {
        viewModelScope.launch {
            val contactList = getVisibleContacts()
            _state.update {
                it.copy(
                    participantItemList = contactList
                )
            }
            getContactsData(contactList)
        }
    }

    private suspend fun getContactsData(contactList: List<ContactItem>) {
        contactList.forEach { contactItem ->
            val contactData = getContactData(contactItem)
            _state.value.participantItemList.apply {
                findItemByHandle(contactItem.handle)?.apply {
                    toMutableList().apply {
                        replaceIfExists(copy(contactData = contactData))
                        _state.update { it.copy(participantItemList = this.sortList()) }
                    }
                }
            }
        }
    }

    private fun observeContactUpdates() {
        viewModelScope.launch {
            monitorContactUpdates().collectLatest { userUpdates ->
                val contactList = applyContactUpdates(_state.value.participantItemList, userUpdates)
                _state.update { it.copy(participantItemList = contactList) }
            }
        }
    }

    private fun observeLastGreenUpdates() {
        viewModelScope.launch {
            monitorLastGreenUpdates().collectLatest { (handle, lastGreen) ->
                _state.value.participantItemList.apply {
                    findItemByHandle(handle)?.apply {
                        toMutableList().apply {
                            replaceIfExists(copy(lastSeen = lastGreen))
                            _state.update { it.copy(participantItemList = this.sortList()) }
                        }
                    }
                }
            }
        }
    }

    private fun observeOnlineStatusUpdates() {
        viewModelScope.launch {
            monitorOnlineStatusUpdates().collectLatest { (userHandle, status) ->
                if (status != UserStatus.Online) {
                    requestLastGreen(userHandle)
                }

                _state.value.participantItemList.apply {
                    findItemByHandle(userHandle)?.apply {
                        toMutableList().apply {
                            replaceIfExists(copy(status = status))
                            _state.update { it.copy(participantItemList = this.sortList()) }
                        }
                    }
                }
            }
        }
    }

    private fun observeNewContacts() {
        viewModelScope.launch {
            monitorContactRequestUpdates().collectLatest { newContacts ->
                val contactList = addNewContacts(_state.value.participantItemList, newContacts)
                _state.update { it.copy(participantItemList = contactList.sortList()) }
            }
        }
    }


    /**
     * Edit scheduled meeting if there is internet connection, shows an error if not.
     */
    fun onEditTap() {
        if (isConnected.value) {
            Timber.d("Edit scheduled meeting")
        } else {
            _state.update { it.copy(error = R.string.check_internet_connection_error) }
        }
    }

    /**
     * See more participants in the list.
     */
    fun onSeeMoreTap() {
    }

    /**
     * Leave group chat.
     */
    fun onLeaveGroupTap() {
    }

    /**
     * Add participants to the chat room if there is internet connection, shows an error if not.
     */
    fun onAddParticipantsTap() {
        if (isConnected.value) {
            Timber.d("Add participants to the chat room")
        } else {
            _state.update { it.copy(error = R.string.check_internet_connection_error) }
        }
    }

    /**
     * Open bottom panel option of a participant.
     */
    fun onParticipantTap(contactItem: ContactItem) {

    }

    /**
     * Create or removed meeting link if there is internet connection, shows an error if not.
     */
    fun onMeetingLinkTap() {
        if (isConnected.value) {
            Timber.d("Add participants to the chat room")
        } else {
            _state.update { it.copy(error = R.string.check_internet_connection_error) }
        }
    }

    /**
     * Share meeting link if there is internet connection, shows an error if not.
     */
    fun onShareMeetingLinkTap() {
        if (isConnected.value) {
            Timber.d("Add participants to the chat room")
        } else {
            _state.update { it.copy(error = R.string.check_internet_connection_error) }
        }
    }

    /**
     * Enable or disable chat notifications if there is internet connection, shows an error if not.
     */
    fun onChatNotificationsTap() {
        if (isConnected.value) {
            Timber.d("Add participants to the chat room")
        } else {
            _state.update { it.copy(error = R.string.check_internet_connection_error) }
        }
    }

    /**
     * Enable or disable the option Allow non-host add participants to the chat room if there is internet connection, shows an error if not.
     */
    fun onAllowAddParticipantsTap() {
        if (isConnected.value) {
            Timber.d("Allow non host add participants to the chat room")
        } else {
            _state.update { it.copy(error = R.string.check_internet_connection_error) }
        }
    }

    /**
     * Show shared files in the chat room.
     */
    fun onSharedFilesTap() {
        Timber.d("Show shared files in the chat room")
    }

    /**
     * Manage chat history if there is internet connection, shows an error if not.
     */
    fun onManageChatHistoryTap() {
        if (isConnected.value) {
            Timber.d("Manage chat history")
        } else {
            _state.update { it.copy(error = R.string.check_internet_connection_error) }
        }
    }

    /**
     * Enable encrypted key rotation if there is internet connection, shows an error if not.
     */
    fun onEnableEncryptedKeyRotationTap() {
        if (isConnected.value) {
            Timber.d("Enable Encrypted Key Rotation")
        } else {
            _state.update { it.copy(error = R.string.check_internet_connection_error) }
        }
    }
}