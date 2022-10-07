package mega.privacy.android.app.presentation.photos.albums

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.presentation.theme.AndroidTheme
import javax.inject.Inject

@AndroidEntryPoint
class AlbumDynamicContentFragment : Fragment() {

    private val viewModel: AlbumsViewModel by activityViewModels()

    @Inject
    lateinit var getThemeMode: GetThemeMode

    companion object {
        @JvmStatic
        fun getInstance(): AlbumDynamicContentFragment {
            return AlbumDynamicContentFragment()
        }
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
                    AlbumContentBody()
                }
            }
        }
    }

    @Composable
    fun AlbumContentBody() {
        val configuration = LocalConfiguration.current
        remember(configuration) {
            (configuration.screenWidthDp.dp - 1.dp) / 3
        }
        Text("AlbumDynamicContentFragment", color = Color.Red)
    }

    override fun onDestroy() {
        viewModel.setCurrentAlbum(null)
        super.onDestroy()
    }

}
