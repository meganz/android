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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.progressindicator.MegaLinearProgressIndicator
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.extensions.body4
import mega.privacy.android.core.ui.theme.tokens.TextColor

internal const val VOICE_CLIP_MESSAGE_VIEW_PLAY_BUTTON_TEST_TAG =
    "chat_voice_clip_message_view:play_button"
internal const val VOICE_CLIP_MESSAGE_VIEW_LOAD_PROGRESS_INDICATOR_TEST_TAG =
    "chat_voice_clip_message_view:load_progress_indicator"


/**
 * Compose view for voice clip message
 *
 * @param isMe whether message is sent from me
 * @param timestamp timestamp of the voice clip. Show total duration of the voice clip when pause;
 *                  show elapsed time when playing.
 * @param modifier modifier
 * @param isError whether message is in error status
 * @param loadProgress loading progress of the message. null if already loaded. The value can be 0-100.
 * @param playProgress playing progress of the audio. null if not playing. The value can be 0-100.
 * @param isPlaying Whether voice clip is playing. Default is false.
 * @param onPlayClicked Callback when play button is clicked.
 */
@Composable
fun CoreVoiceClipMessageView(
    isMe: Boolean,
    timestamp: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    loadProgress: Int? = null,
    playProgress: Int? = null,
    isPlaying: Boolean = false,
    onPlayClicked: () -> Unit = {},
) {
    ChatBubble(isMe = isMe, modifier = modifier.size(width = 209.dp, height = 56.dp)) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
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
                    enabled = !isError && loadProgress == null,
                    isPlaying = isPlaying,
                    onPlayClicked = onPlayClicked
                )
                PlayProgress(
                    isMe = isMe,
                    progress = playProgress,
                    modifier = Modifier.padding(start = 8.dp)
                )
                TimestampText(isMe = isMe, timestamp = timestamp)
            }
            LoadOverlay(loadProgress = loadProgress, isError = isError)
            LoadProgress(loadProgress = loadProgress, isError = isError)
        }
    }
}

@Composable
private fun LoadProgress(loadProgress: Int?, isError: Boolean) {
    loadProgress?.let {
        if (!isError) {
            MegaLinearProgressIndicator(
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
 * Show the play progress
 *
 * @param isMe whether message is sent from me
 * @param progress loading progress of the audio. null if already loaded. The value can be 0-100.
 */
@Composable
private fun PlayProgress(
    isMe: Boolean,
    progress: Int?,
    modifier: Modifier = Modifier,
) {
    // @formatter:off
    val heightList = listOf(12, 12, 18, 14, 8, 12, 16, 20, 20, 24, 8, 32, 14, 14, 14,
        12, 12, 12, 12, 12, 12, 12, 12, 12, 12)
    // @formatter:on

    Row(
        modifier = modifier.size(width = 99.dp, height = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val dividerPos = progress?.let { (it.toFloat() / 100f * heightList.size).toInt() } ?: 0
        val color =
            if (isMe) MegaTheme.colors.icon.inverse else MegaTheme.colors.text.onColorDisabled

        heightList.forEachIndexed { index, height ->
            val alpha = if (index < dividerPos) 1f else 0.5f
            Box(
                modifier = Modifier
                    .size(width = 2.dp, height = height.dp)
                    .background(
                        color = color.copy(alpha = alpha),
                        shape = RoundedCornerShape(8.dp)
                    )
            )
        }
    }
}

@Composable
private fun TimestampText(isMe: Boolean, timestamp: String) {
    MegaText(
        modifier = Modifier.padding(start = 8.dp),
        text = timestamp,
        textColor = if (isMe) TextColor.Inverse else TextColor.Secondary,
        style = MaterialTheme.typography.body4,
    )
}

@Composable
private fun PlayButton(
    isMe: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onPlayClicked: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(
                color = MegaTheme.colors.background.blur,
                shape = CircleShape
            )
            .clickable(enabled = enabled, onClick = onPlayClicked)
            .testTag(VOICE_CLIP_MESSAGE_VIEW_PLAY_BUTTON_TEST_TAG),
        contentAlignment = Alignment.Center,
    ) {
        val iconId =
            if (isPlaying) R.drawable.ic_pause_voice_clip else R.drawable.ic_play_voice_clip
        Icon(
            imageVector = ImageVector.vectorResource(iconId),
            contentDescription = "Play voice clip",
            tint = if (isMe) MegaTheme.colors.icon.inverse else MegaTheme.colors.icon.onColor
        )
    }
}

/**
 * Show an overlay when voice clip is loading.
 *
 * @param loadProgress loading progress of the message. null if already loaded. The value can be 0-100.
 */
@Composable
private fun LoadOverlay(loadProgress: Int?, isError: Boolean) {
    if (loadProgress != null || isError) {
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
private fun VoiceClipMessageViewPreview(
    @PreviewParameter(BooleanProvider::class) isMe: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        CoreVoiceClipMessageView(
            isMe = isMe,
            timestamp = "00:49",
            loadProgress = null,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun LoadingVoiceClipMessageViewPreview(
    @PreviewParameter(BooleanProvider::class) isMe: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        CoreVoiceClipMessageView(
            isMe = isMe,
            timestamp = "00:49",
            loadProgress = 50,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun Playing20VoiceClipMessageViewPreview(
    @PreviewParameter(BooleanProvider::class) isMe: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        CoreVoiceClipMessageView(
            isMe = isMe,
            timestamp = "00:49",
            loadProgress = null,
            isPlaying = true,
            playProgress = 20,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun Playing50VoiceClipMessageViewPreview(
    @PreviewParameter(BooleanProvider::class) isMe: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        CoreVoiceClipMessageView(
            isMe = isMe,
            timestamp = "00:49",
            loadProgress = null,
            isPlaying = true,
            playProgress = 50,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun Playing80VoiceClipMessageViewPreview(
    @PreviewParameter(BooleanProvider::class) isMe: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        CoreVoiceClipMessageView(
            isMe = isMe,
            timestamp = "00:49",
            loadProgress = null,
            isPlaying = true,
            playProgress = 80,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun LoadingVoiceClipWithErrorPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        CoreVoiceClipMessageView(
            isMe = true,
            timestamp = "--:--",
            isError = true,
        )
    }
}

