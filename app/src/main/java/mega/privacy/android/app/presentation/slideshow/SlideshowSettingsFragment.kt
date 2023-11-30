package mega.privacy.android.app.presentation.slideshow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.slideshow.view.SlideshowSettingsView
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

@AndroidEntryPoint
class SlideshowSettingsFragment : Fragment() {

    private val slideshowSettingViewModel: SlideshowSettingViewModel by viewModels()

    @Inject
    lateinit var getThemeMode: GetThemeMode

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val mode by getThemeMode().collectAsStateWithLifecycle(
                    initialValue = ThemeMode.System
                )
                MegaAppTheme(isDark = mode.isDarkMode()) {
                    SlideshowSettingsView(
                        slideshowSettingViewModel = slideshowSettingViewModel,
                    )
                }
            }
        }
    }
}