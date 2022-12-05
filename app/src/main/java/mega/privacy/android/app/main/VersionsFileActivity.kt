package mega.privacy.android.app.main

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ActionMode
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.putThumbnailLocation
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.databinding.ActivityVersionsFileBinding
import mega.privacy.android.app.imageviewer.ImageViewerActivity.Companion.getIntentForSingleNode
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.adapters.VersionsFileAdapter
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.VersionBottomSheetDialogFragment
import mega.privacy.android.app.utils.AlertsAndWarnings.showSaveToDeviceConfirmDialog
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaNodeUtil.manageTextFileIntent
import mega.privacy.android.app.utils.MegaNodeUtil.manageURLNode
import mega.privacy.android.app.utils.MegaNodeUtil.setupStreamingServer
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils.checkNotificationsPermission
import nz.mega.sdk.MegaApiJava
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

/**
 * File Version list activity
 */
class VersionsFileActivity : PasscodeActivity(), MegaRequestListenerInterface, View.OnClickListener,
    MegaGlobalListenerInterface, SnackbarShower {

    private lateinit var binding: ActivityVersionsFileBinding

    var aB: ActionBar? = null
    var selectedNode: MegaNode? = null
    private var selectedNodeHandle: Long = 0
    var selectedPosition = 0

    var mLayoutManager: LinearLayoutManager? = null
    var nodeVersions: ArrayList<MegaNode>? = null
    var node: MegaNode? = null
    var adapter: VersionsFileAdapter? = null

    @JvmField
    var versionsSize: String? = null
    private var actionMode: ActionMode? = null
    var selectMenuItem: MenuItem? = null
    var unSelectMenuItem: MenuItem? = null
    var deleteVersionsMenuItem: MenuItem? = null
    var handler: Handler? = null

    var totalRemoveSelected = 0
    var errorRemove = 0
    var completedRemove = 0
    private var bottomSheetDialogFragment: VersionBottomSheetDialogFragment? = null
    private val nodeSaver = NodeSaver(this, this, this,
        showSaveToDeviceConfirmDialog(this))
    var accessLevel = 0
        private set
    private var deleteVersionConfirmationDialog: AlertDialog? = null
    private var checkPermissionRevertVersionDialog: AlertDialog? = null
    private var deleteVersionHistoryDialog: AlertDialog? = null


    @SuppressWarnings("deprecation")
    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)
        binding = ActivityVersionsFileBinding.inflate(layoutInflater)

        if (shouldRefreshSessionDueToSDK() || shouldRefreshSessionDueToKarere()) {
            return
        }
        megaApi.addGlobalListener(this)
        handler = Handler(Looper.getMainLooper())
        setContentView(binding.root)

        //Set toolbar
        setSupportActionBar(binding.toolbarVersionsFile)

        aB = supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.title_section_versions)
        }

        with(binding.recyclerViewVersionsFile) {
            setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics))
            clipToPadding = false
            addItemDecoration(SimpleDividerItemDecoration(this@VersionsFileActivity))
            mLayoutManager = LinearLayoutManager(this@VersionsFileActivity)
            layoutManager = mLayoutManager
            itemAnimator = Util.noChangeRecyclerViewItemAnimator()
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    checkScroll()
                }
            })
        }


        var nodeHandle = MegaApiJava.INVALID_HANDLE
        if (savedInstanceState != null) {
            nodeHandle =
                savedInstanceState.getLong(Constants.EXTRA_NODE_HANDLE, MegaApiJava.INVALID_HANDLE)
        }
        intent?.extras?.let { extras ->
            if (nodeHandle == MegaApiJava.INVALID_HANDLE) {
                nodeHandle = extras.getLong("handle")
            }
            node = megaApi.getNodeByHandle(nodeHandle)
            node?.let {
                accessLevel = megaApi.getAccess(node)
                nodeVersions = megaApi.getVersions(node)
                val getVersionsSizeTask = GetVersionsSizeTask()
                getVersionsSizeTask.execute()
                binding.recyclerViewVersionsFile.visibility = View.VISIBLE

                adapter?.let {
                    it.setNodes(nodeVersions)
                    it.isMultipleSelect = false
                } ?: run {
                    adapter =
                        VersionsFileAdapter(this,
                            nodeVersions,
                            binding.recyclerViewVersionsFile).also {
                            binding.recyclerViewVersionsFile.adapter = it
                            it.isMultipleSelect = false
                        }
                }

                binding.recyclerViewVersionsFile.adapter = adapter
            } ?: run {
                Timber.e("ERROR: node is NULL")
            }
        }
    }


    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        showSnackbar(type, binding.versionsMainLayout, content, chatId)
    }

    private inner class GetVersionsSizeTask : AsyncTask<String?, Void?, String>() {

        override fun doInBackground(vararg params: String?): String {

            val sizeNumber: Long = nodeVersions?.sumOf { node -> node.size } ?: 0L
            val size = Util.getSizeString(sizeNumber)
            Timber.d("doInBackground-AsyncTask GetVersionsSizeTask: %s", size)
            return size
        }

        override fun onPostExecute(size: String) {
            Timber.d("GetVersionsSizeTask::onPostExecute")
            updateSize(size)
        }
    }

    private inner class ActionBarCallBack : ActionMode.Callback {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            Timber.d("onActionItemClicked")
            val nodes = adapter?.selectedNodes
            when (item.itemId) {
                R.id.cab_menu_select_all -> {
                    selectAll()
                }
                R.id.cab_menu_unselect_all -> {
                    clearSelections()
                }
                R.id.action_download_versions -> {
                    if (nodes?.size == 1) {
                        downloadNodes(nodes)
                        clearSelections()
                        actionMode!!.invalidate()
                    }
                }
                R.id.action_delete_versions -> {
                    nodes?.let {
                        showConfirmationRemoveVersions(it)
                    }
                }
                R.id.action_revert_version -> {
                    if (nodes?.size == 1) {
                        selectedNode = nodes[0]
                        selectedNodeHandle = selectedNode!!.handle
                        checkRevertVersion()
                        clearSelections()
                        actionMode!!.invalidate()
                    }
                }
            }
            return false
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            Timber.d("onCreateActionMode")
            val inflater = mode.menuInflater
            inflater.inflate(R.menu.versions_files_action, menu)
            with(menu) {
                findItem(R.id.cab_menu_select_all).isVisible = true
                findItem(R.id.action_download_versions).isVisible = false
                findItem(R.id.action_delete_versions).isVisible = false
            }
            return true
        }

        override fun onDestroyActionMode(arg0: ActionMode) {
            Timber.d("onDestroyActionMode")
            adapter?.run {
                clearSelections()
                isMultipleSelect = false
            }
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            Timber.d("onPrepareActionMode")

            adapter?.let { adapter ->
                adapter.selectedNodes?.let { selected ->
                    with(menu) {
                        if (selected.size != 0) {
                            val unselect = menu.findItem(R.id.cab_menu_unselect_all)
                            if (selected.size == adapter.itemCount) {
                                findItem(R.id.cab_menu_select_all).isVisible = false
                                unselect.title = getString(R.string.action_unselect_all)
                                unselect.isVisible = true
                            } else {
                                findItem(R.id.cab_menu_select_all).isVisible = true
                                unselect.title = getString(R.string.action_unselect_all)
                                unselect.isVisible = true
                            }
                            if (selected.size == 1) {
                                findItem(R.id.action_revert_version).isVisible =
                                    selectedPosition != 0
                                findItem(R.id.action_download_versions).isVisible = true
                            } else {
                                findItem(R.id.action_revert_version).isVisible = false
                                findItem(R.id.action_download_versions).isVisible = false
                            }
                            findItem(R.id.action_delete_versions).isVisible = true
                        } else {
                            findItem(R.id.cab_menu_select_all).isVisible = true
                            findItem(R.id.cab_menu_unselect_all).isVisible = false
                            findItem(R.id.action_download_versions).isVisible = false
                            findItem(R.id.action_delete_versions).isVisible = false
                            findItem(R.id.action_revert_version).isVisible = false
                        }
                    }
                }
            }
            return false
        }
    }

    fun checkScroll() {
        Util.changeViewElevation(
            aB,
            (binding.recyclerViewVersionsFile.canScrollVertically(-1) &&
                    binding.recyclerViewVersionsFile.visibility == View.VISIBLE
                    ) || adapter?.isMultipleSelect ?: false,
            outMetrics)
    }

    fun showOptionsPanel(sNode: MegaNode?, sPosition: Int) {
        Timber.d("showOptionsPanel")
        if (node == null || bottomSheetDialogFragment.isBottomSheetDialogShown()) return
        selectedNode = sNode
        selectedNodeHandle = selectedNode?.handle ?: 0L
        selectedPosition = sPosition
        bottomSheetDialogFragment = VersionBottomSheetDialogFragment().also {
            it.show(supportFragmentManager, it.tag)
        }
    }

    fun downloadNodes(nodes: List<MegaNode>?) {
        checkNotificationsPermission(this)
        nodes?.let {
            nodeSaver.saveNodes(it, false, false, false, false)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        nodeSaver.handleActivityResult(this, requestCode, resultCode, intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        nodeSaver.handleRequestPermissionsResult(requestCode)
    }

    override fun onDestroy() {
        super.onDestroy()
        megaApi.removeGlobalListener(this)
        megaApi.removeRequestListener(this)
        handler?.removeCallbacksAndMessages(null)
        nodeSaver.destroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        // Inflate the menu items for use in the action bar
        menuInflater.inflate(R.menu.activity_folder_contact_list, menu)
        with(menu) {
            selectMenuItem = findItem(R.id.action_select)
            unSelectMenuItem = findItem(R.id.action_unselect)
            deleteVersionsMenuItem = findItem(R.id.action_delete_version_history)
            findItem(R.id.action_folder_contacts_list_share_folder).isVisible = false
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        when (accessLevel) {
            MegaShare.ACCESS_FULL, MegaShare.ACCESS_OWNER -> {
                selectMenuItem?.isVisible = true
                unSelectMenuItem?.isVisible = false
                deleteVersionsMenuItem?.isVisible = true
            }
            else -> {
                selectMenuItem?.isVisible = false
                unSelectMenuItem?.isVisible = false
                deleteVersionsMenuItem?.isVisible = false
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle presses on the action bar items
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_select -> {
                selectAll()
                true
            }
            R.id.action_delete_version_history -> {
                showDeleteVersionHistoryDialog()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun showDeleteVersionHistoryDialog() {
        Timber.d("showDeleteVersionHistoryDialog")
        deleteVersionHistoryDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.title_delete_version_history)
            .setMessage(R.string.text_delete_version_history)
            .setPositiveButton(R.string.context_delete) { _: DialogInterface?, _: Int -> deleteVersionHistory() }
            .setNegativeButton(R.string.general_cancel) { _: DialogInterface?, _: Int -> }
            .show()
    }

    private fun deleteVersionHistory() {
        val intent = Intent().also {
            it.putExtra(KEY_DELETE_VERSION_HISTORY, true)
            it.putExtra(KEY_DELETE_NODE_HANDLE, node?.handle)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    // Clear all selected items
    private fun clearSelections() {
        adapter?.let {
            if (it.isMultipleSelect) {
                it.clearSelections()
            }
        }
    }

    private fun selectAll() {
        Timber.d("selectAll")
        adapter?.let {
            if (it.isMultipleSelect) {
                it.selectAll()
            } else {
                it.isMultipleSelect = true
                it.selectAll()
                actionMode = startSupportActionMode(ActionBarCallBack())
            }
            Handler(Looper.getMainLooper()).post { updateActionModeTitle() }
        }
    }

    fun showSelectMenuItem(): Boolean =
        adapter?.let { it.isMultipleSelect } ?: false

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        if (request.type == MegaRequest.TYPE_SHARE) {
            Timber.d("onRequestStart - Share")
        }
    }

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.d("onRequestFinish: %s", request.type)
        Timber.d("onRequestFinish: %s", request.requestString)

        adapter?.takeIf { it.isMultipleSelect }?.let {
            it.clearSelections()
            hideMultipleSelect()
        }
        if (request.type == MegaRequest.TYPE_REMOVE) {
            Timber.d("MegaRequest.TYPE_REMOVE")
            totalRemoveSelected--
            if (e.errorCode == MegaError.API_OK) {
                completedRemove++
                checkScroll()
            } else {
                errorRemove++
            }
            if (totalRemoveSelected == 0) {
                if (completedRemove > 0 && errorRemove == 0) {
                    showSnackbar(resources.getQuantityString(R.plurals.versions_deleted_succesfully,
                        completedRemove,
                        completedRemove))
                } else if (completedRemove > 0 && errorRemove > 0) {
                    showSnackbar("""
    ${
                        resources.getQuantityString(R.plurals.versions_deleted_succesfully,
                            completedRemove,
                            completedRemove)
                    }
    ${resources.getQuantityString(R.plurals.versions_not_deleted, errorRemove, errorRemove)}
    """.trimIndent())
                } else {
                    showSnackbar(resources.getQuantityString(R.plurals.versions_not_deleted,
                        errorRemove,
                        errorRemove))
                }
            }
        } else if (request.type == MegaRequest.TYPE_RESTORE) {
            Timber.d("MegaRequest.TYPE_RESTORE")
            if (e.errorCode == MegaError.API_OK) {
                if (accessLevel <= MegaShare.ACCESS_READWRITE) {
                    showSnackbar(getString(R.string.version_as_new_file_created))
                } else {
                    showSnackbar(getString(R.string.version_restored))
                }
            } else {
                showSnackbar(getString(R.string.general_text_error))
            }
        }
    }

    override fun onRequestTemporaryError(
        api: MegaApiJava, request: MegaRequest,
        e: MegaError,
    ) {
        Timber.w("onRequestTemporaryError")
    }

    fun itemClick(position: Int) {
        Timber.d("Position: %s", position)

        nodeVersions?.let {
            val vNode = it[position]
            val mimetype = MimeTypeList.typeForName(vNode.name)
            when {
                adapter?.isMultipleSelect == true -> {
                    adapter!!.toggleSelection(position)
                    updateActionModeTitle()
                }
                mimetype.isImage -> {
                    val intent = getIntentForSingleNode(this, vNode.handle, true)
                    putThumbnailLocation(intent,
                        binding.recyclerViewVersionsFile,
                        position,
                        Constants.VIEWER_FROM_FILE_VERSIONS,
                        adapter)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                }
                mimetype.isVideoReproducible || mimetype.isAudio -> {
                    val mediaIntent: Intent
                    val internalIntent: Boolean
                    var opusFile = false
                    if (mimetype.isVideoNotSupported || mimetype.isAudioNotSupported) {
                        mediaIntent = Intent(Intent.ACTION_VIEW)
                        internalIntent = false
                        val s = vNode.name.split("\\.").toTypedArray()
                        if (s.size > 1 && s[s.size - 1] == "opus") {
                            opusFile = true
                        }
                    } else {
                        mediaIntent = Util.getMediaIntent(this, vNode.name)
                        internalIntent = true
                    }
                    mediaIntent.putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE,
                        Constants.VERSIONS_ADAPTER)
                    putThumbnailLocation(mediaIntent,
                        binding.recyclerViewVersionsFile,
                        position,
                        Constants.VIEWER_FROM_FILE_BROWSER,
                        adapter)
                    mediaIntent.putExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME, vNode.name)
                    val localPath = FileUtil.getLocalFile(vNode)
                    if (localPath != null) {
                        val mediaFile = File(localPath)
                        if (localPath.contains(Environment.getExternalStorageDirectory().path)) {
                            val mediaFileUri = FileProvider.getUriForFile(this,
                                Constants.AUTHORITY_STRING_FILE_PROVIDER,
                                mediaFile)
                            if (mediaFileUri == null) {
                                showSnackbar(Constants.SNACKBAR_TYPE,
                                    getString(R.string.general_text_error),
                                    -1)
                            } else {
                                mediaIntent.setDataAndType(mediaFileUri,
                                    MimeTypeList.typeForName(vNode.name).type)
                            }
                        } else {
                            val mediaFileUri = Uri.fromFile(mediaFile)
                            if (mediaFileUri == null) {
                                showSnackbar(Constants.SNACKBAR_TYPE,
                                    getString(R.string.general_text_error),
                                    -1)
                            } else {
                                mediaIntent.setDataAndType(mediaFileUri,
                                    MimeTypeList.typeForName(vNode.name).type)
                            }
                        }
                        mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } else {
                        setupStreamingServer(megaApi, this)
                        val url = megaApi.httpServerGetLocalLink(vNode)
                        if (url != null) {
                            val parsedUri = Uri.parse(url)
                            if (parsedUri != null) {
                                mediaIntent.setDataAndType(parsedUri, mimetype.type)
                            } else {
                                showSnackbar(Constants.SNACKBAR_TYPE,
                                    getString(R.string.general_text_error),
                                    -1)
                            }
                        } else {
                            showSnackbar(Constants.SNACKBAR_TYPE,
                                getString(R.string.general_text_error),
                                -1)
                        }
                    }
                    mediaIntent.putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, vNode.handle)
                    if (opusFile) {
                        mediaIntent.setDataAndType(mediaIntent.data, "audio/*")
                    }
                    if (internalIntent) {
                        startActivity(mediaIntent)
                    } else {
                        if (MegaApiUtils.isIntentAvailable(this, mediaIntent)) {
                            startActivity(mediaIntent)
                        } else {
                            showSnackbar(Constants.SNACKBAR_TYPE,
                                getString(R.string.intent_not_available),
                                -1)
                            downloadNodes(listOf(vNode))
                        }
                    }
                    overridePendingTransition(0, 0)
                }
                mimetype.isURL -> {
                    manageURLNode(this, megaApi, vNode)
                }
                mimetype.isPdf -> {
                    val pdfIntent = Intent(this, PdfViewerActivity::class.java)
                    pdfIntent.putExtra(Constants.INTENT_EXTRA_KEY_INSIDE, true)
                    pdfIntent.putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE,
                        Constants.VERSIONS_ADAPTER)
                    val localPath = FileUtil.getLocalFile(vNode)
                    if (localPath != null) {
                        val mediaFile = File(localPath)
                        if (localPath.contains(Environment.getExternalStorageDirectory().path)) {
                            pdfIntent.setDataAndType(FileProvider.getUriForFile(this,
                                Constants.AUTHORITY_STRING_FILE_PROVIDER,
                                mediaFile), MimeTypeList.typeForName(vNode.name).type)
                        } else {
                            pdfIntent.setDataAndType(Uri.fromFile(mediaFile),
                                MimeTypeList.typeForName(vNode.name).type)
                        }
                        pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } else {
                        setupStreamingServer(megaApi, this)
                        val url = megaApi.httpServerGetLocalLink(vNode)
                        if (url != null) {
                            val parsedUri = Uri.parse(url)
                            if (parsedUri != null) {
                                pdfIntent.setDataAndType(parsedUri, mimetype.type)
                            } else {
                                showSnackbar(Constants.SNACKBAR_TYPE,
                                    getString(R.string.general_text_error),
                                    -1)
                            }
                        } else {
                            showSnackbar(Constants.SNACKBAR_TYPE,
                                getString(R.string.general_text_error),
                                -1)
                        }
                    }
                    pdfIntent.putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, vNode.handle)
                    putThumbnailLocation(pdfIntent,
                        binding.recyclerViewVersionsFile,
                        position,
                        Constants.VIEWER_FROM_FILE_BROWSER,
                        adapter)
                    if (MegaApiUtils.isIntentAvailable(this, pdfIntent)) {
                        startActivity(pdfIntent)
                    } else {
                        Toast.makeText(this,
                            StringResourcesUtils.getString(R.string.intent_not_available),
                            Toast.LENGTH_LONG).show()
                        downloadNodes(listOf(vNode))
                    }
                    overridePendingTransition(0, 0)
                }
                mimetype.isOpenableTextFile(vNode.size) -> {
                    manageTextFileIntent(this, vNode, Constants.VERSIONS_ADAPTER)
                }
                else -> {
                    showOptionsPanel(vNode, position)
                }
            }
        }
    }

    private fun updateActionModeTitle() {
        Timber.d("updateActionModeTitle")

        actionMode?.let {
            val nodes = adapter?.selectedNodes ?: emptyList()
            val res = resources
            val format = "%d %s"
            it.title = String.format(format,
                nodes.size,
                res.getQuantityString(R.plurals.general_num_files, nodes.size))
            try {
                it.invalidate()
            } catch (e: NullPointerException) {
                e.printStackTrace()
                Timber.e(e, "Invalidate error")
            }
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
        when (v.id) {
            R.id.file_contact_list_layout -> {
                val i = Intent(this, ManagerActivity::class.java).apply {
                    action = Constants.ACTION_REFRESH_PARENTHANDLE_BROWSER
                    putExtra("parentHandle", node?.handle)
                }

                startActivity(i)
                finish()
            }
        }
    }

    fun notifyDataSetChanged() {
        adapter?.notifyDataSetChanged()
    }

    override fun onUsersUpdate(api: MegaApiJava, users: ArrayList<MegaUser>) {
        Timber.d("onUsersUpdate")
    }

    override fun onUserAlertsUpdate(api: MegaApiJava, userAlerts: ArrayList<MegaUserAlert>) {
        Timber.d("onUserAlertsUpdate")
    }

    override fun onEvent(api: MegaApiJava, event: MegaEvent) {}
    override fun onSetsUpdate(api: MegaApiJava, sets: ArrayList<MegaSet>) {}
    override fun onSetElementsUpdate(api: MegaApiJava, elements: ArrayList<MegaSetElement>) {}
    override fun onNodesUpdate(api: MegaApiJava, nodes: ArrayList<MegaNode?>?) {
        Timber.d("onNodesUpdate")
        var thisNode = false
        var anyChild = false
        if (nodes == null) {
            return
        }
        var n: MegaNode? = null
        val it: Iterator<MegaNode?> = nodes.iterator()
        while (it.hasNext()) {
            val nodeToCheck = it.next()
            if (nodeToCheck != null) {
                if (nodeToCheck.handle == node?.handle) {
                    thisNode = true
                    n = nodeToCheck
                } else {
                    nodeVersions?.forEach { version ->
                        if (nodeToCheck.handle == version.handle) {
                            if (!anyChild) {
                                anyChild = true
                            }
                        }
                    }
                }
            }
        }

        if (!thisNode && !anyChild) {
            Timber.w("Exit - Not related to this node")
            return
        }

        //Check if the parent handle has changed
        n?.let {
            if (n.hasChanged(MegaNode.CHANGE_TYPE_PARENT)) {
                val oldParent = megaApi.getParentNode(node)
                val newParent = megaApi.getParentNode(n)
                if (oldParent.handle == newParent.handle) {
                    if (newParent.isFile) {
                        Timber.d("New version added")
                        node = newParent
                    } else {
                        finish()
                    }
                } else {
                    node = n
                }
                Timber.d("Node name: %s", node?.name)
                nodeVersions = if (megaApi.hasVersions(node)) {
                    megaApi.getVersions(node)
                } else {
                    null
                }
            } else if (n.hasChanged(MegaNode.CHANGE_TYPE_REMOVED)) {
                if (thisNode) {
                    if (nodeVersions != null) {
                        node = nodeVersions!![1]
                        nodeVersions = if (megaApi.hasVersions(node)) {
                            megaApi.getVersions(node)
                        } else {
                            null
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
        } ?: run {
            if (anyChild) {
                nodeVersions = if (megaApi.hasVersions(node)) {
                    megaApi.getVersions(node)
                } else {
                    null
                }
            }
        }
        if (nodeVersions == null || nodeVersions!!.size == 1) {
            finish()
        } else {
            Timber.d("After update - nodeVersions size: %s", nodeVersions?.size)

            adapter?.apply {
                setNodes(nodeVersions)
                notifyDataSetChanged()
            } ?: run {
                adapter = VersionsFileAdapter(this, nodeVersions, binding.recyclerViewVersionsFile)
                binding.recyclerViewVersionsFile.adapter = adapter
            }

            val getVersionsSizeTask = GetVersionsSizeTask()
            getVersionsSizeTask.execute()
        }
    }

    override fun onReloadNeeded(api: MegaApiJava) {}

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {}

    override fun onAccountUpdate(api: MegaApiJava) {}

    override fun onContactRequestsUpdate(
        api: MegaApiJava,
        requests: ArrayList<MegaContactRequest>,
    ) {
    }

    fun checkRevertVersion() {
        if (accessLevel <= MegaShare.ACCESS_READWRITE) {
            checkPermissionRevertVersionDialog = MaterialAlertDialogBuilder(this)
                .setCancelable(false)
                .setTitle(R.string.permissions_error_label)
                .setMessage(R.string.alert_not_enough_permissions_revert)
                .setPositiveButton(R.string.create_new_file_action) { _: DialogInterface?, _: Int -> revertVersion() }
                .setNegativeButton(R.string.general_cancel) { _: DialogInterface?, _: Int -> }
                .show()
        } else {
            revertVersion()
        }
    }

    private fun revertVersion() {
        Timber.d("revertVersion")
        megaApi.restoreVersion(selectedNode, this)
    }

    fun removeVersion() {
        Timber.d("removeVersion")
        megaApi.removeVersion(selectedNode, this)
    }

    fun removeVersions(removeNodes: List<MegaNode?>) {
        Timber.d("removeVersion")
        totalRemoveSelected = removeNodes.size
        errorRemove = 0
        completedRemove = 0
        for (i in removeNodes.indices) {
            megaApi.removeVersion(removeNodes[i], this)
        }
    }

    fun showSnackbar(s: String?) {
        showSnackbar(binding.versionsMainLayout, s!!)
    }

    fun updateSize(size: String?) {
        Timber.d("Size: %s", size)
        versionsSize = size
        adapter?.notifyItemChanged(1)
    }

    fun showConfirmationRemoveVersion() {
        deleteVersionConfirmationDialog = MaterialAlertDialogBuilder(this)
            .setTitle(resources.getQuantityString(R.plurals.title_dialog_delete_version, 1))
            .setMessage(getString(R.string.content_dialog_delete_version))
            .setPositiveButton(R.string.context_delete) { _: DialogInterface?, _: Int -> removeVersion() }
            .setNegativeButton(R.string.general_cancel) { _: DialogInterface?, _: Int -> }
            .show()
    }

    fun showConfirmationRemoveVersions(removeNodes: List<MegaNode?>) {
        val builder = MaterialAlertDialogBuilder(this)
        val message: String
        val title: String

        if (removeNodes.size == 1) {
            title = resources.getQuantityString(R.plurals.title_dialog_delete_version, 1)
            message = resources.getString(R.string.content_dialog_delete_version)
        } else {
            title =
                resources.getQuantityString(R.plurals.title_dialog_delete_version, removeNodes.size)
            message = resources.getString(R.string.content_dialog_delete_multiple_version,
                removeNodes.size)
        }
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.context_delete) { _: DialogInterface?, _: Int ->
                removeVersions(removeNodes)
            }
            .setNegativeButton(R.string.general_cancel) { _: DialogInterface?, _: Int -> }
            .show()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        Timber.d("onSaveInstanceState")
        super.onSaveInstanceState(outState)
        outState.putLong(Constants.EXTRA_NODE_HANDLE, node!!.handle)
        outState.putBoolean(CHECKING_REVERT_VERSION,
            checkPermissionRevertVersionDialog != null && checkPermissionRevertVersionDialog!!.isShowing)
        outState.putLong(SELECTED_NODE_HANDLE, selectedNodeHandle)
        outState.putInt(SELECTED_POSITION, selectedPosition)
        outState.putBoolean(DELETING_VERSION_DIALOG_SHOWN,
            deleteVersionConfirmationDialog != null && deleteVersionConfirmationDialog!!.isShowing)
        outState.putBoolean(DELETING_HISTORY_VERSION_DIALOG_SHOWN,
            deleteVersionHistoryDialog != null && deleteVersionHistoryDialog!!.isShowing)
        nodeSaver.saveState(outState)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        selectedNodeHandle =
            savedInstanceState.getLong(SELECTED_NODE_HANDLE, MegaApiJava.INVALID_HANDLE)
        selectedPosition = savedInstanceState.getInt(SELECTED_POSITION)
        nodeSaver.restoreState(savedInstanceState)
        selectedNode = megaApi.getNodeByHandle(selectedNodeHandle)
        if (selectedNode != null) {
            if (savedInstanceState.getBoolean(CHECKING_REVERT_VERSION, false)) {
                checkRevertVersion()
            }
            if (savedInstanceState.getBoolean(DELETING_VERSION_DIALOG_SHOWN, false)) {
                showConfirmationRemoveVersion()
            }
        }
        if (savedInstanceState.getBoolean(DELETING_HISTORY_VERSION_DIALOG_SHOWN, false)) {
            showDeleteVersionHistoryDialog()
        }
    }

    fun startActionMode(position: Int) {
        actionMode = startSupportActionMode(ActionBarCallBack())
        itemClick(position)
    }

    companion object {
        private const val CHECKING_REVERT_VERSION = "CHECKING_REVERT_VERSION"
        private const val SELECTED_NODE_HANDLE = "SELECTED_NODE_HANDLE"
        private const val SELECTED_POSITION = "SELECTED_POSITION"

        private const val DELETING_VERSION_DIALOG_SHOWN = "DELETING_VERSION_DIALOG_SHOWN"
        private const val DELETING_HISTORY_VERSION_DIALOG_SHOWN =
            "DELETING_HISTORY_VERSION_DIALOG_SHOWN"

        const val KEY_DELETE_VERSION_HISTORY = "deleteVersionHistory"
        const val KEY_DELETE_NODE_HANDLE = "nodeHandle"

    }
}