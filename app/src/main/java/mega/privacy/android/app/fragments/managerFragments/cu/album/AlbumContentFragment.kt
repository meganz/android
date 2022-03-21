package mega.privacy.android.app.fragments.managerFragments.cu.album

import android.os.Bundle
import android.view.*
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentAlbumContentBinding
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.fragment.BaseZoomFragment
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants.ALBUM_CONTENT_ADAPTER
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.ZoomUtil

/**
 * AlbumContentFragment is using to show album content when click album cover.
 */
@AndroidEntryPoint
class AlbumContentFragment : BaseZoomFragment() {

    override val viewModel by viewModels<AlbumContentViewModel>()

    private lateinit var binding: FragmentAlbumContentBinding

    /**
     * Current order.
     */
    private var order = 0

    companion object {

        @JvmStatic
        fun getInstance(): AlbumContentFragment {
            return AlbumContentFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        order = viewModel.getOrder()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAlbumContentBinding.inflate(inflater, container, false)
        adapterType = ALBUM_CONTENT_ADAPTER
        setupBinding()
        setupParentActivityUI()
        return binding.root
    }

    /**
     * Setup ManagerActivity UI
     */
    private fun setupParentActivityUI() {
        mManagerActivity.setToolbarTitle()
        mManagerActivity.invalidateOptionsMenu()
        mManagerActivity.hideFabButton()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewCreated()
        subscribeObservers()
    }

    /**
     * Setup UI Binding
     */
    private fun setupBinding() {
        binding.apply {
            viewModel = this@AlbumContentFragment.viewModel
            lifecycleOwner = viewLifecycleOwner
        }

        listView = binding.photoList
        scroller = binding.scroller
    }

    /**
     * Handle init logic when view created
     */
    private fun initViewCreated() {
        val currentZoom = ZoomUtil.ALBUM_ZOOM_LEVEL
        zoomViewModel.setCurrentZoom(currentZoom)
        zoomViewModel.setZoom(currentZoom)
        viewModel.mZoom = currentZoom
        setupEmptyHint()
        setupListView()
        setupListAdapter(currentZoom, viewModel.items.value)
    }

    override fun handleZoomChange(zoom: Int, needReload: Boolean) {
        ZoomUtil.ALBUM_ZOOM_LEVEL = zoom
        handleZoomAdapterLayoutChange(zoom)
        if (needReload) {
            loadPhotos()
        }
    }

    override fun handleOnCreateOptionsMenu() {
        val hasImages = gridAdapterHasData()
        handleOptionsMenuUpdate(hasImages)
    }

    /**
     * Subscribe Observers
     */
    private fun subscribeObservers() {
        viewModel.items.observe(viewLifecycleOwner) {
            // Order changed.
            if (order != viewModel.getOrder()) {
                setupListAdapter(getCurrentZoom(), it)
                order = viewModel.getOrder()
            }

            actionModeViewModel.setNodesData(it.filter { nodeItem -> nodeItem.type != GalleryItem.TYPE_HEADER })
            handleOptionsMenuUpdate(it.isNotEmpty())
        }
    }

    /**
     * Setup empty UI status like text, image
     */
    private fun setupEmptyHint() {
        binding.emptyHint.emptyHintImage.isVisible = false
        binding.emptyHint.emptyHintText.isVisible = false
        binding.emptyHint.emptyHintImage.setImageResource(R.drawable.ic_zero_data_favourites)

        ColorUtils.setImageViewAlphaIfDark(
            context,
            binding.emptyHint.emptyHintImage,
            ColorUtils.DARK_IMAGE_ALPHA
        )
        binding.emptyHint.emptyHintText.text = HtmlCompat.fromHtml(
            TextUtil.formatEmptyScreenText(
                context,
                StringResourcesUtils.getString(R.string.empty_hint_favourite_album)
            ),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }

    /**
     * Handle logic when zoom adapter layout change
     */
    private fun handleZoomAdapterLayoutChange(zoom: Int) {
        val state = listView.layoutManager?.onSaveInstanceState()
        setupListAdapter(zoom, viewModel.items.value)
        viewModel.mZoom = zoom
        listView.layoutManager?.onRestoreInstanceState(state)
    }

    /**
     * Load photos
     */
    fun loadPhotos() {
        if (isAdded) viewModel.loadPhotos(true)
    }

    override fun getOrder() = viewModel.getOrder()

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (!isInThisPage()) {
            return
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (!isInThisPage()) {
            true
        } else super.onOptionsItemSelected(item)
    }

    /**
     * Check if in AlbumContentFragment
     *
     * @return true, in AlbumContentFragment; false, not in AlbumContentFragment
     */
    private fun isInThisPage(): Boolean {
        return mManagerActivity.isInAlbumContentPage
    }

    override fun onDestroyView() {
        viewModel.cancelSearch()
        super.onDestroyView()
    }
}