package mega.privacy.android.app.presentation.meeting.chat.model.messages

import android.content.Context
import android.content.Intent
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity
import mega.privacy.android.app.presentation.imagepreview.fetcher.ChatImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.message.attachment.NodeAttachmentMessageView
import mega.privacy.android.app.presentation.meeting.chat.view.message.attachment.NodeAttachmentMessageViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose.sharedViewModel
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.node.FileNodeContent
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.chat.ChatFile
import mega.privacy.android.navigation.OpenTextEditorParams
import mega.privacy.android.navigation.megaNavigator
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.shared.original.core.ui.controls.layouts.LocalSnackBarHostStateOriginal
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import timber.log.Timber
import java.io.File

/**
 * Node attachment Ui message
 *
 * @param message [NodeAttachmentMessageView]
 */
data class NodeAttachmentUiMessage(
    override val message: NodeAttachmentMessage,
    override val reactions: List<UIReaction>,
) : AvatarMessage() {

    @Composable
    override fun ContentComposable(
        interactionEnabled: Boolean,
        onLongClick: () -> Unit,
        initialiseModifier: (onClick: () -> Unit) -> Modifier,
        navHostController: NavHostController,
    ) {
        val viewModel: NodeAttachmentMessageViewModel = hiltViewModel()
        val chatViewModel: ChatViewModel =
            navHostController.currentBackStackEntry?.sharedViewModel<ChatViewModel>(navController = navHostController)
                ?: throw IllegalStateException("ChatViewModel not found in navigation graph")
        val uiState by viewModel.updateAndGetUiStateFlow(message).collectAsStateWithLifecycle()
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current
        val snackbarHostState = LocalSnackBarHostStateOriginal.current
        val onClick: () -> Unit = {
            coroutineScope.launch {
                if (!message.exists) {
                    snackbarHostState?.showAutoDurationSnackbar(context.getString(R.string.error_file_not_available))
                    return@launch
                }
                runCatching {
                    viewModel.handleFileNode(message)
                }.onSuccess { content ->
                    when (content) {
                        is FileNodeContent.ImageForChat -> {
                            openImagePreview(
                                context,
                                message.chatId,
                                content.allAttachmentMessageIds.toLongArray(),
                                message.fileNode.id.longValue
                            )
                        }

                        is FileNodeContent.AudioOrVideo -> openVideoOrAudioFile(
                            context,
                            message,
                            content.uri,
                            coroutineScope
                        )

                        is FileNodeContent.Other -> openOtherFiles(
                            content.localFile,
                            viewModel,
                            message,
                            context,
                            snackbarHostState,
                            chatViewModel,
                            coroutineScope
                        )

                        is FileNodeContent.Pdf -> openPdfActivity(
                            context = context,
                            message = message,
                            chatId = message.chatId,
                            content = content.uri
                        )

                        FileNodeContent.TextContent -> handleTextEditor(
                            context,
                            message.msgId,
                            message.chatId
                        )

                        FileNodeContent.ImageForNode -> {
                            Timber.i("FileNodeContent.ImageForNode is not handled in NodeAttachmentUiMessage")
                        }

                        is FileNodeContent.UrlContent -> {
                            Timber.i("FileNodeContent.UrlContent is not handled in NodeAttachmentUiMessage")
                        }
                    }
                }.onFailure {
                    Timber.e(it, "Failed to handle file node")
                }
            }
        }

        NodeAttachmentMessageView(
            message = message,
            modifier = initialiseModifier(onClick),
            uiState = uiState,
        )
    }

    private fun openVideoOrAudioFile(
        context: Context,
        message: NodeAttachmentMessage,
        uri: NodeContentUri,
        coroutineScope: CoroutineScope,
    ) {
        coroutineScope.launch {
            runCatching {
                val fileNode = message.fileNode
                context.megaNavigator.openMediaPlayerActivityFromChat(
                    context = context,
                    contentUri = uri,
                    fileNode = fileNode,
                    message = message,
                )
            }.onFailure {
                Timber.e(it, "No activity found to open file")
            }
        }
    }

    private fun openPdfActivity(
        context: Context,
        message: NodeAttachmentMessage,
        chatId: Long,
        content: NodeContentUri,
    ) {
        context.megaNavigator.openPdfViewerFromChat(
            context = context,
            content = content,
            nodeHandle = message.fileNode.id.longValue,
            chatId = chatId,
            messageId = message.msgId,
            mimeType = PdfFileTypeInfo.mimeType,
            title = message.fileNode.name,
        )
    }

    private fun openImagePreview(
        context: Context,
        chatId: Long,
        messageIds: LongArray,
        nodeId: Long,
    ) {
        val intent = ImagePreviewActivity.createSecondaryIntent(
            context = context,
            imageSource = ImagePreviewFetcherSource.CHAT,
            menuOptionsSource = ImagePreviewMenuSource.CHAT,
            anchorImageNodeId = nodeId,
            params = mapOf(
                ChatImageNodeFetcher.CHAT_ROOM_ID to chatId,
                ChatImageNodeFetcher.MESSAGE_IDS to messageIds
            ),
        )
        context.startActivity(intent)
    }

    private fun handleTextEditor(context: Context, msgId: Long, chatId: Long) {
        context.megaNavigator.openTextEditor(
            context = context,
            params = OpenTextEditorParams.Chat(chatId = chatId, messageId = msgId),
        )
    }

    private suspend fun openOtherFiles(
        localFile: File?,
        viewModel: NodeAttachmentMessageViewModel,
        message: NodeAttachmentMessage,
        context: Context,
        snackbarHostState: SnackbarHostState?,
        chatViewModel: ChatViewModel,
        coroutineScope: CoroutineScope,
    ) {
        val fileNode = message.fileNode
        if (localFile != null) {
            if (fileNode.type is ZipFileTypeInfo) {
                openZipFile(
                    context = context,
                    localFile = localFile,
                    fileNode = fileNode,
                    snackbarHostState = snackbarHostState,
                    coroutineScope = coroutineScope
                )
            } else {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    viewModel.applyNodeContentUri(
                        intent = this,
                        content = NodeContentUri.LocalContentUri(localFile),
                        mimeType = fileNode.type.mimeType,
                        isSupported = false
                    )
                }
                runCatching {
                    context.startActivity(intent)
                }.onFailure {
                    // no activity found to open file, just show download snackbar
                    snackbarHostState?.showAutoDurationSnackbar(context.getString(R.string.general_already_downloaded))
                }
            }
        } else {
            chatViewModel.onDownloadForPreviewChatNode(file = fileNode, isOpenWith = false)
        }
    }

    private fun openZipFile(
        context: Context,
        localFile: File,
        fileNode: ChatFile,
        snackbarHostState: SnackbarHostState?,
        coroutineScope: CoroutineScope,
    ) {
        Timber.d("The file is zip, open in-app.")
        context.megaNavigator.openZipBrowserActivity(
            context = context,
            zipFilePath = localFile.absolutePath,
            nodeHandle = fileNode.id.longValue
        ) {
            coroutineScope.launch {
                snackbarHostState?.showAutoDurationSnackbar(context.getString(R.string.message_zip_format_error))
            }
        }
    }

    override val displayAsMine = message.isMine
    override val shouldDisplayForwardIcon = message.exists
    override val timeSent = message.time
    override val userHandle = message.userHandle
    override val id = message.msgId
}
