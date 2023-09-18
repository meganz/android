@file:Suppress("DEPRECATION")

package mega.privacy.android.app.main

import mega.privacy.android.core.R as CoreUiR
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.app.SearchManager
import android.content.BroadcastReceiver
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Display
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.Chronometer
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.MenuItemCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.BusinessExpiredAlertActivity
import mega.privacy.android.app.DownloadService
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.OpenPasswordLinkActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.UploadService
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_CLOSE_CHAT_AFTER_OPEN_TRANSFERS
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_CREDENTIALS
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_FIRST_NAME
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_LAST_NAME
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_NICKNAME
import mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE
import mega.privacy.android.app.constants.BroadcastConstants.EXTRA_USER_HANDLE
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_ON_HOLD_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_STATUS_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_ON_HOLD_CHANGE
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.contacts.ContactsActivity
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.fragments.homepage.HomepageSearchable
import mega.privacy.android.app.fragments.homepage.documents.DocumentsFragment
import mega.privacy.android.app.fragments.homepage.main.HomepageFragment
import mega.privacy.android.app.fragments.homepage.main.HomepageFragmentDirections
import mega.privacy.android.app.fragments.settingsFragments.cookie.CookieDialogHandler
import mega.privacy.android.app.generalusecase.FilePrepareUseCase
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.interfaces.ActionBackupListener
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.ChatManagementCallback
import mega.privacy.android.app.interfaces.MeetingBottomSheetDialogActionListener
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.listeners.ExportListener
import mega.privacy.android.app.listeners.LoadPreviewListener
import mega.privacy.android.app.listeners.RemoveFromChatRoomListener
import mega.privacy.android.app.main.controllers.ContactController
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.dialog.ClearRubbishBinDialogFragment
import mega.privacy.android.app.main.dialog.Enable2FADialogFragment
import mega.privacy.android.app.main.dialog.businessgrace.BusinessGraceDialogFragment
import mega.privacy.android.app.main.dialog.connect.ConfirmConnectDialogFragment
import mega.privacy.android.app.main.dialog.contactlink.ContactLinkDialogFragment
import mega.privacy.android.app.main.dialog.storagestatus.StorageStatusDialogFragment
import mega.privacy.android.app.main.listeners.CreateGroupChatWithPublicLink
import mega.privacy.android.app.main.listeners.FabButtonListener
import mega.privacy.android.app.main.managerSections.ManagerUploadBottomSheetDialogActionHandler
import mega.privacy.android.app.main.managerSections.TurnOnNotificationsFragment
import mega.privacy.android.app.main.mapper.ManagerRedirectIntentMapper
import mega.privacy.android.app.main.megachat.BadgeDrawerArrowDrawable
import mega.privacy.android.app.main.megachat.ChatActivity
import mega.privacy.android.app.main.tasks.CheckOfflineNodesTask
import mega.privacy.android.app.mediaplayer.miniplayer.MiniAudioPlayerController
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.fragments.MeetingHasEndedDialogFragment
import mega.privacy.android.app.middlelayer.inappupdate.InAppUpdateHandler
import mega.privacy.android.app.modalbottomsheet.MeetingBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.SortByBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.UploadBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.nodelabel.NodeLabelBottomSheetDialogFragment
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.presentation.backups.BackupsFragment
import mega.privacy.android.app.presentation.backups.BackupsViewModel
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.bottomsheet.UploadBottomSheetDialogActionListener
import mega.privacy.android.app.presentation.chat.archived.ArchivedChatsActivity
import mega.privacy.android.app.presentation.chat.list.ChatTabsFragment
import mega.privacy.android.app.presentation.clouddrive.FileBrowserComposeFragment
import mega.privacy.android.app.presentation.clouddrive.FileBrowserViewModel
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.extensions.serializable
import mega.privacy.android.app.presentation.fileinfo.FileInfoActivity
import mega.privacy.android.app.presentation.filelink.FileLinkActivity
import mega.privacy.android.app.presentation.filelink.FileLinkComposeActivity
import mega.privacy.android.app.presentation.fingerprintauth.SecurityUpgradeDialogFragment
import mega.privacy.android.app.presentation.folderlink.FolderLinkComposeActivity
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.manager.ManagerViewModel
import mega.privacy.android.app.presentation.manager.UnreadUserAlertsCheckType
import mega.privacy.android.app.presentation.manager.UserInfoViewModel
import mega.privacy.android.app.presentation.manager.model.ManagerState
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.manager.model.Tab
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.presentation.mapper.RestoreNodeResultMapper
import mega.privacy.android.app.presentation.meeting.CreateScheduledMeetingActivity
import mega.privacy.android.app.presentation.meeting.WaitingRoomManagementViewModel
import mega.privacy.android.app.presentation.meeting.view.UsersInWaitingRoomDialog
import mega.privacy.android.app.presentation.movenode.mapper.MoveRequestMessageMapper
import mega.privacy.android.app.presentation.notification.NotificationsFragment
import mega.privacy.android.app.presentation.notification.model.NotificationNavigationHandler
import mega.privacy.android.app.presentation.offline.OfflineFragment
import mega.privacy.android.app.presentation.offline.offlinecompose.OfflineFragmentCompose
import mega.privacy.android.app.presentation.permissions.PermissionsFragment
import mega.privacy.android.app.presentation.photos.PhotosFragment
import mega.privacy.android.app.presentation.photos.albums.AlbumDynamicContentFragment
import mega.privacy.android.app.presentation.photos.albums.AlbumScreenWrapperActivity
import mega.privacy.android.app.presentation.photos.albums.albumcontent.AlbumContentFragment
import mega.privacy.android.app.presentation.photos.mediadiscovery.MediaDiscoveryFragment
import mega.privacy.android.app.presentation.photos.timeline.photosfilter.PhotosFilterFragment
import mega.privacy.android.app.presentation.qrcode.QRCodeActivity
import mega.privacy.android.app.presentation.qrcode.scan.ScanCodeFragment
import mega.privacy.android.app.presentation.recentactions.recentactionbucket.RecentActionBucketFragment
import mega.privacy.android.app.presentation.rubbishbin.RubbishBinComposeFragment
import mega.privacy.android.app.presentation.rubbishbin.RubbishBinViewModel
import mega.privacy.android.app.presentation.search.SearchFragment
import mega.privacy.android.app.presentation.search.SearchViewModel
import mega.privacy.android.app.presentation.settings.SettingsActivity
import mega.privacy.android.app.presentation.settings.exportrecoverykey.ExportRecoveryKeyActivity
import mega.privacy.android.app.presentation.settings.model.TargetPreference
import mega.privacy.android.app.presentation.settings.startscreen.util.StartScreenUtil.CHAT_BNV
import mega.privacy.android.app.presentation.settings.startscreen.util.StartScreenUtil.CLOUD_DRIVE_BNV
import mega.privacy.android.app.presentation.settings.startscreen.util.StartScreenUtil.HOME_BNV
import mega.privacy.android.app.presentation.settings.startscreen.util.StartScreenUtil.NO_BNV
import mega.privacy.android.app.presentation.settings.startscreen.util.StartScreenUtil.PHOTOS_BNV
import mega.privacy.android.app.presentation.settings.startscreen.util.StartScreenUtil.SHARED_ITEMS_BNV
import mega.privacy.android.app.presentation.settings.startscreen.util.StartScreenUtil.getStartBottomNavigationItem
import mega.privacy.android.app.presentation.settings.startscreen.util.StartScreenUtil.getStartDrawerItem
import mega.privacy.android.app.presentation.settings.startscreen.util.StartScreenUtil.setStartScreenTimeStamp
import mega.privacy.android.app.presentation.settings.startscreen.util.StartScreenUtil.shouldCloseApp
import mega.privacy.android.app.presentation.shares.MegaNodeBaseFragment
import mega.privacy.android.app.presentation.shares.SharesPageAdapter
import mega.privacy.android.app.presentation.shares.incoming.IncomingSharesViewModel
import mega.privacy.android.app.presentation.shares.incoming.model.IncomingSharesState
import mega.privacy.android.app.presentation.shares.links.LegacyLinksViewModel
import mega.privacy.android.app.presentation.shares.outgoing.OutgoingSharesViewModel
import mega.privacy.android.app.presentation.shares.outgoing.model.OutgoingSharesState
import mega.privacy.android.app.presentation.startconversation.StartConversationActivity
import mega.privacy.android.app.presentation.transfers.TransfersManagementActivity
import mega.privacy.android.app.presentation.transfers.page.TransferPageFragment
import mega.privacy.android.app.presentation.transfers.page.TransferPageViewModel
import mega.privacy.android.app.psa.PsaManager
import mega.privacy.android.app.psa.PsaViewHolder
import mega.privacy.android.app.service.iar.RatingHandlerImpl
import mega.privacy.android.app.service.push.MegaMessageService
import mega.privacy.android.app.sync.fileBackups.FileBackupManager
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.BackupDialogState.BACKUP_DIALOG_SHOW_CONFIRM
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.BackupDialogState.BACKUP_DIALOG_SHOW_NONE
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.BackupDialogState.BACKUP_DIALOG_SHOW_WARNING
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.OperationType.OPERATION_EXECUTE
import mega.privacy.android.app.upgradeAccount.UpgradeAccountActivity
import mega.privacy.android.app.usecase.DownloadNodeUseCase
import mega.privacy.android.app.usecase.UploadUseCase
import mega.privacy.android.app.usecase.chat.GetChatChangesUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.app.usecase.exception.NotEnoughQuotaMegaException
import mega.privacy.android.app.usecase.exception.QuotaExceededMegaException
import mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.ColorUtils.tintIcon
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_BACKUP_FAB
import mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_BACKUP_SHARE_FOLDER
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_ACTION_TYPE
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_DIALOG_WARN
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_HANDLED_ITEM
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_HANDLED_NODE
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_NODE_TYPE
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showRenameNodeDialog
import mega.privacy.android.app.utils.MegaNodeUtil.getRootParentNode
import mega.privacy.android.app.utils.MegaNodeUtil.showTakenDownNodeActionNotAvailableDialog
import mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog
import mega.privacy.android.app.utils.MegaProgressDialogUtil.showProcessFileDialog
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.app.utils.UploadUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import mega.privacy.android.app.zippreview.ui.ZipBrowserActivity
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.data.model.MegaAttributes
import mega.privacy.android.data.model.MegaPreferences
import mega.privacy.android.domain.entity.MyAccountUpdate
import mega.privacy.android.domain.entity.MyAccountUpdate.Action
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.RestoreNodeResult
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.entity.psa.Psa
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.chat.HasArchivedChatsUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.MonitorEphemeralCredentialsUseCase
import mega.privacy.android.feature.devicecenter.ui.DeviceCenterFragment
import mega.privacy.android.feature.sync.ui.SyncFragment
import mega.privacy.mobile.analytics.event.CloudDriveSearchMenuToolbarEvent
import mega.privacy.mobile.analytics.event.IncomingSharesTabEvent
import mega.privacy.mobile.analytics.event.LinkSharesTabEvent
import mega.privacy.mobile.analytics.event.OutgoingSharesTabEvent
import mega.privacy.mobile.analytics.event.SearchResultOverflowMenuItemEvent
import mega.privacy.mobile.analytics.event.SharedItemsScreenEvent
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatPeerList
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRequestListenerInterface
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaFolderInfo
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaShare
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaUser
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@Suppress("KDocMissingDocumentation")
@AndroidEntryPoint
class ManagerActivity : TransfersManagementActivity(), MegaRequestListenerInterface,
    MegaChatRequestListenerInterface, NavigationView.OnNavigationItemSelectedListener,
    View.OnClickListener,
    BottomNavigationView.OnNavigationItemSelectedListener, UploadBottomSheetDialogActionListener,
    ChatManagementCallback, ActionNodeCallback, SnackbarShower,
    MeetingBottomSheetDialogActionListener, LoadPreviewListener.OnPreviewLoadedCallback,
    NotificationNavigationHandler,
    ParentNodeManager, CameraPermissionManager, NavigationDrawerManager {
    /**
     * The cause bitmap of elevating the app bar
     */
    private var mElevationCause = 0

    /**
     * True if any TabLayout is visible
     */
    private var mShowAnyTabLayout = false

    /**
     * Indicates that ManagerActivity was called from Image Viewer;
     * Transfers tab should go back to [mega.privacy.android.app.imageviewer.ImageViewerActivity]
     */
    private var transfersToImageViewer = false

    internal val viewModel: ManagerViewModel by viewModels()
    internal val fileBrowserViewModel: FileBrowserViewModel by viewModels()
    internal val incomingSharesViewModel: IncomingSharesViewModel by viewModels()
    internal val outgoingSharesViewModel: OutgoingSharesViewModel by viewModels()
    internal val backupsViewModel: BackupsViewModel by viewModels()
    internal val legacyLinksViewModel: LegacyLinksViewModel by viewModels()
    internal val rubbishBinViewModel: RubbishBinViewModel by viewModels()
    internal val searchViewModel: SearchViewModel by viewModels()
    private val userInfoViewModel: UserInfoViewModel by viewModels()
    private val transferPageViewModel: TransferPageViewModel by viewModels()
    private val waitingRoomManagementViewModel: WaitingRoomManagementViewModel by viewModels()

    @Inject
    lateinit var cookieDialogHandler: CookieDialogHandler

    @Inject
    lateinit var filePrepareUseCase: FilePrepareUseCase

    @Inject
    lateinit var getChatChangesUseCase: GetChatChangesUseCase

    @Inject
    lateinit var downloadNodeUseCase: DownloadNodeUseCase

    @Inject
    lateinit var checkNameCollisionUseCase: CheckNameCollisionUseCase

    @Inject
    lateinit var uploadUseCase: UploadUseCase

    @Inject
    lateinit var activityLifecycleHandler: ActivityLifecycleHandler

    @Inject
    internal lateinit var uploadBottomSheetDialogActionHandler: ManagerUploadBottomSheetDialogActionHandler

    @Inject
    lateinit var copyRequestMessageMapper: CopyRequestMessageMapper

    @Inject
    lateinit var moveRequestMessageMapper: MoveRequestMessageMapper

    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    @Inject
    lateinit var monitorEphemeralCredentialsUseCase: MonitorEphemeralCredentialsUseCase

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    lateinit var restoreNodeResultMapper: RestoreNodeResultMapper

    @Inject
    lateinit var hasArchivedChatsUseCase: HasArchivedChatsUseCase

    @Inject
    lateinit var managerRedirectIntentMapper: ManagerRedirectIntentMapper

    @Inject
    lateinit var inAppUpdateHandler: InAppUpdateHandler

    //GET PRO ACCOUNT PANEL
    private lateinit var getProLayout: LinearLayout
    private lateinit var getProText: TextView
    private lateinit var leftCancelButton: TextView
    private lateinit var rightUpgradeButton: TextView
    private lateinit var fabButton: FloatingActionButton
    private var pendingActionsBadge: View? = null

    var rootNode: MegaNode? = null
    val nodeController: NodeController by lazy { NodeController(this) }
    private val contactController: ContactController by lazy { ContactController(this) }
    private val nodeAttacher: MegaAttacher by lazy { MegaAttacher(this) }
    private val nodeSaver: NodeSaver by lazy {
        NodeSaver(
            this, this, this,
            AlertsAndWarnings.showSaveToDeviceConfirmDialog(this)
        )
    }

    private val badgeDrawable: BadgeDrawerArrowDrawable by lazy {
        BadgeDrawerArrowDrawable(
            this, R.color.red_600_red_300,
            R.color.white_dark_grey, R.color.white_dark_grey
        )
    }
    var prefs: MegaPreferences? = null
    var attr: MegaAttributes? = null
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var fragmentContainer: FragmentContainerView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var appBarLayout: AppBarLayout

    private var selectedAccountType = 0
    private var infoManager: ShareInfo? = null
    private var parentNodeManager: MegaNode? = null

    lateinit var drawerLayout: DrawerLayout

    @JvmField
    var openFolderRefresh = false
    var newAccount = false
    private var firstLogin = false
    private var newCreationAccount = false
    private var storageState: StorageState = StorageState.Unknown //Default value
    private var storageStateFromBroadcast: StorageState = StorageState.Unknown //Default value
    private var showStorageAlertWithDelay = false

    private var reconnectDialog: AlertDialog? = null

    private var orientationSaved = 0

    // Determine if in Media discovery page, if it is true, it must in CD drawerItem tab
    private var isInMDMode = false
    private var isInFilterPage = false
    private var isInAlbumContent = false
    var fromAlbumContent = false

    @JvmField
    var turnOnNotifications = false

    override var drawerItem: DrawerItem? = null
    private lateinit var fragmentLayout: LinearLayout
    private lateinit var waitingRoomComposeView: ComposeView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var navigationView: NavigationView

    private var miniAudioPlayerController: MiniAudioPlayerController? = null
    private lateinit var cameraUploadViewTypes: LinearLayout

    //Tabs in Shares
    private lateinit var tabLayoutShares: TabLayout
    private val sharesPageAdapter: SharesPageAdapter by lazy { SharesPageAdapter(this) }
    private lateinit var viewPagerShares: ViewPager2

    //Tabs in Transfers
    private lateinit var callInProgressLayout: RelativeLayout
    private lateinit var callInProgressChrono: Chronometer
    private lateinit var callInProgressText: TextView

    @JvmField
    var firstTimeAfterInstallation = true
    var searchView: SearchView? = null
    var searchExpand = false
    private var isSearching = false
    var openLink = false
    private var requestNotificationsPermissionFirstLogin = false
    var askPermissions = false
        private set
    var megaContacts = true
    private var homepageScreen = HomepageScreen.HOMEPAGE
    var pathNavigationOffline: String? = null

    // Fragments
    private var fileBrowserComposeFragment: FileBrowserComposeFragment? = null
    private var rubbishBinComposeFragment: RubbishBinComposeFragment? = null
    private var syncFragment: SyncFragment? = null
    private var deviceCenterFragment: DeviceCenterFragment? = null
    private var backupsFragment: BackupsFragment? = null
    private var incomingSharesFragment: MegaNodeBaseFragment? = null
    private var outgoingSharesFragment: MegaNodeBaseFragment? = null
    private var linksFragment: MegaNodeBaseFragment? = null
    private var searchFragment: SearchFragment? = null
    private var photosFragment: PhotosFragment? = null
    private var albumContentFragment: Fragment? = null
    private var photosFilterFragment: PhotosFilterFragment? = null
    private var chatTabsFragment: ChatTabsFragment? = null
    private var turnOnNotificationsFragment: TurnOnNotificationsFragment? = null
    private var permissionsFragment: PermissionsFragment? = null
    private var mediaDiscoveryFragment: Fragment? = null
    private var mStopped = true
    private var bottomItemBeforeOpenFullscreenOffline = Constants.INVALID_VALUE
    private var fullscreenOfflineFragmentCompose: OfflineFragmentCompose? = null
    private var pagerOfflineFragmentCompose: OfflineFragmentCompose? = null
    private var fullscreenOfflineFragment: OfflineFragment? = null
    private var pagerOfflineFragment: OfflineFragment? = null

    var statusDialog: AlertDialog? = null
    private var processFileDialog: AlertDialog? = null
    private var permissionsDialog: AlertDialog? = null

    private var searchMenuItem: MenuItem? = null
    private var doNotDisturbMenuItem: MenuItem? = null
    private var archivedMenuItem: MenuItem? = null
    private var clearRubbishBinMenuItem: MenuItem? = null
    private var returnCallMenuItem: MenuItem? = null
    private var openLinkMenuItem: MenuItem? = null
    private var chronometerMenuItem: Chronometer? = null
    private var layoutCallMenuItem: LinearLayout? = null
    private var typesCameraPermission = Constants.INVALID_TYPE_PERMISSIONS

    var comesFromNotifications = false
    var comesFromNotificationsLevel = 0
    var comesFromNotificationHandle = Constants.INVALID_VALUE.toLong()
    var comesFromNotificationHandleSaved = Constants.INVALID_VALUE.toLong()
    private var comesFromNotificationDeepBrowserTreeIncoming = Constants.INVALID_VALUE
    private var comesFromNotificationChildNodeHandleList: LongArray? = null
    private var comesFromNotificationSharedIndex: SharesTab = SharesTab.NONE
    private var bottomNavigationCurrentItem = -1

    private lateinit var chatBadge: View
    private lateinit var callBadge: View
    private val menuView: BottomNavigationMenuView
        get() = bottomNavigationView.getChildAt(0) as BottomNavigationMenuView

    private var joiningToChatLink = false
    private var linkJoinToChatLink: String? = null
    private var onAskingPermissionsFragment = false
    private var initialPermissionsAlreadyAsked = false
    private lateinit var navHostView: View
    private var navController: NavController? = null
    private var mHomepageSearchable: HomepageSearchable? = null
    private var initFabButtonShow = false
    private val fabChangeObserver = Observer { isShow: Boolean ->
        if (drawerItem === DrawerItem.HOMEPAGE) {
            controlFabInHomepage(isShow)
        } else if (isInMDMode) {
            hideFabButton()
        } else {
            if (initFabButtonShow) {
                if (isShow) {
                    showFabButtonAfterScrolling()
                } else {
                    hideFabButtonWhenScrolling()
                }
            }
        }
    }
    private var bottomSheetDialogFragment: BottomSheetDialogFragment? = null
    private var psaViewHolder: PsaViewHolder? = null
    private var openLinkDialog: AlertDialog? = null
    private var openLinkDialogIsErrorShown = false
    private var openLinkText: EditText? = null
    private var openLinkError: RelativeLayout? = null
    private var openLinkErrorText: TextView? = null
    private var openLinkOpenButton: Button? = null
    private var chatLinkDialogType = LINK_DIALOG_CHAT

    // end for Meeting
    private var backupHandleList: ArrayList<Long>? = null
    private var backupDialogType: Int = BACKUP_DIALOG_SHOW_NONE
    private var backupNodeHandle: Long = -1
    private var backupNodeType = 0
    private var backupActionType = 0

    // Version removed
    private var versionsToRemove = 0
    private var versionsRemoved = 0
    private var errorVersionRemove = 0
    var viewInFolderNode: MegaNode? = null

    private var showDialogStorageStatusJob: Job? = null

    private val contactUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == null) return
            val userHandle = intent.getLongExtra(EXTRA_USER_HANDLE, INVALID_HANDLE)
            if (intent.action == ACTION_UPDATE_NICKNAME || intent.action == ACTION_UPDATE_FIRST_NAME || intent.action == ACTION_UPDATE_LAST_NAME) {
                if (isIncomingAdded && (incomingSharesFragment?.itemCount ?: 0) > 0) {
                    incomingSharesFragment?.updateContact(userHandle)
                }
                if (isOutgoingAdded && (outgoingSharesFragment?.itemCount ?: 0) > 0) {
                    outgoingSharesFragment?.updateContact(userHandle)
                }
            }
        }
    }
    private val callStatusObserver: Observer<MegaChatCall> =
        Observer { call: MegaChatCall ->
            when (call.status) {
                MegaChatCall.CALL_STATUS_CONNECTING, MegaChatCall.CALL_STATUS_IN_PROGRESS, MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION, MegaChatCall.CALL_STATUS_DESTROYED, MegaChatCall.CALL_STATUS_USER_NO_PRESENT -> {
                    updateVisibleCallElements(call.chatid)
                    if (call.status == MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION &&
                        call.termCode == MegaChatCall.TERM_CODE_TOO_MANY_PARTICIPANTS
                    ) {
                        showSnackbar(
                            Constants.SNACKBAR_TYPE,
                            getString(R.string.call_error_too_many_participants),
                            MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                        )
                    }
                }
            }
        }
    private val callOnHoldObserver: Observer<MegaChatCall> =
        Observer { call: MegaChatCall -> updateVisibleCallElements(call.chatid) }
    private val sessionOnHoldObserver =
        Observer { sessionAndCall: android.util.Pair<*, *> ->
            val call: MegaChatCall = megaChatApi.getChatCallByCallId(sessionAndCall.first as Long)
            updateVisibleCallElements(call.chatid)
        }
    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goBack()
            }
        }

    private val fileBackupManager: FileBackupManager = initFileBackupManager()

    /**
     * Feature Flag for OfflineCompose
     */
    private var enableOfflineCompose: Boolean = false


    /**
     * Method for updating the visible elements related to a call.
     *
     * @param chatIdReceived The chat ID of a call.
     */
    private fun updateVisibleCallElements(chatIdReceived: Long) {
        if (Util.isScreenInPortrait(this@ManagerActivity)) {
            setCallWidget()
        } else {
            invalidateOptionsMenu()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constants.REQUEST_UPLOAD_CONTACT -> {
                uploadContactInfo(infoManager, parentNodeManager)
            }

            Constants.REQUEST_CAMERA -> {
                if (typesCameraPermission == Constants.TAKE_PICTURE_OPTION) {
                    Timber.d("TAKE_PICTURE_OPTION")
                    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        if (!hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            requestPermission(
                                this,
                                Constants.REQUEST_WRITE_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                        } else {
                            Util.checkTakePicture(this, Constants.TAKE_PHOTO_CODE)
                            typesCameraPermission = Constants.INVALID_TYPE_PERMISSIONS
                        }
                    }
                }
            }

            Constants.REQUEST_READ_WRITE_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showUploadPanel()
                }
            }

            Constants.REQUEST_WRITE_STORAGE -> {
                if (viewModel.state().isFirstLogin) {
                    Timber.d("The first time")
                    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        if (typesCameraPermission == Constants.TAKE_PICTURE_OPTION) {
                            Timber.d("TAKE_PICTURE_OPTION")
                            if (!hasPermissions(this, Manifest.permission.CAMERA)) {
                                requestPermission(
                                    this,
                                    Constants.REQUEST_CAMERA,
                                    Manifest.permission.CAMERA
                                )
                            } else {
                                Util.checkTakePicture(this, Constants.TAKE_PHOTO_CODE)
                                typesCameraPermission = Constants.INVALID_TYPE_PERMISSIONS
                            }
                        }
                    }
                } else {
                    if (typesCameraPermission == Constants.TAKE_PICTURE_OPTION) {
                        Timber.d("TAKE_PICTURE_OPTION")
                        if (!hasPermissions(this, Manifest.permission.CAMERA)) {
                            requestPermission(
                                this,
                                Constants.REQUEST_CAMERA,
                                Manifest.permission.CAMERA
                            )
                        } else {
                            Util.checkTakePicture(this, Constants.TAKE_PHOTO_CODE)
                            typesCameraPermission = Constants.INVALID_TYPE_PERMISSIONS
                        }
                    } else {
                        refreshOfflineNodes()
                    }
                }
                nodeSaver.handleRequestPermissionsResult(requestCode)
            }

            PermissionsFragment.PERMISSIONS_FRAGMENT -> {
                if (getPermissionsFragment() != null) {
                    permissionsFragment?.setNextPermission()
                }
            }
        }
    }

    override fun setTypesCameraPermission(typesCameraPermission: Int) {
        this.typesCameraPermission = typesCameraPermission
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Timber.d("onSaveInstanceState")
        if (drawerItem != null) {
            Timber.d("DrawerItem = %s", drawerItem)
        } else {
            Timber.w("DrawerItem is null")
        }
        super.onSaveInstanceState(outState)
        outState.putSerializable("drawerItem", drawerItem)
        outState.putInt(
            BOTTOM_ITEM_BEFORE_OPEN_FULLSCREEN_OFFLINE,
            bottomItemBeforeOpenFullscreenOffline
        )
        outState.putString("pathNavigationOffline", pathNavigationOffline)
        if (turnOnNotifications) {
            outState.putBoolean("turnOnNotifications", turnOnNotifications)
        }
        outState.putInt("orientationSaved", orientationSaved)
        outState.putInt("bottomNavigationCurrentItem", bottomNavigationCurrentItem)
        outState.putBoolean("searchExpand", searchExpand)
        outState.putBoolean("comesFromNotifications", comesFromNotifications)
        outState.putInt("comesFromNotificationsLevel", comesFromNotificationsLevel)
        outState.putLong("comesFromNotificationHandle", comesFromNotificationHandle)
        outState.putLong("comesFromNotificationHandleSaved", comesFromNotificationHandleSaved)
        outState.putSerializable(
            COMES_FROM_NOTIFICATIONS_SHARED_INDEX,
            comesFromNotificationSharedIndex
        )
        outState.putBoolean("onAskingPermissionsFragment", onAskingPermissionsFragment)
        permissionsFragment =
            supportFragmentManager.findFragmentByTag(FragmentTag.PERMISSIONS.tag) as? PermissionsFragment
        if (onAskingPermissionsFragment && permissionsFragment != null) {
            permissionsFragment?.let {
                supportFragmentManager.putFragment(
                    outState,
                    FragmentTag.PERMISSIONS.tag,
                    it
                )
            }
        }
        outState.putInt("elevation", mElevationCause)
        outState.putSerializable("storageState", storageState)
        outState.putInt(
            "comesFromNotificationDeepBrowserTreeIncoming",
            comesFromNotificationDeepBrowserTreeIncoming
        )
        if (isAlertDialogShown(openLinkDialog)) {
            outState.putBoolean(OPEN_LINK_DIALOG_SHOWN, true)
            outState.putBoolean(OPEN_LINK_ERROR, openLinkDialogIsErrorShown)
            outState.putString(
                OPEN_LINK_TEXT,
                openLinkText?.text?.toString() ?: ""
            )
        }
        outState.putInt(Constants.TYPE_CALL_PERMISSION, typesCameraPermission)
        outState.putBoolean(JOINING_CHAT_LINK, joiningToChatLink)
        outState.putString(LINK_JOINING_CHAT_LINK, linkJoinToChatLink)
        photosFragment?.let {
            if (photosFragment?.isAdded == true) {
                supportFragmentManager.putFragment(
                    outState,
                    FragmentTag.PHOTOS.tag,
                    it
                )
            }
        }
        nodeAttacher.saveState(outState)
        nodeSaver.saveState(outState)
        uploadBottomSheetDialogActionHandler.onSaveInstanceState(outState)
        outState.putBoolean(PROCESS_FILE_DIALOG_SHOWN, isAlertDialogShown(processFileDialog))
        outState.putBoolean(STATE_KEY_IS_IN_MD_MODE, isInMDMode)
        mediaDiscoveryFragment =
            supportFragmentManager.findFragmentByTag(FragmentTag.MEDIA_DISCOVERY.tag)
        mediaDiscoveryFragment?.let {
            supportFragmentManager.putFragment(
                outState,
                FragmentTag.MEDIA_DISCOVERY.tag,
                it
            )
        }
        outState.putBoolean(STATE_KEY_IS_IN_ALBUM_CONTENT, isInAlbumContent)
        albumContentFragment =
            supportFragmentManager.findFragmentByTag(FragmentTag.ALBUM_CONTENT.tag)
        albumContentFragment?.let {
            supportFragmentManager.putFragment(
                outState,
                FragmentTag.ALBUM_CONTENT.tag,
                it
            )
        }
        outState.putBoolean(STATE_KEY_IS_IN_PHOTOS_FILTER, isInFilterPage)
        photosFilterFragment =
            supportFragmentManager.findFragmentByTag(FragmentTag.PHOTOS_FILTER.tag) as? PhotosFilterFragment
        photosFilterFragment?.let {
            supportFragmentManager.putFragment(
                outState,
                FragmentTag.PHOTOS_FILTER.tag,
                it
            )
        }

        // Backup warning dialog
        val backupWarningDialog: AlertDialog? = fileBackupManager.backupWarningDialog
        if (backupWarningDialog?.isShowing == true) {
            backupHandleList = fileBackupManager.backupHandleList
            backupNodeHandle = fileBackupManager.backupNodeHandle ?: -1
            backupNodeType = fileBackupManager.backupNodeType
            backupActionType = fileBackupManager.backupActionType
            backupDialogType = fileBackupManager.backupDialogType
            if (backupHandleList != null) {
                outState.putSerializable(BACKUP_HANDLED_ITEM, backupHandleList)
            }
            outState.putLong(BACKUP_HANDLED_NODE, backupNodeHandle)
            outState.putInt(BACKUP_NODE_TYPE, backupNodeType)
            outState.putInt(BACKUP_ACTION_TYPE, backupActionType)
            outState.putInt(BACKUP_DIALOG_WARN, backupDialogType)
            backupWarningDialog.dismiss()
        }
    }

    override fun onStart() {
        Timber.d("onStart")
        mStopped = false
        super.onStart()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            enableOfflineCompose =
                getFeatureFlagValueUseCase(AppFeatures.OfflineCompose)
        }

        Timber.d("onCreate after call super")
        registerViewModelObservers()
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        if (handleDuplicateLaunches()) return
        lifecycle.addObserver(cookieDialogHandler)
        if (savedInstanceState != null) {
            //Do after view instantiation
            restoreFromSavedInstanceState(savedInstanceState)
        } else {
            Timber.d("Bundle is NULL")
            pathNavigationOffline = Constants.OFFLINE_ROOT
        }
        registerBroadcastReceivers()
        registerEventBusObservers()
        CacheFolderManager.createCacheFolders()
        checkChatChanges()
        Timber.d("retryChatPendingConnections()")
        megaChatApi.retryPendingConnections(false, null)
        MegaApplication.getPushNotificationSettingManagement().pushNotificationSetting

        val display: Display = windowManager.defaultDisplay
        display.getMetrics(outMetrics)
        if (checkDatabaseValues()) return
        if (firstTimeAfterInstallation) {
            setStartScreenTimeStamp(this)
        }
        setupView()
        setGetProLabelText()
        initialiseChatBadgeView()
        setCallBadge()
        if (mElevationCause > 0) {
            // A work around: mElevationCause will be changed unexpectedly shortly
            val elevationCause = mElevationCause
            // Apply the previous Appbar elevation(e.g. before rotation) after all views have been created
            handler.postDelayed({ changeAppBarElevation(true, elevationCause) }, 100)
        }
        setTransfersWidgetLayout(findViewById(R.id.transfers_widget_layout))
        setupAudioPlayerController()
        megaApi.getAccountAchievements(this)
        if (!viewModel.isConnected) {
            Timber.d("No network -> SHOW OFFLINE MODE")
            if (drawerItem == null) {
                drawerItem = DrawerItem.HOMEPAGE
            }
            selectDrawerItem(drawerItem)
            showOfflineMode()
            dbH.credentials?.let {
                val gSession = it.session
                ChatUtil.initMegaChatApi(gSession, this)
            }
            return
        }
        viewModel.renameRecoveryKeyFileIfNeeded(
            FileUtil.OLD_MK_FILE,
            FileUtil.getRecoveryKeyFileName(this)
        )
        viewModel.renameRecoveryKeyFileIfNeeded(
            FileUtil.OLD_RK_FILE,
            FileUtil.getRecoveryKeyFileName(this)
        )
        if (handleRootNodeAndHeartbeatState(savedInstanceState)) return
        userInfoViewModel.checkPasswordReminderStatus()
        checkInitialScreens()
        lifecycleScope.launch {
            PsaManager.checkPsa()
        }
        uploadBottomSheetDialogActionHandler.onRestoreInstanceState(savedInstanceState)
        Timber.d("END onCreate")
        RatingHandlerImpl(this).showRatingBaseOnTransaction()
        when (backupDialogType) {
            BACKUP_DIALOG_SHOW_WARNING -> {
                fileBackupManager.actWithBackupTips(
                    backupHandleList,
                    megaApi.getNodeByHandle(backupNodeHandle),
                    backupNodeType,
                    backupActionType,
                    fileBackupManager.actionBackupNodeCallback
                )
            }

            BACKUP_DIALOG_SHOW_CONFIRM -> {
                fileBackupManager.confirmationActionForBackup(
                    backupHandleList,
                    megaApi.getNodeByHandle(backupNodeHandle),
                    backupNodeType,
                    backupActionType,
                    fileBackupManager.defaultActionBackupNodeCallback
                )
            }

            else -> {
                Timber.d("Backup warning dialog is not show")
            }
        }
        checkForInAppUpdate()
    }

    private fun checkForInAppUpdate() {
        lifecycleScope.launch {
            if (getFeatureFlagValueUseCase(AppFeatures.InAppUpdate)) {
                runCatching {
                    inAppUpdateHandler.checkForAppUpdates()
                }
            }
        }
    }

    private fun setupView() {
        Timber.d("Set view")
        setContentView(R.layout.activity_manager)
        initialiseViews()
        setInitialViewProperties()
        setViewListeners()
    }

    private fun initialiseViews() {
        psaViewHolder = PsaViewHolder(findViewById(R.id.psa_layout), PsaManager)
        appBarLayout = findViewById(R.id.app_bar_layout)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
        waitingRoomComposeView = findViewById(R.id.waiting_room_dialog_compose_view)

        fragmentLayout = findViewById(R.id.fragment_layout)
        bottomNavigationView =
            findViewById(R.id.bottom_navigation_view)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        fabButton = findViewById(R.id.floating_button)
        getProLayout = findViewById(R.id.get_pro_account)
        getProText = findViewById(R.id.get_pro_account_text)
        rightUpgradeButton = findViewById(R.id.btnRight_upgrade)
        leftCancelButton = findViewById(R.id.btnLeft_cancel)
        fragmentContainer = findViewById(R.id.fragment_container)
        cameraUploadViewTypes = findViewById(R.id.cu_view_type)
        tabLayoutShares = findViewById(R.id.sliding_tabs_shares)
        viewPagerShares = findViewById(R.id.shares_tabs_pager)
        callInProgressLayout = findViewById(R.id.call_in_progress_layout)
        callInProgressChrono = findViewById(R.id.call_in_progress_chrono)
        callInProgressText = findViewById(R.id.call_in_progress_text)
        navHostView = findViewById(R.id.nav_host_fragment)
    }


    private fun setInitialViewProperties() {
        viewPagerShares.offscreenPageLimit = 3
        getProLayout.setBackgroundColor(
            if (Util.isDarkMode(this)) ColorUtils.getColorForElevation(
                this,
                8f
            ) else Color.WHITE
        )
        callInProgressLayout.visibility = View.GONE

        waitingRoomComposeView.apply {
            isVisible = true
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val isDark = themeMode.isDarkMode()
                val waitingRoomState by waitingRoomManagementViewModel.state.collectAsStateWithLifecycle()
                AndroidTheme(isDark = isDark) {
                    UsersInWaitingRoomDialog(
                        state = waitingRoomState,
                        onAdmitClick = {
                            waitingRoomManagementViewModel.admitUsersClick()
                        },
                        onDenyClick = {
                            waitingRoomManagementViewModel.denyUsersClick()
                        },
                        onDenyEntryClick = {
                            waitingRoomManagementViewModel.denyEntryClick()
                        },
                        onSeeWaitingRoomClick = {
                            waitingRoomManagementViewModel.seeWaitingRoomClick()
                        },
                        onDismiss = {
                            waitingRoomManagementViewModel.setShowParticipantsInWaitingRoomDialogConsumed()
                        },
                        onCancelDenyEntryClick = {
                            waitingRoomManagementViewModel.cancelDenyEntryClick()
                        },
                    )
                }
            }
        }
    }

    private fun setViewListeners() {
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
        addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

            override fun onDrawerOpened(drawerView: View) {
                refreshDrawerInfo(storageState === StorageState.Unknown)

                LiveEventBus.get(EventConstants.EVENT_DRAWER_OPEN, Boolean::class.java).post(true)
            }

            override fun onDrawerClosed(drawerView: View) {
                LiveEventBus.get(EventConstants.EVENT_DRAWER_OPEN, Boolean::class.java).post(false)
            }

            override fun onDrawerStateChanged(newState: Int) {}

            /**
             * Method to refresh the info displayed in the drawer menu.
             *
             * @param refreshStorageInfo Parameter to indicate if refresh the storage info.
             */
            private fun refreshDrawerInfo(refreshStorageInfo: Boolean) {
                if (!refreshStorageInfo) return
                viewModel.checkNumUnreadUserAlerts(UnreadUserAlertsCheckType.NOTIFICATIONS_TITLE)
                refreshAccountInfo()
            }
        })
        fabButton.setOnClickListener(FabButtonListener(this))
        viewPagerShares.setPageTransformer { _: View?, _: Float -> }
        viewPagerShares.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int,
            ) {
            }

            override fun onPageSelected(position: Int) {
                Timber.d("selectDrawerItemSharedItems - TabId: %s", position)
                supportInvalidateOptionsMenu()
                checkScrollElevation()
                when (SharesTab.fromPosition(position)) {
                    SharesTab.INCOMING_TAB -> {
                        Analytics.tracker.trackEvent(IncomingSharesTabEvent)
                        if (isOutgoingAdded) {
                            outgoingSharesFragment?.hideActionMode()
                        } else if (isLinksAdded) {
                            linksFragment?.hideActionMode()
                        }
                    }

                    SharesTab.OUTGOING_TAB -> {
                        Analytics.tracker.trackEvent(OutgoingSharesTabEvent)
                        if (isIncomingAdded) {
                            incomingSharesFragment?.hideActionMode()
                        } else if (isLinksAdded) {
                            linksFragment?.hideActionMode()
                        }
                    }

                    SharesTab.LINKS_TAB -> {
                        Analytics.tracker.trackEvent(LinkSharesTabEvent)
                        if (isIncomingAdded) {
                            incomingSharesFragment?.hideActionMode()
                        } else if (isOutgoingAdded) {
                            outgoingSharesFragment?.hideActionMode()
                        }
                    }

                    else -> {}
                }
                setToolbarTitle()
                showFabButton()
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
        tabLayoutShares.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    val tabIconColor =
                        ContextCompat.getColor(applicationContext, R.color.red_600_red_300)
                    tab.icon?.setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN)
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                    val tabIconColor =
                        ContextCompat.getColor(applicationContext, R.color.grey_300_grey_600)
                    tab.icon?.setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN)
                }

                override fun onTabReselected(tab: TabLayout.Tab) {}
            }
        )
        callInProgressLayout.setOnClickListener(this)
        (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment)?.let {
            setupNavDestListener(it)
        }
    }

    override fun addDrawerListener(listener: DrawerLayout.DrawerListener) {
        drawerLayout.addDrawerListener(listener)
    }

    private fun initialiseChatBadgeView() {
        val itemView = menuView.getChildAt(3) as BottomNavigationItemView
        chatBadge = LayoutInflater.from(this).inflate(R.layout.bottom_chat_badge, menuView, false)
        itemView.addView(chatBadge)
        callBadge = LayoutInflater.from(this).inflate(R.layout.bottom_call_badge, menuView, false)
        itemView.addView(callBadge)
        callBadge.visibility = View.GONE
        setChatBadge()
    }

    private fun setupAudioPlayerController() {
        miniAudioPlayerController = MiniAudioPlayerController(
            findViewById(R.id.mini_audio_player)
        ) {
            // we need update fragmentLayout's layout params when player view is closed.
            if (bottomNavigationView.visibility == View.VISIBLE) {
                showBNVImmediate()
            }
        }
        miniAudioPlayerController?.let { lifecycle.addObserver(it) }
    }

    private fun setGetProLabelText() {
        var getProTextString: String = getString(R.string.get_pro_account)
        try {
            getProTextString = getProTextString.replace("[A]", "\n")
        } catch (e: Exception) {
            Timber.e(e, "Formatted string: %s", getProTextString)
        }
        getProText.text = getProTextString
    }

    private fun checkDatabaseValues(): Boolean {
        val ephemeral =
            runBlocking { runCatching { monitorEphemeralCredentialsUseCase().firstOrNull() }.getOrNull() }
        if (ephemeral != null) {
            refreshSession()
            return true
        }
        if (dbH.credentials == null) {
            if (intent != null) {
                if (intent.action != null) {
                    if (intent.action == Constants.ACTION_EXPORT_MASTER_KEY || intent.action == Constants.ACTION_OPEN_MEGA_LINK || intent.action == Constants.ACTION_OPEN_MEGA_FOLDER_LINK) {
                        openLink = true
                    } else if (intent.action == Constants.ACTION_CANCEL_CAM_SYNC) {
                        viewModel.stopCameraUploads(shouldReschedule = false)
                        finish()
                        return true
                    }
                }
            }
            if (!openLink) {
                val loginIntent = Intent(this, LoginActivity::class.java)
                loginIntent.putExtra(Constants.VISIBLE_FRAGMENT, Constants.TOUR_FRAGMENT)
                loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(loginIntent)
                finish()
            }
            return true
        }
        prefs = dbH.preferences
        firstTimeAfterInstallation =
            if (prefs == null || prefs?.firstTime == null) true
            else prefs?.firstTime.toBoolean()

        return false
    }

    private fun handleDuplicateLaunches(): Boolean {
        // This block for solving the issue below:
        // Android is installed for the first time. Press the “Open” button on the system installation dialog, press the home button to switch the app to background,
        // and then switch the app to foreground, causing the app to create a new instantiation.
        if (!isTaskRoot) {
            val action = intent.action
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN == action) {
                finish()
                return true
            }
        }
        return false
    }

    private fun registerViewModelObservers() {
        collectFlows()
        viewModel.onGetNumUnreadUserAlerts().observe(
            this
        ) { result: Pair<UnreadUserAlertsCheckType, Int> ->
            updateNumUnreadUserAlerts(
                result
            )
        }
    }

    private fun handleRootNodeAndHeartbeatState(
        savedInstanceState: Bundle?,
    ): Boolean {
        var selectDrawerItemPending = true
        val isHeartBeatAlive: Boolean = MegaApplication.isIsHeartBeatAlive
        rootNode = megaApi.rootNode
        if (rootNode == null || LoginActivity.isBackFromLoginPage || isHeartBeatAlive) {
            Timber.d("Action: %s", intent?.action)
            if (!handleRedirectIntentActions(intent)) {
                refreshSession()
            }
            return true
        } else {
            attr = dbH.attributes
            if (attr?.invalidateSdkCache.toBoolean()) {
                Timber.d("megaApi.invalidateCache();")
                megaApi.invalidateCache()
            }
            dbH.setInvalidateSdkCache(false)
            MegaMessageService.getToken(this)
            userInfoViewModel.getUserInfo()
            preloadPayment()
            megaApi.isGeolocationEnabled(this)
            if (savedInstanceState == null) {
                Timber.d("Run async task to check offline files")
                //Check the consistency of the offline nodes in the DB
                val checkOfflineNodesTask = CheckOfflineNodesTask(this)
                checkOfflineNodesTask.execute()
            }
            if (intent != null) {
                if (intent.action != null) {
                    if (intent.action == Constants.ACTION_EXPORT_MASTER_KEY) {
                        Timber.d("Intent to export Master Key - im logged in!")
                        startActivity(Intent(this, ExportRecoveryKeyActivity::class.java))
                        return true
                    } else if (intent.action == Constants.ACTION_CANCEL_ACCOUNT) {
                        intent.data?.let {
                            Timber.d("Link to cancel: %s", it)
                            showMyAccount(Constants.ACTION_CANCEL_ACCOUNT, it)
                        }
                    } else if (intent.action == Constants.ACTION_CHANGE_MAIL) {
                        intent.data?.let {
                            Timber.d("Link to change mail: %s", it)
                            showMyAccount(Constants.ACTION_CHANGE_MAIL, it)
                        }
                    } else if (intent.action == Constants.ACTION_OPEN_FOLDER) {
                        Timber.d("Open after LauncherFileExplorerActivity ")
                        val locationFileInfo: Boolean = intent.getBooleanExtra(
                            Constants.INTENT_EXTRA_KEY_LOCATION_FILE_INFO,
                            false
                        )
                        val handleIntent: Long = intent.getLongExtra(
                            Constants.INTENT_EXTRA_KEY_PARENT_HANDLE,
                            INVALID_HANDLE
                        )
                        if (intent.getBooleanExtra(
                                Constants.SHOW_MESSAGE_UPLOAD_STARTED,
                                false
                            )
                        ) {
                            val numberUploads: Int =
                                intent.getIntExtra(Constants.NUMBER_UPLOADS, 1)
                            showSnackbar(
                                Constants.SNACKBAR_TYPE,
                                resources.getQuantityString(
                                    R.plurals.upload_began,
                                    numberUploads,
                                    numberUploads
                                ),
                                -1
                            )
                        }
                        intent.getStringExtra(Constants.EXTRA_MESSAGE)?.let {
                            showSnackbar(
                                Constants.SNACKBAR_TYPE,
                                it,
                                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                            )
                        }
                        if (locationFileInfo) {
                            val offlineAdapter: Boolean =
                                intent.getBooleanExtra("offline_adapter", false)
                            if (offlineAdapter) {
                                drawerItem = DrawerItem.HOMEPAGE
                                selectDrawerItem(drawerItem)
                                selectDrawerItemPending = false
                                openFullscreenOfflineFragment(
                                    intent.getStringExtra(Constants.INTENT_EXTRA_KEY_PATH_NAVIGATION)
                                )
                            } else {
                                when (intent.getLongExtra("fragmentHandle", -1)) {
                                    megaApi.rootNode?.handle -> {
                                        drawerItem = DrawerItem.CLOUD_DRIVE
                                        fileBrowserViewModel.setBrowserParentHandle(handleIntent)
                                        selectDrawerItem(drawerItem)
                                        selectDrawerItemPending = false
                                    }

                                    megaApi.rubbishNode?.handle -> {
                                        drawerItem = DrawerItem.RUBBISH_BIN
                                        rubbishBinViewModel.setRubbishBinHandle(handleIntent)
                                        selectDrawerItem(drawerItem)
                                        selectDrawerItemPending = false
                                    }

                                    megaApi.inboxNode?.handle -> {
                                        drawerItem = DrawerItem.BACKUPS
                                        backupsViewModel.updateBackupsHandle(handleIntent)
                                        selectDrawerItem(drawerItem)
                                        selectDrawerItemPending = false
                                    }

                                    else -> {
                                        //Incoming
                                        drawerItem = DrawerItem.SHARED_ITEMS
                                        viewModel.setSharesTab(SharesTab.INCOMING_TAB)
                                        val parentIntentN =
                                            megaApi.getNodeByHandle(handleIntent)
                                        incomingSharesViewModel.setIncomingTreeDepth(
                                            MegaApiUtils.calculateDeepBrowserTreeIncoming(
                                                parentIntentN,
                                                this
                                            ), handleIntent
                                        )
                                        selectDrawerItem(drawerItem)
                                        selectDrawerItemPending = false
                                    }
                                }
                            }
                        } else {
                            actionOpenFolder(handleIntent)
                        }
                        intent = null
                    } else if (intent.action == Constants.ACTION_PASS_CHANGED) {
                        showMyAccount(
                            Constants.ACTION_PASS_CHANGED, null,
                            android.util.Pair<String, Int>(
                                Constants.RESULT, intent.getIntExtra(
                                    Constants.RESULT, MegaError.API_OK
                                )
                            )
                        )
                    } else if (intent.action == Constants.ACTION_RESET_PASS) {
                        intent.data?.let {
                            showMyAccount(Constants.ACTION_RESET_PASS, it)
                        }
                    } else if (intent.action == Constants.ACTION_IPC) {
                        Timber.d("IPC link - go to received request in Contacts")
                        markNotificationsSeen(true)
                        navigateToContactRequests()
                        intent.action = null
                        intent = null
                    } else if (intent.action == Constants.ACTION_CHAT_NOTIFICATION_MESSAGE) {
                        Timber.d("Chat notification received")
                        drawerItem = DrawerItem.CHAT
                        selectDrawerItem(drawerItem)
                        val chatId: Long = intent.getLongExtra(
                            Constants.CHAT_ID,
                            MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                        )
                        if (intent.getBooleanExtra(
                                Constants.EXTRA_MOVE_TO_CHAT_SECTION,
                                false
                            )
                        ) {
                            moveToChatSection(chatId)
                        } else {
                            val text = intent.getStringExtra(Constants.SHOW_SNACKBAR)
                            if (chatId != -1L) {
                                openChat(chatId, text)
                            }
                        }
                        selectDrawerItemPending = false
                        intent.action = null
                        intent = null
                    } else if (intent.action == Constants.ACTION_CHAT_SUMMARY) {
                        Timber.d("Chat notification: ACTION_CHAT_SUMMARY")
                        drawerItem = DrawerItem.CHAT
                        selectDrawerItem(drawerItem)
                        selectDrawerItemPending = false
                        intent.action = null
                        intent = null
                    } else if (intent.action == Constants.ACTION_OPEN_CHAT_LINK) {
                        Timber.d("ACTION_OPEN_CHAT_LINK: %s", intent.dataString)
                        drawerItem = DrawerItem.CHAT
                        selectDrawerItem(drawerItem)
                        selectDrawerItemPending = false
                        megaChatApi.checkChatLink(
                            intent.dataString,
                            LoadPreviewListener(
                                this@ManagerActivity,
                                this@ManagerActivity,
                                Constants.CHECK_LINK_TYPE_UNKNOWN_LINK
                            )
                        )
                        intent.action = null
                        intent = null
                    } else if (intent.action == Constants.ACTION_JOIN_OPEN_CHAT_LINK) {
                        linkJoinToChatLink = intent.dataString
                        joiningToChatLink = true
                        if (megaChatApi.connectionState == MegaChatApi.CONNECTED) {
                            megaChatApi.checkChatLink(
                                linkJoinToChatLink,
                                LoadPreviewListener(
                                    this@ManagerActivity,
                                    this@ManagerActivity,
                                    Constants.CHECK_LINK_TYPE_UNKNOWN_LINK
                                )
                            )
                        }
                        intent.action = null
                        intent = null
                    } else if (intent.action == Constants.ACTION_SHOW_SETTINGS) {
                        Timber.d("Chat notification: SHOW_SETTINGS")
                        selectDrawerItemPending = false
                        moveToSettingsSection()
                        intent.action = null
                        intent = null
                    } else if (intent.action == Constants.ACTION_SHOW_SETTINGS_STORAGE) {
                        Timber.d("ACTION_SHOW_SETTINGS_STORAGE")
                        selectDrawerItemPending = false
                        moveToSettingsSectionStorage()
                        intent.action = null
                        intent = null
                    } else if (intent.action == Constants.ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION) {
                        Timber.d("ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION")
                        markNotificationsSeen(true)
                        drawerItem = DrawerItem.SHARED_ITEMS
                        viewModel.setSharesTab(SharesTab.INCOMING_TAB)
                        selectDrawerItem(drawerItem)
                        selectDrawerItemPending = false
                    } else if (intent.action == Constants.ACTION_SHOW_MY_ACCOUNT) {
                        Timber.d("Intent from chat - show my account")
                        showMyAccount()
                        selectDrawerItemPending = false
                    } else if (intent.action == Constants.ACTION_SHOW_UPGRADE_ACCOUNT) {
                        navigateToUpgradeAccount()
                        selectDrawerItemPending = false
                    } else if (intent.action == Constants.ACTION_OPEN_HANDLE_NODE) {
                        val link = intent.dataString
                        val s =
                            link?.split("#".toRegex())?.dropLastWhile { it.isEmpty() }
                                ?: emptyList()
                        if (s.size > 1) {
                            var nodeHandleLink = s[1]
                            val sSlash = s[1].split("/".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                            if (sSlash.isNotEmpty()) {
                                nodeHandleLink = sSlash[0]
                            }
                            val nodeHandleLinkLong: Long =
                                MegaApiAndroid.base64ToHandle(nodeHandleLink)
                            val nodeLink: MegaNode? = megaApi.getNodeByHandle(nodeHandleLinkLong)
                            var pN: MegaNode? = megaApi.getParentNode(nodeLink)
                            if (pN == null) {
                                pN = megaApi.rootNode
                            }
                            pN?.handle?.let { fileBrowserViewModel.setBrowserParentHandle(it) }
                            drawerItem = DrawerItem.CLOUD_DRIVE
                            selectDrawerItem(drawerItem)
                            selectDrawerItemPending = false
                            val fileInfoIntent = Intent(this, FileInfoActivity::class.java)
                            fileInfoIntent.putExtra("handle", nodeLink?.handle)
                            fileInfoIntent.putExtra(Constants.NAME, nodeLink?.name)
                            startActivity(fileInfoIntent)
                        } else {
                            drawerItem = DrawerItem.CLOUD_DRIVE
                            selectDrawerItem(drawerItem)
                        }
                    } else if (intent.action == Constants.ACTION_IMPORT_LINK_FETCH_NODES) {
                        intent.action = null
                        intent = null
                    } else if (intent.action == Constants.ACTION_OPEN_CONTACTS_SECTION) {
                        markNotificationsSeen(true)
                        openContactLink(intent.getLongExtra(Constants.CONTACT_HANDLE, -1))
                    } else if (intent.action == Constants.ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE) {
                        val chatId: Long = intent.getLongExtra(
                            Constants.CHAT_ID,
                            MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                        )
                        showSnackbar(Constants.MESSAGE_SNACKBAR_TYPE, null, chatId)
                        intent.action = null
                        intent = null
                    }
                }
            }
            Timber.d("Check if there any unread chat")
            if (joiningToChatLink && !TextUtil.isTextEmpty(linkJoinToChatLink)) {
                megaChatApi.checkChatLink(
                    linkJoinToChatLink,
                    LoadPreviewListener(
                        this@ManagerActivity,
                        this@ManagerActivity,
                        Constants.CHECK_LINK_TYPE_UNKNOWN_LINK
                    )
                )
            }
            setChatBadge()
            Timber.d("Check if there any INCOMING pendingRequest contacts")
            viewModel.checkNumUnreadUserAlerts(UnreadUserAlertsCheckType.NOTIFICATIONS_TITLE)
            val firstLaunch =
                intent?.getBooleanExtra(IntentConstants.EXTRA_FIRST_LOGIN, false) == true
            if (drawerItem == null) {
                drawerItem = getStartDrawerItem()
                if (intent != null) {
                    val upgradeAccount: Boolean =
                        intent.getBooleanExtra(IntentConstants.EXTRA_UPGRADE_ACCOUNT, false)
                    newAccount =
                        intent.getBooleanExtra(IntentConstants.EXTRA_NEW_ACCOUNT, false)
                    newCreationAccount = intent.getBooleanExtra(NEW_CREATION_ACCOUNT, false)
                    firstLogin =
                        intent.getBooleanExtra(IntentConstants.EXTRA_FIRST_LOGIN, false)
                    viewModel.setIsFirstLogin(firstLogin)
                    setRequestNotificationsPermissionFirstLogin(savedInstanceState)
                    askPermissions = intent.getBooleanExtra(
                        IntentConstants.EXTRA_ASK_PERMISSIONS,
                        askPermissions
                    )

                    //reset flag to fix incorrect view loaded when orientation changes
                    intent.removeExtra(IntentConstants.EXTRA_NEW_ACCOUNT)
                    intent.removeExtra(IntentConstants.EXTRA_UPGRADE_ACCOUNT)
                    intent.removeExtra(IntentConstants.EXTRA_FIRST_LOGIN)
                    intent.removeExtra(IntentConstants.EXTRA_ASK_PERMISSIONS)
                    if (upgradeAccount) {
                        val accountType: Int =
                            intent.getIntExtra(IntentConstants.EXTRA_ACCOUNT_TYPE, 0)
                        if (accountType != Constants.FREE) {
                            showMyAccount(
                                android.util.Pair(
                                    IntentConstants.EXTRA_ACCOUNT_TYPE,
                                    accountType
                                )
                            )
                        } else if (firstLogin && viewModel.getStorageState() !== StorageState.PayWall) {
                            drawerItem = DrawerItem.PHOTOS
                        } else {
                            showMyAccount()
                        }
                    } else {
                        if (firstLogin && viewModel.getStorageState() !== StorageState.PayWall) {
                            Timber.d("First login. Go to Camera Uploads configuration.")
                            drawerItem = DrawerItem.PHOTOS
                            intent = null
                        }
                    }
                }
            } else {
                Timber.d("DRAWERITEM NOT NULL: %s", drawerItem)
                if (intent != null) {
                    val upgradeAccount: Boolean =
                        intent.getBooleanExtra(IntentConstants.EXTRA_UPGRADE_ACCOUNT, false)
                    newAccount =
                        intent.getBooleanExtra(IntentConstants.EXTRA_NEW_ACCOUNT, false)
                    newCreationAccount = intent.getBooleanExtra(NEW_CREATION_ACCOUNT, false)
                    //reset flag to fix incorrect view loaded when orientation changes
                    intent.removeExtra(IntentConstants.EXTRA_NEW_ACCOUNT)
                    intent.removeExtra(IntentConstants.EXTRA_UPGRADE_ACCOUNT)
                    firstLogin = intent.getBooleanExtra(IntentConstants.EXTRA_FIRST_LOGIN, false)
                    viewModel.setIsFirstLogin(firstLogin)
                    setRequestNotificationsPermissionFirstLogin(savedInstanceState)
                    askPermissions = intent.getBooleanExtra(
                        IntentConstants.EXTRA_ASK_PERMISSIONS,
                        askPermissions
                    )
                    if (upgradeAccount) {
                        closeDrawer()
                        val accountType: Int =
                            intent.getIntExtra(IntentConstants.EXTRA_ACCOUNT_TYPE, 0)
                        if (accountType != Constants.FREE) {
                            showMyAccount(
                                android.util.Pair(
                                    IntentConstants.EXTRA_ACCOUNT_TYPE,
                                    accountType
                                )
                            )
                        } else if (firstLogin && viewModel.getStorageState() !== StorageState.PayWall) {
                            drawerItem = DrawerItem.PHOTOS
                        } else {
                            showMyAccount()
                        }
                    } else {
                        if (firstLogin && !joiningToChatLink) {
                            Timber.d("Intent firstTimeCam==true")
                            if (prefs?.camSyncEnabled != null) {
                                firstLogin = false
                            } else {
                                firstLogin = true
                                if (viewModel.getStorageState() !== StorageState.PayWall && isInPhotosPage) {
                                    drawerItem = DrawerItem.PHOTOS
                                }
                            }
                            intent = null
                        }
                    }
                    if (intent?.action != null) {
                        if (intent.action == Constants.ACTION_SHOW_TRANSFERS) {
                            if (intent.getBooleanExtra(Constants.OPENED_FROM_CHAT, false)) {
                                sendBroadcast(
                                    Intent(ACTION_CLOSE_CHAT_AFTER_OPEN_TRANSFERS).setPackage(
                                        applicationContext.packageName
                                    )
                                )
                            }
                            drawerItem = DrawerItem.TRANSFERS
                            transferPageViewModel.setTransfersTab(
                                intent.serializable(
                                    TRANSFERS_TAB
                                ) ?: TransfersTab.NONE
                            )
                            intent = null
                        } else if (intent.action == Constants.ACTION_REFRESH_AFTER_BLOCKED) {
                            drawerItem = DrawerItem.CLOUD_DRIVE
                            intent = null
                        }
                    }
                }
                closeDrawer()
            }
            checkCurrentStorageStatus()
            viewModel.startCameraUpload()

            //INITIAL FRAGMENT
            if (selectDrawerItemPending) {
                selectDrawerItem(drawerItem)
            }
        }
        return false
    }

    private fun handleRedirectIntentActions(intent: Intent?): Boolean {
        intent ?: return false
        if (intent.action == Constants.ACTION_CANCEL_CAM_SYNC) {
            viewModel.stopCameraUploads(shouldReschedule = false)
            finish()
            return true
        }
        managerRedirectIntentMapper(
            intent = intent,
            enabledFeatureFlags = viewModel.state().enabledFlags,
        )?.let { redirectIntent ->
            startActivity(redirectIntent)
            finish()
            return true
        }

        return false
    }

    private fun registerEventBusObservers() {
        LiveEventBus.get(EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
            .observe(this, callStatusObserver)
        LiveEventBus.get(EVENT_CALL_ON_HOLD_CHANGE, MegaChatCall::class.java)
            .observe(this, callOnHoldObserver)
        LiveEventBus.get(
            EVENT_SESSION_ON_HOLD_CHANGE,
            android.util.Pair::class.java
        ).observe(this, sessionOnHoldObserver)
        observePsa()
    }

    private fun registerBroadcastReceivers() {
        registerContactUpdateReceiver()
    }

    private fun registerContactUpdateReceiver() {
        val contactUpdateFilter =
            IntentFilter(BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE)
        contactUpdateFilter.addAction(ACTION_UPDATE_NICKNAME)
        contactUpdateFilter.addAction(ACTION_UPDATE_FIRST_NAME)
        contactUpdateFilter.addAction(ACTION_UPDATE_LAST_NAME)
        contactUpdateFilter.addAction(ACTION_UPDATE_CREDENTIALS)
        registerReceiver(contactUpdateReceiver, contactUpdateFilter)
    }

    private fun restoreFromSavedInstanceState(savedInstanceState: Bundle) {
        Timber.d("Bundle is NOT NULL")
        askPermissions = savedInstanceState.getBoolean(IntentConstants.EXTRA_ASK_PERMISSIONS)
        drawerItem = savedInstanceState.serializable("drawerItem")
        bottomItemBeforeOpenFullscreenOffline = savedInstanceState.getInt(
            BOTTOM_ITEM_BEFORE_OPEN_FULLSCREEN_OFFLINE
        )
        pathNavigationOffline =
            savedInstanceState.getString("pathNavigationOffline", pathNavigationOffline)
        Timber.d("savedInstanceState -> pathNavigationOffline: %s", pathNavigationOffline)
        selectedAccountType = savedInstanceState.getInt("selectedAccountType", -1)
        turnOnNotifications = savedInstanceState.getBoolean("turnOnNotifications", false)
        orientationSaved = savedInstanceState.getInt("orientationSaved")
        bottomNavigationCurrentItem =
            savedInstanceState.getInt("bottomNavigationCurrentItem", -1)
        searchExpand = savedInstanceState.getBoolean("searchExpand", false)
        comesFromNotifications = savedInstanceState.getBoolean("comesFromNotifications", false)
        comesFromNotificationsLevel =
            savedInstanceState.getInt("comesFromNotificationsLevel", 0)
        comesFromNotificationHandle = savedInstanceState.getLong(
            "comesFromNotificationHandle",
            Constants.INVALID_VALUE.toLong()
        )
        comesFromNotificationHandleSaved = savedInstanceState.getLong(
            "comesFromNotificationHandleSaved",
            Constants.INVALID_VALUE.toLong()
        )
        comesFromNotificationSharedIndex =
            savedInstanceState.serializable(COMES_FROM_NOTIFICATIONS_SHARED_INDEX)
                ?: SharesTab.NONE
        onAskingPermissionsFragment =
            savedInstanceState.getBoolean("onAskingPermissionsFragment", false)
        if (onAskingPermissionsFragment) {
            permissionsFragment = supportFragmentManager.getFragment(
                savedInstanceState,
                FragmentTag.PERMISSIONS.tag
            ) as? PermissionsFragment
        }
        mElevationCause = savedInstanceState.getInt("elevation", 0)
        storageState = savedInstanceState.serializable("storageState")
            ?: StorageState.Unknown
        comesFromNotificationDeepBrowserTreeIncoming = savedInstanceState.getInt(
            "comesFromNotificationDeepBrowserTreeIncoming",
            Constants.INVALID_VALUE
        )
        typesCameraPermission = savedInstanceState.getInt(
            Constants.TYPE_CALL_PERMISSION,
            Constants.INVALID_TYPE_PERMISSIONS
        )
        joiningToChatLink = savedInstanceState.getBoolean(JOINING_CHAT_LINK, false)
        linkJoinToChatLink = savedInstanceState.getString(LINK_JOINING_CHAT_LINK)
        isInMDMode = savedInstanceState.getBoolean(STATE_KEY_IS_IN_MD_MODE, false)
        if (isInMDMode) {
            mediaDiscoveryFragment = supportFragmentManager.getFragment(
                savedInstanceState,
                FragmentTag.MEDIA_DISCOVERY.tag
            )
        }
        isInAlbumContent = savedInstanceState.getBoolean(STATE_KEY_IS_IN_ALBUM_CONTENT, false)
        if (isInAlbumContent) {
            albumContentFragment = supportFragmentManager.getFragment(
                savedInstanceState,
                FragmentTag.ALBUM_CONTENT.tag
            )
        }
        isInFilterPage = savedInstanceState.getBoolean(STATE_KEY_IS_IN_PHOTOS_FILTER, false)
        nodeAttacher.restoreState(savedInstanceState)
        nodeSaver.restoreState(savedInstanceState)

        //upload from device, progress dialog should show when screen orientation changes.
        if (savedInstanceState.getBoolean(PROCESS_FILE_DIALOG_SHOWN, false)) {
            processFileDialog = showProcessFileDialog(this, null)
        }

        // Backup warning dialog
        backupHandleList =
            savedInstanceState.serializable(BACKUP_HANDLED_ITEM)
        backupNodeHandle = savedInstanceState.getLong(BACKUP_HANDLED_NODE, -1)
        backupNodeType = savedInstanceState.getInt(BACKUP_NODE_TYPE, -1)
        backupActionType = savedInstanceState.getInt(BACKUP_ACTION_TYPE, -1)
        backupDialogType =
            savedInstanceState.getInt(BACKUP_DIALOG_WARN, BACKUP_DIALOG_SHOW_NONE)

        if (savedInstanceState.getBoolean(OPEN_LINK_DIALOG_SHOWN, false)) {
            showOpenLinkDialog()
            val text = savedInstanceState.getString(OPEN_LINK_TEXT, "")
            openLinkText?.setText(text)
            text?.length?.let { openLinkText?.setSelection(it) }
            if (savedInstanceState.getBoolean(OPEN_LINK_ERROR, false)) {
                text?.let { openLink(it) }
            }
        }
    }

    /**
     * collecting Flows from ViewModels
     */
    private fun collectFlows() {
        this.collectFlow(
            viewModel.state,
            Lifecycle.State.STARTED
        ) { managerState: ManagerState ->
            val nodeNameCollisionResult = managerState.nodeNameCollisionResult
            if (nodeNameCollisionResult != null) {
                handleNodesNameCollisionResult(nodeNameCollisionResult)
                viewModel.markHandleNodeNameCollisionResult()
            }
            if (managerState.moveRequestResult != null) {
                handleMovementResult(managerState.moveRequestResult)
                viewModel.markHandleMoveRequestResult()
            }
            if (managerState.restoreNodeResult != null) {
                handleRestoreNodeResult(managerState.restoreNodeResult)
                viewModel.markHandleRestoreNodeResult()
            }
            if (managerState.shouldAlertUserAboutSecurityUpgrade) {
                SecurityUpgradeDialogFragment.newInstance()
                    .show(supportFragmentManager, SecurityUpgradeDialogFragment.TAG)
                viewModel.setShouldAlertUserAboutSecurityUpgrade(false)
            }
            if (managerState.isPushNotificationSettingsUpdatedEvent) {
                viewModel.onConsumePushNotificationSettingsUpdateEvent()
            }
            if (managerState.nodeUpdateReceived) {
                // Invalidate the menu will collapse/expand the search view and set the query text to ""
                // (call onQueryTextChanged) (BTW, SearchFragment uses textSubmitted to avoid the query
                // text changed to "" for once)
                if (drawerItem !== DrawerItem.HOMEPAGE) {
                    setToolbarTitle()
                    invalidateOptionsMenu()
                }
                viewModel.nodeUpdateHandled()
            }

            // Update pending actions badge on bottom navigation menu
            updateUnverifiedSharesBadge(managerState.pendingActionsCount)

            //Show 2FA dialog to the user on Second Launch after sign up
            if (managerState.show2FADialog) {
                showEnable2FADialog()
                viewModel.markHandleShow2FADialog()
            }

            if (managerState.titleChatArchivedEvent != null) {
                showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getString(R.string.success_archive_chat, managerState.titleChatArchivedEvent),
                    MEGACHAT_INVALID_HANDLE
                )
                viewModel.onChatArchivedEventConsumed()
            }
        }
        this.collectFlow(
            viewModel.monitorConnectivityEvent,
            Lifecycle.State.STARTED
        ) { isConnected: Boolean ->
            if (isConnected) {
                showOnlineMode()
            } else {
                showOfflineMode()
            }
        }
        this.collectFlow(
            incomingSharesViewModel.state,
            Lifecycle.State.STARTED
        ) { incomingSharesState: IncomingSharesState ->
            addUnverifiedIncomingCountBadge(incomingSharesState.nodes.count { it.second != null })
        }
        this.collectFlow(
            outgoingSharesViewModel.state,
            Lifecycle.State.STARTED
        ) { outgoingSharesState: OutgoingSharesState ->
            addUnverifiedOutgoingCountBadge(
                outgoingSharesState.nodes.count { it.second != null },
            )
        }
        this.collectFlow(
            viewModel.monitorFinishActivityEvent,
            Lifecycle.State.CREATED
        ) { finish: Boolean ->
            Timber.d("MonitorFinishActivity flow collected with Finish %s", finish)
            if (finish) {
                finish()
            }
        }
        collectFlow(
            targetFlow = viewModel.monitorCameraUploadFolderIconUpdateEvent,
            minActiveState = Lifecycle.State.CREATED
        ) {
            handleCameraUploadFolderIconUpdate()
        }
        collectFlow(
            targetFlow = viewModel.monitorMyAccountUpdateEvent,
            minActiveState = Lifecycle.State.CREATED
        ) { data ->
            handleUpdateMyAccount(data)
        }
        collectFlow(targetFlow = viewModel.monitorOfflineNodeAvailabilityEvent) {
            refreshCloudOrder()
        }

        collectFlow(waitingRoomManagementViewModel.state) { state ->
            state.snackbarString?.let {
                showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    it,
                    MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                )
                waitingRoomManagementViewModel.onConsumeSnackBarMessageEvent()
            }
        }
    }

    private fun handleMovementResult(moveRequestResult: Result<MoveRequestResult>) {
        if (moveRequestResult.isSuccess) {
            val data = moveRequestResult.getOrThrow()
            if (data !is MoveRequestResult.DeleteMovement && data !is MoveRequestResult.Copy) {
                showMovementResult(data, data.nodes.first())
            }
            showSnackbar(
                Constants.SNACKBAR_TYPE,
                moveRequestMessageMapper(data),
                MEGACHAT_INVALID_HANDLE
            )
        } else {
            manageCopyMoveException(moveRequestResult.exceptionOrNull())
        }
    }

    private fun handleNodesNameCollisionResult(result: NodeNameCollisionResult) {
        if (result.conflictNodes.isNotEmpty()) {
            nameCollisionActivityContract
                ?.launch(ArrayList(result.conflictNodes.values.map {
                    when (result.type) {
                        NodeNameCollisionType.RESTORE,
                        NodeNameCollisionType.MOVE,
                        -> NameCollision.Movement.getMovementCollision(it)

                        NodeNameCollisionType.COPY -> NameCollision.Copy.getCopyCollision(it)
                    }
                }))
        }
        if (result.noConflictNodes.isNotEmpty()) {
            when (result.type) {
                NodeNameCollisionType.RESTORE -> viewModel.restoreNodes(result.noConflictNodes)
                NodeNameCollisionType.MOVE -> viewModel.moveNodes(result.noConflictNodes)
                NodeNameCollisionType.COPY -> viewModel.copyNodes(result.noConflictNodes)
            }
        }
    }

    private fun handleRestoreNodeResult(result: Result<RestoreNodeResult>) {
        if (result.isSuccess) {
            val restoreNodeResult = result.getOrThrow()
            showRestorationOrRemovalResult(restoreNodeResultMapper(restoreNodeResult))
        } else {
            val exception = result.exceptionOrNull()
            Timber.e(exception)
            if (exception is ForeignNodeException) {
                launchForeignNodeError()
            }
        }
    }

    /**
     *  Update the unverified shares badge count on the navigation bottom item view
     *
     *  This function ensure that the badge view is added again only if it has not been added previously
     *
     *  @param pendingActionsCount if > 0 add the badge view else remove it
     */
    private fun updateUnverifiedSharesBadge(pendingActionsCount: Int) {
        if (pendingActionsCount > 0) {
            val sharedItemsView = menuView.getChildAt(4) as? BottomNavigationItemView ?: return
            pendingActionsBadge?.let {
                sharedItemsView.indexOfChild(pendingActionsBadge)
                    .takeIf { it != -1 }
                    ?.let { sharedItemsView.removeViewAt(it) }
            } ?: run {
                pendingActionsBadge = LayoutInflater.from(this)
                    .inflate(R.layout.bottom_pending_actions_badge, menuView, false)
            }
            sharedItemsView.addView(pendingActionsBadge)
            val tvPendingActionsCount =
                pendingActionsBadge?.findViewById<TextView>(R.id.pending_actions_badge_text)
            tvPendingActionsCount?.text = pendingActionsCount.toString()
        } else {
            pendingActionsBadge?.let {
                val sharedItemsView = menuView.getChildAt(4) as? BottomNavigationItemView ?: return
                sharedItemsView.indexOfChild(pendingActionsBadge)
                    .takeIf { it != -1 }
                    ?.let { sharedItemsView.removeViewAt(it) }
            }
        }
    }

    /**
     * Checks which screen should be shown when an user is logins.
     * There are three different screens or warnings:
     * - Business warning: it takes priority over the other two.
     * - Onboarding permissions screens: it has to be only shown when account is logged in after
     * the installation, and some of the permissions required have not been granted and
     * the business warning is not to be shown.
     * - Notifications permission screen: it has to be shown if the onboarding permissions screens
     * have not been shown.
     */
    private fun checkInitialScreens() {
        when {
            checkBusinessStatus() -> {
                myAccountInfo.isBusinessAlertShown = true
            }

            firstTimeAfterInstallation || askPermissions || newCreationAccount -> {
                if (!initialPermissionsAlreadyAsked && !onAskingPermissionsFragment) {
                    drawerItem = DrawerItem.ASK_PERMISSIONS
                    askForAccess()
                }
            }

            requestNotificationsPermissionFirstLogin -> {
                askForNotificationsPermission()
            }
        }
    }

    /**
     * Checks if SharesCompose enabled from [AppFeatures.SharesCompose]
     */
    private suspend fun isSharesTabComposeEnabled() =
        getFeatureFlagValueUseCase(AppFeatures.SharesCompose)

    /**
     * Checks if some business warning has to be shown due to the status of the account.
     *
     * @return True if some warning has been shown, false otherwise.
     */
    private fun checkBusinessStatus(): Boolean {
        if (!isBusinessAccount) {
            return false
        }
        if (myAccountInfo.isBusinessAlertShown) {
            return false
        }
        if (viewModel.state().isFirstLogin && myAccountInfo.wasNotBusinessAlertShownYet()) {
            val status: Int = megaApi.businessStatus
            if (status == MegaApiJava.BUSINESS_STATUS_EXPIRED) {
                myAccountInfo.isBusinessAlertShown = true
                startActivity(Intent(this, BusinessExpiredAlertActivity::class.java))
                return true
            } else if (megaApi.isMasterBusinessAccount && status == MegaApiJava.BUSINESS_STATUS_GRACE_PERIOD) {
                myAccountInfo.isBusinessAlertShown = true
                showBusinessGraceAlert()
                return true
            }
        }
        return false
    }

    private fun showBusinessGraceAlert() {
        if (supportFragmentManager.findFragmentByTag(BusinessGraceDialogFragment.TAG) != null) return
        BusinessGraceDialogFragment().show(supportFragmentManager, BusinessGraceDialogFragment.TAG)
    }

    private fun openContactLink(handle: Long) {
        if (handle == INVALID_HANDLE) {
            Timber.w("Not valid contact handle")
            return
        }
        if (supportFragmentManager.findFragmentByTag(ContactLinkDialogFragment.TAG) != null) return
        ContactLinkDialogFragment.newInstance(handle)
            .show(supportFragmentManager, ContactLinkDialogFragment.TAG)
    }

    fun askForAccess() {
        askPermissions = false
        showStorageAlertWithDelay = true
        //If mobile device, only portrait mode is allowed
        if (!Util.isTablet(this)) {
            Timber.d("Mobile only portrait mode")
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        val notificationsGranted = (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || hasPermissions(this, Manifest.permission.POST_NOTIFICATIONS))
        val writeStorageGranted: Boolean =
            hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                PermissionUtils.getAudioPermissionByVersion(),
                PermissionUtils.getReadExternalStoragePermission()
            )
        } else {
            arrayOf(
                PermissionUtils.getImagePermissionByVersion(),
                PermissionUtils.getAudioPermissionByVersion(),
                PermissionUtils.getVideoPermissionByVersion(),
                PermissionUtils.getReadExternalStoragePermission()
            )
        }
        val readStorageGranted: Boolean = hasPermissions(this, *permissions)
        val cameraGranted: Boolean = hasPermissions(this, Manifest.permission.CAMERA)
        val microphoneGranted: Boolean = hasPermissions(this, Manifest.permission.RECORD_AUDIO)
        val bluetoothGranted = (Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || hasPermissions(this, Manifest.permission.BLUETOOTH_CONNECT))
        val contactsGranted: Boolean = hasPermissions(this, Manifest.permission.READ_CONTACTS)
        if (!notificationsGranted || !writeStorageGranted || !readStorageGranted || !cameraGranted
            || !microphoneGranted || !bluetoothGranted || !contactsGranted
        ) {
            val currentFragment =
                supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (currentFragment?.tag != FragmentTag.PERMISSIONS.tag) {
                deleteCurrentFragment()
            }
            if (permissionsFragment == null) {
                permissionsFragment = PermissionsFragment()
            }
            permissionsFragment?.let { replaceFragment(it, FragmentTag.PERMISSIONS.tag) }
            onAskingPermissionsFragment = true
            appBarLayout.visibility = View.GONE
            setTabsVisibility()
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            supportInvalidateOptionsMenu()
            hideFabButton()
            showHideBottomNavigationView(true)
        }
    }

    fun destroyPermissionsFragment() {
        initialPermissionsAlreadyAsked = true
        //In mobile, allow all orientation after permission screen
        if (!Util.isTablet(this)) {
            Timber.d("Mobile, all orientation")
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        }
        turnOnNotifications = false
        appBarLayout.visibility = View.VISIBLE
        deleteCurrentFragment()
        onAskingPermissionsFragment = false
        permissionsFragment = null
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        supportInvalidateOptionsMenu()
        drawerItem = if (viewModel.getStorageState() === StorageState.PayWall) {
            DrawerItem.CLOUD_DRIVE
        } else {
            viewModel.setIsFirstLogin(true)
            DrawerItem.PHOTOS
        }
        selectDrawerItem(drawerItem)
    }

    override fun onResume() {
        if (drawerItem === DrawerItem.SEARCH && getSearchFragment() != null) {
            searchFragment?.isWaitingForSearchedNodes = true
        }
        super.onResume()
        queryIfNotificationsAreOn()
        if (resources.configuration.orientation != orientationSaved) {
            orientationSaved = resources.configuration.orientation
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        }
        checkScrollElevation()
        checkTransferOverQuotaOnResume()
        LiveEventBus.get(Constants.EVENT_FAB_CHANGE, Boolean::class.java)
            .observeForever(fabChangeObserver)
        checkForInAppUpdateInstallStatus()
    }

    private fun checkForInAppUpdateInstallStatus() {
        lifecycleScope.launch {
            if (getFeatureFlagValueUseCase(AppFeatures.InAppUpdate)) {
                runCatching {
                    inAppUpdateHandler.checkForInAppUpdateInstallStatus()
                }
            }
        }
    }

    private fun queryIfNotificationsAreOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return
        }
        Timber.d("queryIfNotificationsAreOn")
        if (turnOnNotifications) {
            setTurnOnNotificationsFragment()
        } else {
            val nf = NotificationManagerCompat.from(this)
            Timber.d("Notifications Enabled: %s", nf.areNotificationsEnabled())
            if (!nf.areNotificationsEnabled()) {
                Timber.d("OFF")
                if (dbH.showNotifOff == null || dbH.showNotifOff == "true") {
                    if (megaApi.contacts.isNotEmpty() ||
                        megaChatApi.chatListItems.isNotEmpty()
                    ) {
                        setTurnOnNotificationsFragment()
                    }
                }
            }
        }
    }

    fun deleteTurnOnNotificationsFragment() {
        Timber.d("deleteTurnOnNotificationsFragment")
        turnOnNotifications = false
        appBarLayout.visibility = View.VISIBLE
        turnOnNotificationsFragment = null
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        supportInvalidateOptionsMenu()
        selectDrawerItem(drawerItem)
        Util.setStatusBarColor(this, android.R.color.transparent)
    }

    private fun deleteCurrentFragment() {
        val currentFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment != null) {
            supportFragmentManager.beginTransaction().remove(currentFragment)
                .commitNowAllowingStateLoss()
        }
    }

    private fun setTurnOnNotificationsFragment() {
        Timber.d("setTurnOnNotificationsFragment")
        supportActionBar?.subtitle = null
        appBarLayout.visibility = View.GONE
        deleteCurrentFragment()
        if (turnOnNotificationsFragment == null) {
            turnOnNotificationsFragment = TurnOnNotificationsFragment()
        }
        turnOnNotificationsFragment?.let {
            replaceFragment(
                it,
                FragmentTag.TURN_ON_NOTIFICATIONS.tag
            )
        }
        setTabsVisibility()
        appBarLayout.visibility = View.GONE
        closeDrawer()
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        supportInvalidateOptionsMenu()
        hideFabButton()
        showHideBottomNavigationView(true)
        Util.setStatusBarColor(this, R.color.teal_500_teal_400)
    }

    private fun actionOpenFolder(handleIntent: Long) {
        if (handleIntent == INVALID_HANDLE) {
            Timber.w("handleIntent is not valid")
            return
        }
        val parentIntentN = megaApi.getNodeByHandle(handleIntent)
        if (parentIntentN == null) {
            Timber.w("parentIntentN is null")
            return
        }
        drawerItem = when (megaApi.getAccess(parentIntentN)) {
            MegaShare.ACCESS_READ, MegaShare.ACCESS_READWRITE, MegaShare.ACCESS_FULL -> {
                incomingSharesViewModel.setIncomingTreeDepth(
                    MegaApiUtils.calculateDeepBrowserTreeIncoming(
                        parentIntentN,
                        this
                    ), handleIntent
                )
                DrawerItem.SHARED_ITEMS
            }

            else -> if (megaApi.isInRubbish(parentIntentN)) {
                rubbishBinViewModel.setRubbishBinHandle(handleIntent)
                DrawerItem.RUBBISH_BIN
            } else if (megaApi.isInInbox(parentIntentN)) {
                backupsViewModel.updateBackupsHandle(handleIntent)
                DrawerItem.BACKUPS
            } else {
                fileBrowserViewModel.setBrowserParentHandle(handleIntent)
                DrawerItem.CLOUD_DRIVE
            }
        }
    }

    override fun onPostResume() {
        Timber.d("onPostResume")
        super.onPostResume()
        if (isSearching) {
            selectDrawerItem(DrawerItem.SEARCH)
            isSearching = false
            return
        }
        if (dbH.credentials == null) {
            if (!openLink) {
                return
            } else {
                Timber.d("Not credentials")
                Timber.d("Not credentials -> INTENT")
                if (intent?.action != null) {
                    Timber.d("Intent with ACTION: %s", intent.action)
                    if (intent?.action == Constants.ACTION_EXPORT_MASTER_KEY) {
                        val exportIntent =
                            Intent(this, LoginActivity::class.java)
                        intent.putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                        exportIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        exportIntent.action = intent.action
                        startActivity(exportIntent)
                        finish()
                        return
                    }
                }
            }
        }
        intent?.let {
            Timber.d("Intent not null! %s", it.action)
        }
        // Open folder from the intent
        if (intent?.hasExtra(Constants.EXTRA_OPEN_FOLDER) == true) {
            Timber.d("INTENT: EXTRA_OPEN_FOLDER")
            fileBrowserViewModel.setBrowserParentHandle(
                intent.getLongExtra(
                    Constants.EXTRA_OPEN_FOLDER,
                    -1
                )
            )
            intent.removeExtra(Constants.EXTRA_OPEN_FOLDER)
            intent = null
        }
        if (intent?.action != null) {
            Timber.d("Intent action")
            when (intent.action) {
                Constants.ACTION_EXPLORE_ZIP -> {
                    Timber.d("Open zip browser")
                    intent.extras?.getString(Constants.EXTRA_PATH_ZIP)?.let {
                        ZipBrowserActivity.start(this, it)
                    }
                }

                Constants.ACTION_IMPORT_LINK_FETCH_NODES -> {
                    Timber.d("ACTION_IMPORT_LINK_FETCH_NODES")
                    val loginIntent = Intent(this, LoginActivity::class.java)
                    intent.putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                    loginIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    loginIntent.action = Constants.ACTION_IMPORT_LINK_FETCH_NODES
                    loginIntent.data = Uri.parse(intent.dataString)
                    startActivity(loginIntent)
                    finish()
                    return
                }

                Constants.ACTION_OPEN_MEGA_LINK -> {
                    Timber.d("ACTION_OPEN_MEGA_LINK")
                    val fileLinkIntent =
                        if (viewModel.state().enabledFlags.contains(AppFeatures.FileLinkCompose)) {
                            Intent(this, FileLinkComposeActivity::class.java)
                        } else {
                            Intent(this, FileLinkActivity::class.java)
                        }

                    fileLinkIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    fileLinkIntent.action = Constants.ACTION_IMPORT_LINK_FETCH_NODES
                    intent.dataString?.let {
                        fileLinkIntent.data = Uri.parse(it)
                        startActivity(fileLinkIntent)
                    }
                    finish()
                    return
                }

                Constants.ACTION_OPEN_MEGA_FOLDER_LINK -> {
                    Timber.d("ACTION_OPEN_MEGA_FOLDER_LINK")
                    val intentFolderLink = Intent(this, FolderLinkComposeActivity::class.java)
                    intentFolderLink.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    intentFolderLink.action = Constants.ACTION_OPEN_MEGA_FOLDER_LINK
                    intent.dataString?.let {
                        intentFolderLink.data = Uri.parse(it)
                        startActivity(intentFolderLink)
                    }
                    finish()
                }

                Constants.ACTION_OVERQUOTA_STORAGE -> {
                    showOverQuotaAlert(false)
                }

                Constants.ACTION_PRE_OVERQUOTA_STORAGE -> {
                    showOverQuotaAlert(true)
                }

                Constants.ACTION_CANCEL_CAM_SYNC -> {
                    Timber.d("ACTION_CANCEL_UPLOAD or ACTION_CANCEL_DOWNLOAD or ACTION_CANCEL_CAM_SYNC")
                    drawerItem = DrawerItem.TRANSFERS
                    transferPageViewModel.setTransfersTab(
                        intent.serializable(TRANSFERS_TAB) ?: TransfersTab.NONE
                    )
                    selectDrawerItem(drawerItem)
                    val text: String = getString(R.string.cam_sync_cancel_sync)
                    val builder = MaterialAlertDialogBuilder(this)
                    builder.setMessage(text)
                    builder.setPositiveButton(
                        getString(R.string.general_yes)
                    ) { _: DialogInterface?, _: Int ->
                        viewModel.stopCameraUploads(shouldReschedule = false)
                        transferPageFragment?.destroyActionMode()
                    }
                    builder.setNegativeButton(getString(R.string.general_no), null)
                    val dialog = builder.create()
                    try {
                        dialog.show()
                    } catch (ex: Exception) {
                        Timber.e(ex)
                    }
                }

                Constants.ACTION_SHOW_TRANSFERS -> {
                    if (intent.getBooleanExtra(Constants.OPENED_FROM_CHAT, false)) {
                        sendBroadcast(
                            Intent(ACTION_CLOSE_CHAT_AFTER_OPEN_TRANSFERS).setPackage(
                                applicationContext.packageName
                            )
                        )
                    }
                    drawerItem = DrawerItem.TRANSFERS
                    transferPageViewModel.setTransfersTab(
                        intent.serializable(TRANSFERS_TAB) ?: TransfersTab.NONE
                    )
                    selectDrawerItem(drawerItem)
                }

                Constants.ACTION_TAKE_SELFIE -> {
                    Timber.d("Intent take selfie")
                    Util.checkTakePicture(this, Constants.TAKE_PHOTO_CODE)
                }

                Constants.SHOW_REPEATED_UPLOAD -> {
                    Timber.d("Intent SHOW_REPEATED_UPLOAD")
                    val message = intent.getStringExtra("MESSAGE")
                    showSnackbar(Constants.SNACKBAR_TYPE, message, -1)
                }

                Constants.ACTION_IPC -> {
                    Timber.d("IPC - go to received request in Contacts")
                    markNotificationsSeen(true)
                    navigateToContactRequests()
                }

                Constants.ACTION_CHAT_NOTIFICATION_MESSAGE -> {
                    Timber.d("ACTION_CHAT_NOTIFICATION_MESSAGE")
                    val chatId: Long = intent.getLongExtra(
                        Constants.CHAT_ID,
                        MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                    )
                    if (intent.getBooleanExtra(Constants.EXTRA_MOVE_TO_CHAT_SECTION, false)) {
                        moveToChatSection(chatId)
                    } else {
                        val text = intent.getStringExtra(Constants.SHOW_SNACKBAR)
                        if (chatId != -1L) {
                            openChat(chatId, text)
                        }
                    }
                }

                Constants.ACTION_CHAT_SUMMARY -> {
                    Timber.d("ACTION_CHAT_SUMMARY")
                    drawerItem = DrawerItem.CHAT
                    selectDrawerItem(drawerItem)
                }

                Constants.ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION -> {
                    Timber.d("ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION")
                    markNotificationsSeen(true)
                    drawerItem = DrawerItem.SHARED_ITEMS
                    viewModel.setSharesTab(SharesTab.INCOMING_TAB)
                    selectDrawerItem(drawerItem)
                }

                Constants.ACTION_OPEN_CONTACTS_SECTION -> {
                    Timber.d("ACTION_OPEN_CONTACTS_SECTION")
                    markNotificationsSeen(true)
                    openContactLink(intent.getLongExtra(Constants.CONTACT_HANDLE, -1))
                }

                Constants.ACTION_OPEN_FOLDER -> {
                    Timber.d("Open after LauncherFileExplorerActivity ")
                    val handleIntent: Long =
                        intent.getLongExtra(Constants.INTENT_EXTRA_KEY_PARENT_HANDLE, -1)
                    if (intent.getBooleanExtra(Constants.SHOW_MESSAGE_UPLOAD_STARTED, false)) {
                        val numberUploads: Int =
                            intent.getIntExtra(Constants.NUMBER_UPLOADS, 1)
                        showSnackbar(
                            Constants.SNACKBAR_TYPE,
                            resources.getQuantityString(
                                R.plurals.upload_began,
                                numberUploads,
                                numberUploads
                            ),
                            -1
                        )
                    }
                    intent.getStringExtra(Constants.EXTRA_MESSAGE)?.let {
                        showSnackbar(
                            Constants.SNACKBAR_TYPE,
                            it,
                            MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                        )
                    }
                    actionOpenFolder(handleIntent)
                    selectDrawerItem(drawerItem)
                }

                Constants.ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE -> {
                    val chatId: Long = intent.getLongExtra(
                        Constants.CHAT_ID,
                        MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                    )
                    showSnackbar(Constants.MESSAGE_SNACKBAR_TYPE, null, chatId)
                }
            }
            intent.action = null
            intent = null
        }
        resetNavigationViewMenu(bottomNavigationView.menu)
        when (drawerItem) {
            DrawerItem.CLOUD_DRIVE -> {
                Timber.d("Case CLOUD DRIVE")
                //Check the tab to shown and the title of the actionBar
                setToolbarTitle()
                setBottomNavigationMenuItemChecked(CLOUD_DRIVE_BNV)
            }

            DrawerItem.SHARED_ITEMS -> {
                Timber.d("Case SHARED ITEMS")
                setBottomNavigationMenuItemChecked(SHARED_ITEMS_BNV)
                try {
                    val notificationManager =
                        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(Constants.NOTIFICATION_PUSH_CLOUD_DRIVE)
                } catch (e: Exception) {
                    Timber.e(e, "Exception NotificationManager - remove contact notification")
                }
                setToolbarTitle()
            }

            DrawerItem.SEARCH -> {
                if (searchExpand) {
                    return
                }
                setBottomNavigationMenuItemChecked(NO_BNV)
                setToolbarTitle()
            }

            DrawerItem.CHAT -> {
                setBottomNavigationMenuItemChecked(CHAT_BNV)
            }

            DrawerItem.PHOTOS -> {
                setBottomNavigationMenuItemChecked(PHOTOS_BNV)
            }

            DrawerItem.NOTIFICATIONS -> {}
            DrawerItem.HOMEPAGE -> setBottomNavigationMenuItemChecked(HOME_BNV)
            else -> setBottomNavigationMenuItemChecked(HOME_BNV)
        }
    }

    private fun openChat(chatId: Long, text: String?) {
        Timber.d("Chat ID: %s", chatId)
        if (chatId != -1L) {
            val chat = megaChatApi.getChatRoom(chatId)
            if (chat != null) {
                Timber.d("Open chat with id: %s", chatId)
                val intentToChat = Intent(this, ChatActivity::class.java)
                intentToChat.action = Constants.ACTION_CHAT_SHOW_MESSAGES
                intentToChat.putExtra(Constants.CHAT_ID, chatId)
                intentToChat.putExtra(Constants.SHOW_SNACKBAR, text)
                this.startActivity(intentToChat)
            } else {
                Timber.e("Error, chat is NULL")
            }
        } else {
            Timber.e("Error, chat id is -1")
        }
    }

    override fun onStop() {
        Timber.d("onStop")
        mStopped = true
        super.onStop()
    }

    override fun onPause() {
        Timber.d("onPause")
        transfersManagement.isOnTransfersSection = false
        super.onPause()
    }

    override fun onDestroy() {
        Timber.d("onDestroy()")
        dbH.removeSentPendingMessages()
        megaApi.removeRequestListener(this)
        composite.clear()
        unregisterReceiver(contactUpdateReceiver)
        LiveEventBus.get(Constants.EVENT_FAB_CHANGE, Boolean::class.java)
            .removeObserver(fabChangeObserver)
        cancelSearch()
        reconnectDialog?.cancel()
        dismissAlertDialogIfExists(processFileDialog)
        dismissAlertDialogIfExists(openLinkDialog)
        nodeSaver.destroy()
        super.onDestroy()
    }

    private fun cancelSearch() {
        searchViewModel.cancelSearch()
    }

    fun skipToMediaDiscoveryFragment(fragment: Fragment, mediaHandle: Long) {
        mediaDiscoveryFragment = fragment
        replaceFragment(fragment, FragmentTag.MEDIA_DISCOVERY.tag)
        viewModel.onMediaDiscoveryOpened(mediaHandle)
        isInMDMode = true
        viewModel.setIsFirstNavigationLevel(false)
    }

    fun skipToAlbumContentFragment(fragment: Fragment) {
        albumContentFragment = fragment
        replaceFragment(fragment, FragmentTag.ALBUM_CONTENT.tag)
        isInAlbumContent = true
        viewModel.setIsFirstNavigationLevel(false)
        showHideBottomNavigationView(true)
    }

    fun skipToFilterFragment(fragment: PhotosFilterFragment) {
        photosFilterFragment = fragment
        replaceFragment(fragment, FragmentTag.PHOTOS_FILTER.tag)
        isInFilterPage = true
        viewModel.setIsFirstNavigationLevel(false)
        showHideBottomNavigationView(true)
    }

    private fun replaceFragment(fragment: Fragment, fragmentTag: String?) {
        val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_container, fragment, fragmentTag)
        ft.commitNowAllowingStateLoss()
    }

    fun refreshFragment(fragmentTag: String) {
        supportFragmentManager.findFragmentByTag(fragmentTag)?.let {
            Timber.d("Fragment %s refreshing", fragmentTag)
            supportFragmentManager.beginTransaction().detach(it).commitNowAllowingStateLoss()
            supportFragmentManager.beginTransaction().attach(it).commitNowAllowingStateLoss()
        }
    }

    private fun selectDrawerItemCloudDrive() {
        Timber.d("selectDrawerItemCloudDrive")
        appBarLayout.visibility = View.VISIBLE
        tabLayoutShares.visibility = View.GONE
        viewPagerShares.visibility = View.GONE
        fragmentContainer.visibility = View.VISIBLE

        fileBrowserComposeFragment =
            (supportFragmentManager.findFragmentByTag(FragmentTag.CLOUD_DRIVE_COMPOSE.tag) as? FileBrowserComposeFragment
                ?: FileBrowserComposeFragment.newInstance()).also {
                replaceFragment(it, FragmentTag.CLOUD_DRIVE_COMPOSE.tag)
            }
    }

    private fun showGlobalAlertDialogsIfNeeded() {
        if (showStorageAlertWithDelay) {
            showStorageAlertWithDelay = false
            checkStorageStatus(
                if (storageStateFromBroadcast !== StorageState.Unknown) storageStateFromBroadcast else viewModel.getStorageState()
            )
        }
        if (!firstTimeAfterInstallation) {
            Timber.d("Its NOT first time")
            userInfoViewModel.refreshContactDatabase(false)
        } else {
            Timber.d("Its first time")
            userInfoViewModel.refreshContactDatabase(true)
            firstTimeAfterInstallation = false
            dbH.setFirstTime(false)
        }
        cookieDialogHandler.showDialogIfNeeded(this)
    }

    /**
     * Observe LiveData for PSA, and show PSA view when get it.
     */
    private fun observePsa() {
        LiveEventBus.get(Constants.EVENT_PSA, Psa::class.java)
            .observe(this) { psa: Psa ->
                if (psa.url.isNullOrEmpty()) {
                    showPsa(psa)
                }
            }
    }

    /**
     * Show PSA view for old PSA type.
     *
     * @param psa the PSA to show
     */
    private fun showPsa(psa: Psa?) {
        if (psa == null || drawerItem !== DrawerItem.HOMEPAGE || homepageScreen !== HomepageScreen.HOMEPAGE) {
            updateHomepageFabPosition()
            return
        }
        if (lifecycle.currentState == Lifecycle.State.RESUMED && getProLayout.visibility == View.GONE && TextUtils.isEmpty(
                psa.url
            )
        ) {
            psaViewHolder?.bind(psa)
            handler.post { updateHomepageFabPosition() }
        }
    }

    fun setToolbarTitle(title: String?) {
        supportActionBar?.title = title
    }

    fun setToolbarTitle() = lifecycleScope.launch {
        Timber.d("setToolbarTitle")
        if (drawerItem == null) {
            return@launch
        }
        if (listOf(DrawerItem.SYNC, DrawerItem.DEVICE_CENTER).contains(drawerItem)) {
            supportActionBar?.hide()
        } else {
            supportActionBar?.show()
        }
        when (drawerItem) {
            DrawerItem.CLOUD_DRIVE -> {
                supportActionBar?.subtitle = null
                Timber.d("Cloud Drive SECTION")
                val parentNode = withContext(ioDispatcher) {
                    megaApi.getNodeByHandle(fileBrowserViewModel.state().fileBrowserHandle)
                }
                if (parentNode != null) {
                    if (megaApi.rootNode != null) {
                        if ((parentNode.handle == megaApi.rootNode?.handle
                                    || fileBrowserViewModel.state().fileBrowserHandle == -1L)
                            && !isInMDMode
                        ) {
                            supportActionBar?.title = getString(R.string.section_cloud_drive)
                            viewModel.setIsFirstNavigationLevel(true)
                        } else {
                            supportActionBar?.title = parentNode.name
                            viewModel.setIsFirstNavigationLevel(false)
                        }
                    } else {
                        fileBrowserViewModel.setBrowserParentHandle(-1)
                    }
                } else {
                    if (megaApi.rootNode != null) {
                        fileBrowserViewModel.setBrowserParentHandle(
                            megaApi.rootNode?.handle ?: INVALID_HANDLE
                        )
                        supportActionBar?.title = getString(R.string.title_mega_info_empty_screen)
                        viewModel.setIsFirstNavigationLevel(true)
                    } else {
                        fileBrowserViewModel.setBrowserParentHandle(-1)
                        viewModel.setIsFirstNavigationLevel(true)
                    }
                }
            }

            DrawerItem.SYNC,
            DrawerItem.DEVICE_CENTER,
            -> {
                viewModel.setIsFirstNavigationLevel(false)
            }

            DrawerItem.RUBBISH_BIN -> {
                supportActionBar?.subtitle = null
                val node =
                    megaApi.getNodeByHandle(rubbishBinViewModel.state().rubbishBinHandle)
                val rubbishNode = megaApi.rubbishNode
                if (rubbishNode == null) {
                    rubbishBinViewModel.setRubbishBinHandle(INVALID_HANDLE)
                    viewModel.setIsFirstNavigationLevel(true)
                } else if (rubbishBinViewModel.state().rubbishBinHandle == INVALID_HANDLE || node == null || node.handle == rubbishNode.handle) {
                    supportActionBar?.title = getString(R.string.section_rubbish_bin)
                    viewModel.setIsFirstNavigationLevel(true)
                } else {
                    supportActionBar?.title = node.name
                    viewModel.setIsFirstNavigationLevel(false)
                }
            }

            DrawerItem.SHARED_ITEMS -> {
                setToolbarForSharedItemsDrawerItem()
            }

            DrawerItem.BACKUPS -> {
                supportActionBar?.subtitle = null
                // If the Backups Parent Handle is equal to the My Backups Folder Handle or is -1L,
                // then set the corresponding title and first navigation level
                if (backupsViewModel.isCurrentlyOnBackupFolderLevel()) {
                    supportActionBar?.title =
                        resources.getString(R.string.home_side_menu_backups_title)
                    viewModel.setIsFirstNavigationLevel(true)
                } else {
                    val node = withContext(ioDispatcher) {
                        megaApi.getNodeByHandle(backupsViewModel.state().backupsHandle)
                    }
                    supportActionBar?.title = node?.name
                    viewModel.setIsFirstNavigationLevel(false)
                }
            }

            DrawerItem.NOTIFICATIONS -> {
                supportActionBar?.subtitle = null
                supportActionBar?.title =
                    getString(R.string.title_properties_chat_contact_notifications)
                viewModel.setIsFirstNavigationLevel(true)
            }

            DrawerItem.CHAT -> {
                appBarLayout.visibility = View.VISIBLE
                supportActionBar?.title = getString(R.string.section_chat)
                viewModel.setIsFirstNavigationLevel(true)
            }

            DrawerItem.SEARCH -> {
                supportActionBar?.subtitle = null
                if (searchViewModel.state.value.searchParentHandle == -1L) {
                    viewModel.setIsFirstNavigationLevel(true)
                    if (searchViewModel.state.value.searchQuery != null) {
                        searchViewModel.setTextSubmitted(true)
                        val state = searchViewModel.state.value
                        if (state.searchQuery?.isNotEmpty() == true) {
                            supportActionBar?.setTitle(getString(R.string.action_search) + ": " + searchViewModel.state.value.searchQuery)
                        } else {
                            supportActionBar?.setTitle(getString(R.string.action_search) + ": " + "")
                        }
                    } else {
                        supportActionBar?.setTitle(getString(R.string.action_search) + ": " + "")
                    }
                } else {
                    val parentNode =
                        megaApi.getNodeByHandle(searchViewModel.state.value.searchParentHandle)
                    if (parentNode != null) {
                        supportActionBar?.title = parentNode.name
                        viewModel.setIsFirstNavigationLevel(false)
                    }
                }
            }

            DrawerItem.TRANSFERS -> {
                supportActionBar?.subtitle = null
                supportActionBar?.title = getString(R.string.section_transfers)
                isFirstNavigationLevel = true
            }

            DrawerItem.PHOTOS -> {
                supportActionBar?.subtitle = null
                if (isInAlbumContent) {
                    if (albumContentFragment is AlbumDynamicContentFragment) {
                        val title = (albumContentFragment as? AlbumDynamicContentFragment)
                            ?.getCurrentAlbumTitle()
                        supportActionBar?.setTitle(title)
                    } else if (albumContentFragment is AlbumContentFragment) {
                        val title = (albumContentFragment as? AlbumContentFragment)
                            ?.getCurrentAlbumTitle()
                        supportActionBar?.setTitle(title)
                    } else {
                        supportActionBar?.setTitle(getString(R.string.title_favourites_album))
                    }
                    viewModel.setIsFirstNavigationLevel(false)
                } else if (isInFilterPage) {
                    supportActionBar?.setTitle(getString(R.string.photos_action_filter))
                    viewModel.setIsFirstNavigationLevel(false)
                } else if (getPhotosFragment() != null) {
                    supportActionBar?.title = getString(R.string.sortby_type_photo_first)
                    viewModel.setIsFirstNavigationLevel(!canPhotosEnableCUViewBack())
                }
            }

            DrawerItem.HOMEPAGE -> {
                run {
                    this@ManagerActivity.isFirstNavigationLevel = false
                    var titleId = -1
                    when (homepageScreen) {
                        HomepageScreen.FAVOURITES -> titleId = R.string.favourites_category_title
                        HomepageScreen.DOCUMENTS -> titleId = R.string.section_documents
                        HomepageScreen.AUDIO -> titleId = R.string.upload_to_audio
                        HomepageScreen.VIDEO -> titleId = R.string.sortby_type_video_first
                        HomepageScreen.RECENT_BUCKET -> {
                            getFragmentByType(
                                RecentActionBucketFragment::class.java
                            )?.setupToolbar()
                        }

                        else -> {}
                    }
                    if (titleId != -1) {
                        supportActionBar?.title = getString(titleId)
                    }
                }
                run { Timber.d("Default GONE") }
            }

            else -> {
                Timber.d("Default GONE")
            }
        }
        viewModel.checkNumUnreadUserAlerts(UnreadUserAlertsCheckType.NAVIGATION_TOOLBAR_ICON)
    }

    private fun setToolbarForSharedItemsDrawerItem() {
        Timber.d("Shared Items SECTION")
        supportActionBar?.subtitle = null
        val indexShares: SharesTab = tabItemShares
        if (indexShares === SharesTab.NONE) return
        when (indexShares) {
            SharesTab.INCOMING_TAB -> {
                if (isIncomingAdded) {
                    if (incomingSharesViewModel.state().incomingHandle != -1L) {
                        val node =
                            megaApi.getNodeByHandle(incomingSharesViewModel.state().incomingHandle)
                        if (node == null) {
                            supportActionBar?.setTitle(resources.getString(R.string.title_shared_items))
                        } else {
                            supportActionBar?.setTitle(node.name)
                        }
                        viewModel.setIsFirstNavigationLevel(false)
                    } else {
                        supportActionBar?.title = resources.getString(R.string.title_shared_items)
                        viewModel.setIsFirstNavigationLevel(true)
                    }
                } else {
                    Timber.d("selectDrawerItemSharedItems: inSFLol == null")
                }
            }

            SharesTab.OUTGOING_TAB -> {
                Timber.d("setToolbarTitle: OUTGOING TAB")
                if (isOutgoingAdded) {
                    if (outgoingSharesViewModel.state().outgoingHandle != -1L) {
                        val node =
                            megaApi.getNodeByHandle(outgoingSharesViewModel.state().outgoingHandle)
                        supportActionBar?.title = node?.name
                        viewModel.setIsFirstNavigationLevel(false)
                    } else {
                        supportActionBar?.title = resources.getString(R.string.title_shared_items)
                        viewModel.setIsFirstNavigationLevel(true)
                    }
                }
            }

            SharesTab.LINKS_TAB -> if (isLinksAdded) {
                if (legacyLinksViewModel.state().linksHandle == INVALID_HANDLE) {
                    supportActionBar?.title = resources.getString(R.string.title_shared_items)
                    viewModel.setIsFirstNavigationLevel(true)
                } else {
                    val node =
                        megaApi.getNodeByHandle(legacyLinksViewModel.state().linksHandle)
                    supportActionBar?.title = node?.name
                    viewModel.setIsFirstNavigationLevel(false)
                }
            }

            else -> {
                supportActionBar?.title = resources.getString(R.string.title_shared_items)
                viewModel.setIsFirstNavigationLevel(true)
            }
        }
    }

    fun setToolbarTitleFromFullscreenOfflineFragment(
        title: String?,
        firstNavigationLevel: Boolean, showSearch: Boolean,
    ) {
        supportActionBar?.subtitle = null
        supportActionBar?.title = title
        viewModel.setIsFirstNavigationLevel(firstNavigationLevel)
        viewModel.checkNumUnreadUserAlerts(UnreadUserAlertsCheckType.NAVIGATION_TOOLBAR_ICON)
        searchViewModel.setTextSubmitted(true)
        searchMenuItem?.isVisible = showSearch
    }

    fun updateNavigationToolbarIcon(numUnreadUserAlerts: Int) {
        val totalIncomingContactRequestCount = viewModel.incomingContactRequests.value.size
        val totalNotifications = numUnreadUserAlerts + totalIncomingContactRequestCount
        if (totalNotifications == 0) {
            if (isFirstNavigationLevel) {
                if (drawerItem === DrawerItem.SEARCH || drawerItem === DrawerItem.BACKUPS || drawerItem === DrawerItem.NOTIFICATIONS || drawerItem === DrawerItem.RUBBISH_BIN || drawerItem === DrawerItem.TRANSFERS) {
                    supportActionBar?.setHomeAsUpIndicator(
                        tintIcon(
                            this,
                            R.drawable.ic_arrow_back_white
                        )
                    )
                } else {
                    supportActionBar?.setHomeAsUpIndicator(tintIcon(this, R.drawable.ic_menu_white))
                }
            } else {
                supportActionBar?.setHomeAsUpIndicator(
                    tintIcon(
                        this,
                        R.drawable.ic_arrow_back_white
                    )
                )
            }
        } else {
            if (isFirstNavigationLevel) {
                if (drawerItem === DrawerItem.SEARCH || drawerItem === DrawerItem.BACKUPS || drawerItem === DrawerItem.NOTIFICATIONS || drawerItem === DrawerItem.RUBBISH_BIN || drawerItem === DrawerItem.TRANSFERS) {
                    badgeDrawable?.progress = 1.0f
                } else {
                    badgeDrawable?.progress = 0.0f
                }
            } else {
                badgeDrawable?.progress = 1.0f
            }
            if (totalNotifications > 9) {
                badgeDrawable?.text = "9+"
            } else {
                badgeDrawable?.text = totalNotifications.toString() + ""
            }
            supportActionBar?.setHomeAsUpIndicator(badgeDrawable)
        }
        if (drawerItem === DrawerItem.PHOTOS) {
            setPhotosNavigationToolbarIcon()
        }
    }

    /**
     * When the user is in Photos, this sets the correct Toolbar Icon depending on
     * certain conditions.
     *
     *
     * This is only called when there are no unread notifications
     */
    private fun setPhotosNavigationToolbarIcon() {
        when {
            isInAlbumContent -> {
                supportActionBar?.setHomeAsUpIndicator(
                    tintIcon(
                        this,
                        R.drawable.ic_arrow_back_white
                    )
                )
            }

            isInFilterPage -> {
                supportActionBar?.setHomeAsUpIndicator(tintIcon(this, R.drawable.ic_close_white))
            }

            else -> {
                if (getPhotosFragment() != null) {
                    if (canPhotosEnableCUViewBack()) {
                        supportActionBar?.setHomeAsUpIndicator(
                            tintIcon(
                                this,
                                R.drawable.ic_arrow_back_white
                            )
                        )
                    } else {
                        supportActionBar?.setHomeAsUpIndicator(
                            tintIcon(
                                this,
                                R.drawable.ic_menu_white
                            )
                        )
                    }
                }
            }
        }
    }

    private fun canPhotosEnableCUViewBack() =
        viewModel.state().isFirstLogin
                && photosFragment?.isEnableCameraUploadsViewShown() == true
                && photosFragment?.doesAccountHavePhotos() == true
                && photosFragment?.isInTimeline() == true

    private fun showOnlineMode() {
        Timber.d("showOnlineMode")
        try {
            if (rootNode != null) {
                resetNavigationViewMenu(bottomNavigationView.menu)
                clickDrawerItem(drawerItem)
                supportInvalidateOptionsMenu()
                checkCurrentStorageStatus()
            } else {
                Timber.w("showOnlineMode - Root is NULL")
                if (MegaApplication.openChatId == MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
                    ConfirmConnectDialogFragment().show(
                        supportFragmentManager,
                        ConfirmConnectDialogFragment.TAG
                    )
                }
            }
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    private fun showOfflineMode() {
        Timber.d("showOfflineMode")
        try {
            Timber.d("DrawerItem on start offline: %s", drawerItem)
            if (drawerItem == null) {
                Timber.w("drawerItem == null --> On start OFFLINE MODE")
                drawerItem = getStartDrawerItem()
                disableNavigationViewMenu(bottomNavigationView.menu)
                selectDrawerItem(drawerItem)
            } else {
                disableNavigationViewMenu(bottomNavigationView.menu)
                Timber.d("Change to OFFLINE MODE")
                clickDrawerItem(drawerItem)
            }
            supportInvalidateOptionsMenu()
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    private fun clickDrawerItem(item: DrawerItem?) {
        Timber.d("Item: %s", item)
        val bNVMenu = bottomNavigationView.menu
        if (item == null) {
            drawerMenuItem = bNVMenu.findItem(R.id.bottom_navigation_item_cloud_drive)
            drawerMenuItem?.let { onNavigationItemSelected(it) }
            return
        }
        closeDrawer()
        when (item) {
            DrawerItem.CLOUD_DRIVE -> {
                setBottomNavigationMenuItemChecked(CLOUD_DRIVE_BNV)
            }

            DrawerItem.HOMEPAGE -> {
                setBottomNavigationMenuItemChecked(HOME_BNV)
            }

            DrawerItem.PHOTOS -> {
                setBottomNavigationMenuItemChecked(PHOTOS_BNV)
            }

            DrawerItem.SHARED_ITEMS -> {
                setBottomNavigationMenuItemChecked(SHARED_ITEMS_BNV)
            }

            DrawerItem.CHAT -> {
                setBottomNavigationMenuItemChecked(CHAT_BNV)
            }

            DrawerItem.SEARCH, DrawerItem.TRANSFERS, DrawerItem.NOTIFICATIONS, DrawerItem.BACKUPS -> {
                setBottomNavigationMenuItemChecked(NO_BNV)
            }

            else -> {}
        }
    }

    private fun selectDrawerItemSharedItems() {
        Timber.d("selectDrawerItemSharedItems")
        appBarLayout.visibility = View.VISIBLE
        try {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(Constants.NOTIFICATION_PUSH_CLOUD_DRIVE)
        } catch (e: Exception) {
            Timber.e(e, "Exception NotificationManager - remove contact notification")
        }

        if (viewPagerShares.adapter == null) {
            viewPagerShares.adapter = sharesPageAdapter
        }
        TabLayoutMediator(
            tabLayoutShares,
            viewPagerShares
        ) { tab: TabLayout.Tab, position: Int ->
            when (position) {
                SharesTab.INCOMING_TAB.position -> {
                    tab.setText(R.string.tab_incoming_shares)
                    tab.setIcon(R.drawable.ic_incoming_shares)
                }

                SharesTab.OUTGOING_TAB.position -> {
                    tab.setText(R.string.tab_outgoing_shares)
                    tab.setIcon(R.drawable.ic_outgoing_shares)
                }

                SharesTab.LINKS_TAB.position -> {
                    tab.setText(R.string.tab_links_shares)
                    tab.setIcon(CoreUiR.drawable.link_ic)
                }
            }
        }.attach()
        updateSharesTab()
        setToolbarTitle()
        closeDrawer()
    }

    private fun selectDrawerItemNotifications() {
        Timber.d("selectDrawerItemNotifications")
        appBarLayout.visibility = View.VISIBLE
        drawerItem = DrawerItem.NOTIFICATIONS
        setBottomNavigationMenuItemChecked(NO_BNV)
        var notificationsFragment =
            supportFragmentManager.findFragmentByTag(FragmentTag.NOTIFICATIONS.tag) as? NotificationsFragment
        if (notificationsFragment == null) {
            Timber.w("New NotificationsFragment")
            notificationsFragment = NotificationsFragment.newInstance()
        } else {
            refreshFragment(FragmentTag.NOTIFICATIONS.tag)
        }
        replaceFragment(notificationsFragment, FragmentTag.NOTIFICATIONS.tag)
        setToolbarTitle()
        showFabButton()
    }

    private fun selectDrawerItemTransfers() {
        Timber.d("selectDrawerItemTransfers")
        appBarLayout.visibility = View.VISIBLE
        hideTransfersWidget()
        drawerItem = DrawerItem.TRANSFERS
        setBottomNavigationMenuItemChecked(NO_BNV)
        transfersManagementViewModel.checkIfShouldShowCompletedTab()
        replaceFragment(
            transferPageFragment ?: TransferPageFragment.newInstance(),
            FragmentTag.TRANSFERS_PAGE.tag
        )
        setToolbarTitle()
        showFabButton()
        closeDrawer()
    }

    /**
     * Select the chat drawer item
     *
     * @param chatId    If the chat drawer item is selected when opening a chat room, it is possible
     *                  to pass here the chat ID to determine whether it is a chat or a meeting and
     *                  select the corresponding tab as the initial tab.
     */
    private fun selectDrawerItemChat(chatId: Long? = null) {
        setToolbarTitle()
        chatTabsFragment = chatsFragment
        if (chatTabsFragment == null) {
            var showMeetingTab = false
            chatId?.let {
                megaChatApi.getChatRoom(it)?.let { chatRoom ->
                    showMeetingTab = chatRoom.isMeeting
                }
            }
            chatTabsFragment = ChatTabsFragment.newInstance(showMeetingTab = showMeetingTab)
        } else {
            refreshFragment(FragmentTag.RECENT_CHAT.tag)
        }
        chatTabsFragment?.let { replaceFragment(it, FragmentTag.RECENT_CHAT.tag) }
        closeDrawer()
        PermissionUtils.checkNotificationsPermission(this)
        hideFabButton()
    }

    private fun setBottomNavigationMenuItemChecked(item: Int) {
        if (item == NO_BNV) {
            showHideBottomNavigationView(true)
        } else if (bottomNavigationView.menu.getItem(item) != null) {
            if (bottomNavigationView.menu.getItem(item)?.isChecked == false) {
                bottomNavigationView.menu.getItem(item)?.isChecked = true
            }
        }
        val isCameraUploadItem = item == PHOTOS_BNV
        updateMiniAudioPlayerVisibility(!isCameraUploadItem)
    }

    private fun setTabsVisibility() {
        tabLayoutShares.visibility = View.GONE
        viewPagerShares.visibility = View.GONE
        mShowAnyTabLayout = false
        fragmentContainer.visibility = View.GONE
        navHostView.visibility = View.GONE
        updatePsaViewVisibility()
        if (turnOnNotifications) {
            fragmentContainer.visibility = View.VISIBLE
            closeDrawer()
            return
        }
        when (drawerItem) {
            DrawerItem.SHARED_ITEMS -> {
                val tabItemShares: SharesTab = tabItemShares
                if (tabItemShares === SharesTab.INCOMING_TAB
                    && incomingSharesViewModel.state().incomingHandle != INVALID_HANDLE
                    || tabItemShares === SharesTab.OUTGOING_TAB
                    && outgoingSharesViewModel.state().outgoingHandle != INVALID_HANDLE
                    || tabItemShares === SharesTab.LINKS_TAB
                    && legacyLinksViewModel.state().linksHandle != INVALID_HANDLE
                ) {
                    tabLayoutShares.visibility = View.GONE
                    viewPagerShares.isUserInputEnabled = false
                } else {
                    tabLayoutShares.visibility = View.VISIBLE
                    viewPagerShares.isUserInputEnabled = true
                }
                viewPagerShares.visibility = View.VISIBLE
                mShowAnyTabLayout = true
            }

            DrawerItem.HOMEPAGE -> navHostView.visibility = View.VISIBLE
            else -> {
                fragmentContainer.visibility = View.VISIBLE
            }
        }
        LiveEventBus.get(Constants.EVENT_HOMEPAGE_VISIBILITY, Boolean::class.java)
            .post(drawerItem === DrawerItem.HOMEPAGE)
        closeDrawer()
    }

    /**
     * Hides or shows tabs of a section depending on the navigation level
     * and if select mode is enabled or not.
     *
     * @param hide       If true, hides the tabs, else shows them.
     * @param currentTab The current tab where the action happens.
     */
    fun hideTabs(hide: Boolean, currentTab: Tab) {
        val visibility = if (hide) View.GONE else View.VISIBLE
        when (drawerItem) {
            DrawerItem.SHARED_ITEMS -> {
                if (currentTab !is SharesTab) return
                when (currentTab) {
                    SharesTab.INCOMING_TAB -> if (!isIncomingAdded || !hide && incomingSharesViewModel.state().incomingHandle != INVALID_HANDLE) {
                        return
                    }

                    SharesTab.OUTGOING_TAB -> if (!isOutgoingAdded || !hide && outgoingSharesViewModel.state().outgoingHandle != INVALID_HANDLE) {
                        return
                    }

                    SharesTab.LINKS_TAB -> if (!isLinksAdded || !hide && legacyLinksViewModel.state().linksHandle != INVALID_HANDLE) {
                        return
                    }

                    else -> {}
                }
                tabLayoutShares.visibility = visibility
                viewPagerShares.isUserInputEnabled = !hide
            }

            else -> {}
        }
    }

    private fun removeFragment(fragment: Fragment?) {
        if (fragment != null && fragment.isAdded) {
            val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
            ft.remove(fragment)
            ft.commitAllowingStateLoss()
        }
    }

    /**
     * Set up a listener for navigating to a new destination (screen)
     * This only for Homepage for the time being since it is the only module to
     * which Jetpack Navigation applies.
     * It updates the status variable such as mHomepageScreen, as well as updating
     * BNV, Toolbar title, etc.
     */
    private fun setupNavDestListener(
        navHostFragment: NavHostFragment,
    ) {
        navController = navHostFragment.navController
        navHostFragment.navController.addOnDestinationChangedListener { _: NavController?, destination: NavDestination, _: Bundle? ->
            val destinationId: Int = destination.id
            mHomepageSearchable = null
            when (destinationId) {
                R.id.homepageFragment -> {
                    homepageScreen = HomepageScreen.HOMEPAGE
                    updatePsaViewVisibility()
                    // Showing the bottom navigation view immediately because the initial dimension
                    // of Homepage bottom sheet is calculated based on it
                    showBNVImmediate()
                    if (bottomNavigationCurrentItem == HOME_BNV) {
                        appBarLayout.visibility = View.GONE
                    }
                    updateTransfersWidget()
                    setDrawerLockMode(false)
                    return@addOnDestinationChangedListener
                }

                R.id.favouritesFragment -> {
                    homepageScreen = HomepageScreen.FAVOURITES
                }

                R.id.documentsFragment -> {
                    homepageScreen = HomepageScreen.DOCUMENTS
                }

                R.id.audioFragment -> {
                    homepageScreen = HomepageScreen.AUDIO
                }

                R.id.videoFragment -> {
                    homepageScreen = HomepageScreen.VIDEO
                }

                R.id.fullscreen_offline -> {
                    homepageScreen = HomepageScreen.FULLSCREEN_OFFLINE
                }

                R.id.offline_file_info -> {
                    homepageScreen = HomepageScreen.OFFLINE_FILE_INFO
                    updatePsaViewVisibility()
                    appBarLayout.visibility = View.GONE
                    showHideBottomNavigationView(true)
                }

                R.id.recentBucketFragment -> {
                    homepageScreen = HomepageScreen.RECENT_BUCKET
                }
            }
            updateTransfersWidget()
            updatePsaViewVisibility()
            appBarLayout.visibility = View.VISIBLE
            showHideBottomNavigationView(true)
            supportInvalidateOptionsMenu()
            setToolbarTitle()
            setDrawerLockMode(true)
        }
    }

    /**
     * Hides all views only related to CU section and sets the CU default view.
     */
    private fun resetCUFragment() {
        cameraUploadViewTypes.visibility = View.GONE
        if (getPhotosFragment() != null) {
            showBottomView()
        }
    }

    override fun drawerItemClicked(item: DrawerItem) {
        val oldDrawerItem = drawerItem
        isFirstTimeCam
        checkIfShouldCloseSearchView(oldDrawerItem)
        if (item == DrawerItem.OFFLINE) {
            bottomItemBeforeOpenFullscreenOffline = bottomNavigationCurrentItem
            openFullscreenOfflineFragment(pathNavigationOffline)
        } else {
            drawerItem = item
            selectDrawerItem(item)
        }
    }

    /**
     * Select a drawer item
     *
     * @param item      [DrawerItem] to select
     * @param chatId    If the drawer item is the chat drawer item and it is selected when opening a
     *                  chat room, it is possible to pass here the chat ID to determine whether it
     *                  is a chat or a meeting and select the corresponding tab as the initial tab.
     */
    @SuppressLint("NewApi")
    @JvmOverloads
    fun selectDrawerItem(item: DrawerItem?, chatId: Long? = null) {
        Timber.d("Selected DrawerItem: ${item?.name}. Current drawerItem is ${drawerItem?.name}")
        if (!this::drawerLayout.isInitialized) {
            Timber.d("ManagerActivity doesn't call setContentView")
            return
        }
        drawerItem = item ?: drawerItem ?: DrawerItem.CLOUD_DRIVE

        // Homepage may hide the Appbar before
        appBarLayout.visibility = View.VISIBLE
        drawerItem = item
        Util.resetActionBar(supportActionBar)
        updateTransfersWidget()
        if (drawerItem == DrawerItem.TRANSFERS) {
            transfersViewModel.resetSelectedTab()
        } else {
            transfersViewModel.clearSelectedTab()
        }
        setCallWidget()
        if (item !== DrawerItem.CHAT) {
            //remove recent chat fragment as its life cycle get triggered unexpectedly, e.g. rotate device while not on recent chat page
            removeFragment(chatsFragment)
        }
        if (item !== DrawerItem.PHOTOS) {
            resetCUFragment()
        }
        if (item !== DrawerItem.TRANSFERS) {
            transferPageFragment?.destroyActionModeIfNeeded()
        }
        transfersManagement.isOnTransfersSection = item === DrawerItem.TRANSFERS
        when (item) {
            DrawerItem.CLOUD_DRIVE -> {
                if (!isInMDMode) {
                    selectDrawerItemCloudDrive()
                } else {
                    val mediaHandle: Long = fileBrowserViewModel.getSafeBrowserParentHandle()
                    val mediaDiscoveryFragment =
                        supportFragmentManager.findFragmentByTag(FragmentTag.MEDIA_DISCOVERY.tag)
                            ?: MediaDiscoveryFragment.getNewInstance(mediaHandle, false)

                    skipToMediaDiscoveryFragment(mediaDiscoveryFragment, mediaHandle)
                }
                if (openFolderRefresh) {
                    onNodesCloudDriveUpdate()
                    openFolderRefresh = false
                }
                supportInvalidateOptionsMenu()
                setToolbarTitle()
                showFabButton()
                showHideBottomNavigationView(false)
                if (!comesFromNotifications) {
                    bottomNavigationCurrentItem = CLOUD_DRIVE_BNV
                }
                setBottomNavigationMenuItemChecked(CLOUD_DRIVE_BNV)
                if (intent != null && intent.getBooleanExtra(
                        Constants.INTENT_EXTRA_KEY_LOCATION_FILE_INFO,
                        false
                    )
                ) {
                    fileBrowserViewModel.refreshNodes()
                }
                Timber.d("END for Cloud Drive")
            }

            DrawerItem.RUBBISH_BIN -> {
                showHideBottomNavigationView(true)
                appBarLayout.visibility = View.VISIBLE

                rubbishBinComposeFragment = getRubbishBinComposeFragment()
                    ?: RubbishBinComposeFragment.newInstance()
                rubbishBinComposeFragment?.let {
                    replaceFragment(
                        it,
                        FragmentTag.RUBBISH_BIN_COMPOSE.tag
                    )
                }

                setBottomNavigationMenuItemChecked(NO_BNV)

                if (openFolderRefresh) {
                    onNodesCloudDriveUpdate()
                    openFolderRefresh = false
                }

                supportInvalidateOptionsMenu()
                setToolbarTitle()
                showFabButton()
            }

            DrawerItem.DEVICE_CENTER -> {
                deviceCenterFragment =
                    supportFragmentManager.findFragmentByTag(FragmentTag.DEVICE_CENTER.tag) as? DeviceCenterFragment
                        ?: DeviceCenterFragment.newInstance()
                setBottomNavigationMenuItemChecked(NO_BNV)
                supportInvalidateOptionsMenu()
                deviceCenterFragment?.let { replaceFragment(it, FragmentTag.DEVICE_CENTER.tag) }
                showFabButton()
            }

            DrawerItem.SYNC -> {
                syncFragment =
                    supportFragmentManager.findFragmentByTag(FragmentTag.SYNC.tag) as? SyncFragment
                        ?: SyncFragment.newInstance()

                setBottomNavigationMenuItemChecked(NO_BNV)
                supportInvalidateOptionsMenu()
                syncFragment?.let { replaceFragment(it, FragmentTag.SYNC.tag) }
                showFabButton()
                updateTransfersWidgetPosition(false)
            }

            DrawerItem.HOMEPAGE -> {
                // Don't use fabButton.hide() here.
                fabButton.visibility = View.GONE
                if (homepageScreen === HomepageScreen.HOMEPAGE) {
                    showBNVImmediate()
                    appBarLayout.visibility = View.GONE
                    showHideBottomNavigationView(false)
                } else {
                    // For example, back from Rubbish Bin to Photos
                    setToolbarTitle()
                    invalidateOptionsMenu()
                    showHideBottomNavigationView(true)
                }
                setBottomNavigationMenuItemChecked(HOME_BNV)
                if (!comesFromNotifications) {
                    bottomNavigationCurrentItem = HOME_BNV
                }
                showGlobalAlertDialogsIfNeeded()
                if (homepageScreen === HomepageScreen.HOMEPAGE) {
                    changeAppBarElevation(false)
                }
            }

            DrawerItem.PHOTOS -> {
                if (isInAlbumContent || isInFilterPage) {
                    showHideBottomNavigationView(true)
                } else {
                    appBarLayout.visibility = View.VISIBLE
                    if (getPhotosFragment() == null) {
                        photosFragment =
                            PhotosFragment.newInstance(viewModel.state().isFirstLogin)
                    } else {
                        refreshFragment(FragmentTag.PHOTOS.tag)
                    }
                    photosFragment?.let { replaceFragment(it, FragmentTag.PHOTOS.tag) }
                    setToolbarTitle()
                    supportInvalidateOptionsMenu()
                    showFabButton()
                    showHideBottomNavigationView(false)
                    if (!comesFromNotifications) {
                        bottomNavigationCurrentItem = PHOTOS_BNV
                    }
                    setBottomNavigationMenuItemChecked(PHOTOS_BNV)
                }
            }

            DrawerItem.BACKUPS -> {
                showHideBottomNavigationView(true)
                appBarLayout.visibility = View.VISIBLE
                backupsFragment =
                    supportFragmentManager.findFragmentByTag(FragmentTag.BACKUPS.tag) as? BackupsFragment
                if (backupsFragment == null) {
                    backupsFragment = BackupsFragment.newInstance()
                }
                backupsFragment?.let { replaceFragment(it, FragmentTag.BACKUPS.tag) }
                if (openFolderRefresh) {
                    onNodesBackupsUpdate()
                    openFolderRefresh = false
                }
                supportInvalidateOptionsMenu()
                setToolbarTitle()
                showFabButton()
            }

            DrawerItem.SHARED_ITEMS -> {
                onSelectSharedItemsDrawerItem()
            }

            DrawerItem.NOTIFICATIONS -> {
                showHideBottomNavigationView(true)
                selectDrawerItemNotifications()
                supportInvalidateOptionsMenu()
                showFabButton()
            }

            DrawerItem.SEARCH -> {
                showHideBottomNavigationView(true)
                setBottomNavigationMenuItemChecked(NO_BNV)
                drawerItem = DrawerItem.SEARCH
                if (getSearchFragment() == null) {
                    searchFragment = SearchFragment.newInstance()
                }
                searchFragment?.let { replaceFragment(it, FragmentTag.SEARCH.tag) }
                showFabButton()
            }

            DrawerItem.TRANSFERS -> {
                showHideBottomNavigationView(true)
                supportActionBar?.subtitle = null
                selectDrawerItemTransfers()
                supportInvalidateOptionsMenu()
                showFabButton()
            }

            DrawerItem.CHAT -> {
                Timber.d("Chat selected")
                selectDrawerItemChat(chatId)
                supportInvalidateOptionsMenu()
                showHideBottomNavigationView(false)
                if (!comesFromNotifications) {
                    bottomNavigationCurrentItem = CHAT_BNV
                }
                setBottomNavigationMenuItemChecked(CHAT_BNV)
                hideFabButton()
            }

            else -> {}
        }
        setTabsVisibility()
        checkScrollElevation()
        viewModel.checkToShow2FADialog(newAccount, firstLogin)
    }

    private fun onSelectSharedItemsDrawerItem() {
        Analytics.tracker.trackEvent(SharedItemsScreenEvent)
        lifecycleScope.launch {
            if (isSharesTabComposeEnabled()) {

                showFabButton()
                showHideBottomNavigationView(false)
                if (!comesFromNotifications) {
                    bottomNavigationCurrentItem = SHARED_ITEMS_BNV
                }
                setBottomNavigationMenuItemChecked(SHARED_ITEMS_BNV)
            } else {
                selectDrawerItemSharedItems()
                if (openFolderRefresh) {
                    onNodesSharedUpdate()
                    openFolderRefresh = false
                }
                supportInvalidateOptionsMenu()
                showFabButton()
                showHideBottomNavigationView(false)
                if (!comesFromNotifications) {
                    bottomNavigationCurrentItem = SHARED_ITEMS_BNV
                }
                setBottomNavigationMenuItemChecked(SHARED_ITEMS_BNV)
            }
        }
    }

    private fun navigateToSettingsActivity(targetPreference: TargetPreference?) {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        val settingsIntent = SettingsActivity.getIntent(this, targetPreference)
        startActivity(settingsIntent)
    }

    fun openFullscreenOfflineFragment(path: String?) {
        drawerItem = DrawerItem.HOMEPAGE
        path?.let {
            navController?.navigate(
                if (enableOfflineCompose) {
                    HomepageFragmentDirections.actionHomepageFragmentToOfflineFragmentCompose(
                        it, false
                    )
                } else {
                    HomepageFragmentDirections.actionHomepageToFullscreenOffline(it, false)
                },
                NavOptions.Builder().setLaunchSingleTop(true).build()
            )
        }
    }

    fun fullscreenOfflineFragmentOpened(fragment: OfflineFragment?) {
        fullscreenOfflineFragment = fragment
        showFabButton()
        setBottomNavigationMenuItemChecked(HOME_BNV)
        appBarLayout.visibility = View.VISIBLE
        setToolbarTitle()
        supportInvalidateOptionsMenu()
    }

    fun fullscreenOfflineFragmentClosed(fragment: OfflineFragment) {
        if (fragment === fullscreenOfflineFragment) {
            fullscreenOfflineFragment = null
            if (bottomItemBeforeOpenFullscreenOffline != Constants.INVALID_VALUE && !mStopped) {
                backToDrawerItem(bottomItemBeforeOpenFullscreenOffline)
                bottomItemBeforeOpenFullscreenOffline = Constants.INVALID_VALUE
            }
            pathNavigationOffline = "/"
            // workaround for flicker of AppBarLayout: if we go back to homepage from fullscreen
            // offline, and hide AppBarLayout when immediately on go back, we will see the flicker
            // of AppBarLayout, hide AppBarLayout when fullscreen offline is closed is better.
            if (isInMainHomePage) {
                appBarLayout.visibility = View.GONE
            }
        }
    }

    fun fullscreenOfflineFragmentComposeOpened(fragment: OfflineFragmentCompose?) {
        fullscreenOfflineFragmentCompose = fragment
        showFabButton()
        setBottomNavigationMenuItemChecked(HOME_BNV)
        appBarLayout.visibility = View.VISIBLE
        setToolbarTitle()
        supportInvalidateOptionsMenu()
    }

    fun fullscreenOfflineFragmentComposeClosed(fragment: OfflineFragmentCompose) {
        if (fragment === fullscreenOfflineFragmentCompose) {
            fullscreenOfflineFragmentCompose = null
            if (bottomItemBeforeOpenFullscreenOffline != Constants.INVALID_VALUE && !mStopped) {
                backToDrawerItem(bottomItemBeforeOpenFullscreenOffline)
                bottomItemBeforeOpenFullscreenOffline = Constants.INVALID_VALUE
            }
            pathNavigationOffline = "/"
            // workaround for flicker of AppBarLayout: if we go back to homepage from fullscreen
            // offline, and hide AppBarLayout when immediately on go back, we will see the flicker
            // of AppBarLayout, hide AppBarLayout when fullscreen offline is closed is better.
            if (isInMainHomePage) {
                appBarLayout.visibility = View.GONE
            }
        }
    }

    fun pagerOfflineFragmentComposeOpened(fragment: OfflineFragmentCompose?) {
        pagerOfflineFragmentCompose = fragment
    }

    fun pagerOfflineFragmentComposeClosed(fragment: OfflineFragmentCompose) {
        if (fragment === pagerOfflineFragmentCompose) {
            pagerOfflineFragment = null
        }
    }

    fun pagerOfflineFragmentOpened(fragment: OfflineFragment?) {
        pagerOfflineFragment = fragment
    }

    fun pagerOfflineFragmentClosed(fragment: OfflineFragment) {
        if (fragment === pagerOfflineFragment) {
            pagerOfflineFragment = null
        }
    }

    private fun showBNVImmediate() {
        updateMiniAudioPlayerVisibility(true)
        bottomNavigationView.translationY = 0f
        bottomNavigationView.animate()?.cancel()
        bottomNavigationView.clearAnimation()
        if (bottomNavigationView.visibility != View.VISIBLE) {
            bottomNavigationView.visibility = View.VISIBLE
        }
        bottomNavigationView.visibility = View.VISIBLE
        val params = CoordinatorLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        params.setMargins(
            0, 0, 0,
            resources.getDimensionPixelSize(R.dimen.bottom_navigation_view_height)
        )
        fragmentLayout.layoutParams = params
    }

    /**
     * Update whether we should display the mini audio player. It should only
     * be visible when BNV is visible.
     *
     * @param shouldVisible whether we should display the mini audio player
     * @return is the mini player visible after this update
     */
    private fun updateMiniAudioPlayerVisibility(shouldVisible: Boolean): Boolean {
        if (miniAudioPlayerController != null) {
            miniAudioPlayerController?.shouldVisible = shouldVisible
            handler.post { updateHomepageFabPosition() }
            return miniAudioPlayerController?.visible() ?: false
        }
        return false
    }

    /**
     * Update homepage FAB position, considering the visibility of PSA layout and mini audio player.
     */
    private fun updateHomepageFabPosition() {
        val fragment =
            getFragmentByType(HomepageFragment::class.java)
        if (isInMainHomePage && fragment != null) {
            fragment.updateFabPosition(
                psaViewHolder?.psaLayoutHeight().takeIf { psaViewHolder?.visible() == true } ?: 0,
                miniAudioPlayerController?.playerHeight()
                    .takeIf { miniAudioPlayerController?.visible() == true } ?: 0
            )
        }
    }

    private val isCloudAdded: Boolean
        get() {
            fileBrowserComposeFragment =
                supportFragmentManager.findFragmentByTag(FragmentTag.CLOUD_DRIVE_COMPOSE.tag) as? FileBrowserComposeFragment
            return fileBrowserComposeFragment != null && fileBrowserComposeFragment?.isAdded == true
        }
    private val isIncomingAdded: Boolean
        get() {
            incomingSharesFragment =
                sharesPageAdapter.getFragment(SharesTab.INCOMING_TAB.position) as? MegaNodeBaseFragment
            return incomingSharesFragment != null && incomingSharesFragment?.isAdded == true
        }
    private val isOutgoingAdded: Boolean
        get() {
            outgoingSharesFragment =
                sharesPageAdapter.getFragment(SharesTab.OUTGOING_TAB.position) as? MegaNodeBaseFragment
            return outgoingSharesFragment != null && outgoingSharesFragment?.isAdded == true
        }
    private val isLinksAdded: Boolean
        get() {
            linksFragment =
                sharesPageAdapter.getFragment(SharesTab.LINKS_TAB.position) as? MegaNodeBaseFragment
            return linksFragment != null && linksFragment?.isAdded == true
        }
    private val transferPageFragment: TransferPageFragment?
        get() = supportFragmentManager.findFragmentByTag(FragmentTag.TRANSFERS_PAGE.tag) as? TransferPageFragment

    override val isOnFileManagementManagerSection: Boolean
        get() = drawerItem !== DrawerItem.TRANSFERS
                && drawerItem !== DrawerItem.NOTIFICATIONS
                && drawerItem !== DrawerItem.CHAT
                && drawerItem !== DrawerItem.RUBBISH_BIN
                && drawerItem !== DrawerItem.PHOTOS
                && !isInImagesPage

    fun checkScrollElevation() {
        if (drawerItem == null) {
            return
        }
        when (drawerItem) {
            DrawerItem.HOMEPAGE -> {
                if (enableOfflineCompose) {
                    fullscreenOfflineFragmentCompose?.checkScroll()
                } else {
                    fullscreenOfflineFragment?.checkScroll()
                }
            }

            DrawerItem.BACKUPS -> {
                backupsFragment =
                    supportFragmentManager.findFragmentByTag(FragmentTag.BACKUPS.tag) as? BackupsFragment
                if (backupsFragment != null) {
                    backupsFragment?.checkScroll()
                }
            }

            DrawerItem.SHARED_ITEMS -> {
                checkScrollOnSharedItemsDrawerItem()
            }

            DrawerItem.SEARCH -> {
                if (getSearchFragment() != null) {
                    searchFragment?.checkScroll()
                }
            }

            DrawerItem.CHAT -> {
                chatTabsFragment = chatsFragment
            }

            DrawerItem.TRANSFERS -> {
                transferPageFragment?.updateElevation()
            }

            else -> {}
        }
    }

    private fun checkScrollOnSharedItemsDrawerItem() {
        when {
            tabItemShares === SharesTab.INCOMING_TAB && isIncomingAdded -> incomingSharesFragment?.checkScroll()
            tabItemShares === SharesTab.OUTGOING_TAB && isOutgoingAdded -> outgoingSharesFragment?.checkScroll()
            tabItemShares === SharesTab.LINKS_TAB && isLinksAdded -> linksFragment?.checkScroll()
        }
    }

    private fun showEnable2FADialog() {
        Timber.d("newAccount: %s", newAccount)
        newAccount = false
        if (supportFragmentManager.findFragmentByTag(Enable2FADialogFragment.TAG) != null) return
        Enable2FADialogFragment().show(supportFragmentManager, Enable2FADialogFragment.TAG)
    }

    /**
     * Opens the settings section.
     */
    private fun moveToSettingsSection() {
        navigateToSettingsActivity(null)
    }

    /**
     * Opens the settings section and scrolls to storage category.
     */
    fun moveToSettingsSectionStorage() {
        navigateToSettingsActivity(TargetPreference.Storage)
    }

    /**
     * Opens the settings section and scrolls to start screen setting.
     */
    fun moveToSettingsSectionStartScreen() {
        navigateToSettingsActivity(TargetPreference.StartScreen)
    }

    override fun moveToChatSection(chatId: Long) {
        if (chatId != -1L) {
            val chatIntent = Intent(this, ChatActivity::class.java)
            chatIntent.action = Constants.ACTION_CHAT_SHOW_MESSAGES
            chatIntent.putExtra(Constants.CHAT_ID, chatId)
            this.startActivity(chatIntent)
        }
        drawerItem = DrawerItem.CHAT
        selectDrawerItem(drawerItem, chatId)
    }

    /**
     * Launches an intent to open NotificationsPermissionActivity in order to check if should ask
     * for notifications permission.
     */
    private fun askForNotificationsPermission() {
        requestNotificationsPermissionFirstLogin = false
        PermissionUtils.checkNotificationsPermission(this)
    }

    /**
     * Sets requestNotificationsPermissionFirstLogin as firstLogin only if savedInstanceState
     * is null.
     *
     * @param savedInstanceState Saved state.
     */
    private fun setRequestNotificationsPermissionFirstLogin(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            requestNotificationsPermissionFirstLogin = viewModel.state().isFirstLogin
        }
    }

    /**
     * Launches a MyAccountActivity intent without any intent action, data and extra.
     */
    fun showMyAccount() {
        showMyAccount(null, null, null)
    }

    /**
     * Launches a MyAccountActivity intent without any intent action and data.
     *
     * @param extra Pair<String></String>, Integer> The intent extra. First is the extra key, second the value.
     */
    private fun showMyAccount(extra: android.util.Pair<String, Int>) {
        showMyAccount(null, null, extra)
    }

    /**
     * Launches a MyAccountActivity intent without any extra.
     *
     * @param action The intent action.
     * @param data   The intent data.
     */
    private fun showMyAccount(
        action: String?,
        data: Uri?,
        extra: android.util.Pair<String, Int>? = null,
    ) {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        val accountIntent = Intent(this, MyAccountActivity::class.java)
            .setAction(action)
            .setData(data)
        if (extra != null) {
            accountIntent.putExtra(extra.first, extra.second)
        }
        startActivity(accountIntent)
    }

    private fun closeSearchSection() {
        searchViewModel.resetSearchQuery()
        drawerItem = searchViewModel.state.value.searchDrawerItem
        selectDrawerItem(drawerItem)
        searchViewModel.resetSearchDrawerItem()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Timber.d("onCreateOptionsMenu")
        // Force update the toolbar title to make the the tile length to be updated
        setToolbarTitle()
        // Inflate the menu items for use in the action bar
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.activity_manager, menu)
        searchMenuItem = menu.findItem(R.id.action_search)
        searchView = searchMenuItem?.actionView as? SearchView
        val searchAutoComplete =
            searchView?.findViewById<SearchView.SearchAutoComplete>(androidx.appcompat.R.id.search_src_text)
        searchAutoComplete?.hint = getString(R.string.hint_action_search)
        val v = searchView?.findViewById<View>(androidx.appcompat.R.id.search_plate)
        v?.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
        if (searchView != null) {
            searchView?.setIconifiedByDefault(true)
        }
        searchMenuItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                Timber.d("onMenuItemActionExpand")
                searchExpand = true
                if (drawerItem === DrawerItem.HOMEPAGE) {
                    if (homepageScreen === HomepageScreen.FULLSCREEN_OFFLINE) {
                        setFullscreenOfflineFragmentSearchQuery(searchViewModel.state.value.searchQuery)
                    } else if (mHomepageSearchable != null) {
                        mHomepageSearchable?.searchReady()
                    } else {
                        openSearchOnHomepage()
                    }
                } else if (drawerItem !== DrawerItem.CHAT) {
                    viewModel.setIsFirstNavigationLevel(true)
                    searchViewModel.setSearchParentHandle(-1L)
                    searchViewModel.resetSearchDepth()
                    setSearchDrawerItem()
                    selectDrawerItem(drawerItem)
                } else {
                    Util.resetActionBar(supportActionBar)
                }
                CallUtil.hideCallMenuItem(chronometerMenuItem, returnCallMenuItem)
                CallUtil.hideCallWidget(
                    this@ManagerActivity,
                    callInProgressChrono,
                    callInProgressLayout
                )
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                Timber.d("onMenuItemActionCollapse()")
                searchExpand = false
                setCallWidget()
                CallUtil.setCallMenuItem(
                    returnCallMenuItem,
                    layoutCallMenuItem,
                    chronometerMenuItem
                )
                if (drawerItem === DrawerItem.HOMEPAGE) {
                    if (homepageScreen === HomepageScreen.FULLSCREEN_OFFLINE) {
                        if (!searchViewModel.state.value.textSubmitted) {
                            setFullscreenOfflineFragmentSearchQuery(null)
                            searchViewModel.setTextSubmitted(true)
                        }
                        supportInvalidateOptionsMenu()
                    } else if (mHomepageSearchable != null) {
                        mHomepageSearchable?.exitSearch()
                        searchViewModel.resetSearchQuery()
                        supportInvalidateOptionsMenu()
                    }
                } else {
                    cancelSearch()
                    searchViewModel.setTextSubmitted(true)
                    closeSearchSection()
                }
                return true
            }
        })
        searchView?.maxWidth = Int.MAX_VALUE
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if (drawerItem === DrawerItem.CHAT) {
                    Util.hideKeyboard(this@ManagerActivity, 0)
                } else if (drawerItem === DrawerItem.HOMEPAGE) {
                    if (homepageScreen === HomepageScreen.FULLSCREEN_OFFLINE) {
                        searchExpand = false
                        searchViewModel.setTextSubmitted(true)
                        Util.hideKeyboard(this@ManagerActivity, 0)
                        if (fullscreenOfflineFragment != null) {
                            fullscreenOfflineFragment?.onSearchQuerySubmitted()
                        }
                        setToolbarTitle()
                        supportInvalidateOptionsMenu()
                    } else {
                        Util.hideKeyboard(this@ManagerActivity)
                    }
                } else {
                    searchExpand = false
                    searchViewModel.setSearchQuery("" + query)
                    setToolbarTitle()
                    Timber.d("Search query: %s", query)
                    searchViewModel.setTextSubmitted(true)
                    supportInvalidateOptionsMenu()
                }
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                Timber.d("onQueryTextChange")
                if (drawerItem === DrawerItem.HOMEPAGE) {
                    if (homepageScreen === HomepageScreen.FULLSCREEN_OFFLINE) {
                        if (searchViewModel.state.value.textSubmitted) {
                            searchViewModel.setTextSubmitted(false)
                            return true
                        }
                        searchViewModel.setSearchQuery(newText)
                        setFullscreenOfflineFragmentSearchQuery(searchViewModel.state.value.searchQuery)
                    } else if (mHomepageSearchable != null) {
                        searchViewModel.setSearchQuery(newText)
                        searchViewModel.state.value.searchQuery?.let {
                            mHomepageSearchable?.searchQuery(
                                it
                            )
                        }
                    }
                } else {
                    if (searchViewModel.state.value.textSubmitted) {
                        searchViewModel.setTextSubmitted(false)
                    } else {
                        searchViewModel.setSearchQuery(newText)
                        searchViewModel.performSearch(
                            fileBrowserViewModel.state().fileBrowserHandle,
                            rubbishBinViewModel.state().rubbishBinHandle,
                            backupsViewModel.state().backupsHandle,
                            incomingSharesViewModel.state().incomingHandle,
                            outgoingSharesViewModel.state().outgoingHandle,
                            legacyLinksViewModel.state().linksHandle,
                            viewModel.state().isFirstNavigationLevel
                        )
                    }
                }
                return true
            }
        })
        val enableSelectMenuItem = menu.findItem(R.id.action_enable_select)
        doNotDisturbMenuItem = menu.findItem(R.id.action_menu_do_not_disturb)
        archivedMenuItem = menu.findItem(R.id.action_menu_archived)
        clearRubbishBinMenuItem = menu.findItem(R.id.action_menu_clear_rubbish_bin)
        returnCallMenuItem = menu.findItem(R.id.action_return_call)
        val rootView = returnCallMenuItem?.actionView as? RelativeLayout
        layoutCallMenuItem = rootView?.findViewById(R.id.layout_menu_call)
        chronometerMenuItem = rootView?.findViewById(R.id.chrono_menu)
        chronometerMenuItem?.visibility = View.GONE
        val moreMenuItem = menu.findItem(R.id.action_more)
        openLinkMenuItem = menu.findItem(R.id.action_open_link)
        returnCallMenuItem?.let { menuItem ->
            rootView?.setOnClickListener {
                onOptionsItemSelected(
                    menuItem
                )
            }
        }
        if (drawerItem == null) {
            drawerItem = getStartDrawerItem()
        }
        if (drawerItem === DrawerItem.CLOUD_DRIVE) {
            setBottomNavigationMenuItemChecked(CLOUD_DRIVE_BNV)
        }
        CallUtil.setCallMenuItem(returnCallMenuItem, layoutCallMenuItem, chronometerMenuItem)
        if (viewModel.isConnected) {
            when (drawerItem) {
                DrawerItem.CLOUD_DRIVE -> {
                    openLinkMenuItem?.isVisible = isFirstNavigationLevel
                    moreMenuItem.isVisible = !isFirstNavigationLevel
                    if (!isInMDMode && isCloudAdded && fileBrowserViewModel.state().nodesList.isNotEmpty()) {
                        searchMenuItem?.isVisible = true
                    }
                }

                DrawerItem.HOMEPAGE -> if (homepageScreen === HomepageScreen.FULLSCREEN_OFFLINE) {
                    updateFullscreenOfflineFragmentOptionMenu(true)
                }

                DrawerItem.RUBBISH_BIN -> {
                    moreMenuItem.isVisible = !isFirstNavigationLevel
                    if (rubbishBinViewModel.state().nodeList.isNotEmpty()) {
                        clearRubbishBinMenuItem?.isVisible = isFirstNavigationLevel
                        searchMenuItem?.isVisible = true
                    }
                }

                DrawerItem.PHOTOS -> {}
                DrawerItem.BACKUPS -> {
                    moreMenuItem.isVisible = false
                    if ((getBackupsFragment()?.getNodeCount() ?: 0) > 0) {
                        searchMenuItem?.isVisible = true
                    }
                }

                DrawerItem.SHARED_ITEMS -> {
                    moreMenuItem.isVisible = !isFirstNavigationLevel
                    if (tabItemShares === SharesTab.INCOMING_TAB && isIncomingAdded) {
                        if (isIncomingAdded &&
                            ((incomingSharesFragment?.itemCount ?: 0) > 0)
                        ) {
                            searchMenuItem?.isVisible = true
                        }
                    } else if (tabItemShares === SharesTab.OUTGOING_TAB && isOutgoingAdded) {
                        if (isOutgoingAdded &&
                            (outgoingSharesFragment?.itemCount ?: 0) > 0
                        ) {
                            searchMenuItem?.isVisible = true
                        }
                    } else if (tabItemShares === SharesTab.LINKS_TAB && isLinksAdded) {
                        if (isLinksAdded && ((linksFragment?.itemCount ?: 0) > 0)) {
                            searchMenuItem?.isVisible = true
                        }
                    }
                }

                DrawerItem.SEARCH -> if (searchExpand) {
                    openSearchView()
                    searchFragment?.checkSelectMode()
                } else {
                    moreMenuItem.isVisible = !isFirstNavigationLevel
                }

                DrawerItem.TRANSFERS -> if (transferPageViewModel.transferTab == TransfersTab.PENDING_TAB
                    && transfersViewModel.getActiveTransfers().isNotEmpty()
                ) {
                    enableSelectMenuItem.isVisible = true
                }

                DrawerItem.CHAT -> if (searchExpand) {
                    openSearchView()
                } else {
                    checkArchivedChats()
                    doNotDisturbMenuItem?.isVisible = true
                    openLinkMenuItem?.isVisible = true
                }

                DrawerItem.NOTIFICATIONS -> {}
                else -> {}
            }
        }
        if (drawerItem === DrawerItem.HOMEPAGE) {
            // Get the Searchable again at onCreateOptionsMenu() after screen rotation
            mHomepageSearchable = findHomepageSearchable()
            if (searchExpand) {
                openSearchView()
            } else {
                if (mHomepageSearchable != null) {
                    searchMenuItem?.isVisible = mHomepageSearchable?.shouldShowSearchMenu() == true
                }
            }
        }
        Timber.d("Call to super onCreateOptionsMenu")
        return super.onCreateOptionsMenu(menu)
    }

    private fun openSearchOnHomepage() {
        viewModel.setIsFirstNavigationLevel(true)
        searchViewModel.setSearchParentHandle(-1L)
        searchViewModel.resetSearchDepth()
        setSearchDrawerItem()
        selectDrawerItem(drawerItem)
        Util.resetActionBar(supportActionBar)
    }

    private fun setFullscreenOfflineFragmentSearchQuery(searchQuery: String?) {
        if (fullscreenOfflineFragment != null) {
            fullscreenOfflineFragment?.setSearchQuery(searchQuery)
        }
    }

    fun updateFullscreenOfflineFragmentOptionMenu(openSearchView: Boolean) {
        if (fullscreenOfflineFragment == null) {
            return
        }
        if (searchExpand && openSearchView) {
            openSearchView()
        } else if (!searchExpand) {
            if (viewModel.isConnected) {
                if (((fullscreenOfflineFragment?.getItemCount() ?: 0) > 0) &&
                    (fullscreenOfflineFragment?.searchMode() == false) &&
                    (searchMenuItem != null)
                ) {
                    searchMenuItem?.isVisible = true
                }
            } else {
                supportInvalidateOptionsMenu()
            }
            fullscreenOfflineFragment?.refreshActionBarTitle()
        }
    }

    private fun findHomepageSearchable(): HomepageSearchable? {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        if (navHostFragment != null) {
            for (fragment in navHostFragment.childFragmentManager.fragments) {
                if (fragment is HomepageSearchable) {
                    return fragment
                }
            }
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    fun <F : Fragment?> getFragmentByType(fragmentClass: Class<F>): F? {
        val navHostFragment: Fragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                ?: return null
        for (fragment in navHostFragment.childFragmentManager.fragments) {
            if (fragment.javaClass == fragmentClass) {
                return fragment as? F
            }
        }
        return null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("onOptionsItemSelected")
        typesCameraPermission = Constants.INVALID_TYPE_PERMISSIONS
        Timber.d("retryPendingConnections")
        megaApi.retryPendingConnections()
        megaChatApi.retryPendingConnections(false, null)
        return when (item.itemId) {
            android.R.id.home -> {
                if (isFirstNavigationLevel && drawerItem !== DrawerItem.SEARCH) {
                    if (drawerItem === DrawerItem.SYNC || drawerItem === DrawerItem.DEVICE_CENTER || drawerItem === DrawerItem.RUBBISH_BIN || drawerItem === DrawerItem.BACKUPS || drawerItem === DrawerItem.NOTIFICATIONS || drawerItem === DrawerItem.TRANSFERS) {
                        backToDrawerItem(bottomNavigationCurrentItem)
                        if (transfersToImageViewer) {
                            switchImageViewerToFront()
                        }
                    } else {
                        drawerLayout.openDrawer(navigationView)
                    }
                } else {
                    Timber.d("NOT firstNavigationLevel")
                    if (drawerItem === DrawerItem.CLOUD_DRIVE) {
                        //Check media discovery mode
                        if (isInMDMode) {
                            onBackPressedDispatcher.onBackPressed()
                        } else {
                            //Cloud Drive
                            if (isCloudAdded) {
                                fileBrowserComposeFragment?.onBackPressed()
                            }
                        }
                    } else if (drawerItem === DrawerItem.SYNC || drawerItem === DrawerItem.DEVICE_CENTER) {
                        onBackPressedDispatcher.onBackPressed()
                    } else if (drawerItem === DrawerItem.RUBBISH_BIN) {
                        rubbishBinComposeFragment = getRubbishBinComposeFragment()
                        rubbishBinComposeFragment?.onBackPressed()
                    } else if (drawerItem === DrawerItem.SHARED_ITEMS) {
                        if (tabItemShares === SharesTab.INCOMING_TAB && isIncomingAdded) {
                            incomingSharesFragment?.onBackPressed()
                        } else if (tabItemShares === SharesTab.OUTGOING_TAB && isOutgoingAdded) {
                            outgoingSharesFragment?.onBackPressed()
                        } else if (tabItemShares === SharesTab.LINKS_TAB && isLinksAdded) {
                            linksFragment?.onBackPressed()
                        }
                    } else if (drawerItem === DrawerItem.PHOTOS) {
                        if (getPhotosFragment() != null) {
                            if (canPhotosEnableCUViewBack()) {
                                photosFragment?.onBackPressed()
                            }
                            setToolbarTitle()
                            invalidateOptionsMenu()
                            return true
                        } else if (isInAlbumContent || isInFilterPage) {
                            // When current fragment is AlbumContentFragment, the photosFragment will be null due to replaceFragment.
                            onBackPressedDispatcher.onBackPressed()
                        }
                    } else if (drawerItem === DrawerItem.BACKUPS) {
                        backupsFragment =
                            supportFragmentManager.findFragmentByTag(FragmentTag.BACKUPS.tag) as? BackupsFragment
                        if (backupsFragment != null) {
                            backupsFragment?.onBackPressed()
                            return true
                        }
                    } else if (drawerItem === DrawerItem.SEARCH) {
                        if (getSearchFragment() != null) {
                            onBackPressedDispatcher.onBackPressed()
                            return true
                        }
                    } else if (drawerItem === DrawerItem.TRANSFERS) {
                        drawerItem = getStartDrawerItem()
                        selectDrawerItem(drawerItem)
                        return true
                    } else if (drawerItem === DrawerItem.HOMEPAGE) {
                        if (homepageScreen === HomepageScreen.FULLSCREEN_OFFLINE) {
                            handleBackPressIfFullscreenOfflineFragmentOpened()
                        } else if (navController?.currentDestination != null &&
                            navController?.currentDestination?.id == R.id.favouritesFolderFragment
                        ) {
                            onBackPressedDispatcher.onBackPressed()
                        } else {
                            navController?.navigateUp()
                        }
                    } else {
                        handleSuperBackPressed()
                    }
                }
                true
            }

            R.id.action_search -> {
                if (drawerItem == DrawerItem.CLOUD_DRIVE) {
                    Analytics.tracker.trackEvent(CloudDriveSearchMenuToolbarEvent)
                }
                Timber.d("Action search selected")
                hideItemsWhenSearchSelected()
                true
            }

            R.id.action_open_link -> {
                showOpenLinkDialog()
                true
            }

            R.id.action_menu_do_not_disturb -> {
                if (drawerItem === DrawerItem.CHAT) {
                    if (ChatUtil.getGeneralNotification() == Constants.NOTIFICATIONS_ENABLED) {
                        ChatUtil.createMuteNotificationsChatAlertDialog(this, null)
                    } else {
                        showSnackbar(
                            Constants.MUTE_NOTIFICATIONS_SNACKBAR_TYPE,
                            null,
                            -1
                        )
                    }
                }
                true
            }

            R.id.action_menu_archived -> {
                startActivity(Intent(this, ArchivedChatsActivity::class.java))
                true
            }

            R.id.action_select -> {
                when (drawerItem) {
                    DrawerItem.CLOUD_DRIVE -> if (isCloudAdded) {
                        fileBrowserViewModel.selectAllNodes()
                    }

                    DrawerItem.RUBBISH_BIN -> {
                        if (getRubbishBinComposeFragment() != null) {
                            rubbishBinViewModel.selectAllNodes()
                        }
                    }

                    DrawerItem.SHARED_ITEMS -> onSelectAllSharedItems()

                    DrawerItem.HOMEPAGE -> if (fullscreenOfflineFragment != null) {
                        fullscreenOfflineFragment?.selectAll()
                    }

                    DrawerItem.BACKUPS -> if (getBackupsFragment() != null) {
                        backupsFragment?.selectAll()
                    }

                    DrawerItem.SEARCH -> if (getSearchFragment() != null) {
                        searchFragment?.selectAll()
                    }

                    else -> {}
                }
                true
            }

            R.id.action_menu_clear_rubbish_bin -> {
                ClearRubbishBinDialogFragment().show(
                    supportFragmentManager,
                    ClearRubbishBinDialogFragment.TAG
                )
                true
            }

            R.id.action_scan_qr -> {
                Timber.d("Action menu scan QR code pressed")
                //Check if there is a in progress call:
                checkBeforeOpeningQR(true)
                true
            }

            R.id.action_return_call -> {
                Timber.d("Action menu return to call in progress pressed")
                returnCall()
                true
            }

            R.id.action_enable_select -> {
                transferPageFragment?.activateActionMode()
                true
            }

            R.id.action_more -> {
                showNodeOptionsPanel(
                    getCurrentParentNode(
                        currentParentHandle,
                        Constants.INVALID_VALUE
                    )
                )
                true
            }

            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun onSelectAllSharedItems() {
        lifecycleScope.launch {
            if (isSharesTabComposeEnabled()) {

            } else {
                when (tabItemShares) {
                    SharesTab.INCOMING_TAB -> if (isIncomingAdded) {
                        incomingSharesFragment?.selectAll()
                    }

                    SharesTab.OUTGOING_TAB -> if (isOutgoingAdded) {
                        outgoingSharesFragment?.selectAll()
                    }

                    SharesTab.LINKS_TAB -> if (isLinksAdded) {
                        linksFragment?.selectAll()
                    }

                    else -> {}
                }
            }
        }
    }

    private fun hideItemsWhenSearchSelected() {
        searchViewModel.setTextSubmitted(false)
        if (searchMenuItem != null) {
            doNotDisturbMenuItem?.isVisible = false
            archivedMenuItem?.isVisible = false
            clearRubbishBinMenuItem?.isVisible = false
            searchMenuItem?.isVisible = false
            openLinkMenuItem?.isVisible = false
        }
    }

    /**
     * Method to return to an ongoing call
     */
    fun returnCall() {
        CallUtil.returnActiveCall(this, passcodeManagement)
    }

    private fun checkBeforeOpeningQR(openScanQR: Boolean) {
        if (CallUtil.isNecessaryDisableLocalCamera() != MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
            CallUtil.showConfirmationOpenCamera(this, Constants.ACTION_OPEN_QR, openScanQR)
            return
        }
        openQR(openScanQR)
    }

    fun openQR(openScanQr: Boolean) {
        if (openScanQr) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ScanCodeFragment()).commitNowAllowingStateLoss()
        }
        val qrIntent = Intent(this, QRCodeActivity::class.java)
        qrIntent.putExtra(Constants.OPEN_SCAN_QR, openScanQr)
        startActivity(qrIntent)
    }

    private fun refreshAfterMovingToRubbish() {
        Timber.d("refreshAfterMovingToRubbish")
        if (drawerItem === DrawerItem.CLOUD_DRIVE) {
            refreshCloudDrive()
        } else if (drawerItem === DrawerItem.BACKUPS) {
            onNodesBackupsUpdate()
        } else if (drawerItem === DrawerItem.SHARED_ITEMS) {
            onNodesSharedUpdate()
        } else if (drawerItem === DrawerItem.SEARCH) {
            refreshSearch()
        } else if (drawerItem === DrawerItem.HOMEPAGE) {
            LiveEventBus.get<Boolean>(Constants.EVENT_NODES_CHANGE).post(false)
        }
        refreshRubbishBin()
        setToolbarTitle()
    }

    private fun refreshRubbishBin() {
        rubbishBinViewModel.refreshNodes()
    }

    /**
     * Refreshes the contents of [BackupsFragment] once a sorting method has been selected
     */
    private fun refreshBackupsFragment() {
        if (backupsFragment != null) {
            backupsViewModel.refreshBackupsNodes()
        }
    }

    private fun refreshSearch() {
        if (getSearchFragment() != null) {
            searchFragment?.hideMultipleSelect()
            searchFragment?.refresh()
        }
    }

    private fun goBack() {
        Timber.d("goBack")
        retryConnectionsAndSignalPresence()
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(GravityCompat.START)
            return
        }
        dismissAlertDialogIfExists(statusDialog)
        Timber.d("DRAWERITEM: %s", drawerItem)
        if (turnOnNotifications) {
            deleteTurnOnNotificationsFragment()
            return
        }
        if (onAskingPermissionsFragment) {
            return
        }
        if (navController?.currentDestination != null &&
            navController?.currentDestination?.id == R.id.favouritesFolderFragment
        ) {
            handleSuperBackPressed()
            return
        }
        if (drawerItem === DrawerItem.CLOUD_DRIVE) {
            if (isInMDMode) {
                lifecycleScope.launch {
                    isInMDMode = false
                    fileBrowserViewModel.handleBackFromMD()
                    backToDrawerItem(bottomNavigationCurrentItem)
                }
            } else {
                if (!isCloudAdded || fileBrowserComposeFragment?.onBackPressed() == 0) {
                    performOnBack()
                }
            }
        } else if (drawerItem === DrawerItem.SYNC || drawerItem === DrawerItem.DEVICE_CENTER) {
            backToDrawerItem(bottomNavigationCurrentItem)
        } else if (drawerItem === DrawerItem.RUBBISH_BIN) {
            rubbishBinComposeFragment = getRubbishBinComposeFragment()
            if (rubbishBinComposeFragment == null || rubbishBinComposeFragment?.onBackPressed() == 0) {
                backToDrawerItem(bottomNavigationCurrentItem)
            }

        } else if (drawerItem === DrawerItem.TRANSFERS) {
            backToDrawerItem(bottomNavigationCurrentItem)
            if (transfersToImageViewer) {
                switchImageViewerToFront()
            }
        } else if (drawerItem === DrawerItem.BACKUPS) {
            backupsFragment = supportFragmentManager
                .findFragmentByTag(FragmentTag.BACKUPS.tag) as? BackupsFragment
            if (backupsFragment == null) {
                backToDrawerItem(bottomNavigationCurrentItem)
            } else {
                backupsFragment?.onBackPressed()
            }
        } else if (drawerItem === DrawerItem.NOTIFICATIONS) {
            backToDrawerItem(bottomNavigationCurrentItem)
        } else if (drawerItem === DrawerItem.SHARED_ITEMS) {
            onBackPressedInSharedItemsDrawerItem()
        } else if (drawerItem === DrawerItem.CHAT) {
            performOnBack()
        } else if (drawerItem === DrawerItem.PHOTOS) {
            if (isInAlbumContent) {
                fromAlbumContent = true
                isInAlbumContent = false
                backToDrawerItem(bottomNavigationCurrentItem)
            } else if (isInFilterPage) {
                isInFilterPage = false
                backToDrawerItem(bottomNavigationCurrentItem)
                if (photosFragment == null) {
                    backToDrawerItem(bottomNavigationCurrentItem)
                }
            } else if (getPhotosFragment() == null || photosFragment?.onBackPressed() == 0) {
                performOnBack()
            }
        } else if (drawerItem === DrawerItem.SEARCH) {
            if (getSearchFragment() == null || searchFragment?.onBackPressed() == 0) {
                closeSearchSection()
            }
        } else if (isInMainHomePage) {
            val fragment = getFragmentByType(
                HomepageFragment::class.java
            )
            if (fragment?.isFabExpanded == true) {
                fragment.collapseFab()
            } else {
                performOnBack()
            }
        } else {
            handleBackPressIfFullscreenOfflineFragmentOpened()
        }
    }

    private fun onBackPressedInSharedItemsDrawerItem() {
        lifecycleScope.launch {
            if (isSharesTabComposeEnabled()) {

            } else {
                when (tabItemShares) {
                    SharesTab.INCOMING_TAB -> if (!isIncomingAdded || incomingSharesFragment?.onBackPressed() == 0) {
                        performOnBack()
                    }

                    SharesTab.OUTGOING_TAB -> if (!isOutgoingAdded || outgoingSharesFragment?.onBackPressed() == 0) {
                        performOnBack()
                    }

                    SharesTab.LINKS_TAB -> if (!isLinksAdded || linksFragment?.onBackPressed() == 0) {
                        performOnBack()
                    }

                    else -> performOnBack()
                }
            }
        }
    }

    /**
     * This activity was called by [mega.privacy.android.app.imageviewer.ImageViewerActivity]
     * by putting itself to the front of the history stack. Switching back to the image viewer requires
     * the same process again (reordering and therefore putting the image viewer to the front).
     */
    private fun switchImageViewerToFront() {
        transfersToImageViewer = false
        startActivity(ImageViewerActivity.getIntentFromBackStack(this))
    }

    /**
     * Closes the app if the current DrawerItem is the same as the preferred one.
     * If not, sets the current DrawerItem as the preferred one.
     */
    private fun performOnBack() {
        val startItem: Int = getStartBottomNavigationItem()
        if (drawerItem?.let { shouldCloseApp(startItem, it) } == true) {
            handleSuperBackPressed()
        } else {
            backToDrawerItem(startItem)
        }
    }

    private fun handleBackPressIfFullscreenOfflineFragmentOpened() {
        if (fullscreenOfflineFragment == null || fullscreenOfflineFragment?.onBackPressed() == 0) {
            // workaround for flicker of AppBarLayout: if we go back to homepage from fullscreen
            // offline, and hide AppBarLayout when immediately on go back, we will see the flicker
            // of AppBarLayout, hide AppBarLayout when fullscreen offline is closed is better.
            if (bottomNavigationCurrentItem != HOME_BNV) {
                backToDrawerItem(bottomNavigationCurrentItem)
            } else {
                drawerItem = DrawerItem.HOMEPAGE
            }
            handleSuperBackPressed()
        }
    }

    fun adjustTransferWidgetPositionInHomepage() {
        if (isInMainHomePage) {
            val transfersWidgetLayout: View =
                findViewById(R.id.transfers_widget_layout)
                    ?: return
            val params = transfersWidgetLayout.layoutParams as LinearLayout.LayoutParams
            params.bottomMargin = Util.dp2px(TRANSFER_WIDGET_MARGIN_BOTTOM.toFloat(), outMetrics)
            params.gravity = Gravity.END
            transfersWidgetLayout.layoutParams = params
        }
    }

    /**
     * Update the PSA view visibility. It should only visible in root homepage tab.
     */
    private fun updatePsaViewVisibility() {
        psaViewHolder?.toggleVisible(isInMainHomePage)
        if (psaViewHolder?.visible() == true) {
            handler.post { updateHomepageFabPosition() }
        } else {
            updateHomepageFabPosition()
        }
    }

    /**
     * Exits the Backups Page
     */
    fun exitBackupsPage() {
        backToDrawerItem(bottomNavigationCurrentItem)
    }

    private fun backToDrawerItem(item: Int) {
        if (item == CLOUD_DRIVE_BNV) {
            drawerItem = DrawerItem.CLOUD_DRIVE
            if (isCloudAdded) {
                fileBrowserViewModel.changeTransferOverQuotaBannerVisibility()
            }
        } else if (item == PHOTOS_BNV) {
            drawerItem = DrawerItem.PHOTOS
        } else if (item == CHAT_BNV) {
            drawerItem = DrawerItem.CHAT
        } else if (item == SHARED_ITEMS_BNV) {
            drawerItem = DrawerItem.SHARED_ITEMS
        } else if (item == HOME_BNV || item == -1) {
            drawerItem = DrawerItem.HOMEPAGE
        }
        selectDrawerItem(drawerItem)
    }

    private val isFirstTimeCam: Unit
        get() {
            if (viewModel.state().isFirstLogin) {
                viewModel.setIsFirstLogin(false)
                bottomNavigationCurrentItem = CLOUD_DRIVE_BNV
            }
        }

    private fun checkIfShouldCloseSearchView(oldDrawerItem: DrawerItem?) {
        if (!searchExpand) return
        if (oldDrawerItem === DrawerItem.CHAT
            || (oldDrawerItem === DrawerItem.HOMEPAGE
                    && homepageScreen === HomepageScreen.FULLSCREEN_OFFLINE)
        ) {
            searchExpand = false
        }
    }

    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        Timber.d("onNavigationItemSelected")
        val nVMenu = navigationView.menu
        resetNavigationViewMenu(nVMenu)
        val oldDrawerItem = drawerItem
        when (menuItem.itemId) {
            R.id.bottom_navigation_item_cloud_drive -> {
                if (drawerItem === DrawerItem.CLOUD_DRIVE) {
                    if (isInMDMode) {
                        isInMDMode = false
                    }
                    val rootNode = megaApi.rootNode
                    if (rootNode == null) {
                        Timber.e("Root node is null")
                    }
                    if (rootNode != null && fileBrowserViewModel.state().fileBrowserHandle != INVALID_HANDLE && fileBrowserViewModel.state().fileBrowserHandle != rootNode.handle) {
                        fileBrowserViewModel.setBrowserParentHandle(rootNode.handle)
                        refreshFragment(FragmentTag.CLOUD_DRIVE.tag)
                    }
                } else {
                    drawerItem = DrawerItem.CLOUD_DRIVE
                    setBottomNavigationMenuItemChecked(CLOUD_DRIVE_BNV)
                }
            }

            R.id.bottom_navigation_item_homepage -> {
                drawerItem = DrawerItem.HOMEPAGE
                if (fullscreenOfflineFragment != null) {
                    handleSuperBackPressed()
                    return true
                } else {
                    setBottomNavigationMenuItemChecked(HOME_BNV)
                }
            }

            R.id.bottom_navigation_item_camera_uploads -> {

                // if pre fragment is the same one, do nothing.
                if (oldDrawerItem !== DrawerItem.PHOTOS) {
                    drawerItem = DrawerItem.PHOTOS
                    setBottomNavigationMenuItemChecked(PHOTOS_BNV)
                }
            }

            R.id.bottom_navigation_item_shared_items -> {
                Analytics.tracker.trackEvent(SharedItemsScreenEvent)
                if (drawerItem === DrawerItem.SHARED_ITEMS) {
                    if (tabItemShares === SharesTab.INCOMING_TAB && incomingSharesViewModel.state().incomingHandle != INVALID_HANDLE) {
                        incomingSharesViewModel.resetIncomingTreeDepth()
                        refreshIncomingShares()
                    } else if (tabItemShares === SharesTab.OUTGOING_TAB && outgoingSharesViewModel.state().outgoingHandle != INVALID_HANDLE) {
                        outgoingSharesViewModel.resetOutgoingTreeDepth()
                        refreshOutgoingShares()
                    } else if (tabItemShares === SharesTab.LINKS_TAB && legacyLinksViewModel.state().linksHandle != INVALID_HANDLE) {
                        legacyLinksViewModel.resetLinksTreeDepth()
                        refreshLinks()
                    }
                    refreshSharesPageAdapter()
                } else {
                    drawerItem = DrawerItem.SHARED_ITEMS
                    setBottomNavigationMenuItemChecked(SHARED_ITEMS_BNV)
                }
            }

            R.id.bottom_navigation_item_chat -> {
                drawerItem = DrawerItem.CHAT
                setBottomNavigationMenuItemChecked(CHAT_BNV)
            }
        }
        checkIfShouldCloseSearchView(oldDrawerItem)
        selectDrawerItem(drawerItem)
        closeDrawer()
        return true
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        showSnackbar(type, fragmentContainer, content, chatId)
    }

    /**
     * Restores a list of nodes from Rubbish Bin to their original parent.
     *
     * @param nodes List of nodes.
     */
    fun restoreFromRubbish(nodes: List<MegaNode>) {
        viewModel.checkRestoreNodesNameCollision(nodes)
    }

    /**
     * Shows the final result of a restoration or removal from Rubbish Bin section.
     *
     * @param message      Text message to show as the request result.
     */
    private fun showRestorationOrRemovalResult(message: String) {
        showSnackbar(Constants.SNACKBAR_TYPE, message, MegaChatApiJava.MEGACHAT_INVALID_HANDLE)
    }

    fun showRenameDialog(document: MegaNode?) {
        document?.let { showRenameNodeDialog(this, it, this, this) }
    }

    /**
     * Launches an intent to get the links of the nodes received.
     *
     * @param nodes List of nodes to get their links.
     */
    fun showGetLinkActivity(nodes: List<MegaNode>?) {
        if (nodes == null || nodes.isEmpty()) {
            showSnackbar(
                Constants.SNACKBAR_TYPE,
                getString(R.string.general_text_error),
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
            )
            return
        }
        if (nodes.size == 1) {
            showGetLinkActivity(nodes[0].handle)
            return
        }
        val handles = LongArray(nodes.size)
        for (i in nodes.indices) {
            val node = nodes[i]
            if (showTakenDownNodeActionNotAvailableDialog(node, this)) {
                return
            }
            handles[i] = node.handle
        }
        LinksUtil.showGetLinkActivity(this, handles)
    }

    fun showGetLinkActivity(handle: Long) {
        Timber.d("Handle: %s", handle)
        val node = megaApi.getNodeByHandle(handle)
        if (node == null) {
            showSnackbar(
                Constants.SNACKBAR_TYPE,
                getString(R.string.warning_node_not_exists_in_cloud),
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
            )
            return
        }
        if (showTakenDownNodeActionNotAvailableDialog(node, this)) {
            return
        }
        LinksUtil.showGetLinkActivity(this, handle)
        refreshAfterMovingToRubbish()
    }

    /*
     * Display keyboard
     */
    private fun showKeyboardDelayed(view: View) {
        Timber.d("showKeyboardDelayed")
        handler.postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }, 50)
    }

    fun showWarningDialogOfShare(node: MegaNode, nodeType: Int, actionType: Int) {
        Timber.d("showWarningDialogOfShareFolder")
        if (actionType == ACTION_BACKUP_SHARE_FOLDER) {
            fileBackupManager.shareBackupFolder(
                nodeController,
                node,
                nodeType,
                actionType,
                fileBackupManager.actionBackupNodeCallback
            )
        }
    }

    /**
     * Shows the final result of a movement request.
     *
     * @param result Object containing the request result.
     * @param handle Handle of the node to mode.
     */
    private fun showMovementResult(result: MoveRequestResult, handle: Long) {
        if (result.isSingleAction && result.isSuccess && currentParentHandle == handle) {
            // Return -1L if the unboxing of result.getOldParentHandle() may return a null value
            val oldParentHandle =
                result.oldParentHandle ?: -1L
            when (drawerItem) {
                DrawerItem.CLOUD_DRIVE -> {
                    fileBrowserViewModel.setBrowserParentHandle(oldParentHandle)
                    refreshCloudDrive()
                }

                DrawerItem.BACKUPS -> {
                    backupsViewModel.updateBackupsHandle(oldParentHandle)
                    refreshBackupsList()
                }

                DrawerItem.SHARED_ITEMS -> {
                    when (tabItemShares) {
                        SharesTab.INCOMING_TAB -> {
                            incomingSharesViewModel.decreaseIncomingTreeDepth(
                                if (incomingSharesViewModel.state().incomingTreeDepth == 0) INVALID_HANDLE else oldParentHandle
                            )
                            refreshIncomingShares()
                        }

                        SharesTab.OUTGOING_TAB -> {
                            outgoingSharesViewModel.decreaseOutgoingTreeDepth(
                                if (outgoingSharesViewModel.state().outgoingTreeDepth == 0) INVALID_HANDLE else oldParentHandle
                            )
                            if (outgoingSharesViewModel.state().outgoingHandle == INVALID_HANDLE) {
                                hideTabs(false, SharesTab.OUTGOING_TAB)
                            }
                            refreshOutgoingShares()
                        }

                        SharesTab.LINKS_TAB -> {
                            legacyLinksViewModel.decreaseLinksTreeDepth(
                                if (legacyLinksViewModel.state().linksTreeDepth == 0) INVALID_HANDLE else oldParentHandle
                            )
                            if (legacyLinksViewModel.state().linksHandle == INVALID_HANDLE) {
                                hideTabs(false, SharesTab.LINKS_TAB)
                            }
                            refreshLinks()
                        }

                        else -> {}
                    }
                    searchViewModel.setSearchParentHandle(if (searchViewModel.state.value.searchDepth > 0) oldParentHandle else INVALID_HANDLE)
                    searchViewModel.decreaseSearchDepth()
                    refreshSearch()
                }

                DrawerItem.SEARCH -> {
                    searchViewModel.setSearchParentHandle(if (searchViewModel.state.value.searchDepth > 0) oldParentHandle else INVALID_HANDLE)
                    searchViewModel.decreaseSearchDepth()
                    refreshSearch()
                }

                else -> {}
            }
            setToolbarTitle()
        }
    }

    /**
     * Shows an error in the Open link dialog.
     *
     * @param show  True if should show an error.
     * @param error Error value to identify and show the corresponding error.
     */
    private fun showOpenLinkError(show: Boolean, error: Int) {
        if (openLinkDialog != null) {
            if (show) {
                openLinkDialogIsErrorShown = true
                openLinkText?.let { ColorUtils.setErrorAwareInputAppearance(it, true) }
                openLinkError?.visibility = View.VISIBLE
                if (drawerItem == DrawerItem.CLOUD_DRIVE) {
                    if (openLinkText?.text.toString().isEmpty()) {
                        openLinkErrorText?.setText(R.string.invalid_file_folder_link_empty)
                        return
                    }
                    when (error) {
                        Constants.CHAT_LINK -> {
                            openLinkText?.setTextColor(
                                ColorUtils.getThemeColor(
                                    this,
                                    android.R.attr.textColorPrimary
                                )
                            )
                            openLinkErrorText?.setText(R.string.valid_chat_link)
                            openLinkOpenButton?.setText(R.string.action_open_chat_link)
                        }

                        Constants.CONTACT_LINK -> {
                            openLinkText?.setTextColor(
                                ColorUtils.getThemeColor(
                                    this,
                                    android.R.attr.textColorPrimary
                                )
                            )
                            openLinkErrorText?.setText(R.string.valid_contact_link)
                            openLinkOpenButton?.setText(R.string.action_open_contact_link)
                        }

                        Constants.ERROR_LINK -> {
                            openLinkErrorText?.setText(R.string.invalid_file_folder_link)
                        }
                    }
                } else if (drawerItem === DrawerItem.CHAT || MEETING_TYPE == MeetingActivity.MEETING_ACTION_JOIN) {
                    if (openLinkText?.text.toString().isEmpty()) {
                        openLinkErrorText?.setText(if (chatLinkDialogType == LINK_DIALOG_CHAT) R.string.invalid_chat_link_empty else R.string.invalid_meeting_link_empty)
                        return
                    }
                    openLinkErrorText?.setText(if (chatLinkDialogType == LINK_DIALOG_CHAT) R.string.invalid_chat_link_args else R.string.invalid_meeting_link_args)
                }
            } else {
                openLinkDialogIsErrorShown = false
                if (openLinkError?.visibility == View.VISIBLE) {
                    openLinkText?.let {
                        ColorUtils.setErrorAwareInputAppearance(
                            it, false
                        )
                    }
                    openLinkError?.visibility = View.GONE
                    openLinkOpenButton?.setText(R.string.context_open_link)
                }
            }
        }
    }

    /**
     * Opens a links via Open link dialog.
     *
     * @param link The link to open.
     */
    private fun openLink(link: String) {
        // Password link
        if (Util.matchRegexs(link, Constants.PASSWORD_LINK_REGEXS)) {
            dismissAlertDialogIfExists(openLinkDialog)
            val openLinkIntent = Intent(this, OpenPasswordLinkActivity::class.java)
            openLinkIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            openLinkIntent.data = Uri.parse(link)
            startActivity(openLinkIntent)
            return
        }
        if (drawerItem === DrawerItem.CLOUD_DRIVE) {
            lifecycleScope.launch {
                if (Util.matchRegexs(link, Constants.ALBUM_LINK_REGEXS)) {
                    if (getFeatureFlagValueUseCase(AppFeatures.AlbumSharing)) {
                        dismissAlertDialogIfExists(openLinkDialog)
                        val intent = AlbumScreenWrapperActivity.createAlbumImportScreen(
                            context = this@ManagerActivity,
                            albumLink = AlbumLink(link),
                        ).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(intent)
                        return@launch
                    }
                }

                val linkType = nodeController.importLink(link, viewModel.state().enabledFlags)
                if (openLinkError?.visibility == View.VISIBLE) {
                    when (linkType) {
                        Constants.CHAT_LINK -> {
                            Timber.d("Open chat link: correct chat link")
                            // Identify the link is a meeting or normal chat link
                            megaChatApi.checkChatLink(
                                link,
                                LoadPreviewListener(
                                    this@ManagerActivity,
                                    this@ManagerActivity,
                                    Constants.CHECK_LINK_TYPE_UNKNOWN_LINK
                                )
                            )
                            dismissAlertDialogIfExists(openLinkDialog)
                        }

                        Constants.CONTACT_LINK -> {
                            Timber.d("Open contact link: correct contact link")
                            val s =
                                link.split("C!".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()
                            if (s.size > 1) {
                                val handle: Long =
                                    MegaApiAndroid.base64ToHandle(s[1].trim { it <= ' ' })
                                openContactLink(handle)
                                dismissAlertDialogIfExists(openLinkDialog)
                            }
                        }
                    }
                } else {
                    when (linkType) {
                        Constants.FILE_LINK, Constants.FOLDER_LINK -> {
                            Timber.d("Do nothing: correct file or folder link")
                            dismissAlertDialogIfExists(openLinkDialog)
                        }

                        Constants.CHAT_LINK, Constants.CONTACT_LINK, Constants.ERROR_LINK -> {
                            Timber.w("Show error: invalid link or correct chat or contact link")
                            showOpenLinkError(true, linkType)
                        }
                    }
                }
            }
        } else if (drawerItem === DrawerItem.CHAT || MEETING_TYPE == MeetingActivity.MEETING_ACTION_JOIN) {
            megaChatApi.checkChatLink(
                link,
                LoadPreviewListener(
                    this@ManagerActivity,
                    this@ManagerActivity,
                    Constants.CHECK_LINK_TYPE_UNKNOWN_LINK
                )
            )
        }
    }

    /**
     * Shows an Open link dialog.
     */
    private fun showOpenLinkDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        val inflater: LayoutInflater = layoutInflater
        val v = inflater.inflate(R.layout.dialog_error_hint, null)
        builder.setView(v).setPositiveButton(R.string.context_open_link, null)
            .setNegativeButton(R.string.general_cancel, null)
        openLinkText = v.findViewById(R.id.text)
        openLinkText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                showOpenLinkError(false, 0)
            }
        })
        openLinkText?.setOnEditorActionListener { v1: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                Util.hideKeyboardView(this, v1, 0)
                openLink(openLinkText?.text.toString())
            }
            false
        }
        Util.showKeyboardDelayed(openLinkText)
        openLinkError = v.findViewById(R.id.error)
        openLinkErrorText = v.findViewById(R.id.error_text)
        val isJoinMeeting = MEETING_TYPE == MeetingActivity.MEETING_ACTION_JOIN
        if (drawerItem === DrawerItem.CLOUD_DRIVE) {
            builder.setTitle(R.string.action_open_link)
            openLinkText?.setHint(R.string.hint_paste_link)
        } else if (drawerItem === DrawerItem.CHAT || isJoinMeeting) {
            val fragment = supportFragmentManager
                .findFragmentByTag(MeetingBottomSheetDialogFragment.TAG)
            chatLinkDialogType = if (fragment != null || isJoinMeeting) {
                builder.setTitle(R.string.paste_meeting_link_guest_dialog_title)
                    .setMessage(
                        getString(
                            R.string.paste_meeting_link_guest_instruction
                        )
                    )
                openLinkText?.setHint(R.string.meeting_link)
                LINK_DIALOG_MEETING
            } else {
                builder.setTitle(R.string.action_open_chat_link)
                openLinkText?.setHint(R.string.hint_enter_chat_link)
                LINK_DIALOG_CHAT
            }
        }
        openLinkDialog = builder.create()
        openLinkDialog?.setCanceledOnTouchOutside(false)
        try {
            openLinkDialog?.show()
            openLinkText?.requestFocus()

            // Set onClickListeners for buttons after showing the dialog would prevent
            // the dialog from dismissing automatically on clicking the buttons
            openLinkOpenButton = openLinkDialog?.getButton(AlertDialog.BUTTON_POSITIVE)
            openLinkOpenButton?.setOnClickListener {
                Util.hideKeyboard(this, 0)
                openLink(openLinkText?.text.toString())
            }
            openLinkDialog?.setOnKeyListener { _: DialogInterface?, keyCode: Int, event: KeyEvent ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.repeatCount == 0) {
                    dismissAlertDialogIfExists(openLinkDialog)
                    return@setOnKeyListener true
                }
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception showing Open Link dialog")
        }
    }

    fun showChatLink(link: String?) {
        Timber.d("Link: %s", link)
        val openChatLinkIntent = Intent(this, ChatActivity::class.java)
        if (joiningToChatLink) {
            openChatLinkIntent.action = Constants.ACTION_JOIN_OPEN_CHAT_LINK
            resetJoiningChatLink()
        } else {
            openChatLinkIntent.action = Constants.ACTION_OPEN_CHAT_LINK
        }
        openChatLinkIntent.data = Uri.parse(link)
        startActivity(openChatLinkIntent)
        drawerItem = DrawerItem.CHAT
        selectDrawerItem(drawerItem)
    }

    /**
     * Initializes the variables to join chat by default.
     */
    private fun resetJoiningChatLink() {
        joiningToChatLink = false
        linkJoinToChatLink = null
    }

    override fun uploadFiles() {
        uploadBottomSheetDialogActionHandler.uploadFiles()
    }

    override fun uploadFolder() {
        uploadBottomSheetDialogActionHandler.uploadFolder()
    }

    override fun takePictureAndUpload() {
        uploadBottomSheetDialogActionHandler.takePictureAndUpload()
    }

    override fun scanDocument() {
        uploadBottomSheetDialogActionHandler.scanDocument()
    }

    override fun showNewFolderDialog(typedText: String?) {
        uploadBottomSheetDialogActionHandler.showNewFolderDialog(typedText)
    }

    override fun showNewTextFileDialog(typedName: String?) {
        uploadBottomSheetDialogActionHandler.showNewTextFileDialog(typedName)
    }

    var parentHandleBrowser: Long
        get() = fileBrowserViewModel.getSafeBrowserParentHandle()
        set(parentHandleBrowser) {
            Timber.d("Set value to:%s", parentHandleBrowser)
            fileBrowserViewModel.setBrowserParentHandle(parentHandleBrowser)
        }

    // For home page, its parent is always the root of cloud drive.
    override val currentParentHandle: Long
        get() {
            var parentHandle: Long = -1
            when (drawerItem) {
                DrawerItem.HOMEPAGE ->                 // For home page, its parent is always the root of cloud drive.
                    parentHandle = megaApi.rootNode?.handle ?: INVALID_HANDLE

                DrawerItem.CLOUD_DRIVE -> parentHandle =
                    fileBrowserViewModel.getSafeBrowserParentHandle()

                DrawerItem.BACKUPS -> parentHandle = backupsViewModel.state().backupsHandle
                DrawerItem.RUBBISH_BIN -> parentHandle =
                    rubbishBinViewModel.state().rubbishBinHandle

                DrawerItem.SHARED_ITEMS -> {
                    when {
                        tabItemShares === SharesTab.INCOMING_TAB -> {
                            parentHandle = incomingSharesViewModel.state().incomingHandle
                        }

                        tabItemShares === SharesTab.OUTGOING_TAB -> {
                            parentHandle = outgoingSharesViewModel.state().outgoingHandle
                        }

                        tabItemShares === SharesTab.LINKS_TAB -> {
                            parentHandle = legacyLinksViewModel.state().linksHandle
                        }
                    }
                }

                DrawerItem.SEARCH -> {
                    if (searchViewModel.state.value.searchParentHandle != -1L) {
                        parentHandle = searchViewModel.state.value.searchParentHandle
                    } else if (searchViewModel.state.value.searchDrawerItem != null) {
                        when (searchViewModel.state.value.searchDrawerItem) {
                            DrawerItem.CLOUD_DRIVE -> parentHandle =
                                fileBrowserViewModel.getSafeBrowserParentHandle()

                            DrawerItem.SHARED_ITEMS -> when (searchViewModel.state.value.searchSharesTab) {
                                SharesTab.INCOMING_TAB -> parentHandle =
                                    incomingSharesViewModel.state().incomingHandle

                                SharesTab.OUTGOING_TAB -> parentHandle =
                                    outgoingSharesViewModel.state().outgoingHandle

                                SharesTab.LINKS_TAB -> parentHandle =
                                    legacyLinksViewModel.state().linksHandle

                                else -> {}
                            }

                            DrawerItem.BACKUPS -> parentHandle =
                                backupsViewModel.state().backupsHandle

                            else -> {}
                        }
                    }
                }

                else -> return parentHandle
            }
            return parentHandle
        }

    override fun getCurrentParentNode(parentHandle: Long, error: Int): MegaNode? {
        var errorString: String? = null
        if (error != -1) {
            errorString = getString(error)
        }
        if (parentHandle == -1L && errorString != null) {
            showSnackbar(Constants.SNACKBAR_TYPE, errorString, -1)
            Timber.d("%s: parentHandle == -1", errorString)
            return null
        }
        val parentNode = megaApi.getNodeByHandle(parentHandle)
        if (parentNode == null && errorString != null) {
            showSnackbar(Constants.SNACKBAR_TYPE, errorString, -1)
            Timber.d("%s: parentNode == null", errorString)
            return null
        }
        return parentNode
    }

    override fun createFolder(folderName: String) {
        Timber.d("createFolder")
        if (!viewModel.isConnected) {
            showSnackbar(
                Constants.SNACKBAR_TYPE,
                getString(R.string.error_server_connection_problem),
                -1
            )
            return
        }
        if (isFinishing) {
            return
        }
        val parentNode = getCurrentParentNode(
            currentParentHandle,
            R.string.context_folder_no_created
        )
            ?: return
        val nL: ArrayList<MegaNode> = megaApi.getChildren(parentNode)
        for (i in nL.indices) {
            if (folderName.compareTo(nL[i].name) == 0) {
                showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getString(R.string.context_folder_already_exists),
                    -1
                )
                Timber.d("Folder not created: folder already exists")
                return
            }
        }
        statusDialog = createProgressDialog(
            this,
            getString(R.string.context_creating_folder)
        )
        megaApi.createFolder(folderName, parentNode, this)
    }

    fun chooseAddContactDialog() {
        Timber.d("chooseAddContactDialog")
        if (megaApi.rootNode != null) {
            startActivityForResult(
                StartConversationActivity.getChatIntent(this),
                Constants.REQUEST_CREATE_CHAT
            )
        } else {
            Timber.w("Online but not megaApi")
            showSnackbar(
                Constants.SNACKBAR_TYPE,
                getString(R.string.error_server_connection_problem),
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
            )
        }
    }

    override fun onJoinMeeting() {
        MEETING_TYPE = MeetingActivity.MEETING_ACTION_JOIN
        if (CallUtil.participatingInACall()) {
            CallUtil.showConfirmationInACall(
                this,
                getString(R.string.text_join_call),
                passcodeManagement
            )
        } else {
            showOpenLinkDialog()
        }
    }

    override fun onCreateMeeting() {
        chatsFragment?.onCreateMeeting()
    }

    override fun onScheduleMeeting() {
        startActivity(Intent(this, CreateScheduledMeetingActivity::class.java))
    }

    fun showConfirmationRemoveAllSharingContacts(shares: List<MegaNode?>) {
        if (shares.size == 1) {
            showConfirmationRemoveAllSharingContacts(megaApi.getOutShares(shares[0]), shares[0])
            return
        }
        val builder = MaterialAlertDialogBuilder(this)
        builder.setMessage(
            resources.getQuantityString(
                R.plurals.alert_remove_several_shares,
                shares.size,
                shares.size
            )
        )
            .setPositiveButton(
                R.string.shared_items_outgoing_unshare_confirm_dialog_button_yes
            ) { _: DialogInterface?, _: Int ->
                nodeController.removeSeveralFolderShares(shares)
            }
            .setNegativeButton(
                R.string.shared_items_outgoing_unshare_confirm_dialog_button_no
            ) { _: DialogInterface?, _: Int -> }
            .show()
    }

    fun showConfirmationRemoveAllSharingContacts(
        shareList: ArrayList<MegaShare?>,
        node: MegaNode?,
    ) {
        val builder = MaterialAlertDialogBuilder(this)
        val size = shareList.size
        val message: String = resources.getQuantityString(
            R.plurals.confirmation_remove_outgoing_shares,
            size,
            size
        )
        builder.setMessage(message)
            .setPositiveButton(R.string.general_remove) { _: DialogInterface?, _: Int ->
                nodeController.removeShares(
                    shareList,
                    node
                )
            }
            .setNegativeButton(R.string.general_cancel) { _: DialogInterface?, _: Int -> }
            .show()
    }

    /**
     * Save nodes to device.
     *
     * @param nodes           nodes to save
     * @param highPriority    whether this download is high priority or not
     * @param isFolderLink    whether this download is a folder link
     * @param fromMediaViewer whether this download is from media viewer
     * @param fromChat        whether this download is from chat
     */
    fun saveNodesToDevice(
        nodes: List<MegaNode?>?, highPriority: Boolean, isFolderLink: Boolean,
        fromMediaViewer: Boolean, fromChat: Boolean,
    ) {
        if (nodes == null) return
        PermissionUtils.checkNotificationsPermission(this)
        nodeSaver.saveNodes(
            nodes.filterNotNull(),
            highPriority,
            isFolderLink,
            fromMediaViewer,
            fromChat
        )
    }

    /**
     * Upon a node is tapped, if it cannot be previewed in-app,
     * then download it first, this download will be marked as "download by tap".
     *
     * @param node Node to be downloaded.
     */
    fun saveNodeByTap(node: MegaNode) {
        PermissionUtils.checkNotificationsPermission(this)
        nodeSaver.saveNodes(
            nodes = listOf(element = node),
            highPriority = true,
            isFolderLink = false,
            fromMediaViewer = false,
            needSerialize = false,
            downloadForPreview = true
        )
    }

    /**
     * Upon a node is open with, if it cannot be previewed in-app,
     * then download it first, this download will be marked as "download by open with".
     *
     * @param node Node to be downloaded.
     */
    fun saveNodeByOpenWith(node: MegaNode) {
        PermissionUtils.checkNotificationsPermission(this)
        nodeSaver.saveNodes(
            nodes = listOf(node),
            highPriority = true,
            isFolderLink = false,
            fromMediaViewer = false,
            needSerialize = false,
            downloadForPreview = true,
            downloadByOpenWith = true
        )
    }

    /**
     * Save nodes to device.
     *
     * @param handles         handles of nodes to save
     * @param highPriority    whether this download is high priority or not
     * @param isFolderLink    whether this download is a folder link
     * @param fromMediaViewer whether this download is from media viewer
     * @param fromChat        whether this download is from chat
     */
    fun saveHandlesToDevice(
        handles: List<Long?>?, highPriority: Boolean, isFolderLink: Boolean,
        fromMediaViewer: Boolean, fromChat: Boolean,
    ) {
        if (handles == null) return
        PermissionUtils.checkNotificationsPermission(this)
        nodeSaver.saveHandles(
            handles.filterNotNull(),
            highPriority,
            isFolderLink,
            fromMediaViewer,
            fromChat
        )
    }

    /**
     * Save offline nodes to device.
     *
     * @param nodes nodes to save
     */
    fun saveOfflineNodesToDevice(nodes: List<MegaOffline?>?) {
        if (nodes == null) return
        PermissionUtils.checkNotificationsPermission(this)
        nodeSaver.saveOfflineNodes(nodes.filterNotNull(), false)
    }

    /**
     * Attach node to chats, only used by NodeOptionsBottomSheetDialogFragment.
     *
     * @param node node to attach
     */
    fun attachNodeToChats(node: MegaNode?) {
        node?.let { nodeAttacher.attachNode(it) }
    }

    /**
     * Attach nodes to chats, used by ActionMode of manager fragments.
     *
     * @param nodes nodes to attach
     */
    fun attachNodesToChats(nodes: List<MegaNode?>?) {
        nodes?.let { nodeAttacher.attachNodes(it.filterNotNull()) }
    }

    fun showConfirmationRemovePublicLink(n: MegaNode) {
        Timber.d("showConfirmationRemovePublicLink")
        if (showTakenDownNodeActionNotAvailableDialog(n, this)) {
            return
        }
        val nodes: ArrayList<MegaNode> = ArrayList()
        nodes.add(n)
        showConfirmationRemoveSeveralPublicLinks(nodes)
    }

    fun showConfirmationRemoveSeveralPublicLinks(nodes: ArrayList<MegaNode>) {
        val message: String
        var node: MegaNode? = null
        if (nodes.size == 1) {
            node = nodes[0]
            message = resources.getQuantityString(R.plurals.remove_links_warning_text, 1)
        } else {
            message =
                resources.getQuantityString(R.plurals.remove_links_warning_text, nodes.size)
        }
        val builder = MaterialAlertDialogBuilder(this)
        val finalNode: MegaNode? = node
        builder.setMessage(message)
            .setPositiveButton(R.string.general_remove) { _: DialogInterface?, _: Int ->
                if (finalNode != null) {
                    if (!viewModel.isConnected) {
                        showSnackbar(
                            Constants.SNACKBAR_TYPE,
                            getString(R.string.error_server_connection_problem),
                            -1
                        )
                        return@setPositiveButton
                    }
                    nodeController.removeLink(finalNode, ExportListener(this, 1))
                } else {
                    nodeController.removeLinks(nodes)
                }
            }
            .setNegativeButton(R.string.general_cancel) { _: DialogInterface?, _: Int -> }
            .show()
        refreshAfterMovingToRubbish()
    }

    override fun confirmLeaveChat(chatId: Long) {
        megaChatApi.leaveChat(chatId, RemoveFromChatRoomListener(this))
    }

    override fun confirmLeaveChats(chats: List<MegaChatListItem>) {
        for (chat in chats) {
            megaChatApi.leaveChat(chat.chatId, RemoveFromChatRoomListener(this))
        }
    }

    override fun leaveChatSuccess() {
        // No update needed.
    }

    fun cameraUploadsClicked() {
        Timber.d("cameraUploadsClicked")
        drawerItem = DrawerItem.PHOTOS
        setBottomNavigationMenuItemChecked(PHOTOS_BNV)
        selectDrawerItem(drawerItem)
    }

    /**
     * Refresh the UI of the Photos feature
     */
    fun refreshPhotosFragment() {
        if (!isInPhotosPage) return
        drawerItem = DrawerItem.PHOTOS
        setBottomNavigationMenuItemChecked(PHOTOS_BNV)
        setToolbarTitle()
        (supportFragmentManager.findFragmentByTag(FragmentTag.PHOTOS.tag) as? PhotosFragment)?.refreshViewLayout()
    }

    @JvmOverloads
    fun showNodeOptionsPanel(
        node: MegaNode?,
        mode: Int = NodeOptionsBottomSheetDialogFragment.DEFAULT_MODE,
        shareData: ShareData? = null,
    ) {
        Timber.d("showNodeOptionsPanel")
        if (node == null || bottomSheetDialogFragment.isBottomSheetDialogShown()) return
        if (drawerItem == DrawerItem.SEARCH) {
            Analytics.tracker.trackEvent(SearchResultOverflowMenuItemEvent)
        }
        bottomSheetDialogFragment = NodeOptionsBottomSheetDialogFragment.newInstance(
            nodeId = NodeId(node.handle),
            shareData = shareData,
            mode = mode,
        )
        bottomSheetDialogFragment?.show(supportFragmentManager, bottomSheetDialogFragment?.tag)
    }

    fun showNodeOptionsPanel(
        nodeId: NodeId?,
        mode: Int = NodeOptionsBottomSheetDialogFragment.DEFAULT_MODE,
        shareData: ShareData? = null,
    ) {
        Timber.d("showNodeOptionsPanel")
        if (nodeId == null || bottomSheetDialogFragment.isBottomSheetDialogShown()) return
        bottomSheetDialogFragment = NodeOptionsBottomSheetDialogFragment.newInstance(
            nodeId = nodeId,
            shareData = shareData,
            mode = mode,
        )
        bottomSheetDialogFragment?.show(supportFragmentManager, bottomSheetDialogFragment?.tag)
    }

    fun showNodeLabelsPanel(node: NodeId) {
        Timber.d("showNodeLabelsPanel")
        if (bottomSheetDialogFragment.isBottomSheetDialogShown()) {
            bottomSheetDialogFragment?.dismiss()
        }
        bottomSheetDialogFragment = NodeLabelBottomSheetDialogFragment.newInstance(node.longValue)
        bottomSheetDialogFragment?.show(supportFragmentManager, bottomSheetDialogFragment?.tag)
    }

    fun showNewSortByPanel(orderType: Int) {
        if (bottomSheetDialogFragment.isBottomSheetDialogShown()) {
            return
        }
        bottomSheetDialogFragment = SortByBottomSheetDialogFragment.newInstance(orderType)
        bottomSheetDialogFragment?.show(
            supportFragmentManager,
            bottomSheetDialogFragment?.tag
        )
    }

    fun showMeetingOptionsPanel(showSimpleList: Boolean) {
        if (CallUtil.participatingInACall()) {
            CallUtil.showConfirmationInACall(
                this,
                getString(R.string.ongoing_call_content),
                passcodeManagement
            )
        } else {
            bottomSheetDialogFragment = MeetingBottomSheetDialogFragment.newInstance(showSimpleList)
            bottomSheetDialogFragment?.show(
                supportFragmentManager,
                MeetingBottomSheetDialogFragment.TAG
            )
        }
    }

    /**
     * Shows the upload bottom sheet fragment taking into account the upload type received as param.
     *
     * @param uploadType Indicates the type of upload:
     * - GENERAL_UPLOAD if nothing special has to be taken into account.
     * - DOCUMENTS_UPLOAD if an upload from Documents section.
     */
    @JvmOverloads
    fun showUploadPanel(uploadType: Int = if (drawerItem === DrawerItem.HOMEPAGE) UploadBottomSheetDialogFragment.HOMEPAGE_UPLOAD else UploadBottomSheetDialogFragment.GENERAL_UPLOAD) {
        if (!hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            val permissions = readAndWritePermissions
            requestPermission(this, Constants.REQUEST_READ_WRITE_STORAGE, *permissions)
            return
        }
        if (bottomSheetDialogFragment.isBottomSheetDialogShown()) return
        bottomSheetDialogFragment = UploadBottomSheetDialogFragment.newInstance(uploadType)
        bottomSheetDialogFragment?.show(supportFragmentManager, bottomSheetDialogFragment?.tag)
    }

    private val readAndWritePermissions: Array<String>
        get() = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            PermissionUtils.getImagePermissionByVersion(),
            PermissionUtils.getAudioPermissionByVersion(),
            PermissionUtils.getVideoPermissionByVersion(),
            PermissionUtils.getReadExternalStoragePermission()
        )

    private fun refreshCloudDrive() {
        if (rootNode == null) {
            rootNode = megaApi.rootNode
        }
        if (rootNode == null) {
            Timber.w("Root node is NULL. Maybe user is not logged in")
            return
        }
        if (isCloudAdded) {
            fileBrowserViewModel.refreshNodes()
        }
    }

    private fun refreshSharesPageAdapter() {
        sharesPageAdapter.refreshFragment(SharesTab.INCOMING_TAB.position)
        sharesPageAdapter.refreshFragment(SharesTab.OUTGOING_TAB.position)
        sharesPageAdapter.refreshFragment(SharesTab.LINKS_TAB.position)
    }

    fun refreshCloudOrder() {
        // Refresh Cloud Fragment
        refreshCloudDrive()

        // Refresh Rubbish Fragment
        refreshRubbishBin()

        // Refresh Backups Fragment
        refreshBackupsFragment()
        onNodesSharedUpdate()
        refreshSearch()
    }

    var isFirstNavigationLevel: Boolean
        get() = viewModel.state().isFirstNavigationLevel
        set(firstNavigationLevel) {
            Timber.d("Set value to: %s", firstNavigationLevel)
            viewModel.setIsFirstNavigationLevel(firstNavigationLevel)
        }

    fun setParentHandleRubbish(parentHandleRubbish: Long) {
        Timber.d("setParentHandleRubbish")
        rubbishBinViewModel.setRubbishBinHandle(parentHandleRubbish)
    }

    fun setParentHandleBackups(parentHandleBackups: Long) {
        Timber.d("setParentHandleBackups: %s", parentHandleBackups)
        backupsViewModel.updateBackupsHandle(parentHandleBackups)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Timber.d("onNewIntent")
        if (Intent.ACTION_SEARCH == intent.action) {
            intent.getStringExtra(SearchManager.QUERY)
                ?.let { searchViewModel.setSearchQuery(it) }
            searchViewModel.setSearchParentHandle(-1L)
            setToolbarTitle()
            isSearching = true
            if (searchMenuItem != null) {
                MenuItemCompat.collapseActionView(searchMenuItem)
            }
            return
        } else if (Constants.ACTION_SHOW_UPGRADE_ACCOUNT == intent.action) {
            navigateToUpgradeAccount()
            return
        } else if (Constants.ACTION_SHOW_TRANSFERS == intent.action) {
            if (intent.getBooleanExtra(Constants.OPENED_FROM_CHAT, false)) {
                sendBroadcast(
                    Intent(ACTION_CLOSE_CHAT_AFTER_OPEN_TRANSFERS).setPackage(
                        applicationContext.packageName
                    )
                )
            }
            if (intent.getBooleanExtra(Constants.OPENED_FROM_IMAGE_VIEWER, false)) {
                transfersToImageViewer = true
            }
            drawerItem = DrawerItem.TRANSFERS
            transferPageViewModel.setTransfersTab(
                intent.serializable(TRANSFERS_TAB) ?: TransfersTab.NONE
            )
            selectDrawerItem(drawerItem)
            return
        }
        setIntent(intent)
    }

    override fun navigateToUpgradeAccount() {
        if (drawerLayout.isDrawerOpen(
                navigationView
            )
        ) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        startActivity(Intent(this, UpgradeAccountActivity::class.java))
        myAccountInfo.upgradeOpenedFrom = MyAccountInfo.UpgradeFrom.MANAGER
    }

    override fun navigateToContactRequests() {
        closeDrawer()
        startActivity(ContactsActivity.getReceivedRequestsIntent(this))
    }

    override fun navigateToMyAccount() {
        Timber.d("navigateToMyAccount")
        getProLayout.visibility = View.GONE
        showMyAccount()
    }

    override fun navigateToSharedNode(nodeId: Long, childNodes: LongArray?) {
        openLocation(nodeId, childNodes)
    }

    override fun navigateToContactInfo(email: String) {
        ContactUtil.openContactInfoActivity(this, email)
    }

    override fun onClick(v: View) {
        Timber.d("onClick")
        when (v.id) {
            R.id.btnLeft_cancel -> {
                getProLayout.visibility = View.GONE
            }

            R.id.btnRight_upgrade -> {
                //Add navigation to Upgrade Account
                Timber.d("Click on Upgrade in pro panel!")
                navigateToUpgradeAccount()
            }

            R.id.call_in_progress_layout -> {
                returnCall()
            }
        }
    }

    override fun manageCopyMoveException(throwable: Throwable?): Boolean {
        return when (throwable) {
            is ForeignNodeException -> {
                launchForeignNodeError()
                true
            }

            is QuotaExceededMegaException -> {
                showOverQuotaAlert(false)
                true
            }

            is NotEnoughQuotaMegaException -> {
                showOverQuotaAlert(true)
                true
            }

            else -> false
        }
    }

    @SuppressLint("CheckResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        Timber.d("Request code: %d, Result code:%d", requestCode, resultCode)
        if (nodeSaver.handleActivityResult(this, requestCode, resultCode, intent)) {
            return
        }
        if (nodeAttacher.handleActivityResult(requestCode, resultCode, intent, this)) {
            return
        }
        if (resultCode == Activity.RESULT_FIRST_USER) {
            showSnackbar(
                Constants.SNACKBAR_TYPE,
                getString(R.string.context_no_destination_folder),
                -1
            )
            return
        }
        when {
            requestCode == Constants.REQUEST_CODE_GET_FILES && resultCode == Activity.RESULT_OK -> {
                if (intent == null) {
                    Timber.w("Intent NULL")
                    return
                }
                Timber.d("Intent action: %s", intent.action)
                Timber.d("Intent type: %s", intent.type)
                intent.action = Intent.ACTION_OPEN_DOCUMENT
                processFileDialog = showProcessFileDialog(this, intent)
                filePrepareUseCase.prepareFiles(intent)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { shareInfo: List<ShareInfo> ->
                            onIntentProcessed(shareInfo)
                        },
                        { throwable: Throwable -> Timber.e(throwable) }
                    )
                    .addTo(composite)
            }

            requestCode == Constants.REQUEST_CODE_GET_FOLDER -> {
                UploadUtil.getFolder(this, resultCode, intent, currentParentHandle)
            }

            requestCode == Constants.REQUEST_CODE_GET_FOLDER_CONTENT -> {
                if (intent != null && resultCode == Activity.RESULT_OK) {
                    val result = intent.getStringExtra(Constants.EXTRA_ACTION_RESULT)
                    if (TextUtil.isTextEmpty(result)) {
                        return
                    }
                    showSnackbar(
                        Constants.SNACKBAR_TYPE,
                        result,
                        MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                    )
                }
            }

            requestCode == Constants.WRITE_SD_CARD_REQUEST_CODE && resultCode == Activity.RESULT_OK -> {
                if (!hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    requestPermission(
                        this,
                        Constants.REQUEST_WRITE_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                }
                if (viewModel.getStorageState() === StorageState.PayWall) {
                    showOverDiskQuotaPaywallWarning()
                    return
                }
                val treeUri = intent?.data
                Timber.d("Create the document : %s", treeUri)
                val handleToDownload = intent?.getLongExtra("handleToDownload", -1) ?: -1
                Timber.d("The recovered handle is: %s", handleToDownload)
                //Now, call to the DownloadService
                if (handleToDownload != 0L && handleToDownload != -1L) {
                    PermissionUtils.checkNotificationsPermission(this)
                    val service = Intent(this, DownloadService::class.java)
                    service.putExtra(DownloadService.EXTRA_HASH, handleToDownload)
                    service.putExtra(DownloadService.EXTRA_CONTENT_URI, treeUri.toString())
                    val tempFolder =
                        CacheFolderManager.getCacheFolder(CacheFolderManager.TEMPORARY_FOLDER)
                    if (!FileUtil.isFileAvailable(tempFolder)) {
                        showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.general_error), -1)
                        return
                    }
                    service.putExtra(DownloadService.EXTRA_PATH, tempFolder?.absolutePath)
                    startService(service)
                }
            }

            requestCode == Constants.REQUEST_CODE_SELECT_FILE && resultCode == Activity.RESULT_OK -> {
                Timber.d("requestCode == REQUEST_CODE_SELECT_FILE")
                if (intent == null) {
                    Timber.w("Intent NULL")
                    return
                }
                nodeAttacher.handleSelectFileResult(intent, this)
            }

            requestCode == Constants.REQUEST_CODE_SELECT_CONTACT && resultCode == Activity.RESULT_OK -> {
                Timber.d("onActivityResult REQUEST_CODE_SELECT_CONTACT OK")
                if (intent == null) {
                    Timber.w("Intent NULL")
                    return
                }
                val contactsData = intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS)
                megaContacts = intent.getBooleanExtra(AddContactActivity.EXTRA_MEGA_CONTACTS, true)
                val multiselectIntent = intent.getIntExtra("MULTISELECT", -1)
                if (multiselectIntent == 0) {
                    //One file to share
                    val nodeHandle = intent.getLongExtra(AddContactActivity.EXTRA_NODE_HANDLE, -1)
                    if (fileBackupManager.shareFolder(
                            nodeController,
                            longArrayOf(nodeHandle),
                            contactsData ?: ArrayList(),
                            MegaShare.ACCESS_READ
                        )
                    ) {
                        return
                    }
                    val dialogBuilder = MaterialAlertDialogBuilder(this)
                    dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions))
                    val items = arrayOf<CharSequence>(
                        getString(R.string.file_properties_shared_folder_read_only), getString(
                            R.string.file_properties_shared_folder_read_write
                        ), getString(R.string.file_properties_shared_folder_full_access)
                    )
                    dialogBuilder.setSingleChoiceItems(
                        items,
                        -1
                    ) { _: DialogInterface?, item: Int ->
                        permissionsDialog?.dismiss()

                        lifecycleScope.launch {
                            val node = megaApi.getNodeByHandle(nodeHandle)
                            viewModel.initShareKey(node)
                            nodeController.shareFolder(node, contactsData, item)
                        }
                    }
                    dialogBuilder.setTitle(getString(R.string.dialog_select_permissions))
                    permissionsDialog = dialogBuilder.create()
                    permissionsDialog?.show()
                } else if (multiselectIntent == 1) {
                    //Several folders to share
                    val nodeHandles = intent.getLongArrayExtra(AddContactActivity.EXTRA_NODE_HANDLE)
                    if (fileBackupManager.shareFolder(
                            nodeController,
                            nodeHandles ?: LongArray(0),
                            contactsData ?: ArrayList(),
                            MegaShare.ACCESS_READ
                        )
                    ) {
                        return
                    }
                    val dialogBuilder = MaterialAlertDialogBuilder(this)
                    dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions))
                    val items = arrayOf<CharSequence>(
                        getString(R.string.file_properties_shared_folder_read_only), getString(
                            R.string.file_properties_shared_folder_read_write
                        ), getString(R.string.file_properties_shared_folder_full_access)
                    )
                    dialogBuilder.setSingleChoiceItems(items, -1) { _, item ->
                        permissionsDialog?.dismiss()
                        nodeController.shareFolders(nodeHandles, contactsData, item)
                    }
                    dialogBuilder.setTitle(getString(R.string.dialog_select_permissions))
                    permissionsDialog = dialogBuilder.create()
                    permissionsDialog?.show()
                }
            }

            requestCode == Constants.REQUEST_CODE_SELECT_FOLDER_TO_MOVE && resultCode == Activity.RESULT_OK -> {
                if (intent == null) {
                    Timber.d("Intent NULL")
                    return
                }
                val moveHandles = intent.getLongArrayExtra("MOVE_HANDLES") ?: LongArray(0)
                val toHandle = intent.getLongExtra("MOVE_TO", 0)
                if (moveHandles.isNotEmpty()) {
                    viewModel.checkNodesNameCollision(
                        moveHandles.toList(),
                        toHandle,
                        NodeNameCollisionType.MOVE
                    )
                }
            }

            requestCode == Constants.REQUEST_CODE_SELECT_FOLDER_TO_COPY && resultCode == Activity.RESULT_OK -> {
                Timber.d("REQUEST_CODE_SELECT_COPY_FOLDER")
                if (intent == null) {
                    Timber.w("Intent NULL")
                    return
                }
                val copyHandles = intent.getLongArrayExtra("COPY_HANDLES") ?: LongArray(0)
                val toHandle = intent.getLongExtra("COPY_TO", 0)
                if (copyHandles.isNotEmpty()) {
                    viewModel.checkNodesNameCollision(
                        copyHandles.toList(),
                        toHandle,
                        NodeNameCollisionType.COPY
                    )
                }
            }

            requestCode == Constants.REQUEST_CODE_REFRESH_API_SERVER && resultCode == Activity.RESULT_OK -> {
                Timber.d("Refresh DONE")
                if (intent == null) {
                    Timber.w("Intent NULL")
                    return
                }
                viewModel.askForFullAccountInfo()
                viewModel.askForExtendedAccountDetails()
                if (drawerItem === DrawerItem.CLOUD_DRIVE) {
                    fileBrowserViewModel.refreshNodes()
                } else if (drawerItem === DrawerItem.SHARED_ITEMS) {
                    refreshIncomingShares()
                }
            }

            requestCode == Constants.TAKE_PHOTO_CODE -> {
                Timber.d("TAKE_PHOTO_CODE")
                if (resultCode == Activity.RESULT_OK) {
                    val parentHandle = currentParentHandle
                    val file = UploadUtil.getTemporalTakePictureFile(this)
                    if (file != null) {
                        addPhotoToParent(file, parentHandle)
                    }
                } else {
                    Timber.w("TAKE_PHOTO_CODE--->ERROR!")
                }
            }

            requestCode == Constants.REQUEST_CREATE_CHAT && resultCode == Activity.RESULT_OK -> {
                Timber.d("REQUEST_CREATE_CHAT OK")
                if (intent == null) {
                    Timber.w("Intent NULL")
                    return
                }
                val isNewMeeting =
                    intent.getBooleanExtra(StartConversationActivity.EXTRA_NEW_MEETING, false)
                if (isNewMeeting) {
                    onCreateMeeting()
                    return
                }
                val isJoinMeeting =
                    intent.getBooleanExtra(StartConversationActivity.EXTRA_JOIN_MEETING, false)
                if (isJoinMeeting) {
                    onJoinMeeting()
                    return
                }
                val chatId = intent.getLongExtra(
                    StartConversationActivity.EXTRA_NEW_CHAT_ID,
                    MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                )
                if (chatId != MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
                    startActivity(
                        Intent(this, ChatActivity::class.java)
                            .setAction(Constants.ACTION_CHAT_SHOW_MESSAGES)
                            .putExtra(Constants.CHAT_ID, chatId)
                    )
                    return
                }
                val contactsData = intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS)
                val isGroup = intent.getBooleanExtra(AddContactActivity.EXTRA_GROUP_CHAT, false)
                if (contactsData != null) {
                    if (!isGroup) {
                        Timber.d("Create one to one chat")
                        val user = megaApi.getContact(contactsData[0])
                        if (user != null) {
                            Timber.d("Chat with contact: %s", contactsData.size)
                            startOneToOneChat(user)
                        }
                    } else {
                        Timber.d("Create GROUP chat")
                        val peers: MegaChatPeerList = MegaChatPeerList.createInstance()
                        for (i in contactsData.indices) {
                            val user = megaApi.getContact(contactsData[i])
                            if (user != null) {
                                peers.addPeer(user.handle, MegaChatPeerList.PRIV_STANDARD)
                            }
                        }
                        val chatTitle = intent.getStringExtra(AddContactActivity.EXTRA_CHAT_TITLE)
                        val isEKR = intent.getBooleanExtra(AddContactActivity.EXTRA_EKR, false)
                        var chatLink = false
                        if (!isEKR) {
                            chatLink =
                                intent.getBooleanExtra(AddContactActivity.EXTRA_CHAT_LINK, false)
                        }
                        val allowAddParticipants =
                            intent.getBooleanExtra(AddContactActivity.ALLOW_ADD_PARTICIPANTS, false)
                        createGroupChat(peers, chatTitle, chatLink, isEKR, allowAddParticipants)
                    }
                }
            }

            requestCode == Constants.REQUEST_INVITE_CONTACT_FROM_DEVICE && resultCode == Activity.RESULT_OK -> {
                Timber.d("REQUEST_INVITE_CONTACT_FROM_DEVICE OK")
                if (intent == null) {
                    Timber.w("Intent NULL")
                    return
                }
                val contactsData = intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS)
                megaContacts = intent.getBooleanExtra(AddContactActivity.EXTRA_MEGA_CONTACTS, true)
                if (contactsData != null) {
                    contactController.inviteMultipleContacts(contactsData)
                }
            }

            requestCode == Constants.REQUEST_CODE_FILE_INFO && resultCode == Activity.RESULT_OK -> {
                if (isCloudAdded) {
                    val handle = intent?.getLongExtra(FileInfoActivity.NODE_HANDLE, -1) ?: -1
                    fileBrowserViewModel.setBrowserParentHandle(handle)
                }
                onNodesSharedUpdate()
            }

            requestCode == Constants.REQUEST_WRITE_STORAGE || requestCode == Constants.REQUEST_READ_WRITE_STORAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    when (requestCode) {
                        Constants.REQUEST_WRITE_STORAGE -> {
                            // Take picture scenarios
                            if (typesCameraPermission == Constants.TAKE_PICTURE_OPTION) {
                                if (!hasPermissions(this, Manifest.permission.CAMERA)) {
                                    requestPermission(
                                        this,
                                        Constants.REQUEST_CAMERA,
                                        Manifest.permission.CAMERA
                                    )
                                } else {
                                    Util.checkTakePicture(this, Constants.TAKE_PHOTO_CODE)
                                    typesCameraPermission = Constants.INVALID_TYPE_PERMISSIONS
                                }
                                return
                            }

                            // General download scenario
                            nodeSaver.handleRequestPermissionsResult(requestCode)
                        }

                        Constants.REQUEST_READ_WRITE_STORAGE ->                         // Upload scenario
                            Handler(Looper.getMainLooper()).post { showUploadPanel() }
                    }
                }
            }

            requestCode == Constants.REQUEST_CODE_DELETE_VERSIONS_HISTORY && resultCode == Activity.RESULT_OK -> {
                if (!viewModel.isConnected) {
                    Util.showErrorAlertDialog(
                        getString(R.string.error_server_connection_problem),
                        false,
                        this
                    )
                    return
                }
                if (intent?.getBooleanExtra(
                        VersionsFileActivity.KEY_DELETE_VERSION_HISTORY,
                        false
                    ) == true
                ) {
                    val node = megaApi.getNodeByHandle(
                        intent.getLongExtra(
                            VersionsFileActivity.KEY_DELETE_NODE_HANDLE,
                            0
                        )
                    )
                    val versions: ArrayList<MegaNode> = megaApi.getVersions(node)
                    versionsToRemove = versions.size - 1
                    for (i in 1 until versions.size) {
                        megaApi.removeVersion(versions[i], this)
                    }
                }
            }

            else -> {
                Timber.w("No request code processed")
                super.onActivityResult(requestCode, resultCode, intent)
            }
        }
    }

    private fun addPhotoToParent(file: File, parentHandle: Long) {
        lifecycleScope.launch {
            runCatching {
                checkNameCollisionUseCase.checkNameCollision(file.name, parentHandle)
            }.onSuccess { handle: Long ->
                val list: ArrayList<NameCollision> = ArrayList()
                list.add(
                    NameCollision.Upload.getUploadCollision(
                        handle,
                        file,
                        parentHandle,
                    )
                )
                nameCollisionActivityContract?.launch(list)
            }.onFailure { throwable: Throwable? ->
                if (throwable is MegaNodeException.ParentDoesNotExistException) {
                    Timber.e(throwable)
                    showSnackbar(
                        Constants.SNACKBAR_TYPE,
                        getString(R.string.general_error),
                        MEGACHAT_INVALID_HANDLE
                    )
                } else if (throwable is MegaNodeException.ChildDoesNotExistsException) {
                    uploadFile(file, parentHandle)
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun uploadFile(file: File, parentHandle: Long) {
        PermissionUtils.checkNotificationsPermission(this)
        uploadUseCase.upload(this, file, parentHandle)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { Timber.d("Upload started") },
                { t: Throwable? -> Timber.e(t) })
            .addTo(composite)
    }

    fun createGroupChat(
        peers: MegaChatPeerList,
        chatTitle: String?,
        chatLink: Boolean,
        isEKR: Boolean,
        allowAddParticipants: Boolean,
    ) {
        Timber.d("Create group chat with participants: %s", peers.size())
        if (isEKR) {
            megaChatApi.createGroupChat(peers, chatTitle, false, false, allowAddParticipants, this)
        } else {
            if (chatLink) {
                if (chatTitle != null && chatTitle.isNotEmpty()) {
                    val listener = CreateGroupChatWithPublicLink(this, chatTitle)
                    megaChatApi.createPublicChat(
                        peers,
                        chatTitle,
                        false,
                        false,
                        allowAddParticipants,
                        listener
                    )
                } else {
                    Util.showAlert(this, getString(R.string.message_error_set_title_get_link), null)
                }
            } else {
                megaChatApi.createPublicChat(
                    peers,
                    chatTitle,
                    false,
                    false,
                    allowAddParticipants,
                    this
                )
            }
        }
    }

    fun startOneToOneChat(user: MegaUser?) {
        Timber.d("User Handle: %s", user?.handle)
        val chat = user?.let { megaChatApi.getChatRoomByUser(it.handle) }
        val peers: MegaChatPeerList = MegaChatPeerList.createInstance()
        if (chat == null) {
            Timber.d("No chat, create it!")
            user?.handle?.let { peers.addPeer(it, MegaChatPeerList.PRIV_STANDARD) }
            megaChatApi.createChat(false, peers, this)
        } else {
            Timber.d("There is already a chat, open it!")
            val intentOpenChat = Intent(this, ChatActivity::class.java)
            intentOpenChat.action = Constants.ACTION_CHAT_SHOW_MESSAGES
            intentOpenChat.putExtra(Constants.CHAT_ID, chat.chatId)
            this.startActivity(intentOpenChat)
        }
    }

    private fun disableNavigationViewMenu(menu: Menu) {
        Timber.d("disableNavigationViewMenu")
        var mi = menu.findItem(R.id.bottom_navigation_item_cloud_drive)
        if (mi != null) {
            mi.isChecked = false
            mi.isEnabled = false
        }
        mi = menu.findItem(R.id.bottom_navigation_item_camera_uploads)
        if (mi != null) {
            mi.isChecked = false
            mi.isEnabled = false
        }
        mi = menu.findItem(R.id.bottom_navigation_item_chat)
        if (mi != null) {
            mi.isChecked = false
        }
        mi = menu.findItem(R.id.bottom_navigation_item_shared_items)
        if (mi != null) {
            mi.isChecked = false
            mi.isEnabled = false
        }
        mi = menu.findItem(R.id.bottom_navigation_item_homepage)
        if (mi != null) {
            mi.isChecked = false
        }
    }

    private fun resetNavigationViewMenu(menu: Menu) {
        Timber.d("resetNavigationViewMenu()")
        if (!viewModel.isConnected || megaApi.rootNode == null) {
            disableNavigationViewMenu(menu)
            return
        }
        var mi = menu.findItem(R.id.bottom_navigation_item_cloud_drive)
        if (mi != null) {
            mi.isChecked = false
            mi.isEnabled = true
        }
        mi = menu.findItem(R.id.bottom_navigation_item_camera_uploads)
        if (mi != null) {
            mi.isChecked = false
            mi.isEnabled = true
        }
        mi = menu.findItem(R.id.bottom_navigation_item_chat)
        if (mi != null) {
            mi.isChecked = false
            mi.isEnabled = true
        }
        mi = menu.findItem(R.id.bottom_navigation_item_shared_items)
        if (mi != null) {
            mi.isChecked = false
            mi.isEnabled = true
        }
    }

    private fun showProPanel() {
        Timber.d("showProPanel")
        //Left and Right margin
        val proTextParams = getProText.layoutParams as LinearLayout.LayoutParams
        proTextParams.setMargins(
            Util.scaleWidthPx(24, outMetrics),
            Util.scaleHeightPx(23, outMetrics),
            Util.scaleWidthPx(24, outMetrics),
            Util.scaleHeightPx(23, outMetrics)
        )
        getProText.layoutParams = proTextParams
        rightUpgradeButton.setOnClickListener(this)
        rightUpgradeButton.layoutParams
        //Left and Right margin
        val optionTextParams = rightUpgradeButton.layoutParams as LinearLayout.LayoutParams
        optionTextParams.setMargins(
            Util.scaleWidthPx(6, outMetrics),
            0,
            Util.scaleWidthPx(8, outMetrics),
            0
        )
        rightUpgradeButton.layoutParams = optionTextParams
        leftCancelButton.setOnClickListener(this)
        val cancelButtonLayoutParameters = leftCancelButton.layoutParams
        leftCancelButton.layoutParams = cancelButtonLayoutParameters
        //Left and Right margin
        val cancelTextParams = leftCancelButton.layoutParams as LinearLayout.LayoutParams
        cancelTextParams.setMargins(
            Util.scaleWidthPx(6, outMetrics),
            0,
            Util.scaleWidthPx(6, outMetrics),
            0
        )
        leftCancelButton.layoutParams = cancelTextParams
        getProLayout.visibility = View.VISIBLE
        getProLayout.bringToFront()
    }

    /**
     * Check the current storage state.
     *
     * @param onCreate Flag to indicate if the method was called from "onCreate" or not.
     */
    private fun checkCurrentStorageStatus() {
        // If the current storage state is not initialized is because the app received the
        // event informing about the storage state  during login, the ManagerActivity
        // wasn't active and for this reason the value is stored in the MegaApplication object.
        val storageStateToCheck: StorageState =
            if (storageState !== StorageState.Unknown) storageState else viewModel.getStorageState()
        checkStorageStatus(storageStateToCheck)
    }

    /**
     * Check the storage state provided as first parameter.
     *
     * @param newStorageState Storage state to check.
     */
    private fun checkStorageStatus(newStorageState: StorageState?) {
        when (newStorageState) {
            StorageState.Green -> {
                Timber.d("STORAGE STATE GREEN")
                notifyUploadServiceStorageStateChange(newStorageState, storageState)
                if (myAccountInfo.accountType == MegaAccountDetails.ACCOUNT_TYPE_FREE) {
                    Timber.d("ACCOUNT TYPE FREE")
                    if (Util.showMessageRandom()) {
                        Timber.d("Show message random: TRUE")
                        showProPanel()
                    }
                }
                storageState = newStorageState
                viewModel.startCameraUpload()
            }

            StorageState.Orange -> {
                Timber.w("STORAGE STATE ORANGE")
                notifyUploadServiceStorageStateChange(newStorageState, storageState)
                if (newStorageState.ordinal > storageState.ordinal) {
                    showStorageAlmostFullDialog()
                }
                storageState = newStorageState
                Timber.d("Try to start CU, false.")
                viewModel.startCameraUpload()
            }

            StorageState.Red -> {
                Timber.w("STORAGE STATE RED")
                if (newStorageState.ordinal > storageState.ordinal) {
                    showStorageFullDialog()
                }
            }

            StorageState.PayWall -> Timber.w("STORAGE STATE PAYWALL")
            else -> return
        }
        storageState = newStorageState
    }

    private fun notifyUploadServiceStorageStateChange(
        newStorageState: StorageState,
        oldStorageState: StorageState,
    ) {
        if (newStorageState != oldStorageState) {
            val uploadServiceIntent = Intent(this, UploadService::class.java)
            uploadServiceIntent.action = Constants.ACTION_STORAGE_STATE_CHANGED
            runCatching { ContextCompat.startForegroundService(this, uploadServiceIntent) }
                .onFailure {
                    Timber.e(it, "Exception starting UploadService")
                }
        }
    }

    /**
     * Show a dialog to indicate that the storage space is almost full.
     */
    private fun showStorageAlmostFullDialog() {
        Timber.d("showStorageAlmostFullDialog")
        showStorageStatusDialog(
            storageState = StorageState.Orange,
            overQuotaAlert = false,
            preWarning = false
        )
    }

    /**
     * Show a dialog to indicate that the storage space is full.
     */
    private fun showStorageFullDialog() {
        Timber.d("showStorageFullDialog")
        showStorageStatusDialog(
            storageState = StorageState.Red,
            overQuotaAlert = false,
            preWarning = false
        )
    }

    /**
     * Show an over quota alert dialog.
     *
     * @param preWarning Flag to indicate if is a pre over quota alert or not.
     */
    private fun showOverQuotaAlert(preWarning: Boolean) {
        Timber.d("preWarning: %s", preWarning)
        showStorageStatusDialog(
            if (preWarning) StorageState.Orange else StorageState.Red,
            true, preWarning
        )
    }

    /**
     * Method to show a dialog to indicate the storage status.
     *
     * @param storageState   Storage status.
     * @param overQuotaAlert Flag to indicate that is an overquota alert or not.
     * @param preWarning     Flag to indicate if is a pre-overquota alert or not.
     */
    private fun showStorageStatusDialog(
        storageState: StorageState,
        overQuotaAlert: Boolean,
        preWarning: Boolean,
    ) {
        if (showDialogStorageStatusJob?.isActive == true) return
        showDialogStorageStatusJob = lifecycleScope.launch {
            lifecycle.withStarted {
                StorageStatusDialogFragment.newInstance(storageState, overQuotaAlert, preWarning)
                    .show(supportFragmentManager, StorageStatusDialogFragment.TAG)
            }
        }
    }

    private fun refreshOfflineNodes() {
        Timber.d("updateOfflineView")
        if (fullscreenOfflineFragment != null) {
            fullscreenOfflineFragment?.refreshNodes()
        } else if (pagerOfflineFragment != null) {
            pagerOfflineFragment?.refreshNodes()
        }
    }

    /**
     * Handle processed upload intent.
     *
     * @param infoList List<ShareInfo> containing all the upload info.
    </ShareInfo> */
    private fun onIntentProcessed(infoList: List<ShareInfo>?) {
        Timber.d("onIntentProcessed")
        val parentNode: MegaNode? = getCurrentParentNode(currentParentHandle, -1)
        if (parentNode == null) {
            dismissAlertDialogIfExists(statusDialog)
            dismissAlertDialogIfExists(processFileDialog)
            showSnackbar(
                Constants.SNACKBAR_TYPE,
                getString(R.string.error_temporary_unavaible),
                -1
            )
            return
        }
        if (infoList == null) {
            dismissAlertDialogIfExists(statusDialog)
            dismissAlertDialogIfExists(processFileDialog)
            showSnackbar(
                Constants.SNACKBAR_TYPE,
                getString(R.string.upload_can_not_open),
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
            )
            return
        }
        if (viewModel.getStorageState() === StorageState.PayWall) {
            dismissAlertDialogIfExists(statusDialog)
            dismissAlertDialogIfExists(processFileDialog)
            showOverDiskQuotaPaywallWarning()
            return
        }
        checkNameCollisionUseCase.checkShareInfoList(infoList, parentNode)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { (collisions, withoutCollisions): Pair<ArrayList<NameCollision>, List<ShareInfo>> ->
                    dismissAlertDialogIfExists(statusDialog)
                    dismissAlertDialogIfExists(processFileDialog)

                    if (collisions.isNotEmpty()) {
                        nameCollisionActivityContract?.launch(collisions)
                    }
                    if (withoutCollisions.isNotEmpty()) {
                        showSnackbar(
                            Constants.SNACKBAR_TYPE,
                            resources.getQuantityString(
                                R.plurals.upload_began,
                                withoutCollisions.size,
                                withoutCollisions.size
                            ),
                            MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                        )
                        for (info in withoutCollisions) {
                            if (info.isContact) {
                                requestContactsPermissions(info, parentNode)
                            } else {
                                uploadUseCase.upload(this, info, null, parentNode.handle)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(
                                        { Timber.d("Upload started") },
                                        { t: Throwable? -> Timber.e(t) })
                                    .addTo(composite)
                            }
                        }
                    }
                },
                {
                    dismissAlertDialogIfExists(statusDialog)
                    dismissAlertDialogIfExists(processFileDialog)
                    showSnackbar(
                        Constants.SNACKBAR_TYPE,
                        getString(R.string.error_temporary_unavaible),
                        MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                    )
                }
            )
            .addTo(composite)
    }

    private fun requestContactsPermissions(info: ShareInfo?, parentNode: MegaNode?) {
        Timber.d("requestContactsPermissions")
        if (!hasPermissions(this, Manifest.permission.READ_CONTACTS)) {
            Timber.w("No read contacts permission")
            infoManager = info
            parentNodeManager = parentNode
            requestPermission(
                this,
                Constants.REQUEST_UPLOAD_CONTACT,
                Manifest.permission.READ_CONTACTS
            )
        } else {
            uploadContactInfo(info, parentNode)
        }
    }

    private fun uploadContactInfo(info: ShareInfo?, parentNode: MegaNode?) {
        Timber.d("Upload contact info")
        runCatching {
            val cursorID =
                info?.contactUri?.let { contentResolver.query(it, null, null, null, null) }
                    ?: throw NullPointerException("Cursor of ${info?.contactUri} is null")
            if (cursorID.moveToFirst()) {
                Timber.d("It is a contact")
                var id: String? = null
                try {
                    id =
                        cursorID.getString(cursorID.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                } catch (exception: IllegalArgumentException) {
                    Timber.w(exception, "Exception getting contact ID.")
                }
                var name: String? = null
                try {
                    name =
                        cursorID.getString(cursorID.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                } catch (exception: IllegalArgumentException) {
                    Timber.w(exception, "Exception getting contact display name.")
                }
                var hasPhone = -1
                try {
                    hasPhone =
                        cursorID.getInt(cursorID.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER))
                } catch (exception: IllegalArgumentException) {
                    Timber.w(exception, "Exception getting contact details.")
                }

                // get the user's email address
                var email: String? = null
                val ce = contentResolver.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                    arrayOf(id),
                    null
                )
                if (ce != null && ce.moveToFirst()) {
                    try {
                        email =
                            ce.getString(ce.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.DATA))
                    } catch (exception: IllegalArgumentException) {
                        Timber.w(exception, "Exception getting contact email.")
                    }
                    ce.close()
                }

                // get the user's phone number
                var phone: String? = null
                if (hasPhone > 0) {
                    val cp = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        arrayOf(id),
                        null
                    )
                    if (cp != null && cp.moveToFirst()) {
                        try {
                            phone =
                                cp.getString(cp.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                        } catch (exception: IllegalArgumentException) {
                            Timber.w(exception, "Exception getting contact phone number.")
                        }
                        cp.close()
                    }
                }
                val data = StringBuilder()
                data.append(name)
                if (phone != null) {
                    data.append(", $phone")
                }
                if (email != null) {
                    data.append(", $email")
                }
                createFile(name, data.toString(), parentNode)
            }
            cursorID.close()
        }.onFailure {
            Timber.e(it)
            showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.error_temporary_unavaible), -1)
        }
    }

    private fun createFile(name: String?, data: String, parentNode: MegaNode?) {
        if (viewModel.getStorageState() === StorageState.PayWall) {
            showOverDiskQuotaPaywallWarning()
            return
        }
        val file = FileUtil.createTemporalTextFile(this, name, data)
        if (file == null) {
            showSnackbar(
                Constants.SNACKBAR_TYPE,
                getString(R.string.general_text_error),
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
            )
            return
        }
        if (parentNode == null) return
        lifecycleScope.launch {
            runCatching { checkNameCollisionUseCase.checkAsync(file.name, parentNode) }
                .onSuccess { handle: Long ->
                    val list: ArrayList<NameCollision> = ArrayList()
                    list.add(
                        NameCollision.Upload.getUploadCollision(
                            handle,
                            file,
                            parentNode.handle,
                        )
                    )
                    nameCollisionActivityContract?.launch(list)
                }.onFailure { throwable: Throwable? ->
                    if (throwable is MegaNodeException.ParentDoesNotExistException) {
                        showSnackbar(
                            Constants.SNACKBAR_TYPE,
                            getString(R.string.general_error),
                            MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                        )
                    } else if (throwable is MegaNodeException.ChildDoesNotExistsException) {
                        uploadFile(file, parentNode)
                    }
                }
        }
    }

    @SuppressLint("CheckResult")
    private fun uploadFile(
        file: File,
        parentNode: MegaNode,
    ) {
        PermissionUtils.checkNotificationsPermission(this)
        val text: String =
            resources.getQuantityString(R.plurals.upload_began, 1, 1)
        uploadUseCase.upload(this, file, parentNode.handle)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    text,
                    MEGACHAT_INVALID_HANDLE
                )
            }, { t: Throwable? -> Timber.e(t) })
            .addTo(composite)
    }

    override fun onRequestStart(api: MegaChatApiJava, request: MegaChatRequest) {
        Timber.d("onRequestStart(CHAT): %s", request.requestString)
    }

    override fun onRequestUpdate(api: MegaChatApiJava, request: MegaChatRequest) {}
    override fun onRequestFinish(api: MegaChatApiJava, request: MegaChatRequest, e: MegaChatError) {
        Timber.d("onRequestFinish(CHAT): %s_%d", request.requestString, e.errorCode)
        if (request.type == MegaChatRequest.TYPE_CREATE_CHATROOM) {
            Timber.d("Create chat request finish")
            onRequestFinishCreateChat(e.errorCode, request.chatHandle)
        } else if (request.type == MegaChatRequest.TYPE_DISCONNECT) {
            if (e.errorCode == MegaChatError.ERROR_OK) {
                Timber.d("DISConnected from chat!")
            } else {
                Timber.e("ERROR WHEN DISCONNECTING %s", e.errorString)
            }
        } else if (request.type == MegaChatRequest.TYPE_LOGOUT) {
            Timber.e("onRequestFinish(CHAT): %d", MegaChatRequest.TYPE_LOGOUT)
            if (e.errorCode != MegaError.API_OK) {
                Timber.e("MegaChatRequest.TYPE_LOGOUT:ERROR")
            }
            (application as MegaApplication).disableMegaChatApi()
            loggingSettings.resetLoggerSDK()
        } else if (request.type == MegaChatRequest.TYPE_ARCHIVE_CHATROOM) {
            val chatHandle: Long = request.chatHandle
            val chat: MegaChatRoom = megaChatApi.getChatRoom(chatHandle)
            var chatTitle = ChatUtil.getTitleChat(chat)
            if (chatTitle == null) {
                chatTitle = ""
            } else if (chatTitle.isNotEmpty() && chatTitle.length > 60) {
                chatTitle = chatTitle.substring(0, 59) + "..."
            }
            if (chatTitle.isNotEmpty() && chat.isGroup && !chat.hasCustomTitle()) {
                chatTitle = "\"" + chatTitle + "\""
            }
            if (e.errorCode == MegaChatError.ERROR_OK) {
                if (request.flag) {
                    Timber.d("Chat archived")
                    showSnackbar(
                        Constants.SNACKBAR_TYPE,
                        getString(R.string.success_archive_chat, chatTitle),
                        -1
                    )
                } else {
                    Timber.d("Chat unarchived")
                    showSnackbar(
                        Constants.SNACKBAR_TYPE,
                        getString(R.string.success_unarchive_chat, chatTitle),
                        -1
                    )
                }
            } else {
                if (request.flag) {
                    Timber.e("ERROR WHEN ARCHIVING CHAT %s", e.errorString)
                    showSnackbar(
                        Constants.SNACKBAR_TYPE,
                        getString(R.string.error_archive_chat, chatTitle),
                        -1
                    )
                } else {
                    Timber.e("ERROR WHEN UNARCHIVING CHAT %s", e.errorString)
                    showSnackbar(
                        Constants.SNACKBAR_TYPE,
                        getString(R.string.error_unarchive_chat, chatTitle),
                        -1
                    )
                }
            }
        } else if (request.type == MegaChatRequest.TYPE_SET_LAST_GREEN_VISIBLE) {
            if (e.errorCode == MegaChatError.ERROR_OK) {
                Timber.d("MegaChatRequest.TYPE_SET_LAST_GREEN_VISIBLE: %s", request.flag)
            } else {
                Timber.e("MegaChatRequest.TYPE_SET_LAST_GREEN_VISIBLE:error: %d", e.errorType)
            }
        }
    }

    override fun onRequestTemporaryError(
        api: MegaChatApiJava,
        request: MegaChatRequest,
        e: MegaChatError,
    ) {
    }

    fun onRequestFinishCreateChat(errorCode: Int, chatHandle: Long) {
        if (errorCode == MegaChatError.ERROR_OK) {
            Timber.d("Chat CREATED.")

            //Update chat view
            Timber.d("Open new chat: %s", chatHandle)
            val chatIntent = Intent(this, ChatActivity::class.java)
            chatIntent.action = Constants.ACTION_CHAT_SHOW_MESSAGES
            chatIntent.putExtra(Constants.CHAT_ID, chatHandle)
            this.startActivity(chatIntent)
        } else {
            Timber.e("ERROR WHEN CREATING CHAT %d", errorCode)
            showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.create_chat_error), -1)
        }
    }

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestStart: %s", request.requestString)
    }

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestUpdate: %s", request.requestString)
    }

    @SuppressLint("NewApi")
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.d("onRequestFinish: %s_%d", request.requestString, e.errorCode)
        when (request.type) {
            MegaRequest.TYPE_LOGOUT -> {
                Timber.d("onRequestFinish: %s", MegaRequest.TYPE_LOGOUT)
                if (e.errorCode == MegaError.API_OK) {
                    Timber.d("onRequestFinish:OK:%s", MegaRequest.TYPE_LOGOUT)
                    Timber.d("END logout sdk request - wait chat logout")
                } else if (e.errorCode != MegaError.API_ESID) {
                    showSnackbar(
                        Constants.SNACKBAR_TYPE,
                        getString(R.string.general_text_error),
                        -1
                    )
                }
            }

            MegaRequest.TYPE_SET_ATTR_USER -> {
                if (request.paramType == MegaApiJava.USER_ATTR_PWD_REMINDER) {
                    Timber.d("MK exported - USER_ATTR_PWD_REMINDER finished")
                    if (e.errorCode == MegaError.API_OK || e.errorCode == MegaError.API_ENOENT) {
                        Timber.d("New value of attribute USER_ATTR_PWD_REMINDER: %s", request.text)
                    }
                }
            }

            MegaRequest.TYPE_GET_ATTR_USER -> {
                if (request.paramType == MegaApiJava.USER_ATTR_GEOLOCATION) {
                    if (e.errorCode == MegaError.API_OK) {
                        Timber.d("Attribute USER_ATTR_GEOLOCATION enabled")
                        MegaApplication.isEnabledGeoLocation = true
                    } else {
                        Timber.d("Attribute USER_ATTR_GEOLOCATION disabled")
                        MegaApplication.isEnabledGeoLocation = false
                    }
                }
            }

            MegaRequest.TYPE_GET_CANCEL_LINK -> {
                Timber.d("TYPE_GET_CANCEL_LINK")
                Util.hideKeyboard(this, 0)
                if (e.errorCode == MegaError.API_OK) {
                    Timber.d("Cancellation link received!")
                    Util.showAlert(
                        this,
                        getString(R.string.email_verification_text),
                        getString(R.string.email_verification_title)
                    )
                } else {
                    Timber.e(
                        "Error when asking for the cancellation link: %s___%s",
                        e.errorCode,
                        e.errorString
                    )
                    Util.showAlert(
                        this,
                        getString(R.string.general_text_error),
                        getString(R.string.general_error_word)
                    )
                }
            }

            MegaRequest.TYPE_REMOVE_CONTACT -> {
                when (e.errorCode) {
                    MegaError.API_OK -> {
                        showSnackbar(
                            Constants.SNACKBAR_TYPE,
                            getString(R.string.context_contact_removed),
                            -1
                        )
                    }

                    MegaError.API_EMASTERONLY -> {
                        showSnackbar(
                            Constants.SNACKBAR_TYPE,
                            getString(R.string.error_remove_business_contact, request.email),
                            -1
                        )
                    }

                    else -> {
                        Timber.e("Error deleting contact")
                        showSnackbar(
                            Constants.SNACKBAR_TYPE,
                            getString(R.string.context_contact_not_removed),
                            -1
                        )
                    }
                }
            }

            MegaRequest.TYPE_INVITE_CONTACT -> {
                Timber.d("MegaRequest.TYPE_INVITE_CONTACT finished: %s", request.number)
                dismissAlertDialogIfExists(statusDialog)
                if (request.number == MegaContactRequest.INVITE_ACTION_REMIND.toLong()) {
                    showSnackbar(
                        Constants.SNACKBAR_TYPE,
                        getString(R.string.context_contact_invitation_resent),
                        -1
                    )
                } else {
                    if (e.errorCode == MegaError.API_OK) {
                        Timber.d("OK INVITE CONTACT: %s", request.email)
                        if (request.number == MegaContactRequest.INVITE_ACTION_ADD.toLong()) {
                            showSnackbar(
                                Constants.SNACKBAR_TYPE,
                                getString(R.string.context_contact_request_sent, request.email),
                                -1
                            )
                        } else if (request.number == MegaContactRequest.INVITE_ACTION_DELETE.toLong()) {
                            showSnackbar(
                                Constants.SNACKBAR_TYPE,
                                getString(R.string.context_contact_invitation_deleted),
                                -1
                            )
                        }
                    } else {
                        Timber.e("ERROR invite contact: %s___%s", e.errorCode, e.errorString)
                        if (e.errorCode == MegaError.API_EEXIST) {
                            var found = false
                            val outgoingContactRequests =
                                megaApi.outgoingContactRequests
                            if (outgoingContactRequests?.asSequence()?.map { it.targetEmail }
                                    ?.contains(request.email) == true) {
                                found = true
                            }
                            if (found) {
                                showSnackbar(
                                    Constants.SNACKBAR_TYPE,
                                    getString(
                                        R.string.invite_not_sent_already_sent,
                                        request.email
                                    ),
                                    -1
                                )
                            } else {
                                showSnackbar(
                                    Constants.SNACKBAR_TYPE,
                                    getString(
                                        R.string.context_contact_already_exists,
                                        request.email
                                    ),
                                    -1
                                )
                            }
                        } else if (request.number == MegaContactRequest.INVITE_ACTION_ADD.toLong() && e.errorCode == MegaError.API_EARGS) {
                            showSnackbar(
                                Constants.SNACKBAR_TYPE,
                                getString(R.string.error_own_email_as_contact),
                                -1
                            )
                        } else {
                            showSnackbar(
                                Constants.SNACKBAR_TYPE,
                                getString(R.string.general_error),
                                -1
                            )
                        }
                    }
                }
            }

            MegaRequest.TYPE_CREATE_FOLDER -> {
                dismissAlertDialogIfExists(statusDialog)
                if (e.errorCode == MegaError.API_OK) {
                    showSnackbar(
                        Constants.SNACKBAR_TYPE,
                        getString(R.string.context_folder_created),
                        -1
                    )
                    val folderNode =
                        megaApi.getNodeByHandle(request.nodeHandle) ?: return
                    if (drawerItem === DrawerItem.CLOUD_DRIVE) {
                        if (isCloudAdded) {
                            fileBrowserViewModel.setBrowserParentHandle(folderNode.handle)
                        }
                    } else if (drawerItem === DrawerItem.SHARED_ITEMS) {
                        when (tabItemShares) {
                            SharesTab.INCOMING_TAB -> if (isIncomingAdded) {
                                incomingSharesFragment?.navigateToFolder(folderNode)
                            }

                            SharesTab.OUTGOING_TAB -> if (isOutgoingAdded) {
                                outgoingSharesFragment?.navigateToFolder(folderNode)
                            }

                            SharesTab.LINKS_TAB -> if (isLinksAdded) {
                                linksFragment?.navigateToFolder(folderNode)
                            }

                            else -> {}
                        }
                    }
                } else {
                    Timber.e("TYPE_CREATE_FOLDER ERROR: %s___%s", e.errorCode, e.errorString)
                    showSnackbar(
                        Constants.SNACKBAR_TYPE,
                        getString(R.string.context_folder_no_created),
                        -1
                    )
                }
            }

            MegaRequest.TYPE_SUBMIT_PURCHASE_RECEIPT -> {
                if (e.errorCode == MegaError.API_OK) {
                    Timber.d("PURCHASE CORRECT!")
                    drawerItem = DrawerItem.CLOUD_DRIVE
                    selectDrawerItem(drawerItem)
                } else {
                    Timber.e("PURCHASE WRONG: %s (%d)", e.errorString, e.errorCode)
                }
            }

            MegaRequest.TYPE_REGISTER_PUSH_NOTIFICATION -> {
                if (e.errorCode == MegaError.API_OK) {
                    Timber.d("FCM OK TOKEN MegaRequest.TYPE_REGISTER_PUSH_NOTIFICATION")
                } else {
                    Timber.e(
                        "FCM ERROR TOKEN TYPE_REGISTER_PUSH_NOTIFICATION: %d__%s",
                        e.errorCode,
                        e.errorString
                    )
                }
            }

            MegaRequest.TYPE_FOLDER_INFO -> {
                if (e.errorCode == MegaError.API_OK) {
                    val info: MegaFolderInfo = request.megaFolderInfo
                    val numVersions: Int = info.numVersions
                    Timber.d("Num versions: %s", numVersions)
                    val previousVersions: Long = info.versionsSize
                    Timber.d("Previous versions: %s", previousVersions)
                    myAccountInfo.numVersions = numVersions
                    myAccountInfo.previousVersionsSize = previousVersions
                } else {
                    Timber.e("ERROR requesting version info of the account")
                }
            }

            MegaRequest.TYPE_REMOVE -> {
                if (versionsToRemove > 0) {
                    Timber.d("Remove request finished")
                    if (e.errorCode == MegaError.API_OK) {
                        versionsRemoved++
                    } else {
                        errorVersionRemove++
                    }
                    if (versionsRemoved + errorVersionRemove == versionsToRemove) {
                        if (versionsRemoved == versionsToRemove) {
                            showSnackbar(
                                Constants.SNACKBAR_TYPE,
                                getString(R.string.version_history_deleted),
                                -1
                            )
                        } else {
                            showSnackbar(
                                Constants.SNACKBAR_TYPE,
                                getString(R.string.version_history_deleted_erroneously)
                                        + "\n" + resources.getQuantityString(
                                    R.plurals.versions_deleted_succesfully,
                                    versionsRemoved,
                                    versionsRemoved
                                )
                                        + "\n" + resources.getQuantityString(
                                    R.plurals.versions_not_deleted,
                                    errorVersionRemove,
                                    errorVersionRemove
                                ),
                                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                            )
                        }
                        versionsToRemove = 0
                        versionsRemoved = 0
                        errorVersionRemove = 0
                    }
                } else {
                    Timber.d("Remove request finished")
                    when (e.errorCode) {
                        MegaError.API_OK -> {
                            finish()
                        }

                        MegaError.API_EMASTERONLY -> {
                            showSnackbar(Constants.SNACKBAR_TYPE, e.errorString, -1)
                        }

                        else -> {
                            showSnackbar(
                                Constants.SNACKBAR_TYPE,
                                getString(R.string.context_no_removed),
                                -1
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onRequestTemporaryError(
        api: MegaApiJava, request: MegaRequest,
        e: MegaError,
    ) {
        Timber.w("onRequestTemporaryError: ${request.requestString}__${e.errorCode}__${e.errorString}")
    }

    /**
     * Open location based on where parent node is located
     *
     * @param nodeHandle          parent node handle
     * @param childNodeHandleList list of child nodes handles if comes from notfication about new added nodes to shared folder
     */
    private fun openLocation(nodeHandle: Long, childNodeHandleList: LongArray?) {
        Timber.d("Node handle: %s", nodeHandle)
        val node = megaApi.getNodeByHandle(nodeHandle) ?: return
        comesFromNotifications = true
        comesFromNotificationHandle = nodeHandle
        comesFromNotificationChildNodeHandleList = childNodeHandleList
        val parent: MegaNode? = nodeController.getParent(node)
        when (parent?.handle) {
            megaApi.rootNode?.handle -> {
                //Cloud Drive
                drawerItem = DrawerItem.CLOUD_DRIVE
                openFolderRefresh = true
                comesFromNotificationHandleSaved = fileBrowserViewModel.state().fileBrowserHandle
                fileBrowserViewModel.setBrowserParentHandle(nodeHandle)
                selectDrawerItem(drawerItem)
            }

            megaApi.rubbishNode?.handle -> {
                //Rubbish
                drawerItem = DrawerItem.RUBBISH_BIN
                openFolderRefresh = true
                comesFromNotificationHandleSaved = rubbishBinViewModel.state().rubbishBinHandle
                rubbishBinViewModel.setRubbishBinHandle(nodeHandle)
                selectDrawerItem(drawerItem)
            }

            megaApi.inboxNode?.handle -> {
                // Backups
                drawerItem = DrawerItem.BACKUPS
                openFolderRefresh = true
                comesFromNotificationHandleSaved = backupsViewModel.state().backupsHandle
                backupsViewModel.updateBackupsHandle(nodeHandle)
                selectDrawerItem(drawerItem)
            }

            else -> {
                //Incoming Shares
                drawerItem = DrawerItem.SHARED_ITEMS
                comesFromNotificationSharedIndex =
                    SharesTab.fromPosition(viewPagerShares.currentItem)
                viewModel.setSharesTab(SharesTab.INCOMING_TAB)
                comesFromNotificationDeepBrowserTreeIncoming =
                    incomingSharesViewModel.state().incomingTreeDepth
                comesFromNotificationHandleSaved = incomingSharesViewModel.state().incomingHandle
                if (parent != null) {
                    val depth: Int = MegaApiUtils.calculateDeepBrowserTreeIncoming(node, this)
                    incomingSharesViewModel.setIncomingTreeDepth(depth, nodeHandle)
                    comesFromNotificationsLevel = depth
                }
                openFolderRefresh = true
                selectDrawerItem(drawerItem)
            }
        }
    }

    fun onNodesCloudDriveUpdate() {
        Timber.d("onNodesCloudDriveUpdate")
        refreshRubbishBin()
        pagerOfflineFragment?.refreshNodes()
        refreshCloudDrive()
    }

    fun onNodesBackupsUpdate() {
        backupsFragment =
            supportFragmentManager.findFragmentByTag(FragmentTag.BACKUPS.tag) as? BackupsFragment
        backupsFragment?.hideMultipleSelect()
        backupsViewModel.refreshBackupsNodes()
    }

    fun onNodesSearchUpdate() {
        searchViewModel.setTextSubmitted(true)
        searchFragment?.refresh()
    }

    private fun refreshIncomingShares() {
        incomingSharesViewModel.refreshIncomingSharesNode()
    }

    private fun refreshOutgoingShares() {
        outgoingSharesViewModel.refreshOutgoingSharesNode()
    }

    private fun refreshLinks() {
        legacyLinksViewModel.refreshLinksSharesNode()
    }

    fun refreshBackupsList() {
        backupsFragment =
            supportFragmentManager.findFragmentByTag(FragmentTag.BACKUPS.tag) as? BackupsFragment
        backupsFragment?.invalidateRecyclerView()
    }

    fun refreshSharesFragments() {
        refreshOutgoingShares()
        refreshIncomingShares()
        refreshLinks()
    }

    private fun onNodesSharedUpdate() {
        Timber.d("onNodesSharedUpdate")
        refreshSharesFragments()
        refreshSharesPageAdapter()
    }

    fun setFirstLogin(isFirst: Boolean) {
        viewModel.setIsFirstLogin(isFirst)
    }

    val deepBrowserTreeIncoming: Int
        get() = incomingSharesViewModel.state().incomingTreeDepth

    fun setDeepBrowserTreeIncoming(deep: Int, parentHandle: Long?) {
        parentHandle?.let { incomingSharesViewModel.setIncomingTreeDepth(deep, it) }
    }

    val deepBrowserTreeOutgoing: Int
        get() = outgoingSharesViewModel.state().outgoingTreeDepth
    val deepBrowserTreeLinks: Int
        get() = legacyLinksViewModel.state().linksTreeDepth
    var tabItemShares: SharesTab
        get() = viewPagerShares.currentItem.takeUnless { it == -1 }
            ?.let { SharesTab.fromPosition(it) } ?: SharesTab.NONE
        set(index) {
            viewPagerShares.currentItem = index.position
        }

    fun hideFabButton() {
        initFabButtonShow = false
        fabButton.hide()
    }

    /**
     * Hides the fabButton icon when scrolling.
     */
    private fun hideFabButtonWhenScrolling() {
        fabButton.hide()
    }

    /**
     * Shows the fabButton icon.
     */
    private fun showFabButtonAfterScrolling() {
        fabButton.show()
    }

    /**
     * Updates the fabButton icon and shows it.
     */
    private fun updateFabAndShow() {
        fabButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_add_white))
        fabButton.show()
    }

    /**
     * Shows or hides the fabButton depending on the current section.
     */
    fun showFabButton() = lifecycleScope.launch {
        initFabButtonShow = true
        if (drawerItem == null) {
            return@launch
        }
        when (drawerItem) {
            DrawerItem.CLOUD_DRIVE -> if (!isInMDMode) {
                updateFabAndShow()
            }

            DrawerItem.SHARED_ITEMS -> when (tabItemShares) {
                SharesTab.INCOMING_TAB -> {
                    if (!isIncomingAdded) return@launch
                    val parentNodeInSF: MegaNode? = withContext(ioDispatcher) {
                        megaApi.getNodeByHandle(incomingSharesViewModel.state().incomingHandle)
                    }
                    if (incomingSharesViewModel.state().incomingTreeDepth <= 0 || parentNodeInSF == null) {
                        hideFabButton()
                        return@launch
                    }
                    when (megaApi.getAccess(parentNodeInSF)) {
                        MegaShare.ACCESS_OWNER, MegaShare.ACCESS_READWRITE, MegaShare.ACCESS_FULL -> updateFabAndShow()
                        MegaShare.ACCESS_READ -> hideFabButton()
                    }
                }

                SharesTab.OUTGOING_TAB -> {
                    if (!isOutgoingAdded) return@launch

                    // If the user is in the main page of Outgoing Shares, hide the Fab Button
                    if (outgoingSharesViewModel.state().outgoingTreeDepth <= 0) {
                        hideFabButton()
                    } else {
                        // Otherwise, check if the current parent node of the Outgoing Shares section is a Backup folder or not.
                        // Hide the Fab button if it is a Backup folder. Otherwise, show the Fab button.
                        val outgoingParentNode = withContext(ioDispatcher) {
                            megaApi.getNodeByHandle(outgoingSharesViewModel.state().outgoingHandle)
                        }
                        if (outgoingParentNode != null && megaApi.isInInbox(outgoingParentNode)) {
                            hideFabButton()
                        } else {
                            updateFabAndShow()
                        }
                    }
                }

                SharesTab.LINKS_TAB -> {
                    if (!isLinksAdded) return@launch

                    // If the user is in the main page of Links, hide the Fab Button
                    if (legacyLinksViewModel.state().linksTreeDepth <= 0) {
                        hideFabButton()
                    } else {
                        // Otherwise, check if the current parent node of the Links section is a Backup folder or not.
                        // Hide the Fab button if it is a Backup folder. Otherwise, show the Fab button.
                        val linksParentNode = withContext(ioDispatcher) {
                            megaApi.getNodeByHandle(legacyLinksViewModel.state().linksHandle)
                        }
                        if (linksParentNode != null && megaApi.isInInbox(linksParentNode)) {
                            hideFabButton()
                        } else {
                            updateFabAndShow()
                        }
                    }
                }

                else -> hideFabButton()
            }

            else -> hideFabButton()
        }
    }

    private fun onChatListItemUpdate(item: MegaChatListItem?) {
        if (item != null) {
            Timber.d("Chat ID:%s", item.chatId)
            if (item.isPreview) {
                return
            }
        } else {
            Timber.w("Item NULL")
            return
        }
        if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_UNREAD_COUNT)) {
            Timber.d("Change unread count: %s", item.unreadCount)
            setChatBadge()
            viewModel.checkNumUnreadUserAlerts(UnreadUserAlertsCheckType.NAVIGATION_TOOLBAR_ICON)
        }
    }

    private fun onChatOnlineStatusUpdate(userHandle: Long, status: Int, inProgress: Boolean) {
        Timber.d("Status: %d, In Progress: %s", status, inProgress)
        if (chatsFragment == null) {
            chatTabsFragment =
                supportFragmentManager.findFragmentByTag(FragmentTag.RECENT_CHAT.tag) as? ChatTabsFragment
        }
    }

    private fun onChatConnectionStateUpdate(chatId: Long, newState: Int) {
        Timber.d("Chat ID: %d, New state: %d", chatId, newState)
    }

    fun copyError() {
        try {
            dismissAlertDialogIfExists(statusDialog)
            showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.context_no_copied), -1)
        } catch (ex: Exception) {
            Timber.w(ex)
        }
    }

    private fun setDrawerLockMode(locked: Boolean) {
        if (locked) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        } else {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        }
    }

    /**
     * This method is used to change the elevation of the AppBarLayout for some reason
     *
     * @param withElevation true if need elevation, false otherwise
     * @param cause         for what cause adding/removing elevation. Only if mElevationCause(cause bitmap)
     * is zero will the elevation being eliminated
     */
    @JvmOverloads
    fun changeAppBarElevation(withElevation: Boolean, cause: Int = ELEVATION_SCROLL) {
        if (withElevation) {
            mElevationCause = mElevationCause or cause
        } else if (mElevationCause and cause > 0) {
            mElevationCause = mElevationCause xor cause
        }

        // In landscape mode, if no call in progress layout ("Tap to return call"), then don't show elevation
        if (mElevationCause == ELEVATION_CALL_IN_PROGRESS && callInProgressLayout.visibility != View.VISIBLE) return

        // If any Tablayout is visible, set the background of the toolbar to transparent (or its elevation
        // overlay won't be correctly set via AppBarLayout) and then set the elevation of AppBarLayout,
        // in this way, both Toolbar and TabLayout would have expected elevation overlay.
        // If TabLayout is invisible, directly set toolbar's color for the elevation effect. Set AppBarLayout
        // elevation in this case, a crack would appear between toolbar and ChatRecentFragment's Appbarlayout, for example.
        val elevation: Float = resources.getDimension(R.dimen.toolbar_elevation)
        val toolbarElevationColor = ColorUtils.getColorForElevation(this, elevation)
        val transparentColor = ContextCompat.getColor(this, android.R.color.transparent)
        val onlySetToolbar = Util.isDarkMode(this) && !mShowAnyTabLayout
        if (mElevationCause > 0) {
            if (onlySetToolbar) {
                toolbar.setBackgroundColor(toolbarElevationColor)
            } else {
                toolbar.setBackgroundColor(transparentColor)
                appBarLayout.elevation = elevation
            }
        } else {
            toolbar.setBackgroundColor(transparentColor)
            appBarLayout.elevation = 0f
        }
        ColorUtils.changeStatusBarColorForElevation(
            this,
            mElevationCause > 0 && !isInMainHomePage
        )
    }

    private fun setChatBadge() {
        val numberUnread = megaChatApi.unreadChats
        if (numberUnread == 0) {
            chatBadge.visibility = View.GONE
        } else {
            chatBadge.visibility = View.VISIBLE
            if (numberUnread > 9) {
                chatBadge.findViewById<TextView>(R.id.chat_badge_text).text = "9+"
            } else {
                chatBadge.findViewById<TextView>(R.id.chat_badge_text).text =
                    "$numberUnread"
            }
        }
    }

    private fun setCallBadge() {
        if (!viewModel.isConnected || megaChatApi.numCalls <= 0 || megaChatApi.numCalls == 1 && CallUtil.participatingInACall()) {
            callBadge.visibility = View.GONE
            return
        }
        callBadge.visibility = View.VISIBLE
    }


    /**
     * Shows all the content of bottom view.
     */
    private fun showBottomView() {
        val bottomView: LinearLayout = findViewById(R.id.container_bottom)
        if (isInImagesPage) {
            return
        }
        bottomView.animate().translationY(0f).setDuration(175)
            .withStartAction { bottomView.visibility = View.VISIBLE }
            .start()
    }

    fun showHideBottomNavigationView(hide: Boolean) {
        with(bottomNavigationView) {
            val params = CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            val height: Int =
                resources.getDimensionPixelSize(R.dimen.bottom_navigation_view_height)
            if (hide && visibility == View.VISIBLE) {
                updateMiniAudioPlayerVisibility(false)
                params.setMargins(0, 0, 0, 0)
                fragmentLayout.layoutParams = params
                animate().translationY(height.toFloat())
                    .setDuration(Constants.ANIMATION_DURATION)
                    .withEndAction { bottomNavigationView.visibility = View.GONE }
                    .start()
            } else if (!hide && visibility == View.GONE) {
                animate().translationY(0f).setDuration(Constants.ANIMATION_DURATION)
                    .withStartAction { visibility = View.VISIBLE }
                    .withEndAction {
                        updateMiniAudioPlayerVisibility(true)
                        params.setMargins(0, 0, 0, height)
                        fragmentLayout.layoutParams = params
                    }.start()
            }
            updateTransfersWidgetPosition(hide)
        }
    }

    private fun markNotificationsSeen(fromAndroidNotification: Boolean) {
        Timber.d("fromAndroidNotification: %s", fromAndroidNotification)
        if (fromAndroidNotification) {
            megaApi.acknowledgeUserAlerts()
        } else {
            if (drawerItem === DrawerItem.NOTIFICATIONS && activityLifecycleHandler.isActivityVisible) {
                megaApi.acknowledgeUserAlerts()
            }
        }
    }

    fun showKeyboardForSearch() {
        if (searchView != null) {
            searchView?.findViewById<View>(androidx.appcompat.R.id.search_src_text)
                ?.let { showKeyboardDelayed(it) }
            searchView?.requestFocus()
        }
    }


    fun hideKeyboardSearch() {
        Util.hideKeyboard(this)
        if (searchView != null) {
            searchView?.clearFocus()
        }
    }

    fun openSearchView() {
        if (searchMenuItem != null) {
            searchMenuItem?.expandActionView()
            if (searchView != null) {
                searchView?.setQuery(searchViewModel.state.value.searchQuery, false)
            }
        }
    }

    fun clearSearchViewFocus() {
        if (searchView != null) {
            searchView?.clearFocus()
        }
    }

    fun requestSearchViewFocus() {
        if (searchView == null || searchViewModel.state.value.textSubmitted) {
            return
        }
        searchView?.isIconified = false
    }

    fun openSearchFolder(node: MegaNode) {
        when (drawerItem) {
            DrawerItem.HOMEPAGE -> {
                // Redirect to Cloud drive.
                selectDrawerItem(DrawerItem.CLOUD_DRIVE)
                fileBrowserViewModel.setBrowserParentHandle(node.handle)
                refreshFragment(FragmentTag.CLOUD_DRIVE.tag)
            }

            DrawerItem.CLOUD_DRIVE -> {
                fileBrowserViewModel.setBrowserParentHandle(node.handle)
                refreshFragment(FragmentTag.CLOUD_DRIVE.tag)
            }

            DrawerItem.SHARED_ITEMS -> {
                if (tabItemShares === SharesTab.INCOMING_TAB) {
                    incomingSharesViewModel.increaseIncomingTreeDepth(node.handle)
                } else if (tabItemShares === SharesTab.OUTGOING_TAB) {
                    outgoingSharesViewModel.increaseOutgoingTreeDepth(node.handle)
                } else if (tabItemShares === SharesTab.LINKS_TAB) {
                    legacyLinksViewModel.increaseLinksTreeDepth(node.handle)
                }
                refreshSharesPageAdapter()
            }

            DrawerItem.BACKUPS -> {
                backupsViewModel.updateBackupsHandle(node.handle)
                refreshFragment(FragmentTag.BACKUPS.tag)
            }

            else -> {}
        }
    }

    fun closeSearchView() {
        searchViewModel.setTextSubmitted(true)
        if (searchMenuItem?.isActionViewExpanded == true) {
            searchMenuItem?.collapseActionView()
        }
    }

    fun setTextSubmitted() {
        if (searchView != null) {
            if (!searchViewModel.isSearchQueryValid()) return
            searchView?.setQuery(searchViewModel.state.value.searchQuery, true)
        }
    }

    val isSearchOpen: Boolean
        get() = searchViewModel.state.value.searchQuery != null && searchExpand

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        // Determine which lifecycle or system event was raised.
        //we will stop creating thumbnails while the phone is running low on memory to prevent OOM
        Timber.d("Level: %s", level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            Timber.w("Low memory")
            ThumbnailUtils.isDeviceMemoryLow = true
        } else {
            Timber.d("Memory OK")
            ThumbnailUtils.isDeviceMemoryLow = false
        }
    }

    private fun setSearchDrawerItem() {
        if (drawerItem === DrawerItem.SEARCH) return
        drawerItem?.let { searchViewModel.setSearchDrawerItem(it) }
        searchViewModel.setSearchSharedTab(tabItemShares)
        drawerItem = DrawerItem.SEARCH
    }

    /**
     * This method sets "Tap to return to call" banner when there is a call in progress
     * and it is in Cloud Drive section, Recent section, Incoming section, Outgoing section or in the chats list.
     */
    private fun setCallWidget() {
        setCallBadge()
        if (drawerItem === DrawerItem.SEARCH || drawerItem === DrawerItem.TRANSFERS || drawerItem === DrawerItem.NOTIFICATIONS || drawerItem === DrawerItem.HOMEPAGE || !Util.isScreenInPortrait(
                this
            )
        ) {
            CallUtil.hideCallWidget(this, callInProgressChrono, callInProgressLayout)
            return
        }
        CallUtil.showCallLayout(
            this,
            callInProgressLayout,
            callInProgressChrono,
            callInProgressText
        )
    }

    fun homepageToSearch() {
        hideItemsWhenSearchSelected()
        searchMenuItem?.expandActionView()
    }

    fun setSearchQuery(searchQuery: String) {
        searchViewModel.setSearchQuery(searchQuery)
        searchView?.setQuery(searchQuery, false)
    }

    private fun getSearchFragment(): SearchFragment? {
        return (supportFragmentManager.findFragmentByTag(FragmentTag.SEARCH.tag) as? SearchFragment).also {
            searchFragment = it
        }
    }

    /**
     * Removes a completed transfer from Completed tab in Transfers section.
     *
     * @param transfer       the completed transfer to remove
     * @param isRemovedCache If ture, remove cache file, otherwise doesn't remove cache file
     */
    fun removeCompletedTransfer(transfer: CompletedTransfer, isRemovedCache: Boolean) {
        transfersViewModel.completedTransferRemoved(transfer, isRemovedCache)
    }

    /**
     * Retries a transfer that finished wrongly.
     *
     * @param transfer the transfer to retry
     */
    fun retryTransfer(transfer: CompletedTransfer) {
        when (transfer.type) {
            MegaTransfer.TYPE_DOWNLOAD -> {
                val node = megaApi.getNodeByHandle(transfer.handle) ?: return
                when (transfer.isOffline) {
                    true -> {
                        val offlineFile = File(transfer.originalPath)
                        OfflineUtils.saveOffline(
                            offlineFile.parentFile,
                            node,
                            this@ManagerActivity
                        )
                    }

                    false -> {
                        downloadNodeUseCase.download(this, node, transfer.path)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                { Timber.d("Transfer retried: ${node?.handle}") },
                                { throwable: Throwable? ->
                                    Timber.e(throwable, "Retry transfer failed.")
                                }
                            )
                            .addTo(composite)
                    }

                    null -> {
                        Timber.d("Unable to retrieve transfer isOffline value")
                    }
                }
            }

            MegaTransfer.TYPE_UPLOAD -> {
                PermissionUtils.checkNotificationsPermission(this)
                val file = File(transfer.originalPath)
                uploadUseCase.upload(this, file, transfer.parentHandle)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { Timber.d("Transfer retried.") },
                        { t: Throwable? -> Timber.e(t) })
                    .addTo(composite)
            }

            else -> {
                Timber.d("Unable to retrieve transfer type value")
            }
        }

        removeCompletedTransfer(transfer, false)
    }

    /**
     * Opens a location of a transfer.
     *
     * @param transfer the transfer to open its location
     */
    fun openTransferLocation(transfer: CompletedTransfer) {
        when (transfer.type) {
            MegaTransfer.TYPE_DOWNLOAD -> {
                when (transfer.isOffline) {
                    true -> {
                        selectDrawerItem(DrawerItem.HOMEPAGE)
                        openFullscreenOfflineFragment(
                            OfflineUtils.removeInitialOfflinePath(
                                transfer.path,
                                this
                            ) + Constants.SEPARATOR
                        )
                    }

                    false -> {
                        val file = transfer.path?.let { File(it) }
                        if (!FileUtil.isFileAvailable(file)) {
                            showSnackbar(
                                Constants.SNACKBAR_TYPE,
                                getString(R.string.location_not_exist),
                                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                            )
                            return
                        }
                        val intent = Intent(this, FileStorageActivity::class.java)
                        intent.action = FileStorageActivity.Mode.BROWSE_FILES.action
                        intent.putExtra(FileStorageActivity.EXTRA_PATH, transfer.path)
                        startActivity(intent)
                    }

                    null -> {
                        Timber.d("Unable to retrieve transfer isOffline value")
                    }
                }
            }

            MegaTransfer.TYPE_UPLOAD -> {
                transfer.handle
                    ?.let { megaApi.getNodeByHandle(it) }
                    ?.let { viewNodeInFolder(it) }
            }

            else -> {
                Timber.d("Unable to retrieve transfer type")
            }
        }
    }

    /**
     * Opens the location of a node.
     *
     * @param node the node to open its location
     */
    fun viewNodeInFolder(node: MegaNode) {
        val parentNode = megaApi.getRootParentNode(node)
        viewInFolderNode = node
        if (parentNode.handle == megaApi.rootNode?.handle) {
            fileBrowserViewModel.setBrowserParentHandle(node.parentHandle)
            refreshFragment(FragmentTag.CLOUD_DRIVE.tag)
            selectDrawerItem(DrawerItem.CLOUD_DRIVE)
        } else if (parentNode.handle == megaApi.rubbishNode?.handle) {
            rubbishBinViewModel.setRubbishBinHandle(node.parentHandle)
            refreshFragment(FragmentTag.RUBBISH_BIN.tag)
            selectDrawerItem(DrawerItem.RUBBISH_BIN)
        } else if (parentNode.isInShare) {
            incomingSharesViewModel.setIncomingTreeDepth(
                MegaApiUtils.calculateDeepBrowserTreeIncoming(
                    megaApi.getParentNode(node),
                    this
                ), node.parentHandle
            )
            sharesPageAdapter.refreshFragment(SharesTab.INCOMING_TAB.position)
            viewModel.setSharesTab(SharesTab.INCOMING_TAB)
            viewPagerShares.currentItem = viewModel.state().sharesTab.position
            refreshSharesPageAdapter()
            selectDrawerItem(DrawerItem.SHARED_ITEMS)
        } else if (parentNode.handle == megaApi.inboxNode?.handle) {
            refreshFragment(FragmentTag.BACKUPS.tag)
            selectDrawerItem(DrawerItem.BACKUPS)
        }
    }

    /**
     * Updates the position of the transfers widget.
     *
     * @param bNVHidden true if the bottom navigation view is hidden, false otherwise
     */
    private fun updateTransfersWidgetPosition(bNVHidden: Boolean) {
        val transfersWidgetLayout: View =
            findViewById(R.id.transfers_widget_layout)
                ?: return
        val params = transfersWidgetLayout.layoutParams as LinearLayout.LayoutParams
        params.gravity = Gravity.END
        if (!bNVHidden && isInMainHomePage || drawerItem == DrawerItem.SYNC) {
            params.bottomMargin = Util.dp2px(TRANSFER_WIDGET_MARGIN_BOTTOM.toFloat(), outMetrics)
        } else {
            params.bottomMargin = 0
        }
        transfersWidgetLayout.layoutParams = params
    }

    /**
     * Updates values of TransfersManagement object after the activity comes from background.
     */
    private fun checkTransferOverQuotaOnResume() {
        transfersManagement.isOnTransfersSection = drawerItem === DrawerItem.TRANSFERS
        if (transfersManagement.isTransferOverQuotaNotificationShown) {
            transfersManagement.isTransferOverQuotaBannerShown = true
            transfersManagement.isTransferOverQuotaNotificationShown = false
        }
    }

    /**
     * Retry a single transfer.
     *
     * @param transfer CompletedTransfer to retry.
     */
    fun retrySingleTransfer(transfer: CompletedTransfer) {
        retryTransfer(transfer)
    }

    private fun getRubbishBinComposeFragment(): RubbishBinComposeFragment? {
        return (supportFragmentManager.findFragmentByTag(FragmentTag.RUBBISH_BIN_COMPOSE.tag) as? RubbishBinComposeFragment).also {
            rubbishBinComposeFragment = it
        }
    }

    private fun getPhotosFragment(): PhotosFragment? {
        return (supportFragmentManager
            .findFragmentByTag(FragmentTag.PHOTOS.tag) as? PhotosFragment).also {
            photosFragment = it
        }
    }

    private fun getBackupsFragment(): BackupsFragment? {
        return (supportFragmentManager.findFragmentByTag(FragmentTag.BACKUPS.tag) as? BackupsFragment).also {
            backupsFragment = it
        }
    }

    private val chatsFragment: ChatTabsFragment?
        get() = (supportFragmentManager.findFragmentByTag(FragmentTag.RECENT_CHAT.tag) as? ChatTabsFragment).also {
            chatTabsFragment = it
        }

    private fun getPermissionsFragment(): PermissionsFragment? {
        return (supportFragmentManager.findFragmentByTag(FragmentTag.PERMISSIONS.tag) as? PermissionsFragment).also {
            permissionsFragment = it
        }
    }

    /**
     * Checks whether the current screen is the main of Homepage or Documents.
     * Video / Audio / Photos do not need Fab button
     *
     * @param isShow True if Fab button should be display, False if Fab button should be hidden
     */
    private fun controlFabInHomepage(isShow: Boolean) {
        if (homepageScreen === HomepageScreen.HOMEPAGE) {
            // Control the Fab in homepage
            val fragment =
                getFragmentByType(HomepageFragment::class.java)
            if (fragment != null) {
                if (isShow) {
                    fragment.showFabButton()
                } else {
                    fragment.hideFabButton()
                }
            }
        } else if (homepageScreen === HomepageScreen.DOCUMENTS) {
            // Control the Fab in documents
            val docFragment = getFragmentByType(
                DocumentsFragment::class.java
            )
            if (docFragment != null) {
                if (isShow) {
                    docFragment.showFabButton()
                } else {
                    docFragment.hideFabButton()
                }
            }
        }
    }

    override fun finishRenameActionWithSuccess(newName: String) {
        when (drawerItem) {
            DrawerItem.CLOUD_DRIVE -> refreshCloudDrive()
            DrawerItem.RUBBISH_BIN -> refreshRubbishBin()
            DrawerItem.BACKUPS -> refreshBackupsList()
            DrawerItem.SHARED_ITEMS -> {
                refreshOutgoingShares()
                refreshIncomingShares()
                refreshLinks()
            }

            DrawerItem.HOMEPAGE -> refreshOfflineNodes()
            else -> {}
        }
    }

    override fun actionConfirmed() {
        //No update needed
    }

    override fun onPreviewLoaded(request: MegaChatRequest, alreadyExist: Boolean) {
        val chatId: Long = request.chatHandle
        val isFromOpenChatPreview: Boolean = request.flag
        val type: Int = request.paramType
        val link: String = request.link
        val waitingRoom = MegaChatApi.hasChatOptionEnabled(MegaChatApi.CHAT_OPTION_WAITING_ROOM, request.privilege)
        if (joiningToChatLink && TextUtil.isTextEmpty(link) && chatId == MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
            showSnackbar(
                Constants.SNACKBAR_TYPE,
                getString(R.string.error_chat_link_init_error),
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
            )
            resetJoiningChatLink()
            return
        }
        if (type == Constants.LINK_IS_FOR_MEETING) {
            Timber.d("It's a meeting")
            val linkInvalid =
                TextUtil.isTextEmpty(link) && chatId == MegaChatApiJava.MEGACHAT_INVALID_HANDLE
            if (linkInvalid) {
                Timber.e("Invalid link")
                return
            }
            if (CallUtil.isMeetingEnded(request)) {
                Timber.d("It's a meeting, open dialog: Meeting has ended")
                MeetingHasEndedDialogFragment(object : MeetingHasEndedDialogFragment.ClickCallback {
                    override fun onViewMeetingChat() {
                        showChatLink(link)
                    }

                    override fun onLeave() {}
                }, false).show(
                    supportFragmentManager,
                    MeetingHasEndedDialogFragment.TAG
                )
            } else {
                CallUtil.checkMeetingInProgress(
                    this@ManagerActivity,
                    this@ManagerActivity,
                    chatId,
                    isFromOpenChatPreview,
                    link,
                    request.megaHandleList,
                    request.text,
                    alreadyExist,
                    request.userHandle,
                    passcodeManagement,
                    waitingRoom
                )
            }
        } else {
            Timber.d("It's a chat")
            showChatLink(link)
        }
        dismissAlertDialogIfExists(openLinkDialog)
    }

    override fun onErrorLoadingPreview(errorCode: Int) {
        if (errorCode == MegaChatError.ERROR_NOENT) {
            dismissAlertDialogIfExists(openLinkDialog)
            Util.showAlert(
                this,
                getString(R.string.invalid_chat_link),
                getString(R.string.title_alert_chat_link_error)
            )
        } else {
            showOpenLinkError(true, 0)
        }
    }

    /**
     * Checks if the current screen is the main of Home.
     *
     * @return True if the current screen is the main of Home, false otherwise.
     */
    val isInMainHomePage: Boolean
        get() = drawerItem === DrawerItem.HOMEPAGE && homepageScreen === HomepageScreen.HOMEPAGE

    /**
     * Checks if the current screen is photos section of Homepage.
     *
     * @return True if the current screen is the photos, false otherwise.
     */
    val isInImagesPage: Boolean
        get() = drawerItem === DrawerItem.HOMEPAGE && homepageScreen === HomepageScreen.IMAGES

    /**
     * Checks if the current screen is Album content page.
     *
     * @return True if the current screen is Album content page, false otherwise.
     */
    val isInAlbumContentPage: Boolean
        get() = drawerItem === DrawerItem.PHOTOS && isInAlbumContent

    /**
     * Checks if the current screen is Photos.
     *
     * @return True if the current screen is Photos, false otherwise.
     */
    val isInPhotosPage: Boolean
        get() = drawerItem === DrawerItem.PHOTOS

    /**
     * Checks if the current screen is Media Discovery Fragment.
     *
     * @return True if the current screen is MD, false otherwise.
     */
    fun isInMDMode(): Boolean {
        return drawerItem === DrawerItem.CLOUD_DRIVE && isInMDMode
    }

    /**
     * switch to Cloud Drive file list from media discovery
     */
    fun switchToCDFromMD() {
        isInMDMode = false
        backToDrawerItem(bottomNavigationCurrentItem)
    }

    /**
     * Create the instance of FileBackupManager
     */
    private fun initFileBackupManager() = FileBackupManager(
        this,
        object : ActionBackupListener {
            override fun actionBackupResult(
                actionType: Int,
                operationType: Int,
                result: MoveRequestResult?,
                handle: Long,
            ) {
                if (actionType == ACTION_BACKUP_FAB) {
                    if (operationType == OPERATION_EXECUTE) {
                        if (bottomSheetDialogFragment?.isBottomSheetDialogShown() == true) return
                        bottomSheetDialogFragment = UploadBottomSheetDialogFragment.newInstance(
                            UploadBottomSheetDialogFragment.GENERAL_UPLOAD
                        )
                        bottomSheetDialogFragment?.show(
                            supportFragmentManager,
                            bottomSheetDialogFragment?.tag
                        )
                    }
                } else {
                    Timber.d("Nothing to do for actionType = %s", actionType)
                }
            }
        })

    /**
     * Receive changes to OnChatListItemUpdate, OnChatOnlineStatusUpdate and OnChatConnectionStateUpdate and make the necessary changes
     */
    private fun checkChatChanges() {
        getChatChangesUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ next: GetChatChangesUseCase.Result? ->
                if (next is GetChatChangesUseCase.Result.OnChatListItemUpdate) {
                    val item: MegaChatListItem? = next.item
                    onChatListItemUpdate(item)
                }
                if (next is GetChatChangesUseCase.Result.OnChatOnlineStatusUpdate) {
                    val userHandle = next.userHandle
                    val status = next.status
                    val inProgress = next.inProgress
                    onChatOnlineStatusUpdate(userHandle, status, inProgress)
                }
                if (next is GetChatChangesUseCase.Result.OnChatConnectionStateUpdate) {
                    val chatId = next.chatid
                    val newState = next.newState
                    onChatConnectionStateUpdate(chatId, newState)
                }
            }, { t: Throwable? -> Timber.e(t) })
            .addTo(composite)
    }

    /**
     * Create list of positions of each new node which was added to share folder
     *
     * @param nodes Share folder nodes' list
     * @return positions list
     */
    fun getPositionsList(nodes: List<MegaNode?>): ArrayList<Int> {
        val positions = ArrayList<Int>()
        if (comesFromNotificationChildNodeHandleList != null) {
            val childNodeHandleList = comesFromNotificationChildNodeHandleList ?: LongArray(0)
            for (childNodeHandle in childNodeHandleList) {
                for (i in 1 until nodes.size) {
                    val shareNode: MegaNode? = nodes[i]
                    if (shareNode != null && shareNode.handle == childNodeHandle) {
                        positions.add(i)
                    }
                }
            }
        }
        return positions
    }

    /**
     * Updates the UI related to unread user alerts as per the [UnreadUserAlertsCheckType] received.
     *
     * @param result Pair containing the type of the request and the number of unread user alerts.
     */
    private fun updateNumUnreadUserAlerts(result: Pair<UnreadUserAlertsCheckType, Int>) {
        val type: UnreadUserAlertsCheckType = result.first
        val numUnreadUserAlerts = result.second
        if (type === UnreadUserAlertsCheckType.NAVIGATION_TOOLBAR_ICON) {
            updateNavigationToolbarIcon(numUnreadUserAlerts)
        } else if (type === UnreadUserAlertsCheckType.NOTIFICATIONS_TITLE_AND_TOOLBAR_ICON) {
            updateNavigationToolbarIcon(numUnreadUserAlerts)
        }
    }

    /**
     * Updates the Shares section tab as per the indexShares.
     */
    private fun updateSharesTab() {
        if (viewModel.state().sharesTab === SharesTab.NONE) {
            Timber.d("indexShares is -1")
            return
        }
        Timber.d("The index of the TAB Shares is: %s", viewModel.state().sharesTab)
        viewPagerShares.setCurrentItem(viewModel.state().sharesTab.position, false)
        viewModel.setSharesTab(SharesTab.NONE)
    }

    /**
     * Restores the Shares section after opening it from a notification in the Notifications section.
     */
    fun restoreSharesAfterComingFromNotifications() {
        selectDrawerItem(DrawerItem.NOTIFICATIONS)
        comesFromNotifications = false
        comesFromNotificationsLevel = 0
        comesFromNotificationHandle = Constants.INVALID_VALUE.toLong()
        viewModel.setSharesTab(comesFromNotificationSharedIndex)
        updateSharesTab()
        comesFromNotificationSharedIndex = SharesTab.NONE
        setDeepBrowserTreeIncoming(
            comesFromNotificationDeepBrowserTreeIncoming,
            comesFromNotificationHandleSaved
        )
        comesFromNotificationDeepBrowserTreeIncoming = Constants.INVALID_VALUE
        comesFromNotificationHandleSaved = Constants.INVALID_VALUE.toLong()
        refreshIncomingShares()
    }

    /**
     * Restores the Rubbish section after opening it from a notification in the Notifications section.
     */
    fun restoreRubbishAfterComingFromNotification() {
        comesFromNotifications = false
        comesFromNotificationHandle = -1
        selectDrawerItem(DrawerItem.NOTIFICATIONS)
        rubbishBinViewModel.setRubbishBinHandle(comesFromNotificationHandleSaved)
        comesFromNotificationHandleSaved = -1
    }

    /**
     * Restores the FileBrowser section after opening it from a notification in the Notifications section.
     */
    fun restoreFileBrowserAfterComingFromNotification() {
        comesFromNotifications = false
        comesFromNotificationHandle = -1
        selectDrawerItem(DrawerItem.NOTIFICATIONS)
        fileBrowserViewModel.setBrowserParentHandle(comesFromNotificationHandleSaved)
        comesFromNotificationHandleSaved = -1
        refreshCloudDrive()
    }

    private fun handleSuperBackPressed() {
        onBackPressedCallback.isEnabled = false
        onBackPressedDispatcher.onBackPressed()
        onBackPressedCallback.isEnabled = true
    }

    override fun closeDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private val isBusinessAccount: Boolean
        get() = megaApi.isBusinessAccount && myAccountInfo.accountType == Constants.BUSINESS

    /**
     * Function to add unverified incoming count on tabs
     */
    private fun addUnverifiedIncomingCountBadge(unverifiedNodesCount: Int) {
        val incomingSharesTab = tabLayoutShares.getTabAt(0)
        if (incomingSharesTab != null) {
            if (unverifiedNodesCount > 0) {
                incomingSharesTab.orCreateBadge.number = unverifiedNodesCount
            } else {
                incomingSharesTab.removeBadge()
            }
        }
    }

    /**
     * Function to add unverified outgoing count on tabs
     */
    private fun addUnverifiedOutgoingCountBadge(unverifiedNodesCount: Int) {
        val outgoingSharesTab = tabLayoutShares.getTabAt(1)
        if (outgoingSharesTab != null) {
            if (unverifiedNodesCount > 0) {
                outgoingSharesTab.orCreateBadge.number = unverifiedNodesCount
            } else {
                outgoingSharesTab.removeBadge()
            }
        }
    }

    private fun handleCameraUploadFolderIconUpdate() {
        if (drawerItem === DrawerItem.PHOTOS) {
            cameraUploadsClicked()
        }
        onNodesCloudDriveUpdate()
    }

    private fun handleUpdateMyAccount(data: MyAccountUpdate) {
        when (data.action) {
            Action.STORAGE_STATE_CHANGED -> {
                Timber.d("BROADCAST STORAGE STATE CHANGED")
                storageStateFromBroadcast = data.storageState ?: StorageState.Unknown
                if (!showStorageAlertWithDelay) {
                    checkStorageStatus(
                        if (storageStateFromBroadcast !== StorageState.Unknown) storageStateFromBroadcast else viewModel.getStorageState()
                    )
                }
                return
            }

            Action.UPDATE_ACCOUNT_DETAILS -> {
                Timber.d("BROADCAST TO UPDATE AFTER UPDATE_ACCOUNT_DETAILS")
                if (isFinishing) {
                    return
                }
                checkInitialScreens()
                if (isBusinessAccount) {
                    invalidateOptionsMenu()
                }
            }
        }
    }

    /**
     * Check for existing archived chat rooms and show the menu item accordingly
     */
    private fun checkArchivedChats() {
        lifecycleScope.launch {
            runCatching { hasArchivedChatsUseCase() }
                .onSuccess { archivedMenuItem?.isVisible = it }
                .onFailure { Timber.e(it) }
        }
    }

    companion object {
        const val TRANSFERS_TAB = "TRANSFERS_TAB"
        private const val BOTTOM_ITEM_BEFORE_OPEN_FULLSCREEN_OFFLINE =
            "BOTTOM_ITEM_BEFORE_OPEN_FULLSCREEN_OFFLINE"
        const val NEW_CREATION_ACCOUNT = "NEW_CREATION_ACCOUNT"
        const val JOINING_CHAT_LINK = "JOINING_CHAT_LINK"
        const val LINK_JOINING_CHAT_LINK = "LINK_JOINING_CHAT_LINK"
        private const val PROCESS_FILE_DIALOG_SHOWN = "PROGRESS_DIALOG_SHOWN"
        private const val OPEN_LINK_DIALOG_SHOWN = "OPEN_LINK_DIALOG_SHOWN"
        private const val OPEN_LINK_TEXT = "OPEN_LINK_TEXT"
        private const val OPEN_LINK_ERROR = "OPEN_LINK_ERROR"
        private const val COMES_FROM_NOTIFICATIONS_SHARED_INDEX =
            "COMES_FROM_NOTIFICATIONS_SHARED_INDEX"

        // 8dp + 56dp(Fab size) + 8dp
        const val TRANSFER_WIDGET_MARGIN_BOTTOM = 72

        /**
         * The causes of elevating the app bar
         */
        const val ELEVATION_SCROLL = 0x01
        const val ELEVATION_CALL_IN_PROGRESS = 0x02
        private const val STATE_KEY_IS_IN_MD_MODE = "isInMDMode"
        private const val STATE_KEY_IS_IN_ALBUM_CONTENT = "isInAlbumContent"
        private const val STATE_KEY_IS_IN_PHOTOS_FILTER = "isInFilterPage"
        var drawerMenuItem: MenuItem? = null
        private const val LINK_DIALOG_MEETING = 1
        private const val LINK_DIALOG_CHAT = 2
        private var MEETING_TYPE: String = MeetingActivity.MEETING_ACTION_CREATE
    }
}
