package mega.privacy.android.domain.usecase

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.SetVideoRepeatModeUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetVideoRepeatModeUseCaseTest {
    private lateinit var underTest: SetVideoRepeatModeUseCase
    private val mediaPlayerRepository = mock<MediaPlayerRepository>()
    private val testRepeatValue = RepeatToggleMode.REPEAT_ALL.ordinal

    @BeforeAll
    fun setUp() {
        underTest = SetVideoRepeatModeUseCase(mediaPlayerRepository = mediaPlayerRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(mediaPlayerRepository)
    }

    @Test
    fun `test that the function is invoked as expected`() =
        runTest {
            underTest(testRepeatValue)
            verify(mediaPlayerRepository).setVideoRepeatMode(testRepeatValue)
        }
}