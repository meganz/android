package mega.privacy.android.app.activities.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.activities.settingsActivities.PasscodeLockActivity
import mega.privacy.android.app.activities.settingsActivities.PasscodeLockActivity.Companion.ACTION_RESET_PASSCODE_LOCK
import mega.privacy.android.app.activities.settingsActivities.PasscodeLockActivity.Companion.ACTION_SET_PASSCODE_LOCK

class PassCodeActivityContract : ActivityResultContract<Boolean, Boolean>() {

    override fun createIntent(context: Context, input: Boolean): Intent =
        Intent(context, PasscodeLockActivity::class.java).apply {
            action = if (input) ACTION_RESET_PASSCODE_LOCK else ACTION_SET_PASSCODE_LOCK
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        when (resultCode) {
            Activity.RESULT_OK -> true
            else -> false
        }
}
