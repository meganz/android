package mega.privacy.android.app.main.legacycontact

import android.os.AsyncTask
import mega.privacy.android.app.MegaContactAdapter
import mega.privacy.android.app.main.PhoneContactInfo
import mega.privacy.android.app.main.ShareContactInfo
import mega.privacy.android.app.utils.Constants
import timber.log.Timber
import java.lang.ref.WeakReference

internal class GetPhoneContactsTask(addContactActivity: AddContactActivity) :
    AsyncTask<Void, Void, Void>() {
    private val activityReference = WeakReference(addContactActivity)
    private fun activityRef() = activityReference.get().takeUnless { it == null || it.isFinishing }
    private var inProgressPosition: Int = Constants.INVALID_POSITION

    override fun doInBackground(vararg voids: Void): Void? {
        val addContactActivity = activityRef() ?: return null
        addContactActivity.getDeviceContacts()
        var contactMEGA: MegaContactAdapter?
        var contactPhone: PhoneContactInfo
        var found: Boolean
        addContactActivity.shareContacts.clear()

        if (!addContactActivity.filteredContactsShare.isEmpty()) {
            val pos = addContactActivity.filteredContactsShare.size - 1
            val lastItem = addContactActivity.filteredContactsShare[pos]

            if (lastItem.isProgress) {
                inProgressPosition = pos
            }
        }

        if (addContactActivity.filteredContactsPhone != null && !addContactActivity.filteredContactsPhone.isEmpty()) {
            addContactActivity.shareContacts.add(ShareContactInfo(true, false, true))
            var i = 0
            while (i < addContactActivity.filteredContactsPhone.size) {
                found = false
                contactPhone = addContactActivity.filteredContactsPhone[i]
                for (j in addContactActivity.filteredContactMEGA.indices) {
                    contactMEGA = addContactActivity.filteredContactMEGA[j]
                    if (contactPhone.email == addContactActivity.getMegaContactMail(contactMEGA)) {
                        found = true
                        break
                    }
                }
                if (!found) {
                    addContactActivity.shareContacts.add(ShareContactInfo(contactPhone, null, null))
                } else {
                    addContactActivity.filteredContactsPhone.remove(contactPhone)
                    i--
                }
                i++
            }

            addContactActivity.filteredContactsShare.addAll(addContactActivity.shareContacts)
        }

        return null
    }

    override fun onPostExecute(aVoid: Void?) {
        val addContactActivity = activityRef() ?: return
        Timber.d("onPostExecute: GetPhoneContactsTask")
        if (inProgressPosition != Constants.INVALID_POSITION) {
            addContactActivity.filteredContactsShare.removeAt(inProgressPosition)
        }

        addContactActivity.waitingForPhoneContacts = false
        addContactActivity.setShareAdapterContacts(addContactActivity.filteredContactsShare)
    }
}
