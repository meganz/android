package mega.privacy.android.app.presentation.slideshow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.slideshow.view.SlideshowSettingsView
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

@AndroidEntryPoint
class SlideshowSettingsFragment : Fragment() {

    private lateinit var imageViewerActivity: ImageViewerActivity

    @Inject
    lateinit var getThemeMode: GetThemeMode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageViewerActivity = activity as ImageViewerActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val mode by getThemeMode().collectAsStateWithLifecycle(
                    initialValue = ThemeMode.System
                )
                AndroidTheme(isDark = mode.isDarkMode()) {
                    SlideshowSettingsView()
                }
            }
        }
    }
}