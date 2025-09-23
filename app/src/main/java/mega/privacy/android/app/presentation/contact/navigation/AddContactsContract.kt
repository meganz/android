package mega.privacy.android.app.presentation.contact.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.navigation.destination.AddContactToShare

class AddContactsContract :
    ActivityResultContract<AddContactToShare, AddContactsContract.Output?>() {

    data class Output(
        val emails: List<String>,
        val nodeHandle: Long,
    )

    override fun createIntent(context: Context, input: AddContactToShare): Intent {
        return Intent(context, AddContactActivity::class.java).apply {
            putExtra("contactType", input.contactType.intValue)
            if (input.nodeHandle.size == 1) {
                putExtra("node_handle", input.nodeHandle[0])
                putExtra("MULTISELECT", 0)
            } else if (input.nodeHandle.size > 1) {
                putExtra("nodeHandle", input.nodeHandle.toLongArray())
                putExtra("MULTISELECT", 1)
            }
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Output? {
        if (resultCode != Activity.RESULT_OK || intent == null) return null

        val emails = intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS)?.toList()
            ?: emptyList()
        val nodeHandle = intent.getLongExtra(AddContactActivity.EXTRA_NODE_HANDLE, -1)

        return Output(emails, nodeHandle)
    }
}

internal val AddContactToShare.ContactType.intValue
    get() = when (this) {
        AddContactToShare.ContactType.Mega -> Constants.CONTACT_TYPE_MEGA
        AddContactToShare.ContactType.Device -> Constants.CONTACT_TYPE_DEVICE
        AddContactToShare.ContactType.All -> Constants.CONTACT_TYPE_BOTH
    }