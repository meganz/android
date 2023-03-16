package mega.privacy.android.data.mapper.login

import mega.privacy.android.domain.entity.user.UserCredentials

/**
 * Mapper to convert data into [UserCredentials]
 */
internal fun interface UserCredentialsMapper {

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
    ): UserCredentials
}