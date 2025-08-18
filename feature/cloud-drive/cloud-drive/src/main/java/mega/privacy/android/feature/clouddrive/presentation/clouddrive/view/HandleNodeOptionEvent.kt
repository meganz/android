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
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.dialog.storage.StorageStatusDialogViewM3
import mega.privacy.android.core.nodecomponents.model.NodeActionState
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.feature.clouddrive.R
import mega.privacy.android.navigation.MegaNavigator
import timber.log.Timber

@Composable
fun HandleNodeOptionEvent(
    megaNavigator: MegaNavigator,
    nodeActionState: NodeActionState,
    nodeOptionsActionViewModel: NodeOptionsActionViewModel,
    nameCollisionLauncher: ManagedActivityResultLauncher<ArrayList<NameCollision>, String?>,
    snackbarHostState: SnackbarHostState?,
) {
    val context = LocalContext.current
    var isShowForeignDialog by rememberSaveable { mutableStateOf(false) }
    var isOverQuota by rememberSaveable { mutableStateOf<Boolean?>(null) }
    EventEffect(
        event = nodeActionState.nodeNameCollisionsResult,
        onConsumed = nodeOptionsActionViewModel::markHandleNodeNameCollisionResult,
        action = {
            handleNodesNameCollisionResult(
                nodeActionsViewModel = nodeOptionsActionViewModel,
                nameCollisionActivityLauncher = nameCollisionLauncher,
                result = it
            )
        }
    )

    EventEffect(
        event = nodeActionState.infoToShowEvent,
        onConsumed = nodeOptionsActionViewModel::onInfoToShowEventConsumed,
    ) {
        snackbarHostState?.showAutoDurationSnackbar(it.get(context))
    }

    EventEffect(
        event = nodeActionState.showForeignNodeDialog,
        onConsumed = nodeOptionsActionViewModel::markForeignNodeDialogShown,
        action = {
            isShowForeignDialog = true
        }
    )

    EventEffect(
        event = nodeActionState.showQuotaDialog,
        onConsumed = nodeOptionsActionViewModel::markQuotaDialogShown,
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
            positiveButtonText = stringResource(id = R.string.general_ok),
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

fun handleNodesNameCollisionResult(
    nodeActionsViewModel: NodeOptionsActionViewModel,
    nameCollisionActivityLauncher: ActivityResultLauncher<ArrayList<NameCollision>>,
    result: NodeNameCollisionsResult,
) {
    if (result.conflictNodes.isNotEmpty()) {
        nameCollisionActivityLauncher
            .launch(result.conflictNodes.values.toCollection(ArrayList()))
    }
    if (result.noConflictNodes.isNotEmpty()) {
        when (result.type) {
            NodeNameCollisionType.MOVE -> nodeActionsViewModel.moveNodes(result.noConflictNodes)
            NodeNameCollisionType.COPY -> nodeActionsViewModel.copyNodes(result.noConflictNodes)
            else -> Timber.d("Not implemented")
        }
    }
}