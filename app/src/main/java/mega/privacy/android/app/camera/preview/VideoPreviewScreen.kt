package mega.privacy.android.app.camera.preview

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import mega.privacy.android.app.camera.CameraPreviewScreen
import mega.privacy.android.app.camera.PreviewViewModel
import mega.privacy.android.app.camera.observeAsState
import mega.privacy.android.app.databinding.SimpleVideoPlayerBinding

/**
 * Video preview screen
 *
 * @param uri
 * @param onBackPressed
 * @param onSendVideo
 * @param viewModel
 */
@Composable
internal fun VideoPreviewScreen(
    uri: Uri,
    title: String,
    onBackPressed: () -> Unit,
    onSendVideo: (Uri) -> Unit,
    viewModel: PreviewViewModel = hiltViewModel(),
) {
    CameraPreviewScreen(
        uri = uri,
        title = title,
        onBackPressed = onBackPressed,
        onSend = onSendVideo,
        viewModel = viewModel,
    ) { modifier ->
        PreviewVideoSection(
            modifier = modifier.fillMaxSize(),
            uri = uri,
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PreviewVideoSection(
    uri: Uri,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle by LocalLifecycleOwner.current.lifecycle.observeAsState()
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            addMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }

    AndroidViewBinding(
        modifier = modifier,
        factory = { inflater: LayoutInflater, parent: ViewGroup, attachToParent: Boolean ->
            SimpleVideoPlayerBinding.inflate(inflater, parent, attachToParent).apply {
                playerView.player = player
                player.addListener(
                    object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_READY) {
                                playerView.showController()
                            }
                        }
                    },
                )
            }
        },
        update = {
            when (lifecycle) {
                Lifecycle.Event.ON_PAUSE -> {
                    playerView.onPause()
                    player.pause()
                }

                Lifecycle.Event.ON_RESUME -> playerView.onResume()
                else -> Unit
            }
        }
    )

    DisposableEffect(player) {
        onDispose { player.release() }
    }
}
