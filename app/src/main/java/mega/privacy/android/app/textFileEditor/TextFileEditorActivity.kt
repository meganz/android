package mega.privacy.android.app.textFileEditor

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_TEXT_FILE_UPLOADED
import mega.privacy.android.app.constants.BroadcastConstants.COMPLETED_TRANSFER
import mega.privacy.android.app.databinding.ActivityTextFileEditorBinding
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.listeners.ExportListener
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop
import mega.privacy.android.app.lollipop.controllers.ChatController
import mega.privacy.android.app.textFileEditor.TextFileEditorViewModel.Companion.CONTENT_TEXT
import mega.privacy.android.app.textFileEditor.TextFileEditorViewModel.Companion.EDIT_MODE
import mega.privacy.android.app.textFileEditor.TextFileEditorViewModel.Companion.MODE
import mega.privacy.android.app.utils.AlertsAndWarnings.Companion.showConfirmRemoveLinkDialog
import mega.privacy.android.app.utils.AlertsAndWarnings.Companion.showSaveToDeviceConfirmDialog
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.shareUri
import mega.privacy.android.app.utils.LinksUtil.showGetLinkActivity
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.MegaNodeDialogUtil.Companion.moveToRubbishOrRemove
import mega.privacy.android.app.utils.MegaNodeDialogUtil.Companion.showRenameNodeDialog
import mega.privacy.android.app.utils.MegaNodeUtil.selectFolderToCopy
import mega.privacy.android.app.utils.MegaNodeUtil.selectFolderToMove
import mega.privacy.android.app.utils.MegaNodeUtil.shareLink
import mega.privacy.android.app.utils.MegaNodeUtil.shareNode
import mega.privacy.android.app.utils.MegaNodeUtil.showTakenDownNodeActionNotAvailableDialog
import mega.privacy.android.app.utils.MenuUtils.toggleAllMenuItemsVisibility
import mega.privacy.android.app.utils.RunOnUIThreadUtils.runDelay
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util.isOnline
import mega.privacy.android.app.utils.Util.showKeyboardDelayed
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaShare
import nz.mega.sdk.MegaTransfer.STATE_COMPLETED

@AndroidEntryPoint
class TextFileEditorActivity : PasscodeActivity(), SnackbarShower {

    companion object {
        const val MODIFIED_TEXT = "MODIFIED_TEXT"
        const val CURSOR_POSITION = "CURSOR_POSITION"
        const val DISCARD_CHANGES_SHOWN = "DISCARD_CHANGES_SHOWN"
        const val RENAME_SHOWN = "RENAME_SHOWN"
        const val SAVING_MODE = "SAVING_MODE"
        const val FROM_HOME_PAGE = "FROM_HOME_PAGE"
    }

    private val viewModel by viewModels<TextFileEditorViewModel>()

    private lateinit var binding: ActivityTextFileEditorBinding

    private var menu: Menu? = null

    private var readingContent = false
    private var savingMode: String? = null

    private var discardChangesDialog: AlertDialog? = null
    private var renameDialog: AlertDialog? = null

    private val nodeAttacher by lazy { MegaAttacher(this) }

    private val nodeSaver by lazy {
        NodeSaver(this, this, this, showSaveToDeviceConfirmDialog(this))
    }

    private val completedEditionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null || BROADCAST_ACTION_TEXT_FILE_UPLOADED != intent.action) return

            finishCompletedCreationOrEdition(intent.getLongExtra(COMPLETED_TRANSFER, INVALID_ID.toLong()))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerReceiver(
            completedEditionReceiver,
            IntentFilter(BROADCAST_ACTION_TEXT_FILE_UPLOADED)
        )

        binding = ActivityTextFileEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.fileEditorToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel.setValuesFromIntent(intent, savedInstanceState)
        setUpTextFileName()
        setUpContentTextView(savedInstanceState)
        setUpEditFAB()

        if (savedInstanceState != null) {
            nodeAttacher.restoreState(savedInstanceState)
            nodeSaver.restoreState(savedInstanceState)
            savingMode = savedInstanceState.getString(SAVING_MODE)

            if (savedInstanceState.getBoolean(DISCARD_CHANGES_SHOWN, false)) {
                showDiscardChangesConfirmationDialog()
            }

            if (savedInstanceState.getBoolean(RENAME_SHOWN, false)) {
                renameNode()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(MODE, viewModel.getMode())
        outState.putString(CONTENT_TEXT, viewModel.getContentText())
        outState.putString(MODIFIED_TEXT, binding.contentText.text.toString())
        outState.putInt(CURSOR_POSITION, binding.contentText.selectionStart)
        outState.putBoolean(DISCARD_CHANGES_SHOWN, isDiscardChangesConfirmationDialogShown())
        outState.putBoolean(RENAME_SHOWN, isRenameDialogShown())
        outState.putString(SAVING_MODE, savingMode)

        nodeAttacher.saveState(outState)
        nodeSaver.saveState(outState)

        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()

        viewModel.updateNode()
        refreshMenuOptionsVisibility()
    }

    override fun onDestroy() {
        viewModel.checkIfNeedsStopHttpServer()

        if (isDiscardChangesConfirmationDialogShown()) {
            discardChangesDialog?.dismiss()
        }

        if (isRenameDialogShown()) {
            renameDialog?.dismiss()
        }

        unregisterReceiver(completedEditionReceiver)

        super.onDestroy()
    }

    override fun onBackPressed() {
        if (isFileEdited()) {
            showDiscardChangesConfirmationDialog()
        } else {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.action_save -> saveFile()
            R.id.action_download -> downloadFile()
            R.id.action_get_link, R.id.action_remove_link -> manageLink()
            R.id.action_send_to_chat -> nodeAttacher.attachNode(viewModel.getNode()!!)
            R.id.action_share -> share()
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
            R.id.chat_action_remove -> ChatUtil.removeAttachmentMessage(
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

            if (viewModel.getNode() == null) {
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
     * Sets the name of the file in the corresponding view:
     *  - On footer if is on VIEW_MODE
     *  - On Toolbar if is on CREATE_MODE or EDIT_MODE
     */
    private fun setUpTextFileName() {
        if (viewModel.isViewMode()) {
            supportActionBar?.title = null

            binding.nameText.apply {
                isVisible = true
                text = viewModel.getFileName()
            }
        } else {
            supportActionBar?.title = viewModel.getFileName()
            binding.nameText.isVisible = false
        }
    }

    /**
     * Sets the initial state of content text view.
     * Asks for the content text and sets it when read.
     *
     * @param savedInstanceState Uses it to set the content text if available.
     */
    private fun setUpContentTextView(savedInstanceState: Bundle?) {
        if (viewModel.isViewMode()) {
            binding.contentText.apply {
                isEnabled = false

                if (savedInstanceState != null && viewModel.getContentText() != null) {
                    setText(viewModel.getContentText())
                    return
                }
            }

            val mi = ActivityManager.MemoryInfo()
            (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)

            viewModel.onContentTextRead().observe(this, { contentRead ->
                readingContent = false
                binding.fileEditorScrollView.isVisible = true
                binding.loadingImage.isVisible = false
                binding.loadingProgressBar.isVisible = false
                binding.editFab.isVisible = viewModel.isEditableAdapter()
                binding.contentText.setText(contentRead)
            })

            readingContent = true
            binding.fileEditorScrollView.isVisible = false
            binding.loadingImage.isVisible = true
            binding.loadingProgressBar.isVisible = true
            viewModel.readFileContent(mi)
        } else {
            binding.contentText.apply {
                isEnabled = true

                if (savedInstanceState != null) {
                    setText(savedInstanceState.getString(MODIFIED_TEXT))
                    setSelection(savedInstanceState.getInt(CURSOR_POSITION))
                }

                showKeyboardDelayed(this)
            }
        }
    }

    /**
     * Sets the initial state of edit fab button.
     */
    private fun setUpEditFAB() {
        binding.editFab.apply {
            isVisible = viewModel.isViewMode() && viewModel.isEditableAdapter() && !readingContent

            setOnClickListener {
                viewModel.setEditMode()
                this.hide()
                updateUIAfterChangeMode()
            }
        }
    }

    /**
     * Checks if the content of the file has been modified.
     *
     * @return True if the content has been modified, false otherwise.
     */
    private fun isFileEdited(): Boolean =
        viewModel.getContentText() != binding.contentText.text.toString()

    /**
     * Shows a confirmation dialog before discard text changes.
     */
    private fun showDiscardChangesConfirmationDialog() {
        val builder =
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)

        builder.setTitle(R.string.discard_changes_warning)
            .setCancelable(false)
            .setPositiveButton(R.string.discard_action) { _, _ ->
                finish()
            }
            .setNegativeButton(R.string.button_cancel) { dialog, _ ->
                dialog.dismiss()
            }

        discardChangesDialog = builder.create()
        discardChangesDialog!!.show()
    }

    /**
     * Checks if the confirmation dialog to discard text changes is shown.
     *
     * @return True if the dialog is shown, false otherwise.
     */
    private fun isDiscardChangesConfirmationDialogShown(): Boolean =
        discardChangesDialog?.isShowing ?: false

    /**
     * Starts the save process and updates the UI to show the saving state.
     */
    private fun saveFile() {
        if (isFileEdited()) {
            savingMode = viewModel.getMode()

            if (viewModel.saveFile(binding.contentText.text.toString())) {
                setViewMode(false)
                binding.nameText.text = StringResourcesUtils.getString(R.string.saving_file)
            } else {
                createOrEditActionFailed()
                savingMode = null
            }
        } else {
            setViewMode(true)
        }
    }

    /**
     * Manages the download action.
     */
    private fun downloadFile() {
        when (viewModel.getAdapterType()) {
            OFFLINE_ADAPTER -> nodeSaver.saveOfflineNode(viewModel.getNode()!!.handle, true)
            ZIP_ADAPTER -> nodeSaver.saveUri(
                viewModel.getFileUri(),
                viewModel.getFileName(),
                viewModel.getFileSize(),
                true
            )
            FROM_CHAT -> nodeSaver.saveNode(
                viewModel.getNode()!!,
                highPriority = true,
                isFolderLink = true,
                fromMediaViewer = true
            )
            else -> nodeSaver.saveHandle(
                viewModel.getNode()!!.handle,
                isFolderLink = viewModel.getAdapterType() == FOLDER_LINK_ADAPTER,
                fromMediaViewer = true
            )
        }
    }

    /**
     * Manages the get or remove link action depending on if the node is already exported or not.
     */
    private fun manageLink() {
        if (showTakenDownNodeActionNotAvailableDialog(viewModel.getNode(), this)) {
            return
        }

        if (viewModel.getNode()?.isExported == true) {
            showConfirmRemoveLinkDialog(this) {
                megaApi.disableExport(
                    viewModel.getNode(),
                    ExportListener(this) { runDelay(100L) { finishExportAction() } })
            }
        } else {
            showGetLinkActivity(this, viewModel.getNode()!!.handle)
        }
    }

    /**
     * Manages the share action.
     */
    private fun share() {
        when (viewModel.getAdapterType()) {
            OFFLINE_ADAPTER, ZIP_ADAPTER -> shareUri(
                this,
                viewModel.getFileName(),
                viewModel.getFileUri()
            )
            FILE_LINK_ADAPTER -> shareLink(this, intent.getStringExtra(URL_FILE_LINK))
            else -> shareNode(this, viewModel.getNode()!!) { finishExportAction() }
        }
    }

    /**
     * Updates the UI after finish the export action (get or remove link).
     */
    private fun finishExportAction() {
        viewModel.updateNode()
        refreshMenuOptionsVisibility()
    }

    /**
     * Manages the rename action.
     */
    private fun renameNode() {
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
     * Sets the view mode.
     *
     * @param fabVisible True if the edit fab should be visible, false otherwise.
     */
    private fun setViewMode(fabVisible: Boolean) {
        viewModel.setViewMode()
        if (fabVisible) binding.editFab.show()
        updateUIAfterChangeMode()
    }

    /**
     * Updates the UI after change from one mode to another new one.
     */
    private fun updateUIAfterChangeMode() {
        refreshMenuOptionsVisibility()
        setUpTextFileName()

        binding.contentText.apply {
            isEnabled = !viewModel.isViewMode()

            if (!viewModel.isViewMode()) {
                showKeyboardDelayed(this)
            }
        }
    }

    /**
     * Updates the UI after creation or edition finishes.
     *
     * @param completedTransferId The completed transfer identifier on DB.
     */
    private fun finishCompletedCreationOrEdition(completedTransferId: Long) {
        if (completedTransferId == INVALID_ID.toLong()) {
            logWarning("Invalid completedTransferId")
            return
        }

        val completedTransfer = dbH.getcompletedTransfer(completedTransferId)
        if (completedTransfer == null) {
            logWarning("Invalid completedTransfer")
            return
        }

        if (!viewModel.isSameNode(completedTransfer)) {
            logWarning("Not the same file, no update needed.")
            return
        }

        if (completedTransfer.state != STATE_COMPLETED) {
            createOrEditActionFailed()
            return
        }

        viewModel.setContentText(binding.contentText.text.toString())
        viewModel.updateNode(completedTransfer.nodeHandle.toLong())
        binding.nameText.text = viewModel.getFileName()
        binding.editFab.apply {
            //Necessary to call hide first to avoid not being shown because the view doesn't exist yet
            hide()
            show()
        }

        showSnackbar(
            when {
                savingMode == EDIT_MODE -> StringResourcesUtils.getString(R.string.file_updated)
                intent.getBooleanExtra(
                    FROM_HOME_PAGE,
                    false
                ) -> StringResourcesUtils.getString(
                    R.string.file_saved_to,
                    StringResourcesUtils.getString(R.string.section_cloud_drive)
                )
                else -> StringResourcesUtils.getString(R.string.file_created)
            }
        )

        savingMode = null
    }

    /**
     * Updates the UI and warns about a failure on creation or edition of the file.
     */
    private fun createOrEditActionFailed() {
        viewModel.setEditMode()
        updateUIAfterChangeMode()

        showSnackbar(
            when {
                savingMode == EDIT_MODE -> StringResourcesUtils.getString(R.string.file_update_failed)
                intent.getBooleanExtra(
                    FROM_HOME_PAGE,
                    false
                ) -> StringResourcesUtils.getString(
                    R.string.file_saved_to_failed,
                    StringResourcesUtils.getString(R.string.section_cloud_drive)
                )
                else -> StringResourcesUtils.getString(R.string.file_creation_failed)
            }
        )
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        showSnackbar(type, binding.textFileEditorContainer, content, chatId)
    }
}