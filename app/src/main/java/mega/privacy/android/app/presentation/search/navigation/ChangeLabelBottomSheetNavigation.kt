package mega.privacy.android.app.presentation.search.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.bottomSheet
import mega.privacy.android.app.presentation.node.label.ChangeLabelBottomSheetContent


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
        ChangeLabelBottomSheetContent {
            navHostController.navigateUp()
        }
    }
}

internal const val changeLabelBottomSheetRoute =
    "search/node_bottom_sheet/change_label_bottom_sheet"
internal const val changeLabelBottomSheetRouteNodeIdArg = "node_id"