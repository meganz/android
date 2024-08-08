package mega.privacy.android.app.presentation.transfers.view.navigation.compose

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose.sharedViewModel
import mega.privacy.android.app.presentation.transfers.model.TransfersViewModel
import mega.privacy.android.app.presentation.transfers.view.TransfersView

internal const val transfersRoute = "transfers"

@OptIn(ExperimentalMaterialNavigationApi::class)
internal fun NavGraphBuilder.transfersScreen(
    navHostController: NavHostController,
    bottomSheetNavigator: BottomSheetNavigator,
    scaffoldState: ScaffoldState,
    onBackPress: () -> Unit,
    showInProgressModal: () -> Unit,
) {
    composable(
        route = transfersRoute
    ) { backStackEntry ->
        val viewModel =
            backStackEntry.sharedViewModel<TransfersViewModel>(navController = navHostController)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        TransfersView(
            bottomSheetNavigator = bottomSheetNavigator,
            scaffoldState = scaffoldState,
            onBackPress = onBackPress,
            uiState = uiState,
            onPlayPauseTransfer = viewModel::playOrPauseTransfer,
            onResumeTransfers = viewModel::resumeTransfersQueue,
            onPauseTransfers = viewModel::pauseTransfersQueue,
            onMoreInProgressActions = showInProgressModal,
        )
    }
}