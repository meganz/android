package mega.privacy.android.app.presentation.contact.navigation

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.contract.SelectFileToShareActivityContract
import mega.privacy.android.app.main.megachat.chat.explorer.ChatExplorerActivity
import mega.privacy.android.app.presentation.contactinfo.ContactInfoStorageStateViewModel
import mega.privacy.android.app.presentation.transfers.attach.NodeAttachmentView
import mega.privacy.android.app.presentation.transfers.attach.NodeAttachmentViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.OverDiskQuotaPaywallWarningNavKey
import mega.privacy.android.navigation.destination.ShowChatMessagesNavKey
import nz.mega.sdk.MegaChatApiJava

/**
 * Hosts the app-module collaborators the send file and share contact actions need: the file and
 * chat selection activity launchers, the [NodeAttachmentView] driving the attach flow, and the
 * over disk quota paywall pre-check. [content] renders the Compose contact screen with the two
 * actions wired to those launchers.
 */
@Composable
internal fun ContactAttachmentHost(
    navigationHandler: NavigationHandler,
    content: @Composable (
        onSendFileToChat: (contactEmail: String) -> Unit,
        onShareContact: (contactEmail: String, userHandle: Long) -> Unit,
    ) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackBarHostState.current
    val storageStateViewModel = hiltViewModel<ContactInfoStorageStateViewModel>()
    val nodeAttachmentViewModel = hiltViewModel<NodeAttachmentViewModel>()
    var attachToContactEmail by rememberSaveable { mutableStateOf<String?>(null) }
    val selectFileLauncher = rememberLauncherForActivityResult(
        SelectFileToShareActivityContract()
    ) { result ->
        val nodeHandles = result?.getLongArrayExtra(Constants.NODE_HANDLES)
        val contactEmail = attachToContactEmail
        if (nodeHandles != null && nodeHandles.isNotEmpty() && !contactEmail.isNullOrEmpty()) {
            nodeAttachmentViewModel.attachNodesToChatByEmail(
                nodeIds = nodeHandles.map { NodeId(it) },
                email = contactEmail,
            )
        }
    }
    val shareContactLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val contactEmail = attachToContactEmail
        if (result.resultCode == Activity.RESULT_OK && data != null &&
            !contactEmail.isNullOrEmpty()
        ) {
            nodeAttachmentViewModel.attachContactToChat(
                email = contactEmail,
                chatIds = data.getLongArrayExtra(Constants.SELECTED_CHATS) ?: longArrayOf(),
                userHandles = data.getLongArrayExtra(Constants.SELECTED_USERS) ?: longArrayOf(),
            )
        }
    }
    NodeAttachmentView(
        viewModel = nodeAttachmentViewModel,
        showMessage = { message, chatId ->
            coroutineScope.launch {
                val result = snackbarHostState?.showAutoDurationSnackbar(
                    message = message,
                    actionLabel = context.getString(R.string.action_see)
                        .takeIf { chatId != MegaChatApiJava.MEGACHAT_INVALID_HANDLE },
                )
                if (result == SnackbarResult.ActionPerformed) {
                    navigationHandler.navigate(ShowChatMessagesNavKey(chatId))
                }
            }
        },
    )
    content(
        { contactEmail ->
            if (storageStateViewModel.getStorageState() == StorageState.PayWall) {
                navigationHandler.navigate(OverDiskQuotaPaywallWarningNavKey)
            } else {
                attachToContactEmail = contactEmail
                selectFileLauncher.launch(contactEmail)
            }
        },
        { contactEmail, userHandle ->
            if (storageStateViewModel.getStorageState() == StorageState.PayWall) {
                navigationHandler.navigate(OverDiskQuotaPaywallWarningNavKey)
            } else {
                attachToContactEmail = contactEmail
                shareContactLauncher.launch(
                    Intent(context, ChatExplorerActivity::class.java)
                        .putExtra(Constants.USER_HANDLES, longArrayOf(userHandle))
                )
            }
        },
    )
}
