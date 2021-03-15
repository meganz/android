package mega.privacy.android.app.meeting

import android.os.Bundle
import android.widget.Toast
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.databinding.ActivityMeetingBinding
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION

class MeetingActivity : BaseActivity(), BottomFloatingPanelListener {
    private lateinit var binding: ActivityMeetingBinding

    private lateinit var bottomFloatingPanelViewHolder: BottomFloatingPanelViewHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMeetingBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val actionBar = supportActionBar ?: return
        actionBar.setHomeButtonEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        bottomFloatingPanelViewHolder =
            BottomFloatingPanelViewHolder(binding, this, isGuest, isModerator)

        val megaApi = MegaApplication.getInstance().megaApi
        val avatar = buildAvatarFile(this, megaApi.myEmail + JPG_EXTENSION)

        bottomFloatingPanelViewHolder.setParticipants(
            listOf(
                Participant("Joanna Zhao", avatar, false, true, false, false),
                Participant("Yeray Rosales", avatar, true, false, true, false),
                Participant("Harmen Porter", avatar, false, false, false, true),
                Participant("Katayama Fumiki", avatar, false, false, false, true),
                Participant("Katayama Fumiki", avatar, false, false, false, true),
                Participant("Katayama Fumiki", avatar, false, false, false, true),
                Participant("Katayama Fumiki", avatar, false, false, false, true),
                Participant("Katayama Fumiki", avatar, false, false, false, true),
                Participant("Katayama Fumiki", avatar, false, false, false, true),
                Participant("Katayama Fumiki", avatar, false, false, false, true),
                Participant("Katayama Fumiki", avatar, false, false, false, true),
                Participant("Katayama Fumiki", avatar, false, false, false, true),
                Participant("Katayama Fumiki", avatar, false, false, false, true),
            )
        )

        updateRole()
    }

    override fun onChangeMicState(micOn: Boolean) {
        Toast.makeText(this, "onChangeMicState $micOn", Toast.LENGTH_SHORT).show()
    }

    override fun onChangeCamState(camOn: Boolean) {
        Toast.makeText(this, "onChangeCamState $camOn", Toast.LENGTH_SHORT).show()
    }

    override fun onChangeHoldState(isHold: Boolean) {
        Toast.makeText(this, "onChangeHoldState $isHold", Toast.LENGTH_SHORT).show()
    }

    override fun onChangeSpeakerState(speakerOn: Boolean) {
        Toast.makeText(this, "onChangeSpeakerState $speakerOn", Toast.LENGTH_SHORT).show()
    }

    override fun onEndMeeting() {
        finish()
    }

    override fun onShareLink() {
        Toast.makeText(this, "onShareLink", Toast.LENGTH_SHORT).show()
    }

    override fun onInviteParticipants() {
        Toast.makeText(this, "onInviteParticipants", Toast.LENGTH_SHORT).show()
    }

    override fun onParticipantOption(participant: Participant) {
        Toast.makeText(this, "onParticipantOption ${participant.name}", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private var isGuest = true
        private var isModerator = false

        private fun updateRole() {
            if (isGuest) {
                isGuest = false
            } else if (!isModerator) {
                isModerator = true
            } else {
                isGuest = true
                isModerator = false
            }
        }
    }
}
