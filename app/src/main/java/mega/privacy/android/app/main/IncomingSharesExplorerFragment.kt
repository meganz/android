package mega.privacy.android.app.main

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import android.os.Bundle
import android.text.Spanned
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.databinding.FragmentFileexplorerlistBinding
import mega.privacy.android.app.domain.usecase.search.LegacySearchUseCase
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.main.FileExplorerActivity.Companion.COPY
import mega.privacy.android.app.main.FileExplorerActivity.Companion.INCOMING_FRAGMENT
import mega.privacy.android.app.main.FileExplorerActivity.Companion.MOVE
import mega.privacy.android.app.main.adapters.MegaExplorerAdapter
import mega.privacy.android.app.main.adapters.RotatableAdapter
import mega.privacy.android.app.main.managerSections.RotatableFragment
import mega.privacy.android.app.search.callback.SearchCallback
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants.SCROLLING_UP_DIRECTION
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util.getPreferences
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import timber.log.Timber
import java.util.Stack
import javax.inject.Inject

/**
 * The fragment for incoming shares explorer
 */
@AndroidEntryPoint
class IncomingSharesExplorerFragment : RotatableFragment(), CheckScrollInterface,
    SearchCallback.View {


    /**
     * [LegacySearchUseCase]
     */
    @Inject
    lateinit var legacySearchUseCase: LegacySearchUseCase

    /**
     * [CancelCancelTokenUseCase]
     */
    @Inject
    lateinit var cancelCancelTokenUseCase: CancelCancelTokenUseCase

    /**
     * [MegaApiAndroid] injection
     */
    @Inject
    @MegaApi
    lateinit var megaApi: MegaApiAndroid

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    lateinit var sortOrderIntMapper: SortOrderIntMapper

    private val sortByHeaderViewModel by activityViewModels<SortByHeaderViewModel>()
    private val fileExplorerViewModel by activityViewModels<FileExplorerViewModel>()

    private var _binding: FragmentFileexplorerlistBinding? = null
    private val binding get() = _binding!!

    private val nodes = mutableListOf<MegaNode?>()
    private val originalData = mutableListOf<MegaNode?>()
    private val searchNodes = mutableListOf<MegaNode?>()

    /**
     * Parent handle
     */
    var parentHandle = INVALID_HANDLE
        private set

    private lateinit var adapter: MegaExplorerAdapter

    /**
     * RecyclerView
     */
    lateinit var recyclerView: RecyclerView
        private set

    private var listLayoutManager: LinearLayoutManager? = null
    private var gridLayoutManager: CustomizedGridLayoutManager? = null

    private var modeCloud = 0
    private var selectFile: Boolean = false

    private val lastPositionStack: Stack<Int> = Stack()

    private var actionMode: ActionMode? = null

    private var orderParent = MegaApiAndroid.ORDER_DEFAULT_ASC
    private var order = MegaApiAndroid.ORDER_DEFAULT_ASC

    private var shouldResetNodes = true
    private var hasWritePermissions = true

    private var emptyRootText: Spanned? = null
    private var emptyGeneralText: Spanned? = null

    private lateinit var itemDecoration: PositionDividerItemDecoration

    private val fileExplorerActivity: FileExplorerActivity
        get() = (requireActivity() as FileExplorerActivity)

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.file_explorer_multiaction, menu)
            fileExplorerActivity.hideTabs(true, INCOMING_FRAGMENT)
            checkScroll()
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            adapter.getSelectedNodes().let { selectedNodes ->
                val unselect = menu?.findItem(R.id.cab_menu_unselect_all) ?: return@let
                val selectAll = menu.findItem(R.id.cab_menu_select_all)
                if (selectedNodes.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        runCatching {
                            selectedNodes.size != megaApi.getNumChildFiles(
                                megaApi.getNodeByHandle(parentHandle)
                            )
                        }.onSuccess {
                            withContext(Dispatchers.Main) {
                                selectAll.isVisible = it
                                unselect.title = getString(R.string.action_unselect_all)
                                unselect.isVisible = true
                            }
                        }
                    }
                } else {
                    selectAll.isVisible = true
                    unselect.isVisible = false
                }
            }
            return false
        }

        override fun onActionItemClicked(p0: ActionMode?, p1: MenuItem?): Boolean {
            p1?.itemId.let { id ->
                when (id) {
                    R.id.cab_menu_select_all -> selectAll()
                    R.id.cab_menu_unselect_all -> {
                        clearSelections()
                        hideMultipleSelect()
                    }
                }
            }
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            if (!fileExplorerActivity.shouldReopenSearch()) {
                fileExplorerActivity.hideTabs(false, INCOMING_FRAGMENT)
                fileExplorerActivity.clearQuerySearch()
                updateOriginalData()
            }
            clearSelections()
            adapter.multipleSelected = false
            checkScroll()
        }
    }

    override fun getAdapter(): RotatableAdapter = adapter

    override fun activateActionMode() {
        if (!adapter.multipleSelected) {
            adapter.multipleSelected = true
            actionMode =
                (requireActivity() as AppCompatActivity).startSupportActionMode(actionModeCallback)
            if (isMultiselect()) {
                activateButton(true)
            }
        }
    }

    override fun multipleItemClick(position: Int) = adapter.toggleSelection(position)

    override fun reselectUnHandledSingleItem(position: Int) {}

    override fun checkScroll() {
        if (!isAdded) return

        (recyclerView.canScrollVertically(SCROLLING_UP_DIRECTION) || adapter.multipleSelected)
            .let { elevate ->
                fileExplorerActivity.changeActionBarElevation(elevate, INCOMING_FRAGMENT)
            }
    }

    /**
     * Shows the Sort by panel.
     */
    private fun showSortByPanel() = fileExplorerActivity.showSortByPanel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Timber.d("onCreateView")
        _binding = FragmentFileexplorerlistBinding.inflate(inflater, container, false)
        binding.actionText.setOnClickListener {
            buttonClicked()
        }
        binding.cancelText.setOnClickListener {
            fileExplorerActivity.handleBackNavigation()
        }
        binding.cancelText.text = getString(sharedR.string.general_dialog_cancel_button)
        binding.fabSelect.setOnClickListener {
            buttonClicked()
        }

        // Initialization for recycler view
        listLayoutManager = LinearLayoutManager(requireContext())
        gridLayoutManager = CustomizedGridLayoutManager(requireContext(), SPAN_COUNT)
        binding.fileListViewBrowser.layoutManager = listLayoutManager
        binding.fileGridViewBrowser.layoutManager = gridLayoutManager
        itemDecoration = PositionDividerItemDecoration(requireContext(), resources.displayMetrics)

        if (sortByHeaderViewModel.isListView()) {
            recyclerView = binding.fileListViewBrowser
            binding.fileGridViewBrowser.isVisible = false
            recyclerView.addItemDecoration(itemDecoration)
        } else {
            recyclerView = binding.fileGridViewBrowser
            binding.fileListViewBrowser.isVisible = false
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                checkScroll()
            }
        })

        modeCloud = fileExplorerActivity.mode
        selectFile = fileExplorerActivity.isSelectFile
        parentHandle = fileExplorerActivity.parentHandleIncoming

        if (parentHandle != INVALID_HANDLE)
            fileExplorerActivity.hideTabs(true, INCOMING_FRAGMENT)

        adapter = MegaExplorerAdapter(
            context = requireActivity(),
            fragment = this,
            nodes = nodes,
            parentHandle = parentHandle,
            recyclerView = recyclerView,
            selectFile = selectFile,
            sortByViewModel = sortByHeaderViewModel,
            fileExplorerViewModel = fileExplorerViewModel,
            ioDispatcher = ioDispatcher,
            megaApi = megaApi
        )

        gridLayoutManager?.let {
            it.spanSizeLookup = adapter.getSpanSizeLookup(it.spanCount)
        }

        binding.fileListViewBrowser.adapter = adapter
        binding.fileGridViewBrowser.adapter = adapter
        binding.fastscroll.setRecyclerView(recyclerView)

        getPreferences()?.let { prefs ->
            prefs.preferredSortOthers?.let {
                orderParent = it.toInt()
            }
            prefs.preferredSortCloud?.let {
                order = it.toInt()
            }
        }

        val latestTargetPathTab = fileExplorerViewModel.latestCopyTargetPathTab
        val latestMoveTargetPathTab = fileExplorerViewModel.latestMoveTargetPathTab
        if (latestMoveTargetPathTab == FileExplorerActivity.INCOMING_TAB || latestTargetPathTab == FileExplorerActivity.INCOMING_TAB) {
            var targetPath: Long? = null
            if (modeCloud == COPY) {
                targetPath = fileExplorerViewModel.latestCopyTargetPath?.let {
                    fileExplorerActivity.hideTabs(true, FileExplorerActivity.CLOUD_FRAGMENT)
                    it
                }
            } else if (modeCloud == MOVE) {
                targetPath = fileExplorerViewModel.latestMoveTargetPath?.let {
                    fileExplorerActivity.hideTabs(true, FileExplorerActivity.CLOUD_FRAGMENT)
                    it
                }
            }
            targetPath?.let {
                fileExplorerActivity.hideTabs(true, INCOMING_FRAGMENT)
                setDeepBrowserTree(it)
                setParentHandle(it)
            }
        }

        updateOriginalData()

        when {
            modeCloud == FileExplorerActivity.UPLOAD ->
                binding.actionText.text = getString(R.string.context_upload)

            modeCloud == FileExplorerActivity.IMPORT ->
                binding.actionText.text = getString(R.string.add_to_cloud)

            isMultiselect() -> {
                binding.actionText.text = getString(R.string.context_send)
                activateButton(adapter.getSelectedItemCount() > 0)
            }

            modeCloud == MOVE -> {
                binding.actionText.text = getString(R.string.context_move)
            }

            modeCloud == COPY -> {
                binding.actionText.text = getString(R.string.context_copy)
            }

            modeCloud == FileExplorerActivity.SELECT ->
                binding.optionsExplorerLayout.isVisible = false

            else -> binding.actionText.text = getString(R.string.general_select)
        }

        Timber.d("deepBrowserTree value: ${fileExplorerActivity.deepBrowserTree}")
        setOptionsBarVisibility()

        if (fileExplorerActivity.shouldRestartSearch) {
            isWaitingForSearchedNodes = true
            search(fileExplorerActivity.querySearch)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        sortByHeaderViewModel.refreshData(isUpdatedOrderChangeState = true)

        viewLifecycleOwner.collectFlow(sortByHeaderViewModel.orderChangeState) { order ->
            (activity as? FileExplorerActivity)?.refreshIncomingExplorerOrderNodes(
                sortOrderIntMapper(
                    if (parentHandle == INVALID_HANDLE) {
                        order.othersSortOrder
                    } else {
                        order.cloudSortOrder
                    }
                )
            )
        }

        emptyRootText = TextUtil.formatEmptyScreenText(
            requireContext(),
            getString(R.string.context_empty_incoming)
        )
        emptyGeneralText = TextUtil.formatEmptyScreenText(
            requireContext(),
            getString(R.string.file_browser_empty_folder_new)
        )
        updateEmptyScreen()

        sortByHeaderViewModel.showDialogEvent.observe(
            viewLifecycleOwner,
            EventObserver { showSortByPanel() }
        )
        collectFlow(sortByHeaderViewModel.state) { state ->
            rotatableFragmentViewType = state.viewType
            switchListGridView(state.viewType)
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * For updating view when switch with list and grid
     *
     * @param viewType the view type
     */
    private fun switchListGridView(viewType: ViewType) =
        (viewType == ViewType.LIST).let { isList ->
            if (isList) {
                recyclerView = binding.fileListViewBrowser
                if (recyclerView.itemDecorationCount == 0)
                    recyclerView.addItemDecoration(itemDecoration)
                recyclerView.layoutManager = listLayoutManager
            } else {
                recyclerView = binding.fileGridViewBrowser
                if (recyclerView.itemDecorationCount != 0)
                    recyclerView.removeItemDecoration(itemDecoration)
                recyclerView.layoutManager = gridLayoutManager
            }
            binding.fileListViewBrowser.isVisible = isList
            binding.fileGridViewBrowser.isVisible = !isList
            updateNodesByAdapter(originalData)
        }

    private fun setOptionsBarVisibility() {
        val visibility = !(modeCloud == FileExplorerActivity.SELECT ||
                (!isMultiselect() && (fileExplorerActivity.deepBrowserTree <= 0 || selectFile)))
        binding.optionsExplorerLayout.isVisible = visibility
    }

    /**
     * Original data is initialized
     */
    private fun updateOriginalData() {
        lifecycleScope.launch {
            fetchNodes()
        }
    }

    private suspend fun fetchNodes() {
        if (parentHandle == INVALID_HANDLE) {
            getNodesFromInShares()
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    megaApi.getNodeByHandle(parentHandle)?.let { parentNode ->
                        megaApi.getChildren(parentNode, order)
                    }
                }.onSuccess { nodes ->
                    if (nodes != null) {
                        withContext(Dispatchers.Main) {
                            originalData.clear()
                            originalData.addAll(nodes)
                            updateNodesByAdapter(originalData)
                        }
                    }
                }
            }
        }
    }

    /**
     * Update view
     */
    private fun updateView() {
        val isEmpty = adapter.itemCount == 0
        recyclerView.isVisible = !isEmpty
        binding.fileListEmptyImage.isVisible = isEmpty
        binding.fileListEmptyText.isVisible = isEmpty
        if (isEmpty) {
            updateEmptyScreen()
        }
    }

    /**
     * Update empty view
     */
    private fun updateEmptyScreen() {
        binding.fileListEmptyImage.setImageResource(
            if (parentHandle == INVALID_HANDLE) {
                iconPackR.drawable.ic_folder_arrow_up_glass
            } else {
                iconPackR.drawable.ic_empty_folder_glass
            }
        )
        binding.fileListEmptyTextFirst.text =
            if (parentHandle == INVALID_HANDLE) {
                emptyRootText
            } else {
                emptyGeneralText
            }
        ColorUtils.setImageViewAlphaIfDark(
            requireContext(),
            binding.fileListEmptyImage,
            ColorUtils.DARK_IMAGE_ALPHA
        )
    }

    /**
     * Find nodes
     */
    private suspend fun getNodesFromInShares() {
        Timber.d("getNodesFromInShares")
        fileExplorerActivity.deepBrowserTree = 0
        setOptionsBarVisibility()
        withContext(Dispatchers.IO) {
            runCatching {
                megaApi.getInShares(orderParent)
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    originalData.clear()
                    originalData.addAll(it)
                    updateNodesByAdapter(originalData)
                }
            }.onFailure { throwable ->
                Timber.e(throwable)
            }
        }
    }

    private fun checkWritePermissions() {
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                megaApi.getNodeByHandle(parentHandle)?.let { parentNode ->
                    megaApi.getAccess(parentNode) >= MegaShare.ACCESS_READWRITE
                } ?: false
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    hasWritePermissions = it
                    activateButton(hasWritePermissions)
                }
            }
        }
    }

    /**
     * The behaviour for action text and fab select buttons clicked
     */
    private fun buttonClicked() {
        if (fileExplorerActivity.isMultiselect) {
            if (adapter.getSelectedItemCount() > 0) {
                fileExplorerActivity.buttonClick(adapter.getSelectedHandles())
            } else {
                fileExplorerActivity.showSnackbar(getString(R.string.no_files_selected_warning))
            }
        } else {
            fileExplorerActivity.buttonClick(parentHandle)
        }
    }

    /**
     * Navigating to folder
     *
     * @param handle folder handle
     */
    fun navigateToFolder(handle: Long) {
        Timber.d("navigateToFolder")
        fileExplorerActivity.increaseDeepBrowserTree()
        Timber.d("deepBrowserTree value: ${fileExplorerActivity.deepBrowserTree}")
        setOptionsBarVisibility()

        val lastFirstVisiblePosition = if (fileExplorerActivity.isList) {
            listLayoutManager
        } else {
            gridLayoutManager
        }?.findFirstCompletelyVisibleItemPosition() ?: 0

        Timber.d("Push to stack $lastFirstVisiblePosition position")
        lastPositionStack.push(lastFirstVisiblePosition)

        setParentHandle(handle)
        originalData.clear()
        updateNodesByAdapter(originalData)

        recyclerView.scrollToPosition(0)

        if (modeCloud == MOVE || modeCloud == COPY)
            activateButton(true)
    }

    /**
     * The behaviour that item is clicked
     *
     * @param position the position of item
     */
    fun itemClick(position: Int) {
        if (searchNodes.isNotEmpty()) {
            shouldResetNodes = false
            fileExplorerActivity.setQueryAfterSearch()
            fileExplorerActivity.collapseSearchView()
        }

        adapter.getItem(position)?.also { node ->
            when {
                node.isFolder -> {
                    lifecycleScope.launch {
                        searchNodes.clear()
                        fileExplorerActivity.shouldRestartSearch = false

                        if (selectFile && fileExplorerActivity.isMultiselect && adapter.multipleSelected)
                            hideMultipleSelect()

                        fileExplorerActivity.increaseDeepBrowserTree()
                        Timber.d("deepBrowserTree value: ${fileExplorerActivity.deepBrowserTree}")
                        setOptionsBarVisibility()

                        val lastFirstVisiblePosition = if (fileExplorerActivity.isList) {
                            listLayoutManager
                        } else {
                            gridLayoutManager
                        }?.findFirstCompletelyVisibleItemPosition() ?: 0

                        Timber.d("Push to stack $lastFirstVisiblePosition position")
                        lastPositionStack.push(lastFirstVisiblePosition)

                        setParentHandle(node.handle)
                        fileExplorerActivity.invalidateOptionsMenu()

                        fetchNodes()
                        recyclerView.scrollToPosition(0)
                        fileExplorerActivity.hideTabs(true, INCOMING_FRAGMENT)

                        if (modeCloud == COPY || modeCloud == MOVE) {
                            when {
                                adapter.itemCount == 0 -> activateButton(true)
                                fileExplorerActivity.deepBrowserTree > 0 -> checkCopyMoveButton()
                            }
                        }
                    }
                }

                selectFile -> {
                    if (fileExplorerActivity.isMultiselect) {
                        if (adapter.getSelectedItemCount() == 0) {
                            activateActionMode()
                            adapter.toggleSelection(position)
                            updateActionModeTitle()
                        } else {
                            adapter.toggleSelection(position)
                            if (adapter.getSelectedNodes().isNotEmpty()) {
                                updateActionModeTitle()
                            }
                        }
                    } else {
                        fileExplorerActivity.buttonClick(node.handle)
                    }
                }
            }
        }
        fileExplorerActivity.invalidateOptionsMenu()
        shouldResetNodes = true
    }

    /**
     * The behaviour when back button is pressed
     *
     * @return the result code
     */
    fun onBackPressed(): Int {
        Timber.d("deepBrowserTree ${fileExplorerActivity.deepBrowserTree}")
        fileExplorerActivity.decreaseDeepBrowserTree()
        when {
            fileExplorerActivity.deepBrowserTree == 0 -> {
                lifecycleScope.launch {
                    setParentHandle(INVALID_HANDLE)
                    getNodesFromInShares()
                    fileExplorerActivity.hideTabs(false, INCOMING_FRAGMENT)

                    val lastVisiblePosition = if (lastPositionStack.isNotEmpty()) {
                        lastPositionStack.pop().apply {
                            Timber.d("Pop of the stack $this position")
                        }
                    } else {
                        0
                    }
                    Timber.d("Scroll to $lastVisiblePosition position")
                    if (lastVisiblePosition >= 0) {
                        if (fileExplorerActivity.isList) {
                            listLayoutManager
                        } else {
                            gridLayoutManager
                        }?.scrollToPositionWithOffset(lastVisiblePosition, 0)
                    }
                    setOptionsBarVisibility()
                    fileExplorerActivity.invalidateOptionsMenu()
                }
                return 3
            }

            fileExplorerActivity.deepBrowserTree > 0 -> {
                parentHandle = adapter.parentHandle
                lifecycleScope.launch(Dispatchers.IO) {
                    runCatching {
                        megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle))
                            ?.let { parentNode ->
                                parentNode to megaApi.getChildren(parentNode, order)
                            }
                    }.onSuccess { result ->
                        if (result != null) {
                            val (parentNode, nodes) = result
                            withContext(Dispatchers.Main) {
                                setParentHandle(parentNode.handle)
                                originalData.clear()
                                originalData.addAll(nodes)
                                updateNodesByAdapter(originalData)

                                if (modeCloud == COPY || modeCloud == MOVE) {
                                    checkCopyMoveButton()
                                }

                                val lastVisiblePosition = if (lastPositionStack.isNotEmpty()) {
                                    lastPositionStack.pop().apply {
                                        Timber.d("Pop of the stack $this position")
                                    }
                                } else {
                                    0
                                }
                                Timber.d("Scroll to $lastVisiblePosition position")
                                if (lastVisiblePosition >= 0) {
                                    if (fileExplorerActivity.isList) {
                                        listLayoutManager
                                    } else {
                                        gridLayoutManager
                                    }?.scrollToPositionWithOffset(lastVisiblePosition, 0)
                                }
                                fileExplorerActivity.invalidateOptionsMenu()
                            }
                        }
                    }
                }
                return 2
            }

            else -> {
                recyclerView.isVisible = true
                binding.fileListEmptyImage.isVisible = false
                binding.fileListEmptyText.isVisible = false
                binding.optionsExplorerLayout.isVisible = false
                activateButton(false)
                fileExplorerActivity.deepBrowserTree = 0
                fileExplorerActivity.invalidateOptionsMenu()
                return 0
            }
        }
    }

    private fun setDeepBrowserTree(handle: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                fileExplorerActivity.increaseDeepBrowserTree()
                var parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(handle))
                while (parentNode != null) {
                    fileExplorerActivity.increaseDeepBrowserTree()
                    parentNode = megaApi.getParentNode(parentNode)
                }
            }
        }
    }

    private fun setParentHandle(handle: Long) {
        parentHandle = handle
        adapter.parentHandle = handle
        fileExplorerActivity.parentHandleIncoming = handle
        fileExplorerActivity.changeTitle()
    }

    /**
     * Update nodes by adapter
     *
     * @param data original nodes
     */
    fun updateNodesByAdapter(data: List<MegaNode?>) {
        data.toList().let {
            nodes.clear()
            adapter.setNodes(it)
            nodes.addAll(it)
            updateView()
        }
        checkWritePermissions()
    }

    /**
     * Active button
     *
     * @param show true is that shows button, otherwise is false
     */
    private fun activateButton(show: Boolean) =
        if (modeCloud == FileExplorerActivity.SELECT)
            binding.fabSelect.isVisible = selectFile && show
        else
            binding.actionText.isEnabled = hasWritePermissions && show

    /**
     * Select all items
     */
    private fun selectAll() {
        adapter.selectAll()
        requireActivity().runOnUiThread(::updateActionModeTitle)
    }

    /**
     * Clear all selected items
     */
    private fun clearSelections() {
        if (adapter.multipleSelected)
            adapter.clearSelections()

        if (modeCloud == FileExplorerActivity.SELECT)
            activateButton(false)
    }

    override fun updateActionModeTitle() {
        actionMode?.title = adapter.getSelectedNodes().size.toString()
        actionMode?.invalidate()
    }

    /**
     * Disable selection
     */
    fun hideMultipleSelect() {
        adapter.multipleSelected = false
        adapter.clearSelections()
        actionMode?.finish()
        if (isMultiselect())
            activateButton(false)
    }

    /**
     * Update nodes by order
     *
     * @param order nodes order
     */
    fun updateNodesByOrder(order: Int) {
        if (parentHandle == INVALID_HANDLE) {
            orderParent = order
        } else {
            this.order = order
        }
        updateOriginalData()
    }

    /**
     * Whether multiple select
     *
     * @return true is multiple select, otherwise is false
     */
    private fun isMultiselect() =
        modeCloud == FileExplorerActivity.SELECT && selectFile && fileExplorerActivity.isMultiselect

    /**
     * Search nodes based on search string
     *
     * @param searchString search strings
     */
    fun search(searchString: String?) {
        if (searchString == null || !shouldResetNodes) {
            return
        }
        initNewSearch()
        lifecycleScope.launch {
            runCatching {
                legacySearchUseCase(
                    nodeSourceType = NodeSourceType.INCOMING_SHARES,
                    query = searchString,
                    parentHandle = NodeId(parentHandle)
                )
            }.onSuccess { searchedNodes ->
                finishSearch(searchedNodes)
            }.onFailure { throwable ->
                Timber.e(throwable)
            }
        }
    }

    private fun initNewSearch() {
        updateSearchProgressView(true)
        cancelSearch()
    }

    override fun updateSearchProgressView(inProgress: Boolean) {
        binding.contentLayout.isEnabled = !inProgress
        binding.contentLayout.alpha = if (inProgress) 0.4f else 1f
        binding.progressbarLayout.progressbar.isVisible = inProgress
        recyclerView.isVisible = !inProgress
    }

    private fun cancelSearch() {
        lifecycleScope.launch {
            cancelCancelTokenUseCase()
        }
    }

    override fun finishSearch(searchedNodes: List<MegaNode>) {
        updateSearchProgressView(false)
        setSearchNodes(searchedNodes)
    }

    /**
     * Set search nodes
     *
     * @param nodes search nodes
     */
    fun setSearchNodes(nodes: List<MegaNode?>) {
        searchNodes.clear()
        searchNodes.addAll(nodes)

        fileExplorerActivity.shouldRestartSearch = true
        adapter.setNodes(searchNodes)
        updateView()

        if (isWaitingForSearchedNodes)
            reDoTheSelectionAfterRotation()
    }

    /**
     * Close search mode
     *
     * @param collapsedByClick true is that collapse by click, otherwise is false
     */
    fun closeSearch(collapsedByClick: Boolean) {
        updateSearchProgressView(false)
        cancelSearch()
        if (!collapsedByClick) {
            searchNodes.clear()
        }
        if (shouldResetNodes) {
            updateOriginalData()
        }
    }

    /**
     * Get fast scroller
     *
     * @return fast scroller view
     */
    fun getFastScroller() = binding.fastscroll

    /**
     * Whether folder is empty
     *
     * @return true is empty, otherwise is false
     */
    fun isFolderEmpty() = adapter.itemCount <= 0

    /**
     * Checks if copy or move button should be shown or hidden depending on the current navigation level.
     * Shows it if the current navigation level is not the parent of moving/copying nodes.
     * Hides it otherwise.
     */
    private fun checkCopyMoveButton() {
        fileExplorerActivity.parentMoveCopy().let { parentNode ->
            activateButton(
                modeCloud == COPY || parentNode == null || parentNode.handle != parentHandle
            )
        }
    }

    companion object {
        private const val SPAN_COUNT = 2

        /**
         * Create new instance
         *
         * @return new instance
         */
        fun newInstance(): IncomingSharesExplorerFragment =
            IncomingSharesExplorerFragment().apply {
                Timber.d("newInstance")
            }
    }
}