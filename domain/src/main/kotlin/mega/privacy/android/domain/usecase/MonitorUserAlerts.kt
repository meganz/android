package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.UserAlert

/**
 * Monitor global user alerts for the current logged in user
 */
fun interface MonitorUserAlerts {
    /**
     * Invoke
     *
     * @return a flow of alerts
     */
    operator fun invoke(): Flow<List<UserAlert>>
}
