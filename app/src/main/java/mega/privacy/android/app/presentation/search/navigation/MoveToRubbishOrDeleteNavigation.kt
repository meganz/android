package mega.privacy.android.app.presentation.search.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import mega.privacy.android.app.presentation.node.dialogs.deletenode.MoveToRubbishOrDeleteNodeDialog
import mega.privacy.android.app.presentation.search.nodeListHandle
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import timber.log.Timber


internal fun NavGraphBuilder.moveToRubbishOrDeleteNavigation(
    navHostController: NavHostController,
    listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper
) {
    dialog(
        route = "$moveToRubbishOrDelete/{$argumentIsInRubbish}/{$nodeListHandle}",
        arguments = listOf(
            navArgument(argumentIsInRubbish) { type = NavType.BoolType },
            navArgument(nodeListHandle) { type = NavType.StringType },
        )
    ) {
        val handles = it.arguments?.getString(nodeListHandle)
        handles?.let { handle ->
            runCatching { listToStringWithDelimitersMapper<Long>(handle) }
                .onSuccess { nodeHandles ->
                    MoveToRubbishOrDeleteNodeDialog(
                        onDismiss = { navHostController.navigateUp() },
                        nodesList = nodeHandles,
                        isNodeInRubbish = it.arguments?.getBoolean(argumentIsInRubbish) ?: false,
                    )
                }
                .onFailure { throwable ->
                    Timber.e(throwable)
                }
        }
    }
}

internal const val moveToRubbishOrDelete = "search/moveToRubbishOrDelete/isInRubbish"
internal const val argumentIsInRubbish = "isInRubbish"