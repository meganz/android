package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.entity.SupportTicket

/**
 * Create support ticket
 *
 */
interface CreateSupportTicket {
    /**
     * Invoke
     *
     * @param description
     * @param logFileName
     * @return A support ticket
     */
    suspend operator fun invoke(
        description: String,
        logFileName: String?
    ): SupportTicket
}