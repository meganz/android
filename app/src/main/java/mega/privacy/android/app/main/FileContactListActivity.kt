package mega.privacy.android.app.main

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_CREDENTIALS
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_FIRST_NAME
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_LAST_NAME
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_NICKNAME
import mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE
import mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_MANAGE_SHARE
import mega.privacy.android.app.constants.BroadcastConstants.EXTRA_USER_HANDLE
import mega.privacy.android.app.interfaces.ActionBackupListener
import mega.privacy.android.app.listeners.ShareListener
import mega.privacy.android.app.main.adapters.MegaSharedFolderAdapter
import mega.privacy.android.app.main.controllers.ContactController
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.app.modalbottomsheet.FileContactsListBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.FileContactsListBottomSheetDialogListener
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.presentation.contact.FileContactListViewModel
import mega.privacy.android.app.presentation.contact.shareFolder
import mega.privacy.android.app.sync.fileBackups.FileBackupManager
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.OperationType.OPERATION_EXECUTE
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_BACKUP_SHARE_FOLDER
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_NONE
import mega.privacy.android.app.utils.MegaNodeUtil.checkBackupNodeTypeByHandle
import mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.node.MoveRequestResult
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaEvent
import nz.mega.sdk.MegaGlobalListenerInterface
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaSet
import nz.mega.sdk.MegaSetElement
import nz.mega.sdk.MegaShare
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert
import timber.log.Timber
import java.util.Stack

@AndroidEntryPoint
internal class FileContactListActivity : PasscodeActivity(), View.OnClickListener,
    MegaGlobalListenerInterface, FileContactsListBottomSheetDialogListener {

    internal val viewModel by viewModels<FileContactListViewModel>()

    private lateinit var contactController: ContactController
    internal lateinit var nodeController: NodeController

    private var selectedShare: MegaShare? = null

    private var listView: RecyclerView? = null
    private var mLayoutManager: LinearLayoutManager? = null
    private var emptyImage: ImageView? = null
    private var emptyText: TextView? = null
    private var warningText: TextView? = null
    private var fab: FloatingActionButton? = null

    private var listContacts: ArrayList<MegaShare>? = null
    private var tempListContacts: List<MegaShare>? = null


    private var nodeHandle: Long = 0
    private var node: MegaNode? = null
    private var contactNodes: ArrayList<MegaNode>? = null

    private var adapter: MegaSharedFolderAdapter? = null

    private var parentHandle: Long = -1

    private var parentHandleStack: Stack<Long> = Stack()

    private var actionMode: ActionMode? = null

    private var statusDialog: AlertDialog? = null
    private var permissionsDialog: AlertDialog? = null

    private var handler: Handler? = null

    private var bottomSheetDialogFragment: FileContactsListBottomSheetDialogFragment? = null

    private var fileBackupManager: FileBackupManager? = null

    private val manageShareReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            if (adapter != null) {
                if (adapter?.isMultipleSelect == true) {
                    adapter?.clearSelections()
                    hideMultipleSelect()
                }
                adapter?.setShareList(listContacts)
            }

            permissionsDialog?.dismiss()
            statusDialog?.dismiss()
        }
    }

    private val contactUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == null) return

            if (intent.action == ACTION_UPDATE_NICKNAME || intent.action == ACTION_UPDATE_CREDENTIALS || intent.action == ACTION_UPDATE_FIRST_NAME || intent.action == ACTION_UPDATE_LAST_NAME) {
                updateAdapter(intent.getLongExtra(EXTRA_USER_HANDLE, MegaApiJava.INVALID_HANDLE))
            }
        }
    }

    fun activateActionMode() {
        Timber.d("activateActionMode")
        if (adapter?.isMultipleSelect == false) {
            adapter?.isMultipleSelect = true
            actionMode = startSupportActionMode(ActionBarCallBack())
        }
    }

    override fun onGlobalSyncStateChanged(api: MegaApiJava) {}
    private inner class ActionBarCallBack : ActionMode.Callback {
        @SuppressLint("NonConstantResourceId")
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            Timber.d("onActionItemClicked")
            val shares = adapter?.selectedShares

            val itemId = item.itemId
            when (itemId) {
                R.id.action_file_contact_list_permissions -> { //Change permissions
                    val dialogBuilder = MaterialAlertDialogBuilder(
                        this@FileContactListActivity, R.style.ThemeOverlay_Mega_MaterialAlertDialog
                    )
                    dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions))

                    val items = arrayOf<CharSequence>(
                        getString(R.string.file_properties_shared_folder_read_only), getString(
                            R.string.file_properties_shared_folder_read_write
                        ), getString(R.string.file_properties_shared_folder_full_access)
                    )
                    dialogBuilder.setSingleChoiceItems(
                        items, -1
                    ) { _: DialogInterface?, item1: Int ->
                        clearSelections()
                        permissionsDialog?.dismiss()
                        statusDialog = createProgressDialog(
                            this@FileContactListActivity,
                            getString(R.string.context_permissions_changing_folder)
                        )
                        contactController.changePermissions(
                            contactController.getEmailShares(shares), item1, node
                        )
                    }

                    permissionsDialog = dialogBuilder.create().also {
                        it.show()
                    }

                }

                R.id.action_file_contact_list_delete -> {
                    shares?.takeUnless { it.isEmpty() }?.let {
                        if (it.size > 1) {
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
            val inflater = mode.menuInflater
            inflater.inflate(R.menu.file_contact_shared_browser_action, menu)
            fab?.visibility = View.GONE
            checkScroll()
            return true
        }

        override fun onDestroyActionMode(arg0: ActionMode) {
            Timber.d("onDestroyActionMode")
            adapter?.clearSelections()
            adapter?.isMultipleSelect = false
            fab?.visibility = View.VISIBLE
            checkScroll()
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            Timber.d("onPrepareActionMode")
            val selected = adapter?.selectedShares
            var deleteShare = false
            var permissions = false

            if (selected?.size != 0) {
                permissions = true
                deleteShare = true

                val unselect = menu.findItem(R.id.cab_menu_unselect_all)
                if (selected?.size == adapter?.itemCount) {
                    menu.findItem(R.id.cab_menu_select_all).setVisible(false)
                    unselect.setTitle(getString(R.string.action_unselect_all))
                    unselect.setVisible(true)
                } else {
                    menu.findItem(R.id.cab_menu_select_all).setVisible(true)
                    unselect.setTitle(getString(R.string.action_unselect_all))
                    unselect.setVisible(true)
                }
            } else {
                menu.findItem(R.id.cab_menu_select_all).setVisible(true)
                menu.findItem(R.id.cab_menu_unselect_all).setVisible(false)
            }

            val changePermissionsMenuItem = menu.findItem(R.id.action_file_contact_list_permissions)
            if (node != null && megaApi.isInVault(node)) {
                // If the node came from Backups, hide the Change Permissions option from the Action Bar
                changePermissionsMenuItem.setVisible(false)
                changePermissionsMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            } else {
                // Otherwise, change Change Permissions visibility depending on whether there are
                // selected contacts or none
                changePermissionsMenuItem.setVisible(permissions)
                if (permissions) {
                    changePermissionsMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                } else {
                    changePermissionsMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                }
            }

            menu.findItem(R.id.action_file_contact_list_delete).setVisible(deleteShare)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        this.enableEdgeToEdge()
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)

        initFileBackupManager()

        if (shouldRefreshSessionDueToSDK() || shouldRefreshSessionDueToKarere()) {
            return
        }

        contactController = ContactController(this)
        nodeController = NodeController(this)

        megaApi.addGlobalListener(this)

        handler = Handler()

        listContacts = ArrayList()

        val extras = intent.extras
        if (extras != null) {
            nodeHandle = extras.getLong(Constants.NAME)
            node = megaApi.getNodeByHandle(nodeHandle)

            setContentView(R.layout.activity_file_contact_list)

            //Set toolbar
            val tB = findViewById<Toolbar>(R.id.toolbar_file_contact_list)
            setSupportActionBar(tB)
            supportActionBar?.let {
                it.setDisplayHomeAsUpEnabled(true)
                it.setDisplayShowHomeEnabled(true)
                it.title = node?.name
                it.setSubtitle(R.string.file_properties_shared_folder_select_contact)
            }

            fab = findViewById(R.id.floating_button_file_contact_list)
            fab?.setOnClickListener(this)

            warningText = findViewById(R.id.file_contact_list_text_warning_message)

            listView = findViewById(R.id.file_contact_list_view_browser)
            listView?.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics))
            listView?.setClipToPadding(false)
            listView?.addItemDecoration(SimpleDividerItemDecoration(this))
            mLayoutManager = LinearLayoutManager(this)
            listView?.setLayoutManager(mLayoutManager)
            listView?.setItemAnimator(Util.noChangeRecyclerViewItemAnimator())
            listView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    checkScroll()
                }
            })


            emptyImage = findViewById(R.id.file_contact_list_empty_image)
            emptyText = findViewById(R.id.file_contact_list_empty_text)
            emptyImage?.setImageResource(R.drawable.ic_empty_contacts)
            emptyText?.setText(R.string.contacts_list_empty_text)
        }


        viewModel.getMegaShares(node)

        registerSdkAppropriateReceiver(
            broadcastReceiver = manageShareReceiver,
            filter = IntentFilter(BROADCAST_ACTION_INTENT_MANAGE_SHARE)
        )

        val contactUpdateFilter = IntentFilter(BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE)
        contactUpdateFilter.addAction(ACTION_UPDATE_NICKNAME)
        contactUpdateFilter.addAction(ACTION_UPDATE_FIRST_NAME)
        contactUpdateFilter.addAction(ACTION_UPDATE_LAST_NAME)
        contactUpdateFilter.addAction(ACTION_UPDATE_CREDENTIALS)
        registerSdkAppropriateReceiver(
            broadcastReceiver = contactUpdateReceiver, filter = contactUpdateFilter
        )
        collectFlows()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerSdkAppropriateReceiver(
        broadcastReceiver: BroadcastReceiver,
        filter: IntentFilter,
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.registerReceiver(
                    this, broadcastReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
                )
            } else {
                super.registerReceiver(broadcastReceiver, filter)
            }
        } catch (e: IllegalStateException) {
            Timber.e(e, "IllegalStateException registering receiver")
        }
    }

    private fun collectFlows() {
        this.collectFlow(
            viewModel.showNotVerifiedContactBanner, Lifecycle.State.STARTED
        ) { showBanner: Boolean ->
            warningText?.visibility = if (showBanner) View.VISIBLE else View.GONE
        }
        this.collectFlow(
            viewModel.megaShare, Lifecycle.State.STARTED
        ) { shares: List<MegaShare>? ->
            tempListContacts = shares
            updateListView()
        }
    }

    /**
     * Initializes the FileBackupManager
     */
    private fun initFileBackupManager() {
        fileBackupManager = FileBackupManager(this, object : ActionBackupListener {
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

    fun checkScroll() {
        listView?.let {
            Util.changeViewElevation(
                supportActionBar,
                (it.canScrollVertically(-1) && it.visibility == View.VISIBLE) || (adapter != null && adapter?.isMultipleSelect == true),
                outMetrics
            )
        }
    }


    fun showOptionsPanel(sShare: MegaShare?) {
        Timber.d("showNodeOptionsPanel")

        if (node == null || sShare == null || bottomSheetDialogFragment.isBottomSheetDialogShown()) return

        selectedShare = sShare
        bottomSheetDialogFragment = FileContactsListBottomSheetDialogFragment(
            selectedShare, getSelectedContact(), node, this
        )
        bottomSheetDialogFragment?.show(supportFragmentManager, bottomSheetDialogFragment?.tag)
    }

    override fun onDestroy() {
        super.onDestroy()

        megaApi.removeGlobalListener(this)
        handler?.removeCallbacksAndMessages(null)

        unregisterReceiver(manageShareReceiver)
        unregisterReceiver(contactUpdateReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu items for use in the action bar

        val inflater = menuInflater
        inflater.inflate(R.menu.activity_folder_contact_list, menu)

        menu.findItem(R.id.action_delete_version_history).setVisible(false)

        val selectMenuItem = menu.findItem(R.id.action_select)
        val unSelectMenuItem = menu.findItem(R.id.action_unselect)

        selectMenuItem.setVisible(true)
        unSelectMenuItem.setVisible(false)

        val addSharingContact = menu.findItem(R.id.action_folder_contacts_list_share_folder)
        addSharingContact.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        return super.onCreateOptionsMenu(menu)
    }

    @SuppressLint("NonConstantResourceId")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle presses on the action bar items
        val itemId = item.itemId
        when (itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }

            R.id.action_select -> {
                selectAll()
                return true
            }

            R.id.action_folder_contacts_list_share_folder -> {
                handleShareFolder()
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    /**
     * Handle the process of sharing the Folder to contacts
     *
     *
     * If the Folder is a Backup folder, a warning dialog is displayed and the shared folder can only be
     * configured in read-only mode.
     *
     *
     * Otherwise, no warning dialog is displayed and the shared folder can be configured in different
     * access modes (read-only, read and write, full access)
     */
    private fun handleShareFolder() {

        val nodeType = checkBackupNodeTypeByHandle(megaApi, node)
        if (nodeType != BACKUP_NONE) {
            node?.let { megaNode ->
                fileBackupManager?.let {
                    fileBackupManager?.shareBackupsFolder(
                        nodeController, megaNode, nodeType, it.defaultActionBackupNodeCallback
                    )
                }
            }
        } else {
            shareFolder()
        }
    }

    /**
     * Starts a new Intent to share the folder to different contacts
     */
    private fun shareFolder() {
        val intent = Intent()
        intent.setClass(this, AddContactActivity::class.java)
        intent.putExtra("contactType", Constants.CONTACT_TYPE_BOTH)
        intent.putExtra("MULTISELECT", 0)
        intent.putExtra(AddContactActivity.EXTRA_NODE_HANDLE, node?.handle)
        startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_CONTACT)
    }

    // Clear all selected items
    private fun clearSelections() {
        if (adapter?.isMultipleSelect == true) {
            adapter?.clearSelections()
        }
    }

    fun selectAll() {
        Timber.d("selectAll")
        if (adapter != null) {
            if (adapter?.isMultipleSelect == true) {
                adapter?.selectAll()
            } else {
                adapter?.isMultipleSelect = true
                adapter?.selectAll()

                actionMode = startSupportActionMode(ActionBarCallBack())
            }
            Handler(Looper.getMainLooper()).post { updateActionModeTitle() }
        }
    }

    fun itemClick(position: Int) {
        Timber.d("Position: %s", position)

        if (adapter?.isMultipleSelect == true) {
            adapter?.toggleSelection(position)
            updateActionModeTitle()
        } else {
            val user = listContacts?.get(position)?.user
            val contact = megaApi.getContact(user)
            if (contact?.visibility == MegaUser.VISIBILITY_VISIBLE) {
                ContactUtil.openContactInfoActivity(this, user)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("NotifyDataSetChanged")
    override fun onBackPressed() {
        Timber.d("onBackPressed")
        val psaWebBrowser = psaWebBrowser
        if (psaWebBrowser != null && psaWebBrowser.consumeBack()) return
        retryConnectionsAndSignalPresence()

        if (adapter?.positionClicked != -1) {
            adapter?.positionClicked = -1
            notifyDataSetChanged()
        } else {
            if (parentHandleStack.isEmpty()) {
                super.onBackPressed()
            } else {
                parentHandle = parentHandleStack.pop()
                listView?.visibility = View.VISIBLE
                emptyImage?.visibility = View.GONE
                emptyText?.visibility = View.GONE
                if (parentHandle == -1L) {
                    supportActionBar?.title =
                        getString(R.string.file_properties_shared_folder_select_contact)
                    supportActionBar?.setLogo(R.drawable.ic_action_navigation_accept_white)
                } else {
                    contactNodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandle))
                    supportActionBar?.title = megaApi.getNodeByHandle(parentHandle)?.name
                    supportActionBar?.setLogo(R.drawable.ic_action_navigation_previous_item)
                }
                supportInvalidateOptionsMenu()
                adapter?.setShareList(listContacts)
                listView?.scrollToPosition(0)
            }
        }
    }

    private fun updateActionModeTitle() {
        Timber.d("updateActionModeTitle")
        if (actionMode == null) {
            return
        }
        val contacts = adapter?.selectedShares
        if (contacts != null) {
            Timber.d("Contacts selected: %s", contacts.size)
        }

        actionMode?.title = resources.getQuantityString(
            R.plurals.general_selection_num_contacts, contacts?.size ?: 0, contacts?.size ?: 0
        )
        try {
            actionMode?.invalidate()
        } catch (e: NullPointerException) {
            Timber.e(e, "Invalidate error")
        }
    }

    /*
     * Disable selection
     */
    fun hideMultipleSelect() {
        adapter?.isMultipleSelect = false
        actionMode?.finish()
    }

    override fun onClick(v: View) {
        if (v.id == R.id.floating_button_file_contact_list) {
            handleShareFolder()
        }
    }

    override fun removeFileContactShare(userEmail: String) {
        notifyDataSetChanged()

        showConfirmationRemoveContactFromShare(selectedShare?.user)
    }

    override fun fileContactsDialogDismissed() {
    }

    override fun changePermissions(userEmail: String) {
        Timber.d("changePermissions")
        notifyDataSetChanged()
        val nodeType = checkBackupNodeTypeByHandle(megaApi, node)
        if (nodeType != BACKUP_NONE) {
            showWarningDialog()
            return
        }
        val dialogBuilder =
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
        dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions))
        val items = arrayOf<CharSequence>(
            getString(R.string.file_properties_shared_folder_read_only), getString(
                R.string.file_properties_shared_folder_read_write
            ), getString(R.string.file_properties_shared_folder_full_access)
        )

        dialogBuilder.setSingleChoiceItems(
            items, selectedShare?.access ?: 0
        ) { _: DialogInterface?, item: Int ->
            statusDialog = createProgressDialog(
                this, getString(R.string.context_permissions_changing_folder)
            )
            permissionsDialog?.dismiss()
            contactController.changePermission(
                selectedShare?.user, item, node, ShareListener(
                    this, ShareListener.CHANGE_PERMISSIONS_LISTENER, 1
                )
            )
        }
        permissionsDialog = dialogBuilder.create().also {
            it.show()
        }
    }

    /**
     * Show the warning dialog when change the permissions of this folder
     *
     * @return The dialog
     */
    private fun showWarningDialog(): AlertDialog {
        val dialogClickListener =
            DialogInterface.OnClickListener { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
        val layout = this.layoutInflater
        val view = layout.inflate(R.layout.dialog_backup_operate_tip, null)
        val tvTitle = view.findViewById<TextView>(R.id.title)
        val tvContent = view.findViewById<TextView>(R.id.backup_tip_content)
        tvTitle.setText(R.string.backup_share_permission_title)
        tvContent.setText(R.string.backup_share_permission_text)
        val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(this).setView(view)
        builder.setPositiveButton(
            getString(R.string.button_permission_info), dialogClickListener
        )
        val dialog = builder.show()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    fun setPositionClicked(positionClicked: Int) {
        adapter?.positionClicked = positionClicked
    }

    @SuppressLint("NotifyDataSetChanged")
    fun notifyDataSetChanged() {
        adapter?.notifyDataSetChanged()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (intent == null) {
            return
        }

        if (requestCode == Constants.REQUEST_CODE_SELECT_CONTACT && resultCode == RESULT_OK) {
            if (!Util.isOnline(this)) {
                showSnackbar(getString(R.string.error_server_connection_problem))
                return
            }

            val emails =
                intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS) ?: arrayListOf()
            val nodeHandle = intent.getLongExtra(AddContactActivity.EXTRA_NODE_HANDLE, -1)

            if (nodeHandle != -1L) {
                node = megaApi.getNodeByHandle(nodeHandle)
            }

            if (fileBackupManager?.shareFolder(
                    nodeController, longArrayOf(nodeHandle), emails, MegaShare.ACCESS_READ
                ) == true
            ) {
                return
            }

            node?.let {
                if (it.isFolder) {
                    val dialogBuilder = MaterialAlertDialogBuilder(
                        this, R.style.ThemeOverlay_Mega_MaterialAlertDialog
                    )
                    dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions))
                    val items = arrayOf<CharSequence>(
                        getString(R.string.file_properties_shared_folder_read_only), getString(
                            R.string.file_properties_shared_folder_read_write
                        ), getString(R.string.file_properties_shared_folder_full_access)
                    )
                    dialogBuilder.setSingleChoiceItems(
                        items, -1
                    ) { _: DialogInterface?, item: Int ->
                        permissionsDialog?.dismiss()
                        statusDialog = createProgressDialog(
                            this, getString(R.string.context_sharing_folder)
                        )
                        statusDialog?.show()
                        this.shareFolder(it, emails, item)
                    }
                    permissionsDialog = dialogBuilder.create().also { dialog -> dialog.show() }
                }
            }

        }
    }

    override fun onUsersUpdate(api: MegaApiJava, users: ArrayList<MegaUser>?) {
        Timber.d("onUserupdate")
    }

    override fun onUserAlertsUpdate(api: MegaApiJava, userAlerts: ArrayList<MegaUserAlert>?) {
        Timber.d("onUserAlertsUpdate")
    }

    override fun onEvent(api: MegaApiJava, event: MegaEvent?) {
    }

    override fun onSetsUpdate(api: MegaApiJava, sets: ArrayList<MegaSet>?) {
    }

    override fun onSetElementsUpdate(api: MegaApiJava, elements: ArrayList<MegaSetElement>?) {
    }

    override fun onNodesUpdate(api: MegaApiJava, nodeList: ArrayList<MegaNode>?) {
        Timber.d("onNodesUpdate")

        try {
            statusDialog?.dismiss()
        } catch (ex: Exception) {
            Timber.e(ex, "Error dismiss status dialog")
        }

        if (node?.isFolder == true) {
            viewModel.getMegaShares(node)
        }
    }

    private fun updateListView() {
        listContacts?.clear()
        tempListContacts?.takeUnless { it.isEmpty() }?.let {
            listContacts?.addAll(it)
        }

        if ((listContacts?.size ?: 0) > 0) {
            listView?.visibility = View.VISIBLE
            emptyImage?.visibility = View.GONE
            emptyText?.visibility = View.GONE

            if (adapter != null) {
                adapter?.setNode(node)
                adapter?.setContext(this)
                adapter?.setShareList(listContacts)
                adapter?.listFragment = listView
            } else {
                adapter = MegaSharedFolderAdapter(this, node, listContacts, listView)
                adapter?.isMultipleSelect = false
            }
            listView?.adapter = adapter
        } else {
            listView?.visibility = View.GONE
            emptyImage?.visibility = View.VISIBLE
            emptyText?.visibility = View.VISIBLE
        }
        listView?.invalidate()
    }

    fun showConfirmationRemoveContactFromShare(email: String?) {
        val builder = AlertDialog.Builder(this)
        val message = resources.getString(R.string.remove_contact_shared_folder, email)
        builder.setMessage(message)
            .setPositiveButton(R.string.general_remove) { _: DialogInterface?, _: Int ->
                statusDialog = createProgressDialog(
                    this, getString(R.string.context_removing_contact_folder)
                )
                nodeController.removeShare(
                    ShareListener(
                        this, ShareListener.REMOVE_SHARE_LISTENER, 1
                    ), node, email
                )
            }.setNegativeButton(
                mega.privacy.android.shared.resources.R.string.general_dialog_cancel_button
            ) { _: DialogInterface?, _: Int -> }.show()
    }

    fun showConfirmationRemoveMultipleContactFromShare(contacts: ArrayList<MegaShare>) {
        Timber.d("showConfirmationRemoveMultipleContactFromShare")

        val dialogClickListener =
            DialogInterface.OnClickListener { _: DialogInterface?, which: Int ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        removeMultipleShares(contacts)
                    }

                    DialogInterface.BUTTON_NEGATIVE -> {}
                }
            }

        val builder = AlertDialog.Builder(this)
        val message = resources.getQuantityString(
            R.plurals.remove_multiple_contacts_shared_folder, contacts.size, contacts.size
        )
        builder.setMessage(message).setPositiveButton(R.string.general_remove, dialogClickListener)
            .setNegativeButton(
                mega.privacy.android.shared.resources.R.string.general_dialog_cancel_button,
                dialogClickListener
            ).show()
    }

    private fun removeMultipleShares(shares: ArrayList<MegaShare>) {
        Timber.d("Number of shared to remove: %s", shares.size)

        statusDialog = createProgressDialog(
            this, getString(R.string.context_removing_contact_folder)
        )
        nodeController.removeShares(shares, node)
    }

    override fun onAccountUpdate(api: MegaApiJava) {
    }

    override fun onContactRequestsUpdate(
        api: MegaApiJava,
        requests: ArrayList<MegaContactRequest>?,
    ) {
    }

    fun showSnackbar(snackbarMessage: String) {
        findViewById<RelativeLayout>(R.id.file_contact_list)?.let {
            showSnackbar(it, snackbarMessage)
        }
    }

    private fun getSelectedContact(): MegaUser? {
        val email = selectedShare?.user
        return megaApi.getContact(email)
    }

    private fun updateAdapter(handleReceived: Long) {
        val index = listContacts?.indexOfFirst {
            val email = it.user
            val contact = megaApi.getContact(email)
            val handleUser = contact?.handle
            handleUser == handleReceived
        } ?: -1

        if (index != -1) adapter?.notifyItemChanged(index)
    }

    companion object {
        fun launchIntent(context: Context?, handle: Long?): Intent {
            val intent = Intent(
                context, FileContactListActivity::class.java
            )
            intent.putExtra(Constants.NAME, handle)
            return intent
        }
    }
}

