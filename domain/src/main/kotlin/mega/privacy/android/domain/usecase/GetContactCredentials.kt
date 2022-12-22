package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.contacts.AccountCredentials

/**
 * Gets contact's credentials.
 */
fun interface GetContactCredentials {

    /**
     * Invoke.
     *
     * @param userEmail User's email.
     * @return [AccountCredentials.ContactCredentials]
     */
    suspend operator fun invoke(userEmail: String): AccountCredentials.ContactCredentials?
}