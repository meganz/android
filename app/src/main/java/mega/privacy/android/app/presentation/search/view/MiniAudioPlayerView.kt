package mega.privacy.android.app.presentation.search.view

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mega.privacy.android.app.databinding.MiniAudioPlayerBinding
import mega.privacy.android.app.mediaplayer.miniplayer.MiniAudioPlayerController

/**
 * Mini audio player view
 *
 * @param modifier Modifier
 * @param lifecycle Lifecycle
 */
@Composable
fun MiniAudioPlayerView(modifier: Modifier, lifecycle: Lifecycle) {
    var audioPlayerInitialized by remember {
        mutableStateOf(false)
    }
    var audioPlayerHeight by remember {
        mutableIntStateOf(0)
    }
    val scope = rememberCoroutineScope()
    AndroidViewBinding(
        modifier = modifier.height(audioPlayerHeight.dp),
        factory = MiniAudioPlayerBinding::inflate,
    ) {
        if (audioPlayerInitialized.not()) {
            val audioPlayerController =
                MiniAudioPlayerController(playerView = this.miniAudioPlayer).apply {
                    shouldVisible = true
                }
            lifecycle.addObserver(audioPlayerController)
            scope.launch {
                audioPlayerController.audioBinding.collectLatest {
                    audioPlayerHeight = if (it) 64 else 0
                }
            }
        }
        audioPlayerInitialized = true
    }
}