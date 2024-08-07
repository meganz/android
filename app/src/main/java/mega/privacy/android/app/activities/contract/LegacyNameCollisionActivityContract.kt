package mega.privacy.android.app.activities.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.namecollision.LegacyNameCollisionActivity.Companion.MESSAGE_RESULT
import mega.privacy.android.app.namecollision.LegacyNameCollisionActivity.Companion.getIntentForList
import mega.privacy.android.app.namecollision.LegacyNameCollisionActivity.Companion.getIntentForSingleItem
import mega.privacy.android.app.namecollision.data.NameCollisionUiEntity

/**
 * A contract to start NameCollisionActivity and manage its result.
 */
@Deprecated("Use NameCollisionActivityContract instead, should be removed during NameCollisionActivity cleanup")
class LegacyNameCollisionActivityContract : ActivityResultContract<ArrayList<NameCollisionUiEntity>, String?>() {

    @Suppress("UNCHECKED_CAST")
    override fun createIntent(context: Context, input: ArrayList<NameCollisionUiEntity>): Intent =
        if (input.size == 1) {
            getIntentForSingleItem(context, input[0])
        } else {
            getIntentForList(context, input)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): String? =
        when (resultCode) {
            Activity.RESULT_OK -> intent?.getStringExtra(MESSAGE_RESULT)
            else -> null
        }
}