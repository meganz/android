package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import nz.mega.sdk.MegaNode

/**
 * Monitor global node updates for the current logged in user
 */
interface MonitorNodeUpdates {
    /**
     * Invoke
     *
     * @return a flow of changes
     */
    operator fun invoke(): Flow<List<MegaNode>>
}
