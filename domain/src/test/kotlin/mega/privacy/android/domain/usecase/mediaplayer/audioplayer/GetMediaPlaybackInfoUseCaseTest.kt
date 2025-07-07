package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.repository.MediaPlayerRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetMediaPlaybackInfoUseCaseTest {
    private lateinit var underTest: GetMediaPlaybackInfoUseCase

    private val mediaPlayerRepository = mock<MediaPlayerRepository>()
    private val testHandle = 12345L

    @BeforeAll
    fun setup() {
        underTest =
            GetMediaPlaybackInfoUseCase(mediaPlayerRepository = mediaPlayerRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(mediaPlayerRepository)
    }

    @Test
    fun `test that GetMediaPlaybackInfoUseCase returns null`() = runTest {
        whenever(mediaPlayerRepository.getMediaPlaybackInfo(testHandle)).thenReturn(null)
        underTest(testHandle).let { playbackInfo ->
            assertThat(playbackInfo).isNull()
            verify(mediaPlayerRepository).getMediaPlaybackInfo(testHandle)
        }
    }

    @Test
    fun `test that GetMediaPlaybackInfoUseCase returns as expected`() = runTest {
        val playbackInfo = mock<MediaPlaybackInfo>()

        whenever(mediaPlayerRepository.getMediaPlaybackInfo(testHandle)).thenReturn(playbackInfo)

        underTest(testHandle).let { playbackInfo ->
            assertThat(playbackInfo).isEqualTo(playbackInfo)
            verify(mediaPlayerRepository).getMediaPlaybackInfo(testHandle)
        }
    }
}