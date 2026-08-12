package mega.privacy.android.app.presentation.contactinfo.navigation

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.contract.SelectFileToShareActivityContract
import mega.privacy.android.app.main.ContactFileListActivity
import mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity
import mega.privacy.android.app.main.megachat.chat.explorer.ChatExplorerActivity
import mega.privacy.android.app.presentation.contactinfo.ContactInfoActivity
import mega.privacy.android.app.presentation.contactinfo.ContactInfoStorageStateViewModel
import mega.privacy.android.app.presentation.transfers.attach.NodeAttachmentView
import mega.privacy.android.app.presentation.transfers.attach.NodeAttachmentViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.contact.info.navigation.ContactInfoEntry
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.ContactInfoNavKey
import mega.privacy.android.navigation.destination.ContactSharedFoldersNavKey
import mega.privacy.android.navigation.destination.NodeAttachmentHistoryNavKey
import mega.privacy.android.navigation.destination.OverDiskQuotaPaywallWarningNavKey
import mega.privacy.android.navigation.destination.ShowChatMessagesNavKey
import nz.mega.sdk.MegaChatApiJava

/**
 * Registers the [ContactInfoNavKey] destination. Behind [ApiFeatures.ContactComposeFeature] either
 * renders the Compose [ContactInfoEntry] contact info screen (flag on) or launches the legacy
 * [ContactInfoActivity] and pops itself (flag off).
 */
fun EntryProviderScope<NavKey>.contactInfoDestination(navigationHandler: NavigationHandler) {
    entry<ContactInfoNavKey> { navKey ->
        FeatureFlagGate(
            feature = ApiFeatures.ContactComposeFeature,
            disabled = {
                val context = LocalContext.current
                LaunchedEffect(Unit) {
                    val intent = Intent(context, ContactInfoActivity::class.java)
                    navKey.email?.let { intent.putExtra(Constants.NAME, it) }
                    navKey.chatId?.let { intent.putExtra(Constants.HANDLE, it) }
                    context.startActivity(intent)
                    navigationHandler.back()
                }
            },
            enabled = {
                ComposeContactInfo(
                    navigationHandler = navigationHandler,
                    navKey = navKey,
                )
            },
        )
    }
}

/**
 * Hosts [ContactInfoEntry] together with the app-module collaborators the send file and share
 * contact actions need: the file/chat selection activity launchers, the [NodeAttachmentView]
 * driving the attach flow, and the over disk quota paywall pre-check.
 */
@Composable
private fun ComposeContactInfo(
    navigationHandler: NavigationHandler,
    navKey: ContactInfoNavKey,
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
    ContactInfoEntry(
        navigationHandler = navigationHandler,
        email = navKey.email,
        chatId = navKey.chatId,
        onSendFileToChat = { contactEmail ->
            if (storageStateViewModel.getStorageState() == StorageState.PayWall) {
                navigationHandler.navigate(OverDiskQuotaPaywallWarningNavKey)
            } else {
                attachToContactEmail = contactEmail
                selectFileLauncher.launch(contactEmail)
            }
        },
        onShareContact = { contactEmail, userHandle ->
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

/**
 * Registers the [NodeAttachmentHistoryNavKey] destination, bridging to the legacy
 * [NodeAttachmentHistoryActivity] that lists the files shared in a chat.
 */
fun EntryProviderScope<NavKey>.nodeAttachmentHistoryDestination(
    navigationHandler: NavigationHandler,
) {
    entry<NodeAttachmentHistoryNavKey>(metadata = transparentMetadata()) { navKey ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            context.startActivity(
                Intent(context, NodeAttachmentHistoryActivity::class.java)
                    .putExtra("chatId", navKey.chatId)
            )
            navigationHandler.back()
        }
    }
}

/**
 * Registers the [ContactSharedFoldersNavKey] destination, bridging to the legacy
 * [ContactFileListActivity] that lists the folders a contact shares with the user.
 */
fun EntryProviderScope<NavKey>.contactSharedFoldersDestination(
    navigationHandler: NavigationHandler,
) {
    entry<ContactSharedFoldersNavKey>(metadata = transparentMetadata()) { navKey ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            context.startActivity(
                Intent(context, ContactFileListActivity::class.java)
                    .putExtra(Constants.NAME, navKey.email)
            )
            navigationHandler.back()
        }
    }
}
