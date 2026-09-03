package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetAudioContinuousPlaybackEnabledUseCaseTest {
    lateinit var underTest: SetAudioContinuousPlaybackEnabledUseCase
    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetAudioContinuousPlaybackEnabledUseCase(
            mediaPlayerRepository = mediaPlayerRepository,
            audioPlaybackModeMutex = Mutex(),
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(mediaPlayerRepository)
    }

    private fun stubRepeatMode(mode: RepeatToggleMode) {
        whenever(mediaPlayerRepository.monitorAudioRepeatMode()).thenReturn(flowOf(mode))
    }

    @Test
    fun `test that setAudioContinuousPlaybackEnabled function is invoked as expected with true value`() =
        runTest {
            underTest(true)
            verify(mediaPlayerRepository).setAudioContinuousPlaybackEnabled(true)
        }

    @Test
    fun `test that setAudioRepeatMode is not invoked when enabling continuous playback`() =
        runTest {
            underTest(true)
            verify(mediaPlayerRepository, never()).setAudioRepeatMode(any())
        }

    @Test
    fun `test that setAudioContinuousPlaybackEnabled function is invoked as expected with false value`() =
        runTest {
            stubRepeatMode(RepeatToggleMode.REPEAT_NONE)
            underTest(false)
            verify(mediaPlayerRepository).setAudioContinuousPlaybackEnabled(false)
        }

    @Test
    fun `test that setAudioRepeatMode is not invoked when disabling continuous playback and repeat mode is REPEAT_ONE`() =
        runTest {
            stubRepeatMode(RepeatToggleMode.REPEAT_ONE)
            underTest(false)
            verify(mediaPlayerRepository, never()).setAudioRepeatMode(any())
        }

    @Test
    fun `test that repeat mode is reset before continuous playback is disabled when repeat mode is REPEAT_ALL`() =
        runTest {
            stubRepeatMode(RepeatToggleMode.REPEAT_ALL)
            underTest(false)
            inOrder(mediaPlayerRepository) {
                verify(mediaPlayerRepository)
                    .setAudioRepeatMode(RepeatToggleMode.REPEAT_NONE.ordinal)
                verify(mediaPlayerRepository).setAudioContinuousPlaybackEnabled(false)
            }
        }

    @Test
    fun `test that the automatic continuous playback record is cleared before anything else when enabling`() =
        runTest {
            underTest(true)
            inOrder(mediaPlayerRepository) {
                verify(mediaPlayerRepository).setAudioContinuousPlaybackAutoEnabled(false)
                verify(mediaPlayerRepository).setAudioContinuousPlaybackEnabled(true)
            }
        }

    @Test
    fun `test that the automatic continuous playback record is cleared when disabling while repeat mode is REPEAT_ALL`() =
        runTest {
            stubRepeatMode(RepeatToggleMode.REPEAT_ALL)
            underTest(false)
            inOrder(mediaPlayerRepository) {
                verify(mediaPlayerRepository).setAudioContinuousPlaybackAutoEnabled(false)
                verify(mediaPlayerRepository)
                    .setAudioRepeatMode(RepeatToggleMode.REPEAT_NONE.ordinal)
                verify(mediaPlayerRepository).setAudioContinuousPlaybackEnabled(false)
            }
        }
}
