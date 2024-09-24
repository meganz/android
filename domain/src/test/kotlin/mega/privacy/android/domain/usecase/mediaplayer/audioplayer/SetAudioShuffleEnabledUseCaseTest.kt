package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.MediaPlayerRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetAudioShuffleEnabledUseCaseTest {
    lateinit var underTest: SetAudioShuffleEnabledUseCase
    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetAudioShuffleEnabledUseCase(
            mediaPlayerRepository = mediaPlayerRepository,
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(mediaPlayerRepository)
    }

    @Test
    fun `test that setAudioShuffleEnabled function is invoked as expected with true value`() =
        runTest {
            underTest(true)
            verify(mediaPlayerRepository).setAudioShuffleEnabled(true)
        }

    @Test
    fun `test that setAudioShuffleEnabled function is invoked as expected with false value`() =
        runTest {
            underTest(false)
            verify(mediaPlayerRepository).setAudioShuffleEnabled(false)
        }
}