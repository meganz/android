package mega.privacy.android.app.audioplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.exoplayer2.util.Util.startForegroundService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.audioplayer.service.AudioPlayerService
import mega.privacy.android.app.audioplayer.service.AudioPlayerServiceBinder
import mega.privacy.android.app.audioplayer.trackinfo.TrackInfoFragmentArgs
import mega.privacy.android.app.lollipop.GetLinkActivityLollipop
import mega.privacy.android.app.lollipop.controllers.NodeController
import mega.privacy.android.app.utils.AlertsAndWarnings.Companion.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.Util.*
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import javax.inject.Inject

@AndroidEntryPoint
class AudioPlayerActivity : BaseActivity() {

    @Inject
    lateinit var megaApi: MegaApiAndroid

    private val viewModel: AudioPlayerViewModel by viewModels()

    private lateinit var rootLayout: FrameLayout
    private lateinit var toolbar: Toolbar
    private lateinit var actionBar: ActionBar
    private lateinit var navController: NavController

    private var adapterType = INVALID_VALUE

    private var optionsMenu: Menu? = null
    private var searchMenuItem: MenuItem? = null

    private var viewingTrackInfo: TrackInfoFragmentArgs? = null

    private var playerService: AudioPlayerService? = null

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
                    val currentFragment = navController.currentDestination?.id ?: return@observe
                    refreshMenuOptionsVisibility(currentFragment)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.extras == null) {
            finish()
            return
        }

        adapterType = intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
        if (adapterType == INVALID_VALUE) {
            finish()
            return
        }

        setContentView(R.layout.activity_audio_player)
        changeStatusBarColor(this, window, R.color.black)

        rootLayout = findViewById(R.id.root_layout)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setupToolbar()
        setupNavDestListener()

        val extras = intent.extras ?: return
        val playerServiceIntent = Intent(this, AudioPlayerService::class.java)

        playerServiceIntent.putExtras(extras)

        if (intent.getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)) {
            playerServiceIntent.setDataAndType(intent.data, intent.type)
            startForegroundService(this, playerServiceIntent)
        }

        bindService(playerServiceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)

        setSupportActionBar(toolbar)
        actionBar = supportActionBar!!
        actionBar.setHomeButtonEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
        actionBar.title = ""

        toolbar.setNavigationOnClickListener {
            handleNavigateUp()
        }
    }

    override fun onBackPressed() {
        handleNavigateUp()
    }

    private fun handleNavigateUp() {
        if (!navController.navigateUp()) {
            playerService?.mainPlayerUIClosed()
            finish()
        }
    }

    private fun setupNavDestListener() {
        navController.addOnDestinationChangedListener { _, dest, args ->
            when (dest.id) {
                R.id.audio_player -> {
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
        unbindService(connection)
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

        return true
    }

    private fun toggleAllMenuItemsVisibility(visible: Boolean) {
        val menu = optionsMenu ?: return
        for (i in 0 until menu.size()) {
            menu.getItem(i).isVisible = visible
        }
    }

    private fun refreshMenuOptionsVisibility(currentFragment: Int) {
        when (currentFragment) {
            R.id.playlist -> {
                toggleAllMenuItemsVisibility(false)
                searchMenuItem?.isVisible = true
            }
            R.id.audio_player, R.id.track_info -> {
                if (adapterType == OFFLINE_ADAPTER) {
                    toggleAllMenuItemsVisibility(false)
                    optionsMenu?.findItem(R.id.properties)?.isVisible =
                        currentFragment == R.id.audio_player
                    return
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

                optionsMenu?.findItem(R.id.save_to_device)?.isVisible = adapterType != ZIP_ADAPTER

                optionsMenu?.findItem(R.id.properties)?.isVisible =
                    currentFragment == R.id.audio_player

                optionsMenu?.findItem(R.id.send_to_chat)?.isVisible =
                    adapterType != FROM_CHAT && adapterType != FILE_LINK_ADAPTER
                            && adapterType != ZIP_ADAPTER

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

                when (megaApi.getAccess(node)) {
                    MegaShare.ACCESS_READWRITE, MegaShare.ACCESS_READ, MegaShare.ACCESS_UNKNOWN -> {
                        optionsMenu?.findItem(R.id.rename)?.isVisible = false
                        optionsMenu?.findItem(R.id.move)?.isVisible = false
                        optionsMenu?.findItem(R.id.move_to_trash)?.isVisible = false
                    }
                    MegaShare.ACCESS_FULL, MegaShare.ACCESS_OWNER -> {
                        optionsMenu?.findItem(R.id.rename)?.isVisible = true
                        optionsMenu?.findItem(R.id.move)?.isVisible = true
                        optionsMenu?.findItem(R.id.move_to_trash)?.isVisible = true
                    }
                }

                optionsMenu?.findItem(R.id.copy)?.isVisible =
                    adapterType != FOLDER_LINK_ADAPTER && adapterType != FILE_LINK_ADAPTER
                            && adapterType != ZIP_ADAPTER && adapterType != FROM_CHAT
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val launchIntent = intent ?: return false
        val service = playerService ?: return false
        val playingHandle = service.viewModel.playingHandle
        val isFolderLink = adapterType == FOLDER_LINK_ADAPTER

        when (item.itemId) {
            R.id.save_to_device -> {
                if (adapterType == OFFLINE_ADAPTER) {
                    viewModel.saveOfflineNode(playingHandle) { intent, code ->
                        startActivityForResult(intent, code)
                    }
                } else {
                    viewModel.saveMegaNode(playingHandle, isFolderLink) { intent, code ->
                        startActivityForResult(intent, code)
                    }
                }
                return true
            }
            R.id.properties -> {
                val uri = service.exoPlayer.currentMediaItem?.playbackProperties?.uri ?: return true
                val from = launchIntent.getIntExtra(INTENT_EXTRA_KEY_FROM, INVALID_VALUE)
                navController.navigate(
                    AudioPlayerFragmentDirections.actionPlayerToTrackInfo(
                        adapterType, from, playingHandle, uri
                    )
                )
                return true
            }
            R.id.send_to_chat -> {
                if (app.storageState == MegaApiJava.STORAGE_STATE_PAYWALL) {
                    showOverDiskQuotaPaywallWarning()
                    return true
                }

                val node = megaApi.getNodeByHandle(playingHandle) ?: return true
                NodeController(this, isFolderLink).checkIfNodeIsMineAndSelectChatsToSendNode(node)
                return true
            }
            R.id.get_link -> {
                if (MegaNodeUtil.showTakenDownNodeActionNotAvailableDialog(
                        megaApi.getNodeByHandle(playingHandle), this
                    )
                ) {
                    return true
                }
                val intent = Intent(this, GetLinkActivityLollipop::class.java)
                intent.putExtra(HANDLE, playingHandle)
                startActivity(intent)
                return true
            }
            R.id.remove_link -> {
                val node = megaApi.getNodeByHandle(playingHandle) ?: return true
                if (MegaNodeUtil.showTakenDownNodeActionNotAvailableDialog(node, this)) {
                    return true
                }

                showRemoveLink(node)
                return true
            }
            R.id.rename -> {
                return true
            }
            R.id.move -> {
                return true
            }
            R.id.copy -> {
                return true
            }
            R.id.move_to_trash -> {
                return true
            }
        }
        return false
    }

    private fun showRemoveLink(node: MegaNode) {
        val builder = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialogStyle)

        val dialogLayout = layoutInflater.inflate(R.layout.dialog_link, null)

        dialogLayout.findViewById<TextView>(R.id.dialog_link_link_url).isVisible = false
        dialogLayout.findViewById<TextView>(R.id.dialog_link_link_key).isVisible = false
        dialogLayout.findViewById<TextView>(R.id.dialog_link_symbol).isVisible = false

        val removeText = dialogLayout.findViewById<TextView>(R.id.dialog_link_text_remove)
        (removeText.layoutParams as RelativeLayout.LayoutParams).setMargins(
            scaleWidthPx(REMOVE_LINK_DIALOG_TEXT_MARGIN_LEFT, outMetrics),
            scaleHeightPx(REMOVE_LINK_DIALOG_TEXT_MARGIN_TOP, outMetrics),
            scaleWidthPx(REMOVE_LINK_DIALOG_TEXT_MARGIN_RIGHT, outMetrics),
            0
        )
        removeText.visibility = View.VISIBLE
        removeText.text = getString(R.string.context_remove_link_warning_text)

        val scaleW = getScaleW(outMetrics, resources.displayMetrics.density)
        removeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, REMOVE_LINK_DIALOG_TEXT_SIZE * scaleW)

        builder.setView(dialogLayout)
            .setPositiveButton(getString(R.string.context_remove)) { _, _ ->
                megaApi.disableExport(node)
            }
            .setNegativeButton(getString(R.string.general_cancel), null)
            .create()
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (viewModel.handleActivityResult(requestCode, resultCode, data)) {
            return
        } else if (requestCode == REQUEST_CODE_SELECT_CHAT && resultCode == RESULT_OK
            && data != null
        ) {
            viewModel.handleSelectChatResult(data)
        }
    }

    fun showSnackbar(type: Int, content: String, chatId: Long) {
        showSnackbar(type, rootLayout, content, chatId)
    }

    fun setToolbarTitle(title: String) {
        toolbar.title = title
    }

    fun closeSearch() {
        searchMenuItem?.collapseActionView()
    }

    fun hideToolbar() {
        toolbar.animate()
            .translationY(-toolbar.measuredHeight.toFloat())
            .setDuration(AUDIO_PLAYER_TOOLBAR_SHOW_HIDE_DURATION_MS)
            .start()
    }

    fun showToolbar(animate: Boolean = true) {
        if (animate) {
            toolbar.animate()
                .translationY(0F)
                .setDuration(AUDIO_PLAYER_TOOLBAR_SHOW_HIDE_DURATION_MS)
                .start()
        } else {
            toolbar.animate().cancel()
            toolbar.translationY = 0F
        }
    }

    companion object {
        const val REMOVE_LINK_DIALOG_TEXT_MARGIN_LEFT = 25
        const val REMOVE_LINK_DIALOG_TEXT_MARGIN_TOP = 20
        const val REMOVE_LINK_DIALOG_TEXT_MARGIN_RIGHT = 10
        const val REMOVE_LINK_DIALOG_TEXT_SIZE = 15
    }
}
