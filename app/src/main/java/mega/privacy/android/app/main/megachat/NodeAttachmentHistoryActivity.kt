package mega.privacy.android.app.main.megachat

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.primitives.Longs
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.activities.contract.NameCollisionActivityContract
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_ERROR_COPYING_NODES
import mega.privacy.android.app.constants.BroadcastConstants.ERROR_MESSAGE_TEXT
import mega.privacy.android.app.extensions.consumeInsetsWithToolbar
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.StoreDataBeforeForward
import mega.privacy.android.app.listeners.CreateChatListener
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.main.listeners.MultipleForwardChatProcessor
import mega.privacy.android.app.main.megachat.chatAdapters.NodeAttachmentHistoryAdapter
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.nodeattachment.NodeAttachmentBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.nodeattachment.NodeAttachmentBottomSheetDialogFragment.Companion.newInstance
import mega.privacy.android.app.presentation.chat.NodeAttachmentHistoryViewModel
import mega.privacy.android.app.presentation.chat.model.MediaPlayerOpenedErrorState
import mega.privacy.android.app.presentation.copynode.CopyRequestState
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper
import mega.privacy.android.app.presentation.extensions.getStorageState
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity.Companion.createSecondaryIntent
import mega.privacy.android.app.presentation.imagepreview.fetcher.ChatImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity
import mega.privacy.android.app.presentation.settings.model.StorageTargetPreference
import mega.privacy.android.app.presentation.transfers.starttransfer.StartDownloadViewModel
import mega.privacy.android.app.presentation.transfers.starttransfer.view.createStartTransferView
import mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.ColorUtils.setImageViewAlphaIfDark
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils.checkNotificationsPermission
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.chat.ChatFile
import mega.privacy.android.navigation.MegaNavigator
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatNodeHistoryListenerInterface
import nz.mega.sdk.MegaChatPeerList
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRequestListenerInterface
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaUser
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
internal class NodeAttachmentHistoryActivity : PasscodeActivity(), MegaChatRequestListenerInterface,
    MegaChatNodeHistoryListenerInterface, StoreDataBeforeForward<ArrayList<MegaChatMessage?>?>,
    SnackbarShower {

    @Inject
    lateinit var copyRequestMessageMapper: CopyRequestMessageMapper

    @Inject
    lateinit var megaNavigator: MegaNavigator

    private val viewModel by viewModels<NodeAttachmentHistoryViewModel>()
    private val startDownloadViewModel by viewModels<StartDownloadViewModel>()

    var actionBar: ActionBar? = null
    private var materialToolBar: MaterialToolbar? = null

    var container: RelativeLayout? = null
    var listView: RecyclerView? = null
    var mLayoutManager: LinearLayoutManager? = null
//    var emptyLayout: RelativeLayout? = null
//    var emptyTextView: TextView? = null
//    var emptyImageView: ImageView? = null

//    var importIcon: MenuItem? = null
//    private var thumbViewMenuItem: MenuItem? = null

    var messages: ArrayList<MegaChatMessage>? = null
    private var bufferMessages: ArrayList<MegaChatMessage>? = null

    @JvmField
    var chatRoom: MegaChatRoom? = null

    var adapter: NodeAttachmentHistoryAdapter? = null
    var scrollingUp: Boolean = false
    var getMoreHistory: Boolean = false
    var isLoadingHistory: Boolean = false

    private var actionMode: ActionMode? = null

    var statusDialog: AlertDialog? = null

//    var selectMenuItem: MenuItem? = null
//    var unSelectMenuItem: MenuItem? = null

    var handler: Handler? = null
    var stateHistory: Int = 0

    @JvmField
    var chatId: Long = -1
    private var selectedMessageId: Long = -1

    var chatC: ChatController? = null

    private var myChatFilesFolder: MegaNode? = null
    private var preservedMessagesSelected: ArrayList<MegaChatMessage>? = null
    private var preservedMessagesToImport: ArrayList<MegaChatMessage>? = null

    private var bottomSheetDialogFragment: NodeAttachmentBottomSheetDialogFragment? = null

    private val nameCollisionActivityLauncher = registerForActivityResult(
        NameCollisionActivityContract()
    ) { result: String? ->
        if (result != null) {
            showSnackbar(
                Constants.SNACKBAR_TYPE,
                result,
                MegaApiJava.INVALID_HANDLE
            )
        }
    }

    private val errorCopyingNodesReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BROADCAST_ACTION_ERROR_COPYING_NODES != intent.action) {
                return
            }

            removeProgressDialog()
            showSnackbar(Constants.SNACKBAR_TYPE, intent.getStringExtra(ERROR_MESSAGE_TEXT))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)


        if (shouldRefreshSessionDueToSDK() || shouldRefreshSessionDueToKarere()) {
            return
        }

        chatC = ChatController(this)

        megaChatApi.addNodeHistoryListener(chatId, this)

        handler = Handler()

        registerSdkAppropriateReceiver(
            IntentFilter(BROADCAST_ACTION_ERROR_COPYING_NODES),
            errorCopyingNodesReceiver,
        )
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_node_history)
        materialToolBar = findViewById(R.id.toolbar_node_history)
        materialToolBar?.let { this.consumeInsetsWithToolbar(customToolbar = it) }
        addStartDownloadTransferView()

        if (savedInstanceState != null) {
            chatId = savedInstanceState.getLong(CHAT_ID_KEY, -1)
        }

        //Set toolbar
        setSupportActionBar(materialToolBar)
        actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.setDisplayShowHomeEnabled(true)

        actionBar?.title = getString(R.string.title_chat_shared_files_info)

        container = findViewById(R.id.node_history_main_layout)
        val emptyLayout = findViewById<RelativeLayout>(R.id.empty_layout_node_history)
        val emptyTextView = findViewById<TextView>(R.id.empty_text_node_history)
        val emptyImageView = findViewById<ImageView>(R.id.empty_image_view_node_history)

        setImageViewAlphaIfDark(this, emptyImageView, ColorUtils.DARK_IMAGE_ALPHA)

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            emptyImageView.setImageResource(R.drawable.contacts_empty_landscape)
        } else {
            emptyImageView.setImageResource(R.drawable.ic_empty_contacts)
        }

        var textToShow = String.format(getString(R.string.context_empty_shared_files))
        try {
            textToShow = textToShow.replace(
                "[A]",
                "<font color=\'" + getColorHexString(this, R.color.grey_900_grey_100) + "\'>"
            )
            textToShow = textToShow.replace("[/A]", "</font>")
            textToShow = textToShow.replace(
                "[B]",
                "<font color=\'" + getColorHexString(this, R.color.grey_300_grey_600) + "\'>"
            )
            textToShow = textToShow.replace("[/B]", "</font>")
        } catch (ignored: Exception) {
        }
        val result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY)
        emptyTextView.text = result

        listView = findViewById(R.id.node_history_list_view)
        listView?.addItemDecoration(SimpleDividerItemDecoration(this))
        mLayoutManager = LinearLayoutManager(this)
        mLayoutManager?.orientation = LinearLayoutManager.VERTICAL
        listView?.setLayoutManager(mLayoutManager)
        listView?.setItemAnimator(Util.noChangeRecyclerViewItemAnimator())

        listView?.setClipToPadding(false)
        listView?.setHasFixedSize(true)

        listView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (stateHistory != MegaChatApi.SOURCE_NONE) {
                    scrollingUp = if (dy > 0) {
                        // Scrolling up
                        true
                    } else {
                        // Scrolling down
                        false
                    }

                    if (scrollingUp) {
                        val pos = mLayoutManager?.findFirstVisibleItemPosition() ?: 0

                        if (pos <= NUMBER_MESSAGES_BEFORE_LOAD && getMoreHistory) {
                            Timber.d("DE->loadAttachments:scrolling down")
                            isLoadingHistory = true
                            stateHistory =
                                megaChatApi.loadAttachments(chatId, NUMBER_MESSAGES_TO_LOAD)
                            getMoreHistory = false
                        }
                    }
                }
                checkScroll()
            }
        })


        val extras = intent.extras
        if (extras != null) {
            if (chatId == -1L) {
                chatId = extras.getLong(CHAT_ID_KEY)
            }

            chatRoom = megaChatApi.getChatRoom(chatId)

            if (chatRoom != null) {
                messages = ArrayList()
                bufferMessages = ArrayList()

                if (messages?.isNotEmpty() == true) {
                    emptyLayout.visibility = View.GONE
                    listView?.visibility = View.VISIBLE
                } else {
                    emptyLayout.visibility = View.VISIBLE
                    listView?.visibility = View.GONE
                }

                val resultOpen = megaChatApi.openNodeHistory(chatId, this)
                if (resultOpen) {
                    Timber.d("Node history opened correctly")

                    messages = ArrayList()

                    if (adapter == null) {
                        adapter = NodeAttachmentHistoryAdapter(this, messages, listView)
                    }

                    listView?.setAdapter(adapter)
                    adapter?.isMultipleSelect = false

                    adapter?.setMessages(messages)

                    isLoadingHistory = true
                    Timber.d("A->loadAttachments")
                    stateHistory = megaChatApi.loadAttachments(chatId, NUMBER_MESSAGES_TO_LOAD)
                }
            } else {
                Timber.e("ERROR: node is NULL")
            }
        }

        // Observe snackbar message event
        this.collectFlow(
            viewModel.snackbarMessageEvent,
            Lifecycle.State.STARTED
        ) { messageId: Int? ->
            if (messageId == null) return@collectFlow
            showSnackbar(
                Constants.SNACKBAR_TYPE,
                getString(messageId),
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
            )
            viewModel.onSnackbarMessageConsumed()
        }

        // Observe event to save chat file to offline
        this.collectFlow(
            viewModel.startChatFileOfflineDownloadEvent, Lifecycle.State.STARTED
        ) { chatFile: ChatFile? ->
            if (chatFile == null) return@collectFlow
            startDownloadViewModel.onSaveOfflineClicked(chatFile)
            viewModel.onStartChatFileOfflineDownloadEventConsumed()
        }

        // Observe copy request result
        this.collectFlow(
            viewModel.copyResultFlow,
            Lifecycle.State.STARTED
        ) { copyResult: CopyRequestState? ->
            if (copyResult == null) return@collectFlow
            dismissAlertDialogIfExists(statusDialog)

            val copyThrowable = copyResult.error
            if (copyThrowable != null) {
                manageCopyMoveException(copyThrowable)
            }

            showSnackbar(
                Constants.SNACKBAR_TYPE, if (copyResult.result != null)
                    copyRequestMessageMapper.invoke(copyResult.result)
                else
                    getString(R.string.import_success_error),
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
            )
            viewModel.copyResultConsumed()
        }

        // Observe node collision result
        this.collectFlow(
            viewModel.collisionsFlow,
            Lifecycle.State.STARTED
        ) { collisions: List<NameCollision>? ->
            if (collisions == null) return@collectFlow
            dismissAlertDialogIfExists(statusDialog)
            if (collisions.isNotEmpty()) {
                nameCollisionActivityLauncher.launch(ArrayList(collisions))
                viewModel.nodeCollisionsConsumed()
            }
            Unit
        }

        this.collectFlow(
            viewModel.mediaPlayerOpenedErrorFlow,
            Lifecycle.State.STARTED
        ) { errorState: MediaPlayerOpenedErrorState? ->
            if (errorState == null) return@collectFlow
            Timber.w("No available Intent")
            showNodeAttachmentBottomSheet(errorState.message, errorState.position)
            viewModel.updateMediaPlayerOpenedError(null)
        }
    }


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerSdkAppropriateReceiver(
        filter: IntentFilter, broadcastReceiver: BroadcastReceiver,
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.registerReceiver(
                    this, broadcastReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
                )
            } else {
                super.registerReceiver(broadcastReceiver, filter)
            }
        } catch (e: IllegalStateException) {
            Timber.e(e, "IllegalStateException registering receiver")
        }
    }

    private fun addStartDownloadTransferView() {
        val root = findViewById<ViewGroup>(R.id.node_history_main_layout)
        root.addView(
            createStartTransferView(
                this,
                startDownloadViewModel.state,
                {
                    startDownloadViewModel.consumeDownloadEvent()
                },
                {
                    megaNavigator.openSettings(this, StorageTargetPreference)
                },
                { }
            )
        )
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        super.onDestroy()
        unregisterReceiver(errorCopyingNodesReceiver)

        megaChatApi.removeNodeHistoryListener(chatId, this)
        megaChatApi.closeNodeHistory(chatId, null)
        if (handler != null) {
            handler?.removeCallbacksAndMessages(null)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu items for use in the action bar

        val inflater = menuInflater
        inflater.inflate(R.menu.activity_node_history, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_select).setVisible(messages?.isNotEmpty() == true)

        menu.findItem(R.id.action_unselect).setVisible(false)
        menu.findItem(R.id.action_grid).setVisible(false)

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle presses on the action bar items
        val itemId = item.itemId
        when (itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }

            R.id.action_select -> {
                selectAll()
                return true
            }

            R.id.action_grid -> {
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    fun activateActionMode() {
        Timber.d("activateActionMode")
        if (adapter?.isMultipleSelect == false) {
            adapter?.isMultipleSelect = true
            notifyDataSetChanged()
            actionMode = startSupportActionMode(ActionBarCallBack())
        }
    }

    // Clear all selected items
    private fun clearSelections() {
        if (adapter?.isMultipleSelect == true) {
            adapter?.clearSelections()
        }
    }

    fun selectAll() {
        Timber.d("selectAll")
        if (adapter != null) {
            if (adapter?.isMultipleSelect == true) {
                adapter?.selectAll()
            } else {
                adapter?.isMultipleSelect = true
                adapter?.selectAll()

                actionMode = startSupportActionMode(ActionBarCallBack())
            }
            Handler(Looper.getMainLooper()).post { this.updateActionModeTitle() }
        }
    }

    fun itemClick(position: Int) {
        Timber.d("Position: %s", position)
        megaChatApi.signalPresenceActivity()

        if (position < (messages?.size ?: 0)) {
            val m = messages?.get(position)

            if (adapter?.isMultipleSelect == true) {
                adapter?.toggleSelection(position)
                if (adapter?.selectedMessages?.isNotEmpty() == true) {
                    updateActionModeTitle()
                }
            } else {
                if (m != null) {
                    val nodeList = m.megaNodeList
                    if (nodeList.size() == 1) {
                        val node = nodeList[0]

                        if (typeForName(node.name).isImage) {
                            if (node.hasPreview()) {
                                Timber.d("Show full screen viewer")
                                showFullScreenViewer(m.msgId)
                            } else {
                                Timber.d("Image without preview - show node attachment panel for one node")
                                showNodeAttachmentBottomSheet(m, position)
                            }
                        } else if (typeForName(node.name).isVideoMimeType || typeForName(node.name).isAudio) {
                            viewModel.openMediaPlayer(
                                this,
                                node.handle,
                                m,
                                chatId,
                                node.name,
                                position
                            )
                        } else if (typeForName(node.name).isPdf) {
                            Timber.d("isFile:isPdf")
                            val mimeType = typeForName(node.name).type
                            Timber.d("FILE HANDLE: %d, TYPE: %s", node.handle, mimeType)
                            val pdfIntent = Intent(
                                this,
                                PdfViewerActivity::class.java
                            )
                            pdfIntent.putExtra("inside", true)
                            pdfIntent.putExtra("adapterType", Constants.FROM_CHAT)
                            pdfIntent.putExtra("msgId", m.msgId)
                            pdfIntent.putExtra(CHAT_ID_KEY, chatId)

                            pdfIntent.putExtra("FILENAME", node.name)

                            val localPath = FileUtil.getLocalFile(node)
                            if (localPath != null) {
                                val mediaFile = File(localPath)
                                if (localPath.contains(Environment.getExternalStorageDirectory().path)) {
                                    Timber.d("File Provider Option")
                                    val mediaFileUri = FileProvider.getUriForFile(
                                        this, Constants.AUTHORITY_STRING_FILE_PROVIDER, mediaFile
                                    )
                                    if (mediaFileUri == null) {
                                        Timber.e("ERROR: NULL media file Uri")
                                        showSnackbar(
                                            Constants.SNACKBAR_TYPE,
                                            getString(R.string.general_text_error)
                                        )
                                    } else {
                                        pdfIntent.setDataAndType(
                                            mediaFileUri,
                                            typeForName(node.name).type
                                        )
                                    }
                                } else {
                                    val mediaFileUri = Uri.fromFile(mediaFile)
                                    if (mediaFileUri == null) {
                                        Timber.e("ERROR: NULL media file Uri")
                                        showSnackbar(
                                            Constants.SNACKBAR_TYPE,
                                            getString(R.string.general_text_error)
                                        )
                                    } else {
                                        pdfIntent.setDataAndType(
                                            mediaFileUri,
                                            typeForName(node.name).type
                                        )
                                    }
                                }
                                pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            } else {
                                Timber.w("Local Path NULL")
                                if (viewModel.isOnline()) {
                                    if (megaApi.httpServerIsRunning() == 0) {
                                        megaApi.httpServerStart()
                                        pdfIntent.putExtra(
                                            Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER,
                                            true
                                        )
                                    } else {
                                        Timber.w("ERROR: HTTP server already running")
                                    }
                                    val url = megaApi.httpServerGetLocalLink(node)
                                    if (url != null) {
                                        val parsedUri = Uri.parse(url)
                                        if (parsedUri != null) {
                                            pdfIntent.setDataAndType(parsedUri, mimeType)
                                        } else {
                                            Timber.e("ERROR: HTTP server get local link")
                                            showSnackbar(
                                                Constants.SNACKBAR_TYPE,
                                                getString(R.string.general_text_error)
                                            )
                                        }
                                    } else {
                                        Timber.e("ERROR: HTTP server get local link")
                                        showSnackbar(
                                            Constants.SNACKBAR_TYPE,
                                            getString(R.string.general_text_error)
                                        )
                                    }
                                } else {
                                    showSnackbar(
                                        Constants.SNACKBAR_TYPE,
                                        getString(R.string.error_server_connection_problem) + ". " + getString(
                                            R.string.no_network_connection_on_play_file
                                        )
                                    )
                                }
                            }
                            pdfIntent.putExtra("HANDLE", node.handle)

                            if (MegaApiUtils.isIntentAvailable(this, pdfIntent)) {
                                startActivity(pdfIntent)
                            } else {
                                Timber.w("No available Intent")
                                showNodeAttachmentBottomSheet(m, position)
                            }
                            overridePendingTransition(0, 0)
                        } else if (typeForName(node.name).isOpenableTextFile(node.size)) {
                            ChatUtil.manageTextFileIntent(this, m.msgId, chatId)
                        } else {
                            Timber.d("NOT Image, pdf, audio or video - show node attachment panel for one node")
                            showNodeAttachmentBottomSheet(m, position)
                        }
                    } else {
                        Timber.d("Show node attachment panel")
                        showNodeAttachmentBottomSheet(m, position)
                    }
                }
            }
        } else {
            Timber.w(
                "DO NOTHING: Position (%d) is more than size in messages (size: %d)",
                position,
                messages?.size
            )
        }
    }

    private fun showFullScreenViewer(msgId: Long) {
        var currentNodeHandle = MegaApiJava.INVALID_HANDLE
        val messageIds: MutableList<Long> = ArrayList()

        messages?.forEach { message ->
            messageIds.add(message.msgId)
            if (message.msgId == msgId) {
                currentNodeHandle = message.megaNodeList[0].handle
            }
        }

        val previewParams: MutableMap<String, Any> = HashMap()
        previewParams[ChatImageNodeFetcher.CHAT_ROOM_ID] = chatId
        previewParams[ChatImageNodeFetcher.MESSAGE_IDS] =
            Longs.toArray(messageIds)

        val intent = createSecondaryIntent(
            this,
            ImagePreviewFetcherSource.CHAT,
            ImagePreviewMenuSource.CHAT,
            currentNodeHandle,
            previewParams,
            false
        )
        startActivity(intent)
    }

    private fun updateActionModeTitle() {
        Timber.d("updateActionModeTitle")
        if (actionMode == null) {
            return
        }

        val num = adapter?.selectedItemCount
        try {
            actionMode?.title = num.toString() + ""
            actionMode?.invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
            Timber.e(e, "Invalidate error")
        }
    }

    /*
     * Disable selection
     */
    fun hideMultipleSelect() {
        adapter?.isMultipleSelect = false
        if (actionMode != null) {
            actionMode?.finish()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun notifyDataSetChanged() {
        adapter?.notifyDataSetChanged()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        Timber.d("onSaveInstanceState")
        super.onSaveInstanceState(outState)
        chatRoom?.let { outState.putLong(CHAT_ID_KEY, it.chatId) }
    }

    override fun storedUnhandledData(preservedData: ArrayList<MegaChatMessage?>?) {
    }

    override fun handleStoredData() {
        chatC?.proceedWithForwardOrShare(
            this, myChatFilesFolder, preservedMessagesSelected,
            preservedMessagesToImport, chatId, Constants.FORWARD_ONLY_OPTION
        )
        preservedMessagesSelected = null
        preservedMessagesToImport = null
    }

    override fun storedUnhandledData(
        messagesSelected: ArrayList<MegaChatMessage>,
        messagesToImport: ArrayList<MegaChatMessage>,
    ) {
        preservedMessagesSelected = messagesSelected
        preservedMessagesToImport = messagesToImport
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        container?.let { showSnackbar(type, it, content, chatId) }
    }

    private inner class ActionBarCallBack : ActionMode.Callback {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            Timber.d("onActionItemClicked")
            val messagesSelected = adapter?.selectedMessages

            if (viewModel.getStorageState() == StorageState.PayWall && item.itemId != R.id.cab_menu_select_all && item.itemId != R.id.cab_menu_unselect_all) {
                showOverDiskQuotaPaywallWarning()
                return false
            }

            val itemId = item.itemId
            if (itemId == R.id.cab_menu_select_all) {
                selectAll()
            } else if (itemId == R.id.cab_menu_unselect_all) {
                clearSelections()
            } else if (itemId == R.id.chat_cab_menu_forward) {
                Timber.d("Forward message")
                clearSelections()
                hideMultipleSelect()
                forwardMessages(messagesSelected)
            } else if (itemId == R.id.chat_cab_menu_delete) {
                clearSelections()
                hideMultipleSelect()
                //Delete
                chatRoom?.let { showConfirmationDeleteMessages(messagesSelected, it) }
            } else if (itemId == R.id.chat_cab_menu_download) {
                clearSelections()
                hideMultipleSelect()
                val messageIds = ArrayList<Long>()
                messagesSelected?.forEach { message ->
                    val megaNodeHandle = message.msgId
                    messageIds.add(megaNodeHandle)
                }
                startDownloadViewModel.onDownloadClicked(
                    chatId,
                    messageIds
                )
            } else if (itemId == R.id.chat_cab_menu_import) {
                clearSelections()
                hideMultipleSelect()
                chatC?.importNodesFromMessages(messagesSelected)
            } else if (itemId == R.id.chat_cab_menu_offline) {
                checkNotificationsPermission(this@NodeAttachmentHistoryActivity)
                clearSelections()
                hideMultipleSelect()
                val messageId = messagesSelected?.get(0)?.msgId
                if (getStorageState() == StorageState.PayWall) {
                    showOverDiskQuotaPaywallWarning()
                } else {
                    messageId?.let { viewModel.saveChatNodeToOffline(chatId, it) }
                }
            }
            return false
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            Timber.d("onCreateActionMode")
            val inflater = mode.menuInflater
            inflater.inflate(R.menu.messages_node_history_action, menu)
            checkScroll()
            return true
        }

        override fun onDestroyActionMode(arg0: ActionMode) {
            Timber.d("onDestroyActionMode")
            adapter?.clearSelections()
            notifyDataSetChanged()
            adapter?.isMultipleSelect = false
            checkScroll()
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            Timber.d("onPrepareActionMode")
            val selected = adapter?.selectedMessages
            if (selected?.isNotEmpty() == true) {
                val unselect = menu.findItem(R.id.cab_menu_unselect_all)
                if (selected.size == adapter?.itemCount) {
                    menu.findItem(R.id.cab_menu_select_all).setVisible(false)
                    unselect.setTitle(getString(R.string.action_unselect_all))
                    unselect.setVisible(true)
                } else {
                    menu.findItem(R.id.cab_menu_select_all).setVisible(true)
                    unselect.setTitle(getString(R.string.action_unselect_all))
                    unselect.setVisible(true)
                }

                if (chatRoom?.ownPrivilege == MegaChatRoom.PRIV_RM || chatRoom?.ownPrivilege == MegaChatRoom.PRIV_RO && chatRoom?.isPreview == false) {
                    menu.findItem(R.id.chat_cab_menu_delete).setVisible(false)
                    menu.findItem(R.id.chat_cab_menu_forward).setVisible(false)
                    menu.findItem(R.id.chat_cab_menu_download).setVisible(false)
                    menu.findItem(R.id.chat_cab_menu_offline).setVisible(false)
                } else {
                    Timber.d("Chat with permissions")
                    menu.findItem(R.id.chat_cab_menu_forward).setVisible(
                        viewModel.isOnline() && chatC?.isInAnonymousMode == false
                    )

                    val importIcon = menu.findItem(R.id.chat_cab_menu_import)
                    if (selected.size == 1) {
                        if (selected[0].userHandle == megaChatApi.myUserHandle && selected[0].isDeletable) {
                            Timber.d("One message - Message DELETABLE")
                            menu.findItem(R.id.chat_cab_menu_delete).setVisible(true)
                        } else {
                            menu.findItem(R.id.chat_cab_menu_delete).setVisible(false)
                        }

                        if (viewModel.isOnline()) {
                            menu.findItem(R.id.chat_cab_menu_download).setVisible(true)
                            if (chatC?.isInAnonymousMode == true) {
                                menu.findItem(R.id.chat_cab_menu_offline).setVisible(false)
                                importIcon.setVisible(false)
                            } else {
                                menu.findItem(R.id.chat_cab_menu_offline).setVisible(true)
                                importIcon.setVisible(true)
                            }
                        } else {
                            menu.findItem(R.id.chat_cab_menu_download).setVisible(false)
                            menu.findItem(R.id.chat_cab_menu_offline).setVisible(false)
                            importIcon.setVisible(false)
                        }
                    } else {
                        Timber.d("Many items selected")
                        var showDelete = true
                        var allNodeAttachments = true

                        for (i in selected.indices) {
                            if (showDelete) {
                                if (selected[i].userHandle == megaChatApi.myUserHandle) {
                                    if (!(selected[i].isDeletable)) {
                                        showDelete = false
                                    }
                                } else {
                                    showDelete = false
                                }
                            }

                            if (allNodeAttachments) {
                                if (selected[i].type != MegaChatMessage.TYPE_NODE_ATTACHMENT) {
                                    allNodeAttachments = false
                                }
                            }
                        }
                        if (viewModel.isOnline()) {
                            menu.findItem(R.id.chat_cab_menu_download).setVisible(true)
                            importIcon.setVisible(chatC?.isInAnonymousMode == false)
                        } else {
                            menu.findItem(R.id.chat_cab_menu_download).setVisible(false)
                            importIcon.setVisible(false)
                        }

                        menu.findItem(R.id.chat_cab_menu_delete).setVisible(showDelete)
                        menu.findItem(R.id.chat_cab_menu_forward).setVisible(
                            viewModel.isOnline() && chatC?.isInAnonymousMode == false
                        )
                        // Hide available offline option when multiple attachments are selected
                        menu.findItem(R.id.chat_cab_menu_offline).setVisible(false)
                    }
                }
            } else {
                menu.findItem(R.id.cab_menu_select_all).setVisible(true)
                menu.findItem(R.id.cab_menu_unselect_all).setVisible(false)
                menu.findItem(R.id.chat_cab_menu_download).setVisible(false)
                menu.findItem(R.id.chat_cab_menu_delete).setVisible(false)
                menu.findItem(R.id.chat_cab_menu_offline).setVisible(false)
                menu.findItem(R.id.chat_cab_menu_forward).setVisible(false)
            }
            return false
        }
    }

    fun showConfirmationDeleteMessages(messages: ArrayList<MegaChatMessage?>?, chat: MegaChatRoom) {
        Timber.d("Chat ID: %s", chat.chatId)

        val dialogClickListener =
            DialogInterface.OnClickListener { _: DialogInterface?, which: Int ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        val cC = ChatController(this)
                        cC.deleteMessages(messages, chat)
                    }

                    DialogInterface.BUTTON_NEGATIVE -> {}
                }
            }

        val builder =
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)

        if (messages?.size == 1) {
            builder.setMessage(R.string.confirmation_delete_one_message)
        } else {
            builder.setMessage(R.string.confirmation_delete_several_messages)
        }
        builder.setPositiveButton(R.string.context_remove, dialogClickListener)
            .setNegativeButton(
                mega.privacy.android.shared.resources.R.string.general_dialog_cancel_button,
                dialogClickListener
            ).show()
    }

    fun forwardMessages(messagesSelected: ArrayList<MegaChatMessage?>?) {
        Timber.d("forwardMessages")
        chatC?.prepareMessagesToForward(messagesSelected, chatId)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        Timber.d("Result Code: %s", resultCode)

        if (requestCode == Constants.REQUEST_CODE_SELECT_IMPORT_FOLDER && resultCode == RESULT_OK) {
            if (viewModel.isOnline().not()) {
                try {
                    statusDialog?.dismiss()
                } catch (ignored: Exception) {
                }
                showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getString(R.string.error_server_connection_problem)
                )
                return
            }
            val toHandle = intent?.getLongExtra("IMPORT_TO", 0) ?: 0
            val importMessagesHandles =
                intent?.getLongArrayExtra("HANDLES_IMPORT_CHAT") ?: longArrayOf()

            importNodes(toHandle, importMessagesHandles)
        } else if (requestCode == Constants.REQUEST_CODE_SELECT_CHAT && resultCode == RESULT_OK) {
            if (!viewModel.isOnline()) {
                try {
                    statusDialog?.dismiss()
                } catch (ignored: Exception) {
                }

                showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getString(R.string.error_server_connection_problem)
                )
                return
            }

            showProgressForwarding()

            val idMessages = intent?.getLongArrayExtra(Constants.ID_MESSAGES)
            val chatHandles = intent?.getLongArrayExtra(Constants.SELECTED_CHATS)
            val contactHandles = intent?.getLongArrayExtra(Constants.SELECTED_USERS)

            if (chatHandles != null && chatHandles.isNotEmpty() && idMessages != null) {
                if (contactHandles != null && contactHandles.isNotEmpty()) {
                    val users = ArrayList<MegaUser>()
                    val chats = ArrayList<MegaChatRoom>()

                    for (contactHandle in contactHandles) {
                        val user =
                            megaApi.getContact(MegaApiAndroid.userHandleToBase64(contactHandle))
                        if (user != null) {
                            users.add(user)
                        }
                    }

                    for (chatHandle in chatHandles) {
                        val chatRoom = megaChatApi.getChatRoom(chatHandle)
                        if (chatRoom != null) {
                            chats.add(chatRoom)
                        }
                    }

                    val listener = CreateChatListener(
                        CreateChatListener.SEND_MESSAGES, chats, users, this, this, idMessages,
                        chatId
                    )

                    for (user in users) {
                        val peers = MegaChatPeerList.createInstance()
                        peers.addPeer(user.handle, MegaChatPeerList.PRIV_STANDARD)
                        megaChatApi.createChat(false, peers, listener)
                    }
                } else {
                    val countChat = chatHandles.size
                    Timber.d("Selected: %d chats to send", countChat)

                    val forwardChatProcessor =
                        MultipleForwardChatProcessor(this, chatHandles, idMessages, chatId)
                    forwardChatProcessor.forward(chatRoom)
                }
            } else {
                Timber.e("Error on sending to chat")
            }
        }
    }

    private fun showProgressForwarding() {
        Timber.d("showProgressForwarding")

        statusDialog = createProgressDialog(this, getString(R.string.general_forwarding))
        statusDialog?.show()
    }

    fun removeProgressDialog() {
        try {
            statusDialog?.dismiss()
        } catch (ex: Exception) {
            Timber.e(ex)
        }
    }

    private fun importNodes(toHandle: Long, importMessagesHandles: LongArray?) {
        if (importMessagesHandles == null) return
        statusDialog = createProgressDialog(this, getString(R.string.general_importing))
        statusDialog?.show()
        val messageIds: MutableList<Long> = ArrayList()
        for (id in importMessagesHandles) messageIds.add(id)
        viewModel.importChatNodes(chatId, messageIds, toHandle)
    }

    override fun onRequestStart(api: MegaChatApiJava, request: MegaChatRequest) {
    }

    override fun onRequestUpdate(api: MegaChatApiJava, request: MegaChatRequest) {
    }

    override fun onRequestFinish(api: MegaChatApiJava, request: MegaChatRequest, e: MegaChatError) {
    }

    override fun onRequestTemporaryError(
        api: MegaChatApiJava,
        request: MegaChatRequest,
        e: MegaChatError,
    ) {
    }

    override fun onAttachmentLoaded(api: MegaChatApiJava, msg: MegaChatMessage?) {
        if (msg != null) {
            Timber.d("Message ID%s", msg.msgId)
            if (msg.type == MegaChatMessage.TYPE_NODE_ATTACHMENT) {
                val nodeList = msg.megaNodeList
                if (nodeList != null) {
                    if (nodeList.size() == 1) {
                        val node = nodeList[0]
                        Timber.d("Node Handle: %s", node.handle)
                        bufferMessages?.add(msg)
                        Timber.d("Size of buffer: %s", bufferMessages?.size)
                        Timber.d("Size of messages: %s", messages?.size)
                    }
                }
            }
        } else {
            Timber.d("Message is NULL: end of history")
            val bufferSize = bufferMessages?.size ?: 0
            val listSize = messages?.size ?: 0
            if ((bufferSize + listSize) >= NUMBER_MESSAGES_TO_LOAD) {
                fullHistoryReceivedOnLoad()
                isLoadingHistory = false
            } else {
                Timber.d("Less Number Received")
                if ((stateHistory != MegaChatApi.SOURCE_NONE) && (stateHistory != MegaChatApi.SOURCE_ERROR) && stateHistory != MegaChatApi.SOURCE_INVALID_CHAT) {
                    Timber.d("But more history exists --> loadAttachments")
                    isLoadingHistory = true
                    stateHistory = megaChatApi.loadAttachments(chatId, NUMBER_MESSAGES_TO_LOAD)
                    Timber.d("New state of history: %s", stateHistory)
                    getMoreHistory = false
                    if (stateHistory == MegaChatApi.SOURCE_NONE || stateHistory == MegaChatApi.SOURCE_ERROR || stateHistory == MegaChatApi.SOURCE_INVALID_CHAT) {
                        fullHistoryReceivedOnLoad()
                        isLoadingHistory = false
                    }
                } else {
                    Timber.d("New state of history: %s", stateHistory)
                    fullHistoryReceivedOnLoad()
                    isLoadingHistory = false
                }
            }
        }
    }

    private fun fullHistoryReceivedOnLoad() {
        Timber.d("Messages size: %s", messages?.size)

        if (bufferMessages?.isNotEmpty() == true) {
            Timber.d("Buffer size: %s", bufferMessages?.size)
            findViewById<RelativeLayout>(R.id.empty_layout_node_history).visibility = View.GONE
            listView?.visibility = View.VISIBLE

            bufferMessages?.listIterator()?.let {
                while (it.hasNext()) {
                    messages?.add(it.next())
                }
            }

            if (messages?.isNotEmpty() == true) {
                if (adapter == null) {
                    adapter = NodeAttachmentHistoryAdapter(this, messages, listView)
                    listView?.layoutManager = mLayoutManager
                    listView?.addItemDecoration(SimpleDividerItemDecoration(this))
                    listView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            super.onScrolled(recyclerView, dx, dy)
                            checkScroll()
                        }
                    })
                    listView?.adapter = adapter
                    adapter?.setMessages(messages)
                } else {
                    bufferMessages?.size?.let { adapter?.loadPreviousMessages(messages, it) }
                }
            }
            bufferMessages?.clear()
        }

        Timber.d("getMoreHistoryTRUE")
        getMoreHistory = true

        invalidateOptionsMenu()
    }

    override fun onAttachmentReceived(api: MegaChatApiJava, msg: MegaChatMessage) {
        Timber.d("STATUS: %s", msg.status)
        Timber.d("TEMP ID: %s", msg.tempId)
        Timber.d("FINAL ID: %s", msg.msgId)
        Timber.d("TIMESTAMP: %s", msg.timestamp)
        Timber.d("TYPE: %s", msg.type)

        var lastIndex = 0
        if (messages?.isEmpty() == true) {
            messages?.add(msg)
        } else {
            Timber.d("Status of message: %s", msg.status)

            while ((messages?.get(lastIndex)?.msgIndex ?: -1) > msg.msgIndex) {
                lastIndex++
            }

            Timber.d("Append in position: %s", lastIndex)
            messages?.add(lastIndex, msg)
        }

        //Create adapter
        if (adapter == null) {
            Timber.d("Create adapter")
            adapter = NodeAttachmentHistoryAdapter(this, messages, listView)
            listView?.layoutManager = mLayoutManager
            listView?.addItemDecoration(SimpleDividerItemDecoration(this))
            listView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    checkScroll()
                }
            })
            listView?.adapter = adapter
            adapter?.setMessages(messages)
        } else {
            Timber.d("Update adapter with last index: %s", lastIndex)
            if (lastIndex < 0) {
                Timber.d("Arrives the first message of the chat")
                adapter?.setMessages(messages)
            } else {
                adapter?.addMessage(messages, lastIndex + 1)
                adapter?.notifyItemChanged(lastIndex)
            }
        }
        val emptyLayout = findViewById<RelativeLayout>(R.id.empty_layout_node_history)
        emptyLayout.visibility = View.GONE
        listView?.visibility = View.VISIBLE

        invalidateOptionsMenu()
    }

    override fun onAttachmentDeleted(api: MegaChatApiJava, msgid: Long) {
        Timber.d("Message ID: %s", msgid)

        var indexToChange = -1

        messages?.listIterator()?.let {
            while (it.hasNext()) {
                val messageToCheck = it.next()
                if (messageToCheck.tempId == msgid) {
                    indexToChange = it.previousIndex()
                    break
                }
                if (messageToCheck.msgId == msgid) {
                    indexToChange = it.previousIndex()
                    break
                }
            }
        }

        if (indexToChange != -1) {
            messages?.removeAt(indexToChange)
            Timber.d("Removed index: %d, Messages size: %d", indexToChange, messages?.size)

            adapter?.removeMessage(indexToChange, messages)

            if (messages?.isEmpty() == true) {
                findViewById<RelativeLayout>(R.id.empty_layout_node_history).visibility =
                    View.VISIBLE
                listView?.visibility = View.GONE
            }
        } else {
            Timber.w("Index to remove not found")
        }

        invalidateOptionsMenu()
    }

    override fun onTruncate(api: MegaChatApiJava, msgid: Long) {
        Timber.d("Message ID: %s", msgid)
        invalidateOptionsMenu()
        messages?.clear()
        notifyDataSetChanged()
        listView?.visibility = View.GONE
        findViewById<RelativeLayout>(R.id.empty_layout_node_history).visibility = View.VISIBLE
    }

    fun showNodeAttachmentBottomSheet(message: MegaChatMessage?, position: Int) {
        Timber.d("showNodeAttachmentBottomSheet: %s", position)

        if (message == null || bottomSheetDialogFragment.isBottomSheetDialogShown()) return

        selectedMessageId = message.msgId
        bottomSheetDialogFragment = newInstance(chatId, selectedMessageId)
        bottomSheetDialogFragment?.show(supportFragmentManager, bottomSheetDialogFragment?.tag)
    }

    fun showSnackbar(type: Int, s: String?) {
        container?.let { showSnackbar(type, it, s) }
    }

    fun checkScroll() {
        if (listView != null) {
            val withElevation = listView?.canScrollVertically(-1) ?: false
                    || (adapter != null && adapter?.isMultipleSelect == true)
            val elevation = resources.getDimension(R.dimen.toolbar_elevation)
            materialToolBar?.elevation = if (withElevation) elevation else 0f
        }
    }

    fun setMyChatFilesFolder(myChatFilesFolder: MegaNode?) {
        this.myChatFilesFolder = myChatFilesFolder
    }

    companion object {
        private const val NUMBER_MESSAGES_TO_LOAD: Int = 20
        private const val NUMBER_MESSAGES_BEFORE_LOAD: Int = 8
        private const val CHAT_ID_KEY = "chatId"
    }
}