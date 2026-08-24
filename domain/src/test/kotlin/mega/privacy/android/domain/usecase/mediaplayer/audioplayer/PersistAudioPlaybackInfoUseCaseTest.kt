package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.entity.mediaplayer.MediaType
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.continuewhereleftoff.RemoveRecentlyUsedItemUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersistAudioPlaybackInfoUseCaseTest {

    private lateinit var underTest: PersistAudioPlaybackInfoUseCase

    private val mediaPlayerRepository = mock<MediaPlayerRepository>()
    private val removeRecentlyUsedItemUseCase = mock<RemoveRecentlyUsedItemUseCase>()

    private val mediaHandle = 1234567L

    @BeforeAll
    fun setUp() {
        underTest = PersistAudioPlaybackInfoUseCase(
            mediaPlayerRepository = mediaPlayerRepository,
            removeRecentlyUsedItemUseCase = removeRecentlyUsedItemUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(mediaPlayerRepository, removeRecentlyUsedItemUseCase)
    }

    private fun playbackInfo(
        currentPosition: Long,
        totalDuration: Long = TimeUnit.MINUTES.toMillis(20),
        handle: Long = mediaHandle,
    ) = MediaPlaybackInfo(
        mediaHandle = handle,
        totalDuration = totalDuration,
        currentPosition = currentPosition,
        mediaType = MediaType.Audio,
    )

    @Test
    fun `test that invoke does nothing when the media handle is invalid`() = runTest {
        underTest(playbackInfo(currentPosition = TimeUnit.MINUTES.toMillis(10), handle = -1L))

        verify(mediaPlayerRepository, never()).updateAndPersistAudioPlaybackInfo(any())
        verify(mediaPlayerRepository, never()).deleteMediaPlaybackInfo(any())
    }

    @Test
    fun `test that invoke does nothing when the position is not more than 15 seconds`() =
        runTest {
            underTest(playbackInfo(currentPosition = TimeUnit.SECONDS.toMillis(15)))

            verify(mediaPlayerRepository, never()).updateAndPersistAudioPlaybackInfo(any())
            verify(mediaPlayerRepository, never()).deleteMediaPlaybackInfo(any())
        }

    @Test
    fun `test that invoke persists the playback info when the position is more than 15 seconds`() =
        runTest {
            val info = playbackInfo(currentPosition = TimeUnit.MINUTES.toMillis(10))

            underTest(info)

            verify(mediaPlayerRepository).updateAndPersistAudioPlaybackInfo(info)
            verify(mediaPlayerRepository, never()).deleteMediaPlaybackInfo(any())
        }

    @Test
    fun `test that invoke deletes playback info and recently used item when 30 seconds remain`() =
        runTest {
            val totalDuration = TimeUnit.MINUTES.toMillis(20)
            val info = playbackInfo(
                currentPosition = totalDuration - TimeUnit.SECONDS.toMillis(30),
                totalDuration = totalDuration,
            )

            underTest(info)

            verify(mediaPlayerRepository).deleteMediaPlaybackInfo(mediaHandle)
            verify(removeRecentlyUsedItemUseCase).invoke(mediaHandle)
            verify(mediaPlayerRepository, never()).updateAndPersistAudioPlaybackInfo(any())
        }

    @Test
    fun `test that invoke persists the playback info when just over 30 seconds remain`() =
        runTest {
            val totalDuration = TimeUnit.MINUTES.toMillis(20)
            val info = playbackInfo(
                currentPosition = totalDuration - TimeUnit.SECONDS.toMillis(31),
                totalDuration = totalDuration,
            )

            underTest(info)

            verify(mediaPlayerRepository).updateAndPersistAudioPlaybackInfo(info)
            verify(mediaPlayerRepository, never()).deleteMediaPlaybackInfo(any())
        }

    @Test
    fun `test that invoke persists the playback info when the duration is unknown`() = runTest {
        val info = playbackInfo(currentPosition = TimeUnit.MINUTES.toMillis(10), totalDuration = 0L)

        underTest(info)

        verify(mediaPlayerRepository).updateAndPersistAudioPlaybackInfo(info)
        verify(mediaPlayerRepository, never()).deleteMediaPlaybackInfo(any())
    }
}
