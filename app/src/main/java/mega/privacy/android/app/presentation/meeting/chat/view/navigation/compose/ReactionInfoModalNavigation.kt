package mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.bottomSheet
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.ReactionsInfoBottomSheet

@OptIn(ExperimentalMaterialNavigationApi::class)
internal fun NavGraphBuilder.reactionInfoModal(
    navController: NavHostController,
    scaffoldState: ScaffoldState,
    closeBottomSheets: () -> Unit,
) {
    bottomSheet(route = "reactionInfoModal") { backStackEntry ->
        val viewModel = backStackEntry.sharedViewModel<ChatViewModel>(navController)
        val uiState by viewModel.state.collectAsStateWithLifecycle()

        ReactionsInfoBottomSheet(
            selectedReaction = uiState.selectedReaction,
            reactions = uiState.reactionList,
            getDetailsInReactionList = viewModel::getUserInfoIntoReactionList,
            onUserClicked = { closeBottomSheets() },
            uiState = uiState,
            scaffoldState = scaffoldState,
            getUser = viewModel::getUser,
        )
    }
}

internal fun NavHostController.navigateToReactionInfoModal(navOptions: NavOptions? = null) {
    navigate("reactionInfoModal", navOptions)
}