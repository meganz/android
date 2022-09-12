package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.Event

/**
 * Monitor global [Event] updates
 */
fun interface MonitorEvent {
    /**
     * Invoke
     *
     * @return a flow of changes
     */
    operator fun invoke(): Flow<Event>
}