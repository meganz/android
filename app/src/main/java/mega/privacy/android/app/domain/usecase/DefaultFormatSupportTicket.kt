package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.entity.SupportTicket
import javax.inject.Inject

/**
 * Default format support ticket
 *
 */
class DefaultFormatSupportTicket @Inject constructor() : FormatSupportTicket {
    override fun invoke(ticket: SupportTicket): String {
        return """
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
            Language: ${ticket.currentLanguage}
        """.trimIndent()
    }

}
