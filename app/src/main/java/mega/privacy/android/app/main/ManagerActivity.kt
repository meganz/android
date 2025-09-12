@file:Suppress("DEPRECATION")

package mega.privacy.android.app.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.core.view.MenuItemCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.work.WorkManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.BusinessExpiredAlertActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.activities.contract.NameCollisionActivityContract
import mega.privacy.android.app.activities.contract.VersionsFileActivityContract
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.contacts.ContactsActivity
import mega.privacy.android.app.extensions.consumeParentInsets
import mega.privacy.android.app.extensions.isPortrait
import mega.privacy.android.app.extensions.isTablet
import mega.privacy.android.app.extensions.openTransfersAndConsumeErrorStatus
import mega.privacy.android.app.fragments.homepage.HomepageSearchable
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.fragments.homepage.main.HomepageFragment
import mega.privacy.android.app.fragments.homepage.main.HomepageFragmentDirections
import mega.privacy.android.app.fragments.settingsFragments.cookie.CookieDialogHandler
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.interfaces.ActionBackupListener
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.MeetingBottomSheetDialogActionListener
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbarWithChat
import mega.privacy.android.app.main.ads.AdsContainer
import mega.privacy.android.app.main.ads.AdsContainerViewModel
import mega.privacy.android.app.main.bottomsheets.HomeFabOptionsBottomSheet
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.dialog.ClearRubbishBinDialogFragment
import mega.privacy.android.app.main.dialog.Enable2FADialogFragment
import mega.privacy.android.app.main.dialog.businessgrace.BusinessGraceDialogFragment
import mega.privacy.android.app.main.dialog.contactlink.ContactLinkDialogFragment
import mega.privacy.android.app.main.dialog.link.OpenLinkDialogFragment
import mega.privacy.android.app.main.dialog.storagestatus.StorageStatusDialogFragment
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.app.main.listeners.FabButtonListener
import mega.privacy.android.app.main.managerSections.ManagerUploadBottomSheetDialogActionHandler
import mega.privacy.android.app.main.managerSections.TurnOnNotificationsFragment
import mega.privacy.android.app.main.mapper.ManagerRedirectIntentMapper
import mega.privacy.android.app.main.megachat.BadgeDrawerArrowDrawable
import mega.privacy.android.app.main.share.SharesFragment
import mega.privacy.android.app.main.share.SharesViewModel
import mega.privacy.android.app.main.view.OngoingCallBanner
import mega.privacy.android.app.main.view.OngoingCallViewModel
import mega.privacy.android.app.mediaplayer.miniplayer.MiniAudioPlayerController
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.fragments.MeetingHasEndedDialogFragment
import mega.privacy.android.app.middlelayer.inappupdate.InAppUpdateHandler
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.SortByBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.UploadBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.nodelabel.NodeLabelBottomSheetDialogFragment
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.presentation.advertisements.GoogleAdsManager
import mega.privacy.android.app.presentation.backups.BackupsFragment
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.bottomsheet.UploadBottomSheetDialogActionListener
import mega.privacy.android.app.presentation.bottomsheet.model.NodeDeviceCenterInformation
import mega.privacy.android.app.presentation.chat.archived.ArchivedChatsActivity
import mega.privacy.android.app.presentation.chat.list.ChatTabsFragment
import mega.privacy.android.app.presentation.clouddrive.CloudDriveSyncsFragment
import mega.privacy.android.app.presentation.clouddrive.CloudDriveTab
import mega.privacy.android.app.presentation.clouddrive.FileBrowserActionListener
import mega.privacy.android.app.presentation.clouddrive.FileBrowserViewModel
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper
import mega.privacy.android.app.presentation.documentscanner.dialogs.DocumentScanningErrorDialog
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.favourites.FavouriteFolderFragment
import mega.privacy.android.app.presentation.fileinfo.FileInfoActivity
import mega.privacy.android.app.presentation.filelink.FileLinkComposeActivity
import mega.privacy.android.app.presentation.filestorage.FileStorageActivity
import mega.privacy.android.app.presentation.fingerprintauth.SecurityUpgradeDialogFragment
import mega.privacy.android.app.presentation.folderlink.FolderLinkComposeActivity
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.manager.ManagerViewModel
import mega.privacy.android.app.presentation.manager.UnreadUserAlertsCheckType
import mega.privacy.android.app.presentation.manager.UserInfoViewModel
import mega.privacy.android.app.presentation.manager.model.ManagerState
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.meeting.WaitingRoomManagementViewModel
import mega.privacy.android.app.presentation.meeting.chat.extension.getInfo
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.UpgradeProPlanBottomSheet
import mega.privacy.android.app.presentation.meeting.view.dialog.CallRecordingConsentDialog
import mega.privacy.android.app.presentation.meeting.view.dialog.DenyEntryToCallDialog
import mega.privacy.android.app.presentation.meeting.view.dialog.FreePlanLimitParticipantsDialog
import mega.privacy.android.app.presentation.meeting.view.dialog.UsersInWaitingRoomDialog
import mega.privacy.android.app.presentation.node.NodeSourceTypeMapper
import mega.privacy.android.app.presentation.notification.NotificationsFragment
import mega.privacy.android.app.presentation.notification.model.NotificationNavigationHandler
import mega.privacy.android.app.presentation.offline.offlinecompose.OfflineComposeFragment
import mega.privacy.android.app.presentation.permissions.PermissionsFragment
import mega.privacy.android.app.presentation.photos.PhotosFragment
import mega.privacy.android.app.presentation.photos.albums.albumcontent.AlbumContentFragment
import mega.privacy.android.app.presentation.photos.mediadiscovery.MediaDiscoveryFragment
import mega.privacy.android.app.presentation.photos.timeline.photosfilter.PhotosFilterFragment
import mega.privacy.android.app.presentation.qrcode.QRCodeComposeActivity
import mega.privacy.android.app.presentation.recentactions.recentactionbucket.RecentActionBucketFragment
import mega.privacy.android.app.presentation.requeststatus.RequestStatusProgressContainer
import mega.privacy.android.app.presentation.rubbishbin.RubbishBinComposeFragment
import mega.privacy.android.app.presentation.rubbishbin.RubbishBinViewModel
import mega.privacy.android.app.presentation.search.SearchActivity
import mega.privacy.android.app.presentation.settings.exportrecoverykey.ExportRecoveryKeyActivity
import mega.privacy.android.app.presentation.settings.model.startScreenTargetPreference
import mega.privacy.android.app.presentation.settings.model.storageTargetPreference
import mega.privacy.android.app.presentation.settings.startscreen.util.StartScreenUtil
import mega.privacy.android.app.presentation.settings.startscreen.util.StartScreenUtil.Companion.CHAT_BNV
import mega.privacy.android.app.presentation.settings.startscreen.util.StartScreenUtil.Companion.CLOUD_DRIVE_BNV
import mega.privacy.android.app.presentation.settings.startscreen.util.StartScreenUtil.Companion.HOME_BNV
import mega.privacy.android.app.presentation.settings.startscreen.util.StartScreenUtil.Companion.NO_BNV
import mega.privacy.android.app.presentation.settings.startscreen.util.StartScreenUtil.Companion.PHOTOS_BNV
import mega.privacy.android.app.presentation.settings.startscreen.util.StartScreenUtil.Companion.SHARED_ITEMS_BNV
import mega.privacy.android.app.presentation.shares.SharesActionListener
import mega.privacy.android.app.presentation.shares.incoming.IncomingSharesComposeViewModel
import mega.privacy.android.app.presentation.shares.links.LinksViewModel
import mega.privacy.android.app.presentation.shares.outgoing.OutgoingSharesComposeViewModel
import mega.privacy.android.app.presentation.transfers.attach.NodeAttachmentViewModel
import mega.privacy.android.app.presentation.transfers.attach.createNodeAttachmentView
import mega.privacy.android.app.presentation.transfers.starttransfer.StartDownloadViewModel
import mega.privacy.android.app.presentation.transfers.starttransfer.view.createStartTransferView
import mega.privacy.android.app.psa.PsaViewHolder
import mega.privacy.android.app.service.iar.RatingHandlerImpl
import mega.privacy.android.app.service.push.MegaMessageService
import mega.privacy.android.app.sync.fileBackups.FileBackupManager
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.BackupDialogState.BACKUP_DIALOG_SHOW_NONE
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.BackupDialogState.BACKUP_DIALOG_SHOW_WARNING
import mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.ColorUtils.tintIcon
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaApiUtils
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
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.UploadUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import mega.privacy.android.app.utils.wrapper.MegaNodeUtilWrapper
import mega.privacy.android.core.nodecomponents.mapper.RestoreNodeResultMapper
import mega.privacy.android.core.nodecomponents.mapper.message.NodeMoveRequestMessageMapper
import mega.privacy.android.core.sharedcomponents.serializable
import mega.privacy.android.data.model.MegaAttributes
import mega.privacy.android.data.model.MegaPreferences
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.MyAccountUpdate
import mega.privacy.android.domain.entity.MyAccountUpdate.Action
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.chat.ChatLinkContent
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.document.toDocumentEntity
import mega.privacy.android.domain.entity.meeting.UsersCallLimitReminders
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.RestoreNodeResult
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.chat.IAmOnAnotherCallException
import mega.privacy.android.domain.exception.chat.MeetingEndedException
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.file.CheckFileNameCollisionsUseCase
import mega.privacy.android.domain.usecase.login.MonitorEphemeralCredentialsUseCase
import mega.privacy.android.feature.devicecenter.ui.DeviceCenterFragment
import mega.privacy.android.feature.payment.model.AccountTypeInt
import mega.privacy.android.feature.sync.ui.SyncMonitorState
import mega.privacy.android.feature.sync.ui.SyncMonitorViewModel
import mega.privacy.android.feature.sync.ui.notification.SyncNotificationManager
import mega.privacy.android.feature.sync.ui.settings.SyncSettingsBottomSheetView
import mega.privacy.android.feature.sync.ui.views.SyncPromotionBottomSheet
import mega.privacy.android.feature.sync.ui.views.SyncPromotionViewModel
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.ExtraConstant
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.settings.arguments.TargetPreference
import mega.privacy.android.shared.original.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.shared.original.core.ui.controls.widgets.setTransfersWidgetContent
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.ArchivedChatsMenuItemEvent
import mega.privacy.mobile.analytics.event.ChatRoomDNDMenuItemEvent
import mega.privacy.mobile.analytics.event.ChatRoomsBottomNavigationItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveBottomNavigationItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveSearchMenuToolbarEvent
import mega.privacy.mobile.analytics.event.JoinMeetingPressedEvent
import mega.privacy.mobile.analytics.event.OpenLinkMenuItemEvent
import mega.privacy.mobile.analytics.event.SharedItemsScreenEvent
import mega.privacy.mobile.analytics.event.StartMeetingNowPressedEvent
import mega.privacy.mobile.analytics.event.SyncPromotionBottomSheetDismissedEvent
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject

@Suppress("KDocMissingDocumentation")
@AndroidEntryPoint
class ManagerActivity : PasscodeActivity(), NavigationView.OnNavigationItemSelectedListener,
    BottomNavigationView.OnNavigationItemSelectedListener, UploadBottomSheetDialogActionListener,
    ActionNodeCallback, SnackbarShower,
    MeetingBottomSheetDialogActionListener, NotificationNavigationHandler,
    ParentNodeManager, CameraPermissionManager, NavigationDrawerManager, FileBrowserActionListener,
    SharesActionListener {
    /**
     * The cause bitmap of elevating the app bar
     */
    private var mElevationCause = 0

    internal val viewModel: ManagerViewModel by viewModels()
    private val fileBrowserViewModel: FileBrowserViewModel by viewModels()
    internal val incomingSharesViewModel: IncomingSharesComposeViewModel by viewModels()
    private val outgoingSharesViewModel: OutgoingSharesComposeViewModel by viewModels()
    private val linksViewModel: LinksViewModel by viewModels()
    internal val rubbishBinViewModel: RubbishBinViewModel by viewModels()
    private val callInProgressViewModel: OngoingCallViewModel by viewModels()
    private val userInfoViewModel: UserInfoViewModel by viewModels()
    private val waitingRoomManagementViewModel: WaitingRoomManagementViewModel by viewModels()
    private val startDownloadViewModel: StartDownloadViewModel by viewModels()
    private val nodeAttachmentViewModel by viewModels<NodeAttachmentViewModel>()
    private val sortByHeaderViewModel: SortByHeaderViewModel by viewModels()
    private val syncMonitorViewModel: SyncMonitorViewModel by viewModels()
    private val syncPromotionViewModel: SyncPromotionViewModel by viewModels()
    val sharesViewModel: SharesViewModel by viewModels()
    private val adsContainerViewModel: AdsContainerViewModel by viewModels()

    /**
     * [MegaNavigator]
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    @Inject
    lateinit var megaNodeUtilWrapper: MegaNodeUtilWrapper

    @Inject
    lateinit var startScreenUtil: StartScreenUtil

    private val searchResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val handle =
                    result.data?.getLongExtra(SearchActivity.SEARCH_NODE_HANDLE, INVALID_HANDLE)
                        ?: INVALID_HANDLE
                openSearchFolder(handle)
            }
        }

    private val nameCollisionActivityLauncher = registerForActivityResult(
        NameCollisionActivityContract()
    ) { result ->
        result?.let {
            showSnackbar(SNACKBAR_TYPE, it, INVALID_HANDLE)
        }
    }

    val versionsActivityLauncher =
        registerForActivityResult(VersionsFileActivityContract()) { result ->
            result?.let {
                lifecycleScope.launch {
                    val message = viewModel.deleteVersionHistory(it)
                    showSnackbar(SNACKBAR_TYPE, message, -1)
                }
            }
        }

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    @Inject
    lateinit var cookieDialogHandler: CookieDialogHandler

    @Inject
    lateinit var checkFileNameCollisionsUseCase: CheckFileNameCollisionsUseCase

    @Inject
    lateinit var activityLifecycleHandler: ActivityLifecycleHandler

    @Inject
    internal lateinit var uploadBottomSheetDialogActionHandler: ManagerUploadBottomSheetDialogActionHandler

    @Inject
    lateinit var copyRequestMessageMapper: CopyRequestMessageMapper

    @Inject
    lateinit var nodeMoveRequestMessageMapper: NodeMoveRequestMessageMapper

    @Inject
    lateinit var monitorEphemeralCredentialsUseCase: MonitorEphemeralCredentialsUseCase

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    lateinit var restoreNodeResultMapper: RestoreNodeResultMapper

    @Inject
    lateinit var getChatRoomUseCase: GetChatRoomUseCase

    @Inject
    lateinit var managerRedirectIntentMapper: ManagerRedirectIntentMapper

    @Inject
    lateinit var inAppUpdateHandler: InAppUpdateHandler

    @Inject
    lateinit var nodeSourceTypeMapper: NodeSourceTypeMapper

    @Inject
    lateinit var workManager: WorkManager

    @Inject
    lateinit var crashReporter: CrashReporter

    @Inject
    lateinit var googleAdsManager: GoogleAdsManager

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var syncNotificationManager: SyncNotificationManager

    private lateinit var fabButton: FloatingActionButton
    private var pendingActionsBadge: View? = null

    var rootNode: MegaNode? = null
    val nodeController: NodeController by lazy { NodeController(this) }

    private val badgeDrawable: BadgeDrawerArrowDrawable by lazy {
        BadgeDrawerArrowDrawable(
            this, R.color.color_button_brand,
            R.color.white_dark_grey, R.color.white_dark_grey
        )
    }
    var prefs: MegaPreferences? = null
    var attr: MegaAttributes? = null
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var fragmentContainer: FragmentContainerView
    private lateinit var toolbar: MaterialToolbar
    lateinit var appBarLayout: AppBarLayout

    private var selectedAccountType = 0

    lateinit var drawerLayout: DrawerLayout

    @JvmField
    var openFolderRefresh = false
    private var newAccount = false
    private var firstLogin = false
    private var newCreationAccount = false
    private var storageState: StorageState = StorageState.Unknown //Default value
    private var storageStateFromBroadcast: StorageState = StorageState.Unknown //Default value
    private var showStorageAlertWithDelay = false

    private var reconnectDialog: AlertDialog? = null

    private var orientationSaved = 0

    private var isInFilterPage = false
    private var isInAlbumContent = false
    private var fromAlbumContent = false

    @JvmField
    var turnOnNotifications = false

    override var drawerItem: DrawerItem? = null
    private var nodeSourceType: NodeSourceType = NodeSourceType.OTHER
    private lateinit var fragmentLayout: LinearLayout
    private lateinit var waitingRoomComposeView: ComposeView
    private lateinit var callRecordingConsentDialogComposeView: ComposeView
    private lateinit var documentScanningErrorDialogComposeView: ComposeView
    private lateinit var freePlanLimitParticipantsDialogComposeView: ComposeView
    private lateinit var syncPromotionBottomSheetComposeView: ComposeView
    private lateinit var fabOptionsBottomSheetComposeView: ComposeView
    private lateinit var syncSettingsBottomSheetComposeView: ComposeView
    private lateinit var requestStatusProgressComposeView: ComposeView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var navigationView: NavigationView
    private lateinit var adsContainerView: ComposeView

    private var miniAudioPlayerController: MiniAudioPlayerController? = null
    private lateinit var cameraUploadViewTypes: LinearLayout

    @JvmField
    var firstTimeAfterInstallation = true
    var searchView: SearchView? = null
    var searchExpand = false
    private var isSearching = false
    private var openLink = false
    private var requestNotificationsPermissionFirstLogin = false
    private var askPermissions = false
    private var homepageScreen = HomepageScreen.HOMEPAGE
    private var pathNavigationOffline: String? = null

    // Fragments
    private var cloudDriveSyncsFragment: CloudDriveSyncsFragment? = null
    private var rubbishBinComposeFragment: RubbishBinComposeFragment? = null
    private var photosFragment: PhotosFragment? = null
    private var albumContentFragment: Fragment? = null
    private var photosFilterFragment: PhotosFilterFragment? = null
    private var chatTabsFragment: ChatTabsFragment? = null
    private var turnOnNotificationsFragment: TurnOnNotificationsFragment? = null
    private var permissionsFragment: PermissionsFragment? = null
    private var mStopped = true
    private var bottomItemBeforeOpenFullscreenOffline = Constants.INVALID_VALUE
    private var fullscreenOfflineComposeFragment: OfflineComposeFragment? = null
    private var pagerOfflineComposeFragment: OfflineComposeFragment? = null

    var statusDialog: AlertDialog? = null
    private var processFileDialog: AlertDialog? = null
    private var permissionsDialog: AlertDialog? = null

    private var searchMenuItem: MenuItem? = null
    private var doNotDisturbMenuItem: MenuItem? = null
    private var clearRubbishBinMenuItem: MenuItem? = null
    private var returnCallMenuItem: MenuItem? = null
    private var openLinkMenuItem: MenuItem? = null
    private var moreMenuItem: MenuItem? = null
    private var chronometerMenuItem: Chronometer? = null
    private var layoutCallMenuItem: LinearLayout? = null
    private var typesCameraPermission = Constants.INVALID_TYPE_PERMISSIONS

    var comesFromNotifications = false
    private var comesFromNotificationsLevel = 0
    var comesFromNotificationHandle = Constants.INVALID_VALUE.toLong()
    var comesFromNotificationHandleSaved = Constants.INVALID_VALUE.toLong()
    private var comesFromNotificationDeepBrowserTreeIncoming = Constants.INVALID_VALUE
    private var comesFromNotificationChildNodeHandleList: LongArray? = null
    private var comesFromNotificationSharedIndex: SharesTab = SharesTab.NONE
    private var bottomNavigationCurrentItem = -1

    private lateinit var chatBadge: View
    private lateinit var callBadge: View
    private val menuView: ViewGroup
        get() = bottomNavigationView.getChildAt(0) as ViewGroup

    private var joiningToChatLink = false
    private var linkJoinToChatLink: String? = null
    private var onAskingPermissionsFragment = false
    private var initialPermissionsAlreadyAsked = false
    private lateinit var navHostView: View
    private var navController: NavController? = null
    private var mHomepageSearchable: HomepageSearchable? = null
    private var initFabButtonShow = false

    private var bottomSheetDialogFragment: BottomSheetDialogFragment? = null
    var psaViewHolder: PsaViewHolder? = null

    // end for Meeting
    private var backupHandleList: ArrayList<Long>? = null
    private var backupDialogType: Int = BACKUP_DIALOG_SHOW_NONE
    private var backupNodeHandle: Long = -1
    private var backupNodeType = 0
    private var backupActionType = 0

    private var viewInFolderNode: MegaNode? = null

    private var showDialogStorageStatusJob: Job? = null

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goBack()
            }
        }

    private val fileBackupManager: FileBackupManager by lazy { initFileBackupManager() }

    private val credentials by lazy {
        runBlocking {
            runCatching {
                getAccountCredentialsUseCase()
            }.getOrNull()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constants.REQUEST_READ_WRITE_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showUploadPanel()
                }
            }

            PermissionsFragment.PERMISSIONS_FRAGMENT -> {
                if (getPermissionsFragment() != null) {
                    permissionsFragment?.setNextPermission()
                }
            }

            PermissionsFragment.PERMISSIONS_FRAGMENT_NOTIFICATION_PERMISSION -> {
                if (getPermissionsFragment() != null) {
                    val isPermissionGranted =
                        grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED
                    permissionsFragment?.onNotificationPermissionResult(isPermissionGranted)
                    permissionsFragment?.setNextPermission()
                }
            }

            PermissionsFragment.PERMISSIONS_FRAGMENT_MEDIA_PERMISSION -> {
                if (getPermissionsFragment() != null) {
                    val permissionResults = permissions
                        .mapIndexed { index, permission ->
                            val granted =
                                grantResults.getOrNull(index) == PackageManager.PERMISSION_GRANTED
                            permission to granted
                        }
                        .toMap()

                    permissionsFragment?.onMediaPermissionResult(permissionResults)
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
            outState.putBoolean("turnOnNotifications", true)
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
        uploadBottomSheetDialogActionHandler.onSaveInstanceState(outState)
        outState.putBoolean(PROCESS_FILE_DIALOG_SHOWN, isAlertDialogShown(processFileDialog))
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
        sortByHeaderViewModel.refreshData(isUpdatedOrderChangeState = true)
        viewModel.markHandledMessage()
        super.onStart()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)
        Timber.d("onCreate after call super")
        registerViewModelObservers()
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        if (handleDuplicateLaunches()) return
        if (savedInstanceState != null) {
            //Do after view instantiation
            restoreFromSavedInstanceState(savedInstanceState)
        } else {
            Timber.d("Bundle is NULL")
            pathNavigationOffline = Constants.OFFLINE_ROOT
        }
        CacheFolderManager.createCacheFolders()
        Timber.d("retryChatPendingConnections()")
        megaChatApi.retryPendingConnections(false)

        val display: Display = windowManager.defaultDisplay
        display.getMetrics(outMetrics)
        if (checkDatabaseValues()) return
        if (firstTimeAfterInstallation) {
            startScreenUtil.setStartScreenTimeStamp()
        }
        enableEdgeToEdge()
        setupView()
        handleInsets()
        initialiseChatBadgeView()
        setCallBadge()
        if (mElevationCause > 0) {
            // A work around: mElevationCause will be changed unexpectedly shortly
            val elevationCause = mElevationCause
            // Apply the previous Appbar elevation(e.g. before rotation) after all views have been created
            handler.postDelayed({ changeAppBarElevation(true, elevationCause) }, 100)
        }
        setupAudioPlayerController()
        userInfoViewModel.getUserAchievements()
        if (!viewModel.isConnected) {
            Timber.d("No network -> SHOW OFFLINE MODE")
            if (drawerItem == null) {
                drawerItem = DrawerItem.HOMEPAGE
            }
            selectDrawerItem(drawerItem)
            userInfoViewModel.getUserInfo(false)
            showOfflineMode()
            credentials?.let {
                val gSession = it.session
                ChatUtil.initMegaChatApi(gSession)
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
        uploadBottomSheetDialogActionHandler.onRestoreInstanceState(savedInstanceState)
        Timber.d("END onCreate")
        RatingHandlerImpl(this).showRatingBaseOnTransaction()
        if (backupDialogType == BACKUP_DIALOG_SHOW_WARNING) {
            fileBackupManager.showBackupsWarningDialog(
                handleList = backupHandleList,
                megaNode = megaApi.getNodeByHandle(backupNodeHandle),
                nodeType = backupNodeType,
                actionType = backupActionType,
                actionBackupNodeCallback = fileBackupManager.actionBackupNodeCallback,
            )
        } else {
            Timber.d("Backup warning dialog is not shown")
        }
        checkForInAppUpdate()
        checkForInAppAdvertisement(forceAskConsent = false)
    }

    /**
     * System navigation bar insets of current window
     */
    var systemBarInsets: Insets? = null
        private set

    private fun handleInsets() {
        consumeParentInsets(
            topInset = false,
            bottomInset = false
        ) { windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            systemBarInsets = insets
            appBarLayout.updatePadding(top = insets.top)
            ViewCompat.onApplyWindowInsets(syncPromotionBottomSheetComposeView, windowInsets)
            ViewCompat.onApplyWindowInsets(fabOptionsBottomSheetComposeView, windowInsets)
            ViewCompat.onApplyWindowInsets(freePlanLimitParticipantsDialogComposeView, windowInsets)
            ViewCompat.onApplyWindowInsets(syncSettingsBottomSheetComposeView, windowInsets)
            findViewById<LinearLayout>(R.id.fragment_layout).updatePadding(bottom = insets.bottom)
            // Set minimum height so that system bar padding is there when both bottom nav and ads are hidden
            findViewById<LinearLayout>(R.id.bottom_navigation_container).minimumHeight =
                insets.bottom
            // Adjust the ads container height to add system bar insets bottom padding
            adsContainerView.updatePadding(bottom = if (adsContainerViewModel.isAdsLoaded()) insets.bottom else 0)
            bottomNavigationView.updatePadding(bottom = if (adsContainerViewModel.isAdsLoaded()) 0 else insets.bottom)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
    }

    /**
     * Handles the successful result from [FileInfoActivity]
     *
     * @param intent A potentially nullable [Intent] that returns some data
     * @param resultCode The Activity Result Code
     */
    private fun handleFileInfoSuccessResult(intent: Intent?, resultCode: Int) {
        if (resultCode == Activity.RESULT_OK) {
            if (isCloudAdded) {
                val handle = intent?.getLongExtra(FileInfoActivity.NODE_HANDLE, -1) ?: -1
                fileBrowserViewModel.setFileBrowserHandle(handle)
            }
            onNodesSharedUpdate()
        }
    }

    private fun checkForInAppAdvertisement(forceAskConsent: Boolean) {
        lifecycleScope.launch {
            runCatching {
                googleAdsManager.checkForAdsAvailability()
                if (googleAdsManager.isAdsEnabled()) {
                    setupAdsView()
                    googleAdsManager.checkLatestConsentInformation(
                        activity = this@ManagerActivity,
                        onConsentInformationUpdated = { fetchNewAd() },
                        forceAskConsent = forceAskConsent
                    )
                }
            }.onFailure {
                Timber.e("Failed to fetch latest consent information : ${it.message}")
            }
        }
    }

    private fun checkForInAppUpdate() {
        lifecycleScope.launch {
            runCatching {
                inAppUpdateHandler.checkForAppUpdates()
            }
        }
    }

    private fun setupView() {
        Timber.d("Set view")
        setContentView(R.layout.activity_manager)
        initialiseViews()
        addStartTransferView()
        addNodeAttachmentView()
        findViewById<ComposeView>(R.id.transfers_widget).setTransfersWidgetContent(
            transfersInfoFlow = transfersManagementViewModel.state.map { it.transfersInfo },
            hideFlow = transfersManagementViewModel.state.map { it.hideTransfersWidget },
            onClick = this::onTransfersWidgetClick
        )
        setInitialViewProperties()
        setViewListeners()
    }

    private fun initialiseViews() {
        psaViewHolder = PsaViewHolder(
            psaLayout = findViewById(R.id.psa_layout),
            dismissPsa = { id ->
                updateHomepageFabPosition()
                viewModel.dismissPsa(id)
            }
        )
        appBarLayout = findViewById(R.id.app_bar_layout)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
        waitingRoomComposeView = findViewById(R.id.waiting_room_dialog_compose_view)
        callRecordingConsentDialogComposeView =
            findViewById(R.id.call_recording_consent_dialog_compose_view)
        documentScanningErrorDialogComposeView =
            findViewById(R.id.document_scanning_error_dialog_compose_view)
        freePlanLimitParticipantsDialogComposeView =
            findViewById(R.id.free_plan_limit_dialog_compose_view)
        syncPromotionBottomSheetComposeView =
            findViewById(R.id.sync_promotion_bottom_sheet_compose_view)
        fabOptionsBottomSheetComposeView = findViewById(R.id.options_bottom_sheet_compose_view)
        syncSettingsBottomSheetComposeView =
            findViewById(R.id.sync_settings_bottom_sheet_compose_view)
        requestStatusProgressComposeView =
            findViewById(R.id.request_status_progress_compose_view)
        adsContainerView = findViewById(R.id.ads_web_compose_view)
        fragmentLayout = findViewById(R.id.fragment_layout)
        bottomNavigationView =
            findViewById(R.id.bottom_navigation_view)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        fabButton = findViewById(R.id.floating_button)
        fragmentContainer = findViewById(R.id.fragment_container)
        cameraUploadViewTypes = findViewById(R.id.cu_view_type)
        navHostView = findViewById(R.id.nav_host_fragment)
    }

    private fun addStartTransferView() {
        findViewById<ViewGroup>(R.id.root_content_layout).addView(
            createStartTransferView(
                activity = this,
                transferEventState = startDownloadViewModel.state,
                onConsumeEvent = {
                    if ((startDownloadViewModel.state.value as StateEventWithContentTriggered).content is TransferTriggerEvent.StartUpload) {
                        viewModel.consumeUploadEvent()
                    }
                    startDownloadViewModel.consumeDownloadEvent()
                },
                navigateToStorageSettings = {
                    megaNavigator.openSettings(
                        this,
                        storageTargetPreference
                    )
                }
            )
        )
    }

    private fun addNodeAttachmentView() {
        findViewById<ViewGroup>(R.id.root_content_layout).addView(
            createNodeAttachmentView(
                activity = this,
                viewModel = nodeAttachmentViewModel,
                showMessage = { message, id ->
                    showSnackbarWithChat(message, id)
                }
            )
        )
    }

    @SuppressLint("UnrememberedMutableState")
    @OptIn(ExperimentalComposeUiApi::class)
    private fun setInitialViewProperties() {
        waitingRoomComposeView.apply {
            isVisible = true
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val isDark = themeMode.isDarkMode()
                OriginalTheme(isDark = isDark) {
                    UsersInWaitingRoomDialog()
                    DenyEntryToCallDialog()
                }
            }
        }

        callRecordingConsentDialogComposeView.apply {
            isVisible = true
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val isDark = themeMode.isDarkMode()
                OriginalTheme(isDark = isDark) {
                    CallRecordingConsentDialog()
                }
            }
        }

        documentScanningErrorDialogComposeView.apply {
            isVisible = true
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val isDark = themeMode.isDarkMode()
                val state by viewModel.state.collectAsStateWithLifecycle()

                OriginalTheme(isDark = isDark) {
                    DocumentScanningErrorDialog(
                        documentScanningError = state.documentScanningError,
                        onErrorAcknowledged = { viewModel.onDocumentScanningErrorConsumed() },
                        onErrorDismissed = { viewModel.onDocumentScanningErrorConsumed() },
                    )
                }
            }
        }

        setRequestStatusProgressComposeView()
        setSyncPromotionBottomSheetComposeView()
        setFabOptionsBottomSheetComposeView()
        setSyncSettingsBottomSheetComposeView()

        freePlanLimitParticipantsDialogComposeView.apply {
            isVisible = true
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val isDark = themeMode.isDarkMode()
                val state by viewModel.state.collectAsStateWithLifecycle()
                val coroutineScope = rememberCoroutineScope()
                val upgradeToProPlanBottomSheetState = rememberModalBottomSheetState(
                    initialValue = ModalBottomSheetValue.Hidden,
                    confirmValueChange = {
                        true
                    }
                )
                val isUpgradeToProPlanShown by derivedStateOf {
                    Timber.d("Current Bottom Sheet state ${upgradeToProPlanBottomSheetState.currentValue}")
                    upgradeToProPlanBottomSheetState.currentValue != ModalBottomSheetValue.Hidden
                }

                OriginalTheme(isDark = isDark) {

                    var showUpgradeDialog by rememberSaveable {
                        mutableStateOf(false)
                    }
                    LaunchedEffect(state.shouldUpgradeToProPlan) {
                        Timber.d("shouldUpgradeToProPlan ${state.shouldUpgradeToProPlan}")
                        if (state.shouldUpgradeToProPlan) {
                            showUpgradeDialog = true
                            upgradeToProPlanBottomSheetState.show()
                            viewModel.onConsumeShouldUpgradeToProPlan()
                        }
                    }
                    LaunchedEffect(upgradeToProPlanBottomSheetState.currentValue) {
                        if (upgradeToProPlanBottomSheetState.currentValue == ModalBottomSheetValue.Hidden) {
                            showUpgradeDialog = false
                        }
                    }

                    if (state.callEndedDueToFreePlanLimits && state.isCallUnlimitedProPlanFeatureFlagEnabled &&
                        state.usersCallLimitReminders == UsersCallLimitReminders.Enabled
                    ) {
                        FreePlanLimitParticipantsDialog(
                            onConfirm = {
                                viewModel.onConsumeShowFreePlanParticipantsLimitDialogEvent()
                            },
                        )
                    }
                    if (showUpgradeDialog) {
                        BottomSheet(
                            modifier = Modifier
                                .semantics {
                                    testTagsAsResourceId = true
                                },
                            bottomInsetPadding = false,
                            modalSheetState = upgradeToProPlanBottomSheetState,
                            sheetBody = {
                                when {
                                    isUpgradeToProPlanShown -> {
                                        UpgradeProPlanBottomSheet {
                                            coroutineScope.launch {
                                                upgradeToProPlanBottomSheetState.hide()
                                            }
                                        }
                                    }
                                }
                            })
                    }
                }
            }
        }
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            findViewById<ComposeView>(R.id.call_in_progress_layout).setContent {
                OngoingCallBanner(viewModel = callInProgressViewModel) { isShow ->
                    changeAppBarElevation(isShow, ELEVATION_CALL_IN_PROGRESS)
                }
            }
        }
    }

    private fun setRequestStatusProgressComposeView() {
        requestStatusProgressComposeView.apply {
            isVisible = true
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val isDark = themeMode.isDarkMode()
                OriginalTheme(isDark = isDark) {
                    RequestStatusProgressContainer()
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun setSyncSettingsBottomSheetComposeView() {
        syncSettingsBottomSheetComposeView.apply {
            isVisible = true
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val isDark = themeMode.isDarkMode()
                val state by fileBrowserViewModel.state.collectAsStateWithLifecycle()
                val syncSettingsBottomSheetState = rememberModalBottomSheetState(
                    initialValue = ModalBottomSheetValue.Hidden,
                    confirmValueChange = {
                        true
                    },
                    skipHalfExpanded = true
                )
                var showSyncSettings by rememberSaveable {
                    mutableStateOf(false)
                }
                OriginalTheme(isDark = isDark) {
                    LaunchedEffect(state.showSyncSettings) {
                        if (state.showSyncSettings) {
                            showSyncSettings = true
                            syncSettingsBottomSheetState.show()
                            fileBrowserViewModel.onConsumeSyncSettings()
                        }
                    }
                    LaunchedEffect(syncSettingsBottomSheetState.currentValue) {
                        if (syncSettingsBottomSheetState.currentValue == ModalBottomSheetValue.Hidden) {
                            showSyncSettings = false
                        }
                    }
                    SyncSettingsBottomSheetView(
                        modalSheetState = syncSettingsBottomSheetState,
                        sheetElevation = if (syncSettingsBottomSheetState.isVisible) ModalBottomSheetDefaults.Elevation else 0.dp,
                        shouldShowBottomSheet = showSyncSettings
                    ) {
                        showSyncSettings = false
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun setSyncPromotionBottomSheetComposeView() {
        syncPromotionBottomSheetComposeView.apply {
            isVisible = true
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val isDark = themeMode.isDarkMode()
                val state by syncPromotionViewModel.state.collectAsStateWithLifecycle()
                val coroutineScope = rememberCoroutineScope()
                val syncPromotionBottomSheetState = rememberModalBottomSheetState(
                    initialValue = ModalBottomSheetValue.Hidden,
                    confirmValueChange = {
                        true
                    },
                    skipHalfExpanded = true,
                )

                OriginalTheme(isDark = isDark) {
                    LaunchedEffect(syncPromotionBottomSheetState.targetValue) {
                        if (syncPromotionBottomSheetState.currentValue != ModalBottomSheetValue.Hidden &&
                            syncPromotionBottomSheetState.targetValue == ModalBottomSheetValue.Hidden
                        ) {
                            coroutineScope.launch { syncPromotionViewModel.onConsumeShouldShowSyncPromotion() }
                        }
                    }
                    LaunchedEffect(state.shouldShowSyncPromotion) {
                        Timber.d("shouldShowSyncPromotion ${state.shouldShowSyncPromotion}")
                        if (state.shouldShowSyncPromotion) {
                            if (isOnboarding()) {
                                syncPromotionViewModel.onConsumeShouldShowSyncPromotion()
                            } else {
                                syncPromotionBottomSheetState.show()
                            }
                            syncPromotionViewModel.setSyncPromotionShown()
                        } else {
                            syncPromotionBottomSheetState.hide()
                        }
                    }
                    BottomSheet(
                        modalSheetState = syncPromotionBottomSheetState,
                        sheetElevation = if (syncPromotionBottomSheetState.isVisible) ModalBottomSheetDefaults.Elevation else 0.dp,
                        sheetBody = {
                            SyncPromotionBottomSheet(
                                modifier = Modifier.semantics { testTagsAsResourceId = true },
                                onSyncFoldersClicked = {
                                    syncPromotionViewModel.setSyncPromotionShown(doNotShowAgain = true)
                                    megaNavigator.openNewSync(
                                        context = this@ManagerActivity,
                                        syncType = SyncType.TYPE_TWOWAY,
                                        isFromManagerActivity = true,
                                    )
                                },
                                onBackUpFoldersClicked = {
                                    syncPromotionViewModel.setSyncPromotionShown(doNotShowAgain = true)
                                    megaNavigator.openNewSync(
                                        context = this@ManagerActivity,
                                        syncType = SyncType.TYPE_BACKUP,
                                        isFromManagerActivity = true,
                                    )
                                },
                                onLearnMoreClicked = {
                                    syncPromotionViewModel.setSyncPromotionShown(doNotShowAgain = true)
                                },
                                hideSheet = { coroutineScope.launch { syncPromotionViewModel.onConsumeShouldShowSyncPromotion() } })
                        },
                        expandedRoundedCorners = true,
                    )
                }
            }
        }
    }

    private fun setFabOptionsBottomSheetComposeView() {
        fabOptionsBottomSheetComposeView.apply {
            isVisible = true
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val isDark = themeMode.isDarkMode()
                val state by viewModel.state.collectAsStateWithLifecycle()
                val coroutineScope = rememberCoroutineScope()
                val fabOptionsBottomSheetState = rememberModalBottomSheetState(
                    initialValue = ModalBottomSheetValue.Hidden,
                    confirmValueChange = {
                        true
                    },
                    skipHalfExpanded = true,
                )
                OriginalTheme(isDark = isDark) {
                    LaunchedEffect(fabOptionsBottomSheetState.targetValue) {
                        if (fabOptionsBottomSheetState.targetValue == ModalBottomSheetValue.Hidden) {
                            coroutineScope.launch { viewModel.onConsumeShowHomeFabOptionsBottomSheet() }
                        }
                    }
                    LaunchedEffect(state.showHomeFabOptionsBottomSheet) {
                        if (state.showHomeFabOptionsBottomSheet) {
                            fabOptionsBottomSheetState.show()
                        } else {
                            fabOptionsBottomSheetState.hide()
                        }
                    }
                    BottomSheet(
                        modalSheetState = fabOptionsBottomSheetState,
                        sheetElevation = if (fabOptionsBottomSheetState.isVisible) ModalBottomSheetDefaults.Elevation else 0.dp,
                        sheetBody = {
                            HomeFabOptionsBottomSheet(
                                onUploadFilesClicked = { uploadBottomSheetDialogActionHandler.uploadFiles() },
                                onUploadFolderClicked = { uploadBottomSheetDialogActionHandler.uploadFolder() },
                                onScanDocumentClicked = { viewModel.prepareDocumentScanner() },
                                onCaptureClicked = { uploadBottomSheetDialogActionHandler.takePictureAndUpload() },
                                onCreateNewTextFileClicked = {
                                    uploadBottomSheetDialogActionHandler.showNewTextFileDialog(null)
                                },
                                onAddNewSyncClicked = {
                                    megaNavigator.openNewSync(
                                        context = this@ManagerActivity,
                                        syncType = SyncType.TYPE_TWOWAY,
                                        isFromManagerActivity = true,
                                    )
                                },
                                onAddNewBackupClicked = {
                                    megaNavigator.openNewSync(
                                        context = this@ManagerActivity,
                                        syncType = SyncType.TYPE_BACKUP,
                                        isFromManagerActivity = true,
                                    )
                                },
                                onNewChatClicked = {
                                    getFragmentByType(HomepageFragment::class.java)?.openNewChatActivity()
                                },
                                hideSheet = { coroutineScope.launch { viewModel.onConsumeShowHomeFabOptionsBottomSheet() } }
                            )
                        },
                        expandedRoundedCorners = true,
                    )
                }
            }
        }
    }

    fun onShareTabChanged() {
        supportInvalidateOptionsMenu()
        checkScrollElevation()
        setToolbarTitle()
        showFabButton()
    }

    private fun setViewListeners() {
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
        addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

            override fun onDrawerOpened(drawerView: View) {
                refreshDrawerInfo(storageState === StorageState.Unknown)
            }

            override fun onDrawerClosed(drawerView: View) {}

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
        (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment)?.let {
            setupNavDestListener(it)
        }
    }

    override fun addDrawerListener(listener: DrawerLayout.DrawerListener) {
        drawerLayout.addDrawerListener(listener)
    }

    override fun removeDrawerListener(listener: DrawerLayout.DrawerListener) {
        drawerLayout.removeDrawerListener(listener)
    }

    private fun onTransfersWidgetClick() {
        megaNavigator.openTransfersAndConsumeErrorStatus(
            this@ManagerActivity,
            transfersManagementViewModel,
        )
    }

    private fun initialiseChatBadgeView() {
        val itemView = menuView.getChildAt(3) as ViewGroup
        chatBadge = LayoutInflater.from(this).inflate(R.layout.bottom_chat_badge, menuView, false)
            .apply { isVisible = false }
        itemView.addView(chatBadge)
        callBadge = LayoutInflater.from(this).inflate(R.layout.bottom_call_badge, menuView, false)
        itemView.addView(callBadge)
        callBadge.visibility = View.GONE
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

    private fun checkDatabaseValues(): Boolean {
        val ephemeral =
            runBlocking { runCatching { monitorEphemeralCredentialsUseCase().firstOrNull() }.getOrNull() }
        if (ephemeral != null) {
            refreshSession()
            return true
        }
        if (credentials == null) {
            if (intent != null) {
                if (intent.action != null) {
                    if (intent.action == Constants.ACTION_EXPORT_MASTER_KEY || intent.action == Constants.ACTION_OPEN_MEGA_LINK || intent.action == Constants.ACTION_OPEN_MEGA_FOLDER_LINK) {
                        openLink = true
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
        setIsFirstLaunch()

        return false
    }

    private fun setIsFirstLaunch() {
        firstTimeAfterInstallation =
            intent.getBooleanExtra(IntentConstants.EXTRA_FIRST_LAUNCH, false)
        intent.removeExtra(IntentConstants.EXTRA_FIRST_LAUNCH)
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
            applicationScope.launch {
                MegaMessageService.getToken(workManager, crashReporter)
            }
            userInfoViewModel.getUserInfo()
            if (savedInstanceState == null) {
                // Check the consistency of the offline nodes in the database and sync files
                viewModel.startOfflineSyncWorker()
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
                        val highlightedNames =
                            intent.getStringArrayListExtra(FileStorageActivity.EXTRA_FILE_NAMES)
                        if (intent.getBooleanExtra(
                                Constants.SHOW_MESSAGE_UPLOAD_STARTED,
                                false
                            )
                        ) {
                            val numberUploads: Int =
                                intent.getIntExtra(Constants.NUMBER_UPLOADS, 1)
                            showSnackbar(
                                SNACKBAR_TYPE,
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
                                SNACKBAR_TYPE,
                                it,
                                MEGACHAT_INVALID_HANDLE
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
                                        fileBrowserViewModel.setFileBrowserHandle(handleIntent)
                                        selectDrawerItem(drawerItem)
                                        selectDrawerItemPending = false
                                    }

                                    megaApi.rubbishNode?.handle -> {
                                        drawerItem = DrawerItem.RUBBISH_BIN
                                        rubbishBinViewModel.setRubbishBinHandle(handleIntent)
                                        selectDrawerItem(drawerItem)
                                        selectDrawerItemPending = false
                                    }

                                    megaApi.vaultNode?.handle -> {
                                        drawerItem = DrawerItem.BACKUPS
                                        backupsFragment?.updateBackupsHandle(handleIntent)
                                        selectDrawerItem(drawerItem)
                                        selectDrawerItemPending = false
                                    }

                                    else -> {
                                        //Incoming
                                        drawerItem = DrawerItem.SHARED_ITEMS
                                        viewModel.setSharesTab(SharesTab.INCOMING_TAB)
                                        incomingSharesViewModel.setCurrentHandle(handleIntent)
                                        selectDrawerItem(drawerItem)
                                        selectDrawerItemPending = false
                                    }
                                }
                            }
                        } else {
                            actionOpenFolder(handleIntent, highlightedNames)
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
                            MEGACHAT_INVALID_HANDLE
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
                        viewModel.checkLink(intent.dataString)
                        intent.action = null
                        intent = null
                    } else if (intent.action == Constants.ACTION_JOIN_OPEN_CHAT_LINK) {
                        linkJoinToChatLink = intent.dataString
                        joiningToChatLink = true
                        if (megaChatApi.connectionState == MegaChatApi.CONNECTED) {
                            viewModel.checkLink(linkJoinToChatLink)
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
                            pN?.handle?.let { fileBrowserViewModel.setFileBrowserHandle(it) }
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
                            MEGACHAT_INVALID_HANDLE
                        )
                        showSnackbar(Constants.MESSAGE_SNACKBAR_TYPE, null, chatId)
                        intent.action = null
                        intent = null
                    }
                }
            }
            Timber.d("Check if there any INCOMING pendingRequest contacts")
            viewModel.checkNumUnreadUserAlerts(UnreadUserAlertsCheckType.NOTIFICATIONS_TITLE)
            if (drawerItem == null) {
                drawerItem = startScreenUtil.getStartDrawerItem()
                if (intent != null) {
                    val upgradeAccount: Boolean =
                        intent.getBooleanExtra(ExtraConstant.EXTRA_UPGRADE_ACCOUNT, false)
                    newAccount =
                        intent.getBooleanExtra(ExtraConstant.EXTRA_NEW_ACCOUNT, false)
                    newCreationAccount =
                        intent.getBooleanExtra(ExtraConstant.NEW_CREATION_ACCOUNT, false)
                    firstLogin =
                        intent.getBooleanExtra(ExtraConstant.EXTRA_FIRST_LOGIN, false)
                    viewModel.setIsFirstLogin(firstLogin)
                    setRequestNotificationsPermissionFirstLogin(savedInstanceState)
                    askPermissions = intent.getBooleanExtra(
                        IntentConstants.EXTRA_ASK_PERMISSIONS,
                        askPermissions
                    )

                    //reset flag to fix incorrect view loaded when orientation changes
                    intent.removeExtra(ExtraConstant.EXTRA_NEW_ACCOUNT)
                    intent.removeExtra(ExtraConstant.EXTRA_UPGRADE_ACCOUNT)
                    intent.removeExtra(ExtraConstant.EXTRA_FIRST_LOGIN)
                    intent.removeExtra(IntentConstants.EXTRA_ASK_PERMISSIONS)
                    if (upgradeAccount) {
                        val accountType: Int =
                            intent.getIntExtra(ExtraConstant.EXTRA_ACCOUNT_TYPE, 0)
                        if (accountType != AccountTypeInt.FREE) {
                            showMyAccount(
                                android.util.Pair(
                                    ExtraConstant.EXTRA_ACCOUNT_TYPE,
                                    accountType
                                )
                            )
                        } else if (firstLogin && viewModel.getStorageState() !== StorageState.PayWall) {
                            drawerItem = DrawerItem.HOMEPAGE
                        } else {
                            showMyAccount()
                        }
                    } else {
                        if (firstLogin && viewModel.getStorageState() !== StorageState.PayWall) {
                            drawerItem = DrawerItem.HOMEPAGE
                            intent = null
                        }
                    }
                }
            } else {
                Timber.d("DRAWERITEM NOT NULL: %s", drawerItem)
                if (intent != null) {
                    val upgradeAccount: Boolean =
                        intent.getBooleanExtra(ExtraConstant.EXTRA_UPGRADE_ACCOUNT, false)
                    newAccount =
                        intent.getBooleanExtra(ExtraConstant.EXTRA_NEW_ACCOUNT, false)
                    newCreationAccount = intent.getBooleanExtra(ExtraConstant.NEW_CREATION_ACCOUNT, false)
                    //reset flag to fix incorrect view loaded when orientation changes
                    intent.removeExtra(ExtraConstant.EXTRA_NEW_ACCOUNT)
                    intent.removeExtra(ExtraConstant.EXTRA_UPGRADE_ACCOUNT)
                    firstLogin = intent.getBooleanExtra(ExtraConstant.EXTRA_FIRST_LOGIN, false)
                    viewModel.setIsFirstLogin(firstLogin)
                    setRequestNotificationsPermissionFirstLogin(savedInstanceState)
                    askPermissions = intent.getBooleanExtra(
                        IntentConstants.EXTRA_ASK_PERMISSIONS,
                        askPermissions
                    )
                    if (upgradeAccount) {
                        closeDrawer()
                        val accountType: Int =
                            intent.getIntExtra(ExtraConstant.EXTRA_ACCOUNT_TYPE, 0)
                        if (accountType != AccountTypeInt.FREE) {
                            showMyAccount(
                                android.util.Pair(
                                    ExtraConstant.EXTRA_ACCOUNT_TYPE,
                                    accountType
                                )
                            )
                        } else if (firstLogin && viewModel.getStorageState() !== StorageState.PayWall) {
                            drawerItem = DrawerItem.HOMEPAGE
                        } else {
                            showMyAccount()
                        }
                    } else {
                        if (firstLogin && !joiningToChatLink) {
                            intent = null
                        }
                    }
                    if (intent?.action != null) {
                        if (intent.action == Constants.ACTION_REFRESH_AFTER_BLOCKED) {
                            drawerItem = DrawerItem.CLOUD_DRIVE
                            intent = null
                        }
                    }
                }
                closeDrawer()
            }
            checkCurrentStorageStatus()

            // Load the initial Fragment after logging in for the first time
            if (selectDrawerItemPending) {
                selectDrawerItem(drawerItem)
            }
        }
        return false
    }

    private fun handleRedirectIntentActions(intent: Intent?): Boolean {
        intent ?: return false
        managerRedirectIntentMapper(
            intent = intent,
        )?.let { redirectIntent ->
            startActivity(redirectIntent)
            finish()
            return true
        }

        return false
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
        isInAlbumContent = savedInstanceState.getBoolean(STATE_KEY_IS_IN_ALBUM_CONTENT, false)
        if (isInAlbumContent) {
            albumContentFragment = supportFragmentManager.getFragment(
                savedInstanceState,
                FragmentTag.ALBUM_CONTENT.tag
            )
        }
        isInFilterPage = savedInstanceState.getBoolean(STATE_KEY_IS_IN_PHOTOS_FILTER, false)

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
    }

    /**
     * collecting Flows from ViewModels
     */
    private fun collectFlows() {
        this.collectFlow(
            viewModel.state,
            Lifecycle.State.STARTED
        ) { managerState: ManagerState ->
            val nodeNameCollisionResult = managerState.nodeNameCollisionsResult
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
                    SNACKBAR_TYPE,
                    getString(R.string.success_archive_chat, managerState.titleChatArchivedEvent),
                    MEGACHAT_INVALID_HANDLE
                )
                viewModel.onChatArchivedEventConsumed()
            }

            if (managerState.message != null) {
                showSnackbar(content = managerState.message.getInfo(this))
                viewModel.markHandledMessage()
            }

            managerState.gmsDocumentScanner?.let { gmsDocumentScanner ->
                uploadBottomSheetDialogActionHandler.scanDocument(gmsDocumentScanner)
                viewModel.onGmsDocumentScannerConsumed()
            }

            managerState.chatLinkContent?.let {
                handleCheckLinkResult(it)
                viewModel.markHandleCheckLinkResult()
            }


            if (managerState.uploadEvent is StateEventWithContentTriggered) {
                startDownloadViewModel.onUploadClicked(managerState.uploadEvent.content)
            }
        }
        syncMonitorViewModel.startMonitoring()
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
            syncMonitorViewModel.state,
            Lifecycle.State.CREATED
        ) { state: SyncMonitorState ->
            state.displayNotification?.let { notification ->
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    var notificationId: Int? = null
                    if (!syncNotificationManager.isSyncNotificationDisplayed()) {
                        notificationId =
                            syncNotificationManager.show(this@ManagerActivity, notification)
                    }
                    syncMonitorViewModel.onNotificationShown(
                        syncNotificationMessage = notification,
                        notificationId = notificationId,
                    )
                }
            }
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
                    SNACKBAR_TYPE,
                    it,
                    MEGACHAT_INVALID_HANDLE
                )
                waitingRoomManagementViewModel.onConsumeSnackBarMessageEvent()
            }
            if (state.shouldWaitingRoomBeShown) {
                waitingRoomManagementViewModel.onConsumeShouldWaitingRoomBeShownEvent()
                launchCallScreen()
            }
        }

        collectFlow(
            viewModel.monitorNumUnreadChats,
            Lifecycle.State.RESUMED
        ) { unreadCount ->
            updateChatBadge(unreadCount)
        }

        collectFlow(fileBrowserViewModel.state.map { it.selectedTab }.distinctUntilChanged()) {
            if (it != CloudDriveTab.NONE && drawerItem == DrawerItem.CLOUD_DRIVE) {
                val shouldVisible = it == CloudDriveTab.CLOUD
                searchMenuItem?.isVisible = shouldVisible
                openLinkMenuItem?.isVisible = shouldVisible
                moreMenuItem?.isVisible = !shouldVisible
                fabButton.isInvisible =
                    (!shouldVisible || fileBrowserViewModel.isMediaDiscoveryOpen())
            }
        }
    }

    /**
     * When the system fails to open the ML Kit Document Scanner, display a generic error message
     */
    fun onDocumentScannerFailedToOpen() {
        viewModel.onDocumentScannerFailedToOpen()
    }

    /**
     * Open meeting
     */
    private fun launchCallScreen() {
        val chatId = waitingRoomManagementViewModel.state.value.chatId
        MegaApplication.getInstance().openCallService(chatId)
        passcodeFacade.enablePassCode()

        val intent = Intent(this, MeetingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            action = MeetingActivity.MEETING_ACTION_IN
            putExtra(MeetingActivity.MEETING_CHAT_ID, chatId)
            putExtra(MeetingActivity.MEETING_BOTTOM_PANEL_EXPANDED, true)
        }
        startActivity(intent)
    }

    private fun handleMovementResult(moveRequestResult: Result<MoveRequestResult>) {
        if (moveRequestResult.isSuccess) {
            val data = moveRequestResult.getOrThrow()
            if (data !is MoveRequestResult.DeleteMovement && data !is MoveRequestResult.Copy) {
                showMovementResult(data, data.nodes.first())
            }
            showSnackbar(
                SNACKBAR_TYPE,
                nodeMoveRequestMessageMapper(data),
                MEGACHAT_INVALID_HANDLE
            )
        } else {
            manageCopyMoveException(moveRequestResult.exceptionOrNull())
        }
    }

    private fun handleNodesNameCollisionResult(result: NodeNameCollisionsResult) {
        if (result.conflictNodes.isNotEmpty()) {
            nameCollisionActivityLauncher.launch(result.conflictNodes.values.toCollection(ArrayList()))
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
            val sharedItemsView = menuView.getChildAt(4) as? ViewGroup ?: return
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
                val sharedItemsView = menuView.getChildAt(4) as? ViewGroup ?: return
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

            checkProFlexiStatus() -> {
                myAccountInfo.isBusinessAlertShown = true
            }

            firstTimeAfterInstallation || askPermissions || newCreationAccount -> {
                // Set this value to false after successfully logging in for the first time
                if (firstTimeAfterInstallation) {
                    firstTimeAfterInstallation = false
                    requestNotificationsPermissionFirstLogin = false
                    dbH.setFirstTime(false)
                }
                if (!initialPermissionsAlreadyAsked && !onAskingPermissionsFragment) {
                    askForAccess()
                }
            }

            requestNotificationsPermissionFirstLogin -> {
                askForNotificationsPermission()
            }
        }
    }

    /**
     * Checks the onboarding is pending or in progress
     *
     * @return True in case the the onboarding is pending or in progress. False otherwise.
     */
    private fun isOnboarding(): Boolean {
        when {
            drawerItem == DrawerItem.ASK_PERMISSIONS -> return true

            viewModel.state.value.isFirstLogin && drawerItem != DrawerItem.PHOTOS -> return true

            firstTimeAfterInstallation || askPermissions || newCreationAccount -> {
                if (!initialPermissionsAlreadyAsked && !onAskingPermissionsFragment) {
                    return true
                }
            }

            requestNotificationsPermissionFirstLogin -> return true
        }
        return false
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

    private fun checkProFlexiStatus(): Boolean {
        if (!isProFlexiAccount) return false
        if (myAccountInfo.isBusinessAlertShown) {
            return false
        }
        if (viewModel.state().isFirstLogin && myAccountInfo.wasNotBusinessAlertShownYet()) {
            val status: Int = megaApi.businessStatus
            if (status == MegaApiJava.BUSINESS_STATUS_EXPIRED) {
                myAccountInfo.isBusinessAlertShown = true
                startActivity(Intent(this, BusinessExpiredAlertActivity::class.java))
                return true
            }
        }
        return false
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

    private fun askForAccess() {
        drawerItem = DrawerItem.ASK_PERMISSIONS
        askPermissions = false
        showStorageAlertWithDelay = true

        val shouldShowNotificationPermission =
            intent?.getBooleanExtra(
                IntentConstants.EXTRA_SHOW_NOTIFICATION_PERMISSION,
                false
            ) == true

        val needsToShowPermissions =
            (!isNotificationPermissionGranted() && shouldShowNotificationPermission) || hasMissingPermission()

        if (needsToShowPermissions) {
            val currentFragment =
                supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (currentFragment?.tag != FragmentTag.PERMISSIONS.tag) {
                deleteCurrentFragment()
            }

            if (permissionsFragment == null) {
                permissionsFragment = PermissionsFragment().apply {
                    if (shouldShowNotificationPermission) {
                        arguments = bundleOf(
                            IntentConstants.EXTRA_SHOW_NOTIFICATION_PERMISSION to true
                        )
                    }
                }
            }

            permissionsFragment?.let {
                replaceFragment(it, FragmentTag.PERMISSIONS.tag)
            }

            onAskingPermissionsFragment = true
            selectDrawerItem(drawerItem)
        }
    }

    private fun restrictUiWhileRequestingPermissions() {
        if (onAskingPermissionsFragment && permissionsFragment != null) {
            setAppBarVisibility(false)
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            supportInvalidateOptionsMenu()
            hideFabButton()
            showHideBottomNavigationView(true)
        }
    }

    private fun hasMissingPermission(): Boolean {
        val context = this@ManagerActivity
        // Temporary: This will be removed later once the feature flag is removed. We cannot call this
        // from the ViewModel because it creates a weird race condition. The "askForAccess" is called
        // before the feature flag value is set.
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
        val readStorageGranted: Boolean = hasPermissions(context, *permissions)

        // Check if any permission is missing. Onboarding revamp only requires notifications and read storage
        // permissions, while the old onboarding requires all of them
        return listOf(isNotificationPermissionGranted(), readStorageGranted).any { !it }
    }

    private fun isNotificationPermissionGranted(): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || hasPermissions(this, Manifest.permission.POST_NOTIFICATIONS))

    fun destroyPermissionsFragment(isCameraUploadsEnabled: Boolean) {
        initialPermissionsAlreadyAsked = true
        //In mobile, allow all orientation after permission screen
        if (isTablet().not()) {
            Timber.d("Mobile, all orientation")
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        }
        turnOnNotifications = false
        setAppBarVisibility(true)
        deleteCurrentFragment()
        onAskingPermissionsFragment = false
        permissionsFragment = null
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        supportInvalidateOptionsMenu()
        drawerItem = when {
            viewModel.getStorageState() === StorageState.PayWall -> {
                DrawerItem.CLOUD_DRIVE
            }

            isCameraUploadsEnabled -> {
                DrawerItem.PHOTOS
            }

            else -> {
                viewModel.setIsFirstLogin(true)
                DrawerItem.HOMEPAGE
            }
        }
        selectDrawerItem(drawerItem)
        if (isCameraUploadsEnabled) {
            showSnackbar(
                SNACKBAR_TYPE,
                getString(R.string.onboarding_camera_upload_permission_enabled_success_message),
                -1
            )
        }
    }

    /**
     * Checks for the screen orientation and handle showing the Ads view
     */
    fun handleShowingAds() {
        //slotId is not used for now during the implementation of the new Ads SDK
        if (isShowingAds()) {
            showAdsView()
            showBNVImmediate()
            showHideBottomNavigationView(hide = false)
        } else {
            hideAdsView()
        }
        updateHomepageFabPosition()
    }

    private fun isShowingAds(): Boolean {
        return (isTablet() || isPortrait()) && googleAdsManager.isAdsEnabled()
                && googleAdsManager.hasAdsRequest() &&
                (drawerItem == DrawerItem.CLOUD_DRIVE && !fileBrowserViewModel.isInSelection()
                        || drawerItem == DrawerItem.CHAT
                        || drawerItem == DrawerItem.SHARED_ITEMS
                        || drawerItem == DrawerItem.HOMEPAGE && homepageScreen == HomepageScreen.HOMEPAGE
                        || drawerItem == DrawerItem.HOMEPAGE && homepageScreen == HomepageScreen.FAVOURITES
                        || drawerItem == DrawerItem.HOMEPAGE && homepageScreen == HomepageScreen.DOCUMENTS
                        || drawerItem == DrawerItem.HOMEPAGE && homepageScreen == HomepageScreen.AUDIO
                        || drawerItem == DrawerItem.HOMEPAGE && homepageScreen == HomepageScreen.VIDEO_SECTION
                        || isInPhotosPage)
    }

    private fun setupAdsView() {
        collectFlow(adsContainerViewModel.isAdsLoaded.filterNotNull()) {
            if (it) {
                handleShowingAds()
            } else {
                hideAdsView()
                showBNVImmediate()
                showHideBottomNavigationView(hide = false)
                updateHomepageFabPosition()
            }
        }
        adsContainerView.setContent {
            val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val request by googleAdsManager.request.collectAsStateWithLifecycle()
            OriginalTheme(isDark = themeMode.isDarkMode()) {
                AdsContainer(
                    modifier = Modifier.fillMaxWidth(),
                    request = request,
                    viewModel = adsContainerViewModel,
                )
            }
        }
    }

    /**
     * Fetch a new Ad by fetching a new AdRequest and loading it into the AdView
     */
    private fun fetchNewAd() {
        googleAdsManager.fetchAdRequest()
    }

    private fun showAdsView() {
        adsContainerView.isVisible = true
    }

    fun hideAdsView() {
        adsContainerView.isVisible = false
    }

    override fun onResume() {
        super.onResume()
        queryIfNotificationsAreOn()
        if (resources.configuration.orientation != orientationSaved) {
            orientationSaved = resources.configuration.orientation
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        }
        checkScrollElevation()
        checkForInAppUpdateInstallStatus()
        cookieDialogHandler.onResume()
        updateTransfersWidgetVisibility()
    }

    private fun checkForInAppUpdateInstallStatus() {
        lifecycleScope.launch {
            runCatching {
                inAppUpdateHandler.checkForInAppUpdateInstallStatus()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return
        }
        //This restriction is already enforced indirectly,
        // but stated here explicitly as statusBarColor should not be set from Android 15
        Timber.d("deleteTurnOnNotificationsFragment")
        turnOnNotifications = false
        setAppBarVisibility(true)
        turnOnNotificationsFragment = null
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        supportInvalidateOptionsMenu()
        selectDrawerItem(drawerItem)
        setAppBarColor(ContextCompat.getColor(this, R.color.app_background))
    }

    private fun deleteCurrentFragment() {
        val currentFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment != null) {
            supportFragmentManager.commitNow(allowStateLoss = true) { remove(currentFragment) }
        }
    }


    private fun setTurnOnNotificationsFragment() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return
        }
        //This restriction is already enforced in the calling function,
        // but stated here explicitly as statusBarColor should not be set from Android 15
        Timber.d("setTurnOnNotificationsFragment")
        supportActionBar?.subtitle = null
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
        setAppBarVisibility(false)
        closeDrawer()
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        supportInvalidateOptionsMenu()
        hideFabButton()
        showHideBottomNavigationView(true)
        setAppBarColor(ContextCompat.getColor(this, R.color.teal_500_teal_400))
    }

    private fun actionOpenFolder(handleIntent: Long, highlightedNames: List<String>? = null) {
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
                incomingSharesViewModel.setCurrentHandle(
                    handleIntent,
                    highlightedNames = highlightedNames,
                )
                DrawerItem.SHARED_ITEMS
            }

            else -> if (megaApi.isInRubbish(parentIntentN)) {
                rubbishBinViewModel.setRubbishBinHandle(handleIntent)
                DrawerItem.RUBBISH_BIN
            } else if (megaApi.isInVault(parentIntentN)) {
                backupsFragment?.updateBackupsHandle(handleIntent)
                DrawerItem.BACKUPS
            } else {
                fileBrowserViewModel.setFileBrowserHandle(
                    handleIntent,
                    highlightedNames = highlightedNames
                )
                DrawerItem.CLOUD_DRIVE
            }
        }
    }

    override fun onPostResume() {
        Timber.d("onPostResume")
        super.onPostResume()
        if (credentials == null) {
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
            fileBrowserViewModel.setFileBrowserHandle(
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
                        megaNavigator.openZipBrowserActivity(this, it) {
                            showSnackbar(
                                SNACKBAR_TYPE,
                                getString(R.string.message_zip_format_error),
                                MEGACHAT_INVALID_HANDLE
                            )
                        }
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
                    val fileLinkIntent = Intent(this, FileLinkComposeActivity::class.java)
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

                Constants.ACTION_LOCATE_DOWNLOADED_FILE -> {
                    handleLocateFileNavigationFromIntent()
                }

                Constants.ACTION_SHOW_WARNING -> {
                    val message = intent.getStringExtra(Constants.INTENT_EXTRA_WARNING_MESSAGE)
                    showSnackbar(SNACKBAR_TYPE, message, -1)
                }

                Constants.ACTION_TAKE_SELFIE -> {
                    Timber.d("Intent take selfie")
                    Util.checkTakePicture(this, Constants.TAKE_PHOTO_CODE)
                }

                Constants.SHOW_REPEATED_UPLOAD -> {
                    Timber.d("Intent SHOW_REPEATED_UPLOAD")
                    val message = intent.getStringExtra("MESSAGE")
                    showSnackbar(SNACKBAR_TYPE, message, -1)
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
                        MEGACHAT_INVALID_HANDLE
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
                    val highlightedNames =
                        intent.getStringArrayListExtra(FileStorageActivity.EXTRA_FILE_NAMES)
                    if (intent.getBooleanExtra(Constants.SHOW_MESSAGE_UPLOAD_STARTED, false)) {
                        val numberUploads: Int =
                            intent.getIntExtra(Constants.NUMBER_UPLOADS, 1)
                        showSnackbar(
                            SNACKBAR_TYPE,
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
                            SNACKBAR_TYPE,
                            it,
                            MEGACHAT_INVALID_HANDLE
                        )
                    }
                    actionOpenFolder(handleIntent, highlightedNames)
                    selectDrawerItem(drawerItem)
                }

                Constants.ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE -> {
                    val chatId: Long = intent.getLongExtra(
                        Constants.CHAT_ID,
                        MEGACHAT_INVALID_HANDLE
                    )
                    showSnackbar(Constants.MESSAGE_SNACKBAR_TYPE, null, chatId)
                }
            }
            intent?.action = null
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

            DrawerItem.CHAT -> {
                setBottomNavigationMenuItemChecked(CHAT_BNV)
            }

            DrawerItem.PHOTOS -> {
                setBottomNavigationMenuItemChecked(PHOTOS_BNV)
            }

            DrawerItem.NOTIFICATIONS -> {
                hideAdsView()
            }

            DrawerItem.HOMEPAGE -> {
                setBottomNavigationMenuItemChecked(HOME_BNV)
            }

            else -> {
                setBottomNavigationMenuItemChecked(HOME_BNV)
            }
        }
    }

    private fun openChat(chatId: Long, text: String?) {
        Timber.d("Chat ID: %s", chatId)
        if (chatId != -1L) {
            val chat = megaChatApi.getChatRoom(chatId)
            if (chat != null) {
                megaNavigator.openChat(
                    context = this,
                    chatId = chatId,
                    action = Constants.ACTION_CHAT_SHOW_MESSAGES,
                    text = text
                )
                Timber.d("Open chat with id: %s", chatId)
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
        super.onPause()
    }

    override fun onDestroy() {
        Timber.d("onDestroy()")
        reconnectDialog?.cancel()
        dismissAlertDialogIfExists(processFileDialog)
        cookieDialogHandler.onDestroy()
        super.onDestroy()
    }

    override fun exitCloudDrive() = performOnBack()

    override fun updateCloudDriveToolbarTitle(invalidateOptionsMenu: Boolean) {
        if (invalidateOptionsMenu) {
            invalidateOptionsMenu()
        }
        setToolbarTitle()
    }

    override fun exitSharesPage() = performOnBack()

    override fun updateSharesPageToolbarTitleAndFAB(invalidateOptionsMenu: Boolean) {
        setToolbarTitle()
        showFabButton()
        if (invalidateOptionsMenu) {
            invalidateOptionsMenu()
        }
    }

    override suspend fun showMediaDiscoveryFromCloudDrive(
        mediaHandle: Long,
        isAccessedByIconClick: Boolean,
        replaceFragment: Boolean,
        @StringRes errorMessage: Int?,
    ) {
        lifecycle.withStarted {
            with(fileBrowserViewModel) {
                if (isFromSyncTab().not() || (isFromSyncTab() && state().openedFolderNodeHandles.size == 1) || state().isMediaDiscoveryOpenedByIconClick) {
                    // this call back shouldn't be called when media discovery is already opened from the Manager Activity directly from sync tab
                    // this should be called only when file browser is already opened (state().openedFolderNodeHandles.size == 1)
                    showMediaDiscovery(
                        mediaHandle = mediaHandle,
                        isAccessedByIconClick = isAccessedByIconClick,
                        replaceFragment = replaceFragment,
                        errorMessage = errorMessage,
                    )
                }
            }
        }
    }

    /**
     * Displays the Media Discovery
     *
     * @param mediaHandle The Folder Handle containing Media to be displayed in that View
     * @param isAccessedByIconClick True if Media Discovery is accessed by clicking the Media
     * Discovery Icon
     * @param errorMessage The [StringRes] of the error message to display
     */
    private fun showMediaDiscovery(
        mediaHandle: Long,
        isAccessedByIconClick: Boolean,
        replaceFragment: Boolean = false,
        @StringRes errorMessage: Int?,
    ) {
        transfersManagementViewModel.hideTransfersWidget()
        // Remove the existing Media Discovery View first
        mediaDiscoveryFragment?.let { removeFragment(it) }
        MediaDiscoveryFragment.newInstance(
            mediaHandle = mediaHandle,
            isAccessedByIconClick = isAccessedByIconClick,
            errorMessage = errorMessage,
        ).apply {
            if (replaceFragment) {
                replaceFragment(this, FragmentTag.MEDIA_DISCOVERY.tag)
            } else {
                addFragment(this, FragmentTag.MEDIA_DISCOVERY.tag)
            }
        }
        with(viewModel) {
            onMediaDiscoveryOpened(mediaHandle)
            setIsFirstNavigationLevel(false)
        }
    }

    fun skipToAlbumContentFragment(fragment: Fragment) {
        albumContentFragment = fragment
        replaceFragment(fragment, FragmentTag.ALBUM_CONTENT.tag)
        isInAlbumContent = true
        viewModel.setIsFirstNavigationLevel(false)
        hideAdsView()
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

    private fun addFragment(fragment: Fragment, fragmentTag: String?) {
        supportFragmentManager.apply {
            beginTransaction().apply {
                add(R.id.fragment_container, fragment, fragmentTag)
            }.commit()
            executePendingTransactions()
        }
    }

    /**
     * Displays the specified [Fragment]
     *
     * If the specified [Fragment] [fragmentToReplace] is not null,
     * then [FragmentTransaction.show] or [FragmentTransaction.add] is called depending if
     * [fragmentToReplace] was added to the [Activity] or not
     *
     * Otherwise, [FragmentTransaction.replace] is called using the [newFragmentInstance] and adds
     * it to the Back Stack
     *
     * @param fragmentToReplace The specified [Fragment], which can be nullable
     * @param newFragmentInstance An instantiated [Fragment] if [fragmentToReplace] is null
     * @param fragmentTag An optional [Fragment] tag
     */
    private fun replaceFragmentWithBackStack(
        fragmentToReplace: Fragment?,
        newFragmentInstance: Fragment,
        fragmentTag: String?,
    ) {
        supportFragmentManager.apply {
            beginTransaction().apply {
                fragmentToReplace?.let { nonNullFragmentToReplace ->
                    if (nonNullFragmentToReplace.isAdded) {
                        show(nonNullFragmentToReplace)
                    } else {
                        add(R.id.fragment_container, nonNullFragmentToReplace, fragmentTag)
                    }
                } ?: run {
                    replace(R.id.fragment_container, newFragmentInstance, fragmentTag)
                    addToBackStack(newFragmentInstance::class.java.name)
                }
            }.commit()
            executePendingTransactions()
        }
    }

    private fun refreshFragment(fragmentTag: String) {
        supportFragmentManager.findFragmentByTag(fragmentTag)?.let {
            Timber.d("Fragment %s refreshing", fragmentTag)
            supportFragmentManager.commitNow(allowStateLoss = true) {
                detach(it)
                attach(it)
            }
        }
    }

    private fun selectDrawerItemCloudDrive() {
        Timber.d("selectDrawerItemCloudDrive")
        setAppBarVisibility(true)
        cloudDriveSyncsFragment =
            (supportFragmentManager.findFragmentByTag(FragmentTag.CLOUD_DRIVE_SYNCS.tag) as? CloudDriveSyncsFragment
                ?: CloudDriveSyncsFragment.newInstance()).also {
                replaceFragment(it, FragmentTag.CLOUD_DRIVE_SYNCS.tag)
            }
    }

    private fun showGlobalAlertDialogsIfNeeded() {
        if (showStorageAlertWithDelay) {
            showStorageAlertWithDelay = false
            checkStorageStatus(
                if (storageStateFromBroadcast !== StorageState.Unknown) storageStateFromBroadcast else viewModel.getStorageState()
            )
        }
        Timber.d("Should force refresh contact database after logging in for the first time - $firstTimeAfterInstallation")
        userInfoViewModel.refreshContactDatabase(firstTimeAfterInstallation)
        cookieDialogHandler.showDialogIfNeeded(
            context = this,
            onButtonClicked = {
                handleShowingAds()
                checkForInAppAdvertisement(forceAskConsent = true)
            },
        )
    }

    fun setToolbarTitle(title: String?) {
        supportActionBar?.title = title
    }

    fun setToolbarTitle() = lifecycleScope.launch {
        Timber.d("setToolbarTitle")
        if (drawerItem == null) {
            return@launch
        }

        supportActionBar?.subtitle = null

        when (drawerItem) {
            DrawerItem.CLOUD_DRIVE -> {
                Timber.d("Cloud Drive SECTION")
                val parentNode = withContext(ioDispatcher) {
                    megaApi.getNodeByHandle(fileBrowserViewModel.state().fileBrowserHandle)
                }
                if (parentNode != null) {
                    if (megaApi.rootNode != null) {
                        if ((parentNode.handle == megaApi.rootNode?.handle
                                    || fileBrowserViewModel.state().fileBrowserHandle == -1L)
                            && !fileBrowserViewModel.isMediaDiscoveryOpen()
                        ) {
                            supportActionBar?.title =
                                getString(sharedR.string.general_drive)
                            viewModel.setIsFirstNavigationLevel(true)
                        } else {
                            supportActionBar?.title = parentNode.name
                            viewModel.setIsFirstNavigationLevel(false)
                        }
                    } else {
                        fileBrowserViewModel.setFileBrowserHandle(-1)
                    }
                } else {
                    if (megaApi.rootNode != null) {
                        if (fileBrowserViewModel.state().fileBrowserHandle == INVALID_HANDLE) {
                            fileBrowserViewModel.setFileBrowserHandle(
                                megaApi.rootNode?.handle ?: INVALID_HANDLE
                            )
                            supportActionBar?.title =
                                getString(R.string.title_mega_info_empty_screen)
                            viewModel.setIsFirstNavigationLevel(true)
                        }
                    } else {
                        fileBrowserViewModel.setFileBrowserHandle(-1)
                        viewModel.setIsFirstNavigationLevel(true)
                    }
                }
            }

            DrawerItem.RUBBISH_BIN -> {
                val node =
                    megaApi.getNodeByHandle(rubbishBinViewModel.state().currentHandle)
                val rubbishNode = megaApi.rubbishNode
                if (rubbishNode == null) {
                    rubbishBinViewModel.setRubbishBinHandle(INVALID_HANDLE)
                    viewModel.setIsFirstNavigationLevel(true)
                } else if (rubbishBinViewModel.state().currentHandle == INVALID_HANDLE || node == null || node.handle == rubbishNode.handle) {
                    supportActionBar?.title = getString(sharedR.string.general_section_rubbish_bin)
                    viewModel.setIsFirstNavigationLevel(true)
                } else {
                    supportActionBar?.title = node.name
                    viewModel.setIsFirstNavigationLevel(false)
                }
            }

            DrawerItem.NOTIFICATIONS -> {
                supportActionBar?.title =
                    getString(R.string.title_properties_chat_contact_notifications)
                viewModel.setIsFirstNavigationLevel(true)
            }

            DrawerItem.CHAT -> {
                setAppBarVisibility(true)
                supportActionBar?.title = getString(R.string.section_chat)
                viewModel.setIsFirstNavigationLevel(true)
            }

            DrawerItem.PHOTOS -> {
                if (isInAlbumContent) {
                    if (albumContentFragment is AlbumContentFragment) {
                        val title = (albumContentFragment as? AlbumContentFragment)
                            ?.getCurrentAlbumTitle()
                        supportActionBar?.setTitle(title)
                    } else {
                        supportActionBar?.setTitle(getString(R.string.title_favourites_album))
                    }
                    viewModel.setIsFirstNavigationLevel(false)
                } else if (isInFilterPage) {
                    supportActionBar?.title = getString(R.string.photos_action_filter)
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
                        HomepageScreen.FAVOURITES -> {
                            // Don't override title if we're in a FavouriteFolderFragment (subfolder)
                            if (favouriteFolderFragment == null) {
                                titleId = R.string.favourites_category_title
                            }
                        }

                        HomepageScreen.DOCUMENTS -> titleId = R.string.section_documents
                        HomepageScreen.AUDIO -> titleId = R.string.upload_to_audio
                        HomepageScreen.RECENT_BUCKET -> {
                            getFragmentByType(
                                RecentActionBucketFragment::class.java
                            )?.setupToolbar()
                        }

                        HomepageScreen.FULLSCREEN_OFFLINE -> setToolbarTitleFromFullscreenOfflineFragment(
                            firstNavigationLevel = false,
                            showSearch = true
                        )

                        HomepageScreen.VIDEO_SECTION -> supportActionBar?.hide()
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

    fun setToolbarTitleFromFullscreenOfflineFragment(
        firstNavigationLevel: Boolean, showSearch: Boolean,
    ) {
        if (drawerItem == DrawerItem.HOMEPAGE && homepageScreen == HomepageScreen.FULLSCREEN_OFFLINE) {
            supportActionBar?.subtitle = fullscreenOfflineComposeFragment?.getCurrentPageSubTitle()
            supportActionBar?.title = fullscreenOfflineComposeFragment?.getCurrentPageTitle()
                ?: getString(R.string.section_saved_for_offline_new)
            viewModel.setIsFirstNavigationLevel(firstNavigationLevel)
            viewModel.checkNumUnreadUserAlerts(UnreadUserAlertsCheckType.NAVIGATION_TOOLBAR_ICON)
            searchMenuItem?.isVisible = showSearch
        }
    }

    private fun updateNavigationToolbarIcon(numUnreadUserAlerts: Int) {
        val totalIncomingContactRequestCount = viewModel.incomingContactRequests.value.size
        val totalNotifications = numUnreadUserAlerts + totalIncomingContactRequestCount
        if (totalNotifications == 0) {
            if (isFirstNavigationLevel) {
                if (drawerItem === DrawerItem.BACKUPS || drawerItem === DrawerItem.NOTIFICATIONS || drawerItem === DrawerItem.RUBBISH_BIN) {
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
            if (drawerItem === DrawerItem.PHOTOS) {
                setPhotosNavigationToolbarIcon()
            }
            if (isFirstNavigationLevel) {
                if (drawerItem === DrawerItem.BACKUPS || drawerItem === DrawerItem.NOTIFICATIONS || drawerItem === DrawerItem.RUBBISH_BIN) {
                    badgeDrawable.progress = 1.0f
                } else {
                    badgeDrawable.progress = 0.0f
                }
            } else {
                badgeDrawable.progress = 1.0f
            }
            if (totalNotifications > 9) {
                badgeDrawable.text = "9+"
            } else {
                badgeDrawable.text = totalNotifications.toString() + ""
            }
            supportActionBar?.setHomeAsUpIndicator(badgeDrawable)
        }
        if (drawerItem == DrawerItem.CLOUD_DRIVE) {
            if (fileBrowserViewModel.state().selectedTab != CloudDriveTab.SYNC) {
                openLinkMenuItem?.isVisible = isFirstNavigationLevel
                moreMenuItem?.isVisible = !isFirstNavigationLevel
            }
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
                if (MegaApplication.openChatId == MEGACHAT_INVALID_HANDLE) {
                    lifecycleScope.launch {
                        showSnackbar(
                            SNACKBAR_TYPE,
                            getString(R.string.login_connecting_to_server),
                            -1
                        )
                        val loginSuccess = viewModel.performFastLoginInBackground()
                        if (loginSuccess) {
                            rootNode = megaApi.rootNode
                            showSnackbar(
                                SNACKBAR_TYPE,
                                getString(mega.privacy.android.shared.resources.R.string.login_connected_to_server),
                                -1
                            )
                            if (isInMainHomePage) moveFromOfflineToOnlineMode()
                        } else {
                            navigateToLogin()
                        }
                    }

                }
            }
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
        finish()
    }

    private fun moveFromOfflineToOnlineMode() {
        try {
            if (drawerItem == null) {
                selectDrawerItem(DrawerItem.HOMEPAGE)
            }
            resetNavigationViewMenu(bottomNavigationView.menu)
            supportInvalidateOptionsMenu()
            checkForInAppAdvertisement(forceAskConsent = false)
            userInfoViewModel.getUserInfo()
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
                drawerItem = startScreenUtil.getStartDrawerItem()
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
                if (isInMainHomePage) {
                    handleShowingAds()
                }
            }

            DrawerItem.PHOTOS -> {
                setBottomNavigationMenuItemChecked(PHOTOS_BNV)
            }

            DrawerItem.SHARED_ITEMS -> {
                setBottomNavigationMenuItemChecked(SHARED_ITEMS_BNV)
                hideAdsView()
            }

            DrawerItem.CHAT -> {
                setBottomNavigationMenuItemChecked(CHAT_BNV)
            }

            DrawerItem.NOTIFICATIONS, DrawerItem.BACKUPS -> {
                setBottomNavigationMenuItemChecked(NO_BNV)
                hideAdsView()
            }

            else -> {}
        }
    }

    private fun selectDrawerItemNotifications() {
        Timber.d("selectDrawerItemNotifications")
        setAppBarVisibility(true)
        drawerItem = DrawerItem.NOTIFICATIONS
        setBottomNavigationMenuItemChecked(NO_BNV)
        replaceFragmentWithBackStack(
            fragmentToReplace = notificationsFragment,
            newFragmentInstance = NotificationsFragment.newInstance(),
            fragmentTag = FragmentTag.NOTIFICATIONS.tag,
        )
        setToolbarTitle()
        showFabButton()
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
        fragmentContainer.visibility = View.GONE
        navHostView.visibility = View.GONE
        updatePsaViewVisibility()
        if (turnOnNotifications) {
            fragmentContainer.visibility = View.VISIBLE
            closeDrawer()
            return
        }
        when (drawerItem) {
            DrawerItem.HOMEPAGE -> navHostView.visibility = View.VISIBLE
            else -> {
                fragmentContainer.visibility = View.VISIBLE
            }
        }
        closeDrawer()
    }

    /**
     * Removes a [Fragment] from the FragmentManager
     *
     * @param fragment the [Fragment] to be removed
     */
    private fun removeFragment(fragment: Fragment?) {
        fragment?.let { nonNullFragment ->
            if (!isFinishing) {
                supportFragmentManager.apply {
                    beginTransaction().apply { remove(nonNullFragment) }.commit()
                    executePendingTransactions()
                }
            }
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
                        setAppBarVisibility(false)
                    }
                    handleShowingAds()
                    updateTransfersWidgetVisibility()
                    setDrawerLockMode(false)
                    return@addOnDestinationChangedListener
                }

                R.id.favouritesFragment -> {
                    homepageScreen = HomepageScreen.FAVOURITES
                }

                R.id.documentSectionFragment -> {
                    homepageScreen = HomepageScreen.DOCUMENTS
                }

                R.id.audioSectionFragment -> {
                    homepageScreen = HomepageScreen.AUDIO
                }

                R.id.videoSectionFragment -> {
                    homepageScreen = HomepageScreen.VIDEO_SECTION
                }

                R.id.offlineFragmentCompose,
                    -> {
                    homepageScreen = HomepageScreen.FULLSCREEN_OFFLINE
                    hideAdsView()
                }

                R.id.offline_file_info_compose -> {
                    homepageScreen = HomepageScreen.OFFLINE_FILE_INFO
                    updatePsaViewVisibility()
                    setAppBarVisibility(false)
                    showHideBottomNavigationView(true)
                    hideAdsView()
                }

                R.id.recentBucketFragment -> {
                    homepageScreen = HomepageScreen.RECENT_BUCKET
                }
            }
            updateTransfersWidgetVisibility()
            updatePsaViewVisibility()
            if (destinationId != R.id.offlineFragmentCompose)
                setAppBarVisibility(true)
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
        lifecycleScope.launch {
            val oldDrawerItem = drawerItem
            isFirstTimeCam
            checkIfShouldCloseSearchView(oldDrawerItem)
            if (item == DrawerItem.OFFLINE) {
                bottomItemBeforeOpenFullscreenOffline = bottomNavigationCurrentItem
                openFullscreenOfflineFragment(pathNavigationOffline)
            } else {
                drawerItem = item
            }
            selectDrawerItem(drawerItem)
        }
    }

    /**
     * Select a drawer item
     *
     * @param item      [DrawerItem] to select
     * @param chatId    If the drawer item is the chat drawer item and it is selected when opening a
     *                  chat room, it is possible to pass here the chat ID to determine whether it
     *                  is a chat or a meeting and select the corresponding tab as the initial tab.
     * @param cloudDriveNodeHandle The Node Handle to immediately access a Cloud Drive Node, which
     * is set to [INVALID_HANDLE] by default
     * @param backupsHandle The Node Handle to immediately access a Backups Node, which is set to
     * [INVALID_HANDLE] by default
     * The value is set to -1 by default if no other Backups Node Handle is passed
     * @param errorMessage The [StringRes] of the error message to display
     * @param isFromSyncFolders Indicates if the Node to immediately access is from Sync Folders. False by default.
     */
    @SuppressLint("NewApi")
    @JvmOverloads
    fun selectDrawerItem(
        item: DrawerItem?,
        chatId: Long? = null,
        cloudDriveNodeHandle: Long = INVALID_HANDLE,
        backupsHandle: Long = INVALID_HANDLE,
        @StringRes errorMessage: Int? = null,
        isFromSyncFolders: Boolean = false,
    ) {
        Timber.d("Selected DrawerItem: ${item?.name}. Current drawerItem is ${drawerItem?.name}")
        if (!this::drawerLayout.isInitialized) {
            Timber.d("ManagerActivity doesn't call setContentView")
            return
        }
        drawerItem = item ?: DrawerItem.CLOUD_DRIVE
        callInProgressViewModel.setShow(
            resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                    && drawerItem != DrawerItem.NOTIFICATIONS && drawerItem != DrawerItem.HOMEPAGE
        )

        // Homepage may hide the Appbar before
        setAppBarVisibility(true)
        Util.resetActionBar(supportActionBar)
        updateTransfersWidgetVisibility()
        if (item !== DrawerItem.CHAT) {
            //remove recent chat fragment as its life cycle get triggered unexpectedly, e.g. rotate device while not on recent chat page
            removeFragment(chatsFragment)
        }
        if (item !== DrawerItem.SHARED_ITEMS) {
            removeFragment(supportFragmentManager.findFragmentByTag(SharesFragment.TAG))
        }
        if (item !== DrawerItem.PHOTOS) {
            resetCUFragment()
            isInAlbumContent = false
        }
        when (item) {
            DrawerItem.CLOUD_DRIVE -> {
                if (deviceCenterFragment?.isVisible == true) {
                    removeFragment(deviceCenterFragment)
                }
                // Synchronize the setting of different operations
                lifecycleScope.launch {
                    if (cloudDriveNodeHandle != INVALID_HANDLE) {
                        fileBrowserViewModel.setSyncFolderVisibility(isFromSyncFolders)
                        // Set the specific Folder to Cloud Drive
                        fileBrowserViewModel.openFileBrowserWithSpecificNode(
                            cloudDriveNodeHandle,
                            errorMessage
                        )
                    } else {
                        supportInvalidateOptionsMenu()
                    }
                    ensureActive() // the call openFileBrowserWithSpecificNode may take a long time to finish
                    handleCloudDriveNavigation()
                    if (openFolderRefresh) {
                        onNodesCloudDriveUpdate()
                        openFolderRefresh = false
                    }
                    setToolbarTitle()
                    showFabButton()
                    showHideBottomNavigationView(hide = false)
                    changeAppBarElevation(false)
                    if (!comesFromNotifications) {
                        bottomNavigationCurrentItem = CLOUD_DRIVE_BNV
                    }
                    setBottomNavigationMenuItemChecked(CLOUD_DRIVE_BNV)
                    intent?.run {
                        if (getBooleanExtra(Constants.INTENT_EXTRA_KEY_LOCATION_FILE_INFO, false)) {
                            fileBrowserViewModel.refreshNodes()
                        }
                    }
                    Analytics.tracker.trackEvent(CloudDriveBottomNavigationItemEvent)
                }
            }

            DrawerItem.RUBBISH_BIN -> {
                showHideBottomNavigationView(true)
                setAppBarVisibility(true)
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
                hideAdsView()
                changeAppBarElevation(false)
            }

            DrawerItem.DEVICE_CENTER -> {
                viewModel.setIsFirstNavigationLevel(true)
                setAppBarVisibility(false)
                setBottomNavigationMenuItemChecked(NO_BNV)
                supportInvalidateOptionsMenu()
                hideFabButton()
                hideAdsView()
                with(viewModel) {
                    // Only backup the previous Bottom Navigation item when Device Center is accessed
                    // from the Drawer
                    if (state().deviceCenterPreviousBottomNavigationItem == null) {
                        setDeviceCenterPreviousBottomNavigationItem(bottomNavigationCurrentItem)
                    }
                }
                replaceFragmentWithBackStack(
                    fragmentToReplace = deviceCenterFragment,
                    newFragmentInstance = DeviceCenterFragment.newInstance(),
                    fragmentTag = FragmentTag.DEVICE_CENTER.tag,
                )
            }

            DrawerItem.HOMEPAGE -> {
                // Don't use fabButton.hide() here.
                fabButton.visibility = View.GONE
                if (homepageScreen === HomepageScreen.HOMEPAGE) {
                    showBNVImmediate()
                    setAppBarVisibility(false)
                    showHideBottomNavigationView(false)
                    handleShowingAds()
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
                    setAppBarVisibility(true)
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
                    changeAppBarElevation(false)
                }
            }

            DrawerItem.BACKUPS -> {
                viewModel.setIsFirstNavigationLevel(false)
                showHideBottomNavigationView(hide = true)
                setAppBarVisibility(true)
                if (openFolderRefresh) {
                    onNodesBackupsUpdate()
                    openFolderRefresh = false
                }
                supportInvalidateOptionsMenu()
                hideFabButton()
                hideAdsView()
                replaceFragmentWithBackStack(
                    fragmentToReplace = backupsFragment,
                    newFragmentInstance = createBackupsFragment(backupsHandle, errorMessage),
                    fragmentTag = FragmentTag.BACKUPS.tag,
                )
            }

            DrawerItem.SHARED_ITEMS -> {
                showHideBottomNavigationView(false)
                setAppBarVisibility(false)
                setBottomNavigationMenuItemChecked(SHARED_ITEMS_BNV)
                supportInvalidateOptionsMenu()
                showFabButton()
                updateSharesTab()
                val fragment = supportFragmentManager.findFragmentByTag(SharesFragment.TAG)
                    ?: SharesFragment.newInstance()
                replaceFragment(
                    fragment = fragment,
                    fragmentTag = SharesFragment.TAG
                )
                if (!comesFromNotifications) {
                    bottomNavigationCurrentItem = SHARED_ITEMS_BNV
                }
            }

            DrawerItem.NOTIFICATIONS -> {
                showHideBottomNavigationView(true)
                selectDrawerItemNotifications()
                supportInvalidateOptionsMenu()
                showFabButton()
                hideAdsView()
            }

            DrawerItem.CHAT -> {
                selectDrawerItemChat(chatId)
                supportInvalidateOptionsMenu()
                showHideBottomNavigationView(false)
                if (!comesFromNotifications) {
                    bottomNavigationCurrentItem = CHAT_BNV
                }
                setBottomNavigationMenuItemChecked(CHAT_BNV)
                hideFabButton()
                changeAppBarElevation(false)
                Analytics.tracker.trackEvent(ChatRoomsBottomNavigationItemEvent)
            }

            DrawerItem.ASK_PERMISSIONS -> restrictUiWhileRequestingPermissions()

            else -> {}
        }
        setTabsVisibility()
        checkScrollElevation()
        viewModel.checkToShow2FADialog(newAccount, firstLogin)
    }

    /**
     * Checks if the User should navigate to Cloud Drive or Media Discovery
     */
    private suspend fun handleCloudDriveNavigation() {
        if (fileBrowserViewModel.isMediaDiscoveryOpen()) {
            Timber.d("Show Media Discovery Screen")
            lifecycle.withStarted {
                showMediaDiscovery(
                    mediaHandle = fileBrowserViewModel.getSafeBrowserParentHandle(),
                    isAccessedByIconClick = false,
                    replaceFragment = fileBrowserViewModel.state().hasNoOpenedFolders && fileBrowserViewModel.isFromSyncTab()
                        .not(),
                    errorMessage = fileBrowserViewModel.state.value.errorMessage,
                )
            }
        } else {
            Timber.d("Show Cloud Drive Screen")
            selectDrawerItemCloudDrive()
        }
    }

    private suspend fun navigateToSearchActivity() {
        searchExpand = false
        val parentHandle = viewModel.getParentHandleForSearch(
            browserParentHandle = fileBrowserViewModel.state.value.fileBrowserHandle,
            rubbishBinParentHandle = rubbishBinViewModel.state.value.currentHandle,
            backupsParentHandle = backupsFragment?.getCurrentBackupsFolderHandle() ?: -1L,
            incomingParentHandle = getHandleFromIncomingSharesViewModel(),
            outgoingParentHandle = getHandleFromOutgoingSharesViewModel(),
            linksParentHandle = getHandleFromLinksViewModel(),
            favouritesParentHandle = getParentHandleForFavourites() ?: -1L,
            nodeSourceType = nodeSourceType,
        )

        val searchActivityIntent = SearchActivity.getIntent(
            context = this,
            isFirstNavigationLevel = isFirstNavigationLevel,
            nodeSourceType = nodeSourceType,
            parentHandle = parentHandle
        )

        searchResultLauncher.launch(searchActivityIntent)
    }

    private fun navigateToSettingsActivity(targetPreference: TargetPreference?) {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        megaNavigator.openSettings(this, targetPreference)
    }

    private fun openFullscreenOfflineFragment(path: String?, fileNames: ArrayList<String>? = null) {
        drawerItem = DrawerItem.HOMEPAGE
        path?.let {
            navController?.navigate(
                HomepageFragmentDirections.actionHomepageToFullscreenOfflineCompose(
                    path = it,
                    rootFolderOnly = false,
                    fileNames = fileNames?.toTypedArray()
                ),
                NavOptions.Builder().setLaunchSingleTop(true).build()
            )
        }
    }

    fun fullscreenOfflineFragmentComposeOpened(fragment: OfflineComposeFragment?) {
        fullscreenOfflineComposeFragment = fragment
        showFabButton()
        setBottomNavigationMenuItemChecked(HOME_BNV)
        setAppBarVisibility(true)
        setToolbarTitle()
        supportInvalidateOptionsMenu()
    }

    fun fullscreenOfflineFragmentComposeClosed(fragment: OfflineComposeFragment) {
        if (fragment == fullscreenOfflineComposeFragment) {
            fullscreenOfflineComposeFragment = null
            if (bottomItemBeforeOpenFullscreenOffline != Constants.INVALID_VALUE && !mStopped) {
                goBackToBottomNavigationItem(bottomItemBeforeOpenFullscreenOffline)
                bottomItemBeforeOpenFullscreenOffline = Constants.INVALID_VALUE
            }
            pathNavigationOffline = "/"
            // workaround for flicker of AppBarLayout: if we go back to homepage from fullscreen
            // offline, and hide AppBarLayout when immediately on go back, we will see the flicker
            // of AppBarLayout, hide AppBarLayout when fullscreen offline is closed is better.
            if (isInMainHomePage) {
                setAppBarVisibility(false)
            }
        }
    }

    fun pagerOfflineComposeFragmentOpened(fragment: OfflineComposeFragment?) {
        pagerOfflineComposeFragment = fragment
    }

    fun pagerOfflineComposeFragmentClosed(fragment: OfflineComposeFragment) {
        if (fragment == pagerOfflineComposeFragment) {
            pagerOfflineComposeFragment = null
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
        val height =
            if (adsContainerViewModel.isAdsLoaded()) resources.getDimensionPixelSize(R.dimen.ads_web_view_and_bottom_navigation_view_height)
            else resources.getDimensionPixelSize(R.dimen.bottom_navigation_view_height)
        params.setMargins(0, 0, 0, height)
        fragmentLayout.layoutParams = params
        syncNavigationBarColorWithBottomNavMenu()
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
            val extendsHeight =
                (psaViewHolder?.psaLayoutHeight().takeIf { psaViewHolder?.visible() == true }
                    ?: 0) + (miniAudioPlayerController?.playerHeight()
                    .takeIf { miniAudioPlayerController?.visible() == true }
                    ?: 0)
            fragment.updateFabPosition(extendsHeight)
        }
    }

    private val isCloudAdded: Boolean
        get() {
            cloudDriveSyncsFragment =
                supportFragmentManager.findFragmentByTag(FragmentTag.CLOUD_DRIVE_SYNCS.tag) as? CloudDriveSyncsFragment
            return (cloudDriveSyncsFragment != null && cloudDriveSyncsFragment?.isAdded == true)
        }

    private val deviceCenterFragment: DeviceCenterFragment?
        get() = supportFragmentManager.findFragmentByTag(FragmentTag.DEVICE_CENTER.tag) as? DeviceCenterFragment

    private val notificationsFragment: NotificationsFragment?
        get() = supportFragmentManager.findFragmentByTag(FragmentTag.NOTIFICATIONS.tag) as? NotificationsFragment

    private val backupsFragment: BackupsFragment?
        get() = supportFragmentManager.findFragmentByTag(FragmentTag.BACKUPS.tag) as? BackupsFragment

    private val favouriteFolderFragment: FavouriteFolderFragment?
        get() = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            ?.childFragmentManager?.fragments?.firstOrNull {
                it is FavouriteFolderFragment
            } as? FavouriteFolderFragment

    private val mediaDiscoveryFragment: MediaDiscoveryFragment?
        get() = supportFragmentManager.findFragmentByTag(FragmentTag.MEDIA_DISCOVERY.tag) as? MediaDiscoveryFragment

    private fun createBackupsFragment(backupsHandle: Long, @StringRes errorMessage: Int?) =
        BackupsFragment.newInstance(
            // Default to the User's Root Backups Folder handle if no Backups Handle is passed
            backupsHandle = if (backupsHandle == -1L) {
                viewModel.state().userRootBackupsFolderHandle.longValue
            } else backupsHandle,
            errorMessage = errorMessage,
        )

    private val isOnFileManagementManagerSection: Boolean
        get() = drawerItem !== DrawerItem.NOTIFICATIONS
                && drawerItem !== DrawerItem.CHAT
                && drawerItem !== DrawerItem.RUBBISH_BIN
                && drawerItem !== DrawerItem.PHOTOS
                && !isInImagesPage
                && !isInMediaDiscovery()

    /**
     * Updates the transfers widget.
     */
    private fun updateTransfersWidgetVisibility() {
        if (isOnFileManagementManagerSection) {
            transfersManagementViewModel.showTransfersWidget()
        } else {
            transfersManagementViewModel.hideTransfersWidget()
        }
    }

    fun checkScrollElevation() {
        if (drawerItem == null) {
            return
        }
        when (drawerItem) {
            DrawerItem.BACKUPS -> {
                backupsFragment?.checkScroll()
            }

            DrawerItem.CHAT -> {
                chatTabsFragment = chatsFragment
            }

            else -> {}
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
        navigateToSettingsActivity(storageTargetPreference)
    }

    /**
     * Opens the settings section and scrolls to start screen setting.
     */
    fun moveToSettingsSectionStartScreen() {
        navigateToSettingsActivity(startScreenTargetPreference)
    }

    override fun moveToChatSection(chatId: Long) {
        if (chatId != -1L) {
            megaNavigator.openChat(
                context = this,
                chatId = chatId,
                action = Constants.ACTION_CHAT_SHOW_MESSAGES
            )
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Timber.d("onCreateOptionsMenu")
        // Force update the toolbar title to make the the tile length to be updated
        setToolbarTitle()
        // Inflate the menu items for use in the action bar
        menuInflater.inflate(R.menu.activity_manager, menu)
        searchMenuItem = menu.findItem(R.id.action_search)
        searchView = searchMenuItem?.actionView as? SearchView
        searchView?.queryHint = getString(R.string.hint_action_search)
        val v = searchView?.findViewById<View>(androidx.appcompat.R.id.search_plate)
        v?.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
        searchView?.setIconifiedByDefault(true)
        searchMenuItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                Timber.d("onMenuItemActionExpand")
                searchExpand = true
                if (drawerItem === DrawerItem.HOMEPAGE) {
                    if (homepageScreen === HomepageScreen.FULLSCREEN_OFFLINE) {
                        setFullscreenOfflineFragmentSearchQuery(viewModel.state.value.searchQuery)
                    } else if (mHomepageSearchable != null) {
                        mHomepageSearchable?.searchReady()
                    } else {
                        openSearchOnHomepage()
                    }
                } else if (drawerItem !== DrawerItem.CHAT) {
                    openSearchOnHomepage()
                } else {
                    Util.resetActionBar(supportActionBar)
                }
                CallUtil.hideCallMenuItem(chronometerMenuItem, returnCallMenuItem)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                Timber.d("onMenuItemActionCollapse()")
                searchExpand = false
                CallUtil.setCallMenuItem(
                    returnCallMenuItem,
                    layoutCallMenuItem,
                    chronometerMenuItem
                )
                if (drawerItem === DrawerItem.HOMEPAGE) {
                    if (homepageScreen === HomepageScreen.FULLSCREEN_OFFLINE) {
                        setFullscreenOfflineFragmentSearchQuery(null)
                        supportInvalidateOptionsMenu()
                    } else if (mHomepageSearchable != null) {
                        mHomepageSearchable?.exitSearch()
                        supportInvalidateOptionsMenu()
                    }
                }
                return true
            }
        })
        searchView?.maxWidth = Int.MAX_VALUE
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if (drawerItem === DrawerItem.CHAT) {
                    Util.hideKeyboard(this@ManagerActivity, 0)
                } else if (drawerItem == DrawerItem.HOMEPAGE) {
                    if (homepageScreen == HomepageScreen.FULLSCREEN_OFFLINE || homepageScreen == HomepageScreen.DOCUMENTS || homepageScreen == HomepageScreen.AUDIO) {
                        Util.hideKeyboard(this@ManagerActivity)
                    }
                } else {
                    searchExpand = false
                    viewModel.updateSearchQuery(query)
                    setToolbarTitle()
                    Timber.d("Search query: %s", query)
                    supportInvalidateOptionsMenu()
                }
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                Timber.d("onQueryTextChange")
                if (drawerItem === DrawerItem.HOMEPAGE) {
                    viewModel.updateSearchQuery(newText)
                    if (homepageScreen === HomepageScreen.FULLSCREEN_OFFLINE) {
                        setFullscreenOfflineFragmentSearchQuery(viewModel.state.value.searchQuery)
                    } else {
                        mHomepageSearchable?.searchQuery(newText)
                    }
                }
                return true
            }
        })
        val enableSelectMenuItem = menu.findItem(R.id.action_enable_select)
        doNotDisturbMenuItem = menu.findItem(R.id.action_menu_do_not_disturb)
        clearRubbishBinMenuItem = menu.findItem(R.id.action_menu_clear_rubbish_bin)
        returnCallMenuItem = menu.findItem(R.id.action_return_call)
        val rootView = returnCallMenuItem?.actionView as? RelativeLayout
        layoutCallMenuItem = rootView?.findViewById(R.id.layout_menu_call)
        chronometerMenuItem = rootView?.findViewById(R.id.chrono_menu)
        chronometerMenuItem?.visibility = View.GONE
        moreMenuItem = menu.findItem(R.id.action_more)
        openLinkMenuItem = menu.findItem(R.id.action_open_link)
        returnCallMenuItem?.let { menuItem ->
            rootView?.setOnClickListener {
                onOptionsItemSelected(
                    menuItem
                )
            }
        }
        if (drawerItem == null) {
            drawerItem = startScreenUtil.getStartDrawerItem()
        }
        if (drawerItem === DrawerItem.CLOUD_DRIVE) {
            setBottomNavigationMenuItemChecked(CLOUD_DRIVE_BNV)
        }
        CallUtil.setCallMenuItem(returnCallMenuItem, layoutCallMenuItem, chronometerMenuItem)
        if (viewModel.isConnected) {
            when (drawerItem) {
                DrawerItem.CLOUD_DRIVE -> {
                    if (fileBrowserViewModel.state().selectedTab != CloudDriveTab.SYNC) {
                        openLinkMenuItem?.isVisible = isFirstNavigationLevel
                        moreMenuItem?.isVisible = !isFirstNavigationLevel
                        if (!fileBrowserViewModel.isMediaDiscoveryOpen() && isCloudAdded && fileBrowserViewModel.state().nodesList.isNotEmpty()
                        ) {
                            searchMenuItem?.isVisible = true
                        }
                    } else {
                        moreMenuItem?.isVisible = true
                    }
                }

                DrawerItem.HOMEPAGE -> if (homepageScreen === HomepageScreen.FULLSCREEN_OFFLINE) {
                    updateFullscreenOfflineFragmentOptionMenu(true)
                }

                DrawerItem.RUBBISH_BIN -> {
                    moreMenuItem?.isVisible = !isFirstNavigationLevel
                    if (rubbishBinViewModel.state().nodeList.isNotEmpty()) {
                        clearRubbishBinMenuItem?.isVisible = isFirstNavigationLevel
                        searchMenuItem?.isVisible = true
                    }
                }

                DrawerItem.BACKUPS -> {
                    moreMenuItem?.isVisible = false
                    if ((backupsFragment?.getNodeCount() ?: 0) > 0) {
                        searchMenuItem?.isVisible = true
                    }
                }

                DrawerItem.SHARED_ITEMS -> {
                    moreMenuItem?.isVisible = false
                }

                DrawerItem.CHAT -> if (searchExpand) {
                    openSearchView()
                } else {
                    doNotDisturbMenuItem?.isVisible = true
                    openLinkMenuItem?.isVisible = true
                }

                else -> {}
            }
        }
        if (drawerItem === DrawerItem.HOMEPAGE) {
            if (listOf(
                    HomepageScreen.FAVOURITES,
                    HomepageScreen.DOCUMENTS,
                    HomepageScreen.AUDIO
                ).contains(homepageScreen)
            ) {
                searchMenuItem?.isVisible = true
            } else {
                // Get the Searchable again at onCreateOptionsMenu() after screen rotation
                mHomepageSearchable = findHomepageSearchable()
                if (searchExpand) {
                    openSearchView()
                } else {
                    if (mHomepageSearchable != null) {
                        searchMenuItem?.isVisible =
                            mHomepageSearchable?.shouldShowSearchMenu() == true
                    }
                }
            }
        }
        Timber.d("Call to super onCreateOptionsMenu")
        return super.onCreateOptionsMenu(menu)
    }

    fun openSearchOnHomepage() {
        viewModel.setIsFirstNavigationLevel(true)
        Util.resetActionBar(supportActionBar)
        nodeSourceType = nodeSourceTypeMapper(
            drawerItem = drawerItem,
            homepageScreen = homepageScreen,
            sharesTab = tabItemShares
        )
        lifecycleScope.launch {
            navigateToSearchActivity()
        }
    }

    private fun setFullscreenOfflineFragmentSearchQuery(searchQuery: String?) {
        fullscreenOfflineComposeFragment?.setSearchQuery(searchQuery)
    }

    private fun updateFullscreenOfflineFragmentOptionMenu(openSearchView: Boolean) {
        if (fullscreenOfflineComposeFragment == null) return
        if (searchExpand && openSearchView) {
            openSearchView()
        } else if (!searchExpand) {
            if (viewModel.isConnected) {
                if (fullscreenOfflineComposeFragment?.isInSearchMode() == false) {
                    searchMenuItem?.isVisible = true
                }
            } else {
                supportInvalidateOptionsMenu()
            }
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
    private fun <F : Fragment?> getFragmentByType(fragmentClass: Class<F>): F? {
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
        typesCameraPermission = Constants.INVALID_TYPE_PERMISSIONS
        megaApi.retryPendingConnections()
        megaChatApi.retryPendingConnections(false)
        return when (item.itemId) {
            android.R.id.home -> {
                if (isFirstNavigationLevel) {
                    when (drawerItem) {
                        DrawerItem.RUBBISH_BIN -> {
                            rubbishBinViewModel.resetScrollPosition()
                            goBackToBottomNavigationItem(bottomNavigationCurrentItem)
                        }

                        DrawerItem.DEVICE_CENTER -> handleDeviceCenterBackNavigation()
                        DrawerItem.NOTIFICATIONS -> {
                            handleSuperBackPressed()
                            goBackToBottomNavigationItem(bottomNavigationCurrentItem)
                        }

                        else -> drawerLayout.openDrawer(navigationView)
                    }
                } else {
                    if (drawerItem == DrawerItem.CLOUD_DRIVE) {
                        handleCloudDriveBackNavigation(performBackNavigation = true)
                    } else if (drawerItem == DrawerItem.DEVICE_CENTER) {
                        onBackPressedDispatcher.onBackPressed()
                    } else if (drawerItem == DrawerItem.RUBBISH_BIN) {
                        rubbishBinComposeFragment = getRubbishBinComposeFragment()
                        rubbishBinComposeFragment?.onBackPressed()
                    } else if (drawerItem == DrawerItem.PHOTOS) {
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
                    } else if (drawerItem == DrawerItem.BACKUPS) {
                        backupsFragment?.let {
                            it.onBackPressed()
                            return true
                        }
                    } else if (drawerItem == DrawerItem.HOMEPAGE) {
                        if (homepageScreen == HomepageScreen.FULLSCREEN_OFFLINE) {
                            handleBackPressIfFullscreenOfflineFragmentOpened()
                        } else if (navController?.currentDestination != null &&
                            (navController?.currentDestination?.id == R.id.favouritesFolderFragment ||
                                    navController?.currentDestination?.id == R.id.videoSectionFragment)
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
                Analytics.tracker.trackEvent(OpenLinkMenuItemEvent)
                showOpenLinkDialog()
                true
            }

            R.id.action_menu_do_not_disturb -> {
                if (drawerItem == DrawerItem.CHAT) {
                    Analytics.tracker.trackEvent(ChatRoomDNDMenuItemEvent)
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
                Analytics.tracker.trackEvent(ArchivedChatsMenuItemEvent)
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

                    DrawerItem.BACKUPS -> backupsFragment?.selectAll()

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

            R.id.action_more -> {
                if (fileBrowserViewModel.state().selectedTab != CloudDriveTab.SYNC) {
                    showNodeOptionsPanel(
                        getCurrentParentNode(
                            currentParentHandle,
                            Constants.INVALID_VALUE
                        ),
                        hideHiddenActions = drawerItem == DrawerItem.SHARED_ITEMS
                    )
                } else {
                    Timber.d("Sync settings selected in sync tab")
                    fileBrowserViewModel.showSyncSettings()
                }
                true
            }

            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun onSelectAllSharedItems() {
        lifecycleScope.launch {
            if (!isSharesTabComposeEnabled()) {
                when (tabItemShares) {
                    SharesTab.INCOMING_TAB -> {
                        incomingSharesViewModel.selectAllNodes()
                    }

                    SharesTab.OUTGOING_TAB -> {
                        outgoingSharesViewModel.selectAllNodes()
                    }

                    SharesTab.LINKS_TAB -> {
                        linksViewModel.selectAllNodes()
                    }

                    else -> {}
                }
            }
        }
    }

    private fun hideItemsWhenSearchSelected() {
        if (searchMenuItem != null) {
            doNotDisturbMenuItem?.isVisible = false
            clearRubbishBinMenuItem?.isVisible = false
            searchMenuItem?.isVisible = false
            openLinkMenuItem?.isVisible = false
        }
    }

    /**
     * Method to return to an ongoing call
     */
    fun returnCall() {
        CallUtil.returnActiveCall(this)
    }

    private fun checkBeforeOpeningQR(openScanQR: Boolean) {
        if (CallUtil.isNecessaryDisableLocalCamera() != MEGACHAT_INVALID_HANDLE) {
            CallUtil.showConfirmationOpenCamera(this, Constants.ACTION_OPEN_QR, openScanQR)
            return
        }
        openQR(openScanQR)
    }

    fun openQR(openScanQr: Boolean) {
        val qrIntent = Intent(this, QRCodeComposeActivity::class.java)
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
        }
        refreshRubbishBin()
        setToolbarTitle()
    }

    private fun refreshRubbishBin() {
        rubbishBinViewModel.refreshNodes()
    }

    private fun goBack() {
        retryConnectionsAndSignalPresence()
        if (syncPromotionViewModel.state.value.shouldShowSyncPromotion) {
            Analytics.tracker.trackEvent(SyncPromotionBottomSheetDismissedEvent)
            syncPromotionViewModel.onConsumeShouldShowSyncPromotion()
            return
        }
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(GravityCompat.START)
            return
        }
        dismissAlertDialogIfExists(statusDialog)
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
            handleCloudDriveBackNavigation(performBackNavigation = true)
        } else if (drawerItem == DrawerItem.DEVICE_CENTER) {
            handleDeviceCenterBackNavigation()
        } else if (drawerItem == DrawerItem.RUBBISH_BIN) {
            rubbishBinComposeFragment = getRubbishBinComposeFragment()
            if (rubbishBinComposeFragment == null || rubbishBinComposeFragment?.onBackPressed() == 0) {
                goBackToBottomNavigationItem(bottomNavigationCurrentItem)
            }

        } else if (drawerItem == DrawerItem.BACKUPS) {
            backupsFragment?.onBackPressed() ?: goBackToBottomNavigationItem(
                bottomNavigationCurrentItem
            )
        } else if (drawerItem == DrawerItem.NOTIFICATIONS) {
            handleSuperBackPressed()
            goBackToBottomNavigationItem(bottomNavigationCurrentItem)
        } else if (drawerItem == DrawerItem.SHARED_ITEMS) {
            handleSuperBackPressed()
        } else if (drawerItem == DrawerItem.CHAT) {
            performOnBack()
        } else if (drawerItem == DrawerItem.PHOTOS) {
            if (isInAlbumContent) {
                fromAlbumContent = true
                isInAlbumContent = false
                goBackToBottomNavigationItem(bottomNavigationCurrentItem)
            } else if (isInFilterPage) {
                isInFilterPage = false
                goBackToBottomNavigationItem(bottomNavigationCurrentItem)
                if (photosFragment == null) {
                    goBackToBottomNavigationItem(bottomNavigationCurrentItem)
                }
            } else if (getPhotosFragment() == null || photosFragment?.onBackPressed() == 0) {
                performOnBack()
            }
        } else if (isInMainHomePage) {
            performOnBack()
        } else {
            handleBackPressIfFullscreenOfflineFragmentOpened()
        }
    }

    /**
     * Handles Back Navigation logic when the User is in Device Center
     */
    private fun handleDeviceCenterBackNavigation() {
        with(viewModel) {
            // Ensure that when exiting Device Center, only go back to the previous Bottom Navigation
            // item if Device Center was accessed through a Drawer Click
            state().deviceCenterPreviousBottomNavigationItem?.let {
                bottomNavigationCurrentItem = it
                // Reset back to null
                setDeviceCenterPreviousBottomNavigationItem(null)
            }
        }
        handleSuperBackPressed()
        goBackToBottomNavigationItem(bottomNavigationCurrentItem)
    }

    /**
     * Handles Back Navigation logic when the User is in Cloud Drive or Media Discovery
     *
     * @param performBackNavigation If true, a Back Navigation is performed to remove one level
     * from the Cloud Drive hierarchy
     */
    fun handleCloudDriveBackNavigation(performBackNavigation: Boolean) {
        with(fileBrowserViewModel) {
            when {
                // User is exploring a Sync Folder
                (isSyncFolderOpen() || isFromSyncTab()) && isMediaDiscoveryOpen().not() -> {
                    lifecycleScope.launch {
                        if (isAtAccessedFolder()) {
                            if (isFromSyncTab().not()) {
                                // Remove Cloud Drive and go back to Syncs
                                removeFragment(cloudDriveSyncsFragment)
                                megaNavigator.openSyncs(this@ManagerActivity)
                            }
                            goBackToRootLevel()
                            resetSyncFolderVisibility()
                        } else {
                            performBackNavigation()
                        }
                    }
                }

                // User is in Media Discovery
                isMediaDiscoveryOpen() -> {
                    // Use lifecycleScope.launch to synchronize separate operations. Update the Cloud Drive
                    // UI State first, then remove Media Discovery, and go back to the previous Fragment
                    lifecycleScope.launch {
                        exitMediaDiscovery(performBackNavigation = performBackNavigation)
                        ensureActive()
                        lifecycle.withStarted {
                            removeFragment(mediaDiscoveryFragment)
                            if (performBackNavigation) {
                                checkCloudDriveAccessFromNotification(isNotFromNotificationAction = {
                                    if (isAccessedFolderExited()) {
                                        resetIsAccessedFolderExited()
                                        // Go back to Device Center or Sync List
                                        when {
                                            isSyncFolderOpen() -> {
                                                megaNavigator.openSyncs(this@ManagerActivity)
                                            }

                                            isFromSyncTab().not() -> {
                                                selectDrawerItem(DrawerItem.DEVICE_CENTER)
                                            }

                                            else -> {
                                                fileBrowserViewModel.onTabChanged(CloudDriveTab.SYNC)
                                            }
                                        }
                                        lifecycleScope.launch {
                                            goBackToRootLevel()
                                            resetSyncFolderVisibility()
                                        }
                                    } else {
                                        goBackToBottomNavigationItem(bottomNavigationCurrentItem)
                                    }
                                })
                            } else {
                                goBackToBottomNavigationItem(bottomNavigationCurrentItem)
                            }
                        }
                    }
                }

                // User is in Cloud Drive
                else -> {
                    checkCloudDriveAccessFromNotification(isNotFromNotificationAction = {
                        // Use lifecycleScope.launch to synchronize separate Back operations
                        lifecycleScope.launch {
                            performBackNavigation()
                            ensureActive()
                            lifecycle.withStarted {
                                if (isAccessedFolderExited()) {
                                    resetIsAccessedFolderExited()
                                    //user is navigating to cloud drive from device fragment
                                    // Remove Cloud Drive and go back to Device Center
                                    removeFragment(cloudDriveSyncsFragment)
                                    // user navigates from sync tab so need to remove cloud drive fragment
                                    selectDrawerItem(DrawerItem.DEVICE_CENTER)
                                }
                            }
                        }
                    })
                }
            }
        }
    }

    /**
     * Checks if Cloud Drive was accessed from a Notification and executes specific logic
     *
     * @param isNotFromNotificationAction Lambda that is executed when Cloud Drive was not accessed
     * from a Notification
     */
    private fun checkCloudDriveAccessFromNotification(isNotFromNotificationAction: () -> Unit) {
        if (comesFromNotifications && comesFromNotificationHandle ==
            fileBrowserViewModel.getSafeBrowserParentHandle()
        ) {
            restoreFileBrowserAfterComingFromNotification()
        } else {
            isNotFromNotificationAction.invoke()
        }
    }

    /**
     * Closes the app if the current DrawerItem is the same as the preferred one.
     * If not, sets the current DrawerItem as the preferred one.
     */
    private fun performOnBack() {
        val startItem: Int = startScreenUtil.getStartBottomNavigationItem()
        if (drawerItem?.let { startScreenUtil.shouldCloseApp(startItem, it) } == true) {
            handleSuperBackPressed()
        } else {
            goBackToBottomNavigationItem(startItem)
        }
    }

    private fun handleBackPressIfFullscreenOfflineFragmentOpened() {
        if (fullscreenOfflineComposeFragment == null || fullscreenOfflineComposeFragment?.onBackPressed() == 0) {
            handleOfflineBackClick()
        }
    }

    private fun handleOfflineBackClick() {
        // workaround for flicker of AppBarLayout: if we go back to homepage from fullscreen
        // offline, and hide AppBarLayout when immediately on go back, we will see the flicker
        // of AppBarLayout, hide AppBarLayout when fullscreen offline is closed is better.
        if (bottomNavigationCurrentItem != HOME_BNV) {
            goBackToBottomNavigationItem(bottomNavigationCurrentItem)
        } else {
            drawerItem = DrawerItem.HOMEPAGE
        }
        handleSuperBackPressed()
    }

    fun adjustTransferWidgetPositionInHomepage() {
        if (isInMainHomePage) {
            val transfersWidgetLayout: View =
                findViewById(R.id.transfers_widget)
                    ?: return
            val params = transfersWidgetLayout.layoutParams as LinearLayout.LayoutParams
            params.bottomMargin = Util.dp2px(TRANSFER_WIDGET_MARGIN_BOTTOM.toFloat(), outMetrics)
            params.gravity = Gravity.END
            transfersWidgetLayout.layoutParams = params
        }
    }

    /**
     * Update the PSA view visibility.
     */
    private fun updatePsaViewVisibility() {
        if (psaViewHolder?.visible() == true) {
            handler.post { updateHomepageFabPosition() }
        } else {
            updateHomepageFabPosition()
        }
    }

    /**
     * Exits the Backups Page
     *
     * When the Device Center Feature Flag is enabled, exiting Backups will redirect the User back
     * to Device Center
     *
     * Otherwise, the User goes back to the previous Bottom Navigation Fragment
     */
    fun exitBackupsPage() {
        handleSuperBackPressed()
        selectDrawerItem(DrawerItem.DEVICE_CENTER)
    }

    /**
     * Goes back to the specified Bottom Navigation item
     *
     * @param item The Bottom Navigation item
     */
    private fun goBackToBottomNavigationItem(item: Int) {
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
                // User is in Cloud Drive. Go back to the Root Node level
                if (drawerItem == DrawerItem.CLOUD_DRIVE) {
                    Timber.d("User is in Cloud Drive. Go back to the Root Level")
                    lifecycleScope.launch {
                        if (fileBrowserViewModel.isMediaDiscoveryOpen()) {
                            Timber.d("Remove the Media Discovery Fragment")
                            removeFragment(mediaDiscoveryFragment)
                        }
                        fileBrowserViewModel.goBackToRootLevel()
                    }
                } else {
                    // User is not in Cloud Drive. Navigate to the feature
                    Timber.d("User is not in Cloud Drive. Navigate to Cloud Drive")
                    drawerItem = DrawerItem.CLOUD_DRIVE
                    setBottomNavigationMenuItemChecked(CLOUD_DRIVE_BNV)
                }
                handleShowingAds()
            }

            R.id.bottom_navigation_item_homepage -> {
                drawerItem = DrawerItem.HOMEPAGE
                if (fullscreenOfflineComposeFragment != null) {
                    handleSuperBackPressed()
                    return true
                } else {
                    setBottomNavigationMenuItemChecked(HOME_BNV)
                }
                handleShowingAds()
            }

            R.id.bottom_navigation_item_camera_uploads -> {

                // if pre fragment is the same one, do nothing.
                if (oldDrawerItem != DrawerItem.PHOTOS) {
                    drawerItem = DrawerItem.PHOTOS
                    setBottomNavigationMenuItemChecked(PHOTOS_BNV)
                }
                handleShowingAds()
            }

            R.id.bottom_navigation_item_shared_items -> {
                Analytics.tracker.trackEvent(SharedItemsScreenEvent)
                if (drawerItem == DrawerItem.SHARED_ITEMS) {
                    if (tabItemShares == SharesTab.INCOMING_TAB && getHandleFromIncomingSharesViewModel() != INVALID_HANDLE) {
                        incomingSharesViewModel.goBackToRootLevel()
                    } else if (tabItemShares == SharesTab.OUTGOING_TAB && getHandleFromOutgoingSharesViewModel() != INVALID_HANDLE) {
                        outgoingSharesViewModel.goBackToRootLevel()
                    } else if (tabItemShares == SharesTab.LINKS_TAB && getHandleFromLinksViewModel() != INVALID_HANDLE) {
                        linksViewModel.resetToRoot()
                    }
                } else {
                    drawerItem = DrawerItem.SHARED_ITEMS
                    setBottomNavigationMenuItemChecked(SHARED_ITEMS_BNV)
                }
                handleShowingAds()
            }

            R.id.bottom_navigation_item_chat -> {
                drawerItem = DrawerItem.CHAT
                setBottomNavigationMenuItemChecked(CHAT_BNV)
                handleShowingAds()
            }
        }
        checkIfShouldCloseSearchView(oldDrawerItem)
        selectDrawerItem(drawerItem)
        closeDrawer()
        return true
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        showSnackbar(type, fragmentContainer, content, chatId)
        snackbar?.apply {
            val snackbarLayout = this.view as? Snackbar.SnackbarLayout
            // Add system navigation bar bottom insets padding
            systemBarInsets?.let { insets ->
                snackbarLayout?.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                    bottomMargin = insets.bottom
                }
            }
        }
    }

    override fun showSnackbar(type: Int, content: String, action: () -> Unit) {
        showSnackbar(type = type, view = fragmentContainer, s = content, action = action)
        snackbar?.apply {
            val snackbarLayout = this.view as? Snackbar.SnackbarLayout
            // Add system navigation bar bottom insets padding
            systemBarInsets?.let { insets ->
                snackbarLayout?.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                    bottomMargin = insets.bottom
                }
            }
        }
    }

    /**
     * Restores a list of nodes from Rubbish Bin to their original parent.
     *
     * @param nodes List of nodes.
     */
    fun restoreFromRubbish(nodes: List<TypedNode>) {
        viewModel.checkRestoreNodesNameCollision(nodes)
    }

    /**
     * Shows the final result of a restoration or removal from Rubbish Bin section.
     *
     * @param message      Text message to show as the request result.
     */
    private fun showRestorationOrRemovalResult(message: String) {
        showSnackbar(SNACKBAR_TYPE, message, MEGACHAT_INVALID_HANDLE)
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
        if (nodes.isNullOrEmpty()) {
            showSnackbar(
                SNACKBAR_TYPE,
                getString(R.string.general_text_error),
                MEGACHAT_INVALID_HANDLE
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
                SNACKBAR_TYPE,
                getString(R.string.warning_node_not_exists_in_cloud),
                MEGACHAT_INVALID_HANDLE
            )
            return
        }
        if (showTakenDownNodeActionNotAvailableDialog(node, this)) {
            return
        }
        LinksUtil.showGetLinkActivity(this, handle)
        refreshAfterMovingToRubbish()
    }

    fun showShareBackupsFolderWarningDialog(node: MegaNode, nodeType: Int) {
        fileBackupManager.shareBackupsFolder(
            nodeController = nodeController,
            megaNode = node,
            nodeType = nodeType,
            actionBackupNodeCallback = fileBackupManager.actionBackupNodeCallback,
        )
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
                    /** If the current folder node was moved to rubbish bin or another directory while
                     *  in media discovery mode, then exit
                     */
                    if (isInMediaDiscovery()) {
                        fileBrowserViewModel.setMediaDiscoveryVisibility(
                            isMediaDiscoveryOpen = false,
                            isMediaDiscoveryOpenedByIconClick = false
                        )
                        removeFragment(mediaDiscoveryFragment)
                    }
                    fileBrowserViewModel.setFileBrowserHandle(oldParentHandle)
                }

                DrawerItem.BACKUPS -> {
                    backupsFragment?.let {
                        it.updateBackupsHandle(oldParentHandle)
                        it.invalidateRecyclerView()
                    }
                }

                DrawerItem.SHARED_ITEMS -> {
                    when (tabItemShares) {
                        SharesTab.INCOMING_TAB -> {
                            incomingSharesViewModel.setCurrentHandle(
                                if (incomingSharesViewModel.incomingTreeDepth() == 0) INVALID_HANDLE else oldParentHandle
                            )
                            refreshIncomingShares()
                        }

                        SharesTab.OUTGOING_TAB -> {
                            outgoingSharesViewModel.setCurrentHandle(
                                if (outgoingSharesViewModel.outgoingTreeDepth() == 0) INVALID_HANDLE else oldParentHandle
                            )
                            refreshOutgoingShares()
                        }

                        else -> {}
                    }
                }

                else -> {}
            }
            setToolbarTitle()
        }
    }

    /**
     * Shows an Open link dialog.
     */
    private fun showOpenLinkDialog(isJoinMeeting: Boolean = false) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            val isChatScreen = drawerItem == DrawerItem.CHAT

            OpenLinkDialogFragment.newInstance(
                isChatScreen = isChatScreen,
                isJoinMeeting = isJoinMeeting
            ).show(supportFragmentManager, OpenLinkDialogFragment.TAG)
        }
    }

    fun showChatLink(link: String?, chatId: Long) {
        Timber.d("Link: %s", link)
        val action = if (joiningToChatLink) {
            resetJoiningChatLink()
            Constants.ACTION_JOIN_OPEN_CHAT_LINK
        } else {
            Constants.ACTION_OPEN_CHAT_LINK
        }
        megaNavigator.openChat(
            context = this,
            action = action,
            link = link,
            chatId = chatId,
        )
        if (drawerItem != DrawerItem.CHAT) {
            drawerItem = DrawerItem.CHAT
            selectDrawerItem(drawerItem)
        }
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
        viewModel.prepareDocumentScanner()
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
            fileBrowserViewModel.setFileBrowserHandle(parentHandleBrowser)
        }

    // For home page, its parent is always the root of cloud drive.
    override val currentParentHandle: Long
        get() {
            var parentHandle: Long = -1
            when (drawerItem) {
                DrawerItem.HOMEPAGE ->
                    parentHandle = if (homepageScreen == HomepageScreen.FAVOURITES) {
                        getParentHandleForFavourites() ?: megaApi.rootNode?.handle ?: INVALID_HANDLE
                    } else {
                        // For home page, its parent is always the root of cloud drive.
                        megaApi.rootNode?.handle ?: INVALID_HANDLE
                    }

                DrawerItem.CLOUD_DRIVE -> parentHandle =
                    fileBrowserViewModel.getSafeBrowserParentHandle()

                DrawerItem.BACKUPS -> parentHandle =
                    backupsFragment?.getCurrentBackupsFolderHandle() ?: -1L

                DrawerItem.RUBBISH_BIN -> parentHandle =
                    rubbishBinViewModel.state().currentHandle

                DrawerItem.SHARED_ITEMS -> {
                    when {
                        tabItemShares === SharesTab.INCOMING_TAB -> {
                            parentHandle = getHandleFromIncomingSharesViewModel()
                        }

                        tabItemShares === SharesTab.OUTGOING_TAB -> {
                            parentHandle = getHandleFromOutgoingSharesViewModel()
                        }

                        tabItemShares === SharesTab.LINKS_TAB -> {
                            parentHandle = getHandleFromLinksViewModel()
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
            showSnackbar(SNACKBAR_TYPE, errorString, -1)
            Timber.d("%s: parentHandle == -1", errorString)
            return null
        }
        val parentNode = megaApi.getNodeByHandle(parentHandle)
        if (parentNode == null && errorString != null) {
            showSnackbar(SNACKBAR_TYPE, errorString, -1)
            Timber.d("%s: parentNode == null", errorString)
            return null
        }
        return parentNode
    }

    override fun createFolder(folderName: String) {
        Timber.d("createFolder")
        if (!viewModel.isConnected) {
            showSnackbar(
                SNACKBAR_TYPE,
                getString(R.string.error_server_connection_problem),
                -1
            )
            return
        }
        lifecycleScope.launch {
            statusDialog = createProgressDialog(
                this@ManagerActivity,
                getString(R.string.context_creating_folder)
            )
            runCatching {
                viewModel.createFolder(currentParentHandle, folderName)
            }.onSuccess { node ->
                showSnackbar(
                    SNACKBAR_TYPE,
                    getString(R.string.context_folder_created),
                    -1
                )
                if (drawerItem === DrawerItem.CLOUD_DRIVE) {
                    if (isCloudAdded) {
                        fileBrowserViewModel.setFileBrowserHandle(node.longValue)
                    }
                } else if (drawerItem === DrawerItem.SHARED_ITEMS) {
                    when (tabItemShares) {
                        SharesTab.INCOMING_TAB -> {
                            incomingSharesViewModel.setCurrentHandle(node.longValue)
                        }

                        SharesTab.OUTGOING_TAB -> {
                            outgoingSharesViewModel.setCurrentHandle(node.longValue)
                        }

                        SharesTab.LINKS_TAB -> {
                            linksViewModel.openFolderByHandleWithRetry(node.longValue)
                        }

                        else -> {}
                    }
                }
            }.onFailure {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_folder_no_created), -1)
            }
            dismissAlertDialogIfExists(statusDialog)
        }
    }

    override fun onJoinMeeting() {
        Analytics.tracker.trackEvent(JoinMeetingPressedEvent)
        if (CallUtil.participatingInACall()) {
            CallUtil.showConfirmationInACall(
                this,
                getString(R.string.text_join_call),
            )
        } else {
            showOpenLinkDialog(isJoinMeeting = true)
        }
    }

    override fun onCreateMeeting() {
        Analytics.tracker.trackEvent(StartMeetingNowPressedEvent)
        if (chatsFragment == null) {
            selectDrawerItem(DrawerItem.CHAT)
        }
        chatsFragment?.onCreateMeeting()
    }

    /**
     * Save nodes to device.
     *
     * @param nodes           nodes to save
     * @param highPriority    whether this download is high priority or not
     * @param isFolderLink    whether this download is a folder link
     * @param fromChat        whether this download is from chat
     * @param withStartMessage  Whether show start message or not.
     *                          It should be true only if the widget is not visible.
     */
    fun saveNodesToDevice(
        nodes: List<MegaNode?>?, highPriority: Boolean, isFolderLink: Boolean,
        fromChat: Boolean,
        withStartMessage: Boolean,
    ) {
        if (nodes == null) return
        when {
            isFolderLink || fromChat -> {
                startDownloadViewModel.onMultipleSerializedNodesDownloadClicked(
                    serializedData = nodes.mapNotNull { it?.serialize() },
                    isHighPriority = highPriority,
                    withStartMessage = withStartMessage,
                )
            }

            else -> {
                startDownloadViewModel.onDownloadClicked(
                    nodeIds = nodes.mapNotNull { megaNode -> megaNode?.handle?.let { NodeId(it) } },
                    isHighPriority = highPriority,
                    withStartMessage = withStartMessage,
                )
            }
        }
    }

    /**
     * Upon a node is tapped, if it cannot be previewed in-app,
     * then download it first, this download will be marked as "download by tap".
     *
     * @param node Node to be downloaded.
     */
    fun saveNodeByTap(node: MegaNode) {
        startDownloadViewModel.onDownloadForPreviewClicked(NodeId(node.handle), false)
    }

    /**
     * Upon a node is open with, if it cannot be previewed in-app,
     * then download it first, this download will be marked as "download by open with".
     *
     * @param node Node to be downloaded.
     */
    fun saveNodeByOpenWith(node: MegaNode) {
        startDownloadViewModel.onDownloadForPreviewClicked(NodeId(node.handle), true)
    }

    /**
     * Save nodes to device.
     *
     * @param handles         handles of nodes to save
     * @param highPriority    whether this download is high priority or not
     * @param withStartMessage  Whether show start message or not.
     *                          It should be true only if the widget is not visible.
     */
    fun saveHandlesToDevice(
        handles: List<Long?>?,
        highPriority: Boolean,
        withStartMessage: Boolean,
    ) {
        if (handles == null) return
        startDownloadViewModel.onDownloadClicked(
            nodeIds = handles.mapNotNull { it?.let { NodeId(it) } },
            isHighPriority = highPriority,
            withStartMessage = withStartMessage,
        )
    }

    /**
     * Attach node to chats, only used by NodeOptionsBottomSheetDialogFragment.
     *
     * @param node node to attach
     */
    fun attachNodeToChats(node: MegaNode?) {
        node?.let { nodeAttachmentViewModel.startAttachNodes(listOf(NodeId(it.handle))) }
    }

    /**
     * Attach nodes to chats, used by ActionMode of manager fragments.
     *
     * @param nodes nodes to attach
     */
    fun attachNodesToChats(nodes: List<MegaNode?>?) {
        nodes.orEmpty().filterNotNull().map { NodeId(it.handle) }.let {
            nodeAttachmentViewModel.startAttachNodes(it)
        }
    }

    private fun cameraUploadsClicked() {
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

    /**
     * Instantiates the [NodeOptionsBottomSheetDialogFragment]
     *
     * @param node The [MegaNode]
     * @param mode Specifies the Bottom Sheet style. This is set to
     * [NodeOptionsBottomSheetDialogFragment.DEFAULT_MODE] by default
     * @param shareData An optional [ShareData]
     * @param nodeDeviceCenterInformation An optional [NodeDeviceCenterInformation] which contains
     * specific information of a Device Center Node to be displayed. This is provided when the
     * Bottom Dialog is instantiated from Device Center
     * @param hideHiddenActions if it is true, then don't show hide/unhide, otherwise show it by logic
     */
    @JvmOverloads
    fun showNodeOptionsPanel(
        node: MegaNode?,
        mode: Int = NodeOptionsBottomSheetDialogFragment.DEFAULT_MODE,
        shareData: ShareData? = null,
        nodeDeviceCenterInformation: NodeDeviceCenterInformation? = null,
        hideHiddenActions: Boolean = false,
    ) {
        Timber.d("showNodeOptionsPanel")
        if (node == null || bottomSheetDialogFragment.isBottomSheetDialogShown()) return
        bottomSheetDialogFragment = NodeOptionsBottomSheetDialogFragment.newInstance(
            nodeId = NodeId(node.handle),
            shareData = shareData,
            mode = mode,
            nodeDeviceCenterInformation = nodeDeviceCenterInformation,
            hideHiddenActions = hideHiddenActions,
        )
        bottomSheetDialogFragment?.show(supportFragmentManager, bottomSheetDialogFragment?.tag)
    }

    /**
     * Instantiates the [NodeOptionsBottomSheetDialogFragment]
     *
     * @param nodeId The [NodeId]
     * @param mode Specifies the Bottom Sheet style. This is set to
     * [NodeOptionsBottomSheetDialogFragment.DEFAULT_MODE] by default
     * @param shareData An optional [ShareData]
     * @param nodeDeviceCenterInformation An optional [NodeDeviceCenterInformation] which contains
     * specific information of a Device Center Node to be displayed. This is provided when the
     * Bottom Dialog is instantiated from Device Center
     * @param hideHiddenActions if it is true, then don't show hide/unhide, otherwise show it by logic
     */
    fun showNodeOptionsPanel(
        nodeId: NodeId?,
        mode: Int = NodeOptionsBottomSheetDialogFragment.DEFAULT_MODE,
        shareData: ShareData? = null,
        nodeDeviceCenterInformation: NodeDeviceCenterInformation? = null,
        hideHiddenActions: Boolean = false,
    ) {
        Timber.d("showNodeOptionsPanel")
        if (nodeId == null || bottomSheetDialogFragment.isBottomSheetDialogShown()) return
        bottomSheetDialogFragment = NodeOptionsBottomSheetDialogFragment.newInstance(
            nodeId = nodeId,
            shareData = shareData,
            mode = mode,
            nodeDeviceCenterInformation = nodeDeviceCenterInformation,
            hideHiddenActions = hideHiddenActions,
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

    fun showNodeLabelsPanel(nodeIds: List<NodeId>) {
        Timber.d("showNodeLabelsPanel for multiple nodes")
        if (bottomSheetDialogFragment.isBottomSheetDialogShown()) {
            bottomSheetDialogFragment?.dismiss()
        }
        val handles = nodeIds.map { it.longValue }
        bottomSheetDialogFragment = NodeLabelBottomSheetDialogFragment.newInstance(handles)
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

    /**
     * Shows the upload bottom sheet taking into account the upload type received as param.
     *
     * @param uploadType Indicates the type of upload:
     * - GENERAL_UPLOAD if nothing special has to be taken into account.
     * - DOCUMENTS_UPLOAD if action is from Documents section.
     * - HOMEPAGE_UPLOAD if action is from Home section
     * - CLOUD_DRIVE_UPLOAD if action is from Cloud Drive section
     */
    @JvmOverloads
    fun showUploadPanel(uploadType: Int = if (drawerItem === DrawerItem.HOMEPAGE) UploadBottomSheetDialogFragment.HOMEPAGE_UPLOAD else if (drawerItem === DrawerItem.CLOUD_DRIVE) UploadBottomSheetDialogFragment.CLOUD_DRIVE_UPLOAD else UploadBottomSheetDialogFragment.GENERAL_UPLOAD) {
        if (!hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            val permissions = readAndWritePermissions
            requestPermission(this, Constants.REQUEST_READ_WRITE_STORAGE, *permissions)
            return
        }

        if (drawerItem == DrawerItem.HOMEPAGE && homepageScreen == HomepageScreen.HOMEPAGE) {
            viewModel.showHomeFabOptionsBottomSheet()
        } else {
            if (bottomSheetDialogFragment.isBottomSheetDialogShown()) return
            bottomSheetDialogFragment = UploadBottomSheetDialogFragment.newInstance(uploadType)
            bottomSheetDialogFragment?.show(supportFragmentManager, bottomSheetDialogFragment?.tag)
        }
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
        refreshFragment(SharesFragment.TAG)
    }

    /**
     * Refresh the Cloud Order
     */
    private fun refreshCloudOrder() {
        refreshCloudDrive()
        refreshRubbishBin()
        backupsFragment?.refreshBackupsNodes()
        onNodesSharedUpdate()
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
        backupsFragment?.updateBackupsHandle(parentHandleBackups)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Timber.d("onNewIntent")
        if (Intent.ACTION_SEARCH == intent.action) {
            setToolbarTitle()
            isSearching = true
            if (searchMenuItem != null) {
                MenuItemCompat.collapseActionView(searchMenuItem)
            }
            return
        } else if (Constants.ACTION_SHOW_UPGRADE_ACCOUNT == intent.action) {
            navigateToUpgradeAccount()
            return
        } else if (Constants.ACTION_OPEN_DEVICE_CENTER == intent.action) {
            selectDrawerItem(DrawerItem.DEVICE_CENTER)
        } else if (Constants.ACTION_OPEN_SYNC_MEGA_FOLDER == intent.action) {
            intent.dataString?.split("#")?.get(1)?.toLong()?.let { handle ->
                megaNavigator.openNodeInCloudDrive(
                    activity = this,
                    nodeHandle = handle,
                    errorMessage = null,
                    isFromSyncFolders = true,
                )
            }
        }
        setIntent(intent)
    }

    private fun handleLocateFileNavigationFromIntent() {
        val path = intent.getStringExtra(FileStorageActivity.EXTRA_PATH) ?: return
        val fileNames = intent.getStringArrayListExtra(FileStorageActivity.EXTRA_FILE_NAMES)
        if (intent.getBooleanExtra(Constants.INTENT_EXTRA_IS_OFFLINE_PATH, false)) {
            selectDrawerItem(DrawerItem.HOMEPAGE)
            val offlinePath = path.replace(
                getString(R.string.section_saved_for_offline_new),
                ""
            ) + Constants.SEPARATOR
            openFullscreenOfflineFragment(offlinePath, fileNames)
        } else {
            lifecycleScope.launch {
                if (!fileBrowserViewModel.doesUriPathExists(UriPath(path))) {
                    showSnackbar(
                        SNACKBAR_TYPE,
                        getString(R.string.location_not_exist),
                        MEGACHAT_INVALID_HANDLE
                    )
                    return@launch
                }
                Intent(this@ManagerActivity, FileStorageActivity::class.java).apply {
                    action = FileStorageActivity.Mode.BROWSE_FILES.action
                    putExtra(FileStorageActivity.EXTRA_PATH, path)
                    fileNames?.let {
                        putStringArrayListExtra(FileStorageActivity.EXTRA_FILE_NAMES, it)
                    }
                    startActivity(this)
                }
            }
        }
    }

    override fun navigateToUpgradeAccount() {
        if (drawerLayout.isDrawerOpen(
                navigationView
            )
        ) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        megaNavigator.openUpgradeAccount(this)
        myAccountInfo.upgradeOpenedFrom = MyAccountInfo.UpgradeFrom.MANAGER
    }

    override fun navigateToContactRequests() {
        closeDrawer()
        startActivity(ContactsActivity.getReceivedRequestsIntent(this))
    }

    override fun navigateToMyAccount() {
        Timber.d("navigateToMyAccount")
        showMyAccount()
    }

    override fun navigateToSharedNode(nodeId: Long, childNodes: LongArray?) {
        openLocation(nodeId, childNodes)
    }

    override fun navigateToContactInfo(email: String) {
        ContactUtil.openContactInfoActivity(this, email)
    }

    override fun manageCopyMoveException(throwable: Throwable?): Boolean = when (throwable) {
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

    @SuppressLint("CheckResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        Timber.d("Request code: %d, Result code:%d", requestCode, resultCode)
        if (resultCode == Activity.RESULT_FIRST_USER) {
            showSnackbar(
                SNACKBAR_TYPE,
                getString(R.string.context_no_destination_folder),
                -1
            )
            return
        }
        when {
            requestCode == Constants.REQUEST_CODE_GET_FOLDER -> {
                UploadUtil.getFolder(this, resultCode, intent, currentParentHandle)
            }

            requestCode == Constants.REQUEST_CODE_GET_FOLDER_CONTENT -> {
                if (intent != null && resultCode == Activity.RESULT_OK) {
                    val result = intent.getStringExtra(ExtraConstant.EXTRA_ACTION_RESULT)
                    if (TextUtil.isTextEmpty(result)) {
                        return
                    }
                    showSnackbar(
                        SNACKBAR_TYPE,
                        result,
                        MEGACHAT_INVALID_HANDLE
                    )
                }
            }

            requestCode == Constants.REQUEST_CODE_SELECT_CONTACT && resultCode == Activity.RESULT_OK -> {
                Timber.d("onActivityResult REQUEST_CODE_SELECT_CONTACT OK")
                if (intent == null) {
                    Timber.w("Intent NULL")
                    return
                }
                val contactsData = intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS)
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
                        lifecycleScope.launch {
                            viewModel.initShareKeys(nodeHandles)
                            nodeController.shareFolders(nodeHandles, contactsData, item)
                        }
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

            requestCode == Constants.REQUEST_CODE_FILE_INFO -> {
                handleFileInfoSuccessResult(
                    intent = intent,
                    resultCode = resultCode,
                )
            }

            requestCode == Constants.REQUEST_WRITE_STORAGE || requestCode == Constants.REQUEST_READ_WRITE_STORAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    when (requestCode) {
                        Constants.REQUEST_READ_WRITE_STORAGE ->                         // Upload scenario
                            Handler(Looper.getMainLooper()).post { showUploadPanel() }
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
                checkFileNameCollisionsUseCase(
                    files = listOf(file.toDocumentEntity()),
                    parentNodeId = NodeId(parentHandle)
                )
            }.onSuccess { collisions ->
                collisions.firstOrNull()?.let {
                    nameCollisionActivityLauncher.launch(arrayListOf(it))
                } ?: viewModel.uploadFile(file, parentHandle)
            }.onFailure { throwable: Throwable? ->
                Timber.e(throwable)
                showSnackbar(
                    SNACKBAR_TYPE,
                    getString(R.string.general_error),
                    MEGACHAT_INVALID_HANDLE
                )
            }
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

    /**
     * Check the current storage state.
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
                storageState = newStorageState
            }

            StorageState.Orange -> {
                Timber.w("STORAGE STATE ORANGE")
                if (newStorageState.ordinal > storageState.ordinal) {
                    showStorageAlmostFullDialog()
                }
                storageState = newStorageState
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

    fun handleFileUris(uris: List<Uri>) {
        lifecycleScope.launch {
            runCatching {
                processFileDialog = showProcessFileDialog(this@ManagerActivity, intent)
                val documents = viewModel.prepareFiles(uris)
                onIntentProcessed(documents)
            }.onFailure {
                if (it is IOException) {
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.error_not_enough_free_space))
                }
                dismissAlertDialogIfExists(statusDialog)
                dismissAlertDialogIfExists(processFileDialog)
                Timber.e(it)
            }
        }
    }

    /**
     * Handle processed upload intent.
     *
     * @param entities List<DocumentEntity> containing all the upload info.
     */
    private fun onIntentProcessed(entities: List<DocumentEntity>) {
        Timber.d("onIntentProcessed")
        val parentNode: MegaNode? = getCurrentParentNode(currentParentHandle, -1)
        if (parentNode == null) {
            dismissAlertDialogIfExists(statusDialog)
            dismissAlertDialogIfExists(processFileDialog)
            showSnackbar(
                SNACKBAR_TYPE,
                getString(R.string.error_temporary_unavaible),
                -1
            )
            return
        }
        val parentHandle = parentNode.handle
        if (entities.isEmpty()) {
            dismissAlertDialogIfExists(statusDialog)
            dismissAlertDialogIfExists(processFileDialog)
            showSnackbar(
                SNACKBAR_TYPE,
                getString(R.string.upload_can_not_open),
                MEGACHAT_INVALID_HANDLE
            )
            return
        }
        if (viewModel.getStorageState() === StorageState.PayWall) {
            dismissAlertDialogIfExists(statusDialog)
            dismissAlertDialogIfExists(processFileDialog)
            showOverDiskQuotaPaywallWarning()
            return
        }

        lifecycleScope.launch {
            runCatching {
                checkFileNameCollisionsUseCase(
                    files = entities,
                    parentNodeId = NodeId(parentHandle)
                )
            }.onSuccess { collisions ->
                dismissAlertDialogIfExists(statusDialog)
                dismissAlertDialogIfExists(processFileDialog)
                if (collisions.isNotEmpty()) {
                    collisions.let {
                        nameCollisionActivityLauncher.launch(ArrayList(it))
                    }
                }
                val collidedSharesPath = collisions.map { it.path.value }.toSet()
                val sharesWithoutCollision = entities.filter {
                    collidedSharesPath.contains(it.uri.value).not()
                }
                if (sharesWithoutCollision.isNotEmpty()) {
                    lifecycleScope.launch {
                        viewModel.uploadFiles(
                            pathsAndNames = sharesWithoutCollision.map { it.uri.value }
                                .associateWith { null },
                            destinationId = NodeId(parentNode.handle)
                        )
                    }
                }
            }.onFailure {
                dismissAlertDialogIfExists(statusDialog)
                dismissAlertDialogIfExists(processFileDialog)
                Util.showErrorAlertDialog(
                    getString(R.string.error_temporary_unavaible),
                    false,
                    this@ManagerActivity
                )
            }
        }
    }

    /**
     * Open location based on where parent node is located
     *
     * @param nodeHandle          parent node handle
     * @param childNodeHandleList list of child nodes handles if comes from notification about new added nodes to shared folder
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
                fileBrowserViewModel.setFileBrowserHandle(nodeHandle)
                selectDrawerItem(drawerItem)
            }

            megaApi.rubbishNode?.handle -> {
                //Rubbish
                drawerItem = DrawerItem.RUBBISH_BIN
                openFolderRefresh = true
                comesFromNotificationHandleSaved = rubbishBinViewModel.state().currentHandle
                rubbishBinViewModel.setRubbishBinHandle(nodeHandle)
                selectDrawerItem(drawerItem)
            }

            megaApi.vaultNode?.handle -> {
                // Backups
                drawerItem = DrawerItem.BACKUPS
                openFolderRefresh = true

                comesFromNotificationHandleSaved =
                    backupsFragment?.getCurrentBackupsFolderHandle() ?: -1L
                backupsFragment?.updateBackupsHandle(nodeHandle)

                selectDrawerItem(drawerItem)
            }

            else -> {
                //Incoming Shares
                drawerItem = DrawerItem.SHARED_ITEMS
                comesFromNotificationSharedIndex = sharesViewModel.state.value.currentTab
                viewModel.setSharesTab(SharesTab.INCOMING_TAB)
                comesFromNotificationDeepBrowserTreeIncoming = deepBrowserTreeIncoming
                comesFromNotificationHandleSaved = getHandleFromIncomingSharesViewModel()
                sharesViewModel.onTabSelected(SharesTab.INCOMING_TAB)
                if (parent != null) {
                    val depth: Int = MegaApiUtils.calculateDeepBrowserTreeIncoming(node, this)
                    incomingSharesViewModel.setCurrentHandle(
                        nodeHandle, updateLoadingState = true,
                        refreshNodes = false
                    )
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
        refreshCloudDrive()
    }

    fun onNodesBackupsUpdate() {
        backupsFragment?.let {
            it.hideMultipleSelect()
            it.refreshBackupsNodes()
        }
    }

    private fun refreshIncomingShares() {
        incomingSharesViewModel.refreshNodes()
    }

    private fun refreshOutgoingShares() {
        outgoingSharesViewModel.refreshNodes()
    }

    fun refreshSharesFragments() {
        refreshOutgoingShares()
        refreshIncomingShares()
        linksViewModel.refreshLinkNodes()
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
        get() = incomingSharesViewModel.incomingTreeDepth()

    fun setDeepBrowserTreeIncoming(deep: Int, parentHandle: Long?) {
        parentHandle?.let {
            if (deep == 0)
                incomingSharesViewModel.goBackToRootLevel()
            else
                incomingSharesViewModel.setCurrentHandle(it)
        }
    }

    val deepBrowserTreeOutgoing: Int
        get() = outgoingSharesViewModel.outgoingTreeDepth()

    val tabItemShares: SharesTab
        get() = sharesViewModel.state.value.currentTab

    /**
     * Methods for incoming shares
     */
    private fun getHandleFromIncomingSharesViewModel() =
        incomingSharesViewModel.getCurrentNodeHandle()

    /**
     * Methods for outgoing shares
     */
    private fun getHandleFromOutgoingSharesViewModel() =
        outgoingSharesViewModel.getCurrentNodeHandle()

    /**
     * Returns the current node handle from links viewmodel
     */
    fun getHandleFromLinksViewModel() = linksViewModel.getCurrentNodeHandle()

    private fun getParentHandleForFavourites() = favouriteFolderFragment?.getParentNodeHandle()

    fun hideFabButton() {
        initFabButtonShow = false
        fabButton.isVisible = false
    }

    /**
     * Updates the fabButton icon and shows it.
     */
    private fun updateFabAndShow() {
        if (fileBrowserViewModel.state().selectedTab != CloudDriveTab.SYNC) {
            fabButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_add_white))
            Timber.d("Showing Fab Button")
            fabButton.show()
        }
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
            DrawerItem.CLOUD_DRIVE -> if (!fileBrowserViewModel.isMediaDiscoveryOpen()) {
                updateFabAndShow()
            }

            DrawerItem.SHARED_ITEMS -> when (tabItemShares) {
                SharesTab.INCOMING_TAB -> {
                    val handle = getHandleFromIncomingSharesViewModel()
                    val parentNodeInSF: MegaNode? = withContext(ioDispatcher) {
                        megaApi.getNodeByHandle(handle)
                    }
                    if (deepBrowserTreeIncoming <= 0 || parentNodeInSF == null) {
                        hideFabButton()
                        return@launch
                    }
                    when (megaApi.getAccess(parentNodeInSF)) {
                        MegaShare.ACCESS_OWNER, MegaShare.ACCESS_READWRITE, MegaShare.ACCESS_FULL -> updateFabAndShow()
                        MegaShare.ACCESS_READ -> hideFabButton()
                    }
                }

                SharesTab.OUTGOING_TAB -> {
                    // If the user is in the main page of Outgoing Shares, hide the Fab Button
                    if (deepBrowserTreeOutgoing <= 0) {
                        hideFabButton()
                    } else {
                        // Otherwise, check if the current parent node of the Outgoing Shares section is a Backup folder or not.
                        // Hide the Fab button if it is a Backup folder. Otherwise, show the Fab button.
                        val outgoingParentNode = withContext(ioDispatcher) {
                            megaApi.getNodeByHandle(getHandleFromOutgoingSharesViewModel())
                        }
                        if (outgoingParentNode != null && megaApi.isInVault(outgoingParentNode)) {
                            hideFabButton()
                        } else {
                            updateFabAndShow()
                        }
                    }
                }

                SharesTab.LINKS_TAB -> {
                    // If the user is in the main page of Links, hide the Fab Button
                    if (getHandleFromLinksViewModel() <= 0) {
                        hideFabButton()
                    } else {
                        // Otherwise, check if the current parent node of the Links section is a Backup folder or not.
                        // Hide the Fab button if it is a Backup folder. Otherwise, show the Fab button.
                        val linksParentNode = withContext(ioDispatcher) {
                            megaApi.getNodeByHandle(getHandleFromLinksViewModel())
                        }
                        if (linksParentNode != null && megaApi.isInVault(linksParentNode)) {
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

    fun copyError() {
        try {
            dismissAlertDialogIfExists(statusDialog)
            showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied), -1)
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

        if (mElevationCause == ELEVATION_CALL_IN_PROGRESS && !callInProgressViewModel.isShowing()) return

        // If any Tablayout is visible, set the background of the toolbar to transparent (or its elevation
        // overlay won't be correctly set via AppBarLayout) and then set the elevation of AppBarLayout,
        // in this way, both Toolbar and TabLayout would have expected elevation overlay.
        // If TabLayout is invisible, directly set toolbar's color for the elevation effect. Set AppBarLayout
        // elevation in this case, a crack would appear between toolbar and ChatRecentFragment's Appbarlayout, for example.
        val elevation: Float = resources.getDimension(R.dimen.toolbar_elevation)
        val toolbarElevationColor = ColorUtils.getColorForElevation(this, elevation)
        val transparentColor = ContextCompat.getColor(this, android.R.color.transparent)
        if (mElevationCause > 0) {
            toolbar.setBackgroundColor(toolbarElevationColor)
            appBarLayout.elevation = elevation
        } else {
            toolbar.setBackgroundColor(transparentColor)
            appBarLayout.elevation = 0f
        }
    }

    /**
     * Update chat badge
     *
     * @param unreadCount   Number of unread chats
     */
    private fun updateChatBadge(unreadCount: Int) {
        chatBadge.apply {
            findViewById<TextView>(R.id.chat_badge_text)?.text =
                unreadCount.takeIf { it <= 99 }?.toString() ?: "99+"

            isVisible = unreadCount > 0
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
        val bottomView: ConstraintLayout = findViewById(R.id.container_bottom)
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
            val height = if (adsContainerViewModel.isAdsLoaded())
                resources.getDimensionPixelSize(R.dimen.ads_web_view_and_bottom_navigation_view_height)
            else
                resources.getDimensionPixelSize(R.dimen.bottom_navigation_view_height)

            if (hide && visibility == View.VISIBLE) {
                updateMiniAudioPlayerVisibility(false)
                val bottom =
                    if (adsContainerViewModel.isAdsLoaded() && adsContainerView.isVisible) resources.getDimensionPixelSize(R.dimen.ads_web_view_container_height) else 0
                params.setMargins(0, 0, 0, bottom)
                fragmentLayout.layoutParams = params
                animate().translationY(height.toFloat())
                    .setDuration(Constants.ANIMATION_DURATION)
                    .withEndAction {
                        bottomNavigationView.visibility = View.GONE
                        syncNavigationBarColorWithBottomNavMenu(isReset = bottom == 0)
                    }
                    .start()
            } else if (!hide && visibility == View.VISIBLE) {
                animate().translationY(0f)
                    .setDuration(Constants.ANIMATION_DURATION)
                    .withStartAction {
                        visibility = View.VISIBLE
                        syncNavigationBarColorWithBottomNavMenu()
                    }
                    .withEndAction {
                        updateMiniAudioPlayerVisibility(true)
                        params.setMargins(0, 0, 0, height)
                        fragmentLayout.layoutParams = params
                    }
                    .start()
            } else if (!hide && visibility == View.GONE) {
                animate().translationY(0f).setDuration(Constants.ANIMATION_DURATION)
                    .withStartAction {
                        visibility = View.VISIBLE
                        syncNavigationBarColorWithBottomNavMenu()
                    }
                    .withEndAction {
                        updateMiniAudioPlayerVisibility(true)
                        params.setMargins(0, 0, 0, height)
                        fragmentLayout.layoutParams = params
                    }.start()
            }
            updateTransfersWidgetPosition(hide)
        }
    }

    /**
     * Sync the system navigation bar color with the bottom navigation menu's background color.
     * Applicable only for Android 9 and below as edge-to-edge is not supported on those.
     *
     * @param isReset True if need to reset the color to default, false otherwise.
     */
    private fun syncNavigationBarColorWithBottomNavMenu(
        isReset: Boolean = false,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return

        val colorId = if (isReset) R.color.app_background else R.color.color_background_surface_1
        window.navigationBarColor = resources.getColor(colorId, theme)
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


    fun hideKeyboardSearch() {
        Util.hideKeyboard(this)
        if (searchView != null) {
            searchView?.clearFocus()
        }
    }

    private fun openSearchView() {
        searchMenuItem?.expandActionView()
        searchView?.setQuery(viewModel.state.value.searchQuery, false)
    }

    /**
     * This method is invoked when you click on a folder from search page
     */
    private fun openSearchFolder(handle: Long) {
        when (drawerItem) {
            DrawerItem.HOMEPAGE -> {
                // Redirect to Cloud drive.
                selectDrawerItem(DrawerItem.CLOUD_DRIVE)
                fileBrowserViewModel.setFileBrowserHandle(handle)
                refreshFragment(FragmentTag.CLOUD_DRIVE.tag)
            }

            DrawerItem.CLOUD_DRIVE -> {
                fileBrowserViewModel.setFileBrowserHandle(handle)
                refreshFragment(FragmentTag.CLOUD_DRIVE.tag)
            }

            DrawerItem.SHARED_ITEMS -> {
                if (tabItemShares === SharesTab.INCOMING_TAB) {
                    incomingSharesViewModel.setCurrentHandle(handle)
                } else if (tabItemShares === SharesTab.OUTGOING_TAB) {
                    outgoingSharesViewModel.setCurrentHandle(handle)
                } else if (tabItemShares === SharesTab.LINKS_TAB) {
                    linksViewModel.openFolderByHandle(handle)
                }
                refreshSharesPageAdapter()
            }

            DrawerItem.BACKUPS -> {
                backupsFragment?.let {
                    it.updateBackupsHandle(handle)
                    it.refreshBackupsNodes()
                }
                refreshFragment(FragmentTag.BACKUPS.tag)
            }

            DrawerItem.RUBBISH_BIN -> {
                rubbishBinViewModel.setRubbishBinHandle(handle)
                refreshFragment(FragmentTag.RUBBISH_BIN_COMPOSE.tag)
            }

            else -> {}
        }
    }

    fun closeSearchView() {
        if (searchMenuItem?.isActionViewExpanded == true) {
            searchMenuItem?.collapseActionView()
        }
    }

    fun setTextSubmitted() {
        if (searchView != null) {
            searchView?.setQuery(viewModel.state.value.searchQuery, true)
        }
    }

    fun homepageToSearch() {
        hideItemsWhenSearchSelected()
        searchMenuItem?.expandActionView()
    }

    /**
     * Opens a location of a transfer.
     *
     * @param transfer the transfer to open its location
     */
    fun openTransferLocation(transfer: CompletedTransfer, path: String) {
        when {
            transfer.type.isDownloadType() -> {
                val fileNames = arrayListOf(transfer.fileName)
                when (transfer.isOffline) {
                    true -> {
                        selectDrawerItem(DrawerItem.HOMEPAGE)
                        val offlinePath = transfer.path.replace(
                            getString(R.string.section_saved_for_offline_new),
                            ""
                        ) + Constants.SEPARATOR
                        openFullscreenOfflineFragment(offlinePath, fileNames)
                    }

                    false -> {
                        Intent(this, FileStorageActivity::class.java).apply {
                            action = FileStorageActivity.Mode.BROWSE_FILES.action
                            putExtra(FileStorageActivity.EXTRA_PATH, path)
                            putStringArrayListExtra(FileStorageActivity.EXTRA_FILE_NAMES, fileNames)
                            startActivity(this)
                        }
                    }

                    null -> {
                        Timber.d("Unable to retrieve transfer isOffline value")
                    }
                }
            }

            transfer.type.isUploadType() -> {
                megaApi.getNodeByHandle(transfer.handle)
                    ?.let { viewNodeInFolder(it) } ?: run {
                    showSnackbar(
                        SNACKBAR_TYPE,
                        getString(R.string.warning_node_not_exists_in_cloud),
                        MEGACHAT_INVALID_HANDLE
                    )
                }
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
        val rootParentNode = megaApi.getRootParentNode(node)
        viewInFolderNode = node
        if (rootParentNode.handle == megaApi.rootNode?.handle) {
            fileBrowserViewModel.setFileBrowserHandle(
                node.parentHandle,
                highlightedNode = NodeId(node.handle)
            )
            refreshFragment(FragmentTag.CLOUD_DRIVE.tag)
            selectDrawerItem(DrawerItem.CLOUD_DRIVE)
        } else if (rootParentNode.handle == megaApi.rubbishNode?.handle) {
            rubbishBinViewModel.setRubbishBinHandle(node.parentHandle)
            refreshFragment(FragmentTag.RUBBISH_BIN_COMPOSE.tag)
            selectDrawerItem(DrawerItem.RUBBISH_BIN)
        } else if (rootParentNode.isInShare) {
            incomingSharesViewModel.setCurrentHandle(node.parentHandle)
            incomingSharesViewModel.refreshNodes()
            viewModel.setSharesTab(SharesTab.INCOMING_TAB)
            sharesViewModel.onTabSelected(SharesTab.INCOMING_TAB)
            refreshSharesPageAdapter()
            selectDrawerItem(DrawerItem.SHARED_ITEMS)
        } else if (rootParentNode.handle == megaApi.vaultNode?.handle) {
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
            findViewById(R.id.transfers_widget)
                ?: return
        val params = transfersWidgetLayout.layoutParams as LinearLayout.LayoutParams
        params.gravity = Gravity.END
        if (!bNVHidden && isInMainHomePage) {
            params.bottomMargin = Util.dp2px(TRANSFER_WIDGET_MARGIN_BOTTOM.toFloat(), outMetrics)
        } else {
            params.bottomMargin = 0
        }
        transfersWidgetLayout.layoutParams = params
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

    private val chatsFragment: ChatTabsFragment?
        get() = (supportFragmentManager.findFragmentByTag(FragmentTag.RECENT_CHAT.tag) as? ChatTabsFragment).also {
            chatTabsFragment = it
        }

    private fun getPermissionsFragment(): PermissionsFragment? {
        return (supportFragmentManager.findFragmentByTag(FragmentTag.PERMISSIONS.tag) as? PermissionsFragment).also {
            permissionsFragment = it
        }
    }

    override fun finishRenameActionWithSuccess(newName: String) {
        when (drawerItem) {
            DrawerItem.CLOUD_DRIVE -> refreshCloudDrive()
            DrawerItem.RUBBISH_BIN -> refreshRubbishBin()
            DrawerItem.BACKUPS -> backupsFragment?.invalidateRecyclerView()
            DrawerItem.SHARED_ITEMS -> {
                refreshOutgoingShares()
                refreshIncomingShares()
            }

            else -> {}
        }
    }

    override fun actionConfirmed() {
        //No update needed
    }

    private fun handleCheckLinkResult(result: Result<ChatLinkContent>) {
        if (result.isSuccess) {
            val chatLinkContent = result.getOrNull()
            if (chatLinkContent is ChatLinkContent.MeetingLink) {
                if (joiningToChatLink && TextUtil.isTextEmpty(chatLinkContent.link) && chatLinkContent.chatHandle == MEGACHAT_INVALID_HANDLE) {
                    showSnackbar(
                        SNACKBAR_TYPE,
                        getString(R.string.error_chat_link_init_error),
                        MEGACHAT_INVALID_HANDLE
                    )
                    resetJoiningChatLink()
                    return
                }
                if (chatLinkContent.link.isEmpty()) return
                lifecycleScope.launch {
                    runCatching {
                        getChatRoomUseCase(chatLinkContent.chatHandle)
                    }.onSuccess { chatRoom ->
                        chatRoom?.let {
                            if (chatRoom.isMeeting && chatRoom.isWaitingRoom && chatRoom.ownPrivilege == ChatRoomPermission.Moderator) {
                                viewModel.startOrAnswerMeetingWithWaitingRoomAsHost(
                                    chatId = chatLinkContent.chatHandle,
                                )
                            } else {
                                CallUtil.joinMeetingOrReturnCall(
                                    applicationContext,
                                    chatLinkContent.chatHandle,
                                    chatLinkContent.link,
                                    chatLinkContent.text,
                                    chatLinkContent.exist,
                                    chatLinkContent.userHandle,
                                    chatLinkContent.isWaitingRoom,
                                )
                            }
                        }
                    }.onFailure { exception ->
                        Timber.e(exception)
                    }
                }
            } else if (chatLinkContent is ChatLinkContent.ChatLink) {
                Timber.d("It's a chat")
                if (chatLinkContent.link.isEmpty()) return
                showChatLink(chatLinkContent.link, chatLinkContent.chatHandle)
            }
        } else if (result.exceptionOrNull() != null) {
            when (val e = result.exceptionOrNull()) {
                is IAmOnAnotherCallException -> {
                    CallUtil.showConfirmationInACall(
                        this,
                        getString(R.string.text_join_call),
                    )
                }

                is MeetingEndedException -> {
                    MeetingHasEndedDialogFragment(object :
                        MeetingHasEndedDialogFragment.ClickCallback {
                        override fun onViewMeetingChat() {
                            showChatLink(e.link, e.chatId)
                        }

                        override fun onLeave() {}
                    }, false).show(
                        supportFragmentManager,
                        MeetingHasEndedDialogFragment.TAG
                    )
                }

                is MegaException -> onErrorLoadingPreview(e.errorCode)
            }
        }
    }

    private fun onErrorLoadingPreview(errorCode: Int) {
        if (errorCode == MegaChatError.ERROR_NOENT) {
            Util.showAlert(
                this,
                getString(R.string.invalid_chat_link),
                getString(R.string.title_alert_chat_link_error)
            )
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
    private val isInImagesPage: Boolean
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
    fun isInMediaDiscovery() =
        drawerItem == DrawerItem.CLOUD_DRIVE && fileBrowserViewModel.isMediaDiscoveryOpen()

    /**
     * Create the instance of FileBackupManager
     */
    private fun initFileBackupManager() = FileBackupManager(
        activity = this,
        actionBackupListener = object : ActionBackupListener {
            override fun actionBackupResult(
                actionType: Int,
                operationType: Int,
                result: MoveRequestResult?,
                handle: Long,
            ) = Unit
        },
        getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        megaNodeUtilWrapper = megaNodeUtilWrapper,
    )

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
        sharesViewModel.onTabSelected(viewModel.state().sharesTab)
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
    private fun restoreFileBrowserAfterComingFromNotification() {
        comesFromNotifications = false
        comesFromNotificationHandle = -1
        selectDrawerItem(DrawerItem.NOTIFICATIONS)
        fileBrowserViewModel.setFileBrowserHandle(comesFromNotificationHandleSaved)
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

    fun openDrawer() {
        drawerLayout.openDrawer(navigationView)
    }

    private val isBusinessAccount: Boolean
        get() = megaApi.isBusinessAccount && myAccountInfo.accountType == Constants.BUSINESS
    private val isProFlexiAccount: Boolean
        get() = megaApi.isBusinessAccount && myAccountInfo.accountType == Constants.PRO_FLEXI

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
                checkForInAppAdvertisement(forceAskConsent = true)
                handleShowingAds()
            }
        }
    }

    /**
     * Set app bar visibility
     * We don't set the visibility to the appBarLayout directly because we keep status bar padding
     */
    private fun setAppBarVisibility(isVisible: Boolean) {
        Timber.d("setAppBarVisibility called with $isVisible")
        if (supportActionBar?.isShowing != isVisible) {
            if (isVisible) {
                supportActionBar?.show()
            } else {
                supportActionBar?.hide()
            }
        }
    }

    private fun setAppBarColor(@ColorInt color: Int) {
        appBarLayout.setBackgroundColor(color)
    }

    companion object {
        private const val BOTTOM_ITEM_BEFORE_OPEN_FULLSCREEN_OFFLINE =
            "BOTTOM_ITEM_BEFORE_OPEN_FULLSCREEN_OFFLINE"
        const val JOINING_CHAT_LINK = "JOINING_CHAT_LINK"
        const val LINK_JOINING_CHAT_LINK = "LINK_JOINING_CHAT_LINK"
        private const val PROCESS_FILE_DIALOG_SHOWN = "PROGRESS_DIALOG_SHOWN"
        private const val COMES_FROM_NOTIFICATIONS_SHARED_INDEX =
            "COMES_FROM_NOTIFICATIONS_SHARED_INDEX"

        // 8dp + 56dp(Fab size) + 8dp
        const val TRANSFER_WIDGET_MARGIN_BOTTOM = 72

        /**
         * The causes of elevating the app bar
         */
        const val ELEVATION_SCROLL = 0x01
        const val ELEVATION_CALL_IN_PROGRESS = 0x02
        private const val STATE_KEY_IS_IN_ALBUM_CONTENT = "isInAlbumContent"
        private const val STATE_KEY_IS_IN_PHOTOS_FILTER = "isInFilterPage"
        var drawerMenuItem: MenuItem? = null
    }
}
