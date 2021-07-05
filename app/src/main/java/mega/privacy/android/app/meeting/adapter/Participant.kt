package mega.privacy.android.app.meeting.adapter

import android.content.Context
import android.graphics.Bitmap
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import mega.privacy.android.app.R
import mega.privacy.android.app.meeting.listeners.GroupVideoListener
import mega.privacy.android.app.utils.StringResourcesUtils
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
) : Serializable {

    /**
     * Get the display name for participant list
     *
     * @param context the context
     * @return if is not me, return full name, else return full name with "me"
     */
    fun getDisplayName(context: Context): CharSequence {
        return if (isMe) {
            val spannableString = SpannableString(StringResourcesUtils.getString(R.string.chat_me_text_bracket, name))
            spannableString.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(context, R.color.grey_600)),
                name.length,
                spannableString.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString
        } else {
            name
        }
    }
}

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