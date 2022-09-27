package mega.privacy.android.app.presentation.photos.albums

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentAlbumBinding
import mega.privacy.android.app.fragments.managerFragments.cu.PhotosTabCallback
import mega.privacy.android.app.fragments.managerFragments.cu.album.AlbumContentFragment
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.photos.albums.adapter.AlbumCoverAdapter
import mega.privacy.android.app.presentation.photos.albums.model.AlbumsLoadState
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.callManager
import mega.privacy.android.domain.entity.photos.Album

/**
 * AlbumsFragment is a sub fragment of PhotosFragment. Its sibling is TimelineFragment
 */
@AndroidEntryPoint
class AlbumsFragment : Fragment(), PhotosTabCallback {

    private lateinit var mManagerActivity: ManagerActivity
    private lateinit var binding: FragmentAlbumBinding
    private lateinit var albumList: RecyclerView
    private lateinit var albumCoverAdapter: AlbumCoverAdapter
    val viewModel by viewModels<AlbumsViewModel>()

    /**
     * A config for how many can should in a row in different orientation
     */
    companion object {
        const val SPAN_PORTRAIT = 3
        const val SPAN_LANDSCAPE = 4
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mManagerActivity = activity as ManagerActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAlbumBinding.inflate(inflater, container, false)
        albumList = binding.albumList
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAlbumList()
        setupFlow()
    }

    /**
     * Setup flow
     */
    private fun setupFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.loadState.collect { favouritesState ->
                    when (favouritesState) {
                        is AlbumsLoadState.Success -> {
                            albumCoverAdapter.submitList(favouritesState.albums)
                        }
                        else -> {
                            // current do nothing
                        }
                    }
                }
            }
        }
    }

    /**
     * Set up albumList
     */
    private fun setupAlbumList() {
        elevateToolbarWhenScrolling()
        configList()
    }

    /**
     * Set up the recyclerView and bind adapter
     */
    private fun configList() {
        // check isPortrait
        val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        // calculate span by isPortrait
        val span = if (isPortrait) SPAN_PORTRAIT else SPAN_LANDSCAPE
        setLayoutManagerForAlbumList(span)
        val coverMargin = calculateCoverMargin()
        val coverWidth = calculateCoverWidth(coverMargin, span)
        setAlbumListLayoutParams(coverMargin)
        bindAdapter(coverWidth = coverWidth, coverMargin = coverMargin)
    }

    /**
     * Set LayoutManager For AlbumList
     */
    private fun setLayoutManagerForAlbumList(span: Int) {
        albumList.layoutManager = GridLayoutManager(context, span)
    }

    /**
     * Set up ListAdapter for album list
     */
    private fun bindAdapter(coverWidth: Int, coverMargin: Int) {
        albumCoverAdapter =
            AlbumCoverAdapter(coverWidth, coverMargin, object : AlbumCoverAdapter.Listener {

                override fun onCoverClicked(album: Album) {
                    when (album) {
                        is Album.FavouriteAlbum -> {
                            mManagerActivity.skipToAlbumContentFragment(AlbumContentFragment.getInstance())
                        }
                    }

                }
            })
        albumList.adapter = albumCoverAdapter
    }

    /**
     * Calculate cover width
     */
    private fun calculateCoverWidth(coverMargin: Int, span: Int) =
        (resources.displayMetrics.widthPixels - coverMargin * span * 2 - coverMargin * 2) / span

    /**
     * Calculate cover margin
     */
    private fun calculateCoverMargin() =
        resources.getDimensionPixelSize(R.dimen.cu_fragment_ic_selected_margin_small)

    /**
     * Set album list layout params
     */
    private fun setAlbumListLayoutParams(coverMargin: Int) {
        val params = albumList.layoutParams as ViewGroup.MarginLayoutParams
        params.leftMargin = coverMargin
        params.rightMargin = coverMargin
    }

    /**
     * Handle elevate for Toolbar When Scrolling
     */
    private fun elevateToolbarWhenScrolling() =
        binding.albumList.setOnScrollChangeListener { v: View?, _, _, _, _ ->
            callManager {
                it.changeAppBarElevation(
                    v!!.canScrollVertically(-1)
                )
            }
        }

    override fun checkScroll() {
        val isScrolled = albumList.canScrollVertically(Constants.SCROLLING_UP_DIRECTION)
        mManagerActivity.changeAppBarElevation(isScrolled)
    }

    override fun onBackPressed(): Int {
        return 1
    }
}