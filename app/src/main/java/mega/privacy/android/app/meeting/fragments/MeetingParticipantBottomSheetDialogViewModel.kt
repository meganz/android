package mega.privacy.android.app.meeting.fragments

import android.content.Context
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.lifecycle.ViewModel
import mega.privacy.android.app.R
import mega.privacy.android.app.lollipop.controllers.ChatController
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.StringResourcesUtils
import java.util.*

/**
 * ViewModel for [MeetingParticipantBottomSheetDialogFragment]
 */
class MeetingParticipantBottomSheetDialogViewModel : ViewModel() {

    var isGuest = false
    var isModerator = false
    var isSpeakerMode = false
    var participant: Participant? = null
    var context: Context? = null

    /**
     * Init value for View Model
     */
    fun initValue(
        con: Context,
        moderator: Boolean,
        guest: Boolean,
        speakerMode: Boolean,
        info: Participant
    ) {
        isModerator = moderator
        participant = info
        isGuest = guest
        context = con
        isSpeakerMode = speakerMode
    }

    fun getEmail(): String = participant?.peerId?.let {
        ChatController(context).getParticipantEmail(it)
    } ?: ""

    fun setShowingName(name: TextView) {
        name.text =  if (participant?.isMe == true) {
            context?.let {
                HtmlCompat.fromHtml(
                    "${participant?.name} <font color='${
                        ColorUtils.getColorHexString(it, R.color.grey_600)
                    }'>(${
                        StringResourcesUtils.getString(R.string.bucket_word_me)
                            .toLowerCase(Locale.ROOT)
                    })</font>",
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
            }
        } else {
            participant?.name
        }
    }

    fun isParticipantGuest(): Boolean = participant?.isGuest == true

    /**
     * Determine if show the `Add Contact` item
     *
     * it will show if is not guest and is not contact and not is me
     */
    fun showAddContact(): Boolean =
        !isGuest && !isParticipantGuest() && participant?.isContact == false && participant?.isMe == false

    /**
     * Determine if show the `Contact Info` item
     *
     * it will show if is not guest and is contact and not is me
     */
    fun showContactInfoOrEditProfile(): Boolean = !isGuest &&
            !isParticipantGuest() && (participant?.isContact == true || participant?.isMe == true)


    fun showDividerContactInfo(): Boolean = showAddContact() || showContactInfoOrEditProfile()

    /**
     * Determine if show the `Edit Profile` item
     *
     */
    fun showEditProfile(): Boolean = !isGuest && !isParticipantGuest() && participant?.isMe == true

    /**
     * Determine if show the `Send Message` item
     *
     * it will show if is not guest and is contact and not is me
     */
    fun showSendMessage(): Boolean =
        !isGuest && !isParticipantGuest() && participant?.isContact == true && participant?.isMe == false


    /**
     * Determine if show the `Pin to speaker view` item
     *
     * it will show if current mode is speaker mode
     */
    fun showPinItem(): Boolean = isSpeakerMode

    /**
     * Set the text for Contact Info item
     *
     * if `isMe` is true, will show `Edit Profile` text
     */
    fun getContactItemText(): String? {
        return if (participant?.isMe == true) {
            StringResourcesUtils.getString(R.string.group_chat_edit_profile_label)
        } else {
            StringResourcesUtils.getString(R.string.contact_properties_activity)
        }
    }

    /**
     * Determine if show the `Make Moderator` item
     *
     * When the current user is moderator, and not isMe && the target user is not moderator
     */
    fun showMakeModeratorItem(): Boolean =
        !isGuest && isModerator && !(participant?.isMe == true || participant?.isModerator == true || isParticipantGuest())

    /**
     * Determine if show the `Remove Participant` item
     *
     * When the current user is moderator, and not isMe
     */
    fun showRemoveItem(): Boolean = !isGuest && isModerator && participant?.isMe == false
}