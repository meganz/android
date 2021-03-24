package mega.privacy.android.app.activities.textFileEditor

import android.app.ActivityManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ActivityTextFileEditorBinding
import mega.privacy.android.app.lollipop.PinActivityLollipop
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Util.showKeyboardDelayed
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE

@AndroidEntryPoint
class TextFileEditorActivity : PinActivityLollipop() {

    private val viewModel by viewModels<TextFileEditorViewModel>()

    private lateinit var binding: ActivityTextFileEditorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextFileEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.fileEditorToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel.setModeAndName(
            intent?.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE) ?: INVALID_HANDLE,
            intent?.getStringExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME)
        )

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

        if (viewModel.isViewMode()) {
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

    private fun setUpTextView() {
        if (viewModel.isViewMode()) {
            binding.editText.isEnabled = false
            val mi = ActivityManager.MemoryInfo()
            (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
            binding.editText.setText(viewModel.readFile(mi))
        } else {
            binding.editText.isEnabled = true
            binding.editText.requestFocus()
            showKeyboardDelayed(binding.editText)
        }
    }

    private fun setUpEditFAB() {
        binding.editFab.apply {
            isVisible = viewModel.isViewMode()

            setOnClickListener {
                viewModel.setEditMode()
                this.hide()
                updateUIAfterChangeMode()
            }
        }
    }

    private fun saveFile() {
        viewModel.saveFile()
        binding.editFab.show()
        updateUIAfterChangeMode()
    }

    private fun updateUIAfterChangeMode() {
        setUpTextFileName()
        setUpTextView()
        invalidateOptionsMenu()
    }
}