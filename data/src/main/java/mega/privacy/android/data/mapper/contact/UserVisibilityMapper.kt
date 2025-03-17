package mega.privacy.android.data.mapper.contact

import mega.privacy.android.domain.entity.user.UserVisibility
import nz.mega.sdk.MegaUser
import javax.inject.Inject

class UserVisibilityMapper @Inject constructor() {
    operator fun invoke(user: MegaUser?) = when (user?.visibility) {
        MegaUser.VISIBILITY_HIDDEN -> UserVisibility.Hidden
        MegaUser.VISIBILITY_VISIBLE -> UserVisibility.Visible
        MegaUser.VISIBILITY_INACTIVE -> UserVisibility.Inactive
        MegaUser.VISIBILITY_BLOCKED -> UserVisibility.Blocked
        else -> UserVisibility.Unknown
    }
}