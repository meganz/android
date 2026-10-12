package mega.privacy.android.domain.usecase.call

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CallAudioRouteRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EndCallAudioSessionUseCaseTest {

    private lateinit var underTest: EndCallAudioSessionUseCase

    private val callAudioRouteRepository = mock<CallAudioRouteRepository>()

    @BeforeAll
    fun setUp() {
        underTest = EndCallAudioSessionUseCase(
            callAudioRouteRepository = callAudioRouteRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(callAudioRouteRepository)
    }

    @Test
    fun `test that invoke ends the session in the repository`() = runTest {
        underTest()

        verify(callAudioRouteRepository).endSession()
    }

    @Test
    fun `test that invoke throws when the repository fails`() = runTest {
        whenever(callAudioRouteRepository.endSession()).thenThrow(RuntimeException())

        assertThrows<RuntimeException> { underTest() }
    }
}
