package mega.privacy.android.feature.mediaplayer.videoplayer.presentation

import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.feature.mediaplayer.videoplayer.navigation.VideoPlayerLaunchSourceHolder
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model.VideoPlayerLaunchSource
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model.VideoPlayerLaunchSourceType
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model.VideoPlayerUiState
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VideoPlayerViewModelTest {

    private val launchSourceHolder = mock<VideoPlayerLaunchSourceHolder>()

    private lateinit var underTest: VideoPlayerViewModel

    @BeforeEach
    fun setUp() {
        reset(launchSourceHolder)
    }

    private fun initViewModel() {
        underTest = VideoPlayerViewModel(
            launchId = LAUNCH_ID,
            launchSourceHolder = launchSourceHolder,
        )
    }

    private fun launchSource(
        contentUri: Uri? = mock(),
        handle: Long = TEST_HANDLE,
        fileName: String? = TEST_FILE_NAME,
        sourceType: VideoPlayerLaunchSourceType = VideoPlayerLaunchSourceType.VideoBrowse,
    ) = VideoPlayerLaunchSource(
        contentUri = contentUri,
        handle = handle,
        fileName = fileName,
        sourceType = sourceType,
    )

    private suspend fun assertRejected() {
        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(VideoPlayerUiState.InvalidLaunchSource)
        }
    }

    @Test
    fun `test that uiState emits Data when the holder returns a launch source`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource())
        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(VideoPlayerUiState.Data::class.java)
            with(state as VideoPlayerUiState.Data) {
                assertThat(currentPlayingHandle).isEqualTo(TEST_HANDLE)
                assertThat(currentPlayingItemName).isEqualTo(TEST_FILE_NAME)
                assertThat(launchSourceType).isEqualTo(VideoPlayerLaunchSourceType.VideoBrowse)
            }
        }
    }

    @Test
    fun `test that uiState emits InvalidLaunchSource when the holder returns nothing`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(null)
        initViewModel()

        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(VideoPlayerUiState.InvalidLaunchSource)
        }
    }

    @Test
    fun `test that uiState emits InvalidLaunchSource when the launch source type is Invalid`() =
        runTest {
            whenever(launchSourceHolder.consume(LAUNCH_ID))
                .thenReturn(launchSource(sourceType = VideoPlayerLaunchSourceType.Invalid))
            initViewModel()

            assertRejected()
        }

    @Test
    fun `test that uiState emits InvalidLaunchSource when the content URI is missing`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource(contentUri = null))
        initViewModel()

        assertRejected()
    }

    @Test
    fun `test that uiState emits InvalidLaunchSource when the node handle is invalid`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID))
            .thenReturn(launchSource(handle = INVALID_HANDLE))
        initViewModel()

        assertRejected()
    }

    @Test
    fun `test that uiState emits InvalidLaunchSource when the file name is missing`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource(fileName = null))
        initViewModel()

        assertRejected()
    }

    @Test
    fun `test that uiState emits Data when the launch source type is Unknown`() = runTest {
        whenever(launchSourceHolder.consume(LAUNCH_ID))
            .thenReturn(launchSource(sourceType = VideoPlayerLaunchSourceType.Unknown))
        initViewModel()

        underTest.uiState.test {
            assertThat(awaitItem()).isInstanceOf(VideoPlayerUiState.Data::class.java)
        }
    }

    @Test
    fun `test that the launch source is consumed with the id the ViewModel was created with`() =
        runTest {
            whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource())
            initViewModel()

            underTest.uiState.test { awaitItem() }

            verify(launchSourceHolder).consume(LAUNCH_ID)
        }

    @Test
    fun `test that the launch source is consumed once however often the state is collected`() =
        runTest {
            whenever(launchSourceHolder.consume(LAUNCH_ID)).thenReturn(launchSource())
            initViewModel()

            underTest.uiState.test { awaitItem() }
            underTest.uiState.test { awaitItem() }

            // The holder hands a source out once, so a second read would come back empty and the
            // player would close itself mid-session.
            verify(launchSourceHolder).consume(LAUNCH_ID)
        }

    private companion object {
        const val LAUNCH_ID = "launch-id"
        const val TEST_HANDLE = 123456L
        const val TEST_FILE_NAME = "video.mp4"
    }
}
