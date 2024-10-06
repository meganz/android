package mega.privacy.android.app.main.legacycontact

import android.os.AsyncTask
import mega.privacy.android.app.MegaContactAdapter
import mega.privacy.android.app.main.PhoneContactInfo
import mega.privacy.android.app.main.ShareContactInfo
import mega.privacy.android.app.main.adapters.MegaContactsAdapter
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import timber.log.Timber
import java.lang.ref.WeakReference

internal class RecoverContactsTask(addContactActivity: AddContactActivity) :
    AsyncTask<Void, Void, Void>() {
    private val activityReference = WeakReference(addContactActivity)
    private fun activityRef() = activityReference.get().takeUnless { it == null || it.isFinishing }

    override fun doInBackground(vararg voids: Void): Void? {
        val addContactActivity = activityRef() ?: return null
        if (addContactActivity.contactType == Constants.CONTACT_TYPE_MEGA) {
            addContactActivity.getVisibleMEGAContacts()
            var contactToAddMail: String? = null
            var contactToAdd: MegaContactAdapter
            var contact: MegaContactAdapter?
            for (i in addContactActivity.savedaddedContacts.indices) {
                val mail = addContactActivity.savedaddedContacts[i]
                for (j in addContactActivity.filteredContactMEGA.indices) {
                    contact = addContactActivity.filteredContactMEGA[j]
                    contactToAddMail = addContactActivity.getMegaContactMail(contact)
                    if (contactToAddMail != null && contactToAddMail == mail) {
                        if (!addContactActivity.addedContactsMEGA.contains(contact)) {
                            addContactActivity.addedContactsMEGA.add(contact)
                            val filteredPosition =
                                addContactActivity.filteredContactMEGA.indexOf(contact)
                            if (filteredPosition != Constants.INVALID_POSITION) {
                                addContactActivity.filteredContactMEGA[filteredPosition].isSelected =
                                    true
                            }
                        }
                        break
                    }
                }
                if (contactToAddMail != null && contactToAddMail != mail) {
                    contactToAdd = MegaContactAdapter(null, null, mail)
                    if (!addContactActivity.addedContactsMEGA.contains(contactToAdd)) {
                        addContactActivity.addedContactsMEGA.add(contactToAdd)
                    }
                }
            }
        } else {
            addContactActivity.getBothContacts()
            var contactMEGA: MegaContactAdapter?
            var contactPhone: PhoneContactInfo
            var contact: ShareContactInfo? = null
            var found: Boolean
            addContactActivity.shareContacts.clear()

            if (addContactActivity.filteredContactMEGA != null && !addContactActivity.filteredContactMEGA.isEmpty()) {
                addContactActivity.shareContacts.add(ShareContactInfo(true, true, false))
                for (i in addContactActivity.filteredContactMEGA.indices) {
                    contactMEGA = addContactActivity.filteredContactMEGA[i]
                    contact = ShareContactInfo(null, contactMEGA, null)
                    addContactActivity.shareContacts.add(contact)
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
                        addContactActivity.shareContacts.add(
                            ShareContactInfo(
                                contactPhone,
                                null,
                                null
                            )
                        )
                    } else {
                        addContactActivity.filteredContactsPhone.remove(contactPhone)
                        i--
                    }
                    i++
                }
            }
            addContactActivity.filteredContactsShare.clear()
            addContactActivity.filteredContactsShare.addAll(addContactActivity.shareContacts)
            addContactActivity.addedContactsShare.clear()
            var contactToAddMail: String? = null

            for (i in addContactActivity.savedaddedContacts.indices) {
                val mail = addContactActivity.savedaddedContacts[i]
                Timber.d("mail[%d]: %s", i, mail)
                for (j in addContactActivity.filteredContactsShare.indices) {
                    contact = addContactActivity.filteredContactsShare[j]
                    contactToAddMail = if (contact.isMegaContact && !contact.isHeader) {
                        addContactActivity.getMegaContactMail(contact.getMegaContactAdapter())
                    } else if (!contact.isHeader) {
                        contact.phoneContactInfo.email
                    } else {
                        null
                    }
                    if (contactToAddMail != null && contactToAddMail == mail) {
                        if (!addContactActivity.addedContactsShare.contains(contact)) {
                            addContactActivity.addedContactsShare.add(contact)
                            if (contact.isMegaContact) {
                                val megaPosition =
                                    addContactActivity.filteredContactMEGA.indexOf(contact.getMegaContactAdapter())
                                if (megaPosition != Constants.INVALID_POSITION) {
                                    addContactActivity.filteredContactMEGA[megaPosition].isSelected =
                                        true
                                }

                                val sharePosition =
                                    addContactActivity.filteredContactsShare.indexOf(contact)
                                if (sharePosition != Constants.INVALID_POSITION) {
                                    addContactActivity.filteredContactsShare[sharePosition].megaContactAdapter.isSelected =
                                        true
                                }
                            } else {
                                addContactActivity.filteredContactsPhone.remove(contact.phoneContactInfo)
                                addContactActivity.filteredContactsShare.remove(contact)
                            }
                        }
                        break
                    }
                }
                if (contactToAddMail != null && contactToAddMail != mail) {
                    contact = ShareContactInfo(null, null, mail)
                    if (!addContactActivity.addedContactsShare.contains(contact)) {
                        addContactActivity.addedContactsShare.add(contact)
                    }
                }
            }
        }
        return null
    }

    override fun onPostExecute(aVoid: Void?) {
        val addContactActivity = activityRef() ?: return
        Timber.d("onPostExecute RecoverContactsTask")
        addContactActivity.setAddedAdapterContacts()
        if (addContactActivity.searchExpand) {
            if (addContactActivity.isAsyncTaskRunning(addContactActivity.filterContactsTask)) {
                addContactActivity.filterContactsTask.cancel(true)
            }
            addContactActivity.filterContactsTask = FilterContactsTask(addContactActivity)
            addContactActivity.filterContactsTask.execute()
        } else {
            if (addContactActivity.contactType == Constants.CONTACT_TYPE_MEGA) {
                if (addContactActivity.onNewGroup) {
                    addContactActivity.newGroup()
                } else {
                    addContactActivity.setMegaAdapterContacts(
                        addContactActivity.filteredContactMEGA,
                        MegaContactsAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT
                    )
                }
            } else {
                addContactActivity.setShareAdapterContacts(addContactActivity.filteredContactsShare)
            }
            addContactActivity.setTitleAB()
            addContactActivity.setRecyclersVisibility()
            addContactActivity.visibilityFastScroller()

            if (addContactActivity.isConfirmAddShown) {
                if (addContactActivity.isAsyncTaskRunning(addContactActivity.queryIfContactSouldBeAddedTask)) {
                    addContactActivity.queryIfContactSouldBeAddedTask.cancel(true)
                }
                Util.hideKeyboard(addContactActivity, 0)
                addContactActivity.queryIfContactSouldBeAddedTask = QueryIfContactSouldBeAddedTask(
                    addContactActivity
                )
                addContactActivity.queryIfContactSouldBeAddedTask.execute(true)
            }
        }
    }
}
