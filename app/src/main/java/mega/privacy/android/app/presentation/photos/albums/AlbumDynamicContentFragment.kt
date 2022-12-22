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
import androidx.compose.runtime.remember
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
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.photos.PhotosViewModel
import mega.privacy.android.app.presentation.photos.albums.actionMode.AlbumContentActionModeCallback
import mega.privacy.android.app.presentation.photos.albums.model.getAlbumPhotos
import mega.privacy.android.app.presentation.photos.albums.photosselection.AlbumPhotosSelectionActivity
import mega.privacy.android.app.presentation.photos.albums.view.DynamicView
import mega.privacy.android.app.presentation.photos.albums.view.EmptyView
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.view.showSortByDialog
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.presentation.theme.AndroidTheme
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

        val photos = remember(uiState.albums, uiState.currentSort) {
            uiState.currentAlbum?.let { album ->
                val sourcePhotos = uiState.albums.getAlbumPhotos(album)
                if (uiState.currentSort == Sort.NEWEST) {
                    sourcePhotos.sortedByDescending { it.modificationTime }
                } else {
                    sourcePhotos.sortedBy { it.modificationTime }
                }
            } ?: emptyList()
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
                    Album.FavouriteAlbum -> EmptyView(
                        imageResId = R.drawable.ic_photos_favourite_album,
                        textResId = R.string.empty_hint_favourite_album
                    )
                    Album.GifAlbum -> Back()
                    Album.RawAlbum -> Back()
                    is Album.UserAlbum -> EmptyView(
                        imageResId = R.drawable.ic_photos_user_album_empty,
                        textResId = R.string.photos_user_album_empty_album
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

            if (uiState.currentAlbum is Album.UserAlbum && isAccountHasPhotos) {
                AddFabButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                )
            }
        }
    }

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
                contentDescription = "",
                tint = if (!MaterialTheme.colors.isLight) {
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
            modifier = modifier.padding(8.dp)
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

    private fun openPhoto(photo: Photo) {
        albumsViewModel.state.value.currentAlbum?.let { album ->
            val albumPhotosHandles =
                albumsViewModel.state.value.albums.getAlbumPhotos(album).map { photo ->
                    photo.id
                }

            val intent = ImageViewerActivity.getIntentForChildren(
                requireContext(),
                albumPhotosHandles.toLongArray(),
                photo.id,
            )

            startActivity(intent)
            managerActivity.overridePendingTransition(0, 0)
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
        managerActivity.showHideBottomNavigationView(true)
    }

    private fun exitActionMode() {
        actionMode?.finish()
        actionMode = null
        managerActivity.showHideBottomNavigationView(false)
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
            menu.findItem(R.id.action_menu_filter)?.isVisible = photos.isNotEmpty()
            if (album is Album.UserAlbum) {
                menu.findItem(R.id.action_menu_rename)?.isVisible = true
                menu.findItem(R.id.action_menu_delete)?.isVisible = true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_menu_sort_by -> {
                showSortByDialog(
                    context = managerActivity,
                    checkedItem = albumsViewModel.state.value.currentSort.ordinal,
                    onClickListener = { _, i ->
                        albumsViewModel.setCurrentSort(Sort.values()[i])
                    },
                )
            }
            R.id.action_menu_filter -> {
                //TODO
                Toast.makeText(activity, "Filter is developing...", Toast.LENGTH_SHORT).show()
            }
            R.id.action_menu_delete -> {
                //TODO
                Toast.makeText(activity, "Delete is developing...", Toast.LENGTH_SHORT).show()
            }
            R.id.action_menu_rename -> {
                //TODO
                Toast.makeText(activity, "Rename is developing...", Toast.LENGTH_SHORT).show()
            }
        }
        return super.onOptionsItemSelected(item)
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
