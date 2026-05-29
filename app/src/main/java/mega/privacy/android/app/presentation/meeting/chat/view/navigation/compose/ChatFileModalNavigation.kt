package mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose

import androidx.compose.material.navigation.bottomSheet
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.ChatAttachFileBottomSheet

internal fun NavGraphBuilder.chatFileModal(
    navController: NavHostController,
    closeBottomSheets: () -> Unit,
    onNavigate: (NavKey) -> Unit = {},
) {
    bottomSheet(route = "fileModal") { backStackEntry ->
        val viewModel = backStackEntry.sharedViewModel<ChatViewModel>(navController)
        val uiState by viewModel.state.collectAsStateWithLifecycle()

        ChatAttachFileBottomSheet(
            chatId = uiState.chatId,
            isCloudExplorerAvailable = uiState.isCloudExplorerAvailable,
            onAttachFiles = viewModel::onAttachFiles,
            onAttachNodes = viewModel::onAttachNodes,
            onNavigate = onNavigate,
            hideSheet = closeBottomSheets,
        )
    }
}

internal fun NavHostController.navigateChatFileModal(navOptions: NavOptions? = null) {
    navigate("fileModal", navOptions)
}
