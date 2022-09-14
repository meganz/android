package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.contacts.ContactRequest

/**
 * Monitor global contact request updates for the current logged in user
 */
fun interface MonitorContactRequestUpdates {
    /**
     * Invoke
     *
     * @return a flow of changes
     */
    operator fun invoke(): Flow<List<ContactRequest>>
}