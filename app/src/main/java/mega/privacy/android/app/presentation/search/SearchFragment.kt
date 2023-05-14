package mega.privacy.android.app.presentation.search

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
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.view.ActionMode
import androidx.core.content.FileProvider
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.components.NewGridRecyclerView
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.components.dragger.DragToExitSupport
import mega.privacy.android.app.components.scrollBar.FastScroller
import mega.privacy.android.app.databinding.FragmentSearchBinding
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.PdfViewerActivity
import mega.privacy.android.app.main.adapters.MegaNodeAdapter
import mega.privacy.android.app.main.adapters.RotatableAdapter
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.managerSections.RotatableFragment
import mega.privacy.android.app.presentation.clouddrive.FileBrowserViewModel
import mega.privacy.android.app.presentation.inbox.InboxViewModel
import mega.privacy.android.app.presentation.manager.ManagerViewModel
import mega.privacy.android.app.presentation.rubbishbin.RubbishBinViewModel
import mega.privacy.android.app.presentation.search.model.SearchState
import mega.privacy.android.app.presentation.shares.incoming.IncomingSharesViewModel
import mega.privacy.android.app.presentation.shares.links.LegacyLinksViewModel
import mega.privacy.android.app.presentation.shares.outgoing.OutgoingSharesViewModel
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.MegaNodeUtil.allHaveOwnerAccessAndNotTakenDown
import mega.privacy.android.app.utils.MegaNodeUtil.areAllFileNodesAndNotTakenDown
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.hideKeyboard
import mega.privacy.android.app.utils.displayMetrics
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.preference.ViewType
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import timber.log.Timber
import java.io.File
import java.util.Collections
import javax.inject.Inject

/**
 * Fragment is for search
 */
@AndroidEntryPoint
class SearchFragment : RotatableFragment() {

    companion object {
        /**
         * Returns instance of [SearchFragment]
         */
        @JvmStatic
        fun newInstance(): SearchFragment = SearchFragment()
    }

    /**
     * MegaApi
     */
    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    private lateinit var adapter: MegaNodeAdapter

    private val sortByHeaderViewModel: SortByHeaderViewModel by activityViewModels()
    private val managerViewModel: ManagerViewModel by activityViewModels()
    private val fileBrowserViewModel: FileBrowserViewModel by activityViewModels()
    private val incomingSharesViewModel: IncomingSharesViewModel by activityViewModels()
    private val outgoingSharesViewModel: OutgoingSharesViewModel by activityViewModels()
    private val inboxViewModel: InboxViewModel by activityViewModels()
    private val legacyLinksViewModel: LegacyLinksViewModel by activityViewModels()
    private val rubbishBinViewModel: RubbishBinViewModel by activityViewModels()
    private val searchViewModel: SearchViewModel by activityViewModels()

    private var recyclerView: NewGridRecyclerView? = null
    private lateinit var emptyImageView: ImageView
    private lateinit var emptyTextView: LinearLayout
    private lateinit var emptyTextViewFirst: TextView
    private lateinit var fastScroller: FastScroller
    private lateinit var contentLayout: RelativeLayout
    private lateinit var searchProgressBar: ProgressBar

    //Bindings
    private var _binding: FragmentSearchBinding? = null
    private val binding: FragmentSearchBinding
        get() = _binding!!

    private var gridLayoutManager: CustomizedGridLayoutManager? = null
    private val outMetrics: DisplayMetrics by lazy {
        DisplayMetrics()
    }

    private var layoutManager: LinearLayoutManager? = null

    private var trashIcon: MenuItem? = null

    private var actionMode: ActionMode? = null

    private lateinit var activityContext: Context

    private val itemDecoration: PositionDividerItemDecoration by lazy(LazyThreadSafetyMode.NONE) {
        PositionDividerItemDecoration(requireContext(), displayMetrics())
    }

    /**
     * Returns [SearchState] of [SearchViewModel]
     */
    private fun state() = searchViewModel.state.value

    /**
     * Returns instance of [ManagerActivity] from [requireActivity]
     */
    private val managerActivity: ManagerActivity
        get() = (activityContext as ManagerActivity)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activityContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        Timber.d("onCreateView")

        if (megaApi.rootNode == null) {
            return null
        }
        requireActivity().windowManager.defaultDisplay?.getMetrics(outMetrics)
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        initViews()
        setupRecyclerView()
        initObservers()

        if (this::adapter.isInitialized.not()) {
            adapter = MegaNodeAdapter(
                requireActivity(),
                this@SearchFragment,
                mutableListOf(),
                state().searchParentHandle,
                recyclerView,
                Constants.SEARCH_ADAPTER,
                if (state().currentViewType == ViewType.LIST) MegaNodeAdapter.ITEM_VIEW_TYPE_LIST else MegaNodeAdapter.ITEM_VIEW_TYPE_GRID,
                sortByHeaderViewModel
            )
            adapter.isMultipleSelect = false
        }
        recyclerView?.adapter = adapter
        return binding.root
    }

    /**
     * Init observers
     */
    private fun initObservers() {
        sortByHeaderViewModel.showDialogEvent.observe(
            viewLifecycleOwner,
            EventObserver { managerActivity.showNewSortByPanel(Constants.ORDER_CLOUD) })

        sortByHeaderViewModel.orderChangeEvent.observe(viewLifecycleOwner, EventObserver {
            hideMultipleSelect()
            refresh()
        })

        searchViewModel.updateNodes.observe(
            viewLifecycleOwner,
            EventObserver {
                refresh()
            }
        )
        searchViewModel.stateLiveData.observe(
            viewLifecycleOwner,
            EventObserver { state ->
                updateSearchProgressView(state.isInProgress)
                state.nodes?.let {
                    setNodes(it)
                    switchViewType()
                }
            }
        )

        viewLifecycleOwner.collectFlow(sortByHeaderViewModel.state) { state ->
            handleViewType(state.viewType)
        }
    }

    /**
     * init views
     */
    private fun initViews() {
        recyclerView = binding.fileGridViewBrowser
        emptyImageView = binding.fileGridEmptyImage
        emptyTextView = binding.fileGridEmptyText
        fastScroller = binding.fastscroll
        fastScroller.setRecyclerView(binding.fileGridViewBrowser)
        contentLayout = binding.contentLayout
        searchProgressBar = binding.layoutGeneralProgressBar.progressbar
        emptyImageView = binding.fileGridEmptyImage
        emptyTextView = binding.fileGridEmptyText
        emptyTextViewFirst = binding.fileGridEmptyTextFirst
    }

    /**
     * Establishes the [NewGridRecyclerView]
     */
    private fun setupRecyclerView() {
        recyclerView?.let {
            it.itemAnimator = DefaultItemAnimator()
            it.setPadding(0, 0, 0, Util.scaleHeightPx(85, displayMetrics()))
            it.clipToPadding = false
            it.setHasFixedSize(true)
            it.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    checkScroll()
                }
            })
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val query = state().searchQuery
        query?.let {
            if (isAdded) {
                newSearchNodesTask()
                managerActivity.showFabButton()
            }
        }
    }

    /**
     * This will update progress of search
     * @param inProgress if search is in progress
     */
    private fun updateSearchProgressView(inProgress: Boolean) {
        recyclerView?.let {
            contentLayout.isEnabled = !inProgress
            contentLayout.alpha = if (inProgress) 0.4f else 1f
            searchProgressBar.isVisible = inProgress
            it.isGone = inProgress
        }
    }

    /**
     * Disables select mode by clearing selections and resetting selected items.
     */
    private fun closeSelectMode() {
        clearSelections()
        hideMultipleSelect()
        resetSelectedItems()
    }

    /**
     * Clear all selected items
     **/
    private fun clearSelections() {
        if (adapter.isMultipleSelect) {
            adapter.clearSelections()
        }
    }

    /**
     * Disable selection
     **/
    fun hideMultipleSelect() {
        adapter.isMultipleSelect = false
        actionMode?.finish()
    }

    private inner class ActionBarCallback : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val inflater = mode?.menuInflater
            inflater?.inflate(R.menu.file_browser_action, menu)
            trashIcon = menu?.findItem(R.id.cab_menu_trash)
            managerActivity.hideFabButton()
            managerActivity.setTextSubmitted()
            checkScroll()
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val selected = adapter.selectedNodes
            menu?.findItem(R.id.cab_menu_share_link)?.title =
                requireContext().resources.getQuantityString(R.plurals.get_links, selected.size)

            val unselect = menu?.findItem(R.id.cab_menu_unselect_all)
            var showDownload = false
            var showSendToChat = false
            var showRename = false
            var showCopy = false
            var showMove = false
            var showLink = false
            var showEditLink = false
            var showRemoveLink = false
            var showTrash = false
            var itemsSelected = false

            // Rename
            if ((selected.size == 1) && (megaApi.checkAccessErrorExtended(
                    selected[0],
                    MegaShare.ACCESS_FULL
                ).errorCode == MegaError.API_OK)
            ) {
                showRename = true
            }
            // Link
            if (selected.size == 1 && megaApi.checkAccessErrorExtended(
                    selected[0],
                    MegaShare.ACCESS_OWNER
                ).errorCode == MegaError.API_OK
            ) {
                if (!selected[0].isTakenDown) {
                    if (selected[0].isExported) {
                        //Node has public link
                        showRemoveLink = true
                        showEditLink = true
                    } else {
                        showLink = true
                    }
                }
            } else if (allHaveOwnerAccessAndNotTakenDown(selected)) {
                showLink = true
            }

            if (selected.isNotEmpty()) {
                showDownload = true
                showTrash = true
                showMove = true
                showCopy = true

                //showSendToChat
                showSendToChat = areAllFileNodesAndNotTakenDown(selected)
                for (node in selected) {
                    if (megaApi.checkMoveErrorExtended(
                            node,
                            megaApi.rubbishNode
                        ).errorCode != MegaError.API_OK
                    ) {
                        showTrash = false
                        showMove = false
                        break
                    }
                    if (node.isTakenDown) {
                        showDownload = false
                        showCopy = false
                        showSendToChat = false
                    }
                }
                if (selected.size == adapter.itemCount) {
                    menu?.findItem(R.id.cab_menu_select_all)?.isVisible = false
                    unselect?.title = getString(R.string.action_unselect_all)
                    unselect?.isVisible = true
                } else if (selected.size == 1) {
                    menu?.findItem(R.id.cab_menu_select_all)?.isVisible = true
                    unselect?.title = getString(R.string.action_unselect_all)
                    unselect?.isVisible = true

                    val handle = selected[0].handle
                    var parent = megaApi.getNodeByHandle(handle)
                    while (megaApi.getParentNode(parent) != null) {
                        parent = megaApi.getParentNode(parent)
                    }

                    if (parent?.handle != megaApi.rubbishNode?.handle) {
                        trashIcon?.title = getString(R.string.context_move_to_trash)
                    } else {
                        trashIcon?.title = getString(R.string.context_remove)
                    }
                } else {
                    menu?.findItem(R.id.cab_menu_select_all)?.isVisible = true
                    unselect?.title = getString(R.string.action_unselect_all)
                    unselect?.isVisible = true
                    for (i in selected) {
                        val handle = i.handle
                        var parent = megaApi.getNodeByHandle(handle)
                        while (megaApi.getParentNode(parent) != null) {
                            parent = megaApi.getParentNode(parent)
                        }
                        if (parent?.handle != megaApi.rubbishNode?.handle) {
                            itemsSelected = true
                        }
                    }
                    trashIcon?.title =
                        if (!itemsSelected) getString(R.string.context_remove) else getString(R.string.context_move_to_trash)
                }
            } else {
                menu?.findItem(R.id.cab_menu_select_all)?.isVisible = true
                menu?.findItem(R.id.cab_menu_unselect_all)?.isVisible = false
            }
            var alwaysCount = 0

            if (showDownload) alwaysCount++
            menu?.findItem(R.id.cab_menu_download)?.setVisible(showDownload)
                ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

            if (showSendToChat) alwaysCount++
            menu?.findItem(R.id.cab_menu_send_to_chat)?.setVisible(showSendToChat)
                ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

            if (showLink) {
                alwaysCount++
                menu?.findItem(R.id.cab_menu_share_link_remove)
                    ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                menu?.findItem(R.id.cab_menu_share_link)
                    ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            } else {
                menu?.findItem(R.id.cab_menu_share_link)
                    ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            }
            menu?.findItem(R.id.cab_menu_share_link)?.isVisible = showLink
            menu?.findItem(R.id.cab_menu_rename)?.isVisible = showRename

            if (showMove) alwaysCount++
            menu?.findItem(R.id.cab_menu_move)?.setVisible(showMove)
                ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            menu?.findItem(R.id.cab_menu_copy)?.isVisible = showCopy
            menu?.findItem(R.id.cab_menu_copy)
                ?.setShowAsAction(
                    if (showCopy && alwaysCount < CloudStorageOptionControlUtil.MAX_ACTION_COUNT)
                        MenuItem.SHOW_AS_ACTION_ALWAYS
                    else
                        MenuItem.SHOW_AS_ACTION_NEVER
                )
            menu?.findItem(R.id.cab_menu_share_link_remove)?.isVisible = showRemoveLink

            if (showRemoveLink) {
                menu?.findItem(R.id.cab_menu_share_link)
                    ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                menu?.findItem(R.id.cab_menu_share_link_remove)
                    ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            } else {
                menu?.findItem(R.id.cab_menu_share_link_remove)
                    ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

            }
            menu?.findItem(R.id.cab_menu_edit_link)?.isVisible = showEditLink
            menu?.findItem(R.id.cab_menu_trash)?.isVisible = showTrash

            menu?.findItem(R.id.cab_menu_leave_multiple_share)?.isVisible = false
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            val documents = adapter.selectedNodes

            when (item?.itemId) {
                R.id.cab_menu_download -> {
                    managerActivity.saveNodesToDevice(
                        documents, false, false, false, false
                    )
                    closeSelectMode()
                }
                R.id.cab_menu_rename -> {
                    if (documents.size == 1) {
                        managerActivity.showRenameDialog(documents[0])
                    }
                    closeSelectMode()
                }
                R.id.cab_menu_copy -> {
                    val handleList = arrayListOf<Long>()
                    for (document in documents) {
                        handleList.add(document.handle)
                    }
                    val nC = NodeController(requireActivity())
                    nC.chooseLocationToCopyNodes(handleList)
                    closeSelectMode()
                }
                R.id.cab_menu_move -> {
                    val handleList = arrayListOf<Long>()
                    for (document in documents) {
                        handleList.add(document.handle)
                    }
                    val nC = NodeController(requireActivity())
                    nC.chooseLocationToMoveNodes(handleList)
                    closeSelectMode()
                }
                R.id.cab_menu_share_link -> {
                    managerActivity.showGetLinkActivity(documents)
                    closeSelectMode()
                }

                R.id.cab_menu_share_link_remove -> {
                    Timber.d("Remove public link option")
                    if (documents[0] == null) {
                        Timber.w("The selected node is NULL")
                    } else {
                        managerActivity.showConfirmationRemovePublicLink(documents[0])
                        closeSelectMode()
                    }
                }
                R.id.cab_menu_edit_link -> {
                    Timber.d("Edit link option")
                    if (documents[0] == null) {
                        Timber.w("The selected node is NULL")
                    } else {
                        managerActivity.showGetLinkActivity(documents.get(0).handle)
                        closeSelectMode()
                    }
                }
                R.id.cab_menu_send_to_chat -> {
                    Timber.d("Send files to chat")
                    managerActivity.attachNodesToChats(adapter.arrayListSelectedNodes)
                    closeSelectMode()
                }
                R.id.cab_menu_trash -> {
                    val handleList = arrayListOf<Long>()
                    for (document in documents) {
                        handleList.add(document.handle)
                    }
                    managerActivity.askConfirmationMoveToRubbish(handleList)
                }
                R.id.cab_menu_select_all -> {
                    selectAll()
                }
                R.id.cab_menu_unselect_all -> {
                    closeSelectMode()
                }
            }
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            Timber.d("onDestroyActionMode")
            clearSelections()
            adapter.isMultipleSelect = false
            managerActivity.showFabButton()
            checkScroll()
            managerActivity.requestSearchViewFocus()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        if (this::adapter.isInitialized) adapter.clearTakenDownDialog()
        super.onDestroy()
    }

    /**
     * This method checks scroll of recycler view
     */
    fun checkScroll() {
        recyclerView?.let {
            managerActivity.changeAppBarElevation(it.canScrollVertically(-1) || adapter.isMultipleSelect)
        }
    }

    /**
     * Select all items from adapter
     */
    fun selectAll() {
        Timber.d("selectAll")
        if (this::adapter.isInitialized) {
            if (adapter.isMultipleSelect) {
                adapter.selectAll()
            } else {
                adapter.isMultipleSelect = true
                adapter.selectAll()

                actionMode = managerActivity.startSupportActionMode(ActionBarCallback())
            }

            Handler(Looper.getMainLooper()).post { updateActionModeTitle() }
        }
    }

    /**
     * This function start searching of new nodes
     */
    private fun newSearchNodesTask() {
        searchViewModel.performSearch(
            fileBrowserViewModel.state.value.fileBrowserHandle,
            rubbishBinViewModel.state.value.rubbishBinHandle,
            inboxViewModel.state.value.inboxHandle,
            incomingSharesViewModel.state.value.incomingHandle,
            outgoingSharesViewModel.state.value.outgoingHandle,
            legacyLinksViewModel.state.value.linksHandle,
            managerViewModel.state.value.isFirstNavigationLevel
        )
    }

    override fun getAdapter(): RotatableAdapter {
        return adapter
    }

    override fun activateActionMode() {
        if (!adapter.isMultipleSelect) {
            hideKeyboard(requireActivity())
            adapter.isMultipleSelect = true
            actionMode = managerActivity.startSupportActionMode(ActionBarCallback())
        }
    }

    override fun multipleItemClick(position: Int) = adapter.toggleSelection(position)

    override fun reselectUnHandledSingleItem(position: Int) =
        adapter.reselectUnHandledSingleItem(position)

    override fun updateActionModeTitle() {
        actionMode?.let {
            val documents = adapter.selectedNodes
            var files = 0
            var folders = 0
            for (document in documents) {
                if (document.isFile) files++
                else if (document.isFolder) folders++
            }
            val sum = files + folders
            it.title = if (files == 0 && folders == 0) "0"
            else if (files == 0) "$folders"
            else if (folders == 0) "$files"
            else "$sum"

            runCatching {
                it.invalidate()
            }.getOrElse { throwable ->
                Timber.e(throwable)
            }
        }
    }

    /**
     * This method set nodes and updates the adapter
     * @param nodes List of Mega Nodes
     */
    private fun setNodes(nodes: List<MegaNode>) {
        val mutableListNodes = ArrayList(nodes)
        adapter.setNodes(mutableListNodes)
        visibilityFastScroller()
        if (adapter.itemCount == 0) {
            recyclerView?.visibility = View.GONE
            emptyImageView.visibility = View.VISIBLE
            emptyTextView.visibility = View.VISIBLE

            if (state().searchParentHandle == -1L) {
                if (requireContext().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    emptyImageView.setImageResource(R.drawable.empty_folder_landscape)
                } else {
                    emptyImageView.setImageResource(R.drawable.empty_folder_portrait)
                }
                emptyTextViewFirst.setText(R.string.no_results_found)
            } else if (megaApi.rootNode?.handle == state().searchParentHandle) {
                if (requireContext().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    emptyImageView.setImageResource(R.drawable.cloud_empty_landscape)
                } else {
                    emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive)
                }
                var textToShow = String.format(getString(R.string.context_empty_cloud_drive))
                runCatching {
                    textToShow = textToShow.replace(
                        "[A]", "<font color=\'"
                                + ColorUtils.getColorHexString(
                            requireContext(),
                            R.color.grey_900_grey_100
                        )
                                + "\'>"
                    ).replace("[/A]", "</font>").replace(
                        "[B]", "<font color=\'"
                                + ColorUtils.getColorHexString(
                            requireContext(),
                            R.color.grey_300_grey_600
                        )
                                + "\'>"
                    ).replace("[/B]", "</font>")
                }.getOrElse { }
                val result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY)
                emptyTextViewFirst.text = result
            } else {
                if (requireContext().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder)
                } else {
                    emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder)
                }
                var textToShow = String.format(getString(R.string.file_browser_empty_folder_new))
                runCatching {
                    textToShow = textToShow.replace(
                        "[A]", "<font color=\'"
                                + ColorUtils.getColorHexString(
                            requireContext(),
                            R.color.grey_900_grey_100
                        )
                                + "\'>"
                    ).replace("[/A]", "</font>").replace(
                        "[B]", "<font color=\'"
                                + ColorUtils.getColorHexString(
                            requireContext(),
                            R.color.grey_300_grey_600
                        )
                                + "\'>"
                    ).replace("[/B]", "</font>")
                }.getOrElse { }
                val result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY)
                emptyTextViewFirst.text = result
            }
        } else {
            recyclerView?.visibility = View.VISIBLE
            emptyImageView.visibility = View.GONE
            emptyTextView.visibility = View.GONE
        }

        if (isWaitingForSearchedNodes) {
            reDoTheSelectionAfterRotation()
            reSelectUnhandledItem()
        }
    }

    /**
     * This function sets visibility for [fastScroller]
     */
    private fun visibilityFastScroller() {
        if (this::adapter.isInitialized.not()) {
            fastScroller.visibility = View.GONE
        } else {
            if (adapter.itemCount < Constants.MIN_ITEMS_SCROLLBAR) {
                fastScroller.visibility = View.GONE
            } else {
                fastScroller.visibility = View.VISIBLE
            }
        }
    }

    /**
     * This function refresh the search
     */
    fun refresh() {
        Timber.d("refresh ")
        newSearchNodesTask()
        managerActivity.invalidateOptionsMenu()
        visibilityFastScroller()
    }

    /**
     * Checks if select mode is enabled.
     * If so, clear the focus on SearchView.
     */
    fun checkSelectMode() {
        if (this::adapter.isInitialized.not() || adapter.isMultipleSelect.not()) {
            return
        }
        managerActivity.clearSearchViewFocus()
    }

    /**
     * This function will perfoem click action
     */
    private fun clickAction() {
        newSearchNodesTask()
        managerActivity.apply {
            setToolbarTitle()
            invalidateOptionsMenu()
            showFabButton()
        }
    }

    /**
     * On back pressed clicked on activity
     */
    fun onBackPressed(): Int {
        Timber.d("onBackPressed")
        searchViewModel.onBackClicked()
        val levelSearch = state().searchDepth
        if (levelSearch >= 0) {
            searchViewModel.decreaseSearchDepth()
            clickAction()
            val lastVisiblePosition = searchViewModel.popLastPositionStack()
            Timber.d("Scroll to $lastVisiblePosition position")

            if (lastVisiblePosition >= 0) {
                if (state().currentViewType == ViewType.LIST) {
                    layoutManager?.scrollToPositionWithOffset(lastVisiblePosition, 0)
                } else {
                    gridLayoutManager?.scrollToPositionWithOffset(lastVisiblePosition, 0)
                }
            }
            return 2
        }

        Timber.d("levels == -1")
        resetSelectedItems()
        managerActivity.showFabButton()
        return 0
    }

    /**
     * When an item clicked from adapter it calls below method
     * @param position Position of item which is clicked
     */
    fun itemClick(position: Int) {
        val node = adapter.getItem(position)
        Timber.d("Position: $position")
        if (adapter.isMultipleSelect) {
            adapter.toggleSelection(position)
            if (adapter.selectedNodes.size > 0) {
                updateActionModeTitle()
            }
        } else {
            managerActivity.setTextSubmitted()
            if (!searchViewModel.isSearchQueryValid() && node.isFolder) {
                managerActivity.closeSearchView()
                managerActivity.openSearchFolder(node)
                return
            }
            if (node.isFolder) {
                var lastFirstVisiblePosition: Int
                if (state().currentViewType == ViewType.LIST) {
                    lastFirstVisiblePosition =
                        layoutManager?.findFirstCompletelyVisibleItemPosition() ?: -1
                } else {
                    lastFirstVisiblePosition =
                        (recyclerView as NewGridRecyclerView).findFirstCompletelyVisibleItemPosition()
                    if (lastFirstVisiblePosition == -1) {
                        Timber.w("Completely -1 then find just visible position")
                        lastFirstVisiblePosition =
                            (recyclerView as NewGridRecyclerView).findFirstVisibleItemPosition()
                    }
                }
                searchViewModel.onFolderClicked(
                    node.handle,
                    lastFirstVisiblePosition
                )
                Timber.d("Push to stack $lastFirstVisiblePosition position")
                clickAction()
            } else {
                openFile(node = node, position = position)
            }
        }
    }

    /**
     * Opens file
     * @param node MegaNode
     * @param position position of item clicked
     */
    private fun openFile(node: MegaNode, position: Int) {
        //Is FILE
        val nodes: List<MegaNode> = state().nodes ?: emptyList()
        if (MimeTypeList.typeForName(node.name).isImage) {
            val currentNodeHandle = node.handle
            val nodeHandles = nodes.stream().mapToLong {
                it?.handle ?: INVALID_HANDLE
            }.toArray()
            val intent = ImageViewerActivity.getIntentForChildren(
                requireContext(),
                nodeHandles,
                currentNodeHandle
            )
            DragToExitSupport.putThumbnailLocation(
                intent,
                recyclerView,
                position,
                Constants.VIEWER_FROM_SEARCH,
                adapter
            )
            startActivity(intent)
            managerActivity.overridePendingTransition(0, 0)
        } else if (MimeTypeList.typeForName(node.name).isVideoMimeType ||
            MimeTypeList.typeForName(node.name).isAudio
        ) {
            val mimeType = MimeTypeList.typeForName(node.name).type
            Timber.d("FILE HANDLE: ${node.handle}, TYPE: $mimeType")
            var opusFile = false
            val intentInternalIntentPair =
                if (MimeTypeList.typeForName(node.name).isVideoNotSupported ||
                    MimeTypeList.typeForName(node.name).isAudioNotSupported
                ) {
                    val s = node.name.split("\\.".toRegex())
                    if (s.size > 1 && s[s.size - 1] == "opus") {
                        opusFile = true
                    }
                    Pair(Intent(Intent.ACTION_VIEW), false)
                } else {
                    Pair(
                        Util.getMediaIntent(requireContext(), node.name)
                            .putExtra(Constants.INTENT_EXTRA_KEY_IS_PLAYLIST, false), true
                    )
                }

            intentInternalIntentPair.first.apply {
                putExtra("placeholder", adapter.placeholderCount)
                putExtra("position", position)
                putExtra("searchQuery", state().searchQuery)
                putExtra("adapterType", Constants.SEARCH_ADAPTER)
                putExtra(
                    "parentNodeHandle",
                    if (state().searchParentHandle == -1L) -1L
                    else megaApi.getParentNode(node)?.handle
                )
                putExtra("orderGetChildren", searchViewModel.getOrder())
                putExtra("HANDLE", node.handle)
                putExtra("FILENAME", node.name)
            }
            DragToExitSupport.putThumbnailLocation(
                intentInternalIntentPair.first,
                recyclerView,
                position,
                Constants.VIEWER_FROM_SEARCH,
                adapter
            )
            val localPath = FileUtil.getLocalFile(node)
            localPath?.let {
                val mediaFile = File(it)
                if (it.contains(Environment.getExternalStorageDirectory().path)) {
                    intentInternalIntentPair.first.setDataAndType(
                        FileProvider.getUriForFile(
                            requireActivity(),
                            "mega.privacy.android.app.providers.fileprovider",
                            mediaFile
                        ),
                        MimeTypeList.typeForName(node.name).type
                    )
                } else {
                    intentInternalIntentPair.first.setDataAndType(
                        Uri.fromFile(mediaFile),
                        MimeTypeList.typeForName(node.name).type
                    )
                }
                intentInternalIntentPair.first.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } ?: run {
                if (megaApi.httpServerIsRunning() == 0) {
                    megaApi.httpServerStart()
                    intentInternalIntentPair.first.putExtra(
                        Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER,
                        true
                    )
                }

                val mi = ActivityManager.MemoryInfo()
                val activityManager =
                    requireActivity().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.getMemoryInfo(mi)

                if (mi.totalMem > Constants.BUFFER_COMP) {
                    Timber.d("Total mem: ${mi.totalMem} allocate 32 MB")
                    megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB)
                } else {
                    Timber.d("Total mem: ${mi.totalMem} allocate 16 MB")
                    megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB)
                }

                val url = megaApi.httpServerGetLocalLink(node)
                intentInternalIntentPair.first.setDataAndType(Uri.parse(url), mimeType)
            }
            intentInternalIntentPair.first.putExtra("HANDLE", node.handle)
            if (opusFile) {
                intentInternalIntentPair.first.setDataAndType(
                    intentInternalIntentPair.first.data,
                    "audio/*"
                )
            }
            if (intentInternalIntentPair.second) {
                startActivity(intentInternalIntentPair.first)
            } else {
                if (MegaApiUtils.isIntentAvailable(
                        requireContext(),
                        intentInternalIntentPair.first
                    )
                ) {
                    startActivity(intentInternalIntentPair.first)
                } else {
                    managerActivity.showSnackbar(
                        Constants.SNACKBAR_TYPE,
                        getString(R.string.intent_not_available),
                        -1
                    )
                    adapter.notifyDataSetChanged()
                    managerActivity.saveNodesToDevice(
                        Collections.singletonList(node),
                        true, false, false, false
                    )
                }
            }
            managerActivity.overridePendingTransition(0, 0)
        } else if (MimeTypeList.typeForName(node.name).isPdf) {
            val mimeType = MimeTypeList.typeForName(node.name).type
            Timber.d("FILE HANDLE: ${node.handle}, TYPE: $mimeType")

            val pdfIntent = Intent(requireActivity(), PdfViewerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("adapterType", Constants.RUBBISH_BIN_ADAPTER)
                putExtra("inside", true)
                putExtra("APP", true)
            }
            val localPath = FileUtil.getLocalFile(node)
            localPath?.let {
                val mediaFile = File(it)
                if (localPath.contains(Environment.getExternalStorageDirectory().path)) {
                    pdfIntent.setDataAndType(
                        FileProvider.getUriForFile(
                            requireActivity(),
                            "mega.privacy.android.app.providers.fileprovider",
                            mediaFile
                        ),
                        MimeTypeList.typeForName(node.name).type
                    )
                } else {
                    pdfIntent.setDataAndType(
                        Uri.fromFile(mediaFile),
                        MimeTypeList.typeForName(node.name).type
                    )
                }
                pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } ?: run {
                if (megaApi.httpServerIsRunning() == 0) {
                    megaApi.httpServerStart()
                    pdfIntent.putExtra(
                        Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER,
                        true
                    )
                }
                val mi = ActivityManager.MemoryInfo()
                val activityManager =
                    requireActivity().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.getMemoryInfo(mi)

                if (mi.totalMem > Constants.BUFFER_COMP) {
                    Timber.d("Total mem: ${mi.totalMem} allocate 32 MB")
                    megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB)
                } else {
                    Timber.d("Total mem: ${mi.totalMem} allocate 16 MB")
                    megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB)
                }

                val url = megaApi.httpServerGetLocalLink(node)
                pdfIntent.setDataAndType(Uri.parse(url), mimeType)
            }
            pdfIntent.putExtra("HANDLE", node.handle)
            DragToExitSupport.putThumbnailLocation(
                pdfIntent,
                recyclerView,
                position,
                Constants.VIEWER_FROM_SEARCH,
                adapter
            )
            if (MegaApiUtils.isIntentAvailable(context, pdfIntent)) {
                startActivity(pdfIntent)
            } else {
                Toast.makeText(
                    context,
                    getString(R.string.intent_not_available),
                    Toast.LENGTH_LONG
                ).show()

                managerActivity.saveNodesToDevice(
                    Collections.singletonList(node),
                    true, false, false, false
                )
            }
            managerActivity.overridePendingTransition(0, 0)
        } else if (MimeTypeList.typeForName(node.name).isURL) {
            MegaNodeUtil.manageURLNode(requireActivity(), megaApi, node)
        } else if (MimeTypeList.typeForName(node.name).isOpenableTextFile(node.size)) {
            MegaNodeUtil.manageTextFileIntent(requireActivity(), node, Constants.SEARCH_ADAPTER)
        } else {
            adapter.notifyDataSetChanged()
            MegaNodeUtil.onNodeTapped(
                requireActivity(),
                node,
                managerActivity::saveNodeByTap,
                managerActivity,
                managerActivity
            )
        }
    }

    /**
     * Updates the View Type of this Fragment
     *
     * Changing the View Type will cause the scroll position to be lost. To avoid that, only
     * refresh the contents when the new View Type is different from the original View Type
     *
     * @param viewType The new View Type received from [SearchViewModel]
     */
    private fun handleViewType(viewType: ViewType) {
        if (viewType != state().currentViewType) {
            searchViewModel.setCurrentViewType(viewType)
            switchViewType()
        }
    }

    /**
     * Switches how items in the [MegaNodeAdapter] are being displayed, based on the current
     * [ViewType] in [SearchViewModel]
     */
    private fun switchViewType() {
        recyclerView?.run {
            when (state().currentViewType) {
                ViewType.LIST -> {
                    switchToLinear()
                    if (itemDecorationCount == 0) addItemDecoration(itemDecoration)
                    this@SearchFragment.adapter.adapterType = MegaNodeAdapter.ITEM_VIEW_TYPE_LIST
                }
                ViewType.GRID -> {
                    switchBackToGrid()
                    removeItemDecoration(itemDecoration)
                    (layoutManager as CustomizedGridLayoutManager).apply {
                        spanSizeLookup = this@SearchFragment.adapter.getSpanSizeLookup(spanCount)
                    }
                    this@SearchFragment.adapter.adapterType = MegaNodeAdapter.ITEM_VIEW_TYPE_GRID
                }
            }
        }
    }
}