package mega.privacy.android.app.fragments.offline

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.observeDragSupportEvents
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.putThumbnailLocation
import mega.privacy.android.app.databinding.FragmentOfflineBinding
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.interfaces.Scrollable
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.fragments.homepage.disableRecyclerViewAnimator
import mega.privacy.android.app.fragments.homepage.main.HomepageFragmentDirections
import mega.privacy.android.app.globalmanagement.SortOrderManagement
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.main.*
import mega.privacy.android.app.textEditor.TextEditorActivity
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.setLocalIntentParams
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.OfflineUtils.getOfflineFile
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.app.utils.Util.*
import mega.privacy.android.app.zippreview.ui.ZipBrowserActivity
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import java.io.File
import javax.inject.Inject
import kotlin.collections.ArrayList

@AndroidEntryPoint
class OfflineFragment : Fragment(), ActionMode.Callback, Scrollable {

    companion object {
        const val REFRESH_OFFLINE_FILE_LIST = "refresh_offline_file_list"
        const val SHOW_OFFLINE_WARNING = "SHOW_OFFLINE_WARNING"
    }

    @Inject
    lateinit var sortOrderManagement: SortOrderManagement

    private val args: OfflineFragmentArgs by navArgs()
    private var binding by autoCleared<FragmentOfflineBinding>()
    private val viewModel: OfflineViewModel by viewModels()
    private val sortByHeaderViewModel by viewModels<SortByHeaderViewModel>()

    private var recyclerView: RecyclerView? = null
    private var listDivider: PositionDividerItemDecoration? = null
    private var adapter: OfflineAdapter? = null
    private var actionMode: ActionMode? = null

    private val receiverRefreshOffline = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshNodes()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments == null) {
            arguments = HomepageFragmentDirections.actionHomepageToFullscreenOffline().arguments
        }
    }

    override fun onStart() {
        super.onStart()

        // TODO: workaround for navigation with ManagerActivity
        callManager {
            if (args.rootFolderOnly) {
                it.pagerOfflineFragmentOpened(this)
            } else {
                it.fullscreenOfflineFragmentOpened(this)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        requireContext()
            .registerReceiver(receiverRefreshOffline, IntentFilter(REFRESH_OFFLINE_FILE_LIST))

        viewModel.loadOfflineNodes()
    }

    override fun onPause() {
        super.onPause()

        requireContext().unregisterReceiver(receiverRefreshOffline)
    }

    override fun onStop() {
        super.onStop()

        // TODO: workaround for navigation with ManagerActivity
        callManager {
            if (args.rootFolderOnly) {
                it.pagerOfflineFragmentClosed(this)
            } else {
                it.fullscreenOfflineFragmentClosed(this)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOfflineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupView()
        observeLiveData()

        if (viewModel.path == "" || viewModel.path == args.path) {
            setViewModelDisplayParam(args.path)
        } else {
            setViewModelDisplayParam(viewModel.path)
        }

        observeDragSupportEvents(viewLifecycleOwner, recyclerView!!, VIEWER_FROM_OFFLINE)
    }

    private fun setViewModelDisplayParam(path: String) {
        viewModel.setDisplayParam(
            args.rootFolderOnly, isList(),
            if (isList()) 0 else binding.offlineBrowserGrid.spanCount, path,
            sortOrderManagement.getOrderCloud()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()

        viewModel.clearEmptySearchQuery()
    }

    private fun setupView() {
        setupOfflineWarning()

        adapter =
            OfflineAdapter(isList(), sortByHeaderViewModel, object : OfflineAdapterListener {
                override fun onNodeClicked(position: Int, node: OfflineNode) {
                    var firstVisiblePosition: Int
                    if (isList()) {
                        firstVisiblePosition =
                            (binding.offlineBrowserList.layoutManager as LinearLayoutManager)
                                .findFirstCompletelyVisibleItemPosition()
                    } else {
                        firstVisiblePosition =
                            binding.offlineBrowserGrid.findFirstCompletelyVisibleItemPosition()
                        if (firstVisiblePosition == INVALID_POSITION) {
                            firstVisiblePosition =
                                binding.offlineBrowserGrid.findFirstVisibleItemPosition()
                        }
                    }
                    viewModel.onNodeClicked(position, node, firstVisiblePosition)
                }

                override fun onNodeLongClicked(position: Int, node: OfflineNode) {
                    viewModel.onNodeLongClicked(position, node)
                }

                override fun onOptionsClicked(position: Int, node: OfflineNode) {
                    viewModel.onNodeOptionsClicked(position, node)
                }
            })

        adapter?.setHasStableIds(true)
        adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (!viewModel.skipNextAutoScroll) {
                    scrollToPosition(0)
                }
            }
        })

        binding.offlineBrowserList.layoutManager = LinearLayoutManager(context)

        (binding.offlineBrowserGrid.layoutManager as CustomizedGridLayoutManager).spanSizeLookup =
            object : SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (adapter?.getItemViewType(position) == OfflineAdapter.TYPE_HEADER) {
                        binding.offlineBrowserGrid.spanCount
                    } else {
                        1
                    }
                }
            }

        setupRecyclerView(binding.offlineBrowserList)
        setupRecyclerView(binding.offlineBrowserGrid)

        recyclerView = if (isList()) {
            binding.offlineBrowserList.isVisible = true
            binding.offlineBrowserList
        } else {
            binding.offlineBrowserGrid.isVisible = true
            binding.offlineBrowserGrid
        }
        recyclerView?.adapter = adapter

        if (args.rootFolderOnly) {
            binding.offlineBrowserList.addItemDecoration(
                SimpleDividerItemDecoration(requireContext())
            )
        } else {
            listDivider = PositionDividerItemDecoration(requireContext(), resources.displayMetrics)
            binding.offlineBrowserList.addItemDecoration(listDivider!!)
        }

        var textToShow = StringResourcesUtils.getString(R.string.context_empty_offline)

        try {
            textToShow = textToShow.replace(
                "[A]", "<font color=\'"
                        + getColorHexString(requireContext(), R.color.grey_900_grey_100)
                        + "\'>"
            ).replace("[/A]", "</font>").replace(
                "[B]", "<font color=\'"
                        + getColorHexString(requireContext(), R.color.grey_300_grey_600)
                        + "\'>"
            ).replace("[/B]", "</font>")
        } catch (e: Exception) {
            e.printStackTrace()
            logError("Exception formatting string", e)
        }

        binding.emptyHintText.text = textToShow.toSpannedHtmlText()

        checkScroll()
    }

    private fun setupOfflineWarning() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        binding.offlineWarningLayout.isVisible =
            preferences.getBoolean(SHOW_OFFLINE_WARNING, true)

        binding.offlineWarningClose.setOnClickListener {
            preferences.edit().putBoolean(SHOW_OFFLINE_WARNING, false).apply()
            binding.offlineWarningLayout.isVisible = false
            checkScroll()
        }
    }

    private fun setupRecyclerView(rv: RecyclerView) {
        rv.apply {
            setPadding(0, 0, 0, scaleHeightPx(85, resources.displayMetrics))
            clipToPadding = false
            setHasFixedSize(true)
            itemAnimator = noChangeRecyclerViewItemAnimator()

            if (!args.rootFolderOnly) {
                addOnScrollListener(object : OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        checkScroll()
                    }
                })
            }
        }
    }

    private fun observeLiveData() {
        viewModel.nodes.observe(viewLifecycleOwner) {
            val nodes = it.first
            val autoScrollPos = it.second
            recyclerView?.isVisible = nodes.isNotEmpty()
            binding.emptyHint.isVisible = nodes.isEmpty()

            adapter?.submitList(nodes) {
                if (!viewModel.skipNextAutoScroll) {
                    scrollToPosition(autoScrollPos)
                }
                viewModel.skipNextAutoScroll = false
            }

            listDivider?.setDrawAllDividers(viewModel.searchMode())
            if (!args.rootFolderOnly) {
                callManager { manager ->
                    manager.updateFullscreenOfflineFragmentOptionMenu(false)
                }
            }
        }

        viewModel.openFolderFullscreen.observe(viewLifecycleOwner, EventObserver {
            callManager { manager ->
                manager.openFullscreenOfflineFragment(it)
            }
        })

        viewModel.showOptionsPanel.observe(viewLifecycleOwner, EventObserver {
            callManager { manager ->
                manager.showOptionsPanel(it)
            }
        })

        viewModel.nodeToOpen.observe(viewLifecycleOwner, EventObserver {
            openNode(it.first, it.second)
        })

        viewModel.urlFileOpenAsUrl.observe(viewLifecycleOwner, EventObserver {
            logDebug("Is URL - launch browser intent")
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(it)
            startActivity(intent)
        })

        viewModel.urlFileOpenAsFile.observe(viewLifecycleOwner, EventObserver {
            openFile(it)
        })

        if (args.rootFolderOnly) {
            return
        }

        viewModel.actionMode.observe(viewLifecycleOwner) { visible ->
            val actionModeVal = actionMode

            if (visible) {
                if (actionModeVal == null) {
                    callManager {
                        actionMode = it.startSupportActionMode(this)
                        it.setTextSubmitted()
                    }
                }

                actionMode?.title = viewModel.getSelectedNodesCount().toString()
                actionMode?.invalidate()
            } else {
                if (actionModeVal != null) {
                    actionModeVal.finish()
                    actionMode = null
                }
            }
        }

        viewModel.actionBarTitle.observe(viewLifecycleOwner) {
            callManager { manager ->
                if (viewModel.selecting) {
                    manager.supportActionBar?.setTitle(it)
                } else {
                    manager.setToolbarTitleFromFullscreenOfflineFragment(
                        it, false, !viewModel.searchMode() && getItemCount() > 0
                    )
                }
            }
        }

        viewModel.pathLiveData.observe(viewLifecycleOwner) {
            callManager { manager ->
                manager.pathNavigationOffline = it
            }
        }

        viewModel.submitSearchQuery.observe(viewLifecycleOwner) {
            callManager { manager ->
                manager.setTextSubmitted()
            }
        }

        viewModel.closeSearchView.observe(viewLifecycleOwner) {
            callManager { manager ->
                manager.textSubmitted = true
                manager.closeSearchView()
            }
        }

        viewModel.showSortedBy.observe(viewLifecycleOwner, EventObserver {
            callManager { manager ->
                manager.showNewSortByPanel(ORDER_OFFLINE)
            }
        })

        observeAnimatedItems()

        sortByHeaderViewModel.showDialogEvent.observe(viewLifecycleOwner, EventObserver {
            callManager { manager ->
                manager.showNewSortByPanel(ORDER_OFFLINE)
            }
        })

        sortByHeaderViewModel.orderChangeEvent.observe(viewLifecycleOwner, EventObserver {
            viewModel.setOrder(it.third)
            adapter?.notifyItemChanged(0)
        })

        sortByHeaderViewModel.listGridChangeEvent.observe(viewLifecycleOwner, EventObserver {
            switchListGridView()
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.updateNodes.collect {
                    refreshNodes()
                }
            }
        }
    }

    private fun scrollToPosition(position: Int) {
        val layoutManager = recyclerView?.layoutManager

        if (layoutManager is LinearLayoutManager && position >= 0) {
            layoutManager.scrollToPositionWithOffset(position, 0)
        }
    }

    private fun observeAnimatedItems() {
        var animatorSet: AnimatorSet? = null

        viewModel.nodesToAnimate.observe(viewLifecycleOwner) {
            val rvAdapter = adapter ?: return@observe

            animatorSet?.run {
                // End the started animation if any, or the view may show messy as its property
                // would be wrongly changed by multiple animations running at the same time
                // via contiguous quick clicks on the item
                if (isStarted) {
                    end()
                }
            }

            // Must create a new AnimatorSet, or it would keep all previous
            // animation and play them together
            animatorSet = AnimatorSet()
            val animatorList = mutableListOf<Animator>()

            animatorSet?.addListener(object : AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {
                }

                override fun onAnimationEnd(animation: Animator?) {
                    viewModel.nodes.value?.let { newList ->
                        rvAdapter.submitList(ArrayList(newList.first))
                    }
                }

                override fun onAnimationCancel(animation: Animator?) {
                }

                override fun onAnimationStart(animation: Animator?) {
                }
            })

            it.forEach { pos ->
                recyclerView?.findViewHolderForAdapterPosition(pos)?.let { viewHolder ->
                    val itemView = viewHolder.itemView

                    val imageView: ImageView? = when (rvAdapter.getItemViewType(pos)) {
                        OfflineAdapter.TYPE_LIST -> {
                            val thumbnail = itemView.findViewById<ImageView>(R.id.thumbnail)
                            val param = thumbnail.layoutParams as FrameLayout.LayoutParams
                            param.width = dp2px(
                                OfflineListViewHolder.LARGE_IMAGE_WIDTH,
                                resources.displayMetrics
                            )
                            param.height = param.width
                            param.marginStart = dp2px(
                                OfflineListViewHolder.LARGE_IMAGE_MARGIN_LEFT,
                                resources.displayMetrics
                            )
                            thumbnail.layoutParams = param
                            thumbnail
                        }
                        OfflineAdapter.TYPE_GRID_FOLDER -> {
                            itemView.background = ContextCompat.getDrawable(
                                requireContext(), R.drawable.background_item_grid_selected
                            )
                            itemView.findViewById(R.id.icon)
                        }
                        OfflineAdapter.TYPE_GRID_FILE -> {
                            itemView.background = ContextCompat.getDrawable(
                                requireContext(), R.drawable.background_item_grid_selected
                            )
                            itemView.findViewById(R.id.ic_selected)
                        }
                        else -> null
                    }

                    imageView?.run {
                        setImageResource(R.drawable.ic_select_folder)
                        visibility = View.VISIBLE

                        val animator =
                            AnimatorInflater.loadAnimator(context, R.animator.icon_select)
                        animator.setTarget(this)
                        animatorList.add(animator)
                    }
                }
            }

            animatorSet?.playTogether(animatorList)
            animatorSet?.start()
        }
    }

    private fun openNode(position: Int, node: OfflineNode) {
        val file = getOfflineFile(context, node.node)
        val mime = MimeTypeList.typeForName(file.name)

        when {
            mime.isZip -> {
                logDebug("MimeTypeList ZIP")
                ZipBrowserActivity.start(requireActivity(), file.path)
            }
            mime.isImage -> {
                val handles = adapter!!.getOfflineNodes().map { it.handle.toLong() }.toLongArray()
                val intent = ImageViewerActivity.getIntentForOfflineChildren(
                    requireContext(),
                    handles,
                    node.node.handle.toLongOrNull()
                )
                putThumbnailLocation(intent, recyclerView!!, position, VIEWER_FROM_OFFLINE, adapter!!)
                startActivity(intent)
                requireActivity().overridePendingTransition(0, 0)
            }
            mime.isVideoReproducible || mime.isAudio -> {
                logDebug("Video/Audio file")

                val mediaIntent: Intent
                val internalIntent: Boolean
                var opusFile = false
                if (mime.isVideoNotSupported || mime.isAudioNotSupported) {
                    mediaIntent = Intent(Intent.ACTION_VIEW)
                    internalIntent = false
                    val s: Array<String> = file.name.split("\\.".toRegex()).toTypedArray()
                    if (s.size > 1 && s[s.size - 1] == "opus") {
                        opusFile = true
                    }
                } else {
                    internalIntent = true
                    mediaIntent = getMediaIntent(context, file.name)
                }

                mediaIntent.putExtra(INTENT_EXTRA_KEY_HANDLE, node.node.handle.toLong())
                mediaIntent.putExtra(INTENT_EXTRA_KEY_FILE_NAME, node.node.name)
                mediaIntent.putExtra(INTENT_EXTRA_KEY_PATH, file.absolutePath)
                mediaIntent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, OFFLINE_ADAPTER)
                mediaIntent.putExtra(INTENT_EXTRA_KEY_POSITION, position)
                mediaIntent.putExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, INVALID_HANDLE)
                mediaIntent.putExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY, file.parent)

                putThumbnailLocation(mediaIntent, recyclerView!!, position, VIEWER_FROM_OFFLINE, adapter!!)

                mediaIntent.putExtra(
                    INTENT_EXTRA_KEY_ARRAY_OFFLINE, ArrayList(adapter!!.getOfflineNodes())
                )
                mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                if (!setLocalIntentParams(
                        context, node.node, mediaIntent, file.absolutePath,
                        false, requireActivity() as ManagerActivity
                    )
                ) {
                    return
                }

                if (opusFile) {
                    mediaIntent.setDataAndType(mediaIntent.data, "audio/*")
                }

                if (internalIntent) {
                    startActivity(mediaIntent)
                    requireActivity().overridePendingTransition(0, 0)
                } else if (MegaApiUtils.isIntentAvailable(context, mediaIntent)) {
                    startActivity(mediaIntent)
                } else {
                    callManager {
                        it.showSnackbar(
                            SNACKBAR_TYPE,
                            getString(R.string.intent_not_available),
                            MEGACHAT_INVALID_HANDLE
                        )
                    }

                    val intentShare = Intent(Intent.ACTION_SEND)
                    if (setLocalIntentParams(
                            context, node.node, intentShare, file.absolutePath,
                            false, requireActivity() as ManagerActivity
                        )
                    ) {
                        intentShare.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        if (MegaApiUtils.isIntentAvailable(context, intentShare)) {
                            logDebug("Call to startActivity(intentShare)")
                            startActivity(intentShare)
                        }
                    }
                }
            }
            mime.isPdf -> {
                logDebug("PDF file")

                val pdfIntent = Intent(context, PdfViewerActivity::class.java)

                pdfIntent.putExtra(INTENT_EXTRA_KEY_INSIDE, true)
                pdfIntent.putExtra(INTENT_EXTRA_KEY_HANDLE, node.node.handle.toLong())
                pdfIntent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, OFFLINE_ADAPTER)
                pdfIntent.putExtra(INTENT_EXTRA_KEY_PATH, file.absolutePath)
                pdfIntent.putExtra(INTENT_EXTRA_KEY_PATH_NAVIGATION, viewModel.path)

                putThumbnailLocation(pdfIntent, recyclerView!!, position, VIEWER_FROM_OFFLINE, adapter!!)

                if (setLocalIntentParams(
                        context, node.node, pdfIntent, file.absolutePath, false,
                        requireActivity() as ManagerActivity
                    )
                ) {
                    pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(pdfIntent)
                    requireActivity().overridePendingTransition(0, 0)
                }
            }
            mime.isURL -> {
                logDebug("Is URL file")
                viewModel.processUrlFile(file)
            }
            mime.isOpenableTextFile(file.length()) -> {
                startActivity(
                    Intent(requireContext(), TextEditorActivity::class.java)
                        .putExtra(INTENT_EXTRA_KEY_FILE_NAME, file.name)
                        .putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, OFFLINE_ADAPTER)
                        .putExtra(INTENT_EXTRA_KEY_PATH, file.absolutePath)
                )
            }
            else -> {
                openFile(file)
            }
        }
    }

    private fun openFile(file: File) {
        logDebug("openFile")
        val viewIntent = Intent(Intent.ACTION_VIEW)

        if (!setLocalIntentParams(
                context, file.name, viewIntent, file.absolutePath, false,
                requireActivity() as ManagerActivity
            )
        ) {
            return
        }

        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        if (MegaApiUtils.isIntentAvailable(context, viewIntent)) {
            startActivity(viewIntent)
        } else {
            val intentShare = Intent(Intent.ACTION_SEND)

            if (!setLocalIntentParams(
                    context, file.name, intentShare, file.absolutePath, false,
                    requireActivity() as ManagerActivity
                )
            ) {
                return
            }

            intentShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            if (MegaApiUtils.isIntentAvailable(context, intentShare)) {
                startActivity(intentShare)
            }
        }
    }

    private fun isList(): Boolean {
        return callManager { it.isList } ?: true || args.rootFolderOnly
    }

    override fun checkScroll() {
        val rv = recyclerView ?: return

        callManager { manager ->
            manager.changeAppBarElevation(!args.rootFolderOnly
                    && (rv.canScrollVertically(SCROLLING_UP_DIRECTION) || viewModel.selecting || binding.offlineWarningLayout.isVisible))
        }
    }

    fun setSearchQuery(query: String?) {
        viewModel.setSearchQuery(query)
        recyclerView?.let { disableRecyclerViewAnimator(it) }
    }

    fun onSearchQuerySubmitted() {
        viewModel.onSearchQuerySubmitted()
    }

    fun selectAll() {
        viewModel.selectAll()
    }

    fun onBackPressed(): Int {
        return viewModel.navigateOut(args.path)
    }

    fun getItemCount(): Int {
        return viewModel.getDisplayedNodesCount()
    }

    fun searchMode() = viewModel.searchMode()

    fun refreshNodes() {
        viewModel.loadOfflineNodes()
    }

    fun refreshActionBarTitle() {
        viewModel.refreshActionBarTitle()
    }

    private fun switchListGridView() {
        recyclerView = if (isList()) {
            binding.offlineBrowserList.isVisible = true
            binding.offlineBrowserList.adapter = adapter
            binding.offlineBrowserGrid.isVisible = false
            binding.offlineBrowserGrid.adapter = null
            adapter?.isList = true

            binding.offlineBrowserList
        } else {
            binding.offlineBrowserGrid.isVisible = true
            binding.offlineBrowserGrid.adapter = adapter
            binding.offlineBrowserList.isVisible = false
            binding.offlineBrowserList.adapter = null
            adapter?.isList = false

            binding.offlineBrowserGrid
        }

        setViewModelDisplayParam(viewModel.path)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        viewModel.skipNextAutoScroll = true
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        logDebug("ActionBarCallBack::onActionItemClicked")

        when (item!!.itemId) {
            R.id.cab_menu_download -> {
                callManager { it.saveOfflineNodesToDevice(viewModel.getSelectedNodes()) }
                viewModel.clearSelection()
            }
            R.id.cab_menu_share_out -> {
                OfflineUtils.shareOfflineNodes(requireContext(), viewModel.getSelectedNodes())
                viewModel.clearSelection()
            }
            R.id.cab_menu_delete -> {
                callManager {
                    it.showConfirmationRemoveSomeFromOffline(viewModel.getSelectedNodes()) {
                        viewModel.clearSelection()
                    }
                }
            }
            R.id.cab_menu_select_all -> {
                viewModel.selectAll()
            }
            R.id.cab_menu_clear_selection -> {
                viewModel.clearSelection()
            }
        }

        return false
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        logDebug("ActionBarCallBack::onCreateActionMode")
        val inflater = mode!!.menuInflater

        inflater.inflate(R.menu.offline_browser_action, menu)
        checkScroll()

        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        logDebug("ActionBarCallBack::onPrepareActionMode")

        menu!!.findItem(R.id.cab_menu_select_all).isVisible =
            (viewModel.getSelectedNodesCount()
                    < getItemCount() - viewModel.placeholderCount)

        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        logDebug("ActionBarCallBack::onDestroyActionMode")

        viewModel.clearSelection()
        checkScroll()
    }
}
