package mega.privacy.android.app.audioplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.SearchView
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.exoplayer2.util.Util.startForegroundService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.audioplayer.service.AudioPlayerService
import mega.privacy.android.app.audioplayer.service.AudioPlayerServiceBinder
import mega.privacy.android.app.audioplayer.trackinfo.TrackInfoFragment
import mega.privacy.android.app.audioplayer.trackinfo.TrackInfoFragmentArgs
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.databinding.ActivityAudioPlayerBinding
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.AlertsAndWarnings.Companion.showSaveToDeviceConfirmDialog
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.shareUri
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.MegaNodeUtil.*
import mega.privacy.android.app.utils.MegaNodeUtilKt
import mega.privacy.android.app.utils.MegaNodeUtilKt.Companion.selectCopyFolder
import mega.privacy.android.app.utils.MegaNodeUtilKt.Companion.selectMoveFolder
import mega.privacy.android.app.utils.Util.changeStatusBarColor
import mega.privacy.android.app.utils.Util.isOnline
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import javax.inject.Inject

@AndroidEntryPoint
class AudioPlayerActivity : BaseActivity(), SnackbarShower, ActivityLauncher {

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    private lateinit var binding: ActivityAudioPlayerBinding
    private val viewModel: AudioPlayerViewModel by viewModels()

    private lateinit var actionBar: ActionBar
    private lateinit var navController: NavController

    private var optionsMenu: Menu? = null
    private var searchMenuItem: MenuItem? = null

    private var viewingTrackInfo: TrackInfoFragmentArgs? = null

    private var serviceBound = false
    private var playerService: AudioPlayerService? = null

    private val nodeAttacher = MegaAttacher(this)
    private val nodeSaver = NodeSaver(this, this, this, showSaveToDeviceConfirmDialog(this))

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
        }

        /**
         * Called after a successful bind with our AudioPlayerService.
         */
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is AudioPlayerServiceBinder) {
                playerService = service.service

                service.service.viewModel.playlist.observe(this@AudioPlayerActivity) {
                    if (it.first.isEmpty()) {
                        stopPlayer()
                    } else {
                        val currentFragment = navController.currentDestination?.id ?: return@observe
                        refreshMenuOptionsVisibility(currentFragment)
                    }
                }
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

        binding = ActivityAudioPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        changeStatusBarColor(this, window, R.color.black)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setupToolbar()
        setupNavDestListener()

        val playerServiceIntent = Intent(this, AudioPlayerService::class.java)

        playerServiceIntent.putExtras(extras)

        if (rebuildPlaylist) {
            playerServiceIntent.setDataAndType(intent.data, intent.type)
            startForegroundService(this, playerServiceIntent)
        }

        bindService(playerServiceIntent, connection, Context.BIND_AUTO_CREATE)
        serviceBound = true

        viewModel.itemToRemove.observe(this) {
            playerService?.viewModel?.removeItem(it)
        }
    }

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
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
        actionBar.title = ""

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        if (!navController.navigateUp()) {
            playerService?.mainPlayerUIClosed()
            finish()
        }
    }

    private fun setupNavDestListener() {
        navController.addOnDestinationChangedListener { _, dest, args ->
            when (dest.id) {
                R.id.main_player -> {
                    actionBar.title = ""
                    viewingTrackInfo = null
                }
                R.id.playlist -> {
                    viewingTrackInfo = null
                }
                R.id.track_info -> {
                    actionBar.setTitle(R.string.audio_track_info)

                    if (args != null) {
                        viewingTrackInfo = TrackInfoFragmentArgs.fromBundle(args)
                    }
                }
            }
            refreshMenuOptionsVisibility(dest.id)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        playerService = null
        if (serviceBound) {
            unbindService(connection)
        }

        nodeSaver.destroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        optionsMenu = menu

        menuInflater.inflate(R.menu.audio_player, menu)

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

        val currentFragment = navController.currentDestination?.id
        if (currentFragment != null) {
            refreshMenuOptionsVisibility(currentFragment)
        }

        return true
    }

    private fun toggleAllMenuItemsVisibility(visible: Boolean) {
        val menu = optionsMenu ?: return
        for (i in 0 until menu.size()) {
            menu.getItem(i).isVisible = visible
        }
    }

    private fun refreshMenuOptionsVisibility(currentFragment: Int) {
        val adapterType = playerService?.viewModel?.currentIntent
            ?.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE) ?: return

        when (currentFragment) {
            R.id.playlist -> {
                toggleAllMenuItemsVisibility(false)
                searchMenuItem?.isVisible = true
            }
            R.id.main_player, R.id.track_info -> {
                if (adapterType == OFFLINE_ADAPTER) {
                    toggleAllMenuItemsVisibility(false)

                    optionsMenu?.findItem(R.id.properties)?.isVisible =
                        currentFragment == R.id.main_player

                    optionsMenu?.findItem(R.id.share)?.isVisible =
                        currentFragment == R.id.main_player

                    return
                }

                if (adapterType == FILE_LINK_ADAPTER || adapterType == ZIP_ADAPTER) {
                    toggleAllMenuItemsVisibility(false)
                }

                val service = playerService
                if (service == null) {
                    toggleAllMenuItemsVisibility(false)
                    return
                }

                val node = megaApi.getNodeByHandle(service.viewModel.playingHandle)
                if (node == null) {
                    toggleAllMenuItemsVisibility(false)
                    return
                }

                toggleAllMenuItemsVisibility(true)
                searchMenuItem?.isVisible = false

                optionsMenu?.findItem(R.id.save_to_device)?.isVisible = true

                optionsMenu?.findItem(R.id.properties)?.isVisible =
                    currentFragment == R.id.main_player

                optionsMenu?.findItem(R.id.share)?.isVisible =
                    currentFragment == R.id.main_player && showShareOption(
                        adapterType, adapterType == FOLDER_LINK_ADAPTER, node.handle
                    )

                optionsMenu?.findItem(R.id.send_to_chat)?.isVisible = adapterType != FROM_CHAT

                if (megaApi.getAccess(node) == MegaShare.ACCESS_OWNER) {
                    if (node.isExported) {
                        optionsMenu?.findItem(R.id.get_link)?.isVisible = false
                        optionsMenu?.findItem(R.id.remove_link)?.isVisible = true
                    } else {
                        optionsMenu?.findItem(R.id.get_link)?.isVisible = true
                        optionsMenu?.findItem(R.id.remove_link)?.isVisible = false
                    }
                } else {
                    optionsMenu?.findItem(R.id.get_link)?.isVisible = false
                    optionsMenu?.findItem(R.id.remove_link)?.isVisible = false
                }

                val access = megaApi.getAccess(node)
                when (access) {
                    MegaShare.ACCESS_READWRITE, MegaShare.ACCESS_READ, MegaShare.ACCESS_UNKNOWN -> {
                        optionsMenu?.findItem(R.id.rename)?.isVisible = false
                        optionsMenu?.findItem(R.id.move)?.isVisible = false
                    }
                    MegaShare.ACCESS_FULL, MegaShare.ACCESS_OWNER -> {
                        optionsMenu?.findItem(R.id.rename)?.isVisible = true
                        optionsMenu?.findItem(R.id.move)?.isVisible = true
                    }
                }

                optionsMenu?.findItem(R.id.move_to_trash)?.isVisible =
                    node.parentHandle != megaApi.rubbishNode.handle
                            && (access == MegaShare.ACCESS_FULL || access == MegaShare.ACCESS_OWNER)

                optionsMenu?.findItem(R.id.copy)?.isVisible =
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
                val uri = service.exoPlayer.currentMediaItem?.playbackProperties?.uri ?: return true
                navController.navigate(
                    AudioPlayerFragmentDirections.actionPlayerToTrackInfo(
                        adapterType, adapterType == INCOMING_SHARES_ADAPTER, playingHandle, uri
                    )
                )
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
                    megaApi.disableExport(node)
                }
                return true
            }
            R.id.rename -> {
                val node = megaApi.getNodeByHandle(playingHandle) ?: return true
                AlertsAndWarnings.showRenameDialog(this, node.name, node.isFolder) {
                    playerService?.viewModel?.updateItemName(node.handle, it)
                    updateTrackInfoNodeNameIfNeeded(node.handle, it)
                    MegaNodeUtilKt.renameNode(node, it, this)
                }
                return true
            }
            R.id.move -> {
                selectMoveFolder(this, longArrayOf(playingHandle))
                return true
            }
            R.id.copy -> {
                selectCopyFolder(this, longArrayOf(playingHandle))
                return true
            }
            R.id.move_to_trash -> {
                val node = megaApi.getNodeByHandle(playingHandle) ?: return true
                moveToRubbishBin(node)
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

    /**
     * Shows a confirmation warning before moves a node to rubbish bin.
     *
     * @param node node to be moved to rubbish bin
     */
    private fun moveToRubbishBin(node: MegaNode) {
        logDebug("moveToRubbishBin")
        if (!isOnline(this)) {
            showSnackbar(
                SNACKBAR_TYPE, getString(R.string.error_server_connection_problem),
                MEGACHAT_INVALID_HANDLE
            )
            return
        }

        MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialogStyle)
            .setMessage(R.string.confirmation_move_to_rubbish)
            .setPositiveButton(R.string.general_move) { _, _ ->
                playerService?.viewModel?.removeItem(node.handle)
                MegaNodeUtilKt.moveNodeToRubbishBin(node, this)
            }
            .setNegativeButton(R.string.general_cancel, null)
            .show()
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
        binding.toolbar.title = title
    }

    fun closeSearch() {
        searchMenuItem?.collapseActionView()
    }

    fun hideToolbar() {
        binding.toolbar.animate()
            .translationY(-binding.toolbar.measuredHeight.toFloat())
            .setDuration(AUDIO_PLAYER_TOOLBAR_SHOW_HIDE_DURATION_MS)
            .start()
    }

    fun showToolbar(animate: Boolean = true) {
        if (animate) {
            binding.toolbar.animate()
                .translationY(0F)
                .setDuration(AUDIO_PLAYER_TOOLBAR_SHOW_HIDE_DURATION_MS)
                .start()
        } else {
            binding.toolbar.animate().cancel()
            binding.toolbar.translationY = 0F
        }
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        showSnackbar(type, binding.rootLayout, content, chatId)
    }

    override fun launchActivity(intent: Intent) {
        startActivity(intent)
        stopPlayer()
    }

    override fun launchActivityForResult(intent: Intent, requestCode: Int) {
        startActivityForResult(intent, requestCode)
    }
}
