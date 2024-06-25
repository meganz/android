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
import mega.privacy.android.domain.entity.node.NodeSourceType

internal const val nodeBottomSheetRoute = "search/node_bottom_sheet"
internal const val nodeBottomSheetRouteNodeIdArg = "node_id"
internal const val nodeBottomSheetRouteSourceTypeArg = "source_type"

@OptIn(ExperimentalMaterialNavigationApi::class)
internal fun NavGraphBuilder.nodeBottomSheetNavigation(
    nodeActionHandler: NodeActionHandler,
    navHostController: NavHostController,
    fileTypeIconMapper: FileTypeIconMapper
) {
    bottomSheet(
        route = nodeBottomSheetRoute.plus("/{${nodeBottomSheetRouteNodeIdArg}}")
            .plus("/{${nodeBottomSheetRouteSourceTypeArg}}"),
        arguments = listOf(
            navArgument(nodeBottomSheetRouteNodeIdArg) {
                type = NavType.LongType
            },
            navArgument(nodeBottomSheetRouteSourceTypeArg) {
                type = NavType.StringType
            }
        ),
    ) {
        val nodeId = it.arguments?.getLong(nodeBottomSheetRouteNodeIdArg)
        val sourceType = it.arguments?.getString(nodeBottomSheetRouteSourceTypeArg)
        if (nodeId == null || sourceType == null) {
            navHostController.navigateUp()
            return@bottomSheet
        }
        NodeOptionsBottomSheetContent(
            handler = nodeActionHandler,
            navHostController = navHostController,
            nodeId = nodeId,
            nodeSourceType = NodeSourceType.valueOf(sourceType),
            onDismiss = {
                navHostController.navigateUp()
            },
            fileTypeIconMapper = fileTypeIconMapper
        )

    }
}