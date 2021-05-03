package mega.privacy.android.app.meeting.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBar
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.databinding.ActivityAssignModeratorBinding
import mega.privacy.android.app.meeting.TestTool
import mega.privacy.android.app.meeting.adapter.AssignParticipantsAdapter
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.adapter.SelectedParticipantsAdapter
import org.jetbrains.anko.toast

/**
 * AssignModerator page allow moderator assign other users moderator when they are leaving the meeting
 *
 */
class AssignModeratorActivity : BaseActivity() {
    private lateinit var binding: ActivityAssignModeratorBinding

    private var selectedParticipants: MutableList<Participant> = mutableListOf()
    private var participants: MutableList<Participant> = mutableListOf()

    private lateinit var participantsAdapter: AssignParticipantsAdapter
    private lateinit var selectedParticipantsAdapter: SelectedParticipantsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAssignModeratorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        initRecyclerview()

        binding.btCancel.setOnClickListener { cancel() }
        binding.btOk.setOnClickListener { makeModerators() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home -> finish()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun initRecyclerview() {
        participantsAdapter = AssignParticipantsAdapter(selectCallback)
        selectedParticipantsAdapter = SelectedParticipantsAdapter(deleteCallback)

        binding.participantList.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = null
            clipToPadding = false
            adapter = participantsAdapter
            addItemDecoration(SimpleDividerItemDecoration(context))
        }

        binding.selectedParticipantList.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            itemAnimator = null
            clipToPadding = false
            adapter = selectedParticipantsAdapter
        }

        participantsAdapter.submitList(participants.toList())
    }

    private val selectCallback = fun(position: Int) {
        updateParticipantList(position)

        val participant = participants[position]
        selectedParticipants.add(participant)
        updateSelectedParticipant()
    }

    private fun updateSelectedParticipant() {
        if (selectedParticipants.size > 0) {
            supportActionBar?.subtitle =
                resources.getString(R.string.selected_items, selectedParticipants.size)
            binding.moderatorAddsContainer.isVisible = true
        } else {
            binding.moderatorAddsContainer.isVisible = false
            supportActionBar?.subtitle =
                resources.getString(R.string.pick_new_moderator_message)
        }
        selectedParticipantsAdapter.submitList(selectedParticipants.toList())
    }

    private val deleteCallback = fun(participant: Participant) {
        val position = participants.indexOf(participant)

        selectedParticipants.remove(participant)
        updateSelectedParticipant()
        updateParticipantList(position)
    }

    private fun updateParticipantList(position: Int) {
        val participant = participants[position]
        participant.isSelected = !participant.isSelected
        participants[position] = participant

        participantsAdapter.notifyItemChanged(position)
    }

    private fun makeModerators() {
        toast("makeModerators").show()
        finish()
        // Get the list and assign the user in the list to moderator

    }

    fun cancel() {
        finish()
    }
}