package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.zhpan.bannerview.constants.IndicatorGravity
import com.zhpan.indicator.enums.IndicatorStyle
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.GridViewCallFragmentBinding
import mega.privacy.android.app.meeting.adapter.GridViewPagerAdapter
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.listeners.GridViewListener
import mega.privacy.android.app.meeting.listeners.RequestLowResVideoListener
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaChatSession
import nz.mega.sdk.MegaHandleList

class GridViewCallFragment : MeetingBaseFragment(), GridViewListener {

    private lateinit var viewDataBinding: GridViewCallFragmentBinding

    private var maxWidth = 0

    private var maxHeight = 0
    private var participants: MutableList<Participant> = mutableListOf()

    private var adapterPager: GridViewPagerAdapter? = null


    private val participantsObserver = Observer<MutableList<Participant>> {
        participants = it
        viewDataBinding.gridViewPager.refreshData(sliceBy6(it))
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
            (parentFragment as InMeetingFragment).inMeetingViewModel,
            parentFragment,
            maxWidth,
            maxHeight, this
        )

        viewDataBinding.gridViewPager
            .setScrollDuration(800)
            .setAutoPlay(false)
            .setIndicatorStyle(IndicatorStyle.CIRCLE)
            .setIndicatorSliderGap(Util.dp2px(6f))
            .setIndicatorSliderRadius(
                Util.dp2px(3f),
                Util.dp2px(3f)
            )
            .setIndicatorMargin(0, 0, 0, 170)
            .setIndicatorGravity(IndicatorGravity.CENTER)
            .setIndicatorSliderColor(
                ContextCompat.getColor(requireContext(), R.color.grey_300_grey_600),
                ContextCompat.getColor(requireContext(), R.color.white)
            )
            .setOnPageClickListener(null)
            .setAdapter(
                adapterPager
            )
            .create()

        // TODO test code start
        (parentFragment as InMeetingFragment).inMeetingViewModel.participants.observeForever(
            participantsObserver
        )
        // TODO test code end
    }

    private fun getParticipant(session: MegaChatSession): Participant? {
        val participant = participants.filter {
            it.peerId == session.peerid && it.clientId == session.clientid
        }
        if (participant.isNotEmpty()) {
            return participant.get(0)
        }
        return null
    }

    private fun getParticipant(listPeers: MutableSet<Participant>): Participant? {
        val iterator = listPeers.iterator()
        iterator.forEach { peer ->
            val peerId = peer.peerId
            val clientId = peer.clientId
            val participant = participants.filter {
                it.peerId == peerId && it.clientId == clientId
            }
            if (participant.isNotEmpty()) {
                return participant.get(0)
            }
        }
        return null
    }

    fun updateName(listPeers: MutableSet<Participant>) {
        getParticipant(listPeers)?.let {
            adapterPager?.updateParticipantName(it)
        }
    }

    fun updateOnHold(session: MegaChatSession) {
        getParticipant(session)?.let {
            adapterPager?.updateOnHold(it, session.isOnHold)
        }
    }

    fun updatePrivileges(listPeers: MutableSet<Participant>) {
        getParticipant(listPeers)?.let {
            adapterPager?.updateParticipantPrivileges(it)
        }
    }

    fun updateRemoteAudioVideo(type: Int, session: MegaChatSession) {
        getParticipant(session)?.let {
            adapterPager?.updateParticipantAudioVideo(type, it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (parentFragment as InMeetingFragment).inMeetingViewModel.participants.removeObserver(
            participantsObserver
        )
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

    /**
     * Close Video
     *
     * @param Participant
     */
    override fun onCloseVideo(session: MegaChatSession?, participant: Participant) {
        if (participant.videoListener == null)
            return

        sharedModel.chatRoomLiveData.value?.let {
            session?.let { sessionParticipant ->
                if (sessionParticipant.canRecvVideoLowRes()) {
                    val list: MegaHandleList = MegaHandleList.createInstance()
                    list.addMegaHandle(participant.clientId)
                    sharedModel.stopLowResVideo(
                        it.chatId, list, RequestLowResVideoListener(
                            requireContext()
                        )
                    )
                }
            }

            sharedModel.removeRemoteVideo(
                it.chatId,
                participant.clientId,
                participant.hasHiRes,
                participant.videoListener!!
            )
        }
        participant.videoListener = null
    }

    /**
     * Active Video
     *
     * @param Participant
     */
    override fun onActivateVideo(session: MegaChatSession?, participant: Participant) {
        sharedModel.chatRoomLiveData.value?.let {
            sharedModel.addRemoteVideo(
                it.chatId,
                participant.clientId,
                participant.hasHiRes,
                participant.videoListener!!
            )

            session?.let { sessionParticipant ->
                if (!sessionParticipant.canRecvVideoLowRes()) {
                    val list: MegaHandleList = MegaHandleList.createInstance()
                    list.addMegaHandle(participant.clientId)
                    sharedModel.requestLowResVideo(
                        it.chatId, list, RequestLowResVideoListener(
                            requireContext()
                        )
                    )
                }
            }
        }
    }
}