package mega.privacy.android.app.activities.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MOVE_FROM
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MOVE_HANDLES
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MOVE_TO
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE

class SelectFolderToMoveActivityContract : ActivityResultContract<LongArray, Pair<LongArray, Long>>() {

    override fun createIntent(context: Context, nodeHandles: LongArray): Intent =
        Intent(context, FileExplorerActivity::class.java).apply {
            action = FileExplorerActivity.ACTION_PICK_MOVE_FOLDER
            putExtra(INTENT_EXTRA_KEY_MOVE_FROM, nodeHandles)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Pair<LongArray, Long>? =
        when {
            resultCode == Activity.RESULT_OK && intent?.extras != null -> {
                Pair(
                    intent.getLongArrayExtra(INTENT_EXTRA_KEY_MOVE_HANDLES) ?: longArrayOf(),
                    intent.getLongExtra(INTENT_EXTRA_KEY_MOVE_TO, INVALID_HANDLE)
                )
            }
            else -> null
        }
}
