package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SupportTicket

/**
 * Format support ticket
 *
 */
interface FormatSupportTicket {
    /**
     * Invoke
     *
     * @param ticket
     * @return
     */
    operator fun invoke(ticket: SupportTicket): String
}
