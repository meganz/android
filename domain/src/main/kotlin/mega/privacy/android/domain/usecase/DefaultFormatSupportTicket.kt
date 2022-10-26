package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SupportTicket
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
            Android Version: ${ticket.deviceSdkVersionName} - ${ticket.deviceSdkVersionInt}
            Language: ${ticket.currentLanguage}
        """.trimIndent()
    }

}
