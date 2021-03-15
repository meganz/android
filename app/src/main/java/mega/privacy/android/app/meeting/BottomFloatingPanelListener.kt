package mega.privacy.android.app.meeting

import mega.privacy.android.app.meeting.adapter.Participant

interface BottomFloatingPanelListener {
    fun onChangeMicState(micOn: Boolean)
    fun onChangeCamState(camOn: Boolean)
    fun onChangeHoldState(isHold: Boolean)
    fun onChangeSpeakerState(speakerOn: Boolean)
    fun onEndMeeting()

    fun onShareLink()
    fun onInviteParticipants()

    fun onParticipantOption(participant: Participant)
}
