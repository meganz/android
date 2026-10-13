package mega.privacy.android.feature.chat.components.composer.recorder

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * Screenshot tests for the voice clip recorder overlay, covering the hold, slide to cancel,
 * slide to lock and locked states, and the record button.
 */
class VoiceClipRecorderScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun VoiceClipRecorderOverlayHolding() {
        Overlay(isLocked = false, dragOffset = Offset.Zero)
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun VoiceClipRecorderOverlaySlidingToCancel() {
        Overlay(isLocked = false, dragOffset = Offset(-120f, 0f))
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun VoiceClipRecorderOverlaySlidingToLock() {
        Overlay(isLocked = false, dragOffset = Offset(0f, -60f))
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun VoiceClipRecorderOverlayLocked() {
        Overlay(isLocked = true, dragOffset = Offset.Zero)
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun VoiceClipRecorderViewIdle() {
        AndroidThemeForPreviews {
            VoiceClipRecorderView(
                onVoiceClipEvent = {},
                onRecordAudioPermissionDenied = {},
                slideToCancelText = "Slide to cancel",
                tapAndHoldHintText = "Tap and hold to record",
            )
        }
    }

    @Composable
    private fun Overlay(isLocked: Boolean, dragOffset: Offset) {
        AndroidThemeForPreviews {
            VoiceClipRecorderOverlay(
                isLocked = isLocked,
                dragOffset = dragOffset,
                elapsedSeconds = 75,
                slideToCancelText = "Slide to cancel",
                onCancel = {},
                onSend = {},
                modifier = Modifier.width(360.dp),
            )
        }
    }
}
