package mega.privacy.android.app.activities.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.utils.Constants
import nz.mega.sdk.MegaUser

class SelectFileToShareActivityContract : ActivityResultContract<List<MegaUser>, Intent?>() {

    override fun createIntent(context: Context, users: List<MegaUser>): Intent {
        val emails = users.map { it.email } as ArrayList<String>

        return Intent(context, FileExplorerActivity::class.java).apply {
            action = FileExplorerActivity.ACTION_MULTISELECT_FILE
            putStringArrayListExtra(Constants.SELECTED_CONTACTS, emails)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Intent? =
        when {
            resultCode == Activity.RESULT_OK && intent != null -> intent
            else -> null
        }
}
