package mega.privacy.android.app.textEditor

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Menu
import android.view.MenuItem
import android.view.ViewPropertyAnimator
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.google.android.material.animation.AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.activities.contract.NameCollisionActivityContract
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.constants.EventConstants.EVENT_PERFORM_SCROLL
import mega.privacy.android.app.databinding.ActivityTextFileEditorBinding
import mega.privacy.android.app.interfaces.Scrollable
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.utils.AlertsAndWarnings.showSaveToDeviceConfirmDialog
import mega.privacy.android.app.textEditor.TextEditorViewModel.Companion.VIEW_MODE
import mega.privacy.android.app.usecase.exception.ForeignNodeException
import mega.privacy.android.app.usecase.exception.MegaException
import mega.privacy.android.app.usecase.exception.OverQuotaException
import mega.privacy.android.app.usecase.exception.PreOverQuotaException
import mega.privacy.android.app.utils.AlertsAndWarnings.showForeignStorageOverQuotaWarningDialog
import mega.privacy.android.app.utils.ChatUtil.removeAttachmentMessage
import mega.privacy.android.app.utils.ColorUtils.changeStatusBarColorForElevation
import mega.privacy.android.app.utils.ColorUtils.getColorForElevation
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.MegaNodeDialogUtil.moveToRubbishOrRemove
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showRenameNodeDialog
import mega.privacy.android.app.utils.MegaNodeUtil.selectFolderToCopy
import mega.privacy.android.app.utils.MegaNodeUtil.selectFolderToMove
import mega.privacy.android.app.utils.MenuUtils.toggleAllMenuItemsVisibility
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util.*
import mega.privacy.android.app.utils.ViewUtils.hideKeyboard
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaShare
import kotlin.math.roundToInt

@AndroidEntryPoint
class TextEditorActivity : PasscodeActivity(), SnackbarShower, Scrollable {

    companion object {
        private const val SCROLL_TEXT = "SCROLL_TEXT"
        private const val CURSOR_POSITION = "CURSOR_POSITION"
        private const val DISCARD_CHANGES_SHOWN = "DISCARD_CHANGES_SHOWN"
        private const val RENAME_SHOWN = "RENAME_SHOWN"
        const val TIME_SHOWING_PAGINATION_UI = 4000L
        private const val STATE = "STATE"
        private const val STATE_SHOWN = 0
        private const val STATE_HIDDEN = 1
    }

    private val viewModel by viewModels<TextEditorViewModel>()

    private lateinit var binding: ActivityTextFileEditorBinding

    private var menu: Menu? = null

    private var discardChangesDialog: AlertDialog? = null
    private var renameDialog: AlertDialog? = null
    private var errorReadingContentDialog: AlertDialog? = null

    private var currentUIState = STATE_SHOWN
    private var animator: ViewPropertyAnimator? = null
    private var countDownTimer: CountDownTimer? = null

    private val elevation by lazy { resources.getDimension(R.dimen.toolbar_elevation) }
    private val toolbarElevationColor by lazy { getColorForElevation(this, elevation) }
    private val transparentColor by lazy {
        ContextCompat.getColor(
            this,
            android.R.color.transparent
        )
    }

    private val nodeAttacher by lazy { MegaAttacher(this) }

    private val nodeSaver by lazy {
        NodeSaver(this, this, this, showSaveToDeviceConfirmDialog(this))
    }

    private val performScrollObserver = Observer<Int> { scrollY ->
        binding.fileEditorScrollView.scrollY = scrollY
    }

    private lateinit var nameCollisionActivityContract: ActivityResultLauncher<Any>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTextFileEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupLaunchers()

        setSupportActionBar(binding.fileEditorToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            val mi = ActivityManager.MemoryInfo()
            (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
            viewModel.setInitialValues(
                intent,
                mi,
                PreferenceManager.getDefaultSharedPreferences(this)
            )
        } else if (viewModel.thereIsErrorSettingContent()) {
            binding.editFab.hide()
            showErrorReadingContentDialog()
            return
        }

        setUpObservers()
        setUpView(savedInstanceState)

        if (savedInstanceState != null) {
            nodeAttacher.restoreState(savedInstanceState)
            nodeSaver.restoreState(savedInstanceState)

            if (savedInstanceState.getBoolean(DISCARD_CHANGES_SHOWN, false)) {
                showDiscardChangesConfirmationDialog()
            }

            if (savedInstanceState.getBoolean(RENAME_SHOWN, false)) {
                renameNode()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putFloat(SCROLL_TEXT, getScrollSpot())
        outState.putInt(STATE, currentUIState)
        outState.putInt(CURSOR_POSITION, binding.contentEditText.selectionStart)
        outState.putBoolean(DISCARD_CHANGES_SHOWN, isDiscardChangesConfirmationDialogShown())
        outState.putBoolean(RENAME_SHOWN, isRenameDialogShown())

        nodeAttacher.saveState(outState)
        nodeSaver.saveState(outState)

        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()

        viewModel.updateNode()
    }

    override fun onDestroy() {
        LiveEventBus.get(EVENT_PERFORM_SCROLL, Int::class.java)
            .removeObserver(performScrollObserver)

        if (isDiscardChangesConfirmationDialogShown()) {
            discardChangesDialog?.dismiss()
        }

        if (isRenameDialogShown()) {
            renameDialog?.dismiss()
        }

        if (isErrorReadingContentDialogShown()) {
            errorReadingContentDialog?.dismiss()
        }

        super.onDestroy()
    }

    override fun onBackPressed() {
        if (psaWebBrowser != null && psaWebBrowser.consumeBack()) return
        if (!viewModel.isViewMode() && viewModel.isFileEdited()) {
            binding.contentEditText.hideKeyboard()
            showDiscardChangesConfirmationDialog()
        } else {
            if (viewModel.isCreateMode()) {
                viewModel.saveFile(this, intent.getBooleanExtra(FROM_HOME_PAGE, false))
            } else if (viewModel.isReadingContent()) {
                viewModel.checkIfNeedsStopHttpServer()
            }

            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.action_save -> viewModel.saveFile(
                this,
                intent.getBooleanExtra(FROM_HOME_PAGE, false)
            )
            R.id.action_download -> viewModel.downloadFile(nodeSaver)
            R.id.action_get_link, R.id.action_remove_link -> viewModel.manageLink(this)
            R.id.action_send_to_chat -> nodeAttacher.attachNode(viewModel.getNode()!!)
            R.id.action_share -> viewModel.share(this, intent.getStringExtra(URL_FILE_LINK) ?: "")
            R.id.action_rename -> renameNode()
            R.id.action_move -> selectFolderToMove(this, longArrayOf(viewModel.getNode()!!.handle))
            R.id.action_copy -> selectFolderToCopy(this, longArrayOf(viewModel.getNode()!!.handle))
            R.id.action_line_numbers -> updateLineNumbers()
            R.id.action_move_to_trash, R.id.action_remove -> moveToRubbishOrRemove(
                viewModel.getNode()!!.handle,
                this,
                this
            )
            R.id.chat_action_import -> importNode()
            R.id.chat_action_save_for_offline -> ChatController(this).saveForOffline(
                viewModel.getMsgChat()!!.megaNodeList,
                viewModel.getChatRoom(),
                true,
                this
            )
            R.id.chat_action_remove -> removeAttachmentMessage(
                this,
                viewModel.getChatRoom()!!.chatId,
                viewModel.getMsgChat()
            )
        }

        return super.onOptionsItemSelected(item)
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
                val toHandle = data?.getLongExtra(INTENT_EXTRA_KEY_IMPORT_TO, INVALID_HANDLE)
                    ?: return

                viewModel.copyNode(toHandle)
            }
            REQUEST_CODE_SELECT_FOLDER_TO_MOVE -> {
                val toHandle = data?.getLongExtra(INTENT_EXTRA_KEY_MOVE_TO, INVALID_HANDLE)
                    ?: return

                viewModel.moveNode(toHandle)
            }
            REQUEST_CODE_SELECT_FOLDER_TO_COPY -> {
                val toHandle = data?.getLongExtra(INTENT_EXTRA_KEY_MOVE_TO, INVALID_HANDLE)
                    ?: return

                viewModel.copyNode(toHandle)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_text_file_editor, menu)
        this.menu = menu

        menu?.findItem(R.id.action_get_link)?.title =
            StringResourcesUtils.getQuantityString(R.plurals.get_links, 1)

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
                    if (megaApi.isInRubbish(viewModel.getNode())) {
                        menu.toggleAllMenuItemsVisibility(false)
                        menu.findItem(R.id.action_remove).isVisible = true
                        updateLineNumbersMenuOption(menu.findItem(R.id.action_line_numbers))
                        return
                    }

                    menu.toggleAllMenuItemsVisibility(true)

                    when (viewModel.getNodeAccess()) {
                        MegaShare.ACCESS_OWNER -> {
                            if (viewModel.getNode()!!.isExported) {
                                menu.findItem(R.id.action_get_link).isVisible = false
                            } else {
                                menu.findItem(R.id.action_remove_link).isVisible = false
                            }
                        }
                        MegaShare.ACCESS_READWRITE, MegaShare.ACCESS_READ, MegaShare.ACCESS_UNKNOWN -> {
                            menu.findItem(R.id.action_remove).isVisible = false
                            menu.findItem(R.id.action_move).isVisible = false
                            menu.findItem(R.id.action_move_to_trash).isVisible = false
                        }
                    }

                    menu.findItem(R.id.action_copy).isVisible =
                        viewModel.getAdapterType() != FOLDER_LINK_ADAPTER
                    updateLineNumbersMenuOption(menu.findItem(R.id.action_line_numbers))
                    menu.findItem(R.id.chat_action_import).isVisible = false
                    menu.findItem(R.id.action_remove).isVisible = false
                    menu.findItem(R.id.chat_action_save_for_offline).isVisible = false
                    menu.findItem(R.id.chat_action_remove).isVisible = false
                    menu.findItem(R.id.action_save).isVisible = false
                }
            }
        } else {
            menu.toggleAllMenuItemsVisibility(false)
            menu.findItem(R.id.action_save).isVisible = true
            updateLineNumbersMenuOption(menu.findItem(R.id.action_line_numbers))
        }
    }

    private fun updateLineNumbersMenuOption(lineNumbersOption: MenuItem) {
        lineNumbersOption.apply {
            isVisible = true
            title = StringResourcesUtils.getString(
                if (viewModel.shouldShowLineNumbers()) R.string.action_hide_line_numbers
                else R.string.action_show_line_numbers
            )
        }
    }

    private fun setupLaunchers() {
        nameCollisionActivityContract =
            registerForActivityResult(NameCollisionActivityContract()) { result ->
                if (result != null) {
                    showSnackbar(result)
                }
            }
    }

    /**
     * Sets the initial state of view and asks for the content text.
     *
     * @param savedInstanceState Saved state if available.
     */
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

        binding.fileEditorScrollView.setOnScrollChangeListener { _, _, _, _, _ ->
            checkScroll()
            hideUI()
            animatePaginationUI()
        }
    }

    private fun setUpObservers() {
        viewModel.onTextFileEditorDataUpdate().observe(this) { refreshMenuOptionsVisibility() }
        viewModel.getFileName().observe(this, ::showFileName)
        viewModel.getMode().observe(this, ::showMode)
        viewModel.onContentTextRead().observe(this, ::showContentRead)
        viewModel.onSnackbarMessage().observe(this) { message ->
            showSnackbar(message)
        }
        viewModel.getCollision().observe(this) { collision ->
            nameCollisionActivityContract.launch(collision)
        }
        viewModel.onExceptionThrown().observe(this, ::manageException)

        LiveEventBus.get(EVENT_PERFORM_SCROLL, Int::class.java)
            .observeForever(performScrollObserver)
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

        if (mode == VIEW_MODE) {
            supportActionBar?.title = null
            binding.nameText.isVisible = true

            binding.contentEditText.apply {
                isVisible = false
                hideKeyboard()
            }

            binding.contentText.apply {
                isVisible = true
                text = binding.contentEditText.text
            }

            if (viewModel.canShowEditFab() && currentUIState == STATE_SHOWN) {
                binding.editFab.show()
            }
        } else {
            supportActionBar?.title = viewModel.getNameOfFile()
            binding.nameText.isVisible = false
            binding.contentText.isVisible = false

            binding.contentEditText.apply {
                isVisible = true
                showKeyboardDelayed(this)
            }

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
        binding.loadingLayout.isVisible = false
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        if (viewModel.canShowEditFab() && currentUIState == STATE_SHOWN) {
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
                .setNegativeButton(R.string.button_cancel) { dialog, _ ->
                    dialog.dismiss()
                }.show()
    }

    /**
     * Checks if the confirmation dialog to discard text changes is shown.
     *
     * @return True if the dialog is shown, false otherwise.
     */
    private fun isDiscardChangesConfirmationDialogShown(): Boolean =
        discardChangesDialog?.isShowing ?: false

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
    private fun isRenameDialogShown(): Boolean = renameDialog?.isShowing ?: false

    /**
     * Manages the import node action.
     */
    @Suppress("deprecation") // TODO Migrate to registerForActivityResult()
    private fun importNode() {
        val intent = Intent(this, FileExplorerActivity::class.java)
        intent.action = FileExplorerActivity.ACTION_PICK_IMPORT_FOLDER
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMPORT_FOLDER)
    }

    private fun updateLineNumbers() {
        val enabled = viewModel.setShowLineNumbers()
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
                .setMessage(StringResourcesUtils.getString(R.string.error_opening_file))
                .setCancelable(false)
                .setPositiveButton(R.string.general_ok) { _, _ ->
                    finish()
                }.show()

    }

    /**
     * Checks if the warning due to an error reading content is shown.
     *
     * @return True if it is shown, false otherwise.
     */
    private fun isErrorReadingContentDialogShown(): Boolean =
        errorReadingContentDialog?.isShowing ?: false

    /**
     * Hides some UI elements: Toolbar, bottom view and edit button.
     */
    private fun hideUI() {
        if (currentUIState == STATE_HIDDEN || !viewModel.isViewMode()) {
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

        if (currentUIState == STATE_SHOWN) {
            currentUIState = STATE_HIDDEN
            binding.editFab.hide()
            animateToolbar(false, ANIMATION_DURATION)
            animateBottom(false, ANIMATION_DURATION)
        } else {
            currentUIState = STATE_SHOWN

            if (viewModel.canShowEditFab()) {
                binding.editFab.show()
            }

            animateToolbar(true, ANIMATION_DURATION)
            animateBottom(true, ANIMATION_DURATION)
        }
    }

    /**
     * Shows or hides toolbar with animation.
     *
     * @param show     True if should show it, false if should hide it.
     * @param duration Animation duration.
     */
    private fun animateToolbar(show: Boolean, duration: Long) {
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
    private fun animatePaginationUI() {
        if (!viewModel.isViewMode() || countDownTimer != null) {
            return
        }

        val pagination = viewModel.getPagination()

        if (pagination == null || pagination.size() <= 1) {
            return
        }

        binding.paginationIndicator.apply {
            text = StringResourcesUtils.getString(
                R.string.pagination_progress,
                pagination.getCurrentPage() + 1,
                pagination.size()
            )

            isVisible = true
        }

        if (pagination.shouldShowNext()) {
            binding.next.show()
        }

        if (pagination.shouldShowPrevious()) {
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
        val layout = binding.contentText.layout
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

        when {
            !scrolling -> {
                binding.fileEditorToolbar.setBackgroundColor(transparentColor)
                binding.appBar.elevation = 0f
            }
            isDarkMode(this) -> {
                binding.fileEditorToolbar.setBackgroundColor(toolbarElevationColor)
            }
            else -> {
                binding.fileEditorToolbar.setBackgroundColor(transparentColor)
                binding.appBar.elevation = elevation
            }
        }

        changeStatusBarColorForElevation(this, scrolling)
    }

    /**
     * Shows the result of an exception.
     *
     * @param throwable The exception.
     */
    private fun manageException(throwable: Throwable) {
        when (throwable) {
            is ForeignNodeException -> {
                showForeignStorageOverQuotaWarningDialog(this)
            }
            is OverQuotaException -> {
                startActivity(Intent(this, ManagerActivity::class.java).apply {
                    action = ACTION_OVERQUOTA_STORAGE
                })
                finish()
            }
            is PreOverQuotaException -> {
                startActivity(Intent(this, ManagerActivity::class.java).apply {
                    action = ACTION_PRE_OVERQUOTA_STORAGE
                })
                finish()
            }
            is MegaException -> {
                showSnackbar(throwable.message!!)
            }
        }
    }
}