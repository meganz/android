package mega.privacy.android.app.presentation.search.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import mega.privacy.android.app.presentation.node.dialogs.leaveshare.LeaveShareDialog
import mega.privacy.android.app.presentation.search.nodeListHandle
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import timber.log.Timber

internal fun NavGraphBuilder.leaveFolderShareDialogNavigation(
    navHostController: NavHostController,
    stringWithDelimitersMapper: ListToStringWithDelimitersMapper,
) {
    dialog(
        route = "$searchLeaveShareFolderDialog/{$nodeListHandle}",
        arguments = listOf(
            navArgument(nodeListHandle) { type = NavType.StringType }
        )
    ) {
        val handle = it.arguments?.getString(nodeListHandle)
        handle?.let {
            runCatching {
                stringWithDelimitersMapper<Long>(it)
            }.onSuccess { nodeHandle ->
                LeaveShareDialog(
                    handles = nodeHandle,
                    onDismiss = {
                        navHostController.navigateUp()
                    }
                )
            }.onFailure { throwable ->
                Timber.e(throwable)
            }
        }
    }
}

internal const val searchLeaveShareFolderDialog = "search/leave_share_folder"