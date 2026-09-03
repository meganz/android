package mega.privacy.android.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo.Companion.INVALID_MEDIA_HANDLE
import mega.privacy.android.domain.entity.mediaplayer.MediaType
import mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.continuewhereleftoff.RemoveRecentlyUsedItemUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.TrackPlaybackPositionUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TrackPlaybackPositionUseCaseTest {
    private lateinit var underTest: TrackPlaybackPositionUseCase

    private val mediaPlayerRepository = mock<MediaPlayerRepository>()
    private val removeRecentlyUsedItemUseCase = mock<RemoveRecentlyUsedItemUseCase>()

    private val mediaId: Long = 1234567

    private val getTicker = mock<GetTickerUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = TrackPlaybackPositionUseCase(
            mediaPlayerRepository = mediaPlayerRepository,
            removeRecentlyUsedItemUseCase = removeRecentlyUsedItemUseCase,
            getTickerUseCase = getTicker
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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

    private fun playbackInformation(
        mediaId: Long,
        totalDuration: Long = 200_000,
        currentPosition: Long,
    ) = PlaybackInformation(
        mediaId = mediaId,
        totalDuration = totalDuration,
        currentPosition = currentPosition,
    )

    @Test
    fun `test that invoke with current position of the video is less than 15 seconds`() =
        runTest {
            whenever(getTicker.invoke(any())).thenReturn(flowOf(Unit))
            val currentPlaybackInfo = playbackInfo(currentPosition = 10_000)

            val getCurrentPlaybackInfo =
                mock<() -> MediaPlaybackInfo> { on { invoke() }.thenReturn(currentPlaybackInfo) }
            underTest(getCurrentPlaybackInfo)

            verify(getCurrentPlaybackInfo, times(1)).invoke()
            verify(mediaPlayerRepository, never()).deletePlaybackInformation(mediaId)
            verify(mediaPlayerRepository, never()).updatePlaybackInformation(any())
        }

    @Test
    fun `test that update playback info when the video position is more than 15 seconds`() =
        runTest {
            whenever(getTicker.invoke(any())).thenReturn(flowOf(Unit, Unit))
            val playbackInfo = playbackInfo(currentPosition = 15_000)
            val playbackInfoPassOneSecond = playbackInfo.copy(currentPosition = 16_000)

            val getCurrentPlaybackInfo =
                mock<() -> MediaPlaybackInfo> {
                    on { invoke() }.thenReturn(
                        playbackInfo,
                        playbackInfoPassOneSecond
                    )
                }

            underTest(getCurrentPlaybackInfo)

            verify(getCurrentPlaybackInfo, times(2)).invoke()
            verify(mediaPlayerRepository).updatePlaybackInformation(
                playbackInformation(mediaId = mediaId, currentPosition = 16_000)
            )
        }

    @Test
    fun `test that correct media id is passed when deleting track progress`() = runTest {
        whenever(getTicker.invoke(any())).thenReturn(flowOf(Unit, Unit))
        val currentPlaybackInfo = playbackInfo(currentPosition = 10_000)

        val nextTrackId = mediaId + 1
        val nextPlaybackInfo = playbackInfo(mediaHandle = nextTrackId, currentPosition = 199_000)

        val getCurrentPlaybackInfo =
            mock<() -> MediaPlaybackInfo> {
                on { invoke() }.thenReturn(
                    currentPlaybackInfo,
                    nextPlaybackInfo
                )
            }
        underTest(getCurrentPlaybackInfo)

        verify(getCurrentPlaybackInfo, times(2)).invoke()
        verify(mediaPlayerRepository).deletePlaybackInformation(nextTrackId)
        verify(removeRecentlyUsedItemUseCase).invoke(nextTrackId)
        verify(mediaPlayerRepository, never()).deletePlaybackInformation(mediaId)
        verify(removeRecentlyUsedItemUseCase, never()).invoke(mediaId)
        verify(mediaPlayerRepository, never()).updatePlaybackInformation(any())
    }

    @Test
    fun `test that nothing is tracked when the snapshot carries no media handle`() = runTest {
        whenever(getTicker.invoke(any())).thenReturn(flowOf(Unit))
        val currentPlaybackInfo =
            playbackInfo(mediaHandle = INVALID_MEDIA_HANDLE, currentPosition = 100_000)

        val getCurrentPlaybackInfo =
            mock<() -> MediaPlaybackInfo> { on { invoke() }.thenReturn(currentPlaybackInfo) }
        underTest(getCurrentPlaybackInfo)

        verify(getCurrentPlaybackInfo, times(1)).invoke()
        verify(mediaPlayerRepository, never()).updatePlaybackInformation(any())
        verify(mediaPlayerRepository, never()).deletePlaybackInformation(any())
        verify(removeRecentlyUsedItemUseCase, never()).invoke(any())
    }
}
