package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.UserCredentials

/**
 * Checks if user credentials exists.
 */
interface GetCredentials {
    /**
     * Invoke
     *
     * @return [UserCredentials] if exists.
     */
    suspend operator fun invoke(): UserCredentials?
}