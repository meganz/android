package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow

/**
 * Is chat logged in
 *
 */
interface IsChatLoggedIn {
    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(): Flow<Boolean>
}
