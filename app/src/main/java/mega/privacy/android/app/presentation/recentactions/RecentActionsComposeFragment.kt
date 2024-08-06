package mega.privacy.android.app.presentation.recentactions

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.main.HomepageFragment
import mega.privacy.android.app.fragments.homepage.main.HomepageFragmentDirections
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.contact.authenticitycredendials.AuthenticityCredentialsActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.node.NodeActionsViewModel
import mega.privacy.android.app.presentation.node.action.HandleNodeAction
import mega.privacy.android.app.presentation.recentactions.view.RecentActionsView
import mega.privacy.android.app.presentation.snackbar.LegacySnackBarWrapper
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.RecentActionsSharesType
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import javax.inject.Inject

/**
 * Fragment for recent actions tab
 */
@AndroidEntryPoint
class RecentActionsComposeFragment : Fragment() {

    private val nodeActionsViewModel: NodeActionsViewModel by viewModels()
    private val viewModel: RecentActionsComposeViewModel by activityViewModels()
    private var homepageFragment: HomepageFragment? = null

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val backgroundColor = Color(
            ColorUtils.getColorForElevation(
                requireContext(),
                Util.dp2px(HomepageFragment.BOTTOM_SHEET_ELEVATION).toFloat()
            )
        )

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by getThemeMode().collectAsStateWithLifecycle(
                    initialValue = ThemeMode.System
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val nodeActionState by nodeActionsViewModel.state.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()
                var clickedFile: TypedFileNode? by remember {
                    mutableStateOf(null)
                }

                var parentFolderSharesType: RecentActionsSharesType? by remember {
                    mutableStateOf(null)
                }

                OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                    Surface(
                        modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection()),
                        color = backgroundColor
                    ) {
                        RecentActionsView(
                            uiState = uiState,
                            backgroundColor = backgroundColor,
                            onItemClick = {
                                if (!it.isKeyVerified) {
                                    openAuthenticityCredentialsActivity(it)
                                } else {
                                    if (it.nodes.size == 1) {
                                        clickedFile = it.nodes.first()
                                        parentFolderSharesType = it.parentFolderSharesType
                                    } else {
                                        openBucketDetails(it)
                                    }
                                }
                            },
                            onMenuClick = {
                                if (uiState.isConnected) {
                                    showOptionsMenuForItem(it)
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showAutoDurationSnackbar(
                                            message = getString(R.string.error_server_connection_problem),
                                        )
                                    }
                                }
                            },
                            onShowActivityActionClick = viewModel::disableHideRecentActivitySetting,
                            onScrollStateChanged = ::onScrollStateChanged
                        )
                    }
                    StartTransferComponent(
                        event = nodeActionState.downloadEvent,
                        onConsumeEvent = nodeActionsViewModel::markDownloadEventConsumed,
                        snackBarHostState = snackbarHostState,
                    )
                    LegacySnackBarWrapper(snackbarHostState = snackbarHostState, activity)
                    clickedFile?.let {
                        HandleNodeAction(
                            typedFileNode = it,
                            nodeSourceType = when (parentFolderSharesType) {
                                RecentActionsSharesType.INCOMING_SHARES -> Constants.INCOMING_SHARES_ADAPTER
                                else -> Constants.FILE_BROWSER_ADAPTER
                            },
                            snackBarHostState = snackbarHostState,
                            onActionHandled = {
                                clickedFile = null
                                parentFolderSharesType = null
                            },
                            nodeActionsViewModel = nodeActionsViewModel,
                            coroutineScope = coroutineScope
                        )
                    }
                }
            }
        }
    }

    private fun onScrollStateChanged(isScrolling: Boolean) {
        if (isScrolling)
            homepageFragment?.hideFabButton()
        else
            homepageFragment?.showFabButton()
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        homepageFragment = parentFragment as? HomepageFragment
    }

    /**
     * Shows Options menu for item clicked
     */
    private fun showOptionsMenuForItem(typedFileNode: TypedFileNode) {
        (requireActivity() as ManagerActivity).showNodeOptionsPanel(
            nodeId = typedFileNode.id,
            mode = NodeOptionsBottomSheetDialogFragment.RECENTS_MODE
        )
    }

    /**
     * Opens Authenticity Credentials Activity
     */
    private fun openAuthenticityCredentialsActivity(bucket: RecentActionBucket) {
        Intent(requireActivity(), AuthenticityCredentialsActivity::class.java)
            .apply {
                putExtra(Constants.IS_NODE_INCOMING, bucket.nodes.first().isIncomingShare)
                putExtra(Constants.EMAIL, bucket.userEmail)
            }.let {
                requireActivity().startActivity(it)
            }
    }

    /**
     * Opens Bucket Details
     */
    private fun openBucketDetails(bucket: RecentActionBucket) {
        viewModel.selectBucket(bucket)
        val currentDestination = Navigation.findNavController(requireView()).currentDestination
        if (currentDestination?.id == R.id.homepageFragment) {
            Navigation.findNavController(requireView())
                .navigate(
                    HomepageFragmentDirections.actionHomepageToRecentBucket(),
                    NavOptions.Builder().build()
                )
        }
    }
}