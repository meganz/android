package mega.privacy.android.app.meeting

import mega.privacy.android.app.lollipop.megachat.AppRTCAudioManager
import mega.privacy.android.app.meeting.adapter.Participant

interface BottomFloatingPanelListener {
    fun onChangeMicState(micOn: Boolean)
    fun onChangeCamState(camOn: Boolean)
    fun onChangeHoldState(isHold: Boolean)
    fun onChangeAudioDevice(device: AppRTCAudioManager.AudioDevice)
    fun onEndMeeting()

    fun onShareLink()
    fun onInviteParticipants()

    fun onParticipantOption(participant: Participant)

    fun onAddContact()
    fun onContactInfo()
    fun onSendMessage()
    fun onPingToSpeakerView()
    fun onMakeModerator()
    fun onRemoveParticipant()
}
