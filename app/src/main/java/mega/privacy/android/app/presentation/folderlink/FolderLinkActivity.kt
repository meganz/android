package mega.privacy.android.app.presentation.folderlink

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ActionMode
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.MegaApplication.Companion.isClosedChat
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.observeDragSupportEvents
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.putThumbnailLocation
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.constants.EventConstants.EVENT_UPDATE_VIEW_MODE
import mega.privacy.android.app.databinding.ActivityFolderLinkBinding
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.fragments.settingsFragments.cookie.CookieDialogHandler
import mega.privacy.android.app.imageviewer.ImageViewerActivity.Companion.getIntentForChildren
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.DecryptAlertDialog
import mega.privacy.android.app.main.DecryptAlertDialog.DecryptDialogListener
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.PdfViewerActivity
import mega.privacy.android.app.main.adapters.MegaNodeAdapter
import mega.privacy.android.app.modalbottomsheet.FolderLinkBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.presentation.extensions.getFormattedStringOrDefault
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.transfers.TransfersManagementActivity
import mega.privacy.android.app.usecase.CopyNodeUseCase
import mega.privacy.android.app.usecase.data.CopyRequestResult
import mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists
import mega.privacy.android.app.utils.AlertsAndWarnings.showSaveToDeviceConfirmDialog
import mega.privacy.android.app.utils.ColorUtils.changeStatusBarColorForElevation
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaNodeUtil.manageTextFileIntent
import mega.privacy.android.app.utils.MegaNodeUtil.shareLink
import mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog
import mega.privacy.android.app.utils.PreviewUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils.checkNotificationsPermission
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import mega.privacy.android.data.mapper.sortOrderToInt
import mega.privacy.android.data.model.MegaPreferences
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.preference.ViewType
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber
import java.io.File
import java.util.Objects
import java.util.Stack
import javax.inject.Inject

/**
 * FolderLinkActivity
 */
@AndroidEntryPoint
class FolderLinkActivity : TransfersManagementActivity(), MegaRequestListenerInterface,
    DecryptDialogListener, SnackbarShower {

    private lateinit var binding: ActivityFolderLinkBinding

    /**
     * CheckNameCollisionUseCase
     */
    @Inject
    lateinit var checkNameCollisionUseCase: CheckNameCollisionUseCase

    /**
     * CheckNameCollisionUseCase
     */
    @Inject
    lateinit var copyNodeUseCase: CopyNodeUseCase

    /**
     * CookieDialogHandler
     */
    @Inject
    lateinit var cookieDialogHandler: CookieDialogHandler

    /**
     * Selected node
     */
    var selectedNode: MegaNode? = null
    private var folderLinkActivity: FolderLinkActivity? = this
    private var url: String? = null
    private var folderHandle: String? = null
    private var folderKey: String? = null
    private var folderSubHandle: String? = null
    private var mLayoutManager: LinearLayoutManager? = null
    private var gridLayoutManager: CustomizedGridLayoutManager? = null
    private var parentHandle: Long = -1
    private var nodes = arrayListOf<MegaNode>()
    private var adapterList: MegaNodeAdapter? = null
    private var lastPositionStack: Stack<Int>? = null
    private var toHandle: Long = 0
    private var fragmentHandle: Long = -1
    private var statusDialog: AlertDialog? = null
    private val orderGetChildren = SortOrder.ORDER_DEFAULT_ASC
    private var prefs: MegaPreferences? = null
    private var actionMode: ActionMode? = null
    private var pN: MegaNode? = null
    private var fileLinkFolderLink = false
    private var handleListM = ArrayList<Long>()
    private var bottomSheetDialogFragment: FolderLinkBottomSheetDialogFragment? = null
    private var mKey: String? = null
    private val nodeSaver = NodeSaver(
        this, this, this,
        showSaveToDeviceConfirmDialog(this)
    )

    private val viewModel: FolderLinkViewModel by viewModels()
    private val sortByHeaderViewModel: SortByHeaderViewModel by viewModels()

    private lateinit var selectImportFolderLauncher: ActivityResultLauncher<Intent>

    private val recyclerView: RecyclerView
        get() = if (viewModel.isList) binding.folderLinkListViewBrowser else binding.folderLinkGridViewBrowser

    private val downloadButtonClickListener = View.OnClickListener {
        adapterList?.let {
            if (it.isMultipleSelect) {
                downloadNodes(it.selectedNodes)
                clearSelections()
            } else {
                var rootNode: MegaNode? = null
                if (megaApiFolder.rootNode != null) {
                    rootNode = megaApiFolder.rootNode
                }
                if (rootNode != null) {
                    val parentNode = megaApiFolder.getNodeByHandle(parentHandle)
                    if (parentNode != null) {
                        downloadNodes(listOf(parentNode))
                    } else {
                        downloadNodes(listOf(rootNode))
                    }
                } else {
                    Timber.w("rootNode null!!")
                }
            }
        } ?: return@OnClickListener
    }

    private val importButtonClickListener = View.OnClickListener {
        if (megaApiFolder.rootNode != null) {
            if (fileLinkFolderLink) {
                if (pN != null) {
                    selectedNode = pN
                }
            } else {
                selectedNode = megaApiFolder.rootNode
            }
            importNode()
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Timber.d("onBackPressed")
            retryConnectionsAndSignalPresence()
            if (fileLinkFolderLink) {
                binding.folderLinkFileLinkFragmentContainer.visibility = View.GONE
                binding.folderLinkFragmentContainer.visibility = View.VISIBLE

                setSupportActionBar(binding.toolbarFolderLink)
                supportActionBar?.apply {
                    setDisplayHomeAsUpEnabled(true)
                    setDisplayShowHomeEnabled(true)
                }

                fileLinkFolderLink = false
                pN = null
                val parentNode =
                    megaApiFolder.getParentNode(megaApiFolder.getNodeByHandle(parentHandle))
                if (parentNode != null) {
                    Timber.d("parentNode != NULL")
                    recyclerView.visibility = View.VISIBLE
                    binding.folderLinkListEmptyImage.visibility = View.GONE
                    binding.folderLinkListEmptyText.visibility = View.GONE
                    supportActionBar?.title = parentNode.name
                    invalidateOptionsMenu()
                    parentHandle = parentNode.handle
                    nodes = megaApiFolder.getChildren(parentNode, sortOrderToInt(orderGetChildren))
                    adapterList?.setNodes(ArrayList(nodes))
                    var lastVisiblePosition = 0

                    lastPositionStack?.let {
                        if (it.isNotEmpty()) {
                            lastVisiblePosition = it.pop()
                            Timber.d("Pop of the stack $lastVisiblePosition position")
                        }
                    }
                    Timber.d("Scroll to $lastVisiblePosition position")

                    if (lastVisiblePosition >= 0) {
                        if (viewModel.isList) {
                            mLayoutManager?.scrollToPositionWithOffset(lastVisiblePosition, 0)
                        } else {
                            gridLayoutManager?.scrollToPositionWithOffset(lastVisiblePosition, 0)
                        }
                    }
                    adapterList?.parentHandle = parentHandle
                    return
                } else {
                    Timber.w("parentNode == NULL")
                    finish()
                }
            }

            adapterList?.let {
                Timber.d("adapter !=null")
                parentHandle = it.parentHandle
                val parentNode =
                    megaApiFolder.getParentNode(megaApiFolder.getNodeByHandle(parentHandle))

                if (parentNode != null) {
                    Timber.d("parentNode != NULL")
                    recyclerView.visibility = View.VISIBLE
                    binding.folderLinkListEmptyImage.visibility = View.GONE
                    binding.folderLinkListEmptyText.visibility = View.GONE
                    supportActionBar?.title = parentNode.name
                    invalidateOptionsMenu()
                    parentHandle = parentNode.handle
                    nodes = megaApiFolder.getChildren(parentNode, sortOrderToInt(orderGetChildren))
                    it.setNodes(ArrayList(nodes))
                    var lastVisiblePosition = 0

                    lastPositionStack?.let { stack ->
                        if (stack.isNotEmpty()) {
                            lastVisiblePosition = stack.pop()
                            Timber.d("Pop of the stack $lastVisiblePosition position")
                        }
                    }
                    Timber.d("Scroll to $lastVisiblePosition position")

                    if (lastVisiblePosition >= 0) {
                        if (viewModel.isList) {
                            mLayoutManager?.scrollToPositionWithOffset(lastVisiblePosition, 0)
                        } else {
                            gridLayoutManager?.scrollToPositionWithOffset(lastVisiblePosition, 0)
                        }
                    }
                    it.parentHandle = parentHandle
                    return
                } else {
                    Timber.w("parentNode == NULL")
                    finish()
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    private val selectImportFolderResult =
        ActivityResultCallback<ActivityResult> { activityResult ->
            val resultCode = activityResult.resultCode
            val intent = activityResult.data

            if (resultCode != Activity.RESULT_OK || intent == null) {
                return@ActivityResultCallback
            }

            if (!viewModel.isConnected) {
                try {
                    statusDialog?.dismiss()
                } catch (exception: Exception) {
                    Timber.e(exception)
                }

                showSnackbar(R.string.error_server_connection_problem)
                return@ActivityResultCallback
            }

            toHandle = intent.getLongExtra("IMPORT_TO", 0)
            fragmentHandle = intent.getLongExtra("fragmentH", -1)
            statusDialog =
                createProgressDialog(
                    this,
                    getFormattedStringOrDefault(R.string.general_importing)
                )
            statusDialog?.show()

            if (adapterList?.isMultipleSelect == true) {
                Timber.d("Is multiple select")
                val nodes = adapterList?.selectedNodes ?: listOf()
                if (nodes.isEmpty()) {
                    Timber.w("No selected nodes")
                    showSnackbar(R.string.context_no_copied)
                    return@ActivityResultCallback
                }

                checkNameCollisionUseCase.checkNodeList(nodes, toHandle, NameCollisionType.COPY)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { result: Pair<ArrayList<NameCollision>, List<MegaNode>>, throwable: Throwable? ->
                        if (throwable == null) {
                            val collisions: ArrayList<NameCollision> = result.first
                            if (collisions.isNotEmpty()) {
                                dismissAlertDialogIfExists(statusDialog)
                                nameCollisionActivityContract?.launch(collisions)
                            }
                            val nodesWithoutCollisions: List<MegaNode> = result.second
                            if (nodesWithoutCollisions.isNotEmpty()) {
                                copyNodeUseCase.copy(nodesWithoutCollisions, toHandle)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe { copyRequestResult: CopyRequestResult?, copyThrowable: Throwable? ->
                                        showCopyResult(copyRequestResult, copyThrowable)
                                    }
                            }
                        }
                    }

            } else if (selectedNode != null) {
                Timber.d("No multiple select")
                selectedNode = megaApiFolder.authorizeNode(selectedNode)
                checkNameCollisionUseCase.check(selectedNode, toHandle, NameCollisionType.COPY)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onSuccess = { collisionResult ->
                            dismissAlertDialogIfExists(statusDialog)
                            nameCollisionActivityContract?.launch(arrayListOf(collisionResult))
                        },
                        onError = { error ->
                            Timber.e(error, "No collision.")
                            copyNodeUseCase.copy(selectedNode, toHandle)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(
                                    { showCopyResult(null, null) },
                                    { copyThrowable: Throwable? ->
                                        showCopyResult(null, copyThrowable)
                                    })
                        }
                    )

            } else {
                Timber.w("Selected Node is NULL")
                showSnackbar(R.string.context_no_copied)
            }
        }

    /**
     * Start action mode
     */
    fun activateActionMode() {
        Timber.d("activateActionMode")
        if (adapterList?.isMultipleSelect == false) {
            adapterList?.isMultipleSelect = true
            actionMode = startSupportActionMode(ActionBarCallBack())
        }
    }

    private fun decrypt() {
        if (TextUtils.isEmpty(mKey)) return
        var urlWithKey = ""
        if (url?.contains("#F!") == true) {
            // old folder link format
            urlWithKey = if (mKey?.startsWith("!") == true) {
                Timber.d("Decryption key with exclamation!")
                "$url$mKey"
            } else {
                "$url!$mKey"
            }
        } else if (url?.contains("${Constants.SEPARATOR}folder${Constants.SEPARATOR}") == true) {
            // new folder link format
            urlWithKey = if (mKey?.startsWith("#") == true) {
                Timber.d("Decryption key with hash!")
                "$url$mKey"
            } else {
                "$url#$mKey"
            }
        }
        Timber.d("Folder link to import: $urlWithKey")
        viewModel.folderLogin(urlWithKey, true)
    }

    override fun onDialogPositiveClick(key: String?) {
        mKey = key
        decrypt()
    }

    override fun onDialogNegativeClick() {
        finish()
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        showSnackbar(type, binding.folderLinkFragmentContainer, content, chatId)
    }

    private inner class ActionBarCallBack : ActionMode.Callback {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.cab_menu_download -> {
                    adapterList?.let {
                        downloadNodes(it.selectedNodes)
                        clearSelections()
                    }
                }
                R.id.cab_menu_select_all -> {
                    selectAll()
                }
                R.id.cab_menu_unselect_all -> {
                    clearSelections()
                    hideMultipleSelect()
                }
            }
            return false
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            val inflater = mode.menuInflater
            inflater.inflate(R.menu.folder_link_action, menu)
            changeStatusBarColorForElevation(this@FolderLinkActivity, true)
            // No App bar in this activity, control tool bar instead.
            binding.toolbarFolderLink.elevation = resources.getDimension(R.dimen.toolbar_elevation)
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            clearSelections()
            adapterList?.isMultipleSelect = false
            binding.optionsFolderLinkLayout.visibility = View.VISIBLE
            binding.separator3.visibility = View.VISIBLE

            // No App bar in this activity, control tool bar instead.
            val withElevation: Boolean = recyclerView.canScrollVertically(-1)
            changeStatusBarColorForElevation(this@FolderLinkActivity, withElevation)
            if (!withElevation) {
                binding.toolbarFolderLink.elevation = 0f
            }
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            val selected = adapterList?.selectedNodes ?: listOf()
            var showDownload = false
            if (selected.isNotEmpty()) {
                showDownload = true
                if (selected.size == adapterList?.itemCount) {
                    menu.findItem(R.id.cab_menu_select_all).isVisible = false
                    menu.findItem(R.id.cab_menu_unselect_all).isVisible = true
                } else {
                    menu.findItem(R.id.cab_menu_select_all).isVisible = true
                    menu.findItem(R.id.cab_menu_unselect_all).isVisible = true
                }
            } else {
                menu.findItem(R.id.cab_menu_select_all).isVisible = true
                menu.findItem(R.id.cab_menu_unselect_all).isVisible = false
            }
            menu.findItem(R.id.cab_menu_download).isVisible = showDownload
            return false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.file_folder_link_action, menu)
        menu.findItem(R.id.action_more).isVisible = true
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("onOptionsItemSelected")
        when (item.itemId) {
            android.R.id.home -> onBackPressedDispatcher.onBackPressed()
            R.id.share_link -> shareLink(this, url)
            R.id.action_more -> showOptionsPanel(megaApiFolder.getNodeByHandle(parentHandle))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate()")
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        binding = ActivityFolderLinkBinding.inflate(layoutInflater)

        selectImportFolderLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            selectImportFolderResult
        )

        val intentReceived = intent
        if (intentReceived != null) {
            url = intentReceived.dataString
        }
        if (dbH.credentials != null && (megaApi.rootNode == null)) {
            Timber.d("Refresh session - sdk or karere")
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
            intent.data = Uri.parse(url)
            intent.action = Constants.ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
            return
        }
        if (savedInstanceState != null) {
            nodeSaver.restoreState(savedInstanceState)
        }
        folderLinkActivity = this
        prefs = dbH.preferences

        if (prefs?.preferredViewList == null) {
            viewModel.isList = true
        } else {
            viewModel.isList = prefs?.preferredViewList.toBoolean()
        }

        lastPositionStack = Stack()
        setContentView(binding.root)
        setupView()

        val intent = intent
        if (intent != null) {
            if ((intent.action == Constants.ACTION_OPEN_MEGA_FOLDER_LINK)) {
                if (parentHandle == -1L) {
                    url = intent.dataString
                    url?.let { url ->
                        Timber.d("URL: $url")
                        val s =
                            url.split("!".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        Timber.d("URL parts: ${s.size}")
                        for (i in s.indices) {
                            when (i) {
                                1 -> {
                                    folderHandle = s[1]
                                    Timber.d("URL_handle: $folderHandle")
                                }
                                2 -> {
                                    folderKey = s[2]
                                    Timber.d("URL_key: $folderKey")
                                }
                                3 -> {
                                    folderSubHandle = s[3]
                                    Timber.d("URL_subhandle: $folderSubHandle")
                                }
                            }
                        }
                        viewModel.folderLogin(url)
                    } ?: Timber.w("url NULL")
                }
            }
        }

        binding.folderLinkFragmentContainer.post { cookieDialogHandler.showDialogIfNeeded(this) }
        setupObservers()
    }

    private fun setupView() {
        setSupportActionBar(binding.toolbarFolderLink)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "MEGA - ${getFormattedStringOrDefault(R.string.general_loading)}"
        }

        setTransfersWidgetLayout(findViewById(R.id.transfers_widget_layout))

        binding.apply {
            folderLinkFileLinkFragmentContainer.visibility = View.GONE

            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                folderLinkListEmptyImage.setImageResource(R.drawable.empty_folder_landscape)
            } else {
                folderLinkListEmptyImage.setImageResource(R.drawable.empty_folder_portrait)
            }
            var textToShow = getFormattedStringOrDefault(R.string.file_browser_empty_folder_new)
            try {
                textToShow = textToShow.replace(
                    "[A]",
                    "<font color=\'${
                        getColorHexString(
                            this@FolderLinkActivity,
                            R.color.grey_900_grey_100
                        )
                    }\'>"
                )
                textToShow = textToShow.replace("[/A]", "</font>")
                textToShow = textToShow.replace(
                    "[B]",
                    "<font color=\'${
                        getColorHexString(
                            this@FolderLinkActivity,
                            R.color.grey_300_grey_600
                        )
                    }\'>"
                )
                textToShow = textToShow.replace("[/B]", "</font>")
            } catch (_: Exception) {
            }

            val result = HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY)
            folderLinkListEmptyText.text = result
            folderLinkListEmptyImage.visibility = View.GONE
            folderLinkListEmptyText.visibility = View.GONE

            folderLinkListViewBrowser.apply {
                addItemDecoration(
                    PositionDividerItemDecoration(
                        this@FolderLinkActivity,
                        resources.displayMetrics
                    )
                )
                mLayoutManager = LinearLayoutManager(this@FolderLinkActivity)
                layoutManager = mLayoutManager
                itemAnimator = Util.noChangeRecyclerViewItemAnimator()
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        checkScroll()
                    }
                })
            }

            folderLinkGridViewBrowser.apply {
                gridLayoutManager = layoutManager as CustomizedGridLayoutManager?
                layoutManager = gridLayoutManager
                itemAnimator = DefaultItemAnimator()
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        checkScroll()
                    }
                })
            }

            if (viewModel.isList) {
                folderLinkGridViewBrowser.visibility = View.GONE
                folderLinkListViewBrowser.visibility = View.VISIBLE
            } else {
                folderLinkGridViewBrowser.visibility = View.VISIBLE
                folderLinkListViewBrowser.visibility = View.GONE
            }

            folderLinkButtonDownload.setOnClickListener(downloadButtonClickListener)
            folderLinkImportButton.setOnClickListener(importButtonClickListener)
            folderLinkFileLinkButtonDownload.setOnClickListener(downloadButtonClickListener)
            folderLinkFileLinkButtonImport.apply {
                setOnClickListener(importButtonClickListener)
                visibility = View.INVISIBLE
            }

            if (dbH.credentials != null) {
                folderLinkImportButton.visibility = View.VISIBLE
            } else {
                folderLinkImportButton.visibility = View.GONE
            }
        }
    }

    private fun setupObservers() {
        observeDragSupportEvents(this, recyclerView, Constants.VIEWER_FROM_FOLDER_LINK)

        LiveEventBus.get(EVENT_UPDATE_VIEW_MODE, Boolean::class.java)
            .observe(this) { isList: Boolean -> updateView(isList) }

        collectFlow(viewModel.onViewTypeChanged, Lifecycle.State.STARTED) { viewType: ViewType ->
            updateViewType(viewType)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state.collect {
                    when {
                        it.isInitialState -> {
                            return@collect
                        }
                        it.isLoginComplete && !it.isNodesFetched -> {
                            megaApiFolder.fetchNodes(this@FolderLinkActivity)
                            // Get cookies settings after login.
                            getInstance().checkEnabledCookies()
                        }
                        it.isLoginComplete && it.isNodesFetched -> {}
                        it.askForDecryptionKeyDialog -> {
                            askForDecryptionKeyDialog()
                        }
                        else -> {
                            try {
                                Timber.w("Show error dialog")
                                showErrorDialog(it.errorDialogTitle, it.errorDialogContent)

                            } catch (ex: Exception) {
                                showSnackbar(it.snackBarMessage)
                                finish()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        nodeSaver.saveState(outState)
    }

    private fun checkScroll() {
        val canScroll = recyclerView.canScrollVertically(-1)
        Util.changeToolBarElevation(
            this,
            binding.toolbarFolderLink,
            canScroll || adapterList?.isMultipleSelect ?: false
        )
    }

    private fun askForDecryptionKeyDialog() {
        Timber.d("askForDecryptionKeyDialog")
        val builder = DecryptAlertDialog.Builder()
        builder.setListener(this)
            .setTitle(getFormattedStringOrDefault(R.string.alert_decryption_key))
            .setPosText(R.string.general_decryp).setNegText(R.string.general_cancel)
            .setMessage(getFormattedStringOrDefault(R.string.message_decryption_key))
            .setErrorMessage(R.string.invalid_decryption_key).setKey(mKey)
            .build().show(supportFragmentManager, TAG_DECRYPT)
    }

    override fun onDestroy() {
        megaApiFolder.removeRequestListener(this)
        nodeSaver.destroy()
        super.onDestroy()
    }

    override fun onPause() {
        folderLinkActivity = null
        Timber.d("onPause")
        super.onPause()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.folderLinkListEmptyImage.setImageResource(R.drawable.empty_folder_landscape)
        } else {
            binding.folderLinkListEmptyImage.setImageResource(R.drawable.empty_folder_portrait)
        }
        if (!viewModel.isList) {
            binding.folderLinkGridViewBrowser.measure(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.MATCH_PARENT
            )
            adapterList?.setNodes(ArrayList(nodes))
            gridLayoutManager?.apply {
                spanSizeLookup = adapterList?.getSpanSizeLookup(spanCount)
            }
        }
        cookieDialogHandler.showDialogIfNeeded(this, true)
    }

    override fun onResume() {
        super.onResume()
        folderLinkActivity = this
        Timber.d("onResume")
    }

    private fun downloadNodes(nodes: List<MegaNode>) {
        checkNotificationsPermission(this)
        nodeSaver.saveNodes(
            nodes,
            highPriority = false,
            isFolderLink = true,
            fromMediaViewer = false,
            needSerialize = false
        )
    }

    @SuppressLint("CheckResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        Timber.d("onActivityResult")
        if (intent == null) {
            return
        }
        if (nodeSaver.handleActivityResult(this, requestCode, resultCode, intent)) {
            return
        }
    }

    /**
     * Shows the copy Result.
     *
     * @param copyRequestResult Object containing the request result.
     * @param throwable
     */
    private fun showCopyResult(copyRequestResult: CopyRequestResult?, throwable: Throwable?) {
        dismissAlertDialogIfExists(statusDialog)
        clearSelections()
        hideMultipleSelect()
        if (copyRequestResult != null) {
            showSnackbar(copyRequestResult.getResultText())
        } else throwable?.let { manageCopyMoveException(it) }
            ?: showSnackbar(R.string.context_correctly_copied)
    }

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestStart: ${request.requestString}")
    }

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestUpdate: ${request.requestString}")
    }

    @SuppressLint("NewApi")
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.d("onRequestFinish: ${request.requestString}")
        if (request.type == MegaRequest.TYPE_FETCH_NODES) {
            viewModel.updateIsNodesFetched(true)
            if (e.errorCode == MegaError.API_OK) {
                Timber.d("DOCUMENTNODEHANDLEPUBLIC: ${request.nodeHandle}")
                if (request.nodeHandle != MegaApiJava.INVALID_HANDLE) {
                    dbH.setLastPublicHandle(request.nodeHandle)
                    dbH.setLastPublicHandleTimeStamp()
                    dbH.lastPublicHandleType = MegaApiJava.AFFILIATE_TYPE_FILE_FOLDER
                }
                val rootNode = megaApiFolder.rootNode
                if (rootNode != null) {
                    if (request.flag) {
                        Timber.w("Login into a folder with invalid decryption key")
                        try {
                            showErrorDialog(
                                R.string.general_error_word,
                                R.string.general_error_invalid_decryption_key
                            )

                        } catch (ex: Exception) {
                            showSnackbar(R.string.general_error_folder_not_found)
                            finish()
                        }
                    } else {
                        if (folderSubHandle != null) {
                            pN = megaApiFolder.getNodeByHandle(
                                MegaApiAndroid.base64ToHandle(folderSubHandle)
                            )

                            pN?.let {
                                if (it.isFolder) {
                                    parentHandle = MegaApiAndroid.base64ToHandle(folderSubHandle)
                                    nodes = megaApiFolder.getChildren(it)
                                    supportActionBar?.title = it.name
                                    invalidateOptionsMenu()
                                } else if (it.isFile) {
                                    fileLinkFolderLink = true
                                    parentHandle = MegaApiAndroid.base64ToHandle(folderSubHandle)
                                    setSupportActionBar(binding.toolbarFolderLinkFileLink)

                                    supportActionBar?.apply {
                                        setDisplayHomeAsUpEnabled(true)
                                        setDisplayShowHomeEnabled(true)
                                        title = ""
                                    }

                                    binding.apply {
                                        folderLinkFragmentContainer.isVisible = false
                                        folderLinkFileLinkFragmentContainer.isVisible = true
                                        folderLinkFileLinkName.text = it.name
                                        folderLinkFileLinkSize.text = Util.getSizeString(it.size)
                                        folderLinkFileLinkIcon.setImageResource(
                                            typeForName(it.name).iconResourceId
                                        )
                                        folderLinkFileLinkButtonDownload.isVisible = true

                                        if (dbH.credentials != null) {
                                            folderLinkFileLinkButtonImport.isVisible = true
                                        } else {
                                            folderLinkFileLinkButtonImport.visibility =
                                                View.INVISIBLE
                                        }

                                        var preview = PreviewUtils.getPreviewFromCache(it)
                                        if (preview != null) {
                                            PreviewUtils.previewCache.put(it.handle, preview)
                                            folderLinkFileLinkIcon.setImageBitmap(preview)
                                        } else {
                                            preview = PreviewUtils.getPreviewFromFolder(
                                                it,
                                                this@FolderLinkActivity
                                            )
                                            if (preview != null) {
                                                PreviewUtils.previewCache.put(it.handle, preview)
                                                folderLinkFileLinkIcon.setImageBitmap(preview)
                                            } else {
                                                if (it.hasPreview()) {
                                                    val previewFile = File(
                                                        PreviewUtils.getPreviewFolder(this@FolderLinkActivity),
                                                        "${it.base64Handle}.jpg"
                                                    )
                                                    megaApiFolder.getPreview(
                                                        it,
                                                        previewFile.absolutePath,
                                                        this@FolderLinkActivity
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    parentHandle = rootNode.handle
                                    nodes = megaApiFolder.getChildren(rootNode)
                                    supportActionBar?.title = megaApiFolder.rootNode.name
                                    invalidateOptionsMenu()
                                }
                            } ?: kotlin.run {
                                parentHandle = rootNode.handle
                                nodes = megaApiFolder.getChildren(rootNode)
                                supportActionBar?.title = megaApiFolder.rootNode.name
                                invalidateOptionsMenu()
                            }
                        } else {
                            parentHandle = rootNode.handle
                            nodes = megaApiFolder.getChildren(rootNode)
                            supportActionBar?.title = megaApiFolder.rootNode.name
                            invalidateOptionsMenu()
                        }
                        setupRecyclerViewAdapter()
                    }
                } else {
                    try {
                        showErrorDialog(
                            R.string.general_error_word,
                            R.string.general_error_folder_not_found
                        )

                    } catch (ex: Exception) {
                        showSnackbar(R.string.general_error_folder_not_found)
                        finish()
                    }
                }
            } else {
                Timber.w("Error: ${e.errorCode} ${e.errorString}")
                try {
                    when (e.errorCode) {
                        MegaError.API_EBLOCKED -> {
                            showErrorDialog(
                                R.string.general_error_folder_not_found,
                                R.string.folder_link_unavaible_ToS_violation
                            )
                        }
                        MegaError.API_ETOOMANY -> {
                            showErrorDialog(
                                R.string.general_error_folder_not_found,
                                R.string.file_link_unavaible_delete_account
                            )
                        }
                        else -> {
                            showErrorDialog(
                                R.string.general_error_word,
                                R.string.general_error_folder_not_found
                            )
                        }
                    }

                } catch (ex: Exception) {
                    showSnackbar(R.string.general_error_folder_not_found)
                    finish()
                }
            }
        } else if (request.type == MegaRequest.TYPE_GET_ATTR_FILE) {
            if (e.errorCode == MegaError.API_OK) {
                val previewDir = PreviewUtils.getPreviewFolder(this)
                pN?.let {
                    val preview = File(previewDir, "${it.base64Handle}.jpg")
                    if (preview.exists()) {
                        if (preview.length() > 0) {
                            val bitmap = PreviewUtils.getBitmapForCache(preview, this)
                            PreviewUtils.previewCache.put(it.handle, bitmap)
                            binding.folderLinkFileLinkIcon.setImageBitmap(bitmap)
                        }
                    }
                }
            }
        }
    }

    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.w("onRequestTemporaryError: ${request.requestString}")
    }

    private fun showErrorDialog(@StringRes title: Int, @StringRes message: Int) {
        val builder =
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
        builder.apply {
            setTitle(getFormattedStringOrDefault(title))
            setMessage(getFormattedStringOrDefault(message))
            setPositiveButton(getFormattedStringOrDefault(android.R.string.ok)) { dialog, _ ->
                dialog.dismiss()
                val closedChat = isClosedChat
                if (closedChat) {
                    val backIntent = Intent(
                        Objects.requireNonNullElse(folderLinkActivity, this@FolderLinkActivity),
                        ManagerActivity::class.java
                    )
                    startActivity(backIntent)
                }
                finish()
            }
        }
        builder.create().show()
    }

    private fun setupRecyclerViewAdapter() {
        val adapterType =
            if (viewModel.isList) MegaNodeAdapter.ITEM_VIEW_TYPE_LIST else MegaNodeAdapter.ITEM_VIEW_TYPE_GRID

        adapterList = MegaNodeAdapter(
            this, null, ArrayList(),
            parentHandle, recyclerView, Constants.FOLDER_LINK_ADAPTER,
            adapterType, sortByHeaderViewModel
        )

        adapterList?.apply {
            isMultipleSelect = false
            setNodes(ArrayList(nodes))
            recyclerView.adapter = this
        }

        binding.apply {
            if (viewModel.isList) {
                folderLinkGridViewBrowser.visibility = View.GONE
                folderLinkListViewBrowser.visibility = View.VISIBLE
            } else {
                folderLinkGridViewBrowser.visibility = View.VISIBLE
                folderLinkListViewBrowser.visibility = View.GONE
                gridLayoutManager?.apply {
                    spanSizeLookup = adapterList?.getSpanSizeLookup(spanCount)
                }
            }

            //If folder has not files
            if (adapterList == null || adapterList?.itemCount == 0) {
                folderLinkListViewBrowser.visibility = View.GONE
                folderLinkGridViewBrowser.visibility = View.GONE
                folderLinkListEmptyImage.visibility = View.VISIBLE
                folderLinkListEmptyText.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                folderLinkListEmptyImage.visibility = View.GONE
                folderLinkListEmptyText.visibility = View.GONE
            }
        }
    }

    /**
     * Disable selection
     */
    fun hideMultipleSelect() {
        adapterList?.isMultipleSelect = false
        actionMode?.finish()
        binding.optionsFolderLinkLayout.visibility = View.VISIBLE
        binding.separator3.visibility = View.VISIBLE
    }

    private fun selectAll() {
        adapterList?.apply {
            selectAll()
            if (!isMultipleSelect) {
                isMultipleSelect = true
                actionMode = startSupportActionMode(ActionBarCallBack())
            }
            Handler(Looper.getMainLooper()).post { updateActionModeTitle() }
        }
    }

    /**
     * Clear all selected items
     */
    private fun clearSelections() {
        if (adapterList?.isMultipleSelect == true) {
            adapterList?.clearSelections()
        }
    }

    private fun updateActionModeTitle() {
        if (actionMode == null) {
            return
        }
        val documents = adapterList?.selectedNodes ?: listOf()
        var files = 0
        var folders = 0
        for (document: MegaNode in documents) {
            if (document.isFile) {
                files++
            } else if (document.isFolder) {
                folders++
            }
        }
        val sum = files + folders
        val title = if (files == 0 && folders == 0) {
            sum.toString()
        } else if (files == 0) {
            folders.toString()
        } else if (folders == 0) {
            files.toString()
        } else {
            sum.toString()
        }
        actionMode?.title = title
        try {
            actionMode?.invalidate()
        } catch (e: NullPointerException) {
            Timber.e(e, "Invalidate error")
            e.printStackTrace()
        }
    }

    /**
     * Handle adapter item click
     */
    @SuppressLint("NotifyDataSetChanged")
    fun itemClick(position: Int) {
        val adapterList = adapterList ?: return
        if (adapterList.isMultipleSelect) {
            Timber.d("Multiselect ON")
            adapterList.toggleSelection(position)
            val selectedNodes = adapterList.selectedNodes
            if (selectedNodes.size > 0) {
                updateActionModeTitle()
            }
        } else {
            val node = adapterList.getItem(position)
            if (node.isFolder) {
                val lastFirstVisiblePosition: Int =
                    if (viewModel.isList)
                        mLayoutManager?.findFirstCompletelyVisibleItemPosition() ?: 0
                    else
                        gridLayoutManager?.findFirstCompletelyVisibleItemPosition() ?: 0

                Timber.d("Push to stack $lastFirstVisiblePosition position")
                lastPositionStack?.push(lastFirstVisiblePosition)
                supportActionBar?.title = node.name
                invalidateOptionsMenu()

                parentHandle = node.handle
                adapterList.parentHandle = parentHandle
                nodes = megaApiFolder.getChildren(node, sortOrderToInt(orderGetChildren))
                adapterList.setNodes(ArrayList(nodes))
                recyclerView.scrollToPosition(0)

                //If folder has no files
                binding.apply {
                    if (adapterList.itemCount == 0) {
                        recyclerView.visibility = View.GONE
                        folderLinkListEmptyImage.visibility = View.VISIBLE
                        folderLinkListEmptyText.visibility = View.VISIBLE
                    } else {
                        recyclerView.visibility = View.VISIBLE
                        folderLinkListEmptyImage.visibility = View.GONE
                        folderLinkListEmptyText.visibility = View.GONE
                    }
                }
            } else {
                if (typeForName(node.name).isImage) {
                    val children =
                        nodes.stream().mapToLong { obj: MegaNode -> obj.handle }.toArray()
                    val intent = getIntentForChildren(
                        this,
                        children,
                        node.handle
                    )
                    putThumbnailLocation(
                        intent,
                        recyclerView,
                        position,
                        Constants.VIEWER_FROM_FOLDER_LINK,
                        adapterList
                    )
                    startActivity(intent)
                    overridePendingTransition(0, 0)

                } else if (typeForName(node.name).isVideoMimeType || typeForName(node.name).isAudio) {
                    Timber.d("FILE HANDLE: ${node.handle}")
                    val mediaIntent: Intent
                    val internalIntent: Boolean
                    var opusFile = false

                    if (typeForName(node.name).isVideoNotSupported || typeForName(node.name).isAudioNotSupported) {
                        mediaIntent = Intent(Intent.ACTION_VIEW)
                        internalIntent = false
                        val s: Array<String> =
                            node.name.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                        if ((s.size > 1) && (s[s.size - 1] == "opus")) {
                            opusFile = true
                        }
                    } else {
                        internalIntent = true
                        mediaIntent = Util.getMediaIntent(this, node.name)
                    }

                    mediaIntent.putExtra("orderGetChildren", orderGetChildren)
                    mediaIntent.putExtra("isFolderLink", true)
                    mediaIntent.putExtra("HANDLE", node.handle)
                    mediaIntent.putExtra("FILENAME", node.name)
                    putThumbnailLocation(
                        mediaIntent,
                        recyclerView,
                        position,
                        Constants.VIEWER_FROM_FOLDER_LINK,
                        adapterList
                    )

                    mediaIntent.putExtra("adapterType", Constants.FOLDER_LINK_ADAPTER)
                    val parentNode = megaApiFolder.getParentNode(node)

                    //Null check validation.
                    if (parentNode == null) {
                        Timber.e("${node.name}'s parent node is null")
                        return
                    }

                    if (parentNode.type == MegaNode.TYPE_ROOT) {
                        mediaIntent.putExtra("parentNodeHandle", -1L)
                    } else {
                        mediaIntent.putExtra(
                            "parentNodeHandle",
                            megaApiFolder.getParentNode(node).handle
                        )
                    }

                    val localPath = FileUtil.getLocalFile(node)
                    val api = if (dbH.credentials != null) megaApi else megaApiFolder
                    val paramsSetSuccessfully =
                        if (FileUtil.isLocalFile(node, megaApiFolder, localPath)) {
                            FileUtil.setLocalIntentParams(
                                this, node, mediaIntent,
                                localPath, false, this
                            )
                        } else {
                            FileUtil.setStreamingIntentParams(
                                this, node, api,
                                mediaIntent, this
                            )
                        }

                    if (!paramsSetSuccessfully) {
                        return
                    }

                    if (opusFile) {
                        mediaIntent.setDataAndType(mediaIntent.data, "audio/*")
                    }

                    if (internalIntent) {
                        startActivity(mediaIntent)
                    } else {
                        if (MegaApiUtils.isIntentAvailable(this, mediaIntent)) {
                            startActivity(mediaIntent)
                        } else {
                            showSnackbar(R.string.intent_not_available)
                            adapterList.notifyDataSetChanged()
                            downloadNodes(listOf(node))
                        }
                    }
                    overridePendingTransition(0, 0)

                } else if (typeForName(node.name).isPdf) {
                    val mimeType = typeForName(node.name).type
                    Timber.d("FILE HANDLE: ${node.handle}, TYPE: $mimeType")
                    val pdfIntent = Intent(this@FolderLinkActivity, PdfViewerActivity::class.java)
                    pdfIntent.putExtra("APP", true)
                    pdfIntent.putExtra("adapterType", Constants.FOLDER_LINK_ADAPTER)
                    val localPath = FileUtil.getLocalFile(node)
                    val api = if (dbH.credentials != null) megaApi else megaApiFolder

                    val paramsSetSuccessfully: Boolean =
                        if (FileUtil.isLocalFile(node, megaApiFolder, localPath)) {
                            FileUtil.setLocalIntentParams(
                                this, node, pdfIntent,
                                localPath, false, this
                            )
                        } else {
                            FileUtil.setStreamingIntentParams(
                                this, node, api,
                                pdfIntent, this
                            )
                        }

                    if (!paramsSetSuccessfully) {
                        return
                    }

                    pdfIntent.putExtra("HANDLE", node.handle)
                    pdfIntent.putExtra("isFolderLink", true)
                    pdfIntent.putExtra("inside", true)
                    putThumbnailLocation(
                        pdfIntent,
                        recyclerView,
                        position,
                        Constants.VIEWER_FROM_FOLDER_LINK,
                        adapterList
                    )

                    if (MegaApiUtils.isIntentAvailable(this@FolderLinkActivity, pdfIntent)) {
                        startActivity(pdfIntent)
                    } else {
                        Toast.makeText(
                            this@FolderLinkActivity,
                            getFormattedStringOrDefault(R.string.intent_not_available),
                            Toast.LENGTH_LONG
                        ).show()
                        downloadNodes(listOf(node))
                    }
                    overridePendingTransition(0, 0)

                } else if (typeForName(node.name).isOpenableTextFile(node.size)) {
                    manageTextFileIntent(this, node, Constants.FOLDER_LINK_ADAPTER)

                } else {
                    val hasStoragePermission =
                        hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    if (!hasStoragePermission) {
                        requestPermission(
                            this,
                            Constants.REQUEST_WRITE_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                        handleListM.clear()
                        handleListM.add(node.handle)
                        return
                    }
                    adapterList.notifyDataSetChanged()
                    downloadNodes(listOf(node))
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        nodeSaver.handleRequestPermissionsResult(requestCode)
    }

    /**
     * Handle import option
     */
    fun importNode() {
        val intent = Intent(this, FileExplorerActivity::class.java)
        intent.action = FileExplorerActivity.ACTION_PICK_IMPORT_FOLDER
        selectImportFolderLauncher.launch(intent)
    }

    /**
     * Handle download option
     */
    fun downloadNode() {
        Timber.d("Download option")
        downloadNodes(selectedNode?.let { listOf(it) } ?: listOf())
    }

    /**
     * Handle more options button click
     */
    fun showOptionsPanel(sNode: MegaNode?) {
        Timber.d("showNodeOptionsPanel-Offline")
        if (sNode == null || bottomSheetDialogFragment.isBottomSheetDialogShown()) return
        selectedNode = sNode
        bottomSheetDialogFragment = FolderLinkBottomSheetDialogFragment()
        bottomSheetDialogFragment?.show(supportFragmentManager, bottomSheetDialogFragment?.tag)
    }

    private fun showSnackbar(@StringRes message: Int) {
        Timber.d("showSnackbar")
        showSnackbar(
            Constants.SNACKBAR_TYPE,
            binding.folderLinkFragmentContainer,
            getFormattedStringOrDefault(message)
        )
    }

    private fun showSnackbar(string: String) {
        Timber.d("showSnackbar")
        showSnackbar(Constants.SNACKBAR_TYPE, binding.folderLinkFragmentContainer, string)
    }

    /**
     * Updates the View Type
     *
     * @param viewType The new View Type
     */
    private fun updateViewType(viewType: ViewType) {
        Timber.d("The updated View Type is ${viewType.name}")
        viewModel.isList = viewType === ViewType.LIST
    }

    private fun updateView(isList: Boolean) {
        if (viewModel.isList != isList) {
            viewModel.isList = isList
            dbH.setPreferredViewList(isList)
        }
        setupRecyclerViewAdapter()
    }

    companion object {
        private const val TAG_DECRYPT = "decrypt"
    }
}