package mega.privacy.android.app.presentation.transfers.view.navigation.compose

import androidx.compose.material.ScaffoldState
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import mega.privacy.android.app.presentation.transfers.view.IN_PROGRESS_TAB_INDEX

internal const val tabIndexArg = "tabIndex"
internal const val transfersNavigationRoutePattern =
    "transfers/{$tabIndexArg}"

internal fun transfersNavigationRoutePattern(tabIndex: Int) =
    "transfers/$tabIndex"

@OptIn(ExperimentalMaterialNavigationApi::class)
internal fun NavGraphBuilder.transfersViewNavigationGraph(
    bottomSheetNavigator: BottomSheetNavigator,
    navHostController: NavHostController,
    scaffoldState: ScaffoldState,
    onBackPress: () -> Unit,
) {
    navigation(
        startDestination = transfersRoute,
        route = transfersNavigationRoutePattern,
        arguments = listOf(
            navArgument(tabIndexArg) { NavType.IntType })
    ) {
        transfersScreen(
            navHostController = navHostController,
            bottomSheetNavigator = bottomSheetNavigator,
            scaffoldState = scaffoldState,
            onBackPress = onBackPress,
            showInProgressModal = navHostController::navigateToInProgressActionsModal,
        )

        inProgressActionsModal(navHostController = navHostController)

        cancelAllTransfersDialog(navHostController = navHostController)
    }
}

internal fun NavHostController.navigateToTransfersViewGraph(
    tabIndex: Int = IN_PROGRESS_TAB_INDEX,
    navOptions: NavOptions? = null,
) {
    navigate(transfersNavigationRoutePattern(tabIndex), navOptions)
}

internal class TransfersArgs(val tabIndex: Int) {
    constructor(savedStateHandle: SavedStateHandle) :
            this(tabIndex = checkNotNull(savedStateHandle.get<String>(tabIndexArg)?.toIntOrNull()))
}