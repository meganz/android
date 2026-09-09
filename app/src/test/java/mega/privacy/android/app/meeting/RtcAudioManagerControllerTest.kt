package mega.privacy.android.app.meeting

import android.os.Build
import android.os.Looper
import mega.privacy.android.app.interfaces.OnProximitySensorListener
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Robolectric tests for [RtcAudioManagerController].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], manifest = Config.NONE)
class RtcAudioManagerControllerTest {

    private val rtcAudioManagerGateway = mock<RTCAudioManagerGateway>()
    private val callLocalVideoController = mock<CallLocalVideoController>()

    private val underTest =
        RtcAudioManagerController(rtcAudioManagerGateway, callLocalVideoController)

    @Test
    fun `test that createOrUpdateAudioManager starts monitoring and updates the gateway on the main thread`() {
        val isSpeakerOn = true
        val type = 42

        underTest.createOrUpdateAudioManager(isSpeakerOn, type)
        shadowOf(Looper.getMainLooper()).idle()

        verify(callLocalVideoController).startMonitoring()
        verify(rtcAudioManagerGateway).createOrUpdateAudioManager(isSpeakerOn, type)
    }

    @Test
    fun `test that removeRTCAudioManagerRingIn delegates to the gateway`() {
        underTest.removeRTCAudioManagerRingIn()

        verify(rtcAudioManagerGateway).removeRTCAudioManagerRingIn()
    }

    @Test
    fun `test that startProximitySensor routes proximity changes to the local video controller`() {
        underTest.startProximitySensor()

        val listenerCaptor = argumentCaptor<OnProximitySensorListener>()
        verify(rtcAudioManagerGateway).startProximitySensor(listenerCaptor.capture())
        listenerCaptor.firstValue.needToUpdate(true)
        verify(callLocalVideoController).onProximityChanged(true)
    }
}
