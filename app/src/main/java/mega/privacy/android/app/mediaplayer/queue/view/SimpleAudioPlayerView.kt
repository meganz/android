package mega.privacy.android.app.mediaplayer.queue.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.media3.ui.PlayerView
import mega.privacy.android.app.databinding.SimpleAudioPlayerBinding
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme

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
        modifier = modifier.height(130.dp),
        factory = { inflater: LayoutInflater, parent: ViewGroup, attachToParent: Boolean ->
            SimpleAudioPlayerBinding.inflate(inflater, parent, attachToParent).apply {
                setupAudioPlayer(playerView)
            }
        }
    )
}


@CombinedThemePreviews
@Composable
private fun SimpleAudioPlayerViewPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        SimpleAudioPlayerView(
            setupAudioPlayer = {}
        )
    }
}