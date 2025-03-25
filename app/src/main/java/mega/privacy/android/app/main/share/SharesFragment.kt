package mega.privacy.android.app.main.share

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.shares.incoming.IncomingSharesComposeViewModel
import mega.privacy.android.app.presentation.shares.links.LinksViewModel
import mega.privacy.android.app.presentation.shares.outgoing.OutgoingSharesComposeViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.mobile.analytics.event.IncomingSharesTabEvent
import mega.privacy.mobile.analytics.event.OutgoingSharesTabEvent

/**
 * Fragment to show the shares screen.
 */
@AndroidEntryPoint
class SharesFragment : Fragment() {
    private val viewModel by activityViewModels<SharesViewModel>()
    private val incomingSharesComposeViewModel by activityViewModels<IncomingSharesComposeViewModel>()
    private val outgoingSharesComposeViewModel by activityViewModels<OutgoingSharesComposeViewModel>()
    private val linksViewModel by activityViewModels<LinksViewModel>()
    private lateinit var managerActivity: ManagerActivity

    /**
     * On Attach
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        managerActivity = context as ManagerActivity
    }

    /**
     * Inflates the view of the fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = content {
        val themeMode by viewModel.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.System)
        val uiState by viewModel.state.collectAsStateWithLifecycle()
        val incomingUiState by incomingSharesComposeViewModel.state.collectAsStateWithLifecycle()
        val outgoingUiState by outgoingSharesComposeViewModel.state.collectAsStateWithLifecycle()
        val linksUiState by linksViewModel.state.collectAsStateWithLifecycle()

        val isDark = themeMode.isDarkMode()
        BackHandler {
            when (uiState.currentTab) {
                SharesTab.INCOMING_TAB -> {
                    if (managerActivity.comesFromNotifications
                        && managerActivity.comesFromNotificationHandle == incomingSharesComposeViewModel.getCurrentNodeHandle()
                    ) {
                        managerActivity.restoreSharesAfterComingFromNotifications()
                    } else {
                        incomingSharesComposeViewModel.performBackNavigation()
                    }
                }

                SharesTab.OUTGOING_TAB -> outgoingSharesComposeViewModel.performBackNavigation()
                SharesTab.LINKS_TAB -> linksViewModel.performBackNavigation()
                else -> Unit
            }
        }

        OriginalTheme(isDark = isDark) {
            SharesScreen(
                statusBarPadding = managerActivity.appBarLayout.paddingTop,
                uiState = uiState,
                incomingUiState = incomingUiState,
                outgoingUiState = outgoingUiState,
                linksUiState = linksUiState,
                onSearchClick = {
                    managerActivity.openSearchOnHomepage()
                },
                onMoreClick = {
                    managerActivity.showNodeOptionsPanel(
                        managerActivity.getCurrentParentNode(
                            managerActivity.currentParentHandle,
                            Constants.INVALID_VALUE
                        ),
                        hideHiddenActions = true
                    )
                },
                onPageSelected = {
                    managerActivity.onShareTabChanged()
                    viewModel.onTabSelected(it)
                    when (it) {
                        SharesTab.INCOMING_TAB -> {
                            Analytics.tracker.trackEvent(IncomingSharesTabEvent)
                        }

                        SharesTab.OUTGOING_TAB -> {
                            Analytics.tracker.trackEvent(OutgoingSharesTabEvent)
                        }

                        SharesTab.LINKS_TAB -> {
                            Analytics.tracker.trackEvent(OutgoingSharesTabEvent)
                        }

                        else -> Unit
                    }
                },
                onOpenDrawer = {
                    managerActivity.openDrawer()
                }
            )
        }
    }.also {
        // remove appbar to make SharesFragment full screen
        managerActivity.appBarLayout.isVisible = false
    }

    /**
     * On Destroy
     */
    override fun onDestroyView() {
        super.onDestroyView()
        managerActivity.appBarLayout.isVisible = true
    }

    companion object {
        /**
         * Creates a new instance of the fragment.
         */
        fun newInstance() = SharesFragment()

        /**
         * Tag of the fragment.
         */
        const val TAG = "SharesFragment"
    }
}