package mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.bottomSheet
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.ChatToolbarBottomSheet

@OptIn(ExperimentalMaterialNavigationApi::class)
internal fun NavGraphBuilder.chatToolbarModal(
    navController: NavHostController,
    scaffoldState: ScaffoldState,
    closeBottomSheets: () -> Unit,
) {
    bottomSheet(route = "toolbarModal") { backStackEntry ->
        val viewModel = backStackEntry.sharedViewModel<ChatViewModel>(navController)
        val uiState by viewModel.state.collectAsStateWithLifecycle()

        ChatToolbarBottomSheet(
            onAttachContacts = viewModel::onAttachContacts,
            uiState = uiState,
            scaffoldState = scaffoldState,
            onPickLocation = {
                navController.navigateChatLocationDialog(
                    navOptions {
                        popUpTo("toolbarModal") {
                            inclusive = true
                        }
                    }
                )
            },
            onSendGiphyMessage = viewModel::onSendGiphyMessage,
            closeModal = navController::popBackStack,
            onAttachFiles = viewModel::onAttachFiles,
            hideSheet = closeBottomSheets,
            navigateToFileModal = navController::navigateChatFileModal,
            isVisible = true,
        )
    }
}

internal fun NavHostController.navigateToChatToolbarModal(navOptions: NavOptions? = null) {
    navigate("toolbarModal", navOptions)
}