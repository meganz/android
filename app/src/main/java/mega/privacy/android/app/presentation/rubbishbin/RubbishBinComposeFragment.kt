package mega.privacy.android.app.presentation.rubbishbin

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.dialog.rubbishbin.ConfirmMoveToRubbishBinDialogFragment
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.manager.ManagerViewModel
import mega.privacy.android.app.presentation.rubbishbin.model.RestoreType
import mega.privacy.android.app.presentation.rubbishbin.view.RubbishBinComposeView
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.TypedNode
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
     * Application Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel: RubbishBinViewModel by activityViewModels()
    private val managerViewModel: ManagerViewModel by activityViewModels()
    private val sortByHeaderViewModel: SortByHeaderViewModel by activityViewModels()
    private var actionMode: ActionMode? = null

    /**
     * onCreateView
     */
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
                    RubbishBinComposeView(
                        uiState = uiState,
                        onMenuClick = ::showOptionsMenuForItem,
                        onItemClicked = viewModel::onItemClicked,
                        onLongClick = {
                            viewModel.onLongItemClicked(it)
                            if (actionMode == null) {
                                actionMode =
                                    (requireActivity() as AppCompatActivity).startSupportActionMode(
                                        ActionBarCallback()
                                    )
                            }
                        },
                        onSortOrderClick = { showSortByPanel() },
                        onChangeViewTypeClick = viewModel::onChangeViewTypeClicked,
                        sortOrder = getString(
                            SortByHeaderViewModel.orderNameMap[uiState.sortOrder]
                                ?: R.string.sortby_name
                        ),
                        emptyState = getEmptyFolderDrawable(uiState.isRubbishBinEmpty),
                        onLinkClicked = ::navigateToLink,
                        onDisputeTakeDownClicked = ::navigateToLink
                    )
                }
                updateActionModeTitle(
                    fileCount = uiState.selectedFileNodes,
                    folderCount = uiState.selectedFolderNodes
                )
                handleRestoreBehavior(
                    restoreType = uiState.restoreType,
                    selectedNodes = uiState.selectedMegaNodes,
                    selectedNodeHandles = uiState.selectedNodeHandles,
                )
                itemClickedEvenReceived(uiState.currFileNode)
            }
        }
    }

    /**
     * Handles the "Restore" functionality
     *
     * @param restoreType The behavior when the "Restore" button is clicked
     * @param selectedNodes The list of Nodes selected by the User
     * @param selectedNodeHandles The list of Node Handles selected by the User
     */
    private fun handleRestoreBehavior(
        restoreType: RestoreType?,
        selectedNodes: List<MegaNode>?,
        selectedNodeHandles: List<Long>,
    ) {
        restoreType?.let {
            when (it) {
                RestoreType.MOVE -> {
                    NodeController(requireActivity()).chooseLocationToMoveNodes(selectedNodeHandles)
                }

                RestoreType.RESTORE -> {
                    if (!selectedNodes.isNullOrEmpty()) {
                        ((requireActivity()) as ManagerActivity).restoreFromRubbish(selectedNodes)
                    }
                }
            }
            actionMode?.finish()
            // Notify the ViewModel that the "Restore" behavior has been handled
            viewModel.onRestoreHandled()
        }
    }

    private fun getEmptyFolderDrawable(isRubbishBinEmpty: Boolean): Pair<Int, Int> {
        return if (isRubbishBinEmpty) {
            Pair(
                if (requireActivity().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    R.drawable.rubbish_bin_empty_landscape
                } else {
                    R.drawable.rubbish_bin_empty
                }, R.string.context_empty_rubbish_bin
            )
        } else {
            Pair(
                if (requireActivity().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    R.drawable.ic_zero_landscape_empty_folder
                } else {
                    R.drawable.ic_zero_portrait_empty_folder
                }, R.string.file_browser_empty_folder_new
            )
        }
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObserver()
    }

    /**
     * On Item click event received from [RubbishBinViewModel]
     *
     * @param fileNode [FileNode]
     */
    private fun itemClickedEvenReceived(fileNode: FileNode?) {
        fileNode?.let {
            openFile(fileNode = it)
            viewModel.onItemPerformedClicked()
        } ?: run {
            (requireActivity() as ManagerActivity).setToolbarTitle()
        }
    }

    /**
     * Open File
     * @param fileNode [FileNode]
     */
    private fun openFile(fileNode: FileNode) {
        lifecycleScope.launch {
            runCatching {
                viewModel.getIntent(
                    activity = requireActivity(),
                    fileNode = fileNode
                )?.let {
                    if (MegaApiUtils.isIntentAvailable(context, it)) {
                        startActivity(it)
                    } else {
                        Toast.makeText(
                            context,
                            getString(R.string.intent_not_available),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }.onFailure {
                Timber.e("itemClick:ERROR:httpServerGetLocalLink")
                (requireActivity() as? ManagerActivity)?.showSnackbar(
                    type = Constants.SNACKBAR_TYPE,
                    content = getString(R.string.general_text_error),
                    chatId = -1,
                )
            }
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
                    if (documents.isNotEmpty()) {
                        ConfirmMoveToRubbishBinDialogFragment.newInstance(documents)
                            .show(
                                requireActivity().supportFragmentManager,
                                ConfirmMoveToRubbishBinDialogFragment.TAG
                            )
                    }
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
            actionMode = null
        }
    }

    /**
     * On back pressed from ManagerActivity
     */
    fun onBackPressed(): Int {
        return with(requireActivity() as ManagerActivity) {
            if (comesFromNotifications && comesFromNotificationHandle == rubbishBinViewModel.state.value.rubbishBinHandle) {
                restoreRubbishAfterComingFromNotification()
                2
            } else {
                rubbishBinViewModel.state.value.parentHandle?.let {
                    rubbishBinViewModel.onBackPressed()
                    invalidateOptionsMenu()
                    setToolbarTitle()
                    2
                } ?: run {
                    0
                }
            }
        }
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
    private fun showOptionsMenuForItem(nodeUIItem: NodeUIItem<TypedNode>) {
        (requireActivity() as ManagerActivity).showNodeOptionsPanel(
            nodeId = nodeUIItem.id,
            mode = NodeOptionsBottomSheetDialogFragment.RUBBISH_BIN_MODE
        )
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
            }.onFailure {
                Timber.e(it, "Invalidate error")
            }
        } ?: run {
            return
        }
    }

    @OptIn(FlowPreview::class)
    private fun setupObserver() {
        viewLifecycleOwner.collectFlow(viewModel.state.map { it.isPendingRefresh }
            .sample(500L)) { isPendingRefresh ->
            if (isPendingRefresh) {
                viewModel.apply {
                    refreshNodes()
                    markHandledPendingRefresh()
                }
            }
        }

        viewLifecycleOwner.collectFlow(viewModel.state.map { it.nodeList.isEmpty() }
            .distinctUntilChanged()) {
            requireActivity().invalidateOptionsMenu()
        }
        sortByHeaderViewModel.orderChangeEvent.observe(viewLifecycleOwner, EventObserver {
            viewModel.refreshNodes()
        })
    }

    /**
     * Clicked on link
     * @param link
     */
    private fun navigateToLink(link: String) {
        val uriUrl = Uri.parse(link)
        val launchBrowser = Intent(requireContext(), WebViewActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .setData(uriUrl)
        startActivity(launchBrowser)
    }
}