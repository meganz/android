package mega.privacy.android.data.mapper.contact

import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.user.UserVisibility
import nz.mega.sdk.MegaUser
import javax.inject.Inject

/**
 * Map a MegaUser to a [User]
 *
 */
class UserMapper @Inject constructor(
    private val userChangeMapper: UserChangeMapper,
) {

    /**
     * invoke
     *
     * @param megaUser [MegaUser]
     * @return [User]
     */
    operator fun invoke(megaUser: MegaUser?): User? =
        megaUser?.let {
            User(
                handle = megaUser.handle,
                email = megaUser.email,
                visibility = userVisibility[megaUser.visibility] ?: UserVisibility.Unknown,
                timestamp = megaUser.timestamp,
                userChanges = userChangeMapper(megaUser.changes)
            )
        }

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