package mega.privacy.android.data.mapper.login

import mega.privacy.android.domain.entity.user.UserCredentials
import javax.inject.Inject

/**
 * Mapper to convert data into [UserCredentials]
 */
internal class UserCredentialsMapper @Inject constructor() {

    /**
     * Invoke.
     *
     * @param email User email.
     * @param session Account session.
     * @param firstName User first name.
     * @param lastName User last name.
     * @param myHandle User handle.
     */
    operator fun invoke(
        email: String?,
        session: String?,
        firstName: String?,
        lastName: String?,
        myHandle: String?,
    ) = UserCredentials(
        email = email,
        session = session,
        firstName = firstName,
        lastName = lastName,
        myHandle = myHandle
    )
}