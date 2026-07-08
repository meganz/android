package mega.privacy.android.navigation

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.navigation.destination.AddContactToShareNavKey
import mega.privacy.android.navigation.payment.UpgradeAccountSource
import java.io.File

/**
 * App module navigator
 *
 */
interface AppNavigator {
    /**
     * Navigates to the Settings Camera Uploads page
     *
     * @param context The Context
     */
    fun openSettingsCameraUploads(context: Context)

    /**
     * Navigates to the Backups page to load the contents of the Backup Folder
     *
     * @param activity the Activity
     * @param backupsHandle The Backups Handle used to load its contents
     * @param errorMessage The [StringRes] of the message to display in the error banner
     */
    fun openNodeInBackups(activity: Activity, backupsHandle: Long, @StringRes errorMessage: Int?)

    /**
     * Navigates to the Cloud Drive page to view the selected Node
     *
     * @param activity the Activity
     * @param nodeHandle The Node Handle to view the selected Node. The Root Node will be accessed
     * if no Node Handle is specified
     * @param errorMessage The [StringRes] of the message to display in the error banner
     * @param isFromSyncFolders Indicates if the node is from Sync Folders. False by default.
     */
    fun openNodeInCloudDrive(
        activity: Activity,
        nodeHandle: Long = -1L,
        @StringRes errorMessage: Int?,
        isFromSyncFolders: Boolean = false,
    )

    /**
     * Navigates to the Over Disk Quota Paywall warning screen
     *
     * @param context The Context
     */
    fun openOverDiskQuotaPaywallWarning(context: Context)

    /**
     * Open chat
     *
     * @param context
     * @param chatId chat id of the chat room
     * @param action action of the intent
     * @param link chat link
     * @param text text to show in snackbar
     * @param messageId message id
     * @param isOverQuota is over quota int value
     */
    fun openChat(
        context: Context,
        chatId: Long,
        action: String? = null,
        link: String? = null,
        text: String? = null,
        messageId: Long? = null,
        isOverQuota: Int? = null,
        flags: Int = 0,
    )

    /**
     * Navigates to the new [mega.privacy.android.app.presentation.meeting.managechathistory.view.screen.ManageChatHistoryActivityV2]
     *
     * @param context The context that call this method
     * @param chatId The chat ID of the chat or meeting room
     * @param email The email of the current user
     */
    fun openManageChatHistoryActivity(
        context: Context,
        chatId: Long = -1L,
        email: String? = null,
    )

    /**
     * Open upgrade account screen.
     * This screen allows users to upgrade to a paid plan
     */
    fun openUpgradeAccount(
        context: Context,
        source: UpgradeAccountSource = UpgradeAccountSource.UNKNOWN,
    )

    /**
     * Navigates to the Cancel Account Plan screen.
     * Opens the activity directly (no single-activity navigation).
     *
     * @param context The context to use for navigation.
     * @param usedStorage The formatted used storage string to display, or null for empty.
     */
    fun navigateToCancelAccountPlan(context: Context, usedStorage: String)

    /**
     * Navigates to the Syncs page
     *
     * @param context       Context
     */
    fun openSyncs(context: Context)

    /**
     * Navigates to the Add New Sync page
     *
     * @param context       Context
     * @param syncType      The sync type from [SyncType]
     * @param isFromCloudDrive Indicates if the sync is from Cloud Drive. False by default.
     * @param remoteFolderHandle The remote folder handle
     * @param remoteFolderName The remote folder name
     */
    fun openNewSync(
        context: Context,
        syncType: SyncType,
        isFromCloudDrive: Boolean = false,
        remoteFolderHandle: Long? = null,
        remoteFolderName: String? = null,
    )

    /**
     * Open zip browser
     *
     * @param context Context
     * @param zipFilePath zip file path
     * @param nodeHandle the node handle of zip file
     * @param onError Callback called when zip file format check is not passed
     */
    fun openZipBrowserActivity(
        context: Context,
        zipFilePath: String,
        nodeHandle: Long? = null,
        onError: () -> Unit,
    )

    /**
     * Navigates to the new [InviteContactActivityV2].
     *
     * @param context The context that call this method.
     * @param isFromAchievement Whether the entry point is [InviteFriendsRoute].
     */
    fun openInviteContactActivity(context: Context, isFromAchievement: Boolean)

    /**
     * Navigate to the transfers section.
     */
    fun openTransfers(context: Context)

    /**
     * Open media player by file node
     *
     * @param context Context
     * @param contentUri NodeContentUri
     * @param fileNode TypedFileNode
     * @param sortOrder SortOrder
     * @param viewType the adapter type of the view
     * @param isFolderLink whether the file is a folder link
     * @param isMediaQueueAvailable whether the media queue is available
     * @param searchedItems the list of searched items, this is only used under the search mode
     * @param mediaQueueTitle the title of the media queue
     * @param collectionTitle the title of the video collection
     * @param enableAddToAlbum the flag to show add to album in context menu
     * @param publicLinkUrl the public file link URL, used when the video is opened from a file link
     *   so the node can be fetched via [mega.privacy.android.domain.usecase.filelink.GetPublicNodeUseCase]
     */
    fun openMediaPlayerActivityByFileNode(
        context: Context,
        contentUri: NodeContentUri,
        fileNode: TypedFileNode,
        viewType: Int?,
        sortOrder: SortOrder = SortOrder.ORDER_NONE,
        isFolderLink: Boolean = false,
        isMediaQueueAvailable: Boolean = true,
        searchedItems: List<Long>? = null,
        mediaQueueTitle: String? = null,
        collectionTitle: String? = null,
        collectionId: Long? = null,
        enableAddToAlbum: Boolean? = null,
        serializedData: String? = null,
        publicLinkUrl: String? = null,
    )

    /**
     * Open media player by local file
     *
     * @param context Context
     * @param localFile File
     * @param fileTypeInfo FileTypeInfo
     * @param viewType the adapter type of the view
     * @param handle the handle of the node
     * @param parentId the parent id of the node
     * @param offlineParentId the parent id of the offline
     * @param sortOrder SortOrder
     * @param isFolderLink whether the file is a folder link
     * @param isMediaQueueAvailable whether the media queue is available
     * @param searchedItems the list of searched items, this is only used under the search mode
     * @param collectionTitle the title of the video collection
     */
    suspend fun openMediaPlayerActivityByLocalFile(
        context: Context,
        localFile: File,
        handle: Long,
        viewType: Int? = null,
        parentId: Long = -1L,
        offlineParentId: Int? = null,
        fileTypeInfo: FileTypeInfo? = null,
        sortOrder: SortOrder = SortOrder.ORDER_NONE,
        isFolderLink: Boolean = false,
        isMediaQueueAvailable: Boolean = true,
        searchedItems: List<Long>? = null,
        collectionTitle: String? = null,
        collectionId: Long? = null,
        publicLinkUrl: String? = null,
        localFilePath: String? = null,
    )

    /**
     * Open media player from Chat
     *
     * @param context Context
     * @param contentUri [NodeContentUri]
     * @param message [NodeAttachmentMessage]
     * @param fileNode [FileNode]
     */
    suspend fun openMediaPlayerActivityFromChat(
        context: Context,
        contentUri: NodeContentUri,
        message: NodeAttachmentMessage,
        fileNode: FileNode,
    )

    /**
     * Open media player from Chat
     *
     * @param context Context
     * @param contentUri [NodeContentUri]
     * @param message [NodeAttachmentMessage]
     * @param fileNode [FileNode]
     */
    suspend fun openMediaPlayerActivityFromChat(
        context: Context,
        contentUri: NodeContentUri,
        handle: Long,
        messageId: Long,
        chatId: Long,
        name: String,
    )

    /**
     * Open media player by file node
     *
     * @param context Context
     * @param contentUri NodeContentUri
     * @param name the name of the node
     * @param handle the handle of the node
     * @param parentId the parent id of the node
     * @param fileTypeInfo FileTypeInfo
     * @param sortOrder SortOrder
     * @param viewType the adapter type of the view
     * @param isFolderLink whether the file is a folder link
     * @param isMediaQueueAvailable whether the media queue is available
     * @param searchedItems the list of searched items, this is only used under the search mode
     * @param mediaQueueTitle the title of the media queue
     * @param nodeHandles node handle list
     */
    suspend fun openMediaPlayerActivity(
        context: Context,
        contentUri: NodeContentUri,
        name: String,
        handle: Long,
        viewType: Int? = null,
        parentId: Long = -1L,
        fileTypeInfo: FileTypeInfo? = null,
        sortOrder: SortOrder = SortOrder.ORDER_NONE,
        isFolderLink: Boolean = false,
        isMediaQueueAvailable: Boolean = true,
        searchedItems: List<Long>? = null,
        mediaQueueTitle: String? = null,
        nodeHandles: List<Long>? = null,
        enableAddToAlbum: Boolean = false,
    )

    /**
     * Open internal folder picker
     */
    fun openInternalFolderPicker(
        context: Context,
        launcher: ActivityResultLauncher<Intent>,
        initialUri: Uri? = null,
        isUpload: Boolean = false,
        parentId: NodeId? = null,
    )

    /**
     * Opens the "add contacts to a shared folder" picker for a result. Behind
     * `ContactsComposeUI`, launches the Compose picker (with phone contacts) when the flag is on
     * and the legacy add-contact screen when it is off; both return the legacy result shape into
     * [launcher].
     *
     * @param context the launching context.
     * @param launcher the caller's result launcher receiving the picker's Activity result.
     * @param contactType the contact source to surface in the picker.
     * @param nodeHandles the handle(s) of the folder(s) being shared.
     */
    fun openAddContactToShare(
        context: Context,
        launcher: ActivityResultLauncher<Intent>,
        contactType: AddContactToShareNavKey.ContactType,
        nodeHandles: List<Long>,
    )

    /**
     * Open Sync Mega folder
     * @param handle the handle of the remote folder
     */
    fun openSyncMegaFolder(context: Context, handle: Long)


    /**
     * Open Device Center
     */
    fun openDeviceCenter(context: Context)

    /**
     * Open Stop Backup Destination in SyncHost Activity
     * //stop-backup-mega-picker
     */
    fun openSelectStopBackupDestinationFromSyncsTab(context: Context, folderName: String?)

    /**
     * Open PDF viewer activity
     *
     * @param context Context
     * @param content NodeContentUri
     * @param type the adapter type of the view
     * @param currentFileNode TypedFileNode
     */
    fun openPdfActivity(
        context: Context,
        content: NodeContentUri,
        type: Int?,
        currentFileNode: TypedFileNode,
    )

    /**
     * Open PDF viewer activity
     *
     * @param context Context
     * @param content NodeContentUri
     * @param type the adapter type of the view
     * @param nodeId NodeId
     */
    suspend fun openPdfActivity(
        context: Context,
        content: NodeContentUri.LocalContentUri,
        type: Int?,
        nodeId: NodeId,
    )

    /**
     * Open the legacy PdfViewerActivity for a PDF attachment from chat. The Compose PDF viewer
     * (when the PdfViewerComposeUI flag is enabled) is opened in-place by the chat host via its
     * NavigationHandler, so this method only covers the legacy, flag-disabled path.
     *
     * @param context Context
     * @param content NodeContentUri
     * @param nodeHandle the handle of the file node
     * @param chatId the chat room id
     * @param messageId the message id
     * @param mimeType the MIME type of the file
     */
    fun openPdfViewerFromChat(
        context: Context,
        content: NodeContentUri,
        nodeHandle: Long,
        chatId: Long,
        messageId: Long,
        mimeType: String,
    )

    /**
     * Open image viewer activity
     *
     * @param context Context
     * @param currentFileNode TypedFileNode
     * @param nodeSourceType the adapter type of the view
     */
    fun openImageViewerActivity(
        context: Context,
        currentFileNode: TypedFileNode,
        nodeSourceType: Int?,
    )

    /**
     * Open image viewer activity for offline files
     *
     * @param context Context
     * @param node the NodeId of the current node
     * @param path the local path of the current node
     */
    fun openImageViewerForOfflineNode(
        context: Context,
        node: NodeId,
        path: String,
    )

    /**
     * Open text editor. Routes by [params]: CloudNode/LocalFile/Chat navigate via a NavKey;
     * FileLink starts the legacy Activity directly (Compose not supported for file link yet).
     *
     * @param context Context
     * @param params Determines source (cloud node, local/zip file, chat attachment, or file link)
     */
    fun openTextEditor(
        context: Context,
        params: OpenTextEditorParams,
    )

    /**
     * Open Get Link Activity
     *
     * @param context Context
     * @param handles Node handles (single or multiple)
     */
    fun openGetLinkActivity(
        context: Context,
        vararg handles: Long,
    )

    /**
     * Open File Info Activity
     *
     * @param context Context
     * @param handle Node handle
     */
    fun openFileInfoActivity(
        context: Context,
        handle: Long,
    )

    /**
     * Open Offline File Info Activity
     *
     * @param context Context
     * @param handle Node handle
     */
    fun openOfflineFileInfoActivity(
        context: Context,
        handle: String,
    )

    /**
     * Open File Contact List Activity
     *
     * @param context Context
     * @param handle Node handle
     * @param nodeName Name of the node
     */
    fun openFileContactListActivity(
        context: Context,
        handle: Long,
        nodeName: String,
    )

    /**
     * Open File Contact List Activity
     *
     * @param context Context
     * @param handle Node handle
     */
    @Deprecated("Use the new openFileContactListActivity with nodeName parameter")
    fun openFileContactListActivity(
        context: Context,
        handle: Long,
    )

    /**
     * Open Authenticity Credentials Activity
     *
     * @param context Context
     * @param email Email of the user
     * @param isIncomingShares Indicates if the shares are incoming
     */
    fun openAuthenticityCredentialsActivity(
        context: Context,
        email: String,
        isIncomingShares: Boolean,
    )

    /**
     * Launches a URL with via intent
     *
     * @param context The Context
     * @param url The URL to launch
     * @param appendNoPlansParam When true (default), appends noplans=1 to mega.io/help.mega.io/mega.co.nz
     * URLs to suppress checkout redirects. Pass false for links where checkout should be shown.
     */
    fun launchUrl(context: Context?, url: String?, appendNoPlansParam: Boolean = true)

    /**
     * Open take down policy link in custom tabs
     */
    fun openTakedownPolicyLink(context: Context)

    /**
     * Open dispute take down link in custom tabs
     */
    fun openDisputeTakedownLink(context: Context)

    /**
     * Open achievements screen
     *
     * @param context The context
     */
    fun openAchievements(context: Context)

    /**
     * Open customized plan screen
     *
     * @param context The context
     * @param email The email of the user
     * @param accountType The account type
     */
    fun openAskForCustomizedPlan(context: Context, email: String?, accountType: AccountType)

    /**
     * Open My Account Activity
     *
     * @param context The context
     * @param flags The optional intent flags. If null, defaults to FLAG_ACTIVITY_CLEAR_TOP
     */
    fun openMyAccountActivity(context: Context, flags: Int? = null)

    /**
     * Get a PendingIntent that targets the single activity with the provided destination.
     *
     * If more than one destination is needed, please consider using getPendingIntentConsideringSingleActivity and create the intent with MegaActivity companion helper functions
     *
     * @param context The Context
     * @param singleActivityDestination A lambda that creates the NavKey destination to use
     * @return The PendingIntent targeting the single activity with the provided destination
     */
    suspend fun <T> getPendingIntentWithDestination(
        context: Context,
        singleActivityDestination: () -> T,
    ): PendingIntent where T : NavKey, T : Parcelable

    /**
     * Open Media Discovery Activity
     *
     * @param context The context
     * @param folderId The folder id of the media discovery
     * @param folderName The folder name of the media discovery
     * @param isFromFolderLink True if the media discovery is opened from a folder link, false otherwise
     */
    fun openMediaDiscoveryActivity(
        context: Context,
        folderId: NodeId,
        folderName: String,
        isFromFolderLink: Boolean,
    )

    /**
     * Send a snackbar message via the single activity host.
     * @param context The context
     * @param message The message to send
     */
    suspend fun sendMessageConsideringSingleActivity(
        context: Context,
        message: String,
    )

    /**
     * Method to open ContactInfoActivity.class.
     *
     * @param context Activity context.
     * @param email    The email of the contact.
     */
    fun openContactInfoActivity(context: Context, email: String)

    /**
     * Method to open ContactAttachmentActivity.class.
     *
     * @param context Activity context.
     * @param chatId  The ID of a chat.
     * @param msgId   The ID of a message.
     */
    fun openContactAttachmentActivity(context: Context, chatId: Long, msgId: Long)

    /**
     * Opens the meeting "add participants" picker for a result, choosing the Compose contacts UI
     * or the legacy AddContactActivity based on the ContactsComposeUI flag. The result is delivered
     * to [activity]'s onActivityResult under [requestCode], mirroring the legacy AddContactActivity
     * contract (RESULT_OK + EXTRA_CONTACTS).
     */
    fun openAddMeetingParticipantsForResult(
        activity: Activity,
        chatId: Long,
        callUsersLimit: Int?,
        requestCode: Int,
    )

    /**
     * Opens the chat "add participants" picker for a result, choosing the Compose contacts UI or
     * the legacy AddContactActivity based on the ContactsComposeUI flag. The result is delivered to
     * [activity]'s onActivityResult under [requestCode], mirroring the legacy AddContactActivity
     * contract (RESULT_OK + EXTRA_CONTACTS).
     */
    fun openAddChatParticipantsForResult(
        activity: Activity,
        chatId: Long,
        requestCode: Int,
    )

    /**
     * Opens the chat "add participants" picker for a result, choosing the Compose contacts UI or
     * the legacy AddContactActivity based on the ContactsComposeUI flag. The result is delivered to
     * [launcher], mirroring the legacy AddContactActivity contract (RESULT_OK + EXTRA_CONTACTS).
     */
    fun openAddChatParticipantsForResult(
        context: Context,
        launcher: ActivityResultLauncher<Intent>,
        chatId: Long,
    )

    /**
     * Opens the "create group chat" flow for a result, choosing the Compose contacts UI or the
     * legacy AddContactActivity ("only create group" mode) based on the ContactsComposeUI flag. The
     * result is delivered to [activity]'s onActivityResult under [requestCode], mirroring the legacy
     * AddContactActivity contract (RESULT_OK + EXTRA_CONTACTS plus the group-chat extras).
     */
    fun openCreateGroupChatForResult(
        activity: Activity,
        requestCode: Int,
    )

    /**
     * Opens the "create group chat" flow for a result, choosing the Compose contacts UI or the
     * legacy AddContactActivity ("only create group" mode) based on the ContactsComposeUI flag. The
     * result is delivered to [launcher], mirroring the legacy AddContactActivity contract (RESULT_OK
     * + EXTRA_CONTACTS plus the group-chat extras).
     */
    fun openCreateGroupChatForResult(
        context: Context,
        launcher: ActivityResultLauncher<Intent>,
    )

    /**
     * Opens the "add contacts" picker for a result with the already-chosen participants
     * pre-selected, choosing the Compose contacts UI or the legacy AddContactActivity based on the
     * ContactsComposeUI flag. The result is delivered to [launcher], mirroring the legacy
     * AddContactActivity contract (RESULT_OK + EXTRA_CONTACTS).
     *
     * The flag boundary is crossed with both [preselectedHandles] (used by the Compose path, which
     * selects by handle) and [preselectedEmails] (used by the legacy path, which pre-checks by email).
     *
     * @param context the launching context.
     * @param launcher the caller's result launcher receiving the picker's Activity result.
     * @param preselectedHandles handles of contacts to pre-select in the Compose picker.
     * @param preselectedEmails emails of contacts to pre-select in the legacy picker.
     */
    fun openAddContactsForResult(
        context: Context,
        launcher: ActivityResultLauncher<Intent>,
        preselectedHandles: List<Long>,
        preselectedEmails: List<String>,
    )
}

