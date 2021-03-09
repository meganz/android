package mega.privacy.android.app.meeting

interface BottomFloatingPanelListener {
    fun onChangeMicState(micOn: Boolean)
    fun onChangeCamState(camOn: Boolean)
    fun onChangeHoldState(isHold: Boolean)
    fun onChangeSpeakerState(speakerOn: Boolean)
    fun onEndMeeting()

    fun onShareLink()
    fun onInviteParticipants()
}
