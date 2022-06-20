package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import nz.mega.sdk.MegaContactRequest

/**
 * Monitor global contact request updates for the current logged in user
 */
fun interface MonitorContactRequestUpdates {
    /**
     * Invoke
     *
     * @return a flow of changes
     */
    operator fun invoke(): Flow<List<MegaContactRequest>>
}