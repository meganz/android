package mega.privacy.android.app.main

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.MimeTypeThumbnail
import mega.privacy.android.app.R
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_CREDENTIALS
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_FIRST_NAME
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_LAST_NAME
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_NICKNAME
import mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE
import mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_MANAGE_SHARE
import mega.privacy.android.app.constants.BroadcastConstants.EXTRA_USER_HANDLE
import mega.privacy.android.app.databinding.ActivityFileInfoBinding
import mega.privacy.android.app.databinding.DialogLinkBinding
import mega.privacy.android.app.interfaces.ActionBackupListener
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.listeners.ShareListener
import mega.privacy.android.app.main.adapters.MegaFileInfoSharedContactAdapter
import mega.privacy.android.app.main.controllers.ContactController
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.modalbottomsheet.FileContactsListBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.presentation.clouddrive.FileInfoViewModel
import mega.privacy.android.app.presentation.extensions.getFormattedStringOrDefault
import mega.privacy.android.app.presentation.extensions.getQuantityStringOrDefault
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.sync.fileBackups.FileBackupManager
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.OperationType.OPERATION_EXECUTE
import mega.privacy.android.app.usecase.CopyNodeUseCase
import mega.privacy.android.app.usecase.MoveNodeUseCase
import mega.privacy.android.app.usecase.data.MoveRequestResult
import mega.privacy.android.app.usecase.exception.MegaNodeException.ChildDoesNotExistsException
import mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists
import mega.privacy.android.app.utils.AlertsAndWarnings.showForeignStorageOverQuotaWarningDialog
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.AlertsAndWarnings.showSaveToDeviceConfirmDialog
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.CameraUploadUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ChatUtil.StatusIconLocation
import mega.privacy.android.app.utils.ColorUtils.getColorForElevation
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_BACKUP_SHARE_FOLDER
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_NONE
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showRenameNodeDialog
import mega.privacy.android.app.utils.MegaNodeUtil.checkBackupNodeTypeByHandle
import mega.privacy.android.app.utils.MegaNodeUtil.getFolderIcon
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLocationInfo
import mega.privacy.android.app.utils.MegaNodeUtil.getRootParentNode
import mega.privacy.android.app.utils.MegaNodeUtil.handleLocationClick
import mega.privacy.android.app.utils.MegaNodeUtil.isEmptyFolder
import mega.privacy.android.app.utils.MegaNodeUtil.showConfirmationLeaveIncomingShare
import mega.privacy.android.app.utils.MegaNodeUtil.showTakenDownNodeActionNotAvailableDialog
import mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.PreviewUtils
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils.checkNotificationsPermission
import mega.privacy.android.domain.entity.StorageState
import net.opacapp.multilinecollapsingtoolbar.CollapsingToolbarLayout
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaEvent
import nz.mega.sdk.MegaGlobalListenerInterface
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaSet
import nz.mega.sdk.MegaSetElement
import nz.mega.sdk.MegaShare
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert
import timber.log.Timber
import java.io.File
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

/**
 * Activity for showing file and folder info.
 *
 * @property passCodeFacade [PasscodeCheck] an injected component to enforce a Passcode security check
 * @property checkNameCollisionUseCase [CheckNameCollisionUseCase] injected use case
 * @property moveNodeUseCase [MoveNodeUseCase] injected use case
 * @property copyNodeUseCase [CopyNodeUseCase] injected use case
 */
@AndroidEntryPoint
class FileInfoActivity : BaseActivity(), ActionNodeCallback, SnackbarShower {

    @Inject
    lateinit var passCodeFacade: PasscodeCheck

    @Inject
    lateinit var checkNameCollisionUseCase: CheckNameCollisionUseCase

    @Inject
    lateinit var moveNodeUseCase: MoveNodeUseCase

    @Inject
    lateinit var copyNodeUseCase: CopyNodeUseCase

    private val viewModel: FileInfoViewModel by viewModels()

    private val megaRequestListener = object : MegaRequestListenerInterface {
        override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
            Timber.d("onRequestStart: ${request.name}")
        }

        override fun onRequestUpdate(api: MegaApiJava?, request: MegaRequest?) {
            Timber.d("onRequestUpdate: ${request?.name}")
        }

        override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, error: MegaError?) {
            this@FileInfoActivity.onRequestFinish(api, request, error)
        }

        override fun onRequestTemporaryError(
            api: MegaApiJava, request: MegaRequest,
            e: MegaError,
        ) {
            Timber.w("onRequestTemporaryError:  ${request.name}")
        }
    }
    private val megaGlobalListener = object : MegaGlobalListenerInterface {
        override fun onUsersUpdate(api: MegaApiJava?, users: java.util.ArrayList<MegaUser>?) {
            Timber.d("onUsersUpdate")
        }

        override fun onUserAlertsUpdate(
            api: MegaApiJava,
            users: java.util.ArrayList<MegaUserAlert>,
        ) {
            Timber.d("onUserAlertsUpdate")
        }

        override fun onNodesUpdate(api: MegaApiJava, nodes: java.util.ArrayList<MegaNode>?) {
            this@FileInfoActivity.onNodesUpdate(nodes)
        }

        override fun onReloadNeeded(api: MegaApiJava) {
            Timber.d("onReloadNeeded")
        }

        override fun onAccountUpdate(api: MegaApiJava) {
            Timber.d("onAccountUpdate")
        }

        override fun onContactRequestsUpdate(
            api: MegaApiJava,
            requests: java.util.ArrayList<MegaContactRequest>,
        ) {
            Timber.d("onContactRequestsUpdate")
        }

        override fun onEvent(api: MegaApiJava, event: MegaEvent) {
            Timber.d("onEvent")
        }

        override fun onSetsUpdate(api: MegaApiJava, sets: java.util.ArrayList<MegaSet>) {
            Timber.d("onSetsUpdate")
        }

        override fun onSetElementsUpdate(
            api: MegaApiJava,
            elements: java.util.ArrayList<MegaSetElement>,
        ) {
            Timber.d("onSetElementsUpdate")
        }

    }
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            retryConnectionsAndSignalPresence()
            if (isRemoveOffline) {
                val intent = Intent()
                intent.putExtra(NODE_HANDLE, handle)
                setResult(RESULT_OK, intent)
            }
            finish()
        }

    }

    private lateinit var binding: ActivityFileInfoBinding
    private val bindingContent get() = binding.contentFileInfoActivity
    private var firstIncomingLevel = true
    private val nodeAttacher = MegaAttacher(this)
    private val nodeSaver = NodeSaver(
        this, this, this,
        showSaveToDeviceConfirmDialog(this)
    )
    private var fileBackupManager: FileBackupManager? = null
    private val nodeController: NodeController by lazy { NodeController(this) }
    private var nodeVersions: ArrayList<MegaNode>? = null
    private var upArrow: Drawable? = null
    private var drawableRemoveLink: Drawable? = null
    private var drawableLink: Drawable? = null
    private var drawableShare: Drawable? = null
    private var drawableDots: Drawable? = null
    private var drawableDownload: Drawable? = null
    private var drawableLeave: Drawable? = null
    private var drawableCopy: Drawable? = null
    private var drawableChat: Drawable? = null
    private var isShareContactExpanded = false
    private var owner = true
    private var typeExport = -1
    private var sl: ArrayList<MegaShare>? = null
    private var mOffDelete: MegaOffline? = null
    private var downloadMenuItem: MenuItem? = null
    private var shareMenuItem: MenuItem? = null
    private var getLinkMenuItem: MenuItem? = null
    private var editLinkMenuItem: MenuItem? = null
    private var removeLinkMenuItem: MenuItem? = null
    private var renameMenuItem: MenuItem? = null
    private var moveMenuItem: MenuItem? = null
    private var copyMenuItem: MenuItem? = null
    private var rubbishMenuItem: MenuItem? = null
    private var deleteMenuItem: MenuItem? = null
    private var leaveMenuItem: MenuItem? = null
    private var sendToChatMenuItem: MenuItem? = null
    private lateinit var node: MegaNode
    private var availableOfflineBoolean = false
    private var cC: ContactController? = null
    private var statusDialog: AlertDialog? = null
    private var publicLink = false
    private var moveToRubbish = false
    private val density by lazy { outMetrics.density }
    private var shareIt = true
    private var from = 0
    private var permissionsDialog: AlertDialog? = null
    private var contactMail: String? = null
    private var isRemoveOffline = false
    private var handle: Long = 0
    private var adapterType = 0
    private var selectedShare: MegaShare? = null
    private var listContacts: List<MegaShare> = emptyList()
    private var fullListContacts: List<MegaShare> = emptyList()
    private val adapter: MegaFileInfoSharedContactAdapter by lazy {
        MegaFileInfoSharedContactAdapter(
            this,
            node,
            listContacts,
            bindingContent.fileInfoContactListView
        ).apply {
            setShareList(listContacts)
            positionClicked = -1
            isMultipleSelect = false
        }
    }
    private var actionMode: ActionMode? = null
    private var versionsToRemove = 0
    private var versionsRemoved = 0
    private var errorVersionRemove = 0
    private var bottomSheetDialogFragment: FileContactsListBottomSheetDialogFragment? = null
    private var currentColorFilter = 0
    private val manageShareReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            intent?.let {
                if (adapter.isMultipleSelect) {
                    adapter.clearSelections()
                    hideMultipleSelect()
                }
                adapter.setShareList(listContacts)
                statusDialog?.dismiss()
                permissionsDialog?.dismiss()
            }
        }
    }
    private val contactUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_UPDATE_NICKNAME || intent.action == ACTION_UPDATE_FIRST_NAME || intent.action == ACTION_UPDATE_LAST_NAME || intent.action == ACTION_UPDATE_CREDENTIALS) {
                updateAdapter(intent.getLongExtra(EXTRA_USER_HANDLE, MegaApiJava.INVALID_HANDLE))
            }
        }
    }

    /**
     * activate action mode from adapter
     */
    fun activateActionMode() {
        Timber.d("activateActionMode")
        if (!adapter.isMultipleSelect) {
            adapter.isMultipleSelect = true
            actionMode = startSupportActionMode(ActionBarCallBack())
        }
    }

    /**
     * show snack bar
     */
    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        showSnackbar(type, bindingContent.fileInfoContactListContainer, content, chatId)
    }

    override fun finishRenameActionWithSuccess(newName: String) {
        node = megaApi.getNodeByHandle((node).handle) ?: return
        binding.fileInfoCollapseToolbar.title = node.name
    }

    override fun actionConfirmed() {
        //No update needed
    }

    override fun createFolder(folderName: String) {
        //No action needed
    }

    private inner class ActionBarCallBack : ActionMode.Callback {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            Timber.d("onActionItemClicked")
            val shares = adapter.selectedShares
            when (item.itemId) {
                R.id.action_file_contact_list_permissions -> {
                    //Change permissions
                    val dialogBuilder = MaterialAlertDialogBuilder(
                        this@FileInfoActivity,
                        R.style.ThemeOverlay_Mega_MaterialAlertDialog
                    ).apply {
                        setTitle(getFormattedStringOrDefault(R.string.file_properties_shared_folder_permissions))

                        val items = arrayOf<CharSequence>(
                            getFormattedStringOrDefault(R.string.file_properties_shared_folder_read_only),
                            getFormattedStringOrDefault(R.string.file_properties_shared_folder_read_write),
                            getFormattedStringOrDefault(R.string.file_properties_shared_folder_full_access)
                        )

                        setSingleChoiceItems(items, -1) { _, it ->
                            clearSelections()
                            permissionsDialog?.dismiss()
                            statusDialog = createProgressDialog(
                                this@FileInfoActivity,
                                getFormattedStringOrDefault(R.string.context_permissions_changing_folder)
                            )
                            cC?.changePermissions(cC?.getEmailShares(shares), it, node)
                        }
                    }

                    permissionsDialog = dialogBuilder.show()
                }
                R.id.action_file_contact_list_delete -> {
                    shares?.size?.takeIf { it > 0 }?.let { size ->
                        if (size > 1) {
                            Timber.d("Remove multiple contacts")
                            showConfirmationRemoveMultipleContactFromShare(shares)
                        } else {
                            Timber.d("Remove one contact")
                            showConfirmationRemoveContactFromShare(shares[0].user)
                        }
                    }
                }
                R.id.cab_menu_select_all -> {
                    selectAll()
                }
                R.id.cab_menu_unselect_all -> {
                    clearSelections()
                }
            }
            return false
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            Timber.d("onCreateActionMode")
            mode.menuInflater?.inflate(R.menu.file_contact_shared_browser_action, menu)
            return true
        }

        override fun onDestroyActionMode(arg0: ActionMode) {
            Timber.d("onDestroyActionMode")
            adapter.clearSelections()
            adapter.isMultipleSelect = false
            invalidateOptionsMenu()
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            Timber.d("onPrepareActionMode")
            val selected: List<MegaShare> = adapter.selectedShares
            var deleteShare = false
            var permissions = false
            if (selected.isNotEmpty()) {
                permissions = true
                deleteShare = true
                val unselect = menu.findItem(R.id.cab_menu_unselect_all)
                menu.findItem(R.id.cab_menu_select_all).isVisible =
                    selected.size != adapter.itemCount
                unselect.title = getFormattedStringOrDefault(R.string.action_unselect_all)
                unselect.isVisible = true
            } else {
                menu.findItem(R.id.cab_menu_select_all).isVisible = true
                menu.findItem(R.id.cab_menu_unselect_all).isVisible = false
            }
            val changePermissionsMenuItem = menu.findItem(R.id.action_file_contact_list_permissions)
            if (isNodeInInbox(node)) {
                // If the node came from Backups, hide the Change Permissions option from the Action Bar
                changePermissionsMenuItem.isVisible = false
                changePermissionsMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            } else {
                // Otherwise, change Change Permissions visibility depending on whether there are
                // selected contacts or none
                changePermissionsMenuItem.isVisible = permissions
                if (permissions) {
                    changePermissionsMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                } else {
                    changePermissionsMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                }
            }
            menu.findItem(R.id.action_file_contact_list_delete).isVisible =
                deleteShare
            if (deleteShare) {
                menu.findItem(R.id.action_file_contact_list_delete)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            } else {
                menu.findItem(R.id.action_file_contact_list_delete)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            }
            return false
        }
    }

    /**
     * Perform Activity initialization
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)
        if (shouldRefreshSessionDueToSDK() || shouldRefreshSessionDueToKarere()) {
            finish()
            return
        }
        node = getNodeFromExtras() ?: run {
            finish()
            return
        }
        nodeVersions = megaApi.getVersions(node)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        initFileBackupManager()
        cC = ContactController(this)
        adapterType = intent.getIntExtra("adapterType", Constants.FILE_BROWSER_ADAPTER)

        megaApi.addGlobalListener(megaGlobalListener)
        savedInstanceState?.apply {
            val handle = getLong(KEY_SELECTED_SHARE_HANDLE, MegaApiJava.INVALID_HANDLE)
            if (handle == MegaApiJava.INVALID_HANDLE) {
                return
            }
            val list = megaApi.getOutShares(node)
            for (share in list) {
                if (handle == share.nodeHandle) {
                    selectedShare = share
                    break
                }
            }
            nodeAttacher.restoreState(this)
            nodeSaver.restoreState(this)
        }

        //register receivers
        registerReceiver(manageShareReceiver, IntentFilter(BROADCAST_ACTION_INTENT_MANAGE_SHARE))
        val contactUpdateFilter = IntentFilter(BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE)
        contactUpdateFilter.addAction(ACTION_UPDATE_NICKNAME)
        contactUpdateFilter.addAction(ACTION_UPDATE_FIRST_NAME)
        contactUpdateFilter.addAction(ACTION_UPDATE_LAST_NAME)
        contactUpdateFilter.addAction(ACTION_UPDATE_CREDENTIALS)
        registerReceiver(contactUpdateReceiver, contactUpdateFilter)

        //view
        setupView()
    }

    private fun getNodeFromExtras(): MegaNode? {
        val extras = intent.extras
        if (extras != null) {
            from = extras.getInt("from")
            if (from == Constants.FROM_INCOMING_SHARES) {
                firstIncomingLevel = extras.getBoolean(Constants.INTENT_EXTRA_KEY_FIRST_LEVEL)
            }
            val handleNode = extras.getLong("handle", -1)
            Timber.d("Handle of the selected node: %s", handleNode)
            return megaApi.getNodeByHandle(handleNode) ?: run {
                Timber.w("Node is NULL")
                null
            }

        } else {
            Timber.w("Extras is NULL")
            return null
        }
    }

    private fun setupView() {
        binding = ActivityFileInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getActionBarDrawables()
        with(binding) {
            setSupportActionBar(toolbar)
            filePropertiesPermissionInfo.isVisible = false
            val params = fileInfoIconLayout.layoutParams as CollapsingToolbarLayout.LayoutParams
            val rect = Rect()
            window.decorView.getWindowVisibleDisplayFrame(rect)
            params.setMargins(Util.dp2px(16f), Util.dp2px(90f) + rect.top, 0, Util.dp2px(14f))
            fileInfoIconLayout.layoutParams = params
            fileInfoImageLayout.isVisible = false
            val statusBarColor =
                getColorForElevation(
                    this@FileInfoActivity,
                    resources.getDimension(R.dimen.toolbar_elevation)
                )
            fileInfoCollapseToolbar.setStatusBarScrimColor(statusBarColor)
            if (Util.isDarkMode(this@FileInfoActivity)) {
                fileInfoCollapseToolbar.setContentScrimColor(statusBarColor)
            }
            if (node.hasPreview() || node.hasThumbnail()) {
                appBar.addOnOffsetChangedListener { appBar: AppBarLayout, offset: Int ->
                    val collapsed = offset < 0 && abs(offset) >= appBar.totalScrollRange / 2
                    setActionBarDrawablesColorFilter(
                        resources.getColor(
                            if (collapsed) R.color.grey_087_white_087 else R.color.white_alpha_087,
                            null
                        )
                    )
                }
                fileInfoCollapseToolbar.setCollapsedTitleTextColor(
                    ContextCompat.getColor(this@FileInfoActivity, R.color.grey_087_white_087)
                )
                fileInfoCollapseToolbar.setExpandedTitleColor(
                    resources.getColor(R.color.white_alpha_087, null)
                )
            } else {
                setActionBarDrawablesColorFilter(
                    resources.getColor(
                        R.color.grey_087_white_087,
                        null
                    )
                )
            }
            supportActionBar?.apply {
                bindingContent.nestedLayout.setOnScrollChangeListener { v: NestedScrollView, _: Int, _: Int, _: Int, _: Int ->
                    Util.changeViewElevation(
                        this,
                        v.canScrollVertically(-1) && v.visibility == View.VISIBLE,
                        outMetrics
                    )
                }
                setDisplayShowTitleEnabled(false)
                setHomeButtonEnabled(true)
                setDisplayHomeAsUpEnabled(true)
            }
        }

        with(bindingContent) {
            //Available Offline Layout
            availableOfflineLayout.isVisible = true

            //Share with Layout
            filePropertiesSharedLayout.setOnClickListener {
                sharedContactClicked()
            }
            filePropertiesSharedInfoButton.setOnClickListener {
                sharedContactClicked()
            }
            //Owner Layout
            val ownerString = "(${getFormattedStringOrDefault(R.string.file_properties_owner)})"
            filePropertiesOwnerLabelOwner.text = ownerString
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                filePropertiesOwnerLabel.setMaxWidthEmojis(Util.dp2px(MAX_WIDTH_FILENAME_LAND))
                filePropertiesOwnerInfo.maxWidth = Util.dp2px(MAX_WIDTH_FILENAME_LAND_2)
            } else {
                filePropertiesOwnerLabel.setMaxWidthEmojis(Util.dp2px(MAX_WIDTH_FILENAME_PORT))
                filePropertiesOwnerInfo.maxWidth = Util.dp2px(MAX_WIDTH_FILENAME_PORT_2)
            }
            filePropertiesOwnerLayout.isVisible = false

            //Folder Versions Layout
            filePropertiesFolderVersionsLayout.isVisible = false
            filePropertiesFolderCurrentVersionsLayout.isVisible = false
            filePropertiesFolderPreviousVersionsLayout.isVisible = false

            //Content Layout
            filePropertiesLinkButton.text = getFormattedStringOrDefault(R.string.context_copy)
            filePropertiesLinkButton.setOnClickListener {
                Timber.d("Copy link button")
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Copied Text", node.publicLink)
                clipboard.setPrimaryClip(clip)
                showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getFormattedStringOrDefault(R.string.file_properties_get_link),
                    -1
                )
            }

            val listView = fileInfoContactListView
            listView.itemAnimator = Util.noChangeRecyclerViewItemAnimator()
            listView.addItemDecoration(SimpleDividerItemDecoration(this@FileInfoActivity))
            val mLayoutManager = LinearLayoutManager(this@FileInfoActivity)
            listView.layoutManager = mLayoutManager

            //get shared contact list and max number can be displayed in the list is five
            setContactList()
            moreButton.setOnClickListener {
                val i = Intent(this@FileInfoActivity, FileContactListActivity::class.java)
                i.putExtra(Constants.NAME, node.handle)
                startActivity(i)
            }
            setMoreButtonText()

            //setup adapter
            listView.adapter = adapter
            refreshProperties()
            val parent = nodeController.getParent(node)
            if (!node.isTakenDown && parent.handle != megaApi.rubbishNode.handle) {
                filePropertiesSwitch.isEnabled = true
                filePropertiesSwitch.setOnCheckedChangeListener { _: CompoundButton, _: Boolean ->
                    filePropertiesSwitch()
                }
                filePropertiesAvailableOfflineText.setTextColor(
                    ContextCompat.getColor(this@FileInfoActivity, R.color.grey_087_white_087)
                )
            } else {
                filePropertiesSwitch.isEnabled = false
                filePropertiesAvailableOfflineText.setTextColor(
                    ContextCompat.getColor(this@FileInfoActivity, R.color.grey_700_026_grey_300_026)
                )
            }
            //Location Layout
            getNodeLocationInfo(
                adapterType, from == Constants.FROM_INCOMING_SHARES,
                node.handle
            )?.let { locationInfo ->
                with(filePropertiesInfoDataLocation) {
                    text = locationInfo.location
                    setOnClickListener {
                        handleLocationClick(this@FileInfoActivity, adapterType, locationInfo)
                    }
                }
            } ?: run {
                filePropertiesLocationLayout.isVisible = false
            }

            if (node.isFolder) {
                filePropertiesCreatedLayout.isVisible = false
                if (isEmptyFolder(node)) {
                    availableOfflineLayout.isVisible = false
                    availableOfflineSeparator.isVisible = false
                }
            } else {
                filePropertiesCreatedLayout.isVisible = true
            }
            val name = node.name
            binding.fileInfoCollapseToolbar.title = name
            // If the Node belongs to Backups or has no versions, then hide
            // the Versions layout
            filePropertiesTextNumberVersions.setOnClickListener {
                startActivityForResult(
                    Intent(this@FileInfoActivity, VersionsFileActivity::class.java)
                        .putExtra("handle", node.handle),
                    Constants.REQUEST_CODE_DELETE_VERSIONS_HISTORY
                )
            }
            if (isNodeInInbox(node) || !megaApi.hasVersions(node)) {
                filePropertiesVersionsLayout.isVisible = false
                separatorVersions.isVisible = false
            } else {
                filePropertiesVersionsLayout.isVisible = true
                val text = getQuantityStringOrDefault(
                    R.plurals.number_of_versions,
                    megaApi.getNumVersions(node),
                    megaApi.getNumVersions(node)
                )
                filePropertiesTextNumberVersions.text = text
                separatorVersions.isVisible = true
            }
        }
        setIconResource()
    }

    /**
     * Initializes the FileBackupManager
     */
    private fun initFileBackupManager() {
        fileBackupManager = FileBackupManager(
            this,
            object : ActionBackupListener {
                override fun actionBackupResult(
                    actionType: Int,
                    operationType: Int,
                    result: MoveRequestResult?,
                    handle: Long,
                ) {
                    if (actionType == ACTION_BACKUP_SHARE_FOLDER && operationType == OPERATION_EXECUTE) {
                        shareFolder()
                    }
                }
            })
    }

    private fun getActionBarDrawables() {
        drawableDots = ContextCompat
            .getDrawable(applicationContext, R.drawable.ic_dots_vertical_white)?.mutate()

        upArrow = ContextCompat
            .getDrawable(applicationContext, R.drawable.ic_arrow_back_white)?.mutate()
        drawableRemoveLink = ContextCompat
            .getDrawable(applicationContext, R.drawable.ic_remove_link)?.mutate()
        drawableLink = ContextCompat
            .getDrawable(applicationContext, R.drawable.ic_link_white)?.mutate()
        drawableShare = ContextCompat
            .getDrawable(applicationContext, R.drawable.ic_share)?.mutate()
        drawableDownload = ContextCompat
            .getDrawable(applicationContext, R.drawable.ic_download_white)?.mutate()
        drawableLeave = ContextCompat
            .getDrawable(applicationContext, R.drawable.ic_leave_share_w)?.mutate()
        drawableCopy = ContextCompat
            .getDrawable(applicationContext, R.drawable.ic_copy_white)?.mutate()
        drawableChat = ContextCompat
            .getDrawable(applicationContext, R.drawable.ic_send_to_contact)?.mutate()
    }

    private fun setOwnerState(userHandle: Long) =
        ChatUtil.setContactStatus(
            megaChatApi.getUserOnlineStatus(userHandle),
            bindingContent.filePropertiesOwnerStateIcon,
            StatusIconLocation.STANDARD
        )

    /**
     * creates the options menu for this activity
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.file_info_action, menu)
        downloadMenuItem = menu.findItem(R.id.cab_menu_file_info_download)
        shareMenuItem = menu.findItem(R.id.cab_menu_file_info_share_folder)
        getLinkMenuItem = menu.findItem(R.id.cab_menu_file_info_get_link)
        getLinkMenuItem?.title = getQuantityStringOrDefault(R.plurals.get_links, 1)
        editLinkMenuItem = menu.findItem(R.id.cab_menu_file_info_edit_link)
        removeLinkMenuItem = menu.findItem(R.id.cab_menu_file_info_remove_link)
        renameMenuItem = menu.findItem(R.id.cab_menu_file_info_rename)
        moveMenuItem = menu.findItem(R.id.cab_menu_file_info_move)
        copyMenuItem = menu.findItem(R.id.cab_menu_file_info_copy)
        rubbishMenuItem = menu.findItem(R.id.cab_menu_file_info_rubbish)
        deleteMenuItem = menu.findItem(R.id.cab_menu_file_info_delete)
        leaveMenuItem = menu.findItem(R.id.cab_menu_file_info_leave)
        sendToChatMenuItem = menu.findItem(R.id.cab_menu_file_info_send_to_chat)
        setIconsColorFilter()
        megaApi.getNodeByHandle(node.handle)?.apply {
            val parent = megaApi.getRootParentNode(this)

            if (parent.handle == megaApi.rubbishNode.handle) {
                deleteMenuItem?.isVisible = true
            } else {
                setDefaultOptionsToolbar()
            }
            // Check if read-only properties should be applied on MenuItems
            shouldApplyMenuItemReadOnlyState(parent)
        }
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Checks and applies read-only restrictions (unable to Favourite, Rename, Move, or Move to Rubbish Bin)
     * if the MegaNode is a Backup node.
     *
     * @param node The Mega Node
     */
    private fun shouldApplyMenuItemReadOnlyState(node: MegaNode) {
        if (isNodeInInbox(node)) {
            renameMenuItem?.isVisible = false
            moveMenuItem?.isVisible = false
            rubbishMenuItem?.isVisible = false
        }
    }

    /**
     * Sets up the default items to be displayed on the Toolbar Menu
     */
    private fun setDefaultOptionsToolbar() {
        if (!node.isTakenDown && !node.isFolder) {
            sendToChatMenuItem?.isVisible = true
            sendToChatMenuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
        if (from == Constants.FROM_INCOMING_SHARES) {
            if (!node.isTakenDown) {
                downloadMenuItem?.isVisible = true
                downloadMenuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                leaveMenuItem?.isVisible = firstIncomingLevel
                leaveMenuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                copyMenuItem?.isVisible = true
            }
            when (megaApi.getAccess(node)) {
                MegaShare.ACCESS_OWNER, MegaShare.ACCESS_FULL -> {
                    rubbishMenuItem?.isVisible = !firstIncomingLevel
                    renameMenuItem?.isVisible = true
                }
                MegaShare.ACCESS_READ -> copyMenuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            }
        } else {
            if (!node.isTakenDown) {
                downloadMenuItem?.isVisible = true
                downloadMenuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                if (node.isFolder) {
                    shareMenuItem?.isVisible = true
                    shareMenuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                }
                if (node.isExported) {
                    editLinkMenuItem?.isVisible = true
                    removeLinkMenuItem?.isVisible = true
                    removeLinkMenuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                } else {
                    getLinkMenuItem?.isVisible = true
                    getLinkMenuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                }
                copyMenuItem?.isVisible = true
            }
            rubbishMenuItem?.isVisible = true
            renameMenuItem?.isVisible = true
            moveMenuItem?.isVisible = true
        }
    }

    /**
     * Changes the drawables color in ActionBar depending on the color received.
     *
     * @param color Can be Color.WHITE or Color.WHITE.
     */
    private fun setActionBarDrawablesColorFilter(color: Int) {
        if (currentColorFilter == color || supportActionBar == null) {
            return
        }
        currentColorFilter = color
        upArrow?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        supportActionBar?.setHomeAsUpIndicator(upArrow)
        drawableDots?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        binding.toolbar.overflowIcon = drawableDots
        setIconsColorFilter()
    }

    /**
     * Sets the toolbar icons color.
     */
    private fun setIconsColorFilter() {
        if (removeLinkMenuItem == null || getLinkMenuItem == null || downloadMenuItem == null
            || shareMenuItem == null || leaveMenuItem == null || copyMenuItem == null
            || sendToChatMenuItem == null
        ) {
            return
        }
        drawableRemoveLink?.colorFilter =
            PorterDuffColorFilter(currentColorFilter, PorterDuff.Mode.SRC_IN)
        removeLinkMenuItem?.icon = drawableRemoveLink
        drawableLink?.colorFilter =
            PorterDuffColorFilter(currentColorFilter, PorterDuff.Mode.SRC_IN)
        getLinkMenuItem?.icon = drawableLink
        drawableDownload?.colorFilter =
            PorterDuffColorFilter(currentColorFilter, PorterDuff.Mode.SRC_IN)
        downloadMenuItem?.icon = drawableDownload
        drawableShare?.colorFilter =
            PorterDuffColorFilter(currentColorFilter, PorterDuff.Mode.SRC_IN)
        shareMenuItem?.icon = drawableShare
        drawableLeave?.colorFilter =
            PorterDuffColorFilter(currentColorFilter, PorterDuff.Mode.SRC_IN)
        leaveMenuItem?.icon = drawableLeave
        drawableCopy?.colorFilter =
            PorterDuffColorFilter(currentColorFilter, PorterDuff.Mode.SRC_IN)
        copyMenuItem?.icon = drawableCopy
        drawableChat?.colorFilter =
            PorterDuffColorFilter(currentColorFilter, PorterDuff.Mode.SRC_IN)
        sendToChatMenuItem?.icon = drawableChat
    }

    /**
     * Checks whether the provided Node exists in the Inbox or not
     *
     * @param node The Provided Node
     *
     * @return true if the provided Node exists in the Inbox, and false if otherwise
     */
    private fun isNodeInInbox(node: MegaNode?) = node != null && megaApi.isInInbox(node)


    /**
     * performs the action corresponding to the menu item selected
     */
    @SuppressLint("NonConstantResourceId")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("onOptionsItemSelected")
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
            }
            R.id.cab_menu_file_info_download -> {
                checkNotificationsPermission(this)
                nodeSaver.saveNode(
                    node,
                    highPriority = false,
                    isFolderLink = false,
                    fromMediaViewer = false,
                    needSerialize = false
                )
            }
            R.id.cab_menu_file_info_share_folder -> {
                val nodeType = checkBackupNodeTypeByHandle(megaApi, node)
                if (nodeType != BACKUP_NONE) {
                    // Display a warning dialog when sharing a Backup folder and limit folder
                    // access to read-only
                    fileBackupManager?.defaultActionBackupNodeCallback?.let {
                        fileBackupManager?.shareBackupFolder(
                            nodeController,
                            node,
                            nodeType,
                            ACTION_BACKUP_SHARE_FOLDER,
                            it
                        )
                    }
                } else {
                    shareFolder()
                }
            }
            R.id.cab_menu_file_info_get_link, R.id.cab_menu_file_info_edit_link -> {
                if (showTakenDownNodeActionNotAvailableDialog(node, this)) {
                    return false
                }
                LinksUtil.showGetLinkActivity(this, node.handle)
            }
            R.id.cab_menu_file_info_remove_link -> {
                if (showTakenDownNodeActionNotAvailableDialog(node, this)) {
                    return false
                }
                shareIt = false
                val dialogLayout = DialogLinkBinding.inflate(layoutInflater).apply {
                    (dialogLinkTextRemove.layoutParams as RelativeLayout.LayoutParams).setMargins(
                        Util.scaleWidthPx(
                            25,
                            outMetrics
                        ), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(10, outMetrics), 0
                    )
                    dialogLinkLinkUrl.isVisible = false
                    dialogLinkLinkKey.isVisible = false
                    dialogLinkSymbol.isVisible = false
                    dialogLinkTextRemove.isVisible = true
                    dialogLinkTextRemove.text =
                        getFormattedStringOrDefault(R.string.context_remove_link_warning_text)

                    val isLandscape =
                        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    val size = if (isLandscape) 10 else 15
                    val scaleW = Util.getScaleW(outMetrics, density)
                    dialogLinkTextRemove.setTextSize(TypedValue.COMPLEX_UNIT_SP, size * scaleW)
                }

                MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
                    .setView(dialogLayout.root)
                    .setPositiveButton(getFormattedStringOrDefault(R.string.context_remove)) { _: DialogInterface?, _: Int ->
                        typeExport = TYPE_EXPORT_REMOVE
                        megaApi.disableExport(node, megaRequestListener)
                    }.setNegativeButton(getFormattedStringOrDefault(R.string.general_cancel), null)
                    .show()
            }
            R.id.cab_menu_file_info_copy -> {
                showCopy()
            }
            R.id.cab_menu_file_info_move -> {
                showMove()
            }
            R.id.cab_menu_file_info_rename -> {
                showRenameNodeDialog(this, node, this, this)
            }
            R.id.cab_menu_file_info_leave -> showConfirmationLeaveIncomingShare(this, this, node)
            R.id.cab_menu_file_info_rubbish, R.id.cab_menu_file_info_delete -> {
                moveToTrash()
            }
            R.id.cab_menu_file_info_send_to_chat -> {
                Timber.d("Send chat option")
                nodeAttacher.attachNode(node)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Starts a new Intent to share the folder to different contacts
     */
    private fun shareFolder() =
        startActivityForResult(
            Intent(this, AddContactActivity::class.java)
                .putExtra("contactType", Constants.CONTACT_TYPE_BOTH)
                .putExtra("MULTISELECT", 0)
                .putExtra(AddContactActivity.EXTRA_NODE_HANDLE, node.handle),
            REQUEST_CODE_SELECT_CONTACT
        )

    private fun refreshProperties() {
        Timber.d("refreshProperties")
        if (!node.isTakenDown && node.isExported) {
            publicLink = true
            bindingContent.dividerLinkLayout.isVisible = true
            bindingContent.filePropertiesLinkLayout.isVisible = true
            bindingContent.filePropertiesCopyLayout.isVisible = true
            bindingContent.filePropertiesLinkText.text = node.publicLink
            bindingContent.filePropertiesLinkDate.text = getFormattedStringOrDefault(
                R.string.general_date_label, TimeUtils.formatLongDateTime(
                    node.publicLinkCreationTime
                )
            )
        } else {
            publicLink = false
            bindingContent.dividerLinkLayout.isVisible = false
            bindingContent.filePropertiesLinkLayout.isVisible = false
            bindingContent.filePropertiesCopyLayout.isVisible = false
        }

        if (node.isFile) {
            Timber.d("Node is FILE")
            bindingContent.filePropertiesSharedLayout.isVisible = false
            bindingContent.dividerSharedLayout.isVisible = false
            bindingContent.filePropertiesInfoMenuSize.text =
                getFormattedStringOrDefault(R.string.file_properties_info_size_file)
            bindingContent.filePropertiesInfoDataSize.text = Util.getSizeString(node.size)
            bindingContent.filePropertiesContentLayout.isVisible = false
            if (node.creationTime != 0L) {
                try {
                    bindingContent.filePropertiesInfoDataAdded.text =
                        TimeUtils.formatLongDateTime(
                            node.creationTime
                        )
                } catch (ex: Exception) {
                    bindingContent.filePropertiesInfoDataAdded.text = ""
                }
                if (node.modificationTime != 0L) {
                    try {
                        bindingContent.filePropertiesInfoDataCreated.text =
                            TimeUtils.formatLongDateTime(
                                node.modificationTime
                            )
                    } catch (ex: Exception) {
                        bindingContent.filePropertiesInfoDataCreated.text = ""
                    }
                } else {
                    try {
                        bindingContent.filePropertiesInfoDataCreated.text =
                            TimeUtils.formatLongDateTime(
                                node.creationTime
                            )
                    } catch (ex: Exception) {
                        bindingContent.filePropertiesInfoDataCreated.text = ""
                    }
                }
            } else {
                bindingContent.filePropertiesInfoDataAdded.text = ""
                bindingContent.filePropertiesInfoDataCreated.text = ""
            }
            var thumb: Bitmap?
            var preview: Bitmap?
            thumb = ThumbnailUtils.getThumbnailFromCache(node)
            if (thumb != null) {
                binding.fileInfoToolbarImage.setImageBitmap(thumb)
                binding.fileInfoImageLayout.isVisible = true
                binding.fileInfoIconLayout.isVisible = false
            } else {
                thumb = ThumbnailUtils.getThumbnailFromFolder(node, this)
                if (thumb != null) {
                    binding.fileInfoToolbarImage.setImageBitmap(thumb)
                    binding.fileInfoImageLayout.isVisible = true
                    binding.fileInfoIconLayout.isVisible = false
                }
            }
            preview = PreviewUtils.getPreviewFromCache(node)
            if (preview != null) {
                PreviewUtils.previewCache.put(node.handle, preview)
                binding.fileInfoToolbarImage.setImageBitmap(preview)
                binding.fileInfoImageLayout.isVisible = true
                binding.fileInfoIconLayout.isVisible = false
            } else {
                preview = PreviewUtils.getPreviewFromFolder(node, this)
                if (preview != null) {
                    PreviewUtils.previewCache.put(node.handle, preview)
                    binding.fileInfoToolbarImage.setImageBitmap(preview)
                    binding.fileInfoImageLayout.isVisible = true
                    binding.fileInfoIconLayout.isVisible = false
                } else {
                    if (node.hasPreview()) {
                        val previewFile =
                            File(PreviewUtils.getPreviewFolder(this), node.base64Handle + ".jpg")
                        megaApi.getPreview(node, previewFile.absolutePath, megaRequestListener)
                    }
                }
            }
            // If the Node belongs to Backups or has no versions, then hide
            // the Versions layout
            if (isNodeInInbox(node) || !megaApi.hasVersions(node)) {
                bindingContent.filePropertiesVersionsLayout.isVisible = false
                bindingContent.separatorVersions.isVisible = false
            } else {
                bindingContent.filePropertiesVersionsLayout.isVisible = true
                val text = getQuantityStringOrDefault(
                    R.plurals.number_of_versions,
                    megaApi.getNumVersions(node),
                    megaApi.getNumVersions(node)
                )
                bindingContent.filePropertiesTextNumberVersions.text = text
                bindingContent.separatorVersions.isVisible = true
                nodeVersions = megaApi.getVersions(node)
            }
        } else if (node.isFolder) {
            Timber.d("Node is FOLDER")
            megaApi.getFolderInfo(node, megaRequestListener)
            bindingContent.filePropertiesInfoDataContent.isVisible = true
            bindingContent.filePropertiesInfoMenuContent.isVisible = true
            bindingContent.filePropertiesInfoDataContent.text =
                MegaApiUtils.getMegaNodeFolderInfo(node)
            val sizeFile = megaApi.getSize(node)
            bindingContent.filePropertiesInfoDataSize.text = Util.getSizeString(sizeFile)
            setIconResource()
            if (from == Constants.FROM_INCOMING_SHARES) {
                //Show who is the owner
                bindingContent.contactListThumbnail.setImageBitmap(null)
                val sharesIncoming = megaApi.inSharesList
                for (j in sharesIncoming.indices) {
                    val mS = sharesIncoming[j]
                    if (mS.nodeHandle == node.handle) {
                        val user = megaApi.getContact(mS.user)
                        contactMail = user?.email
                        if (user != null) {
                            val name = ContactUtil.getMegaUserNameDB(user) ?: user.email
                            bindingContent.filePropertiesOwnerLabel.text = name
                            bindingContent.filePropertiesOwnerInfo.text = user.email
                            setOwnerState(user.handle)
                            createDefaultAvatar(bindingContent.contactListThumbnail, user, name)
                        } else {
                            bindingContent.filePropertiesOwnerLabel.text = mS.user
                            bindingContent.filePropertiesOwnerInfo.text = mS.user
                            setOwnerState(-1)
                            createDefaultAvatar(bindingContent.contactListThumbnail, null, mS.user)
                        }
                        val avatar = buildAvatarFile(this, "$contactMail.jpg")
                        var bitmap: Bitmap?
                        if (FileUtil.isFileAvailable(avatar)) {
                            avatar?.takeIf { it.length() > 0 }?.let { avatarNoEmpty ->
                                val bOpts = BitmapFactory.Options()
                                bitmap = BitmapFactory.decodeFile(avatarNoEmpty.absolutePath, bOpts)
                                if (bitmap == null) {
                                    avatarNoEmpty.delete()
                                    megaApi.getUserAvatar(
                                        user,
                                        buildAvatarFile(this, "$contactMail.jpg")?.absolutePath,
                                        megaRequestListener
                                    )
                                } else {
                                    bindingContent.contactListThumbnail.setImageBitmap(bitmap)
                                }
                            } ?: run {
                                //avatar.length == 0
                                megaApi.getUserAvatar(
                                    user,
                                    buildAvatarFile(this, "$contactMail.jpg")?.absolutePath,
                                    megaRequestListener
                                )
                            }
                        } else {
                            megaApi.getUserAvatar(
                                user,
                                buildAvatarFile(this, "$contactMail.jpg")?.absolutePath,
                                megaRequestListener
                            )
                        }
                        bindingContent.filePropertiesOwnerLayout.isVisible = true
                    }
                }
            }
            sl = megaApi.getOutShares(node)
            sl?.let { sl ->
                if (sl.size == 0) {
                    bindingContent.filePropertiesSharedLayout.isVisible = false
                    bindingContent.dividerSharedLayout.isVisible = false
                    //If I am the owner
                    if (megaApi.checkAccessErrorExtended(
                            node,
                            MegaShare.ACCESS_OWNER
                        ).errorCode == MegaError.API_OK
                    ) {
                        binding.filePropertiesPermissionInfo.isVisible = false
                    } else {
                        owner = false
                        //If I am not the owner
                        binding.filePropertiesPermissionInfo.isVisible = true
                        val accessLevel = megaApi.getAccess(node)
                        Timber.d("Node: %s", node.handle)
                        when (accessLevel) {
                            MegaShare.ACCESS_OWNER, MegaShare.ACCESS_FULL -> {
                                binding.filePropertiesPermissionInfo.text =
                                    getFormattedStringOrDefault(R.string.file_properties_shared_folder_full_access)
                                        .uppercase(Locale.getDefault())
                            }
                            MegaShare.ACCESS_READ -> {
                                binding.filePropertiesPermissionInfo.text =
                                    getFormattedStringOrDefault(R.string.file_properties_shared_folder_read_only)
                                        .uppercase(Locale.getDefault())
                            }
                            MegaShare.ACCESS_READWRITE -> {
                                binding.filePropertiesPermissionInfo.text =
                                    getFormattedStringOrDefault(R.string.file_properties_shared_folder_read_write)
                                        .uppercase(Locale.getDefault())
                            }
                        }
                    }
                } else {
                    bindingContent.filePropertiesSharedLayout.isVisible = true
                    bindingContent.dividerSharedLayout.isVisible = true
                    bindingContent.filePropertiesSharedInfoButton.text =
                        getQuantityStringOrDefault(
                            R.plurals.general_selection_num_contacts,
                            sl.size, sl.size
                        )
                }
                if (node.creationTime != 0L) {
                    try {
                        bindingContent.filePropertiesInfoDataAdded.text =
                            DateUtils.getRelativeTimeSpanString(
                                node.creationTime * 1000
                            )
                    } catch (ex: Exception) {
                        bindingContent.filePropertiesInfoDataAdded.text = ""
                    }
                    if (node.modificationTime != 0L) {
                        try {
                            bindingContent.filePropertiesInfoDataCreated.text =
                                DateUtils.getRelativeTimeSpanString(
                                    node.modificationTime * 1000
                                )
                        } catch (ex: Exception) {
                            bindingContent.filePropertiesInfoDataCreated.text = ""
                        }
                    } else {
                        try {
                            bindingContent.filePropertiesInfoDataCreated.text =
                                DateUtils.getRelativeTimeSpanString(
                                    node.creationTime * 1000
                                )
                        } catch (ex: Exception) {
                            bindingContent.filePropertiesInfoDataCreated.text = ""
                        }
                    }
                } else {
                    bindingContent.filePropertiesInfoDataAdded.text = ""
                    bindingContent.filePropertiesInfoDataCreated.text = ""
                }
            } ?: run {
                bindingContent.filePropertiesSharedLayout.isVisible = false
                bindingContent.dividerSharedLayout.isVisible = false
            }
        }

        //Choose the button bindingContent.filePropertiesSwitch
        if (OfflineUtils.availableOffline(this, node)) {
            availableOfflineBoolean = true
            bindingContent.filePropertiesSwitch.isChecked = true
            return
        }
        availableOfflineBoolean = false
        bindingContent.filePropertiesSwitch.isChecked = false
    }

    private fun createDefaultAvatar(
        contactListThumbnail: ImageView,
        user: MegaUser?,
        name: String?,
    ) = contactListThumbnail.setImageBitmap(
        AvatarUtil.getDefaultAvatar(
            AvatarUtil.getColorAvatar(user),
            name,
            Constants.AVATAR_SIZE,
            true
        )
    )

    private fun sharedContactClicked() {
        val sharedContactLayout = bindingContent.sharedContactListContainer
        if (isShareContactExpanded) {
            sl?.let { sl ->
                bindingContent.filePropertiesSharedInfoButton.text =
                    getQuantityStringOrDefault(
                        R.plurals.general_selection_num_contacts,
                        sl.size, sl.size
                    )
            }
            sharedContactLayout.isVisible = false
        } else {
            bindingContent.filePropertiesSharedInfoButton.setText(R.string.general_close)
            sharedContactLayout.isVisible = true
        }
        isShareContactExpanded = !isShareContactExpanded
    }

    private fun filePropertiesSwitch() {
        val isChecked = bindingContent.filePropertiesSwitch.isChecked
        if (viewModel.getStorageState() === StorageState.PayWall) {
            showOverDiskQuotaPaywallWarning()
            bindingContent.filePropertiesSwitch.isChecked = !isChecked
            return
        }
        if (owner) {
            Timber.d("Owner: me")
            if (!isChecked) {
                Timber.d("isChecked")
                isRemoveOffline = true
                handle = node.handle
                availableOfflineBoolean = false
                bindingContent.filePropertiesSwitch.isChecked = false
                mOffDelete = dbH.findByHandle(handle)
                OfflineUtils.removeOffline(mOffDelete, dbH, this)
                showSnackbar(getFormattedStringOrDefault(R.string.file_removed_offline))
            } else {
                Timber.d("NOT Checked")
                isRemoveOffline = false
                handle = -1
                availableOfflineBoolean = true
                bindingContent.filePropertiesSwitch.isChecked = true
                val destination =
                    OfflineUtils.getOfflineParentFile(this, from, node, megaApi)
                Timber.d("Path destination: %s", destination)
                if (FileUtil.isFileAvailable(destination) && destination.isDirectory) {
                    val offlineFile = File(destination, node.name)
                    if (FileUtil.isFileAvailable(offlineFile) && node.size == offlineFile.length() && offlineFile.name == node.name) {
                        //This means that is already available offline
                        return
                    }
                }
                Timber.d("Handle to save for offline : ${node.handle}")
                OfflineUtils.saveOffline(destination, node, this)
            }
            invalidateOptionsMenu()
        } else {
            Timber.d("Not owner")
            if (!isChecked) {
                availableOfflineBoolean = false
                bindingContent.filePropertiesSwitch.isChecked = false
                mOffDelete = dbH.findByHandle(node.handle)
                OfflineUtils.removeOffline(mOffDelete, dbH, this)
                invalidateOptionsMenu()
            } else {
                availableOfflineBoolean = true
                bindingContent.filePropertiesSwitch.isChecked = true
                invalidateOptionsMenu()
                Timber.d("Checking the node%s", node.handle)

                //check the parent
                val result = OfflineUtils.findIncomingParentHandle(node, megaApi)
                Timber.d("IncomingParentHandle: %s", result)
                if (result != -1L) {
                    val destination = OfflineUtils.getOfflineParentFile(
                        this,
                        Constants.FROM_INCOMING_SHARES,
                        node,
                        megaApi
                    )
                    if (FileUtil.isFileAvailable(destination) && destination.isDirectory) {
                        val offlineFile = File(destination, node.name)
                        if (FileUtil.isFileAvailable(offlineFile) && node.size == offlineFile.length() && offlineFile.name == node.name) { //This means that is already available offline
                            return
                        }
                    }
                    OfflineUtils.saveOffline(destination, node, this)
                } else {
                    Timber.w("result=findIncomingParentHandle NOT result!")
                }
            }
        }
    }

    private fun showCopy() = startActivityForResult(
        Intent(this, FileExplorerActivity::class.java)
            .setAction(FileExplorerActivity.ACTION_PICK_COPY_FOLDER)
            .putExtra("COPY_FROM", LongArray(1).apply { this[0] = node.handle }),
        Constants.REQUEST_CODE_SELECT_FOLDER_TO_COPY
    )

    private fun showMove() = startActivityForResult(
        Intent(this, FileExplorerActivity::class.java)
            .setAction(FileExplorerActivity.ACTION_PICK_MOVE_FOLDER)
            .putExtra("MOVE_FROM", LongArray(1).apply { this[0] = node.handle }),
        Constants.REQUEST_CODE_SELECT_FOLDER_TO_MOVE
    )

    private fun moveToTrash() {
        Timber.d("moveToTrash")
        moveToRubbish = false
        if (!viewModel.isConnected) {
            Util.showErrorAlertDialog(
                getFormattedStringOrDefault(R.string.error_server_connection_problem),
                false,
                this
            )
            return
        }
        if (isFinishing) {
            return
        }

        moveToRubbish = nodeController.getParent(node)?.handle != megaApi.rubbishNode.handle

        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog).apply {
            val messageId = if (moveToRubbish) {
                when {
                    CameraUploadUtil.getPrimaryFolderHandle() == handle && CameraUploadUtil.isPrimaryEnabled() -> {
                        R.string.confirmation_move_cu_folder_to_rubbish
                    }
                    CameraUploadUtil.getSecondaryFolderHandle() == handle && CameraUploadUtil.isSecondaryEnabled() -> {
                        R.string.confirmation_move_mu_folder_to_rubbish
                    }
                    else -> {
                        R.string.confirmation_move_to_rubbish
                    }
                }
            } else {
                R.string.confirmation_delete_from_mega
            }

            setMessage(messageId)
            setPositiveButton(R.string.general_remove) { _: DialogInterface?, _: Int ->
                //Check if the node is not yet in the rubbish bin (if so, remove it)
                if (moveToRubbish) {
                    megaApi.moveNode(node, megaApi.rubbishNode, megaRequestListener)

                    try {
                        statusDialog = createProgressDialog(
                            this@FileInfoActivity,
                            getFormattedStringOrDefault(R.string.context_move_to_trash)
                        ).apply { show() }
                    } catch (e: Exception) {
                        Timber.w("Exception showing move to trash confirmation")
                    }
                } else {
                    megaApi.remove(node, megaRequestListener)

                    try {
                        statusDialog = createProgressDialog(
                            this@FileInfoActivity,
                            getFormattedStringOrDefault(R.string.context_delete_from_mega)
                        ).apply { show() }
                    } catch (e: Exception) {
                        Timber.w("Exception showing remove confirmation")
                    }
                }
            }
            setNegativeButton(R.string.general_cancel, null)
            show()
        }
    }

    @SuppressLint("NewApi")
    private fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError?) {
        if (adapter.isMultipleSelect) {
            adapter.clearSelections()
            hideMultipleSelect()
        }
        Timber.d("onRequestFinish: %d__%s", request.type, request.requestString)
        if (request.type == MegaRequest.TYPE_GET_ATTR_FILE) {
            if (e?.errorCode == MegaError.API_OK) {
                val previewDir = PreviewUtils.getPreviewFolder(this)
                val preview = File(previewDir, node.base64Handle + ".jpg")
                if (preview.exists()) {
                    if (preview.length() > 0) {
                        val bitmap = PreviewUtils.getBitmapForCache(preview, this)
                        PreviewUtils.previewCache.put(node.handle, bitmap)
                        binding.fileInfoToolbarImage.setImageBitmap(bitmap)
                        binding.fileInfoImageLayout.isVisible = true
                        binding.fileInfoIconLayout.isVisible = false
                    }
                }
            }
        } else if (request.type == MegaRequest.TYPE_FOLDER_INFO) {

            // If the Folder belongs to Backups, hide all Folder Version layouts
            if (isNodeInInbox(node)) {
                bindingContent.filePropertiesFolderVersionsLayout.isVisible = false
                bindingContent.filePropertiesFolderCurrentVersionsLayout.isVisible = false
                bindingContent.filePropertiesFolderPreviousVersionsLayout.isVisible = false
                return
            }
            if (e?.errorCode == MegaError.API_OK) {
                val info = request.megaFolderInfo
                val numVersions = info.numVersions
                Timber.d("Num versions: %s", numVersions)
                if (numVersions > 0) {
                    bindingContent.filePropertiesFolderVersionsLayout.isVisible = true
                    val text = getQuantityStringOrDefault(
                        R.plurals.number_of_versions_inside_folder,
                        numVersions,
                        numVersions
                    )
                    bindingContent.filePropertiesInfoDataFolderVersions.text = text
                    val currentVersions = info.currentSize
                    Timber.d("Current versions: %s", currentVersions)
                    if (currentVersions > 0) {
                        bindingContent.filePropertiesInfoDataFolderCurrentVersions.text =
                            Util.getSizeString(currentVersions)
                        bindingContent.filePropertiesFolderCurrentVersionsLayout.visibility =
                            View.VISIBLE
                    }
                } else {
                    bindingContent.filePropertiesFolderVersionsLayout.isVisible = false
                    bindingContent.filePropertiesFolderCurrentVersionsLayout.visibility =
                        View.GONE
                }
                val previousVersions = info.versionsSize
                Timber.d("Previous versions: %s", previousVersions)
                if (previousVersions > 0) {
                    bindingContent.filePropertiesInfoDataFolderPreviousVersions.text =
                        Util.getSizeString(previousVersions)
                    bindingContent.filePropertiesFolderPreviousVersionsLayout.visibility =
                        View.VISIBLE
                } else {
                    bindingContent.filePropertiesFolderPreviousVersionsLayout.visibility =
                        View.GONE
                }
            } else {
                bindingContent.filePropertiesFolderPreviousVersionsLayout.isVisible = false
                bindingContent.filePropertiesFolderVersionsLayout.isVisible = false
                bindingContent.filePropertiesFolderCurrentVersionsLayout.isVisible = false
            }
        } else if (request.type == MegaRequest.TYPE_MOVE) {
            try {
                statusDialog?.dismiss()
            } catch (ex: Exception) {
                Timber.d(ex.message)
            }
            if (moveToRubbish) {
                moveToRubbish = false
                Timber.d("Move to rubbish request finished")
            } else {
                Timber.d("Move nodes request finished")
            }
            if (e?.errorCode == MegaError.API_OK) {
                showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getFormattedStringOrDefault(R.string.context_correctly_moved),
                    -1
                )
                val intent = Intent(Constants.BROADCAST_ACTION_INTENT_FILTER_UPDATE_FULL_SCREEN)
                sendBroadcast(intent)
                finish()
            } else if (e?.errorCode == MegaError.API_EOVERQUOTA && api.isForeignNode(request.parentHandle)) {
                showForeignStorageOverQuotaWarningDialog(this)
            } else {
                showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getFormattedStringOrDefault(R.string.context_no_moved),
                    -1
                )
            }
        } else if (request.type == MegaRequest.TYPE_REMOVE) {
            if (versionsToRemove > 0) {
                Timber.d("Remove request finished")
                if (e?.errorCode == MegaError.API_OK) {
                    versionsRemoved++
                } else {
                    errorVersionRemove++
                }
                if (versionsRemoved + errorVersionRemove == versionsToRemove) {
                    if (versionsRemoved == versionsToRemove) {
                        showSnackbar(
                            Constants.SNACKBAR_TYPE,
                            getFormattedStringOrDefault(R.string.version_history_deleted),
                            -1
                        )
                    } else {
                        val firstLine = getFormattedStringOrDefault(
                            R.string.version_history_deleted_erroneously
                        )
                        val secondLine = getQuantityStringOrDefault(
                            R.plurals.versions_deleted_succesfully,
                            versionsRemoved,
                            versionsRemoved
                        )
                        val thirdLine = getQuantityStringOrDefault(
                            R.plurals.versions_not_deleted,
                            errorVersionRemove,
                            errorVersionRemove
                        )
                        showSnackbar(
                            Constants.SNACKBAR_TYPE,
                            "$firstLine\n$secondLine\n$thirdLine",
                            MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                        )
                    }
                    versionsToRemove = 0
                    versionsRemoved = 0
                    errorVersionRemove = 0
                }
            } else {
                Timber.d("Remove request finished")
                when (e?.errorCode) {
                    MegaError.API_OK -> {
                        finish()
                    }
                    MegaError.API_EMASTERONLY -> {
                        showSnackbar(Constants.SNACKBAR_TYPE, e.errorString, -1)
                    }
                    else -> {
                        showSnackbar(
                            Constants.SNACKBAR_TYPE,
                            getFormattedStringOrDefault(R.string.context_no_removed),
                            -1
                        )
                    }
                }
            }
        } else if (request.type == MegaApiJava.USER_ATTR_AVATAR) {
            try {
                statusDialog?.dismiss()
            } catch (ex: Exception) {
                Timber.e(ex)
            }
            if (e?.errorCode == MegaError.API_OK) {
                if (contactMail?.compareTo(request.email) == 0) {
                    val avatar = buildAvatarFile(this, "$contactMail.jpg")
                    if (FileUtil.isFileAvailable(avatar)) {
                        if ((avatar?.length() ?: 0) > 0) {
                            val bOpts = BitmapFactory.Options()
                            val bitmap = BitmapFactory.decodeFile(avatar?.absolutePath, bOpts)
                            if (bitmap == null) {
                                avatar?.delete()
                            } else {
                                bindingContent.contactListThumbnail.setImageBitmap(bitmap)
                                bindingContent.contactListThumbnail.isVisible = true
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Receive the result of requesting permissions
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        nodeSaver.handleRequestPermissionsResult(requestCode)
    }

    /**
     * Checks if there is a name collision before moving or copying the node.
     *
     * @param parentHandle Parent handle of the node in which the node will be moved or copied.
     * @param type         Type of name collision to check.
     */
    private fun checkCollision(parentHandle: Long, type: NameCollisionType) {
        checkNameCollisionUseCase.check(node.handle, parentHandle, type)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { collision: NameCollision ->
                    dismissAlertDialogIfExists(statusDialog)
                    val list = ArrayList<NameCollision>()
                    list.add(collision)
                    nameCollisionActivityContract?.launch(list)
                }
            ) { throwable: Throwable? ->
                dismissAlertDialogIfExists(statusDialog)
                if (throwable is ChildDoesNotExistsException) {
                    if (type === NameCollisionType.MOVE) {
                        move(parentHandle)
                    } else {
                        copy(parentHandle)
                    }
                } else {
                    showSnackbar(
                        Constants.SNACKBAR_TYPE,
                        getFormattedStringOrDefault(R.string.general_error),
                        MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                    )
                }
            }.also { composite.add(it) }
    }

    /**
     * Moves the node.
     *
     * @param parentHandle Parent handle in which the node will be moved.
     */
    private fun move(parentHandle: Long) {
        moveNodeUseCase.move(node.handle, parentHandle)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                dismissAlertDialogIfExists(statusDialog)
                showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getFormattedStringOrDefault(R.string.context_correctly_moved),
                    MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                )
                sendBroadcast(Intent(Constants.BROADCAST_ACTION_INTENT_FILTER_UPDATE_FULL_SCREEN))
                finish()
            }
            ) { throwable: Throwable? ->
                dismissAlertDialogIfExists(statusDialog)
                if (!manageCopyMoveException(throwable)) {
                    showSnackbar(
                        Constants.SNACKBAR_TYPE,
                        getFormattedStringOrDefault(R.string.context_no_moved),
                        MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                    )
                }
            }.also { composite.add(it) }
    }

    /**
     * Copies the node.
     *
     * @param parentHandle Parent handle in which the node will be copied.
     */
    private fun copy(parentHandle: Long) {
        copyNodeUseCase.copy(node.handle, parentHandle)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                dismissAlertDialogIfExists(statusDialog)
                showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getFormattedStringOrDefault(R.string.context_correctly_copied),
                    MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                )
            }
            ) { throwable: Throwable? ->
                dismissAlertDialogIfExists(statusDialog)
                if (!manageCopyMoveException(throwable)) {
                    showSnackbar(
                        Constants.SNACKBAR_TYPE,
                        getFormattedStringOrDefault(R.string.context_no_copied),
                        MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                    )
                }
            }.also { composite.add(it) }
    }

    /**
     * receive the result of the the activity launched by [startActivityForResult]
     * deprecated in favour of activity result API
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        Timber.d("onActivityResult %d____%d", requestCode, resultCode)
        if (nodeAttacher.handleActivityResult(requestCode, resultCode, intent, this)) {
            return
        }
        if (nodeSaver.handleActivityResult(this, requestCode, resultCode, intent)) {
            return
        }
        if (intent == null) {
            return
        }
        if (requestCode == Constants.REQUEST_CODE_SELECT_FOLDER_TO_MOVE && resultCode == RESULT_OK) {
            if (!viewModel.isConnected) {
                Util.showErrorAlertDialog(
                    getFormattedStringOrDefault(R.string.error_server_connection_problem),
                    false,
                    this
                )
                return
            }
            val toHandle = intent.getLongExtra("MOVE_TO", 0)
            moveToRubbish = false
            val temp: AlertDialog
            try {
                temp =
                    createProgressDialog(this, getFormattedStringOrDefault(R.string.context_moving))
                temp.show()
            } catch (e: Exception) {
                return
            }
            statusDialog = temp
            checkCollision(toHandle, NameCollisionType.MOVE)
        } else if (requestCode == Constants.REQUEST_CODE_SELECT_FOLDER_TO_COPY && resultCode == RESULT_OK) {
            if (!viewModel.isConnected) {
                Util.showErrorAlertDialog(
                    getFormattedStringOrDefault(R.string.error_server_connection_problem),
                    false,
                    this
                )
                return
            }
            val toHandle = intent.getLongExtra("COPY_TO", 0)
            val temp: AlertDialog
            try {
                temp = createProgressDialog(
                    this,
                    getFormattedStringOrDefault(R.string.context_copying)
                )
                temp.show()
            } catch (e: Exception) {
                return
            }
            statusDialog = temp
            checkCollision(toHandle, NameCollisionType.COPY)
        } else if (requestCode == REQUEST_CODE_SELECT_CONTACT && resultCode == RESULT_OK) {
            if (!viewModel.isConnected) {
                Util.showErrorAlertDialog(
                    getFormattedStringOrDefault(R.string.error_server_connection_problem),
                    false,
                    this
                )
                return
            }
            val contactsData = intent
                .getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS) ?: return
            val nodeHandle = intent.getLongExtra(AddContactActivity.EXTRA_NODE_HANDLE, -1)
            if (node.isFolder) {
                if (fileBackupManager?.shareFolder(
                        nodeController,
                        longArrayOf(nodeHandle),
                        contactsData,
                        MegaShare.ACCESS_READ
                    ) == true
                ) {
                    return
                }

                val items = arrayOf<CharSequence>(
                    getFormattedStringOrDefault(R.string.file_properties_shared_folder_read_only),
                    getFormattedStringOrDefault(R.string.file_properties_shared_folder_read_write),
                    getFormattedStringOrDefault(R.string.file_properties_shared_folder_full_access)
                )
                permissionsDialog =
                    MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
                        .setTitle(getFormattedStringOrDefault(R.string.file_properties_shared_folder_permissions))
                        .setSingleChoiceItems(items, -1) { _, item ->
                            statusDialog = createProgressDialog(
                                this,
                                getFormattedStringOrDefault(R.string.context_sharing_folder)
                            )
                            permissionsDialog?.dismiss()
                            nodeController.shareFolder(node, contactsData, item)
                        }.show()
            } else {
                Timber.w("ERROR, the file is not folder")
            }
        } else if (requestCode == Constants.REQUEST_CODE_DELETE_VERSIONS_HISTORY && resultCode == RESULT_OK) {
            if (!viewModel.isConnected) {
                Util.showErrorAlertDialog(
                    getFormattedStringOrDefault(R.string.error_server_connection_problem),
                    false,
                    this
                )
                return
            }
            if (intent.getBooleanExtra("deleteVersionHistory", false)) {
                val versions = megaApi.getVersions(node)
                versionsToRemove = versions.size - 1
                for (i in 1 until versions.size) {
                    megaApi.removeVersion(versions[i], megaRequestListener)
                }
            }
        }
    }

    private fun onNodesUpdate(nodes: ArrayList<MegaNode>?) {
        Timber.d("onNodesUpdate")
        var thisNode = false
        var anyChild = false
        var updateContentFolder = false
        if (nodes == null) {
            return
        }
        var n: MegaNode? = null
        for (nodeToCheck in nodes) {
            if (nodeToCheck.handle == node.handle) {
                thisNode = true
                n = nodeToCheck
                break
            } else {
                if (node.isFolder) {
                    var parent = megaApi.getNodeByHandle(nodeToCheck.parentHandle)
                    while (parent != null) {
                        if (parent.handle == node.handle) {
                            updateContentFolder = true
                            break
                        }
                        parent = megaApi.getNodeByHandle(parent.parentHandle)
                    }
                } else {
                    nodeVersions?.let { nodeVersions ->
                        for (j in nodeVersions.indices) {
                            if (nodeToCheck.handle == nodeVersions[j].handle) {
                                if (!anyChild) {
                                    anyChild = true
                                    break
                                }
                            }
                        }
                    }
                }
            }
        }
        if (updateContentFolder) {
            megaApi.getFolderInfo(node, megaRequestListener)
        }
        if (!thisNode && !anyChild) {
            Timber.d("Not related to this node")
            return
        }

        //Check if the parent handle has changed
        if (n != null) {
            if (n.hasChanged(MegaNode.CHANGE_TYPE_PARENT)) {
                val oldParent = megaApi.getParentNode(node)
                val newParent = megaApi.getParentNode(n)
                node = if (oldParent.handle == newParent.handle) {
                    Timber.d("Parents match")
                    if (newParent.isFile) {
                        Timber.d("New version added")
                        newParent
                    } else {
                        n
                    }
                } else {
                    n
                }
                nodeVersions = if (megaApi.hasVersions(node)) {
                    megaApi.getVersions(node)
                } else {
                    null
                }
            } else if (n.hasChanged(MegaNode.CHANGE_TYPE_REMOVED)) {
                if (thisNode) {
                    if (nodeVersions != null) {
                        val nodeHandle = nodeVersions?.get(1)?.handle ?: -1
                        if (megaApi.getNodeByHandle(nodeHandle) != null) {
                            node = megaApi.getNodeByHandle(nodeHandle)
                            nodeVersions = if (megaApi.hasVersions(node)) {
                                megaApi.getVersions(node)
                            } else {
                                null
                            }
                        } else {
                            finish()
                        }
                    } else {
                        finish()
                    }
                } else if (anyChild) {
                    nodeVersions = if (megaApi.hasVersions(n)) {
                        megaApi.getVersions(n)
                    } else {
                        null
                    }
                }
            } else {
                node = n
                nodeVersions = if (megaApi.hasVersions(node)) {
                    megaApi.getVersions(node)
                } else {
                    null
                }
            }
        } else {
            if (anyChild) {
                nodeVersions = if (megaApi.hasVersions(node)) {
                    megaApi.getVersions(node)
                } else {
                    null
                }
            }
        }
        if (moveToRubbish) {
            invalidateOptionsMenu()
        }
        if (!node.isTakenDown && node.isExported) {
            Timber.d("Node HAS public link")
            publicLink = true
            bindingContent.dividerLinkLayout.isVisible = true
            bindingContent.filePropertiesLinkLayout.isVisible = true
            bindingContent.filePropertiesCopyLayout.isVisible = true
            bindingContent.filePropertiesLinkText.text = node.publicLink
            invalidateOptionsMenu()
        } else {
            Timber.d("Node NOT public link")
            publicLink = false
            bindingContent.dividerLinkLayout.isVisible = false
            bindingContent.filePropertiesLinkLayout.isVisible = false
            bindingContent.filePropertiesCopyLayout.isVisible = false
            invalidateOptionsMenu()
        }
        if (node.isFolder) {
            val sizeFile = megaApi.getSize(node)
            bindingContent.filePropertiesInfoDataSize.text = Util.getSizeString(sizeFile)
            bindingContent.filePropertiesInfoDataContent.text =
                MegaApiUtils.getMegaNodeFolderInfo(node)
            setIconResource()
            sl = megaApi.getOutShares(node)
            sl?.let { sl ->
                if (sl.size == 0) {
                    Timber.d("sl.size == 0")
                    bindingContent.filePropertiesSharedLayout.isVisible = false
                    bindingContent.dividerSharedLayout.isVisible = false

                    //If I am the owner
                    if (megaApi.checkAccessErrorExtended(
                            node,
                            MegaShare.ACCESS_OWNER
                        ).errorCode == MegaError.API_OK
                    ) {
                        binding.filePropertiesPermissionInfo.isVisible = false
                    } else {

                        //If I am not the owner
                        owner = false
                        binding.filePropertiesPermissionInfo.isVisible = true
                        val accessLevel = megaApi.getAccess(node)
                        Timber.d("Node: %s", node.handle)
                        when (accessLevel) {
                            MegaShare.ACCESS_OWNER, MegaShare.ACCESS_FULL -> {
                                binding.filePropertiesPermissionInfo.text =
                                    getFormattedStringOrDefault(R.string.file_properties_shared_folder_full_access)
                                        .uppercase(Locale.getDefault())
                            }
                            MegaShare.ACCESS_READ -> {
                                binding.filePropertiesPermissionInfo.text =
                                    getFormattedStringOrDefault(R.string.file_properties_shared_folder_read_only)
                                        .uppercase(Locale.getDefault())
                            }
                            MegaShare.ACCESS_READWRITE -> {
                                binding.filePropertiesPermissionInfo.text =
                                    getFormattedStringOrDefault(R.string.file_properties_shared_folder_read_write)
                                        .uppercase(Locale.getDefault())
                            }
                        }
                    }
                } else {
                    bindingContent.filePropertiesSharedLayout.isVisible = true
                    bindingContent.dividerSharedLayout.isVisible = true
                    bindingContent.filePropertiesSharedInfoButton.text =
                        getQuantityStringOrDefault(
                            R.plurals.general_selection_num_contacts,
                            sl.size, sl.size
                        )
                }
            }
        } else {
            bindingContent.filePropertiesInfoDataSize.text = Util.getSizeString(node.size)
        }
        if (node.creationTime != 0L) {
            try {
                bindingContent.filePropertiesInfoDataAdded.text =
                    DateUtils.getRelativeTimeSpanString(node.creationTime * 1000)
            } catch (ex: Exception) {
                bindingContent.filePropertiesInfoDataAdded.text = ""
            }
            if (node.modificationTime != 0L) {
                try {
                    bindingContent.filePropertiesInfoDataCreated.text =
                        DateUtils.getRelativeTimeSpanString(
                            node.modificationTime * 1000
                        )
                } catch (ex: Exception) {
                    bindingContent.filePropertiesInfoDataCreated.text = ""
                }
            } else {
                try {
                    bindingContent.filePropertiesInfoDataCreated.text =
                        DateUtils.getRelativeTimeSpanString(
                            node.creationTime * 1000
                        )
                } catch (ex: Exception) {
                    bindingContent.filePropertiesInfoDataCreated.text = ""
                }
            }
        } else {
            bindingContent.filePropertiesInfoDataAdded.text = ""
            bindingContent.filePropertiesInfoDataCreated.text = ""
        }

        // If the Node belongs to Backups or has no versions, then hide
        // the Versions layout
        if (isNodeInInbox(node) || !megaApi.hasVersions(node)) {
            bindingContent.filePropertiesVersionsLayout.isVisible = false
            bindingContent.separatorVersions.isVisible = false
        } else {
            bindingContent.filePropertiesVersionsLayout.isVisible = true
            val text = getQuantityStringOrDefault(
                R.plurals.number_of_versions,
                megaApi.getNumVersions(node),
                megaApi.getNumVersions(node)
            )
            bindingContent.filePropertiesTextNumberVersions.text = text
            bindingContent.separatorVersions.isVisible = true
        }
        refresh()
    }


    /**
     * Perform final cleaning when the activity is destroyed
     */
    override fun onDestroy() {
        super.onDestroy()
        megaApi.removeGlobalListener(megaGlobalListener)
        megaApi.removeRequestListener(megaRequestListener)
        if (upArrow != null) upArrow?.colorFilter = null
        if (drawableRemoveLink != null) drawableRemoveLink?.colorFilter = null
        if (drawableLink != null) drawableLink?.colorFilter = null
        if (drawableShare != null) drawableShare?.colorFilter = null
        if (drawableDots != null) drawableDots?.colorFilter = null
        if (drawableDownload != null) drawableDownload?.colorFilter = null
        if (drawableLeave != null) drawableLeave?.colorFilter = null
        if (drawableCopy != null) drawableCopy?.colorFilter = null
        if (drawableChat != null) drawableChat?.colorFilter = null
        unregisterReceiver(contactUpdateReceiver)
        unregisterReceiver(manageShareReceiver)
        nodeSaver.destroy()
    }


    /**
     * Called to retrieve per-instance state from an activity before being killed so that the state can be restored
     */
    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (selectedShare != null) {
            outState.putLong(KEY_SELECTED_SHARE_HANDLE, selectedShare?.nodeHandle ?: -1)
        }
        nodeAttacher.saveState(outState)
        nodeSaver.saveState(outState)
    }

    /**
     * receive the click of an item from the adapter
     */
    fun itemClick(position: Int) {
        Timber.d("Position: %s", position)
        if (adapter.isMultipleSelect) {
            adapter.toggleSelection(position)
            updateActionModeTitle()
        } else {
            val megaUser = listContacts[position].user
            val contact = megaApi.getContact(megaUser)
            if (contact != null && contact.visibility == MegaUser.VISIBILITY_VISIBLE) {
                ContactUtil.openContactInfoActivity(this, megaUser)
            }
        }
    }

    /**
     * receive the show options panel action from the adapter
     */
    fun showOptionsPanel(sShare: MegaShare?) {
        Timber.d("showNodeOptionsPanel")
        if (sShare == null || bottomSheetDialogFragment.isBottomSheetDialogShown()) return
        selectedShare = sShare
        bottomSheetDialogFragment =
            FileContactsListBottomSheetDialogFragment(selectedShare, selectedContact, node)
        bottomSheetDialogFragment?.show(supportFragmentManager, bottomSheetDialogFragment?.tag)
    }

    /**
     * hides the multi select option
     */
    fun hideMultipleSelect() {
        adapter.isMultipleSelect = false
        actionMode?.finish()
    }

    private val selectedContact: MegaUser?
        get() {
            val email = selectedShare?.user
            return megaApi.getContact(email)
        }

    /**
     * Receive the change permissions action to show the different permission options
     */
    fun changePermissions() {
        Timber.d("changePermissions")
        val dialogBuilder =
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
        dialogBuilder.setTitle(getFormattedStringOrDefault(R.string.file_properties_shared_folder_permissions))
        val items = arrayOf<CharSequence>(
            getFormattedStringOrDefault(R.string.file_properties_shared_folder_read_only),
            getFormattedStringOrDefault(R.string.file_properties_shared_folder_read_write),
            getFormattedStringOrDefault(R.string.file_properties_shared_folder_full_access)
        )
        val selected = selectedShare ?: return
        dialogBuilder.setSingleChoiceItems(items, selected.access) { _, item ->
            statusDialog = createProgressDialog(
                this,
                getFormattedStringOrDefault(R.string.context_permissions_changing_folder)
            )
            permissionsDialog?.dismiss()
            cC?.changePermission(
                selected.user,
                item,
                node,
                ShareListener(this, ShareListener.CHANGE_PERMISSIONS_LISTENER, 1)
            )
        }
        permissionsDialog = dialogBuilder.show()
    }

    /**
     * Receive remove contact action to show a confirmation dialog
     */
    fun removeFileContactShare(): AlertDialog =
        showConfirmationRemoveContactFromShare(selectedShare?.user)

    private fun showConfirmationRemoveContactFromShare(email: String?) =
        MaterialAlertDialogBuilder(this)
            .setMessage(getFormattedStringOrDefault(R.string.remove_contact_shared_folder, email))
            .setPositiveButton(R.string.general_remove) { _: DialogInterface?, _: Int ->
                removeShare(email)
            }
            .setNegativeButton(R.string.general_cancel, null)
            .show()

    private fun removeShare(email: String?) {
        statusDialog = createProgressDialog(
            this,
            getFormattedStringOrDefault(R.string.context_removing_contact_folder)
        )
        nodeController.removeShare(
            ShareListener(
                this,
                ShareListener.REMOVE_SHARE_LISTENER,
                1
            ), node, email
        )
    }

    private fun refresh() {
        setContactList()
        setMoreButtonText()
        adapter.setShareList(listContacts)
    }

    private fun setContactList() {
        fullListContacts = megaApi.getOutShares(node)
        listContacts = fullListContacts.take(MAX_NUMBER_OF_CONTACTS_IN_LIST)
    }

    @SuppressLint("SetTextI18n")
    private fun setMoreButtonText() {
        val fullSize = fullListContacts.size
        with(bindingContent.moreButton) {
            if (fullSize > MAX_NUMBER_OF_CONTACTS_IN_LIST) {
                isVisible = true
                text =
                    "${(fullSize - MAX_NUMBER_OF_CONTACTS_IN_LIST)} ${getFormattedStringOrDefault(R.string.label_more)}"
            } else {
                isVisible = false
            }
        }

    }

    private fun showConfirmationRemoveMultipleContactFromShare(contacts: ArrayList<MegaShare>) =
        MaterialAlertDialogBuilder(this)
            .setMessage(
                getQuantityStringOrDefault(
                    R.plurals.remove_multiple_contacts_shared_folder,
                    contacts.size,
                    contacts.size
                )
            )
            .setPositiveButton(getFormattedStringOrDefault(R.string.general_remove)) { _: DialogInterface?, _: Int ->
                removeMultipleShares(contacts)
            }
            .setNegativeButton(R.string.general_cancel, null)
            .show()

    private fun removeMultipleShares(shares: ArrayList<MegaShare>?) {
        Timber.d("removeMultipleShares")
        statusDialog = createProgressDialog(
            this,
            getFormattedStringOrDefault(R.string.context_removing_contact_folder)
        )
        nodeController.removeShares(shares, node)
    }

    // Clear all selected items
    private fun clearSelections() {
        if (adapter.isMultipleSelect) {
            adapter.clearSelections()
        }
    }

    private fun selectAll() {
        Timber.d("selectAll")
        if (adapter.isMultipleSelect) {
            adapter.selectAll()
        } else {
            adapter.isMultipleSelect = true
            adapter.selectAll()
            actionMode = startSupportActionMode(ActionBarCallBack())
        }
        Handler(Looper.getMainLooper()).post { updateActionModeTitle() }
    }

    private fun updateAdapter(handleReceived: Long) {
        for (i in listContacts.indices) {
            val email = listContacts[i].user
            val contact = megaApi.getContact(email)
            val handleUser = contact.handle
            if (handleUser == handleReceived) {
                adapter.notifyItemChanged(i)
                break
            }
        }
    }

    private fun updateActionModeTitle() {
        Timber.d("updateActionModeTitle")
        if (actionMode == null) {
            return
        }
        val contacts: List<MegaShare> = adapter.selectedShares ?: emptyList()
        Timber.d("Contacts selected: %s", contacts.size)
        actionMode?.title = getQuantityStringOrDefault(
            R.plurals.general_selection_num_contacts,
            contacts.size, contacts.size
        )
        try {
            actionMode?.invalidate()
        } catch (e: NullPointerException) {
            Timber.e(e, "Invalidate error")
        }
    }

    private fun setIconResource() {
        val resource = if (node.isFolder) {
            getFolderIcon(
                node,
                if (adapterType == Constants.OUTGOING_SHARES_ADAPTER) DrawerItem.SHARED_ITEMS else DrawerItem.CLOUD_DRIVE
            )
        } else {
            MimeTypeThumbnail.typeForName(node.name).iconResourceId
        }
        binding.fileInfoToolbarIcon.setImageResource(resource)
    }

    companion object {
        private const val MAX_NUMBER_OF_CONTACTS_IN_LIST = 5
        private const val MAX_WIDTH_FILENAME_LAND = 400f
        private const val MAX_WIDTH_FILENAME_LAND_2 = 400f
        private const val MAX_WIDTH_FILENAME_PORT = 170f
        private const val MAX_WIDTH_FILENAME_PORT_2 = 200f

        /**
         * key to return the handle to the calling activity
         */
        const val NODE_HANDLE = "NODE_HANDLE"

        /**
         * remove type
         */
        @JvmField
        var TYPE_EXPORT_REMOVE = 1
        private const val KEY_SELECTED_SHARE_HANDLE = "KEY_SELECTED_SHARE_HANDLE"
        private const val REQUEST_CODE_SELECT_CONTACT = 1000
    }
}