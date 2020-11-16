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
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.exoplayer2.util.Util.startForegroundService
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.audioplayer.service.AudioPlayerService
import mega.privacy.android.app.audioplayer.service.AudioPlayerServiceBinder
import mega.privacy.android.app.audioplayer.trackinfo.TrackInfoFragmentArgs
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.Util.changeStatusBarColor

@AndroidEntryPoint
class AudioPlayerActivity : BaseActivity() {

    private val viewModel: AudioPlayerViewModel by viewModels()

    private lateinit var toolbar: Toolbar
    private lateinit var actionBar: ActionBar
    private lateinit var navController: NavController

    private var adapterType = INVALID_VALUE

    private var optionsMenu: Menu? = null
    private var propertiesMenuItem: MenuItem? = null
    private var getLinkMenuItem: MenuItem? = null
    private var removeLinkMenuItem: MenuItem? = null
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
                    toggleAllMenuItemsVisibility(true)
                    searchMenuItem?.isVisible = false

                    actionBar.title = ""
                    viewingTrackInfo = null
                }
                R.id.playlist -> {
                    toggleAllMenuItemsVisibility(false)
                    searchMenuItem?.isVisible = true

                    viewingTrackInfo = null
                }
                R.id.track_info -> {
                    toggleAllMenuItemsVisibility(true)
                    propertiesMenuItem?.isVisible = false
                    searchMenuItem?.isVisible = false

                    actionBar.setTitle(R.string.audio_track_info)

                    if (args != null) {
                        viewingTrackInfo = TrackInfoFragmentArgs.fromBundle(args)
                    }
                }
            }
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

        propertiesMenuItem = menu.findItem(R.id.properties)
        getLinkMenuItem = menu.findItem(R.id.get_link)
        removeLinkMenuItem = menu.findItem(R.id.remove_link)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val launchIntent = intent ?: return false
        val service = playerService ?: return false
        val uri = service.exoPlayer.currentMediaItem?.playbackProperties?.uri ?: return false

        when (item.itemId) {
            R.id.save_to_device -> {
                if (adapterType == OFFLINE_ADAPTER) {
                    viewModel.saveOfflineNode(service.viewModel.playingHandle) { intent, code ->
                        startActivityForResult(intent, code)
                    }
                } else {
                    viewModel.saveMegaNode(
                        service.viewModel.playingHandle,
                        adapterType == FOLDER_LINK_ADAPTER
                    ) { intent, code ->
                        startActivityForResult(intent, code)
                    }
                }
                return true
            }
            R.id.properties -> {
                val from = launchIntent.getIntExtra(INTENT_EXTRA_KEY_FROM, INVALID_VALUE)
                navController.navigate(
                    AudioPlayerFragmentDirections.actionPlayerToTrackInfo(
                        adapterType, from, service.viewModel.playingHandle, uri
                    )
                )
                return true
            }
            R.id.send_to_chat -> {
                return true
            }
            R.id.get_link -> {
                return true
            }
            R.id.remove_link -> {
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!viewModel.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
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
            toolbar.translationY = 0F
        }
    }
}
