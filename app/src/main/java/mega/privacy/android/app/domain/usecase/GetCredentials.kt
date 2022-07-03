package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.entity.user.UserCredentials

/**
 * Checks if user credentials exists.
 */
fun interface GetCredentials {
    /**
     * Invoke
     *
     * @return [UserCredentials] if exists.
     */
    suspend operator fun invoke(): UserCredentials?
}