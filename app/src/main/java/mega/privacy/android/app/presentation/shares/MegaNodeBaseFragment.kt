package mega.privacy.android.app.presentation.shares

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.view.ActionMode
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.components.NewGridRecyclerView
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.observeDragSupportEvents
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.putThumbnailLocation
import mega.privacy.android.app.components.scrollBar.FastScroller
import mega.privacy.android.app.databinding.FragmentFileBrowserBinding
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.imageviewer.ImageViewerActivity.Companion.getIntentForParentNode
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.adapters.MegaNodeAdapter
import mega.privacy.android.app.main.adapters.RotatableAdapter
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.dialog.removelink.RemovePublicLinkDialogFragment
import mega.privacy.android.app.main.dialog.rubbishbin.ConfirmMoveToRubbishBinDialogFragment
import mega.privacy.android.app.main.dialog.shares.RemoveAllSharingContactDialogFragment
import mega.privacy.android.app.main.managerSections.RotatableFragment
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.manager.model.Tab
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaNodeUtil.manageTextFileIntent
import mega.privacy.android.app.utils.MegaNodeUtil.manageURLNode
import mega.privacy.android.app.utils.MegaNodeUtil.onNodeTapped
import mega.privacy.android.app.utils.MegaNodeUtil.shareNodes
import mega.privacy.android.app.utils.MegaNodeUtil.showConfirmationLeaveIncomingShares
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.displayMetrics
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.SortOrder
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
        get() = megaNodeAdapter?.itemCount ?: 0

    /**
     * viewModel responsible for sorting the list
     */
    protected val sortByHeaderViewModel by viewModels<SortByHeaderViewModel>()

    /**
     * Adapter holding the list of nodes
     */
    protected var megaNodeAdapter: MegaNodeAdapter? = null

    /**
     * Activity bound to the fragment
     */
    protected var managerActivity: ManagerActivity? = null

    /**
     * Contextual action mode of the fragment
     */
    protected var actionMode: ActionMode? = null

    /** UI Components*/
    private var fastScroller: FastScroller? = null

    /**
     * The [NewGridRecyclerView] to display the list of items from [megaNodeAdapter]
     */
    protected var recyclerView: NewGridRecyclerView? = null

    /**
     * The [ImageView] that is displayed when there are no items in [megaNodeAdapter]
     */
    protected var emptyListImageView: ImageView? = null

    /**
     * The [TextView] which displays warning message to the user
     */
    protected var warningTextView: TextView? = null

    /**
     * The [TextView] that is displayed when there are no items in [megaNodeAdapter]
     */
    private var emptyListTextView: TextView? = null

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
    protected abstract val sortOrder: SortOrder

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

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView?.let { observeDragSupportEvents(viewLifecycleOwner, it, viewerFrom) }
        checkScroll()
    }

    /**
     * onAttach
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ManagerActivity) {
            managerActivity = context
        }
    }

    /**
     * onDestroy
     */
    override fun onDestroy() {
        megaNodeAdapter?.clearTakenDownDialog()
        super.onDestroy()
    }

    /**
     * getAdapter
     */
    override fun getAdapter(): RotatableAdapter? = megaNodeAdapter

    /**
     * activateActionMode
     */
    override fun activateActionMode() {
        if (megaNodeAdapter?.isMultipleSelect == false)
            megaNodeAdapter?.isMultipleSelect = true
    }

    /**
     * multipleItemClick
     */
    override fun multipleItemClick(position: Int) {
        megaNodeAdapter?.toggleSelection(position)
    }

    /**
     * reselectUnHandledSingleItem
     */
    override fun reselectUnHandledSingleItem(position: Int) {
        megaNodeAdapter?.reselectUnHandledSingleItem(position)
    }

    /**
     * updateActionModeTitle
     */
    override fun updateActionModeTitle() {
        if (actionMode == null || activity == null || megaNodeAdapter == null)
            return

        val files = megaNodeAdapter?.selectedNodes?.filter { it.isFile }?.size ?: 0
        val folders = megaNodeAdapter?.selectedNodes?.filter { it.isFolder }?.size ?: 0

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
     * Establishes the UI
     */
    protected fun setupUI(inflater: LayoutInflater, container: ViewGroup?): View {
        val binding = FragmentFileBrowserBinding.inflate(inflater, container, false)

        recyclerView = binding.fileBrowserRecyclerView
        fastScroller = binding.fileBrowserFastScroller
        emptyListImageView = binding.fileBrowserEmptyListImage
        emptyListTextView = binding.fileBrowserEmptyListText
        warningTextView = binding.textWarningMessage

        setupFastScroller()
        setupRecyclerView()

        return binding.root
    }

    /**
     * Establishes the [FastScroller] for the [NewGridRecyclerView]
     */
    private fun setupFastScroller() = fastScroller?.setRecyclerView(recyclerView)

    /**
     * Establishes the [NewGridRecyclerView]
     */
    private fun setupRecyclerView() {
        recyclerView?.apply {
            setPadding(
                0,
                0,
                0,
                Util.dp2px(85.toFloat(), displayMetrics())
            )
            clipToPadding = false
            setHasFixedSize(true)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (managerActivity?.tabItemShares == currentSharesTab) {
                        checkScroll()
                    }
                }
            })
        }
    }

    /**
     * Method to update an item when a nickname is added, updated or removed from a contact.
     *
     * @param contactHandle Contact ID.
     */
    fun updateContact(contactHandle: Long) {
        megaNodeAdapter?.updateItem(contactHandle)
    }

    /**
     * Select all items
     */
    fun selectAll() {
        megaNodeAdapter?.let {
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
        if (megaNodeAdapter?.isMultipleSelect == true) {
            megaNodeAdapter?.isMultipleSelect = false
            actionMode?.finish()
        }
    }

    /**
     * Clear the selected nodes
     */
    fun clearSelections() {
        if (megaNodeAdapter?.isMultipleSelect == true)
            megaNodeAdapter?.clearSelections()
    }

    /**
     * Set the visibility of the fast scroller
     */
    fun visibilityFastScroller() {
        fastScroller?.visibility =
            if (megaNodeAdapter == null || (megaNodeAdapter
                    ?: return).itemCount < Constants.MIN_ITEMS_SCROLLBAR
            )
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
                    || megaNodeAdapter?.isMultipleSelect == true
        managerActivity?.changeAppBarElevation(withElevation)
    }

    /**
     * Display the empty view or not
     */
    private fun checkEmptyView() {
        if (megaNodeAdapter?.itemCount == 0) {
            recyclerView?.visibility = View.GONE
            emptyListImageView?.visibility = View.VISIBLE
        } else {
            recyclerView?.visibility = View.VISIBLE
            emptyListImageView?.visibility = View.GONE
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

            (mimeType.isVideoMimeType || mimeType.isAudio) -> {
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
                    putExtra("placeholder", megaNodeAdapter?.placeholderCount ?: 0)
                    putExtra("parentNodeHandle", parentHandle)
                    putExtra("orderGetChildren", sortOrder)
                    putExtra("adapterType", fragmentAdapter)
                    putExtra("HANDLE", node.handle)
                    putExtra("FILENAME", node.name)

                    val localPath = FileUtil.getLocalFile(node)
                    if (localPath != null) {

                        val mediaFile = File(localPath)
                        setDataAndType(
                            FileProvider.getUriForFile(
                                requireContext(),
                                Constants.AUTHORITY_STRING_FILE_PROVIDER,
                                mediaFile
                            ),
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
                            managerActivity?.showSnackbar(
                                Constants.SNACKBAR_TYPE,
                                getString(R.string.general_text_error),
                                MegaApiJava.INVALID_HANDLE
                            )
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
                            FileProvider.getUriForFile(
                                requireContext(),
                                Constants.AUTHORITY_STRING_FILE_PROVIDER,
                                mediaFile
                            ),
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
                            managerActivity?.showSnackbar(
                                Constants.SNACKBAR_TYPE,
                                getString(R.string.general_text_error),
                                MegaApiJava.INVALID_HANDLE
                            )
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
                    onNodeTapped(
                        requireActivity(),
                        node,
                        { node: MegaNode -> it.saveNodeByTap(node) },
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
                putThumbnailLocation(
                    intent,
                    recyclerView ?: return,
                    position,
                    viewerFrom,
                    megaNodeAdapter ?: return
                )
                startActivity(intent)
                managerActivity?.overridePendingTransition(0, 0)
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.intent_not_available),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Setup the empty view
     */
    protected fun setFinalEmptyView(initialText: String?) {
        var text = initialText
            ?: run {
                emptyListImageView?.setImageResource(
                    if (Util.isScreenInPortrait(requireContext()))
                        R.drawable.empty_folder_portrait
                    else R.drawable.empty_folder_landscape
                )
                getString(R.string.file_browser_empty_folder_new)
            }

        try {
            text = text.replace(
                "[A]", "<font color=\'"
                        + getColorHexString(requireContext(), R.color.grey_900_grey_100)
                        + "\'>"
            )
            text = text.replace("[/A]", "</font>")
            text = text.replace(
                "[B]", "<font color=\'"
                        + getColorHexString(requireContext(), R.color.grey_300_grey_600)
                        + "\'>"
            )
            text = text.replace("[/B]", "</font>")
        } catch (e: Exception) {
            Timber.w(e, "Exception formatting text")
        }
        emptyListTextView?.text = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
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

        /**
         * onCreateActionMode
         */
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

        /**
         * onPrepareActionMode
         */
        override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
            selected = megaNodeAdapter?.selectedNodes ?: return false
            menu.findItem(R.id.cab_menu_share_link).title =
                resources.getQuantityString(R.plurals.get_links, selected.size)
            return false
        }

        /**
         * onActionItemClicked
         */
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            Timber.d("onActionItemClicked")

            val handleList = ArrayList<Long>().apply { addAll(selected.map { it.handle }) }

            val nC = NodeController(requireActivity())
            when (item.itemId) {
                R.id.cab_menu_download -> {
                    managerActivity?.saveNodesToDevice(
                        nodes = selected,
                        highPriority = false,
                        isFolderLink = false,
                        fromMediaViewer = false,
                        fromChat = false,
                    )
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
                    RemovePublicLinkDialogFragment.newInstance(selected.map { it.handle })
                        .show(
                            requireActivity().supportFragmentManager,
                            RemovePublicLinkDialogFragment.TAG
                        )
                    hideActionMode()
                }
                R.id.cab_menu_leave_share -> showConfirmationLeaveIncomingShares(
                    requireActivity(),
                    (requireActivity() as SnackbarShower), handleList
                )
                R.id.cab_menu_send_to_chat -> {
                    megaNodeAdapter?.arrayListSelectedNodes?.let {
                        managerActivity?.attachNodesToChats(it)
                    }
                    hideActionMode()
                }
                R.id.cab_menu_trash -> {
                    if (handleList.isNotEmpty()) {
                        ConfirmMoveToRubbishBinDialogFragment.newInstance(handleList)
                            .show(
                                requireActivity().supportFragmentManager,
                                ConfirmMoveToRubbishBinDialogFragment.TAG
                            )
                    }
                }
                R.id.cab_menu_select_all -> selectAll()
                R.id.cab_menu_clear_selection -> hideActionMode()
                R.id.cab_menu_remove_share ->
                    RemoveAllSharingContactDialogFragment.newInstance(selected.map { it.handle })
                        .show(childFragmentManager, RemoveAllSharingContactDialogFragment.TAG)
            }
            return true
        }

        /**
         * onDestroyActionMode
         */
        override fun onDestroyActionMode(actionMode: ActionMode) {
            if (isAdded) {
                clearSelections()
                megaNodeAdapter?.isMultipleSelect = false
                if (requireActivity() is ManagerActivity) {
                    managerActivity?.showFabButton()
                    managerActivity?.hideTabs(false, currentTab)
                    managerActivity?.showHideBottomNavigationView(false)
                }
                checkScroll()
            }
        }

        /**
         * Check if all nodes are selected or not
         *
         * @return true if all nodes selected
         */
        protected fun notAllNodesSelected(): Boolean {
            return selected.size < (megaNodeAdapter?.itemCount?.minus(
                (megaNodeAdapter?.placeholderCount) ?: 0
            ) ?: 0)
        }
    }
}