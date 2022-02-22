package mega.privacy.android.app.meeting.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.TextView
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.lollipop.controllers.ChatController
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.fragments.MeetingParticipantBottomSheetDialogFragment.Companion.EXTRA_FROM_MEETING
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.StringResourcesUtils
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatPeerList
import javax.inject.Inject

/**
 * ViewModel for [MeetingParticipantBottomSheetDialogFragment]
 */
@HiltViewModel
class MeetingParticipantBottomSheetDialogViewModel @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid
) : ViewModel() {

    var isGuest = false
    var isModerator = false
    var isSpeakerMode = false
    var participant: Participant? = null

    /**
     * Init value for View Model
     */
    fun initValue(
        moderator: Boolean,
        guest: Boolean,
        speakerMode: Boolean,
        info: Participant
    ) {
        isModerator = moderator
        participant = info
        isGuest = guest
        isSpeakerMode = speakerMode
    }

    /**
     * Get email
     *
     * @param context the context
     * @return the email
     */
    fun getEmail(context: Context): String = participant?.peerId?.let {
        ChatController(context).getParticipantEmail(it)
    } ?: ""

    /**
     * Set showing name for text view
     *
     * @param name target view
     */
    fun setShowingName(name: TextView) {
        name.text = participant?.getDisplayName(name.context)
    }

    /**
     * Determine if current participant is guest
     *
     * @return if is guest, return true, else false
     */
    fun isParticipantGuest(): Boolean = participant?.isGuest == true

    /**
     * Determine if show the `Add Contact` item
     * it will show if is not guest and is not contact and not is me
     *
     * @return if should show `Add Contact` item, return true, else false
     */
    fun showAddContact(): Boolean =
        !isGuest && !isParticipantGuest() && participant?.isContact == false && participant?.isMe == false

    /**
     * Determine if show the `Contact Info` item
     * it will show if is not guest and is contact and not is me
     *
     * @return if should show `contactInfo Or EditProfile` item, return true, else false
     */
    fun showContactInfoOrEditProfile(): Boolean = !isGuest &&
            !isParticipantGuest() && (participant?.isContact == true || participant?.isMe == true)

    /**
     * Determine if show the divider between info item and option items
     *
     * @return if should show return true, else false
     */
    fun showDividerContactInfo(): Boolean = (showAddContact() || showContactInfoOrEditProfile())
            && (showSendMessage() || showPinItem() || showMakeModeratorItem() || showRemoveItem())


    /**
     * Determine if show the divider between seng message item and option items
     *
     * @return if should show return true, else false
     */
    fun showDividerSendMessage(): Boolean =
        showSendMessage() && (showPinItem() || showMakeModeratorItem() || showRemoveItem())


    /**
     * Determine if show the divider between ping to speaker item and option items
     *
     * @return if should show return true, else false
     */
    fun showDividerPingToSpeaker(): Boolean =
        showPinItem() && (showMakeModeratorItem() || showRemoveItem())

    /**
     * Determine if show the divider between make moderator item and option items
     *
     * @return if should show return true, else false
     */
    fun showDividerMakeModerator(): Boolean = showMakeModeratorItem() && showRemoveItem()

    /**
     * Determine if show the divider between remove moderator item and option items
     *
     * @return if should show return true, else false
     */
    fun showDividerRemoveModerator(): Boolean = showRemoveModeratorItem() && showRemoveItem()

    /**
     * Determine if show the `Edit Profile` item
     *
     * @return if should show `Edit Profile` item, return true, else false
     */
    fun showEditProfile(): Boolean = !isGuest && !isParticipantGuest() && (participant?.isMe == true || participant?.peerId == megaChatApi.myUserHandle)

    /**
     * Determine if show the `Send Message` item
     * it will show if is not guest and is contact and not is me
     *
     * @return if should show `send Message` item, return true, else false
     */
    fun showSendMessage(): Boolean =
        !isGuest && !isParticipantGuest() && participant?.isContact == true && participant?.isMe == false && participant?.peerId != megaChatApi.myUserHandle


    /**
     * Determine if show the `Pin to speaker view` item
     * it will show if current mode is speaker mode
     *
     * @return if it is speaker mode and I am not the participant, return true, else false
     */
    fun showPinItem(): Boolean = isSpeakerMode && participant?.isMe == false

    /**
     * Set the text for Contact Info item
     * if `isMe` is true, will show `Edit Profile` text
     *
     * @return the target text for the item
     */
    fun getContactItemText(): String? {
        return StringResourcesUtils.getString(
            if (participant?.isMe == true || participant?.peerId == megaChatApi.myUserHandle) R.string.group_chat_edit_profile_label
            else R.string.contact_properties_activity
        )
    }

    /**
     * Determine if show the `Make Moderator` item
     * When the current user is moderator, and not isMe && the target user is not moderator
     *
     * @return if should show `Make Moderator` item, return true, else false
     */
    fun showMakeModeratorItem(): Boolean =
        isModerator && participant?.isMe == false && participant?.isModerator == false && participant?.peerId != megaChatApi.myUserHandle

    /**
     * Determine if show the `Remove Moderator` item
     * When the current user is moderator, and not isMe && the target user is moderator
     *
     * @return if should show `Remove Moderator` item, return true, else false
     */
    fun showRemoveModeratorItem(): Boolean =
        isModerator && participant?.isMe == false && participant?.isModerator == true && participant?.peerId != megaChatApi.myUserHandle

    /**
     * Determine if show the `Remove Participant` item
     * When the current user is moderator, and not isMe
     *
     * @return if should show `Remove Participant` item, return true, else false
     */
    fun showRemoveItem(): Boolean = !isGuest && isModerator && participant?.isMe == false

    /**
     * Open edit profile page
     *
     * @param activity the current activity
     */
    fun editProfile(activity: Activity) {
        val editProfile = Intent(activity, ManagerActivityLollipop::class.java)
        editProfile.putExtra(EXTRA_FROM_MEETING, true)
        editProfile.action = Constants.ACTION_SHOW_MY_ACCOUNT
        activity.startActivity(editProfile)
    }

    /**
     * Open sending message page
     *
     * @param activity the current activity
     */
    fun sendMessage(activity: Activity) {
        participant?.peerId?.let {
            val chat = megaChatApi.getChatRoomByUser(it)
            val peers = MegaChatPeerList.createInstance()

            if (chat == null) {
                peers.addPeer(it, MegaChatPeerList.PRIV_STANDARD)
                megaChatApi.createChat(false, peers, null)
            } else {
                val intentOpenChat = Intent(activity, ChatActivityLollipop::class.java)
                intentOpenChat.action = Constants.ACTION_CHAT_SHOW_MESSAGES
                intentOpenChat.putExtra(Constants.CHAT_ID, chat.chatId)
                activity.startActivity(intentOpenChat)
            }
        }
    }
}