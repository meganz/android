package mega.privacy.android.core.ui.controls.chat.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.progressindicator.MegaLinearProgressIndicator
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.extensions.body4
import mega.privacy.android.core.ui.theme.extensions.conditional
import mega.privacy.android.core.ui.theme.tokens.TextColor

internal const val INVALID_TIMESTAMP = "--:--"

/**
 * Compose view for voice clip message
 *
 * @param isMe whether message is sent from me
 * @param timestamp timestamp of the voice clip. Show total duration of the voice clip when pause;
 *                  show elapsed time when playing.
 * @param modifier modifier
 * @param exists whether the voice clip exists
 * @param loadProgress loading progress of the message. null if already loaded. The value can be 0-1.
 * @param playProgress playing progress of the audio. null if not playing. The value can be 0-1.
 * @param isPlaying Whether voice clip is playing. Default is false.
 * @param onPlayClicked Callback when play button is clicked.
 */
@Composable
fun CoreVoiceClipMessageView(
    isMe: Boolean,
    timestamp: String?,
    interactionEnabled: Boolean,
    modifier: Modifier = Modifier,
    exists: Boolean = true,
    loadProgress: Float? = null,
    playProgress: Float? = null,
    isPlaying: Boolean = false,
    onPlayClicked: () -> Unit = {},
    onSeek: (Float) -> Unit = {},
) {
    Box(
        modifier = modifier
            .size(width = 209.dp, height = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = when {
                    !exists -> MegaTheme.colors.button.disabled
                    isMe -> MegaTheme.colors.icon.accent
                    else -> MegaTheme.colors.background.surface2
                }
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .padding(start = 12.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayButton(
                isMe = isMe,
                exists = exists,
                isPlaying = isPlaying,
                onPlayClicked = {
                    if (loadProgress != null)
                        return@PlayButton

                    onPlayClicked()
                },
                interactionEnabled = interactionEnabled,
            )
            PlaySlider(
                isMe = isMe,
                progressByMediaPlayer = playProgress,
                modifier = Modifier.padding(start = 8.dp),
                exists = exists,
                onValueChange = onSeek,
            )
            TimestampText(isMe = isMe, timestamp = timestamp, exists = exists)
        }
        LoadOverlay(loadProgress = loadProgress)
        LoadProgress(loadProgress = loadProgress, exists = exists)
    }
}

@Composable
private fun LoadProgress(loadProgress: Float?, exists: Boolean) {
    loadProgress?.let {
        if (exists) {
            MegaLinearProgressIndicator(
                progress = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .border(width = 0.dp, color = Color.Transparent)
                    .testTag(VOICE_CLIP_MESSAGE_VIEW_LOAD_PROGRESS_INDICATOR_TEST_TAG),
            )
        }
    }
}

/**
 * The slider user can view and control the progress of voice clip playback.
 *
 * @param isMe whether message is sent from me
 * @param progressByMediaPlayer play progress of the audio. null if already loaded. The value can be 0-1.
 * @param exists whether the voice clip exists
 * @param onValueChange callback when the progress is changed
 * @param onValueChangeFinished callback when the progress is changed finished,
 *                  typically when drag or click is finished.
 */
@Composable
private fun PlaySlider(
    isMe: Boolean,
    progressByMediaPlayer: Float?,
    exists: Boolean,
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit = { },
    onValueChangeFinished: () -> Unit = { },
) {
    // @formatter:off
    val heightList = listOf(12, 12, 18, 14, 8, 12, 16, 20, 20, 24, 8, 32, 14, 14, 14,
        12, 12, 12, 12, 12, 12, 12, 12, 12, 12)
    // @formatter:on

    var progressByUser by remember { mutableStateOf(progressByMediaPlayer) }
    var isFingerDown by remember { mutableStateOf(false) }
    val widthInDp = 99
    val heightInDp = 40
    val density = LocalDensity.current
    val widthInPx = remember { with(density) { widthInDp.dp.toPx() }.toFloat() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .size(width = widthInDp.dp, height = heightInDp.dp)
            .testTag(VOICE_CLIP_MESSAGE_VIEW_SLIDER_TEST_TAG)
            .pointerInput(null) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press) {
                            isFingerDown = true
                        } else if (event.type == PointerEventType.Release) {
                            isFingerDown = false
                            onValueChangeFinished()
                        }

                        val x = event.changes.first().position.x.coerceIn(0f, widthInPx)
                        val newProgress = x / widthInPx
                        if (progressByUser != newProgress) {
                            progressByUser = newProgress
                            onValueChange(newProgress)
                        }

                        // If user is using the slider, consume the event to prevent parent to handle it.
                        if (isFingerDown) {
                            event.changes
                                .first()
                                .consume()
                        }
                    }
                }
            },
    ) {
        val progressToShow = if (isFingerDown) progressByUser else progressByMediaPlayer
        val progressPosition = progressToShow?.let { (it * heightList.size).toInt() } ?: 0
        val color =
            if (isMe) MegaTheme.colors.icon.inverse else MegaTheme.colors.text.onColorDisabled

        heightList.forEachIndexed { index, height ->
            val alpha = if (index < progressPosition) 1f else 0.5f
            Box(
                modifier = Modifier
                    .size(width = 2.dp, height = height.dp)
                    .background(
                        color = if (exists) color.copy(alpha = alpha) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
            )
        }
    }
}

@Composable
private fun TimestampText(isMe: Boolean, timestamp: String?, exists: Boolean) {
    MegaText(
        modifier = Modifier
            .padding(start = 8.dp)
            .testTag(VOICE_CLIP_MESSAGE_VIEW_TIMESTAMP_TEST_TAG),
        text = timestamp ?: INVALID_TIMESTAMP,
        textColor = when {
            !exists -> TextColor.Disabled
            isMe -> TextColor.Inverse
            else -> TextColor.Secondary
        },
        style = MaterialTheme.typography.body4,
    )
}

@Composable
private fun PlayButton(
    isMe: Boolean,
    isPlaying: Boolean,
    interactionEnabled: Boolean,
    modifier: Modifier = Modifier,
    exists: Boolean = true,
    onPlayClicked: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                color = if (exists) MegaTheme.colors.background.blur
                else MegaTheme.colors.button.disabled,
            )
            .conditional(interactionEnabled) {
                clickable(onClick = onPlayClicked)
            }
            .testTag(VOICE_CLIP_MESSAGE_VIEW_PLAY_BUTTON_TEST_TAG),
        contentAlignment = Alignment.Center,
    ) {
        val iconId =
            if (isPlaying) R.drawable.ic_pause_voice_clip else R.drawable.ic_play_voice_clip
        Icon(
            imageVector = ImageVector.vectorResource(iconId),
            contentDescription = "Play voice clip",
            tint = if (!exists || isMe) MegaTheme.colors.icon.inverse else MegaTheme.colors.icon.onColor
        )
    }
}

/**
 * Show an overlay when voice clip is loading.
 *
 * @param loadProgress loading progress of the message. null if already loaded. The value can be 0-1.
 */
@Composable
private fun LoadOverlay(loadProgress: Float?) {
    loadProgress?.let {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = if (isSystemInDarkTheme()) Color.Black.copy(alpha = 0.5f)
                    else Color.White.copy(alpha = 0.5f),
                )
        )
    }
}

@CombinedThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(Provider::class) parameter: VoiceClipMessageViewPreviewParameter,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        CoreVoiceClipMessageView(
            isMe = parameter.isMe,
            timestamp = parameter.timestamp,
            loadProgress = parameter.loadProgress,
            playProgress = parameter.playProgress,
            isPlaying = parameter.isPlaying,
            exists = parameter.exists,
            interactionEnabled = true,
        )
    }
}

private class VoiceClipMessageViewPreviewParameter(
    val isMe: Boolean = true,
    val timestamp: String = "00:49",
    val loadProgress: Float? = null,
    val playProgress: Float? = null,
    val isPlaying: Boolean = true,
    val exists: Boolean = true,
)

private class Provider : PreviewParameterProvider<VoiceClipMessageViewPreviewParameter> {
    override val values: Sequence<VoiceClipMessageViewPreviewParameter>
        get() = sequenceOf(
            VoiceClipMessageViewPreviewParameter(isPlaying = false),
            VoiceClipMessageViewPreviewParameter(isMe = false, isPlaying = false),
            VoiceClipMessageViewPreviewParameter(loadProgress = .5f),
            VoiceClipMessageViewPreviewParameter(isMe = false, loadProgress = .5f),
            VoiceClipMessageViewPreviewParameter(playProgress = .2f),
            VoiceClipMessageViewPreviewParameter(isMe = false, playProgress = .2f),
            VoiceClipMessageViewPreviewParameter(playProgress = .5f),
            VoiceClipMessageViewPreviewParameter(isMe = false, playProgress = .5f),
            VoiceClipMessageViewPreviewParameter(playProgress = .8f),
            VoiceClipMessageViewPreviewParameter(isMe = false, playProgress = .8f),
            VoiceClipMessageViewPreviewParameter(
                timestamp = "--:--",
                exists = false,
                isPlaying = false
            ),
            VoiceClipMessageViewPreviewParameter(
                isMe = false,
                timestamp = "--:--",
                exists = false,
                isPlaying = false
            ),
        )
}

internal const val VOICE_CLIP_MESSAGE_VIEW_PLAY_BUTTON_TEST_TAG =
    "chat_voice_clip_message_view:play_button"
internal const val VOICE_CLIP_MESSAGE_VIEW_LOAD_PROGRESS_INDICATOR_TEST_TAG =
    "chat_voice_clip_message_view:load_progress_indicator"

internal const val VOICE_CLIP_MESSAGE_VIEW_TIMESTAMP_TEST_TAG =
    "chat_voice_clip_message_view:timestamp"
internal const val VOICE_CLIP_MESSAGE_VIEW_SLIDER_TEST_TAG = "chat_voice_clip_message_view:slider"
