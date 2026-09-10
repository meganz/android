package mega.privacy.android.app.mediaplayer.gateway

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.feature.mediaplayer.data.AudioQueueItemFactory
import mega.privacy.android.feature.mediaplayer.data.MediaHandleStore
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AudioMediaControllerFacadeTest {

    private val context = mock<Context>()
    private val mediaHandleStore = mock<MediaHandleStore>()
    private val audioQueueItemFactory = mock<AudioQueueItemFactory>()
    private val controller = mock<MediaController> {
        on { currentTimeline } doReturn Timeline.EMPTY
        on { currentMediaItemIndex } doReturn 0
        on { currentPosition } doReturn 0L
        on { duration } doReturn 0L
    }

    private lateinit var underTest: AudioMediaControllerFacade

    @BeforeEach
    fun setUp() {
        underTest = AudioMediaControllerFacade(
            context = context,
            mainDispatcher = UnconfinedTestDispatcher(),
            mediaHandleStore = mediaHandleStore,
            audioQueueItemFactory = audioQueueItemFactory,
        )
        // The session connection cannot be established in a JVM test, so the controller is
        // attached directly to exercise the listener translation.
        underTest.controller = controller
    }

    @ParameterizedTest(name = "reason={0} -> lastItemTransitionWasAuto={1}")
    @MethodSource("transitionReasons")
    fun `test that onMediaItemTransition emits lastItemTransitionWasAuto derived from the reason`(
        reason: Int,
        expected: Boolean,
    ) = runTest {
        val mediaItem = MediaItem.Builder().setMediaId("media-id").build()

        underTest.playerListener.onMediaItemTransition(mediaItem, reason)

        underTest.playerState.test {
            val state = awaitItem()
            assertThat(state.currentMediaItemId).isEqualTo("media-id")
            assertThat(state.lastItemTransitionWasAuto).isEqualTo(expected)
        }
    }

    @Test
    fun `test that onAvailableCommandsChanged emits hasNext true when seek to next is available`() =
        runTest {
            // Player.Commands is backed by SparseBooleanArray, an android.jar stub on the JVM, so
            // the membership check is mocked instead of built.
            val commands = mock<Player.Commands> {
                on { contains(Player.COMMAND_SEEK_TO_NEXT) } doReturn true
            }

            underTest.playerListener.onAvailableCommandsChanged(commands)

            underTest.playerState.test {
                assertThat(awaitItem().hasNext).isTrue()
            }
        }

    @Test
    fun `test that onAvailableCommandsChanged emits hasNext false when seek to next is unavailable`() =
        runTest {
            val commands = mock<Player.Commands> {
                on { contains(Player.COMMAND_SEEK_TO_NEXT) } doReturn false
            }

            underTest.playerListener.onAvailableCommandsChanged(commands)

            underTest.playerState.test {
                assertThat(awaitItem().hasNext).isFalse()
            }
        }

    @Test
    fun `test that skipToNext calls seekToNext on the controller`() {
        underTest.skipToNext()

        verify(controller).seekToNext()
    }

    @Test
    fun `test that skipToPrevious calls seekToPrevious on the controller`() {
        underTest.skipToPrevious()

        verify(controller).seekToPrevious()
    }

    private fun transitionReasons(): Stream<Arguments> = Stream.of(
        Arguments.of(Player.MEDIA_ITEM_TRANSITION_REASON_AUTO, true),
        Arguments.of(Player.MEDIA_ITEM_TRANSITION_REASON_SEEK, false),
        Arguments.of(Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED, false),
        Arguments.of(Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT, false),
    )
}
