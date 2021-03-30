package mega.privacy.android.app.meeting.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.Menu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
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
import mega.privacy.android.app.listeners.ChatChangeVideoStreamListener
import mega.privacy.android.app.meeting.BottomFloatingPanelListener
import mega.privacy.android.app.meeting.BottomFloatingPanelViewHolder
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.fragments.MeetingBaseFragment
import mega.privacy.android.app.meeting.fragments.SelfFeedFloatingWindowFragment
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.IncomingCallNotification
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.VideoCaptureUtils

@AndroidEntryPoint
class MeetingActivity : BaseActivity(), BottomFloatingPanelListener {

    companion object{
        const val MEETING_TYPE = "meetingType"
        const val MEETING_TYPE_JOIN = "join_meeting"
        const val MEETING_TYPE_CREATE = "create_meeting"
        const val MEETING_TYPE_GUEST = "join_meeting_as_guest"
        const val MEETING_TYPE_IN = "in_meeting"

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

    private var gridViewMenuItem: MenuItem? = null
    private var speakerViewMenuItem: MenuItem? = null

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
        MegaApplication.setShowPinScreen(true)

        binding = ActivityMeetingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val meetType = intent.getStringExtra(MEETING_TYPE)

        initReceiver()
        initActionBar(meetType)
        initNavigation(meetType)

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

        bottomFloatingPanelViewHolder.onHeadphoneConnected(wiredHeadsetConnected, bluetoothConnected)

        updateRole()
        showSelFeedFloatingWindow()
    }

    private fun showSelFeedFloatingWindow(){
        val fragment = SelfFeedFloatingWindowFragment()
        addFragment(fragment, R.id.small_camera_fragment)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_meeting, menu)

        menu?.findItem(R.id.swap_camera)?.isVisible = true
        speakerViewMenuItem = menu?.findItem(R.id.speaker_view)
        speakerViewMenuItem?.isVisible = true
        gridViewMenuItem = menu?.findItem(R.id.grid_view)
        gridViewMenuItem?.isVisible = false
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

    private fun AppCompatActivity.addFragment(fragment: Fragment, frameId: Int){
        supportFragmentManager.inTransaction { add(frameId, fragment) }
    }

    private fun AppCompatActivity.replaceFragment(fragment: Fragment, frameId: Int) {
        supportFragmentManager.inTransaction{replace(frameId, fragment)}
    }

    inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
        beginTransaction().func().commit()
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
        when(meetType) {
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
        val navGraph: NavGraph = navHostFragment.navController.navInflater.inflate(R.navigation.meeting)

        when(meetType){
            MEETING_TYPE_CREATE -> navGraph.startDestination = R.id.createMeetingFragment
            MEETING_TYPE_JOIN -> navGraph.startDestination = R.id.joinMeetingFragment
            MEETING_TYPE_GUEST -> navGraph.startDestination = R.id.joinMeetingAsGuestFragment
            MEETING_TYPE_IN -> navGraph.startDestination = R.id.inMeeting
            else -> navGraph.startDestination = R.id.createMeetingFragment
        }

        // Remove app:navGraph="@navigation/meeting" and instead call navController.graph = navGraph
        // Change start destination dynamically
        navController.graph = navGraph
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
        bottomFloatingPanelViewHolder.onHeadphoneConnected(wiredHeadsetConnected, bluetoothConnected)
    }

    override fun onChangeCamState(camOn: Boolean) {
        // Toast.makeText(this, "onChangeCamState $camOn", Toast.LENGTH_SHORT).show()

        bluetoothConnected = !bluetoothConnected
        bottomFloatingPanelViewHolder.onHeadphoneConnected(wiredHeadsetConnected, bluetoothConnected)
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
        when(visible) {
            true-> bottom_floating_panel.visibility = View.VISIBLE;
            false-> bottom_floating_panel.visibility = View.GONE
        }
    }
}
