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
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.MenuUtils.Companion.toggleAllMenuItemsVisibility
import mega.privacy.android.app.utils.Util.isOnline
import mega.privacy.android.app.utils.Util.showKeyboardDelayed
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaShare

@AndroidEntryPoint
class TextFileEditorActivity : PinActivityLollipop() {

    private val viewModel by viewModels<TextFileEditorViewModel>()

    private lateinit var binding: ActivityTextFileEditorBinding

    private var menu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextFileEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.fileEditorToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel.setValuesFromIntent(intent)
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

            if (viewModel.getAdapterType() == RUBBISH_BIN_ADAPTER
                || megaApi.isInRubbish(viewModel.getNode())
            ) {
                toggleAllMenuItemsVisibility(menu, false)
                menu.findItem(R.id.action_remove).isVisible = true
                return
            }

            if (viewModel.getAdapterType() == FILE_LINK_ADAPTER || viewModel.getAdapterType() == ZIP_ADAPTER) {
                toggleAllMenuItemsVisibility(menu, false)
                return
            }

            if (viewModel.getAdapterType() == FROM_CHAT) {
                toggleAllMenuItemsVisibility(menu, false)
                menu.findItem(R.id.action_download).isVisible = true
                menu.findItem(R.id.action_share).isVisible = true

                if (megaChatApi.initState != MegaChatApi.INIT_ANONYMOUS) {
                    menu.findItem(R.id.chat_action_import).isVisible = true
                    menu.findItem(R.id.chat_action_save_for_offline).isVisible = true
                }

                if (viewModel.getMsgChat()?.userHandle == megaChatApi.myUserHandle
                    && viewModel.getMsgChat()?.isDeletable == true) {
                    menu.findItem(R.id.chat_action_remove).isVisible = true
                }

                return
            }

            toggleAllMenuItemsVisibility(menu, true)

            when(viewModel.getNodeAccess()) {
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

            menu.findItem(R.id.action_copy).isVisible = viewModel.getAdapterType() != FOLDER_LINK_ADAPTER
            menu.findItem(R.id.chat_action_import).isVisible = false
            menu.findItem(R.id.chat_action_save_for_offline).isVisible = false
            menu.findItem(R.id.chat_action_remove).isVisible = false
        } else {
            menu.findItem(R.id.action_save)?.isVisible = true
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
        viewModel.saveFile(binding.editText.text.toString())
        binding.editFab.show()
        updateUIAfterChangeMode()
    }

    private fun updateUIAfterChangeMode() {
        setUpTextFileName()
        setUpTextView()
        invalidateOptionsMenu()
    }
}