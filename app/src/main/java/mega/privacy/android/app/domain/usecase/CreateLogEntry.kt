package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.entity.logging.CreateLogEntryRequest
import mega.privacy.android.app.domain.entity.logging.LogEntry

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