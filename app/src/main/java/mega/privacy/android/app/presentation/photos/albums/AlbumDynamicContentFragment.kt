package mega.privacy.android.app.presentation.photos.albums

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.photos.PhotosViewModel
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

    private val viewModel: AlbumsViewModel by activityViewModels()
    private val photosViewModel: PhotosViewModel by activityViewModels()

    @Inject
    lateinit var getThemeMode: GetThemeMode
    internal lateinit var managerActivity: ManagerActivity

    companion object {
        @JvmStatic
        fun getInstance(): AlbumDynamicContentFragment {
            return AlbumDynamicContentFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        managerActivity = activity as ManagerActivity
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
                    AlbumContentBody(viewModel)
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
                onClick = {
                    Toast.makeText(context, "onClick", Toast.LENGTH_SHORT).show()
                    if (viewModel.selectedPhotoIds.isEmpty()) {
                        openPhoto(it)
                    } else {
                        viewModel.onClick(it)
                    }
                },
                onLongPress = {
                    Toast.makeText(context, "onLongPress", Toast.LENGTH_SHORT).show()
                    viewModel.onLongPress(it)
                },
            )
        } else {
            EmptyView()
        }
    }

    private fun openPhoto(photo: Photo) {
        viewModel.state.value.currentAlbum?.let { album ->
            val albumPhotosHandles =
                viewModel.state.value.albums.getAlbumPhotos(album).map { photo ->
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

    override fun onDestroy() {
        viewModel.setCurrentAlbum(null)
        super.onDestroy()
    }

}
