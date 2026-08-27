package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.continuewhereleftoff.AUDIO_PLAYBACK_INFO_SAVE_INTERVAL_MS
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.entity.mediaplayer.MediaType
import mega.privacy.android.domain.usecase.GetTickerUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrackAndSaveAudioPlaybackInfoUseCaseTest {

    private lateinit var underTest: TrackAndSaveAudioPlaybackInfoUseCase

    private val persistAudioPlaybackInfoUseCase = mock<PersistAudioPlaybackInfoUseCase>()
    private val getTickerUseCase = mock<GetTickerUseCase>()

    private val playbackInfo = MediaPlaybackInfo(
        mediaHandle = 1234567L,
        totalDuration = TimeUnit.MINUTES.toMillis(20),
        currentPosition = TimeUnit.MINUTES.toMillis(10),
        mediaType = MediaType.Audio,
    )

    @BeforeAll
    fun setUp() {
        underTest = TrackAndSaveAudioPlaybackInfoUseCase(
            persistAudioPlaybackInfoUseCase = persistAudioPlaybackInfoUseCase,
            getTickerUseCase = getTickerUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(persistAudioPlaybackInfoUseCase, getTickerUseCase)
    }

    @Test
    fun `test that invoke requests a ticker with the save interval`() = runTest {
        whenever(getTickerUseCase.invoke(any())).thenReturn(flowOf())

        underTest { playbackInfo }

        verify(getTickerUseCase).invoke(AUDIO_PLAYBACK_INFO_SAVE_INTERVAL_MS)
    }

    @Test
    fun `test that invoke persists the playback info on every tick`() = runTest {
        whenever(getTickerUseCase.invoke(any())).thenReturn(flowOf(Unit, Unit))
        val advanced = playbackInfo.copy(
            currentPosition = playbackInfo.currentPosition + TimeUnit.SECONDS.toMillis(3)
        )
        val snapshots = ArrayDeque(listOf(playbackInfo, advanced))

        underTest { snapshots.removeFirst() }

        verify(persistAudioPlaybackInfoUseCase).invoke(playbackInfo)
        verify(persistAudioPlaybackInfoUseCase).invoke(advanced)
    }

    @Test
    fun `test that invoke does not persist the playback info when the snapshot is unchanged`() =
        runTest {
            whenever(getTickerUseCase.invoke(any())).thenReturn(flowOf(Unit, Unit, Unit))

            underTest { playbackInfo }

            verify(persistAudioPlaybackInfoUseCase, times(1)).invoke(playbackInfo)
        }

    @Test
    fun `test that invoke keeps tracking when persisting a snapshot fails`() = runTest {
        whenever(getTickerUseCase.invoke(any())).thenReturn(flowOf(Unit, Unit))
        val advanced = playbackInfo.copy(
            currentPosition = playbackInfo.currentPosition + TimeUnit.SECONDS.toMillis(3)
        )
        val snapshots = ArrayDeque(listOf(playbackInfo, advanced))
        whenever(persistAudioPlaybackInfoUseCase.invoke(playbackInfo))
            .thenThrow(RuntimeException("persist failed"))

        underTest { snapshots.removeFirst() }

        verify(persistAudioPlaybackInfoUseCase).invoke(advanced)
    }

    @Test
    fun `test that invoke retries an unchanged snapshot when the previous persist failed`() =
        runTest {
            whenever(getTickerUseCase.invoke(any())).thenReturn(flowOf(Unit, Unit))
            whenever(persistAudioPlaybackInfoUseCase.invoke(playbackInfo))
                .thenThrow(RuntimeException("persist failed"))
                .thenReturn(Unit)

            underTest { playbackInfo }

            verify(persistAudioPlaybackInfoUseCase, times(2)).invoke(playbackInfo)
        }

    @Test
    fun `test that invoke rethrows when persisting is cancelled`() = runTest {
        whenever(getTickerUseCase.invoke(any())).thenReturn(flowOf(Unit))
        whenever(persistAudioPlaybackInfoUseCase.invoke(playbackInfo))
            .thenThrow(CancellationException("cancelled"))

        assertThrows<CancellationException> { underTest { playbackInfo } }
    }

    @Test
    fun `test that invoke does not persist playback info when the ticker never ticks`() =
        runTest {
            whenever(getTickerUseCase.invoke(any())).thenReturn(flowOf())

            underTest { playbackInfo }

            verify(persistAudioPlaybackInfoUseCase, never()).invoke(any())
        }
}
