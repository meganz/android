package mega.privacy.android.app.presentation.search.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import mega.privacy.android.app.presentation.node.dialogs.sharefolder.access.ShareFolderAccessDialog
import mega.privacy.android.app.presentation.search.nodeListHandle
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import timber.log.Timber

internal fun NavGraphBuilder.shareFolderAccessDialogNavigation(
    navHostController: NavHostController,
    listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper,
) {
    dialog(
        route = "$shareFolderAccessDialog/{$contactDataArgumentId}/{$isShareFromBackups}/{$nodeListHandle}",
        arguments = listOf(
            navArgument(contactDataArgumentId) {
                type = NavType.StringType
            },
            navArgument(isShareFromBackups) {
                type = NavType.BoolType
            },
            navArgument(nodeListHandle) {
                type = NavType.StringType
            }
        )
    ) {
        val contactArray =
            it.arguments?.getString(contactDataArgumentId) ?: ""
        val isFromBackups = it.arguments?.getBoolean(isShareFromBackups) ?: false
        val handles = it.arguments?.getString(nodeListHandle)

        handles?.let {
            runCatching {
                listToStringWithDelimitersMapper<Long>(it)
            }.onSuccess { handleList ->
                val contactsData = contactArray.split(contactArraySeparator)
                ShareFolderAccessDialog(
                    handles = handleList,
                    contactData = contactsData,
                    isFromBackups = isFromBackups,
                    onDismiss = {
                        navHostController.navigateUp()
                    })
            }
                .onFailure { exception -> Timber.e(exception) }
        }
    }
}

internal const val shareFolderAccessDialog = "search/shareFolder/shareFolderAccess"
internal const val contactDataArgumentId = "contactDataArgumentId"
internal const val isShareFromBackups = "isShareFromBackups"
internal const val contactArraySeparator = ","