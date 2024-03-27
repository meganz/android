package mega.privacy.android.app.presentation.search.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.bottomSheet
import mega.privacy.android.app.presentation.search.SearchActivityViewModel
import mega.privacy.android.app.presentation.search.view.SearchFilterBottomSheetContent

internal const val searchFilterBottomSheetRoute = "search/filter_bottom_sheet"
internal const val searchFilterBottomSheetRouteArg = "filter"

@OptIn(ExperimentalMaterialNavigationApi::class)
internal fun NavGraphBuilder.searchFilterBottomSheetNavigation(
    navHostController: NavHostController,
    searchActivityViewModel: SearchActivityViewModel,
) {
    bottomSheet(
        route = searchFilterBottomSheetRoute.plus("/{$searchFilterBottomSheetRouteArg}"),
        arguments = listOf(
            navArgument(searchFilterBottomSheetRouteArg) {
                type = NavType.StringType
            },
        ),
    ) {
        it.arguments?.getString(searchFilterBottomSheetRouteArg)?.let { filter ->
            SearchFilterBottomSheetContent(
                filter = filter,
                viewModel = searchActivityViewModel,
                onDismiss = {
                    navHostController.navigateUp()
                },
            )
        }
    }
}

internal const val TYPE = "type"
internal const val DATE_MODIFIED = "date_modified"
internal const val DATE_ADDED = "date_added"
