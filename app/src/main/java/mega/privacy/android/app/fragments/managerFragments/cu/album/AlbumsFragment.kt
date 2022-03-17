package mega.privacy.android.app.fragments.managerFragments.cu.album

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentAlbumBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.fragments.managerFragments.cu.PhotosFragment
import mega.privacy.android.app.fragments.managerFragments.cu.PhotosTabCallback
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.callManager

/**
 * AlbumsFragment is a sub fragment of PhotosFragment. Its sibling is TimelineFragment
 */
@AndroidEntryPoint
class AlbumsFragment : BaseFragment(), PhotosTabCallback {

    private lateinit var mManagerActivity: ManagerActivity

    private lateinit var binding: FragmentAlbumBinding

    private lateinit var albumList: RecyclerView
    private lateinit var albumCoverAdapter: AlbumCoverAdapter
    private lateinit var layoutManager: GridLayoutManager

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
        mManagerActivity = requireActivity() as ManagerActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAlbumBinding.inflate(inflater, container, false)
        albumList = binding.albumList
        albumList.setHasFixedSize(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.createDefaultAlbums(context.resources.getString(R.string.title_favourites_album))
        setupListAdapter()
        subscribeObservers()
    }

    /**
     * Set up ListAdapter for album list
     */
    fun setupListAdapter() {
        elevateToolbarWhenScrolling()
        val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val span = if (isPortrait) SPAN_PORTRAIT else SPAN_LANDSCAPE

        val coverMargin =
            resources.getDimensionPixelSize(R.dimen.cu_fragment_ic_selected_margin_small)
        val coverWidth: Int =
            (outMetrics.widthPixels - coverMargin * span * 2 - coverMargin * 2) / span

        val params = albumList.layoutParams as ViewGroup.MarginLayoutParams
        params.leftMargin = coverMargin
        params.rightMargin = coverMargin

        albumCoverAdapter =
            AlbumCoverAdapter(coverWidth, coverMargin, object : AlbumCoverAdapter.Listener {

                override fun onCoverClicked(album: AlbumCover) {
                    mManagerActivity.skipToAlbumContentFragment(AlbumContentFragment.getInstance())
                }
            })
        albumCoverAdapter.setHasStableIds(true)
        albumList.adapter = albumCoverAdapter

        layoutManager = GridLayoutManager(context, span)
        layoutManager.apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return 1
                }
            }

            albumCoverAdapter.setItemDimen(coverWidth)
        }
        albumList.layoutManager = layoutManager

        albumCoverAdapter.submitList(viewModel.albumList)
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

    /**
     * Subscribe Observers
     */
    private fun subscribeObservers() {
        viewModel.items.observe(viewLifecycleOwner) {
            val updateList = viewModel.updateAlbumCovers(
                it,
                context.resources.getString(R.string.title_favourites_album)
            )
            albumCoverAdapter.submitList(updateList)
        }
    }

    override fun checkScroll() {
        val isScrolled = albumList.canScrollVertically(Constants.SCROLLING_UP_DIRECTION)
        mManagerActivity.changeAppBarElevation(isScrolled)
    }

    override fun onBackPressed(): Int {
        (parentFragment as PhotosFragment).switchToTimeline()
        return 1
    }

    override fun onDestroyView() {
        viewModel.cancelSearch()
        super.onDestroyView()
    }
}