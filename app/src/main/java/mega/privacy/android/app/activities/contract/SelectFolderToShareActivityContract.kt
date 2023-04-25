package mega.privacy.android.app.activities.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.utils.Constants

/**
 * Select folder to share
 */
class SelectFolderToShareActivityContract : ActivityResultContract<String, Intent?>() {

    override fun createIntent(context: Context, input: String): Intent {
        return Intent(context, FileExplorerActivity::class.java).apply {
            action = FileExplorerActivity.ACTION_SELECT_FOLDER_TO_SHARE
            putStringArrayListExtra(Constants.SELECTED_CONTACTS, arrayListOf(input))
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Intent? =
        when {
            resultCode == Activity.RESULT_OK && intent?.extras != null -> intent
            else -> null
        }
}
