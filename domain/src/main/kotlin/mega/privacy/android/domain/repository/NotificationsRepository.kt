package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.ContactRequest
import mega.privacy.android.domain.entity.Event
import mega.privacy.android.domain.entity.UserAlert

/**
 * Notification repository.
 */
interface NotificationsRepository {

    /**
     * Monitor contact request updates.
     *
     * @return A flow of all global contact request updates.
     */
    fun monitorContactRequestUpdates(): Flow<List<ContactRequest>>

    /**
     * Monitor user alerts
     *
     * @return a flow of all global user alerts
     */
    fun monitorUserAlerts(): Flow<List<UserAlert>>

    /**
     * Monitor events
     *
     * @return a flow of global [Event]
     */
    fun monitorEvent(): Flow<Event>

    /**
     * Get user alerts
     *
     * @return list of current user alerts
     */
    suspend fun getUserAlerts(): List<UserAlert>

    /**
     * Acknowledge user alerts have been seen
     */
    suspend fun acknowledgeUserAlerts()

}