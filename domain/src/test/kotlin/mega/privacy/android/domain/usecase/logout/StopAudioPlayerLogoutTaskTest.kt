package mega.privacy.android.domain.usecase.logout

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.StopAudioService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StopAudioPlayerLogoutTaskTest {
    private lateinit var underTest: StopAudioPlayerLogoutTask

    private val stopAudioService = mock<StopAudioService>()

    @BeforeEach
    internal fun setUp() {
        reset(stopAudioService)
        underTest = StopAudioPlayerLogoutTask(stopAudioService = stopAudioService)
    }

    @Test
    fun `test that onLogoutSuccess stops the audio player`() = runTest {
        underTest.onLogoutSuccess()

        verify(stopAudioService).invoke()
    }

    @Test
    fun `test that onLogoutSuccess propagates a failure from stopping the audio player`() =
        runTest {
            whenever(stopAudioService()).thenThrow(RuntimeException("boom"))

            assertThrows<RuntimeException> { underTest.onLogoutSuccess() }
        }

    @Test
    fun `test that onLogoutFailed leaves the audio player alone`() = runTest {
        underTest.onLogoutFailed(RuntimeException("boom"))

        verifyNoInteractions(stopAudioService)
    }
}
