package mega.privacy.android.domain.usecase.logging

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.LoggingRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetZippedLogsUseCaseTest {

    private lateinit var underTest: GetZippedLogsUseCase
    private val loggingRepository: LoggingRepository = Mockito.mock(LoggingRepository::class.java)

    @BeforeEach
    fun setup() {
        underTest = GetZippedLogsUseCase(loggingRepository)
    }

    @Test
    fun `invoke returns compressed logs`() = runTest {
        val expectedLogs = File("compressed logs")
        whenever(loggingRepository.compressLogs()).thenReturn(expectedLogs)

        val actualLogs = underTest.invoke()

        assertEquals(expectedLogs, actualLogs)
    }

    @Test
    fun `invoke throws exception when compressLogs fails`() = runTest {
        whenever(loggingRepository.compressLogs()).thenAnswer { throw Exception("Test exception") }

        assertThrows<Exception> {
            underTest.invoke()
        }
    }
}