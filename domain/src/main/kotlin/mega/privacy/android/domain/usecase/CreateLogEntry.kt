package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.logging.CreateLogEntryRequest
import mega.privacy.android.domain.entity.logging.LogEntry

/**
 * Create a log entry
 *
 */
fun interface CreateLogEntry {
    /**
     * Invoke
     *
     * @param request
     * @return The appropriate log entry or null
     */
    suspend operator fun invoke(request: CreateLogEntryRequest): LogEntry?
}