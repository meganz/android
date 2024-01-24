package mega.privacy.android.app.presentation.search.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.bottomSheet
import mega.privacy.android.app.presentation.node.NodeOptionsBottomSheetViewModel
import mega.privacy.android.app.presentation.node.label.ChangeLabelBottomSheetContent


@OptIn(ExperimentalMaterialNavigationApi::class)
internal fun NavGraphBuilder.changeLabelBottomSheetNavigation(
    navHostController: NavHostController,
    nodeOptionsBottomSheetViewModel: NodeOptionsBottomSheetViewModel,
) {
    bottomSheet(
        route = changeLabelBottomSheetRoute
    ) {
        val nodeOptionsState by nodeOptionsBottomSheetViewModel.state.collectAsStateWithLifecycle()
        nodeOptionsState.node?.let {
            ChangeLabelBottomSheetContent(
                node = it
            ) {
                navHostController.navigateUp()
            }
        }
    }
}

internal const val changeLabelBottomSheetRoute =
    "search/node_bottom_sheet/change_label_bottom_sheet"