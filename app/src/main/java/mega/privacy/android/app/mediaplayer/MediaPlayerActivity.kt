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
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.provider.Settings
import android.provider.Settings.System.ACCELEROMETER_ROTATION
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
import android.provider.Settings.System.getUriFor
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.exoplayer2.util.Util.startForegroundService
import com.google.android.material.snackbar.Snackbar
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.OfflineFileInfoActivity
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.components.dragger.DragToExitSupport
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.databinding.ActivityMediaPlayerBinding
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerServiceGateway
import mega.privacy.android.app.mediaplayer.gateway.PlayerServiceViewModelGateway
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerServiceBinder
import mega.privacy.android.app.mediaplayer.service.VideoPlayerService
import mega.privacy.android.app.mediaplayer.trackinfo.TrackInfoFragment
import mega.privacy.android.app.mediaplayer.trackinfo.TrackInfoFragmentArgs
import mega.privacy.android.app.presentation.fileinfo.FileInfoActivity
import mega.privacy.android.app.usecase.exception.MegaException
import mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.AlertsAndWarnings.showSaveToDeviceConfirmDialog
import mega.privacy.android.app.utils.AlertsAndWarnings.showTakenDownAlert
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil.removeAttachmentMessage
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.EVENT_NOT_ALLOW_PLAY
import mega.privacy.android.app.utils.Constants.EXTRA_SERIALIZE_STRING
import mega.privacy.android.app.utils.Constants.FILE_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.FROM_IMAGE_VIEWER
import mega.privacy.android.app.utils.Constants.FROM_INBOX
import mega.privacy.android.app.utils.Constants.FROM_INCOMING_SHARES
import mega.privacy.android.app.utils.Constants.HANDLE
import mega.privacy.android.app.utils.Constants.INBOX_ADAPTER
import mega.privacy.android.app.utils.Constants.INCOMING_SHARES_ADAPTER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FIRST_LEVEL
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FROM
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IMPORT_TO
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MOVE_HANDLES
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MOVE_TO
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MSG_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.MEDIA_PLAYER_TOOLBAR_SHOW_HIDE_DURATION_MS
import mega.privacy.android.app.utils.Constants.NAME
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.RECENTS_ADAPTER
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_FOLDER_TO_COPY
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_FOLDER_TO_MOVE
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_IMPORT_FOLDER
import mega.privacy.android.app.utils.Constants.RUBBISH_BIN_ADAPTER
import mega.privacy.android.app.utils.Constants.SEARCH_ADAPTER
import mega.privacy.android.app.utils.Constants.URL_FILE_LINK
import mega.privacy.android.app.utils.Constants.VERSIONS_ADAPTER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.app.utils.FileUtil.shareUri
import mega.privacy.android.app.utils.LinksUtil
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
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.getFragmentFromNavHost
import mega.privacy.android.app.utils.permission.PermissionUtils
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import org.jetbrains.anko.configuration
import timber.log.Timber
import java.lang.Integer.max

/**
 * Media player Activity
 */
@AndroidEntryPoint
abstract class MediaPlayerActivity : PasscodeActivity(), SnackbarShower, ActivityLauncher {

    private val viewModel: MediaPlayerViewModel by viewModels()

    private lateinit var binding: ActivityMediaPlayerBinding

    private lateinit var actionBar: ActionBar
    private lateinit var navController: NavController

    private var optionsMenu: Menu? = null
    private var searchMenuItem: MenuItem? = null

    private var viewingTrackInfo: TrackInfoFragmentArgs? = null

    private var serviceBound = false
    private var serviceGateway: MediaPlayerServiceGateway? = null
    private var playerServiceViewModelGateway: PlayerServiceViewModelGateway? = null

    private var takenDownDialog: AlertDialog? = null

    private var currentOrientation: Int = SCREEN_ORIENTATION_SENSOR_PORTRAIT

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
            serviceGateway = null
            playerServiceViewModelGateway = null
        }

        /**
         * Called after a successful bind with our AudioPlayerService.
         */
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MediaPlayerServiceBinder) {
                serviceGateway = service.serviceGateway
                playerServiceViewModelGateway = service.playerServiceViewModelGateway

                refreshMenuOptionsVisibility()

                service.serviceGateway.metadataUpdate()
                    .flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED).onEach { metadata ->
                        binding.toolbar.title =
                            if (configuration.orientation == ORIENTATION_LANDSCAPE) {
                                metadata.title ?: metadata.nodeName
                            } else {
                                ""
                            }
                        dragToExit.nodeChanged(service.playerServiceViewModelGateway.getCurrentPlayingHandle())
                    }.launchIn(lifecycleScope)

                service.serviceGateway.videoSizeUpdate()
                    .flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
                    .onEach { (width, height) ->
                        val rotationMode = Settings.System.getInt(
                            contentResolver,
                            ACCELEROMETER_ROTATION,
                            SCREEN_BRIGHTNESS_MODE_MANUAL
                        )
                        currentOrientation = if (width > height) {
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
                    }.launchIn(lifecycleScope)

                service.playerServiceViewModelGateway.errorUpdate()
                    .flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED).onEach { errorCode ->
                        this@MediaPlayerActivity.onError(errorCode)
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

        val isAudioPlayer = isAudioPlayer(intent)

        if (!isAudioPlayer) {
            currentOrientation = configuration.orientation
            observeRotationSettingsChange()
        }

        binding = ActivityMediaPlayerBinding.inflate(layoutInflater)

        if (isAudioPlayer()) {
            setContentView(binding.root)
            binding.toolbar.setBackgroundColor(Color.TRANSPARENT)
        } else {
            setContentView(dragToExit.wrapContentView(binding.root))
            dragToExit.observeThumbnailLocation(this, intent)
            binding.toolbar.run {
                collapseIcon = AppCompatResources.getDrawable(
                    this@MediaPlayerActivity,
                    androidx.appcompat.R.drawable.abc_ic_ab_back_material
                )
                collapseIcon?.setTint(Color.WHITE)
            }
            binding.toolbar.post {
                updateToolbar(viewModel.isLockUpdate.value)
            }
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

        setupObserver()

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

        if (savedInstanceState == null && isAudioPlayer) {
            PermissionUtils.checkNotificationsPermission(this)
        }
    }

    private fun setupObserver() {
        viewModel.getCollision().observe(this) { collision ->
            nameCollisionActivityContract?.launch(arrayListOf(collision))
        }

        viewModel.onSnackbarMessage().observe(this) { message ->
            showSnackbar(getString(message))
        }

        viewModel.onExceptionThrown().observe(this, ::manageException)

        viewModel.itemToRemove.observe(this) { handle ->
            playerServiceViewModelGateway?.removeItem(handle)
        }

        viewModel.renameUpdate.observe(this) { node ->
            node?.let {
                showRenameNodeDialog(this, it, this, object : ActionNodeCallback {
                    override fun finishRenameActionWithSuccess(newName: String) {
                        playerServiceViewModelGateway?.updateItemName(it.handle, newName)
                        updateTrackInfoNodeNameIfNeeded(it.handle, newName)
                        //Avoid the dialog is shown repeatedly when screen is rotated.
                        viewModel.renameUpdate(null)
                    }
                })
            }
        }

        viewModel.isLockUpdate.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
            .onEach { isLock ->
                updateToolbar(isLock)
            }.launchIn(lifecycleScope)
    }

    private fun observeRotationSettingsChange() {
        contentResolver.registerContentObserver(getUriFor(ACCELEROMETER_ROTATION),
            true,
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
            })
    }

    private fun showNotAllowPlayAlert() {
        showSnackbar(getString(R.string.not_allow_play_alert))
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

    /**
     * Judge the player whether is audio player
     *
     * @return true is audio player, otherwise is false.
     */
    abstract fun isAudioPlayer(): Boolean

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        nodeAttacher.saveState(outState)
        nodeSaver.saveState(outState)
    }

    private fun stopPlayer() {
        serviceGateway?.stopAudioPlayer()
        finish()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.run {
            actionBar = this
        }
        actionBar.setHomeButtonEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.title = ""

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
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
                    actionBar.title = getString(R.string.audio_track_info)

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
            serviceGateway?.mainPlayerUIClosed()
            dragToExit.showPreviousHiddenThumbnail()
        }

        serviceGateway = null
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

        menuInflater.inflate(
            if (isAudioPlayer()) {
                R.menu.media_player
            } else {
                R.menu.menu_video_player
            }, menu
        )

        menu.findItem(R.id.get_link).title =
            resources.getQuantityString(R.plurals.get_links, 1)

        searchMenuItem = menu.findItem(R.id.action_search)

        val searchView = searchMenuItem?.actionView
        if (searchView is SearchView) {
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    playerServiceViewModelGateway?.searchQueryUpdate(newText)
                    return true
                }

            })
        }

        searchMenuItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                playerServiceViewModelGateway?.searchQueryUpdate(null)
                return true
            }
        })

        refreshMenuOptionsVisibility()

        return true
    }

    private fun refreshMenuOptionsVisibility() {
        val menu = optionsMenu
        if (menu == null) {
            Timber.d("refreshMenuOptionsVisibility menu is null")
            return
        }

        val currentFragment = navController.currentDestination?.id
        if (currentFragment == null) {
            Timber.d("refreshMenuOptionsVisibility currentFragment is null")
            return
        }

        playerServiceViewModelGateway?.run {
            val adapterType =
                getCurrentIntent()?.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)

            adapterType?.run {
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
                            || megaApi.isInRubbish(megaApi.getNodeByHandle(getCurrentPlayingHandle()))
                        ) {
                            menu.toggleAllMenuItemsVisibility(false)

                            menu.findItem(R.id.properties).isVisible =
                                currentFragment == R.id.main_player

                            val moveToTrash = menu.findItem(R.id.move_to_trash) ?: return
                            moveToTrash.isVisible = true
                            moveToTrash.title =
                                getString(R.string.context_remove)

                            return
                        }

                        if (adapterType == FROM_CHAT) {
                            menu.toggleAllMenuItemsVisibility(false)

                            menu.findItem(R.id.save_to_device).isVisible = true
                            menu.findItem(R.id.chat_import).isVisible = true
                            menu.findItem(R.id.chat_save_for_offline).isVisible = true


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
                            moveToTrash.title =
                                getString(R.string.context_remove)

                            return
                        }

                        if (adapterType == FILE_LINK_ADAPTER || adapterType == ZIP_ADAPTER) {
                            menu.toggleAllMenuItemsVisibility(false)

                            menu.findItem(R.id.save_to_device).isVisible = true
                            menu.findItem(R.id.share).isVisible = true

                            return
                        }

                        if (adapterType == FOLDER_LINK_ADAPTER || adapterType == FROM_IMAGE_VIEWER || adapterType == VERSIONS_ADAPTER) {
                            menu.toggleAllMenuItemsVisibility(false)
                            menu.findItem(R.id.save_to_device).isVisible = true

                            return
                        }

                        val node = megaApi.getNodeByHandle(getCurrentPlayingHandle())
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
                            currentFragment == R.id.main_player

                        menu.findItem(R.id.share).isVisible =
                            currentFragment == R.id.main_player && showShareOption(
                                adapterType = adapterType,
                                isFolderLink = adapterType == FOLDER_LINK_ADAPTER,
                                handle = node.handle
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
                            node.parentHandle != megaApi.rubbishNode.handle
                                    && (access == MegaShare.ACCESS_FULL || access == MegaShare.ACCESS_OWNER)

                        menu.findItem(R.id.copy).isVisible = adapterType != FOLDER_LINK_ADAPTER
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
        playerServiceViewModelGateway?.getCurrentPlayingHandle()?.let { playingHandle ->
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

    @Suppress("deprecation")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val launchIntent = playerServiceViewModelGateway?.getCurrentIntent() ?: return false
        val playingHandle = playerServiceViewModelGateway?.getCurrentPlayingHandle() ?: return false
        val adapterType = launchIntent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
        val isFolderLink = adapterType == FOLDER_LINK_ADAPTER

        when (item.itemId) {
            R.id.save_to_device -> {
                when (adapterType) {
                    OFFLINE_ADAPTER -> nodeSaver.saveOfflineNode(playingHandle, true)
                    ZIP_ADAPTER -> {
                        val mediaItem = serviceGateway?.getCurrentMediaItem()
                        val uri = mediaItem?.localConfiguration?.uri ?: return false
                        val playlistItem =
                            playerServiceViewModelGateway?.getPlaylistItem(mediaItem.mediaId)
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
                                Timber.d("currentDocument NOT NULL")
                                nodeSaver.saveNode(
                                    currentDocument,
                                    isFolderLink = isFolderLink,
                                    fromMediaViewer = true,
                                    needSerialize = true
                                )
                            } else {
                                Timber.w("currentDocument is NULL")
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
                        serviceGateway?.getCurrentMediaItem()?.localConfiguration?.uri
                            ?: return true
                    navController.navigate(
                        MediaPlayerFragmentDirections.actionPlayerToTrackInfo(
                            adapterType,
                            adapterType == INCOMING_SHARES_ADAPTER,
                            playingHandle,
                            uri.toString()
                        )
                    )
                } else {
                    val intent: Intent

                    if (adapterType == OFFLINE_ADAPTER) {
                        intent = Intent(this, OfflineFileInfoActivity::class.java)
                        intent.putExtra(HANDLE, playingHandle.toString())

                        Timber.d("onOptionsItemSelected properties offline handle $playingHandle")
                    } else {
                        intent = Intent(this, FileInfoActivity::class.java)
                        intent.putExtra(HANDLE, playingHandle)

                        val node = megaApi.getNodeByHandle(playingHandle)
                        if (node == null) {
                            Timber.e("onOptionsItemSelected properties non-offline null node")

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
                        val mediaItem = serviceGateway?.getCurrentMediaItem()
                        val nodeName =
                            playerServiceViewModelGateway?.getPlaylistItem(mediaItem?.mediaId)?.nodeName
                                ?: return false
                        val uri = mediaItem?.localConfiguration?.uri ?: return false

                        shareUri(this, nodeName, uri)
                    }
                    FILE_LINK_ADAPTER -> {
                        shareLink(this, launchIntent.getStringExtra(URL_FILE_LINK))
                    }
                    else -> {
                        playerServiceViewModelGateway?.run {
                            shareNode(
                                this@MediaPlayerActivity,
                                megaApi.getNodeByHandle(getCurrentPlayingHandle())
                            )
                        }
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
                    megaApi.disableExport(
                        node,
                        OptionalMegaRequestListenerInterface(onRequestFinish = { _, error ->
                            if (error.errorCode == MegaError.API_OK) {
                                // Some times checking node.isExported immediately will still
                                // get true, so let's add some delay here.
                                runDelay(100L) {
                                    refreshMenuOptionsVisibility()
                                }
                            }
                        })
                    )
                }
                return true
            }
            R.id.chat_save_for_offline -> {
                PermissionUtils.checkNotificationsPermission(this)

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
                viewModel.renameUpdate(megaApi.getNodeByHandle(playingHandle))
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
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        nodeSaver.handleRequestPermissionsResult(requestCode)
    }

    @Suppress("deprecation")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (nodeAttacher.handleActivityResult(requestCode, resultCode, intent, this)) {
            return
        }

        if (nodeSaver.handleActivityResult(this, requestCode, resultCode, intent)) {
            return
        }

        when (requestCode) {
            REQUEST_CODE_SELECT_IMPORT_FOLDER -> {
                val node = getChatMessageNode() ?: return

                val toHandle = intent?.getLongExtra(INTENT_EXTRA_KEY_IMPORT_TO, INVALID_HANDLE)
                    ?: return

                viewModel.copyNode(node = node, newParentHandle = toHandle, context = this)
            }
            REQUEST_CODE_SELECT_FOLDER_TO_MOVE -> {
                val moveHandles = intent?.getLongArrayExtra(INTENT_EXTRA_KEY_MOVE_HANDLES)
                    ?: return
                val toHandle = intent.getLongExtra(INTENT_EXTRA_KEY_MOVE_TO, INVALID_HANDLE)

                viewModel.moveNode(moveHandles[0], toHandle, this)
            }
            REQUEST_CODE_SELECT_FOLDER_TO_COPY -> {
                val copyHandles = intent?.getLongArrayExtra(Constants.INTENT_EXTRA_KEY_COPY_HANDLES)
                    ?: return
                val toHandle = intent.getLongExtra(INTENT_EXTRA_KEY_MOVE_TO, INVALID_HANDLE)

                viewModel.copyNode(
                    nodeHandle = copyHandles[0],
                    newParentHandle = toHandle,
                    context = this
                )
            }
        }
    }

    /**
     * Set toolbar title
     *
     * @param title toolbar title
     */
    fun setToolbarTitle(title: String) {
        binding.toolbar.title = title
    }

    /**
     * Close search mode
     */
    fun closeSearch() {
        searchMenuItem?.collapseActionView()
    }

    /**
     * Hide tool bar
     *
     * @param animate true is that adds hide animation, otherwise is false
     */
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
        if (!isAudioPlayer()) {
            hideSystemUI()
        }
    }

    /**
     * Show tool bar
     *
     * @param animate true is that adds show animation, otherwise is false
     */
    fun showToolbar(animate: Boolean = true) {
        if (animate) {
            binding.toolbar.animate()
                .translationY(TRANSLATION_Y_ZERO)
                .setDuration(MEDIA_PLAYER_TOOLBAR_SHOW_HIDE_DURATION_MS)
                .start()
        } else {
            binding.toolbar.animate().cancel()
            binding.toolbar.translationY = TRANSLATION_Y_ZERO
        }
        if (!isAudioPlayer()) {
            showSystemUI()
        }
    }

    private fun hideSystemUI() {
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    /**
     * Show system UI
     */
    fun showSystemUI() {
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
    fun updateToolbar(isHide: Boolean) {
        binding.toolbar.animate().cancel()
        binding.toolbar.translationY = if (isHide) {
            -binding.toolbar.measuredHeight.toFloat()
        } else {
            TRANSLATION_Y_ZERO
        }
    }

    /**
     * Setup toolbar colors
     *
     * @param showElevation true is show elevation, otherwise is false.
     */
    fun setupToolbarColors(showElevation: Boolean = false) {
        val isDarkMode = Util.isDarkMode(this)
        val isMainPlayer = navController.currentDestination?.id == R.id.main_player
        val isPlaylist = navController.currentDestination?.id == R.id.playlist
        @ColorRes val toolbarBackgroundColor: Int
        @ColorInt val statusBarColor: Int
        val toolbarElevation: Float
        val isVideoPlayerMainView = !isAudioPlayer() && isMainPlayer
        val isVideoPlaylist = !isAudioPlayer() && isPlaylist

        WindowCompat.setDecorFitsSystemWindows(
            window,
            !isVideoPlayerMainView || !isVideoPlaylist
        )

        updatePaddingForSystemUI(isVideoPlayerMainView, isVideoPlaylist)

        binding.rootLayout.setBackgroundColor(
            getColor(
                if (isAudioPlayer()) {
                    R.color.white_dark_grey
                } else {
                    R.color.dark_grey
                }
            )
        )

        when {
            isAudioPlayer() && isMainPlayer -> {
                toolbarElevation = TOOLBAR_ELEVATION_ZERO
                toolbarBackgroundColor = android.R.color.transparent
                statusBarColor = ContextCompat.getColor(this, R.color.grey_020_grey_800)
            }
            (isVideoPlayerMainView || isVideoPlaylist) && !isDarkMode -> {
                moveToDarkModeUI()
                toolbarElevation = TOOLBAR_ELEVATION_ZERO
                if (isVideoPlayerMainView) {
                    toolbarBackgroundColor = R.color.grey_alpha_070
                    statusBarColor = ContextCompat.getColor(this, R.color.dark_grey)
                } else {
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
            }
            isDarkMode -> {
                toolbarElevation = TOOLBAR_ELEVATION_ZERO
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
                    TOOLBAR_ELEVATION_ZERO
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

    private fun moveToDarkModeUI() {
        binding.toolbar.context.setTheme(R.style.videoPlayerToolbarThemeDark)
        binding.toolbar.navigationIcon?.setTint(Color.WHITE)
        binding.toolbar.setTitleTextColor(Color.WHITE)
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
                                    Pair(insets.left, max(insets.top, insets.bottom))
                                } else {
                                    Pair(max(insets.top, insets.bottom), insets.right)
                                }
                            } else {
                                Pair(0, 0)
                            }
                        binding.toolbar.updatePadding(0, insets.top, 0, 0)
                        binding.rootLayout.updatePadding(
                            horizontalInsets.first,
                            0,
                            horizontalInsets.second,
                            insets.bottom
                        )
                    }
                }
                WindowInsetsCompat.CONSUMED
            }
        }
    }

    /**
     * Set draggable
     *
     * @param draggable ture is draggable, otherwise is false
     */
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

    @Suppress("deprecation")
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
            throwable.message?.run {
                showSnackbar(this)
            }
        }
    }

    /**
     * Show the customized snackbar for video player because video player always shows dark mode.
     *
     * @param message the message that will be shown
     */
    fun showSnackbarForVideoPlayer(message: String) {
        Snackbar.make(binding.rootLayout, message, Snackbar.LENGTH_LONG).let { snackbar ->
            with(snackbar.view as Snackbar.SnackbarLayout) {
                setBackgroundColor(getColor(R.color.white))
                findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                    .setTextColor(getColor(R.color.dark_grey))
            }
            snackbar.show()
        }
    }

    companion object {

        /**
         * The zero value for toolbar elevation
         */
        const val TOOLBAR_ELEVATION_ZERO = 0F

        /**
         * The zero value for translation Y
         */
        const val TRANSLATION_Y_ZERO = 0F

        /**
         * Judge the player whether is audio player
         *
         * @param intent Intent
         * @return true is audio player, otherwise is false.
         */
        fun isAudioPlayer(intent: Intent?): Boolean {
            val nodeName = intent?.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME) ?: return true

            return MimeTypeList.typeForName(nodeName).isAudio
        }
    }
}
