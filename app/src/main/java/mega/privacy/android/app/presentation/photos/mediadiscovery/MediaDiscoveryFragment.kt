package mega.privacy.android.app.presentation.photos.mediadiscovery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.photos.PhotosViewModel
import mega.privacy.android.app.presentation.photos.mediadiscovery.actionMode.MediaDiscoveryActionModeCallback
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.view.PhotosGridView
import mega.privacy.android.app.presentation.photos.view.showSortByDialog
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.presentation.theme.AndroidTheme
import javax.inject.Inject

/**
 * New Album Content View
 */
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
            val fragment = MediaDiscoveryFragment()
            val args = Bundle()
            args.putLong(INTENT_KEY_CURRENT_FOLDER_ID, mediaHandle)
            fragment.arguments = args

            return fragment
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
        managerActivity.setToolbarTitle()
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
                    menu?.let { menu ->
                        //TODO
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

        PhotosGridView(
            currentZoomLevel = uiState.currentZoomLevel,
            downloadPhoto = photosViewModel::downloadPhoto,
            onClick = this::onClick,
            onLongPress = this::onLongPress,
            selectedPhotoIds = uiState.selectedPhotoIds,
            uiPhotoList = uiState.uiPhotoList,
        )
    }

    private fun openPhoto(photo: Photo) {
        //TODO
    }

    private fun onClick(photo: Photo) {
        if (mediaDiscoveryViewModel.selectedPhotoIds.isEmpty()) {
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
        if (mediaDiscoveryViewModel.selectedPhotoIds.isEmpty()) {
            if (actionMode == null) {
                enterActionMode()
            }
            mediaDiscoveryViewModel.togglePhotoSelection(photo.id)
        } else {
            onClick(photo)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_album_content_toolbar, menu)
        super.onCreateOptionsMenu(menu, inflater)
        this.menu = menu
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
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
}

