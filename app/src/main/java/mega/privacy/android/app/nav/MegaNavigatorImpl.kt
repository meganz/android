package mega.privacy.android.app.nav

import android.app.Activity
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.activities.ManageChatHistoryActivity
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.mediaplayer.AudioPlayerActivity
import mega.privacy.android.app.mediaplayer.LegacyVideoPlayerActivity
import mega.privacy.android.app.presentation.contact.invite.InviteContactActivity
import mega.privacy.android.app.presentation.contact.invite.InviteContactActivityV2
import mega.privacy.android.app.presentation.contact.invite.InviteContactViewModel
import mega.privacy.android.app.presentation.meeting.chat.ChatHostActivity
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_ACTION
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_LINK
import mega.privacy.android.app.presentation.meeting.chat.view.message.attachment.NodeContentUriIntentMapper
import mega.privacy.android.app.presentation.meeting.managechathistory.view.screen.ManageChatHistoryActivityV2
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsActivity
import mega.privacy.android.app.presentation.transfers.EXTRA_TAB
import mega.privacy.android.app.presentation.transfers.TransfersActivity
import mega.privacy.android.app.presentation.zipbrowser.ZipBrowserComposeActivity
import mega.privacy.android.app.upgradeAccount.UpgradeAccountActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.EXTRA_HANDLE_ZIP
import mega.privacy.android.app.utils.Constants.EXTRA_PATH_ZIP
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_FOLDER_LINK
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MEDIA_QUEUE_TITLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PATH
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PLACEHOLDER
import mega.privacy.android.app.zippreview.ui.ZipBrowserActivity
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.navigation.MegaNavigator
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
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val nodeContentUriIntentMapper: NodeContentUriIntentMapper,
) : MegaNavigator,
    AppNavigatorImpl {
    override fun openSettingsCameraUploads(activity: Activity) {
        applicationScope.launch {
            activity.startActivity(
                Intent(
                    activity,
                    SettingsCameraUploadsActivity::class.java,
                )
            )
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

    override fun openUpgradeAccount(context: Context) {
        applicationScope.launch {
            val intent = Intent(context, UpgradeAccountActivity::class.java)
            context.startActivity(intent)
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
            val activity =
                if (getFeatureFlagValueUseCase(AppFeatures.NewManageChatHistoryActivity)) {
                    ManageChatHistoryActivityV2::class.java
                } else {
                    ManageChatHistoryActivity::class.java
                }
            val intent = Intent(context, activity).apply {
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
            if (getFeatureFlagValueUseCase(AppFeatures.NewZipBrowser)) {
                if (ZipBrowserComposeActivity.zipFileFormatCheck(context, zipFilePath)) {
                    ZipBrowserComposeActivity::class.java
                } else null
            } else {
                if (ZipBrowserActivity.zipFileFormatCheck(context, zipFilePath)) {
                    ZipBrowserActivity::class.java
                } else null
            }?.let { activity ->
                context.startActivity(Intent(context, activity).apply {
                    putExtra(EXTRA_PATH_ZIP, zipFilePath)
                    putExtra(EXTRA_HANDLE_ZIP, nodeHandle)
                })
            } ?: run {
                onError()
            }
        }
    }

    override fun openInviteContactActivity(context: Context, isFromAchievement: Boolean) {
        applicationScope.launch {
            val activity =
                if (getFeatureFlagValueUseCase(AppFeatures.NewInviteContactActivity)) {
                    InviteContactActivityV2::class.java
                } else {
                    InviteContactActivity::class.java
                }
            val intent = Intent(context, activity).apply {
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

    override fun openMediaPlayerActivityByFileNode(
        context: Context,
        contentUri: NodeContentUri,
        fileNode: TypedFileNode,
        viewType: Int,
        sortOrder: SortOrder,
        isFolderLink: Boolean,
        searchedItems: List<Long>?,
        mediaQueueTitle: String?,
        onError: () -> Unit,
    ) {
        applicationScope.launch {
            runCatching {
                manageMediaIntent(
                    context = context,
                    contentUri = contentUri,
                    fileTypeInfo = fileNode.type,
                    sortOrder = sortOrder,
                    viewType = viewType,
                    name = fileNode.name,
                    handle = fileNode.id.longValue,
                    parentHandle = fileNode.parentId.longValue,
                    isFolderLink = isFolderLink,
                    searchedItems = searchedItems,
                    mediaQueueTitle = mediaQueueTitle
                )
            }.onFailure {
                Timber.e(it)
                onError()
            }
        }
    }

    private fun manageMediaIntent(
        context: Context,
        contentUri: NodeContentUri,
        fileTypeInfo: FileTypeInfo,
        sortOrder: SortOrder,
        viewType: Int,
        name: String,
        handle: Long,
        parentHandle: Long,
        isFolderLink: Boolean,
        path: String? = null,
        offlineParent: String? = null,
        searchedItems: List<Long>? = null,
        mediaQueueTitle: String? = null,
    ) {
        val intent = when {
            fileTypeInfo.isSupported && fileTypeInfo is VideoFileTypeInfo ->
                Intent(context, LegacyVideoPlayerActivity::class.java)

            fileTypeInfo.isSupported && fileTypeInfo is AudioFileTypeInfo ->
                Intent(context, AudioPlayerActivity::class.java)

            else -> Intent(Intent.ACTION_VIEW)
        }.apply {
            putExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN, sortOrder)
            putExtra(INTENT_EXTRA_KEY_PLACEHOLDER, 0)
            putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, viewType)
            putExtra(INTENT_EXTRA_KEY_FILE_NAME, name)
            putExtra(INTENT_EXTRA_KEY_HANDLE, handle)
            putExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, parentHandle)
            putExtra(INTENT_EXTRA_KEY_IS_FOLDER_LINK, isFolderLink)
            path?.let {
                putExtra(INTENT_EXTRA_KEY_PATH, path)
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
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val mimeType =
            if (fileTypeInfo.extension == "opus") "audio/*" else fileTypeInfo.mimeType
        nodeContentUriIntentMapper(intent, contentUri, mimeType, fileTypeInfo.isSupported)
        context.startActivity(intent)
    }

    override fun openMediaPlayerActivityByLocalFile(
        context: Context,
        localFile: File,
        fileTypeInfo: FileTypeInfo,
        viewType: Int,
        handle: Long,
        parentId: Long,
        sortOrder: SortOrder,
        isFolderLink: Boolean,
        searchedItems: List<Long>?,
        onError: () -> Unit,
    ) {
        runCatching {
            val contentUri = NodeContentUri.LocalContentUri(localFile)
            manageMediaIntent(
                context = context,
                contentUri = contentUri,
                fileTypeInfo = fileTypeInfo,
                sortOrder = sortOrder,
                viewType = viewType,
                name = localFile.name,
                handle = handle,
                parentHandle = parentId,
                isFolderLink = isFolderLink,
                path = localFile.absolutePath,
                offlineParent = localFile.parent,
                searchedItems = searchedItems
            )
        }.onFailure {
            Timber.e(it)
            onError()
        }
    }
}
