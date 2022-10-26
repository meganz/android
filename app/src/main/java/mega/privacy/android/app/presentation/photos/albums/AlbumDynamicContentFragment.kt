package mega.privacy.android.app.presentation.photos.albums

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.photos.PhotosViewModel
import mega.privacy.android.app.presentation.photos.albums.actionMode.AlbumContentActionModeCallback
import mega.privacy.android.app.presentation.photos.albums.model.getAlbumPhotos
import mega.privacy.android.app.presentation.photos.albums.view.DynamicView
import mega.privacy.android.app.presentation.photos.albums.view.EmptyView
import mega.privacy.android.domain.entity.ThemeMode
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

    // Action mode
    private var actionMode: ActionMode? = null
    private lateinit var actionModeCallback: AlbumContentActionModeCallback

    companion object {
        @JvmStatic
        fun getInstance(): AlbumDynamicContentFragment {
            return AlbumDynamicContentFragment()
        }
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
        setupFlow()
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

        val photos = remember(uiState.albums) {
            uiState.albums.getAlbumPhotos(uiState.currentAlbum!!)
        }

        if (photos.isNotEmpty()) {
            DynamicView(
                photos = photos,
                smallWidth = smallWidth,
                photoDownload = photosViewModel::downloadPhoto,
                onClick = this::onClick,
                onLongPress = this::onLongPress,
                selectedPhotoIds = uiState.selectedPhotoIds
            )
        } else {
            EmptyView()
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

    fun onClick(photo: Photo) {
        if (albumsViewModel.selectedPhotoIds.isEmpty()) {
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
        if (albumsViewModel.selectedPhotoIds.isEmpty()) {
            if (actionMode == null) {
                enterActionMode()
            }
            albumsViewModel.togglePhotoSelection(photo.id)
        } else {
            onClick(photo)
        }
    }

    override fun onDestroy() {
        albumsViewModel.setCurrentAlbum(null)
        super.onDestroy()
    }

}
