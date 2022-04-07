package mega.privacy.android.app.mediaplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.IBinder
import android.view.*
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.*
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.exoplayer2.util.Util.startForegroundService
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.OfflineFileInfoActivity
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.components.dragger.DragToExitSupport
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.databinding.ActivityMediaPlayerBinding
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.FileInfoActivity
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerServiceBinder
import mega.privacy.android.app.mediaplayer.service.VideoPlayerService
import mega.privacy.android.app.mediaplayer.trackinfo.TrackInfoFragment
import mega.privacy.android.app.mediaplayer.trackinfo.TrackInfoFragmentArgs
import mega.privacy.android.app.usecase.exception.MegaException
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.AlertsAndWarnings.showSaveToDeviceConfirmDialog
import mega.privacy.android.app.utils.AlertsAndWarnings.showTakenDownAlert
import mega.privacy.android.app.utils.ChatUtil.removeAttachmentMessage
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.shareUri
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.MegaNodeDialogUtil.moveToRubbishOrRemove
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showRenameNodeDialog
import mega.privacy.android.app.utils.MegaNodeUtil.selectFolderToCopy
import mega.privacy.android.app.utils.MegaNodeUtil.selectFolderToMove
import mega.privacy.android.app.utils.MegaNodeUtil.shareLink
import mega.privacy.android.app.utils.MegaNodeUtil.shareNode
import mega.privacy.android.app.utils.MegaNodeUtil.showShareOption
import mega.privacy.android.app.utils.MegaNodeUtil.showTakenDownNodeActionNotAvailableDialog
import mega.privacy.android.app.utils.MenuUtils.toggleAllMenuItemsVisibility
import mega.privacy.android.app.utils.RunOnUIThreadUtils.post
import mega.privacy.android.app.utils.RunOnUIThreadUtils.runDelay
import nz.mega.sdk.*
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
abstract class MediaPlayerActivity : PasscodeActivity(), SnackbarShower, ActivityLauncher {

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    @Inject
    lateinit var megaChatApi: MegaChatApiAndroid

    private val viewModel: MediaPlayerViewModel by viewModels()

    private lateinit var binding: ActivityMediaPlayerBinding

    private lateinit var actionBar: ActionBar
    private lateinit var navController: NavController

    private var optionsMenu: Menu? = null
    private var searchMenuItem: MenuItem? = null

    private var viewingTrackInfo: TrackInfoFragmentArgs? = null

    private var serviceBound = false
    private var playerService: MediaPlayerService? = null

    private var takenDownDialog: AlertDialog? = null

    private val nodeAttacher by lazy { MegaAttacher(this) }

    private val nodeSaver by lazy {
        NodeSaver(this, this, this, showSaveToDeviceConfirmDialog(this))
    }

    private val dragToExit by lazy {
        DragToExitSupport(this, this::onDragActivated) {
            finish()
            overridePendingTransition(0, android.R.anim.fade_out)
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
        }

        /**
         * Called after a successful bind with our AudioPlayerService.
         */
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MediaPlayerServiceBinder) {
                playerService = service.service

                refreshMenuOptionsVisibility()

                service.service.metadata.observe(this@MediaPlayerActivity) {
                    dragToExit.nodeChanged(service.service.viewModel.playingHandle)
                }

                service.service.viewModel.error.observe(
                    this@MediaPlayerActivity, this@MediaPlayerActivity::onError
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extras = intent.extras
        if (extras == null) {
            finish()
            return
        }

        val rebuildPlaylist = intent.getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)
        val adapterType = intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
        if (adapterType == INVALID_VALUE && rebuildPlaylist) {
            finish()
            return
        }

        if (savedInstanceState != null) {
            nodeAttacher.restoreState(savedInstanceState)
            nodeSaver.restoreState(savedInstanceState)
        }

        val isAudioPlayer = isAudioPlayer(intent)

        binding = ActivityMediaPlayerBinding.inflate(layoutInflater)

        if (isAudioPlayer()) {
            setContentView(binding.root)
            binding.toolbar.setBackgroundColor(Color.TRANSPARENT)
        } else {
            setContentView(dragToExit.wrapContentView(binding.root))
            MediaPlayerService.pauseAudioPlayer(this)
            dragToExit.observeThumbnailLocation(this, intent)
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setupToolbar()
        setupNavDestListener()

        val playerServiceIntent = Intent(
            this,
            if (isAudioPlayer) AudioPlayerService::class.java else VideoPlayerService::class.java
        )

        playerServiceIntent.putExtras(extras)

        if (rebuildPlaylist && savedInstanceState == null) {
            playerServiceIntent.setDataAndType(intent.data, intent.type)
            if (isAudioPlayer) {
                startForegroundService(this, playerServiceIntent)
            } else {
                startService(playerServiceIntent)
            }
        }

        bindService(playerServiceIntent, connection, Context.BIND_AUTO_CREATE)
        serviceBound = true

        viewModel.getCollision().observe(this) { collision ->
            nameCollisionActivityContract.launch(arrayListOf(collision))
        }

        viewModel.onSnackbarMessage().observe(this) { message ->
            showSnackbar(message)
        }

        viewModel.onExceptionThrown().observe(this, ::manageException)

        viewModel.itemToRemove.observe(this) {
            playerService?.viewModel?.removeItem(it)
        }

        if (savedInstanceState == null && !isAudioPlayer) {
            // post to next UI cycle so that MediaPlayerFragment's onCreateView is called
            post {
                getFragmentFromNavHost(R.id.nav_host_fragment, MediaPlayerFragment::class.java)
                    ?.runEnterAnimation(dragToExit)
            }
        }

        if (CallUtil.participatingInACall()) {
            showNotAllowPlayAlert()
        }

        LiveEventBus.get(EVENT_NOT_ALLOW_PLAY, Boolean::class.java)
            .observe(this) {
                showNotAllowPlayAlert()
            }
    }

    private fun showNotAllowPlayAlert() {
        showSnackbar(StringResourcesUtils.getString(R.string.not_allow_play_alert))
    }

    override fun onResume() {
        super.onResume()

        refreshMenuOptionsVisibility()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (isAudioPlayer()) {
            window.setFormat(PixelFormat.RGBA_8888) // Needed to fix bg gradient banding
        }
    }

    abstract fun isAudioPlayer(): Boolean

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        nodeAttacher.saveState(outState)
        nodeSaver.saveState(outState)
    }

    private fun stopPlayer() {
        playerService?.stopAudioPlayer()
        finish()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        actionBar = supportActionBar!!
        actionBar.setHomeButtonEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.title = ""

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        if (psaWebBrowser != null && psaWebBrowser.consumeBack()) return
        if (!navController.navigateUp()) {
            finish()
        }
    }

    private fun setupNavDestListener() {
        navController.addOnDestinationChangedListener { _, dest, args ->
            setupToolbarColors()

            when (dest.id) {
                R.id.main_player -> {
                    actionBar.title = ""
                    viewingTrackInfo = null
                }
                R.id.playlist -> {
                    viewingTrackInfo = null
                }
                R.id.track_info -> {
                    actionBar.title = StringResourcesUtils.getString(R.string.audio_track_info)
                        .uppercase(Locale.getDefault())

                    if (args != null) {
                        viewingTrackInfo = TrackInfoFragmentArgs.fromBundle(args)
                    }
                }
            }

            refreshMenuOptionsVisibility()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isFinishing) {
            playerService?.mainPlayerUIClosed()
            dragToExit.showPreviousHiddenThumbnail()
        }

        playerService = null
        if (serviceBound) {
            unbindService(connection)
        }

        nodeSaver.destroy()

        if (isFinishing && !isAudioPlayer(intent)) {
            MediaPlayerService.resumeAudioPlayer(this)
        }

        dismissAlertDialogIfExists(takenDownDialog)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        optionsMenu = menu

        menuInflater.inflate(R.menu.media_player, menu)

        menu.findItem(R.id.get_link).title =
            StringResourcesUtils.getQuantityString(R.plurals.get_links, 1)

        searchMenuItem = menu.findItem(R.id.action_search)

        val searchView = searchMenuItem?.actionView
        if (searchView is SearchView) {
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    playerService?.viewModel?.playlistSearchQuery = newText
                    return true
                }

            })
        }

        searchMenuItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                playerService?.viewModel?.playlistSearchQuery = null
                return true
            }
        })

        refreshMenuOptionsVisibility()

        return true
    }

    private fun refreshMenuOptionsVisibility() {
        val menu = optionsMenu
        if (menu == null) {
            logDebug("refreshMenuOptionsVisibility menu is null")
            return
        }

        val currentFragment = navController.currentDestination?.id
        if (currentFragment == null) {
            logDebug("refreshMenuOptionsVisibility currentFragment is null")
            return
        }

        val service = playerService
        if (service == null) {
            logDebug("refreshMenuOptionsVisibility null service")

            menu.toggleAllMenuItemsVisibility(false)
            return
        }

        val adapterType = service.viewModel.currentIntent
            ?.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)

        if (adapterType == null) {
            logDebug("refreshMenuOptionsVisibility null adapterType")

            menu.toggleAllMenuItemsVisibility(false)
            return
        }

        when (currentFragment) {
            R.id.playlist -> {
                menu.toggleAllMenuItemsVisibility(false)
                searchMenuItem?.isVisible = true
                // Display the select option
                menu.findItem(R.id.select).isVisible = true
            }
            R.id.main_player, R.id.track_info -> {
                if (adapterType == OFFLINE_ADAPTER) {
                    menu.toggleAllMenuItemsVisibility(false)

                    menu.findItem(R.id.properties).isVisible =
                        currentFragment == R.id.main_player

                    menu.findItem(R.id.share).isVisible =
                        currentFragment == R.id.main_player

                    return
                }

                if (adapterType == RUBBISH_BIN_ADAPTER
                    || megaApi.isInRubbish(megaApi.getNodeByHandle(service.viewModel.playingHandle))
                ) {
                    menu.toggleAllMenuItemsVisibility(false)

                    menu.findItem(R.id.properties).isVisible =
                        currentFragment == R.id.main_player

                    val moveToTrash = menu.findItem(R.id.move_to_trash) ?: return
                    moveToTrash.isVisible = true
                    moveToTrash.title = StringResourcesUtils.getString(R.string.context_remove)

                    return
                }

                if (adapterType == FROM_CHAT) {
                    menu.toggleAllMenuItemsVisibility(false)

                    menu.findItem(R.id.save_to_device).isVisible = true
                    menu.findItem(R.id.chat_import).isVisible = true
                    menu.findItem(R.id.chat_save_for_offline).isVisible = true

                    // TODO: share option will be added in AND-12831
                    menu.findItem(R.id.share).isVisible = false

                    val moveToTrash = menu.findItem(R.id.move_to_trash) ?: return

                    val pair = getChatMessage()
                    val message = pair.second

                    val canRemove = message != null &&
                            message.userHandle == megaChatApi.myUserHandle && message.isDeletable

                    if (!canRemove) {
                        moveToTrash.isVisible = false
                        return
                    }

                    moveToTrash.isVisible = true
                    moveToTrash.title = StringResourcesUtils.getString(R.string.context_remove)

                    return
                }

                if (adapterType == FILE_LINK_ADAPTER || adapterType == ZIP_ADAPTER) {
                    menu.toggleAllMenuItemsVisibility(false)

                    menu.findItem(R.id.save_to_device).isVisible = true
                    menu.findItem(R.id.share).isVisible = true

                    return
                }

                if (adapterType == FOLDER_LINK_ADAPTER || adapterType == FROM_IMAGE_VIEWER) {
                    menu.toggleAllMenuItemsVisibility(false)

                    menu.findItem(R.id.save_to_device).isVisible = true

                    return
                }

                val node = megaApi.getNodeByHandle(service.viewModel.playingHandle)
                if (node == null) {
                    logDebug("refreshMenuOptionsVisibility node is null")

                    menu.toggleAllMenuItemsVisibility(false)
                    return
                }

                menu.toggleAllMenuItemsVisibility(true)
                searchMenuItem?.isVisible = false

                menu.findItem(R.id.save_to_device).isVisible = true
                // Hide the select, select all, and clear options
                menu.findItem(R.id.select).isVisible = false
                menu.findItem(R.id.remove).isVisible = false

                menu.findItem(R.id.properties).isVisible = currentFragment == R.id.main_player

                menu.findItem(R.id.share).isVisible =
                    currentFragment == R.id.main_player && showShareOption(
                        adapterType, adapterType == FOLDER_LINK_ADAPTER, node.handle
                    )

                menu.findItem(R.id.send_to_chat).isVisible = true

                if (megaApi.getAccess(node) == MegaShare.ACCESS_OWNER) {
                    if (node.isExported) {
                        menu.findItem(R.id.get_link).isVisible = false
                        menu.findItem(R.id.remove_link).isVisible = true
                    } else {
                        menu.findItem(R.id.get_link).isVisible = true
                        menu.findItem(R.id.remove_link).isVisible = false
                    }
                } else {
                    menu.findItem(R.id.get_link).isVisible = false
                    menu.findItem(R.id.remove_link).isVisible = false
                }

                menu.findItem(R.id.chat_import).isVisible = false
                menu.findItem(R.id.chat_save_for_offline).isVisible = false

                val access = megaApi.getAccess(node)
                when (access) {
                    MegaShare.ACCESS_READWRITE, MegaShare.ACCESS_READ, MegaShare.ACCESS_UNKNOWN -> {
                        menu.findItem(R.id.rename).isVisible = false
                        menu.findItem(R.id.move).isVisible = false
                    }
                    MegaShare.ACCESS_FULL, MegaShare.ACCESS_OWNER -> {
                        menu.findItem(R.id.rename).isVisible = true
                        menu.findItem(R.id.move).isVisible = true
                    }
                }

                menu.findItem(R.id.move_to_trash).isVisible =
                    node.parentHandle != megaApi.rubbishNode.handle
                            && (access == MegaShare.ACCESS_FULL || access == MegaShare.ACCESS_OWNER)

                menu.findItem(R.id.copy).isVisible = adapterType != FOLDER_LINK_ADAPTER
            }
        }
    }

    @Suppress("deprecation") // TODO Migrate to registerForActivityResult()
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val service = playerService ?: return false
        val launchIntent = service.viewModel.currentIntent ?: return false
        val playingHandle = service.viewModel.playingHandle
        val adapterType = launchIntent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
        val isFolderLink = adapterType == FOLDER_LINK_ADAPTER

        when (item.itemId) {
            R.id.save_to_device -> {
                when (adapterType) {
                    OFFLINE_ADAPTER -> nodeSaver.saveOfflineNode(playingHandle, true)
                    ZIP_ADAPTER -> {
                        val uri = service.player.currentMediaItem?.localConfiguration?.uri
                            ?: return false
                        val playlistItem =
                            service.viewModel.getPlaylistItem(service.player.currentMediaItem?.mediaId)
                                ?: return false

                        nodeSaver.saveUri(uri, playlistItem.nodeName, playlistItem.size, true)
                    }
                    FROM_CHAT -> {
                        val node = getChatMessageNode() ?: return true

                        nodeSaver.saveNode(
                            node, highPriority = true, fromMediaViewer = true, needSerialize = true
                        )
                    }
                    FILE_LINK_ADAPTER -> {
                        launchIntent.getStringExtra(EXTRA_SERIALIZE_STRING)?.let { serialize ->
                            val currentDocument = MegaNode.unserialize(serialize)
                            if (currentDocument != null) {
                                logDebug("currentDocument NOT NULL")
                                nodeSaver.saveNode(
                                    currentDocument,
                                    isFolderLink = isFolderLink,
                                    fromMediaViewer = true,
                                    needSerialize = true
                                )
                            } else {
                                LogUtil.logWarning("currentDocument is NULL")
                            }
                        }
                    }
                    else -> {
                        nodeSaver.saveHandle(
                            playingHandle,
                            isFolderLink = isFolderLink,
                            fromMediaViewer = true
                        )
                    }
                }

                return true
            }
            R.id.properties -> {
                if (isAudioPlayer()) {
                    val uri =
                        service.player.currentMediaItem?.localConfiguration?.uri ?: return true
                    navController.navigate(
                        MediaPlayerFragmentDirections.actionPlayerToTrackInfo(
                            adapterType, adapterType == INCOMING_SHARES_ADAPTER, playingHandle, uri
                        )
                    )
                } else {
                    val intent: Intent

                    if (adapterType == OFFLINE_ADAPTER) {
                        intent = Intent(this, OfflineFileInfoActivity::class.java)
                        intent.putExtra(HANDLE, playingHandle.toString())

                        logDebug("onOptionsItemSelected properties offline handle $playingHandle")
                    } else {
                        intent = Intent(this, FileInfoActivity::class.java)
                        intent.putExtra(HANDLE, playingHandle)

                        val node = megaApi.getNodeByHandle(playingHandle)
                        if (node == null) {
                            logError("onOptionsItemSelected properties non-offline null node")

                            return false
                        }

                        intent.putExtra(NAME, node.name)

                        val fromIncoming =
                            if (adapterType == SEARCH_ADAPTER || adapterType == RECENTS_ADAPTER) {
                                NodeController(this).nodeComesFromIncoming(node)
                            } else {
                                false
                            }

                        when {
                            adapterType == INCOMING_SHARES_ADAPTER || fromIncoming -> {
                                intent.putExtra(INTENT_EXTRA_KEY_FROM, FROM_INCOMING_SHARES)
                                intent.putExtra(INTENT_EXTRA_KEY_FIRST_LEVEL, false)
                            }
                            adapterType == INBOX_ADAPTER -> {
                                intent.putExtra(INTENT_EXTRA_KEY_FROM, FROM_INBOX)
                            }
                        }
                    }

                    startActivity(intent)
                }
                return true
            }
            R.id.chat_import -> {
                val intent = Intent(this, FileExplorerActivity::class.java)
                intent.action = FileExplorerActivity.ACTION_PICK_IMPORT_FOLDER
                startActivityForResult(intent, REQUEST_CODE_SELECT_IMPORT_FOLDER)
                return true
            }
            R.id.share -> {
                when (adapterType) {
                    OFFLINE_ADAPTER, ZIP_ADAPTER -> {
                        val nodeName =
                            service.viewModel.getPlaylistItem(service.player.currentMediaItem?.mediaId)?.nodeName
                                ?: return false
                        val uri = service.player.currentMediaItem?.localConfiguration?.uri
                            ?: return false

                        shareUri(this, nodeName, uri)
                    }
                    FILE_LINK_ADAPTER -> {
                        shareLink(this, launchIntent.getStringExtra(URL_FILE_LINK))
                    }
                    else -> {
                        shareNode(this, megaApi.getNodeByHandle(service.viewModel.playingHandle))
                    }
                }
                return true
            }
            R.id.send_to_chat -> {
                nodeAttacher.attachNode(playingHandle)
                return true
            }
            R.id.get_link -> {
                if (showTakenDownNodeActionNotAvailableDialog(
                        megaApi.getNodeByHandle(playingHandle), this
                    )
                ) {
                    return true
                }
                LinksUtil.showGetLinkActivity(this, playingHandle)
                return true
            }
            R.id.remove_link -> {
                val node = megaApi.getNodeByHandle(playingHandle) ?: return true
                if (showTakenDownNodeActionNotAvailableDialog(node, this)) {
                    return true
                }

                AlertsAndWarnings.showConfirmRemoveLinkDialog(this) {
                    megaApi.disableExport(node, object : BaseListener(this) {
                        override fun onRequestFinish(
                            api: MegaApiJava, request: MegaRequest, e: MegaError
                        ) {
                            if (e.errorCode == MegaError.API_OK) {
                                // Some times checking node.isExported immediately will still
                                // get true, so let's add some delay here.
                                runDelay(100L) {
                                    refreshMenuOptionsVisibility()
                                }
                            }
                        }
                    })
                }
                return true
            }
            R.id.chat_save_for_offline -> {
                val pair = getChatMessage()
                val message = pair.second

                if (message != null) {
                    ChatController(this).saveForOffline(
                        message.megaNodeList, megaChatApi.getChatRoom(pair.first), true, this
                    )
                }

                return true
            }
            R.id.rename -> {
                val node = megaApi.getNodeByHandle(playingHandle) ?: return true
                showRenameNodeDialog(this, node, this, object : ActionNodeCallback {
                    override fun finishRenameActionWithSuccess(newName: String) {
                        playerService?.viewModel?.updateItemName(node.handle, newName)
                        updateTrackInfoNodeNameIfNeeded(node.handle, newName)
                    }
                })
                return true
            }
            R.id.move -> {
                selectFolderToMove(this, longArrayOf(playingHandle))
                return true
            }
            R.id.copy -> {
                selectFolderToCopy(this, longArrayOf(playingHandle))
                return true
            }
            R.id.move_to_trash -> {
                if (adapterType == FROM_CHAT) {
                    val pair = getChatMessage()
                    val message = pair.second

                    if (message != null) {
                        removeAttachmentMessage(this, pair.first, message)
                    }
                } else {
                    moveToRubbishOrRemove(playingHandle, this, this)
                }
                return true
            }
        }
        return false
    }

    /**
     * Get chat id and chat message from the launch intent.
     *
     * @return first is chat id, second is chat message
     */
    private fun getChatMessage(): Pair<Long, MegaChatMessage?> {
        val chatId = intent.getLongExtra(INTENT_EXTRA_KEY_CHAT_ID, INVALID_HANDLE)
        val msgId = intent.getLongExtra(INTENT_EXTRA_KEY_MSG_ID, INVALID_HANDLE)

        if (chatId == INVALID_HANDLE || msgId == INVALID_HANDLE) {
            return Pair(chatId, null)
        }

        var message = megaChatApi.getMessage(chatId, msgId)

        if (message == null) {
            message = megaChatApi.getMessageFromNodeHistory(chatId, msgId)
        }

        return Pair(chatId, message)
    }

    /**
     * Get chat message node from the launch intent.
     *
     * @return chat message node
     */
    private fun getChatMessageNode(): MegaNode? {
        val pair = getChatMessage()
        val message = pair.second ?: return null

        return ChatController(this).authorizeNodeIfPreview(
            message.megaNodeList.get(0), megaChatApi.getChatRoom(pair.first)
        )
    }

    /**
     * Update node name if current displayed fragment is TrackInfoFragment.
     *
     * @param handle node handle
     * @param newName new node name
     */
    private fun updateTrackInfoNodeNameIfNeeded(handle: Long, newName: String) {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) ?: return
        val firstChild = navHostFragment.childFragmentManager.fragments.firstOrNull() ?: return
        if (firstChild is TrackInfoFragment) {
            firstChild.updateNodeNameIfNeeded(handle, newName)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        nodeSaver.handleRequestPermissionsResult(requestCode)
    }

    @Suppress("deprecation") // TODO Migrate to registerForActivityResult()
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (nodeAttacher.handleActivityResult(requestCode, resultCode, data, this)) {
            return
        }

        if (nodeSaver.handleActivityResult(this, requestCode, resultCode, data)) {
            return
        }

        when (requestCode) {
            REQUEST_CODE_SELECT_IMPORT_FOLDER -> {
                val node = getChatMessageNode() ?: return

                val toHandle = data?.getLongExtra(INTENT_EXTRA_KEY_IMPORT_TO, INVALID_HANDLE)
                    ?: return

                viewModel.copyNode(node = node, newParentHandle = toHandle)
            }
            REQUEST_CODE_SELECT_FOLDER_TO_MOVE -> {
                val moveHandles = data?.getLongArrayExtra(INTENT_EXTRA_KEY_MOVE_HANDLES)
                    ?: return
                val toHandle = data.getLongExtra(INTENT_EXTRA_KEY_MOVE_TO, INVALID_HANDLE)

                viewModel.moveNode(moveHandles[0], toHandle)
            }
            REQUEST_CODE_SELECT_FOLDER_TO_COPY -> {
                val copyHandles = data?.getLongArrayExtra(Constants.INTENT_EXTRA_KEY_COPY_HANDLES)
                    ?: return
                val toHandle = data.getLongExtra(INTENT_EXTRA_KEY_MOVE_TO, INVALID_HANDLE)

                viewModel.copyNode(nodeHandle = copyHandles[0], newParentHandle = toHandle)
            }
        }
    }

    fun setToolbarTitle(title: String) {
        binding.toolbar.title = title
    }

    fun closeSearch() {
        searchMenuItem?.collapseActionView()
    }

    fun hideToolbar(animate: Boolean = true) {
        if (animate) {
            binding.toolbar.animate()
                .translationY(-binding.toolbar.measuredHeight.toFloat())
                .setDuration(MEDIA_PLAYER_TOOLBAR_SHOW_HIDE_DURATION_MS)
                .start()
        } else {
            binding.toolbar.animate().cancel()
            binding.toolbar.translationY = -binding.toolbar.measuredHeight.toFloat()
        }
    }

    fun showToolbar(animate: Boolean = true) {
        if (animate) {
            binding.toolbar.animate()
                .translationY(0F)
                .setDuration(MEDIA_PLAYER_TOOLBAR_SHOW_HIDE_DURATION_MS)
                .start()
        } else {
            binding.toolbar.animate().cancel()
            binding.toolbar.translationY = 0F
        }
    }

    fun setupToolbarColors(showElevation: Boolean = false) {
        val isDarkMode = Util.isDarkMode(this)
        val isMainPlayer = navController.currentDestination?.id == R.id.main_player
        @ColorRes val toolbarBackgroundColor: Int
        @ColorInt val statusBarColor: Int
        val toolbarElevation: Float
        val isVideoPlayerMainView = !isAudioPlayer() && isMainPlayer


        WindowCompat.setDecorFitsSystemWindows(window, !isVideoPlayerMainView)

        binding.rootLayout.post {
            // Apply system bars top and bottom insets
            ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout) { _, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                binding.toolbar.updatePadding(0, if (isVideoPlayerMainView) insets.top else 0, 0, 0)
                binding.rootLayout.updatePadding(
                    if (isVideoPlayerMainView) insets.left else 0,
                    0,
                    if (isVideoPlayerMainView) insets.right else 0,
                    if (isVideoPlayerMainView) insets.bottom else 0
                )
                WindowInsetsCompat.CONSUMED
            }
        }

        when {
            isAudioPlayer() && isMainPlayer -> {
                toolbarElevation = 0F
                toolbarBackgroundColor = android.R.color.transparent
                statusBarColor = ContextCompat.getColor(this, R.color.grey_020_grey_800)
            }
            isVideoPlayerMainView -> {
                toolbarElevation = 0F
                toolbarBackgroundColor = R.color.white_alpha_070_grey_alpha_070
                statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
            }
            isDarkMode -> {
                toolbarElevation = 0F
                toolbarBackgroundColor = if (showElevation) {
                    R.color.action_mode_background
                } else {
                    R.color.dark_grey
                }
                statusBarColor = if (showElevation) {
                    val elevation = resources.getDimension(R.dimen.toolbar_elevation)
                    ColorUtils.getColorForElevation(this, elevation)
                } else {
                    ContextCompat.getColor(this, android.R.color.transparent)
                }
            }
            else -> {
                toolbarElevation = if (showElevation) {
                    resources.getDimension(R.dimen.toolbar_elevation)
                } else {
                    0F
                }
                toolbarBackgroundColor = if (showElevation) {
                    R.color.white
                } else {
                    android.R.color.transparent
                }
                statusBarColor = ContextCompat.getColor(this, R.color.white_dark_grey)
            }
        }

        window.statusBarColor = statusBarColor
        binding.toolbar.setBackgroundColor(ContextCompat.getColor(this, toolbarBackgroundColor))
        binding.toolbar.elevation = toolbarElevation
    }

    fun setDraggable(draggable: Boolean) {
        dragToExit.setDraggable(draggable)
    }

    private fun onDragActivated(activated: Boolean) {
        getFragmentFromNavHost(R.id.nav_host_fragment, MediaPlayerFragment::class.java)
            ?.onDragActivated(dragToExit, activated)
    }

    private fun onError(code: Int) {
        when (code) {
            MegaError.API_EOVERQUOTA -> showGeneralTransferOverQuotaWarning()
            MegaError.API_EBLOCKED -> {
                if (!isAlertDialogShown(takenDownDialog)) {
                    takenDownDialog = showTakenDownAlert(this)
                }
            }
            MegaError.API_ENOENT -> stopPlayer()
        }
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        showSnackbar(type, binding.rootLayout, content, chatId)
    }

    override fun launchActivity(intent: Intent) {
        startActivity(intent)
        stopPlayer()
    }

    @Suppress("deprecation") // TODO Migrate to registerForActivityResult()
    override fun launchActivityForResult(intent: Intent, requestCode: Int) {
        startActivityForResult(intent, requestCode)
    }

    /**
     * Shows the result of an exception.
     *
     * @param throwable The exception.
     */
    private fun manageException(throwable: Throwable) {
        if (!manageCopyMoveException(throwable) && throwable is MegaException) {
            showSnackbar(throwable.message!!)
        }
    }

    companion object {
        fun isAudioPlayer(intent: Intent?): Boolean {
            val nodeName = intent?.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME) ?: return true

            return MimeTypeList.typeForName(nodeName).isAudio
        }
    }
}
