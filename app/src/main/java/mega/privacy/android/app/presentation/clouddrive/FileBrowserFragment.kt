package mega.privacy.android.app.presentation.clouddrive

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.components.NewGridRecyclerView
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.observeDragSupportEvents
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.putThumbnailLocation
import mega.privacy.android.app.components.scrollBar.FastScroller
import mega.privacy.android.app.constants.EventConstants.EVENT_SHOW_MEDIA_DISCOVERY
import mega.privacy.android.app.databinding.FragmentFileBrowserBinding
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
import mega.privacy.android.app.presentation.movenode.MoveRequestResult
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
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.displayMetrics
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.preference.ViewType
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import timber.log.Timber
import java.io.File
import java.util.Locale
import javax.inject.Inject

/**
 * A [RotatableFragment] that displays the user's content
 */
@AndroidEntryPoint
class FileBrowserFragment : RotatableFragment() {

    /**
     * [TransfersManagement]
     */
    @Inject
    lateinit var transfersManagement: TransfersManagement

    /**
     * Used to perform API Calls
     */
    @Inject
    @MegaApi
    lateinit var megaApi: MegaApiAndroid

    private val managerViewModel by activityViewModels<ManagerViewModel>()
    private val fileBrowserViewModel by activityViewModels<FileBrowserViewModel>()
    private val sortByHeaderViewModel by activityViewModels<SortByHeaderViewModel>()

    private var actionMode: ActionMode? = null

    // UI Elements
    private var emptyListImageView: ImageView? = null
    private var emptyListTextView: TextView? = null
    private var fastScroller: FastScroller? = null
    private var transferOverQuotaBanner: RelativeLayout? = null
    private var transferOverQuotaBannerTextView: TextView? = null
    private var recyclerView: NewGridRecyclerView? = null
    private val itemDecoration: PositionDividerItemDecoration by lazy(LazyThreadSafetyMode.NONE) {
        PositionDividerItemDecoration(requireContext(), displayMetrics())
    }

    private var megaNodeAdapter: MegaNodeAdapter? = null

    // Backup warning dialog
    private var backupWarningDialog: AlertDialog? = null
    private var backupHandleList: ArrayList<Long>? = null
    private var backupDialogType = -1
    private var backupNodeHandle: Long? = null
    private var backupNodeType = 0
    private var backupActionType = 0
    private var fileBackupManager: FileBackupManager? = null

    private var mediaDiscoveryViewSettings = MediaDiscoveryViewSettings.INITIAL.ordinal

    /**
     * The UI State from [FileBrowserViewModel]
     *
     * @return The UI State
     */
    private fun fileBrowserState() = fileBrowserViewModel.state.value

    /**
     * Checks whether the [MegaNodeAdapter]'s Multiple Select feature is enabled or not
     */
    private val multipleSelectEnabled: Boolean
        get() = megaNodeAdapter?.isMultipleSelect == true

    /**
     * Retrieves the total number of items in the [MegaNodeAdapter]
     */
    val itemCount: Int
        get() = megaNodeAdapter?.itemCount ?: 0

    /**
     * onSaveInstanceState
     */
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

    /**
     * onAttach
     */
    override fun onAttach(context: Context) {
        Timber.d("onAttach")
        super.onAttach(context)

        fileBackupManager = FileBackupManager(
            activity = requireActivity(),
            actionBackupListener = object : ActionBackupListener {
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

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        Timber.d("onCreateView")

        if (megaApi.rootNode == null || !isAdded) return null

        val binding = FragmentFileBrowserBinding.inflate(inflater, container, false)

        handleBackupNodeBehavior(savedInstanceState)
        setupToolbar()
        setupUIBindings(binding)
        setupTransferOverQuotaBannerListeners(binding)
        changeTransferOverQuotaBannerVisibility()
        setupFastScroller()
        setupAdapter()
        setupRecyclerView()
        selectNewlyAddedNodes()
        switchViewType()
        animateViewInFolderNode()

        return binding.root
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    /**
     * onDestroyView
     */
    override fun onDestroyView() {
        super.onDestroyView()

        megaNodeAdapter?.clearTakenDownDialog()
    }

    /**
     * onBackPressed
     */
    fun onBackPressed(): Int {
        Timber.d("onBackPressed")
        megaNodeAdapter?.let {

            Timber.d("Parent Handle is: ${fileBrowserViewModel.getSafeBrowserParentHandle()}")
            val managerActivity = requireActivity() as ManagerActivity
            return if (managerActivity.comesFromNotifications && managerActivity.comesFromNotificationHandle == fileBrowserViewModel.getSafeBrowserParentHandle()) {
                managerActivity.restoreFileBrowserAfterComingFromNotification()
                2
            } else {
                fileBrowserState().parentHandle?.let {
                    fileBrowserViewModel.onBackPressed()
                    recyclerView?.visibility = View.VISIBLE
                    emptyListImageView?.visibility = View.GONE
                    emptyListTextView?.visibility = View.GONE
                    setupToolbar()

                    val lastVisiblePosition = fileBrowserViewModel.popLastPositionStack()
                    Timber.d("Scroll to $lastVisiblePosition position")
                    if (lastVisiblePosition >= 0 && fileBrowserState().currentViewType == ViewType.LIST) {
                        recyclerView?.scrollToPosition(lastVisiblePosition)
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
     * updateActionModeTitle
     */
    override fun updateActionModeTitle() {
        Timber.d("updateActionModeTitle")
        if (actionMode == null) {
            Timber.w("RETURN: null values")
            return
        }
        val documents = megaNodeAdapter?.selectedNodes
        val fileOrFolderCount = documents?.count { it.isFile || it.isFolder } ?: 0
        actionMode?.title = "$fileOrFolderCount"
        try {
            actionMode?.invalidate()
        } catch (e: NullPointerException) {
            e.printStackTrace()
            Timber.e(e, "Invalidate error")
        }
    }

    /**
     * getAdapter
     */
    override fun getAdapter(): RotatableAdapter? = megaNodeAdapter

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
     * activateActionMode
     */
    override fun activateActionMode() {
        Timber.d("activateActionMode")
        if (!multipleSelectEnabled) {
            megaNodeAdapter?.isMultipleSelect = true
            actionMode =
                (requireActivity() as? AppCompatActivity)?.startSupportActionMode(ActionBarCallBack())
        }
    }

    /**
     * Handle specific behavior when the Node is a Backup Node
     *
     * @param savedInstanceState A potentially nullable Saved State
     */
    private fun handleBackupNodeBehavior(savedInstanceState: Bundle?) {
        savedInstanceState?.let { nonNullState ->
            backupHandleList = nonNullState.serializable(BACKUP_HANDLED_ITEM) as ArrayList<Long>?
            backupNodeHandle = nonNullState.getLong(BACKUP_HANDLED_NODE, -1)
            backupNodeType = nonNullState.getInt(BACKUP_NODE_TYPE, -1)
            backupActionType = nonNullState.getInt(BACKUP_ACTION_TYPE, -1)
            backupDialogType = nonNullState.getInt(BACKUP_DIALOG_WARN, -1)
            when (backupDialogType) {
                0 -> {
                    val backupNode = backupNodeHandle?.let { megaApi.getNodeByHandle(it) } ?: return
                    fileBackupManager?.actionBackupNodeCallback?.let {
                        fileBackupManager?.actWithBackupTips(
                            handleList = backupHandleList,
                            pNodeBackup = backupNode,
                            nodeType = backupNodeType,
                            actionType = backupActionType,
                            actionBackupNodeCallback = it,
                        )
                    }
                }
                1 -> {
                    val backupNode = backupNodeHandle?.let { megaApi.getNodeByHandle(it) } ?: return
                    fileBackupManager?.defaultActionBackupNodeCallback?.let {
                        fileBackupManager?.confirmationActionForBackup(
                            handleList = backupHandleList,
                            pNodeBackup = backupNode,
                            nodeType = backupNodeType,
                            actionType = backupActionType,
                            actionBackupNodeCallback = it,
                        )
                    }
                }
                else -> Timber.e("Backup warning dialog is not show")
            }
        }
    }

    /**
     * Establishes the Toolbar
     */
    private fun setupToolbar() {
        (requireActivity() as? ManagerActivity)?.run {
            this.setToolbarTitle()
            this.invalidateOptionsMenu()
        }
    }

    /**
     * Establishes the UI Bindings
     *
     * @param binding [FragmentFileBrowserBinding]
     */
    private fun setupUIBindings(binding: FragmentFileBrowserBinding) {
        recyclerView = binding.fileBrowserRecyclerView
        emptyListImageView = binding.fileBrowserEmptyListImage
        emptyListTextView = binding.fileBrowserEmptyListText
        fastScroller = binding.fileBrowserFastScroller
        transferOverQuotaBanner = binding.fileBrowserOverQuotaBanner.transferOverQuotaBanner
        transferOverQuotaBannerTextView = binding.fileBrowserOverQuotaBanner.bannerContentText
    }

    /**
     * Setup listeners for the Transfer Over Quota Banner
     *
     * @param binding [FragmentFileBrowserBinding]
     */
    private fun setupTransferOverQuotaBannerListeners(binding: FragmentFileBrowserBinding) {
        with(binding.fileBrowserOverQuotaBanner) {
            bannerDismissButton.setOnClickListener { hideTransferOverQuotaBanner() }
            bannerUpgradeButton.setOnClickListener {
                hideTransferOverQuotaBanner()
                (requireActivity() as? ManagerActivity)?.navigateToUpgradeAccount()
            }
        }
    }

    /**
     * Changes the Transfer Over Quota banner visibility based on certain conditions
     */
    fun changeTransferOverQuotaBannerVisibility() {
        if (transfersManagement.isTransferOverQuotaBannerShown) {
            transferOverQuotaBanner?.visibility = View.VISIBLE
            transferOverQuotaBannerTextView?.text =
                getString(
                    R.string.current_text_depleted_transfer_overquota,
                    TimeUtils.getHumanizedTime(
                        megaApi.bandwidthOverquotaDelay
                    )
                )
            TimeUtils.createAndShowCountDownTimer(
                R.string.current_text_depleted_transfer_overquota,
                transferOverQuotaBanner,
                transferOverQuotaBannerTextView
            )
        } else {
            transferOverQuotaBanner?.visibility = View.GONE
        }
    }

    /**
     * Establishes the [FastScroller] for the [NewGridRecyclerView]
     */
    private fun setupFastScroller() = fastScroller?.setRecyclerView(recyclerView)

    /**
     * Establishes the [MegaNodeAdapter]
     */
    private fun setupAdapter() {
        megaNodeAdapter = MegaNodeAdapter(
            requireActivity(),
            this,
            emptyList(),
            fileBrowserViewModel.getSafeBrowserParentHandle(),
            recyclerView,
            Constants.FILE_BROWSER_ADAPTER,
            if (fileBrowserState().currentViewType == ViewType.LIST) MegaNodeAdapter.ITEM_VIEW_TYPE_LIST else MegaNodeAdapter.ITEM_VIEW_TYPE_GRID,
            sortByHeaderViewModel
        ).also {
            it.isMultipleSelect = false
        }
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
            it.adapter = megaNodeAdapter
        }
    }

    /**
     * If user navigates from notification about new nodes added to shared folder select all nodes and scroll to the first node in the list
     */
    private fun selectNewlyAddedNodes() {
        val positions =
            (requireActivity() as ManagerActivity).getPositionsList(fileBrowserState().nodes)
                .takeUnless { it.isEmpty() } ?: return
        activateActionMode()
        positions.forEach {
            if (multipleSelectEnabled) {
                megaNodeAdapter?.toggleSelection(it)
            }
        }
        val selectedNodes = megaNodeAdapter?.selectedNodes
        if (selectedNodes?.isNotEmpty() == true) {
            updateActionModeTitle()
        }
        recyclerView?.scrollToPosition(positions.minOrNull() ?: 0)
    }

    /**
     * When the User taps the "View in Folder" option from the Recents tab, this animates the Node,
     * once the View is loaded
     */
    private fun animateViewInFolderNode() {
        val viewInFolderNode = (requireActivity() as? ManagerActivity)?.viewInFolderNode
        val nodes = fileBrowserState().nodes

        if (viewInFolderNode != null) {
            val nodePosition =
                nodes.indexOfFirst { it.handle == viewInFolderNode.handle }.coerceAtLeast(0)

            // Scroll the position to the 3rd position before of the target position.
            recyclerView?.run {
                this.scrollToPosition(nodePosition - 3)
                this.postDelayed({
                    val holder = this.findViewHolderForAdapterPosition(nodePosition)
                    if (holder != null) {
                        val animFadeIn = AnimationUtils.loadAnimation(
                            requireContext().applicationContext, R.anim.fade_in
                        )
                        animFadeIn.duration = DURATION_ANIMATION.toLong()
                        holder.itemView.startAnimation(animFadeIn)
                    }
                }, DELAY_RECYCLERVIEW_POST.toLong())
            }
        }
    }

    /**
     * This method checks scroll of recycler view
     */
    fun checkScroll() {
        recyclerView?.let {
            val visible =
                ((multipleSelectEnabled || transfersManagement.isTransferOverQuotaBannerShown) || it.canScrollVertically(
                    -1
                ) && it.visibility == View.VISIBLE)
            (requireActivity() as? ManagerActivity)?.changeAppBarElevation(visible)
        }
    }

    /**
     * Establishes the Observers
     */
    private fun setupObservers() {
        observeDragSupportEvents(
            lifecycleOwner = viewLifecycleOwner,
            rv = recyclerView,
            viewerFrom = Constants.VIEWER_FROM_FILE_BROWSER,
        )

        viewLifecycleOwner.collectFlow(sortByHeaderViewModel.state) { state ->
            handleViewType(state.viewType)
        }

        fileBrowserViewModel.state.flowWithLifecycle(
            viewLifecycleOwner.lifecycle,
            Lifecycle.State.RESUMED
        ).onEach {
            mediaDiscoveryViewSettings = it.mediaDiscoveryViewSettings
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                fileBrowserViewModel.state.collect {
                    hideMultipleSelect()
                    setNodes(it.nodes.toMutableList())
                    changeFastScrollerVisibility()
                    updateUI(nodeHandle = fileBrowserViewModel.getSafeBrowserParentHandle())
                    recyclerView?.invalidate()
                }
            }
        }
        sortByHeaderViewModel.showDialogEvent.observe(viewLifecycleOwner,
            EventObserver { showSortByPanel() }
        )
        sortByHeaderViewModel.orderChangeEvent.observe(viewLifecycleOwner, EventObserver {
            fileBrowserViewModel.refreshNodes()
            hideMultipleSelect()
        })

        LiveEventBus.get(EVENT_SHOW_MEDIA_DISCOVERY, Unit::class.java)
            .observe(this) { showMediaDiscovery(true) }
    }

    /**
     * Updates the View Type of this Fragment
     *
     * Changing the View Type will cause the scroll position to be lost. To avoid that, only
     * refresh the contents when the new View Type is different from the original View Type
     *
     * @param viewType The new View Type received from [SortByHeaderViewModel]
     */
    private fun handleViewType(viewType: ViewType) {
        if (viewType != fileBrowserState().currentViewType) {
            fileBrowserViewModel.setCurrentViewType(viewType)
            switchViewType()
        }
    }

    /**
     * Switches how items in the [MegaNodeAdapter] are being displayed, based on the current
     * [ViewType] in [FileBrowserViewModel]
     */
    private fun switchViewType() {
        recyclerView?.run {
            when (fileBrowserState().currentViewType) {
                ViewType.LIST -> {
                    switchToLinear()
                    if (itemDecorationCount == 0) addItemDecoration(itemDecoration)
                    megaNodeAdapter?.adapterType = MegaNodeAdapter.ITEM_VIEW_TYPE_LIST
                }
                ViewType.GRID -> {
                    switchBackToGrid()
                    removeItemDecoration(itemDecoration)
                    (layoutManager as CustomizedGridLayoutManager).apply {
                        spanSizeLookup = megaNodeAdapter?.getSpanSizeLookup(spanCount)
                    }
                    megaNodeAdapter?.adapterType = MegaNodeAdapter.ITEM_VIEW_TYPE_GRID
                }
            }
        }
    }

    /**
     * Shows the Sort by panel
     */
    private fun showSortByPanel() =
        (requireActivity() as? ManagerActivity)?.showNewSortByPanel(Constants.ORDER_CLOUD)

    /**
     * Opens file
     * @param node MegaNode
     * @param position position of item clicked
     */
    private fun openFile(node: MegaNode, position: Int) {
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
                megaNodeAdapter
            )
            startActivity(intent)
            (requireActivity() as? ManagerActivity)?.overridePendingTransition(0, 0)
        } else if (MimeTypeList.typeForName(node.name).isVideoMimeType || MimeTypeList.typeForName(
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
                mediaIntent = Util.getMediaIntent(requireContext(), node.name)
                internalIntent = true
            }
            mediaIntent.putExtra("position", position)
            mediaIntent.putExtra("placeholder", megaNodeAdapter?.placeholderCount)
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
                launchIntent = mediaIntent,
                rv = recyclerView,
                position = position,
                viewerFrom = Constants.VIEWER_FROM_FILE_BROWSER,
                thumbnailGetter = megaNodeAdapter,
            )
            mediaIntent.putExtra("FILENAME", node.name)
            val localPath = FileUtil.getLocalFile(node)
            if (localPath != null) {
                val mediaFile = File(localPath)
                if (localPath.contains(Environment.getExternalStorageDirectory().path)) {
                    Timber.d("itemClick:FileProviderOption")
                    val mediaFileUri = FileProvider.getUriForFile(
                        requireContext(),
                        FILE_PROVIDER_AUTHORITY,
                        mediaFile
                    )
                    if (mediaFileUri == null) {
                        Timber.d("itemClick:ERROR:NULLmediaFileUri")
                        (requireActivity() as? ManagerActivity)?.showSnackbar(
                            type = Constants.SNACKBAR_TYPE,
                            content = getString(R.string.general_text_error),
                            chatId = -1,
                        )
                    } else {
                        mediaIntent.setDataAndType(
                            mediaFileUri,
                            MimeTypeList.typeForName(node.name).type
                        )
                    }
                } else {
                    val mediaFileUri = Uri.fromFile(mediaFile)
                    if (mediaFileUri == null) {
                        Timber.e("itemClick:ERROR:NULLmediaFileUri")
                        (requireActivity() as? ManagerActivity)?.showSnackbar(
                            type = Constants.SNACKBAR_TYPE,
                            content = getString(R.string.general_text_error),
                            chatId = -1,
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
                    requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
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
                        (requireActivity() as? ManagerActivity)?.showSnackbar(
                            type = Constants.SNACKBAR_TYPE,
                            content = getString(R.string.general_text_error),
                            chatId = -1,
                        )
                    }
                } else {
                    Timber.e("itemClick:ERROR:httpServerGetLocalLink")
                    (requireActivity() as? ManagerActivity)?.showSnackbar(
                        type = Constants.SNACKBAR_TYPE,
                        content = getString(R.string.general_text_error),
                        chatId = -1,
                    )
                }
            }
            mediaIntent.putExtra("HANDLE", node.handle)
            if (opusFile) {
                mediaIntent.setDataAndType(mediaIntent.data, "audio/*")
            }
            if (internalIntent) {
                startActivity(mediaIntent)
            } else {
                Timber.d("itemClick:externalIntent")
                if (MegaApiUtils.isIntentAvailable(requireContext(), mediaIntent)) {
                    startActivity(mediaIntent)
                } else {
                    Timber.w("itemClick:noAvailableIntent")
                    (requireActivity() as? ManagerActivity)?.let {
                        it.showSnackbar(
                            type = Constants.SNACKBAR_TYPE,
                            content = getString(R.string.intent_not_available),
                            chatId = -1,
                        )
                        it.saveNodesToDevice(
                            nodes = listOf<MegaNode?>(node),
                            highPriority = true,
                            isFolderLink = false,
                            fromMediaViewer = false,
                            fromChat = false,
                        )
                    }
                }
            }
            (requireActivity() as? ManagerActivity)?.overridePendingTransition(0, 0)
        } else if (MimeTypeList.typeForName(node.name).isURL) {
            manageURLNode(requireContext(), megaApi, node)
        } else if (MimeTypeList.typeForName(node.name).isPdf) {
            Timber.d("itemClick:isFile:isPdf")
            val mimeType = MimeTypeList.typeForName(node.name).type
            val pdfIntent = Intent(requireContext(), PdfViewerActivity::class.java)
            pdfIntent.putExtra("inside", true)
            pdfIntent.putExtra("adapterType", Constants.FILE_BROWSER_ADAPTER)
            val localPath = FileUtil.getLocalFile(node)
            if (localPath != null) {
                val mediaFile = File(localPath)
                if (localPath.contains(Environment.getExternalStorageDirectory().path)) {
                    pdfIntent.setDataAndType(
                        FileProvider.getUriForFile(
                            requireContext(),
                            FILE_PROVIDER_AUTHORITY,
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
                        (requireActivity() as? ManagerActivity)?.showSnackbar(
                            type = Constants.SNACKBAR_TYPE,
                            content = getString(R.string.general_text_error),
                            chatId = -1,
                        )
                    }
                } else {
                    Timber.e("itemClick:ERROR:httpServerGetLocalLink")
                    (requireActivity() as? ManagerActivity)?.showSnackbar(
                        type = Constants.SNACKBAR_TYPE,
                        content = getString(R.string.general_text_error),
                        chatId = -1,
                    )
                }
            }
            pdfIntent.putExtra("HANDLE", node.handle)
            putThumbnailLocation(
                pdfIntent,
                recyclerView,
                position,
                Constants.VIEWER_FROM_FILE_BROWSER,
                megaNodeAdapter
            )
            if (MegaApiUtils.isIntentAvailable(requireContext(), pdfIntent)) {
                startActivity(pdfIntent)
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.intent_not_available),
                    Toast.LENGTH_LONG
                ).show()
                (requireActivity() as? ManagerActivity)?.saveNodesToDevice(
                    nodes = listOf<MegaNode?>(node),
                    highPriority = true,
                    isFolderLink = false,
                    fromMediaViewer = false,
                    fromChat = false,
                )
            }
            (requireActivity() as? ManagerActivity)?.overridePendingTransition(0, 0)
        } else if (MimeTypeList.typeForName(node.name).isOpenableTextFile(
                node.size
            )
        ) {
            manageTextFileIntent(requireContext(), node, Constants.FILE_BROWSER_ADAPTER)
        } else {
            Timber.d("itemClick:isFile:otherOption")

            val managerActivity = requireActivity() as? ManagerActivity ?: return
            onNodeTapped(
                context = requireActivity(),
                node = node,
                nodeDownloader = { saveNode: MegaNode ->
                    (requireActivity() as? ManagerActivity)?.saveNodeByTap(
                        saveNode
                    )
                },
                activityLauncher = managerActivity,
                snackbarShower = managerActivity,
            )
        }
    }

    /**
     * When an item clicked from adapter it calls below method
     * @param position Position of item which is clicked
     */
    fun itemClick(position: Int) {
        Timber.d("Position:$position")
        if (multipleSelectEnabled) {
            Timber.d("Multiselect ON")
            megaNodeAdapter?.toggleSelection(position)

            val selectedNodes = megaNodeAdapter?.selectedNodes
            if (selectedNodes.isNullOrEmpty().not()) {
                updateActionModeTitle()
            }
        } else {
            megaNodeAdapter?.getItem(position)?.let { node ->
                if (node.isFolder) {
                    fileBrowserViewModel.setBrowserParentHandle(node.handle)
                    viewLifecycleOwner.lifecycleScope.launch {
                        if (fileBrowserViewModel.shouldEnterMediaDiscoveryMode(
                                parentHandle = node.handle,
                                mediaDiscoveryViewSettings = mediaDiscoveryViewSettings,
                            )
                        ) {
                            showMediaDiscovery()
                        } else {
                            val lastFirstVisiblePosition =
                                if (fileBrowserState().currentViewType == ViewType.LIST) {
                                    recyclerView?.findFirstCompletelyVisibleItemPosition()
                                        ?: RecyclerView.NO_POSITION
                                } else {
                                    val pos =
                                        recyclerView?.findFirstCompletelyVisibleItemPosition()
                                            ?: RecyclerView.NO_POSITION
                                    if (pos == RecyclerView.NO_POSITION) {
                                        Timber.w("Completely -1 then find just visible position")
                                        recyclerView?.findFirstVisibleItemPosition()
                                            ?: RecyclerView.NO_POSITION
                                    }
                                    pos
                                }
                            Timber.d("Push to stack $lastFirstVisiblePosition position")
                            fileBrowserViewModel.onFolderItemClicked(
                                lastFirstVisiblePosition = lastFirstVisiblePosition,
                                handle = node.handle,
                            )
                            setFolderInfoNavigation(node)
                        }
                    }
                } else {
                    openFile(node = node, position = position)
                }
            }
        }
    }

    /**
     * Opens the Folder Node
     *
     * @param node The [MegaNode]
     */
    fun setFolderInfoNavigation(node: MegaNode?) {
        setupToolbar()
        megaNodeAdapter?.parentHandle = fileBrowserState().fileBrowserHandle
        // If folder has no files
        updateUI(nodeHandle = node?.handle)
    }

    /**
     * Select all items from adapter
     */
    fun selectAll() {
        Timber.d("selectAll")
        megaNodeAdapter?.let {
            if (multipleSelectEnabled) {
                it.selectAll()
            } else {
                it.isMultipleSelect = true
                it.selectAll()
                actionMode = (requireActivity() as? AppCompatActivity)?.startSupportActionMode(
                    ActionBarCallBack()
                )
            }
            Handler(Looper.getMainLooper()).post { updateActionModeTitle() }
        }
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
        if (multipleSelectEnabled) megaNodeAdapter?.clearSelections()
    }

    /**
     * Hides multi select option
     */
    fun hideMultipleSelect() {
        Timber.d("hideMultipleSelect")
        megaNodeAdapter?.isMultipleSelect = false
        actionMode?.finish()
    }

    /**
     * Navigates to the first element in the [NewGridRecyclerView]
     */
    fun scrollToFirstPosition() = recyclerView?.scrollToPosition(0)

    /**
     * This method set nodes and updates the adapter
     *
     * @param nodes List of Mega Nodes
     */
    private fun setNodes(nodes: MutableList<MegaNode>) {
        Timber.d("Nodes size: ${nodes.size}")
        val megaNodes = nodes.toMutableList()
        megaNodeAdapter?.setNodes(megaNodes)
    }

    /**
     * Changes the [FastScroller] visibility based on the Item Count
     */
    private fun changeFastScrollerVisibility() {
        fastScroller?.visibility = megaNodeAdapter?.let {
            if (itemCount < Constants.MIN_ITEMS_SCROLLBAR) View.GONE else View.VISIBLE
        } ?: run {
            View.GONE
        }
    }

    /**
     * Updates several UI Elements
     *
     * @param nodeHandle the Node Handle
     */
    private fun updateUI(nodeHandle: Long?) {
        changeRecyclerViewVisibility()
        changeInformationDisplay(nodeHandle = nodeHandle)
        checkScroll()
    }

    /**
     * Changes the display of the [NewGridRecyclerView] and several UI elements based on
     * whether there are items in the [MegaNodeAdapter] or not
     */
    private fun changeRecyclerViewVisibility() {
        Timber.d("The Adapter Item Count is $itemCount")
        if (itemCount == 0) {
            recyclerView?.visibility = View.GONE
            emptyListImageView?.visibility = View.VISIBLE
            emptyListTextView?.visibility = View.VISIBLE
        } else {
            recyclerView?.visibility = View.VISIBLE
            emptyListImageView?.visibility = View.GONE
            emptyListTextView?.visibility = View.GONE
        }
    }

    /**
     * Changes the information displayed based on whether there are items in the [MegaNodeAdapter] or not
     *
     * @param nodeHandle The Node Handle
     */
    private fun changeInformationDisplay(nodeHandle: Long?) {
        if (itemCount == 0) {
            if (megaApi.rootNode != null && megaApi.rootNode.handle == nodeHandle) {
                emptyListImageView?.setImageResource(R.drawable.empty_cloud_drive_portrait)
                runCatching {
                    emptyListTextView?.text = Html.fromHtml(
                        formatRequiredText(
                            text = getString(R.string.context_empty_cloud_drive).uppercase(
                                Locale.getDefault()
                            ),
                            colorResPrimary = R.color.grey_900_grey_100,
                            colorResSecondary = R.color.grey_300_grey_600,
                        ), Html.FROM_HTML_MODE_LEGACY
                    )
                }.getOrElse {
                    Timber.e(it)
                }
            } else {
                emptyListImageView?.setImageResource(R.drawable.empty_folder_portrait)

                runCatching {
                    emptyListTextView?.text = Html.fromHtml(
                        formatRequiredText(
                            text = getString(R.string.file_browser_empty_folder_new).uppercase(
                                Locale.getDefault()
                            ),
                            colorResPrimary = R.color.grey_900_grey_100,
                            colorResSecondary = R.color.grey_300_grey_600,
                        ), Html.FROM_HTML_MODE_LEGACY
                    )
                }.getOrElse {
                    Timber.e(it)
                }
            }
        }
    }

    /**
     * Hides the "Transfer Over Quota" banner
     */
    private fun hideTransferOverQuotaBanner() {
        transfersManagement.isTransferOverQuotaBannerShown = false
        changeTransferOverQuotaBannerVisibility()
    }

    /**
     * Show Media discovery and launch [MediaDiscoveryFragment]
     */
    private fun showMediaDiscovery(isOpenByMDIcon: Boolean = false) {
        requireActivity().lifecycleScope.launch {
            (requireActivity() as? ManagerActivity)?.skipToMediaDiscoveryFragment(
                fragment = MediaDiscoveryFragment.getNewInstance(
                    mediaHandle = fileBrowserState().mediaHandle,
                    isOpenByMDIcon = isOpenByMDIcon,
                ),
                mediaHandle = fileBrowserState().mediaHandle,
            )
        }
    }

    private inner class ActionBarCallBack : ActionMode.Callback {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            Timber.d("onActionItemClicked")
            val documents = megaNodeAdapter?.selectedNodes
            val handleList = ArrayList<Long>()
            when (item.itemId) {
                R.id.cab_menu_download -> {
                    (requireActivity() as? ManagerActivity)?.saveNodesToDevice(
                        nodes = documents,
                        highPriority = false,
                        isFolderLink = false,
                        fromMediaViewer = false,
                        fromChat = false,
                    )
                    clearSelections()
                    hideMultipleSelect()
                }
                R.id.cab_menu_rename -> {
                    if (documents?.size == 1) {
                        (requireActivity() as? ManagerActivity)?.showRenameDialog(documents[0])
                    }
                    clearSelections()
                    hideMultipleSelect()
                }
                R.id.cab_menu_copy -> {
                    documents?.map { it.handle }?.let {
                        handleList.addAll(it)
                        val nC = NodeController(requireActivity())
                        nC.chooseLocationToCopyNodes(handleList)
                    }
                    clearSelections()
                    hideMultipleSelect()
                }
                R.id.cab_menu_move -> {
                    val nC = NodeController(requireActivity())
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
                            val nC = NodeController(requireActivity())
                            fileBackupManager?.let { backupManager ->
                                if (!backupManager.shareBackupFolderInMenu(
                                        nC = nC,
                                        handleList = handleList,
                                        actionBackupNodeCallback = backupManager.actionBackupNodeCallback,
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
                        (requireActivity() as? ManagerActivity)?.showGetLinkActivity(documents)
                    } else {
                        Timber.w("The selected node is NULL")
                    }
                    clearSelections()
                    hideMultipleSelect()
                }
                R.id.cab_menu_remove_link -> {
                    Timber.d("Remove public link option")
                    if (documents?.isNotEmpty() == true) {
                        (requireActivity() as? ManagerActivity)?.showConfirmationRemovePublicLink(
                            documents[0]
                        )
                    } else {
                        Timber.w("The selected node is NULL")
                    }
                    clearSelections()
                    hideMultipleSelect()
                }
                R.id.cab_menu_send_to_chat -> {
                    Timber.d("Send files to chat")
                    (requireActivity() as? ManagerActivity)?.attachNodesToChats(megaNodeAdapter?.arrayListSelectedNodes)
                    clearSelections()
                    hideMultipleSelect()
                }
                R.id.cab_menu_trash -> {
                    documents?.map { it.handle }?.let {
                        handleList.addAll(it)
                        (requireActivity() as? ManagerActivity)?.askConfirmationMoveToRubbish(
                            handleList
                        )
                    }
                }
                R.id.cab_menu_select_all -> {
                    selectAll()
                }
                R.id.cab_menu_clear_selection -> {
                    clearSelections()
                    hideMultipleSelect()
                }
                R.id.cab_menu_remove_share -> documents?.let {
                    (requireActivity() as? ManagerActivity)?.showConfirmationRemoveAllSharingContacts(
                        it
                    )
                }
                R.id.cab_menu_dispute -> {
                    startActivity(
                        Intent(requireContext(), WebViewActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            .setData(Uri.parse(Constants.DISPUTE_URL))
                    )
                }
            }
            return true
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            Timber.d("onCreateActionMode")
            val inflater = mode.menuInflater
            inflater.inflate(R.menu.cloud_storage_action, menu)
            (requireActivity() as? ManagerActivity)?.let {
                it.hideFabButton()
                it.showHideBottomNavigationView(true)
            }
            checkScroll()
            return true
        }

        override fun onDestroyActionMode(arg0: ActionMode) {
            Timber.d("onDestroyActionMode")
            clearSelections()
            megaNodeAdapter?.isMultipleSelect = false
            (requireActivity() as? ManagerActivity)?.let {
                it.showFabButton()
                it.showHideBottomNavigationView(false)
            }
            checkScroll()
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            Timber.d("onPrepareActionMode")
            val selected =
                megaNodeAdapter?.selectedNodes?.takeUnless { it.isEmpty() } ?: return false
            menu.findItem(R.id.cab_menu_share_link).title =
                resources.getQuantityString(R.plurals.get_links, selected.size)
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
            var showDispute = false
            for (node in selected) {
                if (node?.isTakenDown == true) {
                    showDispute = true
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
            if (showDispute) {
                control.trash().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                control.move().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                if (selected.size == 1) {
                    control.disputeTakedown().isVisible = true
                    control.disputeTakedown().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                    control.rename().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                }
            }
            if (!showDownload) {
                control.saveToDevice().isVisible = false
            }
            megaNodeAdapter?.let {
                control.selectAll().isVisible = (selected.size
                        < itemCount - it.placeholderCount)
            }
            CloudStorageOptionControlUtil.applyControl(menu, control)
            return true
        }
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
        private const val FILE_PROVIDER_AUTHORITY =
            "mega.privacy.android.app.providers.fileprovider"
    }
}
