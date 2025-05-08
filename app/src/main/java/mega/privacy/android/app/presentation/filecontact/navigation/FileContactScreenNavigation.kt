package mega.privacy.android.app.presentation.filecontact.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.filecontact.ShareRecipientsViewModel
import mega.privacy.android.app.presentation.filecontact.view.FileContactHomeScreen
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.ShareRecipient

@Serializable
class FileContactInfo(
    val folderHandle: Long,
    val folderName: String,
) {
    val folderId: NodeId
        get() = NodeId(folderHandle)
}

internal fun NavGraphBuilder.fileContacts(
    onNavigateBack: () -> Unit,
    onNavigateToInfo: (ShareRecipient) -> Unit,
) {
    composable<FileContactInfo> {
        val viewModel = hiltViewModel<ShareRecipientsViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        FileContactHomeScreen(
            state = state,
            onBackPressed = onNavigateBack,
            removeContacts = viewModel::removeShare,
            shareFolder = viewModel::shareFolder,
            updatePermissions = viewModel::changePermissions,
            shareRemovedEventHandled = viewModel::onShareRemovedEventHandled,
            shareCompletedEventHandled = viewModel::onSharingCompletedEventHandled,
            navigateToInfo = onNavigateToInfo,
        )
    }
}