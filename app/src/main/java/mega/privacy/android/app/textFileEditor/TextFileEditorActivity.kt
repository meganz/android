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
import mega.privacy.android.app.listeners.ExportListener
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop
import mega.privacy.android.app.lollipop.controllers.ChatController
import mega.privacy.android.app.textFileEditor.TextFileEditorViewModel.Companion.CONTENT_TEXT
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
import mega.privacy.android.app.utils.MenuUtils.Companion.toggleAllMenuItemsVisibility
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util.isOnline
import mega.privacy.android.app.utils.Util.showKeyboardDelayed
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaShare

@AndroidEntryPoint
class TextFileEditorActivity : PasscodeActivity(), SnackbarShower {

    companion object {
        const val MODIFIED_TEXT = "MODIFIED_TEXT"
        const val CURSOR_POSITION = "CURSOR_POSITION"
        const val DISCARD_CHANGES_SHOWN = "DISCARD_CHANGES_SHOWN"
        const val RENAME_SHOWN = "RENAME_SHOWN"
    }

    private val viewModel by viewModels<TextFileEditorViewModel>()

    private lateinit var binding: ActivityTextFileEditorBinding

    private var menu: Menu? = null

    private var readingContent = false

    private var discardChangesDialog: AlertDialog? = null
    private var renameDialog: AlertDialog? = null

    private val nodeAttacher by lazy { MegaAttacher(this) }

    private val nodeSaver by lazy {
        NodeSaver(this, this, this, showSaveToDeviceConfirmDialog(this))
    }

    private val completedEditionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null || BROADCAST_ACTION_TEXT_FILE_UPLOADED != intent.action) return

            finishCompletedEdition(intent.getLongExtra(COMPLETED_TRANSFER, INVALID_ID.toLong()))
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
        setUpTextView(savedInstanceState)
        setUpEditFAB()

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
        outState.putString(MODE, viewModel.getMode())
        outState.putString(CONTENT_TEXT, viewModel.getContentText())
        outState.putString(MODIFIED_TEXT, binding.editText.text.toString())
        outState.putInt(CURSOR_POSITION, binding.editText.selectionStart)
        outState.putBoolean(DISCARD_CHANGES_SHOWN, isDiscardChangesConfirmationDialogShown())
        outState.putBoolean(RENAME_SHOWN, isRenameDialogShown())

        nodeAttacher.saveState(outState)
        nodeSaver.saveState(outState)

        super.onSaveInstanceState(outState)
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
            R.id.action_download -> downloadFile(nodeSaver)
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

    private fun refreshMenuOptionsVisibility() {
        val menu = this.menu ?: return

        if (!isOnline(this)) {
            toggleAllMenuItemsVisibility(menu, false)
            return
        }

        if (viewModel.isViewMode()) {
            if (viewModel.getAdapterType() == OFFLINE_ADAPTER) {
                toggleAllMenuItemsVisibility(menu, false)
                menu.findItem(R.id.action_share).isVisible = true
                return
            }

            if (viewModel.getNode() == null) {
                toggleAllMenuItemsVisibility(menu, false)
                return
            }

            when (viewModel.getAdapterType()) {
                RUBBISH_BIN_ADAPTER -> {
                    toggleAllMenuItemsVisibility(menu, false)
                    menu.findItem(R.id.action_remove).isVisible = true
                }
                FILE_LINK_ADAPTER, ZIP_ADAPTER -> {
                    toggleAllMenuItemsVisibility(menu, false)
                    menu.findItem(R.id.action_download).isVisible = true
                    menu.findItem(R.id.action_share).isVisible = true
                }
                FOLDER_LINK_ADAPTER -> {
                    toggleAllMenuItemsVisibility(menu, false)
                    menu.findItem(R.id.action_download).isVisible = true
                }
                FROM_CHAT -> {
                    toggleAllMenuItemsVisibility(menu, false)
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
                        toggleAllMenuItemsVisibility(menu, false)
                        menu.findItem(R.id.action_remove).isVisible = true
                        return
                    }

                    toggleAllMenuItemsVisibility(menu, true)

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
            toggleAllMenuItemsVisibility(menu, false)
            menu.findItem(R.id.action_save).isVisible = true
        }
    }

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

    private fun setUpTextView(savedInstanceState: Bundle?) {
        if (viewModel.isViewMode()) {
            binding.editText.apply {
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
                binding.progressBar.isVisible = false
                binding.editFab.isVisible = viewModel.isEditableAdapter()
                binding.editText.setText(contentRead)
            })

            readingContent = true
            binding.fileEditorScrollView.isVisible = false
            binding.progressBar.isVisible = true
            viewModel.readFileContent(mi)
        } else {
            binding.editText.apply {
                isEnabled = true

                if (savedInstanceState != null) {
                    setText(savedInstanceState.getString(MODIFIED_TEXT))
                    setSelection(savedInstanceState.getInt(CURSOR_POSITION))
                }

                showKeyboardDelayed(this)
            }
        }
    }

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

    private fun isFileEdited(): Boolean =
        viewModel.getContentText() != binding.editText.text.toString()

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

    private fun isDiscardChangesConfirmationDialogShown(): Boolean =
        discardChangesDialog?.isShowing ?: false

    private fun saveFile() {
        if (isFileEdited()) {
            viewModel.saveFile(binding.editText.text.toString())
            setViewMode(false)
            binding.nameText.text = StringResourcesUtils.getString(R.string.saving_file)
        } else {
            setViewMode(true)
        }
    }

    private fun downloadFile(nodeSaver: NodeSaver) {
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

    private fun manageLink() {
        if (showTakenDownNodeActionNotAvailableDialog(viewModel.getNode(), this)) {
            return
        }

        if (viewModel.getNode()?.isExported == true) {
            showConfirmRemoveLinkDialog(this) {
                megaApi.disableExport(viewModel.getNode(), ExportListener(this, ACTION_REMOVE_LINK))
            }
        } else {
            showGetLinkActivity(this, viewModel.getNode()!!.handle)
        }
    }

    private fun share() {
        when (viewModel.getAdapterType()) {
            OFFLINE_ADAPTER, ZIP_ADAPTER -> shareUri(
                this,
                viewModel.getFileName(),
                viewModel.getFileUri()
            )
            FILE_LINK_ADAPTER -> shareLink(this, intent.getStringExtra(URL_FILE_LINK))
            else -> shareNode(this, viewModel.getNode()!!)
        }
    }

    private fun renameNode() {
        renameDialog =
            showRenameNodeDialog(this, viewModel.getNode()!!, this, object : ActionNodeCallback {
                override fun finishRenameActionWithSuccess(newName: String) {
                    binding.nameText.text = newName
                }
            })
    }

    private fun isRenameDialogShown(): Boolean = renameDialog?.isShowing ?: false

    private fun importNode() {
        val intent = Intent(this, FileExplorerActivityLollipop::class.java)
        intent.action = FileExplorerActivityLollipop.ACTION_PICK_IMPORT_FOLDER
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMPORT_FOLDER)
    }

    private fun setViewMode(fabVisible: Boolean) {
        viewModel.setViewMode()
        if (fabVisible) binding.editFab.show()
        updateUIAfterChangeMode()
    }

    private fun updateUIAfterChangeMode() {
        refreshMenuOptionsVisibility()
        setUpTextFileName()

        binding.editText.apply {
            isEnabled = !viewModel.isViewMode()

            if (!viewModel.isViewMode()) {
                showKeyboardDelayed(this)
            }
        }
    }

    private fun finishCompletedEdition(completedTransferId: Long) {
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

        viewModel.setContentText(binding.editText.text.toString())
        viewModel.updateNode(completedTransfer.nodeHandle.toLong())
        binding.nameText.text = viewModel.getFileName()
        binding.editFab.show()
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        showSnackbar(type, binding.textFileEditorContainer, content, chatId)
    }
}