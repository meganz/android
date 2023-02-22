package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.UserAlert

/**
 * Monitor user alerts
 *
 */
fun interface MonitorUserAlerts {
    /**
     * Invoke
     *
     * @return user alerts as a flow
     */
    suspend operator fun invoke(): Flow<List<UserAlert>>
}