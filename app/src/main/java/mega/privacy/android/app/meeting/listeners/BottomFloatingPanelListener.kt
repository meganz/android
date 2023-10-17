package mega.privacy.android.app.meeting.listeners

import mega.privacy.android.app.meeting.adapter.Participant

interface BottomFloatingPanelListener {
    /**
     * Listener for button bar
     */
    fun onChangeMicState(micOn: Boolean)
    fun onChangeCamState(camOn: Boolean)
    fun onChangeHoldState(isHold: Boolean)
    fun onChangeSpeakerState()
    fun onEndMeeting()

    /**
     * Listener for invite button
     */
    fun onInviteParticipants()

    /**
     * Listener for participant item
     */
    fun onParticipantOption(participant: Participant)
}
