package mega.privacy.android.app.meeting.adapter

import android.graphics.Rect
import android.view.SurfaceHolder
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.components.CustomizedGridCallRecyclerView
import mega.privacy.android.app.databinding.ItemParticipantVideoBinding
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.app.meeting.listeners.GridViewListener
import mega.privacy.android.app.meeting.listeners.MeetingVideoListener
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.LogUtil.logDebug
import nz.mega.sdk.MegaApiAndroid
import org.jetbrains.anko.displayMetrics
import javax.inject.Inject

/**
 * When use DataBinding here, when user fling the RecyclerView, the bottom sheet will have
 * extra top offset. Not use DataBinding could avoid this bug.
 */
class VideoGridViewHolder(
    private val binding: ItemParticipantVideoBinding,
    private val gridView: CustomizedGridCallRecyclerView,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val listener: GridViewListener
) : RecyclerView.ViewHolder(binding.root) {

    @Inject
    lateinit var megaApi: MegaApiAndroid

    lateinit var inMeetingViewModel: InMeetingViewModel

    lateinit var holder: SurfaceHolder

    fun bind(
        inMeetingViewModel: InMeetingViewModel,
        participant: Participant,
        itemCount: Int,
        isFirstPage: Boolean
    ) {
        this.inMeetingViewModel = inMeetingViewModel

        layout(isFirstPage, itemCount)

        binding.name.text = participant.name

        initAvatar(participant)
        initStatus(participant)
        holder = binding.video.holder
    }

    fun updateName(participant: Participant) {
        binding.name.text = participant.name
    }

    fun updatePrivilegeIcon(participant: Participant) {
        binding.moderatorIcon.isVisible = participant.isModerator
    }

    fun updateAudioIcon(participant: Participant) {
        binding.muteIcon.isVisible = !participant.isAudioOn
    }

    fun updateOnHold(participant: Participant, isOnHold: Boolean) {
        if (isOnHold) {
            binding.onHoldIcon.isVisible = true
            binding.avatar.alpha = 0.5f
            showAvatar(participant)
        } else {
            binding.onHoldIcon.isVisible = false
            binding.avatar.alpha = 1f
            if(participant.isVideoOn){
                activateVideo(participant)
            }else{
                showAvatar(participant)
            }
        }
    }

    fun updateVideo(participant: Participant) {
        when {
            participant.isVideoOn -> {
                activateVideo(participant)
            }
            else -> {
                showAvatar(participant)
            }
        }
    }

    private fun initAvatar(participant: Participant) {
        inMeetingViewModel.getChat()?.let {
            var avatar = CallUtil.getImageAvatarCall(it, participant.peerId)
            if (avatar == null) {
                avatar = CallUtil.getDefaultAvatarCall(
                    MegaApplication.getInstance().applicationContext,
                    participant.peerId
                )
            }

            binding.avatar.setImageBitmap(avatar)
        }
    }

    private fun initStatus(participant: Participant) {
        val session = inMeetingViewModel.getSession(participant.peerId)
        val call = inMeetingViewModel.getCall()
        call?.let {
            if (participant.isVideoOn && !it.isOnHold && (session == null || !session.isOnHold)) {
                activateVideo(participant)
            } else {
                showAvatar(participant)
            }

            updateAudioIcon(participant)
            updatePrivilegeIcon(participant)
        }
    }

    private fun showAvatar(participant: Participant) {
        binding.avatar.isVisible = true
        closeVideo(participant)
    }

    /**
     * Method for activating the video.
     */
    private fun activateVideo(participant: Participant) {
        binding.avatar.isVisible = false

        if (participant.videoListener == null) {
            var vListener = MeetingVideoListener(
                binding.video,
                MegaApplication.getInstance().applicationContext.displayMetrics,
                participant.clientId,
                false
            )

            participant.videoListener = vListener

            listener.onActivateVideo(
                inMeetingViewModel.getSession(participant.clientId),
                participant
            )
        }

        binding.video.isVisible = true
    }

    /**
     * Method to close Video
     */
    private fun closeVideo(participant: Participant) {
        binding.video.isVisible = false
        listener.onCloseVideo(inMeetingViewModel.getSession(participant.clientId), participant)
    }

    private fun layout(isFirstPage: Boolean, itemCount: Int) {
        var w = 0
        var h = 0

        val layoutParams: GridLayoutManager.LayoutParams =
            binding.root.layoutParams as GridLayoutManager.LayoutParams

        val verticalMargin = ((screenHeight - screenWidth / 2 * 3) / 2)

        if (isFirstPage) {
            when (itemCount) {
                1 -> {
                    w = screenWidth
                    h = screenHeight
                    layoutParams.setMargins(0, 0, 0, 0)
                }
                2 -> {
                    w = screenWidth
                    h = screenHeight / 2
                    layoutParams.setMargins(0, 0, 0, 0)
                }
                3 -> {
                    w = (screenWidth * 0.8).toInt()
                    h = screenHeight / 3
                    layoutParams.setMargins((screenWidth - w) / 2, 0, (screenWidth - w) / 2, 0)
                }
                5 -> {
                    w = screenWidth / 2
                    h = w

                    when (adapterPosition) {
                        0, 1 -> {
                            layoutParams.setMargins(0, verticalMargin, 0, 0)
                        }
                        4 -> {
                            layoutParams.setMargins(
                                (screenWidth - w) / 2,
                                0,
                                (screenWidth - w) / 2,
                                0
                            )
                        }
                        else -> {
                            layoutParams.setMargins(0, 0, 0, 0)
                        }
                    }
                }
                4, 6 -> {
                    val pair = layout46(layoutParams, verticalMargin)
                    h = pair.first
                    w = pair.second
                }
            }
        } else {
            val pair = layout46(layoutParams, verticalMargin)
            h = pair.first
            w = pair.second
        }

        layoutParams.width = w
        layoutParams.height = h
    }

    private fun layout46(
        layoutParams: GridLayoutManager.LayoutParams,
        verticalMargin: Int
    ): Pair<Int, Int> {
        val w = screenWidth / 2
        when (adapterPosition) {
            0, 1 -> {
                layoutParams.setMargins(0, verticalMargin, 0, 0)
            }
            else -> {
                layoutParams.setMargins(0, 0, 0, 0)
            }
        }
        return Pair(w, w)
    }
}
