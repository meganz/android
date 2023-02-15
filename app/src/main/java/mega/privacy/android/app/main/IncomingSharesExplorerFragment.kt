package mega.privacy.android.app.main

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
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.databinding.FragmentFileexplorerlistBinding
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.main.FileExplorerActivity.Companion.COPY
import mega.privacy.android.app.main.FileExplorerActivity.Companion.INCOMING_FRAGMENT
import mega.privacy.android.app.main.FileExplorerActivity.Companion.MOVE
import mega.privacy.android.app.main.adapters.MegaExplorerAdapter
import mega.privacy.android.app.main.adapters.RotatableAdapter
import mega.privacy.android.app.main.managerSections.RotatableFragment
import mega.privacy.android.app.search.callback.SearchCallback
import mega.privacy.android.app.search.usecase.SearchNodesUseCase
import mega.privacy.android.app.search.usecase.SearchNodesUseCase.Companion.TYPE_INCOMING_EXPLORER
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants.SCROLLING_UP_DIRECTION
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util.getPreferences
import mega.privacy.android.app.utils.Util.isScreenInPortrait
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.preference.ViewType
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaCancelToken
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
    SearchCallback.View, SearchCallback.Data {

    /**
     * [SearchNodesUseCase] injection
     */
    @Inject
    lateinit var searchNodesUseCase: SearchNodesUseCase

    /**
     * [MegaApiAndroid] injection
     */
    @Inject
    @MegaApi
    lateinit var megaApi: MegaApiAndroid

    private val sortByHeaderViewModel by viewModels<SortByHeaderViewModel>()

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

    private var searchCancelToken: MegaCancelToken? = null

    private var shouldResetNodes = true
    private var hasWritePermissions = true

    private var emptyRootText: Spanned? = null
    private var emptyGeneralText: Spanned? = null

    private lateinit var itemDecoration: PositionDividerItemDecoration

    private var disposable: Disposable? = null

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
                    selectAll.isVisible = selectedNodes.size != megaApi.getNumChildFiles(
                        megaApi.getNodeByHandle(parentHandle)
                    )
                    unselect.title = getString(R.string.action_unselect_all)
                    unselect.isVisible = true
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
                initOriginalData()
                updateNodesByAdapter(originalData)
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

    override fun checkScroll() =
        (recyclerView.canScrollHorizontally(SCROLLING_UP_DIRECTION) || adapter.multipleSelected)
            .let { elevate ->
                fileExplorerActivity.changeActionBarElevation(elevate, INCOMING_FRAGMENT)
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
            fileExplorerActivity.finishAndRemoveTask()
        }
        binding.cancelText.text = getString(R.string.general_cancel)
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

        initOriginalData()

        when {
            modeCloud == FileExplorerActivity.UPLOAD ->
                binding.actionText.text = getString(R.string.context_upload)
            modeCloud == FileExplorerActivity.IMPORT ->
                binding.actionText.text = getString(R.string.add_to_cloud)
            isMultiselect() -> {
                binding.actionText.text = getString(R.string.context_send)
                activateButton(adapter.getSelectedItemCount() > 0)
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
        emptyRootText = TextUtil.formatEmptyScreenText(
            requireContext(),
            getString(R.string.context_empty_cloud_drive)
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
            switchListGridView(state.viewType)
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposable?.dispose()
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
    private fun initOriginalData() {
        if (parentHandle == INVALID_HANDLE) {
            getNodesFromInShares()
        } else {
            megaApi.getNodeByHandle(parentHandle)?.let { parentNode ->
                originalData.clear()
                originalData.addAll(megaApi.getChildren(parentNode, order))
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
                if (isScreenInPortrait(requireContext())) {
                    R.drawable.incoming_shares_empty
                } else {
                    R.drawable.incoming_empty_landscape
                }
            } else {
                if (isScreenInPortrait(requireContext())) {
                    R.drawable.ic_zero_portrait_empty_folder
                } else {
                    R.drawable.ic_zero_landscape_empty_folder
                }
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
    private fun getNodesFromInShares() {
        Timber.d("getNodesFromInShares")
        fileExplorerActivity.deepBrowserTree = 0

        setOptionsBarVisibility()
        originalData.clear()
        originalData.addAll(
            megaApi.getInShares(
                if (orderParent == MegaApiAndroid.ORDER_DEFAULT_DESC)
                    orderParent
                else
                    order
            )
        )
    }

    private fun checkWritePermissions() {
        hasWritePermissions = megaApi.getNodeByHandle(parentHandle)?.let { parentNode ->
            megaApi.getAccess(parentNode) >= MegaShare.ACCESS_READWRITE
        } ?: false

        activateButton(hasWritePermissions)
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
        val clickNodes = mutableListOf<MegaNode?>()
        if (searchNodes.isNotEmpty()) {
            clickNodes.addAll(searchNodes)
            shouldResetNodes = false
            fileExplorerActivity.setQueryAfterSearch()
            fileExplorerActivity.collapseSearchView()
        } else {
            clickNodes.addAll(nodes)
        }

        if (position < 0 || position >= clickNodes.size) return

        clickNodes[position]?.let { node ->
            when {
                node.isFolder -> {
                    searchNodes.clear()
                    fileExplorerActivity.hideTabs(true, INCOMING_FRAGMENT)
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

                    updateNodesByAdapter(megaApi.getChildren(node, order))
                    recyclerView.scrollToPosition(0)

                    if (modeCloud == COPY || modeCloud == MOVE) {
                        when {
                            adapter.itemCount == 0 -> activateButton(true)
                            fileExplorerActivity.deepBrowserTree > 0 -> checkCopyMoveButton()
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
                setParentHandle(INVALID_HANDLE)
                fileExplorerActivity.hideTabs(false, INCOMING_FRAGMENT)
                getNodesFromInShares()
                updateNodesByAdapter(originalData)

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
                return 3
            }
            fileExplorerActivity.deepBrowserTree > 0 -> {
                parentHandle = adapter.parentHandle
                megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle))
                    ?.let { parentNode ->
                        setParentHandle(parentNode.handle)
                        originalData.clear()
                        originalData.addAll(megaApi.getChildren(parentNode, order))
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
                        return 2
                    } ?: let {
                    setOptionsBarVisibility()
                    return 2
                }
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
            nodes.addAll(adapter.getNodes())
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
            getNodesFromInShares()
        } else {
            this.order = order
            originalData.clear()
            originalData.addAll(megaApi.getChildren(megaApi.getNodeByHandle(parentHandle), order))
        }
        updateNodesByAdapter(originalData)
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
        if (searchString == null && !shouldResetNodes) {
            return
        }
        searchCancelToken = initNewSearch()
        searchCancelToken?.let {
            disposable?.dispose()
            disposable = searchNodesUseCase.get(query = searchString,
                parentHandleSearch = INVALID_HANDLE,
                parentHandle = parentHandle,
                searchType = TYPE_INCOMING_EXPLORER,
                megaCancelToken = it)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { searchedNodes ->
                        finishSearch(searchedNodes)
                    },
                    { throwable ->
                        Timber.e(throwable)
                    }
                )
        }
    }

    override fun initNewSearch(): MegaCancelToken {
        updateSearchProgressView(true)
        cancelSearch()
        return MegaCancelToken.createInstance()
    }

    override fun updateSearchProgressView(inProgress: Boolean) {
        binding.contentLayout.isEnabled = !inProgress
        binding.contentLayout.alpha = if (inProgress) 0.4f else 1f
        binding.progressbarLayout.progressbar.isVisible = inProgress
        recyclerView.isVisible = !inProgress
    }

    override fun cancelSearch() {
        searchCancelToken?.cancel()
    }

    override fun finishSearch(searchedNodes: ArrayList<MegaNode>) {
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
            initOriginalData()
            updateNodesByAdapter(nodes)
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