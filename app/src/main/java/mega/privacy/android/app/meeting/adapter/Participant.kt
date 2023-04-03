package mega.privacy.android.app.meeting.adapter

import android.content.Context
import android.graphics.Bitmap
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DiffUtil
import mega.privacy.android.app.R
import mega.privacy.android.app.meeting.listeners.GroupVideoListener
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import timber.log.Timber
import java.io.Serializable

data class Participant(
    val peerId: Long,
    val clientId: Long,
    var name: String,
    var avatar: Bitmap?,
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
    var isGuest: Boolean = false,
    var hasOptionsAllowed: Boolean = true,

    ) : Serializable {

    /**
     * Get the display name for participant list
     *
     * @param context the context
     * @return if is not me, return full name, else return full name with "me"
     */
    fun getDisplayName(context: Context): CharSequence {
        return if (isMe) {
            var displayName = context.getString(R.string.meeting_me_text_bracket, name)

            try {
                displayName = displayName.replace(
                    "[A]", "<font color='"
                            + getColorHexString(context, R.color.grey_200) + "'>"
                )
                displayName = displayName.replace("[/A]", "</font>")
            } catch (e: Exception) {
                Timber.w(e, "Exception formatting string")
            }

            HtmlCompat.fromHtml(
                displayName,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
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