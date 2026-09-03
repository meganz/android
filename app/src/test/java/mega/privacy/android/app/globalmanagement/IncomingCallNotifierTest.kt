package mega.privacy.android.app.globalmanagement

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import mega.privacy.android.app.meeting.RtcAudioManagerController
import mega.privacy.android.app.utils.Constants
import nz.mega.sdk.MegaChatCall
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric tests for [IncomingCallNotifier].
 *
 * Robolectric supplies a real application [Context] so the legacy static
 * `LegacyAudioPlayerService.pauseAudioPlayer` (which casts a system service to `ActivityManager`)
 * runs harmlessly. [IncomingCallNotifier.showNotification] is stubbed because
 * `ChatAdvancedNotificationBuilder.newInstance` resolves a Hilt entry point at construction.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], manifest = Config.NONE)
class IncomingCallNotifierTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val rtcAudioManagerController = mock<RtcAudioManagerController>()

    private val underTest = spy(IncomingCallNotifier(context, rtcAudioManagerController)).also {
        doNothing().whenever(it).showNotification(any())
    }

    @Test
    fun `test that invoke registers the ringing audio manager and shows the notification`() {
        val incomingCall = mock<MegaChatCall>()

        underTest(incomingCall)

        verify(rtcAudioManagerController).createOrUpdateAudioManager(
            false,
            Constants.AUDIO_MANAGER_CALL_RINGING,
        )
        verify(underTest).showNotification(incomingCall)
    }

    @Test
    fun `test that invoke registers the audio manager before showing the notification`() {
        val incomingCall = mock<MegaChatCall>()

        underTest(incomingCall)

        inOrder(rtcAudioManagerController, underTest) {
            verify(rtcAudioManagerController).createOrUpdateAudioManager(
                false,
                Constants.AUDIO_MANAGER_CALL_RINGING,
            )
            verify(underTest).showNotification(incomingCall)
        }
    }
}
