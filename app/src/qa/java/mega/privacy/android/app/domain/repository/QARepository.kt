package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.Progress

/**
 * Qa repository
 *
 * Provides QA related functionality
 */
interface QARepository {
    /**
     * Update app from QA distribution channel
     *
     * @return Download progress as a float
     */
    fun updateApp(): Flow<Progress>

}