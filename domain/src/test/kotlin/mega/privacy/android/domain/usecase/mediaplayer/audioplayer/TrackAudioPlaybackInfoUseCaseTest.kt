package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.entity.mediaplayer.MediaType
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.GetTickerUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class TrackAudioPlaybackInfoUseCaseTest {

    private lateinit var underTest: TrackAudioPlaybackInfoUseCase

    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    private val mediaHandle: Long = 1234567

    private val getTicker = mock<GetTickerUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = TrackAudioPlaybackInfoUseCase(
            mediaPlayerRepository = mediaPlayerRepository,
            getTickerUseCase = getTicker
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that invoke with current position of the audio is less than 15 minutes`() =
        runTest {
            whenever(getTicker.invoke(any())).thenReturn(flowOf(Unit))
            val currentPlaybackInfo = MediaPlaybackInfo(
                mediaHandle = mediaHandle,
                totalDuration = TimeUnit.MINUTES.toMillis(20),
                currentPosition = TimeUnit.MINUTES.toMillis(15),
                mediaType = MediaType.Audio
            )

            val getCurrentPlaybackInfo =
                mock<() -> MediaPlaybackInfo> { on { invoke() }.thenReturn(currentPlaybackInfo) }
            underTest(getCurrentPlaybackInfo)

            verify(getCurrentPlaybackInfo, times(1)).invoke()
            verify(mediaPlayerRepository, never()).deleteMediaPlaybackInfo(mediaHandle)
            verify(mediaPlayerRepository, never()).updateAudioPlaybackInfo(currentPlaybackInfo)
        }

    @Test
    fun `test that update playback info when the audio position is more than 15 minutes`() =
        runTest {
            whenever(getTicker.invoke(any())).thenReturn(flowOf(Unit, Unit))
            val currentPlaybackInfo = MediaPlaybackInfo(
                mediaHandle = mediaHandle,
                totalDuration = TimeUnit.MINUTES.toMillis(20),
                currentPosition = TimeUnit.MINUTES.toMillis(15),
                mediaType = MediaType.Audio
            )

            val playbackInfoPassOneSecond = currentPlaybackInfo.copy(
                currentPosition = TimeUnit.MINUTES.toMillis(15) + 1000
            )

            val getCurrentPlaybackInfo =
                mock<() -> MediaPlaybackInfo> {
                    on { invoke() }.thenReturn(
                        currentPlaybackInfo,
                        playbackInfoPassOneSecond
                    )
                }
            underTest(getCurrentPlaybackInfo)

            verify(getCurrentPlaybackInfo, times(2)).invoke()
            verify(mediaPlayerRepository).updateAudioPlaybackInfo(playbackInfoPassOneSecond)
        }

    @Test
    fun `test that correct media id is passed when deleting track progress`() = runTest {
        whenever(getTicker.invoke(any())).thenReturn(flowOf(Unit, Unit))
        val currentPlaybackInfo = MediaPlaybackInfo(
            mediaHandle = mediaHandle,
            totalDuration = TimeUnit.MINUTES.toMillis(20),
            currentPosition = TimeUnit.MINUTES.toMillis(15),
            mediaType = MediaType.Audio
        )

        val nextTrackId = mediaHandle + 1

        val nextPlaybackInfo = MediaPlaybackInfo(
            mediaHandle = nextTrackId,
            totalDuration = TimeUnit.MINUTES.toMillis(20),
            currentPosition = TimeUnit.MINUTES.toMillis(20) - 1000,
            mediaType = MediaType.Audio
        )

        val getCurrentPlaybackInfo =
            mock<() -> MediaPlaybackInfo> {
                on { invoke() }.thenReturn(
                    currentPlaybackInfo,
                    nextPlaybackInfo
                )
            }
        underTest(getCurrentPlaybackInfo)

        verify(getCurrentPlaybackInfo, times(2)).invoke()
        verify(mediaPlayerRepository).deleteMediaPlaybackInfo(nextTrackId)
        verify(mediaPlayerRepository, never()).deleteMediaPlaybackInfo(mediaHandle)
        verify(mediaPlayerRepository, never()).updateAudioPlaybackInfo(any())
    }
}