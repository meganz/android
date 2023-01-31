package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.user.UserCredentials

/**
 * Use case for getting the credentials of the current logged in account.
 */
fun interface GetAccountCredentials {

    /**
     * Invoke.
     *
     * @return [UserCredentials]
     */
    suspend operator fun invoke(): UserCredentials?
}