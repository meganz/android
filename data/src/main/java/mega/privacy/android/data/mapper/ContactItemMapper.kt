package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import nz.mega.sdk.MegaUser

/**
 * Mapper for converting [MegaUser] into [ContactItem]
 */
typealias ContactItemMapper = (
    @JvmSuppressWildcards MegaUser,
    @JvmSuppressWildcards ContactData,
    @JvmSuppressWildcards String,
    @JvmSuppressWildcards Boolean,
    @JvmSuppressWildcards Int,
    @JvmSuppressWildcards Int?,
) -> @JvmSuppressWildcards ContactItem

internal fun toContactItem(
    user: MegaUser,
    contactData: ContactData,
    defaultAvatarColor: String,
    areCredentialsVerified: Boolean,
    status: Int,
    lastSeen: Int?,
) = ContactItem(
    handle = user.handle,
    email = user.email,
    contactData = contactData,
    defaultAvatarColor = defaultAvatarColor,
    visibility = userVisibility[user.visibility] ?: UserVisibility.Unknown,
    timestamp = user.timestamp,
    areCredentialsVerified = areCredentialsVerified,
    status = userStatus[status] ?: UserStatus.Invalid,
    lastSeen = lastSeen
)

/**
 * User visibility
 */
val userVisibility = mapOf(
    MegaUser.VISIBILITY_UNKNOWN to UserVisibility.Unknown,
    MegaUser.VISIBILITY_HIDDEN to UserVisibility.Hidden,
    MegaUser.VISIBILITY_VISIBLE to UserVisibility.Visible,
    MegaUser.VISIBILITY_INACTIVE to UserVisibility.Inactive,
    MegaUser.VISIBILITY_BLOCKED to UserVisibility.Blocked,
)