package mega.privacy.android.domain.usecase.support

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SupportTicket
import mega.privacy.android.domain.usecase.CreateSupportTicketUseCase
import mega.privacy.android.domain.usecase.FormatSupportTicketUseCase
import mega.privacy.android.domain.usecase.GetSupportEmailUseCase
import mega.privacy.android.domain.usecase.logging.GetZippedLogsUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

class CreateSupportTicketEmailUseCaseTest {
    private lateinit var underTest: CreateSupportTicketEmailUseCase

    private val getSupportEmailUseCase = mock<GetSupportEmailUseCase>()
    private val getZippedLogsUseCase = mock<GetZippedLogsUseCase>()
    private val createSupportTicketUseCase = mock<CreateSupportTicketUseCase>()
    private val formatSupportTicketUseCase = mock<FormatSupportTicketUseCase>()

    private val emailBody = "Test email body"
    private val email = "test@mega.nz"
    private val logs = File("testLogs.zip")
    private val ticket = mock<SupportTicket>()
    private val formattedTicket = "Formatted test ticket"

    @BeforeEach
    fun setUp() {
        underTest = CreateSupportTicketEmailUseCase(
            getSupportEmailUseCase = getSupportEmailUseCase,
            getZippedLogsUseCase = getZippedLogsUseCase,
            createSupportTicketUseCase = createSupportTicketUseCase,
            formatSupportTicketUseCase = formatSupportTicketUseCase,
        )
    }

    @Test
    fun `test that ticket is created correctly`() = runTest {
        whenever(getSupportEmailUseCase()).thenReturn(email)
        whenever(getZippedLogsUseCase()).thenReturn(logs)
        whenever(createSupportTicketUseCase(any(), any(), eq(null))).thenReturn(ticket)
        whenever(formatSupportTicketUseCase(ticket)).thenReturn(formattedTicket)

        val result = underTest(emailBody, true)

        assertThat(result.email).isEqualTo(email)
        assertThat(result.ticket).isEqualTo(formattedTicket)
        assertThat(result.logs).isEqualTo(logs)
        verify(getZippedLogsUseCase).invoke()
    }

    @Test
    fun `test that log is not fetched when includeLogs is set to false`() = runTest {
        whenever(getSupportEmailUseCase()).thenReturn(email)
        whenever(createSupportTicketUseCase(any(), eq(null), eq(null))).thenReturn(ticket)
        whenever(formatSupportTicketUseCase(ticket)).thenReturn(formattedTicket)

        underTest(emailBody, false)
        verifyNoInteractions(getZippedLogsUseCase)
    }
}