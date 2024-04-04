package mega.privacy.android.domain.usecase.support

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AppInfo
import mega.privacy.android.domain.entity.SupportTicket
import mega.privacy.android.domain.repository.EnvironmentRepository
import mega.privacy.android.domain.usecase.CreateSupportTicketUseCase
import mega.privacy.android.domain.usecase.FormatSupportTicketUseCase
import mega.privacy.android.domain.usecase.GetSupportEmailUseCase
import mega.privacy.android.domain.usecase.logging.GetZippedLogsUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

class CreateSupportTicketEmailUseCaseTest {
    private lateinit var underTest: CreateSupportTicketEmailUseCase

    private val getSupportEmailUseCase = mock<GetSupportEmailUseCase>()
    private val getZippedLogsUseCase = mock<GetZippedLogsUseCase>()
    private val createSupportTicketUseCase = mock<CreateSupportTicketUseCase>()
    private val formatSupportTicketUseCase = mock<FormatSupportTicketUseCase>()
    private val environmentRepository = mock<EnvironmentRepository>()

    @BeforeEach
    fun setUp() {
        underTest = CreateSupportTicketEmailUseCase(
            getSupportEmailUseCase = getSupportEmailUseCase,
            getZippedLogsUseCase = getZippedLogsUseCase,
            createSupportTicketUseCase = createSupportTicketUseCase,
            formatSupportTicketUseCase = formatSupportTicketUseCase,
            environmentRepository = environmentRepository,
        )
    }

    @Test
    fun `test that ticket is created correctly`() = runTest {
        val emailBody = "Test email body"
        val email = "test@mega.nz"
        val logs = File("testLogs.zip")
        val ticket = mock<SupportTicket>()
        val formattedTicket = "Formatted test ticket"
        val appVersion = "6.0.0"
        val versionCode = 1

        whenever(getSupportEmailUseCase()).thenReturn(email)
        whenever(getZippedLogsUseCase()).thenReturn(logs)
        whenever(createSupportTicketUseCase(any(), any(), eq(null))).thenReturn(ticket)
        whenever(formatSupportTicketUseCase(ticket)).thenReturn(formattedTicket)
        whenever(environmentRepository.getAppInfo()).thenReturn(AppInfo(appVersion, ""))
        whenever(environmentRepository.getInstalledVersionCode()).thenReturn(versionCode)

        val result = underTest(emailBody)

        assertThat(result.email).isEqualTo(email)
        assertThat(result.ticket).isEqualTo(formattedTicket)
        assertThat(result.logs).isEqualTo(logs)
        assertThat(result.subject).contains(appVersion)
        assertThat(result.subject).contains(versionCode.toString())
    }
}