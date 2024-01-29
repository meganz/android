package mega.privacy.android.app.presentation.search.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import mega.privacy.android.app.presentation.node.NodeOptionsBottomSheetViewModel
import mega.privacy.android.app.presentation.node.dialogs.sharefolder.access.ShareFolderAccessDialog
import mega.privacy.android.app.presentation.search.SearchActivityViewModel

internal fun NavGraphBuilder.shareFolderAccessDialogNavigation(
    navHostController: NavHostController,
    searchActivityViewModel: SearchActivityViewModel,
    nodeOptionsBottomSheetViewModel: NodeOptionsBottomSheetViewModel,
) {
    dialog(
        route = "$shareFolderAccessDialog/{$contactDataArgumentId}/{$isShareFromBackups}",
        arguments = listOf(
            navArgument(contactDataArgumentId) {
                type = NavType.StringType
            },
            navArgument(isShareFromBackups) {
                type = NavType.BoolType
            }
        )
    ) {
        val contactArray =
            it.arguments?.getString(contactDataArgumentId) ?: ""
        val searchStateList by searchActivityViewModel.state.collectAsStateWithLifecycle()
        val nodeBottomSheet by nodeOptionsBottomSheetViewModel.state.collectAsStateWithLifecycle()
        val handleList = nodeBottomSheet.node?.let {
            listOf(it.id.longValue)
        } ?: run {
            searchStateList.selectedNodes.map {
                it.id.longValue
            }
        }
        val isFromBackups = it.arguments?.getBoolean(isShareFromBackups) ?: false
        val contactsData = contactArray.split(contactArraySeparator)
        ShareFolderAccessDialog(
            handles = handleList,
            contactData = contactsData,
            isFromBackups = isFromBackups,
            onDismiss = {
                navHostController.navigateUp()
            })
    }
}

internal const val shareFolderAccessDialog = "search/shareFolder/shareFolderAccess"
internal const val contactDataArgumentId = "contactDataArgumentId"
internal const val isShareFromBackups = "isShareFromBackups"
internal const val contactArraySeparator = ","