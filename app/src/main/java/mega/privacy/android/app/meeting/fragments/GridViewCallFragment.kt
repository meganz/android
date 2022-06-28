package mega.privacy.android.app.meeting.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.viewpager2.widget.ViewPager2
import mega.privacy.android.app.databinding.GridViewCallFragmentBinding
import mega.privacy.android.app.meeting.adapter.GridViewPagerAdapter
import mega.privacy.android.app.meeting.adapter.Participant
import nz.mega.sdk.MegaChatSession
import timber.log.Timber

class GridViewCallFragment : MeetingBaseFragment() {

    private lateinit var viewDataBinding: GridViewCallFragmentBinding
    private val viewModel by viewModels<InMeetingViewModel>({ requireParentFragment() })

    private var maxWidth = 0
    private var maxHeight = 0
    private var isFirsTime = true
    private var currentPage = 0

    private var participants: MutableList<Participant> = mutableListOf()
    private var viewPagerData: List<List<Participant>> = mutableListOf()

    private lateinit var adapterPager: GridViewPagerAdapter

    @SuppressLint("NotifyDataSetChanged")
    private val participantsObserver = Observer<MutableList<Participant>> {
        participants = it
        val newData = sliceBy6(it)
        if (isFirsTime) {
            Timber.d("Participants changed")
            isFirsTime = false
            adapterPager.apply {
                setNewData(newData)
                notifyDataSetChanged()
            }
            viewPagerData = newData
            updateVisibleParticipantsGrid(newData)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        viewDataBinding = GridViewCallFragmentBinding.inflate(inflater, container, false)
        return viewDataBinding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    @Suppress("DEPRECATION")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val display = meetingActivity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        maxWidth = outMetrics.widthPixels
        maxHeight = outMetrics.heightPixels

        adapterPager = GridViewPagerAdapter(
            viewPagerData,
            viewModel,
            maxWidth,
            maxHeight,
            (parentFragment as InMeetingFragment)::onPageClick
        )

        viewDataBinding.gridViewPager.offscreenPageLimit = 1
        viewDataBinding.gridViewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (currentPage != position) {
                    currentPage = position
                    Timber.d("New page selected $position")
                    viewModel.removeAllParticipantVisible()

                    val data = sliceBy6(participants)
                    updateVisibleParticipantsGrid(data)
                }
            }
        })

        viewModel.participants.value?.let {
            participants = it
        }
        val newData = sliceBy6(participants)
        adapterPager.let {
            it.setNewData(newData)
            it.notifyDataSetChanged()
        }

        viewPagerData = newData
        viewDataBinding.gridViewPager.adapter = adapterPager
        updateVisibleParticipantsGrid(newData)

        viewModel.participants.observe(
            viewLifecycleOwner,
            participantsObserver
        )
    }

    /**
     * Method that updates the number of participants visible on the current page.
     *
     * @param data The list of participants on each page
     */
    fun updateVisibleParticipantsGrid(data: List<List<Participant>>) {
        if (data.isNullOrEmpty())
            return

        if (data.size > currentPage) {
            val dataInPage = data[currentPage]
            viewModel.updateVisibleParticipants(
                dataInPage
            )
        } else {
            viewModel.removeAllParticipantVisible()
        }

        closeVideoWhenScroll()
        activateVideoWhenScroll()
    }

    /**
     * Method that asks to receive videos from participants who are visible
     */
    private fun activateVideoWhenScroll() {
        val visibleParticipants = viewModel.visibleParticipants
        if (visibleParticipants.isNotEmpty()) {
            val iteratorParticipants = visibleParticipants.iterator()
            iteratorParticipants.forEach {
                if (it.isVideoOn) {
                    Timber.d("Activate video of participant visible")
                    adapterPager.updateVideoWhenScroll(
                        true,
                        it,
                        currentPage,
                        viewDataBinding.gridViewPager
                    )
                }
            }
        }
    }

    /**
     * Method to stop receiving videos from participants who are not visible
     */
    private fun closeVideoWhenScroll() {
        val visibleParticipants = viewModel.visibleParticipants

        participants.let { iteratorParticipants ->
            iteratorParticipants.iterator().forEach { participant ->
                if (participant.isVideoOn) {
                    if (visibleParticipants.isEmpty()) {
                        Timber.d("Close video of participant visible")
                        adapterPager.updateVideoWhenScroll(
                            false,
                            participant,
                            currentPage,
                            viewDataBinding.gridViewPager
                        )
                    } else {
                        val participantVisible = visibleParticipants.filter {
                            it.peerId == participant.peerId && it.clientId == participant.clientId
                        }

                        if (participantVisible.isEmpty()) {
                            Timber.d("Close video of participant visible")
                            adapterPager.updateVideoWhenScroll(
                                false,
                                participant,
                                currentPage,
                                viewDataBinding.gridViewPager
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Updating the participant who joined or left the call
     *
     * @param isAdded True, if the participant has been added. False, if the participant has left.
     * @param position The participant's position in the list
     */
    fun peerAddedOrRemoved(
        isAdded: Boolean,
        position: Int,
    ) {
        val newData = sliceBy6(participants)

        adapterPager.let {
            adapterPager.setNewData(newData)
            if (isAdded) {
                Timber.d("Participant added in $position")
                it.participantAdded(viewPagerData, newData, position)
            } else {
                Timber.d("Participant removed in $position")
                it.participantRemoved(viewPagerData, newData, position)
            }
        }

        viewPagerData = newData
        updateVisibleParticipantsGrid(newData)
    }

    /**
     * Check changes call on hold
     *
     * @param isCallOnHold True, if the call is on hold. False, otherwise
     */
    fun updateCallOnHold(isCallOnHold: Boolean) {
        val iterator = participants.iterator()
        iterator.forEach {
            Timber.d("Update call on hold status")
            adapterPager.updateCallOnHold(
                it,
                isCallOnHold,
                currentPage,
                viewDataBinding.gridViewPager
            )
        }
    }

    /**
     * Method to control when the video listener should be added or removed.
     *
     * @param participant The participant whose listener of the video is to be added or deleted
     * @param shouldAddListener True, should add the listener. False, should remove the listener
     * @param isHiRes True, if is High resolution. False, if is Low resolution
     */
    fun updateListener(participant: Participant, shouldAddListener: Boolean, isHiRes: Boolean) {
        adapterPager.updateListener(
            participant,
            shouldAddListener,
            isHiRes,
            currentPage,
            viewDataBinding.gridViewPager
        )
    }

    /**
     * Check changes session on hold
     *
     * @param session MegaChatSession
     */
    fun updateSessionOnHold(session: MegaChatSession) {
        viewModel.getParticipant(
            session.peerid,
            session.clientid
        )?.let {
            Timber.d("Update session on hold status")
            adapterPager.updateSessionOnHold(
                it,
                session.isOnHold,
                currentPage,
                viewDataBinding.gridViewPager
            )
        }
    }

    /**
     * Check changes in remote A/V flags
     *
     * @param type type of change, Audio or Video
     * @param session MegaChatSession
     */
    fun updateRemoteAudioVideo(type: Int, session: MegaChatSession) {
        viewModel.getParticipant(
            session.peerid,
            session.clientid
        )?.let {
            Timber.d("Update remote A/V")
            adapterPager.updateParticipantAudioVideo(
                type,
                it,
                currentPage,
                viewDataBinding.gridViewPager
            )
        }
    }

    /**
     * Check changes in name
     *
     * @param listPeers List of participants with changes
     * @param typeChange the type of change, name or avatar
     */
    fun updateNameOrAvatar(listPeers: MutableSet<Participant>, typeChange: Int) {
        val iterator = listPeers.iterator()
        iterator.forEach { peer ->
            viewModel.getParticipant(
                peer.peerId,
                peer.clientId
            )?.let {
                Timber.d("Update participant name")
                adapterPager.updateParticipantNameOrAvatar(
                    it,
                    currentPage,
                    viewDataBinding.gridViewPager,
                    typeChange
                )
            }
        }
    }

    /**
     * Check changes in privileges
     *
     * @param listPeers List of participants with changes
     */
    fun updatePrivileges(listPeers: MutableSet<Participant>) {
        val iterator = listPeers.iterator()
        iterator.forEach { peer ->
            viewModel.getParticipant(
                peer.peerId,
                peer.clientId
            )?.let {
                Timber.d("Update participant privileges")
                adapterPager.updateParticipantPrivileges(
                    it,
                    currentPage,
                    viewDataBinding.gridViewPager
                )
            }
        }
    }

    /**
     * Update layout base on the new orientation
     *
     * @param widthPixels The screen width
     * @param heightPixels The screen height
     */
    fun updateLayout(widthPixels: Int, heightPixels: Int) {
        adapterPager.updateOrientation(widthPixels, heightPixels)

        val currentItem = viewDataBinding.gridViewPager.currentItem
        viewDataBinding.gridViewPager.adapter = adapterPager
        viewDataBinding.gridViewPager.currentItem = currentItem
    }

    /**
     * Method to delete the videos and texture views of participants
     */
    fun removeTextureView() {
        val iterator = participants.iterator()
        iterator.forEach {
            adapterPager.removeTextureView(it, currentPage, viewDataBinding.gridViewPager)
        }
    }

    override fun onDestroyView() {
        Timber.d("View destroyed")
        removeTextureView()
        super.onDestroyView()
    }

    /**
     * Method for grouping participants 6 by 6
     *
     * @param data mutable list with all participants
     * @return mutable list with participants grouped
     */
    private fun sliceBy6(data: MutableList<Participant>): MutableList<List<Participant>> {
        val result = mutableListOf<List<Participant>>()
        val sliceCount =
            if (data.size % PARTICIPANTS_COUNT_PER_SCREEN == 0) data.size / PARTICIPANTS_COUNT_PER_SCREEN else data.size / PARTICIPANTS_COUNT_PER_SCREEN + 1

        for (i in 0 until sliceCount) {
            var to = i * PARTICIPANTS_COUNT_PER_SCREEN + PARTICIPANTS_COUNT_PER_SCREEN - 1
            if (to >= data.size) {
                to = data.size - 1
            }

            result.add(i, data.slice(IntRange(i * PARTICIPANTS_COUNT_PER_SCREEN, to)))
        }

        return result
    }

    companion object {

        const val TAG = "GridViewCallFragment"

        const val PARTICIPANTS_COUNT_PER_SCREEN = 6

        @JvmStatic
        fun newInstance() = GridViewCallFragment()
    }
}