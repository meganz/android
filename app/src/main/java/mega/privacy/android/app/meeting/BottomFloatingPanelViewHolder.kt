package mega.privacy.android.app.meeting

import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import mega.privacy.android.app.R
import mega.privacy.android.app.components.OnOffFab
import mega.privacy.android.app.databinding.ActivityMeetingBinding
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.adapter.ParticipantsAdapter
import mega.privacy.android.app.utils.RunOnUIThreadUtils.post
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.noChangeRecyclerViewItemAnimator

class BottomFloatingPanelViewHolder(
    private val binding: ActivityMeetingBinding,
    private val listener: BottomFloatingPanelListener,
    private var isGuest: Boolean,
    private var isModerator: Boolean
) {
    private val context = binding.root.context

    private val bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomFloatingPanel.root)
    private val propertyUpdaters = ArrayList<(Float) -> Unit>()
    private var bottomFloatingPanelExpanded = false

    private val participantsAdapter = ParticipantsAdapter(listener)

    init {
        setupBottomSheet()
        listenButtons()
        setupRecyclerView()

        if (isGuest) {
            binding.bottomFloatingPanel.shareLink.isVisible = false
            binding.bottomFloatingPanel.invite.isVisible = false
        } else {
            binding.bottomFloatingPanel.guestShareLink.isVisible = false
        }

        binding.bottomFloatingPanel.fabEnd.setImageResource(if (isModerator) R.drawable.ic_end_call else R.drawable.ic_remove)

        post {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            bottomFloatingPanelExpanded = true
            onBottomFloatingPanelSlide(1F)
        }
    }

    fun setParticipants(participants: List<Participant>) {
        participantsAdapter.submitList(participants)

        binding.bottomFloatingPanel.participantsNum.text = getString(
            R.string.participants_number, participants.size
        )
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                bottomFloatingPanelExpanded = newState == BottomSheetBehavior.STATE_EXPANDED
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                onBottomFloatingPanelSlide(slideOffset)
            }
        })

        propertyUpdaters.add(
            propertyUpdater(
                binding.bottomFloatingPanel.backgroundMask,
                BOTTOM_PANEL_MIN_ALPHA, 1F
            ) { view, value -> view.alpha = value })

        val indicatorColorStart = 0x4F
        val indicatorColorEnd = 0xBD
        propertyUpdaters.add(
            propertyUpdater(
                binding.bottomFloatingPanel.indicator, indicatorColorStart, indicatorColorEnd
            ) { view, value ->
                view.backgroundTintList = ColorStateList.valueOf(composeColor(value))
            })

        setupFabUpdater()
        setupFabLabelUpdater()
    }

    private fun listenButtons() {
        binding.bottomFloatingPanel.fabMic.setOnOffCallback {
            updateBottomFloatingPanelIfNeeded()

            listener.onChangeMicState(binding.bottomFloatingPanel.fabMic.isOn)
        }
        binding.bottomFloatingPanel.fabCam.setOnOffCallback {
            updateBottomFloatingPanelIfNeeded()

            listener.onChangeCamState(binding.bottomFloatingPanel.fabCam.isOn)
        }
        binding.bottomFloatingPanel.fabHold.setOnOffCallback {
            updateBottomFloatingPanelIfNeeded()

            listener.onChangeHoldState(binding.bottomFloatingPanel.fabHold.isOn)
        }
        binding.bottomFloatingPanel.fabSpeaker.setOnOffCallback {
            updateBottomFloatingPanelIfNeeded()

            listener.onChangeSpeakerState(binding.bottomFloatingPanel.fabSpeaker.isOn)
        }
        binding.bottomFloatingPanel.fabEnd.setOnClickListener {
            listener.onEndMeeting()
        }

        binding.bottomFloatingPanel.shareLink.setOnClickListener {
            listener.onShareLink()
        }
        binding.bottomFloatingPanel.guestShareLink.setOnClickListener {
            listener.onShareLink()
        }
        binding.bottomFloatingPanel.invite.setOnClickListener {
            listener.onInviteParticipants()
        }
    }

    private fun setupRecyclerView() {
        val rv = binding.bottomFloatingPanel.participants
        rv.layoutManager = LinearLayoutManager(context)
        rv.itemAnimator = noChangeRecyclerViewItemAnimator()
        rv.clipToPadding = false
        rv.setHasFixedSize(true)

        rv.adapter = participantsAdapter
        //participantsAdapter.setHasStableIds(true)
    }

    private fun updateBottomFloatingPanelIfNeeded() {
        if (bottomFloatingPanelExpanded) {
            onBottomFloatingPanelSlide(1F)
        }
    }

    private fun onBottomFloatingPanelSlide(slideOffset: Float) {
        for (updater in propertyUpdaters) {
            updater(slideOffset)
        }
    }

    private fun setupFabLabelUpdater() {
        setupFabLabelUpdater(binding.bottomFloatingPanel.fabMicLabel)
        setupFabLabelUpdater(binding.bottomFloatingPanel.fabCamLabel)
        setupFabLabelUpdater(binding.bottomFloatingPanel.fabHoldLabel)
        setupFabLabelUpdater(binding.bottomFloatingPanel.fabSpeakerLabel)
        setupFabLabelUpdater(binding.bottomFloatingPanel.fabEndLabel)
    }

    private fun setupFabLabelUpdater(label: TextView) {
        if (Util.isDarkMode(context)) {
            label.setTextColor(ContextCompat.getColor(context, R.color.white_alpha_087))
        } else {
            val fabLabelColorStart = 0xFF
            val fabLabelColorEnd = 0x21

            propertyUpdaters.add(
                propertyUpdater(
                    label, fabLabelColorStart, fabLabelColorEnd
                ) { view, value -> view.setTextColor(composeColor(value)) })
        }
    }

    private fun setupFabUpdater() {
        setupFabBackgroundTintUpdater(binding.bottomFloatingPanel.fabMic)
        setupFabBackgroundTintUpdater(binding.bottomFloatingPanel.fabCam)
        setupFabBackgroundTintUpdater(binding.bottomFloatingPanel.fabHold)
        setupFabBackgroundTintUpdater(binding.bottomFloatingPanel.fabSpeaker)
    }

    private fun setupFabBackgroundTintUpdater(fab: OnOffFab) {
        if (Util.isDarkMode(context)) {
            fab.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white_alpha_054))
        } else {
            val fabBackgroundTintStart = 0x4F
            val fabBackgroundTintEnd = 0x75

            propertyUpdaters.add(
                propertyUpdater(
                    fab, fabBackgroundTintStart, fabBackgroundTintEnd
                ) { view, value ->
                    if (view.isOn) {
                        view.backgroundTintList = ColorStateList.valueOf(composeColor(value))
                    }
                })
        }
    }

    private fun <V : View> propertyUpdater(
        view: V,
        startP: Int,
        endP: Int,
        update: (view: V, value: Int) -> Unit
    ): (Float) -> Unit {
        return {
            update(view, (startP + (endP - startP) * it).toInt())
        }
    }

    private fun <V : View> propertyUpdater(
        view: V,
        startP: Float,
        endP: Float,
        update: (view: V, value: Float) -> Unit
    ): (Float) -> Unit {
        return {
            update(view, startP + (endP - startP) * it)
        }
    }

    private fun composeColor(component: Int): Int {
        return ((component.shl(16) or component.shl(8) or component).toLong() or 0xFF000000).toInt()
    }

    companion object {
        private const val BOTTOM_PANEL_MIN_ALPHA = 0.66F
    }
}
