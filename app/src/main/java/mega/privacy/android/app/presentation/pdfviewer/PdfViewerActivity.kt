package mega.privacy.android.app.presentation.pdfviewer

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.provider.OpenableColumns
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shockwave.pdfium.PdfDocument.Bookmark
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.OfflineFileInfoActivity
import mega.privacy.android.app.activities.contract.NameCollisionActivityContract
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.dragger.DragToExitSupport
import mega.privacy.android.app.databinding.ActivityPdfviewerBinding
import mega.privacy.android.app.extensions.enableEdgeToEdgeAndConsumeInsets
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbarWithChat
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.presentation.extensions.getStorageState
import mega.privacy.android.app.presentation.fileinfo.FileInfoActivity
import mega.privacy.android.app.presentation.hidenode.HiddenNodesOnboardingActivity
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.presentation.transfers.attach.NodeAttachmentViewModel
import mega.privacy.android.app.presentation.transfers.attach.createNodeAttachmentView
import mega.privacy.android.app.presentation.transfers.starttransfer.StartDownloadViewModel
import mega.privacy.android.app.presentation.transfers.starttransfer.view.createStartTransferView
import mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.AlertsAndWarnings.showTakenDownAlert
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.HANDLE
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaNodeDialogUtil.moveToRubbishOrRemove
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showRenameNodeDialog
import mega.privacy.android.app.utils.MegaNodeUtil.getRootParentNode
import mega.privacy.android.app.utils.MegaNodeUtil.shareLink
import mega.privacy.android.app.utils.MegaNodeUtil.shareNode
import mega.privacy.android.app.utils.MegaNodeUtil.showShareOption
import mega.privacy.android.app.utils.MegaNodeUtil.showTakenDownNodeActionNotAvailableDialog
import mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog
import mega.privacy.android.app.utils.RunOnUIThreadUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.navigation.ExtraConstant
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.DocumentPreviewHideNodeMenuItemEvent
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaEvent
import nz.mega.sdk.MegaGlobalListenerInterface
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaSet
import nz.mega.sdk.MegaSetElement
import nz.mega.sdk.MegaShare
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransferListenerInterface
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * PDF viewer.
 *
 * @property passCodeFacade            [PasscodeCheck]
 * @property password                  Typed password
 * @property maxIntents                Max of intents for a wrong password.
 * @property pdfFileName               Name of the PDF.
 * @property isToolbarVisible          True if the toolbar is visible, false otherwise.
 * @property takenDownDialog           Taken down dialog.
 * @property progressBar               Loading progress bar.
 */
@AndroidEntryPoint
class PdfViewerActivity : BaseActivity(), MegaGlobalListenerInterface, OnPageChangeListener,
    OnLoadCompleteListener, OnPageErrorListener, MegaRequestListenerInterface,
    MegaTransferListenerInterface, ActionNodeCallback, SnackbarShower {

    @Inject
    lateinit var passCodeFacade: PasscodeCheck

    /**
     * Application scope
     */
    @ApplicationScope
    @Inject
    lateinit var applicationScope: CoroutineScope

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    private lateinit var binding: ActivityPdfviewerBinding

    private var menu: Menu? = null

    var password: String? = null
    var maxIntents = 3
    var pdfFileName: String? = null
    var isToolbarVisible = true
    var takenDownDialog: AlertDialog? = null
    val progressBar
        get() = binding.pdfViewerProgressBar

    private var isUrl = false
    private var defaultScrollHandle: DefaultScrollHandle? = null
    private var uri: Uri? = null
    private var handle: Long = -1
    private var isFolderLink = false
    private var currentPage = 0
    private var type = 0
    private var isOffLine = false
    private var statusDialog: AlertDialog? = null
    private var renamed = false
    private var path: String? = null
    private var pathNavigation: String? = null

    // it's only used for enter animation
    private val dragToExit = DragToExitSupport(this, lifecycleScope, null, null)
    private var nC: NodeController? = null
    private var handler: Handler? = null
    private var fromChat = false
    private var isDeleteDialogShow = false
    private var fromDownload = false
    private var chatC: ChatController? = null
    private var msgId: Long = -1
    private var chatId: Long = -1
    private var msgChat: MegaChatMessage? = null
    private var notChangePage = false
    private var inside = false
    private var node: MegaNode? = null

    private val viewModel by viewModels<PdfViewerViewModel>()
    private val startDownloadViewModel by viewModels<StartDownloadViewModel>()
    private val nodeAttachmentViewModel by viewModels<NodeAttachmentViewModel>()

    private var isHiddenNodesEnabled: Boolean = false
    private var tempNodeId: NodeId? = null

    private val nameCollisionActivityContract = registerForActivityResult(
        NameCollisionActivityContract()
    ) { result ->
        result?.let {
            showSnackbar(SNACKBAR_TYPE, it, INVALID_HANDLE)
        }
    }


    override fun shouldSetStatusBarTextColor() = false

    /**
     * Attach base context
     *
     */
    override fun attachBaseContext(newBase: Context?) {
        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
        super.attachBaseContext(newBase)
    }

    private suspend fun isHiddenNodesActive(): Boolean {
        val result = runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
        }
        return result.getOrNull() ?: false
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        enableEdgeToEdgeAndConsumeInsets()
        super.onCreate(savedInstanceState)
        appContainerWrapper.setPasscodeCheck(passCodeFacade)
        if (intent == null) {
            Timber.w("Intent null")
            finish()
            return
        }
        val credentials = runBlocking {
            runCatching { getAccountCredentialsUseCase() }
                .onFailure { Timber.e(it) }
        }

        binding = ActivityPdfviewerBinding.inflate(layoutInflater)

        lifecycleScope.launch {
            runCatching {
                isHiddenNodesEnabled = isHiddenNodesActive()
                invalidateOptionsMenu()
            }.onFailure { Timber.e(it) }
        }

        with(window) {
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        handler = Handler(Looper.getMainLooper())

        if (savedInstanceState != null) {
            Timber.d("saveInstanceState")
            currentPage = savedInstanceState.getInt("currentPage")
            handle = savedInstanceState.getLong("HANDLE")
            pdfFileName = savedInstanceState.getString("pdfFileName")
            uri = Uri.parse(savedInstanceState.getString("uri"))
            renamed = savedInstanceState.getBoolean("renamed")
            isDeleteDialogShow = savedInstanceState.getBoolean("isDeleteDialogShow", false)
            isToolbarVisible = savedInstanceState.getBoolean("toolbarVisible", isToolbarVisible)
            password = savedInstanceState.getString("password")
            maxIntents = savedInstanceState.getInt("maxIntents", 3)
        } else {
            currentPage = 1
            isDeleteDialogShow = false
            handle = intent.getLongExtra("HANDLE", -1)
            uri = intent.data
            if (uri == null) {
                Timber.e("Uri null")
                finish()
                return
            }
            Timber.d("URI pdf: $uri")
        }

        fromDownload = intent.getBooleanExtra("fromDownloadService", false)
        inside = intent.getBooleanExtra("inside", false)

        if (!inside) {
            passCodeFacade.disablePasscode()
        }

        isFolderLink = intent.getBooleanExtra("isFolderLink", false)
        type = intent.getIntExtra("adapterType", 0)
        path = intent.getStringExtra("path")
        initializeBasedOnAdapterType()

        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        if (!isOffLine && type != Constants.ZIP_ADAPTER) {
            if (msgId != -1L && chatId != -1L) {
                msgChat = megaChatApi.getMessage(chatId, msgId)
                if (msgChat == null) {
                    msgChat = megaChatApi.getMessageFromNodeHistory(chatId, msgId)
                }
                msgChat?.apply {
                    node = chatC?.authorizeNodeIfPreview(
                        megaNodeList[0],
                        megaChatApi.getChatRoom(chatId)
                    )
                    if (isDeleteDialogShow) {
                        showConfirmationDeleteNode(chatId, msgChat)
                    }
                }
            } else {
                Timber.w("msgId or chatId null")
            }
            Timber.d("Add transfer listener")
            megaApi.addTransferListener(this)
            megaApi.addGlobalListener(this)
            if (uri.toString().contains("http://")) {
                when {
                    credentials != null -> megaApi
                    isFolderLink -> megaApiFolder
                    else -> null
                }?.apply { initStreaming() }

                if (savedInstanceState != null && !isFolderLink) {
                    var url: String? = null
                    if (node != null) {
                        url = megaApi.httpServerGetLocalLink(node)
                        if (url != null) {
                            uri = Uri.parse(url)
                        }
                    }
                    if (node == null || url == null || uri == null) {
                        showSnackbar(
                            SNACKBAR_TYPE,
                            getString(R.string.error_streaming),
                            MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                        )
                    }
                }
            }
            if (isFolderLink) {
                Timber.d("Folder link node")
                node = megaApiFolder.authorizeNode(megaApiFolder.getNodeByHandle(handle))
                if (node == null) {
                    Timber.w("CurrentDocumentAuth is null")
                    showSnackbar(
                        SNACKBAR_TYPE,
                        getString(R.string.error_streaming) + ": node not authorized",
                        -1
                    )
                } else {
                    Timber.d("CurrentDocumentAuth is not null")
                    val url: String? = if (credentials != null) {
                        megaApi.httpServerGetLocalLink(node)
                    } else {
                        megaApiFolder.httpServerGetLocalLink(node)
                    }
                    if (url != null) {
                        uri = Uri.parse(url)
                    }
                }
            }
        }

        pdfFileName = getFileName(uri)
        defaultScrollHandle = DefaultScrollHandle(this@PdfViewerActivity)
        loading = true
        if (uri.toString().contains("http://")) {
            isUrl = true
            loadStreamPDF()
        } else {
            isUrl = false
            loadLocalPDF()
        }

        setupView()

        collectFLows()
        if (savedInstanceState == null) {
            runEnterAnimation(intent)
        }
    }

    private fun collectFLows() {
        collectFlow(viewModel.uiState) { pdfViewerState ->
            with(pdfViewerState) {
                (startChatOfflineDownloadEvent as? StateEventWithContentTriggered)?.let { event ->
                    startDownloadViewModel.onSaveOfflineClicked(
                        chatFile = event.content,
                        withStartMessage = true,
                    )
                    viewModel.onConsumeStartChatOfflineDownloadEvent()
                }

                if (snackBarMessage != null) {
                    dismissAlertDialogIfExists(statusDialog)
                    showSnackbar(
                        SNACKBAR_TYPE,
                        getString(snackBarMessage),
                        MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                    )
                    viewModel.onConsumeSnackBarMessage()
                }
                if (shouldFinishActivity) {
                    finish()
                }
                if (nodeMoveError != null) {
                    handleCopyMoveError(copyMoveError = nodeMoveError, isCopy = false)
                    viewModel.onConsumeNodeMoveError()
                }
                if (nodeCopyError != null) {
                    handleCopyMoveError(copyMoveError = nodeCopyError, isCopy = true)
                    viewModel.onConsumeNodeCopyError()
                }
                if (nameCollision != null) {
                    dismissAlertDialogIfExists(statusDialog)
                    nameCollisionActivityContract.launch(arrayListOf(nameCollision))
                }
                if (pdfStreamData != null && lastPageViewed != null) {
                    try {
                        binding.pdfView.fromBytes(pdfStreamData)
                            .defaultPage(lastPageViewed.toInt() - 1)
                            .onPageChange(this@PdfViewerActivity)
                            .enableAnnotationRendering(true)
                            .onLoad(this@PdfViewerActivity)
                            .scrollHandle(defaultScrollHandle)
                            .spacing(10) // in dp
                            .onPageError(this@PdfViewerActivity)
                            .password(password)
                            .load()
                    } catch (e: Exception) {
                        Timber.w("Exception loading PDF as stream", e)
                    }
                    viewModel.resetPdfStreamData()
                    if (loading) {
                        binding.pdfViewerProgressBar.isVisible = true
                    }
                }

                if (pdfUriData != null && lastPageViewed != null) {
                    loadLocalPDF(lastPageViewed.toInt())
                }
            }
        }
    }

    private fun handleCopyMoveError(copyMoveError: Throwable, isCopy: Boolean) {
        dismissAlertDialogIfExists(statusDialog)
        if (!manageCopyMoveException(copyMoveError)) {
            showSnackbar(
                SNACKBAR_TYPE,
                getString(if (isCopy) R.string.context_no_copied else R.string.context_no_moved),
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
            )
        }
    }

    private fun MegaApiAndroid.initStreaming() {
        if (httpServerIsRunning() == 0) {
            httpServerStart()
        }
    }

    private fun setupView() = with(binding) {
        setContentView(binding.root)
        toolbarPdfViewer.isVisible = true
        setSupportActionBar(toolbarPdfViewer)
        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        pdfView.setBackgroundColor(Color.LTGRAY)
        title = pdfFileName
        pdfViewerFileName.apply {
            maxWidth = Util.scaleWidthPx(300, outMetrics)
            text = pdfFileName
        }

        if (!inside) {
            supportActionBar?.title = pdfFileName
            uploadContainerLayoutBottom.isVisible = true
            pdfViewerLayoutBottom.isVisible = false
        } else {
            supportActionBar?.title = " "
            uploadContainerLayoutBottom.isVisible = false
            pdfViewerLayoutBottom.isVisible = true
        }
        setupBottomClick()

        if (!isToolbarVisible) {
            setToolbarVisibilityHide(0L)
        }
        addStartDownloadTransferView()
        addNodeAttachmentView()
    }

    private fun addNodeAttachmentView() {
        binding.root.addView(
            createNodeAttachmentView(
                this,
                nodeAttachmentViewModel,
                ::showSnackbarWithChat
            )
        )
    }

    private fun addStartDownloadTransferView() {
        binding.root.addView(
            createStartTransferView(
                activity = this,
                transferEventState = startDownloadViewModel.state,
                onConsumeEvent = startDownloadViewModel::consumeDownloadEvent,
            )
        )
    }

    private fun setupBottomClick() = binding.uploadContainerLayoutBottom.setOnClickListener {
        Timber.d("onClick uploadContainer")
        val intent = Intent(this@PdfViewerActivity, FileExplorerActivity::class.java).apply {
            action = Intent.ACTION_SEND
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_STREAM, intent.data)
            type = "application/pdf"
        }
        startActivity(intent)
        finish()
    }

    private fun initializeBasedOnAdapterType() = when (type) {
        Constants.OFFLINE_ADAPTER -> {
            isOffLine = true
            pathNavigation = intent.getStringExtra("pathNavigation")
        }

        Constants.FILE_LINK_ADAPTER -> {
            val serialize = intent.getStringExtra(Constants.EXTRA_SERIALIZE_STRING)
            if (serialize != null) {
                node = MegaNode.unserialize(serialize)
                Timber.d("currentDocument is $node")
            }
            isOffLine = false
            fromChat = false
        }

        else -> {
            isOffLine = false
            pathNavigation = null
            if (type == Constants.FROM_CHAT) {
                fromChat = true
                chatC = ChatController(this)
                msgId = intent.getLongExtra("msgId", -1)
                chatId = intent.getLongExtra("chatId", -1)
            } else {
                fromChat = false
                node = megaApi.getNodeByHandle(handle)
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Timber.d("onNewIntent")

        handler = Handler(Looper.getMainLooper())
        if (intent.getBooleanExtra("inside", false)) {
            setIntent(intent)
            if (!intent.getBooleanExtra("isUrl", true)) {
                isUrl = false
                uri = intent.data
                invalidateOptionsMenu()
            }
        } else {
            passCodeFacade.disablePasscode()
            type = intent.getIntExtra("adapterType", 0)
            path = intent.getStringExtra("path")
            currentPage = 1
            inside = false
            initializeBasedOnAdapterType()
            handle = getIntent().getLongExtra("HANDLE", -1)
            uri = intent.data
            if (uri == null) {
                Timber.e("Uri null")
                finish()
                return
            }
            val newIntent = Intent()
            newIntent.setDataAndType(uri, "application/pdf")
            newIntent.action = Constants.ACTION_OPEN_FOLDER
            setIntent(newIntent)
            setContentView(binding.root)

            if (!isOffLine && type != Constants.ZIP_ADAPTER) {
                if (msgId != -1L && chatId != -1L) {
                    msgChat = megaChatApi.getMessage(chatId, msgId)
                    if (msgChat == null) {
                        msgChat = megaChatApi.getMessageFromNodeHistory(chatId, msgId)
                    }
                    msgChat?.let { node = it.megaNodeList[0] }
                } else {
                    Timber.w("msgId or chatId null")
                }
                Timber.d("Add transfer listener")
                megaApi.addTransferListener(this)
                megaApi.addGlobalListener(this)
            }

            binding.toolbarPdfViewer.isVisible = true
            setSupportActionBar(binding.toolbarPdfViewer)
            supportActionBar?.apply {
                setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
                setHomeButtonEnabled(true)
                setDisplayHomeAsUpEnabled(true)
            }

            binding.pdfView.setBackgroundColor(Color.LTGRAY)
            defaultScrollHandle = DefaultScrollHandle(this@PdfViewerActivity)
            isUrl = false
            loadLocalPDF(currentPage)
            pdfFileName = getFileName(uri)
            path = uri?.path
            title = pdfFileName
            supportActionBar?.title = pdfFileName
            binding.pdfViewerFileName.apply {
                maxWidth = Util.scaleWidthPx(300, outMetrics)
                text = pdfFileName
            }

            binding.uploadContainerLayoutBottom.isVisible = true
            binding.pdfViewerLayoutBottom.isVisible = false
            setupBottomClick()
            runEnterAnimation(intent)
        }
    }

    private fun runEnterAnimation(intent: Intent) = with(binding) {
        dragToExit.runEnterAnimation(intent, pdfView) { animationStart: Boolean ->
            if (animationStart) {
                if (supportActionBar?.isShowing == true) {
                    toolbarPdfViewer.animate().translationY(-220f).setDuration(0)
                        .withEndAction { supportActionBar?.hide() }.start()
                    pdfViewerLayoutBottom.animate().translationY(220f).setDuration(0)
                        .start()
                    uploadContainerLayoutBottom.animate().translationY(220f)
                        .setDuration(0).start()
                }
            } else if (!isFinishing) {
                setToolbarVisibilityShow()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Timber.d("onSaveInstanceState")
        super.onSaveInstanceState(outState)
        outState.apply {
            putInt("currentPage", currentPage)
            putLong("HANDLE", handle)
            putString("pdfFileName", pdfFileName)
            putString("uri", uri.toString())
            putBoolean("renamed", renamed)
            putBoolean("isDeleteDialogShow", isDeleteDialogShow)
            putBoolean("toolbarVisible", isToolbarVisible)
            putString("password", password)
            putInt("maxIntents", maxIntents)
        }
    }

    override fun onUsersUpdate(api: MegaApiJava, users: ArrayList<MegaUser>?) {}

    override fun onUserAlertsUpdate(api: MegaApiJava, userAlerts: ArrayList<MegaUserAlert>?) {
        Timber.d("onUserAlertsUpdate")
    }

    override fun onNodesUpdate(api: MegaApiJava, nodeList: ArrayList<MegaNode>?) {
        Timber.d("onNodesUpdate")
        lifecycleScope.launch {
            val node = withContext(ioDispatcher) {
                megaApi.getNodeByHandle(handle)
            }
            if (node == null) {
                return@launch
            }
            invalidateOptionsMenu()
        }
    }

    override fun onAccountUpdate(api: MegaApiJava) {}

    override fun onContactRequestsUpdate(
        api: MegaApiJava,
        requests: ArrayList<MegaContactRequest>?,
    ) {
    }

    override fun onEvent(api: MegaApiJava, event: MegaEvent?) {}

    override fun onSetsUpdate(api: MegaApiJava, sets: ArrayList<MegaSet>?) {}

    override fun onSetElementsUpdate(api: MegaApiJava, elements: ArrayList<MegaSetElement>?) {}

    override fun onGlobalSyncStateChanged(api: MegaApiJava) {}

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        showSnackbar(type, binding.pdfViewerContainer, content, chatId)
    }

    override fun finishRenameActionWithSuccess(newName: String) {
        lifecycleScope.launch {
            updateFile()
        }
    }

    override fun actionConfirmed() {
        //No update needed
    }

    override fun createFolder(folderName: String) {
        //No action needed
    }

    /**
     * Reload view with password.
     */
    fun reloadPDFwithPassword(password: String?) {
        this.password = password
        maxIntents--

        if (isUrl) {
            loadStreamPDF()
        } else {
            loadLocalPDF()
        }
    }

    private fun loadStreamPDF() {
        Timber.d("loading: $loading")
        viewModel.loadPdfStream(uri.toString())
    }

    private fun loadLocalPDF(currentPage: Int? = null) {
        val uri = this.uri ?: return
        Timber.d("loading: $loading")
        binding.pdfViewerProgressBar.isVisible = true

        if (currentPage == null) {
            viewModel.setPdfUriData(uri)
        } else {
            try {
                binding.pdfView.fromUri(uri)
                    .defaultPage(currentPage - 1)
                    .onPageChange(this)
                    .enableAnnotationRendering(true)
                    .onLoad(this)
                    .scrollHandle(defaultScrollHandle)
                    .spacing(10) // in dp
                    .onPageError(this)
                    .password(password)
                    .load()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun download() {
        if (fromChat) {
            startDownloadViewModel.onDownloadClicked(
                chatId = chatId,
                messageId = msgId,
                withStartMessage = true,
            )
        } else if (type == Constants.FILE_LINK_ADAPTER) {
            node?.serialize()?.let {
                startDownloadViewModel.onDownloadClicked(
                    serializedData = it,
                    withStartMessage = true,
                )
            }
        } else if (isFolderLink) {
            node?.handle?.let {
                startDownloadViewModel.onFolderLinkChildNodeDownloadClicked(
                    nodeId = NodeId(it),
                    withStartMessage = true,
                )
            }
        } else {
            node?.handle?.let {
                startDownloadViewModel.onDownloadClicked(
                    nodeId = NodeId(it),
                    withStartMessage = true,
                )
            }
        }
    }

    private fun setToolbarVisibilityShow() = with(binding) {
        Timber.d("setToolbarVisibilityShow")
        isToolbarVisible = true
        supportActionBar?.show()
        adjustPositionOfScroller()
        toolbarPdfViewer.animate().translationY(0f).setDuration(200L).start()
        pdfViewerLayoutBottom.animate().translationY(0f).setDuration(200L).start()
        uploadContainerLayoutBottom.animate().translationY(0f).setDuration(200L).start()
    }

    /**
     * Hides toolbar with animation.
     */
    fun setToolbarVisibilityHide(duration: Long) = with(binding) {
        Timber.d("Duration: $duration")
        isToolbarVisible = false
        toolbarPdfViewer.animate().translationY(-220f).setDuration(duration)
            .withEndAction { supportActionBar?.hide() }
            .start()
        pdfViewerLayoutBottom.animate().translationY(220f).setDuration(duration).start()
        uploadContainerLayoutBottom.animate().translationY(220f).setDuration(duration)
            .start()
    }

    /*
     * Adjust the position of scroller below the ActionBar
     */
    @SuppressLint("ObjectAnimatorBinding")
    private fun adjustPositionOfScroller() {
        defaultScrollHandle?.post {
            val location = IntArray(2)
            defaultScrollHandle?.getLocationInWindow(location)
            val frame = Rect()
            window.decorView.getWindowVisibleDisplayFrame(frame)
            val height = supportActionBar?.height ?: 0

            // When there is an intersection between the scroller and the ActionBar, move the scroller.
            if (location[1] < height) {
                val animator = ObjectAnimator.ofFloat(
                    defaultScrollHandle,
                    "translationY",
                    (height + 16).toFloat()
                )
                animator.setDuration(200L).start()
            }
        }
    }

    /**
     * Sets toolbar visibility.
     */
    fun setToolbarVisibility() {
        val page = binding.pdfView.currentPage
        if (queryIfPdfIsHorizontal(page) && resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT && !binding.pdfView.isZooming) {
            notChangePage = true
            binding.pdfView.jumpTo(page - 1)
        }
        if (supportActionBar?.isShowing == true) {
            setToolbarVisibilityHide(200L)
        } else {
            setToolbarVisibilityShow()
        }
    }

    private fun queryIfPdfIsHorizontal(page: Int) = binding.pdfView.getPageSize(page).let {
        it.width > it.height
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Timber.d("onCreateOptionsMenu")
        this.menu = menu
        val inflater = menuInflater
        inflater.inflate(R.menu.activity_pdfviewer, menu)
        val shareMenuItem = menu.findItem(R.id.pdf_viewer_share)
        val downloadMenuItem = menu.findItem(R.id.pdf_viewer_download)
        val chatMenuItem = menu.findItem(R.id.pdf_viewer_chat)
        val propertiesMenuItem = menu.findItem(R.id.pdf_viewer_properties)
        val getLinkMenuItem = menu.findItem(R.id.pdf_viewer_get_link)
        getLinkMenuItem.title =
            resources.getQuantityString(sharedR.plurals.label_share_links, 1)
        val renameMenuItem = menu.findItem(R.id.pdf_viewer_rename)
        val hideMenuItem = menu.findItem(R.id.pdf_viewer_hide)
        val unhideMenuItem = menu.findItem(R.id.pdf_viewer_unhide)
        val moveMenuItem = menu.findItem(R.id.pdf_viewer_move)
        val copyMenuItem = menu.findItem(R.id.pdf_viewer_copy)
        val moveToTrashMenuItem = menu.findItem(R.id.pdf_viewer_move_to_trash)
        val removeMenuItem = menu.findItem(R.id.pdf_viewer_remove)
        val removeLinkMenuItem = menu.findItem(R.id.pdf_viewer_remove_link)
        val importMenuItem = menu.findItem(R.id.chat_pdf_viewer_import)
        val saveForOfflineMenuItem = menu.findItem(R.id.chat_pdf_viewer_save_for_offline)
        val chatRemoveMenuItem = menu.findItem(R.id.chat_pdf_viewer_remove)
        if (!inside) {
            propertiesMenuItem.isVisible = false
            chatMenuItem.isVisible = false
            downloadMenuItem.isVisible = false
            getLinkMenuItem.isVisible = false
            renameMenuItem.isVisible = false
            hideMenuItem.isVisible = false
            unhideMenuItem.isVisible = false
            moveMenuItem.isVisible = false
            copyMenuItem.isVisible = false
            moveToTrashMenuItem.isVisible = false
            removeMenuItem.isVisible = false
            removeLinkMenuItem.isVisible = false
            importMenuItem.isVisible = false
            saveForOfflineMenuItem.isVisible = false
            chatRemoveMenuItem.isVisible = false
            shareMenuItem.isVisible = true
        } else {
            if (nC == null) {
                nC = NodeController(this)
            }
            var fromIncoming = false
            if (type == Constants.SEARCH_ADAPTER) {
                fromIncoming = nC!!.nodeComesFromIncoming(megaApi.getNodeByHandle(handle))
            }
            shareMenuItem.isVisible = showShareOption(type, isFolderLink, handle)
            if (type == Constants.OFFLINE_ADAPTER) {
                getLinkMenuItem.isVisible = false
                removeLinkMenuItem.isVisible = false
                propertiesMenuItem.isVisible = true
                downloadMenuItem.isVisible = true
                renameMenuItem.isVisible = false
                hideMenuItem.isVisible = false
                unhideMenuItem.isVisible = false
                moveMenuItem.isVisible = false
                copyMenuItem.isVisible = false
                moveToTrashMenuItem.isVisible = false
                removeMenuItem.isVisible = false
                chatMenuItem.isVisible = false
                importMenuItem.isVisible = false
                saveForOfflineMenuItem.isVisible = false
                chatRemoveMenuItem.isVisible = false
            } else if (type == NodeSourceTypeInt.RUBBISH_BIN_ADAPTER
                || megaApi.isInRubbish(megaApi.getNodeByHandle(handle))
            ) {
                shareMenuItem.isVisible = false
                getLinkMenuItem.isVisible = false
                removeLinkMenuItem.isVisible = false
                propertiesMenuItem.isVisible = true
                downloadMenuItem.isVisible = false
                renameMenuItem.isVisible = false
                hideMenuItem.isVisible = false
                unhideMenuItem.isVisible = false
                moveMenuItem.isVisible = false
                copyMenuItem.isVisible = false
                moveToTrashMenuItem.isVisible = false
                removeMenuItem.isVisible = true
                chatMenuItem.isVisible = false
                importMenuItem.isVisible = false
                saveForOfflineMenuItem.isVisible = false
                chatRemoveMenuItem.isVisible = false
            } else if (type == Constants.SEARCH_ADAPTER && !fromIncoming) {
                val node = megaApi.getNodeByHandle(handle)
                if (node?.isExported == true) {
                    removeLinkMenuItem.isVisible = true
                    getLinkMenuItem.isVisible = false
                } else {
                    removeLinkMenuItem.isVisible = false
                    getLinkMenuItem.isVisible = true
                }
                downloadMenuItem.isVisible = true
                propertiesMenuItem.isVisible = true
                renameMenuItem.isVisible = true
                hideMenuItem.isVisible = false
                unhideMenuItem.isVisible = false
                moveMenuItem.isVisible = true
                copyMenuItem.isVisible = true
                chatMenuItem.isVisible = true
                var parent = megaApi.getNodeByHandle(handle)
                while (megaApi.getParentNode(parent) != null) {
                    parent = megaApi.getParentNode(parent)
                }
                if (parent?.handle != megaApi.rubbishNode?.handle) {
                    moveToTrashMenuItem.isVisible = true
                    removeMenuItem.isVisible = false
                } else {
                    moveToTrashMenuItem.isVisible = false
                    removeMenuItem.isVisible = true
                }
                importMenuItem.isVisible = false
                saveForOfflineMenuItem.isVisible = false
                chatRemoveMenuItem.isVisible = false
            } else if (type == Constants.FROM_CHAT) {
                getLinkMenuItem.isVisible = false
                removeLinkMenuItem.isVisible = false
                propertiesMenuItem.isVisible = false
                renameMenuItem.isVisible = false
                hideMenuItem.isVisible = false
                unhideMenuItem.isVisible = false
                moveMenuItem.isVisible = false
                copyMenuItem.isVisible = false
                moveToTrashMenuItem.isVisible = false
                removeMenuItem.isVisible = false
                chatMenuItem.isVisible = false
                if (!Util.isOnline(this)) {
                    downloadMenuItem.isVisible = false
                    importMenuItem.isVisible = false
                    saveForOfflineMenuItem.isVisible = false
                    chatRemoveMenuItem.isVisible = (msgChat!!.userHandle == megaChatApi.myUserHandle
                            && msgChat!!.isDeletable)
                } else if (node != null) {
                    downloadMenuItem.isVisible = true
                    if (chatC!!.isInAnonymousMode) {
                        importMenuItem.isVisible = false
                        saveForOfflineMenuItem.isVisible = false
                    } else {
                        importMenuItem.isVisible = true
                        saveForOfflineMenuItem.isVisible = true
                    }
                    chatRemoveMenuItem.isVisible =
                        msgChat!!.userHandle == megaChatApi.myUserHandle && msgChat!!.isDeletable
                } else {
                    downloadMenuItem.isVisible = false
                    importMenuItem.isVisible = false
                    saveForOfflineMenuItem.isVisible = false
                    chatRemoveMenuItem.isVisible = false
                }
            } else if (type == Constants.FILE_LINK_ADAPTER || isFolderLink) {
                Timber.d("FILE_LINK_ADAPTER")
                getLinkMenuItem.isVisible = false
                removeLinkMenuItem.isVisible = false
                propertiesMenuItem.isVisible = false
                downloadMenuItem.isVisible = true
                renameMenuItem.isVisible = false
                hideMenuItem.isVisible = false
                unhideMenuItem.isVisible = false
                moveMenuItem.isVisible = false
                copyMenuItem.isVisible = false
                moveToTrashMenuItem.isVisible = false
                removeMenuItem.isVisible = false
                chatMenuItem.isVisible = false
                importMenuItem.isVisible = true
                saveForOfflineMenuItem.isVisible = false
                chatRemoveMenuItem.isVisible = false
            } else if (type == Constants.ZIP_ADAPTER) {
                propertiesMenuItem.isVisible = false
                chatMenuItem.isVisible = false
                downloadMenuItem.isVisible = false
                getLinkMenuItem.isVisible = false
                renameMenuItem.isVisible = false
                hideMenuItem.isVisible = false
                unhideMenuItem.isVisible = false
                moveMenuItem.isVisible = false
                copyMenuItem.isVisible = false
                moveToTrashMenuItem.isVisible = false
                removeMenuItem.isVisible = false
                removeLinkMenuItem.isVisible = false
                importMenuItem.isVisible = false
                saveForOfflineMenuItem.isVisible = false
                chatRemoveMenuItem.isVisible = false
            } else if (type == NodeSourceTypeInt.INCOMING_SHARES_ADAPTER || fromIncoming) {
                propertiesMenuItem.isVisible = true
                chatMenuItem.isVisible = true
                copyMenuItem.isVisible = true
                removeMenuItem.isVisible = false
                importMenuItem.isVisible = false
                saveForOfflineMenuItem.isVisible = false
                hideMenuItem.isVisible = false
                unhideMenuItem.isVisible = false
                chatRemoveMenuItem.isVisible = false
                getLinkMenuItem.isVisible = false
                removeLinkMenuItem.isVisible = false
                downloadMenuItem.isVisible = true
                val node = megaApi.getNodeByHandle(handle)
                when (megaApi.getAccess(node)) {
                    MegaShare.ACCESS_FULL -> {
                        Timber.d("Access FULL")
                        renameMenuItem.isVisible = true
                        moveMenuItem.isVisible = true
                        moveToTrashMenuItem.isVisible = true
                    }

                    MegaShare.ACCESS_READ -> Timber.d("Access read")
                    MegaShare.ACCESS_READWRITE -> {
                        Timber.d("Access read & write")
                        renameMenuItem.isVisible = false
                        moveMenuItem.isVisible = false
                        moveToTrashMenuItem.isVisible = false
                    }

                    else -> {}
                }
            } else if (type == Constants.RECENTS_ADAPTER) {
                val node = megaApi.getNodeByHandle(handle)
                chatRemoveMenuItem.isVisible = false
                removeMenuItem.isVisible = false
                getLinkMenuItem.isVisible = false
                removeLinkMenuItem.isVisible = false
                hideMenuItem.isVisible = false
                unhideMenuItem.isVisible = false
                importMenuItem.isVisible = false
                saveForOfflineMenuItem.isVisible = false
                when (megaApi.getAccess(node)) {
                    MegaShare.ACCESS_READWRITE, MegaShare.ACCESS_READ, MegaShare.ACCESS_UNKNOWN -> {
                        renameMenuItem.isVisible = false
                        moveMenuItem.isVisible = false
                        moveToTrashMenuItem.isVisible = false
                    }

                    MegaShare.ACCESS_FULL, MegaShare.ACCESS_OWNER -> {
                        renameMenuItem.isVisible = true
                        moveMenuItem.isVisible = true
                        moveToTrashMenuItem.isVisible = true
                    }

                    else -> {}
                }
            } else {
                setDefaultOptionsToolbar(menu)
            }
        }

        // After establishing the Options menu, check if read-only properties should be applied
        checkIfShouldApplyReadOnlyState(menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Checks and applies read-only restrictions (unable to Favourite, Rename, Move, or Move to Rubbish Bin)
     * on the Options toolbar if the MegaNode is a Backup node.
     *
     * @param menu The Options Menu
     */
    private fun checkIfShouldApplyReadOnlyState(menu: Menu) {
        val node = megaApi.getNodeByHandle(handle)
        if (node != null && megaApi.isInVault(node)) {
            menu.findItem(R.id.pdf_viewer_rename).isVisible = false
            menu.findItem(R.id.pdf_viewer_move).isVisible = false
            menu.findItem(R.id.pdf_viewer_move_to_trash).isVisible = false
        }
    }

    /**
     * Sets up the default items to be displayed on the Toolbar Menu
     *
     * @param menu Menu object
     */
    private fun setDefaultOptionsToolbar(menu: Menu) {
        val isInSharedItems = type in listOf(
            NodeSourceTypeInt.INCOMING_SHARES_ADAPTER,
            NodeSourceTypeInt.OUTGOING_SHARES_ADAPTER,
            NodeSourceTypeInt.LINKS_ADAPTER
        )
        val downloadMenuItem = menu.findItem(R.id.pdf_viewer_download)
        val chatMenuItem = menu.findItem(R.id.pdf_viewer_chat)
        val propertiesMenuItem = menu.findItem(R.id.pdf_viewer_properties)
        val getLinkMenuItem = menu.findItem(R.id.pdf_viewer_get_link)
        val renameMenuItem = menu.findItem(R.id.pdf_viewer_rename)
        val hideMenuItem = menu.findItem(R.id.pdf_viewer_hide)
        val unhideMenuItem = menu.findItem(R.id.pdf_viewer_unhide)
        val moveMenuItem = menu.findItem(R.id.pdf_viewer_move)
        val copyMenuItem = menu.findItem(R.id.pdf_viewer_copy)
        val moveToTrashMenuItem = menu.findItem(R.id.pdf_viewer_move_to_trash)
        val removeMenuItem = menu.findItem(R.id.pdf_viewer_remove)
        val removeLinkMenuItem = menu.findItem(R.id.pdf_viewer_remove_link)
        val importMenuItem = menu.findItem(R.id.chat_pdf_viewer_import)
        val saveForOfflineMenuItem = menu.findItem(R.id.chat_pdf_viewer_save_for_offline)
        val chatRemoveMenuItem = menu.findItem(R.id.chat_pdf_viewer_remove)
        var node = megaApi.getNodeByHandle(handle)
        val rootParentNode = node?.let { megaApi.getRootParentNode(it) }
        if (node == null) {
            getLinkMenuItem.isVisible = false
            removeLinkMenuItem.isVisible = false
            propertiesMenuItem.isVisible = false
            downloadMenuItem.isVisible = false
            renameMenuItem.isVisible = false
            hideMenuItem.isVisible = false
            unhideMenuItem.isVisible = false
            moveMenuItem.isVisible = false
            copyMenuItem.isVisible = false
            moveToTrashMenuItem.isVisible = false
            removeMenuItem.isVisible = false
            chatMenuItem.isVisible = false
        } else {
            copyMenuItem.isVisible = true
            if (node.isExported) {
                getLinkMenuItem.isVisible = false
                removeLinkMenuItem.isVisible = true
            } else if (type == Constants.CONTACT_FILE_ADAPTER || isFolderLink || type == Constants.VERSIONS_ADAPTER) {
                getLinkMenuItem.isVisible = false
                removeLinkMenuItem.isVisible = false
            } else {
                getLinkMenuItem.isVisible = true
                removeLinkMenuItem.isVisible = false
            }
            if (isFolderLink || type == Constants.VERSIONS_ADAPTER) {
                propertiesMenuItem.isVisible = false
                moveToTrashMenuItem.isVisible = false
                removeMenuItem.isVisible = false
                renameMenuItem.isVisible = false
                hideMenuItem.isVisible = false
                unhideMenuItem.isVisible = false
                moveMenuItem.isVisible = false
                copyMenuItem.isVisible = false
                chatMenuItem.isVisible = false
            } else {
                propertiesMenuItem.isVisible = true
                hideMenuItem.isVisible = false
                unhideMenuItem.isVisible = false
                if (type == Constants.CONTACT_FILE_ADAPTER) {
                    removeMenuItem.isVisible = false
                    node = megaApi.getNodeByHandle(handle)
                    when (megaApi.getAccess(node)) {
                        MegaShare.ACCESS_OWNER, MegaShare.ACCESS_FULL -> {
                            renameMenuItem.isVisible = true
                            moveMenuItem.isVisible = true
                            moveToTrashMenuItem.isVisible = true
                            chatMenuItem.isVisible = true
                        }

                        MegaShare.ACCESS_READWRITE, MegaShare.ACCESS_READ -> {
                            renameMenuItem.isVisible = false
                            moveMenuItem.isVisible = false
                            moveToTrashMenuItem.isVisible = false
                            chatMenuItem.isVisible = false
                        }

                        else -> {}
                    }
                } else {
                    chatMenuItem.isVisible = true
                    renameMenuItem.isVisible = true
                    moveMenuItem.isVisible = true
                    node = megaApi.getNodeByHandle(handle)
                    val handle = node?.handle ?: MegaApiJava.INVALID_HANDLE
                    var parent = megaApi.getNodeByHandle(handle)
                    while (megaApi.getParentNode(parent) != null) {
                        parent = megaApi.getParentNode(parent)
                    }
                    moveToTrashMenuItem.isVisible = true
                    removeMenuItem.isVisible = false
                }
            }
            downloadMenuItem.isVisible = true
        }

        val currentParentNode = megaApi.getParentNode(node)
        val isSensitiveInherited =
            currentParentNode?.let { megaApi.isSensitiveInherited(it) } == true
        val isInShare = rootParentNode?.isInShare == true
        val isPaidAccount = viewModel.uiState.value.accountType?.isPaid == true
        val isBusinessAccountExpired = viewModel.uiState.value.isBusinessAccountExpired
        val isNotInShare =
            !isInSharedItems && !isInShare
        val isNodeInBackups = viewModel.uiState.value.isNodeInBackups

        val shouldShowHideNode = when {
            !isHiddenNodesEnabled || isInShare || isInSharedItems || isNodeInBackups -> false
            isPaidAccount && !isBusinessAccountExpired && ((node != null && node.isMarkedSensitive) || isSensitiveInherited) -> false
            else -> true
        }

        val shouldShowUnhideNode = node != null
                && isHiddenNodesEnabled
                && isNotInShare
                && node.isMarkedSensitive
                && !isSensitiveInherited
                && isPaidAccount
                && !isBusinessAccountExpired
                && !isNodeInBackups

        hideMenuItem.isVisible = shouldShowHideNode
        unhideMenuItem.isVisible = shouldShowUnhideNode

        importMenuItem.isVisible = false
        saveForOfflineMenuItem.isVisible = false
        chatRemoveMenuItem.isVisible = false
    }

    @SuppressLint("NonConstantResourceId")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("onOptionsItemSelected")
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
            }

            R.id.pdf_viewer_share -> {
                if (type == Constants.ZIP_ADAPTER) {
                    FileUtil.shareFile(this, File(uri.toString()), node?.name)
                } else if (type == Constants.OFFLINE_ADAPTER || !inside) {
                    FileUtil.shareWithUri(this, "pdf", uri)
                } else if (type == Constants.FILE_LINK_ADAPTER) {
                    shareLink(this, intent.getStringExtra(Constants.URL_FILE_LINK), node?.name)
                } else {
                    val node = megaApi.getNodeByHandle(handle)
                    shareNode(this, node)
                }
            }

            R.id.pdf_viewer_download -> download()

            R.id.pdf_viewer_chat -> nodeAttachmentViewModel.startAttachNodes(listOf(NodeId(handle)))

            R.id.pdf_viewer_properties -> showPropertiesActivity()

            R.id.pdf_viewer_get_link -> {
                if (showTakenDownNodeActionNotAvailableDialog(
                        megaApi.getNodeByHandle(handle),
                        this
                    )
                ) {
                    return false
                }
                LinksUtil.showGetLinkActivity(this, handle)
            }

            R.id.pdf_viewer_remove_link -> {
                if (showTakenDownNodeActionNotAvailableDialog(
                        megaApi.getNodeByHandle(handle),
                        this
                    )
                ) {
                    return false
                }
                showRemoveLink()
            }

            R.id.pdf_viewer_rename -> {
                showRenameNodeDialog(this, megaApi.getNodeByHandle(handle), this, this)
            }

            R.id.pdf_viewer_hide -> {
                Analytics.tracker.trackEvent(DocumentPreviewHideNodeMenuItemEvent)
                handleHideNodeClick(playingHandle = handle)
            }

            R.id.pdf_viewer_unhide -> {
                viewModel.hideOrUnhideNode(
                    nodeId = NodeId(handle),
                    hide = false,
                )
                val message =
                    resources.getQuantityString(sharedR.plurals.unhidden_nodes_result_message, 1, 1)
                Util.showSnackbar(this, message)

                RunOnUIThreadUtils.runDelay(500L) {
                    item.isVisible = false
                    menu?.findItem(R.id.pdf_viewer_hide)?.isVisible = true
                }
            }

            R.id.pdf_viewer_move -> showMove()

            R.id.pdf_viewer_copy -> showCopy()

            R.id.pdf_viewer_move_to_trash, R.id.pdf_viewer_remove -> {
                moveToRubbishOrRemove(handle, this, this)
            }

            R.id.chat_pdf_viewer_import -> importNode()

            R.id.chat_pdf_viewer_save_for_offline -> {
                if (getStorageState() == StorageState.PayWall) {
                    AlertsAndWarnings.showOverDiskQuotaPaywallWarning()
                } else {
                    viewModel.saveChatNodeToOffline(
                        chatId = chatId,
                        messageId = msgId
                    )
                }
            }

            R.id.chat_pdf_viewer_remove -> {
                if (msgChat != null && chatId != -1L) {
                    showConfirmationDeleteNode(chatId, msgChat)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun importNode() {
        node?.let {
            Timber.d("importNode")
            val intent = Intent(this, FileExplorerActivity::class.java)
            intent.action = FileExplorerActivity.ACTION_PICK_IMPORT_FOLDER
            startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_IMPORT_FOLDER)
        }
    }

    private fun showConfirmationDeleteNode(chatId: Long, message: MegaChatMessage?) {
        Timber.d("showConfirmationDeleteNode")
        val dialogClickListener =
            DialogInterface.OnClickListener { _: DialogInterface?, which: Int ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        if (chatC == null) {
                            chatC = ChatController(this@PdfViewerActivity)
                        }
                        chatC?.deleteMessage(message, chatId)
                        isDeleteDialogShow = false
                        finish()
                    }

                    DialogInterface.BUTTON_NEGATIVE ->                     //No button clicked
                        isDeleteDialogShow = false
                }
            }
        val builder =
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
        builder.setMessage(R.string.confirmation_delete_one_attachment)
        builder.setPositiveButton(R.string.context_remove, dialogClickListener)
            .setNegativeButton(sharedR.string.general_dialog_cancel_button, dialogClickListener)
            .show()
        isDeleteDialogShow = true
        builder.setOnDismissListener { isDeleteDialogShow = false }
    }

    private fun showCopy() {
        Timber.d("showCopy")
        val handleList = ArrayList<Long>()
        handleList.add(handle)
        val intent = Intent(this, FileExplorerActivity::class.java)
        intent.action = FileExplorerActivity.ACTION_PICK_COPY_FOLDER
        val longArray = LongArray(handleList.size)
        for (i in handleList.indices) {
            longArray[i] = handleList[i]
        }
        intent.putExtra(Constants.INTENT_EXTRA_KEY_COPY_FROM, longArray)
        startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_FOLDER_TO_COPY)
    }

    private fun showMove() {
        Timber.d("showMove")
        val handleList = ArrayList<Long>()
        handleList.add(handle)
        val intent = Intent(this, FileExplorerActivity::class.java)
        intent.action = FileExplorerActivity.ACTION_PICK_MOVE_FOLDER
        val longArray = LongArray(handleList.size)
        for (i in handleList.indices) {
            longArray[i] = handleList[i]
        }
        intent.putExtra(Constants.INTENT_EXTRA_KEY_MOVE_FROM, longArray)
        startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_FOLDER_TO_MOVE)
    }

    private fun showPropertiesActivity() {
        val intent = if (isOffLine) {
            Intent(this, OfflineFileInfoActivity::class.java).apply {
                putExtra(HANDLE, handle.toString())
            }
        } else {
            Intent(this, FileInfoActivity::class.java).also { i ->
                val node = megaApi.getNodeByHandle(handle)
                i.putExtra("handle", node?.handle)
                i.putExtra(Constants.NAME, node?.name)
                if (nC == null) {
                    nC = NodeController(this)
                }
                var fromIncoming = false
                if (type == Constants.SEARCH_ADAPTER || type == Constants.RECENTS_ADAPTER) {
                    fromIncoming = nC?.nodeComesFromIncoming(node) ?: false
                }
                if (type == NodeSourceTypeInt.INCOMING_SHARES_ADAPTER || fromIncoming) {
                    i.putExtra("from", Constants.FROM_INCOMING_SHARES)
                    i.putExtra(Constants.INTENT_EXTRA_KEY_FIRST_LEVEL, false)
                } else if (type == NodeSourceTypeInt.BACKUPS_ADAPTER) {
                    i.putExtra("from", Constants.FROM_BACKUPS)
                }
            }
        }
        startActivity(intent)
        renamed = false
    }

    private fun showRemoveLink() {
        val removeLinkDialog: AlertDialog
        val builder =
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
        val dialogLayout = layoutInflater.inflate(R.layout.dialog_link, null)
        val url = dialogLayout.findViewById<View>(R.id.dialog_link_link_url) as TextView
        val key = dialogLayout.findViewById<View>(R.id.dialog_link_link_key) as TextView
        val symbol = dialogLayout.findViewById<View>(R.id.dialog_link_symbol) as TextView
        val removeText = dialogLayout.findViewById<View>(R.id.dialog_link_text_remove) as TextView
        (removeText.layoutParams as RelativeLayout.LayoutParams).setMargins(
            Util.scaleWidthPx(
                25,
                outMetrics
            ), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(10, outMetrics), 0
        )
        url.isVisible = false
        key.isVisible = false
        symbol.isVisible = false
        removeText.isVisible = true
        removeText.text = getString(R.string.context_remove_link_warning_text)
        val scaleW = Util.getScaleW(outMetrics, resources.displayMetrics.density)
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            removeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10 * scaleW)
        } else {
            removeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15 * scaleW)
        }
        builder.setView(dialogLayout)
        builder.setPositiveButton(getString(R.string.context_remove)) { _: DialogInterface?, _: Int ->
            megaApi.disableExport(megaApi.getNodeByHandle(handle), this@PdfViewerActivity)
        }
        builder.setNegativeButton(getString(sharedR.string.general_dialog_cancel_button)) { _: DialogInterface?, _: Int -> }
        removeLinkDialog = builder.create()
        removeLinkDialog.show()
    }

    private suspend fun updateFile() {
        if (pdfFileName != null && handle != -1L) {
            val file = withContext(ioDispatcher) {
                megaApi.getNodeByHandle(handle)
            }
            if (file != null) {
                Timber.d("Pdf File: $pdfFileName node file: ${file.name}")
                if (pdfFileName != file.name) {
                    Timber.d("Update File")
                    pdfFileName = file.name
                    if (supportActionBar != null) {
                        binding.toolbarPdfViewer.isVisible = true
                        setSupportActionBar(binding.toolbarPdfViewer)
                    }
                    supportActionBar?.title = " "
                    title = pdfFileName
                    binding.pdfViewerFileName.text = pdfFileName
                    invalidateOptionsMenu()
                    val localPath = FileUtil.getLocalFile(file)
                    if (localPath != null) {
                        val mediaFile = File(localPath)
                        uri = runCatching { FileUtil.getUriForFile(this@PdfViewerActivity, mediaFile) }.getOrNull()

                        if (uri == null) {
                            initStreaming(file)
                        }
                    } else {
                        initStreaming(file)
                    }
                    renamed = true
                }
            }
        }
    }

    private suspend fun initStreaming(node: MegaNode) {
        withContext(ioDispatcher) {
            megaApi.initStreaming()
            uri = megaApi.httpServerGetLocalLink(node)?.let { Uri.parse(it) }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        Timber.d("onActivityResult: ${requestCode}____$resultCode")
        if (intent == null) {
            return
        }
        if (requestCode == Constants.REQUEST_CODE_SELECT_FOLDER_TO_MOVE && resultCode == RESULT_OK) {
            if (!Util.isOnline(this)) {
                showSnackbar(
                    SNACKBAR_TYPE,
                    getString(R.string.error_server_connection_problem),
                    -1
                )
                return
            }
            val toHandle = intent.getLongExtra(Constants.INTENT_EXTRA_KEY_MOVE_TO, 0)
            val temp: AlertDialog
            try {
                temp = createProgressDialog(this, getString(R.string.context_moving))
                temp.show()
            } catch (e: Exception) {
                return
            }
            statusDialog = temp
            node?.let { megaNode ->
                viewModel.moveNode(nodeHandle = megaNode.handle, newParentHandle = toHandle)
            }
        } else if (requestCode == Constants.REQUEST_CODE_SELECT_FOLDER_TO_COPY && resultCode == RESULT_OK) {
            if (!Util.isOnline(this)) {
                showSnackbar(
                    SNACKBAR_TYPE,
                    getString(R.string.error_server_connection_problem),
                    -1
                )
                return
            }
            val toHandle = intent.getLongExtra(Constants.INTENT_EXTRA_KEY_COPY_TO, 0)
            val temp: AlertDialog
            try {
                temp = createProgressDialog(this, getString(R.string.context_copying))
                temp.show()
            } catch (e: Exception) {
                return
            }
            statusDialog = temp
            node?.let { megaNode ->
                viewModel.copyNode(nodeHandle = megaNode.handle, newParentHandle = toHandle)
            }
        } else if (requestCode == Constants.REQUEST_CODE_SELECT_IMPORT_FOLDER && resultCode == RESULT_OK) {
            Timber.d("REQUEST_CODE_SELECT_IMPORT_FOLDER OK")
            if (!Util.isOnline(this)) {
                try {
                    statusDialog?.dismiss()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                showSnackbar(
                    SNACKBAR_TYPE,
                    getString(R.string.error_server_connection_problem),
                    -1
                )
                return
            }
            val toHandle = intent.getLongExtra(Constants.INTENT_EXTRA_KEY_IMPORT_TO, 0)
            node?.let { megaNode ->
                if (fromChat)
                    viewModel.importChatNode(
                        chatId = chatId,
                        messageId = msgId,
                        newParentHandle = NodeId(toHandle)
                    )
                else
                    viewModel.copyNode(
                        nodeHandle = megaNode.handle,
                        newParentHandle = toHandle
                    )
            }
        }
    }

    override fun onPageChanged(page: Int, pageCount: Int) {
        Timber.d("page: $page, pageCount: $pageCount")
        if (!notChangePage) {
            currentPage = page + 1
            title = "$pdfFileName $currentPage / $pageCount"
        } else {
            notChangePage = false
        }
    }

    override fun onPageError(page: Int, t: Throwable) {
        Timber.e("Cannot load page $page")
    }

    override fun loadComplete(nbPages: Int) {
        defaultScrollHandle?.setTotalPages(nbPages)
        val meta = binding.pdfView.documentMeta
        Timber.d("Title = ${meta.title}")
        Timber.d("Author = ${meta.author}")
        Timber.d("Subject = ${meta.subject}")
        Timber.d("Keywords = ${meta.keywords}")
        Timber.d("Creator = ${meta.creator}")
        Timber.d("Producer = ${meta.producer}")
        Timber.d("Creation Date = ${meta.creationDate}")
        Timber.d("Mod. Date = ${meta.modDate}")
        printBookmarksTree(binding.pdfView.tableOfContents, "-")
        handler?.postDelayed({ if (isToolbarVisible) setToolbarVisibilityHide(200L) }, 2000)
    }

    @SuppressLint("DefaultLocale")
    private fun printBookmarksTree(tree: List<Bookmark>, sep: String) {
        for (b in tree) {
            Timber.d("$sep ${b.title}, p ${b.pageIdx}")
            if (b.hasChildren()) {
                printBookmarksTree(b.children, "$sep-")
            }
        }
    }

    private fun getFileName(uri: Uri?): String? {
        if (uri == null || uri.scheme == null) {
            Timber.w("URI is null")
            return null
        }
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                contentResolver.query(uri, null, null, null, null).use { cursor ->
                    if (cursor != null && cursor.moveToFirst()) {
                        result =
                            cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Exception getting PDF file name.")
            }
        }
        if (result == null) {
            result = uri.lastPathSegment
        }
        return if (result != null) FileUtil.addPdfFileExtension(result) else null
    }

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {}

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {}

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.d("onRequestFinish")
        if (request.type == MegaRequest.TYPE_LOGIN) {
            if (e.errorCode != MegaError.API_OK) {
                Timber.w("Login failed with error code: ${e.errorCode}")
            } else {
                //LOGIN OK
                lifecycleScope.launch {
                    runCatching {
                        saveAccountCredentialsUseCase()
                    }.onFailure {
                        Timber.e(it)
                    }
                }
                Timber.d("Logged in with session")
                Timber.d("Setting account auth token for folder links.")
                megaApiFolder.accountAuth = megaApi.accountAuth
                megaApi.fetchNodes(this)

                // Get cookies settings after login.
                getInstance().checkEnabledCookies()
            }
        } else if (request.type == MegaRequest.TYPE_FETCH_NODES) {
            if (e.errorCode == MegaError.API_OK) {
                lifecycleScope.launch {
                    runCatching {
                        saveAccountCredentialsUseCase()
                    }.onFailure {
                        Timber.e(it)
                    }
                }
                download()
            }
        }
    }

    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.w("onRequestTemporaryError")
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume")
        if (!isOffLine && !fromChat && !isFolderLink && type != Constants.FILE_LINK_ADAPTER && type != Constants.ZIP_ADAPTER) {
            lifecycleScope.launch {
                val node = withContext(ioDispatcher) {
                    megaApi.getNodeByHandle(handle)
                }
                if (node == null && inside && !fromDownload) {
                    finish()
                    return@launch
                }
                updateFile()
            }
        }
    }

    override fun onDestroy() {
        Timber.d("onDestroy()")
        binding.pdfView.recycle()
        val needStopHttpServer =
            intent.getBooleanExtra(ExtraConstant.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, false)
        applicationScope.launch {
            megaApi.removeTransferListener(this@PdfViewerActivity)
            megaApi.removeGlobalListener(this@PdfViewerActivity)
            if (needStopHttpServer) {
                megaApi.httpServerStop()
            }
            if (needStopHttpServer) {
                megaApiFolder.httpServerStop()
            }
            Timber.d("PdfViewerActivity::HttpServerStop")
        }
        handler?.removeCallbacksAndMessages(null)
        dismissAlertDialogIfExists(takenDownDialog)
        super.onDestroy()
    }

    override fun onTransferStart(api: MegaApiJava, transfer: MegaTransfer) {}

    override fun onTransferFinish(api: MegaApiJava, transfer: MegaTransfer, e: MegaError) {}

    override fun onTransferUpdate(api: MegaApiJava, transfer: MegaTransfer) {}

    override fun onTransferTemporaryError(api: MegaApiJava, transfer: MegaTransfer, e: MegaError) {
        if (e.errorCode == MegaError.API_EOVERQUOTA) {
            if (transfer.isForeignOverquota.not() && e.value != 0L) {
                Timber.w("TRANSFER OVERQUOTA ERROR: ${e.errorCode}")
                viewModel.broadcastTransferOverQuota()
            }
        } else if (e.errorCode == MegaError.API_EBLOCKED && !isAlertDialogShown(takenDownDialog)) {
            takenDownDialog = showTakenDownAlert(this)
        }
    }

    override fun onTransferData(
        api: MegaApiJava,
        transfer: MegaTransfer,
        buffer: ByteArray,
    ): Boolean = false

    override fun onFolderTransferUpdate(
        api: MegaApiJava,
        transfer: MegaTransfer,
        stage: Int,
        folderCount: Long,
        createdFolderCount: Long,
        fileCount: Long,
        currentFolder: String?,
        currentFileLeafName: String?,
    ) {
    }

    private fun handleHideNodeClick(playingHandle: Long) {
        var isPaid: Boolean
        var isHiddenNodesOnboarded: Boolean
        var isBusinessAccountExpired: Boolean
        with(viewModel.uiState.value) {
            isPaid = this.accountType?.isPaid ?: false
            isHiddenNodesOnboarded = this.isHiddenNodesOnboarded
            isBusinessAccountExpired = this.isBusinessAccountExpired
        }

        if (!isPaid || isBusinessAccountExpired) {
            val intent = HiddenNodesOnboardingActivity.createScreen(
                context = this,
                isOnboarding = false,
            )
            hiddenNodesOnboardingLauncher.launch(intent)
            this.overridePendingTransition(0, 0)
        } else if (isHiddenNodesOnboarded) {
            viewModel.hideOrUnhideNode(
                nodeId = NodeId(playingHandle),
                hide = true,
            )
            val message = resources.getQuantityString(R.plurals.hidden_nodes_result_message, 1, 1)
            Util.showSnackbar(this, message)
        } else {
            tempNodeId = NodeId(longValue = playingHandle)
            showHiddenNodesOnboarding()
        }
    }

    private fun showHiddenNodesOnboarding() {
        viewModel.setHiddenNodesOnboarded()

        val intent = HiddenNodesOnboardingActivity.createScreen(
            context = this,
            isOnboarding = true,
        )
        hiddenNodesOnboardingLauncher.launch(intent)
        this.overridePendingTransition(0, 0)
    }

    private val hiddenNodesOnboardingLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleHiddenNodesOnboardingResult,
        )

    private fun handleHiddenNodesOnboardingResult(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK) return

        viewModel.hideOrUnhideNode(
            nodeId = NodeId(tempNodeId?.longValue ?: 0),
            hide = true,
        )

        val message =
            resources.getQuantityString(
                R.plurals.hidden_nodes_result_message,
                1,
                1,
            )
        Util.showSnackbar(this, message)

        RunOnUIThreadUtils.runDelay(500L) {
            menu?.findItem(R.id.pdf_viewer_hide)?.isVisible = false
            menu?.findItem(R.id.pdf_viewer_unhide)?.isVisible = true
        }
    }

    companion object {

        /**
         * True if the pdf file is loading, false otherwise.
         */
        @JvmField
        var loading = true
    }

    override fun onPause() {
        super.onPause()
        viewModel.setOrUpdateLastPageViewed(currentPage.toLong())
    }
}