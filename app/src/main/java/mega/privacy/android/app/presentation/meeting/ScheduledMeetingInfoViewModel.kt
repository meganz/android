package mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoAction
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoState
import mega.privacy.android.domain.entity.contacts.ContactItem
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
    private val monitorConnectivity: MonitorConnectivity,
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduledMeetingInfoState())
    val state: StateFlow<ScheduledMeetingInfoState> = _state

    /**
     * Monitor connectivity event
     */
    val monitorConnectivityEvent =
        monitorConnectivity().shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    /**
     * Is network connected
     */
    val isConnected: Boolean
        get() = monitorConnectivity().value

    /**
     * Edit scheduled meeting if there is internet connection, shows an error if not.
     */
    fun onEditTap() {
        if (isConnected) {
            Timber.d("Edit scheduled meeting")
        } else {
            showError()
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
     * Tap in a button action
     */
    fun onActionTap(action: ScheduledMeetingInfoAction) {
        when (action) {
            ScheduledMeetingInfoAction.MeetingLink -> onMeetingLinkTap()
            ScheduledMeetingInfoAction.ShareMeetingLink -> onShareMeetingLinkTap()
            ScheduledMeetingInfoAction.ChatNotifications -> onChatNotificationsTap()
            ScheduledMeetingInfoAction.AllowNonHostAddParticipants -> onAllowAddParticipantsTap()
            ScheduledMeetingInfoAction.ShareFiles -> onSharedFilesTap()
            ScheduledMeetingInfoAction.ManageChatHistory -> onManageChatHistoryTap()
            ScheduledMeetingInfoAction.EnableEncryptedKeyRotation -> onEnableEncryptedKeyRotationTap()
        }
    }

    /**
     * Add participants to the chat room if there is internet connection, shows an error if not.
     */
    fun onAddParticipantsTap() {
        if (isConnected) {
            Timber.d("Add participants to the chat room")
        } else {
            showError()
        }
    }

    /**
     * Open bottom panel option of a participant.
     */
    fun onParticipantTap(contactItem: ContactItem) {
        Timber.d("Participant ${contactItem.handle} clicked")
    }

    /**
     * Create or removed meeting link if there is internet connection, shows an error if not.
     */
    fun onMeetingLinkTap() {
        if (isConnected) {
            Timber.d("Add participants to the chat room")
        } else {
            showError()
        }
    }

    /**
     * Share meeting link if there is internet connection, shows an error if not.
     */
    fun onShareMeetingLinkTap() {
        if (isConnected) {
            Timber.d("Add participants to the chat room")
        } else {
            showError()
        }
    }

    /**
     * Enable or disable chat notifications if there is internet connection, shows an error if not.
     */
    fun onChatNotificationsTap() {
        if (isConnected) {
            Timber.d("Add participants to the chat room")
        } else {
            showError()
        }
    }

    /**
     * Enable or disable the option Allow non-host add participants to the chat room if there is internet connection, shows an error if not.
     */
    fun onAllowAddParticipantsTap() {
        if (isConnected) {
            Timber.d("Allow non host add participants to the chat room")
        } else {
            showError()
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
        if (isConnected) {
            Timber.d("Manage chat history")
        } else {
            showError()
        }
    }

    /**
     * Enable encrypted key rotation if there is internet connection, shows an error if not.
     */
    fun onEnableEncryptedKeyRotationTap() {
        if (isConnected) {
            Timber.d("Enable Encrypted Key Rotation")
        } else {
            showError()
        }
    }

    /**
     * Add error when there is no internet connection
     */
    private fun showError() {
        _state.update { it.copy(error = R.string.check_internet_connection_error) }
    }
}