package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CallRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StartVideoDeviceUseCaseTest {
    private val callRepository: CallRepository = mock()

    private lateinit var underTest: StartVideoDeviceUseCase

    @BeforeEach
    fun setup() {
        underTest = StartVideoDeviceUseCase(callRepository)
    }

    @Test
    fun `test that openVideoDevice is called when start is true`() = runTest {
        underTest(true)
        verify(callRepository).openVideoDevice()
        verifyNoMoreInteractions(callRepository)
    }

    @Test
    fun `test that releaseVideoDevice is called when start is false`() = runTest {
        underTest(false)
        verify(callRepository).releaseVideoDevice()
        verifyNoMoreInteractions(callRepository)
    }
}
