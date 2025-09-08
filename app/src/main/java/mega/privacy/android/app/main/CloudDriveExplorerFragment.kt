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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
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
import mega.privacy.android.app.main.FileExplorerActivity.Companion.ACTION_SAVE_TO_CLOUD
import mega.privacy.android.app.main.FileExplorerActivity.Companion.CLOUD_FRAGMENT
import mega.privacy.android.app.main.adapters.FileExplorerPagerAdapter.Companion.TAB_POSITION_INCOMING
import mega.privacy.android.app.main.adapters.MegaExplorerAdapter
import mega.privacy.android.app.main.adapters.RotatableAdapter
import mega.privacy.android.app.main.managerSections.RotatableFragment
import mega.privacy.android.app.search.callback.SearchCallback
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.SCROLLING_UP_DIRECTION
import mega.privacy.android.app.utils.TextUtil.formatEmptyScreenText
import mega.privacy.android.app.utils.Util.getPreferences
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_ASC
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.util.Stack
import javax.inject.Inject

/**
 * The fragment for cloud drive explorer
 */
@AndroidEntryPoint
class CloudDriveExplorerFragment : RotatableFragment(), CheckScrollInterface, SearchCallback.View {

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
     * [DatabaseHandler]
     */
    @Inject
    lateinit var dbH: DatabaseHandler

    /**
     * [MegaApiAndroid]
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
    private lateinit var binding: FragmentFileexplorerlistBinding

    private val nodes = mutableListOf<MegaNode?>()
    private val originalData = mutableListOf<MegaNode?>()
    private val searchNodes = mutableListOf<MegaNode?>()

    /**
     * Parent handle
     */
    var parentHandle = INVALID_HANDLE
        private set

    private lateinit var adapter: MegaExplorerAdapter

    private var modeCloud: Int = INVALID_VALUE
    private var selectFile = false

    private var actionMode: ActionMode? = null

    /**
     * RecyclerView
     */
    lateinit var recyclerView: RecyclerView
        private set

    private var listLayoutManager: LinearLayoutManager? = null
    private var gridLayoutManager: CustomizedGridLayoutManager? = null

    private val lastPositionStack: Stack<Int> = Stack()

    private var order = ORDER_DEFAULT_ASC

    private var shouldResetNodes = true

    private var emptyRootText: Spanned? = null
    private var emptyGeneralText: Spanned? = null

    private lateinit var itemDecoration: PositionDividerItemDecoration

    /**
     * Job used to save the orderNodes operation
     */
    private var orderJob: Job? = null

    private val fileExplorerActivity: FileExplorerActivity
        get() = (requireActivity() as FileExplorerActivity)

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            Timber.d("onCreateActionMode")
            mode?.menuInflater?.inflate(R.menu.file_explorer_multiaction, menu)
            (requireActivity() as FileExplorerActivity).hideTabs(true, CLOUD_FRAGMENT)
            checkScroll()
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            Timber.d("onPrepareActionMode")
            adapter.getSelectedNodes().let { selectedNodes ->
                val unselect = menu?.findItem(R.id.cab_menu_unselect_all)
                val selectAll = menu?.findItem(R.id.cab_menu_select_all)
                if (selectedNodes.isNotEmpty()) {
                    megaApi.getNodeByHandle(parentHandle).let { node ->
                        if (selectedNodes.size == megaApi.getNumChildFiles(node)) {
                            selectAll?.isVisible = false
                            unselect?.title = getString(R.string.action_unselect_all)
                            unselect?.isVisible = true
                        } else {
                            if (modeCloud == FileExplorerActivity.SELECT) {
                                if (selectFile && (requireActivity() as FileExplorerActivity).isMultiselect) {
                                    selectAll?.isVisible =
                                        selectedNodes.size != megaApi.getNumChildFiles(node)
                                }
                            } else {
                                selectAll?.isVisible = true
                            }
                            unselect?.title = getString(R.string.action_unselect_all)
                            unselect?.isVisible = true
                        }
                    }
                } else {
                    selectAll?.isVisible = true
                    unselect?.isVisible = false
                }
            }
            return false
        }

        override fun onActionItemClicked(p0: ActionMode?, p1: MenuItem?): Boolean {
            Timber.d("onActionItemClicked")
            p1?.let { menuItem ->
                when (menuItem.itemId) {
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
            (requireActivity() as FileExplorerActivity).let { activity ->
                if (!activity.shouldReopenSearch()) {
                    activity.hideTabs(false, CLOUD_FRAGMENT)
                    activity.clearQuerySearch()
                    initOriginalData()
                }
            }
            clearSelections()
            adapter.multipleSelected = false
            checkScroll()
        }
    }

    override fun getAdapter(): RotatableAdapter = adapter

    override fun activateActionMode() {
        Timber.d("activateActionMode")
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

        (recyclerView.canScrollVertically(SCROLLING_UP_DIRECTION) || (adapter.multipleSelected))
            .let { elevate ->
                (requireActivity() as FileExplorerActivity).changeActionBarElevation(
                    elevate,
                    CLOUD_FRAGMENT
                )
            }
    }

    private fun showSortByPanel() = (requireActivity() as FileExplorerActivity).showSortByPanel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Timber.d("onCreateView $this")
        binding = FragmentFileexplorerlistBinding.inflate(
            LayoutInflater.from(requireContext()),
            container,
            false
        )

        binding.actionText.setOnClickListener { buttonClicked() }
        binding.cancelText.setOnClickListener {
            (requireActivity() as? FileExplorerActivity)?.handleBackNavigation()
        }
        binding.cancelText.text = getString(sharedR.string.general_dialog_cancel_button)
        binding.fabSelect.setOnClickListener { buttonClicked() }

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

        (requireActivity() as FileExplorerActivity).let {
            modeCloud = it.mode
            selectFile = it.isSelectFile
            parentHandle = it.parentHandleCloud

            if (parentHandle != INVALID_HANDLE && megaApi.rootNode != null && parentHandle != megaApi.rootNode?.handle) {
                it.hideTabs(true, CLOUD_FRAGMENT)
            }
        }

        adapter = MegaExplorerAdapter(
            context = requireActivity(),
            fragment = this,
            nodes = nodes,
            parentHandle = parentHandle,
            recyclerView = recyclerView,
            selectFile = selectFile,
            sortByViewModel = sortByHeaderViewModel,
            ioDispatcher = ioDispatcher,
            megaApi = megaApi
        )

        gridLayoutManager?.let {
            it.spanSizeLookup = adapter.getSpanSizeLookup(it.spanCount)
        }

        binding.fileListViewBrowser.adapter = adapter
        binding.fileGridViewBrowser.adapter = adapter
        binding.fastscroll.setRecyclerView(recyclerView)

        when {
            modeCloud == FileExplorerActivity.SELECT_CAMERA_FOLDER -> setParentHandle(INVALID_HANDLE)
            parentHandle == INVALID_HANDLE -> {
                val rootHandle = megaApi.rootNode?.handle ?: INVALID_HANDLE
                var targetPath = rootHandle
                val latestTargetPathTab = fileExplorerViewModel.latestCopyTargetPathTab
                val latestMoveTargetPathTab = fileExplorerViewModel.latestMoveTargetPathTab

                if (latestTargetPathTab == FileExplorerActivity.CLOUD_TAB || latestMoveTargetPathTab == FileExplorerActivity.CLOUD_TAB) {
                    targetPath = when (modeCloud) {
                        FileExplorerActivity.COPY -> fileExplorerViewModel.latestCopyTargetPath
                        FileExplorerActivity.MOVE -> fileExplorerViewModel.latestMoveTargetPath
                        else -> null
                    }?.also {
                        // Don't hide tabs if the target path is root of cloud drive
                        if (it != rootHandle) {
                            fileExplorerActivity.hideTabs(true, CLOUD_FRAGMENT)
                        }
                    } ?: rootHandle
                }
                setParentHandle(targetPath)
            }
        }

        getPreferences().let { prefs ->
            order = if (prefs != null && prefs.preferredSortCloud != null)
                prefs.preferredSortCloud.toInt()
            else
                ORDER_DEFAULT_ASC
        }

        initOriginalData()
        fileExplorerViewModel.initCloudDriveExplorerContent()

        when (modeCloud) {
            FileExplorerActivity.MOVE,
            FileExplorerActivity.COPY,
                -> {
                binding.actionText.text = getString(
                    if (modeCloud == FileExplorerActivity.MOVE)
                        R.string.context_move
                    else
                        R.string.context_copy
                )
                (requireActivity() as FileExplorerActivity).parentMoveCopy().let { parentNode ->
                    activateButton(modeCloud == FileExplorerActivity.COPY || parentNode == null || parentNode.handle != parentHandle)
                }
            }

            FileExplorerActivity.UPLOAD -> binding.actionText.text =
                getString(R.string.context_upload)

            FileExplorerActivity.IMPORT -> binding.actionText.text =
                getString(R.string.add_to_cloud)

            FileExplorerActivity.ALBUM_IMPORT -> binding.actionText.text =
                getString(R.string.general_save_to_cloud_drive)

            FileExplorerActivity.SAVE -> binding.actionText.text = getString(R.string.save_action)
            FileExplorerActivity.SELECT -> {
                binding.optionsExplorerLayout.isVisible = false
                activateButton(shouldShowOptionsBar(megaApi.getNodeByHandle(parentHandle)))
                binding.actionText.text = getString(R.string.general_select)
            }

            else -> binding.actionText.text = getString(R.string.general_select)
        }

        (requireActivity() as FileExplorerActivity).let {
            if (it.shouldRestartSearch) {
                isWaitingForSearchedNodes = true
                search(it.querySearch)
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        emptyRootText =
            formatEmptyScreenText(requireContext(), getString(R.string.context_empty_cloud_drive))
        emptyGeneralText = formatEmptyScreenText(
            requireContext(),
            getString(R.string.file_browser_empty_folder_new)
        )
        updateEmptyScreen()

        with(sortByHeaderViewModel) {
            showDialogEvent.observe(viewLifecycleOwner, EventObserver { showSortByPanel() })

            state.flowWithLifecycle(
                viewLifecycleOwner.lifecycle, Lifecycle.State.RESUMED
            ).onEach { state ->
                rotatableFragmentViewType = state.viewType
                switchListGridView(state.viewType)
            }.launchIn(viewLifecycleOwner.lifecycleScope)

            refreshData(isUpdatedOrderChangeState = true)

            viewLifecycleOwner.collectFlow(orderChangeState) { order ->
                (this@CloudDriveExplorerFragment.activity as? FileExplorerActivity)?.refreshCloudExplorerOrderNodes(
                    sortOrderIntMapper(order.cloudSortOrder)
                )
            }
        }
        super.onViewCreated(view, savedInstanceState)
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
            initOriginalData()
        }

    /**
     * Original data is initialized
     */
    private fun initOriginalData() =
        lifecycleScope.launch {
            val chosenNode = withContext(ioDispatcher) {
                when (fileExplorerActivity.mode) {
                    FileExplorerActivity.COPY -> {
                        megaApi.getNodeByHandle(parentHandle)
                            .takeIf { fileExplorerViewModel.latestCopyTargetPathTab == FileExplorerActivity.CLOUD_TAB }
                    }

                    FileExplorerActivity.MOVE -> {
                        megaApi.getNodeByHandle(parentHandle)
                            .takeIf { fileExplorerViewModel.latestMoveTargetPathTab == FileExplorerActivity.CLOUD_TAB }
                    }

                    else -> {
                        megaApi.getNodeByHandle(parentHandle)
                    }
                }
            }
            ensureActive()
            if (chosenNode != null && chosenNode.type != MegaNode.TYPE_ROOT) {
                updateChildNodes(chosenNode)
                Timber.d("chosenNode is: ${chosenNode.name}")
            } else {
                megaApi.rootNode?.let { rootNode ->
                    setParentHandle(rootNode.handle)
                    updateChildNodes(rootNode)
                }
            }
        }

    private suspend fun updateChildNodes(node: MegaNode) {
        val childNodes = withContext(ioDispatcher) { megaApi.getChildren(node, order) }
        setParentHandle(node.handle)
        originalData.clear()
        originalData.addAll(childNodes)
        updateNodesByAdapter(childNodes)
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
        megaApi.rootNode?.handle.let { rootHandle ->
            binding.fileListEmptyImage.setImageResource(
                if (rootHandle == parentHandle) {
                    iconPackR.drawable.ic_empty_cloud_glass
                } else {
                    iconPackR.drawable.ic_empty_folder_glass
                }
            )
            binding.fileListEmptyTextFirst.text = if (rootHandle == parentHandle) {
                emptyRootText
            } else {
                emptyGeneralText
            }
        }
    }

    /**
     * The behaviour for action text and fab select buttons clicked
     */
    private fun buttonClicked() {
        dbH.setLastCloudFolder(parentHandle.toString())
        (requireActivity() as FileExplorerActivity).let { activity ->
            if (activity.isMultiselect) {
                Timber.d("Send several files to chat")
                if (adapter.getSelectedItemCount() > 0) {
                    activity.buttonClick(adapter.getSelectedHandles())
                } else {
                    activity.showSnackbar(getString(R.string.no_files_selected_warning))
                }
            } else {
                activity.buttonClick(parentHandle)
            }
        }
    }

    /**
     * Navigating to folder
     *
     * @param handle folder handle
     */
    fun navigateToFolder(handle: Long) {
        Timber.d("Handle: %s", handle)
        var lastFirstVisiblePosition = 0
        if (sortByHeaderViewModel.isListView()) {
            listLayoutManager?.let {
                lastFirstVisiblePosition = it.findFirstCompletelyVisibleItemPosition()
            } ?: let {
                Timber.e("mLayoutManager is null")
                listLayoutManager = LinearLayoutManager(requireContext())
                recyclerView.layoutManager = listLayoutManager
            }
        } else {
            lastFirstVisiblePosition =
                gridLayoutManager?.findFirstCompletelyVisibleItemPosition() ?: 0
        }

        Timber.d("Push to stack $lastFirstVisiblePosition position")
        lastPositionStack.push(lastFirstVisiblePosition)

        setParentHandle(handle)
        originalData.clear()
        updateNodesByAdapter(originalData)
        recyclerView.scrollToPosition(0)

        if (modeCloud == FileExplorerActivity.MOVE || modeCloud == FileExplorerActivity.COPY || modeCloud == FileExplorerActivity.SELECT)
            activateButton(true)
    }

    /**
     * The behaviour that item is clicked
     *
     * @param position the position of item
     */
    fun itemClick(position: Int) {
        Timber.d("Position: $position")
        if (searchNodes.isNotEmpty()) {
            shouldResetNodes = false
            (requireActivity() as FileExplorerActivity).let {
                it.setQueryAfterSearch()
                it.collapseSearchView()
            }
        }

        (requireActivity() as FileExplorerActivity).let { activity ->
            adapter.getItem(position)?.let { node ->
                if (node.isFolder) {
                    searchNodes.clear()
                    activity.shouldRestartSearch = false

                    if (selectFile && activity.isMultiselect && adapter.multipleSelected) {
                        hideMultipleSelect()
                    }
                    activity.hideTabs(true, CLOUD_FRAGMENT)

                    var lastFirstVisiblePosition = 0
                    if (sortByHeaderViewModel.isListView()) {
                        listLayoutManager?.let {
                            lastFirstVisiblePosition = it.findFirstCompletelyVisibleItemPosition()
                        } ?: let {
                            Timber.e("mLayoutManager is null")
                            listLayoutManager = LinearLayoutManager(requireContext())
                            recyclerView.layoutManager = listLayoutManager
                        }
                    } else {
                        // For grid view, just add null check
                        lastFirstVisiblePosition =
                            gridLayoutManager?.findFirstCompletelyVisibleItemPosition() ?: 0
                    }
                    Timber.d("Push to stack $lastFirstVisiblePosition position")
                    lastPositionStack.push(lastFirstVisiblePosition)

                    if (modeCloud == FileExplorerActivity.SELECT)
                        activateButton(!selectFile)

                    setParentHandle(node.handle)
                    updateNodesByAdapter(megaApi.getChildren(node, order))
                    recyclerView.scrollToPosition(0)

                    if (modeCloud == FileExplorerActivity.MOVE || modeCloud == FileExplorerActivity.COPY) {
                        if (adapter.itemCount == 0) {
                            activateButton(true)
                        } else {
                            activity.parentMoveCopy().let { parentNode ->
                                activateButton(modeCloud == FileExplorerActivity.COPY || parentNode == null || parentNode.handle != parentHandle)
                            }
                        }
                    }
                } else {
                    if (selectFile) {
                        if (activity.isMultiselect) {
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
                            //Send file
                            activity.buttonClick(node.handle)
                        }
                    }
                }
            }
        }
        shouldResetNodes = true
    }

    /**
     * Whether should show option bar
     *
     * @param parentNode parent node
     * @return true is that shows option bar, otherwise is false
     */
    private fun shouldShowOptionsBar(parentNode: MegaNode?): Boolean {
        if (!selectFile) {
            megaApi.rootNode?.let { rootNode ->
                return parentNode != null && parentNode.handle != rootNode.handle
            }
        }
        return false
    }

    /**
     * The behaviour when back button is pressed
     *
     * @return the result code
     */
    fun onBackPressed(): Int =
        (requireActivity() as FileExplorerActivity).let { activity ->
            Timber.d("onBackPressed")
            if (selectFile && activity.isMultiselect && adapter.multipleSelected)
                hideMultipleSelect()
            val parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle))
                ?: when {
                    activity.intent.action == ACTION_SAVE_TO_CLOUD -> {
                        megaApi.rootNode?.takeIf { parentHandle != it.handle }?.also {
                            fileExplorerActivity.setCurrentTab(TAB_POSITION_INCOMING)
                        }
                    }

                    else -> null
                }

            if (parentNode == null) return 0

            if (modeCloud == FileExplorerActivity.SELECT)
                activateButton(shouldShowOptionsBar(parentNode))

            setParentHandle(parentNode.handle)
            if (parentNode.type == MegaNode.TYPE_ROOT)
                activity.hideTabs(false, CLOUD_FRAGMENT)

            activity.changeTitle()

            if (modeCloud == FileExplorerActivity.MOVE || modeCloud == FileExplorerActivity.COPY) {
                activity.parentMoveCopy()?.let {
                    activateButton(modeCloud == FileExplorerActivity.COPY || it.handle != parentNode.handle)
                } ?: activateButton(true)
            }

            recyclerView.isVisible = true
            binding.fileListEmptyImage.isVisible = false
            binding.fileListEmptyText.isVisible = false

            updateNodesByAdapter(megaApi.getChildren(parentNode, order))
            var lastVisiblePosition = 0
            if (lastPositionStack.isNotEmpty()) {
                lastVisiblePosition = lastPositionStack.pop()
                Timber.d("Pop of the stack $lastVisiblePosition position")
            }
            Timber.d("Scroll to $lastVisiblePosition position")

            if (lastVisiblePosition >= 0) {
                if (sortByHeaderViewModel.isListView()) {
                    listLayoutManager
                } else {
                    gridLayoutManager
                }?.scrollToPositionWithOffset(lastVisiblePosition, 0)
            }
            2
        }

    private fun setParentHandle(handle: Long) {
        Timber.d("Parent handle: $handle")
        parentHandle = handle
        adapter.parentHandle = handle
        (requireActivity() as FileExplorerActivity).let {
            it.parentHandleCloud = handle
            it.changeTitle()
        }
    }

    /**
     * Update nodes by adapter
     *
     * @param sourceData original nodes
     */
    fun updateNodesByAdapter(sourceData: List<MegaNode?>) {
        val data = if (fileExplorerViewModel.showHiddenItems) {
            sourceData
        } else {
            sourceData.filter {
                it != null && !it.isMarkedSensitive && !megaApi.isSensitiveInherited(it)
            }
        }
        data.toList().let {
            nodes.clear()
            adapter.setAccountDetail(fileExplorerViewModel.accountDetail)
            adapter.setNodes(it)
            nodes.addAll(it)
            updateView()
        }
    }

    /**
     * Select all items
     */
    private fun selectAll() {
        Timber.d("selectAll")
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
        Timber.d("hideMultipleSelect")
        adapter.multipleSelected = false
        adapter.clearSelections()
        actionMode?.finish()
        if (isMultiselect()) {
            activateButton(false)
        }
    }

    /**
     * Active button
     *
     * @param show true is that shows button, otherwise is false
     */
    private fun activateButton(show: Boolean) {
        if (modeCloud == FileExplorerActivity.SELECT)
            if (selectFile) {
                binding.fabSelect.isVisible = show
            } else {
                binding.optionsExplorerLayout.isVisible = show
            }
        else
            binding.actionText.isEnabled = show
    }

    /**
     * Set nodes by order
     *
     * @param order nodes order
     */
    fun orderNodes(order: Int) {
        this.order = order
        orderJob?.cancel() // Cancel last job
        orderJob = lifecycleScope.launch {
            val children = withContext(ioDispatcher) {
                runCatching {
                    megaApi.getChildren(
                        if (parentHandle == INVALID_HANDLE) {
                            megaApi.rootNode
                        } else {
                            megaApi.getNodeByHandle(parentHandle)
                        }, order
                    )
                }.getOrNull()
            }

            children?.takeIf { isActive }?.let {
                originalData.clear()
                originalData.addAll(it)
                updateNodesByAdapter(originalData)
            }
        }
    }

    /**
     * Whether multiple select
     *
     * @return true is multiple select, otherwise is false
     */
    private fun isMultiselect() = modeCloud == FileExplorerActivity.SELECT
            && selectFile && (requireActivity() as FileExplorerActivity).isMultiselect

    /**
     * Search nodes based on search string
     *
     * @param searchString search strings
     */
    fun search(searchString: String?) {
        if (searchString == null || !shouldResetNodes) {
            return
        }
        if (parentHandle == INVALID_HANDLE)
            setParentHandle(megaApi.rootNode?.handle ?: INVALID_HANDLE)
        lifecycleScope.launch {
            initNewSearch()
            runCatching {
                legacySearchUseCase(
                    query = searchString,
                    parentHandle = NodeId(parentHandle),
                    nodeSourceType = NodeSourceType.CLOUD_DRIVE
                )
            }.onSuccess {
                finishSearch(it)
            }.onFailure {
                Timber.e(it)
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
    private fun setSearchNodes(nodes: List<MegaNode>) {
        searchNodes.clear()
        searchNodes.addAll(nodes)
        (requireActivity() as FileExplorerActivity).shouldRestartSearch = true
        adapter.setNodes(searchNodes)
        updateView()
        if (isWaitingForSearchedNodes) {
            reDoTheSelectionAfterRotation()
        }
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

    companion object {
        private const val SPAN_COUNT = 2

        /**
         * Create new instance
         *
         * @return new instance
         */
        fun newInstance(): CloudDriveExplorerFragment {
            Timber.d("newInstance")
            return CloudDriveExplorerFragment()
        }
    }
}