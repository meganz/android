package mega.privacy.android.app.presentation.audiosection

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
import mega.privacy.android.app.presentation.validator.toolbaractions.ToolbarActionsValidator
import mega.privacy.android.app.presentation.validator.toolbaractions.model.ToolbarActionsRequest
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.MenuUtils.toggleAllMenuItemsVisibility
import mega.privacy.android.app.utils.Util
import mega.privacy.android.shared.resources.R as sharedR

internal class AudioSectionActionModeCallback(
    private val fragment: AudioSectionFragment,
    private val managerActivity: ManagerActivity,
    private val childFragmentManager: FragmentManager,
    private val audioSectionViewModel: AudioSectionViewModel,
    private val toolbarActionsValidator: ToolbarActionsValidator,
    private val onActionModeFinished: () -> Unit,
) : ActionMode.Callback {
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.menuInflater?.inflate(R.menu.cloud_storage_action, menu)
        menu?.toggleAllMenuItemsVisibility(false)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        val actionModeState = audioSectionViewModel.state.value.toolbarActionsModifierItem
        if (actionModeState == null) {
            return false
        }
        menu?.findItem(R.id.cab_menu_share_link)?.title =
            managerActivity.resources.getQuantityString(
                sharedR.plurals.label_share_links,
                audioSectionViewModel.state.value.selectedNodes.size
            )
        val control = toolbarActionsValidator(
            request = ToolbarActionsRequest(
                modifierItem = actionModeState,
                selectedNodes = audioSectionViewModel.state.value.selectedNodes,
                totalNodes = audioSectionViewModel.state.value.allAudios.size
            )
        )
        CloudStorageOptionControlUtil.applyControl(
            menu,
            control
        )
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
            val selectedAudios = audioSectionViewModel.state.value.selectedNodes.map { it.id }

            when (item.itemId) {
                R.id.cab_menu_download -> managerActivity.saveNodesToDevice(
                    nodes = audioSectionViewModel.getSelectedMegaNode(),
                    highPriority = false,
                    isFolderLink = false,
                    fromChat = false,
                    withStartMessage = false,
                )

                R.id.cab_menu_rename -> managerActivity.showRenameDialog(audioSectionViewModel.getSelectedMegaNode()[0])

                R.id.cab_menu_share_out ->
                    MegaNodeUtil.shareNodes(
                        managerActivity,
                        audioSectionViewModel.getSelectedMegaNode()
                    )

                R.id.cab_menu_share_link,
                R.id.cab_menu_edit_link,
                    -> managerActivity.showGetLinkActivity(audioSectionViewModel.getSelectedMegaNode())

                R.id.cab_menu_remove_link ->
                    RemovePublicLinkDialogFragment.newInstance(
                        audioSectionViewModel.getSelectedNodes().map { node -> node.id.longValue })
                        .show(childFragmentManager, RemovePublicLinkDialogFragment.TAG)


                R.id.cab_menu_send_to_chat -> managerActivity.attachNodesToChats(
                    audioSectionViewModel.getSelectedMegaNode()
                )

                R.id.cab_menu_trash ->
                    selectedAudios.takeIf { handles ->
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
                        audioSectionViewModel.getSelectedNodes().map { node -> node.id.longValue })
                        .show(childFragmentManager, RemoveAllSharingContactDialogFragment.TAG)

                R.id.cab_menu_select_all -> audioSectionViewModel.selectAllNodes()

                R.id.cab_menu_hide -> fragment.handleHideNodeClick()

                R.id.cab_menu_unhide -> {
                    val nodes = audioSectionViewModel.getSelectedNodes()
                    audioSectionViewModel.hideOrUnhideNodes(
                        nodeIds = nodes.map { it.id },
                        hide = false,
                    )
                    val message = fragment.resources.getQuantityString(
                        sharedR.plurals.unhidden_nodes_result_message,
                        nodes.size,
                        nodes.size,
                    )
                    Util.showSnackbar(managerActivity, message)
                }

                R.id.cab_menu_copy ->
                    NodeController(managerActivity).chooseLocationToCopyNodes(selectedAudios)

                R.id.cab_menu_move ->
                    NodeController(managerActivity).chooseLocationToMoveNodes(selectedAudios)
            }

            // Clear all selected audios if the action is not select all
            if (item.itemId != R.id.cab_menu_select_all) {
                audioSectionViewModel.clearAllSelectedAudios()
            }
        }
    }
}
