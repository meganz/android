package mega.privacy.android.app.gallery.ui

import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentMediaDecoveryBinding
import mega.privacy.android.app.fragments.homepage.*
import mega.privacy.android.app.gallery.constant.MEDIA_HANDLE
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.fragment.BaseZoomFragment
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.Constants.*
import java.util.*

@AndroidEntryPoint
class MediaDiscoveryFragment : BaseZoomFragment() {

    override val viewModel by viewModels<MediaViewModel>()

    private lateinit var binding: FragmentMediaDecoveryBinding

    /**
     * Current order.
     */
    private var order = 0

    companion object {

        @JvmStatic
        fun getInstance(mediaHandle:Long): MediaDiscoveryFragment {
            val fragment = MediaDiscoveryFragment()
            val args = Bundle()
            args.putLong(MEDIA_HANDLE, mediaHandle);
            fragment.arguments = args

            return fragment
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
        binding = FragmentMediaDecoveryBinding.inflate(inflater, container, false)
        adapterType = MEDIA_BROWSE_ADAPTER
        setupBinding()
        setupParentActivityUI()
        return binding.root
    }

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

    private fun setupBinding() {
        binding.apply {
            viewModel = this@MediaDiscoveryFragment.viewModel
            lifecycleOwner = viewLifecycleOwner
            viewTypePanel = photosViewType.root
        }

        listView = binding.photoList
        scroller = binding.scroller
        viewTypePanel = binding.photosViewType.root
        yearsButton = binding.photosViewType.yearsButton
        monthsButton = binding.photosViewType.monthsButton
        daysButton = binding.photosViewType.daysButton
        allButton = binding.photosViewType.allButton
    }

    private fun initViewCreated() {
        val currentZoom = ZoomUtil.MEDIA_ZOOM_LEVEL
        zoomViewModel.setCurrentZoom(currentZoom)
        zoomViewModel.setZoom(currentZoom)
        viewModel.mZoom = currentZoom
        setupEmptyHint()
        setupListView()
        setupTimePanel()
        setupListAdapter(currentZoom, viewModel.items.value)
    }

    override fun handleZoomChange(zoom: Int, needReload: Boolean) {
        ZoomUtil.MEDIA_ZOOM_LEVEL = zoom
        handleZoomAdapterLayoutChange(zoom)
        if (needReload) {
            loadPhotos()
        }
    }

    override fun handleOnCreateOptionsMenu() {
        val hasImages = gridAdapterHasData()
        handleOptionsMenuUpdate(hasImages && shouldShowZoomMenuItem())
    }

    private fun subscribeObservers() {
        viewModel.items.observe(viewLifecycleOwner) {
            // Order changed.
            if (order != viewModel.getOrder()) {
                setupListAdapter(getCurrentZoom(), it)
                order = viewModel.getOrder()
            }

            actionModeViewModel.setNodesData(it.filter { nodeItem -> nodeItem.type != GalleryItem.TYPE_HEADER })
            if (it.isEmpty()) {
                handleOptionsMenuUpdate(false)
                viewTypePanel.visibility = View.GONE
            } else {
                handleOptionsMenuUpdate(shouldShowZoomMenuItem())
                viewTypePanel.visibility = View.VISIBLE
            }
        }
    }

    private fun setupEmptyHint() {
        binding.emptyHint.emptyHintImage.isVisible = false
        binding.emptyHint.emptyHintText.isVisible = false
        binding.emptyHint.emptyHintText.text =
            getString(R.string.homepage_empty_hint_photos).toUpperCase(Locale.ROOT)
    }

    /**
     * Show the selected card view after corresponding button is clicked.
     *
     * @param selectedView The selected view.
     */
    override fun newViewClicked(selectedView: Int) {
        if(this.selectedView == selectedView) return

        super.newViewClicked(selectedView)
        setHideBottomViewScrollBehaviour()
    }

    private fun handleZoomAdapterLayoutChange(zoom: Int) {
        val state = listView.layoutManager?.onSaveInstanceState()
        setupListAdapter(zoom, viewModel.items.value)
        viewModel.mZoom = zoom
        listView.layoutManager?.onRestoreInstanceState(state)
    }

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

    private fun isInThisPage(): Boolean {
        return mManagerActivity.isInMDPage
    }
}