package mega.privacy.android.app.presentation.slideshow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.imageviewer.ImageViewerViewModel
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.photos.PhotosViewModel
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

/**
 * Image Viewer fragment that contains a slideshow to show each Image
 */
@AndroidEntryPoint
class SlideshowFragment : Fragment() {
    private val slideshowViewModel: SlideshowViewModel by viewModels()
    private val imageViewerViewModel: ImageViewerViewModel by activityViewModels()
    private val photosViewModel: PhotosViewModel by viewModels()

    @Inject
    lateinit var getThemeMode: GetThemeMode

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
                    SlideshowBody()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObserver()
    }

    private fun setupObserver() {
        imageViewerViewModel.images.observe(viewLifecycleOwner) { items ->
            items?.let {
                slideshowViewModel.setData(it)
            }
        }
    }

    @Composable
    private fun SlideshowBody() {
        //TODO
    }
}

