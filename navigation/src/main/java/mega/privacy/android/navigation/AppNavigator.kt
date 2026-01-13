package mega.privacy.android.navigation

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
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
     * @param isFromManagerActivity Indicates if the sync is from Manager Activity. False by default.
     * @param isFromCloudDrive Indicates if the sync is from Cloud Drive. False by default.
     * @param remoteFolderHandle The remote folder handle
     * @param remoteFolderName The remote folder name
     */
    fun openNewSync(
        context: Context,
        syncType: SyncType,
        isFromManagerActivity: Boolean = false,
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
     * Navigate to [TransfersActivity]
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
     * Open text editor activity
     *
     * @param context Context
     * @param currentNodeId the NodeId of the current node
     * @param mode the mode of the text editor, e.g., "view", "edit"
     * @param nodeSourceType the adapter type of the view
     * @param fileName the name of the file to be created
     */
    fun openTextEditorActivity(
        context: Context,
        currentNodeId: NodeId,
        nodeSourceType: Int?,
        mode: TextEditorMode,
        fileName: String? = null,
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
     */
    fun launchUrl(context: Context?, url: String?)

    /**
     * Open SaveScannedDocumentsActivity
     *
     * @param context The context
     * @param originatedFromChat Whether the scan originated from chat
     * @param cloudDriveParentHandle The parent handle in cloud drive
     * @param scanPdfUri The PDF URI from scan result
     * @param scanSoloImageUri The solo image URI from scan result
     */
    fun openSaveScannedDocumentsActivity(
        context: Context,
        originatedFromChat: Boolean = false,
        cloudDriveParentHandle: Long,
        scanPdfUri: Uri,
        scanSoloImageUri: Uri?,
    )

    /**
     * Open Search Activity
     *
     * @param context The context
     * @param nodeSourceType The source type of the node
     * @param parentHandle The parent handle of the node
     */
    fun openSearchActivity(
        context: Context,
        nodeSourceType: NodeSourceType,
        parentHandle: Long,
    )

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
     * Open Manager Activity
     *
     * @param context The context
     * @param bundle Optional bundle containing extras to be added to the intent
     */
    @Deprecated("This function will be removed after SingleActivity flag goes live. Note that any calls to it while the flag is enabled will result in an exception")
    fun openManagerActivity(
        context: Context,
        data: Uri? = null,
        action: String? = null,
        bundle: Bundle? = null,
        flags: Int? = null,
    )

    /**
     * Open Manager Activity if Single activity feature flag is false, otherwise it opens the single activity destination in MegaActivity.
     *
     * @param context The context
     * @param data
     * @param action
     * @param bundle Optional bundle containing extras to be added to the intent
     * @param flags
     * @param singleActivityDestination the destination of Single activity. It can be null, in this case MegaActivity will be opened without any specific destination
     * @param singleActivityMessage Message that will be displayed as a snackbar message in case single activity is enabled
     * @param onIntentCreated callback that allows further configuration of the created intent before starting the ManagerActivity
     */
    fun openManagerActivity(
        context: Context,
        data: Uri? = null,
        action: String? = null,
        bundle: Bundle? = null,
        flags: Int? = null,
        singleActivityMessage: String? = null,
        singleActivityDestination: NavKey?,
        onIntentCreated: (suspend (Intent) -> Unit)? = null,
    )

    /**
     * Open Manager Activity if Single activity feature flag is false, otherwise it opens the single activity destination in MegaActivity.
     *
     * @param context The context
     * @param data
     * @param action
     * @param bundle Optional bundle containing extras to be added to the intent
     * @param flags
     * @param singleActivityDestinations the destination of Single activity. It can be null, in this case MegaActivity will be opened without any specific destination
     * @param singleActivityMessage Message that will be displayed as a snackbar message in case single activity is enabled
     * @param onIntentCreated callback that allows further configuration of the created intent before starting the ManagerActivity
     */
    fun openManagerActivity(
        context: Context,
        data: Uri? = null,
        action: String? = null,
        bundle: Bundle? = null,
        flags: Int? = null,
        singleActivityMessage: String? = null,
        singleActivityDestinations: List<NavKey>,
        onIntentCreated: (suspend (Intent) -> Unit)? = null,
    )

    /**
     * Get a PendingIntent considering the SingleActivity feature flag.
     * If SingleActivity is enabled, returns the PendingIntent created by singleActivityPendingIntent.
     * Otherwise, creates an Intent for the legacy activity and uses createPendingIntent to create the PendingIntent.
     *
     * @param context The Context
     * @param legacyActivityClass The Activity class to create the Intent for when SingleActivity is disabled
     * @param createPendingIntent A lambda that creates a PendingIntent from an Intent
     * @param singleActivityPendingIntent A lambda that creates the PendingIntent to use when SingleActivity is enabled
     * @return The appropriate PendingIntent based on the feature flag
     */
    suspend fun getPendingIntentConsideringSingleActivity(
        context: Context,
        legacyActivityClass: Class<out Activity>,
        createPendingIntent: (Intent) -> PendingIntent,
        singleActivityPendingIntent: () -> PendingIntent,
    ): PendingIntent

    /**
     * Get a PendingIntent considering the SingleActivity feature flag.
     * If SingleActivity is enabled, returns the PendingIntent to the single activity with provided destination.
     * Otherwise, creates an Intent for the legacy activity and uses createPendingIntent to create the PendingIntent.
     *
     * If more than one destination is needed, please consider using getPendingIntentConsideringSingleActivity and create the intent with MegaActivity companion helper functions
     *
     * @param context The Context
     * @param legacyActivityClass The Activity class to create the Intent for when SingleActivity is disabled
     * @param createPendingIntent A lambda that creates a PendingIntent from an Intent
     * @param singleActivityDestination A lambda that creates the NavKey destination to use when SingleActivity is enabled
     * @return The appropriate PendingIntent based on the feature flag
     */
    suspend fun <T> getPendingIntentConsideringSingleActivityWithDestination(
        context: Context,
        legacyActivityClass: Class<out Activity>,
        createPendingIntent: (Intent) -> PendingIntent,
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
     * Send a snackbar message considering SingleActivity feature flag.
     * @param context The context
     * @param message The message to send
     */
    suspend fun sendMessageConsideringSingleActivity(
        context: Context,
        message: String,
    )
}

/**
 * Get a PendingIntent considering the SingleActivity feature flag using reified generics.
 *
 * @param T The Activity class to create the Intent for when SingleActivity is disabled
 * @param context The Context
 * @param createPendingIntent A lambda that creates a PendingIntent from an Intent
 * @param singleActivityPendingIntent A lambda that creates the PendingIntent to use when SingleActivity is enabled
 * @return The appropriate PendingIntent based on the feature flag
 */
suspend inline fun <reified T : Activity> AppNavigator.getPendingIntentConsideringSingleActivity(
    context: Context,
    noinline createPendingIntent: (Intent) -> PendingIntent,
    noinline singleActivityPendingIntent: () -> PendingIntent,
): PendingIntent = getPendingIntentConsideringSingleActivity(
    context = context,
    legacyActivityClass = T::class.java,
    createPendingIntent = createPendingIntent,
    singleActivityPendingIntent = singleActivityPendingIntent,
)

/**
 * Get a PendingIntent considering the SingleActivity feature flag using reified generics.
 * If SingleActivity is enabled, returns the PendingIntent to the single activity with provided destination.
 * Otherwise, creates an Intent for the legacy activity and uses createPendingIntent to create the PendingIntent.
 *
 * If more than one destination is needed, please consider using getPendingIntentConsideringSingleActivity and create the intent with MegaActivity companion helper functions
 *
 * @param N The Activity class to create the Intent for when SingleActivity is disabled
 * @param context The Context
 * @param createPendingIntent A lambda that creates a PendingIntent from an Intent
 * @param singleActivityDestination A lambda that creates the NavKey destination to use when SingleActivity is enabled
 * @return The appropriate PendingIntent based on the feature flag
 */
suspend inline fun <reified A, N> AppNavigator.getPendingIntentConsideringSingleActivityWithDestination(
    context: Context,
    noinline createPendingIntent: (Intent) -> PendingIntent,
    noinline singleActivityDestination: () -> N,
): PendingIntent where N : NavKey, N : Parcelable, A : Activity =
    getPendingIntentConsideringSingleActivityWithDestination(
        context = context,
        legacyActivityClass = A::class.java,
        createPendingIntent = createPendingIntent,
        singleActivityDestination = singleActivityDestination,
    )
