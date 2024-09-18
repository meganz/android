package mega.privacy.android.app.mediaplayer

import mega.privacy.android.shared.resources.R as sharedR
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.contract.NameCollisionActivityContract
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.dragger.DragToExitSupport
import mega.privacy.android.app.databinding.ActivityAudioPlayerBinding
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerServiceGateway
import mega.privacy.android.app.mediaplayer.gateway.PlayerServiceViewModelGateway
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerServiceBinder
import mega.privacy.android.app.mediaplayer.trackinfo.TrackInfoFragmentArgs
import mega.privacy.android.app.presentation.extensions.getStorageState
import mega.privacy.android.app.presentation.hidenode.HiddenNodesOnboardingActivity
import mega.privacy.android.app.usecase.exception.MegaException
import mega.privacy.android.app.utils.AlertDialogUtil
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.EXTRA_SERIALIZE_STRING
import mega.privacy.android.app.utils.Constants.FILE_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.INCOMING_SHARES_ADAPTER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_COPY_FROM
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MOVE_FROM
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.LINKS_ADAPTER
import mega.privacy.android.app.utils.Constants.MEDIA_PLAYER_TOOLBAR_SHOW_HIDE_DURATION_MS
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.OUTGOING_SHARES_ADAPTER
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.app.utils.Constants.URL_FILE_LINK
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaNodeDialogUtil
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.MegaNodeUtil.getRootParentNode
import mega.privacy.android.app.utils.MenuUtils.toggleAllMenuItemsVisibility
import mega.privacy.android.app.utils.RunOnUIThreadUtils
import mega.privacy.android.app.utils.Util.isDarkMode
import mega.privacy.android.app.utils.getFragmentFromNavHost
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.BlockedMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.mobile.analytics.event.AudioPlayerHideNodeMenuItemEvent
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import timber.log.Timber
import javax.inject.Inject

/**
 * Extending MediaPlayerActivity is to declare portrait in manifest,
 * to avoid crash when set requestedOrientation.
 */
@AndroidEntryPoint
class AudioPlayerActivity : MediaPlayerActivity() {
    private lateinit var binding: ActivityAudioPlayerBinding

    private var viewingTrackInfo: TrackInfoFragmentArgs? = null

    private var serviceBound = false

    private var isHiddenNodesEnabled: Boolean = false

    private var takenDownDialog: AlertDialog? = null

    private var tempNodeId: NodeId? = null

    private val nameCollisionActivityContract = registerForActivityResult(
        NameCollisionActivityContract()
    ) { result ->
        result?.let {
            showSnackbar(SNACKBAR_TYPE, it, INVALID_HANDLE)
        }
    }

    /**
     * Inject [GetFeatureFlagValueUseCase] to the Fragment
     */
    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    private var serviceGateway: MediaPlayerServiceGateway? = null
    private var playerServiceGateway: PlayerServiceViewModelGateway? = null

    private val dragToExit by lazy {
        DragToExitSupport(
            context = this,
            coroutineScope = lifecycleScope,
            dragActivated = this::onDragActivated
        ) {
            finish()
            overridePendingTransition(0, android.R.anim.fade_out)
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            serviceGateway = null
            playerServiceGateway = null
        }

        /**
         * Called after a successful bind with our AudioPlayerService.
         */
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MediaPlayerServiceBinder) {
                serviceGateway = service.serviceGateway
                playerServiceGateway = service.playerServiceViewModelGateway

                refreshMenuOptionsVisibility()

                collectFlow(service.serviceGateway.metadataUpdate()) { metadata ->
                    viewModel.updateMetaData(metadata)
                    dragToExit.nodeChanged(
                        lifecycleOwner = this@AudioPlayerActivity,
                        handle = service.playerServiceViewModelGateway.getCurrentPlayingHandle()
                    )
                }

                collectFlow(service.playerServiceViewModelGateway.errorUpdate()) { megaException ->
                    megaException?.let {
                        this@AudioPlayerActivity.onError(it)
                    }
                }

                collectFlow(service.playerServiceViewModelGateway.itemsClearedUpdate()) { isCleared ->
                    if (isCleared == true) {
                        stopPlayer()
                    }
                }

                service.serviceGateway.monitorMediaNotAllowPlayState().onEach { notAllow ->
                    if (notAllow) {
                        showNotAllowPlayAlert()
                    }
                }.launchIn(lifecycleScope)
            }
        }
    }

    /**
     * Handle events when a Back Press is detected
     */
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            retryConnectionsAndSignalPresence()
            if (!navController.navigateUp()) {
                finish()
            }
        }
    }

    /**
     * onCreate
     */
    @androidx.annotation.OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Setup the Back Press dispatcher to receive Back Press events
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

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

        binding = ActivityAudioPlayerBinding.inflate(layoutInflater)

        lifecycleScope.launch {
            runCatching {
                isHiddenNodesEnabled = getFeatureFlagValueUseCase(AppFeatures.HiddenNodes)
                invalidateOptionsMenu()
            }.onFailure { Timber.e(it) }
        }

        setContentView(binding.root)
        addStartDownloadTransferView(binding.root)
        addNodeAttachmentView(binding.root)
        binding.toolbar.setBackgroundColor(Color.TRANSPARENT)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setupToolbar()
        setupNavDestListener()

        val playerServiceIntent = Intent(this, AudioPlayerService::class.java).putExtras(extras)

        if (savedInstanceState == null) {
            PermissionUtils.checkNotificationsPermission(this)
            if (rebuildPlaylist) {
                playerServiceIntent.setDataAndType(intent.data, intent.type)
                Util.startForegroundService(this, playerServiceIntent)
            }
        }

        bindService(playerServiceIntent, connection, Context.BIND_AUTO_CREATE)
        serviceBound = true

        setupObserver()

        if (CallUtil.participatingInACall()) {
            showNotAllowPlayAlert()
        }
    }

    @OptIn(FlowPreview::class)
    private fun setupObserver() {
        with(viewModel) {
            onStartChatFileOfflineDownload().observe(this@AudioPlayerActivity) {
                startDownloadViewModel.onSaveOfflineClicked(it)
            }

            getCollision().observe(this@AudioPlayerActivity) { collision ->
                nameCollisionActivityContract.launch(arrayListOf(collision))
            }

            onSnackbarMessage().observe(this@AudioPlayerActivity) { message ->
                showSnackbar(getString(message))
            }

            onExceptionThrown().observe(this@AudioPlayerActivity, ::manageException)

            itemToRemove.observe(this@AudioPlayerActivity) { handle ->
                playerServiceGateway?.removeItem(handle)
            }

            renameUpdate.observe(this@AudioPlayerActivity) { node ->
                node?.let {
                    MegaNodeDialogUtil.showRenameNodeDialog(
                        context = this@AudioPlayerActivity,
                        node = it,
                        snackbarShower = this@AudioPlayerActivity,
                        actionNodeCallback = object : ActionNodeCallback {
                            override fun finishRenameActionWithSuccess(newName: String) {
                                playerServiceGateway?.updateItemName(it.handle, newName)
                                //Avoid the dialog is shown repeatedly when screen is rotated.
                                viewModel.renameUpdate(null)
                            }
                        })
                }
            }
        }

        collectFlow(viewModel.menuClickEventFlow.debounce { (menuId) ->
            if (menuId == R.id.share) {
                TIMEOUT_FOR_SHARED_MENU_ITEM
            } else {
                TIMEOUT_FOR_DEFAULT_MENU_ITEM
            }
        }) { (menuId, adapterType, playingHandle, launchIntent) ->
            when (menuId) {
                R.id.save_to_device -> {
                    when (adapterType) {
                        ZIP_ADAPTER -> {
                            val mediaItem = serviceGateway?.getCurrentMediaItem()
                            mediaItem?.localConfiguration?.uri?.let { uri ->
                                playerServiceGateway?.getPlaylistItem(mediaItem.mediaId)
                                    ?.let { playlistItem ->
                                        startDownloadViewModel.onCopyUriClicked(
                                            uri = uri,
                                            name = playlistItem.nodeName,
                                        )
                                    }
                            }
                        }

                        FROM_CHAT -> {
                            saveChatNode()
                        }

                        FILE_LINK_ADAPTER -> {
                            launchIntent.getStringExtra(EXTRA_SERIALIZE_STRING)
                                ?.let { serialize ->
                                    saveFileLinkNode(serialize)
                                }
                        }

                        FOLDER_LINK_ADAPTER -> {
                            saveNodeFromFolderLink(NodeId(playingHandle))
                        }

                        else -> {
                            saveNode(NodeId(playingHandle))
                        }
                    }
                }

                R.id.properties -> {
                    serviceGateway?.getCurrentMediaItem()?.localConfiguration?.uri?.let { uri ->
                        navController.navigate(
                            AudioPlayerFragmentDirections.actionAudioPlayerToTrackInfo(
                                adapterType = adapterType,
                                fromIncomingShare = adapterType == INCOMING_SHARES_ADAPTER,
                                handle = playingHandle,
                                uri = uri.toString()
                            )
                        )
                    }
                }

                R.id.chat_import -> {
                    selectImportFolderLauncher.launch(
                        Intent(
                            this,
                            FileExplorerActivity::class.java
                        ).apply {
                            action = FileExplorerActivity.ACTION_PICK_IMPORT_FOLDER
                        }
                    )
                }

                R.id.share -> {
                    when (adapterType) {
                        OFFLINE_ADAPTER, ZIP_ADAPTER -> {
                            val mediaItem = serviceGateway?.getCurrentMediaItem()
                            playerServiceGateway?.getPlaylistItem(mediaItem?.mediaId)?.nodeName
                                ?.let { nodeName ->
                                    mediaItem?.localConfiguration?.uri?.let { uri ->
                                        FileUtil.shareUri(
                                            this,
                                            nodeName,
                                            uri
                                        )
                                    }
                                }
                        }

                        FILE_LINK_ADAPTER -> {
                            val mediaItem = serviceGateway?.getCurrentMediaItem()
                            val nodeName =
                                playerServiceGateway?.getPlaylistItem(mediaItem?.mediaId)?.nodeName
                            MegaNodeUtil.shareLink(
                                context = this,
                                fileLink = launchIntent.getStringExtra(URL_FILE_LINK),
                                title = nodeName
                            )
                        }

                        else -> {
                            playerServiceGateway?.let {
                                MegaNodeUtil.shareNode(
                                    context = this,
                                    node = megaApi.getNodeByHandle(it.getCurrentPlayingHandle())
                                )
                            }
                        }
                    }
                }

                R.id.send_to_chat -> {
                    nodeAttachmentViewModel.startAttachNodes(listOf(NodeId(playingHandle)))
                }

                R.id.get_link -> {
                    if (!MegaNodeUtil.showTakenDownNodeActionNotAvailableDialog(
                            node = megaApi.getNodeByHandle(playingHandle),
                            context = this
                        )
                    ) {
                        LinksUtil.showGetLinkActivity(this, playingHandle)
                    }
                }

                R.id.remove_link -> {
                    megaApi.getNodeByHandle(playingHandle)?.let { node ->
                        if (!MegaNodeUtil.showTakenDownNodeActionNotAvailableDialog(
                                node,
                                this
                            )
                        ) {
                            AlertsAndWarnings.showConfirmRemoveLinkDialog(this) {
                                megaApi.disableExport(
                                    node,
                                    OptionalMegaRequestListenerInterface(onRequestFinish = { _, error ->
                                        if (error.errorCode == MegaError.API_OK) {
                                            // Some times checking node.isExported immediately will still
                                            // get true, so let's add some delay here.
                                            RunOnUIThreadUtils.runDelay(500L) {
                                                refreshMenuOptionsVisibility()
                                            }
                                        }
                                    })
                                )
                            }
                        }
                    }
                }

                R.id.chat_save_for_offline -> {
                    if (getStorageState() == StorageState.PayWall) {
                        AlertsAndWarnings.showOverDiskQuotaPaywallWarning()
                    } else {
                        viewModel.saveChatNodeToOffline(
                            chatId = getChatId(),
                            messageId = getMessageId()
                        )
                    }
                }

                R.id.rename -> {
                    viewModel.renameUpdate(node = megaApi.getNodeByHandle(playingHandle))
                }

                R.id.hide -> {
                    Analytics.tracker.trackEvent(AudioPlayerHideNodeMenuItemEvent)
                    handleHideNodeClick(playingHandle = playingHandle)
                }

                R.id.unhide -> {
                    hideOrUnhideNode(playingHandle = playingHandle, hide = false)
                }

                R.id.move -> {
                    selectFolderToMoveLauncher.launch(
                        Intent(this, FileExplorerActivity::class.java).apply {
                            action = FileExplorerActivity.ACTION_PICK_MOVE_FOLDER
                            putExtra(INTENT_EXTRA_KEY_MOVE_FROM, longArrayOf(playingHandle))
                        }
                    )
                }

                R.id.copy -> {
                    if (getStorageState() == StorageState.PayWall) {
                        AlertsAndWarnings.showOverDiskQuotaPaywallWarning()
                    } else {
                        selectFolderToCopyLauncher.launch(
                            Intent(this, FileExplorerActivity::class.java).apply {
                                action = FileExplorerActivity.ACTION_PICK_COPY_FOLDER
                                putExtra(INTENT_EXTRA_KEY_COPY_FROM, longArrayOf(playingHandle))
                            }
                        )
                    }
                }

                R.id.move_to_trash -> {
                    if (adapterType == FROM_CHAT) {
                        getChatMessage().let { (chatId, message) ->
                            message?.let {
                                ChatUtil.removeAttachmentMessage(this, chatId, it)
                            }
                        }
                    } else {
                        MegaNodeDialogUtil.moveToRubbishOrRemove(
                            handle = playingHandle,
                            activity = this,
                            snackbarShower = this
                        )
                    }
                }

            }
        }
    }

    private fun showNotAllowPlayAlert() {
        showSnackbar(getString(R.string.not_allow_play_alert))
    }

    /**
     * onResume
     */
    override fun onResume() {
        super.onResume()
        refreshMenuOptionsVisibility()
    }

    /**
     * onAttachedToWindow
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        window.setFormat(PixelFormat.RGBA_8888) // Needed to fix bg gradient banding
    }

    override fun showSystemUI() {
        WindowInsetsControllerCompat(
            window,
            binding.root
        ).show(WindowInsetsCompat.Type.systemBars())
    }

    private fun setupNavDestListener() {
        navController.addOnDestinationChangedListener { _, dest, args ->
            setupToolbarColors()
            when (dest.id) {
                R.id.audio_main_player,
                -> {
                    if (dest.id == R.id.audio_main_player) {
                        supportActionBar?.title = ""
                    }
                    viewingTrackInfo = null
                }

                R.id.track_info -> {
                    supportActionBar?.title = getString(R.string.audio_track_info)
                    if (args != null) {
                        viewingTrackInfo = TrackInfoFragmentArgs.fromBundle(args)
                    }
                }
            }
            refreshMenuOptionsVisibility()
        }
    }

    /**
     * onDestroy
     */
    override fun onDestroy() {
        super.onDestroy()

        if (isFinishing) {
            dragToExit.showPreviousHiddenThumbnail()
        }
        serviceGateway?.stopAudioServiceWhenAudioPlayerClosedWithUserNotLogin()

        serviceGateway = null
        playerServiceGateway = null
        if (serviceBound) {
            unbindService(connection)
        }
        AlertDialogUtil.dismissAlertDialogIfExists(takenDownDialog)
    }

    /**
     * onCreateOptionsMenu
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        optionsMenu = menu

        menuInflater.inflate(R.menu.media_player, menu)

        menu.findItem(R.id.get_link).title =
            resources.getQuantityString(sharedR.plurals.label_share_links, 1)

        searchMenuItem = menu.findItem(R.id.action_search).apply {
            actionView?.let { searchView ->
                if (searchView is SearchView) {
                    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String): Boolean = true

                        override fun onQueryTextChange(newText: String): Boolean {
                            playerServiceGateway?.searchQueryUpdate(newText)
                            return true
                        }

                    })
                }
            }
            setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                    playerServiceGateway?.setSearchMode(true)
                    return true
                }

                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    playerServiceGateway?.searchQueryUpdate(null)
                    playerServiceGateway?.setSearchMode(false)
                    return true
                }
            })
        }

        refreshMenuOptionsVisibility()

        return true
    }

    /**
     * onOptionsItemSelected
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val launchIntent = playerServiceGateway?.getCurrentIntent() ?: return false
        val playingHandle = playerServiceGateway?.getCurrentPlayingHandle() ?: return false
        val adapterType = launchIntent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)

        when (item.itemId) {
            R.id.save_to_device,
            R.id.properties,
            R.id.chat_import,
            R.id.share,
            R.id.send_to_chat,
            R.id.get_link,
            R.id.remove_link,
            R.id.chat_save_for_offline,
            R.id.rename,
            R.id.hide,
            R.id.unhide,
            R.id.move,
            R.id.copy,
            R.id.move_to_trash,
            -> {
                viewModel.updateMenuClickEventFlow(
                    menuId = item.itemId,
                    adapterType = adapterType,
                    playingHandle = playingHandle,
                    launchIntent = launchIntent
                )
                return true
            }
        }
        return false
    }

    override fun setupToolbar() {
        setSupportActionBar(binding.toolbar)

        supportActionBar?.run {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            title = ""
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun setToolbarTitle(title: String) {
        binding.toolbar.title = title
    }

    override fun hideToolbar(animate: Boolean) {
        with(binding.toolbar) {
            if (animate) {
                animate()
                    .translationY(-measuredHeight.toFloat())
                    .setDuration(MEDIA_PLAYER_TOOLBAR_SHOW_HIDE_DURATION_MS)
                    .start()
            } else {
                animate().cancel()
                translationY = -measuredHeight.toFloat()
            }
        }
    }

    override fun showToolbar(animate: Boolean) {
        with(binding.toolbar) {
            if (animate) {
                animate()
                    .translationY(TRANSLATION_Y_ZERO)
                    .setDuration(MEDIA_PLAYER_TOOLBAR_SHOW_HIDE_DURATION_MS)
                    .start()
            } else {
                animate().cancel()
                translationY = TRANSLATION_Y_ZERO
            }
        }
    }

    override fun setupToolbarColors(showElevation: Boolean) {
        val isDarkMode = isDarkMode(this)
        val isMainPlayer = navController.currentDestination?.id == R.id.audio_main_player
        @ColorRes val toolbarBackgroundColor: Int
        @ColorInt val statusBarColor: Int
        val toolbarElevation: Float

        binding.rootLayout.setBackgroundColor(
            getColor(
                R.color.white_dark_grey
            )
        )

        when {
            isMainPlayer -> {
                toolbarElevation = TOOLBAR_ELEVATION_ZERO
                toolbarBackgroundColor = android.R.color.transparent
                statusBarColor = ContextCompat.getColor(this, R.color.grey_020_grey_800)
            }

            isDarkMode -> {
                toolbarElevation = TOOLBAR_ELEVATION_ZERO
                toolbarBackgroundColor =
                    if (showElevation) {
                        R.color.action_mode_background
                    } else {
                        R.color.dark_grey
                    }
                statusBarColor =
                    if (showElevation) {
                        val elevation = resources.getDimension(R.dimen.toolbar_elevation)
                        ColorUtils.getColorForElevation(this, elevation)
                    } else {
                        ContextCompat.getColor(this, android.R.color.transparent)
                    }
            }

            else -> {
                toolbarElevation =
                    if (showElevation) {
                        resources.getDimension(R.dimen.toolbar_elevation)
                    } else {
                        TOOLBAR_ELEVATION_ZERO
                    }
                toolbarBackgroundColor =
                    if (showElevation) {
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

    override fun setDraggable(draggable: Boolean) {
        dragToExit.setDraggable(draggable)
    }

    /**
     * Show snackbar
     * @param type
     * @param content
     * @param chatId
     */
    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        showSnackbar(type, binding.rootLayout, content, chatId)
    }


    private fun onDragActivated(activated: Boolean) {
        getFragmentFromNavHost(
            navHostId = R.id.nav_host_fragment,
            fragmentClass = AudioPlayerFragment::class.java
        )
            ?.onDragActivated(activated = activated)
    }

    private fun onError(megaException: mega.privacy.android.domain.exception.MegaException) {
        when (megaException) {
            is QuotaExceededMegaException -> showGeneralTransferOverQuotaWarning()
            is BlockedMegaException -> {
                if (!AlertDialogUtil.isAlertDialogShown(takenDownDialog)) {
                    takenDownDialog = AlertsAndWarnings.showTakenDownAlert(this)
                }
            }
        }
    }

    /**
     * Shows the result of an exception.
     *
     * @param throwable The exception.
     */
    private fun manageException(throwable: Throwable) {
        if (!manageCopyMoveException(throwable) && throwable is MegaException) {
            throwable.message?.let { errorMessage ->
                showSnackbar(errorMessage)
            }
        }
    }

    /**
     * Launch Activity and stop player
     */
    override fun launchActivity(intent: Intent) {
        startActivity(intent)
        stopPlayer()
    }

    private fun stopPlayer() {
        serviceGateway?.stopPlayer()
        finish()
    }

    private fun refreshMenuOptionsVisibility() {
        val menu = optionsMenu
        if (menu == null) {
            Timber.d("refreshMenuOptionsVisibility menu is null")
            return
        }

        val currentFragmentId = navController.currentDestination?.id
        if (currentFragmentId == null) {
            Timber.d("refreshMenuOptionsVisibility currentFragment is null")
            return
        }

        playerServiceGateway?.let {
            it.getCurrentIntent()?.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
                ?.let { adapterType ->
                    val isInSharedItems = adapterType in listOf(
                        INCOMING_SHARES_ADAPTER,
                        OUTGOING_SHARES_ADAPTER,
                        LINKS_ADAPTER,
                    )


                    when (currentFragmentId) {
                        R.id.audio_queue -> {
                            menu.toggleAllMenuItemsVisibility(false)
                            searchMenuItem?.isVisible = true
                            // Display the select option
                            menu.findItem(R.id.select).isVisible = true
                        }

                        R.id.audio_main_player, R.id.track_info -> {
                            when {
                                adapterType == OFFLINE_ADAPTER -> {
                                    menu.toggleAllMenuItemsVisibility(false)

                                    menu.findItem(R.id.properties).isVisible =
                                        currentFragmentId == R.id.audio_main_player

                                    menu.findItem(R.id.share).isVisible =
                                        currentFragmentId == R.id.audio_main_player
                                }

                                adapterType == Constants.RUBBISH_BIN_ADAPTER || megaApi.isInRubbish(
                                    megaApi.getNodeByHandle(
                                        it.getCurrentPlayingHandle()
                                    )
                                ) -> {
                                    menu.toggleAllMenuItemsVisibility(false)

                                    menu.findItem(R.id.properties).isVisible =
                                        currentFragmentId == R.id.audio_main_player

                                    val moveToTrash = menu.findItem(R.id.move_to_trash) ?: return
                                    moveToTrash.isVisible = true
                                    moveToTrash.title = getString(R.string.context_remove)
                                }

                                adapterType == FROM_CHAT -> {
                                    menu.toggleAllMenuItemsVisibility(false)

                                    menu.findItem(R.id.save_to_device).isVisible = true
                                    menu.findItem(R.id.chat_import).isVisible = true
                                    menu.findItem(R.id.chat_save_for_offline).isVisible = true

                                    menu.findItem(R.id.share).isVisible = false

                                    menu.findItem(R.id.move_to_trash)?.let { moveToTrash ->
                                        val pair = getChatMessage()
                                        val message = pair.second

                                        val canRemove = message != null
                                                && message.userHandle == megaChatApi.myUserHandle
                                                && message.isDeletable
                                        moveToTrash.isVisible = canRemove
                                        if (canRemove) {
                                            moveToTrash.title = getString(R.string.context_remove)
                                        }
                                    }
                                }

                                adapterType == FILE_LINK_ADAPTER || adapterType == ZIP_ADAPTER -> {
                                    menu.toggleAllMenuItemsVisibility(false)

                                    menu.findItem(R.id.save_to_device).isVisible = true
                                    menu.findItem(R.id.share).isVisible = true
                                }

                                adapterType == FOLDER_LINK_ADAPTER
                                        || adapterType == Constants.FROM_IMAGE_VIEWER
                                        || adapterType == Constants.FROM_ALBUM_SHARING
                                        || adapterType == Constants.VERSIONS_ADAPTER -> {
                                    menu.toggleAllMenuItemsVisibility(false)
                                    menu.findItem(R.id.save_to_device).isVisible = true
                                }

                                else -> {
                                    val node = megaApi.getNodeByHandle(it.getCurrentPlayingHandle())
                                    if (node == null) {
                                        Timber.d("refreshMenuOptionsVisibility node is null")

                                        menu.toggleAllMenuItemsVisibility(false)
                                        return
                                    }

                                    menu.toggleAllMenuItemsVisibility(true)
                                    searchMenuItem?.isVisible = false

                                    menu.findItem(R.id.save_to_device).isVisible = true
                                    // Hide the select, select all, and clear options
                                    menu.findItem(R.id.select).isVisible = false
                                    menu.findItem(R.id.remove).isVisible = false

                                    menu.findItem(R.id.properties).isVisible =
                                        currentFragmentId == R.id.audio_main_player

                                    menu.findItem(R.id.share).isVisible =
                                        currentFragmentId == R.id.audio_main_player
                                                && MegaNodeUtil.showShareOption(
                                            adapterType = adapterType,
                                            isFolderLink = false,
                                            handle = node.handle
                                        )
                                    menu.findItem(R.id.send_to_chat).isVisible = true

                                    val access = megaApi.getAccess(node)
                                    val isAccessOwner = access == MegaShare.ACCESS_OWNER

                                    menu.findItem(R.id.get_link).isVisible =
                                        isAccessOwner && !node.isExported
                                    menu.findItem(R.id.remove_link).isVisible =
                                        isAccessOwner && node.isExported

                                    menu.findItem(R.id.chat_import).isVisible = false
                                    menu.findItem(R.id.chat_save_for_offline).isVisible = false

                                    val parentNode = megaApi.getParentNode(node)
                                    val isSensitiveInherited =
                                        parentNode?.let { megaApi.isSensitiveInherited(it) } == true
                                    val isRootParentInShare =
                                        megaApi.getRootParentNode(node).isInShare
                                    val accountType = viewModel.state.value.accountType
                                    val isPaidAccount = accountType?.isPaid == true
                                    val isNodeInBackup = megaApi.isInInbox(node)


                                    val shouldShowHideNode = isHiddenNodesEnabled
                                            && (!isPaidAccount
                                            || (!isInSharedItems
                                            && !isRootParentInShare
                                            && !isNodeInBackup
                                            && !node.isMarkedSensitive
                                            && !isSensitiveInherited))

                                    val shouldShowUnhideNode = isHiddenNodesEnabled
                                            && !isInSharedItems
                                            && !isRootParentInShare
                                            && node.isMarkedSensitive
                                            && isPaidAccount
                                            && !isSensitiveInherited
                                            && !isNodeInBackup

                                    menu.findItem(R.id.hide)?.isVisible = shouldShowHideNode
                                    menu.findItem(R.id.unhide)?.isVisible = shouldShowUnhideNode

                                    when (access) {
                                        MegaShare.ACCESS_READWRITE,
                                        MegaShare.ACCESS_READ,
                                        MegaShare.ACCESS_UNKNOWN,
                                        -> {
                                            menu.findItem(R.id.rename).isVisible = false
                                            menu.findItem(R.id.move).isVisible = false
                                        }

                                        MegaShare.ACCESS_FULL,
                                        MegaShare.ACCESS_OWNER,
                                        -> {
                                            menu.findItem(R.id.rename).isVisible = true
                                            menu.findItem(R.id.move).isVisible = true
                                        }
                                    }

                                    menu.findItem(R.id.move_to_trash).isVisible =
                                        node.parentHandle != megaApi.rubbishNode?.handle
                                                && (access == MegaShare.ACCESS_FULL
                                                || access == MegaShare.ACCESS_OWNER)

                                    menu.findItem(R.id.copy).isVisible = true
                                }
                            }
                        }
                    }

                    // After establishing the Options menu, check if read-only properties should be applied
                    checkIfShouldApplyReadOnlyState(menu)
                } ?: run {
                Timber.d("refreshMenuOptionsVisibility null adapterType")
                menu.toggleAllMenuItemsVisibility(false)
            }
        } ?: run {
            Timber.d("refreshMenuOptionsVisibility null service")
            menu.toggleAllMenuItemsVisibility(false)
        }
    }

    /**
     * Checks and applies read-only restrictions (unable to Favourite, Rename, Move, or Move to Rubbish Bin)
     * on the Options toolbar if the [MegaNode] is a Backup node.
     *
     * @param menu The Options Menu
     */
    private fun checkIfShouldApplyReadOnlyState(menu: Menu) {
        playerServiceGateway?.getCurrentPlayingHandle()?.let { playingHandle ->
            megaApi.getNodeByHandle(playingHandle)?.let { node ->
                if (megaApi.isInInbox(node)) {
                    with(menu) {
                        findItem(R.id.move_to_trash).isVisible = false
                        findItem(R.id.move).isVisible = false
                        findItem(R.id.rename).isVisible = false
                    }
                }
            }
        }
    }

    private fun handleHideNodeClick(playingHandle: Long) {
        val (isPaid, isHiddenNodesOnboarded) = with(viewModel.state.value) {
            (this.accountType?.isPaid ?: false) to this.isHiddenNodesOnboarded
        }

        if (!isPaid) {
            val intent = HiddenNodesOnboardingActivity.createScreen(
                context = this,
                isOnboarding = false,
            )
            hiddenNodesOnboardingLauncher.launch(intent)
            this.overridePendingTransition(0, 0)
        } else if (isHiddenNodesOnboarded) {
            hideOrUnhideNode(
                playingHandle = playingHandle,
                hide = true,
            )
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

        hideOrUnhideNode(
            playingHandle = tempNodeId?.longValue ?: 0,
            hide = true,
        )

        val message =
            resources.getQuantityString(
                R.plurals.hidden_nodes_result_message,
                1,
                1,
            )
        mega.privacy.android.app.utils.Util.showSnackbar(this, message)
    }

    private fun hideOrUnhideNode(playingHandle: Long, hide: Boolean) =
        megaApi.getNodeByHandle(playingHandle)?.let { node ->
            megaApi.setNodeSensitive(
                node,
                hide,
                OptionalMegaRequestListenerInterface(onRequestFinish = { _, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        // Some times checking node.isMarkedSensitive immediately will still
                        // get true, so let's add some delay here.
                        RunOnUIThreadUtils.runDelay(500L) {
                            refreshMenuOptionsVisibility()
                        }
                    }
                })
            )
        }
}
