package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.entity.SupportTicket

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
