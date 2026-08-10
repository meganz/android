package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.LoggingRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@ExperimentalCoroutinesApi
class InitialiseLoggingUseCaseTest {
    private lateinit var underTest: InitialiseLoggingUseCase

    private val loggingRepository = mock<LoggingRepository>()

    @Before
    fun setUp() {
        underTest = InitialiseLoggingUseCase(loggingRepository = loggingRepository)
    }

    @Test
    fun `test that invoke initialises the logging repository`() = runTest {
        underTest()

        verify(loggingRepository, times(1)).initialise()
        verifyNoMoreInteractions(loggingRepository)
    }
}
