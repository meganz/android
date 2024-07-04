package mega.privacy.android.app.meeting.fragments

import android.graphics.Color
import android.os.Bundle
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
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
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.meeting.ChatSessionStatus
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatRoom
import org.jetbrains.anko.displayMetrics
import timber.log.Timber
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
            sharedModel.state.value.chatId.let {
                chatId = it
            }
        }

        setupView()
        collectFlows()

        inMeetingViewModel.participants.observe(viewLifecycleOwner) { participants ->
            participants?.let {
                update(participants.filter { inMeetingViewModel.isStandardUser(it.peerId) && !it.isGuest }
                    .map { it.copy() }
                    .toMutableList())
            }
        }
    }

    private fun collectFlows() {
        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.changesInStatusInSession }
            .distinctUntilChanged()) {
            it?.apply {
                if (!inMeetingViewModel.isOneToOneCall()) {
                    when (status) {
                        ChatSessionStatus.Invalid -> {}
                        ChatSessionStatus.Progress -> {
                            Timber.d("Session in progress, clientID = $clientId")
                            inMeetingViewModel.addParticipant(it)
                        }

                        ChatSessionStatus.Destroyed -> {
                            Timber.d("Session destroyed, clientID = $clientId")
                            inMeetingViewModel.removeParticipant(it)
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.call?.status }
            .distinctUntilChanged()) {
            it?.let { callStatus ->
                if (callStatus == ChatCallStatus.TerminatingUserParticipation || callStatus == ChatCallStatus.Destroyed) {
                    disableLocalCamera()
                    finishActivity()
                }
            }
        }
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
            text = getString(R.string.assign_moderator)

            setTextColor(ContextCompat.getColor(requireContext(), R.color.black_white))
        }

        toolbarSubtitle = meetingActivity.binding.subtitleToolbar

        toolbarSubtitle.apply {
            text = getString(R.string.pick_new_moderator_message)
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
                getString(R.string.selected_items, selectedParticipants.size)
            binding.btOk.isEnabled = true
        } else {
            toolbarSubtitle.text =
                getString(R.string.pick_new_moderator_message)
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
        inMeetingViewModel.hangCurrentCall()
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
        if (inMeetingViewModel.state.value.hasLocalVideo) {
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
