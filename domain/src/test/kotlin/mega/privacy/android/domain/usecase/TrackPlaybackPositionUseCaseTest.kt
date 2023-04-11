package mega.privacy.android.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.mediaplayer.TrackPlaybackPositionUseCase
import org.junit.After
import org.junit.Assert.*
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

    private val mediaId: Long = 1234567

    private val getTicker = mock<GetTicker>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = TrackPlaybackPositionUseCase(
            mediaPlayerRepository = mediaPlayerRepository,
            getTicker = getTicker
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that invoke with current position of the video is less than 15 seconds`() =
        runTest {
            whenever(getTicker.invoke(any())).thenReturn(flowOf(Unit))
            val currentPlaybackInfo = PlaybackInformation(
                mediaId = mediaId,
                totalDuration = 200_000,
                currentPosition = 10_000
            )

            val getCurrentPlaybackInformation =
                mock<() -> PlaybackInformation> { on { invoke() }.thenReturn(currentPlaybackInfo) }
            underTest(getCurrentPlaybackInformation)

            verify(getCurrentPlaybackInformation, times(1)).invoke()
            verify(mediaPlayerRepository, never()).deletePlaybackInformation(mediaId)
            verify(mediaPlayerRepository, never()).updatePlaybackInformation(currentPlaybackInfo)
        }

    @Test
    fun `test that update playback info when the video position is more than 15 seconds`() =
        runTest {
            whenever(getTicker.invoke(any())).thenReturn(flowOf(Unit, Unit))
            val playbackInfo = PlaybackInformation(
                mediaId = mediaId,
                totalDuration = 200_000,
                currentPosition = 15_000
            )
            val playbackInfoPassOneSecond = playbackInfo.copy(currentPosition = 16_000)

            val getCurrentPlaybackInformation =
                mock<() -> PlaybackInformation> {
                    on { invoke() }.thenReturn(
                        playbackInfo,
                        playbackInfoPassOneSecond
                    )
                }

            underTest(getCurrentPlaybackInformation)

            verify(getCurrentPlaybackInformation, times(2)).invoke()
            verify(mediaPlayerRepository).updatePlaybackInformation(playbackInfoPassOneSecond)
        }

    @Test
    fun `test that correct media id is passed when deleting track progress`() = runTest {
        whenever(getTicker.invoke(any())).thenReturn(flowOf(Unit, Unit))
        val currentPlaybackInfo = PlaybackInformation(
            mediaId = mediaId,
            totalDuration = 200_000,
            currentPosition = 10_000
        )

        val nextTrackId = mediaId + 1

        val nextPlaybackInfo = PlaybackInformation(
            mediaId = nextTrackId,
            totalDuration = 200_000,
            currentPosition = 199_000
        )

        val getCurrentPlaybackInformation =
            mock<() -> PlaybackInformation> {
                on { invoke() }.thenReturn(
                    currentPlaybackInfo,
                    nextPlaybackInfo
                )
            }
        underTest(getCurrentPlaybackInformation)

        verify(getCurrentPlaybackInformation, times(2)).invoke()
        verify(mediaPlayerRepository).deletePlaybackInformation(nextTrackId)
        verify(mediaPlayerRepository, never()).deletePlaybackInformation(mediaId)
        verify(mediaPlayerRepository, never()).updatePlaybackInformation(any())
    }
}