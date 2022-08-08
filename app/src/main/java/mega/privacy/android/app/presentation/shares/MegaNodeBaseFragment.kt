package mega.privacy.android.app.presentation.shares

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

@AndroidEntryPoint
abstract class MegaNodeBaseFragment : RotatableFragment() {

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    protected var adapter: MegaNodeAdapter? = null

    protected var managerActivity: ManagerActivity? = null

    var actionMode: ActionMode? = null
        protected set

    protected val sortByHeaderViewModel by viewModels<SortByHeaderViewModel>()

    protected var fastScroller: FastScroller? = null
    protected var recyclerView: RecyclerView? = null
    protected var mLayoutManager: LinearLayoutManager? = null
    protected var gridLayoutManager: CustomizedGridLayoutManager? = null
    protected var emptyImageView: ImageView? = null
    protected var emptyLinearLayout: LinearLayout? = null
    protected var emptyTextViewFirst: TextView? = null


    abstract fun onBackPressed(): Int
    abstract fun itemClick(position: Int)
    protected abstract val viewerFrom: Int
    protected abstract val intentOrder: Int

    /**
     * Navigates to a new child folder.
     *
     * @param node The folder node.
     */
    abstract fun navigateToFolder(node: MegaNode)

    abstract fun updateContact(contactHandle: Long)

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

    protected abstract inner class BaseActionBarCallBack(private val currentTab: Tab) :
        ActionMode.Callback {
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
            selected = adapter!!.selectedNodes
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
                    managerActivity!!.saveNodesToDevice(selected, false, false, false, false)
                    hideActionMode()
                }
                R.id.cab_menu_rename -> {
                    managerActivity!!.showRenameDialog(selected[0])
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
                    managerActivity!!.showGetLinkActivity(selected[0].handle)
                    hideActionMode()
                }
                R.id.cab_menu_remove_link -> {
                    val nodes = ArrayList(selected)
                    managerActivity!!.showConfirmationRemoveSeveralPublicLinks(nodes)
                    hideActionMode()
                }
                R.id.cab_menu_leave_share -> showConfirmationLeaveIncomingShares(requireActivity(),
                    (requireActivity() as SnackbarShower), handleList)
                R.id.cab_menu_send_to_chat -> {
                    managerActivity!!.attachNodesToChats(adapter!!.arrayListSelectedNodes)
                    hideActionMode()
                }
                R.id.cab_menu_trash -> managerActivity!!.askConfirmationMoveToRubbish(handleList)
                R.id.cab_menu_select_all -> selectAll()
                R.id.cab_menu_clear_selection -> hideActionMode()
                R.id.cab_menu_remove_share -> managerActivity!!.showConfirmationRemoveAllSharingContacts(
                    selected)
            }
            return true
        }

        override fun onDestroyActionMode(actionMode: ActionMode) {
            clearSelections()
            adapter!!.isMultipleSelect = false
            if (requireActivity() is ManagerActivity) {
                managerActivity!!.showFabButton()
                managerActivity!!.hideTabs(false, currentTab)
                managerActivity!!.showHideBottomNavigationView(false)
            }
            checkScroll()
        }

        protected fun notAllNodesSelected(): Boolean {
            return selected!!.size < adapter!!.itemCount - adapter!!.placeholderCount
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ManagerActivity) {
            managerActivity = context
        }
    }

    override fun onDestroy() {
        if (adapter != null) {
            adapter!!.clearTakenDownDialog()
        }
        super.onDestroy()
    }

    override fun getAdapter(): RotatableAdapter {
        return adapter!!
    }

    override fun activateActionMode() {
        if (adapter != null && !adapter!!.isMultipleSelect) {
            adapter!!.isMultipleSelect = true
        }
    }

    override fun multipleItemClick(position: Int) {
        if (adapter != null) {
            adapter!!.toggleSelection(position)
        }
    }

    override fun reselectUnHandledSingleItem(position: Int) {
        if (adapter != null) {
            adapter!!.filClicked(position)
        }
    }

    override fun updateActionModeTitle() {
        if (actionMode == null || activity == null || adapter == null) {
            return
        }
        val documents = adapter!!.selectedNodes
        var files = 0
        var folders = 0
        for (document in documents) {
            if (document.isFile) {
                files++
            } else if (document.isFolder) {
                folders++
            }
        }
        val title: String
        val sum = files + folders
        title = if (files == 0 && folders == 0) {
            Integer.toString(sum)
        } else if (files == 0) {
            Integer.toString(folders)
        } else if (folders == 0) {
            Integer.toString(files)
        } else {
            Integer.toString(sum)
        }
        actionMode!!.title = title
        try {
            actionMode!!.invalidate()
        } catch (e: NullPointerException) {
            e.printStackTrace()
            Timber.e(e, "Invalidate error")
        }
    }

    /**
     * Shows the Sort by panel.
     */
    protected abstract fun showSortByPanel()
    val isMultipleSelect: Boolean
        get() = adapter != null && adapter!!.isMultipleSelect

    fun selectAll() {
        if (adapter != null) {
            if (!adapter!!.isMultipleSelect) {
                activateActionMode()
            }
            adapter!!.selectAll()
            Handler(Looper.getMainLooper()).post { updateActionModeTitle() }
        }
    }

    fun hideMultipleSelect() {
        if (adapter != null) {
            adapter!!.isMultipleSelect = false
        }
        if (actionMode != null) {
            actionMode!!.finish()
        }
    }

    fun clearSelections() {
        if (adapter != null && adapter!!.isMultipleSelect) {
            adapter!!.clearSelections()
        }
    }

    fun notifyDataSetChanged() {
        if (adapter != null) {
            adapter!!.notifyDataSetChanged()
        }
    }

    val itemCount: Int
        get() = if (adapter != null) {
            adapter!!.itemCount
        } else 0

    fun visibilityFastScroller() {
        if (adapter == null || adapter!!.itemCount < Constants.MIN_ITEMS_SCROLLBAR) {
            fastScroller!!.visibility = View.GONE
        } else {
            fastScroller!!.visibility = View.VISIBLE
        }
    }

    fun checkScroll() {
        managerActivity!!.changeAppBarElevation((recyclerView != null && recyclerView!!.canScrollVertically(
            -1)
                && recyclerView!!.visibility == View.VISIBLE)
                || adapter != null && adapter!!.isMultipleSelect)
    }

    protected fun checkEmptyView() {
        if (adapter != null && adapter!!.itemCount == 0) {
            recyclerView!!.visibility = View.GONE
            emptyImageView!!.visibility = View.VISIBLE
            emptyLinearLayout!!.visibility = View.VISIBLE
        } else {
            recyclerView!!.visibility = View.VISIBLE
            emptyImageView!!.visibility = View.GONE
            emptyLinearLayout!!.visibility = View.GONE
        }
    }

    fun openFile(node: MegaNode, fragmentAdapter: Int, position: Int) {
        val mimeType = MimeTypeList.typeForName(node.name)
        val mimeTypeType = mimeType.type
        val intent: Intent
        var internalIntent = false
        if (mimeType.isImage) {
            intent = getIntentForParentNode(
                requireContext(),
                parentHandle,
                intentOrder,
                node.handle
            )
            launchIntent(intent, true, position)
        } else if (mimeType.isVideoReproducible || mimeType.isAudio) {
            var opusFile = false
            if (mimeType.isVideoNotSupported || mimeType.isAudioNotSupported) {
                intent = Intent(Intent.ACTION_VIEW)
                val s = node.name.split("\\.".toRegex()).toTypedArray()
                opusFile = s.size > 1 && s[s.size - 1] == "opus"
            } else {
                intent = Util.getMediaIntent(requireContext(), node.name)
                internalIntent = true
            }
            intent.putExtra("position", position)
            intent.putExtra("placeholder", adapter!!.placeholderCount)
            intent.putExtra("parentNodeHandle", parentHandle)
            intent.putExtra("orderGetChildren", intentOrder)
            intent.putExtra("adapterType", fragmentAdapter)
            intent.putExtra("HANDLE", node.handle)
            intent.putExtra("FILENAME", node.name)
            val localPath = FileUtil.getLocalFile(node)
            if (localPath != null) {
                val mediaFile = File(localPath)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intent.setDataAndType(FileProvider.getUriForFile(requireContext(),
                        Constants.AUTHORITY_STRING_FILE_PROVIDER,
                        mediaFile), MimeTypeList.typeForName(node.name).type)
                } else {
                    intent.setDataAndType(Uri.fromFile(mediaFile),
                        MimeTypeList.typeForName(node.name).type)
                }
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                if (megaApi!!.httpServerIsRunning() == 0) {
                    megaApi!!.httpServerStart()
                    intent.putExtra(Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
                } else {
                    Timber.w("ERROR:httpServerAlreadyRunning")
                }
                val mi = ActivityManager.MemoryInfo()
                val activityManager =
                    requireActivity().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.getMemoryInfo(mi)
                if (mi.totalMem > Constants.BUFFER_COMP) {
                    Timber.d("total mem: %d allocate 32 MB", mi.totalMem)
                    megaApi!!.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB)
                } else {
                    Timber.d("total mem: %d allocate 16 MB", mi.totalMem)
                    megaApi!!.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB)
                }
                val url = megaApi!!.httpServerGetLocalLink(node)
                var parsedUri: Uri? = null
                if (url != null) {
                    parsedUri = Uri.parse(url)
                }
                if (parsedUri != null) {
                    intent.setDataAndType(parsedUri, mimeTypeType)
                } else {
                    Timber.e("ERROR:httpServerGetLocalLink")
                    managerActivity!!.showSnackbar(Constants.SNACKBAR_TYPE,
                        getString(R.string.general_text_error),
                        MegaApiJava.INVALID_HANDLE)
                    return
                }
            }
            if (opusFile) {
                intent.setDataAndType(intent.data, "audio/*")
            }
            launchIntent(intent, internalIntent, position)
        } else if (mimeType.isURL) {
            manageURLNode(requireContext(), megaApi!!, node)
        } else if (mimeType.isPdf) {
            Timber.d("isFile:isPdf")
            intent = Intent(requireContext(), PdfViewerActivity::class.java)
            intent.putExtra("inside", true)
            intent.putExtra("adapterType", fragmentAdapter)
            intent.putExtra("HANDLE", node.handle)
            val localPath = FileUtil.getLocalFile(node)
            if (localPath != null) {
                val mediaFile = File(localPath)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intent.setDataAndType(FileProvider.getUriForFile(requireContext(),
                        Constants.AUTHORITY_STRING_FILE_PROVIDER,
                        mediaFile), mimeTypeType)
                } else {
                    intent.setDataAndType(Uri.fromFile(mediaFile), mimeTypeType)
                }
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                if (megaApi!!.httpServerIsRunning() == 0) {
                    megaApi!!.httpServerStart()
                    intent.putExtra(Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
                }
                val mi = ActivityManager.MemoryInfo()
                val activityManager =
                    requireActivity().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.getMemoryInfo(mi)
                if (mi.totalMem > Constants.BUFFER_COMP) {
                    Timber.d("Total mem: %d allocate 32 MB", mi.totalMem)
                    megaApi!!.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB)
                } else {
                    Timber.d("Total mem: %d allocate 16 MB", mi.totalMem)
                    megaApi!!.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB)
                }
                val url = megaApi!!.httpServerGetLocalLink(node)
                var parsedUri: Uri? = null
                if (url != null) {
                    parsedUri = Uri.parse(url)
                }
                if (parsedUri != null) {
                    intent.setDataAndType(parsedUri, mimeTypeType)
                } else {
                    Timber.e("ERROR:httpServerGetLocalLink")
                    managerActivity!!.showSnackbar(Constants.SNACKBAR_TYPE,
                        getString(R.string.general_text_error),
                        MegaApiJava.INVALID_HANDLE)
                    return
                }
            }
            launchIntent(intent, false, position)
        } else if (mimeType.isOpenableTextFile(node.size)) {
            manageTextFileIntent(requireContext(), node, fragmentAdapter)
        } else {
            Timber.d("itemClick:isFile:otherOption")
            onNodeTapped(requireActivity(),
                node,
                { node: MegaNode? -> managerActivity!!.saveNodeByTap(node) },
                managerActivity!!,
                managerActivity!!)
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
                putThumbnailLocation(intent, recyclerView!!, position, viewerFrom, adapter!!)
                startActivity(intent)
                managerActivity!!.overridePendingTransition(0, 0)
            } else {
                Toast.makeText(requireContext(),
                    StringResourcesUtils.getString(R.string.intent_not_available),
                    Toast.LENGTH_LONG).show()
            }
        }
    }

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
        if (adapter != null) {
            adapter!!.adapterType = MegaNodeAdapter.ITEM_VIEW_TYPE_LIST
        }
        return v
    }

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
        if (adapter != null) {
            adapter!!.adapterType = MegaNodeAdapter.ITEM_VIEW_TYPE_GRID
        }
        return v
    }

    /**
     * Gets the current shares tab depending on the current Fragment instance.
     *
     * @return The current shares tab.
     */
    protected abstract val currentSharesTab: SharesTab
    private fun setRecyclerView() {
        recyclerView!!.setPadding(0,
            0,
            0,
            Util.dp2px(MARGIN_BOTTOM_LIST.toFloat(), resources.displayMetrics))
        recyclerView!!.setHasFixedSize(true)
        recyclerView!!.clipToPadding = false
        recyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val tab = currentSharesTab
                if (managerActivity!!.tabItemShares === tab) {
                    checkScroll()
                }
            }
        })
        fastScroller!!.setRecyclerView(recyclerView)
    }

    private val generalEmptyView: String
        private get() {
            if (Util.isScreenInPortrait(requireContext())) {
                emptyImageView!!.setImageResource(R.drawable.empty_folder_portrait)
            } else {
                emptyImageView!!.setImageResource(R.drawable.empty_folder_landscape)
            }
            return StringResourcesUtils.getString(R.string.file_browser_empty_folder_new)
        }

    protected fun setFinalEmptyView(text: String?) {
        var text = text
        if (text == null) {
            text = generalEmptyView
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
        emptyTextViewFirst!!.text = HtmlCompat.fromHtml(text!!, HtmlCompat.FROM_HTML_MODE_LEGACY)
        checkEmptyView()
    }

    protected abstract val parentHandle: Long
    protected fun hideActionMode() {
        clearSelections()
        hideMultipleSelect()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeDragSupportEvents(viewLifecycleOwner, recyclerView!!, viewerFrom)
        checkScroll()
    }

    companion object {
        private const val MARGIN_BOTTOM_LIST = 85
    }
}