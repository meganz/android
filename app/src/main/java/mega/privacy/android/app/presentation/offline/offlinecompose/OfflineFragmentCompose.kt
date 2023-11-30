package mega.privacy.android.app.presentation.offline.offlinecompose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.fragments.homepage.main.HomepageFragmentDirections
import mega.privacy.android.app.interfaces.Scrollable
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.offline.view.OfflineFeatureScreen
import mega.privacy.android.app.utils.callManager
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

/**
 * OfflineFragment with Compose
 */
@AndroidEntryPoint
class OfflineFragmentCompose : Fragment(), Scrollable {

    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel: OfflineComposeViewModel by viewModels()
    private val args: OfflineFragmentComposeArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments == null) {
            arguments =
                HomepageFragmentDirections.actionHomepageFragmentToOfflineFragmentCompose().arguments
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val themeMode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val isDarkMode = themeMode.isDarkMode()
                MegaAppTheme(isDark = isDarkMode) {
                    OfflineFeatureScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        callManager {
            if (args.rootFolderOnly) {
                it.pagerOfflineFragmentComposeOpened(this)
            } else {
                it.fullscreenOfflineFragmentComposeOpened(this)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        callManager {
            if (args.rootFolderOnly) {
                it.pagerOfflineFragmentComposeClosed(this)
            } else {
                it.fullscreenOfflineFragmentComposeClosed(this)
            }
        }
    }

    /**
     * checkScroll
     */
    override fun checkScroll() {
        //Change Appbar Elevation
    }
}