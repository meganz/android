package mega.privacy.android.app.activities.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.main.AddContactActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.node.NodeId

/**
 * Activity contract to start and receive the result of the AddContactActivity for sharing nodes
 */
class SelectUsersToShareActivityContract :
    ActivityResultContract<NodeId, List<String>?>() {
    override fun createIntent(context: Context, input: NodeId): Intent =
        Intent(context, AddContactActivity::class.java)
            .putExtra(
                AddContactActivity.EXTRA_CONTACT_TYPE,
                Constants.CONTACT_TYPE_BOTH
            )
            .putExtra(AddContactActivity.EXTRA_MULTISELECT, 0)
            .putExtra(AddContactActivity.EXTRA_NODE_HANDLE, input.longValue)

    override fun parseResult(resultCode: Int, intent: Intent?): List<String>? =
        when (resultCode) {
            Activity.RESULT_OK -> {
                intent?.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS)
            }
            else -> null
        }
}
