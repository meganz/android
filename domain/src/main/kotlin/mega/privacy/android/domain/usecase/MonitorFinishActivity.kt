package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow

/**
 * Monitor Finish Activity
 *
 */
fun interface MonitorFinishActivity {
    /**
     * Invoke
     *
     * @return Flow
     */
    operator fun invoke(): Flow<Boolean>
}