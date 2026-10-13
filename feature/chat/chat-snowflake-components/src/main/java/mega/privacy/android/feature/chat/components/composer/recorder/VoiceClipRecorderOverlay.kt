package mega.privacy.android.feature.chat.components.composer.recorder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.TextOnlyButtonM3
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.resources.R as sharedR
import kotlin.math.roundToInt

/**
 * Overlay that shows while the user records a voice clip.
 *
 * The bottom-end corner of the overlay is [VoiceClipRecorderDefaults.ScrollerSize] / 2 from the
 * centre of the record button on both axes, so that the scroller covers the record button.
 *
 * @param isLocked True to show the cancel and send controls instead of the slide to cancel hint
 * @param dragOffset Distance that the finger moved from the press position, in px
 * @param elapsedSeconds Duration of the recording, in seconds
 * @param slideToCancelText Hint that tells the user to slide towards the start to cancel
 * @param onCancel Called when the user taps cancel in the locked state
 * @param onSend Called when the user taps send in the locked state
 * @param modifier [Modifier]
 */
@Composable
internal fun VoiceClipRecorderOverlay(
    isLocked: Boolean,
    dragOffset: Offset,
    elapsedSeconds: Long,
    slideToCancelText: String,
    onCancel: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollerRadius = VoiceClipRecorderDefaults.ScrollerSize / 2
    val barEndPadding = scrollerRadius - VoiceClipRecorderDefaults.LockBarWidth / 2

    Box(
        modifier = modifier
            .height(VoiceClipRecorderDefaults.LockBarHeight + barEndPadding)
            .testTag(VOICE_CLIP_RECORDER_OVERLAY_TAG),
    ) {
        if (!isLocked) {
            LockBar(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = barEndPadding, bottom = barEndPadding),
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(end = barEndPadding, bottom = barEndPadding)
                .fillMaxWidth()
                .height(VoiceClipRecorderDefaults.BarHeight)
                .clip(CircleShape)
                .background(DSTokens.colors.background.surface1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecordingTimer(
                elapsedSeconds = elapsedSeconds,
                modifier = Modifier.padding(start = 12.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isLocked) {
                RecordingWave()
                Spacer(modifier = Modifier.weight(1f))
                TextOnlyButtonM3(
                    text = stringResource(sharedR.string.general_dialog_cancel_button),
                    onClick = onCancel,
                    modifier = Modifier.testTag(VOICE_CLIP_RECORDER_CANCEL_TAG),
                )
                IconButton(
                    onClick = onSend,
                    modifier = Modifier.testTag(VOICE_CLIP_RECORDER_SEND_TAG),
                ) {
                    MegaIcon(
                        painter = painterResource(IconPackR.drawable.ic_send_horizontal_medium_thin_outline),
                        tint = IconColor.Accent,
                        modifier = Modifier.size(24.dp),
                    )
                }
            } else {
                SlideToCancel(
                    text = slideToCancelText,
                    modifier = Modifier
                        .padding(end = VoiceClipRecorderDefaults.ScrollerSize - barEndPadding + 8.dp)
                        .offset { IntOffset(dragOffset.x.roundToInt(), 0) },
                )
            }
        }
        if (!isLocked) {
            Scroller(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) },
            )
        }
    }
}

@Composable
private fun LockBar(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(VoiceClipRecorderDefaults.LockBarWidth)
            .height(VoiceClipRecorderDefaults.LockBarHeight)
            .clip(CircleShape)
            .background(DSTokens.colors.background.surface1)
            .padding(top = 12.dp)
            .testTag(VOICE_CLIP_RECORDER_LOCK_BAR_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MegaIcon(
            painter = painterResource(IconPackR.drawable.ic_lock_medium_thin_solid),
            tint = IconColor.Secondary,
            modifier = Modifier.size(18.dp),
        )
        MegaIcon(
            painter = painterResource(IconPackR.drawable.ic_chevron_up_medium_thin_outline),
            tint = IconColor.Disabled,
            modifier = Modifier
                .padding(top = 4.dp)
                .size(18.dp),
        )
    }
}

@Composable
private fun RecordingTimer(elapsedSeconds: Long, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(DSTokens.colors.components.interactive),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(DSTokens.colors.icon.onColor),
            )
        }
        MegaText(
            text = elapsedSeconds.formatRecordingTime(),
            textColor = TextColor.Secondary,
            style = AppTheme.typography.titleSmall,
            modifier = Modifier
                .padding(start = 8.dp)
                .testTag(VOICE_CLIP_RECORDER_TIMER_TAG),
        )
    }
}

@Composable
private fun SlideToCancel(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MegaIcon(
            painter = painterResource(IconPackR.drawable.ic_chevron_left_medium_thin_outline),
            tint = IconColor.Disabled,
            modifier = Modifier.size(18.dp),
        )
        MegaText(
            text = text,
            textColor = TextColor.Placeholder,
            style = AppTheme.typography.titleSmall,
            maxLines = 1,
        )
    }
}

/**
 * Static wave, because the recorder does not receive the audio amplitude.
 */
@Composable
private fun RecordingWave(modifier: Modifier = Modifier) {
    val color = DSTokens.colors.icon.secondary
    Canvas(
        modifier = modifier
            .width(WaveBarStep * WaveBarHeights.size)
            .height(WaveHeight),
    ) {
        val barWidth = WaveBarWidth.toPx()
        val step = WaveBarStep.toPx()
        WaveBarHeights.forEachIndexed { index, fraction ->
            val barHeight = size.height * fraction
            drawRoundRect(
                color = color,
                topLeft = Offset(index * step, (size.height - barHeight) / 2),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2),
            )
        }
    }
}

@Composable
private fun Scroller(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(VoiceClipRecorderDefaults.ScrollerSize)
            .clip(CircleShape)
            .background(DSTokens.colors.components.interactive)
            .testTag(VOICE_CLIP_RECORDER_SCROLLER_TAG),
        contentAlignment = Alignment.Center,
    ) {
        MegaIcon(
            painter = painterResource(IconPackR.drawable.ic_mic_medium_thin_solid),
            tint = IconColor.OnColor,
            modifier = Modifier.size(24.dp),
        )
    }
}

internal fun Long.formatRecordingTime(): String {
    val minutes = (this / 60).toString().padStart(2, '0')
    val seconds = (this % 60).toString().padStart(2, '0')
    return "$minutes:$seconds"
}

internal object VoiceClipRecorderDefaults {
    val ScrollerSize = 80.dp
    val BarHeight = 40.dp
    val LockBarWidth = 40.dp
    val LockBarHeight = 120.dp

    /**
     * Slide distance up to lock. At this distance the top of the scroller reaches the top of
     * the lock bar.
     */
    val LockThreshold = LockBarHeight - ScrollerSize

    val CancelThreshold = 150.dp

    /**
     * Space between the start of the window and the start of the overlay.
     */
    val WindowMargin = 16.dp
}

private val WaveHeight = 24.dp
private val WaveBarWidth = 2.dp
private val WaveBarStep = 4.dp
private val WaveBarHeights = listOf(
    0.3f, 0.5f, 0.8f, 0.6f, 0.4f, 0.7f, 1f, 0.6f, 0.3f, 0.5f, 0.9f, 0.7f, 0.4f, 0.6f, 0.8f,
    0.5f, 0.3f, 0.6f, 0.9f, 0.5f,
)

internal const val VOICE_CLIP_RECORDER_OVERLAY_TAG = "voice_clip_recorder:overlay"
internal const val VOICE_CLIP_RECORDER_LOCK_BAR_TAG = "voice_clip_recorder:lock_bar"
internal const val VOICE_CLIP_RECORDER_SCROLLER_TAG = "voice_clip_recorder:scroller"
internal const val VOICE_CLIP_RECORDER_TIMER_TAG = "voice_clip_recorder:timer"
internal const val VOICE_CLIP_RECORDER_CANCEL_TAG = "voice_clip_recorder:cancel"
internal const val VOICE_CLIP_RECORDER_SEND_TAG = "voice_clip_recorder:send"

@CombinedThemePreviews
@Composable
private fun VoiceClipRecorderOverlayPreview(
    @PreviewParameter(LockedProvider::class) isLocked: Boolean,
) {
    AndroidThemeForPreviews {
        VoiceClipRecorderOverlay(
            isLocked = isLocked,
            dragOffset = Offset.Zero,
            elapsedSeconds = 75,
            slideToCancelText = "Slide to cancel",
            onCancel = {},
            onSend = {},
            modifier = Modifier.width(360.dp),
        )
    }
}

private class LockedProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(false, true)
}
