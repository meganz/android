package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.flow.Flow

/**
 * Use case for monitoring fetch nodes finish.
 */
fun interface MonitorFetchNodesFinish {

    /**
     * Invoke.
     *
     * @return Flow of Boolean.
     */
    operator fun invoke(): Flow<Boolean>
}