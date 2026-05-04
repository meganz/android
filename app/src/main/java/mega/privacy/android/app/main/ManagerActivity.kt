package mega.privacy.android.app.main

import android.net.Uri
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.activities.contract.VersionsFileActivityContract
import mega.privacy.android.app.main.share.SharesViewModel
import mega.privacy.android.app.presentation.bottomsheet.model.NodeDeviceCenterInformation
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.notification.model.NotificationNavigationHandler
import mega.privacy.android.app.presentation.offline.offlinecompose.OfflineComposeFragment
import mega.privacy.android.app.presentation.rubbishbin.LegacyRubbishBinViewModel
import mega.privacy.android.app.presentation.shares.incoming.IncomingSharesComposeViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import nz.mega.sdk.MegaNode

/**
 * Legacy ManagerActivity stub.
 *
 * The activity has been retired as part of the ManagerActivity removal project. All external
 * callers were migrated to MegaActivity in Phase A; the navigator API surface was removed in
 * Phase B. This stub remains only because the eventual file deletion is user-owned (Phase E).
 *
 * `enforceSingleActivityGuard = true` ensures any stale Intent that still targets this activity
 * is redirected to MegaActivity at launch time, so none of the stub bodies below ever run.
 *
 * The members below preserve the public surface that circular fragments / adapters / dialogs
 * still reference at compile time. Each Phase C deletion MR will remove the corresponding
 * member here as the calling fragment is removed.
 */
@AndroidEntryPoint
class ManagerActivity : PasscodeActivity(), NotificationNavigationHandler {

    override val enforceSingleActivityGuard = true

    override fun navigateToSharedNode(nodeId: Long) = Unit
    override fun navigateToMyAccount() = Unit
    override fun navigateToContactInfo(email: String) = Unit
    override fun navigateToContactRequests() = Unit
    override fun moveToChatSection(chatId: Long) = Unit

    internal val rubbishBinViewModel: LegacyRubbishBinViewModel by viewModels()
    internal val incomingSharesViewModel: IncomingSharesComposeViewModel by viewModels()
    val sharesViewModel: SharesViewModel by viewModels()

    val versionsActivityLauncher =
        registerForActivityResult(VersionsFileActivityContract()) { /* no-op */ }

    var drawerItem: DrawerItem? = null
    @JvmField
    var openFolderRefresh: Boolean = false
    @JvmField
    var turnOnNotifications: Boolean = false
    var comesFromNotifications: Boolean = false
    var comesFromNotificationHandle: Long = Constants.INVALID_VALUE.toLong()
    var parentHandleBrowser: Long = -1L
    var isFirstNavigationLevel: Boolean = false

    val tabItemShares: SharesTab get() = SharesTab.INCOMING_TAB
    val deepBrowserTreeIncoming: Int = 0
    val deepBrowserTreeOutgoing: Int = 0

    fun setToolbarTitle() = Unit
    fun setToolbarTitle(title: String?) = Unit
    fun setToolbarTitleFromFullscreenOfflineFragment(
        firstNavigationLevel: Boolean,
        showSearch: Boolean,
    ) = Unit

    override fun showSnackbar(type: Int, content: String?, chatId: Long) = Unit
    override fun showSnackbar(type: Int, content: String, action: () -> Unit) = Unit

    fun restoreFromRubbish(nodes: List<TypedNode>) = Unit

    fun showNewSortByPanel(orderType: Int) = Unit

    fun showNodeOptionsPanel(
        node: MegaNode?,
        mode: Int = 0,
        shareData: ShareData? = null,
        nodeDeviceCenterInformation: NodeDeviceCenterInformation? = null,
        hideHiddenActions: Boolean = false,
    ) = Unit

    fun showNodeOptionsPanel(
        nodeId: NodeId?,
        mode: Int = 0,
        shareData: ShareData? = null,
        nodeDeviceCenterInformation: NodeDeviceCenterInformation? = null,
        hideHiddenActions: Boolean = false,
    ) = Unit

    fun showNodeLabelsPanel(node: NodeId) = Unit
    fun showNodeLabelsPanel(nodeIds: List<NodeId>) = Unit

    fun showRenameDialog(document: MegaNode?) = Unit
    fun showGetLinkActivity(nodes: List<MegaNode>?) = Unit
    fun showGetLinkActivity(handle: Long) = Unit
    fun showUploadPanel(uploadType: Int = 0) = Unit
    fun showShareBackupsFolderWarningDialog(node: MegaNode, nodeType: Int) = Unit
    fun showNewTextFileDialog(typedName: String?) = Unit

    fun saveNodeByTap(node: MegaNode) = Unit
    fun saveNodeByOpenWith(node: MegaNode) = Unit

    fun saveNodesToDevice(
        nodes: List<MegaNode?>?,
        highPriority: Boolean,
        isFolderLink: Boolean,
        fromChat: Boolean,
        withStartMessage: Boolean,
    ) = Unit

    fun saveHandlesToDevice(
        handles: List<Long?>?,
        highPriority: Boolean,
        withStartMessage: Boolean,
    ) = Unit

    fun attachNodeToChats(node: MegaNode?) = Unit
    fun attachNodesToChats(nodes: List<MegaNode?>?) = Unit
    fun viewNodeInFolder(node: MegaNode) = Unit

    fun onNodesCloudDriveUpdate() = Unit
    fun onNodesBackupsUpdate() = Unit
    fun refreshSharesFragments() = Unit
    fun exitBackupsPage() = Unit
    fun changeAppBarElevation(withElevation: Boolean, cause: Int = 0) = Unit
    fun adjustTransferWidgetPositionInHomepage() = Unit
    fun updateTransfersWidgetVisibility() = Unit
    fun hideTransfersWidget() = Unit

    fun setParentHandleBackups(parentHandleBackups: Long) = Unit
    fun setParentHandleRubbish(parentHandleRubbish: Long) = Unit
    fun setDeepBrowserTreeIncoming(deep: Int, parentHandle: Long?) = Unit

    fun getHandleFromLinksViewModel(): Long = -1L

    fun hideFabButton() = Unit
    fun showFabButton() = Unit
    fun showHideBottomNavigationView(hide: Boolean) = Unit
    fun setTextSubmitted() = Unit
    fun restoreSharesAfterComingFromNotifications() = Unit
    fun restoreRubbishAfterComingFromNotification() = Unit
    fun isInMediaDiscovery(): Boolean = false
    fun hideAdsView() = Unit
    fun handleShowingAds() = Unit
    fun onCreateMeeting() = Unit
    fun onJoinMeeting() = Unit
    fun showNewTextFileDialog() = Unit

    lateinit var appBarLayout: AppBarLayout
    lateinit var drawerLayout: DrawerLayout

    var comesFromNotificationHandleSaved: Long = Constants.INVALID_VALUE.toLong()
    val currentParentHandle: Long = -1L
    val isInMainHomePage: Boolean = false

    @JvmOverloads
    fun selectDrawerItem(
        item: DrawerItem?,
        chatId: Long? = null,
        cloudDriveNodeHandle: Long = -1L,
        backupsHandle: Long = -1L,
        @StringRes errorMessage: Int? = null,
        isFromSyncFolders: Boolean = false,
    ) = Unit

    fun deleteTurnOnNotificationsFragment() = Unit

    fun closeSearchView() = Unit
    suspend fun getCurrentParentNode(parentHandle: Long, error: Int): MegaNode? = null
    fun destroyPermissionsFragment(isCameraUploadsEnabled: Boolean) = Unit
    fun fullscreenOfflineFragmentComposeOpened(fragment: OfflineComposeFragment?) = Unit
    fun fullscreenOfflineFragmentComposeClosed(fragment: OfflineComposeFragment) = Unit
    fun pagerOfflineComposeFragmentOpened(fragment: OfflineComposeFragment?) = Unit
    fun pagerOfflineComposeFragmentClosed(fragment: OfflineComposeFragment) = Unit
    fun handleCloudDriveBackNavigation(performBackNavigation: Boolean) = Unit
    fun handleFileUris(uris: List<Uri>, pitagTrigger: PitagTrigger) = Unit
    fun handleVideoSectionAddAction() = Unit
    fun hideKeyboardSearch() = Unit
    fun homepageToSearch() = Unit
    fun moveToSettingsSectionStartScreen() = Unit
    fun onDocumentScannerFailedToOpen() = Unit
    fun onShareTabChanged() = Unit
    fun openDrawer() = Unit
    fun openSearchOnHomepage() = Unit
    fun returnCall() = Unit
    fun showMyAccount() = Unit
    fun takePictureAndUpload() = Unit
    fun uploadFiles() = Unit
}
