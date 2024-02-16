package mega.privacy.android.app.presentation.search.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.bottomSheet
import mega.privacy.android.app.presentation.node.label.ChangeLabelBottomSheetContent
import mega.privacy.android.domain.entity.node.NodeId


@OptIn(ExperimentalMaterialNavigationApi::class)
internal fun NavGraphBuilder.changeLabelBottomSheetNavigation(
    navHostController: NavHostController,
) {
    bottomSheet(
        route = changeLabelBottomSheetRoute.plus("/{${changeLabelBottomSheetRouteNodeIdArg}}"),
        arguments = listOf(
            navArgument(nodeBottomSheetRouteNodeIdArg) {
                type = NavType.LongType
            },
        )
    ) {
        val nodeId = it.arguments?.getLong(changeLabelBottomSheetRouteNodeIdArg)
        nodeId?.let { nodeHandle ->
            ChangeLabelBottomSheetContent(
                nodeId = NodeId(nodeHandle)
            ) {
                navHostController.navigateUp()
            }
        }
    }
}

internal const val changeLabelBottomSheetRouteNodeIdArg = "node_id"
internal const val changeLabelBottomSheetRoute =
    "search/node_bottom_sheet/change_label_bottom_sheet"