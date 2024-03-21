package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.user.UserVisibility
import javax.inject.Inject

/**
 * A use case to determine whether the given email already exists in MEGA contacts
 */
class IsAMegaContactByEmailUseCase @Inject constructor() {

    /**
     * Invocation method for this use case
     */
    operator fun invoke(user: User, email: String): Boolean {
        val hasSameEmail = user.email == email
        val isContactVisible = user.visibility == UserVisibility.Visible
        return hasSameEmail && isContactVisible
    }
}
