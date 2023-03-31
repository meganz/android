package mega.privacy.android.app.presentation.contact.model

import android.graphics.Bitmap
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserStatus

/**
 * Contact info UI state
 *
 * @property error                  String resource id for showing an error.
 * @property isCallStarted          Handle when a call is started.
 * @property userStatus             user status
 * @property lastGreen              last seen time gap in minutes
 * @property isFromContacts         Checks if contact info launched from contacts screen
 * @property contactItem            Contact item contains all the info about the user
 * @property chatRoom               Chat room info
 * @property avatar                 Bitmap of user avatar
 * @property isOnline               Checks if connected to internet
 * @property snackBarMessage        One time snack bar message to be shown to the user
 * @property isUserRemoved          Checks if selected user is removed from contacts
 */
data class ContactInfoState(
    val error: Int? = null,
    val isCallStarted: Boolean? = false,
    val userStatus: UserStatus = UserStatus.Invalid,
    val lastGreen: Int = 0,
    val isFromContacts: Boolean = false,
    val avatar: Bitmap? = null,
    val contactItem: ContactItem? = null,
    val chatRoom: ChatRoom? = null,
    val isOnline: Boolean = false,
    val snackBarMessage: Int? = null,
    val isUserRemoved: Boolean = false,
) {

    /**
     * checks if credentials are verified
     */
    val areCredentialsVerified = contactItem?.areCredentialsVerified ?: false

    /**
     * primary display name its displayed as toolbar title
     */
    val primaryDisplayName by lazy(LazyThreadSafetyMode.NONE) {
        contactItem?.contactData?.alias ?: contactItem?.contactData?.fullName ?: ""
    }

    private val hasAlias = !contactItem?.contactData?.alias.isNullOrEmpty()

    /**
     * secondary display name   Full name of the user shown only if user has a nick name
     */
    val secondaryDisplayName: String? by lazy(LazyThreadSafetyMode.NONE) {
        if (hasAlias) contactItem?.contactData?.fullName else null
    }

    /**
     * user email
     */
    val email by lazy(LazyThreadSafetyMode.NONE) { chatRoom?.let { contactItem?.email } }

    /**
     * shows edit or add nick name
     */
    val modifyNickNameTextId by lazy(LazyThreadSafetyMode.NONE) { if (hasAlias) R.string.edit_nickname else R.string.add_nickname }

}
