package mega.privacy.android.app.presentation.shares

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.view.ActionMode
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.observeDragSupportEvents
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.putThumbnailLocation
import mega.privacy.android.app.components.scrollBar.FastScroller
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.imageviewer.ImageViewerActivity.Companion.getIntentForParentNode
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.PdfViewerActivity
import mega.privacy.android.app.main.adapters.MegaNodeAdapter
import mega.privacy.android.app.main.adapters.RotatableAdapter
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.managerSections.RotatableFragment
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.manager.model.Tab
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaNodeUtil.manageTextFileIntent
import mega.privacy.android.app.utils.MegaNodeUtil.manageURLNode
import mega.privacy.android.app.utils.MegaNodeUtil.onNodeTapped
import mega.privacy.android.app.utils.MegaNodeUtil.shareNodes
import mega.privacy.android.app.utils.MegaNodeUtil.showConfirmationLeaveIncomingShares
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Abstract fragment representing a page used for populating shares section
 */
@AndroidEntryPoint
abstract class MegaNodeBaseFragment : RotatableFragment() {

    /**
     * MegaApi
     */
    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    /**
     * Number of items in the adapter
     */
    val itemCount: Int
        get() = adapter?.itemCount ?: 0

    /**
     * viewModel responsible for sorting the list
     */
    protected val sortByHeaderViewModel by viewModels<SortByHeaderViewModel>()

    /**
     * Adapter holding the list of nodes
     */
    protected var adapter: MegaNodeAdapter? = null

    /**
     * Activity bound to the fragment
     */
    protected var managerActivity: ManagerActivity? = null

    /**
     * Contextual action mode of the fragment
     */
    protected var actionMode: ActionMode? = null

    /** UI Components*/
    protected var fastScroller: FastScroller? = null
    protected var recyclerView: RecyclerView? = null
    protected var mLayoutManager: LinearLayoutManager? = null
    protected var gridLayoutManager: CustomizedGridLayoutManager? = null
    protected var emptyImageView: ImageView? = null
    protected var emptyLinearLayout: LinearLayout? = null
    protected var emptyTextViewFirst: TextView? = null

    /**
     * Viewer identifier corresponding to the fragment
     */
    protected abstract val viewerFrom: Int

    /**
     * Shares tab corresponding to the fragment
     */
    protected abstract val currentSharesTab: SharesTab

    /**
     * Current sort order
     */
    protected abstract val sortOrder: Int

    /**
     * Current parent handle
     */
    protected abstract val parentHandle: Long

    /**
     * OnBackPressed delegation
     */
    abstract fun onBackPressed(): Int

    /**
     * Item click delegation
     */
    abstract fun itemClick(position: Int)

    /**
     * Navigates to a new child folder.
     *
     * @param node The folder node.
     */
    abstract fun navigateToFolder(node: MegaNode)

    /**
     * Shows the Sort by panel.
     */
    protected abstract fun showSortByPanel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        sortByHeaderViewModel.showDialogEvent.observe(viewLifecycleOwner, EventObserver {
            showSortByPanel()
        })
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView?.let { observeDragSupportEvents(viewLifecycleOwner, it, viewerFrom) }
        checkScroll()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ManagerActivity) {
            managerActivity = context
        }
    }

    override fun onDestroy() {
        adapter?.clearTakenDownDialog()
        super.onDestroy()
    }

    override fun getAdapter(): RotatableAdapter? = adapter

    override fun activateActionMode() {
        if (adapter?.isMultipleSelect == false)
            adapter?.isMultipleSelect = true
    }

    override fun multipleItemClick(position: Int) {
        adapter?.toggleSelection(position)
    }

    override fun reselectUnHandledSingleItem(position: Int) {
        adapter?.filClicked(position)
    }

    override fun updateActionModeTitle() {
        if (actionMode == null || activity == null || adapter == null)
            return

        val files = adapter?.selectedNodes?.filter { it.isFile }?.size ?: 0
        val folders = adapter?.selectedNodes?.filter { it.isFolder }?.size ?: 0

        actionMode?.title = when {
            (files == 0 && folders == 0) -> 0.toString()
            files == 0 -> folders.toString()
            folders == 0 -> files.toString()
            else -> (files + folders).toString()
        }

        try {
            actionMode?.invalidate()
        } catch (e: NullPointerException) {
            e.printStackTrace()
            Timber.e(e, "Invalidate error")
        }
    }

    /**
     * Method to update an item when a nickname is added, updated or removed from a contact.
     *
     * @param contactHandle Contact ID.
     */
    fun updateContact(contactHandle: Long) {
        adapter?.updateItem(contactHandle)
    }

    /**
     * Select all items
     */
    fun selectAll() {
        adapter?.let {
            activateActionMode()
            it.selectAll()
            updateActionModeTitle()
        }
    }

    /**
     * Deactivate action mode and clear selection
     */
    fun hideActionMode() {
        clearSelections()
        hideMultipleSelect()
    }

    /**
     * Deactivate action mode
     */
    fun hideMultipleSelect() {
        if (adapter?.isMultipleSelect == true) {
            adapter?.isMultipleSelect = false
            actionMode?.finish()
        }
    }

    /**
     * Clear the selected nodes
     */
    fun clearSelections() {
        if (adapter?.isMultipleSelect == true)
            adapter?.clearSelections()
    }

    /**
     * Refresh the list
     */
    fun notifyDataSetChanged() {
        adapter?.notifyDataSetChanged()
    }

    /**
     * Set the visibility of the fast scroller
     */
    fun visibilityFastScroller() {
        fastScroller?.visibility =
            if (adapter == null || (adapter ?: return).itemCount < Constants.MIN_ITEMS_SCROLLBAR)
                View.GONE
            else
                View.VISIBLE
    }

    /**
     * Display the elevation of the app bar or not
     */
    fun checkScroll() {
        val withElevation =
            (recyclerView?.canScrollVertically(-1) == true && recyclerView?.visibility == View.VISIBLE)
                    || adapter?.isMultipleSelect == true
        managerActivity?.changeAppBarElevation(withElevation)
    }

    /**
     * Display the empty view or not
     */
    protected fun checkEmptyView() {
        if (adapter?.itemCount == 0) {
            recyclerView?.visibility = View.GONE
            emptyImageView?.visibility = View.VISIBLE
            emptyLinearLayout?.visibility = View.VISIBLE
        } else {
            recyclerView?.visibility = View.VISIBLE
            emptyImageView?.visibility = View.GONE
            emptyLinearLayout?.visibility = View.GONE
        }
    }

    /**
     * Open a file
     *
     * @param node the file to open
     * @param fragmentAdapter an id identifying the fragment
     * @param position the position of the file in the list
     */
    fun openFile(node: MegaNode, fragmentAdapter: Int, position: Int) {
        val mimeType = MimeTypeList.typeForName(node.name)
        val mimeTypeType = mimeType.type
        val intent: Intent
        var internalIntent = false

        when {
            mimeType.isImage -> {
                intent = getIntentForParentNode(
                    requireContext(),
                    parentHandle,
                    sortOrder,
                    node.handle
                )
                launchIntent(intent, true, position)
            }

            (mimeType.isVideoReproducible || mimeType.isAudio) -> {
                var opusFile = false

                if (mimeType.isVideoNotSupported || mimeType.isAudioNotSupported) {
                    intent = Intent(Intent.ACTION_VIEW)
                    val s = node.name.split("\\.".toRegex()).toTypedArray()
                    opusFile = s.size > 1 && s[s.size - 1] == "opus"
                } else {
                    intent = Util.getMediaIntent(requireContext(), node.name)
                    internalIntent = true
                }

                intent.apply {
                    putExtra("position", position)
                    putExtra("placeholder", adapter?.placeholderCount ?: 0)
                    putExtra("parentNodeHandle", parentHandle)
                    putExtra("orderGetChildren", sortOrder)
                    putExtra("adapterType", fragmentAdapter)
                    putExtra("HANDLE", node.handle)
                    putExtra("FILENAME", node.name)

                    val localPath = FileUtil.getLocalFile(node)
                    if (localPath != null) {

                        val mediaFile = File(localPath)
                        setDataAndType(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                FileProvider.getUriForFile(requireContext(),
                                    Constants.AUTHORITY_STRING_FILE_PROVIDER,
                                    mediaFile)
                            } else Uri.fromFile(mediaFile),
                            MimeTypeList.typeForName(node.name).type
                        )
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    } else {

                        if (megaApi.httpServerIsRunning() == 0) {
                            megaApi.httpServerStart()
                            putExtra(Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
                        } else {
                            Timber.w("ERROR:httpServerAlreadyRunning")
                        }

                        val mi = ActivityManager.MemoryInfo()
                        val activityManager =
                            requireActivity().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                        activityManager.getMemoryInfo(mi)

                        if (mi.totalMem > Constants.BUFFER_COMP) {
                            Timber.d("total mem: %d allocate 32 MB", mi.totalMem)
                            megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB)
                        } else {
                            Timber.d("total mem: %d allocate 16 MB", mi.totalMem)
                            megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB)
                        }

                        val url = megaApi.httpServerGetLocalLink(node)

                        url?.let { Uri.parse(it) }?.let {
                            setDataAndType(it, mimeTypeType)
                        } ?: run {
                            Timber.e("ERROR:httpServerGetLocalLink")
                            managerActivity?.showSnackbar(Constants.SNACKBAR_TYPE,
                                getString(R.string.general_text_error),
                                MegaApiJava.INVALID_HANDLE)
                            return
                        }
                    }

                    if (opusFile) {
                        setDataAndType(intent.data, "audio/*")
                    }
                }

                launchIntent(intent, internalIntent, position)
            }

            mimeType.isURL -> {
                manageURLNode(requireContext(), megaApi, node)
            }

            mimeType.isPdf -> {
                Timber.d("isFile:isPdf")
                intent = Intent(requireContext(), PdfViewerActivity::class.java)
                intent.apply {
                    putExtra("inside", true)
                    putExtra("adapterType", fragmentAdapter)
                    putExtra("HANDLE", node.handle)

                    val localPath = FileUtil.getLocalFile(node)
                    if (localPath != null) {
                        val mediaFile = File(localPath)
                        setDataAndType(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                FileProvider.getUriForFile(requireContext(),
                                    Constants.AUTHORITY_STRING_FILE_PROVIDER,
                                    mediaFile)
                            } else Uri.fromFile(mediaFile),
                            mimeTypeType
                        )
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } else {
                        if (megaApi.httpServerIsRunning() == 0) {
                            megaApi.httpServerStart()
                            intent.putExtra(Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
                        }
                        val mi = ActivityManager.MemoryInfo()
                        val activityManager =
                            requireActivity().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                        activityManager.getMemoryInfo(mi)
                        if (mi.totalMem > Constants.BUFFER_COMP) {
                            Timber.d("Total mem: %d allocate 32 MB", mi.totalMem)
                            megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB)
                        } else {
                            Timber.d("Total mem: %d allocate 16 MB", mi.totalMem)
                            megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB)
                        }
                        val url = megaApi.httpServerGetLocalLink(node)

                        url?.let { Uri.parse(it) }?.let {
                            setDataAndType(it, mimeTypeType)
                        } ?: run {
                            Timber.e("ERROR:httpServerGetLocalLink")
                            managerActivity?.showSnackbar(Constants.SNACKBAR_TYPE,
                                getString(R.string.general_text_error),
                                MegaApiJava.INVALID_HANDLE)
                            return
                        }
                    }
                }

                launchIntent(intent, false, position)
            }

            mimeType.isOpenableTextFile(node.size) -> {
                manageTextFileIntent(requireContext(), node, fragmentAdapter)
            }

            else -> {
                Timber.d("itemClick:isFile:otherOption")
                managerActivity?.let {
                    onNodeTapped(requireActivity(),
                        node,
                        { node: MegaNode? -> it.saveNodeByTap(node) },
                        it,
                        it
                    )
                }
            }
        }
    }

    /**
     * Launch corresponding intent to open the file based on its type.
     *
     * @param intent         Intent to launch activity.
     * @param internalIntent true, if the intent is for launching an intent in-app; false, otherwise.
     * @param position       Clicked item position.
     */
    private fun launchIntent(intent: Intent?, internalIntent: Boolean, position: Int) {
        if (intent != null) {
            if (internalIntent || MegaApiUtils.isIntentAvailable(requireContext(), intent)) {
                putThumbnailLocation(intent,
                    recyclerView ?: return,
                    position,
                    viewerFrom,
                    adapter ?: return)
                startActivity(intent)
                managerActivity?.overridePendingTransition(0, 0)
            } else {
                Toast.makeText(requireContext(),
                    StringResourcesUtils.getString(R.string.intent_not_available),
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Inflate a vertical list
     */
    protected fun getListView(inflater: LayoutInflater, container: ViewGroup?): View {
        val v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false)
        recyclerView = v.findViewById(R.id.file_list_view_browser)
        mLayoutManager = LinearLayoutManager(requireContext())
        recyclerView?.layoutManager = mLayoutManager
        recyclerView?.addItemDecoration(PositionDividerItemDecoration(requireContext(),
            resources.displayMetrics))
        fastScroller = v.findViewById(R.id.fastscroll)
        setRecyclerView()
        recyclerView?.itemAnimator = Util.noChangeRecyclerViewItemAnimator()
        emptyImageView = v.findViewById(R.id.file_list_empty_image)
        emptyLinearLayout = v.findViewById(R.id.file_list_empty_text)
        emptyTextViewFirst = v.findViewById(R.id.file_list_empty_text_first)
        adapter?.adapterType = MegaNodeAdapter.ITEM_VIEW_TYPE_LIST

        return v
    }

    /**
     * Inflate a grid list
     */
    protected fun getGridView(inflater: LayoutInflater, container: ViewGroup?): View {
        val v = inflater.inflate(R.layout.fragment_filebrowsergrid, container, false)
        recyclerView = v.findViewById(R.id.file_grid_view_browser)
        gridLayoutManager = recyclerView?.layoutManager as CustomizedGridLayoutManager?
        fastScroller = v.findViewById(R.id.fastscroll)
        setRecyclerView()
        recyclerView?.itemAnimator = DefaultItemAnimator()
        emptyImageView = v.findViewById(R.id.file_grid_empty_image)
        emptyLinearLayout = v.findViewById(R.id.file_grid_empty_text)
        emptyTextViewFirst = v.findViewById(R.id.file_grid_empty_text_first)
        adapter?.adapterType = MegaNodeAdapter.ITEM_VIEW_TYPE_GRID

        return v
    }

    /**
     * Setup the recyclerview
     */
    private fun setRecyclerView() {
        recyclerView?.setPadding(0,
            0,
            0,
            Util.dp2px(85.toFloat(), resources.displayMetrics))
        recyclerView?.setHasFixedSize(true)
        recyclerView?.clipToPadding = false
        recyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val tab = currentSharesTab
                if (managerActivity?.tabItemShares === tab) {
                    checkScroll()
                }
            }
        })
        fastScroller?.setRecyclerView(recyclerView)
    }

    /**
     * Setup the empty view
     */
    protected fun setFinalEmptyView(initialText: String?) {

        var text = initialText
            ?: run {
                emptyImageView?.setImageResource(
                    if (Util.isScreenInPortrait(requireContext()))
                        R.drawable.empty_folder_portrait
                    else R.drawable.empty_folder_landscape
                )
                StringResourcesUtils.getString(R.string.file_browser_empty_folder_new)
            }

        try {
            text = text.replace("[A]", "<font color=\'"
                    + getColorHexString(requireContext(), R.color.grey_900_grey_100)
                    + "\'>")
            text = text.replace("[/A]", "</font>")
            text = text.replace("[B]", "<font color=\'"
                    + getColorHexString(requireContext(), R.color.grey_300_grey_600)
                    + "\'>")
            text = text.replace("[/B]", "</font>")
        } catch (e: Exception) {
            Timber.w(e, "Exception formatting text")
        }
        emptyTextViewFirst?.text = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
        checkEmptyView()
    }

    /**
     * Base callback action mode
     */
    protected abstract inner class BaseActionBarCallBack(private val currentTab: Tab) :
        ActionMode.Callback {
        /**
         * Selected nodes
         */
        protected var selected: List<MegaNode> = ArrayList()

        override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
            val inflater = actionMode.menuInflater
            inflater.inflate(R.menu.cloud_storage_action, menu)
            if (activity is ManagerActivity) {
                managerActivity?.hideFabButton()
                managerActivity?.hideTabs(true, currentTab)
                managerActivity?.showHideBottomNavigationView(true)
            }
            checkScroll()
            return true
        }

        override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
            selected = adapter?.selectedNodes ?: return false
            menu.findItem(R.id.cab_menu_share_link).title =
                StringResourcesUtils.getQuantityString(R.plurals.get_links, selected.size)
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            Timber.d("onActionItemClicked")
            val handleList = ArrayList<Long>()
            for (node in selected) {
                handleList.add(node.handle)
            }
            val nC = NodeController(requireActivity())
            when (item.itemId) {
                R.id.cab_menu_download -> {
                    managerActivity?.saveNodesToDevice(selected, false, false, false, false)
                    hideActionMode()
                }
                R.id.cab_menu_rename -> {
                    managerActivity?.showRenameDialog(selected[0])
                    hideActionMode()
                }
                R.id.cab_menu_copy -> {
                    nC.chooseLocationToCopyNodes(handleList)
                    hideActionMode()
                }
                R.id.cab_menu_move -> {
                    nC.chooseLocationToMoveNodes(handleList)
                    hideActionMode()
                }
                R.id.cab_menu_share_folder -> {
                    nC.selectContactToShareFolders(handleList)
                    hideActionMode()
                }
                R.id.cab_menu_share_out -> {
                    shareNodes(requireActivity(), selected)
                    hideActionMode()
                }
                R.id.cab_menu_share_link, R.id.cab_menu_edit_link -> {
                    managerActivity?.showGetLinkActivity(selected[0].handle)
                    hideActionMode()
                }
                R.id.cab_menu_remove_link -> {
                    val nodes = ArrayList(selected)
                    managerActivity?.showConfirmationRemoveSeveralPublicLinks(nodes)
                    hideActionMode()
                }
                R.id.cab_menu_leave_share -> showConfirmationLeaveIncomingShares(requireActivity(),
                    (requireActivity() as SnackbarShower), handleList)
                R.id.cab_menu_send_to_chat -> {
                    adapter?.arrayListSelectedNodes?.let {
                        managerActivity?.attachNodesToChats(it)
                    }
                    hideActionMode()
                }
                R.id.cab_menu_trash -> managerActivity?.askConfirmationMoveToRubbish(handleList)
                R.id.cab_menu_select_all -> selectAll()
                R.id.cab_menu_clear_selection -> hideActionMode()
                R.id.cab_menu_remove_share -> managerActivity?.showConfirmationRemoveAllSharingContacts(
                    selected)
            }
            return true
        }

        override fun onDestroyActionMode(actionMode: ActionMode) {
            clearSelections()
            adapter?.isMultipleSelect = false
            if (requireActivity() is ManagerActivity) {
                managerActivity?.showFabButton()
                managerActivity?.hideTabs(false, currentTab)
                managerActivity?.showHideBottomNavigationView(false)
            }
            checkScroll()
        }

        /**
         * Check if all nodes are selected or not
         *
         * @return true if all nodes selected
         */
        protected fun notAllNodesSelected(): Boolean {
            return selected.size < (adapter?.itemCount?.minus((adapter?.placeholderCount) ?: 0)
                ?: 0)
        }
    }
}