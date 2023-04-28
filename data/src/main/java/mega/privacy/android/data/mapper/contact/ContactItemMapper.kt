package mega.privacy.android.data.mapper.contact

import mega.privacy.android.data.mapper.chat.OnlineStatusMapper.Companion.userStatus
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import nz.mega.sdk.MegaUser
import javax.inject.Inject

/**
 * Mapper for converting [MegaUser] into [ContactItem]
 */
internal class ContactItemMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param megaUser [MegaUser]
     * @param contactData [ContactData]
     * @param defaultAvatarColor [String]
     * @param areCredentialsVerified [Boolean]
     * @param status [Int]
     * @param lastSeen nullable [Int]
     *
     * @return the [ContactItem] of [megaUser]
     */
    operator fun invoke(
        megaUser: MegaUser,
        contactData: ContactData,
        defaultAvatarColor: String?,
        areCredentialsVerified: Boolean,
        status: Int,
        lastSeen: Int?,
    ) = ContactItem(
        handle = megaUser.handle,
        email = megaUser.email,
        contactData = contactData,
        defaultAvatarColor = defaultAvatarColor,
        visibility = userVisibility[megaUser.visibility] ?: UserVisibility.Unknown,
        timestamp = megaUser.timestamp,
        areCredentialsVerified = areCredentialsVerified,
        status = userStatus[status] ?: UserStatus.Invalid,
        lastSeen = lastSeen
    )

    companion object {
        internal val userVisibility = mapOf(
            MegaUser.VISIBILITY_UNKNOWN to UserVisibility.Unknown,
            MegaUser.VISIBILITY_HIDDEN to UserVisibility.Hidden,
            MegaUser.VISIBILITY_VISIBLE to UserVisibility.Visible,
            MegaUser.VISIBILITY_INACTIVE to UserVisibility.Inactive,
            MegaUser.VISIBILITY_BLOCKED to UserVisibility.Blocked,
        )
    }
}