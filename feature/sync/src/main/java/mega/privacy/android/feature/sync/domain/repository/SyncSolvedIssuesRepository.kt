package mega.privacy.android.feature.sync.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue

/**
 * Repository for solved stalled issues
 *
 * This repository is used to monitor and store the solved stalled issues
 */
interface SyncSolvedIssuesRepository {

    /**
     * Monitors the solved stalled issues
     */
    fun monitorSolvedIssues(): Flow<List<SolvedIssue>>

    /**
     * Inserts a solved stalled issue
     */
    suspend fun insertSolvedIssues(solvedIssues: SolvedIssue)

    /**
     * Clears all solved stalled issues
     */
    suspend fun clear()

    /**
     * Clears all solved stalled issues related to a sync by syncId
     */
    suspend fun removeBySyncId(syncId: Long)
}