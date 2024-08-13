package mega.privacy.android.app.presentation.photos.mediadiscovery

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.view.ActionMode
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
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
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.advertisements.model.AdsSlotIDs.TAB_CLOUD_SLOT_ID
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.hidenode.HiddenNodesOnboardingActivity
import mega.privacy.android.app.presentation.photos.albums.importlink.ImagePreviewProvider
import mega.privacy.android.app.presentation.photos.mediadiscovery.actionMode.MediaDiscoveryActionModeCallback
import mega.privacy.android.app.presentation.photos.mediadiscovery.model.MediaDiscoveryViewState
import mega.privacy.android.app.presentation.photos.mediadiscovery.view.MediaDiscoveryView
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.settings.SettingsActivity
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
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
    lateinit var imagePreviewProvider: ImagePreviewProvider

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
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val mode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val uiState by mediaDiscoveryViewModel.state.collectAsStateWithLifecycle()

                OriginalTempTheme(isDark = mode.isDarkMode()) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colors.background)
                            .clickable(
                                enabled = false,
                                onClick = {}
                            )
                    ) {
                        MediaDiscoveryView(
                            mediaDiscoveryGlobalStateViewModel = mediaDiscoveryGlobalStateViewModel,
                            mediaDiscoveryViewModel = mediaDiscoveryViewModel,
                            onOKButtonClicked = this@MediaDiscoveryFragment::onOKButtonClicked,
                            onSettingButtonClicked = this@MediaDiscoveryFragment::onSettingButtonClicked,
                            showSettingDialog = showSettingsDialog(uiState.mediaDiscoveryViewSettings),
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
                            onModalSheetVisibilityChange = this@MediaDiscoveryFragment::onModalSheetVisibilityChange,
                        )
                    }
                }
            }
        }
    }

    private fun onStartModalSheetShow() {
        managerActivity?.showHideBottomNavigationView(hide = true)
    }

    private fun onEndModalSheetHide() {
        managerActivity?.showHideBottomNavigationView(hide = false)
    }

    private fun onModalSheetVisibilityChange(isVisible: Boolean) {
        managerActivity?.showHideBottomNavigationView(hide = isVisible)
        if (isVisible) {
            managerActivity?.hideAdsView()
        } else {
            managerActivity?.handleShowingAds(TAB_CLOUD_SLOT_ID)
        }
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
            managerActivity?.handleCloudDriveBackNavigation(performBackNavigation = false)
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
                        handleGoBackLogic(state)
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

    private fun handleGoBackLogic(state: MediaDiscoveryViewState) {
        if (state.shouldGoBack)
            managerActivity?.onBackPressedDispatcher?.onBackPressed()
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

    private fun showSettingsDialog(mediaDiscoveryViewSettings: Int?): Boolean =
        mediaDiscoveryViewSettings == MediaDiscoveryViewSettings.INITIAL.ordinal
                && arguments?.getBoolean(INTENT_KEY_IS_ACCESSED_BY_ICON_CLICK, false) == false

    private fun onClick(photo: Photo) {
        if (mediaDiscoveryViewModel.state.value.selectedPhotoIds.isEmpty()) {
            imagePreviewProvider.onPreviewPhotoFromMD(
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
        managerActivity?.hideAdsView()
    }

    private fun exitActionMode() {
        actionMode?.finish()
        actionMode = null
        managerActivity?.showHideBottomNavigationView(false)
        managerActivity?.handleShowingAds(TAB_CLOUD_SLOT_ID)
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
                if (managerActivity?.isInMediaDiscovery() == false) {
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

    fun handleHideNodeClick() {
        val (isPaid, isHiddenNodesOnboarded) = with(mediaDiscoveryViewModel.state.value) {
            (this.accountType?.isPaid ?: false) to this.isHiddenNodesOnboarded
        }

        if (!isPaid) {
            val intent = HiddenNodesOnboardingActivity.createScreen(
                context = requireContext(),
                isOnboarding = false,
            )
            hiddenNodesOnboardingLauncher.launch(intent)
            activity?.overridePendingTransition(0, 0)
        } else if (isHiddenNodesOnboarded) {
            mediaDiscoveryViewModel.hideOrUnhideNodes(
                hide = true,
            )
        } else {
            showHiddenNodesOnboarding()
        }
    }

    private fun showHiddenNodesOnboarding() {
        mediaDiscoveryViewModel.setHiddenNodesOnboarded()

        val intent = HiddenNodesOnboardingActivity.createScreen(
            context = requireContext(),
            isOnboarding = true,
        )
        hiddenNodesOnboardingLauncher.launch(intent)
        activity?.overridePendingTransition(0, 0)
    }

    private val hiddenNodesOnboardingLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleHiddenNodesOnboardingResult,
        )

    private fun handleHiddenNodesOnboardingResult(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK) return

        mediaDiscoveryViewModel.hideOrUnhideNodes(
            hide = true,
        )
        val selectedSize = mediaDiscoveryViewModel.getSelectedIds().size

        val message =
            resources.getQuantityString(
                R.plurals.hidden_nodes_result_message,
                selectedSize,
                selectedSize,
            )
        Util.showSnackbar(requireActivity(), message)
    }

    companion object {
        internal const val INTENT_KEY_CURRENT_FOLDER_ID = "CURRENT_FOLDER_ID"
        private const val INTENT_KEY_IS_ACCESSED_BY_ICON_CLICK = "IS_ACCESSED_BY_ICON_CLICK"

        /**
         * The message to be displayed in the error banner
         */
        internal const val PARAM_ERROR_MESSAGE = "PARAM_ERROR_MESSAGE"

        /**
         * Creates a new instance of [MediaDiscoveryFragment]
         *
         * @param mediaHandle The Folder Handle used to view its Media
         * @param isAccessedByIconClick true if [MediaDiscoveryFragment] was accessed by clicking the Media
         * Discovery icon
         * @param errorMessage The [StringRes] of the error message to display
         */
        fun newInstance(
            mediaHandle: Long,
            isAccessedByIconClick: Boolean = false,
            @StringRes errorMessage: Int?,
        ): MediaDiscoveryFragment {
            return MediaDiscoveryFragment().apply {
                arguments = bundleOf(
                    INTENT_KEY_CURRENT_FOLDER_ID to mediaHandle,
                    INTENT_KEY_IS_ACCESSED_BY_ICON_CLICK to isAccessedByIconClick,
                    PARAM_ERROR_MESSAGE to errorMessage,
                )
            }
        }
    }
}

