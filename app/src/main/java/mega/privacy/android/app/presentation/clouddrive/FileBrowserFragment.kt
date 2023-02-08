package mega.privacy.android.app.presentation.clouddrive

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
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
import androidx.annotation.ColorRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
import mega.privacy.android.app.databinding.FragmentFilebrowsergridBinding
import mega.privacy.android.app.databinding.FragmentFilebrowserlistBinding
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.imageviewer.ImageViewerActivity.Companion.getIntentForParentNode
import mega.privacy.android.app.interfaces.ActionBackupListener
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.PdfViewerActivity
import mega.privacy.android.app.main.adapters.MegaNodeAdapter
import mega.privacy.android.app.main.adapters.RotatableAdapter
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.managerSections.RotatableFragment
import mega.privacy.android.app.presentation.extensions.serializable
import mega.privacy.android.app.presentation.manager.ManagerViewModel
import mega.privacy.android.app.presentation.photos.mediadiscovery.MediaDiscoveryFragment
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
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
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import timber.log.Timber
import java.io.File
import java.util.Locale
import javax.inject.Inject

/**
 * Fragment is for File Browser
 */
@AndroidEntryPoint
class FileBrowserFragment : RotatableFragment() {

    @Inject
    lateinit var transfersManagement: TransfersManagement

    @Inject
    @MegaApi
    lateinit var megaApi: MegaApiAndroid

    private var _browserListBinding: FragmentFilebrowserlistBinding? = null
    private val browserListBinding: FragmentFilebrowserlistBinding
        get() = _browserListBinding!!

    private var _browserGridBinding: FragmentFilebrowsergridBinding? = null
    private val browserGridBinding: FragmentFilebrowsergridBinding
        get() = _browserGridBinding!!

    private val managerViewModel by activityViewModels<ManagerViewModel>()
    private val fileBrowserViewModel by activityViewModels<FileBrowserViewModel>()
    private val sortByHeaderViewModel by activityViewModels<SortByHeaderViewModel>()

    private var aB: ActionBar? = null

    private var recyclerView: RecyclerView? = null

    private lateinit var fastScroller: FastScroller
    private lateinit var emptyImageView: ImageView
    private lateinit var emptyTextView: LinearLayout
    private lateinit var emptyTextViewFirst: TextView
    private var adapter: MegaNodeAdapter? = null

    private var density = 0f
    private var outMetrics: DisplayMetrics? = null
    private var display: Display? = null
    private var actionMode: ActionMode? = null
    private lateinit var mLayoutManager: LinearLayoutManager
    private var gridLayoutManager: CustomizedGridLayoutManager? = null
    private var downloadLocationDefaultPath: String? = null
    private var transferOverQuotaBanner: RelativeLayout? = null
    private var transferOverQuotaBannerText: TextView? = null

    // Backup warning dialog
    private var backupWarningDialog: AlertDialog? = null
    private var backupHandleList: ArrayList<Long>? = null
    private var backupDialogType = -1
    private var backupNodeHandle: Long? = null
    private var backupNodeType = 0
    private var backupActionType = 0
    private var fileBackupManager: FileBackupManager? = null

    private var mediaDiscoveryViewSettings = MediaDiscoveryViewSettings.INITIAL.ordinal

    private val isList: Boolean
        get() = (requireActivity() as ManagerActivity).isList

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
                        documents, false, false, false, false
                    )
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
                    documents
                )
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
                    if (megaApi.checkAccessErrorExtended(
                            megaNode,
                            MegaShare.ACCESS_OWNER
                        ).errorCode
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
                        node.name
                    )
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
        super.onCreate(savedInstanceState)
        Timber.d("After onCreate called super")
    }

    /**
     * This method checks scroll of recycler view
     */
    fun checkScroll() {
        if (recyclerView == null) return
        val visible =
            (adapter?.isMultipleSelect == true
                    || transfersManagement.isTransferOverQuotaBannerShown
                    || recyclerView?.canScrollVertically(
                -1
            ) == true && recyclerView?.visibility == View.VISIBLE)
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                fileBrowserViewModel.state.collect {
                    hideMultipleSelect()
                    setNodes(it.nodes.toMutableList())
                    recyclerView?.invalidate()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                sortByHeaderViewModel.showDialogEvent.observe(viewLifecycleOwner,
                    EventObserver { showSortByPanel() }
                )
            }
        }

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

        if (adapter == null) {
            adapter = MegaNodeAdapter(
                activity,
                this,
                emptyList(),
                fileBrowserViewModel.getSafeBrowserParentHandle(),
                recyclerView,
                Constants.FILE_BROWSER_ADAPTER,
                if (isList) MegaNodeAdapter.ITEM_VIEW_TYPE_LIST else MegaNodeAdapter.ITEM_VIEW_TYPE_GRID,
                sortByHeaderViewModel
            )
        }
        (activity as? ManagerActivity)?.setToolbarTitle()
        (activity as? ManagerActivity)?.supportInvalidateOptionsMenu()
        val view = if (isList) {
            Timber.d("isList")
            _browserListBinding = FragmentFilebrowserlistBinding.inflate(inflater, container, false)
            recyclerView = browserListBinding.fileListViewBrowser
            fastScroller = browserListBinding.fastscroll
            mLayoutManager = LinearLayoutManager(context)
            mLayoutManager.orientation = LinearLayoutManager.VERTICAL
            adapter?.let {
                it.parentHandle = (requireActivity() as ManagerActivity).parentHandleBrowser
                it.setListFragment(recyclerView)
                it.adapterType = MegaNodeAdapter.ITEM_VIEW_TYPE_LIST
                it.isMultipleSelect = false
            }

            recyclerView?.apply {
                setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics))
                clipToPadding = false
                layoutManager = mLayoutManager
                setHasFixedSize(true)
                itemAnimator = Util.noChangeRecyclerViewItemAnimator()
                addItemDecoration(
                    PositionDividerItemDecoration(
                        requireContext(),
                        resources.displayMetrics
                    )
                )
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        checkScroll()
                    }
                })
                adapter = this@FileBrowserFragment.adapter
                fastScroller.setRecyclerView(this)
            }
            emptyImageView = browserListBinding.fileListEmptyImage
            emptyTextView = browserListBinding.fileListEmptyText
            emptyTextViewFirst = browserListBinding.fileListEmptyTextFirst
            adapter?.isMultipleSelect = false
            if (adapter?.itemCount == 0) {
                Timber.d("itemCount is 0")
                recyclerView?.visibility = View.GONE
                emptyImageView.visibility = View.VISIBLE
                emptyTextView.visibility = View.VISIBLE
            } else {
                Timber.d("itemCount is ${adapter?.itemCount}")
                recyclerView?.visibility = View.VISIBLE
                emptyImageView.visibility = View.GONE
                emptyTextView.visibility = View.GONE
            }
            transferOverQuotaBanner =
                browserListBinding.layoutTransferOverQuotaBanner.transferOverQuotaBanner
            transferOverQuotaBannerText =
                browserListBinding.layoutTransferOverQuotaBanner.bannerContentText
            browserListBinding.layoutTransferOverQuotaBanner.bannerDismissButton.setOnClickListener {
                hideTransferOverQuotaBanner()
            }
            browserListBinding.layoutTransferOverQuotaBanner.bannerUpgradeButton.setOnClickListener {
                hideTransferOverQuotaBanner()
                (activity as? ManagerActivity)?.navigateToUpgradeAccount()
            }
            browserListBinding.root
        } else {
            Timber.d("Grid View")
            _browserGridBinding = FragmentFilebrowsergridBinding.inflate(inflater, container, false)
            recyclerView = browserGridBinding.fileGridViewBrowser
            fastScroller = browserGridBinding.fastscroll
            emptyImageView = browserGridBinding.fileGridEmptyImage
            emptyTextView = browserGridBinding.fileGridEmptyText
            emptyTextViewFirst = browserGridBinding.fileGridEmptyTextFirst

            adapter?.let {
                it.parentHandle = (requireActivity() as ManagerActivity).parentHandleBrowser
                it.setListFragment(recyclerView)
                it.adapterType = MegaNodeAdapter.ITEM_VIEW_TYPE_GRID
                it.isMultipleSelect = false
            }

            recyclerView?.apply {
                setPadding(0, 0, 0, Util.scaleHeightPx(80, outMetrics))
                clipToPadding = false
                setHasFixedSize(true)
                gridLayoutManager = layoutManager as CustomizedGridLayoutManager
                itemAnimator = DefaultItemAnimator()
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        checkScroll()
                    }
                })
                adapter = this@FileBrowserFragment.adapter
                fastScroller.setRecyclerView(this)
            }
            gridLayoutManager?.let {
                it.spanSizeLookup =
                    adapter?.getSpanSizeLookup(it.spanCount)
            }
            if (adapter?.itemCount == 0) {
                recyclerView?.visibility = View.GONE
                emptyImageView.visibility = View.VISIBLE
                emptyTextView.visibility = View.VISIBLE
            } else {
                recyclerView?.visibility = View.VISIBLE
                emptyImageView.visibility = View.GONE
                emptyTextView.visibility = View.GONE
            }
            transferOverQuotaBanner =
                browserGridBinding.layoutTransferOverQuotaBanner.transferOverQuotaBanner
            transferOverQuotaBannerText =
                browserGridBinding.layoutTransferOverQuotaBanner.bannerContentText
            browserGridBinding.layoutTransferOverQuotaBanner.bannerDismissButton.setOnClickListener {
                hideTransferOverQuotaBanner()
            }
            browserGridBinding.layoutTransferOverQuotaBanner.bannerUpgradeButton.setOnClickListener {
                hideTransferOverQuotaBanner()
                (activity as? ManagerActivity)?.navigateToUpgradeAccount()
            }
            browserGridBinding.root
        }
        setTransferOverQuotaBannerVisibility()
        selectNewlyAddedNodes()
        if ((activity as? ManagerActivity)?.viewInFolderNode != null) {
            animateNode(fileBrowserViewModel.state.value.nodes)
        }

        fileBrowserViewModel.state.flowWithLifecycle(
            viewLifecycleOwner.lifecycle,
            Lifecycle.State.RESUMED
        ).onEach {
            mediaDiscoveryViewSettings = it.mediaDiscoveryViewSettings
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeDragSupportEvents(
            viewLifecycleOwner,
            recyclerView,
            Constants.VIEWER_FROM_FILE_BROWSER
        )
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
                    Timber.d("Nothing to do for actionType = $actionType")
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _browserGridBinding = null
        _browserListBinding = null
    }

    override fun onDestroy() {
        adapter?.clearTakenDownDialog()
        super.onDestroy()
    }

    override fun getAdapter(): RotatableAdapter? = adapter

    /**
     * Opens file
     * @param node MegaNode
     * @param position position of item clicked
     */
    fun openFile(node: MegaNode, position: Int) {
        if (MimeTypeList.typeForName(node.name).isImage) {
            val intent = getIntentForParentNode(
                requireContext(),
                megaApi.getParentNode(node).handle,
                managerViewModel.getOrder(),
                node.handle
            )
            putThumbnailLocation(
                intent,
                recyclerView,
                position,
                Constants.VIEWER_FROM_FILE_BROWSER,
                adapter
            )
            startActivity(intent)
            (activity as? ManagerActivity)?.overridePendingTransition(0, 0)
        } else if (MimeTypeList.typeForName(node.name).isVideoReproducible || MimeTypeList.typeForName(
                node.name
            ).isAudio
        ) {
            val mimeType = MimeTypeList.typeForName(node.name).type
            val mediaIntent: Intent
            val internalIntent: Boolean
            var opusFile = false
            if (MimeTypeList.typeForName(node.name).isVideoNotSupported || MimeTypeList.typeForName(
                    node.name
                ).isAudioNotSupported
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
            putThumbnailLocation(
                mediaIntent,
                recyclerView,
                position,
                Constants.VIEWER_FROM_FILE_BROWSER,
                adapter
            )
            mediaIntent.putExtra("FILENAME", node.name)
            val localPath = FileUtil.getLocalFile(node)
            if (localPath != null) {
                val mediaFile = File(localPath)
                if (localPath.contains(Environment.getExternalStorageDirectory().path)) {
                    Timber.d("itemClick:FileProviderOption")
                    val mediaFileUri = FileProvider.getUriForFile(
                        requireContext(),
                        "mega.privacy.android.app.providers.fileprovider",
                        mediaFile
                    )
                    if (mediaFileUri == null) {
                        Timber.d("itemClick:ERROR:NULLmediaFileUri")
                        (activity as? ManagerActivity)?.showSnackbar(
                            Constants.SNACKBAR_TYPE,
                            getString(
                                R.string.general_text_error
                            ),
                            -1
                        )
                    } else {
                        mediaIntent.setDataAndType(
                            mediaFileUri, MimeTypeList.typeForName(
                                node.name
                            ).type
                        )
                    }
                } else {
                    val mediaFileUri = Uri.fromFile(mediaFile)
                    if (mediaFileUri == null) {
                        Timber.e("itemClick:ERROR:NULLmediaFileUri")
                        (activity as? ManagerActivity)?.showSnackbar(
                            Constants.SNACKBAR_TYPE,
                            getString(
                                R.string.general_text_error
                            ),
                            -1
                        )
                    } else {
                        mediaIntent.setDataAndType(
                            mediaFileUri, MimeTypeList.typeForName(
                                node.name
                            ).type
                        )
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
                    Timber.d("itemClick:total mem: ${mi.totalMem} allocate 32 MB")
                    megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB)
                } else {
                    Timber.d("itemClick:total mem: ${mi.totalMem} allocate 16 MB")
                    megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB)
                }
                val url = megaApi.httpServerGetLocalLink(node)
                if (url != null) {
                    val parsedUri = Uri.parse(url)
                    if (parsedUri != null) {
                        mediaIntent.setDataAndType(parsedUri, mimeType)
                    } else {
                        Timber.e("itemClick:ERROR:httpServerGetLocalLink")
                        (activity as? ManagerActivity)?.showSnackbar(
                            Constants.SNACKBAR_TYPE,
                            getString(
                                R.string.general_text_error
                            ),
                            -1
                        )
                    }
                } else {
                    Timber.e("itemClick:ERROR:httpServerGetLocalLink")
                    (activity as? ManagerActivity)?.showSnackbar(
                        Constants.SNACKBAR_TYPE, getString(
                            R.string.general_text_error
                        ), -1
                    )
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
                    (activity as? ManagerActivity)?.showSnackbar(
                        Constants.SNACKBAR_TYPE, getString(
                            R.string.intent_not_available
                        ), -1
                    )
                    (activity as? ManagerActivity)?.saveNodesToDevice(
                        listOf<MegaNode?>(node),
                        true, false, false, false
                    )
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
                if (localPath.contains(Environment.getExternalStorageDirectory().path)) {
                    pdfIntent.setDataAndType(
                        FileProvider.getUriForFile(
                            requireContext(),
                            "mega.privacy.android.app.providers.fileprovider",
                            mediaFile
                        ),
                        MimeTypeList.typeForName(
                            node.name
                        ).type
                    )
                } else {
                    pdfIntent.setDataAndType(
                        Uri.fromFile(mediaFile), MimeTypeList.typeForName(
                            node.name
                        ).type
                    )
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
                    Timber.d("Total mem: ${mi.totalMem} allocate 32 MB")
                    megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB)
                } else {
                    Timber.d("Total mem: ${mi.totalMem} allocate 16 MB")
                    megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB)
                }
                val url = megaApi.httpServerGetLocalLink(node)
                if (url != null) {
                    val parsedUri = Uri.parse(url)
                    if (parsedUri != null) {
                        pdfIntent.setDataAndType(parsedUri, mimeType)
                    } else {
                        Timber.e("itemClick:ERROR:httpServerGetLocalLink")
                        (activity as? ManagerActivity)?.showSnackbar(
                            Constants.SNACKBAR_TYPE,
                            getString(
                                R.string.general_text_error
                            ),
                            -1
                        )
                    }
                } else {
                    Timber.e("itemClick:ERROR:httpServerGetLocalLink")
                    (activity as? ManagerActivity)?.showSnackbar(
                        Constants.SNACKBAR_TYPE, getString(
                            R.string.general_text_error
                        ), -1
                    )
                }
            }
            pdfIntent.putExtra("HANDLE", node.handle)
            putThumbnailLocation(
                pdfIntent,
                recyclerView,
                position,
                Constants.VIEWER_FROM_FILE_BROWSER,
                adapter
            )
            if (MegaApiUtils.isIntentAvailable(context, pdfIntent)) {
                context?.startActivity(pdfIntent)
            } else {
                Toast.makeText(
                    context,
                    requireContext().resources.getString(R.string.intent_not_available),
                    Toast.LENGTH_LONG
                ).show()
                (activity as? ManagerActivity)?.saveNodesToDevice(
                    listOf<MegaNode?>(node),
                    true, false, false, false
                )
            }
            (activity as? ManagerActivity)?.overridePendingTransition(0, 0)
        } else if (MimeTypeList.typeForName(node.name).isOpenableTextFile(
                node.size
            )
        ) {
            manageTextFileIntent(requireContext(), node, Constants.FILE_BROWSER_ADAPTER)
        } else {
            Timber.d("itemClick:isFile:otherOption")

            val managerActivity = activity as? ManagerActivity ?: return
            onNodeTapped(
                requireActivity(),
                node,
                { saveNode: MegaNode? -> (activity as? ManagerActivity)?.saveNodeByTap(saveNode) },
                managerActivity,
                managerActivity
            )
        }
    }

    /**
     * When an item clicked from adapter it calls below method
     * @param position Position of item which is clicked
     */
    fun itemClick(position: Int) {
        Timber.d("Position:$position")
        if (adapter?.isMultipleSelect == true) {
            Timber.d("Multiselect ON")
            adapter?.toggleSelection(position)

            val selectedNodes = adapter?.selectedNodes
            if (selectedNodes.isNullOrEmpty().not()) {
                updateActionModeTitle()
            }
        } else {
            adapter?.getItem(position)?.let { node ->
                if (node.isFolder) {
                    fileBrowserViewModel.setBrowserParentHandle(node.handle)
                    if (fileBrowserViewModel.shouldEnterMDMode(
                            mediaDiscoveryViewSettings
                        )
                    ) {
                        showMediaDiscovery()
                    } else {
                        val lastFirstVisiblePosition =
                            if (isList) {
                                mLayoutManager.findFirstCompletelyVisibleItemPosition()
                            } else {
                                val pos =
                                    (recyclerView as NewGridRecyclerView).findFirstCompletelyVisibleItemPosition()
                                if (pos == -1) {
                                    Timber.w("Completely -1 then find just visible position")
                                    (recyclerView as NewGridRecyclerView).findFirstVisibleItemPosition()
                                }
                                pos
                            }
                        Timber.d("Push to stack $lastFirstVisiblePosition position")
                        fileBrowserViewModel.onFolderItemClicked(
                            lastFirstVisiblePosition,
                            node.handle
                        )
                        setFolderInfoNavigation(node)
                    }
                } else {
                    openFile(node = node, position = position)
                }
            }
        }
    }

    override fun multipleItemClick(position: Int) {
        adapter?.toggleSelection(position)
    }

    override fun reselectUnHandledSingleItem(position: Int) {
        adapter?.filClicked(position)
    }

    /**
     * Opens Folder
     * @param n MegaNode
     */
    @Suppress("DEPRECATION")
    fun setFolderInfoNavigation(n: MegaNode?) {
        (requireActivity() as ManagerActivity).setToolbarTitle()
        (requireActivity() as ManagerActivity).invalidateOptionsMenu()

        adapter?.parentHandle = fileBrowserViewModel.state.value.fileBrowserHandle

        //If folder has no files
        checkAndConfigureAdapter(
            handle = n?.handle,
            colorPrimary = R.color.grey_900_grey_100,
            colorSecondary = R.color.grey_300_grey_600
        )
        checkScroll()
    }

    /**
     * To show select menu item
     * @return if adapter's multiselect is on or off
     */
    fun showSelectMenuItem(): Boolean = adapter?.isMultipleSelect ?: false

    /**
     * Select all items from adapter
     */
    fun selectAll() {
        Timber.d("selectAll")
        adapter?.let {
            if (it.isMultipleSelect) {
                it.selectAll()
            } else {
                it.isMultipleSelect = true
                it.selectAll()
                actionMode = (activity as? AppCompatActivity)?.startSupportActionMode(
                    ActionBarCallBack()
                )
            }
            Handler(Looper.getMainLooper()).post { updateActionModeTitle() }
        }
    }

    /**
     * Action to be performed based on adapter's items
     * @param handle handle of node
     * @param colorPrimary Primary color for the text to be highlighted
     * @param colorSecondary Secondary color for the text to be displayed
     */
    private fun checkAndConfigureAdapter(
        handle: Long?,
        @ColorRes colorPrimary: Int,
        @ColorRes colorSecondary: Int,
    ) {
        if (adapter?.itemCount == 0) {
            recyclerView?.visibility = View.GONE
            emptyImageView.visibility = View.VISIBLE
            emptyTextView.visibility = View.VISIBLE
            if (megaApi.rootNode != null && megaApi.rootNode.handle == handle) {
                if (requireContext().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    emptyImageView.setImageResource(R.drawable.empty_cloud_drive_landscape)
                } else {
                    emptyImageView.setImageResource(R.drawable.empty_cloud_drive_portrait)
                }
                runCatching {
                    emptyTextViewFirst.text = Html.fromHtml(
                        formatRequiredText(
                            text = getString(R.string.context_empty_cloud_drive).uppercase(
                                Locale.getDefault()
                            ),
                            colorResPrimary = colorPrimary,
                            colorResSecondary = colorSecondary
                        ), Html.FROM_HTML_MODE_LEGACY
                    )
                }.getOrElse {
                    Timber.e(it)
                }
            } else {
                if (requireContext().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    emptyImageView.setImageResource(R.drawable.empty_folder_landscape)
                } else {
                    emptyImageView.setImageResource(R.drawable.empty_folder_portrait)
                }

                runCatching {
                    emptyTextViewFirst.text = Html.fromHtml(
                        formatRequiredText(
                            text = getString(R.string.file_browser_empty_folder_new).uppercase(
                                Locale.getDefault()
                            ),
                            colorResPrimary = colorPrimary,
                            colorResSecondary = colorSecondary
                        ), Html.FROM_HTML_MODE_LEGACY
                    )
                }.getOrElse {
                    Timber.e(it)
                }
            }
        } else {
            recyclerView?.visibility = View.VISIBLE
            emptyImageView.visibility = View.GONE
            emptyTextView.visibility = View.GONE
        }
        checkScroll()
    }

    /**
     * This method will format text to be displayed on fragment when we need to show empty message
     * @param text Text to be formatted and displayed
     * @param colorResPrimary Primary color for the text to be highlighted
     * @param colorResSecondary Secondary color for the text to be displayed
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun formatRequiredText(
        text: String,
        @ColorRes colorResPrimary: Int,
        @ColorRes colorResSecondary: Int,
    ): String {
        return runCatching {
            var textToShow = text
            textToShow = textToShow.replace(
                "[A]", "<font color=\'"
                        + getColorHexString(requireActivity(), colorResPrimary)
                        + "\'>"
            )
            textToShow = textToShow.replace("[/A]", "</font>")
            textToShow = textToShow.replace(
                "[B]", "<font color=\'"
                        + getColorHexString(requireActivity(), colorResSecondary)
                        + "\'>"
            )
            textToShow = textToShow.replace("[/B]", "</font>")
            textToShow
        }.getOrElse {
            throw it
        }
    }

    /**
     * Clear all selected items
     **/
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

    /**
     * Hides multi select option
     */
    fun hideMultipleSelect() {
        Timber.d("hideMultipleSelect")
        adapter?.isMultipleSelect = false
        if (actionMode != null) {
            actionMode?.finish()
        }
    }

    /**
     * On back pressed clicked on activity
     */
    @Suppress("DEPRECATION")
    fun onBackPressed(): Int {
        Timber.d("onBackPressed")
        adapter?.let {

            Timber.d("Parent Handle is: ${fileBrowserViewModel.getSafeBrowserParentHandle()}")
            val managerActivity = requireActivity() as ManagerActivity
            return if (managerActivity.comesFromNotifications && managerActivity.comesFromNotificationHandle == fileBrowserViewModel.getSafeBrowserParentHandle()) {
                managerActivity.restoreFileBrowserAfterComingFromNotification()
                2
            } else {
                fileBrowserViewModel.state.value.parentHandle?.let {
                    fileBrowserViewModel.onBackPressed()
                    recyclerView?.visibility = View.VISIBLE
                    emptyImageView.visibility = View.GONE
                    emptyTextView.visibility = View.GONE
                    managerActivity.supportInvalidateOptionsMenu()
                    managerActivity.setToolbarTitle()

                    val lastVisiblePosition = fileBrowserViewModel.popLastPositionStack()
                    Timber.d("Scroll to $lastVisiblePosition position")
                    if (lastVisiblePosition >= 0) {
                        if (isList) {
                            mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0)
                        } else {
                            gridLayoutManager?.scrollToPositionWithOffset(lastVisiblePosition, 0)
                        }
                    }
                    Timber.d("return 2")
                    2
                } ?: run {
                    0
                }
            }
        }
        return 0
    }

    /**
     * Scrolls list to 1st item/position
     */
    fun scrollToFirstPosition() {
        if (isList) {
            mLayoutManager.scrollToPositionWithOffset(0, 0)
        } else {
            gridLayoutManager?.scrollToPositionWithOffset(0, 0)
        }
    }

    /**
     * This method set nodes and updates the adapter
     * @param nodes List of Mega Nodes
     */
    @Suppress("DEPRECATION")
    private fun setNodes(nodes: MutableList<MegaNode?>) {
        Timber.d("Nodes size: ${nodes.size}")
        visibilityFastScroller()
        val megaNodes = nodes.toMutableList()
        adapter?.let {
            it.setNodes(megaNodes)
            checkAndConfigureAdapter(
                handle = fileBrowserViewModel.getSafeBrowserParentHandle(),
                colorPrimary = R.color.grey_900_grey_100,
                colorSecondary = R.color.grey_300_grey_600
            )
        } ?: run {
            Timber.w("Adapter is NULL")
        }
    }

    /**
     * If adapter's multiple select is on or off
     */
    val isMultipleselect: Boolean
        get() = adapter?.isMultipleSelect == true

    /**
     * Gets total number of items in an adapter
     */
    val itemCount: Int
        get() = adapter?.itemCount ?: 0

    /**
     * This will set the visibility of fast scroller based on item count
     */
    fun visibilityFastScroller() {
        fastScroller.visibility = adapter?.let {
            if (itemCount < Constants.MIN_ITEMS_SCROLLBAR) View.GONE else View.VISIBLE
        } ?: run {
            View.GONE
        }
    }

    /**
     * Sets the "transfer over quota" banner visibility.
     */
    fun setTransferOverQuotaBannerVisibility() {
        if (transfersManagement.isTransferOverQuotaBannerShown) {
            transferOverQuotaBanner?.visibility = View.VISIBLE
            transferOverQuotaBannerText?.text =
                context?.getString(
                    R.string.current_text_depleted_transfer_overquota,
                    TimeUtils.getHumanizedTime(
                        megaApi.bandwidthOverquotaDelay
                    )
                )
            TimeUtils.createAndShowCountDownTimer(
                R.string.current_text_depleted_transfer_overquota,
                transferOverQuotaBanner,
                transferOverQuotaBannerText
            )
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
                    outState.putLong(
                        BACKUP_HANDLED_NODE,
                        backupNode
                    )
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
     * Show Media discovery and launch [MediaDiscoveryFragment]
     */
    private fun showMediaDiscovery() {
        activity?.lifecycleScope?.launch {
            (activity as? ManagerActivity)?.skipToMediaDiscoveryFragment(
                MediaDiscoveryFragment.getNewInstance(fileBrowserViewModel.state.value.mediaHandle),
                fileBrowserViewModel.state.value.mediaHandle
            )
        }
    }

    /**
     * If user navigates from notification about new nodes added to shared folder select all nodes and scroll to the first node in the list
     */
    private fun selectNewlyAddedNodes() {
        val positions =
            (requireActivity() as ManagerActivity).getPositionsList(fileBrowserViewModel.state.value.nodes)
                .takeUnless { it.isEmpty() } ?: return
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
    private fun animateNode(nodes: List<MegaNode?>) {
        val node = (activity as? ManagerActivity)?.viewInFolderNode
        nodePosition = nodes.indexOfFirst { it?.handle == node?.handle }.coerceAtLeast(0)

        //Scroll the position to the 3rd position before of the target position.
        recyclerView?.scrollToPosition(nodePosition - 3)
        recyclerView?.postDelayed({
            val holder = recyclerView?.findViewHolderForAdapterPosition(nodePosition)
            if (null != holder) {
                val animFadeIn = AnimationUtils.loadAnimation(
                    context?.applicationContext, R.anim.fade_in
                )
                animFadeIn.duration = DURATION_ANIMATION.toLong()
                holder.itemView.startAnimation(animFadeIn)
            }
        }, DELAY_RECYCLERVIEW_POST.toLong())
    }

    companion object {
        /**
         * Returns the instance of FileBrowserFragment
         */
        @JvmStatic
        fun newInstance(): FileBrowserFragment {
            Timber.d("newInstance")
            return FileBrowserFragment()
        }

        private const val DURATION_ANIMATION = 1000
        private const val DELAY_RECYCLERVIEW_POST = 500
    }
}
