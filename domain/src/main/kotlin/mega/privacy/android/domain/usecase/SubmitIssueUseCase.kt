package mega.privacy.android.domain.usecase

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.SubmitIssueRequest
import mega.privacy.android.domain.repository.SupportRepository
import mega.privacy.android.domain.usecase.logging.GetZippedLogsUseCase
import javax.inject.Inject

/**
 * SubmitIssueUseCase
 *
 * @property supportRepository
 * @property createSupportTicketUseCase
 * @property formatSupportTicketUseCase
 * @property getZippedLogsUseCase
 */
class SubmitIssueUseCase @Inject constructor(
    private val supportRepository: SupportRepository,
    private val createSupportTicketUseCase: CreateSupportTicketUseCase,
    private val formatSupportTicketUseCase: FormatSupportTicketUseCase,
    private val getZippedLogsUseCase: GetZippedLogsUseCase,
) {
    /**
     * Invoke
     *
     * @param request
     * @return
     */
    suspend operator fun invoke(request: SubmitIssueRequest): Flow<Progress> {
        return flow {
            val logFileName = uploadLogs(request.includeLogs)
            if (currentCoroutineContext().isActive) {
                val formattedTicket = createFormattedSupportTicket(request.description, logFileName)
                supportRepository.logTicket(formattedTicket)
            }
        }
    }

    private suspend fun FlowCollector<Progress>.uploadLogs(
        includeLogs: Boolean,
    ): String? {
        val logs = if (shouldCompressLogs(includeLogs)) getZippedLogsUseCase() else null
        logs?.let {
            emitAll(
                supportRepository.uploadFile(it)
                    .map { percentage -> Progress(percentage) }
            )
        }
        return logs?.name
    }

    private suspend fun shouldCompressLogs(includeLogs: Boolean) =
        includeLogs && currentCoroutineContext().isActive

    private suspend fun createFormattedSupportTicket(
        description: String,
        logFileName: String?,
    ) = formatSupportTicketUseCase(
        createSupportTicketUseCase(description, logFileName)
    )

}