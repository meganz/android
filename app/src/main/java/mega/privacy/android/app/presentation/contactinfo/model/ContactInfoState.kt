package mega.privacy.android.app.presentation.contactinfo.model

import android.graphics.Bitmap
import mega.privacy.android.app.R
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.entity.node.UnTypedNode

/**
 * Contact info UI state
 *
 * @property error                                      String resource id for showing an error.
 * @property isCallStarted                              Handle when a call is started.
 * @property userStatus                                 user status
 * @property lastGreen                                  last seen time gap in minutes
 * @property isFromContacts                             Checks if contact info launched from contacts screen
 * @property contactItem                                Contact item contains all the info about the user
 * @property chatRoom                                   Chat room info
 * @property avatar                                     Bitmap of user avatar
 * @property isOnline                                   Checks if connected to internet
 * @property snackBarMessage                            One time snack bar message to be shown to the user
 * @property snackBarMessageString                      One time snack bar message to be shown to the user
 * @property isUserRemoved                              Checks if selected user is removed from contacts
 * @property inShares                                   In shares for the user
 * @property callStatusChanged                          when chat call status is changed
 * @property isPushNotificationSettingsUpdated          Push notification settings updated event
 * @property shouldNavigateToChat                       Triggers navigation to chat activity
 * @property isChatNotificationChange                   Mute or Un-mute chat notification for the user
 * @property isStorageOverQuota                         Storage quota over limits
 * @property isNodeUpdated                              Checks if incoming shares are updated
 * @property isCopyInProgress                           Checks if folder copy in progress
 * @property nameCollisions                             Checks if copy has name collisions
 * @property isTransferComplete                         checks if file transfer complete
 * @property copyError                                  Copy node error
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
    val snackBarMessageString: String? = null,
    val isUserRemoved: Boolean = false,
    val callStatusChanged: Boolean = false,
    val isPushNotificationSettingsUpdated: Boolean = false,
    val shouldNavigateToChat: Boolean = false,
    val isChatNotificationChange: Boolean = false,
    val isStorageOverQuota: Boolean = false,
    val isNodeUpdated: Boolean = false,
    val isCopyInProgress: Boolean = false,
    val isTransferComplete: Boolean = false,
    val nameCollisions: List<NameCollision> = emptyList(),
    val copyError: Throwable? = null,
    val inShares: List<UnTypedNode> = emptyList(),
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
