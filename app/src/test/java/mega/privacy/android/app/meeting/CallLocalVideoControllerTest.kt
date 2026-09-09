package mega.privacy.android.app.meeting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.meeting.gateway.CameraInUseGateway
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.environment.ScreenLockState
import mega.privacy.android.domain.usecase.call.GetChatCallInProgress
import mega.privacy.android.domain.usecase.call.ResolveLocalVideoActionUseCase
import mega.privacy.android.domain.usecase.environment.MonitorScreenLockStateUseCase
import mega.privacy.android.domain.usecase.meeting.BroadcastLocalVideoChangedDueToProximitySensorUseCase
import mega.privacy.android.domain.usecase.meeting.EnableOrDisableVideoUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CallLocalVideoControllerTest {

    private val resolveLocalVideoActionUseCase = ResolveLocalVideoActionUseCase()
    private val getChatCallInProgress = mock<GetChatCallInProgress>()
    private val cameraInUseGateway = mock<CameraInUseGateway>()
    private val enableOrDisableVideoUseCase = mock<EnableOrDisableVideoUseCase>()
    private val broadcastLocalVideoChangedDueToProximitySensorUseCase =
        mock<BroadcastLocalVideoChangedDueToProximitySensorUseCase>()
    private val rtcAudioManagerGateway = mock<RTCAudioManagerGateway>()
    private val monitorScreenLockStateUseCase = mock<MonitorScreenLockStateUseCase>()

    private lateinit var applicationScope: CoroutineScope
    private lateinit var screenLockStateFlow: MutableSharedFlow<ScreenLockState>

    @BeforeEach
    fun resetMocks() {
        reset(
            getChatCallInProgress,
            cameraInUseGateway,
            enableOrDisableVideoUseCase,
            broadcastLocalVideoChangedDueToProximitySensorUseCase,
            rtcAudioManagerGateway,
            monitorScreenLockStateUseCase,
        )
        applicationScope = CoroutineScope(UnconfinedTestDispatcher())
        screenLockStateFlow = MutableSharedFlow(
            extraBufferCapacity = 8,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
        whenever(monitorScreenLockStateUseCase()).thenReturn(screenLockStateFlow)
    }

    @AfterEach
    fun tearDown() {
        applicationScope.cancel()
    }

    private fun createUnderTest() = CallLocalVideoController(
        resolveLocalVideoActionUseCase = resolveLocalVideoActionUseCase,
        getChatCallInProgress = getChatCallInProgress,
        cameraInUseGateway = cameraInUseGateway,
        enableOrDisableVideoUseCase = enableOrDisableVideoUseCase,
        broadcastLocalVideoChangedDueToProximitySensorUseCase =
            broadcastLocalVideoChangedDueToProximitySensorUseCase,
        rtcAudioManagerGateway = rtcAudioManagerGateway,
        monitorScreenLockStateUseCase = monitorScreenLockStateUseCase,
        applicationScope = applicationScope,
    )

    private fun stubCallInProgress(hasLocalVideo: Boolean) {
        val call = ChatCall(
            chatId = CHAT_ID,
            callId = CALL_ID,
            status = ChatCallStatus.InProgress,
            hasLocalVideo = hasLocalVideo,
        )
        getChatCallInProgress.stub { onBlocking { invoke() } doReturn call }
    }

    @Test
    fun `test that startMonitoring maps Locked to disabling local video when video is enabled`() =
        runTest {
            stubCallInProgress(hasLocalVideo = true)
            val underTest = createUnderTest()
            underTest.setVideoStatus(CHAT_ID, true)

            underTest.startMonitoring()
            screenLockStateFlow.emit(ScreenLockState.Locked)

            verify(rtcAudioManagerGateway).muteOrUnMute(true)
            verify(enableOrDisableVideoUseCase).invoke(CHAT_ID, false)
            assertThat(underTest.isInTemporaryState).isTrue()
        }

    @Test
    fun `test that startMonitoring maps Unlocked to enabling local video when video is enabled`() =
        runTest {
            stubCallInProgress(hasLocalVideo = false)
            val underTest = createUnderTest()
            underTest.setVideoStatus(CHAT_ID, true)

            underTest.startMonitoring()
            screenLockStateFlow.emit(ScreenLockState.Unlocked)

            verify(enableOrDisableVideoUseCase).invoke(CHAT_ID, true)
            assertThat(underTest.isInTemporaryState).isFalse()
        }

    @Test
    fun `test that onProximityChanged with near disables local video and broadcasts the change`() =
        runTest {
            stubCallInProgress(hasLocalVideo = true)
            whenever(cameraInUseGateway.isFrontCameraInUse()).thenReturn(true)
            val underTest = createUnderTest()
            underTest.setVideoStatus(CHAT_ID, true)

            underTest.onProximityChanged(isNear = true)

            verify(broadcastLocalVideoChangedDueToProximitySensorUseCase).invoke(false)
            verify(enableOrDisableVideoUseCase).invoke(CHAT_ID, false)
            assertThat(underTest.isInTemporaryState).isTrue()
        }

    @Test
    fun `test that onProximityChanged with far enables local video and broadcasts the change`() =
        runTest {
            stubCallInProgress(hasLocalVideo = false)
            whenever(cameraInUseGateway.isFrontCameraInUse()).thenReturn(true)
            val underTest = createUnderTest()
            underTest.setVideoStatus(CHAT_ID, true)

            underTest.onProximityChanged(isNear = false)

            verify(broadcastLocalVideoChangedDueToProximitySensorUseCase).invoke(true)
            verify(enableOrDisableVideoUseCase).invoke(CHAT_ID, true)
            assertThat(underTest.isInTemporaryState).isFalse()
        }

    @Test
    fun `test that stopMonitoring stops applying actions for later screen lock events`() = runTest {
        stubCallInProgress(hasLocalVideo = true)
        val underTest = createUnderTest()
        underTest.setVideoStatus(CHAT_ID, true)

        underTest.startMonitoring()
        screenLockStateFlow.emit(ScreenLockState.Locked)

        underTest.stopMonitoring()
        screenLockStateFlow.emit(ScreenLockState.Locked)

        verify(rtcAudioManagerGateway, times(1)).muteOrUnMute(true)
    }

    @Test
    fun `test that getVideoStatus returns the value set by setVideoStatus`() {
        val underTest = createUnderTest()

        underTest.setVideoStatus(CHAT_ID, true)

        assertThat(underTest.getVideoStatus(CHAT_ID)).isTrue()
    }

    @Test
    fun `test that getVideoStatus defaults to false for an unknown chat`() {
        val underTest = createUnderTest()

        assertThat(underTest.getVideoStatus(CHAT_ID)).isFalse()
    }

    @Test
    fun `test that removeVideoStatus clears the stored value`() {
        val underTest = createUnderTest()
        underTest.setVideoStatus(CHAT_ID, true)

        underTest.removeVideoStatus(CHAT_ID)

        assertThat(underTest.getVideoStatus(CHAT_ID)).isFalse()
    }

    private companion object {
        const val CHAT_ID = 123L
        const val CALL_ID = 456L
    }
}
