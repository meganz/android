package mega.privacy.android.app.meeting.fragments

import android.animation.ArgbEvaluator
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import mega.privacy.android.app.R
import mega.privacy.android.app.components.OnOffFab
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.databinding.InMeetingFragmentBinding
import mega.privacy.android.app.main.megachat.AppRTCAudioManager
import mega.privacy.android.app.meeting.LockableBottomSheetBehavior
import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.listeners.BottomFloatingPanelListener
import mega.privacy.android.app.presentation.meeting.WaitingRoomManagementViewModel
import mega.privacy.android.app.presentation.meeting.view.ParticipantsBottomPanelView
import mega.privacy.android.app.utils.Util
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.meeting.ParticipantsSection
import timber.log.Timber

/**
 * Bottom Panel view holder package the view and logic code of floating panel
 *
 * @property inMeetingViewModel InMeetingViewModel, get some values and do some logic actions
 * @property binding  InMeetingFragmentBinding, get views from this binding
 * @property listener listen to the actions of all buttons
 */
class BottomFloatingPanelViewHolder(
    private val inMeetingViewModel: InMeetingViewModel,
    private val meetingViewModel: MeetingActivityViewModel,
    private val waitingRoomManagementViewModel: WaitingRoomManagementViewModel,
    private val binding: InMeetingFragmentBinding,
    private val listener: BottomFloatingPanelListener,
    private val displayMetrics: DisplayMetrics,
) {
    private val context = binding.root.context
    private val floatingPanelView = binding.bottomFloatingPanel

    private val bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomFloatingPanel.root)
    val propertyUpdaters = ArrayList<(Float) -> Unit>()

    private var bottomFloatingPanelExpanded = false
    private var expandedTop = 0
    private var collapsedTop = 0

    private var popWindow: PopupWindow? = null
    private lateinit var itemDecoration: PositionDividerItemDecoration

    /**
     * Save the Mic & Cam state, for revering state when hold state changed
     */
    private var savedMicState: Boolean = false
    private var savedCamState: Boolean = false
    private var savedSpeakerState: AppRTCAudioManager.AudioDevice =
        AppRTCAudioManager.AudioDevice.NONE

    private var currentHeight = 0

    /**
     * Observer the change of layout
     */
    val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        initTipsAndRatio()
    }

    init {
        initButtonsState()
        setupBottomSheet()
        listenButtons()
        setupRecyclerView()
        updatePanel()
        initTipsAndRatio()

        binding.root.addOnLayoutChangeListener(layoutListener)
    }

    /**
     * Remove listener and dismiss the pop window when activity is destroyed
     */
    fun onDestroy() {
        binding.root.removeOnLayoutChangeListener(layoutListener)
        popWindow?.dismiss()
    }

    /**
     * Init the tips window & Calculate the ratio for bottom sheet behavior
     */
    private fun initTipsAndRatio() {
        floatingPanelView.backgroundMask.post {
            if (binding.root.measuredHeight == currentHeight)
                return@post

            currentHeight = binding.root.measuredHeight
            if (inMeetingViewModel.shouldShowTips()) {
                initPopWindow(floatingPanelView.backgroundMask)
            }

            val peekHeight =
                context.resources.getDimensionPixelSize(R.dimen.meeting_bottom_floating_panel_peek_height)

            var ratio = peekHeight.toFloat() / binding.root.measuredHeight
            if ((ratio <= 0) || (ratio >= 1)) {
                ratio = 0.001f
            }
            bottomSheetBehavior.halfExpandedRatio = ratio
        }
    }

    /**
     * Determine if the tips is showing
     */
    fun isPopWindowShowing(): Boolean = popWindow != null && popWindow?.isShowing == true

    /**
     * Dismiss the tips window
     */
    fun dismissPopWindow() {
        popWindow?.dismiss()
        inMeetingViewModel.updateShowTips()
    }

    /**
     * Init the tips window
     *
     * @param anchor the anchor view, the tips widow should show base on it's location
     */
    private fun initPopWindow(anchor: View) {
        if (inMeetingViewModel.state.value.isOneToOneCall) {
            return
        }

        val view: View = LayoutInflater.from(context)
            .inflate(R.layout.view_tip_meeting_bottom_panel, null, false)
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

        if (isPopWindowShowing()) {
            popWindow?.dismiss()
        }

        popWindow = PopupWindow(
            view,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isFocusable = false
            isOutsideTouchable = false
            setBackgroundDrawable(null)
        }

        val confirm = view.findViewById<Button>(R.id.bt_ok)
        confirm.setOnClickListener {
            dismissPopWindow()
        }

        val location = intArrayOf(0, 0)
        anchor.getLocationInWindow(location)

        popWindow?.showAtLocation(
            anchor,
            Gravity.NO_GRAVITY,
            (location[0] + anchor.width / 2) - view.measuredWidth / 2,
            location[1] - view.measuredHeight
        )
    }

    /**
     * Expanded bottom sheet when the meeting is group chat
     * If the meeting is one-to-one chat, just show the control button, and would not let user drag the bottom panel
     *
     * @param shouldExpand determine if should expand panel when update
     */
    fun updatePanel(shouldExpand: Boolean = true) {
        if (shouldExpand) {
            collapse()
        }

        floatingPanelView.indicator.isVisible = !inMeetingViewModel.state.value.isOneToOneCall
    }


    /**
     * Get the drawable for background for dark mode
     *
     * @param background the color for background
     * @return the final drawable for the background
     */
    fun getCheckedDrawable(background: Int): Drawable {
        val roundRect = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = context.resources.getDimension(R.dimen.elevation_upgrade_low)
            setColor(background)
        }

        val round2 = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(ContextCompat.getColor(context, R.color.white_alpha_007))
            cornerRadius = context.resources.getDimension(R.dimen.elevation_upgrade_low)
        }

        val insetLayer2 = InsetDrawable(round2, 0, 0, 0, 0)

        return LayerDrawable(arrayOf(roundRect, insetLayer2))
    }

    /**
     * Init the state for the Mic, Cam and End button on button bar
     */
    private fun initButtonsState() {
        disableEnableButtons(
            inMeetingViewModel.isCallEstablished(),
            inMeetingViewModel.isCallOnHold()
        )
        floatingPanelView.fabMic.isOn = savedMicState
        floatingPanelView.fabCam.isOn = savedCamState
        updateSpeakerIcon(savedSpeakerState)
    }

    /**
     * Init Participants and update the list, and update the text showing participants size
     *
     * @param participants newest participant list
     * @param myOwnParticipant me as a participant
     */
    fun setParticipantsPanel(
        participants: MutableList<Participant>,
        myOwnParticipant: Participant,
    ) {
        updateParticipants(participants, myOwnParticipant)
    }

    /**
     * Update participants list
     *
     * @param participants newest participant list
     * @param myOwnParticipant me as a participant
     */
    fun updateParticipants(participants: MutableList<Participant>, myOwnParticipant: Participant) {
        participants.add(myOwnParticipant)
        meetingViewModel.updateChatParticipantsInCall(participants)
    }

    /**
     * Set the listener for bottom sheet behavior and property list
     */
    private fun setupBottomSheet() {
        if (bottomSheetBehavior is LockableBottomSheetBehavior<*>) {
            (bottomSheetBehavior as LockableBottomSheetBehavior<*>).setLocked(
                inMeetingViewModel.state.value.isOneToOneCall
            )
        }

        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                bottomFloatingPanelExpanded = newState == BottomSheetBehavior.STATE_EXPANDED
                meetingViewModel.setBottomPanelExpanded(bottomFloatingPanelExpanded)

                if (newState == BottomSheetBehavior.STATE_DRAGGING && inMeetingViewModel.state.value.isOneToOneCall) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }

                when {
                    newState == BottomSheetBehavior.STATE_EXPANDED && expandedTop == 0 -> {
                        expandedTop = binding.bottomFloatingPanel.root.top
                    }

                    newState == BottomSheetBehavior.STATE_COLLAPSED && collapsedTop == 0 -> {
                        collapsedTop = binding.bottomFloatingPanel.root.top
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (!inMeetingViewModel.state.value.isOneToOneCall) {
                    onBottomFloatingPanelSlide(slideOffset)
                    if (slideOffset > 0.1f) {
                        dismissPopWindow()
                    }
                }
            }
        })

        initUpdaters()
    }

    /**
     * Method that disables or enables buttons depending on whether the call is connected or not
     *
     * @param isCallEstablished True, if the call is connected. False, otherwise
     * @param isHold True, if the call is on hold. False, otherwise
     */
    fun disableEnableButtons(isCallEstablished: Boolean, isHold: Boolean) {
        val shouldBeEnable = !(!isCallEstablished || isHold)
        floatingPanelView.apply {
            fabMic.enable = shouldBeEnable
            fabCam.enable = shouldBeEnable
            fabHold.enable = isCallEstablished
            fabHold.isOn = !isHold
            fabSpeaker.enable = shouldBeEnable
        }
    }

    /**
     * Init listener for all the button
     */
    private fun listenButtons() {
        floatingPanelView.apply {
            fabMic.setOnOffCallback {
                savedMicState = it
                listener.onChangeMicState(binding.bottomFloatingPanel.fabMic.isOn)
            }

            fabMic.setOnChangeCallback {
                updateBottomFloatingPanelIfNeeded()
            }

            fabCam.setOnOffCallback {
                savedCamState = it
                listener.onChangeCamState(binding.bottomFloatingPanel.fabCam.isOn)
            }

            fabCam.setOnChangeCallback {
                updateBottomFloatingPanelIfNeeded()
            }

            fabSpeaker.setOnClickListener {
                listener.onChangeSpeakerState()
            }

            fabSpeaker.setOnChangeCallback {
                updateBottomFloatingPanelIfNeeded()
            }

            fabHold.setOnOffCallback {
                listener.onChangeHoldState(binding.bottomFloatingPanel.fabHold.isOn)
            }

            fabHold.setOnChangeCallback {
                updateBottomFloatingPanelIfNeeded()
            }

            fabEnd.setOnClickListener {
                listener.onEndMeeting()
            }
        }
    }

    /**
     * Init recyclerview
     */
    private fun setupRecyclerView() {
        itemDecoration = PositionDividerItemDecoration(context, displayMetrics)
        itemDecoration.setDrawAllDividers(true)
    }

    /**
     * Update the style of views those should be updated if bottom panel is expanded after some click actions
     */
    private fun updateBottomFloatingPanelIfNeeded() {
        if (bottomFloatingPanelExpanded) {
            onBottomFloatingPanelSlide(BOTTOM_PANEL_EXPAND_OFFSET)
        }
    }

    /**
     * Update all the views base on the slide offset of panel
     *
     * @param slideOffset panel offset distance
     */
    private fun onBottomFloatingPanelSlide(slideOffset: Float) {
        val ratio = if (slideOffset < BOTTOM_PANEL_PROPERTY_UPDATER_OFFSET_THRESHOLD) {
            slideOffset / BOTTOM_PANEL_PROPERTY_UPDATER_OFFSET_THRESHOLD
        } else {
            BOTTOM_PANEL_EXPAND_OFFSET
        }

        for (updater in propertyUpdaters) {
            updater(ratio)
        }
    }

    /**
     * Init views those should update their background base on the slide offset of panel
     */
    private fun initUpdaters() {
        propertyUpdaters.add(
            propertyUpdater(
                binding.bottomFloatingPanel.backgroundMask,
                BOTTOM_PANEL_MIN_ALPHA, BOTTOM_PANEL_EXPAND_OFFSET
            ) { view, value ->
                run {
                    val argbEvaluator = ArgbEvaluator()
                    val background = argbEvaluator.evaluate(
                        value,
                        ContextCompat.getColor(
                            context, R.color.grey_070_dark_grey_066
                        ), ContextCompat.getColor(
                            context, R.color.white_dark_grey
                        )
                    ) as Int

                    view.background =
                        if (Util.isDarkMode(context)) {
                            getCheckedDrawable(background)
                        } else {
                            val grad: GradientDrawable = view.background as GradientDrawable
                            grad.setColor(background)
                            grad
                        }
                }
            })

        propertyUpdaters.add(
            propertyUpdater(
                binding.bottomFloatingPanel.indicator, INDICATOR_COLOR_START, INDICATOR_COLOR_END
            ) { view, value ->
                view.backgroundTintList = ColorStateList.valueOf(composeColor(value))
            })

        binding.bottomFloatingPanel.participantsComposeView.apply {
            isVisible = true
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val meetingState by meetingViewModel.state.collectAsStateWithLifecycle()
                AndroidTheme(isDark = Util.isDarkMode(context)) {
                    ParticipantsBottomPanelView(
                        state = meetingState,
                        onWaitingRoomClick = {
                            meetingViewModel.updateParticipantsSection(
                                ParticipantsSection.WaitingRoomSection
                            )
                        },
                        onInCallClick = {
                            meetingViewModel.updateParticipantsSection(
                                ParticipantsSection.InCallSection
                            )
                        },
                        onNotInCallClick = {
                            meetingViewModel.updateParticipantsSection(
                                ParticipantsSection.NotInCallSection
                            )
                        },
                        onAdmitAllClick = { waitingRoomManagementViewModel.admitUsersClick() },
                        onSeeAllClick = {
                            meetingViewModel.onSeeAllClick()
                            if (meetingViewModel.state.value.participantsSection == ParticipantsSection.WaitingRoomSection) {
                                waitingRoomManagementViewModel.setShowParticipantsInWaitingRoomDialogConsumed()
                            }
                        },
                        onInviteParticipantsClick = { listener.onInviteParticipants() },
                        onShareMeetingLinkClick = {
                            meetingViewModel.queryMeetingLink(
                                shouldShareMeetingLink = true
                            )
                        },
                        onAllowAddParticipantsClick = {
                            meetingViewModel.allowAddParticipantsClick()
                        },
                        onAdmitParticipantClicked = {
                            waitingRoomManagementViewModel.admitUsersClick(
                                it
                            )
                        },
                        onParticipantMoreOptionsClicked = { chatParticipant ->
                            meetingViewModel.state.value.usersInCall.find { it.peerId == chatParticipant.handle }
                                ?.let {
                                    listener.onParticipantOption(it)
                                }
                        },
                        onDenyParticipantClicked = {
                            waitingRoomManagementViewModel.denyUsersClick(
                                it
                            )
                        })
                }
            }
        }

        setupFabUpdater()
        setupFabLabelUpdater()
    }

    /**
     * Add fab label views into the update list and set up the color of background
     */
    private fun setupFabLabelUpdater() {
        setupFabLabelUpdater(binding.bottomFloatingPanel.fabMicLabel)
        setupFabLabelUpdater(binding.bottomFloatingPanel.fabCamLabel)
        setupFabLabelUpdater(binding.bottomFloatingPanel.fabHoldLabel)
        setupFabLabelUpdater(binding.bottomFloatingPanel.fabSpeakerLabel)
        setupFabLabelUpdater(binding.bottomFloatingPanel.fabEndLabel)
    }

    /**
     * Set up the background color for updating when panel is sliding
     *
     * @param label the target textview
     */
    private fun setupFabLabelUpdater(label: TextView) {
        val isDarkMode = Util.isDarkMode(context)
        val fabLabelColorStart =
            if (isDarkMode) FAB_LABEL_COLOR_DARK_MODE else FAB_LABEL_COLOR_START_LIGHT_MODE
        val fabLabelColorEnd =
            if (isDarkMode) FAB_LABEL_COLOR_DARK_MODE else FAB_LABEL_COLOR_END_LIGHT_MODE

        propertyUpdaters.add(
            propertyUpdater(
                label, fabLabelColorStart, fabLabelColorEnd
            ) { view, value -> view.setTextColor(composeColor(value)) })
    }

    /**
     * Add fab icon views into the update list and set up the color of background
     */
    private fun setupFabUpdater() {
        setupFabBackgroundTintUpdater(binding.bottomFloatingPanel.fabMic)
        setupFabBackgroundTintUpdater(binding.bottomFloatingPanel.fabCam)
        setupFabBackgroundTintUpdater(binding.bottomFloatingPanel.fabHold)
        setupFabBackgroundTintUpdater(binding.bottomFloatingPanel.fabSpeaker)
    }

    /**
     * Set up the background tint color for updating when panel is sliding
     *
     * @param fab the target icon
     */
    private fun setupFabBackgroundTintUpdater(fab: OnOffFab) {
        propertyUpdaters.add(
            propertyUpdater(
                fab, FAB_TINT_COLOR_START, FAB_TINT_COLOR_END
            ) { view, value ->
                if (view.isOn || !view.enable) {
                    run {
                        val argbEvaluator = ArgbEvaluator()
                        val background = argbEvaluator.evaluate(
                            value,
                            ContextCompat.getColor(context, R.color.grey_032_white_054),
                            ContextCompat.getColor(context, R.color.grey_060_white_054)
                        ) as Int

                        view.backgroundTintList = ColorStateList.valueOf(background)
                    }
                }
            })
    }

    /**
     * Collapse the bottom sheet
     */
    fun collapse() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomFloatingPanelExpanded = false
        meetingViewModel.setBottomPanelExpanded(false)
        onBottomFloatingPanelSlide(BOTTOM_PANEL_COLLAPSE_OFFSET)
    }

    /**
     * Get current state of bottom panel
     */
    fun getState() = bottomSheetBehavior.state

    /**
     * Expand the bottom sheet
     */
    fun expand() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomFloatingPanelExpanded = true
        meetingViewModel.setBottomPanelExpanded(true)
        onBottomFloatingPanelSlide(BOTTOM_PANEL_EXPAND_OFFSET)
    }

    /**
     * Update the mic icon, also update the own item's mic icon
     *
     * @param micOn True, if the audio is on. False, if the audio is off
     */
    fun updateMicIcon(micOn: Boolean) {
        savedMicState = micOn
        floatingPanelView.fabMic.isOn = micOn
    }

    /**
     * Update the cam icon, also update the own item's cam icon
     *
     * @param camOn True, if the video is on. False, if the video is off
     */
    fun updateCamIcon(camOn: Boolean) {
        savedCamState = camOn
        floatingPanelView.fabCam.isOn = camOn
    }

    /**
     * Enabling or disabling the on hold button
     *
     * @param isEnabled True, if enabled. False, if disabled
     * @param isHold True, if it is an on hold button. False, if it is switch call button
     */
    fun enableHoldIcon(isEnabled: Boolean, isHold: Boolean) {
        disableEnableButtons(isEnabled, isHold)
    }

    /**
     * Method to control when to switch the button from on hold to switch call
     *
     * @param isAnotherCallOnHold True, if another call is in progress. False, if not
     */
    fun changeOnHoldIcon(isAnotherCallOnHold: Boolean) {
        if (isAnotherCallOnHold) {
            changeOnHoldIconDrawable(true)
            floatingPanelView.fabHold.isOn = true
        } else {
            floatingPanelView.fabHold.isOn = false
            changeOnHoldIconDrawable(false)
        }
    }

    /**
     * Method of changing the on hold button icon appropriately
     *
     * @param existsAnotherCallOnHold True, if another call is in progress. False, if not.
     */
    fun changeOnHoldIconDrawable(existsAnotherCallOnHold: Boolean) {
        floatingPanelView.fabHold.setOnIcon(
            if (existsAnotherCallOnHold) R.drawable.ic_call_swap
            else R.drawable.ic_transfers_pause
        )
    }

    /**
     * Method that updates the speaker icon according to the selected AudioDevice
     *
     * @param device Current device selected
     */
    fun updateSpeakerIcon(device: AppRTCAudioManager.AudioDevice) {
        Timber.d("Update speaker icon. Audio device is $device")
        when (device) {
            AppRTCAudioManager.AudioDevice.SPEAKER_PHONE -> {
                floatingPanelView.fabSpeaker.setOnIcon(R.drawable.ic_speaker_on)
                floatingPanelView.fabSpeaker.enable = true
                floatingPanelView.fabSpeaker.isOn = true
                floatingPanelView.fabSpeakerLabel.text = context.getString(R.string.general_speaker)
            }

            AppRTCAudioManager.AudioDevice.EARPIECE -> {
                floatingPanelView.fabSpeaker.setOnIcon(R.drawable.ic_speaker_off)
                floatingPanelView.fabSpeaker.enable = true
                floatingPanelView.fabSpeaker.isOn = false
                floatingPanelView.fabSpeakerLabel.text = context.getString(R.string.general_speaker)
            }

            AppRTCAudioManager.AudioDevice.WIRED_HEADSET,
            AppRTCAudioManager.AudioDevice.BLUETOOTH,
            -> {
                floatingPanelView.fabSpeaker.setOnIcon(R.drawable.ic_headphone)
                floatingPanelView.fabSpeaker.enable = true
                floatingPanelView.fabSpeaker.isOn = true
                floatingPanelView.fabSpeakerLabel.text =
                    context.getString(R.string.general_headphone)
            }

            else -> {
                floatingPanelView.fabSpeaker.setOnIcon(R.drawable.ic_speaker_on)
                floatingPanelView.fabSpeaker.enable = false
                floatingPanelView.fabSpeaker.isOn = true
                floatingPanelView.fabSpeakerLabel.text = context.getString(R.string.general_speaker)
            }
        }
    }

    /**
     * Change the panel's width for landscape and portrait screen
     *
     * @param orientation the current orientation
     * @param widthPixels the width of the screen
     */
    fun updateWidth(orientation: Int, widthPixels: Int) {
        val params = floatingPanelView.root.layoutParams
        params.width = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            widthPixels / 2
        } else FrameLayout.LayoutParams.MATCH_PARENT

        floatingPanelView.root.layoutParams = params
    }

    /**
     * The updating function for views
     *
     * @param view the target view
     * @param startP start int value
     * @param endP end int value
     * @param update call back for updating
     * @return the callback as a function
     */
    private fun <V : View> propertyUpdater(
        view: V,
        startP: Int,
        endP: Int,
        update: (view: V, value: Int) -> Unit,
    ): (Float) -> Unit {
        return {
            update(view, (startP + (endP - startP) * it).toInt())
        }
    }

    /**
     * The updating function for views
     *
     * @param view the target view
     * @param startP start float value
     * @param endP end float value
     * @param update call back for updating
     * @return the callback as a function
     */
    private fun <V : View> propertyUpdater(
        view: V,
        startP: Float,
        endP: Float,
        update: (view: V, value: Float) -> Unit,
    ): (Float) -> Unit {
        return {
            update(view, startP + (endP - startP) * it)
        }
    }

    /**
     * Calculate the color base on the slide offset value
     *
     * @param component the original value
     * @return the final value
     */
    private fun composeColor(component: Int): Int {
        return ((component.shl(16) or component.shl(8) or component).toLong() or 0xFF000000).toInt()
    }

    /**
     * Update the warning icon for mic permission
     *
     * @param isGranted if have mic permission, is true, else is false
     */
    fun updateMicPermissionWaring(isGranted: Boolean) {
        floatingPanelView.micWarning.isVisible = !isGranted
    }

    /**
     * Update the warning icon for Cam permission
     *
     * @param isGranted if have cam permission, is true, else is false
     */
    fun updateCamPermissionWaring(isGranted: Boolean) {
        floatingPanelView.camWarning.isVisible = !isGranted
    }

    companion object {
        private const val BOTTOM_PANEL_MIN_ALPHA = 0.32F
        private const val BOTTOM_PANEL_PROPERTY_UPDATER_OFFSET_THRESHOLD = 0.5F

        private const val BOTTOM_PANEL_EXPAND_OFFSET = 1F
        private const val BOTTOM_PANEL_COLLAPSE_OFFSET = 0F

        private const val INDICATOR_COLOR_START = 0x4F
        private const val INDICATOR_COLOR_END = 0xBD

        private const val FAB_LABEL_COLOR_DARK_MODE = 0xE2
        private const val FAB_LABEL_COLOR_START_LIGHT_MODE = 0xFF
        private const val FAB_LABEL_COLOR_END_LIGHT_MODE = 0x21
        private const val FAB_TINT_COLOR_START = 0.0F
        private const val FAB_TINT_COLOR_END = 1.0F
    }
}

