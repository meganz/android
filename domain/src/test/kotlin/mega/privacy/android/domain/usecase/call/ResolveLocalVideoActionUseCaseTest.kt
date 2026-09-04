package mega.privacy.android.domain.usecase.call

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.LocalVideoAction
import mega.privacy.android.domain.entity.call.LocalVideoContext
import mega.privacy.android.domain.entity.call.LocalVideoTrigger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResolveLocalVideoActionUseCaseTest {

    private lateinit var underTest: ResolveLocalVideoActionUseCase

    @BeforeEach
    fun setUp() {
        underTest = ResolveLocalVideoActionUseCase()
    }

    private fun call(
        chatId: Long = CHAT_ID,
        status: ChatCallStatus? = ChatCallStatus.InProgress,
        hasLocalVideo: Boolean = true,
    ) = LocalVideoContext.CallInProgress(
        chatId = chatId,
        status = status,
        hasLocalVideo = hasLocalVideo,
    )

    private fun context(
        callInProgress: LocalVideoContext.CallInProgress? = call(),
        isVideoEnabled: Boolean = true,
        isScreenOn: Boolean = true,
        isFrontCameraInUse: Boolean = true,
        isDisablingLocalVideo: Boolean = false,
        isInTemporaryState: Boolean = false,
    ) = LocalVideoContext(
        callInProgress = callInProgress,
        isVideoEnabled = isVideoEnabled,
        isScreenOn = isScreenOn,
        isFrontCameraInUse = isFrontCameraInUse,
        isDisablingLocalVideo = isDisablingLocalVideo,
        isInTemporaryState = isInTemporaryState,
    )

    @ParameterizedTest(name = "trigger = {0}")
    @MethodSource("allTriggers")
    fun `test that invoke returns no actions when there is no call in progress`(
        trigger: LocalVideoTrigger,
    ) {
        val result = underTest(trigger, context(callInProgress = null))

        assertThat(result).isEmpty()
    }

    @ParameterizedTest(name = "trigger = {0}, status = {1}")
    @MethodSource("triggersWithNonActiveStatuses")
    fun `test that invoke returns no actions when the call status is not joining or in progress`(
        trigger: LocalVideoTrigger,
        status: ChatCallStatus?,
    ) {
        val result = underTest(trigger, context(callInProgress = call(status = status)))

        assertThat(result).isEmpty()
    }

    @ParameterizedTest(name = "trigger = {0}, status = {1}")
    @MethodSource("screenTriggersWithActiveStatuses")
    fun `test that invoke processes screen triggers for both joining and in progress statuses`(
        trigger: LocalVideoTrigger,
        status: ChatCallStatus,
    ) {
        val result = underTest(trigger, context(callInProgress = call(status = status)))

        assertThat(result).isNotEmpty()
    }

    @Test
    fun `test that invoke on ScreenOff with video disabled only clears the temporary state`() {
        val result = underTest(LocalVideoTrigger.ScreenOff, context(isVideoEnabled = false))

        assertThat(result).containsExactly(LocalVideoAction.SetTemporaryState(false))
    }

    @Test
    fun `test that invoke on ScreenOff with local video mutes disables video and updates flags`() {
        val result = underTest(
            LocalVideoTrigger.ScreenOff,
            context(callInProgress = call(hasLocalVideo = true)),
        )

        assertThat(result).containsExactly(
            LocalVideoAction.MuteMic,
            LocalVideoAction.SetDisablingLocalVideo(false),
            LocalVideoAction.SetTemporaryState(true),
            LocalVideoAction.SetScreenOn(false),
            LocalVideoAction.DisableVideo(CHAT_ID),
        ).inOrder()
    }

    @Test
    fun `test that invoke on ScreenOff without local video mutes and updates flags but does not disable video`() {
        val result = underTest(
            LocalVideoTrigger.ScreenOff,
            context(callInProgress = call(hasLocalVideo = false)),
        )

        assertThat(result).containsExactly(
            LocalVideoAction.MuteMic,
            LocalVideoAction.SetDisablingLocalVideo(false),
            LocalVideoAction.SetTemporaryState(true),
        ).inOrder()
    }

    @Test
    fun `test that invoke on ScreenOff ignores the screen on and front camera gates`() {
        val result = underTest(
            LocalVideoTrigger.ScreenOff,
            context(
                callInProgress = call(hasLocalVideo = true),
                isScreenOn = false,
                isFrontCameraInUse = false,
            ),
        )

        assertThat(result).containsExactly(
            LocalVideoAction.MuteMic,
            LocalVideoAction.SetDisablingLocalVideo(false),
            LocalVideoAction.SetTemporaryState(true),
            LocalVideoAction.SetScreenOn(false),
            LocalVideoAction.DisableVideo(CHAT_ID),
        ).inOrder()
    }

    @Test
    fun `test that invoke on ScreenOff disables video even when disabling local video was already true`() {
        val result = underTest(
            LocalVideoTrigger.ScreenOff,
            context(callInProgress = call(hasLocalVideo = true), isDisablingLocalVideo = true),
        )

        assertThat(result).contains(LocalVideoAction.DisableVideo(CHAT_ID))
    }

    @Test
    fun `test that invoke on UserPresent with video disabled only clears the temporary state`() {
        val result = underTest(LocalVideoTrigger.UserPresent, context(isVideoEnabled = false))

        assertThat(result).containsExactly(LocalVideoAction.SetTemporaryState(false))
    }

    @Test
    fun `test that invoke on UserPresent without local video enables video and updates flags`() {
        val result = underTest(
            LocalVideoTrigger.UserPresent,
            context(callInProgress = call(hasLocalVideo = false)),
        )

        assertThat(result).containsExactly(
            LocalVideoAction.SetDisablingLocalVideo(false),
            LocalVideoAction.SetTemporaryState(false),
            LocalVideoAction.SetScreenOn(true),
            LocalVideoAction.EnableVideo(CHAT_ID),
        ).inOrder()
    }

    @Test
    fun `test that invoke on UserPresent with local video updates flags but does not enable video`() {
        val result = underTest(
            LocalVideoTrigger.UserPresent,
            context(callInProgress = call(hasLocalVideo = true)),
        )

        assertThat(result).containsExactly(
            LocalVideoAction.SetDisablingLocalVideo(false),
            LocalVideoAction.SetTemporaryState(false),
        ).inOrder()
    }

    @Test
    fun `test that invoke on UserPresent ignores the screen on and front camera gates`() {
        val result = underTest(
            LocalVideoTrigger.UserPresent,
            context(
                callInProgress = call(hasLocalVideo = false),
                isScreenOn = false,
                isFrontCameraInUse = false,
            ),
        )

        assertThat(result).containsExactly(
            LocalVideoAction.SetDisablingLocalVideo(false),
            LocalVideoAction.SetTemporaryState(false),
            LocalVideoAction.SetScreenOn(true),
            LocalVideoAction.EnableVideo(CHAT_ID),
        ).inOrder()
    }

    @Test
    fun `test that invoke on UserPresent enables video even when disabling local video was already true`() {
        val result = underTest(
            LocalVideoTrigger.UserPresent,
            context(callInProgress = call(hasLocalVideo = false), isDisablingLocalVideo = true),
        )

        assertThat(result).contains(LocalVideoAction.EnableVideo(CHAT_ID))
    }

    @ParameterizedTest(name = "trigger = {0}")
    @MethodSource("proximityTriggers")
    fun `test that invoke on proximity returns no actions when the screen is off`(
        trigger: LocalVideoTrigger,
    ) {
        val result = underTest(trigger, context(isScreenOn = false))

        assertThat(result).isEmpty()
    }

    @ParameterizedTest(name = "trigger = {0}")
    @MethodSource("proximityTriggers")
    fun `test that invoke on proximity returns no actions when the front camera is not in use`(
        trigger: LocalVideoTrigger,
    ) {
        val result = underTest(trigger, context(isFrontCameraInUse = false))

        assertThat(result).isEmpty()
    }

    @ParameterizedTest(name = "trigger = {0}")
    @MethodSource("proximityTriggers")
    fun `test that invoke on proximity with video disabled only clears the temporary state`(
        trigger: LocalVideoTrigger,
    ) {
        val result = underTest(trigger, context(isVideoEnabled = false))

        assertThat(result).containsExactly(LocalVideoAction.SetTemporaryState(false))
    }

    @Test
    fun `test that invoke on ProximityNear with local video broadcasts and disables video`() {
        val result = underTest(
            LocalVideoTrigger.ProximityNear,
            context(callInProgress = call(hasLocalVideo = true)),
        )

        assertThat(result).containsExactly(
            LocalVideoAction.SetDisablingLocalVideo(false),
            LocalVideoAction.SetTemporaryState(true),
            LocalVideoAction.BroadcastLocalVideoChanged(false),
            LocalVideoAction.DisableVideo(CHAT_ID),
        ).inOrder()
    }

    @Test
    fun `test that invoke on ProximityNear without local video broadcasts but does not disable video`() {
        val result = underTest(
            LocalVideoTrigger.ProximityNear,
            context(callInProgress = call(hasLocalVideo = false)),
        )

        assertThat(result).containsExactly(
            LocalVideoAction.SetDisablingLocalVideo(false),
            LocalVideoAction.SetTemporaryState(true),
            LocalVideoAction.BroadcastLocalVideoChanged(false),
        ).inOrder()
    }

    @Test
    fun `test that invoke on ProximityFar without local video broadcasts and enables video`() {
        val result = underTest(
            LocalVideoTrigger.ProximityFar,
            context(callInProgress = call(hasLocalVideo = false)),
        )

        assertThat(result).containsExactly(
            LocalVideoAction.SetDisablingLocalVideo(false),
            LocalVideoAction.SetTemporaryState(false),
            LocalVideoAction.BroadcastLocalVideoChanged(true),
            LocalVideoAction.EnableVideo(CHAT_ID),
        ).inOrder()
    }

    @Test
    fun `test that invoke on ProximityFar with local video broadcasts but does not enable video`() {
        val result = underTest(
            LocalVideoTrigger.ProximityFar,
            context(callInProgress = call(hasLocalVideo = true)),
        )

        assertThat(result).containsExactly(
            LocalVideoAction.SetDisablingLocalVideo(false),
            LocalVideoAction.SetTemporaryState(false),
            LocalVideoAction.BroadcastLocalVideoChanged(true),
        ).inOrder()
    }

    @ParameterizedTest(name = "trigger = {0}")
    @MethodSource("proximityTriggers")
    fun `test that invoke on proximity never updates the screen on flag`(
        trigger: LocalVideoTrigger,
    ) {
        val near = underTest(trigger, context(callInProgress = call(hasLocalVideo = true)))
        val far = underTest(trigger, context(callInProgress = call(hasLocalVideo = false)))

        assertThat(near.filterIsInstance<LocalVideoAction.SetScreenOn>()).isEmpty()
        assertThat(far.filterIsInstance<LocalVideoAction.SetScreenOn>()).isEmpty()
    }

    @Test
    fun `test that invoke on ProximityNear disables video even when disabling local video was already true`() {
        val result = underTest(
            LocalVideoTrigger.ProximityNear,
            context(callInProgress = call(hasLocalVideo = true), isDisablingLocalVideo = true),
        )

        assertThat(result).contains(LocalVideoAction.DisableVideo(CHAT_ID))
    }

    @Test
    fun `test that invoke on ProximityFar enables video even when disabling local video was already true`() {
        val result = underTest(
            LocalVideoTrigger.ProximityFar,
            context(callInProgress = call(hasLocalVideo = false), isDisablingLocalVideo = true),
        )

        assertThat(result).contains(LocalVideoAction.EnableVideo(CHAT_ID))
    }

    @Test
    fun `test that invoke propagates the chat id to the video actions`() {
        val otherChatId = 987654L

        val disable = underTest(
            LocalVideoTrigger.ScreenOff,
            context(callInProgress = call(chatId = otherChatId, hasLocalVideo = true)),
        )
        val enable = underTest(
            LocalVideoTrigger.UserPresent,
            context(callInProgress = call(chatId = otherChatId, hasLocalVideo = false)),
        )

        assertThat(disable).contains(LocalVideoAction.DisableVideo(otherChatId))
        assertThat(enable).contains(LocalVideoAction.EnableVideo(otherChatId))
    }

    @ParameterizedTest(name = "trigger = {0}")
    @MethodSource("allTriggers")
    fun `test that invoke ignores the input temporary state flag`(trigger: LocalVideoTrigger) {
        val withTemporaryState = underTest(trigger, context(isInTemporaryState = true))
        val withoutTemporaryState = underTest(trigger, context(isInTemporaryState = false))

        assertThat(withTemporaryState).isEqualTo(withoutTemporaryState)
    }

    companion object {
        private const val CHAT_ID = 123L

        @JvmStatic
        private fun allTriggers() = listOf(
            LocalVideoTrigger.ScreenOff,
            LocalVideoTrigger.UserPresent,
            LocalVideoTrigger.ProximityNear,
            LocalVideoTrigger.ProximityFar,
        )

        @JvmStatic
        private fun proximityTriggers() = listOf(
            LocalVideoTrigger.ProximityNear,
            LocalVideoTrigger.ProximityFar,
        )

        @JvmStatic
        private fun triggersWithNonActiveStatuses(): List<Arguments> {
            val nonActiveStatuses = ChatCallStatus.entries.filterNot {
                it == ChatCallStatus.Joining || it == ChatCallStatus.InProgress
            } + null
            return allTriggers().flatMap { trigger ->
                nonActiveStatuses.map { status ->
                    Arguments.of(trigger, status)
                }
            }
        }

        @JvmStatic
        private fun screenTriggersWithActiveStatuses(): List<Arguments> {
            val screenTriggers = listOf(LocalVideoTrigger.ScreenOff, LocalVideoTrigger.UserPresent)
            val activeStatuses = listOf(ChatCallStatus.Joining, ChatCallStatus.InProgress)
            return screenTriggers.flatMap { trigger ->
                activeStatuses.map { status ->
                    Arguments.of(trigger, status)
                }
            }
        }
    }
}
