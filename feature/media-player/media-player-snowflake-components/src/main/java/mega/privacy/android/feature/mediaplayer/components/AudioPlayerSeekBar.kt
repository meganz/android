package mega.privacy.android.feature.mediaplayer.components

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.TimeBar
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens

@OptIn(UnstableApi::class)
@Composable
fun AudioPlayerSeekBar(
    currentPosition: Long,
    duration: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    sleepTimerLabel: String? = null,
) {
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableLongStateOf(0L) }
    val currentOnSeekTo by rememberUpdatedState(onSeekTo)

    val displayPosition = if (isScrubbing) scrubPosition else currentPosition
    val brandColor = DSTokens.colors.brand.default
    val brandColorArgb = remember(brandColor) { brandColor.toArgb() }

    Column(modifier = modifier.fillMaxWidth()) {
        AndroidView(
            factory = { context ->
                DefaultTimeBar(context).apply {
                    setPlayedColor(brandColorArgb)
                    setScrubberColor(brandColorArgb)
                    addListener(object : TimeBar.OnScrubListener {
                        override fun onScrubStart(timeBar: TimeBar, position: Long) {
                            isScrubbing = true
                            scrubPosition = position
                        }

                        override fun onScrubMove(timeBar: TimeBar, position: Long) {
                            scrubPosition = position
                        }

                        override fun onScrubStop(
                            timeBar: TimeBar,
                            position: Long,
                            canceled: Boolean,
                        ) {
                            isScrubbing = false
                            if (!canceled) currentOnSeekTo(position)
                        }
                    })
                }
            },
            update = { timeBar ->
                timeBar.setDuration(duration)
                if (!isScrubbing) {
                    timeBar.setPosition(currentPosition)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MegaText(
                text = formatPlaybackTime(displayPosition),
                style = MaterialTheme.typography.bodySmall,
                textColor = TextColor.Secondary,
                modifier = Modifier.weight(1f),
            )
            if (sleepTimerLabel != null) {
                MegaText(
                    text = sleepTimerLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    textColor = TextColor.Primary,
                )
            }
            MegaText(
                text = formatPlaybackTime(duration),
                style = MaterialTheme.typography.bodySmall,
                textColor = TextColor.Secondary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
            )
        }
    }
}
