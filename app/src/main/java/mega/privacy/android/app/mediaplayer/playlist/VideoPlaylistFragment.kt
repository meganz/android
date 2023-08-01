package mega.privacy.android.app.mediaplayer.playlist

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.FragmentAudioPlaylistBinding
import mega.privacy.android.app.di.mediaplayer.VideoPlayer
import mega.privacy.android.app.mediaplayer.MediaPlayerActivity
import mega.privacy.android.app.mediaplayer.VideoPlayerViewModel
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.autoCleared
import javax.inject.Inject

/**
 * Playlist fragment for displaying the playlist of videos
 */
@AndroidEntryPoint
class VideoPlaylistFragment : Fragment(), PlaylistItemOperation, DragStartListener {
    /**
     * MediaPlayerGateway for video player
     */
    @VideoPlayer
    @Inject
    lateinit var mediaPlayerGateway: MediaPlayerGateway

    private val videoViewModel: VideoPlayerViewModel by activityViewModels()

    private var binding by autoCleared<FragmentAudioPlaylistBinding>()

    private var adapter: PlaylistAdapter? = null
    private lateinit var listLayoutManager: LinearLayoutManager

    private var playlistObserved = false

    private var itemTouchHelper: ItemTouchHelper? = null
    private var actionMode: ActionMode? = null
    private var playlistActionModeCallback: PlaylistActionModeCallback? = null
    private var isActionMode = false

    private lateinit var itemDecoration: PlaylistItemDecoration

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
        activity?.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.select -> {
                        // Activate the action mode for selecting the tracks
                        videoViewModel.setActionMode(true)
                    }
                }
                return false
            }

        })
        (activity as? MediaPlayerActivity)?.showToolbar(false)
        binding.playerView.isVisible = false
        adapter?.setCurrentPlayingPosition(mediaPlayerGateway.getCurrentPlayingPosition())
        videoViewModel.getPlaylistItems().let { items ->
            if (items.isNotEmpty()) {
                adapter?.submitList(items)
            }
        }

        tryObservePlaylist()
        videoViewModel.scrollToPlayingPosition()
        // Initial item touch helper after the service is connected.
        setupItemTouchHelper(videoViewModel)
        tryObservePlaylist()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        playlistObserved = false
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
                isAudio = false
            )
        }
        listLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        binding.playlist.let { recyclerView ->
            val params = recyclerView.layoutParams as RelativeLayout.LayoutParams
            params.topMargin = Util.dp2px(70f)
            recyclerView.layoutParams = params

            recyclerView.setHasFixedSize(true)
            recyclerView.setBackgroundColor(requireActivity().getColor(R.color.grey_800))
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
                    requireContext().getDrawable(R.drawable.playlist_divider_layer_video),
                    requireContext().getDrawable(R.drawable.playlist_divider_layer_next_video),
                    it
                )
            }

            recyclerView.addItemDecoration(itemDecoration)
        }
    }

    /**
     * Initial the item touch helper and attach to recycle view.
     *
     * @param viewModel VideoPlayerViewModel
     */
    private fun setupItemTouchHelper(viewModel: VideoPlayerViewModel) {
        adapter?.let {
            val itemTouchCallBack = PlaylistItemTouchCallBack(
                adapter = it,
                playingPosition = viewModel.getPlayingPosition(),
                swapItems = { currentPosition, targetPosition ->
                    viewModel.swapItems(currentPosition, targetPosition)
                },
                updatePlaySource = { viewModel.updatePlaySource() },
                playlistItemDecoration = itemDecoration
            )
            itemTouchHelper = ItemTouchHelper(itemTouchCallBack)
        }
        itemTouchHelper?.attachToRecyclerView(binding.playlist)
    }

    /**
     * Observe playlist LiveData when view is created and service is connected.
     */
    private fun tryObservePlaylist() {
        with(videoViewModel) {
            if (!playlistObserved && view != null) {
                playlistObserved = true

                viewLifecycleOwner.collectFlow(playlistItemsState) {
                    adapter?.setPaused(mediaPlaybackState.value)

                    adapter?.submitList(it.first) {
                        if (it.second != -1) {
                            listLayoutManager.scrollToPositionWithOffset(it.second, 0)
                        }
                    }
                }

                viewLifecycleOwner.collectFlow(playlistTitleState) { title ->
                    title?.let {
                        (requireActivity() as MediaPlayerActivity).setToolbarTitle(it)
                    }
                }

                viewLifecycleOwner.collectFlow(itemsSelectedCountState) { selectedCount ->
                    actionMode?.run {
                        // Set title according to the number of selected tracks
                        title = if (selectedCount <= 0) {
                            resources.getString(R.string.title_select_tracks)
                        } else {
                            selectedCount.toString()
                        }
                        menu.findItem(R.id.remove).isVisible = selectedCount > 0
                    }
                }

                viewLifecycleOwner.collectFlow(actionModeState) { isActionMode ->
                    if (isActionMode) {
                        activateActionMode()
                        itemTouchHelper?.attachToRecyclerView(null)
                    } else {
                        actionMode?.finish()
                        itemTouchHelper?.attachToRecyclerView(binding.playlist)
                    }
                    this@VideoPlaylistFragment.isActionMode = isActionMode
                }

                viewLifecycleOwner.collectFlow(mediaPlaybackState) { paused ->
                    adapter?.refreshPausedState(paused)
                }
            }
        }
    }

    /**
     * Activated the action mode
     */
    private fun activateActionMode() {
        if (playlistActionModeCallback == null) {
            with(videoViewModel) {
                playlistActionModeCallback = PlaylistActionModeCallback(
                    removeSelections = { removeAllSelectedItems() },
                    clearSelections = { clearSelections() }
                )
            }
        }
        playlistActionModeCallback?.run {
            actionMode = (activity as? AppCompatActivity)?.startSupportActionMode(this)
            actionMode?.title = resources.getString(R.string.title_select_tracks)
        }
    }

    override fun onItemClick(
        view: View,
        item: PlaylistItem,
        holder: PlaylistViewHolder,
        position: Int,
    ) {
        with(videoViewModel) {
            if (actionModeState.value) {
                itemSelected(item.nodeHandle)
                adapter?.startAnimation(holder, position)
            } else {
                if (view.id == R.id.transfers_list_option_reorder) {
                    return
                }
                if (!CallUtil.participatingInACall()) {
                    getIndexFromPlaylistItems(item)?.let { index ->
                        mediaPlayerGateway.playerSeekTo(index)
                        with(videoViewModel) {
                            updateCurrentMediaId(
                                playlistItemsState.value.first.getOrNull(index)?.nodeHandle.toString()
                            )
                        }
                        resetRetryState()
                    }
                }
                with(requireActivity() as MediaPlayerActivity) {
                    closeSearch()
                    onBackPressedDispatcher.onBackPressed()
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
}
