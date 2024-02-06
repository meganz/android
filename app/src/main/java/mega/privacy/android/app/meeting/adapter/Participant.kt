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

/**
 * Participant in a call
 *
 * @param peerId                Peer Id.
 * @param clientId              Client Id.
 * @param name                  Participant's name.
 * @param avatar                Participant's avatar.
 * @param isMe                  True if it's me. False if not.
 * @param isModerator           True if it's moderator. False if not.
 * @param isAudioOn             True if audio is on. False if audio is off.
 * @param isVideoOn             True if video is on. False if video is off.
 * @param isAudioDetected       True if audio is detected. False if not.
 * @param isContact             True if it's my contact. False if not.
 * @param isSpeaker             True if it's speaker. False if not.
 * @param hasHiRes              True if has high video resolution. False if has low video resolution.
 * @param videoListener         [GroupVideoListener].
 * @param isChosenForAssign     True if it's chosen for assign. False if not.
 * @param isGuest               True if it's guest. False if not.
 * @param hasOptionsAllowed     True if has options allowed. False if not.
 * @param isPresenting          True if the user is presenting. False if not.
 * @param isScreenShared        True if this participant is the screen shared. False, if not.
 * @param isCameraOn            True if camera is on. False if camera is off.
 * @param isScreenShareOn       True if screen share is on. False if screen share is off.
 * @param isMuted               True, if it's muted. False, if not.
 */
data class Participant(
    val peerId: Long,
    val clientId: Long,
    var name: String,
    var avatar: Bitmap?,
    val isMe: Boolean,
    var isModerator: Boolean,
    var isAudioOn: Boolean,
    var isVideoOn: Boolean,
    var isAudioDetected: Boolean,
    var isContact: Boolean = true,
    var isSpeaker: Boolean = false,
    var hasHiRes: Boolean = false,
    var videoListener: GroupVideoListener? = null,
    var isChosenForAssign: Boolean = false,
    var isGuest: Boolean = false,
    var hasOptionsAllowed: Boolean = true,
    var isPresenting: Boolean = false,
    var isScreenShared: Boolean = false,
    var isCameraOn: Boolean = false,
    var isScreenShareOn: Boolean = false,
    var isMuted: Boolean = false,
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