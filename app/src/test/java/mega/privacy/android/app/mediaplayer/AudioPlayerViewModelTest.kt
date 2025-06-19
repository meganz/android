package mega.privacy.android.app.mediaplayer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.model.AudioSpeedPlaybackItem
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class AudioPlayerViewModelTest {
    private lateinit var underTest: AudioPlayerViewModel

    private val mediaPlayerGateway = mock<MediaPlayerGateway>()

    @BeforeEach
    fun setUp() {
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = AudioPlayerViewModel(
            mediaPlayerGateway = mediaPlayerGateway,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            mediaPlayerGateway
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
}