package mega.privacy.android.app.getLink

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.app.interfaces.Scrollable
import mega.privacy.android.core.sharedcomponents.extension.isDarkMode
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.feature.photos.presentation.albums.copyright.CopyRightScreen
import javax.inject.Inject

/**
 * Fragment of [GetLinkActivity] which informs the user about Copyright.
 */
@AndroidEntryPoint
class CopyrightFragment : Fragment(), Scrollable {

    private val viewModel: GetLinkViewModel by activityViewModels()

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    private val customBackPressed: Boolean by lazy {
        arguments?.getBoolean("back_press") ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by monitorThemeModeUseCase()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val isDarkMode = themeMode.isDarkMode()

                AndroidTheme(isDark = isDarkMode) {
                    CopyRightScreen(
                        onAgree = {
                            viewModel.updateShowCopyRight(false)
                            viewModel.agreeCopyrightTerms()

                            if (!customBackPressed) {
                                requireActivity().onBackPressedDispatcher.onBackPressed()
                            }
                        },
                        onDisagree = {
                            viewModel.updateShowCopyRight(true)
                            requireActivity().finish()
                        },
                        disableBackPress = customBackPressed
                    )
                }
            }
        }
    }

    override fun checkScroll() {
        // Scroll detection is handled internally by MegaScaffold in CopyRightScreen
        // Set elevation to false as the screen doesn't have a scrollable content that needs elevation
        viewModel.setElevation(false)
    }
}