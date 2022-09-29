package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.StateFlow


/**
 * Monitor connectivity
 *
 */
fun interface MonitorConnectivity {
    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(): StateFlow<Boolean>
}
