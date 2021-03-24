package mega.privacy.android.app.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.isVisible
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ActivityTextFileEditorBinding
import mega.privacy.android.app.lollipop.PinActivityLollipop
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil.TXT_EXTENSION
import mega.privacy.android.app.utils.Util.showKeyboardDelayed
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode

class TextFileEditorActivity : PinActivityLollipop() {

    companion object {
        const val CREATE_MODE = "CREATE_MODE"
        const val VIEW_AND_EDIT_MODE = "VIEW_AND_EDIT_MODE"
        const val EDITING_MODE = "EDITING_MODE"
    }

    private lateinit var binding: ActivityTextFileEditorBinding

    private var fileName: String? = null
    private var node: MegaNode? = null
    private var mode = VIEW_AND_EDIT_MODE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextFileEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.fileEditorToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setUpMode()

        val receivedName = intent.getStringExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME)

        fileName = if (receivedName != null) receivedName + TXT_EXTENSION else node?.name

        setUpTextFileName()
        setUpTextView()
        setUpEditFAB()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.action_save -> saveFile()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_text_file_editor, menu)

        if (mode == VIEW_AND_EDIT_MODE) {
            val downloadMenuItem = menu?.findItem(R.id.action_download)
            val infoMenuItem = menu?.findItem(R.id.action_properties)
            val shareMenuItem = menu?.findItem(R.id.action_share)
            val sendToChatMenuItem = menu?.findItem(R.id.action_send_to_chat)
            val getLinkMenuItem = menu?.findItem(R.id.action_get_link)
            val removeLinkMenuItem = menu?.findItem(R.id.action_remove_link)
            val renameMenuItem = menu?.findItem(R.id.action_rename)
            val moveMenuItem = menu?.findItem(R.id.action_move)
            val copyMenuItem = menu?.findItem(R.id.action_copy)
            val moveToTrashMenuItem = menu?.findItem(R.id.action_move_to_trash)
            val removeMenuItem = menu?.findItem(R.id.action_remove)
            val chatImportMenuItem = menu?.findItem(R.id.chat_action_import)
            val chatOfflineMenuItem = menu?.findItem(R.id.chat_action_save_for_offline)
            val chatRemoveMenuItem = menu?.findItem(R.id.chat_action_remove)
        } else {
            menu?.findItem(R.id.action_save)?.isVisible = true
        }

        return super.onCreateOptionsMenu(menu)
    }

    private fun setUpMode() {
        node = megaApi.getNodeByHandle(
            intent?.getLongExtra(
                Constants.INTENT_EXTRA_KEY_HANDLE,
                INVALID_HANDLE
            ) ?: INVALID_HANDLE
        )

        mode = if (node == null || node?.isFolder == true) CREATE_MODE else VIEW_AND_EDIT_MODE
    }

    private fun setUpTextFileName() {
        if (mode == VIEW_AND_EDIT_MODE) {
            supportActionBar?.title = null

            binding.nameText.apply {
                isVisible = true
                text = fileName
            }
        } else {
            supportActionBar?.title = fileName
            binding.nameText.isVisible = false
        }
    }

    private fun setUpTextView() {
        if (mode == VIEW_AND_EDIT_MODE) {
            binding.editText.isEnabled = false
        } else {
            binding.editText.isEnabled = true
            binding.editText.requestFocus()
            showKeyboardDelayed(binding.editText)
        }
    }

    private fun setUpEditFAB() {
        binding.editFab.apply {
            isVisible = mode == VIEW_AND_EDIT_MODE

            setOnClickListener {
                mode = EDITING_MODE
                this.hide()
                updateUIAfterChangeMode()
            }
        }
    }

    private fun saveFile() {
        mode = VIEW_AND_EDIT_MODE
        binding.editFab.show()
        updateUIAfterChangeMode()
    }

    private fun updateUIAfterChangeMode() {
        setUpTextFileName()
        setUpTextView()
        invalidateOptionsMenu()
    }
}