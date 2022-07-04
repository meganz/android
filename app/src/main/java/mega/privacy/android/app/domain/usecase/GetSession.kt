package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.data.model.UserCredentials

/**
 * Checks if user credentials exists.
 */
fun interface GetSession {
    /**
     * Invoke
     *
     * @return [UserCredentials] if exists.
     */
    suspend operator fun invoke(): String?
}