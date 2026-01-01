package mega.privacy.android.app.nav

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.result.ActivityResultLauncher
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.activities.OfflineFileInfoActivity
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.getLink.GetLinkActivity
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.mediaplayer.AudioPlayerActivity
import mega.privacy.android.app.mediaplayer.VideoPlayerComposeActivity
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.presentation.contact.authenticitycredendials.AuthenticityCredentialsActivity
import mega.privacy.android.app.presentation.contact.invite.InviteContactActivity
import mega.privacy.android.app.presentation.contact.invite.InviteContactViewModel
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsActivity
import mega.privacy.android.app.presentation.filecontact.FileContactListActivity
import mega.privacy.android.app.presentation.fileinfo.FileInfoActivity
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity
import mega.privacy.android.app.presentation.imagepreview.fetcher.OfflineImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.meeting.chat.ChatActivity
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_ACTION
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_LINK
import mega.privacy.android.app.presentation.meeting.managechathistory.view.screen.ManageChatHistoryActivity
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity
import mega.privacy.android.app.presentation.photos.mediadiscovery.MediaDiscoveryActivity
import mega.privacy.android.app.presentation.search.SearchActivity
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsActivity
import mega.privacy.android.app.presentation.settings.compose.navigation.SettingsNavigatorImpl
import mega.privacy.android.app.presentation.transfers.TransfersActivity
import mega.privacy.android.app.presentation.zipbrowser.ZipBrowserComposeActivity
import mega.privacy.android.app.textEditor.TextEditorActivity
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
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_APP
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_INSIDE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_PLAYLIST
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MSG_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE
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
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetFileTypeInfoUseCase
import mega.privacy.android.feature.payment.presentation.upgrade.ChooseAccountActivity
import mega.privacy.android.feature.sync.navigation.SyncNewFolder
import mega.privacy.android.feature.sync.ui.SyncHostActivity
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.AchievementNavKey
import mega.privacy.android.navigation.destination.AuthenticityCredentialsNavKey
import mega.privacy.android.navigation.destination.ChatNavKey
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.navigation.destination.DeviceCenterNavKey
import mega.privacy.android.navigation.destination.FileContactInfoNavKey
import mega.privacy.android.navigation.destination.FileInfoNavKey
import mega.privacy.android.navigation.destination.GetLinkNavKey
import mega.privacy.android.navigation.destination.InviteContactNavKey
import mega.privacy.android.navigation.destination.ManageChatHistoryNavKey
import mega.privacy.android.navigation.destination.MyAccountNavKey
import mega.privacy.android.navigation.destination.OfflineInfoNavKey
import mega.privacy.android.navigation.destination.SaveScannedDocumentsNavKey
import mega.privacy.android.navigation.destination.LegacySearchNavKey
import mega.privacy.android.navigation.destination.SettingsCameraUploadsNavKey
import mega.privacy.android.navigation.destination.SyncListNavKey
import mega.privacy.android.navigation.destination.SyncNewFolderNavKey
import mega.privacy.android.navigation.destination.SyncSelectStopBackupDestinationNavKey
import mega.privacy.android.navigation.destination.TransfersNavKey
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
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
    private val mediaPlayerIntentMapper: MediaPlayerIntentMapper,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val navigationQueue: NavigationEventQueue,
    private val activityLifecycleHandler: ActivityLifecycleHandler,
    private val snackbarEventQueue: SnackbarEventQueue,
) : MegaNavigator,
    AppNavigatorImpl, SettingsNavigator by settingsNavigator {

    private fun navigateForSingleActivity(
        context: Context,
        singleActivityDestination: NavKey?,
        legacyNavigation: suspend () -> Unit,
    ) {
        applicationScope.launch {
            runCatching { getFeatureFlagValueUseCase(AppFeatures.SingleActivity) }
                .onFailure {
                    legacyNavigation()
                }.onSuccess { singleActivity ->
                    if (singleActivity) {
                        launchMegaActivityIfNeeded(context)
                        singleActivityDestination?.let { navigationQueue.emit(it) }
                    } else {
                        legacyNavigation()
                    }
                }
        }
    }

    /**
     * Only use navKey if current activity is MegaActivity
     * Otherwise, use legacy navigation
     */
    private fun navigateIfInSingleActivity(
        singleActivityDestination: NavKey?,
        legacyNavigation: suspend () -> Unit,
    ) {
        applicationScope.launch {
            runCatching { getFeatureFlagValueUseCase(AppFeatures.SingleActivity) }
                .onFailure {
                    legacyNavigation()
                }.onSuccess { singleActivity ->
                    val isMegaActivity =
                        activityLifecycleHandler.getCurrentActivity() is MegaActivity
                    if (singleActivity && isMegaActivity) {
                        singleActivityDestination?.let { navigationQueue.emit(it) }
                    } else {
                        legacyNavigation()
                    }
                }
        }
    }

    override fun launchMegaActivityIfNeeded(context: Context) {
        val isMegaActivity =
            activityLifecycleHandler.getCurrentActivity() is MegaActivity
        if (!isMegaActivity) {
            context.startActivity(
                Intent(context, MegaActivity::class.java)
            )
        }
    }

    override fun openSettingsCameraUploads(context: Context) {
        navigateForSingleActivity(
            context = context, singleActivityDestination = SettingsCameraUploadsNavKey
        ) {
            context.startActivity(Intent(context, SettingsCameraUploadsActivity::class.java))
        }
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
        navigateIfInSingleActivity(
            singleActivityDestination = ChatNavKey(
                chatId = chatId,
                action = action,
                link = link,
                snackbarText = text,
                messageId = messageId,
                isOverQuota = isOverQuota,
            )
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
    }

    override fun openUpgradeAccount(context: Context, source: UpgradeAccountSource) {
        navigateIfInSingleActivity(
            singleActivityDestination = UpgradeAccountNavKey(source = source)
        ) {
            ChooseAccountActivity.navigateToUpgradeAccount(
                context = context, source = source
            )
        }
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
        val intent = Intent(context, ChatActivity::class.java).apply {
            this.action = action
            putExtra(EXTRA_ACTION, action)
            text?.let { putExtra(Constants.SHOW_SNACKBAR, text) }
            putExtra(Constants.CHAT_ID, chatId)
            messageId?.let { putExtra(Constants.ID_MSG, messageId) }
            isOverQuota?.let { putExtra(Constants.IS_OVERQUOTA, isOverQuota) }
            // Use setFlags for consistency with ChatHostDestination
            if (flags > 0) {
                setFlags(flags)
            }
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
        navigateForSingleActivity(
            context = context,
            singleActivityDestination = ManageChatHistoryNavKey(
                chatId = chatId,
                email = email,
            )
        ) {
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
        navigateIfInSingleActivity(
            singleActivityDestination = InviteContactNavKey(isFromAchievement = isFromAchievement)
        ) {
            val intent = Intent(context, InviteContactActivity::class.java).apply {
                putExtra(InviteContactViewModel.KEY_FROM, isFromAchievement)
            }
            context.startActivity(intent)
        }
    }

    override fun openTransfers(context: Context) {
        navigateForSingleActivity(
            context = context, singleActivityDestination = TransfersNavKey()
        ) {
            context.startActivity(TransfersActivity.getIntent(context))
        }
    }

    override fun openMediaPlayerActivityByFileNode(
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
        val intent = mediaPlayerIntentMapper(
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
        val intent = mediaPlayerIntentMapper(
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
        context.startActivity(intent)
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
        val intent = mediaPlayerIntentMapper(
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
        context.startActivity(intent)
    }

    override fun openSyncs(context: Context) {
        navigateForSingleActivity(
            context = context, singleActivityDestination = SyncListNavKey
        ) {
            context.startActivity(Intent(context, SyncHostActivity::class.java))
        }
    }


    override fun openNewSync(
        context: Context,
        syncType: SyncType,
        isFromManagerActivity: Boolean,
        isFromCloudDrive: Boolean,
        remoteFolderHandle: Long?,
        remoteFolderName: String?,
    ) {
        navigateForSingleActivity(
            context = context,
            singleActivityDestination = SyncNewFolderNavKey(
                syncType = syncType,
                isFromManagerActivity = isFromManagerActivity,
                isFromCloudDrive = isFromCloudDrive,
                remoteFolderHandle = remoteFolderHandle,
                remoteFolderName = remoteFolderName
            )
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
        navigateForSingleActivity(
            context = context,
            singleActivityDestination = CloudDriveNavKey(nodeHandle = handle)
        ) {
            navigateToManagerActivity(
                context = context,
                action = ACTION_OPEN_SYNC_MEGA_FOLDER,
                data = null,
                bundle = Bundle().apply {
                    putLong(INTENT_EXTRA_KEY_HANDLE, handle)
                },
                flags = FLAG_ACTIVITY_CLEAR_TOP
            )
        }

    }

    override fun openDeviceCenter(context: Context) {
        navigateForSingleActivity(
            context = context,
            singleActivityDestination = DeviceCenterNavKey
        ) {
            navigateToManagerActivity(
                context = context,
                action = ACTION_OPEN_DEVICE_CENTER,
                data = null,
                bundle = null,
                flags = FLAG_ACTIVITY_CLEAR_TOP
            )
        }
    }

    override fun openSelectStopBackupDestinationFromSyncsTab(
        context: Context,
        folderName: String?,
    ) {
        navigateForSingleActivity(
            context = context,
            singleActivityDestination = SyncSelectStopBackupDestinationNavKey(folderName = folderName)
        ) {
            context.startActivity(Intent(context, SyncHostActivity::class.java).apply {
                putExtra(SyncHostActivity.EXTRA_IS_FROM_CLOUD_DRIVE, true)
                putExtra(SyncHostActivity.EXTRA_OPEN_SELECT_STOP_BACKUP_DESTINATION, true)
                putExtra(SyncHostActivity.EXTRA_FOLDER_NAME, folderName)
            })
        }
    }

    override fun openPdfActivity(
        context: Context,
        content: NodeContentUri,
        type: Int?,
        currentFileNode: TypedFileNode,
    ) {
        val pdfIntent = PdfViewerActivity.createIntent(
            context = context,
            nodeHandle = currentFileNode.id.longValue,
            nodeSourceType = type,
        )
        val mimeType = currentFileNode.type.mimeType
        nodeContentUriIntentMapper(
            intent = pdfIntent,
            content = content,
            mimeType = mimeType,
        )
        context.startActivity(pdfIntent)
    }

    override suspend fun openPdfActivity(
        context: Context,
        content: NodeContentUri.LocalContentUri,
        type: Int?,
        nodeId: NodeId,
    ) {
        val pdfIntent = Intent(context, PdfViewerActivity::class.java)
        val mimeType = getFileTypeInfoUseCase(content.file).mimeType
        pdfIntent.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(INTENT_EXTRA_KEY_HANDLE, nodeId.longValue)
            putExtra(INTENT_EXTRA_KEY_INSIDE, true)
            putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, type)
            putExtra(INTENT_EXTRA_KEY_APP, true)
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
        ImagePreviewActivity.createIntent(
            context,
            currentFileNode.id.longValue,
            currentFileNode.parentId.longValue,
            nodeSourceType
        )?.let { intent ->
            context.startActivity(intent)
        }
    }

    override fun openImageViewerForOfflineNode(
        context: Context,
        node: NodeId,
        path: String,
    ) {
        val intent = ImagePreviewActivity.createIntent(
            context = context,
            imageSource = ImagePreviewFetcherSource.OFFLINE,
            menuOptionsSource = ImagePreviewMenuSource.OFFLINE,
            anchorImageNodeId = node,
            params = mapOf(OfflineImageNodeFetcher.PATH to path),
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
        val textFileIntent = TextEditorActivity.createIntent(
            context = context,
            nodeHandle = currentNodeId.longValue,
            mode = mode.value,
            nodeSourceType = nodeSourceType,
            fileName = fileName,
        )
        context.startActivity(textFileIntent)
    }

    override fun openGetLinkActivity(context: Context, vararg handles: Long) {
        if (handles.isEmpty()) {
            Timber.e("openGetLinkActivity: No handles provided, aborting operation.")
            return
        }

        val handlesList = handles.toList()
        navigateForSingleActivity(
            context = context,
            singleActivityDestination = GetLinkNavKey(handles = handlesList)
        ) {
            val intent = Intent(context, GetLinkActivity::class.java).apply {
                if (handles.size == 1) {
                    putExtra(Constants.HANDLE, handles[0])
                } else {
                    putExtra(Constants.HANDLE_LIST, longArrayOf(*handles))
                }
            }
            context.startActivity(intent)
        }
    }

    override fun openFileInfoActivity(context: Context, handle: Long) {
        navigateForSingleActivity(
            context = context,
            singleActivityDestination = FileInfoNavKey(handle = handle)
        ) {
            context.startActivity(
                Intent(context, FileInfoActivity::class.java)
                    .putExtra(Constants.HANDLE, handle)
            )
        }
    }

    override fun openOfflineFileInfoActivity(
        context: Context,
        handle: String,
    ) {
        navigateForSingleActivity(
            context = context, singleActivityDestination = OfflineInfoNavKey(handle = handle)
        ) {
            context.startActivity(
                Intent(context, OfflineFileInfoActivity::class.java).putExtra(
                    Constants.HANDLE,
                    handle
                )
            )
        }
    }

    override fun openFileContactListActivity(
        context: Context,
        handle: Long,
        nodeName: String,
    ) {
        navigateForSingleActivity(
            context = context,
            legacyNavigation = {
                @Suppress("DEPRECATION")
                openFileContactListActivity(context = context, handle = handle)
            },
            singleActivityDestination = FileContactInfoNavKey(
                folderHandle = handle,
                folderName = nodeName
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
        navigateForSingleActivity(
            context = context,
            singleActivityDestination = AuthenticityCredentialsNavKey(
                email = email,
                isIncomingShares = isIncomingShares
            )
        ) {
            context.startActivity(
                AuthenticityCredentialsActivity.getIntent(
                    context = context,
                    email = email,
                    isIncomingShares = isIncomingShares
                )
            )
        }
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
        navigateForSingleActivity(
            context = context,
            singleActivityDestination = SaveScannedDocumentsNavKey(
                originatedFromChat = originatedFromChat,
                cloudDriveParentHandle = cloudDriveParentHandle,
                scanPdfUri = scanPdfUri.toString(),
                scanSoloImageUri = scanSoloImageUri?.toString(),
            )
        ) {
            val intent = SaveScannedDocumentsActivity.getIntent(
                context = context,
                fromChat = originatedFromChat,
                parentHandle = cloudDriveParentHandle,
                pdfUri = scanPdfUri,
                imageUris = scanSoloImageUri?.let { listOf(it) } ?: emptyList(),
            )
            context.startActivity(intent)
        }
    }

    override fun openSearchActivity(
        context: Context,
        nodeSourceType: NodeSourceType,
        parentHandle: Long,
    ) {
        navigateForSingleActivity(
            context = context, singleActivityDestination = LegacySearchNavKey(
                nodeSourceType = nodeSourceType, parentHandle = parentHandle
            )
        ) {
            context.startActivity(
                SearchActivity.getIntent(
                    context = context,
                    nodeSourceType = nodeSourceType,
                    parentHandle = parentHandle,
                )
            )
        }
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
        navigateForSingleActivity(
            context = context, singleActivityDestination = AchievementNavKey
        ) {
            context.startActivity(
                Intent(
                    context,
                    MyAccountActivity::class.java
                ).setAction(IntentConstants.ACTION_OPEN_ACHIEVEMENTS)
            )
        }
    }

    override fun openAskForCustomizedPlan(
        context: Context,
        email: String?,
        accountType: AccountType,
    ) {
        AlertsAndWarnings.askForCustomizedPlan(
            context = context,
            myEmail = email,
            accountType = accountType
        )
    }

    override fun openMyAccountActivity(context: Context, flags: Int?) {
        navigateForSingleActivity(
            context = context, singleActivityDestination = MyAccountNavKey()
        ) {
            val intent = Intent(context, MyAccountActivity::class.java)
            flags?.let {
                intent.flags = flags
            }
            context.startActivity(intent)
        }
    }

    @Deprecated("This function will be removed after SingleActivity flag goes live. Note that any calls to it while the flag is enabled will result in an exception")
    override fun openManagerActivity(
        context: Context,
        data: Uri?,
        action: String?,
        bundle: Bundle?,
        flags: Int?,
    ) {
        applicationScope.launch {
            runCatching { getFeatureFlagValueUseCase(AppFeatures.SingleActivity) }
                .onSuccess {
                    if (it) {
                        throw IllegalStateException("Navigating to ManagerActivity is not allowed when the SingleActivity flag is enabled")
                    } else {
                        navigateToManagerActivity(context, action, data, bundle, flags)
                    }
                }
                .onFailure {
                    navigateToManagerActivity(context, action, data, bundle, flags)
                }
        }
    }

    override fun openManagerActivity(
        context: Context,
        data: Uri?,
        action: String?,
        bundle: Bundle?,
        flags: Int?,
        singleActivityDestination: NavKey?,
        onIntentCreated: (suspend (Intent) -> Unit)?,
    ) {
        navigateForSingleActivity(
            context = context,
            singleActivityDestination = singleActivityDestination,
        ) {
            navigateToManagerActivity(context, action, data, bundle, flags, onIntentCreated)
        }
    }

    override suspend fun getPendingIntentConsideringSingleActivity(
        context: Context,
        legacyActivityClass: Class<out Activity>,
        createPendingIntent: (Intent) -> PendingIntent,
        singleActivityPendingIntent: () -> PendingIntent,
    ): PendingIntent = if (getFeatureFlagValueUseCase(AppFeatures.SingleActivity)) {
        singleActivityPendingIntent()
    } else {
        createPendingIntent(Intent(context, legacyActivityClass))
    }

    override suspend fun <T> getPendingIntentConsideringSingleActivityWithDestination(
        context: Context,
        legacyActivityClass: Class<out Activity>,
        createPendingIntent: (Intent) -> PendingIntent,
        singleActivityDestination: () -> T,
    ): PendingIntent where T : NavKey, T : Parcelable =
        if (getFeatureFlagValueUseCase(AppFeatures.SingleActivity)) {
            MegaActivity.getPendingIntentWithExtraDestination(context, singleActivityDestination())
        } else {
            createPendingIntent(Intent(context, legacyActivityClass))
        }

    @SuppressLint("ManagerActivityIntent")
    private suspend fun navigateToManagerActivity(
        context: Context,
        action: String?,
        data: Uri?,
        bundle: Bundle?,
        flags: Int? = null,
        onIntentCreated: (suspend (Intent) -> Unit)? = null,
    ) {
        val intent = Intent(context, ManagerActivity::class.java)
        intent.action = action
        intent.data = data
        flags?.let { intent.flags = it }
        bundle?.let { intent.putExtras(it) }
        onIntentCreated?.invoke(intent)
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
            isFromFolderLink = isFromFolderLink
        )
    }

    override suspend fun sendMessageConsideringSingleActivity(context: Context, message: String) {
        if (getFeatureFlagValueUseCase(AppFeatures.SingleActivity)) {
            launchMegaActivityIfNeeded(context)
            snackbarEventQueue.queueMessage(message)
        } else {
            navigateToManagerActivity(
                context = context,
                action = Constants.ACTION_SHOW_WARNING,
                data = null,
                flags = FLAG_ACTIVITY_CLEAR_TOP,
                bundle = Bundle().apply {
                    putString(
                        Constants.INTENT_EXTRA_WARNING_MESSAGE,
                        message,
                    )
                }
            )
        }
    }
}
