package mega.privacy.android.app.presentation.search.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.bottomSheet
import mega.privacy.android.app.presentation.node.NodeActionHandler
import mega.privacy.android.app.presentation.node.view.NodeOptionsBottomSheetContent
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper

internal const val nodeBottomSheetRoute = "search/node_bottom_sheet"
internal const val nodeBottomSheetRouteNodeIdArg = "node_id"

@OptIn(ExperimentalMaterialNavigationApi::class)
internal fun NavGraphBuilder.nodeBottomSheetNavigation(
    nodeActionHandler: NodeActionHandler,
    navHostController: NavHostController,
    fileTypeIconMapper: FileTypeIconMapper
) {
    bottomSheet(
        route = nodeBottomSheetRoute.plus("/{${nodeBottomSheetRouteNodeIdArg}}"),
        arguments = listOf(
            navArgument(nodeBottomSheetRouteNodeIdArg) {
                type = NavType.LongType
            },
        ),
    ) {
        it.arguments?.getLong(nodeBottomSheetRouteNodeIdArg)?.let { nodeId ->
            NodeOptionsBottomSheetContent(
                handler = nodeActionHandler,
                navHostController = navHostController,
                nodeId = nodeId,
                onDismiss = {
                    navHostController.navigateUp()
                },
                fileTypeIconMapper = fileTypeIconMapper
            )
        }
    }
}