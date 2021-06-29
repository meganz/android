package mega.privacy.android.app.meeting.adapter

import android.graphics.Bitmap
import androidx.recyclerview.widget.DiffUtil
import mega.privacy.android.app.meeting.listeners.GroupVideoListener
import java.io.Serializable

data class Participant(
    val peerId: Long,
    val clientId: Long,
    var name: String,
    val avatar: Bitmap?,
    val isMe: Boolean,
    var isModerator: Boolean,
    var isAudioOn: Boolean,
    var isVideoOn: Boolean,
    var isContact: Boolean = true,
    var isSpeaker: Boolean = false,
    var hasHiRes: Boolean = false,
    var videoListener: GroupVideoListener? = null,
    // Flag for selected for assign moderator
    var isChosenForAssign: Boolean = false,
    var isGuest: Boolean = false
) : Serializable


/**
 * Diff Call back for assign participant recyclerview
 *
 */
class AssignParticipantDiffCallback : DiffUtil.ItemCallback<Participant>() {
    override fun areItemsTheSame(oldItem: Participant, newItem: Participant): Boolean =
        (oldItem.peerId == newItem.peerId) && (oldItem.isChosenForAssign == newItem.isChosenForAssign)

    override fun areContentsTheSame(oldItem: Participant, newItem: Participant): Boolean =
        oldItem == newItem
}