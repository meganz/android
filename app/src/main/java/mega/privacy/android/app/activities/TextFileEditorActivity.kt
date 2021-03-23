package mega.privacy.android.app.activities

import android.os.Bundle
import android.view.MenuItem
import androidx.core.view.isVisible
import mega.privacy.android.app.databinding.ActivityTextFileEditorBinding
import mega.privacy.android.app.lollipop.PinActivityLollipop
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil.TXT_EXTENSION
import mega.privacy.android.app.utils.StringResourcesUtils
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode

class TextFileEditorActivity : PinActivityLollipop() {

    companion object {
        const val CREATE_MODE = "CREATE_MODE"
        const val VIEW_AND_EDIT_MODE = "VIEW_AND_EDIT_MODE"
        const val EDITING_MODE = "EDITING_MODE"
    }

    private lateinit var bindings: ActivityTextFileEditorBinding

    private var node: MegaNode? = null
    private var mode = VIEW_AND_EDIT_MODE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindings = ActivityTextFileEditorBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        setSupportActionBar(bindings.fileEditorToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = null

        setUpMode()
        setUpTextFileName()
        setUpEditFAB()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }

        return super.onOptionsItemSelected(item)
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
        val receivedName = intent.getStringExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME)

        bindings.nameText.text =
            if (mode == CREATE_MODE && receivedName != null) receivedName + TXT_EXTENSION
            else node?.name
    }

    private fun setUpEditFAB() {
        bindings.editFab.apply {
            isVisible = mode == VIEW_AND_EDIT_MODE

            setOnClickListener {
                mode = EDITING_MODE
                this.hide()
                invalidateOptionsMenu()
            }
        }
    }
}