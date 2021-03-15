package mega.privacy.android.app.meeting.activity

import android.os.Bundle
import android.widget.Toast
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ActivityMeetingBinding
import mega.privacy.android.app.meeting.BottomFloatingPanelListener
import mega.privacy.android.app.meeting.BottomFloatingPanelViewHolder
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.FileUtil

class MeetingActivity : BaseActivity(), BottomFloatingPanelListener {
    companion object{
        const val MEETING_TYPE = "meetingType"
        const val MEETING_TYPE_JOIN = "join_meeting"
        const val MEETING_TYPE_CREATE = "create_meeting"

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

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        if (savedInstanceState == null) {
//            val navHostFragment =
//                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
//            val navController = navHostFragment.navController

//            supportFragmentManager.beginTransaction()
//                .replace(R.id.container, CreateMeetingFragment.newInstance())
//                .commitNow()
        }
        val navGraph: NavGraph = navHostFragment.navController.navInflater.inflate(R.navigation.meeting)
        when(intent.getStringExtra(MEETING_TYPE)){
            MEETING_TYPE_JOIN->navGraph.startDestination = R.id.joinMeetingFragment
            MEETING_TYPE_CREATE->navGraph.startDestination = R.id.createMeetingFragment
        }
        navController.graph = navGraph

        bottomFloatingPanelViewHolder =
            BottomFloatingPanelViewHolder(binding, this, isGuest, isModerator)

        val megaApi = MegaApplication.getInstance().megaApi
        val avatar =
            CacheFolderManager.buildAvatarFile(this, megaApi.myEmail + FileUtil.JPG_EXTENSION)

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
}
