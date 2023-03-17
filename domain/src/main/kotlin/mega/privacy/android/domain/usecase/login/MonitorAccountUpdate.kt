package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.flow.Flow

/**
 * Use case for monitoring an account update.
 */
fun interface MonitorAccountUpdate {

    /**
     * Invoke.
     *
     * @return Flow of Boolean.
     */
    operator fun invoke(): Flow<Boolean>
}