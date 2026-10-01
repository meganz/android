package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow

/**
 * Is chat logged in
 *
 */
fun interface IsChatLoggedIn {
    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(): Flow<Boolean>
}
