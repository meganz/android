package mega.privacy.android.app.presentation.photos.mediadiscovery

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.photos.albums.importlink.AlbumImportPreviewProvider
import mega.privacy.android.app.presentation.photos.mediadiscovery.actionMode.MediaDiscoveryActionModeCallback
import mega.privacy.android.app.presentation.photos.mediadiscovery.view.MediaDiscoveryView
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.settings.SettingsActivity
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import javax.inject.Inject

/**
 * New Album Content View
 */
@AndroidEntryPoint
class MediaDiscoveryFragment : Fragment() {

    internal val mediaDiscoveryViewModel: MediaDiscoveryViewModel by viewModels()
    private val mediaDiscoveryGlobalStateViewModel: MediaDiscoveryGlobalStateViewModel by activityViewModels()

    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var getFeatureFlagUseCase: GetFeatureFlagValueUseCase
    internal var managerActivity: ManagerActivity? = null
    private var menu: Menu? = null

    // Action mode
    private var actionMode: ActionMode? = null
    private lateinit var actionModeCallback: MediaDiscoveryActionModeCallback

    @Inject
    lateinit var albumImportPreviewProvider: AlbumImportPreviewProvider

    companion object {
        @JvmStatic
        fun getNewInstance(
            mediaHandle: Long,
            isOpenByMDIcon: Boolean = false,
        ): MediaDiscoveryFragment {
            return MediaDiscoveryFragment().apply {
                arguments = bundleOf(
                    INTENT_KEY_CURRENT_FOLDER_ID to mediaHandle,
                    INTENT_KEY_OPEN_MEDIA_DISCOVERY_BY_MD_ICON to isOpenByMDIcon
                )
            }
        }

        internal const val INTENT_KEY_CURRENT_FOLDER_ID = "CURRENT_FOLDER_ID"
        private const val INTENT_KEY_OPEN_MEDIA_DISCOVERY_BY_MD_ICON =
            "OPEN_MEDIA_DISCOVERY_BY_MD_ICON"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        managerActivity = activity as? ManagerActivity
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
                val uiState by mediaDiscoveryViewModel.state.collectAsStateWithLifecycle()
                val isNewMediaDiscoveryFabEnabled by produceState(initialValue = false) {
                    value = getFeatureFlagUseCase(AppFeatures.NewMediaDiscoveryFab)
                }
                AndroidTheme(isDark = mode.isDarkMode()) {
                    MediaDiscoveryView(
                        mediaDiscoveryGlobalStateViewModel = mediaDiscoveryGlobalStateViewModel,
                        mediaDiscoveryViewModel = mediaDiscoveryViewModel,
                        onOKButtonClicked = this@MediaDiscoveryFragment::onOKButtonClicked,
                        onSettingButtonClicked = this@MediaDiscoveryFragment::onSettingButtonClicked,
                        showSettingDialog = showSettingDialog(uiState.mediaDiscoveryViewSettings),
                        onZoomIn = this@MediaDiscoveryFragment::handleZoomIn,
                        onZoomOut = this@MediaDiscoveryFragment::handleZoomOut,
                        onPhotoClicked = this@MediaDiscoveryFragment::onClick,
                        onPhotoLongPressed = this@MediaDiscoveryFragment::onLongPress,
                        onCardClick = mediaDiscoveryViewModel::onCardClick,
                        onTimeBarTabSelected = mediaDiscoveryViewModel::onTimeBarTabSelected,
                        onSwitchListView = this@MediaDiscoveryFragment::onSwitchListView,
                        onCapture = this@MediaDiscoveryFragment::onCapture,
                        onUploadFiles = this@MediaDiscoveryFragment::onUploadFiles,
                        onStartModalSheetShow = this@MediaDiscoveryFragment::onStartModalSheetShow,
                        onEndModalSheetHide = this@MediaDiscoveryFragment::onEndModalSheetHide,
                        isNewMediaDiscoveryFabEnabled = isNewMediaDiscoveryFabEnabled
                    )
                }
            }
        }
    }

    private fun onStartModalSheetShow() {
        managerActivity?.showHideBottomNavigationView(true)
    }

    private fun onEndModalSheetHide() {
        managerActivity?.showHideBottomNavigationView(false)
    }

    private fun onUploadFiles() {
        managerActivity?.uploadFiles()
    }

    private fun onCapture() {
        managerActivity?.takePictureAndUpload()
    }

    private fun onSwitchListView() {
        lifecycleScope.launch {
            mediaDiscoveryViewModel.setListViewTypeClicked()
            managerActivity?.switchToCDFromMD()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFlow()
        setupParentActivityUI()
        setupMenu()
    }

    /**
     * Setup ManagerActivity UI
     */
    private fun setupParentActivityUI() {
        managerActivity?.run {
            setToolbarTitle()
            invalidateOptionsMenu()
            hideFabButton()
        }
        managerActivity?.invalidateOptionsMenu()
        managerActivity?.hideFabButton()
    }

    private fun setupFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
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

                        handleSlidersMenuIconVisibility()
                    }
                }

                launch {
                    mediaDiscoveryGlobalStateViewModel.state.collect { zoomLevel ->
                        mediaDiscoveryViewModel.updateZoomLevel(zoomLevel)
                    }
                }

                launch {
                    mediaDiscoveryGlobalStateViewModel.filterState.collect { filterType ->
                        mediaDiscoveryViewModel.setCurrentMediaType(filterType)
                    }
                }
            }
        }
    }

    private fun onOKButtonClicked() {
        mediaDiscoveryViewModel.setMediaDiscoveryViewSettings(
            MediaDiscoveryViewSettings.ENABLED.ordinal
        )
    }

    private fun onSettingButtonClicked() {
        mediaDiscoveryViewModel.setMediaDiscoveryViewSettings(
            MediaDiscoveryViewSettings.ENABLED.ordinal
        )
        requireContext().startActivity(
            Intent(
                requireActivity(),
                SettingsActivity::class.java
            )
        )
    }


    private fun showSettingDialog(mediaDiscoveryViewSettings: Int?): Boolean {
        return mediaDiscoveryViewSettings == MediaDiscoveryViewSettings.INITIAL.ordinal
                && arguments?.getBoolean(
            INTENT_KEY_OPEN_MEDIA_DISCOVERY_BY_MD_ICON,
            false
        ) == false
    }

    private fun onClick(photo: Photo) {
        if (mediaDiscoveryViewModel.state.value.selectedPhotoIds.isEmpty()) {
            albumImportPreviewProvider.onPreviewPhotoFromMD(
                activity = this.requireActivity(),
                photo = photo,
                photoIds = mediaDiscoveryViewModel.getAllPhotoIds(),
                currentSort = mediaDiscoveryViewModel.state.value.currentSort,
                folderNodeId = mediaDiscoveryViewModel.state.value.currentFolderId
            )
        } else if (actionMode != null) {
            mediaDiscoveryViewModel.togglePhotoSelection(photo.id)
        }
    }

    private fun onLongPress(photo: Photo) {
        handleActionMode(photo)
    }

    private fun enterActionMode() {
        actionMode = managerActivity?.startSupportActionMode(
            actionModeCallback
        )
        managerActivity?.showHideBottomNavigationView(true)
    }

    private fun exitActionMode() {
        actionMode?.finish()
        actionMode = null
        managerActivity?.showHideBottomNavigationView(false)
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

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                if (managerActivity?.isInMDMode() == false) {
                    return
                }
                menuInflater.inflate(R.menu.fragment_media_discovery_toolbar, menu)
                this@MediaDiscoveryFragment.menu = menu
                handleSlidersMenuIconVisibility()
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    R.id.action_menu_sliders -> {
                        mediaDiscoveryViewModel.showSlidersPopup(
                            !mediaDiscoveryViewModel.state.value.showSlidersPopup
                        )
                        true
                    }

                    else -> true
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun handleSlidersMenuIconVisibility() {
        menu?.findItem(R.id.action_menu_sliders)?.isVisible =
            mediaDiscoveryViewModel.state.value.selectedTimeBarTab == TimeBarTab.All
    }

    private fun handleZoomOut() {
        mediaDiscoveryGlobalStateViewModel.zoomOut()
        with(mediaDiscoveryViewModel) {
            handlePhotoItems(
                sortAndFilterPhotos(state.value.sourcePhotos),
                state.value.sourcePhotos
            )
        }
    }

    private fun handleZoomIn() {
        mediaDiscoveryGlobalStateViewModel.zoomIn()
        with(mediaDiscoveryViewModel) {
            handlePhotoItems(
                sortAndFilterPhotos(state.value.sourcePhotos),
                state.value.sourcePhotos
            )
        }
    }
}

