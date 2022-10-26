package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow

/**
 * Monitor hide recent activity setting
 */
fun interface MonitorHideRecentActivity {
    /**
     * Invoke
     *
     * @return flow of changes to the setting
     */
    operator fun invoke(): Flow<Boolean>
}
