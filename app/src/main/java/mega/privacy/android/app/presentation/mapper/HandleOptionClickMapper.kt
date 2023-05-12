package mega.privacy.android.app.presentation.mapper

import android.view.MenuItem
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.clouddrive.OptionItems
import mega.privacy.android.app.presentation.clouddrive.model.OptionsItemInfo
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Handles click events for toolbar item
 */
class HandleOptionClickMapper @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke(
        item: MenuItem,
        selectedNodeHandle: List<Long>,
    ): OptionsItemInfo {
        val selectedMegaNodes = mutableListOf<MegaNode>()
        val selectedNodes = mutableListOf<Node>()

        selectedNodeHandle.forEach {
            getNodeByIdUseCase(NodeId(it))?.let { node ->
                selectedNodes.add(node)
            }
            getNodeByHandle(handle = it)?.let { megaNode ->
                selectedMegaNodes.add(megaNode)
            }
        }

        val optionClickedType = when (item.itemId) {
            R.id.cab_menu_download -> {
                OptionItems.DOWNLOAD_CLICKED
            }
            R.id.cab_menu_rename -> {
                OptionItems.RENAME_CLICKED
            }
            R.id.cab_menu_copy -> {
                OptionItems.COPY_CLICKED
            }
            R.id.cab_menu_move -> {
                OptionItems.MOVE_CLICKED
            }
            R.id.cab_menu_share_folder -> {
                OptionItems.SHARE_FOLDER_CLICKED
            }
            R.id.cab_menu_share_out -> {
                OptionItems.SHARE_OUT_CLICKED
            }
            R.id.cab_menu_share_link, R.id.cab_menu_edit_link -> {
                OptionItems.SHARE_EDIT_LINK_CLICKED
            }
            R.id.cab_menu_remove_link -> {
                OptionItems.REMOVE_LINK_CLICKED
            }
            R.id.cab_menu_send_to_chat -> {
                OptionItems.SEND_TO_CHAT_CLICKED
            }
            R.id.cab_menu_trash -> {
                OptionItems.MOVE_TO_RUBBISH_CLICKED
            }
            R.id.cab_menu_select_all -> {
                OptionItems.SELECT_ALL_CLICKED
            }
            R.id.cab_menu_clear_selection -> {
                OptionItems.CLEAR_ALL_CLICKED
            }
            R.id.cab_menu_remove_share -> {
                OptionItems.REMOVE_SHARE_CLICKED
            }
            R.id.cab_menu_dispute -> {
                OptionItems.DISPUTE_CLICKED
            }
            else -> {
                OptionItems.CLEAR_ALL_CLICKED
            }
        }
        return OptionsItemInfo(
            optionClickedType = optionClickedType,
            selectedMegaNode = selectedMegaNodes,
            selectedNode = selectedNodes
        )
    }
}