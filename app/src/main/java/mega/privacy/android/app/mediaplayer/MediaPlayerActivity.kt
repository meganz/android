package mega.privacy.android.app.mediaplayer

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.exoplayer2.util.Util.startForegroundService
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.OfflineFileInfoActivity
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.components.dragger.DragToExitSupport
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.databinding.ActivityAudioPlayerBinding
import mega.privacy.android.app.databinding.ActivityVideoPlayerBinding
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerServiceBinder
import mega.privacy.android.app.mediaplayer.service.VideoPlayerService
import mega.privacy.android.app.mediaplayer.trackinfo.TrackInfoFragment
import mega.privacy.android.app.mediaplayer.trackinfo.TrackInfoFragmentArgs
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.AlertsAndWarnings.Companion.showSaveToDeviceConfirmDialog
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.shareUri
import mega.privacy.android.app.utils.MegaNodeDialogUtil.Companion.moveToRubbishOrRemove
import mega.privacy.android.app.utils.MegaNodeDialogUtil.Companion.showRenameNodeDialog
import mega.privacy.android.app.utils.MegaNodeUtil.selectFolderToCopy
import mega.privacy.android.app.utils.MegaNodeUtil.selectFolderToMove
import mega.privacy.android.app.utils.MegaNodeUtil.shareLink
import mega.privacy.android.app.utils.MegaNodeUtil.shareNode
import mega.privacy.android.app.utils.MegaNodeUtil.showShareOption
import mega.privacy.android.app.utils.MegaNodeUtil.showTakenDownAlert
import mega.privacy.android.app.utils.MegaNodeUtil.showTakenDownNodeActionNotAvailableDialog
import mega.privacy.android.app.utils.RunOnUIThreadUtils.post
import mega.privacy.android.app.utils.RunOnUIThreadUtils.runDelay
import nz.mega.sdk.*
import javax.inject.Inject

@AndroidEntryPoint
abstract class MediaPlayerActivity : BaseActivity(), SnackbarShower, ActivityLauncher {

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    private lateinit var rootLayout: ViewGroup
    private lateinit var toolbar: Toolbar
    private val viewModel: MediaPlayerViewModel by viewModels()

    private lateinit var actionBar: ActionBar
    private lateinit var navController: NavController

    private var optionsMenu: Menu? = null
    private var searchMenuItem: MenuItem? = null

    private var viewingTrackInfo: TrackInfoFragmentArgs? = null

    private var serviceBound = false
    private var playerService: MediaPlayerService? = null

    private lateinit var nodeAttacher: MegaAttacher
    private lateinit var nodeSaver: NodeSaver
    private lateinit var dragToExit: DragToExitSupport

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
        }

        /**
         * Called after a successful bind with our AudioPlayerService.
         */
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MediaPlayerServiceBinder) {
                playerService = service.service

                service.service.viewModel.playlist.observe(this@MediaPlayerActivity) {
                    if (service.service.viewModel.playlistSearchQuery != null) {
                        return@observe
                    }

                    if (it.first.isEmpty()) {
                        stopPlayer()
                    } else {
                        refreshMenuOptionsVisibility()
                    }
                }

                service.service.metadata.observe(this@MediaPlayerActivity) {
                    dragToExit.nodeChanged(service.service.viewModel.playingHandle)
                }

                service.service.viewModel.error.observe(
                    this@MediaPlayerActivity, this@MediaPlayerActivity::onError
                )
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nodeAttacher = MegaAttacher(this)
        nodeSaver = NodeSaver(this, this, this, showSaveToDeviceConfirmDialog(this))

        dragToExit = DragToExitSupport(this, this::onDragActivated) {
            finish()
            overridePendingTransition(0, android.R.anim.fade_out)
        }

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

        if (isAudioPlayer) {
            val binding = ActivityAudioPlayerBinding.inflate(layoutInflater)
            setContentView(binding.root)

            rootLayout = binding.rootLayout
            toolbar = binding.toolbar
        } else {
            val binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
            setContentView(dragToExit.wrapContentView(binding.root))

            rootLayout = binding.rootLayout
            toolbar = binding.toolbar

            toolbar.setBackgroundColor(Color.TRANSPARENT)
            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white_alpha_087))

            MediaPlayerService.pauseAudioPlayer(this)

            dragToExit.viewerFrom = intent.getIntExtra(INTENT_EXTRA_KEY_VIEWER_FROM, INVALID_VALUE)
            dragToExit.observeThumbnailLocation(this)
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

        if (rebuildPlaylist) {
            playerServiceIntent.setDataAndType(intent.data, intent.type)
            if (isAudioPlayer) {
                startForegroundService(this, playerServiceIntent)
            } else {
                startService(playerServiceIntent)
            }
        }

        bindService(playerServiceIntent, connection, Context.BIND_AUTO_CREATE)
        serviceBound = true

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

        if (!isAudioPlayer) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
    }

    override fun onResume() {
        super.onResume()

        refreshMenuOptionsVisibility()
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
        setSupportActionBar(toolbar)
        actionBar = supportActionBar!!
        actionBar.setHomeButtonEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.title = ""

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        if (!navController.navigateUp()) {
            finish()
        }
    }

    private fun setupNavDestListener() {
        navController.addOnDestinationChangedListener { _, dest, args ->
            if (isAudioPlayer()) {
                toolbar.elevation = 0F

                val color = ContextCompat.getColor(
                    this,
                    if (dest.id == R.id.main_player) R.color.grey_020_grey_800 else R.color.white_dark_grey
                )

                window.statusBarColor = color
                toolbar.setBackgroundColor(color)
            } else {
                window.statusBarColor = Color.BLACK
                toolbar.setBackgroundColor(Color.TRANSPARENT)
            }

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

        playerService?.mainPlayerUIClosed()

        playerService = null
        if (serviceBound) {
            unbindService(connection)
        }

        nodeSaver.destroy()

        if (!isAudioPlayer(intent)) {
            MediaPlayerService.resumeAudioPlayer(this)
        }

        dragToExit.showPreviousHiddenThumbnail()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        optionsMenu = menu

        menuInflater.inflate(R.menu.media_player, menu)

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

    private fun toggleAllMenuItemsVisibility(menu: Menu, visible: Boolean) {
        for (i in 0 until menu.size()) {
            menu.getItem(i).isVisible = visible
        }
    }

    private fun refreshMenuOptionsVisibility() {
        val menu = optionsMenu ?: return
        val currentFragment = navController.currentDestination?.id ?: return

        val adapterType = playerService?.viewModel?.currentIntent
            ?.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE) ?: return

        when (currentFragment) {
            R.id.playlist -> {
                toggleAllMenuItemsVisibility(menu, false)
                searchMenuItem?.isVisible = true
            }
            R.id.main_player, R.id.track_info -> {
                if (adapterType == OFFLINE_ADAPTER) {
                    toggleAllMenuItemsVisibility(menu, false)

                    menu.findItem(R.id.properties).isVisible =
                        currentFragment == R.id.main_player

                    menu.findItem(R.id.share).isVisible =
                        currentFragment == R.id.main_player

                    return
                }

                if (adapterType == RUBBISH_BIN_ADAPTER) {
                    toggleAllMenuItemsVisibility(menu, false)

                    menu.findItem(R.id.properties).isVisible =
                        currentFragment == R.id.main_player

                    val moveToTrash = menu.findItem(R.id.move_to_trash) ?: return
                    moveToTrash.isVisible = true
                    moveToTrash.title = StringResourcesUtils.getString(R.string.context_remove)

                    return
                }

                if (adapterType == FILE_LINK_ADAPTER || adapterType == ZIP_ADAPTER) {
                    toggleAllMenuItemsVisibility(menu, false)
                }

                val service = playerService
                if (service == null) {
                    toggleAllMenuItemsVisibility(menu, false)
                    return
                }

                val node = megaApi.getNodeByHandle(service.viewModel.playingHandle)
                if (node == null) {
                    toggleAllMenuItemsVisibility(menu, false)
                    return
                }

                toggleAllMenuItemsVisibility(menu, true)
                searchMenuItem?.isVisible = false

                menu.findItem(R.id.save_to_device).isVisible = true

                menu.findItem(R.id.properties).isVisible =
                    currentFragment == R.id.main_player

                menu.findItem(R.id.share).isVisible =
                    currentFragment == R.id.main_player && showShareOption(
                        adapterType, adapterType == FOLDER_LINK_ADAPTER, node.handle
                    )

                menu.findItem(R.id.send_to_chat).isVisible = adapterType != FROM_CHAT

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

                menu.findItem(R.id.copy).isVisible =
                    adapterType != FOLDER_LINK_ADAPTER && adapterType != FROM_CHAT
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val service = playerService ?: return false
        val launchIntent = service.viewModel.currentIntent ?: return false
        val playingHandle = service.viewModel.playingHandle
        val adapterType = launchIntent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
        val isFolderLink = adapterType == FOLDER_LINK_ADAPTER

        when (item.itemId) {
            R.id.save_to_device -> {
                if (adapterType == OFFLINE_ADAPTER) {
                    nodeSaver.saveOfflineNode(playingHandle, true)
                } else {
                    nodeSaver.saveHandle(
                        playingHandle,
                        isFolderLink = isFolderLink,
                        fromMediaViewer = true
                    )
                }
                return true
            }
            R.id.properties -> {
                if (isAudioPlayer()) {
                    val uri =
                        service.exoPlayer.currentMediaItem?.playbackProperties?.uri ?: return true
                    navController.navigate(
                        MediaPlayerFragmentDirections.actionPlayerToTrackInfo(
                            adapterType, adapterType == INCOMING_SHARES_ADAPTER, playingHandle, uri
                        )
                    )
                } else {
                    val nodeName =
                        service.viewModel.getPlaylistItem(service.exoPlayer.currentMediaItem?.mediaId)?.nodeName
                            ?: return false

                    val intent: Intent

                    if (adapterType == OFFLINE_ADAPTER) {
                        intent = Intent(this, OfflineFileInfoActivity::class.java)
                    } else {
                        intent = Intent(this, FileInfoActivityLollipop::class.java)
                        intent.putExtra(NAME, nodeName)
                    }

                    intent.putExtra(HANDLE, playingHandle)
                    startActivity(intent)
                }
                return true
            }
            R.id.share -> {
                when (adapterType) {
                    OFFLINE_ADAPTER, ZIP_ADAPTER -> {
                        val nodeName =
                            service.viewModel.getPlaylistItem(service.exoPlayer.currentMediaItem?.mediaId)?.nodeName
                                ?: return false
                        val uri = service.exoPlayer.currentMediaItem?.playbackProperties?.uri
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
                moveToRubbishOrRemove(playingHandle, this, this)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (nodeAttacher.handleActivityResult(requestCode, resultCode, data, this)) {
            return
        }

        if (nodeSaver.handleActivityResult(requestCode, resultCode, data)) {
            return
        }

        viewModel.handleActivityResult(requestCode, resultCode, data, this, this)
    }

    fun setToolbarTitle(title: String) {
        toolbar.title = title
    }

    fun closeSearch() {
        searchMenuItem?.collapseActionView()
    }

    fun hideToolbar(animate: Boolean = true, hideStatusBar: Boolean = true) {
        if (animate) {
            toolbar.animate()
                .translationY(-toolbar.measuredHeight.toFloat())
                .setDuration(MEDIA_PLAYER_TOOLBAR_SHOW_HIDE_DURATION_MS)
                .start()
        } else {
            toolbar.animate().cancel()
            toolbar.translationY = -toolbar.measuredHeight.toFloat()
        }

        if (!isAudioPlayer() && hideStatusBar) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    fun showToolbar(animate: Boolean = true) {
        if (animate) {
            toolbar.animate()
                .translationY(0F)
                .setDuration(MEDIA_PLAYER_TOOLBAR_SHOW_HIDE_DURATION_MS)
                .start()
        } else {
            toolbar.animate().cancel()
            toolbar.translationY = 0F
        }

        if (!isAudioPlayer()) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    fun showToolbarElevation(withElevation: Boolean) {
        // This is the actual color when using Util.changeToolBarElevation, but video player
        // use different toolbar theme (to force dark theme), which breaks
        // Util.changeToolBarElevation, so we just use the actual color here.
        val darkElevationColor = Color.parseColor("#282828")

        if (!isAudioPlayer() || Util.isDarkMode(this)) {
            toolbar.setBackgroundColor(
                when {
                    withElevation -> darkElevationColor
                    isAudioPlayer() -> Color.TRANSPARENT
                    else -> ContextCompat.getColor(this, R.color.dark_grey)
                }
            )

            post {
                window.statusBarColor = if (withElevation) darkElevationColor else Color.BLACK
            }
        } else {
            toolbar.elevation =
                if (withElevation) resources.getDimension(R.dimen.toolbar_elevation) else 0F
        }
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
            MegaError.API_EBLOCKED -> showTakenDownAlert(this)
        }
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        showSnackbar(type, rootLayout, content, chatId)
    }

    override fun launchActivity(intent: Intent) {
        startActivity(intent)
        stopPlayer()
    }

    override fun launchActivityForResult(intent: Intent, requestCode: Int) {
        startActivityForResult(intent, requestCode)
    }

    companion object {
        fun isAudioPlayer(intent: Intent?): Boolean {
            val nodeName = intent?.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME) ?: return true

            return MimeTypeList.typeForName(nodeName).isAudio
        }
    }
}
