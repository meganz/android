package mega.privacy.android.app.textEditor

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.provider.Settings.System.FONT_SCALE
import android.provider.Settings.System.getFloat
import android.util.TypedValue.COMPLEX_UNIT_PX
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.animation.AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.shared.resources.R as sharedResR
import mega.privacy.android.app.activities.contract.NameCollisionActivityContract
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.ActivityTextFileEditorBinding
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.Scrollable
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.interfaces.showSnackbarWithChat
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.presentation.extensions.getStorageState
import mega.privacy.android.app.presentation.hidenode.HiddenNodesOnboardingActivity
import mega.privacy.android.app.presentation.settings.model.storageTargetPreference
import mega.privacy.android.app.presentation.transfers.attach.NodeAttachmentViewModel
import mega.privacy.android.app.presentation.transfers.attach.createNodeAttachmentView
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferEvent
import mega.privacy.android.app.presentation.transfers.starttransfer.view.createStartTransferView
import mega.privacy.android.app.textEditor.TextEditorViewModel.Companion.CONVERTED_FILE_NAME
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.ChatUtil.removeAttachmentMessage
import mega.privacy.android.app.utils.Constants.ANIMATION_DURATION
import mega.privacy.android.app.utils.Constants.CHAT_ID
import mega.privacy.android.app.utils.Constants.FILE_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.FROM_HOME_PAGE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_COPY_TO
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IMPORT_TO
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MOVE_TO
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.LONG_SNACKBAR_DURATION
import mega.privacy.android.app.utils.Constants.MESSAGE_ID
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_FOLDER_TO_COPY
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_FOLDER_TO_MOVE
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_IMPORT_FOLDER
import mega.privacy.android.app.utils.Constants.SCROLLING_UP_DIRECTION
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.app.utils.Constants.URL_FILE_LINK
import mega.privacy.android.app.utils.Constants.VERSIONS_ADAPTER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.app.utils.MegaNodeDialogUtil.moveToRubbishOrRemove
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showRenameNodeDialog
import mega.privacy.android.app.utils.MegaNodeUtil.getRootParentNode
import mega.privacy.android.app.utils.MegaNodeUtil.selectFolderToCopy
import mega.privacy.android.app.utils.MegaNodeUtil.selectFolderToMove
import mega.privacy.android.app.utils.MenuUtils.toggleAllMenuItemsVisibility
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.isOnline
import mega.privacy.android.app.utils.ViewUtils.hideKeyboard
import mega.privacy.android.app.utils.permission.PermissionUtils.checkNotificationsPermission
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt.INCOMING_SHARES_ADAPTER
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt.LINKS_ADAPTER
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt.OUTGOING_SHARES_ADAPTER
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt.RUBBISH_BIN_ADAPTER
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.TextEditorCloseMenuToolbarEvent
import mega.privacy.mobile.analytics.event.TextEditorCopyMenuItemEvent
import mega.privacy.mobile.analytics.event.TextEditorDownloadMenuToolbarEvent
import mega.privacy.mobile.analytics.event.TextEditorEditButtonPressedEvent
import mega.privacy.mobile.analytics.event.TextEditorExportFileMenuToolbarEvent
import mega.privacy.mobile.analytics.event.TextEditorHideLineNumbersMenuItemEvent
import mega.privacy.mobile.analytics.event.TextEditorHideNodeMenuItemEvent
import mega.privacy.mobile.analytics.event.TextEditorMoveMenuItemEvent
import mega.privacy.mobile.analytics.event.TextEditorMoveToTheRubbishBinMenuItemEvent
import mega.privacy.mobile.analytics.event.TextEditorRenameMenuItemEvent
import mega.privacy.mobile.analytics.event.TextEditorSaveEditMenuToolbarEvent
import mega.privacy.mobile.analytics.event.TextEditorScreenEvent
import mega.privacy.mobile.analytics.event.TextEditorSendToChatMenuToolbarEvent
import mega.privacy.mobile.analytics.event.TextEditorShareLinkMenuToolbarEvent
import mega.privacy.mobile.analytics.event.TextEditorShowLineNumbersMenuItemEvent
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaShare
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Activity to view and edit text files
 */
@AndroidEntryPoint
class TextEditorActivity : PasscodeActivity(), SnackbarShower, Scrollable {

    companion object {
        private const val SCROLL_TEXT = "SCROLL_TEXT"
        private const val CURSOR_POSITION = "CURSOR_POSITION"
        private const val DISCARD_CHANGES_SHOWN = "DISCARD_CHANGES_SHOWN"
        private const val RENAME_SHOWN = "RENAME_SHOWN"
        private const val TIME_SHOWING_PAGINATION_UI = 4000L
        private const val STATE = "STATE"
        private const val STATE_SHOWN = 0
        private const val STATE_HIDDEN = 1
    }

    /**
     * Mega navigator
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    private val viewModel by viewModels<TextEditorViewModel>()
    private val nodeAttachmentViewModel by viewModels<NodeAttachmentViewModel>()
    private lateinit var binding: ActivityTextFileEditorBinding
    private var menu: Menu? = null
    private var discardChangesDialog: AlertDialog? = null
    private var renameDialog: AlertDialog? = null
    private var errorReadingContentDialog: AlertDialog? = null
    private var currentUIState = STATE_SHOWN
    private var animator: ViewPropertyAnimator? = null
    private var countDownTimer: CountDownTimer? = null
    private var isHiddenNodesEnabled: Boolean = false
    private var tempNodeId: NodeId? = null
    private var originalContentTextSize: Float = 0f
    private var originalNameTextSize: Float = 0f
    private val elevation by lazy { resources.getDimension(R.dimen.toolbar_elevation) }
    private var statusBarHeight = 0
    private var appBarHeight = 0
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!viewModel.isViewMode() && viewModel.isFileEdited()) {
                binding.contentEditText.hideKeyboard()
                showDiscardChangesConfirmationDialog()
            } else {
                if (viewModel.isEditMode()) {
                    viewModel.setViewMode()
                    return
                }

                if (viewModel.isCreateMode()) {
                    viewModel.saveFile(intent.getBooleanExtra(FROM_HOME_PAGE, false))
                    return //it will finish once saved successfully
                } else if (viewModel.isReadingContent()) {
                    viewModel.finishBeforeClosing()
                }
                retryConnectionsAndSignalPresence()
                finish()
            }
        }
    }
    private val nameCollisionActivityContract = registerForActivityResult(
        NameCollisionActivityContract()
    ) { result ->
        result?.let {
            showSnackbar(SNACKBAR_TYPE, it, INVALID_HANDLE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTextFileEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Analytics.tracker.trackEvent(TextEditorScreenEvent)

        lifecycleScope.launch {
            runCatching {
                isHiddenNodesEnabled = isHiddenNodesActive()
                invalidateOptionsMenu()
            }.onFailure { Timber.e(it) }
        }

        binding.fileEditorToolbar.post {
            appBarHeight = binding.fileEditorToolbar.height
            updateScrollerViewTopPadding(appBarHeight)
        }

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        getOriginalTextSize()

        setSupportActionBar(binding.fileEditorToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        updateContentPadding()

        if (savedInstanceState == null) {
            viewModel.setInitialValues(
                intent,
                PreferenceManager.getDefaultSharedPreferences(this)
            )
        } else if (viewModel.thereIsErrorSettingContent()) {
            binding.editFab.hide()
            showErrorReadingContentDialog()
            return
        }

        setUpObservers()
        setUpView(savedInstanceState)
        addStartTransferView()
        addNodeAttachmentView()

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(DISCARD_CHANGES_SHOWN, false)) {
                showDiscardChangesConfirmationDialog()
            }

            if (savedInstanceState.getBoolean(RENAME_SHOWN, false)) {
                renameNode()
            }
        }
    }

    private fun updateContentPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }

            binding.fileEditorToolbar.updatePadding(top = insets.top)
            statusBarHeight = insets.top

            val imeHeight = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val isImeVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime())
            binding.contentEditText.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = if (isImeVisible) {
                    imeHeight
                } else {
                    0
                }
            }
            WindowInsetsCompat.CONSUMED
        }
    }

    private suspend fun isHiddenNodesActive(): Boolean {
        val result = runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
        }
        return result.getOrNull() == true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        getScrollSpot().takeIf { it >= 0.0f }?.let {
            outState.putFloat(SCROLL_TEXT, it)
        }
        outState.putInt(STATE, currentUIState)
        outState.putInt(CURSOR_POSITION, binding.contentEditText.selectionStart)
        outState.putBoolean(DISCARD_CHANGES_SHOWN, isDiscardChangesConfirmationDialogShown())
        outState.putBoolean(RENAME_SHOWN, isRenameDialogShown())

        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        // Because of the onResume function of PasscodeActivity invokes setAppFontSize function of Util, the font scale of configuration cannot be more than 1.1.
        // The PasscodeActivity is used for many places, so this is a workaround for making text size of current page follow system font size.
        setTextSizeBasedOnSystem(binding.contentEditText, originalContentTextSize)
        setTextSizeBasedOnSystem(binding.contentText, originalContentTextSize)
        setTextSizeBasedOnSystem(binding.nameText, originalNameTextSize)

        viewModel.updateNode()
    }

    /**
     * Set text size based on system font scale
     * @param textView TextView that will be set text size
     * @param originalTextSize original text size
     */
    private fun setTextSizeBasedOnSystem(textView: TextView, originalTextSize: Float) {
        try {
            val size = originalTextSize * getFloat(contentResolver, FONT_SCALE)
            textView.setTextSize(COMPLEX_UNIT_PX, size)
        } catch (exception: Settings.SettingNotFoundException) {
            Timber.e(exception)
        }
    }

    /**
     * Get original text size for setting text size based on system font scale
     */
    private fun getOriginalTextSize() {
        val fontScale = resources.configuration.fontScale
        originalContentTextSize = binding.contentText.textSize / fontScale
        originalNameTextSize = binding.nameText.textSize / fontScale
    }

    override fun onDestroy() {
        if (isDiscardChangesConfirmationDialogShown()) {
            discardChangesDialog?.dismiss()
        }

        if (isRenameDialogShown()) {
            renameDialog?.dismiss()
        }

        if (isErrorReadingContentDialogShown()) {
            errorReadingContentDialog?.dismiss()
        }
        binding.contentWebView.destroy()
        File(cacheDir, CONVERTED_FILE_NAME).apply {
            if (exists()) delete()
        }
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                Analytics.tracker.trackEvent(TextEditorCloseMenuToolbarEvent)
                onBackPressedDispatcher.onBackPressed()
            }

            R.id.action_save -> {
                Analytics.tracker.trackEvent(TextEditorSaveEditMenuToolbarEvent)
                viewModel.saveFile(intent.getBooleanExtra(FROM_HOME_PAGE, false))
            }

            R.id.action_download -> {
                Analytics.tracker.trackEvent(TextEditorDownloadMenuToolbarEvent)
                checkNotificationsPermission(this)
                viewModel.downloadFile()
            }

            R.id.action_get_link -> {
                Analytics.tracker.trackEvent(TextEditorShareLinkMenuToolbarEvent)
                viewModel.manageLink(this)
            }

            R.id.action_remove_link -> viewModel.manageLink(this)
            R.id.action_send_to_chat -> {
                Analytics.tracker.trackEvent(TextEditorSendToChatMenuToolbarEvent)
                viewModel.getNode()?.let { node ->
                    nodeAttachmentViewModel.startAttachNodes(listOf(NodeId(node.handle)))
                }
            }

            R.id.action_share -> {
                Analytics.tracker.trackEvent(TextEditorExportFileMenuToolbarEvent)
                viewModel.share(this, intent.getStringExtra(URL_FILE_LINK) ?: "")
            }

            R.id.action_rename -> {
                Analytics.tracker.trackEvent(TextEditorRenameMenuItemEvent)
                renameNode()
            }

            R.id.action_hide -> {
                Analytics.tracker.trackEvent(TextEditorHideNodeMenuItemEvent)
                handleHideNodeClick(handle = viewModel.getNode()?.handle ?: 0)
            }

            R.id.action_unhide -> {
                viewModel.hideOrUnhideNode(
                    nodeId = NodeId(viewModel.getNode()?.handle ?: 0),
                    hide = false,
                )
                val message =
                    resources.getQuantityString(sharedR.plurals.unhidden_nodes_result_message, 1, 1)
                Util.showSnackbar(this, message)
            }

            R.id.action_move -> {
                Analytics.tracker.trackEvent(TextEditorMoveMenuItemEvent)
                selectFolderToMove(this, longArrayOf(viewModel.getNode()!!.handle))
            }

            R.id.action_copy -> {
                Analytics.tracker.trackEvent(TextEditorCopyMenuItemEvent)
                selectFolderToCopy(this, longArrayOf(viewModel.getNode()!!.handle))
            }

            R.id.action_line_numbers -> {
                updateLineNumbers()
            }

            R.id.action_move_to_trash, R.id.action_remove -> {
                Analytics.tracker.trackEvent(TextEditorMoveToTheRubbishBinMenuItemEvent)
                moveToRubbishOrRemove(
                    viewModel.getNode()!!.handle,
                    this,
                    this
                )
            }

            R.id.chat_action_import -> importNode()
            R.id.chat_action_save_for_offline -> {
                if (getStorageState() == StorageState.PayWall) {
                    AlertsAndWarnings.showOverDiskQuotaPaywallWarning()
                } else {
                    val msgId = intent.getLongExtra(MESSAGE_ID, MEGACHAT_INVALID_HANDLE)
                    val chatId = intent.getLongExtra(CHAT_ID, MEGACHAT_INVALID_HANDLE)
                    if (chatId == MEGACHAT_INVALID_HANDLE)
                        return false
                    viewModel.saveChatNodeToOffline(
                        chatId = chatId,
                        messageId = msgId
                    )
                }
            }

            R.id.chat_action_remove -> removeAttachmentMessage(
                this,
                viewModel.getChatRoom()!!.chatId,
                viewModel.getMsgChat()
            )
        }

        return super.onOptionsItemSelected(item)
    }

    @Suppress("deprecation")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        when (requestCode) {
            REQUEST_CODE_SELECT_IMPORT_FOLDER -> {
                val toHandle = intent?.getLongExtra(INTENT_EXTRA_KEY_IMPORT_TO, INVALID_HANDLE)
                    ?: return

                viewModel.getNode()?.let {
                    viewModel.importNode(toHandle)
                }
            }

            REQUEST_CODE_SELECT_FOLDER_TO_MOVE -> {
                val toHandle = intent?.getLongExtra(INTENT_EXTRA_KEY_MOVE_TO, INVALID_HANDLE)
                    ?: return

                viewModel.getNode()?.handle?.let {
                    viewModel.moveNode(it, toHandle)
                }
            }

            REQUEST_CODE_SELECT_FOLDER_TO_COPY -> {
                val toHandle = intent?.getLongExtra(INTENT_EXTRA_KEY_COPY_TO, INVALID_HANDLE)
                    ?: return
                viewModel.getNode()?.handle?.let {
                    viewModel.copyNode(it, toHandle)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_text_file_editor, menu)
        this.menu = menu

        menu.findItem(R.id.action_get_link)?.title =
            resources.getQuantityString(sharedR.plurals.label_share_links, 1)

        refreshMenuOptionsVisibility()

        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Sets the right Toolbar options depending on current situation.
     */
    private fun refreshMenuOptionsVisibility() {
        val menu = this.menu ?: return

        if (!isOnline(this)) {
            menu.toggleAllMenuItemsVisibility(false)
            updateLineNumbersMenuOption(menu.findItem(R.id.action_line_numbers))
            return
        }

        if (viewModel.isViewMode()) {
            if (viewModel.getAdapterType() == OFFLINE_ADAPTER) {
                menu.toggleAllMenuItemsVisibility(false)
                menu.findItem(R.id.action_share).isVisible = true
                updateLineNumbersMenuOption(menu.findItem(R.id.action_line_numbers))
                return
            }

            if (viewModel.getNode() == null || viewModel.getNode()!!.isFolder) {
                menu.toggleAllMenuItemsVisibility(false)
                menu.findItem(R.id.action_download).isVisible = true
                updateLineNumbersMenuOption(menu.findItem(R.id.action_line_numbers))
                return
            }

            when (viewModel.getAdapterType()) {
                RUBBISH_BIN_ADAPTER -> {
                    menu.toggleAllMenuItemsVisibility(false)
                    menu.findItem(R.id.action_remove).isVisible = true
                    updateLineNumbersMenuOption(menu.findItem(R.id.action_line_numbers))
                }

                FILE_LINK_ADAPTER, ZIP_ADAPTER -> {
                    menu.toggleAllMenuItemsVisibility(false)
                    menu.findItem(R.id.action_download).isVisible = true
                    menu.findItem(R.id.action_share).isVisible = true
                    updateLineNumbersMenuOption(menu.findItem(R.id.action_line_numbers))
                }

                FOLDER_LINK_ADAPTER, VERSIONS_ADAPTER -> {
                    menu.toggleAllMenuItemsVisibility(false)
                    menu.findItem(R.id.action_download).isVisible = true
                    updateLineNumbersMenuOption(menu.findItem(R.id.action_line_numbers))
                }

                FROM_CHAT -> {
                    menu.toggleAllMenuItemsVisibility(false)
                    menu.findItem(R.id.action_download).isVisible = true
                    updateLineNumbersMenuOption(menu.findItem(R.id.action_line_numbers))

                    if (megaChatApi.initState != MegaChatApi.INIT_ANONYMOUS) {
                        menu.findItem(R.id.chat_action_import).isVisible = true
                        menu.findItem(R.id.chat_action_save_for_offline).isVisible = true
                    }

                    if (viewModel.getMsgChat()?.userHandle == megaChatApi.myUserHandle
                        && viewModel.getMsgChat()?.isDeletable == true
                    ) {
                        menu.findItem(R.id.chat_action_remove).isVisible = true
                    }
                }

                else -> {
                    val node = viewModel.getNode()
                    val rootParentNode = node?.let { megaApi.getRootParentNode(it) }
                    val adapterType = viewModel.getAdapterType()
                    val isInSharedItems = adapterType in listOf(
                        INCOMING_SHARES_ADAPTER,
                        OUTGOING_SHARES_ADAPTER,
                        LINKS_ADAPTER
                    )
                    if (megaApi.isInRubbish(node)) {
                        menu.toggleAllMenuItemsVisibility(false)
                        menu.findItem(R.id.action_remove).isVisible = true
                        updateLineNumbersMenuOption(menu.findItem(R.id.action_line_numbers))
                        return
                    }

                    menu.toggleAllMenuItemsVisibility(true)

                    when (viewModel.getNodeAccess()) {
                        MegaShare.ACCESS_OWNER -> {
                            if (node?.isExported == true) {
                                menu.findItem(R.id.action_get_link).isVisible = false
                            } else {
                                menu.findItem(R.id.action_remove_link).isVisible = false
                            }
                        }

                        MegaShare.ACCESS_FULL -> {
                            menu.findItem(R.id.action_get_link).isVisible = false
                            menu.findItem(R.id.action_remove_link).isVisible = false
                            menu.findItem(R.id.action_hide).isVisible = false
                            menu.findItem(R.id.action_unhide).isVisible = false
                        }

                        MegaShare.ACCESS_READWRITE, MegaShare.ACCESS_READ, MegaShare.ACCESS_UNKNOWN -> {
                            menu.findItem(R.id.action_remove).isVisible = false
                            menu.findItem(R.id.action_move).isVisible = false
                            menu.findItem(R.id.action_move_to_trash).isVisible = false
                            menu.findItem(R.id.action_get_link).isVisible = false
                            menu.findItem(R.id.action_remove_link).isVisible = false
                            menu.findItem(R.id.action_hide).isVisible = false
                            menu.findItem(R.id.action_unhide).isVisible = false
                        }
                    }

                    menu.findItem(R.id.action_copy).isVisible =
                        viewModel.getAdapterType() != FOLDER_LINK_ADAPTER
                    updateLineNumbersMenuOption(menu.findItem(R.id.action_line_numbers))
                    menu.findItem(R.id.chat_action_import).isVisible = false
                    menu.findItem(R.id.action_remove).isVisible = false
                    menu.findItem(R.id.chat_action_save_for_offline).isVisible = false
                    menu.findItem(R.id.chat_action_remove).isVisible = false

                    val currentParentNode = megaApi.getParentNode(node)
                    val isSensitiveInherited =
                        currentParentNode?.let { megaApi.isSensitiveInherited(it) } == true
                    val isInShare = rootParentNode?.isInShare == true
                    val isPaidAccount = viewModel.uiState.value.accountType?.isPaid == true

                    val isNotInShare = !isInSharedItems && !isInShare
                    val isNodeInBackups = viewModel.uiState.value.isNodeInBackups
                    val isBusinessAccountExpired = viewModel.uiState.value.isBusinessAccountExpired

                    val shouldShowHideNode = when {
                        !isHiddenNodesEnabled || isInShare || isInSharedItems || isNodeInBackups -> false
                        isPaidAccount && !isBusinessAccountExpired && ((node != null && node.isMarkedSensitive) || isSensitiveInherited) -> false
                        else -> true
                    }

                    val shouldShowUnhideNode = node != null
                            && isHiddenNodesEnabled
                            && isNotInShare
                            && node.isMarkedSensitive
                            && !isSensitiveInherited
                            && isPaidAccount
                            && !isBusinessAccountExpired
                            && !isNodeInBackups

                    menu.findItem(R.id.action_hide)?.isVisible = shouldShowHideNode
                    menu.findItem(R.id.action_unhide)?.isVisible = shouldShowUnhideNode

                    menu.findItem(R.id.action_save).isVisible = false
                }
            }
        } else {
            binding.contentText.isVisible = false
            menu.toggleAllMenuItemsVisibility(false)
            menu.findItem(R.id.action_save).isVisible = true
            updateLineNumbersMenuOption(menu.findItem(R.id.action_line_numbers))
        }

        // After establishing the Options menu, check if read-only properties should be applied
        checkIfShouldApplyReadOnlyState(menu)
    }

    /**
     * Checks and applies read-only restrictions (unable to Favourite, Rename, Move, or Move to Rubbish Bin)
     * on the Options toolbar if the MegaNode is a Backup node.
     *
     * @param menu The Options Menu
     */
    private fun checkIfShouldApplyReadOnlyState(menu: Menu) {
        viewModel.getNode()?.let {
            if (megaApi.isInVault(it)) {
                with(menu) {
                    findItem(R.id.action_rename).isVisible = false
                    findItem(R.id.action_move).isVisible = false
                    findItem(R.id.action_move_to_trash).isVisible = false
                }
            }
        }
    }

    private fun updateLineNumbersMenuOption(lineNumbersOption: MenuItem) {
        lineNumbersOption.apply {
            isVisible = true
            title = getString(
                if (viewModel.shouldShowLineNumbers()) R.string.action_hide_line_numbers
                else R.string.action_show_line_numbers
            )
        }
    }

    /**
     * Sets the initial state of view and asks for the content text.
     *
     * @param savedInstanceState Saved state if available.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setUpView(savedInstanceState: Bundle?) {
        binding.contentEditText.apply {
            doAfterTextChanged { editable ->
                viewModel.setEditedText(editable?.toString())
            }

            setLineNumberEnabled(viewModel.shouldShowLineNumbers())
        }

        binding.contentText.apply {
            setLineNumberEnabled(viewModel.shouldShowLineNumbers())
            setOnClickListener { if (viewModel.isViewMode()) animateUI() }
        }

        binding.contentWebView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (viewModel.isViewMode()) animateUI()
            }
            return@setOnTouchListener false
        }

        if (savedInstanceState != null && viewModel.thereIsNoErrorSettingContent()
            && !viewModel.needsReadOrIsReadingContent()
        ) {
            currentUIState = savedInstanceState.getInt(STATE, STATE_SHOWN)

            if (currentUIState == STATE_HIDDEN) {
                animateToolbar(false, 0)
                animateBottom(false, 0)
            }

            val text = viewModel.getCurrentText()
            val firstLineNumber = viewModel.getPagination()?.getFirstLineNumber() ?: 1

            binding.contentEditText.apply {
                setText(text, firstLineNumber)

                val cursorPosition = savedInstanceState.getInt(CURSOR_POSITION, INVALID_VALUE)
                if (text != null && cursorPosition >= 0 && cursorPosition < text.length) {
                    setSelection(cursorPosition)
                }
            }

            binding.contentText.setText(text, firstLineNumber)

            val scrollSpot = savedInstanceState.getFloat(SCROLL_TEXT, INVALID_VALUE.toFloat())
            if (scrollSpot != INVALID_VALUE.toFloat()) {
                binding.fileEditorScrollView.post { setScrollSpot(scrollSpot) }
            }
        }

        binding.editFab.apply {
            hide()

            setOnClickListener {
                Analytics.tracker.trackEvent(TextEditorEditButtonPressedEvent)
                viewModel.setEditMode()
                binding.previous.hide()
                binding.next.hide()
            }
        }

        binding.previous.apply {
            hide()
            setOnClickListener { viewModel.previousClicked() }
        }

        binding.next.apply {
            hide()
            setOnClickListener { viewModel.nextClicked() }
        }

        binding.fileEditorScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            checkScroll()
            animatePaginationUI()
            val topPadding = if (scrollY == 0) {
                appBarHeight
            } else {
                statusBarHeight
            }
            animateScrollerViewTopPadding(topPadding, ANIMATION_DURATION / 2) // Faster animation for scroll
            binding.fileEditorScrollView.let {
                if ((it.getChildAt(0).bottom <= it.height + scrollY) || scrollY == 0) {
                    showUI()
                } else {
                    hideUI()
                }
            }
        }
    }

    private fun updateScrollerViewTopPadding(padding: Int) =
        with(binding.fileEditorScrollView) {
            if (paddingTop != padding) {
                updatePadding(top = padding)
            }
        }

    /**
     * Animates the top padding of the scroll view.
     *
     * @param targetPadding Target padding value to animate to.
     * @param duration Animation duration in milliseconds.
     */
    @SuppressLint("RestrictedApi")
    private fun animateScrollerViewTopPadding(
        targetPadding: Int,
        duration: Long = ANIMATION_DURATION,
    ) {
        val currentPadding = binding.fileEditorScrollView.paddingTop
        if (currentPadding == targetPadding) return

        val animator = ValueAnimator.ofInt(currentPadding, targetPadding)
        animator.duration = duration
        animator.interpolator = FAST_OUT_LINEAR_IN_INTERPOLATOR
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            binding.fileEditorScrollView.updatePadding(top = animatedValue)
        }
        animator.start()
    }

    private fun addStartTransferView() {
        binding.root.addView(
            createStartTransferView(
                activity = this,
                transferEventState = viewModel.uiState.map { it.transferEvent },
                onConsumeEvent = {
                    viewModel.consumeTransferEvent()
                },
                onScanningFinished = {
                    // Close activity if upload has started successfully
                    if (it is StartTransferEvent.FinishUploadProcessing) {
                        finish()
                    }
                },
                onCancelNotEnoughSpaceForUploadDialog = {
                    if (!viewModel.isFileEdited()) {
                        finish()
                    }
                },
                navigateToStorageSettings = {
                    megaNavigator.openSettings(
                        this,
                        storageTargetPreference
                    )
                }
            )
        )
    }

    private fun addNodeAttachmentView() {
        binding.root.addView(
            createNodeAttachmentView(
                this,
                nodeAttachmentViewModel,
            ) { message, chatId ->
                showSnackbarWithChat(message, chatId)
            }
        )
    }

    private fun setUpObservers() {
        viewModel.onTextFileEditorDataUpdate().observe(this) { refreshMenuOptionsVisibility() }
        viewModel.getFileName().observe(this, ::showFileName)
        viewModel.getMode().observe(this, ::showMode)
        viewModel.onContentTextRead().observe(this, ::showContentRead)
        viewModel.onSnackBarMessage().observe(this) { message ->
            showSnackbar(getString(message))
        }
        viewModel.getCollision().observe(this) { collision ->
            nameCollisionActivityContract.launch(arrayListOf(collision))
        }
        viewModel.onExceptionThrown().observe(this, ::manageException)
        viewModel.onFatalError().observe(this) {
            showFatalErrorWarningAndFinish()
        }
        collectFlow(viewModel.uiState.map { it.isMarkDownFile }) { isMarkDownFile ->
            binding.contentText.isVisible = !isMarkDownFile
            binding.contentWebView.isVisible = isMarkDownFile
        }

        collectFlow(viewModel.uiState.map { it.markDownFileLoaded }) { isLoaded ->
            if (viewModel.uiState.value.isMarkDownFile) {
                binding.loadingLayout.isVisible = !isLoaded
                binding.editFab.isVisible = isLoaded
            }
        }
    }

    /**
     * Updates the UI depending on the current mode.
     *
     * @param mode Current mode.
     */
    private fun showMode(mode: String) {
        refreshMenuOptionsVisibility()

        if (viewModel.needsReadContent()) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
            viewModel.readFileContent()
        }

        if (viewModel.needsReadOrIsReadingContent()) {
            showLoadingView()
        }

        // Reset UI state when switching modes to ensure proper display
        resetUIState()

        if (mode == TextEditorMode.View.value) {
            supportActionBar?.title = null
            binding.nameText.isVisible = true

            binding.contentEditText.apply {
                isVisible = false
                hideKeyboard()
            }

            binding.contentText.apply {
                isVisible = !viewModel.uiState.value.isMarkDownFile
                text = binding.contentEditText.text
            }

            binding.contentWebView.isVisible = viewModel.uiState.value.isMarkDownFile

            if (viewModel.canShowEditFab() && currentUIState == STATE_SHOWN) {
                binding.editFab.show()
            }
        } else {
            supportActionBar?.title = viewModel.getNameOfFile()
            binding.nameText.isVisible = false
            binding.contentText.isVisible = false
            binding.contentWebView.isVisible = false

            binding.contentEditText.isVisible = true
            binding.editFab.hide()
        }
    }

    /**
     * Shows the loading view.
     */
    private fun showLoadingView() {
        binding.fileEditorScrollView.isVisible = false
        binding.loadingLayout.isVisible = true
    }

    /**
     * Shows the file name.
     *
     * @param name File name.
     */
    private fun showFileName(name: String) {
        supportActionBar?.title = name
        binding.nameText.text = name
    }

    /**
     * Updates the UI and shows the read content.
     *
     * @param content Pagination object with the read content.
     */
    private fun showContentRead(content: Pagination) {
        val currentContent = content.getCurrentPageText()

        if (viewModel.uiState.value.isMarkDownFile) {
            binding.loadingLayout.isVisible = true
            viewModel.convertMarkDownToHtml(this, binding.contentWebView)
        }

        if (viewModel.needsReadOrIsReadingContent()
            || (content.isNotEmpty() && currentContent == binding.contentText.text.toString())
        ) {
            return
        }

        if (binding.contentEditText.text?.isNotEmpty() == true) {
            countDownTimer?.cancel()
            countDownTimer = null
            animatePaginationUI()
        }

        val firstLineNumber = content.getFirstLineNumber()
        binding.contentText.setText(currentContent, firstLineNumber)
        binding.contentEditText.setText(currentContent, firstLineNumber)
        binding.fileEditorScrollView.isVisible = true
        binding.fileEditorScrollView.smoothScrollTo(0, 0)
        binding.loadingLayout.isVisible = false || viewModel.uiState.value.isMarkDownFile
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        if (viewModel.canShowEditFab()
            && currentUIState == STATE_SHOWN
            && !viewModel.uiState.value.isMarkDownFile
        ) {
            binding.editFab.show()
        }

        checkScroll()
    }

    /**
     * Shows a confirmation dialog before discard text changes.
     */
    private fun showDiscardChangesConfirmationDialog() {
        if (isDiscardChangesConfirmationDialogShown()) {
            return
        }

        discardChangesDialog =
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
                .setTitle(R.string.discard_changes_warning)
                .setCancelable(false)
                .setPositiveButton(R.string.discard_close_action) { _, _ ->
                    finish()
                }
                .setNegativeButton(sharedR.string.general_dialog_cancel_button) { dialog, _ ->
                    dialog.dismiss()
                }.show()
    }

    /**
     * Checks if the confirmation dialog to discard text changes is shown.
     *
     * @return True if the dialog is shown, false otherwise.
     */
    private fun isDiscardChangesConfirmationDialogShown(): Boolean =
        discardChangesDialog?.isShowing == true

    /**
     * Manages the rename action.
     */
    private fun renameNode() {
        if (isRenameDialogShown()) {
            return
        }

        renameDialog =
            showRenameNodeDialog(this, viewModel.getNode()!!, this, object : ActionNodeCallback {
                override fun finishRenameActionWithSuccess(newName: String) {
                    binding.nameText.text = newName
                }
            })
    }

    /**
     * Checks if the rename dialog is shown.
     *
     * @return True if the dialog is shown, false otherwise.
     */
    private fun isRenameDialogShown(): Boolean = renameDialog?.isShowing == true

    /**
     * Manages the import node action.
     */
    @Suppress("deprecation")
    private fun importNode() {
        val intent = Intent(this, FileExplorerActivity::class.java)
        intent.action = FileExplorerActivity.ACTION_PICK_IMPORT_FOLDER
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMPORT_FOLDER)
    }

    private fun updateLineNumbers() {
        val enabled = viewModel.setShowLineNumbers()
        Analytics.tracker.trackEvent(
            if (enabled) {
                TextEditorShowLineNumbersMenuItemEvent
            } else {
                TextEditorHideLineNumbersMenuItemEvent
            }
        )
        menu?.findItem(R.id.action_line_numbers)?.let { updateLineNumbersMenuOption(it) }
        binding.contentText.setLineNumberEnabled(enabled)
        binding.contentEditText.setLineNumberEnabled(enabled)
    }

    /**
     * Shows a warning due to an error reading content.
     */
    private fun showErrorReadingContentDialog() {
        if (isErrorReadingContentDialogShown()) {
            return
        }

        errorReadingContentDialog =
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
                .setMessage(getString(R.string.error_opening_file))
                .setCancelable(false)
                .setPositiveButton(sharedResR.string.general_ok) { _, _ ->
                    finish()
                }.show()

    }

    /**
     * Checks if the warning due to an error reading content is shown.
     *
     * @return True if it is shown, false otherwise.
     */
    private fun isErrorReadingContentDialogShown(): Boolean =
        errorReadingContentDialog?.isShowing == true

    /**
     * Hides some UI elements: Toolbar, bottom view and edit button.
     */
    private fun hideUI() {
        if (currentUIState == STATE_HIDDEN || !viewModel.isViewMode()) {
            return
        }

        animateUI()
    }

    private fun showUI() {
        if (currentUIState == STATE_SHOWN || !viewModel.isViewMode()) {
            return
        }

        animateUI()
    }

    /**
     * Shows or hides some UI elements: Toolbar, bottom view and edit button.
     * The action depends on the current UI state:
     *  - STATE_SHOWN: Hides de UI.
     *  - STATE_HIDDEN: Shows the UI.
     */
    private fun animateUI() {
        if (isFinishing || animator != null) {
            return
        }
        Timber.d("animateUI $currentUIState")
        if (currentUIState == STATE_SHOWN) {
            currentUIState = STATE_HIDDEN
            binding.editFab.hide()
            animateToolbar(false, ANIMATION_DURATION)
            animateBottom(false, ANIMATION_DURATION)
            animateScrollerViewTopPadding(statusBarHeight)
        } else {
            currentUIState = STATE_SHOWN

            if (viewModel.canShowEditFab()) {
                binding.editFab.show()
            }

            animateToolbar(true, ANIMATION_DURATION)
            animateBottom(true, ANIMATION_DURATION)
            animateScrollerViewTopPadding(appBarHeight)
        }
    }

    /**
     * Shows or hides toolbar with animation.
     *
     * @param show     True if should show it, false if should hide it.
     * @param duration Animation duration.
     */
    private fun animateToolbar(show: Boolean, duration: Long) {
        @SuppressLint("RestrictedApi")
        animator = binding.appBar
            .animate()
            .translationY(if (show) 0F else -binding.fileEditorToolbar.height.toFloat())
            .setInterpolator(FAST_OUT_LINEAR_IN_INTERPOLATOR)
            .setDuration(duration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    animator = null
                }
            })

        if (show) {
            animator?.withStartAction { binding.appBar.isVisible = true }
        } else {
            animator?.withEndAction { binding.appBar.isVisible = false }
        }
    }

    /**
     * Shows or hides bottom view with animation.
     *
     * @param show     True if should show it, false if should hide it.
     * @param duration Animation duration.
     */
    private fun animateBottom(show: Boolean, duration: Long) {
        @SuppressLint("RestrictedApi")
        val animator = binding.nameText
            .animate()
            .translationY(if (show) 0F else binding.nameText.height.toFloat())
            .setInterpolator(FAST_OUT_LINEAR_IN_INTERPOLATOR)
            .setDuration(duration)

        if (show) {
            animator.withStartAction { binding.nameText.isVisible = true }
        } else {
            animator.withEndAction { binding.nameText.isVisible = false }
        }
    }

    /**
     * Shows pagination UI elements and leaves them visible for TIME_SHOWING_PAGINATION_UI.
     */
    @SuppressLint("StringFormatMatches")
    private fun animatePaginationUI() {
        if (!viewModel.isViewMode() || countDownTimer != null) {
            return
        }

        val pagination = viewModel.getPagination()

        if (pagination == null || pagination.size() <= 1) {
            return
        }

        binding.paginationIndicator.apply {
            text = getString(
                R.string.pagination_progress,
                pagination.getCurrentPage() + 1,
                pagination.size()
            )

            isVisible = !viewModel.uiState.value.isMarkDownFile
        }

        if (pagination.shouldShowNext() && !viewModel.uiState.value.isMarkDownFile) {
            binding.next.show()
        }

        if (pagination.shouldShowPrevious() && !viewModel.uiState.value.isMarkDownFile) {
            binding.previous.show()
        }

        countDownTimer = object :
            CountDownTimer(TIME_SHOWING_PAGINATION_UI, TIME_SHOWING_PAGINATION_UI) {
            override fun onTick(millisUntilFinished: Long) {
            }

            override fun onFinish() {
                binding.paginationIndicator.isVisible = false
                binding.next.hide()
                binding.previous.hide()
                countDownTimer = null
            }
        }.start()
    }

    /**
     * Gets the scroll spot to restore it after rotate the screen.
     *
     * @return The scroll spot.
     */
    private fun getScrollSpot(): Float {
        val y = binding.fileEditorScrollView.scrollY
        val layout = binding.contentText.layout ?: return -1f
        val topPadding = -layout.topPadding

        if (y <= topPadding) {
            return (topPadding - y).toFloat() / binding.contentText.lineHeight
        }

        val line = layout.getLineForVertical(y - 1) + 1
        val offset = layout.getLineStart(line)
        val above = layout.getLineTop(line) - y
        return offset + above.toFloat() / binding.contentText.lineHeight
    }

    /**
     * Sets the scroll spot to restore it after rotate the screen.
     *
     * @param spot The scroll spot.
     */
    private fun setScrollSpot(spot: Float) {
        val offset = spot.roundToInt()
        val above = ((spot - offset) * binding.contentText.lineHeight).roundToInt()
        val layout = binding.contentText.layout
        val line = layout.getLineForOffset(offset)
        val y = (if (line == 0) -layout.topPadding else layout.getLineTop(line)) - above
        binding.fileEditorScrollView.scrollTo(0, y)
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        showSnackbar(type, binding.textFileEditorContainer, content, chatId)
    }

    override fun checkScroll() {
        if (!this::binding.isInitialized) {
            return
        }

        val scrolling = binding.fileEditorScrollView.canScrollVertically(SCROLLING_UP_DIRECTION)
        binding.fileEditorToolbar.elevation = if (scrolling) elevation else 0f
    }

    /**
     * Shows the result of an exception.
     *
     * @param throwable The exception.
     */
    private fun manageException(throwable: Throwable) {
        if (!manageCopyMoveException(throwable) && throwable is MegaException) {
            throwable.message?.let { showSnackbar(it) }
        }
    }

    /**
     * Shows the fatal warning and finishes the activity.
     */
    private fun showFatalErrorWarningAndFinish() {
        showSnackbar(getString(R.string.error_temporary_unavaible))
        Handler(Looper.getMainLooper()).postDelayed({ finish() }, LONG_SNACKBAR_DURATION)
    }

    private fun handleHideNodeClick(handle: Long) {
        var isPaid: Boolean
        var isHiddenNodesOnboarded: Boolean
        var isBusinessAccountExpired: Boolean
        with(viewModel.uiState.value) {
            isPaid = this.accountType?.isPaid == true
            isHiddenNodesOnboarded = this.isHiddenNodesOnboarded
            isBusinessAccountExpired = this.isBusinessAccountExpired
        }

        if (!isPaid || isBusinessAccountExpired) {
            val intent = HiddenNodesOnboardingActivity.createScreen(
                context = this,
                isOnboarding = false,
            )
            hiddenNodesOnboardingLauncher.launch(intent)
            this.overridePendingTransition(0, 0)
        } else if (isHiddenNodesOnboarded) {
            viewModel.hideOrUnhideNode(
                nodeId = NodeId(handle),
                hide = true,
            )
            val message = resources.getQuantityString(R.plurals.hidden_nodes_result_message, 1, 1)
            Util.showSnackbar(this, message)
        } else {
            tempNodeId = NodeId(longValue = handle)
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
        if (result.resultCode != RESULT_OK) return

        viewModel.hideOrUnhideNode(
            nodeId = NodeId(tempNodeId?.longValue ?: 0),
            hide = true,
        )

        val message =
            resources.getQuantityString(
                R.plurals.hidden_nodes_result_message,
                1,
                1,
            )
        Util.showSnackbar(this, message)
    }

    /**
     * Resets the UI state to shown when switching modes to ensure proper display.
     */
    private fun resetUIState() {
        if (currentUIState == STATE_HIDDEN) {
            currentUIState = STATE_SHOWN
            binding.appBar.isVisible = true
            binding.nameText.isVisible = true
            binding.appBar.translationY = 0f
            binding.nameText.translationY = 0f

            // Show edit FAB if appropriate
            if (viewModel.canShowEditFab()) {
                binding.editFab.show()
            }
        }
    }
}