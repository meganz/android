package mega.privacy.android.app.presentation.favourites

import mega.privacy.android.shared.resources.R as sharedR
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.dialog.rubbishbin.ConfirmMoveToRubbishBinDialogFragment
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.MenuUtils.toggleAllMenuItemsVisibility
import mega.privacy.android.domain.entity.node.NodeId
import timber.log.Timber

/**
 * Action mode callback is use for feature regarding to favourite
 * @param mainActivity ManagerActivity
 * @param viewModel FavouritesViewModel
 */
class FavouriteActionModeCallback(
    private val mainActivity: ManagerActivity,
    private val viewModel: FavouritesViewModel,
    private val fragment: FavouritesFragment,
    private val context: Context,
) :
    ActionMode.Callback {

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.menuInflater?.inflate(R.menu.cloud_storage_action, menu)
        menu?.toggleAllMenuItemsVisibility(false)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        menu?.let {
            it.findItem(R.id.cab_menu_share_link)?.let { item ->
                item.title = fragment.context?.resources?.getQuantityString(
                    sharedR.plurals.label_share_links,
                    viewModel.getItemsSelected().size
                )
            }

            handleHiddenNodes(it)
        }
        return true
    }

    private fun handleHiddenNodes(menu: Menu) {
        mainActivity.lifecycleScope.launch {
            runCatching {
                val isHiddenNodesEnabled =
                    mainActivity.getFeatureFlagValueUseCase(AppFeatures.HiddenNodes)
                val selectedNodes = viewModel.getItemsSelected().mapNotNull { it.value.typedNode }
                val isHidingActionAllowed = selectedNodes.all {
                    viewModel.isHidingActionAllowed(it.id)
                } && !selectedNodes.any { it.isSensitiveInherited }

                if (isHiddenNodesEnabled && isHidingActionAllowed) {
                    val isPaid =
                        viewModel.getIsPaidAccount()

                    val hasNonSensitiveNode = selectedNodes.any { !it.isMarkedSensitive }
                    menu.findItem(R.id.cab_menu_hide)?.isVisible =
                        hasNonSensitiveNode || !isPaid

                    menu.findItem(R.id.cab_menu_unhide)?.isVisible =
                        !hasNonSensitiveNode && isPaid
                } else {
                    menu.findItem(R.id.cab_menu_hide)?.isVisible = false
                    menu.findItem(R.id.cab_menu_unhide)?.isVisible = false
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        item?.run {
            val nodeHandles = arrayListOf<Long>().apply {
                addAll(
                    viewModel.getItemsSelected().values.map {
                        it.typedNode.id.longValue
                    })
            }
            val selectedNodes = viewModel.getItemsSelected().values.map { it.node }

            if (itemId != R.id.cab_menu_select_all) {
                viewModel.clearSelections()
            }

            when (itemId) {
                R.id.cab_menu_download -> {
                    mainActivity.saveNodesToDevice(selectedNodes, false, false, false)
                }

                R.id.cab_menu_copy -> {
                    NodeController(mainActivity).chooseLocationToCopyNodes(nodeHandles)
                }

                R.id.cab_menu_move -> {
                    NodeController(mainActivity).chooseLocationToMoveNodes(nodeHandles)
                }

                R.id.cab_menu_share_out -> {
                    MegaNodeUtil.shareNodes(mainActivity, selectedNodes)
                }

                R.id.cab_menu_share_link -> {
                    Timber.d("Public link option")
                    if (nodeHandles.isNotEmpty()) {
                        if (nodeHandles.size > 1) {
                            LinksUtil.showGetLinkActivity(mainActivity, nodeHandles.toLongArray())
                        } else {
                            LinksUtil.showGetLinkActivity(mainActivity, nodeHandles[0])
                        }
                    } else {
                    }
                }

                R.id.cab_menu_send_to_chat -> {
                    Timber.d("Send files to chat")
                    mainActivity.attachNodesToChats(selectedNodes)
                }

                R.id.cab_menu_trash -> {
                    if (nodeHandles.isNotEmpty()) {
                        ConfirmMoveToRubbishBinDialogFragment.newInstance(nodeHandles)
                            .show(
                                mainActivity.supportFragmentManager,
                                ConfirmMoveToRubbishBinDialogFragment.TAG
                            )
                    } else {
                    }
                }

                R.id.cab_menu_select_all -> viewModel.selectAll()

                R.id.cab_menu_hide -> {
                    fragment.onHideClicked(
                        nodeIds = nodeHandles.map { NodeId(longValue = it) },
                    )
                }

                R.id.cab_menu_unhide -> viewModel.hideOrUnhideNodes(
                    nodeIds = nodeHandles.map { NodeId(longValue = it) },
                    hide = false,
                )

                R.id.cab_menu_remove_favourites -> {
                    viewModel.favouritesRemoved(nodeHandles)
                }

                R.id.cab_menu_rename -> {
                    mainActivity.showRenameDialog(selectedNodes.first())
                }

                R.id.cab_menu_share_folder -> {
                    NodeController(mainActivity).selectContactToShareFolders(nodeHandles)
                }

                R.id.cab_menu_dispute -> {
                    context.startActivity(
                        Intent(context, WebViewActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            .setData(Uri.parse(Constants.DISPUTE_URL))
                    )
                }

                else -> {}
            }
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        // When the action mode is finished, clear the selections
        viewModel.clearSelections()
    }
}