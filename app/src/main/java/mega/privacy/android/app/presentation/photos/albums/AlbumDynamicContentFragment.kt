package mega.privacy.android.app.presentation.photos.albums

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.getQuantityStringOrDefault
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.photos.PhotosViewModel
import mega.privacy.android.app.presentation.photos.albums.actionMode.AlbumContentActionModeCallback
import mega.privacy.android.app.presentation.photos.albums.model.AlbumsViewState
import mega.privacy.android.app.presentation.photos.albums.model.getAlbumPhotos
import mega.privacy.android.app.presentation.photos.albums.photosselection.AlbumPhotosSelectionActivity
import mega.privacy.android.app.presentation.photos.albums.view.DeleteAlbumsConfirmationDialog
import mega.privacy.android.app.presentation.photos.albums.view.DynamicView
import mega.privacy.android.app.presentation.photos.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.view.FilterDialog
import mega.privacy.android.app.presentation.photos.view.SortByDialog
import mega.privacy.android.app.utils.StringUtils.formatColorTag
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.presentation.controls.MegaEmptyView
import mega.privacy.android.presentation.theme.AndroidTheme
import mega.privacy.android.presentation.theme.black
import mega.privacy.android.presentation.theme.dark_grey
import mega.privacy.android.presentation.theme.white
import javax.inject.Inject

/**
 * New Album Content View
 */
@AndroidEntryPoint
class AlbumDynamicContentFragment : Fragment() {

    internal val albumsViewModel: AlbumsViewModel by activityViewModels()
    private val photosViewModel: PhotosViewModel by activityViewModels()

    @Inject
    lateinit var getThemeMode: GetThemeMode
    internal lateinit var managerActivity: ManagerActivity
    private var menu: Menu? = null

    // Action mode
    private var actionMode: ActionMode? = null
    private lateinit var actionModeCallback: AlbumContentActionModeCallback

    private var closeScreen by mutableStateOf(false)

    companion object {
        @JvmStatic
        fun getInstance(isAccountHasPhotos: Boolean): AlbumDynamicContentFragment {
            return AlbumDynamicContentFragment().apply {
                arguments =
                    bundleOf(INTENT_KEY_IS_ACCOUNT_HAS_PHOTOS to isAccountHasPhotos)
            }
        }

        internal const val INTENT_KEY_IS_ACCOUNT_HAS_PHOTOS = "IS_ACCOUNT_HAS_PHOTOS"
    }

    private val isAccountHasPhotos: Boolean by lazy(LazyThreadSafetyMode.NONE) {
        arguments?.getBoolean(INTENT_KEY_IS_ACCOUNT_HAS_PHOTOS,
            false) ?: false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        managerActivity = activity as ManagerActivity
        actionModeCallback =
            AlbumContentActionModeCallback(this, albumsViewModel.state.value.currentAlbum)
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
                    AlbumContentBody(albumsViewModel)
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
                        state.currentAlbum?.let { album ->
                            val photos = state.albums.getAlbumPhotos(album)
                            menu.findItem(R.id.action_menu_sort_by)?.isVisible =
                                photos.isNotEmpty()
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AlbumContentBody(
        viewModel: AlbumsViewModel = viewModel(),
    ) {
        val uiState by viewModel.state.collectAsStateWithLifecycle()
        val configuration = LocalConfiguration.current
        val smallWidth = remember(configuration) {
            (configuration.screenWidthDp.dp - 1.dp) / 3
        }

        val photos = remember(uiState.albums, uiState.currentSort, uiState.currentMediaType) {
            uiState.currentAlbum?.let { album ->
                val sourcePhotos = uiState.albums.getAlbumPhotos(album)
                sourcePhotos
                    .applyFilter(currentMediaType = uiState.currentMediaType)
                    .takeIf {
                        sourcePhotos.setFilterMenuItemVisibility()
                    }?.applySortBy(currentSort = uiState.currentSort)
                    ?: sourcePhotos.applySortBy(currentSort = uiState.currentSort)
            } ?: emptyList()
        }

        if (closeScreen) Back()

        if (uiState.showDeleteAlbumsConfirmation) {
            val album = uiState.currentAlbum as? Album.UserAlbum
            DeleteAlbumsConfirmationDialog(
                selectedAlbumIds = listOfNotNull(album?.id),
                onCancelClicked = albumsViewModel::closeDeleteAlbumsConfirmation,
                onDeleteClicked = { deleteAlbum() },
            )
        }

        Box {
            if (photos.isNotEmpty()) {
                DynamicView(
                    photos = photos,
                    smallWidth = smallWidth,
                    photoDownload = photosViewModel::downloadPhoto,
                    onClick = this@AlbumDynamicContentFragment::onClick,
                    onLongPress = this@AlbumDynamicContentFragment::onLongPress,
                    selectedPhotoIds = uiState.selectedPhotoIds
                )
            } else {
                when (uiState.currentAlbum) {
                    Album.FavouriteAlbum -> MegaEmptyView(
                        imageResId = R.drawable.ic_photos_favourite_album,
                        isVectorImage = true,
                        isBimapImage = false,
                        text = getString(R.string.empty_hint_favourite_album)
                            .formatColorTag(requireContext(), 'A', R.color.grey_900_grey_100)
                            .formatColorTag(requireContext(), 'B', R.color.grey_300_grey_600)
                            .toSpannedHtmlText()
                    )
                    Album.GifAlbum -> Back()
                    Album.RawAlbum -> Back()
                    is Album.UserAlbum -> MegaEmptyView(
                        imageResId = R.drawable.ic_photos_user_album_empty,
                        isVectorImage = true,
                        isBimapImage = false,
                        text = getString(R.string.photos_user_album_empty_album)
                            .formatColorTag(requireContext(), 'A', R.color.grey_900_grey_100)
                            .formatColorTag(requireContext(), 'B', R.color.grey_300_grey_600)
                            .toSpannedHtmlText()
                    )
                    null -> Back()
                }
            }

            if (uiState.snackBarMessage.isNotEmpty()) {
                SnackBar(
                    message = uiState.snackBarMessage,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp, 80.dp)
                )
            }

            if (showFilterFabButton(uiState)) {
                FilterFabButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 88.dp)
                )
            }

            if (showAddFabButton(uiState)) {
                AddFabButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                )
            }

            if (uiState.showSortByDialog) {
                SortByDialog(
                    onDialogDismissed = {
                        albumsViewModel.showSortByDialog(showSortByDialog = false)
                    },
                    selectedOption = uiState.currentSort,
                    onOptionSelected = {
                        albumsViewModel.setCurrentSort(it)
                    }
                )
            }

            if (uiState.showFilterDialog) {
                FilterDialog(
                    onDialogDismissed = {
                        albumsViewModel.showFilterDialog(showFilterDialog = false)
                    },
                    selectedOption = uiState.currentMediaType,
                    onOptionSelected = {
                        albumsViewModel.setCurrentMediaType(it)
                    }
                )
            }
        }
    }

    @Composable
    private fun showFilterFabButton(uiState: AlbumsViewState) =
        (uiState.currentMediaType != FilterMediaType.ALL_MEDIA
                && uiState.selectedPhotoIds.isEmpty())

    @Composable
    private fun showAddFabButton(uiState: AlbumsViewState) =
        uiState.currentAlbum is Album.UserAlbum
                && isAccountHasPhotos
                && uiState.selectedPhotoIds.isEmpty()

    @Composable
    private fun AddFabButton(
        modifier: Modifier,
    ) {
        FloatingActionButton(
            onClick = this::onFabClick,
            modifier = modifier
                .size(56.dp)
        ) {
            Icon(
                painter = painterResource(id = if (MaterialTheme.colors.isLight) {
                    R.drawable.ic_add_white
                } else {
                    R.drawable.ic_add
                }),
                contentDescription = "Add",
                tint = if (!MaterialTheme.colors.isLight) {
                    Color.Black
                } else {
                    Color.White
                }
            )
        }
    }

    @Composable
    private fun FilterFabButton(
        modifier: Modifier,
    ) {
        FloatingActionButton(
            onClick = { albumsViewModel.showFilterDialog(true) },
            modifier = modifier
                .size(40.dp),
            backgroundColor = if (MaterialTheme.colors.isLight) {
                Color.White
            } else {
                dark_grey
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_filter_light),
                contentDescription = "Filter",
                tint = if (MaterialTheme.colors.isLight) {
                    Color.Black
                } else {
                    Color.White
                }
            )
        }
    }

    @Composable
    private fun SnackBar(
        message: String,
        modifier: Modifier,
    ) {
        Snackbar(
            modifier = modifier.padding(8.dp),
            backgroundColor = black.takeIf { MaterialTheme.colors.isLight } ?: white,
        ) {
            Text(
                text = message
            )
        }

        LaunchedEffect(true) {
            delay(3000L)
            albumsViewModel.setSnackBarMessage("")
        }
    }

    @Composable
    private fun Back() {
        val onBackPressedDispatcher =
            LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
        LaunchedEffect(key1 = true) {
            onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun List<Photo>.applySortBy(currentSort: Sort) =
        if (currentSort == Sort.NEWEST) {
            this.sortedByDescending { it.modificationTime }
        } else {
            this.sortedBy { it.modificationTime }
        }

    private fun List<Photo>.applyFilter(currentMediaType: FilterMediaType) =
        when (currentMediaType) {
            FilterMediaType.ALL_MEDIA -> this
            FilterMediaType.IMAGES -> this.filterIsInstance<Photo.Image>()
            FilterMediaType.VIDEOS -> this.filterIsInstance<Photo.Video>()
        }

    private fun openPhoto(photo: Photo) {
        albumsViewModel.state.value.currentAlbum?.let { album ->
            albumsViewModel.state.value.apply {
                val sourcePhotos = albums.getAlbumPhotos(album)
                val currentAlbumPhotos = sourcePhotos
                    .applyFilter(currentMediaType = currentMediaType)
                    .takeIf {
                        currentMediaType != FilterMediaType.ALL_MEDIA
                    }?.applySortBy(currentSort = currentSort)
                    ?: sourcePhotos.applySortBy(currentSort = currentSort)

                val intent = ImageViewerActivity.getIntentForChildren(
                    requireContext(),
                    currentAlbumPhotos.map { it.id }.toLongArray(),
                    photo.id,
                )

                startActivity(intent)
                managerActivity.overridePendingTransition(0, 0)
            }
        }
    }

    private fun onFabClick() {
        if (albumsViewModel.state.value.currentAlbum is Album.UserAlbum) {
            val userAlbum = albumsViewModel.state.value.currentAlbum as Album.UserAlbum
            openAlbumPhotosSelection(albumId = userAlbum.id)
        }
    }

    private val albumPhotosSelectionLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleAlbumPhotosSelectionResult,
        )

    private fun handleAlbumPhotosSelectionResult(result: ActivityResult) {
        val message =
            result.data?.getStringExtra(AlbumPhotosSelectionActivity.MESSAGE) // Added 5 items to "Color ️‍🌈"
        albumsViewModel.setSnackBarMessage(snackBarMessage = message ?: "")
    }

    private fun openAlbumPhotosSelection(albumId: AlbumId) {
        val intent = AlbumPhotosSelectionActivity.create(requireContext(), albumId)
        albumPhotosSelectionLauncher.launch(intent)
        managerActivity.overridePendingTransition(0, 0)

    }

    fun onClick(photo: Photo) {
        if (albumsViewModel.state.value.selectedPhotoIds.isEmpty()) {
            openPhoto(photo)
        } else {
            if (actionMode != null) {
                albumsViewModel.togglePhotoSelection(photo.id)
            }
        }
    }

    fun onLongPress(photo: Photo) {
        handleActionMode(photo)
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

    private fun handleActionMode(photo: Photo) {
        if (albumsViewModel.state.value.selectedPhotoIds.isEmpty()) {
            if (actionMode == null) {
                enterActionMode()
            }
            albumsViewModel.togglePhotoSelection(photo.id)
        } else {
            onClick(photo)
        }
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
            }
        }
    }

    private fun List<Photo>.setFilterMenuItemVisibility(): Boolean {
        val imageCount = this.count { it is Photo.Image }
        val showFilterMenuItem = this.isNotEmpty() &&
                imageCount != this.size && imageCount != 0
        if (!showFilterMenuItem) {
            albumsViewModel.setCurrentMediaType(FilterMediaType.DEFAULT)
        }
        menu?.findItem(R.id.action_menu_filter)?.isVisible = showFilterMenuItem
        return showFilterMenuItem
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
                //TODO
                Toast.makeText(activity, "Rename is developing...", Toast.LENGTH_SHORT).show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleAlbumDeletion() {
        val photos = albumsViewModel.getCurrentUIAlbum()?.photos.orEmpty()
        if (photos.isEmpty()) {
            deleteAlbum()
        } else {
            albumsViewModel.showDeleteAlbumsConfirmation()
        }
    }

    private fun deleteAlbum() {
        val album = albumsViewModel.state.value.currentAlbum as? Album.UserAlbum
        albumsViewModel.deleteAlbums(albumIds = listOfNotNull(album?.id))
        albumsViewModel.updateAlbumDeletedMessage(
            message = context?.getQuantityStringOrDefault(
                R.plurals.photos_album_deleted_message,
                quantity = 1,
                album?.title,
            ).orEmpty()
        )

        closeScreen = true
    }

    /**
     * Get current page title
     */
    fun getCurrentAlbumTitle(): String {
        val currentUIAlbum = albumsViewModel.getCurrentUIAlbum()
        return if (context != null && currentUIAlbum != null) {
            currentUIAlbum.title
        } else {
            getString(R.string.tab_title_album)
        }
    }
}
