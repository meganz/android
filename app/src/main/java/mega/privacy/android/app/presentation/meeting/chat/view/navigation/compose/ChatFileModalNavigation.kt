package mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.bottomSheet
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.ChatAttachFileBottomSheet

@OptIn(ExperimentalMaterialNavigationApi::class)
internal fun NavGraphBuilder.chatFileModal(
    navController: NavHostController,
    closeBottomSheets: () -> Unit,
) {
    bottomSheet(route = "fileModal") { backStackEntry ->
        val viewModel = backStackEntry.sharedViewModel<ChatViewModel>(navController)

        ChatAttachFileBottomSheet(
            onAttachFiles = viewModel::onAttachFiles,
            onAttachNodes = viewModel::onAttachNodes,
            hideSheet = closeBottomSheets,
        )
    }
}

internal fun NavHostController.navigateChatFileModal(navOptions: NavOptions? = null) {
    navigate("fileModal", navOptions)
}