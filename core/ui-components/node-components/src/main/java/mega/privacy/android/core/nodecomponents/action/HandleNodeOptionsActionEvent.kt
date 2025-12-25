package mega.privacy.android.core.nodecomponents.action

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.core.nodecomponents.dialog.rename.RenameNodeDialogNavKey
import mega.privacy.android.core.nodecomponents.dialog.sharefolder.ShareFolderAccessDialogNavKey
import mega.privacy.android.core.nodecomponents.dialog.sharefolder.ShareFolderDialogNavKey
import mega.privacy.android.core.nodecomponents.dialog.storage.StorageStatusDialogViewM3
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.core.nodecomponents.model.NodeActionState
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.navigation.extensions.rememberMegaResultContract
import mega.privacy.android.shared.resources.R as sharedResR
import timber.log.Timber

/**
 * Handles node option events and triggers appropriate actions based on the event.
 *
 * @param nodeActionState The current state of node actions.
 * @param onCopyNodes Callback to handle copying nodes.
 * @param onMoveNodes Callback to handle moving nodes.
 * @param consumeNameCollisionResult Callback to consume the name collision result.
 * @param consumeInfoToShow Callback to consume the info to show event.
 * @param consumeForeignNodeDialog Callback to consume the foreign node dialog event.
 * @param consumeQuotaDialog Callback to consume the quota dialog event.
 */
@Composable
internal fun HandleNodeOptionsActionEvent(
    nodeActionState: NodeActionState,
    onCopyNodes: (nodes: Map<Long, Long>) -> Unit,
    onMoveNodes: (nodes: Map<Long, Long>) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    onNavigate: (NavKey) -> Unit,
    onShareContactSelected: (List<String>, List<Long>) -> Unit,
    consumeNameCollisionResult: () -> Unit,
    consumeInfoToShow: () -> Unit,
    consumeForeignNodeDialog: () -> Unit,
    consumeQuotaDialog: () -> Unit,
    consumeDownloadEvent: () -> Unit,
    consumeRenameNodeRequest: () -> Unit,
    consumeNavigationEvent: () -> Unit,
    consumeDismissEvent: () -> Unit,
    consumeAccessDialogShown: () -> Unit,
    consumeShareFolderEvent: () -> Unit,
    consumeShareFolderDialogEvent: () -> Unit,
    onActionTriggered: () -> Unit = {},
) {
    val snackbarQueue = rememberSnackBarQueue()
    val megaNavigator = rememberMegaNavigator()
    val megaResultContract = rememberMegaResultContract()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isShowForeignDialog by rememberSaveable { mutableStateOf(false) }
    var isOverQuota by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val nameCollisionLauncher = rememberLauncherForActivityResult(
        contract = megaResultContract.nameCollisionActivityContract
    ) { message ->
        if (!message.isNullOrEmpty()) {
            coroutineScope.launch {
                snackbarQueue.queueMessage(message)
            }
        }
    }
    val shareFolderLauncher = rememberLauncherForActivityResult(
        contract = megaResultContract.shareFolderActivityResultContract
    ) { result ->
        result?.let { (contactIds, nodeHandles) ->
            onShareContactSelected(contactIds, nodeHandles)
        }
    }
    val nodeHandlesToJsonMapper = remember { NodeHandlesToJsonMapper() }

    EventEffect(
        event = nodeActionState.nodeNameCollisionsResult,
        onConsumed = consumeNameCollisionResult,
        action = {
            onActionTriggered()
            handleNodesNameCollisionResult(
                nameCollisionActivityLauncher = nameCollisionLauncher,
                result = it,
                onHandleNodesWithoutConflict = { collisionType, nodes ->
                    when (collisionType) {
                        NodeNameCollisionType.MOVE -> onMoveNodes(nodes)
                        NodeNameCollisionType.COPY -> onCopyNodes(nodes)
                        else -> {
                            /* No-op for other types */
                        }
                    }
                }
            )
        }
    )

    EventEffect(
        event = nodeActionState.infoToShowEvent,
        onConsumed = consumeInfoToShow,
    ) {
        onActionTriggered()
        snackbarQueue.queueMessage(it.get(context))
    }

    EventEffect(
        event = nodeActionState.showForeignNodeDialog,
        onConsumed = consumeForeignNodeDialog,
        action = {
            onActionTriggered()
            isShowForeignDialog = true
        }
    )

    EventEffect(
        event = nodeActionState.showQuotaDialog,
        onConsumed = consumeQuotaDialog,
        action = {
            onActionTriggered()
            isOverQuota = it
        }
    )

    EventEffect(
        event = nodeActionState.downloadEvent,
        onConsumed = consumeDownloadEvent,
        action = {
            onActionTriggered()
            onTransfer(it)
        }
    )

    EventEffect(
        event = nodeActionState.renameNodeRequestEvent,
        onConsumed = consumeRenameNodeRequest
    ) { nodeId ->
        onActionTriggered()
        onNavigate(RenameNodeDialogNavKey(nodeId.longValue))
    }

    EventEffect(
        event = nodeActionState.navigationEvent,
        onConsumed = consumeNavigationEvent,
        action = {
            onActionTriggered()
            onNavigate(it)
        }
    )

    EventEffect(
        event = nodeActionState.dismissEvent,
        onConsumed = consumeDismissEvent,
        action = onActionTriggered
    )

    EventEffect(
        event = nodeActionState.contactsData,
        onConsumed = consumeAccessDialogShown,
        action = { (contactData, isFromBackups, nodeHandles) ->
            onActionTriggered()
            onNavigate(
                ShareFolderAccessDialogNavKey(
                    nodes = nodeHandles,
                    contacts = contactData.joinToString(separator = ","),
                    isFromBackups = isFromBackups,
                )
            )
        },
    )

    EventEffect(
        event = nodeActionState.shareFolderDialogEvent,
        onConsumed = consumeShareFolderDialogEvent,
        action = { handles ->
            val nodes = nodeHandlesToJsonMapper(handles)
            onNavigate(ShareFolderDialogNavKey(nodes))
        }
    )

    EventEffect(
        event = nodeActionState.shareFolderEvent,
        onConsumed = consumeShareFolderEvent,
        action = { handles ->
            onActionTriggered()
            shareFolderLauncher.launch(handles.toLongArray())
        }
    )

    if (isShowForeignDialog) {
        BasicDialog(
            title = "",
            description = stringResource(id = R.string.warning_share_owner_storage_quota),
            onPositiveButtonClicked = {
                isShowForeignDialog = false
            },
            positiveButtonText = stringResource(id = sharedResR.string.general_ok),
        )
    }

    if (isOverQuota != null) {
        StorageStatusDialogViewM3(
            storageState = if (isOverQuota == true) StorageState.Red else StorageState.Orange,
            preWarning = isOverQuota == false,
            overQuotaAlert = true,
            onUpgradeClick = {
                megaNavigator.openUpgradeAccount(context = context)
            },
            onCustomizedPlanClick = { email, accountType ->
                megaNavigator.openAskForCustomizedPlan(
                    context = context,
                    email = email,
                    accountType = accountType
                )
            },
            onAchievementsClick = {
                megaNavigator.openAchievements(context = context)
            },
            onClose = {
                isOverQuota = null
            },
        )
    }
}

/**
 * Helper function to handle node name collision results
 *
 * @param nameCollisionActivityLauncher Launcher for handling name collision resolution
 * @param result The collision result containing conflict and no-conflict nodes
 * @param onHandleNodesWithoutConflict Callback to handle nodes without conflicts, receives collision type and node map
 */
fun handleNodesNameCollisionResult(
    nameCollisionActivityLauncher: ActivityResultLauncher<ArrayList<NameCollision>>,
    result: NodeNameCollisionsResult,
    onHandleNodesWithoutConflict: (NodeNameCollisionType, Map<Long, Long>) -> Unit,
) {
    if (result.conflictNodes.isNotEmpty()) {
        nameCollisionActivityLauncher
            .launch(result.conflictNodes.values.toCollection(ArrayList()))
    }
    if (result.noConflictNodes.isNotEmpty()) {
        when (result.type) {
            NodeNameCollisionType.MOVE, NodeNameCollisionType.COPY -> {
                onHandleNodesWithoutConflict(result.type, result.noConflictNodes)
            }

            else -> Timber.d("Not implemented")
        }
    }
}