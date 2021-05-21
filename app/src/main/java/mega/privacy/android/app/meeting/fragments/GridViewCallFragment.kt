package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.viewpager2.widget.ViewPager2
import mega.privacy.android.app.databinding.GridViewCallFragmentBinding
import mega.privacy.android.app.meeting.MegaSurfaceRenderer
import mega.privacy.android.app.meeting.adapter.*
import mega.privacy.android.app.utils.LogUtil.logDebug
import nz.mega.sdk.MegaChatSession

class GridViewCallFragment : MeetingBaseFragment(),
    MegaSurfaceRenderer.MegaSurfaceRendererListener {

    private lateinit var viewDataBinding: GridViewCallFragmentBinding

    private var maxWidth = 0
    private var maxHeight = 0
    private var isFirsTime = true
    private var currentPage = 0

    private var participants: MutableList<Participant> = mutableListOf()
    private var viewPagerData: List<List<Participant>> = mutableListOf()

    private lateinit var adapterPager: GridViewPagerAdapter

    private val participantsObserver = Observer<MutableList<Participant>> {
        participants = it
        val newData = sliceBy6(it)
        if (isFirsTime) {
            logDebug("Participants changed")
            isFirsTime = false
            adapterPager.let {
                it.setNewData(newData)
                it.notifyDataSetChanged()
            }
            viewPagerData = newData
        }

        updateVisibleParticipantsGrid(newData)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewDataBinding = GridViewCallFragmentBinding.inflate(inflater, container, false)
        return viewDataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val display = meetingActivity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        maxWidth = outMetrics.widthPixels
        maxHeight = outMetrics.heightPixels

        adapterPager = GridViewPagerAdapter(
            viewPagerData,
            (parentFragment as InMeetingFragment).inMeetingViewModel,
            parentFragment,
            maxWidth,
            maxHeight,
            this
        )

        viewDataBinding.gridViewPager.offscreenPageLimit = 1
        viewDataBinding.gridViewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (currentPage != position) {
                    currentPage = position

                    logDebug("New page selected $position")
                    (parentFragment as InMeetingFragment).inMeetingViewModel.removeVisibleParticipants()

                    val data = sliceBy6(participants)
                    updateVisibleParticipantsGrid(data)

                    (parentFragment as InMeetingFragment).inMeetingViewModel.requestVideosWhenScroll()
                    (parentFragment as InMeetingFragment).inMeetingViewModel.stopVideosWhenScroll()
                }
            }
        })

        logDebug("View created and participants added")
        val newData = sliceBy6(participants)
        adapterPager.let {
            it.setNewData(newData)
            it.notifyDataSetChanged()
        }

        viewPagerData = newData
        viewDataBinding.gridViewPager.adapter = adapterPager

        (parentFragment as InMeetingFragment).inMeetingViewModel.participants.observeForever(
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
            (parentFragment as InMeetingFragment).inMeetingViewModel.updateVisibleParticipants(
                dataInPage
            )
        } else {
            (parentFragment as InMeetingFragment).inMeetingViewModel.removeVisibleParticipants()
        }
    }

    /**
     * Updating the participant who joined or left the call
     *
     * @param isAdded
     * @param position
     */
    fun peerAddedOrRemoved(
        isAdded: Boolean,
        position: Int,
    ) {
        val newData = sliceBy6(participants)
        adapterPager.let {
            adapterPager.setNewData(newData)
            if (isAdded) {
                logDebug("Participant added in $position")
                it.participantAdded(viewPagerData, newData, position)
            } else {
                logDebug("Participant removed in $position")
                it.participantRemoved(viewPagerData, newData, position)
            }
        }
        viewPagerData = newData
    }

    /**
     * Check changes call on hold
     *
     * @param isCallOnHold True, if the call is on hold. False, otherwise
     */
    fun updateCallOnHold(isCallOnHold: Boolean) {
        val iterator = participants.iterator()
        iterator.forEach {
            logDebug("Update call on hold status")
            adapterPager.updateCallOnHold(
                it,
                isCallOnHold,
                currentPage,
                viewDataBinding.gridViewPager
            )
        }
    }

    /**
     * Check changes session on hold
     *
     * @param session MegaChatSession
     */
    fun updateSessionOnHold(session: MegaChatSession) {
        (parentFragment as InMeetingFragment).inMeetingViewModel.getParticipant(
            session.peerid,
            session.clientid
        )?.let {
            logDebug("Update session on hold status")
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
        (parentFragment as InMeetingFragment).inMeetingViewModel.getParticipant(
            session.peerid,
            session.clientid
        )?.let {
            logDebug("Update remote A/V")
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
     */
    fun updateName(listPeers: MutableSet<Participant>) {
        val iterator = listPeers.iterator()
        iterator.forEach { peer ->
            (parentFragment as InMeetingFragment).inMeetingViewModel.getParticipant(
                peer.peerId,
                peer.clientId
            )?.let {
                logDebug("Update participant name")
                adapterPager.updateParticipantName(it, currentPage, viewDataBinding.gridViewPager)
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
            (parentFragment as InMeetingFragment).inMeetingViewModel.getParticipant(
                peer.peerId,
                peer.clientId
            )?.let {
                logDebug("Update participant privileges")
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
     * @param newOrientation
     * @param widthPixels
     * @param heightPixels
     */
    fun updateLayout(newOrientation: Int, widthPixels: Int, heightPixels: Int) {
        adapterPager.updateOrientation(newOrientation, widthPixels, heightPixels)
    }

    /**
     * Method for resizing the listener
     *
     * @param peerId
     * @param clientId
     */
    override fun resetSize(peerId: Long, clientId: Long) {
        (parentFragment as InMeetingFragment).inMeetingViewModel.getParticipant(
            peerId,
            clientId
        )?.let {
            if (it.isVideoOn) {
                it.videoListener?.let {
                    logDebug("Resize participant listener")
                    it.height = 0
                    it.width = 0
                }
            }
        }
    }

    /**
     * Method to delete the videos and texture views of participants
     */
    private fun removeTextureView() {
        val iterator = participants.iterator()
        iterator.forEach {
            (parentFragment as InMeetingFragment).inMeetingViewModel.onCloseVideo(it)
            logDebug("Remove texture view")
            adapterPager.removeTextureView(it, currentPage, viewDataBinding.gridViewPager)
        }
    }

    override fun onResume() {
        val iterator = participants.iterator()
        iterator.forEach { participant ->
            participant.videoListener?.let {
                logDebug("Resize speaker listener")
                it.height = 0
                it.width = 0
            }
        }
        super.onResume()
    }

    override fun onDestroyView() {
        logDebug("View destroyed")
        removeTextureView()
        super.onDestroyView()
    }

    override fun onDestroy() {
        (parentFragment as InMeetingFragment).inMeetingViewModel.participants.removeObserver(
            participantsObserver
        )
        super.onDestroy()
    }

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