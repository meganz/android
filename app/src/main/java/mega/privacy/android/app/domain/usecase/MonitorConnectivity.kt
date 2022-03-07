package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow


/**
 * Monitor connectivity
 *
 */
interface MonitorConnectivity {
    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(): Flow<Boolean>
}
