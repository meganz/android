package mega.privacy.android.data.mapper.login

import mega.privacy.android.domain.entity.account.AccountSession
import javax.inject.Inject

/**
 * Mapper to convert data into [AccountSession].
 */
internal class AccountSessionMapper @Inject constructor() {

    /**
     * Invoke.
     *
     * @param email Account email.
     * @param session Account session.
     * @param myHandle Account handle.
     */
    operator fun invoke(email: String?, session: String?, myHandle: Long?) =
        AccountSession(email = email, session = session, myHandle = myHandle ?: -1)
}