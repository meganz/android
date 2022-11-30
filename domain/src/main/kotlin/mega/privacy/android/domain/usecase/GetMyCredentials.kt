package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.contacts.AccountCredentials

/**
 * Gets my credentials.
 */
fun interface GetMyCredentials {

    /**
     * Invoke.
     *
     * @return [AccountCredentials.MyAccountCredentials]
     */
    suspend operator fun invoke(): AccountCredentials.MyAccountCredentials?
}