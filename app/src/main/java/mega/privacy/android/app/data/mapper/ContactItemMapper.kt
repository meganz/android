package mega.privacy.android.app.data.mapper

import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import nz.mega.sdk.MegaUser

/**
 * Mapper for converting [MegaUser] into [ContactItem]
 */
typealias ContactItemMapper = (
    @JvmSuppressWildcards MegaUser,
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards String,
    @JvmSuppressWildcards String,
    @JvmSuppressWildcards Boolean,
    @JvmSuppressWildcards Int,
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards String?,
) -> @JvmSuppressWildcards ContactItem

internal fun toContactItem(
    user: MegaUser,
    fullName: String?,
    alias: String?,
    defaultAvatarContent: String,
    defaultAvatarColor: String,
    areCredentialsVerified: Boolean,
    status: Int,
    avatarUri: String?,
    lastSeen: String?,
) = ContactItem(
    handle = user.handle,
    email = user.email,
    fullName = fullName,
    alias = alias,
    defaultAvatarContent = defaultAvatarContent,
    defaultAvatarColor = defaultAvatarColor,
    visibility = userVisibility[user.visibility] ?: UserVisibility.Unknown,
    timestamp = user.timestamp,
    areCredentialsVerified = areCredentialsVerified,
    status = userStatus[status] ?: UserStatus.Invalid,
    avatarUri = avatarUri,
    lastSeen = lastSeen
)

private val userVisibility = mapOf(
    MegaUser.VISIBILITY_UNKNOWN to UserVisibility.Unknown,
    MegaUser.VISIBILITY_HIDDEN to UserVisibility.Hidden,
    MegaUser.VISIBILITY_VISIBLE to UserVisibility.Visible,
    MegaUser.VISIBILITY_INACTIVE to UserVisibility.Inactive,
    MegaUser.VISIBILITY_BLOCKED to UserVisibility.Blocked,
)