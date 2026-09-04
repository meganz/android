package mega.privacy.android.domain.usecase.mediaplayer.videoplayer

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.continuewhereleftoff.CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS
import mega.privacy.android.domain.entity.continuewhereleftoff.CWLO_NEAR_COMPLETION_THRESHOLD_MS
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo.Companion.INVALID_MEDIA_HANDLE
import mega.privacy.android.domain.entity.mediaplayer.MediaType
import mega.privacy.android.domain.repository.MediaPlayerRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersistVideoPlaybackInfoUseCaseTest {
    private lateinit var underTest: PersistVideoPlaybackInfoUseCase

    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    private val mediaHandle = 123456L
    private val totalDuration = 200_000L

    @BeforeEach
    fun setUp() {
        reset(mediaPlayerRepository)
        underTest = PersistVideoPlaybackInfoUseCase(mediaPlayerRepository)
    }

    private fun playbackInfo(
        handle: Long = mediaHandle,
        duration: Long = totalDuration,
        currentPosition: Long,
    ) = MediaPlaybackInfo(
        mediaHandle = handle,
        totalDuration = duration,
        currentPosition = currentPosition,
        mediaType = MediaType.Video,
    )

    @Test
    fun `test that invoke does nothing when the snapshot carries no media handle`() = runTest {
        underTest(playbackInfo(handle = INVALID_MEDIA_HANDLE, currentPosition = 100_000))

        verifyNoInteractions(mediaPlayerRepository)
    }

    @Test
    fun `test that invoke does nothing when the total duration is unknown`() = runTest {
        underTest(playbackInfo(duration = 0, currentPosition = 100_000))

        verifyNoInteractions(mediaPlayerRepository)
    }

    @Test
    fun `test that invoke persists the playback info for a position in the middle`() = runTest {
        val info = playbackInfo(currentPosition = 100_000)

        underTest(info)

        verify(mediaPlayerRepository).updateAndPersistPlaybackInfo(info)
        verifyNoMoreInteractions(mediaPlayerRepository)
    }

    @Test
    fun `test that invoke deletes the playback info when the position is back at the start`() =
        runTest {
            underTest(playbackInfo(currentPosition = CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS))

            verify(mediaPlayerRepository).deleteMediaPlaybackInfo(mediaHandle)
            verifyNoMoreInteractions(mediaPlayerRepository)
        }

    @Test
    fun `test that invoke persists the playback info when the position is just past the start`() =
        runTest {
            val info = playbackInfo(currentPosition = CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS + 1)

            underTest(info)

            verify(mediaPlayerRepository).updateAndPersistPlaybackInfo(info)
        }

    @Test
    fun `test that invoke deletes the playback info when the item is watched through`() = runTest {
        underTest(
            playbackInfo(currentPosition = totalDuration - CWLO_NEAR_COMPLETION_THRESHOLD_MS + 1)
        )

        verify(mediaPlayerRepository).deleteMediaPlaybackInfo(mediaHandle)
        verify(mediaPlayerRepository, never()).updateAndPersistPlaybackInfo(any())
    }

    @Test
    fun `test that invoke persists the playback info when it is just outside the completion threshold`() =
        runTest {
            val info =
                playbackInfo(currentPosition = totalDuration - CWLO_NEAR_COMPLETION_THRESHOLD_MS)

            underTest(info)

            verify(mediaPlayerRepository).updateAndPersistPlaybackInfo(info)
        }

    @Test
    fun `test that invoke deletes the playback info when the position is past the total duration`() =
        runTest {
            underTest(playbackInfo(currentPosition = totalDuration + 1))

            verify(mediaPlayerRepository).deleteMediaPlaybackInfo(mediaHandle)
            verify(mediaPlayerRepository, never()).updateAndPersistPlaybackInfo(any())
        }

}
