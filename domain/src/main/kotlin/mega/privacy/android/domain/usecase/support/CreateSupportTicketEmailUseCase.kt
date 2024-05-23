package mega.privacy.android.domain.usecase.support

import mega.privacy.android.domain.entity.support.SupportEmailTicket
import mega.privacy.android.domain.usecase.CreateSupportTicketUseCase
import mega.privacy.android.domain.usecase.FormatSupportTicketUseCase
import mega.privacy.android.domain.usecase.GetSupportEmailUseCase
import mega.privacy.android.domain.usecase.logging.GetZippedLogsUseCase
import javax.inject.Inject

/**
 * Create support ticket email use case
 *
 * @property getSupportEmailUseCase
 * @property getZippedLogsUseCase
 * @property createSupportTicketUseCase
 * @property formatSupportTicketUseCase
 */
class CreateSupportTicketEmailUseCase @Inject constructor(
    private val getSupportEmailUseCase: GetSupportEmailUseCase,
    private val getZippedLogsUseCase: GetZippedLogsUseCase,
    private val createSupportTicketUseCase: CreateSupportTicketUseCase,
    private val formatSupportTicketUseCase: FormatSupportTicketUseCase,
) {

    /**
     * Invoke
     *
     * @param emailBody
     */
    suspend operator fun invoke(
        emailBody: String,
        includeLogs: Boolean = true,
    ): SupportEmailTicket {
        val email = getSupportEmailUseCase()
        val logs = if (includeLogs) runCatching { getZippedLogsUseCase() }.getOrNull() else null
        val logFileName = if (includeLogs) logs?.name ?: "No logs found" else null
        val formattedTicket = getFormattedTicket(emailBody, logFileName)

        return SupportEmailTicket(
            email = email,
            ticket = formattedTicket,
            logs = logs,
            subject = "Android Login Issue"
        )
    }


    private suspend fun CreateSupportTicketEmailUseCase.getFormattedTicket(
        emailBody: String,
        logFileName: String?,
    ) = formatSupportTicketUseCase(
        ticket = createTicket(emailBody, logFileName)
    )

    private suspend fun CreateSupportTicketEmailUseCase.createTicket(
        emailBody: String,
        logFileName: String?,
    ) = createSupportTicketUseCase(
        description = buildString {
            append(emailBody)
            repeat(5) { appendLine() }
        },
        logFileName = logFileName,
        accountDetails = null,
    )
}
