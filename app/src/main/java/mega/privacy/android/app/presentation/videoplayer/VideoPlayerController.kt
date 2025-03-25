package mega.privacy.android.app.presentation.videoplayer

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.queue.audio.AudioQueueFragment.Companion.SINGLE_PLAYLIST_SIZE
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.mobile.analytics.event.LoopButtonPressedEvent
import javax.inject.Singleton

@Singleton
class VideoPlayerController(
    private val context: Context,
    private val viewModel: VideoPlayerViewModel,
    container: ViewGroup,
) : LifecycleEventObserver {
    private val playlist = container.findViewById<ImageButton>(R.id.playlist)
    private val trackName = container.findViewById<TextView>(R.id.track_name)
    private val repeatToggleButton = container.findViewById<ImageButton>(R.id.repeat_toggle)
    private val playerComposeView = container.findViewById<PlayerView>(R.id.player_compose_view)

    private var sharingScope: CoroutineScope? = null

    init {
        val state = viewModel.uiState.value

        setupRepeatToggleButton(state.repeatToggleMode) {
            val repeatToggleMode =
                viewModel.uiState.value.repeatToggleMode.let { repeatToggleMode ->
                    if (repeatToggleMode == RepeatToggleMode.REPEAT_NONE) {
                        Analytics.tracker.trackEvent(LoopButtonPressedEvent)
                        RepeatToggleMode.REPEAT_ONE

                    } else {
                        RepeatToggleMode.REPEAT_NONE
                    }
                }
            viewModel.setRepeatToggleModeForPlayer(repeatToggleMode)
        }
        setupVideoPlayQueueButton(state.items.size) {
        }
    }

    /**
     * Setup video play queue button.
     *
     * @param size the video play queue size
     * @param openPlaylist the callback when playlist button is clicked
     */
    private fun setupVideoPlayQueueButton(size: Int, openPlaylist: () -> Unit) {
        togglePlayQueueEnabled(size)

        playlist.setOnClickListener {
            openPlaylist()
        }
    }

    /**
     * Setup the repeat toggle button
     *
     * @param defaultRepeatToggleMode the default RepeatToggleMode
     * @param clickedCallback the callback of repeat toggle button clicked
     */
    private fun setupRepeatToggleButton(
        defaultRepeatToggleMode: RepeatToggleMode,
        clickedCallback: () -> Unit,
    ) {
        repeatToggleButton.isVisible = true
        updateRepeatToggleButtonUI(context, defaultRepeatToggleMode)
        repeatToggleButton.setOnClickListener { clickedCallback() }
    }

    /**
     * Update repeat toggle button UI
     *
     * @param context Context
     * @param repeatToggleMode the current RepeatToggleMode
     */
    private fun updateRepeatToggleButtonUI(
        context: Context,
        repeatToggleMode: RepeatToggleMode,
    ) {
        repeatToggleButton.setColorFilter(
            if (repeatToggleMode == RepeatToggleMode.REPEAT_NONE) {
                context.getColor(R.color.white)
            } else {
                context.getColor(R.color.color_button_brand)
            }
        )
    }

    /**
     * Toggle the playlist button.
     *
     * @param itemSize the item size
     */
    private fun togglePlayQueueEnabled(itemSize: Int) {
        playlist.visibility =
            if (itemSize > SINGLE_PLAYLIST_SIZE)
                View.VISIBLE
            else
                View.INVISIBLE

    }

    /**
     * Display node metadata.
     *
     * @param metadata metadata to display
     */
    private fun displayMetadata(metadata: Metadata) {
        trackName.text = metadata.title ?: metadata.nodeName
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> onCreated(source)
            Lifecycle.Event.ON_RESUME -> onResume()
            Lifecycle.Event.ON_PAUSE -> onPause()
            Lifecycle.Event.ON_DESTROY -> onDestroy()
            else -> return
        }
    }

    private fun onCreated(source: LifecycleOwner) {
        if (sharingScope == null) {
            sharingScope = source.lifecycleScope
            sharingScope?.launch {
                viewModel.uiState.map { it.metadata }.distinctUntilChanged()
                    .collectLatest { metadata ->
                        displayMetadata(metadata)
                    }
            }

            sharingScope?.launch {
                viewModel.uiState.map { it.items }.distinctUntilChanged()
                    .collectLatest {
                        togglePlayQueueEnabled(it.size)
                    }
            }

            sharingScope?.launch {
                viewModel.uiState.map { it.repeatToggleMode }.distinctUntilChanged()
                    .collectLatest {
                        updateRepeatToggleButtonUI(context, it)
                    }
            }
        }
    }

    /**
     * The onResume function is called when Lifecycle event ON_RESUME
     *
     */
    fun onResume() {
        playerComposeView.onResume()
    }

    /**
     * The onPause function is called when Lifecycle event ON_PAUSE
     */
    fun onPause() {
        playerComposeView.onPause()
    }

    /**
     * The onDestroy function is called when Lifecycle event ON_DESTROY
     */
    fun onDestroy() {
        sharingScope?.cancel()
        sharingScope = null
    }
}