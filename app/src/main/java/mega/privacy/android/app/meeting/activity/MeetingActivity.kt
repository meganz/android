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
import mega.privacy.android.app.meeting.AnimationTool.fadeInOut
import mega.privacy.android.app.meeting.BottomFloatingPanelListener
import mega.privacy.android.app.meeting.BottomFloatingPanelViewHolder
import mega.privacy.android.app.meeting.TestTool
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.fragments.MeetingBaseFragment
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.IncomingCallNotification

@AndroidEntryPoint
class MeetingActivity : BaseActivity(), BottomFloatingPanelListener {

    companion object {
        const val MEETING_TYPE = "meetingType"
        const val MEETING_TYPE_JOIN = "join_meeting"
        const val MEETING_TYPE_CREATE = "create_meeting"
        const val MEETING_TYPE_GUEST = "join_meeting_as_guest"
        const val MEETING_TYPE_IN = "in_meeting"

        const val MEETING_NAME = "meeting_name"
        const val MEETING_LINK = "meeting_link"
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

        IncomingCallNotification.cancelIncomingCallNotification(this)

        binding = ActivityMeetingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val meetType = intent.getStringExtra(MEETING_TYPE)

        initReceiver()
        initActionBar(meetType)
        initNavigation(meetType)

        // TODO: pass real role here
        bottomFloatingPanelViewHolder = BottomFloatingPanelViewHolder(binding, this, false, true)

        // TODO: pass real headphone state here
        bottomFloatingPanelViewHolder.onHeadphoneConnected(false, false)

        // TODO: load real participants and set it
        bottomFloatingPanelViewHolder.setParticipants(TestTool.getTestParticipants(this))
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
     * @param meetType Create Meeting or Join Meeting
     */
    private fun initActionBar(meetType: String?) {
        setSupportActionBar(binding.toolbar)
        val actionBar = supportActionBar ?: return
        actionBar.setHomeButtonEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setTitle("")

        when (meetType) {
            MEETING_TYPE_JOIN -> actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white)
            MEETING_TYPE_CREATE -> actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white)
        }

    }

    /**
     * Initialize Navigation and set startDestination according to param
     *
     * @param meetType Create Meeting or Join Meeting
     */
    private fun initNavigation(meetType: String?) {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val navGraph: NavGraph =
            navHostFragment.navController.navInflater.inflate(R.navigation.meeting)

        val bundle = Bundle()

        when (meetType) {
            MEETING_TYPE_CREATE -> navGraph.startDestination = R.id.createMeetingFragment
            MEETING_TYPE_JOIN -> {
                bundle.putString(MEETING_LINK, intent.dataString)
                navGraph.startDestination = R.id.joinMeetingFragment
            }
            MEETING_TYPE_GUEST -> navGraph.startDestination = R.id.joinMeetingAsGuestFragment
            MEETING_TYPE_IN -> navGraph.startDestination = R.id.inMeetingFragment
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
        Toast.makeText(this, "onChangeMicState $micOn", Toast.LENGTH_SHORT).show()
    }

    override fun onChangeCamState(camOn: Boolean) {
        Toast.makeText(this, "onChangeCamState $camOn", Toast.LENGTH_SHORT).show()
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

    fun bottomFloatingPanelInOut() {
        bottom_floating_panel.fadeInOut()
    }

    fun collpaseFloatingPanel() {
        bottomFloatingPanelViewHolder.collpase()
    }

    fun hideActionBar() {
        binding.toolbar.visibility = View.GONE
    }
}
