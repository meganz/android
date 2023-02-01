package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.account.AccountSession

/**
 * Use case for saving in data base the user credentials of the current logged in account.
 */
fun interface SaveAccountCredentials {

    /**
     * Invoke.
     */
    suspend operator fun invoke(): AccountSession
}