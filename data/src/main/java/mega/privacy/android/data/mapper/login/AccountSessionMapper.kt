package mega.privacy.android.data.mapper.login

import mega.privacy.android.domain.entity.account.AccountSession

/**
 * Mapper to convert data into [AccountSession].
 */
internal fun interface AccountSessionMapper {

    /**
     * Invoke.
     *
     * @param email Account email.
     * @param session Account session.
     * @param myHandle Account handle.
     */
    operator fun invoke(email: String?, session: String?, myHandle: Long?): AccountSession
}