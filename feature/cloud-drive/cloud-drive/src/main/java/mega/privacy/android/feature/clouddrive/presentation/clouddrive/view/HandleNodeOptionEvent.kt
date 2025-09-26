package mega.privacy.android.feature.clouddrive.presentation.clouddrive.view

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.core.nodecomponents.dialog.storage.StorageStatusDialogViewM3
import mega.privacy.android.core.nodecomponents.model.NodeActionState
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.feature.clouddrive.R
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.resources.R as sharedResR
import timber.log.Timber

/**
 * Handles node option events and displays appropriate dialogs
 *
 * @param megaNavigator Navigator for handling navigation actions
 * @param nodeActionState Current state of node actions
 * @param nameCollisionLauncher Launcher for handling name collision resolution
 * @param snackbarHostState State for showing snackbar messages
 * @param onNodeNameCollisionResultHandled Callback when node name collision result is handled
 * @param onInfoToShowEventConsumed Callback when info to show event is consumed
 * @param onForeignNodeDialogShown Callback when foreign node dialog is shown
 * @param onQuotaDialogShown Callback when quota dialog is shown
 * @param onHandleNodesWithoutConflict Callback to handle nodes without conflicts, receives collision type and node map
 */
@Composable
fun HandleNodeOptionEvent(
    megaNavigator: MegaNavigator,
    nodeActionState: NodeActionState,
    nameCollisionLauncher: ManagedActivityResultLauncher<ArrayList<NameCollision>, String?>,
    snackbarHostState: SnackbarHostState?,
    onNodeNameCollisionResultHandled: () -> Unit,
    onInfoToShowEventConsumed: () -> Unit,
    onForeignNodeDialogShown: () -> Unit,
    onQuotaDialogShown: () -> Unit,
    onHandleNodesWithoutConflict: (NodeNameCollisionType, Map<Long, Long>) -> Unit,
) {
    val context = LocalContext.current
    var isShowForeignDialog by rememberSaveable { mutableStateOf(false) }
    var isOverQuota by rememberSaveable { mutableStateOf<Boolean?>(null) }
    EventEffect(
        event = nodeActionState.nodeNameCollisionsResult,
        onConsumed = onNodeNameCollisionResultHandled,
        action = {
            handleNodesNameCollisionResult(
                nameCollisionActivityLauncher = nameCollisionLauncher,
                result = it,
                onHandleNodesWithoutConflict = onHandleNodesWithoutConflict
            )
        }
    )

    EventEffect(
        event = nodeActionState.infoToShowEvent,
        onConsumed = onInfoToShowEventConsumed,
    ) {
        snackbarHostState?.showAutoDurationSnackbar(it.get(context))
    }

    EventEffect(
        event = nodeActionState.showForeignNodeDialog,
        onConsumed = onForeignNodeDialogShown,
        action = {
            isShowForeignDialog = true
        }
    )

    EventEffect(
        event = nodeActionState.showQuotaDialog,
        onConsumed = onQuotaDialogShown,
        action = {
            isOverQuota = it
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