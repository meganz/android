package mega.privacy.android.app.mediaplayer.queue.view

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.content.ContextCompat
import androidx.media3.ui.PlayerView
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.SimpleAudioPlayerBinding
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Simple audio player view for showing the audio player in the audio queue
 *
 * @param setupAudioPlayer callback for Setup player view
 * @param modifier Modifier
 */
@Composable
fun SimpleAudioPlayerView(
    setupAudioPlayer: (PlayerView) -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidViewBinding(
        modifier = modifier.wrapContentHeight(),
        factory = { inflater: LayoutInflater, parent: ViewGroup, attachToParent: Boolean ->
            SimpleAudioPlayerBinding.inflate(inflater, parent, attachToParent).apply {
                setupAudioPlayer(playerView)
                playerView.findViewById<ImageButton>(R.id.exo_prev)
                    ?.setImageDrawable(
                        ContextCompat.getDrawable(
                            inflater.context,
                            iconPackR.drawable.ic_prev_audio_player
                        )
                    )
                playerView.findViewById<ImageButton>(R.id.exo_rew)
                    ?.setImageDrawable(
                        ContextCompat.getDrawable(
                            inflater.context,
                            R.drawable.media_player_15_minus
                        )
                    )
                playerView.findViewById<ImageButton>(R.id.exo_next)
                    ?.setImageDrawable(
                        ContextCompat.getDrawable(
                            inflater.context,
                            iconPackR.drawable.ic_next_audio_player
                        )
                    )
                playerView.findViewById<ImageButton>(R.id.exo_ffwd)
                    ?.setImageDrawable(
                        ContextCompat.getDrawable(
                            inflater.context,
                            R.drawable.media_player_15_plus
                        )
                    )
            }
        }
    )
}


@CombinedThemePreviews
@Composable
private fun SimpleAudioPlayerViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SimpleAudioPlayerView(
            setupAudioPlayer = {}
        )
    }
}