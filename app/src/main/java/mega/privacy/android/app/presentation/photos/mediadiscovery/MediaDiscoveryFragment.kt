package mega.privacy.android.app.presentation.photos.mediadiscovery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.photos.PhotosViewModel
import mega.privacy.android.app.presentation.photos.mediadiscovery.actionMode.MediaDiscoveryActionModeCallback
import mega.privacy.android.app.presentation.photos.mediadiscovery.model.MediaDiscoveryViewState
import mega.privacy.android.app.presentation.photos.model.DateCard
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.model.ZoomLevel
import mega.privacy.android.app.presentation.photos.view.CardListView
import mega.privacy.android.app.presentation.photos.view.PhotosGridView
import mega.privacy.android.app.presentation.photos.view.TimeSwitchBar
import mega.privacy.android.app.presentation.photos.view.showSortByDialog
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.presentation.theme.AndroidTheme
import javax.inject.Inject

/**
 * New Album Content View
 */
@AndroidEntryPoint
class MediaDiscoveryFragment : Fragment() {

    private val photosViewModel: PhotosViewModel by viewModels()
    internal val mediaDiscoveryViewModel: MediaDiscoveryViewModel by viewModels()

    @Inject
    lateinit var getThemeMode: GetThemeMode
    internal lateinit var managerActivity: ManagerActivity
    private var menu: Menu? = null

    // Action mode
    private var actionMode: ActionMode? = null
    private lateinit var actionModeCallback: MediaDiscoveryActionModeCallback

    companion object {
        @JvmStatic
        fun getNewInstance(mediaHandle: Long): MediaDiscoveryFragment {
            return MediaDiscoveryFragment().apply {
                arguments = bundleOf(INTENT_KEY_CURRENT_FOLDER_ID to mediaHandle)
            }
        }

        internal const val INTENT_KEY_CURRENT_FOLDER_ID = "CURRENT_FOLDER_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        managerActivity = activity as ManagerActivity
        actionModeCallback =
            MediaDiscoveryActionModeCallback(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val mode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                AndroidTheme(isDark = mode.isDarkMode()) {
                    MDContentBody(mediaDiscoveryViewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        setupFlow()
        setupParentActivityUI()
    }

    /**
     * Setup ManagerActivity UI
     */
    private fun setupParentActivityUI() {
        managerActivity.run {
            setToolbarTitle()
            invalidateOptionsMenu()
            hideFabButton()
        }
        managerActivity.invalidateOptionsMenu()
        managerActivity.hideFabButton()
    }

    private fun setupFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                mediaDiscoveryViewModel.state.collect { state ->
                    if (state.selectedPhotoIds.isEmpty()) {
                        if (actionMode != null) {
                            exitActionMode()
                        }
                    } else {
                        if (actionMode == null) {
                            enterActionMode()
                        }
                        actionMode?.title = state.selectedPhotoIds.size.toString()
                    }
                    menu?.let {
                        handleMenuIconsVisibility(isShowing = state.selectedTimeBarTab == TimeBarTab.All)
                        if (state.selectedTimeBarTab == TimeBarTab.All) {
                            handleZoomMenuEnableStatus()
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun MDContentBody(
        viewModel: MediaDiscoveryViewModel = viewModel(),
    ) {
        val uiState by viewModel.state.collectAsStateWithLifecycle()

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomEnd,
        ) {
            if (uiState.selectedTimeBarTab == TimeBarTab.All) {
                PhotosGridView(uiState = uiState)
            } else {
                val dateCards = when (uiState.selectedTimeBarTab) {
                    TimeBarTab.Years -> uiState.yearsCardList
                    TimeBarTab.Months -> uiState.monthsCardList
                    TimeBarTab.Days -> uiState.daysCardList
                    else -> uiState.daysCardList
                }
                CardListView(dateCards = dateCards, uiState = uiState)
            }

            if (uiState.selectedPhotoIds.isEmpty()) {
                TimeSwitchBar(uiState = uiState)
            }
        }
    }

    @Composable
    fun CardListView(
        dateCards: List<DateCard>,
        uiState: MediaDiscoveryViewState,
    ) = CardListView(
        dateCards = dateCards,
        photoDownload = photosViewModel::downloadPhoto,
        onCardClick = mediaDiscoveryViewModel::onCardClick,
        state = rememberSaveable(
            uiState.scrollStartIndex,
            uiState.scrollStartOffset,
            saver = LazyGridState.Saver
        ) {
            LazyGridState(
                uiState.scrollStartIndex,
                uiState.scrollStartOffset
            )
        }
    )

    @Composable
    fun PhotosGridView(uiState: MediaDiscoveryViewState) = PhotosGridView(
        currentZoomLevel = uiState.currentZoomLevel,
        photoDownland = photosViewModel::downloadPhoto,
        onClick = this::onClick,
        onLongPress = this::onLongPress,
        selectedPhotoIds = mediaDiscoveryViewModel.state.value.selectedPhotoIds,
        uiPhotoList = uiState.uiPhotoList,
    )

    @Composable
    fun TimeSwitchBar(uiState: MediaDiscoveryViewState) = TimeSwitchBar(
        selectedTimeBarTab = uiState.selectedTimeBarTab,
        onTimeBarTabSelected = mediaDiscoveryViewModel::onTimeBarTabSelected
    )

    private fun openPhoto(photo: Photo) {
        ImageViewerActivity.getIntentForChildren(
            requireContext(),
            mediaDiscoveryViewModel.getAllPhotoIds().toLongArray(),
            photo.id,
        ).run {
            startActivity(this)
        }
        managerActivity.overridePendingTransition(0, 0)
    }

    private fun onClick(photo: Photo) {
        if (mediaDiscoveryViewModel.state.value.selectedPhotoIds.isEmpty()) {
            openPhoto(photo)
        } else if (actionMode != null) {
            mediaDiscoveryViewModel.togglePhotoSelection(photo.id)
        }
    }

    private fun onLongPress(photo: Photo) {
        handleActionMode(photo)
    }

    private fun enterActionMode() {
        actionMode = managerActivity.startSupportActionMode(
            actionModeCallback
        )
        managerActivity.showHideBottomNavigationView(true)
    }

    private fun exitActionMode() {
        actionMode?.finish()
        actionMode = null
        managerActivity.showHideBottomNavigationView(false)
    }

    private fun handleActionMode(photo: Photo) {
        if (mediaDiscoveryViewModel.state.value.selectedPhotoIds.isEmpty()) {
            if (actionMode == null) {
                enterActionMode()
            }
            mediaDiscoveryViewModel.togglePhotoSelection(photo.id)
        } else {
            onClick(photo)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_media_discovery_toolbar, menu)
        super.onCreateOptionsMenu(menu, inflater)
        this.menu = menu
        handleMenuIconsVisibility(
            isShowing = mediaDiscoveryViewModel.state.value.selectedTimeBarTab == TimeBarTab.All
        )
    }

    private fun handleMenuIconsVisibility(isShowing: Boolean) {
        this.menu?.apply {
            findItem(R.id.action_zoom_in)?.isVisible = isShowing
            findItem(R.id.action_zoom_out)?.isVisible = isShowing
            findItem(R.id.action_menu_sort_by)?.isVisible = isShowing
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_zoom_in -> {
                mediaDiscoveryViewModel.zoomIn()
            }
            R.id.action_zoom_out -> {
                mediaDiscoveryViewModel.zoomOut()
            }
            R.id.action_menu_sort_by -> {
                showSortByDialog(
                    context = managerActivity,
                    checkedItem = mediaDiscoveryViewModel.state.value.currentSort.ordinal,
                    onClickListener = { _, i ->
                        mediaDiscoveryViewModel.setCurrentSort(Sort.values()[i])
                    },
                )
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleZoomMenuEnableStatus() {
        val isZoomInValid =
            mediaDiscoveryViewModel.state.value.currentZoomLevel != ZoomLevel.values()
                .first()
        val isZoomOutValid =
            mediaDiscoveryViewModel.state.value.currentZoomLevel != ZoomLevel.values()
                .last()
        this.menu?.let { menu ->
            menu.findItem(R.id.action_zoom_in)?.let {
                it.isEnabled = isZoomInValid
                it.icon?.alpha = if (isZoomInValid) 255 else 125
            }
            menu.findItem(R.id.action_zoom_out)?.let {
                it.isEnabled = isZoomOutValid
                it.icon?.alpha = if (isZoomOutValid) 255 else 125
            }
        }
    }
}

