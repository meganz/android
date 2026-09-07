package mega.privacy.android.domain.usecase.mediaplayer.videoplayer

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.continuewhereleftoff.CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS
import mega.privacy.android.domain.entity.continuewhereleftoff.CWLO_NEAR_COMPLETION_THRESHOLD_MS
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.entity.mediaplayer.MediaType
import mega.privacy.android.domain.repository.MediaPlayerRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MigrateVideoPlaybackInfosUseCaseTest {
    private lateinit var underTest: MigrateVideoPlaybackInfosUseCase

    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    private val totalDuration = 200_000L

    private val legacyInfos = listOf(playbackInfo(currentPosition = 100_000L))

    private fun playbackInfo(
        handle: Long = 1L,
        duration: Long = totalDuration,
        currentPosition: Long,
    ) = MediaPlaybackInfo(
        mediaHandle = handle,
        totalDuration = duration,
        currentPosition = currentPosition,
        mediaType = MediaType.Video,
    )

    @BeforeEach
    fun setUp() {
        reset(mediaPlayerRepository)
        underTest = MigrateVideoPlaybackInfosUseCase(mediaPlayerRepository)
    }

    @Test
    fun `test that invoke migrates the legacy records and marks the migration done`() = runTest {
        whenever(mediaPlayerRepository.isVideoPlaybackInfoMigrated()).thenReturn(false)
        whenever(mediaPlayerRepository.getLegacyVideoPlaybackInfos()).thenReturn(legacyInfos)

        underTest()

        verify(mediaPlayerRepository).insertMigratedVideoPlaybackInfos(legacyInfos)
        verify(mediaPlayerRepository).setVideoPlaybackInfoMigrated()
    }

    @Test
    fun `test that invoke does nothing when the migration has already run`() = runTest {
        whenever(mediaPlayerRepository.isVideoPlaybackInfoMigrated()).thenReturn(true)

        underTest()

        verify(mediaPlayerRepository).isVideoPlaybackInfoMigrated()
        verifyNoMoreInteractions(mediaPlayerRepository)
    }

    @Test
    fun `test that invoke marks the migration done when the legacy record is unreadable`() =
        runTest {
            whenever(mediaPlayerRepository.isVideoPlaybackInfoMigrated()).thenReturn(false)
            whenever(mediaPlayerRepository.getLegacyVideoPlaybackInfos()).thenReturn(null)

            underTest()

            verify(mediaPlayerRepository, never()).insertMigratedVideoPlaybackInfos(any())
            verify(mediaPlayerRepository).setVideoPlaybackInfoMigrated()
        }

    @Test
    fun `test that invoke drops legacy records the player would never keep`() = runTest {
        val kept = playbackInfo(handle = 1L, currentPosition = 100_000L)
        whenever(mediaPlayerRepository.isVideoPlaybackInfoMigrated()).thenReturn(false)
        whenever(mediaPlayerRepository.getLegacyVideoPlaybackInfos()).thenReturn(
            listOf(
                kept,
                playbackInfo(handle = 2L, duration = 0L, currentPosition = 100_000L),
                playbackInfo(handle = 3L, currentPosition = CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS),
                playbackInfo(
                    handle = 4L,
                    currentPosition = totalDuration - CWLO_NEAR_COMPLETION_THRESHOLD_MS + 1,
                ),
            )
        )

        underTest()

        verify(mediaPlayerRepository).insertMigratedVideoPlaybackInfos(listOf(kept))
        verify(mediaPlayerRepository).setVideoPlaybackInfoMigrated()
    }

    @Test
    fun `test that invoke marks the migration done when every legacy record is dropped`() =
        runTest {
            whenever(mediaPlayerRepository.isVideoPlaybackInfoMigrated()).thenReturn(false)
            whenever(mediaPlayerRepository.getLegacyVideoPlaybackInfos()).thenReturn(
                listOf(playbackInfo(currentPosition = 0L))
            )

            underTest()

            verify(mediaPlayerRepository, never()).insertMigratedVideoPlaybackInfos(any())
            verify(mediaPlayerRepository).setVideoPlaybackInfoMigrated()
        }

    @Test
    fun `test that invoke does not mark the migration done when the legacy record cannot be read`() =
        runTest {
            whenever(mediaPlayerRepository.isVideoPlaybackInfoMigrated()).thenReturn(false)
            whenever(mediaPlayerRepository.getLegacyVideoPlaybackInfos())
                .thenThrow(RuntimeException("read failed"))

            assertThrows<RuntimeException> { underTest() }

            verify(mediaPlayerRepository, never()).setVideoPlaybackInfoMigrated()
        }

    @Test
    fun `test that invoke marks the migration done when the legacy record is empty`() = runTest {
        whenever(mediaPlayerRepository.isVideoPlaybackInfoMigrated()).thenReturn(false)
        whenever(mediaPlayerRepository.getLegacyVideoPlaybackInfos()).thenReturn(emptyList())

        underTest()

        verify(mediaPlayerRepository, never()).insertMigratedVideoPlaybackInfos(any())
        verify(mediaPlayerRepository).setVideoPlaybackInfoMigrated()
    }
}
