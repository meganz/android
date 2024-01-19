package mega.privacy.android.app.activities.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.main.VersionsFileActivity
import mega.privacy.android.app.main.VersionsFileActivity.Companion.KEY_DELETE_VERSION_HISTORY
import mega.privacy.android.app.utils.Constants

/**
 * Versions file activity contract
 */
class VersionsFileActivityContract :
    ActivityResultContract<Long, Long?>() {

    override fun createIntent(context: Context, input: Long): Intent =
        Intent(context, VersionsFileActivity::class.java).apply {
            putExtra(Constants.HANDLE, input)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Long? =
        when {
            resultCode == Activity.RESULT_OK && intent?.extras != null
                    && intent.getBooleanExtra(KEY_DELETE_VERSION_HISTORY, false) -> {
                intent.getLongExtra(VersionsFileActivity.KEY_DELETE_NODE_HANDLE, -1)
            }

            else -> null
        }
}
