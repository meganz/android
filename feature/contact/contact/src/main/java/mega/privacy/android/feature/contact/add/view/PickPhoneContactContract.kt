package mega.privacy.android.feature.contact.add.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContract

/**
 * Activity result contract that opens the OS multi-select contact picker
 * (`ACTION_PICK_CONTACTS`), requesting the contacts' email data.
 *
 * [parseResult] returns the session [Uri] handed back by the picker as-is; resolving that Uri into
 * contacts is done by the ViewModel via `GetLocalContactsFromUriUseCase`, so no `ContentResolver`
 * work happens here.
 *
 * This contract is only launched on devices at or above `ACTION_PICK_CONTACTS`'s minimum SDK
 * (see [AddContactViewModel.ANDROID_PICKER_MIN_SDK]).
 */
class PickPhoneContactContract : ActivityResultContract<Unit, Uri?>() {

    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(ACTION_PICK_CONTACTS).apply {
            type = ContactsContract.CommonDataKinds.Email.CONTENT_TYPE
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? = intent?.data

    private companion object {
        const val ACTION_PICK_CONTACTS = "android.intent.action.PICK_CONTACTS"
    }
}
