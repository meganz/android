package mega.privacy.android.app.meeting.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ActivityMeetingBinding
import mega.privacy.android.app.listeners.ChatChangeVideoStreamListener
import mega.privacy.android.app.meeting.BottomFloatingPanelListener
import mega.privacy.android.app.meeting.BottomFloatingPanelViewHolder
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.fragments.MeetingParticipantBottomSheetDialogFragment
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.IncomingCallNotification
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.VideoCaptureUtils

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

    private var gridViewMenuItem: MenuItem? = null
    private var speakerViewMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        IncomingCallNotification.cancelIncomingCallNotification(this)
        MegaApplication.setShowPinScreen(true)

        binding = ActivityMeetingBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val actionBar = supportActionBar ?: return
        actionBar.setHomeButtonEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        binding.meetingTitle.setText("Meeting title")

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
            MEETING_TYPE_JOIN -> navGraph.startDestination = R.id.joinMeetingFragment
            MEETING_TYPE_CREATE -> navGraph.startDestination = R.id.createMeetingFragment
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_meeting, menu)

        menu?.findItem(R.id.swap_camera)?.isVisible = true
        speakerViewMenuItem = menu?.findItem(R.id.speaker_view)
        speakerViewMenuItem?.isVisible = true
        gridViewMenuItem = menu?.findItem(R.id.grid_view)
        gridViewMenuItem?.isVisible = true

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.swap_camera -> {
                logDebug("Swap camera")
                VideoCaptureUtils.swapCamera(ChatChangeVideoStreamListener(applicationContext))
                true
            }
            R.id.grid_view -> {
                logDebug("Change to grid view")
                gridViewMenuItem?.isVisible = false
                speakerViewMenuItem?.isVisible = true
                true
            }
            R.id.speaker_view -> {
                logDebug("Change to speaker view")
                gridViewMenuItem?.isVisible = true
                speakerViewMenuItem?.isVisible = false
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
        val bottomSheetDialogFragment = MeetingParticipantBottomSheetDialogFragment()
        bottomSheetDialogFragment.show(supportFragmentManager, bottomSheetDialogFragment.tag)
    }

    override fun onAddContact() {
        Toast.makeText(this, "onAddContact", Toast.LENGTH_SHORT).show()
    }

    override fun onContactInfo() {
        Toast.makeText(this, "onContactInfo", Toast.LENGTH_SHORT).show()
    }

    override fun onSendMessage() {
        Toast.makeText(this, "onSendMessage", Toast.LENGTH_SHORT).show()
    }

    override fun onPingToSpeakerView() {
        Toast.makeText(this, "onPingToSpeakerView", Toast.LENGTH_SHORT).show()
    }

    override fun onMakeModerator() {
        Toast.makeText(this, "onMakeModerator", Toast.LENGTH_SHORT).show()
    }

    override fun onRemoveParticipant() {
        Toast.makeText(this, "onRemoveParticipant", Toast.LENGTH_SHORT).show()
    }
}
