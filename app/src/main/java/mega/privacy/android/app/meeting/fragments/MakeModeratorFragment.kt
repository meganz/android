package mega.privacy.android.app.meeting.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.components.twemoji.EmojiTextView
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.databinding.MakeModeratorFragmentBinding
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.adapter.AssignParticipantsAdapter
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.adapter.SelectedParticipantsAdapter
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaChatSession
import org.jetbrains.anko.displayMetrics
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MakeModeratorFragment : MeetingBaseFragment() {
    private lateinit var binding: MakeModeratorFragmentBinding
    private var selectedParticipants: MutableList<Participant> = mutableListOf()
    private var participants: MutableList<Participant> = mutableListOf()

    private lateinit var selectedParticipantsAdapter: SelectedParticipantsAdapter
    private lateinit var participantsAdapter: AssignParticipantsAdapter

    private lateinit var itemDecoration: PositionDividerItemDecoration
    private var chatId: Long? = MEGACHAT_INVALID_HANDLE

    // Views
    lateinit var toolbar: MaterialToolbar
    lateinit var toolbarTitle: EmojiTextView
    lateinit var toolbarSubtitle: TextView
    val inMeetingViewModel: InMeetingViewModel by activityViewModels()

    @Inject
    lateinit var passcodeManagement: PasscodeManagement

    private val callStatusObserver = Observer<MegaChatCall> {
        if (inMeetingViewModel.isSameCall(it.callId)) {
            when (it.status) {
                MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION,
                MegaChatCall.CALL_STATUS_DESTROYED,
                -> {
                    disableLocalCamera()
                    finishActivity()
                }
            }
        }
    }

    private val sessionStatusObserver =
        Observer<Pair<MegaChatCall?, MegaChatSession>> { callAndSession ->
            val call = callAndSession.first ?: return@Observer

            if (inMeetingViewModel.isSameCall(call.callId) && !inMeetingViewModel.isOneToOneCall()) {
                when (callAndSession.second.status) {
                    MegaChatSession.SESSION_STATUS_IN_PROGRESS -> {
                        Timber.d("Session in progress, clientID = ${callAndSession.second.clientid}")
                        inMeetingViewModel.addParticipant(callAndSession.second)
                    }
                    MegaChatSession.SESSION_STATUS_DESTROYED -> {
                        Timber.d("Session destroyed, clientID = ${callAndSession.second.clientid}")
                        inMeetingViewModel.removeParticipant(callAndSession.second)
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = MakeModeratorFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("In make moderator fragment")
        chatId = arguments?.getLong(MeetingActivity.MEETING_CHAT_ID, MEGACHAT_INVALID_HANDLE)
        if (chatId == MEGACHAT_INVALID_HANDLE) {
            sharedModel.currentChatId.value?.let {
                chatId = it
            }
        }

        setupView()
        initLiveEvent()

        inMeetingViewModel.participants.observe(viewLifecycleOwner) { participants ->
            participants?.let {
                update(participants.filter { inMeetingViewModel.isStandardUser(it.peerId) && !it.isGuest }
                    .map { it.copy() }
                    .toMutableList())
            }
        }
    }

    /**
     * Method for initialising UI elements
     */
    private fun initLiveEvent() {
        //Sessions Level
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EventConstants.EVENT_SESSION_STATUS_CHANGE)
            .observe(this, sessionStatusObserver as Observer<Any>)

        LiveEventBus.get(EventConstants.EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
            .observe(this, callStatusObserver)
    }

    /**
     * Method for initialising UI elements
     */
    private fun setupView() {
        Timber.d("Update toolbar elements")
        binding.btCancel.setOnClickListener { cancel() }
        binding.btOk.setOnClickListener { makeModerators() }

        val root = meetingActivity.binding.root
        root.apply {
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white_black))
        }
        toolbar = meetingActivity.binding.toolbar

        toolbar.apply {
            background = null
            setBackgroundColor(Color.TRANSPARENT)
        }

        toolbarTitle = meetingActivity.binding.titleToolbar
        toolbarTitle.apply {
            text = StringResourcesUtils.getString(R.string.assign_moderator)
                .uppercase(Locale.getDefault())

            setTextColor(ContextCompat.getColor(requireContext(), R.color.black_white))
        }

        toolbarSubtitle = meetingActivity.binding.subtitleToolbar

        toolbarSubtitle.apply {
            text = StringResourcesUtils.getString(R.string.pick_new_moderator_message)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_300))
        }

        meetingActivity.setSupportActionBar(toolbar)
        val actionBar = meetingActivity.supportActionBar ?: return
        actionBar.apply {
            title = null
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(
                ColorUtils.tintIcon(
                    requireContext(),
                    R.drawable.ic_close_white,
                    ContextCompat.getColor(requireContext(), R.color.black_white)
                )
            )
        }

        setHasOptionsMenu(true)
        initRecyclerview()
    }

    private fun initRecyclerview() {
        participantsAdapter = AssignParticipantsAdapter(sharedModel, selectCallback)
        selectedParticipantsAdapter =
            SelectedParticipantsAdapter(sharedModel, deleteCallback)

        itemDecoration =
            PositionDividerItemDecoration(context, MegaApplication.getInstance().displayMetrics)
        itemDecoration.setDrawAllDividers(true)

        binding.participantList.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = Util.noChangeRecyclerViewItemAnimator()
            clipToPadding = false
            adapter = participantsAdapter
            addItemDecoration(itemDecoration)
        }

        binding.selectedParticipantList.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            itemAnimator = null
            clipToPadding = false
            adapter = selectedParticipantsAdapter
        }

        participantsAdapter.submitList(participants.toList())
    }

    /**
     * The call back function when users select participants, will update the selected list and participant list
     */
    private val selectCallback = fun(position: Int) {
        updateParticipantList(position)

        val participant = participants[position]

        if (participant.isChosenForAssign) {
            selectedParticipants.add(participant)
        } else {
            selectedParticipants.remove(participant)
        }

        updateSelectedParticipant()
    }

    /**
     * The call back function when users delete participants, will update the selected list and participant list
     */
    private val deleteCallback = fun(participant: Participant) {
        val position = participants.indexOf(participant)

        selectedParticipants.remove(participant)
        updateSelectedParticipant()
        updateParticipantList(position)
    }

    /**
     * Update the participant list when user choose or delete the participant
     */
    private fun updateParticipantList(position: Int) {
        val participant = participants[position]
        participant.isChosenForAssign = !participant.isChosenForAssign
        participants[position] = participant

        participantsAdapter.notifyItemChanged(position)
    }

    /**
     * Update the selected participant list when user choose or delete the participant
     */
    private fun updateSelectedParticipant() {
        if (selectedParticipants.size > 0) {
            toolbarSubtitle.text =
                StringResourcesUtils.getString(R.string.selected_items, selectedParticipants.size)
            binding.btOk.isEnabled = true
        } else {
            toolbarSubtitle.text =
                StringResourcesUtils.getString(R.string.pick_new_moderator_message)
            binding.btOk.isEnabled = false
        }

        binding.rlSelectedParticipantList.isVisible = selectedParticipants.size > 0

        selectedParticipantsAdapter.submitList(selectedParticipants.toMutableList())
    }

    /**
     * Update the participant list and selected list when someone leave this meeting
     *
     * @param participantsList new participant list
     */
    fun update(participantsList: MutableList<Participant>) {
        // Get the current selected id
        participants = participantsList
        val oldSelect = selectedParticipants.map { it.peerId }
        participants.forEach {
            if (oldSelect.contains(it.peerId)) {
                it.isChosenForAssign = true
            }
        }

        participantsAdapter.submitList(participants.toMutableList())

        val newSelect = participants.filter { it.isChosenForAssign }.map { it.peerId }
        selectedParticipants.run {
            removeIf {
                !newSelect.contains(it.peerId)
            }
            forEach {
                it.isChosenForAssign = true
            }
        }

        selectedParticipantsAdapter.submitList(selectedParticipants.toMutableList())
    }

    /**
     * Make selected participants to moderator
     */
    private fun makeModerators() {
        // Get the list and assign the user in the list to moderator
        selectedParticipants.forEach {
            sharedModel.changeParticipantPermissions(it.peerId, MegaChatRoom.PRIV_MODERATOR)
        }

        disableLocalCamera()
        Timber.d("Leave meeting")
        inMeetingViewModel.checkClickEndButton()
    }

    /**
     * Method to control how the meeting activity should finish correctly
     */
    fun finishActivity() {
        sharedModel.clickEndCall()
    }

    /**
     * Method to disable the local camera
     */
    fun disableLocalCamera() {
        if (inMeetingViewModel.isLocalCameraOn()) {
            Timber.d("Disable local camera")
            sharedModel.clickCamera(false)
        }
    }

    /**
     * Cancel this action and close this page
     */
    fun cancel() {
        val action = MakeModeratorFragmentDirections.actionGlobalInMeeting(
            action = MeetingActivity.MEETING_ACTION_IN,
            chatId = inMeetingViewModel.getChatId()
        )
        findNavController().navigate(action)
    }
}