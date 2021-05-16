package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.viewpager2.widget.ViewPager2
import mega.privacy.android.app.databinding.GridViewCallFragmentBinding
import mega.privacy.android.app.meeting.MegaSurfaceRendererGroup
import mega.privacy.android.app.meeting.adapter.*
import mega.privacy.android.app.utils.LogUtil.logDebug
import nz.mega.sdk.MegaChatSession

class GridViewCallFragment : MeetingBaseFragment(),
    MegaSurfaceRendererGroup.MegaSurfaceRendererGroupListener {

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
        if (isFirsTime) {
            isFirsTime = false
            val newData = sliceBy6(it)
            adapterPager.let {
                it.setNewData(newData)
                it.notifyDataSetChanged()
            }

            viewPagerData = newData
        }
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
                currentPage = position
                logDebug("New page selected $position")
            }
        })

        viewDataBinding.gridViewPager.adapter = adapterPager

        (parentFragment as InMeetingFragment).inMeetingViewModel.participants.observeForever(
            participantsObserver
        )
    }

    /**
     * Updating the participant who joined or left the call
     *
     * @param isAdded
     * @param position
     * @param participantList
     */
    fun peerAddedOrRemoved(
        isAdded: Boolean,
        position: Int,
        participantList: MutableList<Participant>
    ) {
        val newData = sliceBy6(participantList)
        adapterPager.let {
            it.setNewData(newData)
            if (isAdded) {
                logDebug("New participant added")
                it.participantAdded(viewPagerData, newData, position)
            } else {
                logDebug("New participant removed")
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
            adapterPager.updateParticipantAudioVideo(
                type,
                it,
                currentPage,
                viewDataBinding.gridViewPager
            )
        }
    }

    /**
     * Check changes in resolution
     *
     * @param session MegaChatSession
     */
    fun updateRemoteResolution(session: MegaChatSession) {
        (parentFragment as InMeetingFragment).inMeetingViewModel.getParticipant(
            session.peerid,
            session.clientid
        )?.let {
            adapterPager.updateRemoteResolution(it, currentPage, viewDataBinding.gridViewPager)
        }
    }

    /**
     * Check changes in resolution
     *
     * @param listPeers List of participants with changes
     */
    fun updateRes(listPeers: MutableSet<Participant>) {
        val iterator = listPeers.iterator()
        iterator.forEach { peer ->
            (parentFragment as InMeetingFragment).inMeetingViewModel.getParticipant(
                peer.peerId,
                peer.clientId
            )?.let {
                adapterPager.updateParticipantRes(it, currentPage, viewDataBinding.gridViewPager)
            }
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

    override fun resetSize(peerId: Long, clientId: Long) {
        (parentFragment as InMeetingFragment).inMeetingViewModel.getParticipant(
            peerId,
            clientId
        )?.let {
            if (it.isVideoOn) {
                it.videoListener?.let {
                    it.height = 0
                    it.width = 0
                }
            }
        }
    }

    /**
     * Method to destroy the surfaceView.
     */
    private fun removeSurfaceView() {
        val iterator = participants.iterator()
        iterator.forEach {
            adapterPager.removeSurfaceView(it, currentPage, viewDataBinding.gridViewPager)
        }
    }

    override fun onResume() {
        val iterator = participants.iterator()
        iterator.forEach { participant ->
            participant.videoListener?.let {
                it.height = 0
                it.width = 0
            }
        }
        super.onResume()
    }

    override fun onDestroyView() {
        removeSurfaceView()
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