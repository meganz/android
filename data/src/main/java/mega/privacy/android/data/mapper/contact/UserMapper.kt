package mega.privacy.android.data.mapper.contact

import mega.privacy.android.domain.entity.contacts.User
import nz.mega.sdk.MegaUser
import javax.inject.Inject

/**
 * Map a MegaUser to a [User]
 *
 */
class UserMapper @Inject constructor(
    private val userChangeMapper: UserChangeMapper,
    private val userVisibilityMapper: UserVisibilityMapper,
) {

    /**
     * invoke
     *
     * @param megaUser [MegaUser]
     * @return [User]
     */
    operator fun invoke(megaUser: MegaUser): User =
        User(
            handle = megaUser.handle,
            email = megaUser.email,
            visibility = userVisibilityMapper(megaUser),
            timestamp = megaUser.timestamp,
            userChanges = userChangeMapper(megaUser.changes)
        )

}