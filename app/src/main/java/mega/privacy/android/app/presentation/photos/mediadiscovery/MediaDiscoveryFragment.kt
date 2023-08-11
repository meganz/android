package mega.privacy.android.app.presentation.photos.mediadiscovery

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
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
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.photos.albums.importlink.AlbumImportPreviewProvider
import mega.privacy.android.app.presentation.photos.mediadiscovery.actionMode.MediaDiscoveryActionModeCallback
import mega.privacy.android.app.presentation.photos.mediadiscovery.view.MediaDiscoveryView
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.model.ZoomLevel
import mega.privacy.android.app.presentation.settings.SettingsActivity
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import mega.privacy.android.app.utils.Util
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetThemeMode
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
    internal lateinit var managerActivity: ManagerActivity
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
                val uiState by mediaDiscoveryViewModel.state.collectAsStateWithLifecycle()

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
                    )
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
                        menu?.let {
                            handleKebabMenuIconsVisibility(
                                isShowing = state.selectedTimeBarTab == TimeBarTab.All
                            )
                            handleZoomMenuIconsVisibility(
                                isShowing = state.selectedTimeBarTab == TimeBarTab.All
                                        && state.uiPhotoList.isNotEmpty()
                            )
                            if (state.selectedTimeBarTab == TimeBarTab.All && state.uiPhotoList.isNotEmpty()) {
                                handleZoomMenuEnableStatus()
                            }
                            if (state.selectedTimeBarTab == TimeBarTab.All) {
                                handleSortByMenuItemEnableStatus()
                            }
                        }
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
            )
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
        if (!managerActivity.isInMDMode()) {
            return
        }
        inflater.inflate(R.menu.fragment_media_discovery_toolbar, menu)
        super.onCreateOptionsMenu(menu, inflater)
        this.menu = menu
        mediaDiscoveryViewModel.state.value.apply {
            handleKebabMenuIconsVisibility(
                isShowing = selectedTimeBarTab == TimeBarTab.All
            )
            handleZoomMenuIconsVisibility(
                isShowing = selectedTimeBarTab == TimeBarTab.All
                        && uiPhotoList.isNotEmpty()
            )
            if (selectedTimeBarTab == TimeBarTab.All) {
                handleSortByMenuItemEnableStatus()
            }
        }
    }

    private fun handleZoomMenuIconsVisibility(isShowing: Boolean) {
        this.menu?.apply {
            findItem(R.id.action_zoom_in)?.isVisible = isShowing
            findItem(R.id.action_zoom_out)?.isVisible = isShowing
        }
    }

    private fun handleKebabMenuIconsVisibility(isShowing: Boolean) {
        this.menu?.apply {
            findItem(R.id.action_menu_sort_by)?.isVisible = isShowing
            findItem(R.id.action_menu_filter)?.isVisible = isShowing
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_zoom_in -> {
                handleZoomIn()
            }

            R.id.action_zoom_out -> {
                handleZoomOut()
            }

            R.id.action_menu_sort_by -> {
                mediaDiscoveryViewModel.showSortByDialog(showSortByDialog = true)
            }

            R.id.action_menu_filter -> {
                mediaDiscoveryViewModel.showFilterDialog(showFilterDialog = true)
            }
        }
        return super.onOptionsItemSelected(item)
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

    private fun handleSortByMenuItemEnableStatus() {
        mediaDiscoveryViewModel.state.value.apply {
            this@MediaDiscoveryFragment.menu?.findItem(R.id.action_menu_sort_by)?.let {
                val isSortByValid = uiPhotoList.isNotEmpty()
                it.isEnabled = isSortByValid
                val color = if (Util.isDarkMode(requireContext())) {
                    android.graphics.Color.argb(38, 255, 255, 255)
                } else {
                    android.graphics.Color.argb(38, 0, 0, 0)
                }
                it.isEnabled = isSortByValid
                val title = it.title.toString()
                val s = SpannableString(title)
                if (!isSortByValid) {
                    s.setSpan(
                        ForegroundColorSpan(color),
                        0,
                        s.length,
                        0
                    )
                    it.title = s
                } else {
                    it.title = s
                }
            }
        }
    }
}

