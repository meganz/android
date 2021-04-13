package mega.privacy.android.app.meeting.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_meeting.*
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.databinding.ActivityMeetingBinding
import mega.privacy.android.app.lollipop.megachat.AppRTCAudioManager
import mega.privacy.android.app.meeting.BottomFloatingPanelListener
import mega.privacy.android.app.meeting.BottomFloatingPanelViewHolder
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.fragments.MeetingBaseFragment
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil

@AndroidEntryPoint
class MeetingActivity : BaseActivity(), BottomFloatingPanelListener {

    companion object {
//        const val MEETING_TYPE = "meetingType"
        const val MEETING_ACTION_JOIN = "join_meeting"
        const val MEETING_ACTION_CREATE = "create_meeting"
        const val MEETING_ACTION_GUEST = "join_meeting_as_guest"
        const val MEETING_ACTION_IN = "in_meeting"

        const val MEETING_NAME = "meeting_name"
        const val MEETING_LINK = "meeting_link"

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

        private var wiredHeadsetConnected = false
        private var bluetoothConnected = false
    }

    private lateinit var binding: ActivityMeetingBinding
    private lateinit var bottomFloatingPanelViewHolder: BottomFloatingPanelViewHolder

    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return

            when (intent.getIntExtra(BroadcastConstants.ACTION_TYPE, -1)) {
                Constants.GO_OFFLINE -> getCurrentFragment()?.processOfflineMode(true)
                Constants.GO_ONLINE -> getCurrentFragment()?.processOfflineMode(false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMeetingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val meetingAction = intent.action

        initReceiver()
        initActionBar(meetingAction)
        initNavigation(meetingAction)

        if (savedInstanceState == null) {
//            val navHostFragment =
//                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
//            val navController = navHostFragment.navController

//            supportFragmentManager.beginTransaction()
//                .replace(R.id.container, CreateMeetingFragment.newInstance())
//                .commitNow()
        }


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

        bottomFloatingPanelViewHolder.onHeadphoneConnected(
            wiredHeadsetConnected,
            bluetoothConnected
        )

        updateRole()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(networkReceiver)
    }

    /**
     * Register broadcast receiver that needed
     */
    private fun initReceiver() {
        registerReceiver(
            networkReceiver, IntentFilter(Constants.BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE)
        )
    }

    /**
     * Initialize Action Bar and set icon according to param
     *
     * @param meetAction Create Meeting or Join Meeting
     */
    private fun initActionBar(meetAction: String?) {
        setSupportActionBar(binding.toolbar)
        val actionBar = supportActionBar ?: return
        actionBar.setHomeButtonEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.title = ""

        when (meetAction) {
            MEETING_ACTION_JOIN, MEETING_ACTION_CREATE, MEETING_ACTION_GUEST
                -> actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white)
            MEETING_ACTION_IN -> actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
        }
    }

    /**
     * Initialize Navigation and set startDestination according to param
     *
     * @param meetAction Create Meeting or Join Meeting
     */
    private fun initNavigation(meetAction: String?) {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val navGraph: NavGraph =
            navHostFragment.navController.navInflater.inflate(R.navigation.meeting)

        val bundle = Bundle()

        if (meetAction == MEETING_ACTION_GUEST || meetAction == MEETING_ACTION_JOIN) {
            bundle.putString(MEETING_LINK, intent.dataString)
        }

        when (meetAction) {
            MEETING_ACTION_CREATE -> navGraph.startDestination = R.id.createMeetingFragment
            MEETING_ACTION_JOIN -> navGraph.startDestination = R.id.joinMeetingFragment
            MEETING_ACTION_GUEST -> navGraph.startDestination = R.id.joinMeetingAsGuestFragment
            MEETING_ACTION_IN -> navGraph.startDestination = R.id.inMeeting
            else -> navGraph.startDestination = R.id.createMeetingFragment
        }

        // Remove app:navGraph="@navigation/meeting" and instead call navController.graph = navGraph
        // Change start destination dynamically
        navController.setGraph(navGraph, bundle)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Get current fragment from navHostFragment
     */
    fun getCurrentFragment(): MeetingBaseFragment? {
        val navHostFragment: Fragment? =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        return navHostFragment?.childFragmentManager?.fragments?.get(0) as MeetingBaseFragment?
    }

    override fun onChangeMicState(micOn: Boolean) {
        // Toast.makeText(this, "onChangeMicState $micOn", Toast.LENGTH_SHORT).show()

        wiredHeadsetConnected = !wiredHeadsetConnected
        bottomFloatingPanelViewHolder.onHeadphoneConnected(
            wiredHeadsetConnected,
            bluetoothConnected
        )
    }

    override fun onChangeCamState(camOn: Boolean) {
        // Toast.makeText(this, "onChangeCamState $camOn", Toast.LENGTH_SHORT).show()

        bluetoothConnected = !bluetoothConnected
        bottomFloatingPanelViewHolder.onHeadphoneConnected(
            wiredHeadsetConnected,
            bluetoothConnected
        )
    }

    override fun onChangeHoldState(isHold: Boolean) {
        Toast.makeText(this, "onChangeHoldState $isHold", Toast.LENGTH_SHORT).show()
    }

    override fun onChangeAudioDevice(device: AppRTCAudioManager.AudioDevice) {
        Toast.makeText(this, "onChangeAudioDevice $device", Toast.LENGTH_SHORT).show()
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

    override fun onAddContact() {

    }

    override fun onContactInfo() {

    }

    override fun onSendMessage() {

    }

    override fun onPingToSpeakerView() {

    }

    override fun onMakeModerator() {

    }

    override fun onRemoveParticipant() {

    }

    fun setBottomFloatingPanelViewHolder(visible: Boolean) {
        when (visible) {
            true -> bottom_floating_panel.visibility = View.VISIBLE;
            false -> bottom_floating_panel.visibility = View.GONE
        }
    }
}
