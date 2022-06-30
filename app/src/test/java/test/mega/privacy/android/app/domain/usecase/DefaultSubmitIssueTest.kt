package test.mega.privacy.android.app.domain.usecase

import androidx.test.filters.Suppress
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.entity.Progress
import mega.privacy.android.app.domain.entity.SubmitIssueRequest
import mega.privacy.android.app.domain.repository.LoggingRepository
import mega.privacy.android.app.domain.repository.SupportRepository
import mega.privacy.android.app.domain.usecase.CreateSupportTicket
import mega.privacy.android.app.domain.usecase.DefaultSubmitIssue
import mega.privacy.android.app.domain.usecase.FormatSupportTicket
import mega.privacy.android.app.domain.usecase.SubmitIssue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.*
import test.mega.privacy.android.app.TEST_SUPPORT_TICKET
import java.io.File

@ExperimentalCoroutinesApi
class DefaultSubmitIssueTest {
    private lateinit var underTest: SubmitIssue

    private val compressedLogs = File("path/to/${TEST_SUPPORT_TICKET.logFileName}")
    private val percentage = 1F
    private val formattedSupportTicket = "formattedSupportTicket"

    private val loggingRepository = mock<LoggingRepository> {
        onBlocking { compressLogs() }.thenReturn(compressedLogs)
    }

    private val supportRepository = mock<SupportRepository> {
        on { uploadFile(compressedLogs) }.thenReturn(flowOf(percentage))
    }

    private val createSupportTicket = mock<CreateSupportTicket> {
        onBlocking {
            invoke(TEST_SUPPORT_TICKET.description, TEST_SUPPORT_TICKET.logFileName)
        }.thenReturn(TEST_SUPPORT_TICKET)
    }

    private val formatSupportTicket = mock<FormatSupportTicket> {
        on {
            invoke(any())
        }.thenReturn(formattedSupportTicket)
    }

    @Before
    fun setUp() {
        Mockito.clearInvocations(
                loggingRepository,
                supportRepository,
                createSupportTicket,
                formatSupportTicket,
        )

        underTest = DefaultSubmitIssue(
                loggingRepository = loggingRepository,
                supportRepository = supportRepository,
                createSupportTicket = createSupportTicket,
                formatSupportTicket = formatSupportTicket,
        )
    }


    @Test
    fun `test that logs are compressed when include logs is set to true`() = runTest {
        underTest.call(true).test { cancelAndIgnoreRemainingEvents() }
        verify(loggingRepository).compressLogs()
    }

    @Test
    fun `test that logs are not compressed when include logs is set to false`() = runTest {
        underTest.call(false).test { cancelAndIgnoreRemainingEvents() }
        verify(loggingRepository, never()).compressLogs()
    }

    @Test
    fun `test that logs are uploaded after compressing`() = runTest {
        underTest.call(true).test { cancelAndIgnoreRemainingEvents() }
        verify(supportRepository).uploadFile(compressedLogs)
    }

    @Test
    fun `test that file upload progress is returned`() = runTest {
        underTest.call(true).test {
            assertThat(awaitItem()).isEqualTo(Progress(percentage))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that support ticket is created`() = runTest {
        underTest.call(false).test { cancelAndIgnoreRemainingEvents() }

        verify(createSupportTicket).invoke(TEST_SUPPORT_TICKET.description, null)
    }

    @Test
    fun `test that support ticket is created with log file name if include logs is set to true`() =
            runTest {
                val compressedLogs = File("log/path/file.zip")
                whenever(loggingRepository.compressLogs()).thenReturn(compressedLogs)
                whenever(supportRepository.uploadFile(any())).thenReturn(emptyFlow())

                underTest.call(true).test { cancelAndIgnoreRemainingEvents() }

                verify(createSupportTicket).invoke(TEST_SUPPORT_TICKET.description, compressedLogs.name)
            }

    @Test
    fun `test that ticket is formatted`() = runTest {

        underTest.call(true).test { cancelAndIgnoreRemainingEvents() }

        verify(formatSupportTicket).invoke(TEST_SUPPORT_TICKET)
    }

    @Test
    fun `test that formatted ticket is submitted`() = runTest {
        underTest(
                SubmitIssueRequest(
                        description = TEST_SUPPORT_TICKET.description,
                        includeLogs = true
                )
        ).test { cancelAndIgnoreRemainingEvents() }

        verify(supportRepository).logTicket(formattedSupportTicket)
    }

    @Test
    fun `test that ticket is not created if cancelled`() = runTest {
        underTest.call(true).first()

        verify(loggingRepository).compressLogs()
        verify(formatSupportTicket, never()).invoke(any())
        verify(supportRepository, never()).logTicket(any())
    }

    private suspend fun SubmitIssue.call(includeLogs: Boolean) = invoke(
            SubmitIssueRequest(
                    description = TEST_SUPPORT_TICKET.description,
                    includeLogs = includeLogs
            )
    )
}