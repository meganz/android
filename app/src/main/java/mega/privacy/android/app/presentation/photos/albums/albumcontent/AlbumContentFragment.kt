package mega.privacy.android.app.presentation.photos.albums.albumcontent

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
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity
import mega.privacy.android.app.presentation.imagepreview.fetcher.AlbumContentImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.photos.PhotoDownloaderViewModel
import mega.privacy.android.app.presentation.photos.albums.AlbumScreenWrapperActivity
import mega.privacy.android.app.presentation.photos.albums.AlbumsViewModel
import mega.privacy.android.app.presentation.photos.albums.model.getAlbumType
import mega.privacy.android.app.presentation.photos.albums.photosselection.AlbumFlow
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.TimelineViewModel
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Album.FavouriteAlbum
import mega.privacy.android.domain.entity.photos.Album.GifAlbum
import mega.privacy.android.domain.entity.photos.Album.RawAlbum
import mega.privacy.android.domain.entity.photos.Album.UserAlbum
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.mobile.analytics.event.AlbumContentDeleteAlbumEvent
import mega.privacy.mobile.analytics.event.AlbumContentScreenEvent
import mega.privacy.mobile.analytics.event.AlbumContentShareLinkMenuToolbarEvent
import javax.inject.Inject

/**
 * New Album Content View
 */
@AndroidEntryPoint
class AlbumContentFragment : Fragment() {
    private val timelineViewModel: TimelineViewModel by activityViewModels()
    private val albumsViewModel: AlbumsViewModel by activityViewModels()

    private val photoDownloaderViewModel: PhotoDownloaderViewModel by viewModels()
    internal val albumContentViewModel: AlbumContentViewModel by viewModels()

    internal lateinit var managerActivity: ManagerActivity

    // Action mode
    private var menu: Menu? = null
    private var actionMode: ActionMode? = null
    private lateinit var actionModeCallback: AlbumContentActionModeCallback

    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    @Inject
    @DefaultDispatcher
    lateinit var defaultDispatcher: CoroutineDispatcher

    private var isContextMenuInitialized: Boolean = false

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
                MegaAppTheme(isDark = mode.isDarkMode()) {
                    AlbumContentScreen(
                        photoDownloaderViewModel = photoDownloaderViewModel,
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

    override fun onResume() {
        super.onResume()
        albumContentViewModel.revalidateAlbumNameInput(
            albumNames = albumsViewModel.getAllUserAlbumsNames(),
        )
        Analytics.tracker.trackEvent(AlbumContentScreenEvent)
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
                albumContentViewModel.state.collect { state ->
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
                        menu.findItem(R.id.action_menu_sort_by)?.isVisible =
                            state.photos.isNotEmpty()
                    }

                    if (!state.showRenameDialog) {
                        managerActivity.setToolbarTitle(getCurrentAlbumTitle())
                    }

                    if (state.uiAlbum != null && !isContextMenuInitialized) {
                        managerActivity.invalidateOptionsMenu()
                        isContextMenuInitialized = true
                    }
                }
            }
        }
    }

    private fun openPhotoPreview(anchorPhoto: Photo, photos: List<Photo>) {
        albumsViewModel.state.value.currentAlbum?.let { currentAlbum ->
            lifecycleScope.launch {
                if (getFeatureFlagValueUseCase(AppFeatures.ImagePreview)) {
                    val params = buildMap {
                        this[AlbumContentImageNodeFetcher.ALBUM_TYPE] = currentAlbum.getAlbumType()

                        if (currentAlbum is UserAlbum) {
                            this[AlbumContentImageNodeFetcher.CUSTOM_ALBUM_ID] = currentAlbum.id.id
                        }
                    }
                    val intent = ImagePreviewActivity.createIntent(
                        context = requireContext(),
                        imageSource = ImagePreviewFetcherSource.ALBUM_CONTENT,
                        menuOptionsSource = ImagePreviewMenuSource.ALBUM_CONTENT,
                        anchorImageNodeId = NodeId(anchorPhoto.id),
                        params = params,
                    )
                    startActivity(intent)
                } else {
                    val intent = ImageViewerActivity.getIntentForChildren(
                        requireContext(),
                        photos.map { it.id }.toLongArray(),
                        anchorPhoto.id,
                    )
                    startActivity(intent)
                    managerActivity.overridePendingTransition(0, 0)
                }
            }
        }
    }

    private fun openAlbumPhotosSelection(album: UserAlbum) {
        val intent = AlbumScreenWrapperActivity.createAlbumPhotosSelectionScreen(
            context = requireContext(),
            albumId = album.id,
            albumFlow = AlbumFlow.Addition,
        )
        albumPhotosSelectionLauncher.launch(intent)
    }

    private fun handleAlbumPhotosSelectionResult(result: ActivityResult) {}

    private fun openAlbumCoverSelectionScreen() {
        val album = albumContentViewModel.state.value.uiAlbum?.id
        if (album !is UserAlbum) return

        val intent = AlbumScreenWrapperActivity.createAlbumCoverSelectionScreen(
            context = requireContext(),
            albumId = album.id,
        )
        albumCoverSelectionLauncher.launch(intent)
    }

    private fun handleAlbumCoverSelectionResult(result: ActivityResult) {
        val message = result.data?.getStringExtra(AlbumScreenWrapperActivity.MESSAGE)
        albumContentViewModel.setSnackBarMessage(message.orEmpty())
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
        val album = albumContentViewModel.state.value.uiAlbum?.id ?: return
        val photos = albumContentViewModel.state.value.photos

        viewLifecycleOwner.lifecycleScope.launch {
            menu.findItem(R.id.action_menu_sort_by)?.isVisible = photos.isNotEmpty()
            menu.findItem(R.id.action_menu_filter)?.isVisible = withContext(defaultDispatcher) {
                val imageCount = photos.count { it is Photo.Image }
                val videoCount = photos.size - imageCount

                imageCount > 0 && videoCount > 0
            }

            if (album !is UserAlbum) return@launch
            val isAlbumSharingEnabled = getFeatureFlagValueUseCase(AppFeatures.AlbumSharing)

            menu.findItem(R.id.action_menu_get_link)?.let { menu ->
                menu.title =
                    context?.resources?.getQuantityString(R.plurals.album_share_get_links, 1)
                menu.isVisible = isAlbumSharingEnabled && album.isExported == false
            }
            menu.findItem(R.id.action_menu_manage_link)?.let { menu ->
                menu.isVisible = isAlbumSharingEnabled && album.isExported == true
            }
            menu.findItem(R.id.action_menu_remove_link)?.let { menu ->
                menu.isVisible = isAlbumSharingEnabled && album.isExported == true
            }

            menu.findItem(R.id.action_menu_rename)?.isVisible = true
            menu.findItem(R.id.action_menu_delete)?.isVisible = true

            photos.setSelectAlbumCoverMenuItemVisibility()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_menu_get_link -> {
                Analytics.tracker.trackEvent(AlbumContentShareLinkMenuToolbarEvent)
                openAlbumGetLinkScreen(isNewLink = true)
            }

            R.id.action_menu_manage_link -> {
                Analytics.tracker.trackEvent(AlbumContentShareLinkMenuToolbarEvent)
                openAlbumGetLinkScreen(isNewLink = false)
            }

            R.id.action_menu_remove_link -> {
                albumContentViewModel.showRemoveLinkConfirmation()
            }

            R.id.action_menu_sort_by -> {
                albumContentViewModel.showSortByDialog(showSortByDialog = true)
            }

            R.id.action_menu_filter -> {
                albumContentViewModel.showFilterDialog(showFilterDialog = true)
            }

            R.id.action_menu_delete -> {
                Analytics.tracker.trackEvent(AlbumContentDeleteAlbumEvent)
                handleAlbumDeletion()
            }

            R.id.action_menu_rename -> {
                albumContentViewModel.showRenameDialog(showRenameDialog = true)
            }

            R.id.action_menu_select_album_cover -> {
                openAlbumCoverSelectionScreen()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun List<Photo>.setSelectAlbumCoverMenuItemVisibility() {
        menu?.findItem(R.id.action_menu_select_album_cover)?.isVisible = this.isNotEmpty()
    }

    private fun handleAlbumDeletion() {
        val photos = albumContentViewModel.state.value.photos
        if (photos.isEmpty()) {
            albumContentViewModel.deleteAlbum()
        } else {
            albumContentViewModel.showDeleteAlbumsConfirmation()
        }
    }

    /**
     * Get current page title
     */
    fun getCurrentAlbumTitle(): String {
        val currentUIAlbum = albumContentViewModel.state.value.uiAlbum
        return if (context != null && currentUIAlbum != null) {
            currentUIAlbum.title.getTitleString(requireContext())
        } else {
            getString(R.string.tab_title_album)
        }
    }

    override fun onPause() {
        ackPhotosAddingProgressCompleted()
        ackPhotosRemovingProgressCompleted()
        albumContentViewModel.setSnackBarMessage("")
        super.onPause()
    }

    private fun ackPhotosAddingProgressCompleted() {
        val album = albumContentViewModel.state.value.uiAlbum?.id as? UserAlbum ?: return
        val isProgressCompleted = albumContentViewModel.state.value.isAddingPhotosProgressCompleted

        if (!isProgressCompleted) return
        albumContentViewModel.updatePhotosAddingProgressCompleted(albumId = album.id)
    }

    private fun ackPhotosRemovingProgressCompleted() {
        val album = albumContentViewModel.state.value.uiAlbum?.id as? UserAlbum ?: return
        val isProgressCompleted =
            albumContentViewModel.state.value.isRemovingPhotosProgressCompleted

        if (!isProgressCompleted) return
        albumContentViewModel.updatePhotosRemovingProgressCompleted(albumId = album.id)
    }

    private fun openAlbumGetLinkScreen(isNewLink: Boolean) {
        val album = albumContentViewModel.state.value.uiAlbum?.id as? UserAlbum ?: return
        val intent = AlbumScreenWrapperActivity.createAlbumGetLinkScreen(
            context = requireContext(),
            albumId = album.id,
            isNewLink = isNewLink,
        )
        startActivity(intent)
        activity?.overridePendingTransition(0, 0)
    }

    companion object {
        @JvmStatic
        fun getInstance(album: Album): AlbumContentFragment {
            return AlbumContentFragment().apply {
                arguments = bundleOf(
                    "type" to when (album) {
                        FavouriteAlbum -> "favourite"
                        GifAlbum -> "gif"
                        RawAlbum -> "raw"
                        else -> "custom"
                    },
                    "id" to if (album is UserAlbum) album.id.id else null,
                )
            }
        }
    }
}
