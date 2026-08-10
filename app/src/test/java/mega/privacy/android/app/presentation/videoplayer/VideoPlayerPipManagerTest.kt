package mega.privacy.android.app.presentation.videoplayer

import android.content.pm.PackageManager
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.lang.ref.WeakReference

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoPlayerPipManagerTest {

    private lateinit var mediaPlayerGateway: MediaPlayerGateway
    private lateinit var packageManager: PackageManager
    private var finishCalled = false
    private var launchMainAppCalled = false
    private var isTaskRootValue = false
    private lateinit var underTest: VideoPlayerPipManager

    @AfterEach
    fun tearDown() {
        VideoPlayerPipManager.activePipInstance = null
    }

    @BeforeEach
    fun setUp() {
        VideoPlayerPipManager.activePipInstance = null
        mediaPlayerGateway = mock()
        packageManager = mock()
        finishCalled = false
        launchMainAppCalled = false
        isTaskRootValue = false
        whenever(packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE))
            .thenReturn(false)
        underTest = buildManager()
    }

    // region onPipModeChanged / onStart

    @Test
    fun `test that onPipModeChanged sets isExitingPipMode when exiting PIP`() {
        underTest.onPipModeChanged(isInPipMode = false)

        val result = underTest.onStop()

        assertThat(result).isTrue()
    }

    @Test
    fun `test that onPipModeChanged does not set isExitingPipMode when entering PIP`() {
        underTest.onPipModeChanged(isInPipMode = true)

        val result = underTest.onStop()

        assertThat(result).isFalse()
    }

    @Test
    fun `test that onStart resets isExitingPipMode`() {
        underTest.onPipModeChanged(isInPipMode = false)
        underTest.onStart()

        val result = underTest.onStop()

        assertThat(result).isFalse()
    }

    // endregion

    // region onStop

    @Test
    fun `test that onStop returns true and calls finish when isExitingPipMode is true`() {
        underTest.onPipModeChanged(isInPipMode = false)

        val result = underTest.onStop()

        assertThat(result).isTrue()
        assertThat(finishCalled).isTrue()
    }

    @Test
    fun `test that onStop launches main app when isExitingPipMode and isTaskRoot`() {
        isTaskRootValue = true
        underTest.onPipModeChanged(isInPipMode = false)

        underTest.onStop()

        assertThat(launchMainAppCalled).isTrue()
    }

    @Test
    fun `test that onStop does not launch main app when isExitingPipMode and not isTaskRoot`() {
        isTaskRootValue = false
        underTest.onPipModeChanged(isInPipMode = false)

        underTest.onStop()

        assertThat(launchMainAppCalled).isFalse()
    }

    @Test
    fun `test that onStop returns true when isBeingReplacedByNewInstance is true`() {
        // Simulate a new manager taking over: put underTest in PIP mode, then let another
        // manager call initialize() which marks underTest as replaced and calls its onFinish.
        underTest.onPipModeChanged(isInPipMode = true)
        VideoPlayerPipManager.activePipInstance = WeakReference(underTest)
        buildManager().initialize()
        finishCalled = false // reset — only assert what onStop does, not what initialize did

        val result = underTest.onStop()

        assertThat(result).isTrue()
        assertThat(finishCalled).isFalse()
    }

    @Test
    fun `test that onStop returns false when no flag is set`() {
        val result = underTest.onStop()

        assertThat(result).isFalse()
    }

    // endregion

    // region onDestroy

    @Test
    fun `test that onDestroy clears activePipInstance when it points to this manager`() {
        VideoPlayerPipManager.activePipInstance = WeakReference(underTest)

        underTest.onDestroy()

        assertThat(VideoPlayerPipManager.activePipInstance).isNull()
    }

    @Test
    fun `test that onDestroy does not clear activePipInstance when it points to another manager`() {
        val otherManager = buildManager()
        VideoPlayerPipManager.activePipInstance = WeakReference(otherManager)

        underTest.onDestroy()

        assertThat(VideoPlayerPipManager.activePipInstance?.get()).isEqualTo(otherManager)
    }

    // endregion

    // region initialize

    @Test
    fun `test that initialize sets activePipInstance to the current manager`() {
        underTest.initialize()

        assertThat(VideoPlayerPipManager.activePipInstance?.get()).isEqualTo(underTest)
    }

    @Test
    fun `test that initialize finishes existing PIP instance when one is in PIP mode`() {
        var existingFinishCalled = false
        val existingManager = buildManager(onFinish = { existingFinishCalled = true })
        existingManager.onPipModeChanged(isInPipMode = true)
        VideoPlayerPipManager.activePipInstance = WeakReference(existingManager)

        underTest.initialize()

        assertThat(existingFinishCalled).isTrue()
        verify(mediaPlayerGateway).playerStop()
        verify(mediaPlayerGateway).playerRelease()
    }

    @Test
    fun `test that initialize does not finish existing instance when it is not in PIP mode`() {
        var existingFinishCalled = false
        val existingManager = buildManager(onFinish = { existingFinishCalled = true })
        VideoPlayerPipManager.activePipInstance = WeakReference(existingManager)

        underTest.initialize()

        assertThat(existingFinishCalled).isFalse()
    }

    // endregion

    // region helpers

    private fun buildManager(
        onFinish: () -> Unit = { finishCalled = true },
    ) = VideoPlayerPipManager(
        isPipEnabled = { false },
        getVideoSize = { null },
        onEnterPipMode = {},
        isTaskRoot = { isTaskRootValue },
        onLaunchMainApp = { launchMainAppCalled = true },
        onFinish = onFinish,
        mediaPlayerGateway = mediaPlayerGateway,
        packageManager = packageManager,
    )

    // endregion
}
