package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

/**
 * Test class for [ReportUploadFinishedUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ReportUploadFinishedUseCaseTest {

    private lateinit var underTest: ReportUploadFinishedUseCase

    private val reportUploadStatusUseCase = mock<ReportUploadStatusUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = ReportUploadFinishedUseCase(
            reportUploadStatusUseCase = reportUploadStatusUseCase
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(reportUploadStatusUseCase)
    }

    @Test
    internal fun `test that report upload finished is reporting primary and secondary heartbeats`() =
        runTest {
            underTest(
                lastPrimaryNodeHandle = 1L,
                lastSecondaryNodeHandle = 2L,
                lastPrimaryTimestamp = 1L,
                lastSecondaryTimestamp = 1L,
            )
            verify(reportUploadStatusUseCase, times(2)).invoke(
                any(), any(), any(), any(), any()
            )
        }
}
