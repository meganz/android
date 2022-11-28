package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow

/**
 * Monitor media discovery view setting
 */
fun interface MonitorMediaDiscoveryView {

    /**
     * Invoke
     *
     * @return flow of changes of setting
     */
    operator fun invoke(): Flow<Int?>
}