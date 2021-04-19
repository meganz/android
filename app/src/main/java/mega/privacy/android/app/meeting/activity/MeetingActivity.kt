package mega.privacy.android.app.meeting.activity

import android.content.*
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_meeting.*
import kotlinx.android.synthetic.main.meeting_component_onofffab.*
import kotlinx.android.synthetic.main.meeting_on_boarding_fragment.*
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.databinding.ActivityMeetingBinding
import mega.privacy.android.app.lollipop.megachat.AppRTCAudioManager
import mega.privacy.android.app.meeting.AnimationTool.fadeInOut
import mega.privacy.android.app.meeting.BottomFloatingPanelListener
import mega.privacy.android.app.meeting.BottomFloatingPanelViewHolder
import mega.privacy.android.app.meeting.ParticipantRepository
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.fragments.*
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.IncomingCallNotification
import mega.privacy.android.app.utils.LogUtil
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRequestListenerInterface

@AndroidEntryPoint
class MeetingActivity : BaseActivity(), BottomFloatingPanelListener,
    MegaChatRequestListenerInterface {

    companion object {
        const val MEETING_ACTION_JOIN = "join_meeting"
        const val MEETING_ACTION_CREATE = "create_meeting"
        const val MEETING_ACTION_GUEST = "join_meeting_as_guest"
        const val MEETING_ACTION_IN = "in_meeting"

        const val MEETING_NAME = "meeting_name"
        const val MEETING_LINK = "meeting_link"
    }

    private lateinit var binding: ActivityMeetingBinding
    lateinit var bottomFloatingPanelViewHolder: BottomFloatingPanelViewHolder

    private var isGuest = false
    private var isModerator = false

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

        val meetingAction = intent.action

        initReceiver()
        initActionBar(meetingAction)
        initNavigation(meetingAction)
        initFloatingPanel(meetingAction)
    }

    private fun initFloatingPanel(meetAction: String?) {
        // Get the value from meet action
        isGuest = meetAction == MEETING_ACTION_GUEST
        isModerator = meetAction == MEETING_ACTION_CREATE

        bottomFloatingPanelViewHolder =
            BottomFloatingPanelViewHolder(binding, this, isGuest, isModerator).apply {
                // Create a repository get the participants
                setParticipants(ParticipantRepository().getTestParticipants(this@MeetingActivity))
                onHeadphoneConnected(wiredHeadset = false, bluetooth = false)
            }
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
            bundle.putString(MEETING_NAME, intent.getStringExtra(MEETING_NAME))
        }

        when (meetAction) {
            MEETING_ACTION_CREATE -> navGraph.startDestination = R.id.createMeetingFragment
            MEETING_ACTION_JOIN -> navGraph.startDestination = R.id.joinMeetingFragment
            MEETING_ACTION_GUEST -> navGraph.startDestination = R.id.joinMeetingAsGuestFragment
            MEETING_ACTION_IN -> navGraph.startDestination = R.id.inMeetingFragment
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

    val chatId = 12345L

    /**
     * First, should determine the permission
     * Should notify in-meeting fragment update if the state change successful
     */
    override fun onChangeMicState(micOn: Boolean): Boolean {
        if (!haveAudioPermission()) {
            return false
        }

        if (micOn) {
            megaChatApi.enableAudio(chatId, this)
        } else {
            megaChatApi.disableAudio(chatId, this)
        }

        return true
    }

    override fun onChangeCamState(camOn: Boolean): Boolean {
        if (!haveVideoPermission()) {
            return false
        }

        if (camOn) {
            megaChatApi.enableVideo(chatId, this)
        } else {
            megaChatApi.disableVideo(chatId, this)
        }

        return true
    }

    override fun onChangeHoldState(isHold: Boolean): Boolean {
        return if (haveVideoPermission() && haveAudioPermission()) {
            megaChatApi.setCallOnHold(chatId, isHold, this)
            true
        } else {
            false
        }
    }

    /**
     * Speaker Button
     */
    override fun onChangeAudioDevice(device: AppRTCAudioManager.AudioDevice): Boolean =
        haveAudioPermission()

    /**
     * Pop up dialog
     */
    override fun onEndMeeting() {
        if (isModerator) {
            val endMeetingBottomSheetDialogFragment =
                EndMeetingBottomSheetDialogFragment.newInstance()
            endMeetingBottomSheetDialogFragment.show(
                supportFragmentManager,
                endMeetingBottomSheetDialogFragment.tag
            )
        } else {
            askConfirmationEndMeetingForUser()
        }
    }

    private fun askConfirmationEndMeetingForUser() {
        LogUtil.logDebug("askConfirmationEndMeeting")
        val dialogClickListener =
            DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        megaChatApi.hangChatCall(chatId, this)
                        finish()
                    }
                    DialogInterface.BUTTON_NEGATIVE -> {
                        dialog.dismiss()
                    }
                }
            }
        MaterialAlertDialogBuilder(this).apply {
            setTitle(getString(R.string.title_end_meeting))
            setPositiveButton(R.string.general_ok, dialogClickListener)
            setNegativeButton(R.string.general_cancel, dialogClickListener)
            show()
        }
    }


    override fun onShareLink() {
        Toast.makeText(this, "onShareLink", Toast.LENGTH_SHORT).show()
    }

    /**
     * Open invite page
     */
    override fun onInviteParticipants() {
        Toast.makeText(this, "onInviteParticipants", Toast.LENGTH_SHORT).show()
    }

    override fun onParticipantOption(participant: Participant) {
        Toast.makeText(this, "onParticipantOption ${participant.name}", Toast.LENGTH_SHORT).show()
        val participantBottomSheet =
            MeetingParticipantBottomSheetDialogFragment.newInstance(
                isGuest,
                isModerator,
                participant
            )
        participantBottomSheet.show(supportFragmentManager, participantBottomSheet.tag)
    }


    /**
     * Can use the permission same with `CreateMeetingFragment`
     */
    private fun haveAudioPermission(): Boolean {
        return true
    }

    /**
     * Can use the permission same with `CreateMeetingFragment`
     */
    private fun haveVideoPermission(): Boolean {
        return true
    }

    fun setBottomFloatingPanelViewHolder(visible: Boolean) {
        bottom_floating_panel.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun bottomFloatingPanelInOut() {
        bottom_floating_panel.fadeInOut()
    }

    fun collpaseFloatingPanel() {
        bottomFloatingPanelViewHolder.collapse()
    }

    fun hideActionBar() {
        binding.toolbar.visibility = View.GONE
    }

    override fun onRequestStart(api: MegaChatApiJava?, request: MegaChatRequest?) {
        LogUtil.logDebug("Type: " + request?.type)
    }

    override fun onRequestUpdate(api: MegaChatApiJava?, request: MegaChatRequest?) {
        LogUtil.logDebug("Type: " + request?.type)
    }

    override fun onRequestFinish(
        api: MegaChatApiJava?,
        request: MegaChatRequest?,
        e: MegaChatError?
    ) {
        LogUtil.logDebug("Type: " + request?.type)
    }

    override fun onRequestTemporaryError(
        api: MegaChatApiJava?,
        request: MegaChatRequest?,
        e: MegaChatError?
    ) {
        LogUtil.logDebug("Type: " + request?.type)
    }
}
