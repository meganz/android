package mega.privacy.android.app.mediaplayer

import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.navigation.NavController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerServiceGateway
import mega.privacy.android.app.mediaplayer.gateway.PlayerServiceViewModelGateway
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.FILE_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.FROM_IMAGE_VIEWER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_COPY_HANDLES
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_COPY_TO
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IMPORT_TO
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MOVE_HANDLES
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MOVE_TO
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MSG_ID
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.RUBBISH_BIN_ADAPTER
import mega.privacy.android.app.utils.Constants.VERSIONS_ADAPTER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.MenuUtils.toggleAllMenuItemsVisibility
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import timber.log.Timber

/**
 * Media player Activity
 */
@AndroidEntryPoint
abstract class MediaPlayerActivity : PasscodeActivity() {
    internal val viewModel: MediaPlayerViewModel by viewModels()

    internal var searchMenuItem: MenuItem? = null
    internal var optionsMenu: Menu? = null

    internal lateinit var navController: NavController

    internal var playerServiceGateway: PlayerServiceViewModelGateway? = null
    internal var serviceGateway: MediaPlayerServiceGateway? = null

    internal val nodeAttacher by lazy { MegaAttacher(this) }
    private var currentRequestCode: Int? = null

    internal val nodeSaver by lazy {
        NodeSaver(
            activityLauncher = this,
            permissionRequester = this,
            snackbarShower = this,
            confirmDialogShower = AlertsAndWarnings.showSaveToDeviceConfirmDialog(this)
        )
    }

    private val nodeAttacherLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            currentRequestCode?.let {
                if (nodeAttacher.handleActivityResult(
                        requestCode = it,
                        resultCode = result.resultCode,
                        data = result.data,
                        snackbarShower = this
                    )
                ) {
                    currentRequestCode = null
                }
            }

        }

    private val nodeSaverLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            currentRequestCode?.let {
                if (nodeSaver.handleActivityResult(
                        activity = this,
                        requestCode = it,
                        resultCode = result.resultCode,
                        intent = result.data
                    )
                ) {
                    currentRequestCode = null
                }
            }
        }

    internal val selectImportFolderLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val node = getChatMessageNode()
            val toHandle = result.data?.getLongExtra(INTENT_EXTRA_KEY_IMPORT_TO, INVALID_HANDLE)
            if (node != null && toHandle != null) {
                viewModel.copyNode(node = node, newParentHandle = toHandle)
            }
        }

    internal val selectFolderToMoveLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val moveHandles = result.data?.getLongArrayExtra(INTENT_EXTRA_KEY_MOVE_HANDLES)
            val toHandle = result.data?.getLongExtra(INTENT_EXTRA_KEY_MOVE_TO, INVALID_HANDLE)
            if (moveHandles != null && toHandle != null)
                viewModel.moveNode(
                    nodeHandle = moveHandles[0],
                    newParentHandle = toHandle,
                )
        }

    internal val selectFolderToCopyLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val copyHandles = result.data?.getLongArrayExtra(INTENT_EXTRA_KEY_COPY_HANDLES)
            val toHandle = result.data?.getLongExtra(INTENT_EXTRA_KEY_COPY_TO, INVALID_HANDLE)
            if (copyHandles != null && toHandle != null) {
                viewModel.copyNode(
                    nodeHandle = copyHandles[0],
                    newParentHandle = toHandle,
                )
            }
        }

    internal fun refreshMenuOptionsVisibility() {
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

        playerServiceGateway?.let {
            it.getCurrentIntent()?.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
                ?.let { adapterType ->
                    when (currentFragment) {
                        R.id.playlist -> {
                            menu.toggleAllMenuItemsVisibility(false)
                            searchMenuItem?.isVisible = true
                            // Display the select option
                            menu.findItem(R.id.select).isVisible = true
                        }

                        R.id.main_player, R.id.track_info -> {
                            when {
                                adapterType == OFFLINE_ADAPTER -> {
                                    menu.toggleAllMenuItemsVisibility(false)

                                    menu.findItem(R.id.properties).isVisible =
                                        currentFragment == R.id.main_player

                                    menu.findItem(R.id.share).isVisible =
                                        currentFragment == R.id.main_player
                                }

                                adapterType == RUBBISH_BIN_ADAPTER || megaApi.isInRubbish(
                                    megaApi.getNodeByHandle(
                                        it.getCurrentPlayingHandle()
                                    )
                                ) -> {
                                    menu.toggleAllMenuItemsVisibility(false)

                                    menu.findItem(R.id.properties).isVisible =
                                        currentFragment == R.id.main_player

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
                                        || adapterType == FROM_IMAGE_VIEWER
                                        || adapterType == VERSIONS_ADAPTER -> {
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
                                        currentFragment == R.id.main_player

                                    menu.findItem(R.id.share).isVisible =
                                        currentFragment == R.id.main_player
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

    /**
     * Get chat id and chat message from the launch intent.
     *
     * @return first is chat id, second is chat message
     */
    internal fun getChatMessage(): Pair<Long, MegaChatMessage?> {
        val chatId = intent.getLongExtra(INTENT_EXTRA_KEY_CHAT_ID, INVALID_HANDLE)
        val msgId = intent.getLongExtra(INTENT_EXTRA_KEY_MSG_ID, INVALID_HANDLE)

        if (chatId == INVALID_HANDLE || msgId == INVALID_HANDLE) {
            return Pair(chatId, null)
        }

        return Pair(
            first = chatId,
            second = megaChatApi.getMessage(chatId, msgId)
                ?: megaChatApi.getMessageFromNodeHistory(
                    chatId,
                    msgId
                )
        )
    }

    /**
     * Get chat message node from the launch intent.
     *
     * @return chat message node
     */
    internal fun getChatMessageNode(): MegaNode? {
        val pair = getChatMessage()
        val message = pair.second ?: return null

        return ChatController(this).authorizeNodeIfPreview(
            message.megaNodeList.get(0),
            megaChatApi.getChatRoom(pair.first)
        )
    }

    /**
     * Close search mode
     */
    fun closeSearch() {
        searchMenuItem?.collapseActionView()
    }

    internal fun stopPlayer() {
        serviceGateway?.stopAudioPlayer()
        finish()
    }

    /**
     * Launch Activity and stop player
     */
    override fun launchActivity(intent: Intent) {
        startActivity(intent)
        stopPlayer()
    }

    /**
     * launch activity for result
     *
     * @param intent
     * @param requestCode
     */
    override fun launchActivityForResult(intent: Intent, requestCode: Int) {
        currentRequestCode = requestCode
        if (currentRequestCode == Constants.REQUEST_CODE_SELECT_CHAT) {
            nodeAttacherLauncher
        } else {
            nodeSaverLauncher
        }.launch(intent)
    }

    internal abstract fun setupToolbar()

    /**
     * Set toolbar title
     *
     * @param title toolbar title
     */
    internal abstract fun setToolbarTitle(title: String)

    /**
     * Hide tool bar
     *
     * @param animate true is that adds hide animation, otherwise is false
     */
    abstract fun hideToolbar(animate: Boolean = true)

    /**
     * Show tool bar
     *
     * @param animate true is that adds show animation, otherwise is false
     */
    abstract fun showToolbar(animate: Boolean = true)

    /**
     * Set draggable
     *
     * @param draggable ture is draggable, otherwise is false
     */
    abstract fun setDraggable(draggable: Boolean)

    /**
     * Show system UI
     */
    abstract fun showSystemUI()

    /**
     * Setup toolbar colors
     *
     * @param showElevation true is show elevation, otherwise is false.
     */
    abstract fun setupToolbarColors(showElevation: Boolean = false)

    companion object {

        /**
         * The zero value for toolbar elevation
         */
        const val TOOLBAR_ELEVATION_ZERO = 0F

        /**
         * The zero value for translation Y
         */
        const val TRANSLATION_Y_ZERO = 0F
    }
}
