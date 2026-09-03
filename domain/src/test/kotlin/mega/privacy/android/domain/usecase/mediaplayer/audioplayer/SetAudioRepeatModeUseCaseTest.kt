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
class SetAudioRepeatModeUseCaseTest {
    lateinit var underTest: SetAudioRepeatModeUseCase
    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetAudioRepeatModeUseCase(
            mediaPlayerRepository = mediaPlayerRepository,
            audioPlaybackModeMutex = Mutex(),
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(mediaPlayerRepository)
    }

    private fun stubPreferences(
        continuousPlaybackEnabled: Boolean?,
        continuousPlaybackAutoEnabled: Boolean? = false,
    ) {
        whenever(mediaPlayerRepository.monitorAudioContinuousPlaybackEnabled())
            .thenReturn(flowOf(continuousPlaybackEnabled))
        whenever(mediaPlayerRepository.monitorAudioContinuousPlaybackAutoEnabled())
            .thenReturn(flowOf(continuousPlaybackAutoEnabled))
    }

    @Test
    fun `test that setAudioRepeatMode function is invoked as expected`() = runTest {
        stubPreferences(continuousPlaybackEnabled = true)

        underTest(RepeatToggleMode.REPEAT_ONE.ordinal)

        verify(mediaPlayerRepository).setAudioRepeatMode(RepeatToggleMode.REPEAT_ONE.ordinal)
    }

    @Test
    fun `test that continuous playback is untouched when it is already on and mode is REPEAT_ALL`() =
        runTest {
            stubPreferences(continuousPlaybackEnabled = true)

            underTest(RepeatToggleMode.REPEAT_ALL.ordinal)

            verify(mediaPlayerRepository, never()).setAudioContinuousPlaybackEnabled(any())
            verify(mediaPlayerRepository, never()).setAudioContinuousPlaybackAutoEnabled(any())
        }

    @Test
    fun `test that continuous playback is untouched when the preference is unset and mode is REPEAT_ALL`() =
        runTest {
            stubPreferences(continuousPlaybackEnabled = null)

            underTest(RepeatToggleMode.REPEAT_ALL.ordinal)

            verify(mediaPlayerRepository, never()).setAudioContinuousPlaybackEnabled(any())
        }

    @Test
    fun `test that continuous playback is enabled and recorded before REPEAT_ALL is set when it is off`() =
        runTest {
            stubPreferences(continuousPlaybackEnabled = false)

            underTest(RepeatToggleMode.REPEAT_ALL.ordinal)

            inOrder(mediaPlayerRepository) {
                verify(mediaPlayerRepository).setAudioContinuousPlaybackEnabled(true)
                verify(mediaPlayerRepository).setAudioContinuousPlaybackAutoEnabled(true)
                verify(mediaPlayerRepository)
                    .setAudioRepeatMode(RepeatToggleMode.REPEAT_ALL.ordinal)
            }
        }

    @Test
    fun `test that continuous playback is restored after leaving REPEAT_ALL when it was turned on automatically`() =
        runTest {
            stubPreferences(
                continuousPlaybackEnabled = true,
                continuousPlaybackAutoEnabled = true,
            )

            underTest(RepeatToggleMode.REPEAT_ONE.ordinal)

            inOrder(mediaPlayerRepository) {
                verify(mediaPlayerRepository)
                    .setAudioRepeatMode(RepeatToggleMode.REPEAT_ONE.ordinal)
                verify(mediaPlayerRepository).setAudioContinuousPlaybackAutoEnabled(false)
                verify(mediaPlayerRepository).setAudioContinuousPlaybackEnabled(false)
            }
        }

    @Test
    fun `test that continuous playback is restored after leaving REPEAT_ALL for REPEAT_NONE`() =
        runTest {
            stubPreferences(
                continuousPlaybackEnabled = true,
                continuousPlaybackAutoEnabled = true,
            )

            underTest(RepeatToggleMode.REPEAT_NONE.ordinal)

            verify(mediaPlayerRepository).setAudioContinuousPlaybackEnabled(false)
        }

    @Test
    fun `test that continuous playback is kept after leaving REPEAT_ALL when the user turned it on`() =
        runTest {
            stubPreferences(
                continuousPlaybackEnabled = true,
                continuousPlaybackAutoEnabled = false,
            )

            underTest(RepeatToggleMode.REPEAT_NONE.ordinal)

            verify(mediaPlayerRepository, never()).setAudioContinuousPlaybackEnabled(any())
            verify(mediaPlayerRepository, never()).setAudioContinuousPlaybackAutoEnabled(any())
        }
}
