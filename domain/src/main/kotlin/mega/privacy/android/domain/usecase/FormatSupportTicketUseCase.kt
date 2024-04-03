package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SupportTicket
import javax.inject.Inject

/**
 * Format support ticket
 *
 */
class FormatSupportTicketUseCase @Inject constructor() {
    /**
     * Invoke
     *
     * @param ticket
     * @return
     */
    operator fun invoke(ticket: SupportTicket) = """
        ${ticket.description}
        
        Report filename: ${ticket.logFileName ?: "No log file"}
                    
        Account Information:
        Email: ${ticket.accountEmail}
        Type: ${ticket.accountType}
        
        AppInformation:
        App name: Mega
        App version: ${ticket.androidAppVersion}
        Sdk version: ${ticket.sdkVersion}
        
        Device Information:
        Device: ${ticket.device}
        Android Version: ${ticket.deviceSdkVersionName} - ${ticket.deviceSdkVersionInt}
        Language: ${ticket.currentLanguage}
    """.trimIndent()
}
