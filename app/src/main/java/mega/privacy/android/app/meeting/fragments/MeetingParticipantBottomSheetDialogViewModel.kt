package mega.privacy.android.app.meeting.fragments

import android.content.Context
import androidx.lifecycle.ViewModel
import mega.privacy.android.app.R
import mega.privacy.android.app.meeting.adapter.Participant

/**
 * ViewModel for [MeetingParticipantBottomSheetDialogFragment]
 */
class MeetingParticipantBottomSheetDialogViewModel : ViewModel() {

    var isGuest = false
    var isModerator = false

    var participant: Participant? = null
    var context: Context? = null

    /**
     * Init value for View Model
     */
    fun initValue(con: Context, moderator: Boolean, guest: Boolean, info: Participant) {
        isModerator = moderator
        isGuest = guest
        participant = info
        context = con
    }

    /**
     * Determine if show the `Add Contact` item
     *
     * it will show if is not guest and is not contact and not is me
     */
    fun showAddContact(): Boolean =
        !isGuest && participant?.isContact == false && participant?.isMe == false

    /**
     * Determine if show the `Contact Info` item
     *
     * it will show if is not guest and is contact and not is me
     */
    fun showContactInfoOrEditProfile(): Boolean =
        !isGuest && (participant?.isContact == true || participant?.isMe == true)


    fun showDividerContactInfo(): Boolean = showAddContact() || showContactInfoOrEditProfile()

    /**
     * Determine if show the `Edit Profile` item
     *
     */
    fun showEditProfile(): Boolean = !isGuest && participant?.isMe == true

    /**
     * Determine if show the `Send Message` item
     *
     * it will show if is not guest and is contact and not is me
     */
    fun showSendMessage(): Boolean =
        !isGuest && participant?.isContact == true && participant?.isMe == false

    /**
     * Set the text for Contact Info item
     *
     * if `isMe` is true, will show `Edit Profile` text
     */
    fun getContactItemText(): String? {
        return if (participant?.isMe == true) {
            context?.getString(R.string.group_chat_edit_profile_label)
        } else {
            context?.getString(R.string.contact_properties_activity)
        }
    }

    /**
     * Determine if show the `Make Moderator` item
     *
     * When the current user is moderator, and not isMe && the target user is not moderator
     */
    fun showMakeModeratorItem(): Boolean =
        isModerator && !(participant?.isMe == true || participant?.isModerator == true)

    /**
     * Determine if show the `Remove Participant` item
     *
     * When the current user is moderator, and not isMe
     */
    fun showRemoveItem(): Boolean = isModerator && participant?.isMe == false

}