package mega.privacy.android.domain.usecase.mediaplayer.videoplayer

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo.Companion.INVALID_MEDIA_HANDLE
import mega.privacy.android.domain.entity.mediaplayer.MediaType
import mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.continuewhereleftoff.RemoveRecentlyUsedItemUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SaveVideoPlaybackInfoUseCaseTest {
    private lateinit var underTest: SaveVideoPlaybackInfoUseCase

    private val mediaPlayerRepository = mock<MediaPlayerRepository>()
    private val removeRecentlyUsedItemUseCase = mock<RemoveRecentlyUsedItemUseCase>()
    private val persistVideoPlaybackInfoUseCase = mock<PersistVideoPlaybackInfoUseCase>()

    private val mediaId: Long = 1234567

    @BeforeEach
    fun setUp() {
        // PER_CLASS keeps one instance for the whole class, so the shared mocks carry
        // invocations and stubbing between tests unless they are reset first.
        reset(
            mediaPlayerRepository,
            removeRecentlyUsedItemUseCase,
            persistVideoPlaybackInfoUseCase,
        )
        underTest = SaveVideoPlaybackInfoUseCase(
            mediaPlayerRepository = mediaPlayerRepository,
            removeRecentlyUsedItemUseCase = removeRecentlyUsedItemUseCase,
            persistVideoPlaybackInfoUseCase = persistVideoPlaybackInfoUseCase,
        )
    }

    private fun playbackInfo(
        mediaHandle: Long = mediaId,
        totalDuration: Long = 200_000,
        currentPosition: Long,
    ) = MediaPlaybackInfo(
        mediaHandle = mediaHandle,
        totalDuration = totalDuration,
        currentPosition = currentPosition,
        mediaType = MediaType.Video,
    )

    @Test
    fun `test that invoke writes the position to both stores`() = runTest {
        val info = playbackInfo(currentPosition = 16_000)

        underTest(info)

        verify(mediaPlayerRepository).updatePlaybackInformation(
            PlaybackInformation(
                mediaId = mediaId,
                totalDuration = 200_000,
                currentPosition = 16_000,
            )
        )
        verify(persistVideoPlaybackInfoUseCase).invoke(info)
    }

    @Test
    fun `test that invoke drops the item when it is played to the last 3 seconds`() = runTest {
        val info = playbackInfo(currentPosition = 199_000)

        underTest(info)

        verify(mediaPlayerRepository).deletePlaybackInformation(mediaId)
        verify(removeRecentlyUsedItemUseCase).invoke(mediaId)
        verify(mediaPlayerRepository, never()).updatePlaybackInformation(any())
        verify(persistVideoPlaybackInfoUseCase).invoke(info)
    }

    @Test
    fun `test that invoke still persists to the database when dropping the item from the carousel fails`() =
        runTest {
            val info = playbackInfo(currentPosition = 199_000)
            whenever(removeRecentlyUsedItemUseCase(mediaId)).thenThrow(RuntimeException("boom"))

            runCatching { underTest(info) }

            verify(mediaPlayerRepository).deletePlaybackInformation(mediaId)
            verify(persistVideoPlaybackInfoUseCase).invoke(info)
        }

    @Test
    fun `test that invoke still drops the item from the carousel when deleting the position fails`() =
        runTest {
            val info = playbackInfo(currentPosition = 199_000)
            whenever(mediaPlayerRepository.deletePlaybackInformation(mediaId))
                .thenThrow(RuntimeException("boom"))

            runCatching { underTest(info) }

            verify(removeRecentlyUsedItemUseCase).invoke(mediaId)
            verify(persistVideoPlaybackInfoUseCase).invoke(info)
        }

    @Test
    fun `test that invoke does nothing when the snapshot carries no media handle`() = runTest {
        underTest(playbackInfo(mediaHandle = INVALID_MEDIA_HANDLE, currentPosition = 100_000))

        verifyNoInteractions(
            mediaPlayerRepository,
            removeRecentlyUsedItemUseCase,
            persistVideoPlaybackInfoUseCase,
        )
    }

    @Test
    fun `test that invoke does nothing when the duration is not known yet`() = runTest {
        underTest(playbackInfo(totalDuration = 0, currentPosition = 100_000))

        verifyNoInteractions(
            mediaPlayerRepository,
            removeRecentlyUsedItemUseCase,
            persistVideoPlaybackInfoUseCase,
        )
    }

    @Test
    fun `test that invoke does nothing when the position is below the minimum playback threshold`() =
        runTest {
            underTest(playbackInfo(currentPosition = 10_000))

            verifyNoInteractions(
                mediaPlayerRepository,
                removeRecentlyUsedItemUseCase,
                persistVideoPlaybackInfoUseCase,
            )
        }

    @Test
    fun `test that invoke leaves the Continue Where Left Off index alone when the position is kept`() =
        runTest {
            underTest(playbackInfo(currentPosition = 16_000))

            verifyNoInteractions(removeRecentlyUsedItemUseCase)
        }

    @Test
    fun `test that invoke writes to the DataStore path when the database write fails`() = runTest {
        val info = playbackInfo(currentPosition = 16_000)
        whenever(persistVideoPlaybackInfoUseCase(info)).thenThrow(RuntimeException("boom"))

        runCatching { underTest(info) }

        verify(mediaPlayerRepository).updatePlaybackInformation(
            PlaybackInformation(
                mediaId = mediaId,
                totalDuration = 200_000,
                currentPosition = 16_000,
            )
        )
    }
}
