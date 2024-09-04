package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorAudioRepeatModeUseCaseTest {
    private lateinit var underTest: MonitorAudioRepeatModeUseCase

    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    @BeforeAll
    fun initialise() {
        underTest = MonitorAudioRepeatModeUseCase(mediaPlayerRepository = mediaPlayerRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(mediaPlayerRepository)
    }

    @Test
    fun `test that monitorAudioRepeatMode function returns as expected`() =
        runTest {
            val expectedTRepeatMode = RepeatToggleMode.REPEAT_ALL
            mediaPlayerRepository.stub {
                on { monitorAudioRepeatMode() }.thenReturn(flowOf(expectedTRepeatMode))
            }

            underTest().test {
                assertThat(awaitItem()).isEqualTo(expectedTRepeatMode)
                awaitComplete()
            }
        }
}