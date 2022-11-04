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
    monitorConnectivity: MonitorConnectivity
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
                    contactItemList = contactList,
                    emptyViewVisible = contactList.isEmpty(),
                )
            }
            getContactsData(contactList)
        }
    }

    private suspend fun getContactsData(contactList: List<ContactItem>) {
        contactList.forEach { contactItem ->
            val contactData = getContactData(contactItem)
            _state.value.contactItemList.apply {
                findItemByHandle(contactItem.handle)?.apply {
                    toMutableList().apply {
                        replaceIfExists(copy(contactData = contactData))
                        _state.update { it.copy(contactItemList = this.sortList()) }
                    }
                }
            }
        }
    }

    private fun observeContactUpdates() {
        viewModelScope.launch {
            monitorContactUpdates().collectLatest { userUpdates ->
                val contactList = applyContactUpdates(_state.value.contactItemList, userUpdates)
                _state.update { it.copy(contactItemList = contactList) }
            }
        }
    }

    private fun observeLastGreenUpdates() {
        viewModelScope.launch {
            monitorLastGreenUpdates().collectLatest { (handle, lastGreen) ->
                _state.value.contactItemList.apply {
                    findItemByHandle(handle)?.apply {
                        toMutableList().apply {
                            replaceIfExists(copy(lastSeen = lastGreen))
                            _state.update { it.copy(contactItemList = this.sortList()) }
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

                _state.value.contactItemList.apply {
                    findItemByHandle(userHandle)?.apply {
                        toMutableList().apply {
                            replaceIfExists(copy(status = status))
                            _state.update { it.copy(contactItemList = this.sortList()) }
                        }
                    }
                }
            }
        }
    }

    private fun observeNewContacts() {
        viewModelScope.launch {
            monitorContactRequestUpdates().collectLatest { newContacts ->
                val contactList = addNewContacts(_state.value.contactItemList, newContacts)
                _state.update { it.copy(contactItemList = contactList.sortList()) }
            }
        }
    }


    /**
     * Starts a conversation if there is internet connection, shows an error if not.
     */
    fun onEditTap() {
        if (isConnected.value) {
            Timber.d("***************** onEditTap")
        } else {
            _state.update { it.copy(error = R.string.check_internet_connection_error) }
        }
    }

    /**
     * Starts a conversation if there is internet connection, shows an error if not.
     */
    fun onAddParticipantsTap() {
        if (isConnected.value) {
            Timber.d("***************** onAddParticipantsTap")
        } else {
            _state.update { it.copy(error = R.string.check_internet_connection_error) }
        }
    }

    /**
     * Starts a conversation if there is internet connection, shows an error if not.
     */
    fun onMeetingLinkTap() {
        Timber.d("***************** onMeetingLinkTap")
    }

    /**
     * Starts a conversation if there is internet connection, shows an error if not.
     */
    fun onShareMeetingLinkTap() {
        Timber.d("***************** onShareMeetingLinkTap")
    }

    /**
     * Starts a conversation if there is internet connection, shows an error if not.
     */
    fun onChatNotificationsTap() {
        Timber.d("***************** onChatNotificationsTap")
    }

    /**
     * Starts a conversation if there is internet connection, shows an error if not.
     */
    fun onAllowAddParticipantsTap() {
        Timber.d("***************** onAllowAddParticipantsTap")
    }
    /**
     * Starts a conversation if there is internet connection, shows an error if not.
     */
    fun onSharedFilesTap() {
        Timber.d("***************** onSharedFilesTap")
    }
    /**
     * Starts a conversation if there is internet connection, shows an error if not.
     */
    fun onManageChatHistoryTap() {
        Timber.d("***************** onManageChatHistoryTap")
    }

}