package mega.privacy.android.app.mediaplayer.playlist

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentAudioPlaylistBinding
import mega.privacy.android.app.mediaplayer.AudioPlayerActivity
import mega.privacy.android.app.mediaplayer.MediaPlayerActivity
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerServiceGateway
import mega.privacy.android.app.mediaplayer.gateway.PlayerServiceViewModelGateway
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerServiceBinder
import mega.privacy.android.app.mediaplayer.service.VideoPlayerService
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.autoCleared

/**
 * Playlist fragment for displaying the playlist of audios or videos
 */
class PlaylistFragment : Fragment(), PlaylistItemOperation, DragStartListener {
    private var binding by autoCleared<FragmentAudioPlaylistBinding>()

    private var serviceGateway: MediaPlayerServiceGateway? = null
    private var playerServiceViewModelGateway: PlayerServiceViewModelGateway? = null

    private var adapter: PlaylistAdapter? = null
    private lateinit var listLayoutManager: LinearLayoutManager

    private var playlistObserved = false

    private var itemTouchHelper: ItemTouchHelper? = null
    private var actionMode: ActionMode? = null
    private var playlistActionModeCallback: PlaylistActionModeCallback? = null
    private var isActionMode = false

    private var isAudioPlayer = false

    private lateinit var itemDecoration: PlaylistItemDecoration

    private val positionUpdateHandler = Handler(Looper.getMainLooper())
    private val positionUpdateRunnable = object : Runnable {
        override fun run() {
            // Up the frequency of refresh, keeping in sync with Exoplayer.
            positionUpdateHandler.postDelayed(this, UPDATE_INTERVAL_PLAYING_POSITION)
            adapter?.setCurrentPlayingPosition(serviceGateway?.getCurrentPlayingPosition())
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
                    if (isAudioPlayer) {
                        setupPlayerView()
                    } else {
                        binding.playerView.isVisible = false
                        adapter?.setCurrentPlayingPosition(serviceGateway?.getCurrentPlayingPosition())
                    }
                    if (getPlaylistItems().isNotEmpty()) {
                        adapter?.submitList(getPlaylistItems())
                    }
                    if (isAudioPlayer && !isPaused()) {
                        positionUpdateHandler.post(positionUpdateRunnable)
                    }
                    tryObservePlaylist()
                    scrollToPlayingPosition()
                    // Initial item touch helper after the service is connected.
                    setupItemTouchHelper()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        isAudioPlayer = activity is AudioPlayerActivity
        val playerServiceIntent = Intent(
            requireContext(),
            if (isAudioPlayer)
                AudioPlayerService::class.java
            else
                VideoPlayerService::class.java
        )
        playerServiceIntent.putExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, false)
        requireContext().bindService(playerServiceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAudioPlaylistBinding.inflate(inflater, container, false)
        setupRecycleView()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MediaPlayerActivity).showToolbar(false)
        tryObservePlaylist()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.select -> {
                // Activate the action mode for selecting the tracks
                playerServiceViewModelGateway?.setActionMode(true)
            }
        }
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        playlistObserved = false
        positionUpdateHandler.removeCallbacks(positionUpdateRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceGateway = null
        requireContext().unbindService(connection)
    }

    /**
     * Setup recycle view
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setupRecycleView() {
        context?.let {
            adapter = PlaylistAdapter(
                it,
                this,
                dragStartListener = this,
                isAudio = isAudioPlayer
            )
        }
        listLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        binding.playlist.let { recyclerView ->
            if (!isAudioPlayer) {
                val params = recyclerView.layoutParams as RelativeLayout.LayoutParams
                params.topMargin = Util.dp2px(70f)
                recyclerView.layoutParams = params
            }
            recyclerView.setHasFixedSize(true)
            recyclerView.setBackgroundColor(
                requireActivity().getColor(
                    if (isAudioPlayer) {
                        R.color.grey_020_grey_800
                    } else {
                        R.color.grey_800
                    }
                )
            )
            // Avoid the item is flash when it is refreshed.
            (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
            recyclerView.layoutManager = listLayoutManager
            recyclerView.adapter = adapter

            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    (requireActivity() as MediaPlayerActivity).setupToolbarColors(
                        recyclerView.canScrollVertically(-1)
                    )
                }
            })

            adapter?.let {
                itemDecoration = PlaylistItemDecoration(
                    requireContext().getDrawable(
                        if (isAudioPlayer) {
                            R.drawable.playlist_divider_layer
                        } else {
                            R.drawable.playlist_divider_layer_video
                        }
                    ),
                    requireContext().getDrawable(
                        if (isAudioPlayer) {
                            R.drawable.playlist_divider_layer_next
                        } else {
                            R.drawable.playlist_divider_layer_next_video
                        }
                    ),
                    it
                )
            }

            recyclerView.addItemDecoration(itemDecoration)
        }
    }

    /**
     * Initial the item touch helper and attach to recycle view.
     */
    private fun setupItemTouchHelper() {
        playerServiceViewModelGateway?.let { gateway ->
            adapter?.let {
                val itemTouchCallBack = PlaylistItemTouchCallBack(it, gateway, itemDecoration)
                itemTouchHelper = ItemTouchHelper(itemTouchCallBack)
            }
        }
        itemTouchHelper?.attachToRecyclerView(binding.playlist)
    }

    /**
     * Observe playlist LiveData when view is created and service is connected.
     */
    private fun tryObservePlaylist() {
        playerServiceViewModelGateway?.run {
            if (!playlistObserved && view != null) {
                playlistObserved = true

                playlistUpdate().flowWithLifecycle(
                    viewLifecycleOwner.lifecycle,
                    Lifecycle.State.RESUMED
                ).onEach {
                    adapter?.setPaused(isPaused())

                    adapter?.submitList(it.first) {
                        if (it.second != -1) {
                            listLayoutManager.scrollToPositionWithOffset(it.second, 0)
                        }
                        if (isAudioPlayer && it.first.isNotEmpty()) {
                            // Trigger the visibility update of the pause icon of the
                            // playing (paused) audio.
                            adapter?.notifyItemChanged(it.second)
                        }
                    }
                }.launchIn(viewLifecycleOwner.lifecycleScope)

                playlistTitleUpdate().flowWithLifecycle(
                    viewLifecycleOwner.lifecycle,
                    Lifecycle.State.RESUMED
                ).onEach { title ->
                    (requireActivity() as MediaPlayerActivity).setToolbarTitle(title)
                }.launchIn(viewLifecycleOwner.lifecycleScope)

                itemsSelectedCountUpdate().flowWithLifecycle(
                    viewLifecycleOwner.lifecycle,
                    Lifecycle.State.RESUMED
                ).onEach { selectedCount ->
                    actionMode?.run {
                        // Set title according to the number of selected tracks
                        if (selectedCount <= 0) {
                            title = resources.getString(R.string.title_select_tracks)
                            menu.findItem(R.id.remove).isVisible = false
                        } else {
                            title = selectedCount.toString()
                            menu.findItem(R.id.remove).isVisible = true
                        }
                    }
                }.launchIn(viewLifecycleOwner.lifecycleScope)

                actionModeUpdate().flowWithLifecycle(
                    viewLifecycleOwner.lifecycle,
                    Lifecycle.State.RESUMED
                ).onEach { isActionMode ->
                    if (isActionMode) {
                        activateActionMode()
                        itemTouchHelper?.attachToRecyclerView(null)
                    } else {
                        actionMode?.finish()
                        itemTouchHelper?.attachToRecyclerView(binding.playlist)
                    }
                    this@PlaylistFragment.isActionMode = isActionMode
                }.launchIn(viewLifecycleOwner.lifecycleScope)

                mediaPlaybackUpdate().flowWithLifecycle(
                    viewLifecycleOwner.lifecycle,
                    Lifecycle.State.RESUMED
                ).onEach { paused ->
                    playerServiceViewModelGateway?.let {
                        if (isAudioPlayer) {
                            if (paused) {
                                positionUpdateHandler.removeCallbacks(positionUpdateRunnable)
                            } else {
                                positionUpdateHandler.post(positionUpdateRunnable)
                            }
                        }
                    }
                    adapter?.refreshPausedState(paused)
                }.launchIn(viewLifecycleOwner.lifecycleScope)
            }
        }
    }

    /**
     * Activated the action mode
     */
    private fun activateActionMode() {
        if (playlistActionModeCallback == null) {
            playerServiceViewModelGateway?.run {
                playlistActionModeCallback = PlaylistActionModeCallback(this)
            }
        }
        playlistActionModeCallback?.run {
            actionMode = (context as AppCompatActivity).startSupportActionMode(this)
            actionMode?.title = resources.getString(R.string.title_select_tracks)
        }
    }

    /**
     * Setup PlayerView
     */
    private fun setupPlayerView() {
        serviceGateway?.setupPlayerView(
            playerView = binding.playerView,
            isAudioPlayer = isAudioPlayer,
            showShuffleButton = true
        )
    }

    override fun onItemClick(
        view: View,
        item: PlaylistItem,
        holder: PlaylistViewHolder,
        position: Int,
    ) {
        playerServiceViewModelGateway?.run {
            if (isActionMode() == true) {
                itemSelected(item.nodeHandle)
                adapter?.startAnimation(holder, position)
            } else {
                if (view.id == R.id.transfers_list_option_reorder) {
                    return
                }
                if (!CallUtil.participatingInACall()) {
                    getIndexFromPlaylistItems(item)?.let { index ->
                        serviceGateway?.seekTo(index)
                    }
                }
                (requireActivity() as MediaPlayerActivity).closeSearch()

                if (!isAudioPlayer) {
                    (requireActivity() as MediaPlayerActivity).onBackPressedDispatcher.onBackPressed()
                }
                return
            }
        }
    }

    override fun onDragStarted(holder: PlaylistViewHolder) {
        if (!isActionMode) {
            itemTouchHelper?.startDrag(holder)
        }
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
