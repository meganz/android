package mega.privacy.android.app.gallery.ui

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.annotation.NonNull
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentMediaDecoveryBinding
import mega.privacy.android.app.fragments.homepage.*
import mega.privacy.android.app.gallery.adapter.GalleryCardAdapter
import mega.privacy.android.app.gallery.constant.MEDIA_HANDLE
import mega.privacy.android.app.gallery.data.GalleryCard
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.fragment.BaseZoomFragment
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.Constants.*
import java.util.*

@AndroidEntryPoint
class MediaDiscoveryFragment : BaseZoomFragment(), GalleryCardAdapter.Listener {

    private val viewModel by viewModels<MediaViewModel>()

    private lateinit var binding: FragmentMediaDecoveryBinding

    private var selectedView = ALL_VIEW

    private var currentHandle: Long = 0L

    companion object {
        private var instance: MediaDiscoveryFragment? = null

        fun getInstance():MediaDiscoveryFragment{
            if (instance == null){
                instance  = MediaDiscoveryFragment()
            }
            return instance!!
        }

        var isInMediaDiscovery = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        isInMediaDiscovery = true
        binding = FragmentMediaDecoveryBinding.inflate(inflater, container, false)
        arguments?.let {
            currentHandle = it.getLong(MEDIA_HANDLE)
            viewModel.setHandle(currentHandle)
        }

        setupBinding()
        setupParentActivityUI()
        return binding.root
    }

    private fun setupParentActivityUI() {
        (context as ManagerActivityLollipop).setToolbarTitle()
        (context as ManagerActivityLollipop).invalidateOptionsMenu()
        (context as ManagerActivityLollipop).hideFabButton()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchData()
        initViewCreated()
        subscribeObservers()
    }

    private fun fetchData() {
        viewModel.getAndFilterFilesByHandle()
    }

    private fun setupBinding() {
        binding.apply {
            viewModel = this@MediaDiscoveryFragment.viewModel
            lifecycleOwner = viewLifecycleOwner
            viewTypePanel = photosViewType.root
        }

        listView = binding.photoList
        scroller = binding.scroller
    }

    private fun initViewCreated() {
        val currentZoom = ZoomUtil.MEDIA_ZOOM_LEVEL
        zoomViewModel.setCurrentZoom(currentZoom)
        zoomViewModel.setZoom(currentZoom)
        viewModel.setZoom(currentZoom)
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
            setupListAdapter(getCurrentZoom(), it)
            actionModeViewModel.setNodesData(it.filter { nodeItem -> nodeItem.type != GalleryItem.TYPE_HEADER })
            if (it.isEmpty()) {
                handleOptionsMenuUpdate(false)
                viewTypePanel.visibility = View.GONE
            } else {
                handleOptionsMenuUpdate(shouldShowZoomMenuItem())
                viewTypePanel.visibility = View.VISIBLE
            }
        }

        viewModel.dateCards.observe(viewLifecycleOwner, ::showCards)

        viewModel.refreshCards.observe(viewLifecycleOwner) {
            if (it && selectedView != ALL_VIEW) {
                showCards(viewModel.dateCards.value)
                viewModel.refreshCompleted()
            }
        }
    }

    override fun getViewType() = selectedView

    override fun getAdapterType() = MEDIA_BROWSE_ADAPTER

    override fun onCardClicked(position: Int, @NonNull card: GalleryCard) {
        when (selectedView) {
            DAYS_VIEW -> {
                zoomViewModel.restoreDefaultZoom()
                handleZoomMenuItemStatus()
                newViewClicked(ALL_VIEW)
                val photoPosition = gridAdapter.getNodePosition(card.node.handle)
                layoutManager.scrollToPosition(photoPosition)

                val node = gridAdapter.getNodeAtPosition(photoPosition)
                node?.let {
                    RunOnUIThreadUtils.post {
                        openPhoto(getOrder(), it)
                    }
                }
            }
            MONTHS_VIEW -> {
                newViewClicked(DAYS_VIEW)
                layoutManager.scrollToPosition(viewModel.monthClicked(position, card))
            }
            YEARS_VIEW -> {
                newViewClicked(MONTHS_VIEW)
                layoutManager.scrollToPosition(viewModel.yearClicked(position, card))
            }
        }

        showViewTypePanel()
    }

    private fun setupEmptyHint() {
        binding.emptyHint.emptyHintImage.isVisible = false
        binding.emptyHint.emptyHintText.isVisible = false
        binding.emptyHint.emptyHintText.text =
            getString(R.string.homepage_empty_hint_photos).toUpperCase(Locale.ROOT)
    }

    private fun setupTimePanel() {
        yearsButton = binding.photosViewType.yearsButton.apply {
            setOnClickListener {
                newViewClicked(YEARS_VIEW)
            }
        }
        monthsButton = binding.photosViewType.monthsButton.apply {
            setOnClickListener {
                newViewClicked(MONTHS_VIEW)
            }
        }
        daysButton = binding.photosViewType.daysButton.apply {
            setOnClickListener {
                newViewClicked(DAYS_VIEW)
            }
        }
        allButton = binding.photosViewType.allButton.apply {
            setOnClickListener {
                newViewClicked(ALL_VIEW)
            }
        }

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val params = viewTypePanel.layoutParams
            params.width = outMetrics.heightPixels
            viewTypePanel.layoutParams = params
        }

        updateViewSelected()
        setHideBottomViewScrollBehaviour()
    }

    /**
     * First make all the buttons unselected,
     * then apply selected style for the selected button regarding to the selected view.
     */
    private fun updateViewSelected() {
        super.updateViewSelected(allButton, daysButton, monthsButton, yearsButton, selectedView)
    }

    /**
     * Show the selected card view after corresponding button is clicked.
     *
     * @param selectedView The selected view.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun newViewClicked(selectedView: Int) {
        if (this.selectedView == selectedView) return

        this.selectedView = selectedView
        setupListAdapter(getCurrentZoom(), viewModel.items.value)

        when (selectedView) {
            DAYS_VIEW, MONTHS_VIEW, YEARS_VIEW -> {
                showCards(
                    viewModel.dateCards.value
                )

                listView.setOnTouchListener(null)
            }
            else -> {
                listView.setOnTouchListener(scaleGestureHandler)
            }
        }
        handleOptionsMenuUpdate(shouldShowZoomMenuItem())
        updateViewSelected()
        setHideBottomViewScrollBehaviour()
    }

    /**
     * Only refresh the list items of uiDirty = true
     */
    override fun updateUiWhenAnimationEnd() {
        viewModel.items.value?.let {
            val newList = ArrayList(it)
            gridAdapter.submitList(newList)
        }
    }

    private fun handleZoomAdapterLayoutChange(zoom: Int) {
        val state = listView.layoutManager?.onSaveInstanceState()
        setupListAdapter(zoom, viewModel.items.value)
        viewModel.setZoom(zoom)
        listView.layoutManager?.onRestoreInstanceState(state)
    }

    /**
     * Display the view type buttons panel with animation effect, after a card is clicked.
     */
    private fun showViewTypePanel() {
        val params = viewTypePanel.layoutParams as CoordinatorLayout.LayoutParams
        params.setMargins(
            0, 0, 0,
            resources.getDimensionPixelSize(R.dimen.cu_view_type_button_vertical_margin)
        )
        viewTypePanel.animate().translationY(0f).setDuration(175)
            .withStartAction { viewTypePanel.visibility = View.VISIBLE }
            .withEndAction { viewTypePanel.layoutParams = params }.start()
    }

    fun loadPhotos() {
        if(isAdded) viewModel.loadPhotos(true)
    }

    /**
     * Show the view with the data of years, months or days depends on selected view.
     *
     * @param dateCards
     *          The first element is the cards of days.
     *          The second element is the cards of months.
     *          The third element is the cards of years.
     */
    private fun showCards(dateCards: List<List<GalleryCard>?>?) {
        val index = when (selectedView) {
            DAYS_VIEW -> DAYS_INDEX
            MONTHS_VIEW -> MONTHS_INDEX
            YEARS_VIEW -> YEARS_INDEX
            else -> -1
        }

        if (index != -1) {
            cardAdapter.submitList(dateCards?.get(index))
        }

        updateFastScrollerVisibility()
    }

    override fun getNodeCount() = viewModel.getRealPhotoCount()

    override fun getOrder() = viewModel.getOrder()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.selectedViewTypeMedia = selectedView
    }

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

    fun isInThisPage(): Boolean {
        return isInMediaDiscovery
    }

    override fun onDestroy() {
        isInMediaDiscovery = false
        instance = null
        super.onDestroy()
    }

    fun onBackPressed(){
        (context as ManagerActivityLollipop).onBackPressed()
    }
}