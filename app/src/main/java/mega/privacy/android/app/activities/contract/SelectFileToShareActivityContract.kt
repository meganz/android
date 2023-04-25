package mega.privacy.android.app.activities.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.utils.Constants

/**
 * Select file to share
 */
class SelectFileToShareActivityContract : ActivityResultContract<String, Intent?>() {

    override fun createIntent(context: Context, input: String): Intent {
        return Intent(context, FileExplorerActivity::class.java).apply {
            action = FileExplorerActivity.ACTION_MULTISELECT_FILE
            putStringArrayListExtra(Constants.SELECTED_CONTACTS, arrayListOf(input))
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Intent? =
        when {
            resultCode == Activity.RESULT_OK && intent != null -> intent
            else -> null
        }
}
