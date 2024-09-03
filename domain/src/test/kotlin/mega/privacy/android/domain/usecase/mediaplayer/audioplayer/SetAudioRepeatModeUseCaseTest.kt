package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetAudioRepeatModeUseCaseTest {
    lateinit var underTest: SetAudioRepeatModeUseCase
    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetAudioRepeatModeUseCase(
            mediaPlayerRepository = mediaPlayerRepository,
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(mediaPlayerRepository)
    }

    @Test
    fun `test that setAudioRepeatMode function is invoked as expected`() = runTest {
        val expectedRepeatMode = RepeatToggleMode.REPEAT_ONE.ordinal
        underTest(expectedRepeatMode)
        verify(mediaPlayerRepository).setAudioRepeatMode(expectedRepeatMode)
    }
}