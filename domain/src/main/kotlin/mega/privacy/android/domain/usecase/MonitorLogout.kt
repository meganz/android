package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow

/**
 * Use case for monitoring logouts.
 */
fun interface MonitorLogout {

    /**
     * Invoke.
     *
     * @return Flow of Boolean.
     */
    operator fun invoke(): Flow<Boolean>
}