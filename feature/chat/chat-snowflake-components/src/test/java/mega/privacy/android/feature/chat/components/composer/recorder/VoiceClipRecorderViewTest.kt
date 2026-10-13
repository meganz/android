package mega.privacy.android.feature.chat.components.composer.recorder

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

@RunWith(AndroidJUnit4::class)
class VoiceClipRecorderViewTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val timeSource = TestTimeSource()
    private val events = mutableListOf<VoiceClipRecordEvent>()
    private var isPermissionDenied = false

    @Test
    fun `test that the overlay shows and Start is sent when the user presses the button`() {
        initComposeRuleContent()

        recordButton().performTouchInput { down(center) }

        assertThat(events).containsExactly(VoiceClipRecordEvent.Start)
        composeRule.onNodeWithTag(VOICE_CLIP_RECORDER_OVERLAY_TAG).assertExists()
        composeRule.onNodeWithText(SLIDE_TO_CANCEL).assertExists()
    }

    @Test
    fun `test that Finish is sent and the overlay hides when the user releases after the minimum duration`() {
        initComposeRuleContent()

        recordButton().performTouchInput { down(center) }
        timeSource += 1.seconds
        recordButton().performTouchInput { up() }

        assertThat(events)
            .containsExactly(VoiceClipRecordEvent.Start, VoiceClipRecordEvent.Finish)
            .inOrder()
        composeRule.onNodeWithTag(VOICE_CLIP_RECORDER_OVERLAY_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that Cancel is sent and the hint shows when the user releases too early`() {
        initComposeRuleContent()

        recordButton().performTouchInput {
            down(center)
            up()
        }

        assertThat(events)
            .containsExactly(VoiceClipRecordEvent.Start, VoiceClipRecordEvent.Cancel)
            .inOrder()
        composeRule.onNodeWithText(TAP_AND_HOLD).assertExists()
    }

    @Test
    fun `test that Cancel is sent when the user slides towards the start past the threshold`() {
        initComposeRuleContent()

        recordButton().performTouchInput {
            down(center)
            moveBy(Offset(-(CANCEL_THRESHOLD + 10f), 0f))
        }

        assertThat(events)
            .containsExactly(VoiceClipRecordEvent.Start, VoiceClipRecordEvent.Cancel)
            .inOrder()
        composeRule.onNodeWithTag(VOICE_CLIP_RECORDER_OVERLAY_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that no event is sent after Cancel when the user releases after sliding to cancel`() {
        initComposeRuleContent()

        recordButton().performTouchInput {
            down(center)
            moveBy(Offset(-(CANCEL_THRESHOLD + 10f), 0f))
        }
        timeSource += 2.seconds
        recordButton().performTouchInput { up() }

        assertThat(events)
            .containsExactly(VoiceClipRecordEvent.Start, VoiceClipRecordEvent.Cancel)
            .inOrder()
    }

    @Test
    fun `test that recording continues when the user slides towards the start less than the threshold`() {
        initComposeRuleContent()

        recordButton().performTouchInput {
            down(center)
            moveBy(Offset(-(CANCEL_THRESHOLD - 10f), 0f))
        }

        assertThat(events).containsExactly(VoiceClipRecordEvent.Start)
        composeRule.onNodeWithTag(VOICE_CLIP_RECORDER_OVERLAY_TAG).assertExists()
    }

    @Test
    fun `test that Lock is sent and the locked controls show when the user slides up past the threshold`() {
        initComposeRuleContent()

        recordButton().performTouchInput {
            down(center)
            moveBy(Offset(0f, -(LOCK_THRESHOLD + 10f)))
        }

        assertThat(events)
            .containsExactly(VoiceClipRecordEvent.Start, VoiceClipRecordEvent.Lock)
            .inOrder()
        composeRule.onNodeWithTag(VOICE_CLIP_RECORDER_CANCEL_TAG).assertExists()
        composeRule.onNodeWithTag(VOICE_CLIP_RECORDER_SEND_TAG).assertExists()
        composeRule.onNodeWithTag(VOICE_CLIP_RECORDER_LOCK_BAR_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(VOICE_CLIP_RECORDER_SCROLLER_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that recording continues when the user releases a locked recording`() {
        initComposeRuleContent()

        lockRecording()

        assertThat(events)
            .containsExactly(VoiceClipRecordEvent.Start, VoiceClipRecordEvent.Lock)
            .inOrder()
        composeRule.onNodeWithTag(VOICE_CLIP_RECORDER_OVERLAY_TAG).assertExists()
    }

    @Test
    fun `test that Cancel is sent when the user taps cancel on a locked recording`() {
        initComposeRuleContent()
        lockRecording()

        composeRule.onNodeWithTag(VOICE_CLIP_RECORDER_CANCEL_TAG).performClick()

        assertThat(events.last()).isEqualTo(VoiceClipRecordEvent.Cancel)
        composeRule.onNodeWithTag(VOICE_CLIP_RECORDER_OVERLAY_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that Finish is sent when the user taps send on a locked recording`() {
        initComposeRuleContent()
        lockRecording()

        composeRule.onNodeWithTag(VOICE_CLIP_RECORDER_SEND_TAG).performClick()

        assertThat(events.last()).isEqualTo(VoiceClipRecordEvent.Finish)
        composeRule.onNodeWithTag(VOICE_CLIP_RECORDER_OVERLAY_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the permission is requested and Start is not sent when the permission is not granted`() {
        initComposeRuleContent(isPermissionGranted = false)

        recordButton().performTouchInput { down(center) }

        assertThat(events).isEmpty()
        composeRule.onNodeWithTag(VOICE_CLIP_RECORDER_OVERLAY_TAG).assertDoesNotExist()
        assertThat(shadowOf(composeRule.activity).lastRequestedPermission.requestedPermissions)
            .asList()
            .containsExactly(Manifest.permission.RECORD_AUDIO)
    }

    @Test
    fun `test that onRecordAudioPermissionDenied is called when the user denies the permission`() {
        initComposeRuleContent(isPermissionGranted = false)
        recordButton().performTouchInput {
            down(center)
            up()
        }

        val request = shadowOf(composeRule.activity).lastRequestedPermission
        composeRule.runOnUiThread {
            @Suppress("DEPRECATION")
            composeRule.activity.onRequestPermissionsResult(
                request.requestCode,
                request.requestedPermissions,
                intArrayOf(PackageManager.PERMISSION_DENIED),
            )
        }
        composeRule.waitForIdle()

        assertThat(isPermissionDenied).isTrue()
    }

    @Test
    fun `test that Cancel is sent when the view leaves the composition during a recording`() {
        var isShown by mutableStateOf(true)
        initComposeRuleContent(isShown = { isShown })

        recordButton().performTouchInput { down(center) }
        isShown = false
        composeRule.waitForIdle()

        assertThat(events)
            .containsExactly(VoiceClipRecordEvent.Start, VoiceClipRecordEvent.Cancel)
            .inOrder()
    }

    private fun lockRecording() {
        recordButton().performTouchInput {
            down(center)
            moveBy(Offset(0f, -(LOCK_THRESHOLD + 10f)))
            up()
        }
    }

    private fun recordButton() = composeRule.onNodeWithTag(VOICE_CLIP_RECORDER_BUTTON_TAG)

    private fun initComposeRuleContent(
        isPermissionGranted: Boolean = true,
        isShown: () -> Boolean = { true },
    ) {
        if (isPermissionGranted) {
            shadowOf(composeRule.activity.application)
                .grantPermissions(Manifest.permission.RECORD_AUDIO)
        } else {
            shadowOf(composeRule.activity.application)
                .denyPermissions(Manifest.permission.RECORD_AUDIO)
        }
        val state = VoiceClipRecorderState(
            lockThresholdPx = LOCK_THRESHOLD,
            cancelThresholdPx = CANCEL_THRESHOLD,
            timeSource = timeSource,
        )
        composeRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isShown()) {
                    VoiceClipRecorderView(
                        onVoiceClipEvent = { events.add(it) },
                        onRecordAudioPermissionDenied = { isPermissionDenied = true },
                        slideToCancelText = SLIDE_TO_CANCEL,
                        tapAndHoldHintText = TAP_AND_HOLD,
                        state = state,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                    )
                }
            }
        }
    }

    companion object {
        private const val LOCK_THRESHOLD = 100f
        private const val CANCEL_THRESHOLD = 300f
        private const val SLIDE_TO_CANCEL = "Slide to cancel"
        private const val TAP_AND_HOLD = "Tap and hold to record"
    }
}
