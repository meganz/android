package mega.privacy.android.app.presentation.inbox

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Spanned
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.constraintlayout.widget.Group
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.components.NewGridRecyclerView
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.observeDragSupportEvents
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.putThumbnailLocation
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.imageviewer.ImageViewerActivity.Companion.getIntentForParentNode
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.PdfViewerActivity
import mega.privacy.android.app.main.adapters.MegaNodeAdapter
import mega.privacy.android.app.main.adapters.RotatableAdapter
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.managerSections.RotatableFragment
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaNodeUtil.areAllNotTakenDown
import mega.privacy.android.app.utils.MegaNodeUtil.manageTextFileIntent
import mega.privacy.android.app.utils.MegaNodeUtil.manageURLNode
import mega.privacy.android.app.utils.MegaNodeUtil.onNodeTapped
import mega.privacy.android.app.utils.MegaNodeUtil.shareNodes
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import java.util.Stack
import javax.inject.Inject

/**
 * An instance of [RotatableFragment] that displays all content that were backed up by the user
 */
@AndroidEntryPoint
class InboxFragment : RotatableFragment() {

    /**
     * Inject [MegaApiAndroid] to the Fragment
     */
    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    /**
     * [Boolean] referenced from [ManagerActivity]
     *
     *
     * If "true", the contents are displayed in a List View-like manner
     * If "false", the contents are displayed in a Grid View-like manner
     */
    private val isList: Boolean
        get() = (requireActivity() as ManagerActivity).isList

    /**
     * [Long] referenced from [ManagerActivity] that returns the Parent Handle
     * of the Inbox Node
     */
    private val parentHandleInbox: Long
        get() = (requireActivity() as ManagerActivity).parentHandleInbox

    /**
     * Returns the number of items found in [MegaNodeAdapter]
     */
    val itemCount: Int
        get() = adapter?.itemCount ?: 0

    /**
     * [RecyclerView] of this Fragment, which has a reference to [ManagerActivity]
     */
    var recyclerView: RecyclerView? = null

    private var linearLayoutManager: LinearLayoutManager? = null
    private var gridLayoutManager: CustomizedGridLayoutManager? = null
    private var emptyFolderImageView: ImageView? = null
    private var emptyFolderTitleTextView: TextView? = null
    private var emptyFolderDescriptionTextView: TextView? = null
    private var emptyFolderContentGroup: Group? = null

    private var adapter: MegaNodeAdapter? = null
    private var inboxNode: MegaNode? = null
    private var nodes: ArrayList<MegaNode>? = null
    private var lastPositionStack: Stack<Int>? = null
    private var actionMode: ActionMode? = null

    private val viewModel by viewModels<InboxViewModel>()

    companion object {
        /**
         * Creates a new instance of [InboxFragment]
         */
        @JvmStatic
        fun newInstance(): InboxFragment {
            Timber.d("newInstance()")
            return InboxFragment()
        }
    }

    /**
     * onCreate Implementation
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate()")
        lastPositionStack = Stack()
    }

    /**
     * onCreateView Implementation
     */
    @Suppress("DEPRECATION")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        Timber.d("onCreateView()")

        val sortByHeaderViewModel = ViewModelProvider(this)[SortByHeaderViewModel::class.java]

        sortByHeaderViewModel.showDialogEvent.observe(viewLifecycleOwner,
            EventObserver { showSortByPanel() })

        viewModel.updateNodes.observe(viewLifecycleOwner,
            EventObserver<List<MegaNode?>> {
                hideMultipleSelect()
                refresh()
            }
        )

        val display = requireActivity().windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        if (parentHandleInbox == -1L || parentHandleInbox == megaApi.inboxNode.handle) {
            Timber.w("Parent Handle == -1")
            if (megaApi.inboxNode != null) {
                Timber.d("InboxNode != null")
                inboxNode = megaApi.inboxNode
                nodes = megaApi.getChildren(inboxNode, viewModel.getOrder())
            }
        } else {
            Timber.d("Parent Handle: %d", parentHandleInbox)
            val parentNode = megaApi.getNodeByHandle(parentHandleInbox)
            if (parentNode != null) {
                logParentNodeHandle(parentNode)
                nodes = megaApi.getChildren(parentNode, viewModel.getOrder())
            }
        }
        (requireActivity() as ManagerActivity).invalidateOptionsMenu()
        (requireActivity() as ManagerActivity).setToolbarTitle()
        return if (isList) {
            Timber.d("InboxFragment is on a ListView")
            val v = inflater.inflate(R.layout.fragment_inbox_list, container, false)

            recyclerView = v.findViewById(R.id.inbox_list_recycler_view)
            linearLayoutManager = LinearLayoutManager(requireActivity())

            // Add bottom padding for recyclerView like in other fragments.
            recyclerView?.let {
                it.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics))
                it.clipToPadding = false
                it.layoutManager = linearLayoutManager
                it.itemAnimator = Util.noChangeRecyclerViewItemAnimator()
                it.addItemDecoration(PositionDividerItemDecoration(requireContext(),
                    resources.displayMetrics))
                it.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        checkScroll()
                    }
                })
            }

            emptyFolderImageView = v.findViewById(R.id.empty_list_folder_image_view)
            emptyFolderContentGroup = v.findViewById(R.id.empty_list_folder_content_group)
            emptyFolderTitleTextView = v.findViewById(R.id.empty_list_folder_title_text_view)
            emptyFolderDescriptionTextView =
                v.findViewById(R.id.empty_list_folder_description_text_view)
            if (adapter == null) {
                adapter = MegaNodeAdapter(requireActivity(),
                    this,
                    nodes,
                    parentHandleInbox,
                    recyclerView,
                    Constants.INBOX_ADAPTER,
                    MegaNodeAdapter.ITEM_VIEW_TYPE_LIST,
                    sortByHeaderViewModel)
            } else {
                adapter?.parentHandle = parentHandleInbox
                adapter?.setListFragment(recyclerView)
                adapter?.adapterType = MegaNodeAdapter.ITEM_VIEW_TYPE_LIST
            }
            adapter?.isMultipleSelect = false
            recyclerView?.adapter = adapter
            setNodes(nodes)
            v
        } else {
            Timber.d("InboxFragment is on a GridView")
            val v = inflater.inflate(R.layout.fragment_inbox_grid, container, false)

            recyclerView = v.findViewById(R.id.inbox_grid_recycler_view)
            recyclerView?.let {
                it.setHasFixedSize(true)
                it.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        checkScroll()
                    }
                })
                it.itemAnimator = DefaultItemAnimator()
            }

            gridLayoutManager = recyclerView?.layoutManager as CustomizedGridLayoutManager?
            emptyFolderImageView = v.findViewById(R.id.empty_grid_folder_image_view)
            emptyFolderContentGroup = v.findViewById(R.id.empty_grid_folder_content_group)
            emptyFolderTitleTextView = v.findViewById(R.id.empty_grid_folder_text_view)
            if (adapter == null) {
                adapter = MegaNodeAdapter(requireActivity(),
                    this,
                    nodes,
                    parentHandleInbox,
                    recyclerView,
                    Constants.INBOX_ADAPTER,
                    MegaNodeAdapter.ITEM_VIEW_TYPE_GRID,
                    sortByHeaderViewModel)
            } else {
                adapter?.let {
                    it.parentHandle = parentHandleInbox
                    it.setListFragment(recyclerView)
                    it.adapterType = MegaNodeAdapter.ITEM_VIEW_TYPE_GRID
                }
            }

            gridLayoutManager?.let {
                adapter?.getSpanSizeLookup(it.spanCount)
            }
            recyclerView?.adapter = adapter
            setNodes(nodes)
            setContent()
            v
        }
    }

    /**
     * onViewCreated implementation
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeDragSupportEvents(viewLifecycleOwner, recyclerView, Constants.VIEWER_FROM_INBOX)
    }

    /**
     * getAdapter implementation
     */
    override fun getAdapter(): RotatableAdapter? = adapter

    /**
     * activateActionMode implementation
     */
    override fun activateActionMode() {
        Timber.d("activateActionMode()")
        adapter?.let {
            if (!it.isMultipleSelect) {
                it.isMultipleSelect = true
                actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(
                    ActionBarCallBack())
            }
        }
    }

    /**
     * multipleItemClick implementation
     */
    override fun multipleItemClick(position: Int) {
        adapter?.toggleSelection(position)
    }

    /**
     * reselectUnHandledSingleItem implementation
     */
    override fun reselectUnHandledSingleItem(position: Int) = Unit

    /**
     * Updates the Action Mode Title
     */
    override fun updateActionModeTitle() {
        if (actionMode == null) {
            return
        }

        val documents = adapter?.selectedNodes ?: emptyList()
        val files = documents.filter { it.isFile }.size
        val folders = documents.filter { it.isFolder }.size
        val sum = files + folders

        actionMode?.let {
            it.title = if (files == 0 && folders == 0) {
                sum.toString()
            } else if (files == 0) {
                folders.toString()
            } else if (folders == 0) {
                files.toString()
            } else {
                sum.toString()
            }

            try {
                actionMode?.invalidate()
            } catch (e: NullPointerException) {
                e.printStackTrace()
                Timber.e(e, "Invalidate error")
            }
        }
    }

    private inner class ActionBarCallBack : ActionMode.Callback {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            val selectedNodes = adapter?.selectedNodes ?: emptyList()
            when (item.itemId) {
                R.id.cab_menu_download -> {
                    (requireActivity() as ManagerActivity).saveNodesToDevice(
                        selectedNodes, false, false, false, false)
                    clearSelections()
                    hideMultipleSelect()
                }
                R.id.cab_menu_copy -> {
                    val handleList = ArrayList(selectedNodes.map { it.handle })
                    NodeController(requireActivity()).also {
                        it.chooseLocationToCopyNodes(handleList)
                    }
                    clearSelections()
                    hideMultipleSelect()
                }
                R.id.cab_menu_select_all -> {
                    selectAll()
                }
                R.id.cab_menu_unselect_all -> {
                    clearSelections()
                    hideMultipleSelect()
                }
                R.id.cab_menu_share_link -> {
                    (requireActivity() as ManagerActivity).showGetLinkActivity(selectedNodes)
                    clearSelections()
                    hideMultipleSelect()
                }
                R.id.cab_menu_share_out -> {
                    shareNodes(requireActivity(), selectedNodes)
                    clearSelections()
                    hideMultipleSelect()
                }
            }
            return false
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            val inflater = mode.menuInflater
            inflater.inflate(R.menu.inbox_action, menu)
            checkScroll()
            return true
        }

        override fun onDestroyActionMode(arg0: ActionMode) {
            Timber.d("onDestroyActionMode()")
            clearSelections()
            adapter?.isMultipleSelect = false
            checkScroll()
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            val selectedNodes = adapter?.selectedNodes ?: emptyList()
            val areAllNotTakenDown = selectedNodes.areAllNotTakenDown()
            var showDownload = false
            var showCopy = false
            val selectAll = menu.findItem(R.id.cab_menu_select_all)
            val unselectAll = menu.findItem(R.id.cab_menu_unselect_all)
            val download = menu.findItem(R.id.cab_menu_download)
            val copy = menu.findItem(R.id.cab_menu_copy)

            if (selectedNodes.isNotEmpty()) {
                selectAll.isVisible = selectedNodes.size != itemCount
                unselectAll.title = getString(R.string.action_unselect_all)
                unselectAll.isVisible = true
                showDownload = areAllNotTakenDown
                showCopy = areAllNotTakenDown
            } else {
                selectAll.isVisible = true
                unselectAll.isVisible = false
            }
            download.isVisible = showDownload
            if (showDownload) {
                download.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }
            copy.isVisible = showCopy
            if (showCopy) {
                copy.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }
            return false
        }
    }

    /**
     * Logs the Parent Node Handle
     *
     * @param parentNode The Parent Node
     */
    private fun logParentNodeHandle(parentNode: MegaNode) =
        Timber.d("Parent Node Handle: %s", parentNode.handle)

    /**
     * Selects all items from [MegaNodeAdapter]
     */
    fun selectAll() {
        adapter?.let {
            if (it.isMultipleSelect) {
                it.selectAll()
            } else {
                it.isMultipleSelect = true
                it.selectAll()
                actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(
                    ActionBarCallBack())
            }
            updateActionModeTitle()
        }
    }

    /**
     * Shows the Sort by panel
     */
    private fun showSortByPanel() {
        (requireActivity() as ManagerActivity).showNewSortByPanel(Constants.ORDER_CLOUD)
    }

    /**
     * Checks the Scrolling Behavior
     */
    fun checkScroll() {
        recyclerView?.let {
            if ((it.canScrollVertically(-1) && it.isVisible) ||
                adapter?.isMultipleSelect == true
            ) {
                (requireActivity() as ManagerActivity).changeAppBarElevation(true)
            } else {
                (requireActivity() as ManagerActivity).changeAppBarElevation(false)
            }
        }
    }

    /**
     * Refresh the contents of the Fragment
     */
    fun refresh() {
        Timber.d("refresh()")

        inboxNode?.let {
            if (parentHandleInbox == -1L || parentHandleInbox == it.handle) {
                nodes = megaApi.getChildren(it, viewModel.getOrder())
            } else {
                val parentNode = megaApi.getNodeByHandle(parentHandleInbox)
                if (parentNode != null) {
                    logParentNodeHandle(parentNode)
                    nodes = megaApi.getChildren(parentNode, viewModel.getOrder())
                }
            }
        }
        setNodes(nodes)
    }

    /**
     * Opens the file
     *
     * @param node The [MegaNode] to be opened
     * @param position The [MegaNode] position
     */
    private fun openFile(node: MegaNode, position: Int) {
        if (MimeTypeList.typeForName(node.name).isImage) {
            val intent = getIntentForParentNode(
                context = requireContext(),
                parentNodeHandle = megaApi.getParentNode(node).handle,
                childOrder = viewModel.getOrder(),
                currentNodeHandle = node.handle,
            )
            putThumbnailLocation(
                launchIntent = intent,
                rv = recyclerView,
                position = position,
                viewerFrom = Constants.VIEWER_FROM_INBOX,
                thumbnailGetter = adapter
            )
            startActivity(intent)
            (requireActivity() as ManagerActivity).overridePendingTransition(0, 0)
        } else if (MimeTypeList.typeForName(node.name).isVideoReproducible ||
            MimeTypeList.typeForName(node.name).isAudio
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
                val s = node.name.split("\\.".toRegex()).toTypedArray()
                if (s.size > 1 && s[s.size - 1] == "opus") {
                    opusFile = true
                }
            } else {
                internalIntent = true
                mediaIntent = Util.getMediaIntent(requireActivity(), node.name)
            }

            mediaIntent.putExtra(Constants.INTENT_EXTRA_KEY_POSITION, position)
            if (megaApi.getParentNode(node).type == MegaNode.TYPE_INCOMING) {
                mediaIntent.putExtra(Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, -1L)
            } else {
                mediaIntent.putExtra(Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE,
                    megaApi.getParentNode(node).handle)
            }
            mediaIntent.putExtra(Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN,
                viewModel.getOrder())
            putThumbnailLocation(
                launchIntent = mediaIntent,
                rv = recyclerView,
                position = position,
                viewerFrom = Constants.VIEWER_FROM_INBOX,
                thumbnailGetter = adapter
            )

            mediaIntent.putExtra(Constants.INTENT_EXTRA_KEY_PLACEHOLDER, adapter?.placeholderCount)
            mediaIntent.putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, node.handle)
            mediaIntent.putExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME, node.name)
            mediaIntent.putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.INBOX_ADAPTER)

            val localPath = FileUtil.getLocalFile(node)
            if (localPath != null) {
                val mediaFile = File(localPath)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                    localPath.contains(Environment.getExternalStorageDirectory().path)
                ) {
                    mediaIntent.setDataAndType(FileProvider.getUriForFile(
                        requireActivity(),
                        "mega.privacy.android.app.providers.fileprovider",
                        mediaFile),
                        MimeTypeList.typeForName(node.name).type)
                } else {
                    mediaIntent.setDataAndType(Uri.fromFile(mediaFile),
                        MimeTypeList.typeForName(node.name).type)
                }
                mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                if (megaApi.httpServerIsRunning() == 0) {
                    megaApi.httpServerStart()
                    mediaIntent.putExtra(Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
                }
                val memoryInfo = ActivityManager.MemoryInfo()
                val activityManager =
                    requireActivity().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.getMemoryInfo(memoryInfo)
                if (memoryInfo.totalMem > Constants.BUFFER_COMP) {
                    Timber.d("Total memory: %d allocate 32 MB", memoryInfo.totalMem)
                    megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB)
                } else {
                    Timber.d("Total memory: %d allocate 16 MB", memoryInfo.totalMem)
                    megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB)
                }
                val url = megaApi.httpServerGetLocalLink(node)
                mediaIntent.setDataAndType(Uri.parse(url), mimeType)
            }
            if (opusFile) {
                mediaIntent.setDataAndType(mediaIntent.data, "audio/*")
            }
            if (internalIntent) {
                startActivity(mediaIntent)
            } else {
                if (MegaApiUtils.isIntentAvailable(requireActivity(), mediaIntent)) {
                    startActivity(mediaIntent)
                } else {
                    (requireActivity() as ManagerActivity).showSnackbar(Constants.SNACKBAR_TYPE,
                        getString(R.string.intent_not_available),
                        -1
                    )
                    adapter?.notifyDataSetChanged()
                    (requireActivity() as ManagerActivity).saveNodesToDevice(listOf(node),
                        true, false, false, false)
                }
            }
            (requireActivity() as ManagerActivity).overridePendingTransition(0, 0)
        } else if (MimeTypeList.typeForName(node.name).isPdf) {
            val mimeType = MimeTypeList.typeForName(node.name).type
            val pdfIntent = Intent(requireActivity(), PdfViewerActivity::class.java)
            pdfIntent.putExtra(Constants.INTENT_EXTRA_KEY_INSIDE, true)
            pdfIntent.putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.INBOX_ADAPTER)
            val localPath = FileUtil.getLocalFile(node)
            if (localPath != null) {
                val mediaFile = File(localPath)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().path)) {
                    pdfIntent.setDataAndType(FileProvider.getUriForFile(
                        requireActivity(),
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
                    requireActivity().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.getMemoryInfo(mi)
                if (mi.totalMem > Constants.BUFFER_COMP) {
                    Timber.d("Total memory: %d allocate 32 MB", mi.totalMem)
                    megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB)
                } else {
                    Timber.d("Total memory: %d allocate 16 MB", mi.totalMem)
                    megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB)
                }
                val url = megaApi.httpServerGetLocalLink(node)
                pdfIntent.setDataAndType(Uri.parse(url), mimeType)
            }
            pdfIntent.putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, node.handle)
            putThumbnailLocation(
                launchIntent = pdfIntent,
                rv = recyclerView,
                position = position,
                viewerFrom = Constants.VIEWER_FROM_INBOX,
                thumbnailGetter = adapter,
            )
            if (MegaApiUtils.isIntentAvailable(requireActivity(), pdfIntent)) {
                startActivity(pdfIntent)
            } else {
                Toast.makeText(requireContext(),
                    getString(R.string.intent_not_available),
                    Toast.LENGTH_LONG).show()
                (requireActivity() as ManagerActivity).saveNodesToDevice(listOf(node),
                    true, false, false, false)
            }
            (requireActivity() as ManagerActivity).overridePendingTransition(0, 0)
        } else if (MimeTypeList.typeForName(node.name).isURL) {
            manageURLNode(requireActivity(), megaApi, node)
        } else if (MimeTypeList.typeForName(node.name).isOpenableTextFile(node.size)) {
            manageTextFileIntent(requireContext(), node, Constants.INBOX_ADAPTER)
        } else {
            adapter?.notifyDataSetChanged()
            onNodeTapped(
                context = requireActivity(),
                node = node,
                nodeDownloader = { nodeToDownload: MegaNode? ->
                    (requireActivity() as ManagerActivity).saveNodeByTap(nodeToDownload)
                },
                activityLauncher = (requireActivity() as ManagerActivity),
                snackbarShower = (requireActivity() as ManagerActivity),
            )
        }
    }

    /**
     * Handles the Item Click behavior
     */
    fun itemClick(position: Int) {
        Timber.d("itemClick()")
        if (adapter?.isMultipleSelect == true) {
            Timber.d("Multi Select is Enabled")
            adapter?.toggleSelection(position)
            val selectedNodes = adapter?.selectedNodes ?: emptyList()
            if (selectedNodes.isNotEmpty()) updateActionModeTitle()
        } else {
            if (nodes?.get(position)?.isFolder == true) {
                var lastFirstVisiblePosition: Int
                if (isList) {
                    lastFirstVisiblePosition =
                        linearLayoutManager?.findFirstCompletelyVisibleItemPosition()
                            ?: RecyclerView.NO_POSITION
                } else {
                    lastFirstVisiblePosition =
                        (recyclerView as NewGridRecyclerView?)?.findFirstCompletelyVisibleItemPosition()
                            ?: RecyclerView.NO_POSITION
                    if (lastFirstVisiblePosition == -1) {
                        Timber.d("Completely -1 then find just visible position")
                        lastFirstVisiblePosition =
                            (recyclerView as NewGridRecyclerView?)?.findFirstVisibleItemPosition()
                                ?: RecyclerView.NO_POSITION
                    }
                }
                Timber.d("Push to stack %d position", lastFirstVisiblePosition)
                lastPositionStack?.push(lastFirstVisiblePosition)
                (requireActivity() as ManagerActivity).invalidateOptionsMenu()
                (requireActivity() as ManagerActivity).setToolbarTitle()

                nodes?.let {
                    (requireActivity() as ManagerActivity).parentHandleInbox = it[position].handle
                }
                nodes = megaApi.getChildren((nodes ?: return)[position],
                    viewModel.getOrder())
                adapter?.setNodes(nodes)
                setContent()
                recyclerView?.scrollToPosition(0)
                checkScroll()
            } else {
                openFile((nodes ?: return)[position], position)
            }
        }
    }

    /**
     * Clear all selected items
     */
    private fun clearSelections() {
        adapter?.let { if (it.isMultipleSelect) it.clearSelections() }
    }

    /**
     * Disable selection
     */
    fun hideMultipleSelect() {
        Timber.d("hideMultipleSelect()")
        adapter?.let { it.isMultipleSelect = false }
        actionMode?.finish()
    }

    /**
     * onBackPressed behavior
     */
    fun onBackPressed(): Int {
        Timber.d("onBackPressed()")
        if (adapter == null) {
            return 0
        }
        return if ((requireActivity() as ManagerActivity).comesFromNotifications &&
            (requireActivity() as ManagerActivity).comesFromNotificationHandle == parentHandleInbox
        ) {
            (requireActivity() as ManagerActivity).comesFromNotifications = false
            (requireActivity() as ManagerActivity).comesFromNotificationHandle = -1
            (requireActivity() as ManagerActivity).selectDrawerItem(DrawerItem.NOTIFICATIONS)
            (requireActivity() as ManagerActivity).parentHandleInbox =
                (requireActivity() as ManagerActivity).comesFromNotificationHandleSaved
            (requireActivity() as ManagerActivity).comesFromNotificationHandleSaved = -1
            2
        } else {
            val parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandleInbox))
            if (parentNode != null) {
                logParentNodeHandle(parentNode)
                (requireActivity() as ManagerActivity).invalidateOptionsMenu()
                (requireActivity() as ManagerActivity).parentHandleInbox = parentNode.handle
                (requireActivity() as ManagerActivity).setToolbarTitle()
                nodes = megaApi.getChildren(parentNode, viewModel.getOrder())
                setNodes(nodes)

                var lastVisiblePosition = 0
                lastPositionStack?.let {
                    if (!it.empty()) {
                        lastVisiblePosition = it.pop()
                        Timber.d("Pop of the stack %d position", lastVisiblePosition)
                    }
                }
                Timber.d("Scroll to %d position", lastVisiblePosition)
                if (lastVisiblePosition >= 0) {
                    if (isList) {
                        linearLayoutManager?.scrollToPositionWithOffset(lastVisiblePosition, 0)
                    } else {
                        gridLayoutManager?.scrollToPositionWithOffset(lastVisiblePosition, 0)
                    }
                }
                2
            } else {
                0
            }
        }
    }

    /**
     * Sets the content to [MegaNodeAdapter]
     *
     * @param nodes The content to be displayed
     */
    fun setNodes(nodes: ArrayList<MegaNode>?) {
        Timber.d("setNodes()")
        this.nodes = nodes
        adapter?.let {
            it.setNodes(nodes)
            setContent()
        }
    }

    /**
     * Sets all content of the feature.
     *
     * If no nodes are available, empty folder information will be displayed. Otherwise, it will
     * display all available nodes.
     */
    fun setContent() {
        Timber.d("setContent()")
        if (itemCount == 0) {
            recyclerView?.visibility = View.GONE
            emptyFolderContentGroup?.visibility = View.VISIBLE
            if (megaApi.inboxNode.handle == parentHandleInbox || parentHandleInbox == -1L) {
                setEmptyFolderTextContent(getString(R.string.backups_empty_state_title),
                    getString(R.string.backups_empty_state_body))
                emptyFolderImageView?.setImageResource(
                    if (requireContext().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        R.drawable.ic_zero_landscape_empty_folder
                    } else {
                        R.drawable.ic_zero_portrait_empty_folder
                    }
                )
            } else {
                setEmptyFolderTextContent(getString(R.string.file_browser_empty_folder_new),
                    "")
                emptyFolderImageView?.setImageResource(
                    if (requireContext().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        R.drawable.empty_folder_landscape
                    } else {
                        R.drawable.empty_folder_portrait
                    }
                )

            }
        } else {
            recyclerView?.visibility = View.VISIBLE
            emptyFolderContentGroup?.visibility = View.GONE
        }
    }

    /**
     * Sets the title and description text when the folder is empty
     * Null checking exists for the description since this only exists on the list configuration
     * of the feature
     *
     * @param title Empty folder title
     * @param description Empty folder description
     */
    private fun setEmptyFolderTextContent(title: String, description: String) {
        emptyFolderTitleTextView?.text = formatEmptyFolderTitleString(title)
        emptyFolderDescriptionTextView?.text = description
    }

    /**
     * Formats a String through a specified color formatting, which is then used for the title
     * message when the folder is empty
     *
     * @param title The title to be formatted
     * @return The [Spanned] title to be immediately used by the [TextView]
     */
    private fun formatEmptyFolderTitleString(title: String): Spanned {
        var textToFormat = title

        try {
            textToFormat = textToFormat.replace(
                "[A]", "<font color='"
                        + getColorHexString(requireContext(), R.color.grey_900_grey_100)
                        + "'>"
            ).replace("[/A]", "</font>").replace(
                "[B]", "<font color='"
                        + getColorHexString(requireContext(), R.color.grey_300_grey_600)
                        + "'>"
            ).replace("[/B]", "</font>")
        } catch (exception: Exception) {
            exception.printStackTrace()
        }

        return HtmlCompat.fromHtml(textToFormat, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
}