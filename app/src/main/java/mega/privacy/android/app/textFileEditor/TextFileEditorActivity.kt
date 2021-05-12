package mega.privacy.android.app.textFileEditor

import android.app.ActivityManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Observer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.constants.EventConstants.EVENT_TEXT_FILE_UPLOADED
import mega.privacy.android.app.databinding.ActivityTextFileEditorBinding
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop
import mega.privacy.android.app.lollipop.controllers.ChatController
import mega.privacy.android.app.textFileEditor.TextFileEditorViewModel.Companion.NON_UPDATE_FINISH_ACTION
import mega.privacy.android.app.textFileEditor.TextFileEditorViewModel.Companion.SUCCESS_FINISH_ACTION
import mega.privacy.android.app.textFileEditor.TextFileEditorViewModel.Companion.VIEW_MODE
import mega.privacy.android.app.utils.AlertsAndWarnings.showSaveToDeviceConfirmDialog
import mega.privacy.android.app.utils.ChatUtil.removeAttachmentMessage
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.MegaNodeDialogUtil.Companion.moveToRubbishOrRemove
import mega.privacy.android.app.utils.MegaNodeDialogUtil.Companion.showRenameNodeDialog
import mega.privacy.android.app.utils.MegaNodeUtil.selectFolderToCopy
import mega.privacy.android.app.utils.MegaNodeUtil.selectFolderToMove
import mega.privacy.android.app.utils.MenuUtils.toggleAllMenuItemsVisibility
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util.isOnline
import mega.privacy.android.app.utils.Util.showKeyboardDelayed
import nz.mega.documentscanner.utils.ViewUtils.hideKeyboard
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaShare

@AndroidEntryPoint
class TextFileEditorActivity : PasscodeActivity(), SnackbarShower {

    companion object {
        const val CURSOR_POSITION = "CURSOR_POSITION"
        const val DISCARD_CHANGES_SHOWN = "DISCARD_CHANGES_SHOWN"
        const val RENAME_SHOWN = "RENAME_SHOWN"
        const val FROM_HOME_PAGE = "FROM_HOME_PAGE"
    }

    private val viewModel by viewModels<TextFileEditorViewModel>()

    private lateinit var binding: ActivityTextFileEditorBinding

    private var menu: Menu? = null

    private var discardChangesDialog: AlertDialog? = null
    private var renameDialog: AlertDialog? = null
    private var errorReadingContentDialog: AlertDialog? = null

    private val nodeAttacher by lazy { MegaAttacher(this) }

    private val nodeSaver by lazy {
        NodeSaver(this, this, this, showSaveToDeviceConfirmDialog(this))
    }

    private val completedEditionObserver = Observer<Long> { completedTransferId ->
        val result = viewModel.finishCreationOrEdition(completedTransferId)

        if (result != NON_UPDATE_FINISH_ACTION) {
            showCreationOrEditionResult(result)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LiveEventBus.get(EVENT_TEXT_FILE_UPLOADED, Long::class.java)
            .observeForever(completedEditionObserver)

        binding = ActivityTextFileEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.fileEditorToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            val mi = ActivityManager.MemoryInfo()
            (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
            viewModel.setValuesFromIntent(intent, mi)
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
        if (isDiscardChangesConfirmationDialogShown()) {
            discardChangesDialog?.dismiss()
        }

        if (isRenameDialogShown()) {
            renameDialog?.dismiss()
        }

        if (isErrorReadingContentDialogShown()) {
            errorReadingContentDialog?.dismiss()
        }

        LiveEventBus.get(EVENT_TEXT_FILE_UPLOADED, Long::class.java)
            .removeObserver(completedEditionObserver)

        super.onDestroy()
    }

    override fun onBackPressed() {
        if (viewModel.isFileEdited()) {
            showDiscardChangesConfirmationDialog()
        } else {
            if (viewModel.isCreateMode()) {
                viewModel.saveFile(this)
            } else if (viewModel.isReadingContent()) {
                viewModel.checkIfNeedsStopHttpServer()
            }

            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.action_save -> viewModel.saveFile(this)
            R.id.action_download -> viewModel.downloadFile(nodeSaver)
            R.id.action_get_link, R.id.action_remove_link -> viewModel.manageLink(this)
            R.id.action_send_to_chat -> nodeAttacher.attachNode(viewModel.getNode()!!)
            R.id.action_share -> viewModel.share(this, intent.getStringExtra(URL_FILE_LINK) ?: "")
            R.id.action_rename -> renameNode()
            R.id.action_move -> selectFolderToMove(this, longArrayOf(viewModel.getNode()!!.handle))
            R.id.action_copy -> selectFolderToCopy(this, longArrayOf(viewModel.getNode()!!.handle))
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_text_file_editor, menu)
        this.menu = menu

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
            return
        }

        if (viewModel.isViewMode()) {
            if (viewModel.getAdapterType() == OFFLINE_ADAPTER) {
                menu.toggleAllMenuItemsVisibility(false)
                menu.findItem(R.id.action_share).isVisible = true
                return
            }

            if (viewModel.getNode() == null || viewModel.getNode()!!.isFolder) {
                menu.toggleAllMenuItemsVisibility(false)
                return
            }

            when (viewModel.getAdapterType()) {
                RUBBISH_BIN_ADAPTER -> {
                    menu.toggleAllMenuItemsVisibility(false)
                    menu.findItem(R.id.action_remove).isVisible = true
                }
                FILE_LINK_ADAPTER, ZIP_ADAPTER -> {
                    menu.toggleAllMenuItemsVisibility(false)
                    menu.findItem(R.id.action_download).isVisible = true
                    menu.findItem(R.id.action_share).isVisible = true
                }
                FOLDER_LINK_ADAPTER -> {
                    menu.toggleAllMenuItemsVisibility(false)
                    menu.findItem(R.id.action_download).isVisible = true
                }
                FROM_CHAT -> {
                    menu.toggleAllMenuItemsVisibility(false)
                    menu.findItem(R.id.action_download).isVisible = true

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

            if (savedInstanceState != null && viewModel.thereIsNoErrorSettingContent()) {
                if (viewModel.needsReadOrIsReadingContent()) {
                    return@apply
                }

                setText(viewModel.getEditedText())
                setSelection(savedInstanceState.getInt(CURSOR_POSITION))
            }
        }

        binding.editFab.apply {
            hide()

            setOnClickListener {
                viewModel.setEditMode()
            }
        }
    }

    private fun setUpObservers() {
        viewModel.onTextFileEditorDataUpdate().observe(this) { refreshMenuOptionsVisibility() }
        viewModel.getFileName().observe(this, ::showFileName)
        viewModel.getMode().observe(this, ::showMode)
        viewModel.onSavingMode().observe(this, ::showSavingMode)
        viewModel.onContentTextRead().observe(this, ::showContentRead)
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

            if (viewModel.canShowEditFab()) {
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
        binding.loadingImage.isVisible = true
        binding.loadingProgressBar.isVisible = true
    }

    /**
     * Updates the UI by setting the saving mode.
     *
     * @param savingMode The pre-saving mode.
     */
    private fun showSavingMode(savingMode: String?) {
        if (savingMode != null) {
            binding.nameText.text = StringResourcesUtils.getString(R.string.saving_file)
        }
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
     * @param contentRead Read content.
     */
    private fun showContentRead(contentRead: String) {
        if (viewModel.needsReadOrIsReadingContent()
            || (contentRead.isNotEmpty() && contentRead == binding.contentText.text.toString())
        ) {
            return
        }

        val lines = contentRead.split("(?<=\\G.{" + 1000 + "})")
        for (line in lines) {
            try {
                binding.contentText.append(line)
                binding.contentEditText.append(line)
            } catch (t: Throwable) {
                viewModel.errorSettingContent()
                binding.contentText.text = null
                binding.contentEditText.text = null
                showErrorReadingContentDialog()
                logError("Error setting content.", t)
                break
            }
        }

        binding.fileEditorScrollView.isVisible = true
        binding.loadingImage.isVisible = false
        binding.loadingProgressBar.isVisible = false
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        if (viewModel.canShowEditFab()) {
            binding.editFab.show()
        }
    }

    /**
     * Shows the result of a create or edit action.
     *
     * @param success SUCCESS_FINISH_ACTION if the action finished with success,
     *  ERROR_FINISH_ACTION otherwise.
     */
    private fun showCreationOrEditionResult(success: Int) {
        refreshMenuOptionsVisibility()

        val successful = success == SUCCESS_FINISH_ACTION

        if (successful) {
            binding.nameText.apply {
                isVisible = true
                text = viewModel.getNameOfFile()
            }

            binding.editFab.apply {
                //Necessary to call hide first to avoid not being shown because the view doesn't exist yet
                hide()
                show()
            }
        }

        showSnackbar(
            when {
                viewModel.isSavingModeEdit() -> StringResourcesUtils.getString(if (successful) R.string.file_updated else R.string.file_update_failed)
                intent.getBooleanExtra(
                    FROM_HOME_PAGE,
                    false
                ) -> StringResourcesUtils.getString(
                    if (successful) R.string.file_saved_to else R.string.file_saved_to_failed,
                    StringResourcesUtils.getString(R.string.section_cloud_drive)
                )
                else -> StringResourcesUtils.getString(if (successful) R.string.file_created else R.string.file_creation_failed)
            }
        )

        viewModel.resetSavingMode()
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
    private fun importNode() {
        val intent = Intent(this, FileExplorerActivityLollipop::class.java)
        intent.action = FileExplorerActivityLollipop.ACTION_PICK_IMPORT_FOLDER
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMPORT_FOLDER)
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

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        showSnackbar(type, binding.textFileEditorContainer, content, chatId)
    }
}