package mega.privacy.android.domain.usecase.mediaplayer.videoplayer

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
import mega.privacy.android.domain.usecase.GetTickerUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrackPlaybackPositionUseCaseTest {
    private lateinit var underTest: TrackPlaybackPositionUseCase

    private val saveVideoPlaybackInfoUseCase = mock<SaveVideoPlaybackInfoUseCase>()
    private val getTicker = mock<GetTickerUseCase>()

    private val mediaId: Long = 1234567

    @BeforeEach
    fun setUp() {
        // PER_CLASS keeps one instance for the whole class, so the shared mocks carry
        // invocations and stubbing between tests unless they are reset first.
        reset(saveVideoPlaybackInfoUseCase, getTicker)
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = TrackPlaybackPositionUseCase(
            saveVideoPlaybackInfoUseCase = saveVideoPlaybackInfoUseCase,
            getTickerUseCase = getTicker,
        )
    }

    @AfterEach
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

    private fun snapshotProvider(vararg infos: MediaPlaybackInfo) =
        mock<() -> MediaPlaybackInfo> {
            on { invoke() }.thenReturn(infos.first(), *infos.drop(1).toTypedArray())
        }

    @Test
    fun `test that invoke saves the snapshot when the video position is more than 15 seconds`() =
        runTest {
            whenever(getTicker.invoke(any())).thenReturn(flowOf(Unit))
            val info = playbackInfo(currentPosition = 16_000)

            underTest(snapshotProvider(info))

            verify(saveVideoPlaybackInfoUseCase).invoke(info)
        }

    @Test
    fun `test that invoke does not save when the video position is less than 15 seconds`() =
        runTest {
            whenever(getTicker.invoke(any())).thenReturn(flowOf(Unit))

            underTest(snapshotProvider(playbackInfo(currentPosition = 10_000)))

            verify(saveVideoPlaybackInfoUseCase, never()).invoke(any())
        }

    @Test
    fun `test that invoke does not save when the snapshot carries no media handle`() = runTest {
        whenever(getTicker.invoke(any())).thenReturn(flowOf(Unit))

        underTest(
            snapshotProvider(
                playbackInfo(mediaHandle = INVALID_MEDIA_HANDLE, currentPosition = 100_000)
            )
        )

        verify(saveVideoPlaybackInfoUseCase, never()).invoke(any())
    }

    @Test
    fun `test that invoke does not save when the duration is not known yet`() = runTest {
        whenever(getTicker.invoke(any())).thenReturn(flowOf(Unit))

        underTest(
            snapshotProvider(playbackInfo(totalDuration = 0, currentPosition = 100_000))
        )

        verify(saveVideoPlaybackInfoUseCase, never()).invoke(any())
    }

    @Test
    fun `test that invoke saves an unchanged snapshot only once`() = runTest {
        whenever(getTicker.invoke(any())).thenReturn(flowOf(Unit, Unit, Unit))
        val info = playbackInfo(currentPosition = 16_000)
        val getCurrentPlaybackInfo = snapshotProvider(info)

        underTest(getCurrentPlaybackInfo)

        verify(getCurrentPlaybackInfo, times(3)).invoke()
        verify(saveVideoPlaybackInfoUseCase, times(1)).invoke(info)
    }

    @Test
    fun `test that invoke saves a snapshot again when the previous attempt failed`() = runTest {
        whenever(getTicker.invoke(any())).thenReturn(flowOf(Unit, Unit))
        val info = playbackInfo(currentPosition = 16_000)
        whenever(saveVideoPlaybackInfoUseCase(info))
            .thenThrow(RuntimeException("boom"))
            .thenAnswer { }

        underTest(snapshotProvider(info))

        verify(saveVideoPlaybackInfoUseCase, times(2)).invoke(info)
    }

    @Test
    fun `test that invoke keeps tracking when saving a snapshot fails`() = runTest {
        whenever(getTicker.invoke(any())).thenReturn(flowOf(Unit, Unit))
        val failing = playbackInfo(currentPosition = 16_000)
        val next = playbackInfo(currentPosition = 19_000)
        whenever(saveVideoPlaybackInfoUseCase(failing)).thenThrow(RuntimeException("boom"))

        underTest(snapshotProvider(failing, next))

        verify(saveVideoPlaybackInfoUseCase).invoke(next)
    }
}
