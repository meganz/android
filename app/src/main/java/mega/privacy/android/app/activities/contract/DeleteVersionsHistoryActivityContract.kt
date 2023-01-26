package mega.privacy.android.app.activities.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.main.VersionsFileActivity
import mega.privacy.android.app.utils.Constants
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE

/**
 * Activity contract to start VersionsFileActivity.
 * The result will be the node's handle if delete history is triggered
 */
class DeleteVersionsHistoryActivityContract :
    ActivityResultContract<Long, Long>() {
    override fun createIntent(context: Context, input: Long) =
        Intent(context, VersionsFileActivity::class.java)
            .putExtra(Constants.HANDLE, input)

    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): Long =
        if (resultCode == Activity.RESULT_OK && intent != null) {
            intent.getLongExtra(VersionsFileActivity.KEY_DELETE_NODE_HANDLE, INVALID_HANDLE)
        } else {
            INVALID_HANDLE
        }
}