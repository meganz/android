package mega.privacy.android.app.presentation.transfers.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.psa.PsaContainer
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.app.presentation.settings.model.StorageTargetPreference
import mega.privacy.android.app.presentation.transfers.preview.view.FakePreviewInfo
import mega.privacy.android.app.presentation.transfers.preview.view.fakePreviewScreen
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import javax.inject.Inject

@AndroidEntryPoint
class FakePreviewFragment : Fragment() {

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    /**
     * The centralized navigator in the :app module
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val mode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            OriginalTheme(isDark = mode.isDarkMode()) {
                PasscodeContainer(
                    passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                    content = {
                        PsaContainer {
                            val navHostController = rememberNavController()
                            val transferPath =
                                arguments?.getString(EXTRA_FILE_PATH)
                                    .takeUnless { it.isNullOrEmpty() }
                            val transferUniqueId =
                                arguments?.getLong(EXTRA_TRANSFER_UNIQUE_ID, -1)
                                    .takeUnless { it == -1L }
                            val transferTagToCancel =
                                arguments?.getInt(EXTRA_TRANSFER_TAG, -1)
                                    .takeUnless { it == -1 }

                            NavHost(
                                navController = navHostController,
                                startDestination = FakePreviewInfo(
                                    transferPath = transferPath,
                                    transferUniqueId = transferUniqueId,
                                    transferTagToCancel = transferTagToCancel,
                                ),
                                modifier = Modifier.navigationBarsPadding(),
                            ) {
                                fakePreviewScreen(
                                    onBackPress = { requireActivity().supportFinishAfterTransition() },
                                    navigateToStorageSettings = {
                                        megaNavigator.openSettings(
                                            requireActivity(),
                                            StorageTargetPreference
                                        )
                                    },
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_TRANSFER_UNIQUE_ID = "TRANSFER_UNIQUE_ID"
        const val EXTRA_FILE_PATH = "FILE_PATH"
        const val EXTRA_ERROR = "ERROR"
        const val EXTRA_TRANSFER_TAG = "TRANSFER_TAG"
    }
}