package mega.privacy.android.app.presentation.meeting.chat.view.actions

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.app.namecollision.NameCollisionActivity
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.presentation.meeting.chat.view.message.attachment.NodeAttachmentMessageViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openFileExplorerActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openNameCollisionActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.ui.controls.layouts.LocalSnackBarHostState
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

internal class ImportMessageAction(
    private val launchFolderPicker: (Context, ActivityResultLauncher<Intent>) -> Unit = ::openFileExplorerActivity,
    private val launchNameCollisionActivity: (Context, List<NameCollision>, ActivityResultLauncher<Intent>) -> Unit = ::openNameCollisionActivity,
) : MessageAction(
    text = R.string.general_import,
    icon = R.drawable.ic_cloud_upload_medium_regular_outline,
    testTag = "import_node",
) {
    override fun appliesTo(messages: Set<TypedMessage>) = messages.isNotEmpty()
            && messages.all { it is NodeAttachmentMessage }

    @Composable
    override fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit) {
        val snackbarHostState = LocalSnackBarHostState.current
        val error = stringResource(id = R.string.import_success_error)
        var handleWhereToImport by rememberSaveable { mutableStateOf<Long?>(null) }
        var collisionsResult by rememberSaveable { mutableStateOf<String?>(null) }
        val folderPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleWhereToImport =
                result.data?.getLongExtra(Constants.INTENT_EXTRA_KEY_IMPORT_TO, -1)
                    ?.takeIf { it != -1L }

            if (handleWhereToImport == null) {
                onHandled()
            }
        }
        val collisionsLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            collisionsResult = result.data?.getStringExtra(NameCollisionActivity.MESSAGE_RESULT)
                .takeIf { !it.isNullOrEmpty() }

            if (collisionsResult.isNullOrEmpty()) {
                onHandled()
            }
        }
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            launchFolderPicker(
                context,
                folderPickerLauncher
            )
        }
        handleWhereToImport?.let {
            val viewModel = hiltViewModel<NodeAttachmentMessageViewModel>()
            LaunchedEffect(Unit) {
                runCatching {
                    viewModel.importNodes(
                        messages.map { (it as NodeAttachmentMessage).fileNode },
                        it
                    )
                }.onSuccess { result ->
                    with(result) {
                        if (copySuccess + copyError > 0) {
                            viewModel.getCopyNodesResult(result)
                        } else {
                            null
                        }?.let {
                            snackbarHostState?.showSnackbar(it)
                        }
                        if (conflictNodes.isNotEmpty()) {
                            launchNameCollisionActivity(
                                context,
                                conflictNodes.map { collision ->
                                    messages.first { (it as NodeAttachmentMessage).fileNode.id.longValue == collision.nodeHandle }
                                        .let { message ->
                                            with(collision) {
                                                NameCollision.Import(
                                                    collisionHandle = collisionHandle,
                                                    nodeHandle = nodeHandle,
                                                    chatId = message.chatId,
                                                    messageId = message.msgId,
                                                    name = collision.name,
                                                    size = collision.size,
                                                    lastModified = collision.lastModified,
                                                    parentHandle = collision.parentHandle,
                                                )
                                            }
                                        }
                                },
                                collisionsLauncher,
                            )
                        } else {
                            onHandled()
                        }
                    }
                }.onFailure {
                    snackbarHostState?.showSnackbar(error)
                    onHandled()
                }
            }
        }
        collisionsResult?.let {
            LaunchedEffect(Unit) {
                collisionsResult?.let { snackbarHostState?.showSnackbar(it) }
                onHandled()
            }
        }
    }
}