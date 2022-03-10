package mega.privacy.android.app.activities.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.main.megachat.ChatExplorerActivity
import mega.privacy.android.app.utils.Constants
import nz.mega.sdk.MegaUser

class SelectChatsToAttachActivityContract : ActivityResultContract<MegaUser, Intent?>() {

    override fun createIntent(context: Context, user: MegaUser): Intent =
        Intent(context, ChatExplorerActivity::class.java).apply {
            putExtra(Constants.USER_HANDLES, longArrayOf(user.handle))
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Intent? =
        when {
            resultCode == Activity.RESULT_OK && intent != null -> intent
            else -> null
        }
}
