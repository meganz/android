package mega.privacy.android.app.nav

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.getLink.GetLinkActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.mediaplayer.AudioPlayerActivity
import mega.privacy.android.app.mediaplayer.VideoPlayerComposeActivity
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.presentation.contact.authenticitycredendials.AuthenticityCredentialsActivity
import mega.privacy.android.app.presentation.contact.invite.InviteContactActivity
import mega.privacy.android.app.presentation.contact.invite.InviteContactViewModel
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsActivity
import mega.privacy.android.app.presentation.filecontact.FileContactListActivity
import mega.privacy.android.app.presentation.filecontact.FileContactListComposeActivity
import mega.privacy.android.app.presentation.fileinfo.FileInfoActivity
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity
import mega.privacy.android.app.presentation.imagepreview.fetcher.CloudDriveImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.RubbishBinImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.SharedItemsImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.meeting.chat.ChatHostActivity
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_ACTION
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_LINK
import mega.privacy.android.app.presentation.meeting.managechathistory.view.screen.ManageChatHistoryActivity
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity
import mega.privacy.android.app.presentation.photos.mediadiscovery.MediaDiscoveryActivity
import mega.privacy.android.app.presentation.search.SearchActivity
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsActivity
import mega.privacy.android.app.presentation.settings.compose.navigation.SettingsNavigatorImpl
import mega.privacy.android.app.presentation.transfers.EXTRA_TAB
import mega.privacy.android.app.presentation.transfers.TransfersActivity
import mega.privacy.android.app.presentation.zipbrowser.ZipBrowserComposeActivity
import mega.privacy.android.app.textEditor.TextEditorActivity
import mega.privacy.android.app.textEditor.TextEditorViewModel
import mega.privacy.android.app.uploadFolder.UploadFolderActivity
import mega.privacy.android.app.uploadFolder.UploadFolderType
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.ACTION_OPEN_DEVICE_CENTER
import mega.privacy.android.app.utils.Constants.ACTION_OPEN_SYNC_MEGA_FOLDER
import mega.privacy.android.app.utils.Constants.EXTRA_HANDLE_ZIP
import mega.privacy.android.app.utils.Constants.EXTRA_PATH_ZIP
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_FOLDER_LINK
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_PLAYLIST
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MEDIA_QUEUE_TITLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MSG_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PATH
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PLACEHOLDER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_VIDEO_ADD_TO_ALBUM
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_VIDEO_COLLECTION_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_VIDEO_COLLECTION_TITLE
import mega.privacy.android.app.utils.Constants.NODE_HANDLES
import mega.privacy.android.core.nodecomponents.mapper.NodeContentUriIntentMapper
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.GetFileTypeInfoByNameUseCase
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase
import mega.privacy.android.domain.usecase.file.GetFileTypeInfoUseCase
import mega.privacy.android.feature.payment.presentation.upgrade.ChooseAccountActivity
import mega.privacy.android.feature.sync.navigation.SyncNewFolder
import mega.privacy.android.feature.sync.ui.SyncHostActivity
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.payment.UpgradeAccountSource
import mega.privacy.android.navigation.settings.SettingsNavigator
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Mega navigator impl
 * Centralized navigation logic instead of call navigator separately
 * We will replace with navigation component in the future
 */
internal class MegaNavigatorImpl @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val nodeContentUriIntentMapper: NodeContentUriIntentMapper,
    private val getFileTypeInfoUseCase: GetFileTypeInfoUseCase,
    private val getFileTypeInfoByNameUseCase: GetFileTypeInfoByNameUseCase,
    private val settingsNavigator: SettingsNavigatorImpl,
    private val getDomainNameUseCase: GetDomainNameUseCase,
) : MegaNavigator,
    AppNavigatorImpl, SettingsNavigator by settingsNavigator {

    override fun openSettingsCameraUploads(context: Context) {
        context.startActivity(Intent(context, SettingsCameraUploadsActivity::class.java))
    }

    override fun openChat(
        context: Context,
        chatId: Long,
        action: String?,
        link: String?,
        text: String?,
        messageId: Long?,
        isOverQuota: Int?,
        flags: Int,
    ) {
        val intent = getChatActivityIntent(
            context = context,
            action = action,
            link = link,
            text = text,
            chatId = chatId,
            messageId = messageId,
            isOverQuota = isOverQuota,
            flags = flags
        )
        context.startActivity(intent)
    }

    override fun openUpgradeAccount(context: Context, source: UpgradeAccountSource) {
        ChooseAccountActivity.navigateToUpgradeAccount(
            context = context,
            source = source
        )
    }

    private fun getChatActivityIntent(
        context: Context,
        action: String?,
        link: String?,
        text: String?,
        chatId: Long,
        messageId: Long?,
        isOverQuota: Int?,
        flags: Int,
    ): Intent {
        val intent = Intent(context, ChatHostActivity::class.java).apply {
            this.action = action
            putExtra(EXTRA_ACTION, action)
            text?.let { putExtra(Constants.SHOW_SNACKBAR, text) }
            putExtra(Constants.CHAT_ID, chatId)
            messageId?.let { putExtra("ID_MSG", messageId) }
            isOverQuota?.let { putExtra("IS_OVERQUOTA", isOverQuota) }
            if (flags > 0) setFlags(flags)
        }
        link?.let {
            intent.putExtra(EXTRA_LINK, it)
        }
        return intent
    }

    override fun openManageChatHistoryActivity(
        context: Context,
        chatId: Long,
        email: String?,
    ) {
        applicationScope.launch {
            val intent = Intent(context, ManageChatHistoryActivity::class.java).apply {
                putExtra(Constants.CHAT_ID, chatId)
                email?.let { putExtra(Constants.EMAIL, it) }
            }
            context.startActivity(intent)
        }
    }

    override fun openZipBrowserActivity(
        context: Context,
        zipFilePath: String,
        nodeHandle: Long?,
        onError: () -> Unit,
    ) {
        applicationScope.launch {
            if (ZipBrowserComposeActivity.zipFileFormatCheck(context, zipFilePath)) {
                context.startActivity(Intent(context, ZipBrowserComposeActivity::class.java).apply {
                    putExtra(EXTRA_PATH_ZIP, zipFilePath)
                    putExtra(EXTRA_HANDLE_ZIP, nodeHandle)
                })
            } else {
                onError()
            }
        }
    }

    override fun openInviteContactActivity(context: Context, isFromAchievement: Boolean) {
        applicationScope.launch {
            val intent = Intent(context, InviteContactActivity::class.java).apply {
                putExtra(InviteContactViewModel.KEY_FROM, isFromAchievement)
            }
            context.startActivity(intent)
        }
    }

    override fun openTransfers(context: Context, tab: Int) {
        Intent(context, TransfersActivity::class.java).apply {
            putExtra(EXTRA_TAB, tab)
        }.let(context::startActivity)
    }

    override suspend fun openMediaPlayerActivityByFileNode(
        context: Context,
        contentUri: NodeContentUri,
        fileNode: TypedFileNode,
        viewType: Int?,
        sortOrder: SortOrder,
        isFolderLink: Boolean,
        isMediaQueueAvailable: Boolean,
        searchedItems: List<Long>?,
        mediaQueueTitle: String?,
        collectionTitle: String?,
        collectionId: Long?,
        enableAddToAlbum: Boolean?,
    ) {
        manageMediaIntent(
            context = context,
            contentUri = contentUri,
            fileTypeInfo = fileNode.type,
            sortOrder = sortOrder,
            viewType = viewType ?: NodeSourceTypeInt.FILE_BROWSER_ADAPTER,
            name = fileNode.name,
            handle = fileNode.id.longValue,
            parentHandle = fileNode.parentId.longValue,
            isFolderLink = isFolderLink,
            isMediaQueueAvailable = isMediaQueueAvailable,
            searchedItems = searchedItems,
            mediaQueueTitle = mediaQueueTitle,
            collectionTitle = collectionTitle,
            collectionId = collectionId,
            enableAddToAlbum = enableAddToAlbum ?: run {
                viewType in listOf(
                    NodeSourceTypeInt.FILE_BROWSER_ADAPTER,
                    NodeSourceTypeInt.OUTGOING_SHARES_ADAPTER,
                )
            },
        )
    }

    private suspend fun manageMediaIntent(
        context: Context,
        contentUri: NodeContentUri,
        fileTypeInfo: FileTypeInfo,
        sortOrder: SortOrder,
        name: String,
        handle: Long,
        parentHandle: Long,
        isFolderLink: Boolean,
        isMediaQueueAvailable: Boolean,
        viewType: Int? = null,
        path: String? = null,
        offlineParentId: Int? = null,
        offlineParent: String? = null,
        searchedItems: List<Long>? = null,
        mediaQueueTitle: String? = null,
        nodeHandles: List<Long>? = null,
        collectionTitle: String? = null,
        collectionId: Long? = null,
        enableAddToAlbum: Boolean = false,
    ) {
        val intent = getIntent(context, fileTypeInfo).apply {
            putExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN, sortOrder)
            putExtra(INTENT_EXTRA_KEY_PLACEHOLDER, 0)
            putExtra(INTENT_EXTRA_KEY_FILE_NAME, name)
            putExtra(INTENT_EXTRA_KEY_HANDLE, handle)
            putExtra(INTENT_EXTRA_KEY_IS_FOLDER_LINK, isFolderLink)
            viewType?.let {
                putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, viewType)
            }
            if (isMediaQueueAvailable) {
                putExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, parentHandle)
            }
            putExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, isMediaQueueAvailable)
            path?.let {
                putExtra(INTENT_EXTRA_KEY_PATH, path)
            }
            offlineParentId?.let {
                putExtra(INTENT_EXTRA_KEY_PARENT_ID, it)
            }
            offlineParent?.let {
                putExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY, offlineParent)
            }
            searchedItems?.let {
                putExtra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH, it.toLongArray())
            }
            mediaQueueTitle?.let {
                putExtra(INTENT_EXTRA_KEY_MEDIA_QUEUE_TITLE, it)
            }
            nodeHandles?.let {
                putExtra(NODE_HANDLES, it.toLongArray())
            }
            collectionTitle?.let {
                putExtra(INTENT_EXTRA_KEY_VIDEO_COLLECTION_TITLE, it)
            }
            collectionId?.let {
                putExtra(INTENT_EXTRA_KEY_VIDEO_COLLECTION_ID, it)
            }
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(INTENT_EXTRA_KEY_VIDEO_ADD_TO_ALBUM, enableAddToAlbum)
        }
        val mimeType =
            if (fileTypeInfo.extension == "opus") "audio/*" else fileTypeInfo.mimeType
        nodeContentUriIntentMapper(intent, contentUri, mimeType, fileTypeInfo.isSupported)
        context.startActivity(intent)
    }

    private fun getIntent(context: Context, fileTypeInfo: FileTypeInfo) = when {
        fileTypeInfo.isSupported && fileTypeInfo is VideoFileTypeInfo ->
            Intent(context, VideoPlayerComposeActivity::class.java)

        fileTypeInfo.isSupported && fileTypeInfo is AudioFileTypeInfo ->
            Intent(context, AudioPlayerActivity::class.java)

        else -> Intent(Intent.ACTION_VIEW)
    }

    override suspend fun openMediaPlayerActivityByLocalFile(
        context: Context,
        localFile: File,
        handle: Long,
        viewType: Int?,
        parentId: Long,
        offlineParentId: Int?,
        fileTypeInfo: FileTypeInfo?,
        sortOrder: SortOrder,
        isFolderLink: Boolean,
        isMediaQueueAvailable: Boolean,
        searchedItems: List<Long>?,
        collectionTitle: String?,
        collectionId: Long?,
    ) {
        val contentUri = NodeContentUri.LocalContentUri(localFile)
        val info = fileTypeInfo ?: getFileTypeInfoUseCase(localFile)
        manageMediaIntent(
            context = context,
            contentUri = contentUri,
            fileTypeInfo = info,
            sortOrder = sortOrder,
            viewType = viewType,
            name = localFile.name,
            handle = handle,
            parentHandle = parentId,
            isFolderLink = isFolderLink,
            isMediaQueueAvailable = isMediaQueueAvailable,
            path = localFile.absolutePath,
            offlineParentId = offlineParentId,
            offlineParent = localFile.parent,
            searchedItems = searchedItems,
            collectionTitle = collectionTitle,
            collectionId = collectionId
        )

    }

    override suspend fun openMediaPlayerActivityFromChat(
        context: Context,
        contentUri: NodeContentUri,
        message: NodeAttachmentMessage,
        fileNode: FileNode,
    ) {
        val intent = getIntent(context, fileNode.type).apply {
            putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, FROM_CHAT)
            putExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, false)
            putExtra(INTENT_EXTRA_KEY_MSG_ID, message.msgId)
            putExtra(INTENT_EXTRA_KEY_CHAT_ID, message.chatId)
            putExtra(INTENT_EXTRA_KEY_FILE_NAME, fileNode.name)
            putExtra(INTENT_EXTRA_KEY_HANDLE, fileNode.id.longValue)
        }

        val mimeType =
            if (fileNode.type.extension == "opus") "audio/*" else fileNode.type.mimeType
        nodeContentUriIntentMapper(intent, contentUri, mimeType, fileNode.type.isSupported)
        context.startActivity(intent)
    }

    override suspend fun openMediaPlayerActivityFromChat(
        context: Context,
        contentUri: NodeContentUri,
        handle: Long,
        messageId: Long,
        chatId: Long,
        name: String,
    ) {
        val fileType = getFileTypeInfoByNameUseCase(name)
        val intent = getIntent(context, fileType).apply {
            putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, FROM_CHAT)
            putExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, false)
            putExtra(INTENT_EXTRA_KEY_MSG_ID, messageId)
            putExtra(INTENT_EXTRA_KEY_CHAT_ID, chatId)
            putExtra(INTENT_EXTRA_KEY_FILE_NAME, name)
            putExtra(INTENT_EXTRA_KEY_HANDLE, handle)
        }

        val mimeType =
            if (fileType.extension == "opus") "audio/*" else fileType.mimeType
        nodeContentUriIntentMapper(intent, contentUri, mimeType, fileType.isSupported)
        context.startActivity(intent)
    }

    override suspend fun openMediaPlayerActivity(
        context: Context,
        contentUri: NodeContentUri,
        name: String,
        handle: Long,
        viewType: Int?,
        parentId: Long,
        fileTypeInfo: FileTypeInfo?,
        sortOrder: SortOrder,
        isFolderLink: Boolean,
        isMediaQueueAvailable: Boolean,
        searchedItems: List<Long>?,
        mediaQueueTitle: String?,
        nodeHandles: List<Long>?,
        enableAddToAlbum: Boolean,
    ) {
        val info = fileTypeInfo ?: getFileTypeInfoByNameUseCase(name)
        manageMediaIntent(
            context = context,
            contentUri = contentUri,
            fileTypeInfo = info,
            sortOrder = sortOrder,
            viewType = viewType,
            name = name,
            handle = handle,
            parentHandle = parentId,
            isFolderLink = isFolderLink,
            isMediaQueueAvailable = isMediaQueueAvailable,
            searchedItems = searchedItems,
            mediaQueueTitle = mediaQueueTitle,
            nodeHandles = nodeHandles,
            enableAddToAlbum = enableAddToAlbum,
        )
    }

    override fun openSyncs(context: Context) {
        context.startActivity(Intent(context, SyncHostActivity::class.java))
    }

    override fun openNewSync(
        context: Context,
        syncType: SyncType,
        isFromManagerActivity: Boolean,
        isFromCloudDrive: Boolean,
        remoteFolderHandle: Long?,
        remoteFolderName: String?,
    ) {
        context.startActivity(Intent(context, SyncHostActivity::class.java).apply {
            putExtra(
                SyncHostActivity.EXTRA_NEW_FOLDER_DETAIL,
                SyncNewFolder(
                    syncType = syncType,
                    isFromManagerActivity = isFromManagerActivity,
                    remoteFolderHandle = remoteFolderHandle,
                    remoteFolderName = remoteFolderName,
                )
            )
            putExtra(SyncHostActivity.EXTRA_IS_FROM_CLOUD_DRIVE, isFromCloudDrive)
        })
    }

    override fun openInternalFolderPicker(
        context: Context,
        launcher: ActivityResultLauncher<Intent>,
        initialUri: Uri?,
        isUpload: Boolean,
        parentId: NodeId?,
    ) {
        launcher.launch(
            Intent(context, UploadFolderActivity::class.java).apply {
                data = initialUri
                if (isUpload) {
                    putExtra(
                        UploadFolderActivity.UPLOAD_FOLDER_TYPE,
                        UploadFolderType.SELECT_AND_UPLOAD
                    )
                } else {
                    putExtra(
                        UploadFolderActivity.UPLOAD_FOLDER_TYPE,
                        UploadFolderType.SINGLE_SELECT
                    )
                }
                putExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, parentId?.longValue ?: -1L)
            }
        )
    }

    override fun openSyncMegaFolder(context: Context, handle: Long) {
        context.startActivity(
            Intent(context, ManagerActivity::class.java)
                .setAction(ACTION_OPEN_SYNC_MEGA_FOLDER)
                .setFlags(FLAG_ACTIVITY_CLEAR_TOP)
                .setData("https://${getDomainNameUseCase()}/opensync#${handle}".toUri())
        )
    }

    override fun openDeviceCenter(context: Context) {
        context.startActivity(
            Intent(context, ManagerActivity::class.java)
                .setAction(ACTION_OPEN_DEVICE_CENTER)
                .setFlags(FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    override fun openSelectStopBackupDestinationFromSyncsTab(
        context: Context,
        folderName: String?,
    ) {
        context.startActivity(Intent(context, SyncHostActivity::class.java).apply {
            putExtra(SyncHostActivity.EXTRA_IS_FROM_CLOUD_DRIVE, true)
            putExtra(SyncHostActivity.EXTRA_OPEN_SELECT_STOP_BACKUP_DESTINATION, true)
            putExtra(SyncHostActivity.EXTRA_FOLDER_NAME, folderName)
        })
    }

    override fun openPdfActivity(
        context: Context,
        content: NodeContentUri,
        type: Int?,
        currentFileNode: TypedFileNode,
    ) {
        val pdfIntent = Intent(context, PdfViewerActivity::class.java)
        val mimeType = currentFileNode.type.mimeType
        pdfIntent.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, currentFileNode.id.longValue)
            putExtra(Constants.INTENT_EXTRA_KEY_INSIDE, true)
            putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, type)
            putExtra(Constants.INTENT_EXTRA_KEY_APP, true)
        }
        nodeContentUriIntentMapper(
            intent = pdfIntent,
            content = content,
            mimeType = mimeType,
        )
        context.startActivity(pdfIntent)
    }

    override fun openImageViewerActivity(
        context: Context,
        currentFileNode: TypedFileNode,
        nodeSourceType: Int?,
    ) {
        val currentFileNodeParentId = currentFileNode.parentId.longValue

        val (imageSource, menuOptionsSource, paramKey) = when (nodeSourceType) {
            NodeSourceTypeInt.FILE_BROWSER_ADAPTER -> Triple(
                ImagePreviewFetcherSource.CLOUD_DRIVE,
                ImagePreviewMenuSource.CLOUD_DRIVE,
                CloudDriveImageNodeFetcher.PARENT_ID
            )

            NodeSourceTypeInt.RUBBISH_BIN_ADAPTER -> Triple(
                ImagePreviewFetcherSource.RUBBISH_BIN,
                ImagePreviewMenuSource.RUBBISH_BIN,
                RubbishBinImageNodeFetcher.PARENT_ID
            )

            NodeSourceTypeInt.INCOMING_SHARES_ADAPTER,
            NodeSourceTypeInt.OUTGOING_SHARES_ADAPTER,
                -> Triple(
                ImagePreviewFetcherSource.SHARED_ITEMS,
                ImagePreviewMenuSource.SHARED_ITEMS,
                SharedItemsImageNodeFetcher.PARENT_ID
            )

            NodeSourceTypeInt.LINKS_ADAPTER -> Triple(
                ImagePreviewFetcherSource.SHARED_ITEMS,
                ImagePreviewMenuSource.LINKS,
                SharedItemsImageNodeFetcher.PARENT_ID
            )

            NodeSourceTypeInt.BACKUPS_ADAPTER -> Triple(
                ImagePreviewFetcherSource.CLOUD_DRIVE,
                ImagePreviewMenuSource.CLOUD_DRIVE,
                CloudDriveImageNodeFetcher.PARENT_ID
            )

            else -> {
                Timber.e("Unknown node source type: $nodeSourceType")
                return
            }
        }

        val intent = ImagePreviewActivity.createIntent(
            context = context,
            imageSource = imageSource,
            menuOptionsSource = menuOptionsSource,
            anchorImageNodeId = currentFileNode.id,
            params = mapOf(paramKey to currentFileNodeParentId),
            enableAddToAlbum = nodeSourceType in listOf(
                NodeSourceTypeInt.FILE_BROWSER_ADAPTER,
                NodeSourceTypeInt.OUTGOING_SHARES_ADAPTER,
            )
        )

        context.startActivity(intent)
    }

    override fun openTextEditorActivity(
        context: Context,
        currentNodeId: NodeId,
        nodeSourceType: Int?,
        mode: TextEditorMode,
        fileName: String?,
    ) {
        val textFileIntent = Intent(context, TextEditorActivity::class.java)
        textFileIntent.putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, currentNodeId.longValue)
            .putExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME, fileName)
            .putExtra(TextEditorViewModel.MODE, mode.value)
            .putExtra(
                Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE,
                nodeSourceType ?: NodeSourceTypeInt.FILE_BROWSER_ADAPTER
            )
        context.startActivity(textFileIntent)
    }

    override fun openGetLinkActivity(context: Context, handle: Long) {
        context.startActivity(
            Intent(context, GetLinkActivity::class.java)
                .putExtra(Constants.HANDLE, handle)
        )
    }

    override fun openGetLinkActivity(context: Context, handles: LongArray) {
        context.startActivity(
            Intent(context, GetLinkActivity::class.java)
                .putExtra(Constants.HANDLE_LIST, handles)
        )
    }

    override fun openFileInfoActivity(context: Context, handle: Long) {
        context.startActivity(
            Intent(context, FileInfoActivity::class.java)
                .putExtra(Constants.HANDLE, handle)
        )
    }

    override fun openFileContactListActivity(
        context: Context,
        handle: Long,
        nodeName: String,
    ) {
        context.startActivity(
            FileContactListComposeActivity.newIntent(
                context = context,
                nodeHandle = handle,
                nodeName = nodeName
            )
        )
    }

    @Deprecated("Use the new openFileContactListActivity with nodeName parameter")
    override fun openFileContactListActivity(
        context: Context,
        handle: Long,
    ) {
        context.startActivity(
            FileContactListActivity.launchIntent(
                context = context,
                handle = handle
            )
        )
    }

    override fun openAuthenticityCredentialsActivity(
        context: Context,
        email: String,
        isIncomingShares: Boolean,
    ) {
        context.startActivity(
            AuthenticityCredentialsActivity.getIntent(
                context = context,
                email = email,
                isIncomingShares = isIncomingShares
            )
        )
    }

    override fun launchUrl(context: Context?, url: String?) {
        context?.launchUrl(url)
    }

    override fun openSaveScannedDocumentsActivity(
        context: Context,
        originatedFromChat: Boolean,
        cloudDriveParentHandle: Long,
        scanPdfUri: Uri,
        scanSoloImageUri: Uri?,
    ) {
        val intent = Intent(context, SaveScannedDocumentsActivity::class.java).apply {
            putExtra(SaveScannedDocumentsActivity.EXTRA_ORIGINATED_FROM_CHAT, originatedFromChat)
            putExtra(
                SaveScannedDocumentsActivity.EXTRA_CLOUD_DRIVE_PARENT_HANDLE,
                cloudDriveParentHandle
            )
            putExtra(SaveScannedDocumentsActivity.EXTRA_SCAN_PDF_URI, scanPdfUri)
            putExtra(SaveScannedDocumentsActivity.EXTRA_SCAN_SOLO_IMAGE_URI, scanSoloImageUri)
        }
        context.startActivity(intent)
    }

    override fun openSearchActivity(
        context: Context,
        nodeSourceType: NodeSourceType,
        parentHandle: Long,
        isFirstNavigationLevel: Boolean,
    ) {
        context.startActivity(
            SearchActivity.getIntent(
                context = context,
                nodeSourceType = nodeSourceType,
                parentHandle = parentHandle,
                isFirstNavigationLevel = isFirstNavigationLevel
            )
        )
    }

    override fun openTakedownPolicyLink(context: Context) {
        launchUrl(
            context = context,
            url = "https://${getDomainNameUseCase()}/takedown"
        )
    }

    override fun openDisputeTakedownLink(context: Context) {
        launchUrl(
            context = context,
            url = "https://${getDomainNameUseCase()}/dispute"
        )
    }

    override fun openAchievements(context: Context) {
        context.startActivity(
            Intent(context, MyAccountActivity::class.java)
                .setAction(IntentConstants.ACTION_OPEN_ACHIEVEMENTS)
        )
    }

    override fun openAskForCustomizedPlan(
        context: Context,
        myEmail: String?,
        accountType: AccountType,
    ) {
        AlertsAndWarnings.askForCustomizedPlan(
            context = context,
            myEmail = myEmail,
            accountType = accountType
        )
    }

    override fun openMyAccountActivity(context: Context, flags: Int?) {
        val intent = Intent(context, MyAccountActivity::class.java)
        flags?.let {
            intent.flags = flags
        }
        context.startActivity(intent)
    }

    override fun openManagerActivity(context: Context, bundle: Bundle?) {
        val intent = Intent(context, ManagerActivity::class.java)
        bundle?.let { intent.putExtras(it) }
        context.startActivity(intent)
    }

    override fun openMediaDiscoveryActivity(
        context: Context,
        folderId: NodeId,
        folderName: String,
        isFromFolderLink: Boolean,
    ) {
        MediaDiscoveryActivity.startMDActivity(
            context = context,
            mediaHandle = folderId.longValue,
            folderName = folderName,
            isOpenByMDIcon = true,
            isFromFolderLink = false
        )
    }
}
