package mega.privacy.android.app.mediaplayer.queue.audio

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.ui.PlayerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.mediaplayer.MediaPlayerActivity
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerServiceGateway
import mega.privacy.android.app.mediaplayer.gateway.PlayerServiceViewModelGateway
import mega.privacy.android.app.mediaplayer.playlist.PlaylistActionModeCallback
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemUiEntity
import mega.privacy.android.app.mediaplayer.queue.view.AudioQueueView
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerServiceBinder
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MenuUtils.toggleAllMenuItemsVisibility
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import javax.inject.Inject

/**
 * Audio queue fragment for displaying the audio queue.
 */
@AndroidEntryPoint
class AudioQueueFragment : Fragment() {
    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val audioQueueViewModel by viewModels<AudioQueueViewModel>()

    private var simpleAudioPlayerView: PlayerView? = null

    private var serviceGateway: MediaPlayerServiceGateway? = null
    private var playerServiceViewModelGateway: PlayerServiceViewModelGateway? = null

    private var playlistObserved = false

    private var actionMode: ActionMode? = null
    private var playlistActionModeCallback: PlaylistActionModeCallback? = null

    private val positionUpdateHandler = Handler(Looper.getMainLooper())
    private val positionUpdateRunnable = object : Runnable {
        override fun run() {
            // Up the frequency of refresh, keeping in sync with Exoplayer.
            positionUpdateHandler.postDelayed(this, UPDATE_INTERVAL_PLAYING_POSITION)
            audioQueueViewModel.updateCurrentPlayingPosition(
                serviceGateway?.getCurrentPlayingPosition() ?: 0
            )
        }
    }

    private val queueSearchViewProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.media_player, menu)

            val searchMenuItem = menu.findItem(R.id.action_search).apply {
                actionView?.let { searchView ->
                    if (searchView is SearchView) {
                        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                            override fun onQueryTextSubmit(query: String): Boolean = true

                            override fun onQueryTextChange(newText: String): Boolean {
                                audioQueueViewModel.searchQueryUpdate(newText)
                                return true
                            }

                        })
                    }
                }
                setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                        audioQueueViewModel.updateSearchMode(true)
                        return true
                    }

                    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                        audioQueueViewModel.updateSearchMode(false)
                        return true
                    }
                })
            }
            menu.toggleAllMenuItemsVisibility(false)
            searchMenuItem?.isVisible = true
            menu.findItem(R.id.select).isVisible = true
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            when (menuItem.itemId) {
                R.id.select -> {
                    activateActionMode()
                }
            }
            return false
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            serviceGateway = null
            playerServiceViewModelGateway = null
        }

        /**
         * Called after a successful bind with our AudioPlayerService.
         */
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MediaPlayerServiceBinder) {
                serviceGateway = service.serviceGateway
                playerServiceViewModelGateway = service.playerServiceViewModelGateway

                playerServiceViewModelGateway?.run {
                    audioQueueViewModel.initMediaQueueItemList(getUpdatedPlaylistItems())
                    audioQueueViewModel.updatePlaybackState(isPaused())
                    simpleAudioPlayerView?.let { setupPlayerView(it) }
                    tryObservePlaylist()
                    scrollToPlayingPosition()
                }
            }
        }
    }

    /**
     * Setup PlayerView
     */
    private fun setupPlayerView(playerView: PlayerView) {
        serviceGateway?.setupPlayerView(
            playerView = playerView,
            showShuffleButton = true
        )
    }

    /**
     * Observe playlist LiveData when view is created and service is connected.
     */
    private fun tryObservePlaylist() {
        playerServiceViewModelGateway?.run {
            if (!playlistObserved && view != null) {
                playlistObserved = true

                viewLifecycleOwner.collectFlow(playlistTitleUpdate()) { title ->
                    (requireActivity() as MediaPlayerActivity).setToolbarTitle(title)
                }

                viewLifecycleOwner.collectFlow(monitorMediaItemTransitionState()) { handle ->
                    if (handle != null) {
                        audioQueueViewModel.updateMediaQueueAfterMediaItemTransition(handle)
                    }
                }

                viewLifecycleOwner.collectFlow(mediaPlaybackUpdate()) { paused ->
                    audioQueueViewModel.updatePlaybackState(paused)
                }

                viewLifecycleOwner.collectFlow(
                    audioQueueViewModel.uiState.map { it.isPaused }.distinctUntilChanged()
                ) { isPaused ->
                    if (isPaused) {
                        positionUpdateHandler.removeCallbacks(positionUpdateRunnable)
                    } else {
                        positionUpdateHandler.post(positionUpdateRunnable)
                    }
                }
            }
        }

        viewLifecycleOwner.collectFlow(audioQueueViewModel.uiState.map { it.selectedItemHandles }
            .distinctUntilChanged()) { handles ->
            actionMode?.run {
                if (handles.isEmpty()) {
                    title = resources.getString(R.string.title_select_tracks)
                    menu.findItem(R.id.remove).isVisible = false
                } else {
                    title = handles.size.toString()
                    menu.findItem(R.id.remove).isVisible = true
                }
            }
        }
    }

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                    AudioQueueView(
                        viewModel = audioQueueViewModel,
                        setupAudioPlayer = { playerView ->
                            simpleAudioPlayerView = playerView
                            setupPlayerView(playerView)
                        },
                        onClick = { index, item -> itemClicked(index, item) },
                        onDragFinished = { playerServiceViewModelGateway?.updatePlaySource() },
                        onMove = { from, to ->
                            audioQueueViewModel.updateMediaQueueAfterReorder(from, to)
                            playerServiceViewModelGateway?.swapItems(from, to)
                        }
                    )
                }
            }
        }

    private fun itemClicked(index: Int, item: MediaQueueItemUiEntity) =
        viewLifecycleOwner.lifecycleScope.launch {
            if (actionMode != null) {
                audioQueueViewModel.onItemClicked(index, item)
                playerServiceViewModelGateway?.itemSelected(item.id.longValue)
                return@launch
            }

            if (!audioQueueViewModel.isParticipatingInChatCall()) {
                playerServiceViewModelGateway?.getIndexFromPlaylistItems(item.id.longValue)
                    ?.let { index ->
                        serviceGateway?.seekTo(index)
                    }
            }
            (requireActivity() as MediaPlayerActivity).closeSearch()
        }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.addMenuProvider(queueSearchViewProvider)

        context?.bindService(
            Intent(
                requireContext(),
                AudioPlayerService::class.java
            ).apply {
                putExtra(Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST, false)
            }, connection, Context.BIND_AUTO_CREATE
        )
        (activity as? MediaPlayerActivity)?.showToolbar(false)
        tryObservePlaylist()
    }


    /**
     * Activated the action mode
     */
    private fun activateActionMode() {
        if (playlistActionModeCallback == null) {
            playlistActionModeCallback = PlaylistActionModeCallback(
                removeSelections = {
                    playerServiceViewModelGateway?.removeAllSelectedItems()
                    audioQueueViewModel.removeSelectedItems()
                    actionMode?.finish()
                },
                clearSelections = {
                    audioQueueViewModel.clearAllSelectedItems()
                    playerServiceViewModelGateway?.clearSelections()
                    actionMode = null
                }
            )
        }
        playlistActionModeCallback?.let {
            actionMode = (activity as? AppCompatActivity)?.startSupportActionMode(it)
            actionMode?.title = resources.getString(R.string.title_select_tracks)
        }
    }


    /**
     * onDestroyView
     */
    override fun onDestroyView() {
        super.onDestroyView()
        actionMode?.let {
            it.finish()
            actionMode = null
        }
        playlistObserved = false
        positionUpdateHandler.removeCallbacks(positionUpdateRunnable)
        activity?.removeMenuProvider(queueSearchViewProvider)
        serviceGateway = null
        playerServiceViewModelGateway = null
        context?.unbindService(connection)
    }

    companion object {
        /**
         * The update interval for playing position
         */
        const val UPDATE_INTERVAL_PLAYING_POSITION: Long = 500

        /**
         * The minimum size of single playlist
         */
        const val SINGLE_PLAYLIST_SIZE = 2
    }
}
