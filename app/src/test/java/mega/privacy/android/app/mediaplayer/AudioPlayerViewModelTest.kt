package mega.privacy.android.app.mediaplayer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.model.AudioSpeedPlaybackItem
import mega.privacy.android.app.presentation.videoplayer.model.PlaybackPositionStatus
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetMediaPlaybackInfoUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class AudioPlayerViewModelTest {
    private lateinit var underTest: AudioPlayerViewModel

    private val mediaPlayerGateway = mock<MediaPlayerGateway>()
    private val getMediaPlaybackInfoUseCase = mock<GetMediaPlaybackInfoUseCase>()

    private val testHandle = 12345L
    private val testName = "Test Audio"
    private val testPosition = 100000L

    @BeforeEach
    fun setUp() {
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = AudioPlayerViewModel(
            mediaPlayerGateway = mediaPlayerGateway,
            getMediaPlaybackInfoUseCase = getMediaPlaybackInfoUseCase
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            mediaPlayerGateway,
            getMediaPlaybackInfoUseCase
        )
    }

    @Test
    fun `test that initial state is correct`() = runTest {
        val currentSpeed = 0.5F
        val currentSpeedItem = AudioSpeedPlaybackItem.PlaybackSpeed_0_5X
        whenever(mediaPlayerGateway.getCurrentPlaybackSpeed()).thenReturn(currentSpeed)
        initUnderTest()

        underTest.uiState.test {
            assertThat(awaitItem().currentSpeedPlayback).isEqualTo(currentSpeedItem)
        }
    }

    @Test
    fun `test that state is updated correctly when updateIsSpeedPopupShown is invoked`() = runTest {
        initUnderTest()

        underTest.uiState.test {
            assertThat(awaitItem().isSpeedPopupShown).isFalse()
            underTest.updateIsSpeedPopupShown(true)
            assertThat(awaitItem().isSpeedPopupShown).isTrue()
            underTest.updateIsSpeedPopupShown(false)
            assertThat(awaitItem().isSpeedPopupShown).isFalse()
        }
    }


    @Test
    fun `test that state is updated correctly when updateCurrentSpeedPlaybackItem is invoked`() =
        runTest {
            val currentSpeed = 0.5F
            val currentSpeedItem = AudioSpeedPlaybackItem.PlaybackSpeed_0_5X
            val updatedSpeedItem = AudioSpeedPlaybackItem.PlaybackSpeed_1_75X
            whenever(mediaPlayerGateway.getCurrentPlaybackSpeed()).thenReturn(currentSpeed)
            initUnderTest()

            underTest.uiState.test {
                assertThat(awaitItem().currentSpeedPlayback).isEqualTo(currentSpeedItem)
                underTest.updateCurrentSpeedPlaybackItem(updatedSpeedItem)
                assertThat(awaitItem().currentSpeedPlayback).isEqualTo(updatedSpeedItem)
                verify(mediaPlayerGateway).updatePlaybackSpeed(updatedSpeedItem)
            }
        }

    @Test
    fun `test that state is updated correctly when checkPlaybackPositionOfPlayingItem invoked, and playback position status is Initial`() =
        runTest {
            val testPlaybackInfo = mock<MediaPlaybackInfo> {
                on { mediaHandle }.thenReturn(testHandle)
                on { currentPosition }.thenReturn(testPosition)
            }
            whenever(getMediaPlaybackInfoUseCase(testHandle)).thenReturn(testPlaybackInfo)
            initUnderTest()

            underTest.checkPlaybackPositionOfPlayingItem(testHandle, testName)
            advanceUntilIdle()
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.showPlaybackDialog).isTrue()
                assertThat(actual.playbackPosition).isEqualTo(testPosition)
                assertThat(actual.currentPlayingHandle).isEqualTo(testHandle)
                assertThat(actual.currentPlayingItemName).isEqualTo(testName)
            }
        }

    @ParameterizedTest(name = "when playback status is {0}")
    @EnumSource(PlaybackPositionStatus::class)
    fun `test that functions are invoked expected`(status: PlaybackPositionStatus) = runTest {
        whenever(mediaPlayerGateway.getPlayWhenReady()).thenReturn(false)

        initUnderTest()
        underTest.updatePlaybackPositionStatus(status, testPosition)
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(awaitItem().showPlaybackDialog).isFalse()
            if (status == PlaybackPositionStatus.Resume) {
                verify(mediaPlayerGateway).playerSeekToPositionInMs(testPosition)
            }
            verify(mediaPlayerGateway).setPlayWhenReady(true)
        }
    }
}