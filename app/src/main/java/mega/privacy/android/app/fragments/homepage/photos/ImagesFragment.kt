package mega.privacy.android.app.fragments.homepage.photos

import android.os.Bundle
import android.view.*
import androidx.annotation.NonNull
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentImagesBinding
import mega.privacy.android.app.fragments.homepage.*
import mega.privacy.android.app.gallery.data.GalleryCard
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.fragment.BaseZoomFragment
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.Constants.*
import nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_DESC
import java.util.*

@AndroidEntryPoint
class ImagesFragment : BaseZoomFragment() {

    override val viewModel by viewModels<ImagesViewModel>()

    private lateinit var binding: FragmentImagesBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentImagesBinding.inflate(inflater, container, false)
        adapterType = PHOTOS_BROWSE_ADAPTER
        setupBinding()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewCreated()
        subscribeObservers()
    }

    private fun setupBinding() {
        binding.apply {
            viewModel = this@ImagesFragment.viewModel
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
        val currentZoom = ZoomUtil.IMAGES_ZOOM_LEVEL
        zoomViewModel.setCurrentZoom(currentZoom)
        zoomViewModel.setZoom(currentZoom)
        viewModel.mZoom = currentZoom
        setupEmptyHint()
        setupListView()
        setupTimePanel()
        setupListAdapter(currentZoom, viewModel.items.value)
    }

    override fun handleZoomChange(zoom: Int, needReload: Boolean) {
        ZoomUtil.IMAGES_ZOOM_LEVEL = zoom
        handleZoomAdapterLayoutChange(zoom)
        if (needReload) {
            loadPhotos()
        }
    }

    override fun handleOnCreateOptionsMenu() {
        val hasImages = gridAdapterHasData()
        handleOptionsMenuUpdate(hasImages && shouldShowZoomMenuItem())
        removeSortByMenu()
    }

    private fun subscribeObservers() {
        viewModel.items.observe(viewLifecycleOwner) {
            actionModeViewModel.setNodesData(it.filter { nodeItem -> nodeItem.type == GalleryItem.TYPE_IMAGE })
            if (it.isEmpty()) {
                handleOptionsMenuUpdate(false)
                viewTypePanel.visibility = View.GONE
            } else {
                handleOptionsMenuUpdate(shouldShowZoomMenuItem())
                viewTypePanel.visibility = View.VISIBLE
            }
            removeSortByMenu()
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
        removeSortByMenu()
        // If selected view is not all view, add layout param behaviour, so that button panel will go off when scroll.
        setHideBottomViewScrollBehaviour()
    }

    private fun handleZoomAdapterLayoutChange(zoom: Int) {
        val state = listView.layoutManager?.onSaveInstanceState()
        setupListAdapter(zoom, viewModel.items.value)
        viewModel.mZoom = zoom
        listView.layoutManager?.onRestoreInstanceState(state)
    }

    fun loadPhotos() = viewModel.loadPhotos(true)

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (activity as ManagerActivityLollipop? != null && (activity as ManagerActivityLollipop?)!!.isInImagesPage) {
            super.onCreateOptionsMenu(menu, inflater)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (activity as ManagerActivityLollipop? != null && (activity as ManagerActivityLollipop?)!!.isInImagesPage) {
            return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun getOrder() = ORDER_MODIFICATION_DESC
}