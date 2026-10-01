package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.entity.mediaplayer.MediaType
import mega.privacy.android.domain.usecase.mediaplayer.GetMediaPlaybackInfoUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetAudioResumePositionUseCaseTest {

    private lateinit var underTest: GetAudioResumePositionUseCase

    private val getMediaPlaybackInfoUseCase = mock<GetMediaPlaybackInfoUseCase>()

    private val handle = 12345L

    @BeforeAll
    fun setUp() {
        underTest = GetAudioResumePositionUseCase(
            getMediaPlaybackInfoUseCase = getMediaPlaybackInfoUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getMediaPlaybackInfoUseCase)
    }

    private fun playbackInfo(
        currentPosition: Long,
        totalDuration: Long,
        mediaType: MediaType = MediaType.Audio,
    ) = MediaPlaybackInfo(
        mediaHandle = handle,
        totalDuration = totalDuration,
        currentPosition = currentPosition,
        mediaType = mediaType,
    )

    private suspend fun stubPlaybackInfo(info: MediaPlaybackInfo?) {
        whenever(getMediaPlaybackInfoUseCase(handle)).thenReturn(info)
    }

    @Test
    fun `test that invoke returns null when there is no saved playback info`() = runTest {
        stubPlaybackInfo(null)

        assertThat(underTest(handle)).isNull()
    }

    @Test
    fun `test that invoke returns null when the saved record is not audio`() = runTest {
        stubPlaybackInfo(
            playbackInfo(
                currentPosition = TimeUnit.MINUTES.toMillis(15),
                totalDuration = TimeUnit.MINUTES.toMillis(20),
                mediaType = MediaType.Video,
            )
        )

        assertThat(underTest(handle)).isNull()
    }

    @Test
    fun `test that invoke returns null when the duration is not longer than 6 minutes`() =
        runTest {
            stubPlaybackInfo(
                playbackInfo(
                    currentPosition = TimeUnit.MINUTES.toMillis(1),
                    totalDuration = TimeUnit.MINUTES.toMillis(6),
                )
            )

            assertThat(underTest(handle)).isNull()
        }

    @Test
    fun `test that invoke returns null when the duration is unknown`() = runTest {
        stubPlaybackInfo(
            playbackInfo(currentPosition = TimeUnit.MINUTES.toMillis(1), totalDuration = 0L)
        )

        assertThat(underTest(handle)).isNull()
    }

    @Test
    fun `test that invoke returns null when the saved position is zero`() = runTest {
        stubPlaybackInfo(
            playbackInfo(currentPosition = 0L, totalDuration = TimeUnit.MINUTES.toMillis(20))
        )

        assertThat(underTest(handle)).isNull()
    }

    @Test
    fun `test that invoke returns null when the saved position is not before the end`() = runTest {
        val totalDuration = TimeUnit.MINUTES.toMillis(20)
        stubPlaybackInfo(
            playbackInfo(currentPosition = totalDuration, totalDuration = totalDuration)
        )

        assertThat(underTest(handle)).isNull()
    }

    @Test
    fun `test that invoke returns null when 30 seconds or less remain`() = runTest {
        val totalDuration = TimeUnit.MINUTES.toMillis(20)
        stubPlaybackInfo(
            playbackInfo(
                currentPosition = totalDuration - TimeUnit.SECONDS.toMillis(30),
                totalDuration = totalDuration,
            )
        )

        assertThat(underTest(handle)).isNull()
    }

    @Test
    fun `test that invoke returns the saved position when just over 30 seconds remain`() =
        runTest {
            val totalDuration = TimeUnit.MINUTES.toMillis(20)
            val position = totalDuration - TimeUnit.SECONDS.toMillis(31)
            stubPlaybackInfo(
                playbackInfo(currentPosition = position, totalDuration = totalDuration)
            )

            assertThat(underTest(handle)).isEqualTo(position)
        }

    @Test
    fun `test that invoke returns the saved position for long audio`() = runTest {
        val position = TimeUnit.MINUTES.toMillis(15)
        stubPlaybackInfo(
            playbackInfo(currentPosition = position, totalDuration = TimeUnit.MINUTES.toMillis(20))
        )

        assertThat(underTest(handle)).isEqualTo(position)
    }
}
