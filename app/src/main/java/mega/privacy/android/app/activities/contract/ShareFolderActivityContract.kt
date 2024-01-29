package mega.privacy.android.app.activities.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.main.AddContactActivity
import mega.privacy.android.app.utils.Constants


/**
 * Versions file activity contract
 */
class ShareFolderActivityContract :
    ActivityResultContract<LongArray, List<String>?>() {
    override fun createIntent(context: Context, input: LongArray): Intent {
        return Intent().apply {
            setClass(context, AddContactActivity::class.java)
            putExtra("contactType", Constants.CONTACT_TYPE_BOTH)
            putExtra(AddContactActivity.EXTRA_NODE_HANDLE, input)
            putExtra("MULTISELECT", if (input.size == 1) 0 else 1)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<String>? {
        return when {
            resultCode == Activity.RESULT_OK &&
                    intent?.extras != null -> {
                val contactsData = intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS)
                when (intent.getIntExtra("MULTISELECT", -1)) {
                    0 -> {
                        contactsData ?: emptyList()
                    }

                    1 -> {
                        contactsData ?: emptyList()
                    }

                    else -> null
                }
            }

            else -> null
        }
    }
}