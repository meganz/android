package mega.privacy.android.feature.sync.ui

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedResR
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.navigation.syncNavGraph
import mega.privacy.android.feature.sync.navigation.syncRoute
import mega.privacy.android.feature.sync.ui.mapper.SyncChipValueMapper
import mega.privacy.android.feature.sync.ui.newfolderpair.TAG_SYNC_NEW_FOLDER_SCREEN_TOOLBAR
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.sync.ui.SyncEmptyState
import javax.inject.Inject

/**
 * Screen for syncing local folder with MEGA
 */
@AndroidEntryPoint
class SyncFragment : Fragment() {

    companion object {
        /**
         * To specify the fragment title value
         */
        const val TITLE_KEY = "titleKey"

        /**
         * To specify if the view to create a new sync should be directly displayed
         */
        const val OPEN_NEW_SYNC_KEY = "openNewSyncKey"

        /**
         * Returns the instance of SyncFragment
         */
        @JvmStatic
        fun newInstance(title: String? = null, openNewSync: Boolean = false): SyncFragment {
            val args = Bundle().apply {
                putString(TITLE_KEY, title)
                putBoolean(OPEN_NEW_SYNC_KEY, openNewSync)
            }
            return SyncFragment().apply { arguments = args }
        }
    }

    /**
     * Allows navigation to specific features in the monolith :app
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    /**
     * Get Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * Get [FileTypeIconMapper]
     */
    @Inject
    lateinit var fileTypeIconMapper: FileTypeIconMapper

    /**
     * Get [SyncChipValueMapper]
     */
    @Inject
    lateinit var syncChipValueMapper: SyncChipValueMapper

    /**
     * Get [SyncPermissionsManager]
     */
    @Inject
    lateinit var syncPermissionsManager: SyncPermissionsManager

    private val viewModel by viewModels<SyncViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val title = arguments?.getString(TITLE_KEY)
                val openNewSync = arguments?.getBoolean(OPEN_NEW_SYNC_KEY) ?: false
                val animatedNavController = rememberNavController()
                val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val state by viewModel.state.collectAsStateWithLifecycle()

                OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                    if (state.isNetworkConnected) {
                        AndroidSyncFeatureNavigation(animatedNavController, title, openNewSync)
                    } else {
                        SyncNoNetworkState()
                    }
                }
            }
        }
    }

    @Composable
    private fun AndroidSyncFeatureNavigation(
        animatedNavController: NavHostController,
        title: String?,
        openNewSync: Boolean,
    ) {
        val context = LocalContext.current
        NavHost(
            navController = animatedNavController,
            startDestination = syncRoute,
        ) {
            syncNavGraph(
                navController = animatedNavController,
                fileTypeIconMapper = fileTypeIconMapper,
                syncChipValueMapper = syncChipValueMapper,
                syncPermissionsManager = syncPermissionsManager,
                openUpgradeAccountPage = {
                    megaNavigator.openUpgradeAccount(context)
                },
                title = title,
                openNewSync = openNewSync,
            )
        }
    }

    /**
     * A [Composable] which displays a No network connectivity state
     */
    @Composable
    private fun SyncNoNetworkState() {
        val scaffoldState = rememberScaffoldState()

        MegaScaffold(
            scaffoldState = scaffoldState,
            topBar = {
                MegaAppBar(
                    modifier = Modifier.testTag(TAG_SYNC_NEW_FOLDER_SCREEN_TOOLBAR),
                    appBarType = AppBarType.BACK_NAVIGATION,
                    title = stringResource(R.string.sync_toolbar_title),
                    onNavigationPressed = { requireActivity().onBackPressed() },
                    elevation = 0.dp
                )
            }, content = { _ ->
                SyncEmptyState(
                    iconId = iconPackR.drawable.ic_no_cloud,
                    iconSize = 144.dp,
                    iconDescription = "No network connectivity state",
                    textId = sharedResR.string.sync_no_network_state,
                    testTag = SYNC_NO_NETWORK_STATE
                )
            }
        )
    }


}

internal const val SYNC_NO_NETWORK_STATE = "sync:no_network_state"
