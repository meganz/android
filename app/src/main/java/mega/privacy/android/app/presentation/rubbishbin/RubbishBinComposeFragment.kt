package mega.privacy.android.app.presentation.rubbishbin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.manager.ManagerViewModel
import mega.privacy.android.app.presentation.view.NodesView
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetThemeMode
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * Fragment is for Rubbish Bin
 */
@AndroidEntryPoint
class RubbishBinComposeFragment : Fragment() {
    companion object {
        /**
         * Returns the instance of RubbishBinFragment
         */
        @JvmStatic
        fun newInstance() = RubbishBinComposeFragment()
    }

    /**
     * String formatter for file desc
     */
    @Inject
    lateinit var stringUtilWrapper: StringUtilWrapper

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel: RubbishBinViewModel by activityViewModels()
    private val managerViewModel: ManagerViewModel by activityViewModels()

    private var actionMode: ActionMode? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val uiState by viewModel.state.collectAsStateWithLifecycle()
                AndroidTheme(isDark = themeMode.isDarkMode()) {
                    NodesView(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        nodeUIItems = uiState.nodeList,
                        stringUtilWrapper = stringUtilWrapper,
                        onMenuClick = ::showOptionsMenuForItem,
                        onItemClicked = viewModel::onItemClicked,
                        onLongClick = {
                            viewModel.onLongItemClicked(it)
                            actionMode =
                                (requireActivity() as AppCompatActivity).startSupportActionMode(
                                    ActionBarCallback()
                                )
                        },
                        sortOrder = getString(
                            SortByHeaderViewModel.orderNameMap[uiState.sortOrder]
                                ?: R.string.sortby_name
                        ),
                        isListView = uiState.currentViewType == ViewType.LIST,
                        onSortOrderClick = { showSortByPanel() },
                        onChangeViewTypeClick = viewModel::onChangeViewTypeClicked,
                    )
                }
                updateActionModeTitle(
                    fileCount = uiState.selectedFileNodes,
                    folderCount = uiState.selectedFolderNodes
                )
                restoreMegaNode(uiState.selectedMegaNodes)
            }
        }
    }

    /**
     * Restore mega node from RubbishBin
     */
    private fun restoreMegaNode(selectedMegaNodes: List<MegaNode>?) {
        selectedMegaNodes?.let {
            ((requireActivity()) as ManagerActivity).restoreFromRubbish(it)
            viewModel.clearAllSelectedNodes()
            actionMode?.finish()
        }
    }

    private inner class ActionBarCallback : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val inflater = mode?.menuInflater
            inflater?.inflate(R.menu.rubbish_bin_action, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val totalSelectedNode =
                viewModel.state.value.selectedFileNodes + viewModel.state.value.selectedFolderNodes
            menu?.findItem(R.id.cab_menu_select_all)?.isVisible =
                totalSelectedNode < viewModel.state.value.nodeList.size
            menu?.findItem(R.id.cab_menu_restore_from_rubbish)?.isVisible = true

            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            if (!managerViewModel.isConnected) {
                ((requireActivity()) as ManagerActivity).showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getString(R.string.error_server_connection_problem),
                    MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                )
                return false
            }
            when (item?.itemId) {
                R.id.cab_menu_restore_from_rubbish -> {
                    viewModel.onRestoreClicked()
                }
                R.id.cab_menu_delete -> {
                    val documents = viewModel.state.value.selectedNodeHandles
                    ((requireActivity()) as ManagerActivity).askConfirmationMoveToRubbish(documents)
                    actionMode?.finish()
                }
                R.id.cab_menu_select_all -> viewModel.selectAllNodes()
                R.id.cab_menu_clear_selection -> {
                    viewModel.clearAllSelectedNodes()
                    actionMode?.finish()
                }
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            Timber.d("onDestroyActionMode")
            viewModel.clearAllSelectedNodes()
        }
    }

    /**
     * On back pressed from ManagerActivity
     */
    fun onBackPressed() {

    }

    /**
     * Shows the Sort by panel.
     */
    private fun showSortByPanel() {
        (requireActivity() as ManagerActivity).showNewSortByPanel(Constants.ORDER_CLOUD)
    }

    /**
     * Shows Options menu for item clicked
     */
    private fun showOptionsMenuForItem(nodeUIItem: NodeUIItem) {
        (requireActivity() as ManagerActivity).showNodeOptionsPanel(nodeId = nodeUIItem.id)
    }

    private fun updateActionModeTitle(fileCount: Int, folderCount: Int) {
        actionMode?.let {
            actionMode?.title = when {
                (fileCount == 0 && folderCount == 0) -> {
                    actionMode?.finish()
                    0.toString()
                }
                fileCount == 0 -> folderCount.toString()
                folderCount == 0 -> fileCount.toString()
                else -> (fileCount + folderCount).toString()
            }

            runCatching {
                actionMode?.invalidate()
            }.getOrElse {
                Timber.e(it, "Invalidate error")
            }
        } ?: run {
            return
        }
    }
}