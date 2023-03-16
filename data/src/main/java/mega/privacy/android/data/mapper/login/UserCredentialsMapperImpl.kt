package mega.privacy.android.data.mapper.login

import mega.privacy.android.domain.entity.user.UserCredentials
import javax.inject.Inject

/**
 * Default implementation of [UserCredentialsMapper].
 */
internal class UserCredentialsMapperImpl @Inject constructor() : UserCredentialsMapper {

    override fun invoke(
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