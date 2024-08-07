package mega.privacy.android.app.presentation.transfers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.components.session.SessionContainer
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.app.presentation.transfers.view.IN_PROGRESS_TAB_INDEX
import mega.privacy.android.app.presentation.transfers.view.navigation.compose.navigateToTransfersViewGraph
import mega.privacy.android.app.presentation.transfers.view.navigation.compose.transfersViewNavigationGraph
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import javax.inject.Inject

/**
 * Extra for the tab to show in the transfers screen.
 */
const val EXTRA_TAB = "TAB"

@AndroidEntryPoint
internal class TransfersFragment : Fragment() {

    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    @OptIn(ExperimentalMaterialNavigationApi::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val mode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            SessionContainer(shouldCheckChatSession = true) {
                OriginalTempTheme(isDark = mode.isDarkMode()) {
                    PasscodeContainer(
                        passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                        content = {
                            val bottomSheetNavigator = rememberBottomSheetNavigator()
                            val navHostController = rememberNavController(bottomSheetNavigator)
                            val scaffoldState = rememberScaffoldState()
                            val tabIndex = arguments?.getInt(EXTRA_TAB) ?: IN_PROGRESS_TAB_INDEX

                            NavHost(
                                navController = navHostController,
                                startDestination = "start",
                                modifier = Modifier.navigationBarsPadding()
                            ) {
                                composable("start") {
                                    navHostController.navigateToTransfersViewGraph(
                                        tabIndex = tabIndex,
                                        navOptions = navOptions {
                                            popUpTo("start") {
                                                inclusive = true
                                            }
                                        })
                                }

                                transfersViewNavigationGraph(
                                    bottomSheetNavigator = bottomSheetNavigator,
                                    navHostController = navHostController,
                                    scaffoldState = scaffoldState,
                                    onBackPress = { requireActivity().supportFinishAfterTransition() }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}