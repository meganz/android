package mega.privacy.android.app.mediaplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.database.ContentObserver
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.provider.Settings
import android.provider.Settings.System.ACCELEROMETER_ROTATION
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.snackbar.Snackbar
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.OfflineFileInfoActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.dragger.DragToExitSupport
import mega.privacy.android.app.databinding.ActivityVideoPlayerBinding
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerServiceBinder
import mega.privacy.android.app.mediaplayer.service.VideoPlayerService
import mega.privacy.android.app.mediaplayer.trackinfo.TrackInfoFragment
import mega.privacy.android.app.mediaplayer.trackinfo.TrackInfoFragmentArgs
import mega.privacy.android.app.presentation.extensions.getStorageState
import mega.privacy.android.app.presentation.fileinfo.FileInfoActivity
import mega.privacy.android.app.usecase.exception.MegaException
import mega.privacy.android.app.utils.AlertDialogUtil
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.EVENT_NOT_ALLOW_PLAY
import mega.privacy.android.app.utils.Constants.EXTRA_SERIALIZE_STRING
import mega.privacy.android.app.utils.Constants.FILE_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.FROM_INBOX
import mega.privacy.android.app.utils.Constants.FROM_INCOMING_SHARES
import mega.privacy.android.app.utils.Constants.HANDLE
import mega.privacy.android.app.utils.Constants.INBOX_ADAPTER
import mega.privacy.android.app.utils.Constants.INCOMING_SHARES_ADAPTER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_COPY_FROM
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FIRST_LEVEL
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FROM
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.MEDIA_PLAYER_TOOLBAR_SHOW_HIDE_DURATION_MS
import mega.privacy.android.app.utils.Constants.NAME
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.RECENTS_ADAPTER
import mega.privacy.android.app.utils.Constants.SEARCH_ADAPTER
import mega.privacy.android.app.utils.Constants.URL_FILE_LINK
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaNodeDialogUtil
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.RunOnUIThreadUtils
import mega.privacy.android.app.utils.Util.isDarkMode
import mega.privacy.android.app.utils.getFragmentFromNavHost
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.domain.entity.StorageState
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import org.jetbrains.anko.configuration
import timber.log.Timber

/**
 * Extending MediaPlayerActivity is to declare portrait in manifest,
 * to avoid crash when set requestedOrientation.
 */
class VideoPlayerActivity : MediaPlayerActivity() {
    private lateinit var binding: ActivityVideoPlayerBinding

    private var viewingTrackInfo: TrackInfoFragmentArgs? = null

    private val videoViewModel: VideoPlayerViewModel by viewModels()

    private var serviceBound = false

    private var takenDownDialog: AlertDialog? = null

    private var currentOrientation: Int = SCREEN_ORIENTATION_SENSOR_PORTRAIT

    private val dragToExit by lazy {
        DragToExitSupport(
            context = this,
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
                    binding.toolbar.title =
                        if (configuration.orientation == ORIENTATION_LANDSCAPE) {
                            metadata.title ?: metadata.nodeName
                        } else {
                            ""
                        }
                    dragToExit.nodeChanged(
                        service.playerServiceViewModelGateway.getCurrentPlayingHandle()
                    )
                }

                collectFlow(service.serviceGateway.videoSizeUpdate()) { (width, height) ->
                    val rotationMode = Settings.System.getInt(
                        contentResolver,
                        ACCELEROMETER_ROTATION,
                        SCREEN_BRIGHTNESS_MODE_MANUAL
                    )

                    currentOrientation =
                        if (width > height) {
                            SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        } else {
                            SCREEN_ORIENTATION_SENSOR_PORTRAIT
                        }

                    requestedOrientation =
                        if (rotationMode == SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                            SCREEN_ORIENTATION_SENSOR
                        } else {
                            currentOrientation
                        }
                }

                collectFlow(service.playerServiceViewModelGateway.errorUpdate()) { errorCode ->
                    this@VideoPlayerActivity.onError(errorCode)
                }
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

    override fun onCreate(savedInstanceState: Bundle?) {
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

        if (savedInstanceState != null) {
            nodeAttacher.restoreState(savedInstanceState)
            nodeSaver.restoreState(savedInstanceState)
        }

        currentOrientation = configuration.orientation
        observeRotationSettingsChange()

        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)

        setContentView(dragToExit.wrapContentView(binding.root))
        dragToExit.observeThumbnailLocation(this, intent)

        binding.toolbar.apply {
            collapseIcon =
                AppCompatResources.getDrawable(
                    this@VideoPlayerActivity,
                    androidx.appcompat.R.drawable.abc_ic_ab_back_material
                )
            collapseIcon?.setTint(Color.WHITE)
        }.post {
            updateToolbar(videoViewModel.isLockUpdate.value)
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setupToolbar()
        setupNavDestListener()

        val playerServiceIntent = Intent(this, VideoPlayerService::class.java).putExtras(extras)

        if (rebuildPlaylist && savedInstanceState == null) {
            playerServiceIntent.setDataAndType(intent.data, intent.type)
            startService(playerServiceIntent)
        }

        bindService(playerServiceIntent, connection, Context.BIND_AUTO_CREATE)
        serviceBound = true

        setupObserver()

        if (savedInstanceState == null) {
            // post to next UI cycle so that MediaPlayerFragment's onCreateView is called
            RunOnUIThreadUtils.post {
                getFragmentFromNavHost(
                    navHostId = R.id.nav_host_fragment,
                    fragmentClass = VideoPlayerFragment::class.java
                )?.runEnterAnimation(dragToExit)
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

    private fun setupObserver() {
        with(viewModel) {
            getCollision().observe(this@VideoPlayerActivity) { collision ->
                nameCollisionActivityContract?.launch(arrayListOf(collision))
            }

            onSnackbarMessage().observe(this@VideoPlayerActivity) { message ->
                showSnackbarForVideoPlayer(getString(message))
            }

            onExceptionThrown().observe(this@VideoPlayerActivity, ::manageException)

            itemToRemove.observe(this@VideoPlayerActivity) { handle ->
                playerServiceGateway?.removeItem(handle)
            }

            renameUpdate.observe(this@VideoPlayerActivity) { node ->
                node?.let {
                    MegaNodeDialogUtil.showRenameNodeDialog(
                        context = this@VideoPlayerActivity,
                        node = it,
                        snackbarShower = this@VideoPlayerActivity,
                        actionNodeCallback = object : ActionNodeCallback {
                            override fun finishRenameActionWithSuccess(newName: String) {
                                playerServiceGateway?.updateItemName(it.handle, newName)
                                updateTrackInfoNodeNameIfNeeded(it.handle, newName)
                                //Avoid the dialog is shown repeatedly when screen is rotated.
                                viewModel.renameUpdate(null)
                            }
                        })
                }
            }
        }

        collectFlow(videoViewModel.isLockUpdate) { isLock ->
            updateToolbar(isLock)
        }
    }

    private val rotationContentObserver by lazy(LazyThreadSafetyMode.NONE) {
        object : ContentObserver(Handler(mainLooper)) {
            override fun onChange(selfChange: Boolean) {
                val rotationMode = Settings.System.getInt(
                    contentResolver,
                    ACCELEROMETER_ROTATION,
                    SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                requestedOrientation =
                    if (rotationMode == SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                        SCREEN_ORIENTATION_SENSOR
                    } else {
                        currentOrientation
                    }
            }
        }
    }

    private fun observeRotationSettingsChange() {
        contentResolver.registerContentObserver(
            Settings.System.getUriFor(ACCELEROMETER_ROTATION),
            true,
            rotationContentObserver
        )
    }

    private fun showNotAllowPlayAlert() {
        showSnackbarForVideoPlayer(getString(R.string.not_allow_play_alert))
    }

    override fun onResume() {
        super.onResume()
        refreshMenuOptionsVisibility()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        nodeAttacher.saveState(outState)
        nodeSaver.saveState(outState)
    }

    private fun setupNavDestListener() {
        navController.addOnDestinationChangedListener { _, dest, args ->
            setupToolbarColors()
            when (dest.id) {
                R.id.main_player,
                R.id.playlist,
                -> {
                    if (dest.id == R.id.main_player) {
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

    override fun onDestroy() {
        super.onDestroy()

        contentResolver.unregisterContentObserver(rotationContentObserver)
        if (isFinishing) {
            serviceGateway?.mainPlayerUIClosed()
            dragToExit.showPreviousHiddenThumbnail()
        }
        serviceGateway = null
        if (serviceBound) {
            unbindService(connection)
        }
        nodeSaver.destroy()
        if (isFinishing) {
            MediaPlayerService.resumeAudioPlayer(this)
        }
        AlertDialogUtil.dismissAlertDialogIfExists(takenDownDialog)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        optionsMenu = menu

        menuInflater.inflate(R.menu.menu_video_player, menu)

        menu.findItem(R.id.get_link).title = resources.getQuantityString(R.plurals.get_links, 1)

        searchMenuItem = menu.findItem(R.id.action_search).apply {
            actionView?.let { searchView ->
                if (searchView is SearchView) {
                    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String): Boolean {
                            return true
                        }

                        override fun onQueryTextChange(newText: String): Boolean {
                            playerServiceGateway?.searchQueryUpdate(newText)
                            return true
                        }

                    })
                }
            }
            setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                    return true
                }

                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    playerServiceGateway?.searchQueryUpdate(null)
                    return true
                }
            })
        }

        refreshMenuOptionsVisibility()

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val launchIntent = playerServiceGateway?.getCurrentIntent() ?: return false
        val playingHandle = playerServiceGateway?.getCurrentPlayingHandle() ?: return false
        val adapterType = launchIntent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)

        when (item.itemId) {
            R.id.save_to_device -> {
                videoViewModel.sendSaveToDeviceButtonClickedEvent()
                when (adapterType) {
                    OFFLINE_ADAPTER -> nodeSaver.saveOfflineNode(
                        handle = playingHandle,
                        fromMediaViewer = true
                    )

                    ZIP_ADAPTER -> {
                        val mediaItem = serviceGateway?.getCurrentMediaItem()
                        val uri = mediaItem?.localConfiguration?.uri ?: return false
                        val playlistItem =
                            playerServiceGateway?.getPlaylistItem(mediaItem.mediaId) ?: return false
                        nodeSaver.saveUri(
                            uri = uri,
                            name = playlistItem.nodeName,
                            size = playlistItem.size,
                            fromMediaViewer = true
                        )
                    }

                    FROM_CHAT -> {
                        getChatMessageNode()?.let { node ->
                            nodeSaver.saveNode(
                                node = node,
                                highPriority = true,
                                fromMediaViewer = true,
                                needSerialize = true
                            )
                        }
                    }

                    FILE_LINK_ADAPTER -> {
                        launchIntent.getStringExtra(EXTRA_SERIALIZE_STRING)?.let { serialize ->
                            MegaNode.unserialize(serialize)?.let { currentDocument ->
                                Timber.d("currentDocument NOT NULL")
                                nodeSaver.saveNode(
                                    currentDocument,
                                    isFolderLink = false,
                                    fromMediaViewer = true,
                                    needSerialize = true
                                )
                            } ?: Timber.w("currentDocument is NULL")
                        }
                    }

                    else -> {
                        nodeSaver.saveHandle(
                            handle = playingHandle,
                            isFolderLink = adapterType == FOLDER_LINK_ADAPTER,
                            fromMediaViewer = true
                        )
                    }
                }

                return true
            }

            R.id.properties -> {
                videoViewModel.sendInfoButtonClickedEvent()
                val intent: Intent
                if (adapterType == OFFLINE_ADAPTER) {
                    intent = Intent(this, OfflineFileInfoActivity::class.java).apply {
                        putExtra(HANDLE, playingHandle.toString())
                    }
                    Timber.d("onOptionsItemSelected properties offline handle $playingHandle")
                } else {
                    val node = megaApi.getNodeByHandle(playingHandle)
                    if (node == null) {
                        Timber.e("onOptionsItemSelected properties non-offline null node")
                        return false
                    }
                    intent = Intent(this, FileInfoActivity::class.java).apply {
                        putExtra(HANDLE, playingHandle)
                        putExtra(NAME, node.name)
                    }

                    val fromIncoming =
                        (adapterType == SEARCH_ADAPTER || adapterType == RECENTS_ADAPTER) &&
                                NodeController(this).nodeComesFromIncoming(node)

                    when {
                        adapterType == INCOMING_SHARES_ADAPTER || fromIncoming -> {
                            intent.putExtra(
                                INTENT_EXTRA_KEY_FROM,
                                FROM_INCOMING_SHARES
                            )
                            intent.putExtra(INTENT_EXTRA_KEY_FIRST_LEVEL, false)
                        }

                        adapterType == INBOX_ADAPTER -> {
                            intent.putExtra(
                                INTENT_EXTRA_KEY_FROM,
                                FROM_INBOX
                            )
                        }
                    }
                }
                startActivity(intent)
                return true
            }

            R.id.chat_import -> {
                selectImportFolderLauncher.launch(
                    Intent(this, FileExplorerActivity::class.java).apply {
                        action = FileExplorerActivity.ACTION_PICK_IMPORT_FOLDER
                    }
                )
                return true
            }

            R.id.share -> {
                videoViewModel.sendShareButtonClickedEvent()
                when (adapterType) {
                    OFFLINE_ADAPTER, ZIP_ADAPTER -> {
                        val mediaItem = serviceGateway?.getCurrentMediaItem()
                        val nodeName =
                            playerServiceGateway?.getPlaylistItem(mediaItem?.mediaId)?.nodeName
                                ?: return false
                        val uri = mediaItem?.localConfiguration?.uri ?: return false

                        FileUtil.shareUri(this, nodeName, uri)
                    }

                    FILE_LINK_ADAPTER -> {
                        MegaNodeUtil.shareLink(
                            context = this,
                            fileLink = launchIntent.getStringExtra(URL_FILE_LINK)
                        )
                    }

                    else -> {
                        playerServiceGateway?.run {
                            MegaNodeUtil.shareNode(
                                context = this@VideoPlayerActivity,
                                node = megaApi.getNodeByHandle(getCurrentPlayingHandle())
                            )
                        }
                    }
                }
                return true
            }

            R.id.send_to_chat -> {
                videoViewModel.sendSendToChatButtonClickedEvent()
                nodeAttacher.attachNode(handle = playingHandle)
                return true
            }

            R.id.get_link -> {
                videoViewModel.sendGetLinkButtonClickedEvent()
                if (!MegaNodeUtil.showTakenDownNodeActionNotAvailableDialog(
                        node = megaApi.getNodeByHandle(playingHandle),
                        context = this
                    )
                ) {
                    LinksUtil.showGetLinkActivity(this, playingHandle)
                }
                return true
            }

            R.id.remove_link -> {
                videoViewModel.sendRemoveLinkButtonClickedEvent()
                megaApi.getNodeByHandle(playingHandle)?.let { node ->
                    if (!MegaNodeUtil.showTakenDownNodeActionNotAvailableDialog(node, this)) {
                        AlertsAndWarnings.showConfirmRemoveLinkDialog(this) {
                            megaApi.disableExport(
                                node,
                                OptionalMegaRequestListenerInterface(
                                    onRequestFinish = { _, error ->
                                        if (error.errorCode == MegaError.API_OK) {
                                            // Some times checking node.isExported immediately will still
                                            // get true, so let's add some delay here.
                                            RunOnUIThreadUtils.runDelay(100L) {
                                                refreshMenuOptionsVisibility()
                                            }
                                        }
                                    })
                            )
                        }
                    }
                }
                return true
            }

            R.id.chat_save_for_offline -> {
                PermissionUtils.checkNotificationsPermission(this)
                getChatMessage().let { (chatId, message) ->
                    message?.let {
                        ChatController(this).saveForOffline(
                            it.megaNodeList,
                            megaChatApi.getChatRoom(chatId),
                            true,
                            this
                        )
                    }
                }
                return true
            }

            R.id.rename -> {
                viewModel.renameUpdate(node = megaApi.getNodeByHandle(playingHandle))
                return true
            }

            R.id.move -> {
                selectFolderToMoveLauncher.launch(
                    Intent(this, FileExplorerActivity::class.java).apply {
                        action = FileExplorerActivity.ACTION_PICK_MOVE_FOLDER
                        putExtra(Constants.INTENT_EXTRA_KEY_MOVE_FROM, longArrayOf(playingHandle))
                    }
                )
                return true
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
                return true
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
                return true
            }
        }
        return false
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
        navHostFragment.childFragmentManager.fragments.firstOrNull()?.let { firstChild ->
            if (firstChild is TrackInfoFragment) {
                firstChild.updateNodeNameIfNeeded(handle, newName)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        nodeSaver.handleRequestPermissionsResult(requestCode = requestCode)
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
        hideSystemUI()
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
        showSystemUI()
    }

    private fun hideSystemUI() {
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun showSystemUI() {
        WindowInsetsControllerCompat(
            window,
            binding.root
        ).show(WindowInsetsCompat.Type.systemBars())
    }

    /**
     * Update toolbar
     *
     * @param isHide true is hidden, otherwise is shown
     */
    private fun updateToolbar(isHide: Boolean) {
        with(binding.toolbar) {
            animate().cancel()
            translationY =
                if (isHide) {
                    -measuredHeight.toFloat()
                } else {
                    TRANSLATION_Y_ZERO
                }
        }
    }

    override fun setupToolbarColors(showElevation: Boolean) {
        val isDarkMode = isDarkMode(this)
        val isMainPlayer = navController.currentDestination?.id == R.id.main_player
        val isPlaylist = navController.currentDestination?.id == R.id.playlist
        @ColorRes val toolbarBackgroundColor: Int
        @ColorInt val statusBarColor: Int
        val toolbarElevation: Float

        val elevationStatusBarColor =
            if (showElevation) {
                val elevation = resources.getDimension(R.dimen.toolbar_elevation)
                ColorUtils.getColorForElevation(this, elevation)
            } else {
                ContextCompat.getColor(this, android.R.color.transparent)
            }

        val elevationToolbarBackgroundColor =
            if (showElevation) {
                R.color.action_mode_background
            } else {
                R.color.dark_grey
            }

        WindowCompat.setDecorFitsSystemWindows(window, !isMainPlayer || !isPlaylist)

        updatePaddingForSystemUI(isMainPlayer, isPlaylist)

        binding.rootLayout.setBackgroundColor(getColor(R.color.dark_grey))

        when {
            (isMainPlayer || isPlaylist) && !isDarkMode -> {
                moveToDarkModeUI()
                toolbarElevation = TOOLBAR_ELEVATION_ZERO
                if (isMainPlayer) {
                    toolbarBackgroundColor = R.color.grey_alpha_070
                    statusBarColor = ContextCompat.getColor(this, R.color.dark_grey)
                } else {
                    toolbarBackgroundColor = elevationToolbarBackgroundColor
                    statusBarColor = elevationStatusBarColor
                }
            }

            isDarkMode -> {
                toolbarElevation = TOOLBAR_ELEVATION_ZERO
                toolbarBackgroundColor = elevationToolbarBackgroundColor
                statusBarColor = elevationStatusBarColor
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

    private fun moveToDarkModeUI() {
        with(binding.toolbar) {
            context.setTheme(R.style.videoPlayerToolbarThemeDark)
            navigationIcon?.setTint(Color.WHITE)
            setTitleTextColor(Color.WHITE)
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightNavigationBars = false
            isAppearanceLightStatusBars = false
        }
        window.navigationBarColor = getColor(R.color.dark_grey)
    }

    private fun updatePaddingForSystemUI(
        isVideoPlayerMainView: Boolean,
        isVideoPlaylist: Boolean,
    ) {
        binding.rootLayout.post {
            ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout) { _, windowInsets ->
                if (isVideoPlayerMainView || isVideoPlaylist) {
                    windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).let { insets ->
                        val horizontalInsets: Pair<Int, Int> =
                            if (resources.configuration.orientation == ORIENTATION_LANDSCAPE) {
                                // If the navigation bar at left side, add the padding that the
                                // width is equals height of status bar for right side.
                                if (insets.left > insets.right) {
                                    Pair(insets.left, Integer.max(insets.top, insets.bottom))
                                } else {
                                    Pair(Integer.max(insets.top, insets.bottom), insets.right)
                                }
                            } else {
                                Pair(0, 0)
                            }
                        binding.toolbar.updatePadding(
                            left = 0,
                            top = insets.top,
                            right = 0,
                            bottom = 0
                        )
                        binding.rootLayout.updatePadding(
                            left = horizontalInsets.first,
                            top = 0,
                            right = horizontalInsets.second,
                            bottom = insets.bottom
                        )
                    }
                }
                WindowInsetsCompat.CONSUMED
            }
        }
    }

    override fun setDraggable(draggable: Boolean) {
        dragToExit.setDraggable(draggable)
    }

    private fun onDragActivated(activated: Boolean) {
        getFragmentFromNavHost(
            navHostId = R.id.nav_host_fragment,
            fragmentClass = VideoPlayerFragment::class.java
        )?.onDragActivated(dragToExit = dragToExit, activated = activated)
    }

    /**
     * Show snackbar
     * @param type
     * @param content
     * @param chatId
     */
    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        content?.let {
            showSnackbarForVideoPlayer(it)
        }
    }

    private fun onError(code: Int) {
        when (code) {
            MegaError.API_EOVERQUOTA -> showGeneralTransferOverQuotaWarning()
            MegaError.API_EBLOCKED -> {
                if (!AlertDialogUtil.isAlertDialogShown(takenDownDialog)) {
                    takenDownDialog = AlertsAndWarnings.showTakenDownAlert(this)
                }
            }

            MegaError.API_ENOENT -> stopPlayer()
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
                showSnackbarForVideoPlayer(errorMessage)
            }
        }
    }

    /**
     * Show the customized snackbar for video player because video player always shows dark mode.
     *
     * @param message the message that will be shown
     */
    internal fun showSnackbarForVideoPlayer(message: String) {
        Snackbar.make(binding.rootLayout, message, Snackbar.LENGTH_LONG).let { snackbar ->
            with(snackbar.view as Snackbar.SnackbarLayout) {
                setBackgroundColor(getColor(R.color.white))
                findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                    .setTextColor(getColor(R.color.dark_grey))
            }
            snackbar.show()
        }
    }
}
