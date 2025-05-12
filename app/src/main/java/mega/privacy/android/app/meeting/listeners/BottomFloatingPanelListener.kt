package mega.privacy.android.app.meeting.listeners

interface BottomFloatingPanelListener {
    /**
     * Listener for button bar
     */
    fun onChangeMicState(micOn: Boolean)
    fun onChangeCamState(camOn: Boolean)
    fun onChangeSpeakerState()
    fun onEndMeeting()

    /**
     * Listener for invite button
     */
    fun onInviteParticipants()
}
