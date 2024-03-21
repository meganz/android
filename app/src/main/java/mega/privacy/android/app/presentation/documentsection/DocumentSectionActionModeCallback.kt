package mega.privacy.android.app.presentation.documentsection

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.dialog.removelink.RemovePublicLinkDialogFragment
import mega.privacy.android.app.main.dialog.rubbishbin.ConfirmMoveToRubbishBinDialogFragment
import mega.privacy.android.app.main.dialog.shares.RemoveAllSharingContactDialogFragment
import mega.privacy.android.app.presentation.mapper.GetOptionsForToolbarMapper
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.MenuUtils.toggleAllMenuItemsVisibility

internal class DocumentSectionActionModeCallback(
    private val managerActivity: ManagerActivity,
    private val childFragmentManager: FragmentManager,
    private val documentSectionViewModel: DocumentSectionViewModel,
    private val getOptionsForToolbarMapper: GetOptionsForToolbarMapper,
    private val onActionModeFinished: () -> Unit,
) : ActionMode.Callback {
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.menuInflater?.inflate(R.menu.cloud_storage_action, menu)
        menu?.toggleAllMenuItemsVisibility(false)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        val selectedHandles = documentSectionViewModel.uiState.value.selectedDocumentHandles
        val selected = selectedHandles.takeUnless { it.isEmpty() } ?: return false
        menu?.findItem(R.id.cab_menu_share_link)?.title =
            managerActivity.resources.getQuantityString(R.plurals.get_links, selected.size)
        managerActivity.lifecycleScope.launch {
            val control = getOptionsForToolbarMapper(
                selectedNodeHandleList = selectedHandles,
                totalNodes = documentSectionViewModel.uiState.value.allDocuments.size
            )
            CloudStorageOptionControlUtil.applyControl(menu, control)
        }
        return true
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        item?.let {
            performItemOptionsClick(it)
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) = onActionModeFinished()

    private fun performItemOptionsClick(
        item: MenuItem,
    ) {
        managerActivity.lifecycleScope.launch {
            val selectedHandles = documentSectionViewModel.uiState.value.selectedDocumentHandles

            when (item.itemId) {
                R.id.cab_menu_download -> managerActivity.saveNodesToDevice(
                    nodes = documentSectionViewModel.getSelectedMegaNode(),
                    highPriority = false,
                    isFolderLink = false,
                    fromMediaViewer = false,
                    fromChat = false,
                )

                R.id.cab_menu_rename ->
                    managerActivity.showRenameDialog(
                        documentSectionViewModel.getSelectedMegaNode()[0]
                    )

                R.id.cab_menu_share_out ->
                    MegaNodeUtil.shareNodes(
                        managerActivity,
                        documentSectionViewModel.getSelectedMegaNode()
                    )

                R.id.cab_menu_share_link,
                R.id.cab_menu_edit_link,
                -> managerActivity.showGetLinkActivity(
                    documentSectionViewModel.getSelectedMegaNode()
                )

                R.id.cab_menu_remove_link ->
                    RemovePublicLinkDialogFragment.newInstance(
                        documentSectionViewModel.getSelectedNodes()
                            .map { node -> node.id.longValue })
                        .show(childFragmentManager, RemovePublicLinkDialogFragment.TAG)


                R.id.cab_menu_send_to_chat -> managerActivity.attachNodesToChats(
                    documentSectionViewModel.getSelectedMegaNode()
                )

                R.id.cab_menu_trash ->
                    selectedHandles.takeIf { handles ->
                        handles.isNotEmpty()
                    }?.let { handles ->
                        ConfirmMoveToRubbishBinDialogFragment.newInstance(handles)
                            .show(
                                managerActivity.supportFragmentManager,
                                ConfirmMoveToRubbishBinDialogFragment.TAG
                            )
                    }

                R.id.cab_menu_remove_share ->
                    RemoveAllSharingContactDialogFragment.newInstance(
                        documentSectionViewModel.getSelectedNodes()
                            .map { node -> node.id.longValue })
                        .show(childFragmentManager, RemoveAllSharingContactDialogFragment.TAG)

                R.id.cab_menu_select_all -> documentSectionViewModel.selectAllNodes()

                R.id.cab_menu_copy ->
                    NodeController(managerActivity).chooseLocationToCopyNodes(selectedHandles)

                R.id.cab_menu_move ->
                    NodeController(managerActivity).chooseLocationToMoveNodes(selectedHandles)
            }

            // Clear all selected audios if the action is not select all
            if (item.itemId != R.id.cab_menu_select_all) {
                documentSectionViewModel.clearAllSelectedDocuments()
            }
        }
    }
}