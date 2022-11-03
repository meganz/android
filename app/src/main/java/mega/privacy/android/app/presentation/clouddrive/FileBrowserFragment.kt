package mega.privacy.android.app.presentation.clouddrive

import mega.privacy.android.app.presentation.photos.mediadiscovery.MediaDiscoveryFragment as NewMediaDiscoveryFragment
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.DisplayMetrics
import android.view.Display
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.components.NewGridRecyclerView
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.observeDragSupportEvents
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.putThumbnailLocation
import mega.privacy.android.app.components.scrollBar.FastScroller
import mega.privacy.android.app.constants.EventConstants.EVENT_SHOW_MEDIA_DISCOVERY
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.gallery.ui.MediaDiscoveryFragment
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.imageviewer.ImageViewerActivity.Companion.getIntentForParentNode
import mega.privacy.android.app.interfaces.ActionBackupListener
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.PdfViewerActivity
import mega.privacy.android.app.main.adapters.MegaNodeAdapter
import mega.privacy.android.app.main.adapters.RotatableAdapter
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.managerSections.RotatableFragment
import mega.privacy.android.app.presentation.extensions.serializable
import mega.privacy.android.app.presentation.manager.ManagerViewModel
import mega.privacy.android.app.sync.fileBackups.FileBackupManager
import mega.privacy.android.app.usecase.data.MoveRequestResult
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_ACTION_TYPE
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_DIALOG_WARN
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_HANDLED_ITEM
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_HANDLED_NODE
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_NODE_TYPE
import mega.privacy.android.app.utils.MegaNodeUtil.allHaveOwnerAccessAndNotTakenDown
import mega.privacy.android.app.utils.MegaNodeUtil.isOutShare
import mega.privacy.android.app.utils.MegaNodeUtil.manageTextFileIntent
import mega.privacy.android.app.utils.MegaNodeUtil.manageURLNode
import mega.privacy.android.app.utils.MegaNodeUtil.onNodeTapped
import mega.privacy.android.app.utils.MegaNodeUtil.shareNodes
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.usecase.GetFeatureFlagValue
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import timber.log.Timber
import java.io.File
import java.util.Locale
import java.util.Stack
import javax.inject.Inject

@AndroidEntryPoint
class FileBrowserFragment : RotatableFragment() {

    @Inject
    lateinit var transfersManagement: TransfersManagement

    @Inject
    @MegaApi
    lateinit var megaApi: MegaApiAndroid

    /**
     * SortOrderIntMapper
     */
    @Inject
    lateinit var sortOrderIntMapper: SortOrderIntMapper

    private val managerViewModel by activityViewModels<ManagerViewModel>()
    private val fileBrowserViewModel by viewModels<FileBrowserViewModel>()

    private var aB: ActionBar? = null

    var recyclerView: RecyclerView? = null

    private var fastScroller: FastScroller? = null
    private var emptyImageView: ImageView? = null
    private var emptyTextView: LinearLayout? = null
    private var emptyTextViewFirst: TextView? = null
    private var adapter: MegaNodeAdapter? = null
    private var lastPositionStack: Stack<Int>? = null


    private var density = 0f
    private var outMetrics: DisplayMetrics? = null
    private var display: Display? = null
    private var _nodes = mutableListOf<MegaNode>()
    private var actionMode: ActionMode? = null
    private var mLayoutManager: LinearLayoutManager? = null
    private var gridLayoutManager: CustomizedGridLayoutManager? = null
    private var downloadLocationDefaultPath: String? = null
    private var transferOverQuotaBanner: RelativeLayout? = null
    private var transferOverQuotaBannerText: TextView? = null
    private var mediaHandle: Long = 0

    // Backup warning dialog
    private var backupWarningDialog: AlertDialog? = null
    private var backupHandleList: ArrayList<Long>? = null
    private var backupDialogType = -1
    private var backupNodeHandle: Long? = null
    private var backupNodeType = 0
    private var backupActionType = 0
    private var fileBackupManager: FileBackupManager? = null

    @Inject
    lateinit var getFeatureFlag: GetFeatureFlagValue

    override fun activateActionMode() {
        Timber.d("activateActionMode")
        adapter?.let {
            if (!it.isMultipleSelect) {
                it.isMultipleSelect = true
                actionMode =
                    (activity as? AppCompatActivity)?.startSupportActionMode(ActionBarCallBack())
            }
        }
    }

    /**
     * Shows the Sort by panel.
     */
    private fun showSortByPanel() {
        (activity as? ManagerActivity)?.showNewSortByPanel(Constants.ORDER_CLOUD)
    }

    private inner class ActionBarCallBack : ActionMode.Callback {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            Timber.d("onActionItemClicked")
            val documents = adapter?.selectedNodes
            val handleList = ArrayList<Long>()
            when (item.itemId) {
                R.id.cab_menu_download -> {
                    (activity as? ManagerActivity)?.saveNodesToDevice(
                        documents, false, false, false, false)
                    clearSelections()
                    hideMultipleSelect()
                }
                R.id.cab_menu_rename -> {
                    if (documents?.size == 1) {
                        (activity as? ManagerActivity)?.showRenameDialog(documents[0])
                    }
                    clearSelections()
                    hideMultipleSelect()
                }
                R.id.cab_menu_copy -> {
                    documents?.map { it.handle }?.let {
                        handleList.addAll(it)
                        val nC = NodeController(activity)
                        nC.chooseLocationToCopyNodes(handleList)
                    }
                    clearSelections()
                    hideMultipleSelect()
                }
                R.id.cab_menu_move -> {
                    val nC = NodeController(activity)
                    documents?.map { it.handle }?.let {
                        handleList.addAll(it)
                        nC.chooseLocationToMoveNodes(handleList)
                    }
                    clearSelections()
                    hideMultipleSelect()
                }
                R.id.cab_menu_share_folder -> {
                    documents?.filter { it.isFolder }
                        ?.map { it.handle }?.let {
                            handleList.addAll(it)
                            val nC = NodeController(activity)
                            fileBackupManager?.let { backupManager ->
                                if (!backupManager.shareBackupFolderInMenu(
                                        nC,
                                        handleList, backupManager.actionBackupNodeCallback
                                    )
                                ) {
                                    nC.selectContactToShareFolders(handleList)
                                }
                            }
                        }
                    clearSelections()
                    hideMultipleSelect()
                }
                R.id.cab_menu_share_out -> {
                    documents?.let { shareNodes(requireContext(), it) }
                    clearSelections()
                    hideMultipleSelect()
                }
                R.id.cab_menu_share_link, R.id.cab_menu_edit_link -> {
                    Timber.d("Public link option")
                    if (documents?.isNotEmpty() == true) {
                        (activity as? ManagerActivity)?.showGetLinkActivity(documents)
                    } else {
                        Timber.w("The selected node is NULL")
                    }
                    clearSelections()
                    hideMultipleSelect()
                }
                R.id.cab_menu_remove_link -> {
                    Timber.d("Remove public link option")
                    if (documents?.isNotEmpty() == true) {
                        (activity as? ManagerActivity)?.showConfirmationRemovePublicLink(documents[0])
                    } else {
                        Timber.w("The selected node is NULL")
                    }
                    clearSelections()
                    hideMultipleSelect()
                }
                R.id.cab_menu_send_to_chat -> {
                    Timber.d("Send files to chat")
                    (activity as? ManagerActivity)?.attachNodesToChats(adapter?.arrayListSelectedNodes)
                    clearSelections()
                    hideMultipleSelect()
                }
                R.id.cab_menu_trash -> {
                    documents?.map { it.handle }?.let {
                        handleList.addAll(it)
                        (activity as? ManagerActivity)?.askConfirmationMoveToRubbish(handleList)
                    }
                }
                R.id.cab_menu_select_all -> {
                    selectAll()
                }
                R.id.cab_menu_clear_selection -> {
                    clearSelections()
                    hideMultipleSelect()
                }
                R.id.cab_menu_remove_share -> (activity as? ManagerActivity)?.showConfirmationRemoveAllSharingContacts(
                    documents)
            }
            return true
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            Timber.d("onCreateActionMode")
            val inflater = mode.menuInflater
            inflater.inflate(R.menu.cloud_storage_action, menu)
            (activity as? ManagerActivity)?.hideFabButton()
            (activity as? ManagerActivity)?.showHideBottomNavigationView(true)
            checkScroll()
            return true
        }

        override fun onDestroyActionMode(arg0: ActionMode) {
            Timber.d("onDestroyActionMode")
            clearSelections()
            adapter?.isMultipleSelect = false
            (activity as? ManagerActivity)?.showFabButton()
            (activity as? ManagerActivity)?.showHideBottomNavigationView(false)
            checkScroll()
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            Timber.d("onPrepareActionMode")
            val selected = adapter?.selectedNodes?.takeUnless { it.isEmpty() } ?: return false
            menu.findItem(R.id.cab_menu_share_link).title =
                StringResourcesUtils.getQuantityString(R.plurals.get_links, selected.size)
            val control = CloudStorageOptionControlUtil.Control()
            if (selected.size == 1) {
                val megaNode = selected[0]
                if (!megaNode.isTakenDown) {
                    if (megaApi.checkAccessErrorExtended(megaNode,
                            MegaShare.ACCESS_OWNER).errorCode
                        == MegaError.API_OK
                    ) {
                        if (megaNode.isExported) {
                            control.manageLink().setVisible(true).showAsAction =
                                MenuItem.SHOW_AS_ACTION_ALWAYS
                            control.removeLink().isVisible = true
                        } else {
                            control.link.setVisible(true).showAsAction =
                                MenuItem.SHOW_AS_ACTION_ALWAYS
                        }
                    }
                }
                if (megaApi.checkAccessErrorExtended(megaNode, MegaShare.ACCESS_FULL).errorCode
                    == MegaError.API_OK
                ) {
                    control.rename().isVisible = true
                }
            } else if (allHaveOwnerAccessAndNotTakenDown(selected)) {
                control.link.setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
            }
            var showSendToChat = true
            var showShareFolder = true
            var showTrash = true
            var showRemoveShare = true
            var showShareOut = true
            var showCopy = true
            var showDownload = true
            var mediaCounter = 0
            for (node in selected) {
                if (node?.isTakenDown == true) {
                    showShareOut = false
                    showCopy = false
                    showDownload = false
                }
                if (!node.isFile || node.isTakenDown) {
                    showSendToChat = false
                } else if (node.isFile) {
                    val nodeMime = MimeTypeList.typeForName(
                        node.name)
                    if (nodeMime.isImage || nodeMime.isVideo) {
                        mediaCounter++
                    }
                }
                if (node.isTakenDown || !node.isFolder || isOutShare(node) && selected.size > 1) {
                    showShareFolder = false
                }
                if (megaApi.checkMoveErrorExtended(node, megaApi.rubbishNode).errorCode
                    != MegaError.API_OK
                ) {
                    showTrash = false
                }
                if (node.isTakenDown || !node.isFolder || !isOutShare(node)) {
                    showRemoveShare = false
                }
            }
            if (showSendToChat) {
                control.sendToChat().setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
            }
            if (showShareFolder) {
                control.shareFolder().setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
            }
            if (showRemoveShare) {
                control.removeShare().isVisible = true
            }
            control.trash().isVisible = showTrash
            if (showShareOut) {
                control.shareOut().isVisible = true
                if (control.alwaysActionCount() < CloudStorageOptionControlUtil.MAX_ACTION_COUNT) {
                    control.shareOut().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                }
            }
            control.move().isVisible = true
            if (selected.size > 1
                && control.alwaysActionCount() < CloudStorageOptionControlUtil.MAX_ACTION_COUNT
            ) {
                control.move().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
            }
            if (showCopy) {
                control.copy().isVisible = true
                if (control.alwaysActionCount() < CloudStorageOptionControlUtil.MAX_ACTION_COUNT) {
                    control.copy().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                }
            }
            if (!showDownload) {
                control.saveToDevice().isVisible = false
            }
            adapter?.let {
                control.selectAll().isVisible = (selected.size
                        < it.itemCount - it.placeholderCount)
            }
            CloudStorageOptionControlUtil.applyControl(menu, control)
            return true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        downloadLocationDefaultPath = FileUtil.getDownloadLocation()
        lastPositionStack = Stack()
        super.onCreate(savedInstanceState)
        Timber.d("After onCreate called super")
    }

    fun checkScroll() {
        if (recyclerView == null) return
        val visible =
            (adapter?.isMultipleSelect == true
                    || transfersManagement.isTransferOverQuotaBannerShown
                    || recyclerView?.canScrollVertically(
                -1) == true && recyclerView?.visibility == View.VISIBLE)
        (activity as? ManagerActivity)?.changeAppBarElevation(visible)
    }

    @Suppress("DEPRECATION")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        Timber.d("onCreateView")
        if (!isAdded) {
            return null
        }
        val sortByHeaderViewModel = ViewModelProvider(this)[SortByHeaderViewModel::class.java]
        sortByHeaderViewModel.showDialogEvent.observe(viewLifecycleOwner,
            EventObserver { showSortByPanel() })

        managerViewModel.updateBrowserNodes.observe(viewLifecycleOwner,
            EventObserver { nodes: List<MegaNode> ->
                hideMultipleSelect()
                setNodes(nodes.toMutableList())
                recyclerView?.invalidate()
            }
        )
        LiveEventBus.get(EVENT_SHOW_MEDIA_DISCOVERY, Unit::class.java)
            .observe(this) { showMediaDiscovery() }
        Timber.d("Fragment ADDED")
        if (aB == null) {
            aB = (activity as? AppCompatActivity)?.supportActionBar
        }
        if (megaApi.rootNode == null) {
            return null
        }
        display = activity?.windowManager?.defaultDisplay
        outMetrics = DisplayMetrics()
        display?.getMetrics(outMetrics)
        density = resources.displayMetrics.density
        getNodes()
        (activity as? ManagerActivity)?.setToolbarTitle()
        (activity as? ManagerActivity)?.supportInvalidateOptionsMenu()
        val v: View
        if ((activity as? ManagerActivity)?.isList == true) {
            Timber.d("isList")
            v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false)
            recyclerView = v.findViewById(R.id.file_list_view_browser)
            fastScroller = v.findViewById(R.id.fastscroll)
            recyclerView?.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics))
            recyclerView?.clipToPadding = false
            mLayoutManager = LinearLayoutManager(context)
            mLayoutManager?.orientation = LinearLayoutManager.VERTICAL
            recyclerView?.layoutManager = mLayoutManager
            recyclerView?.setHasFixedSize(true)
            recyclerView?.itemAnimator = Util.noChangeRecyclerViewItemAnimator()
            recyclerView?.addItemDecoration(PositionDividerItemDecoration(requireContext(),
                resources.displayMetrics))
            recyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    checkScroll()
                }
            })
            emptyImageView = v.findViewById(R.id.file_list_empty_image)
            emptyTextView = v.findViewById(R.id.file_list_empty_text)
            emptyTextViewFirst = v.findViewById(R.id.file_list_empty_text_first)
            if (adapter == null) {
                adapter = MegaNodeAdapter(activity,
                    this,
                    _nodes,
                    managerViewModel.getSafeBrowserParentHandle(),
                    recyclerView,
                    Constants.FILE_BROWSER_ADAPTER,
                    MegaNodeAdapter.ITEM_VIEW_TYPE_LIST,
                    sortByHeaderViewModel)
            } else {
                (activity as? ManagerActivity)?.parentHandleBrowser?.let {
                    adapter?.parentHandle = it
                }
                adapter?.setListFragment(recyclerView)
                adapter?.adapterType = MegaNodeAdapter.ITEM_VIEW_TYPE_LIST
            }
            adapter?.isMultipleSelect = false
            recyclerView?.adapter = adapter
            fastScroller?.setRecyclerView(recyclerView)
            setNodes(_nodes)
            if (adapter?.itemCount == 0) {
                Timber.d("itemCount is 0")
                recyclerView?.visibility = View.GONE
                emptyImageView?.visibility = View.VISIBLE
                emptyTextView?.visibility = View.VISIBLE
            } else {
                Timber.d("itemCount is %s", adapter?.itemCount)
                recyclerView?.visibility = View.VISIBLE
                emptyImageView?.visibility = View.GONE
                emptyTextView?.visibility = View.GONE
            }
        } else {
            Timber.d("Grid View")
            v = inflater.inflate(R.layout.fragment_filebrowsergrid, container, false)
            recyclerView = v.findViewById(R.id.file_grid_view_browser)
            fastScroller = v.findViewById(R.id.fastscroll)
            recyclerView?.setPadding(0, 0, 0, Util.scaleHeightPx(80, outMetrics))
            recyclerView?.clipToPadding = false
            recyclerView?.setHasFixedSize(true)
            gridLayoutManager = recyclerView?.layoutManager as CustomizedGridLayoutManager?
            recyclerView?.itemAnimator = DefaultItemAnimator()
            recyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    checkScroll()
                }
            })
            emptyImageView = v.findViewById(R.id.file_grid_empty_image)
            emptyTextView = v.findViewById(R.id.file_grid_empty_text)
            emptyTextViewFirst = v.findViewById(R.id.file_grid_empty_text_first)
            if (adapter == null) {
                adapter = MegaNodeAdapter(activity,
                    this,
                    _nodes,
                    managerViewModel.getSafeBrowserParentHandle(),
                    recyclerView,
                    Constants.FILE_BROWSER_ADAPTER,
                    MegaNodeAdapter.ITEM_VIEW_TYPE_GRID,
                    sortByHeaderViewModel)
            } else {
                (activity as? ManagerActivity)?.parentHandleBrowser?.let {
                    adapter?.parentHandle = it
                }
                adapter?.setListFragment(recyclerView)
                adapter?.adapterType = MegaNodeAdapter.ITEM_VIEW_TYPE_GRID
            }
            gridLayoutManager?.let {
                it.spanSizeLookup =
                    adapter?.getSpanSizeLookup(it.spanCount)
            }
            adapter?.isMultipleSelect = false
            recyclerView?.adapter = adapter
            fastScroller?.setRecyclerView(recyclerView)
            setNodes(_nodes)
            if (adapter?.itemCount == 0) {
                recyclerView?.visibility = View.GONE
                emptyImageView?.visibility = View.VISIBLE
                emptyTextView?.visibility = View.VISIBLE
            } else {
                recyclerView?.visibility = View.VISIBLE
                emptyImageView?.visibility = View.GONE
                emptyTextView?.visibility = View.GONE
            }
        }
        transferOverQuotaBanner = v.findViewById(R.id.transfer_over_quota_banner)
        transferOverQuotaBannerText = v.findViewById(R.id.banner_content_text)
        v.findViewById<View>(R.id.banner_dismiss_button)
            .setOnClickListener { hideTransferOverQuotaBanner() }
        v.findViewById<View>(R.id.banner_upgrade_button).setOnClickListener {
            hideTransferOverQuotaBanner()
            (activity as? ManagerActivity)?.navigateToUpgradeAccount()
        }
        setTransferOverQuotaBannerVisibility()
        selectNewlyAddedNodes()
        if ((activity as? ManagerActivity)?.viewInFolderNode != null) {
            animateNode(_nodes)
        }
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeDragSupportEvents(viewLifecycleOwner,
            recyclerView,
            Constants.VIEWER_FROM_FILE_BROWSER)
    }

    override fun onAttach(context: Context) {
        Timber.d("onAttach")
        super.onAttach(context)
        aB = (context as AppCompatActivity).supportActionBar
        fileBackupManager = FileBackupManager(
            requireActivity(),
            object : ActionBackupListener {
                override fun actionBackupResult(
                    actionType: Int,
                    operationType: Int,
                    result: MoveRequestResult?,
                    handle: Long,
                ) {
                    Timber.d("Nothing to do for actionType = %s",
                        actionType)
                }
            }
        )
    }

    override fun onDestroy() {
        adapter?.clearTakenDownDialog()
        super.onDestroy()
    }

    override fun getAdapter(): RotatableAdapter? {
        return adapter
    }

    private fun getNodes() {
        val parentHandleBrowser = managerViewModel.getSafeBrowserParentHandle()
        if (parentHandleBrowser == -1L || parentHandleBrowser == megaApi.rootNode.handle) {
            Timber.w("After consulting... the parent keeps -1 or ROOTNODE: %s", parentHandleBrowser)
            _nodes = megaApi.getChildren(megaApi.rootNode,
                sortOrderIntMapper(managerViewModel.getOrder()))
            mediaHandle = megaApi.rootNode.handle
        } else {
            val parentNode = megaApi.getNodeByHandle(parentHandleBrowser)
            _nodes =
                megaApi.getChildren(parentNode, sortOrderIntMapper(managerViewModel.getOrder()))
            mediaHandle = parentHandleBrowser
        }
    }

    fun refreshNodes() {
        if (adapter != null) {
            getNodes()
            adapter?.setNodes(_nodes)
        }
    }

    fun openFile(node: MegaNode, position: Int) {
        if (MimeTypeList.typeForName(node.name).isImage) {
            val intent = getIntentForParentNode(
                requireContext(),
                megaApi.getParentNode(node).handle,
                managerViewModel.getOrder(),
                node.handle
            )
            putThumbnailLocation(intent,
                recyclerView,
                position,
                Constants.VIEWER_FROM_FILE_BROWSER,
                adapter)
            startActivity(intent)
            (activity as? ManagerActivity)?.overridePendingTransition(0, 0)
        } else if (MimeTypeList.typeForName(node.name).isVideoReproducible || MimeTypeList.typeForName(
                node.name).isAudio
        ) {
            val mimeType = MimeTypeList.typeForName(node.name).type
            val mediaIntent: Intent
            val internalIntent: Boolean
            var opusFile = false
            if (MimeTypeList.typeForName(node.name).isVideoNotSupported || MimeTypeList.typeForName(
                    node.name).isAudioNotSupported
            ) {
                mediaIntent = Intent(Intent.ACTION_VIEW)
                internalIntent = false
                val s = node.name.split("\\.").toTypedArray()
                if (s.size > 1 && s[s.size - 1] == "opus") {
                    opusFile = true
                }
            } else {
                mediaIntent = Util.getMediaIntent(context, node.name)
                internalIntent = true
            }
            mediaIntent.putExtra("position", position)
            mediaIntent.putExtra("placeholder", adapter?.placeholderCount)
            val megaNode = megaApi.getParentNode(node)
            if (megaNode != null) {
                if (megaNode.type == MegaNode.TYPE_ROOT) {
                    mediaIntent.putExtra("parentNodeHandle", -1L)
                } else {
                    mediaIntent.putExtra("parentNodeHandle", megaApi.getParentNode(node).handle)
                }
            }
            mediaIntent.putExtra("orderGetChildren", managerViewModel.getOrder())
            mediaIntent.putExtra("adapterType", Constants.FILE_BROWSER_ADAPTER)
            putThumbnailLocation(mediaIntent,
                recyclerView,
                position,
                Constants.VIEWER_FROM_FILE_BROWSER,
                adapter)
            mediaIntent.putExtra("FILENAME", node.name)
            val localPath = FileUtil.getLocalFile(node)
            if (localPath != null) {
                val mediaFile = File(localPath)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().path)) {
                    Timber.d("itemClick:FileProviderOption")
                    val mediaFileUri = FileProvider.getUriForFile(
                        requireContext(),
                        "mega.privacy.android.app.providers.fileprovider",
                        mediaFile)
                    if (mediaFileUri == null) {
                        Timber.d("itemClick:ERROR:NULLmediaFileUri")
                        (activity as? ManagerActivity)?.showSnackbar(Constants.SNACKBAR_TYPE,
                            getString(
                                R.string.general_text_error),
                            -1)
                    } else {
                        mediaIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(
                            node.name).type)
                    }
                } else {
                    val mediaFileUri = Uri.fromFile(mediaFile)
                    if (mediaFileUri == null) {
                        Timber.e("itemClick:ERROR:NULLmediaFileUri")
                        (activity as? ManagerActivity)?.showSnackbar(Constants.SNACKBAR_TYPE,
                            getString(
                                R.string.general_text_error),
                            -1)
                    } else {
                        mediaIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(
                            node.name).type)
                    }
                }
                mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                Timber.d("itemClick:localPathNULL")
                if (megaApi.httpServerIsRunning() == 0) {
                    megaApi.httpServerStart()
                    mediaIntent.putExtra(Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
                } else {
                    Timber.w("itemClick:ERROR:httpServerAlreadyRunning")
                }
                val mi = ActivityManager.MemoryInfo()
                val activityManager =
                    context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.getMemoryInfo(mi)
                if (mi.totalMem > Constants.BUFFER_COMP) {
                    Timber.d("itemClick:total mem: %d allocate 32 MB", mi.totalMem)
                    megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB)
                } else {
                    Timber.d("itemClick:total mem: %d allocate 16 MB", mi.totalMem)
                    megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB)
                }
                val url = megaApi.httpServerGetLocalLink(node)
                if (url != null) {
                    val parsedUri = Uri.parse(url)
                    if (parsedUri != null) {
                        mediaIntent.setDataAndType(parsedUri, mimeType)
                    } else {
                        Timber.e("itemClick:ERROR:httpServerGetLocalLink")
                        (activity as? ManagerActivity)?.showSnackbar(Constants.SNACKBAR_TYPE,
                            getString(
                                R.string.general_text_error),
                            -1)
                    }
                } else {
                    Timber.e("itemClick:ERROR:httpServerGetLocalLink")
                    (activity as? ManagerActivity)?.showSnackbar(Constants.SNACKBAR_TYPE, getString(
                        R.string.general_text_error), -1)
                }
            }
            mediaIntent.putExtra("HANDLE", node.handle)
            if (opusFile) {
                mediaIntent.setDataAndType(mediaIntent.data, "audio/*")
            }
            if (internalIntent) {
                context?.startActivity(mediaIntent)
            } else {
                Timber.d("itemClick:externalIntent")
                if (MegaApiUtils.isIntentAvailable(context, mediaIntent)) {
                    context?.startActivity(mediaIntent)
                } else {
                    Timber.w("itemClick:noAvailableIntent")
                    (activity as? ManagerActivity)?.showSnackbar(Constants.SNACKBAR_TYPE, getString(
                        R.string.intent_not_available), -1)
                    (activity as? ManagerActivity)?.saveNodesToDevice(listOf<MegaNode?>(node),
                        true, false, false, false)
                }
            }
            (activity as? ManagerActivity)?.overridePendingTransition(0, 0)
        } else if (MimeTypeList.typeForName(node.name).isURL) {
            manageURLNode(requireContext(), megaApi, node)
        } else if (MimeTypeList.typeForName(node.name).isPdf) {
            Timber.d("itemClick:isFile:isPdf")
            val mimeType = MimeTypeList.typeForName(node.name).type
            val pdfIntent = Intent(context, PdfViewerActivity::class.java)
            pdfIntent.putExtra("inside", true)
            pdfIntent.putExtra("adapterType", Constants.FILE_BROWSER_ADAPTER)
            val localPath = FileUtil.getLocalFile(node)
            if (localPath != null) {
                val mediaFile = File(localPath)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().path)) {
                    pdfIntent.setDataAndType(FileProvider.getUriForFile(
                        requireContext(),
                        "mega.privacy.android.app.providers.fileprovider",
                        mediaFile),
                        MimeTypeList.typeForName(
                            node.name).type)
                } else {
                    pdfIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(
                        node.name).type)
                }
                pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                if (megaApi.httpServerIsRunning() == 0) {
                    megaApi.httpServerStart()
                    pdfIntent.putExtra(Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
                }
                val mi = ActivityManager.MemoryInfo()
                val activityManager =
                    requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.getMemoryInfo(mi)
                if (mi.totalMem > Constants.BUFFER_COMP) {
                    Timber.d("Total mem: %d allocate 32 MB", mi.totalMem)
                    megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB)
                } else {
                    Timber.d("Total mem: %d allocate 16 MB", mi.totalMem)
                    megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB)
                }
                val url = megaApi.httpServerGetLocalLink(node)
                if (url != null) {
                    val parsedUri = Uri.parse(url)
                    if (parsedUri != null) {
                        pdfIntent.setDataAndType(parsedUri, mimeType)
                    } else {
                        Timber.e("itemClick:ERROR:httpServerGetLocalLink")
                        (activity as? ManagerActivity)?.showSnackbar(Constants.SNACKBAR_TYPE,
                            getString(
                                R.string.general_text_error),
                            -1)
                    }
                } else {
                    Timber.e("itemClick:ERROR:httpServerGetLocalLink")
                    (activity as? ManagerActivity)?.showSnackbar(Constants.SNACKBAR_TYPE, getString(
                        R.string.general_text_error), -1)
                }
            }
            pdfIntent.putExtra("HANDLE", node.handle)
            putThumbnailLocation(pdfIntent,
                recyclerView,
                position,
                Constants.VIEWER_FROM_FILE_BROWSER,
                adapter)
            if (MegaApiUtils.isIntentAvailable(context, pdfIntent)) {
                context?.startActivity(pdfIntent)
            } else {
                Toast.makeText(context,
                    requireContext().resources.getString(R.string.intent_not_available),
                    Toast.LENGTH_LONG).show()
                (activity as? ManagerActivity)?.saveNodesToDevice(listOf<MegaNode?>(node),
                    true, false, false, false)
            }
            (activity as? ManagerActivity)?.overridePendingTransition(0, 0)
        } else if (MimeTypeList.typeForName(node.name).isOpenableTextFile(
                node.size)
        ) {
            manageTextFileIntent(requireContext(), node, Constants.FILE_BROWSER_ADAPTER)
        } else {
            Timber.d("itemClick:isFile:otherOption")

            val managerActivity = activity as? ManagerActivity ?: return
            onNodeTapped(requireActivity(),
                node,
                { saveNode: MegaNode? -> (activity as? ManagerActivity)?.saveNodeByTap(saveNode) },
                managerActivity,
                managerActivity)
        }
    }

    fun itemClick(position: Int) {
        Timber.d("item click position: %s", position)
        if (adapter?.isMultipleSelect == true) {
            Timber.d("itemClick:multiselectON")
            adapter?.toggleSelection(position)
            val selectedNodes = adapter?.selectedNodes
            if (selectedNodes?.isNotEmpty() == true) {
                updateActionModeTitle()
            }
        } else {
            Timber.d("itemClick:multiselectOFF")
            val clickedNode = _nodes.getOrNull(position)
            if (clickedNode?.isFolder == true) {
                mediaHandle = clickedNode.handle
                managerViewModel.setBrowserParentHandle(clickedNode.handle)
                val childNodes: List<MegaNode> = megaApi.getChildren(clickedNode,
                    sortOrderIntMapper(managerViewModel.getOrder()))
                if (fileBrowserViewModel.shouldEnterMDMode(childNodes)) {
                    showMediaDiscovery()
                } else {
                    var lastFirstVisiblePosition: Int?
                    if ((activity as? ManagerActivity)?.isList == true) {
                        lastFirstVisiblePosition =
                            mLayoutManager?.findFirstCompletelyVisibleItemPosition()
                        Timber.d("lastFirstVisiblePosition: %s", lastFirstVisiblePosition)
                    } else {
                        lastFirstVisiblePosition =
                            (recyclerView as NewGridRecyclerView?)?.findFirstCompletelyVisibleItemPosition()
                        if (lastFirstVisiblePosition == -1) {
                            Timber.d("Completely -1 then find just visible position")
                            lastFirstVisiblePosition =
                                (recyclerView as NewGridRecyclerView?)?.findFirstVisibleItemPosition()
                        }
                    }
                    Timber.d("Push to stack $lastFirstVisiblePosition position")
                    lastFirstVisiblePosition?.let { lastPositionStack?.push(it) }
                    setFolderInfoNavigation(clickedNode)
                }
            } else {
                //Is file
                openFile(_nodes[position], position)
            }
        }
    }

    override fun multipleItemClick(position: Int) {
        adapter?.toggleSelection(position)
    }

    override fun reselectUnHandledSingleItem(position: Int) {
        adapter?.filClicked(position)
    }

    @Suppress("DEPRECATION")
    fun setFolderInfoNavigation(n: MegaNode?) {
        (activity as? ManagerActivity)?.supportInvalidateOptionsMenu()
        (activity as? ManagerActivity)?.setToolbarTitle()
        adapter?.parentHandle = managerViewModel.getSafeBrowserParentHandle()
        _nodes = megaApi.getChildren(n, sortOrderIntMapper(managerViewModel.getOrder()))
        adapter?.setNodes(_nodes)
        recyclerView?.scrollToPosition(0)
        visibilityFastScroller()

        //If folder has no files
        if (adapter?.itemCount == 0) {
            recyclerView?.visibility = View.GONE
            emptyImageView?.visibility = View.VISIBLE
            emptyTextView?.visibility = View.VISIBLE
            if (megaApi.rootNode != null && megaApi.rootNode.handle == n?.handle) {
                if (requireContext().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    emptyImageView?.setImageResource(R.drawable.empty_cloud_drive_landscape)
                } else {
                    emptyImageView?.setImageResource(R.drawable.empty_cloud_drive_portrait)
                }
                var textToShow =
                    requireContext().getString(R.string.context_empty_cloud_drive).uppercase(
                        Locale.getDefault())
                try {
                    textToShow = textToShow.replace("[A]", "<font color=\'"
                            + getColorHexString(requireContext(), R.color.grey_900_grey_100)
                            + "\'>")
                    textToShow = textToShow.replace("[/A]", "</font>")
                    textToShow = textToShow.replace("[B]", "<font color=\'"
                            + getColorHexString(requireContext(), R.color.grey_300_grey_600)
                            + "\'>")
                    textToShow = textToShow.replace("[/B]", "</font>")
                } catch (e: Exception) {
                    Timber.e(e)
                }
                val result = HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY)
                emptyTextViewFirst?.text = result
            } else {
                if (context?.resources?.configuration?.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    emptyImageView?.setImageResource(R.drawable.empty_folder_landscape)
                } else {
                    emptyImageView?.setImageResource(R.drawable.empty_folder_portrait)
                }
                var textToShow = requireContext().getString(R.string.file_browser_empty_folder_new)
                try {
                    textToShow = textToShow.replace("[A]", "<font color=\'"
                            + getColorHexString(requireContext(), R.color.grey_900_grey_100)
                            + "\'>")
                    textToShow = textToShow.replace("[/A]", "</font>")
                    textToShow = textToShow.replace("[B]", "<font color=\'"
                            + getColorHexString(requireContext(), R.color.grey_300_grey_600)
                            + "\'>")
                    textToShow = textToShow.replace("[/B]", "</font>")
                } catch (e: Exception) {
                    Timber.e(e)
                }
                val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY)
                } else {
                    Html.fromHtml(textToShow)
                }
                emptyTextViewFirst?.text = result
            }
        } else {
            recyclerView?.visibility = View.VISIBLE
            emptyImageView?.visibility = View.GONE
            emptyTextView?.visibility = View.GONE
        }
        checkScroll()
    }

    fun showSelectMenuItem(): Boolean {
        Timber.d("showSelectMenuItem")
        return adapter?.isMultipleSelect == true
    }

    fun selectAll() {
        Timber.d("selectAll")
        if (adapter != null) {
            if (adapter?.isMultipleSelect == true) {
                adapter?.selectAll()
            } else {
                adapter?.isMultipleSelect = true
                adapter?.selectAll()
                actionMode = (activity as? AppCompatActivity)?.startSupportActionMode(
                    ActionBarCallBack())
            }
            Handler(Looper.getMainLooper()).post { updateActionModeTitle() }
        }
    }

    /*
     * Clear all selected items
     */
    private fun clearSelections() {
        if (adapter?.isMultipleSelect == true) {
            adapter?.clearSelections()
        }
    }

    override fun updateActionModeTitle() {
        Timber.d("updateActionModeTitle")
        if (actionMode == null || activity == null) {
            Timber.w("RETURN: null values")
            return
        }
        val documents = adapter?.selectedNodes
        val files = documents?.count { it.isFile } ?: 0
        val folders = documents?.count { it.isFolder } ?: 0

        val title: String
        val sum = files + folders
        title = if (files == 0 && folders == 0) {
            sum.toString()
        } else if (files == 0) {
            folders.toString()
        } else if (folders == 0) {
            files.toString()
        } else {
            sum.toString()
        }
        actionMode?.title = title
        try {
            actionMode?.invalidate()
        } catch (e: NullPointerException) {
            e.printStackTrace()
            Timber.e(e, "Invalidate error")
        }
    }

    /*
     * Disable selection
     */
    fun hideMultipleSelect() {
        Timber.d("hideMultipleSelect")
        adapter?.isMultipleSelect = false
        if (actionMode != null) {
            actionMode?.finish()
        }
    }

    @Suppress("DEPRECATION")
    fun onBackPressed(): Int {
        Timber.d("onBackPressed")
        if (adapter != null) {
            Timber.d("Parent Handle is: %s", managerViewModel.getSafeBrowserParentHandle())
            val managerActivity = activity as? ManagerActivity ?: return 0
            return if (managerActivity.comesFromNotifications && managerActivity.comesFromNotificationHandle == managerViewModel.getSafeBrowserParentHandle()) {
                managerActivity.comesFromNotifications = false
                managerActivity.comesFromNotificationHandle = -1
                managerActivity.selectDrawerItem(DrawerItem.NOTIFICATIONS)
                managerViewModel.setBrowserParentHandle(managerActivity.comesFromNotificationHandleSaved)
                managerActivity.comesFromNotificationHandleSaved = -1
                managerActivity.refreshCloudDrive()
                2
            } else {
                val parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(
                    managerViewModel.getSafeBrowserParentHandle()))
                if (parentNode != null) {
                    mediaHandle = parentNode.handle
                    recyclerView?.visibility = View.VISIBLE
                    emptyImageView?.visibility = View.GONE
                    emptyTextView?.visibility = View.GONE
                    managerViewModel.setBrowserParentHandle(parentNode.handle)
                    managerActivity.supportInvalidateOptionsMenu()
                    managerActivity.setToolbarTitle()
                    _nodes = megaApi.getChildren(parentNode,
                        sortOrderIntMapper(managerViewModel.getOrder()))
                    adapter?.setNodes(_nodes)
                    visibilityFastScroller()
                    var lastVisiblePosition = 0
                    lastPositionStack?.takeIf { it.isNotEmpty() }?.pop()?.let {
                        lastVisiblePosition = it
                        Timber.d("Pop of the stack $lastVisiblePosition position")
                    }
                    Timber.d("Scroll to %d position", lastVisiblePosition)
                    if (lastVisiblePosition >= 0) {
                        if (managerActivity.isList) {
                            mLayoutManager?.scrollToPositionWithOffset(lastVisiblePosition, 0)
                        } else {
                            gridLayoutManager?.scrollToPositionWithOffset(lastVisiblePosition, 0)
                        }
                    }
                    Timber.d("return 2")
                    2
                } else {
                    Timber.w("ParentNode is NULL")
                    0
                }
            }
        }
        return 0
    }

    fun scrollToFirstPosition() {
        if ((activity as? ManagerActivity)?.isList == true) {
            mLayoutManager?.scrollToPositionWithOffset(0, 0)
        } else {
            gridLayoutManager?.scrollToPositionWithOffset(0, 0)
        }
    }

    @Suppress("DEPRECATION")
    fun setNodes(nodes: MutableList<MegaNode>) {
        Timber.d("Nodes size: ${nodes.size}")
        visibilityFastScroller()
        this._nodes = nodes
        if (adapter != null) {
            adapter?.setNodes(nodes)
            if (adapter?.itemCount == 0) {
                recyclerView?.visibility = View.GONE
                emptyImageView?.visibility = View.VISIBLE
                emptyTextView?.visibility = View.VISIBLE
                if (megaApi.rootNode != null && megaApi.rootNode.handle == managerViewModel.getSafeBrowserParentHandle() || managerViewModel.getSafeBrowserParentHandle() == -1L) {
                    if (requireContext().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        emptyImageView?.setImageResource(R.drawable.empty_cloud_drive_landscape)
                    } else {
                        emptyImageView?.setImageResource(R.drawable.empty_cloud_drive_portrait)
                    }
                    var textToShow =
                        requireContext().getString(R.string.context_empty_cloud_drive).uppercase(
                            Locale.getDefault())
                    try {
                        textToShow = textToShow.replace("[A]", "<font color=\'"
                                + getColorHexString(requireContext(), R.color.grey_900_grey_100)
                                + "\'>")
                        textToShow = textToShow.replace("[/A]", "</font>")
                        textToShow = textToShow.replace("[B]", "<font color=\'"
                                + getColorHexString(requireContext(), R.color.grey_300_grey_600)
                                + "\'>")
                        textToShow = textToShow.replace("[/B]", "</font>")
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                    val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY)
                    } else {
                        Html.fromHtml(textToShow)
                    }
                    emptyTextViewFirst?.text = result
                } else {
                    if (requireContext().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        emptyImageView?.setImageResource(R.drawable.empty_folder_landscape)
                    } else {
                        emptyImageView?.setImageResource(R.drawable.empty_folder_portrait)
                    }
                    var textToShow =
                        requireContext().getString(R.string.file_browser_empty_folder_new)
                    try {
                        textToShow = textToShow.replace("[A]", "<font color=\'"
                                + getColorHexString(requireContext(), R.color.grey_900_grey_100)
                                + "\'>")
                        textToShow = textToShow.replace("[/A]", "</font>")
                        textToShow = textToShow.replace("[B]", "<font color=\'"
                                + getColorHexString(requireContext(), R.color.grey_300_grey_600)
                                + "\'>")
                        textToShow = textToShow.replace("[/B]", "</font>")
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                    val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY)
                    } else {
                        Html.fromHtml(textToShow)
                    }
                    emptyTextViewFirst?.text = result
                }
            } else {
                recyclerView?.visibility = View.VISIBLE
                emptyImageView?.visibility = View.GONE
                emptyTextView?.visibility = View.GONE
            }
        } else {
            Timber.w("Adapter is NULL")
        }
    }

    val isMultipleselect: Boolean
        get() = adapter?.isMultipleSelect == true
    val itemCount: Int
        get() = adapter?.itemCount ?: 0

    fun visibilityFastScroller() {
        if (adapter == null) {
            fastScroller?.visibility = View.GONE
        } else {
            if (itemCount < Constants.MIN_ITEMS_SCROLLBAR) {
                fastScroller?.visibility = View.GONE
            } else {
                fastScroller?.visibility = View.VISIBLE
            }
        }
    }

    //refresh list when item updated
    @SuppressLint("NotifyDataSetChanged")
    fun refresh(handle: Long) {
        if (handle == -1L) {
            return
        }
        updateNode(handle)
        adapter?.notifyDataSetChanged()
    }

    private fun updateNode(handle: Long) {
        val index = _nodes.indexOfFirst { it.handle == handle }
        _nodes[index] = megaApi.getNodeByHandle(handle)
    }

    /**
     * Sets the "transfer over quota" banner visibility.
     */
    fun setTransferOverQuotaBannerVisibility() {
        if (transfersManagement.isTransferOverQuotaBannerShown) {
            transferOverQuotaBanner?.visibility = View.VISIBLE
            transferOverQuotaBannerText?.text =
                context?.getString(R.string.current_text_depleted_transfer_overquota,
                    TimeUtils.getHumanizedTime(
                        megaApi.bandwidthOverquotaDelay))
            TimeUtils.createAndShowCountDownTimer(R.string.current_text_depleted_transfer_overquota,
                transferOverQuotaBanner,
                transferOverQuotaBannerText)
        } else {
            transferOverQuotaBanner?.visibility = View.GONE
        }
    }

    /**
     * Hides the "transfer over quota" banner.
     */
    private fun hideTransferOverQuotaBanner() {
        transfersManagement.isTransferOverQuotaBannerShown = false
        setTransferOverQuotaBannerVisibility()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        fileBackupManager?.let {
            backupWarningDialog = it.backupWarningDialog
            if (backupWarningDialog != null && backupWarningDialog?.isShowing == true) {
                backupHandleList = it.backupHandleList
                backupNodeHandle = it.backupNodeHandle
                backupNodeType = it.backupNodeType
                backupActionType = it.backupActionType
                backupDialogType = it.backupDialogType
                if (backupHandleList != null) {
                    outState.putSerializable(BACKUP_HANDLED_ITEM, backupHandleList)
                }
                backupNodeHandle?.let { backupNode ->
                    outState.putLong(BACKUP_HANDLED_NODE,
                        backupNode)
                }
                outState.putInt(BACKUP_NODE_TYPE, backupNodeType)
                outState.putInt(BACKUP_ACTION_TYPE, backupActionType)
                outState.putInt(BACKUP_DIALOG_WARN, backupDialogType)
                backupWarningDialog?.dismiss()
            }
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            backupHandleList =
                savedInstanceState.serializable(BACKUP_HANDLED_ITEM) as ArrayList<Long>?
            backupNodeHandle = savedInstanceState.getLong(BACKUP_HANDLED_NODE, -1)
            backupNodeType = savedInstanceState.getInt(BACKUP_NODE_TYPE, -1)
            backupActionType = savedInstanceState.getInt(BACKUP_ACTION_TYPE, -1)
            backupDialogType = savedInstanceState.getInt(BACKUP_DIALOG_WARN, -1)
            when (backupDialogType) {
                0 -> {
                    val backupNode = backupNodeHandle?.let { megaApi.getNodeByHandle(it) } ?: return
                    fileBackupManager?.actionBackupNodeCallback?.let {
                        fileBackupManager?.actWithBackupTips(
                            backupHandleList,
                            backupNode,
                            backupNodeType,
                            backupActionType,
                            it
                        )
                    }
                }
                1 -> {
                    val backupNode = backupNodeHandle?.let { megaApi.getNodeByHandle(it) } ?: return
                    fileBackupManager?.defaultActionBackupNodeCallback?.let {
                        fileBackupManager?.confirmationActionForBackup(
                            backupHandleList,
                            backupNode,
                            backupNodeType,
                            backupActionType,
                            it
                        )
                    }
                }
                else -> {
                    Timber.d("Backup warning dialog is not show")
                }
            }
        }
    }

    /**
     * Get the nodes for operation of file backup
     *
     * @return the list of selected nodes
     */
    val nodeList: List<MegaNode>
        get() = _nodes

    fun showMediaDiscovery() {
        activity?.lifecycleScope?.launch {
            val newMediaDiscoveryEnable = getFeatureFlag(AppFeatures.NewMediaDiscovery)

            val f = if (newMediaDiscoveryEnable) {
                NewMediaDiscoveryFragment.getNewInstance(mediaHandle)
            } else {
                MediaDiscoveryFragment.getInstance(mediaHandle)
            }
            (activity as? ManagerActivity)?.skipToMediaDiscoveryFragment(f, mediaHandle)
        }
    }

    /**
     * If user navigates from notification about new nodes added to shared folder select all nodes and scroll to the first node in the list
     */
    private fun selectNewlyAddedNodes() {
        val positions =
            (activity as? ManagerActivity)?.getPositionsList(_nodes)?.takeUnless { it.isEmpty() }
                ?: return
        activateActionMode()
        positions.forEach {
            if (isMultipleselect) {
                adapter?.toggleSelection(it)
            }
        }
        val selectedNodes = adapter?.selectedNodes
        if (selectedNodes?.isNotEmpty() == true) {
            updateActionModeTitle()
        }
        recyclerView?.scrollToPosition(positions.minOrNull() ?: 0)
    }

    private var nodePosition = 0

    /**
     * When user tap View in Folder option from Recents tab, animate the node once view is loaded
     */
    fun animateNode(nodes: List<MegaNode>) {
        val node = (activity as? ManagerActivity)?.viewInFolderNode
        nodePosition = nodes.indexOfFirst { it.handle == node?.handle }.coerceAtLeast(0)

        //Scroll the position to the 3rd position before of the target position.
        recyclerView?.scrollToPosition(nodePosition - 3)
        recyclerView?.postDelayed({
            val holder = recyclerView?.findViewHolderForAdapterPosition(nodePosition)
            if (null != holder) {
                val animFadeIn = AnimationUtils.loadAnimation(
                    context?.applicationContext, R.anim.fade_in)
                animFadeIn.duration = DURATION_ANIMATION.toLong()
                holder.itemView.startAnimation(animFadeIn)
            }
        }, DELAY_RECYCLERVIEW_POST.toLong())
    }

    companion object {
        @JvmStatic
        fun newInstance(): FileBrowserFragment {
            Timber.d("newInstance")
            return FileBrowserFragment()
        }

        private const val DURATION_ANIMATION = 1000
        private const val DELAY_RECYCLERVIEW_POST = 500
    }
}