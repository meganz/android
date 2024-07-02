package mega.privacy.android.app.presentation.audiosection

import mega.privacy.android.shared.resources.R as sharedR
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.dialog.removelink.RemovePublicLinkDialogFragment
import mega.privacy.android.app.main.dialog.rubbishbin.ConfirmMoveToRubbishBinDialogFragment
import mega.privacy.android.app.main.dialog.shares.RemoveAllSharingContactDialogFragment
import mega.privacy.android.app.presentation.mapper.GetOptionsForToolbarMapper
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.MenuUtils.toggleAllMenuItemsVisibility

internal class AudioSectionActionModeCallback(
    private val fragment: AudioSectionFragment,
    private val managerActivity: ManagerActivity,
    private val childFragmentManager: FragmentManager,
    private val audioSectionViewModel: AudioSectionViewModel,
    private val getOptionsForToolbarMapper: GetOptionsForToolbarMapper,
    private val onActionModeFinished: () -> Unit,
) : ActionMode.Callback {
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.menuInflater?.inflate(R.menu.cloud_storage_action, menu)
        menu?.toggleAllMenuItemsVisibility(false)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        val selected =
            audioSectionViewModel.state.value.selectedAudioHandles.takeUnless { it.isEmpty() }
                ?: return false
        menu?.findItem(R.id.cab_menu_share_link)?.title =
            managerActivity.resources.getQuantityString(
                sharedR.plurals.label_share_links,
                selected.size
            )
        managerActivity.lifecycleScope.launch {
            val control = getOptionsForToolbarMapper(
                selectedNodeHandleList = audioSectionViewModel.state.value.selectedAudioHandles,
                totalNodes = audioSectionViewModel.state.value.allAudios.size
            )
            CloudStorageOptionControlUtil.applyControl(menu, control)

            val selectedNodes = audioSectionViewModel.getSelectedNodes()
            val isHiddenNodesEnabled =
                managerActivity.getFeatureFlagValueUseCase(AppFeatures.HiddenNodes)
            val includeSensitiveInheritedNode = selectedNodes.any { it.isSensitiveInherited }

            if (isHiddenNodesEnabled && !includeSensitiveInheritedNode) {
                val hasNonSensitiveNode = selectedNodes.any { !it.isMarkedSensitive }
                val isPaid =
                    audioSectionViewModel.state.value.accountDetail?.levelDetail?.accountType?.isPaid
                        ?: false

                menu?.findItem(R.id.cab_menu_hide)?.isVisible =
                    (hasNonSensitiveNode || !isPaid)

                menu?.findItem(R.id.cab_menu_unhide)?.isVisible =
                    !hasNonSensitiveNode && isPaid
            } else {
                menu?.findItem(R.id.cab_menu_hide)?.isVisible = false
                menu?.findItem(R.id.cab_menu_unhide)?.isVisible = false
            }
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
            val selectedAudios = audioSectionViewModel.state.value.selectedAudioHandles

            when (item.itemId) {
                R.id.cab_menu_download -> managerActivity.saveNodesToDevice(
                    nodes = audioSectionViewModel.getSelectedMegaNode(),
                    highPriority = false,
                    isFolderLink = false,
                    fromChat = false,
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

                R.id.cab_menu_unhide -> audioSectionViewModel.hideOrUnhideNodes(
                    nodeIds = audioSectionViewModel.getSelectedNodes().map { it.id },
                    hide = false,
                )

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