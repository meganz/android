package mega.privacy.android.app.presentation.photos.albums

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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.photos.PhotosViewModel
import mega.privacy.android.app.presentation.photos.albums.actionMode.AlbumContentActionModeCallback
import mega.privacy.android.app.presentation.photos.albums.model.getAlbumPhotos
import mega.privacy.android.app.presentation.photos.albums.photosselection.AlbumFlow
import mega.privacy.android.app.presentation.photos.compose.albumcontent.AlbumContentScreen
import mega.privacy.android.app.presentation.photos.compose.albumcontent.isFilterable
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.TimelineViewModel
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

/**
 * New Album Content View
 */
@AndroidEntryPoint
class AlbumDynamicContentFragment : Fragment() {
    internal val timelineViewModel: TimelineViewModel by activityViewModels()
    internal val albumsViewModel: AlbumsViewModel by activityViewModels()

    private val photosViewModel: PhotosViewModel by activityViewModels()
    private val albumContentViewModel: AlbumContentViewModel by viewModels()

    @Inject
    lateinit var getThemeMode: GetThemeMode
    internal lateinit var managerActivity: ManagerActivity
    private var menu: Menu? = null

    // Action mode
    private var actionMode: ActionMode? = null
    private lateinit var actionModeCallback: AlbumContentActionModeCallback

    companion object {
        @JvmStatic
        fun getInstance(isAccountHasPhotos: Boolean): AlbumDynamicContentFragment {
            return AlbumDynamicContentFragment()
        }
    }

    private val albumPhotosSelectionLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleAlbumPhotosSelectionResult,
        )

    private val albumCoverSelectionLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleAlbumCoverSelectionResult,
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        managerActivity = activity as ManagerActivity
        actionModeCallback = AlbumContentActionModeCallback(this)
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
                    AlbumContentScreen(
                        photosViewModel = photosViewModel,
                        timelineViewModel = timelineViewModel,
                        albumsViewModel = albumsViewModel,
                        albumContentViewModel = albumContentViewModel,
                        onNavigatePhotoPreview = ::openPhotoPreview,
                        onNavigatePhotosSelection = ::openAlbumPhotosSelection,
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
        managerActivity.setToolbarTitle()
        managerActivity.invalidateOptionsMenu()
        managerActivity.hideFabButton()
    }

    private fun setupFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                albumsViewModel.state.collect { state ->
                    if (state.selectedPhotos.isEmpty()) {
                        if (actionMode != null) {
                            exitActionMode()
                        }
                    } else {
                        if (actionMode == null) {
                            enterActionMode()
                        }
                        actionMode?.title = state.selectedPhotos.size.toString()
                    }
                    menu?.let { menu ->
                        state.currentAlbum?.let { album ->
                            val photos = state.albums.getAlbumPhotos(album)
                            menu.findItem(R.id.action_menu_sort_by)?.isVisible =
                                photos.isNotEmpty()
                        }
                    }
                    if (!state.showRenameDialog){
                        managerActivity.setToolbarTitle(getCurrentAlbumTitle())
                    }
                }
            }
        }
    }

    private fun openPhotoPreview(anchorPhoto: Photo, photos: List<Photo>) {
        val intent = ImageViewerActivity.getIntentForChildren(
            requireContext(),
            photos.map { it.id }.toLongArray(),
            anchorPhoto.id,
        )
        startActivity(intent)
        managerActivity.overridePendingTransition(0, 0)
    }

    private fun openAlbumPhotosSelection(album: Album.UserAlbum) {
        val intent = AlbumScreenWrapperActivity.createAlbumPhotosSelectionScreen(
            context = requireContext(),
            albumId = album.id,
            albumFlow = AlbumFlow.Addition,
        )
        albumPhotosSelectionLauncher.launch(intent)
        managerActivity.overridePendingTransition(0, 0)
    }

    private fun handleAlbumPhotosSelectionResult(result: ActivityResult) {}

    private fun openAlbumCoverSelectionScreen() {
        val album = albumsViewModel.state.value.currentAlbum
        if (album !is Album.UserAlbum) return

        val intent = AlbumScreenWrapperActivity.createAlbumCoverSelectionScreen(
            context = requireContext(),
            albumId = album.id,
        )
        albumCoverSelectionLauncher.launch(intent)
        managerActivity.overridePendingTransition(0, 0)
    }

    private fun handleAlbumCoverSelectionResult(result: ActivityResult) {
        val message = result.data?.getStringExtra(AlbumScreenWrapperActivity.MESSAGE)
        albumsViewModel.setSnackBarMessage(message.orEmpty())
    }

    private fun enterActionMode() {
        actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(
            actionModeCallback
        )
    }

    private fun exitActionMode() {
        actionMode?.finish()
        actionMode = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_album_content_toolbar, menu)
        super.onCreateOptionsMenu(menu, inflater)
        this.menu = menu
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        albumsViewModel.state.value.currentAlbum?.let { album ->
            val photos = albumsViewModel.state.value.albums.getAlbumPhotos(album)
            menu.findItem(R.id.action_menu_sort_by)?.isVisible = photos.isNotEmpty()
            photos.setFilterMenuItemVisibility()
            if (album is Album.UserAlbum) {
                menu.findItem(R.id.action_menu_rename)?.isVisible = true
                menu.findItem(R.id.action_menu_delete)?.isVisible = true

                photos.setSelectAlbumCoverMenuItemVisibility()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_menu_sort_by -> {
                albumsViewModel.showSortByDialog(showSortByDialog = true)
            }
            R.id.action_menu_filter -> {
                albumsViewModel.showFilterDialog(showFilterDialog = true)
            }
            R.id.action_menu_delete -> {
                handleAlbumDeletion()
            }
            R.id.action_menu_rename -> {
                albumsViewModel.showRenameDialog(showRenameDialog = true)
            }
            R.id.action_menu_select_album_cover -> {
                openAlbumCoverSelectionScreen()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun List<Photo>.setFilterMenuItemVisibility() {
        menu?.findItem(R.id.action_menu_filter)?.isVisible = isFilterable()
    }

    private fun List<Photo>.setSelectAlbumCoverMenuItemVisibility() {
        menu?.findItem(R.id.action_menu_select_album_cover)?.isVisible = this.isNotEmpty()
    }

    private fun handleAlbumDeletion() {
        val photos = albumsViewModel.state.value.currentUIAlbum?.photos.orEmpty()
        if (photos.isEmpty()) {
            albumContentViewModel.deleteAlbum()
        } else {
            albumsViewModel.showDeleteAlbumsConfirmation()
        }
    }

    /**
     * Get current page title
     */
    fun getCurrentAlbumTitle(): String {
        val currentUIAlbum = albumsViewModel.state.value.currentUIAlbum
        return if (context != null && currentUIAlbum != null) {
            currentUIAlbum.title
        } else {
            getString(R.string.tab_title_album)
        }
    }

    val currentAlbum: Album? get() = albumsViewModel.state.value.currentAlbum

    override fun onPause() {
        ackPhotosAddingProgressCompleted()
        super.onPause()
    }

    private fun ackPhotosAddingProgressCompleted() {
        val album = albumsViewModel.state.value.currentUserAlbum ?: return
        val isProgressCompleted = albumContentViewModel.state.value.isAddingPhotosProgressCompleted

        if (!isProgressCompleted) return
        albumContentViewModel.updatePhotosAddingProgressCompleted(albumId = album.id)
    }
}
