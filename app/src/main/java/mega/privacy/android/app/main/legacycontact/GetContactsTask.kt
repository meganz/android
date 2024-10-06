package mega.privacy.android.app.main.legacycontact

import android.os.AsyncTask
import android.view.View
import mega.privacy.android.app.MegaContactAdapter
import mega.privacy.android.app.main.PhoneContactInfo
import mega.privacy.android.app.main.ShareContactInfo
import mega.privacy.android.app.main.adapters.MegaContactsAdapter
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import timber.log.Timber
import java.lang.ref.WeakReference

internal class GetContactsTask(addContactActivity: AddContactActivity) :
    AsyncTask<Void, Void, Void>() {
    private val activityReference = WeakReference(addContactActivity)
    private fun activityRef() = activityReference.get().takeUnless { it == null || it.isFinishing }
    override fun doInBackground(vararg voids: Void): Void? {
        val addContactActivity = activityRef() ?: return null
        if (addContactActivity.contactType == Constants.CONTACT_TYPE_MEGA) {
            addContactActivity.getVisibleMEGAContacts()
            if (addContactActivity.newGroup) {
                var mail: String
                var contact: MegaContactAdapter
                for (i in addContactActivity.contactsNewGroup.indices) {
                    mail = addContactActivity.contactsNewGroup[i]
                    for (j in addContactActivity.filteredContactMEGA.indices) {
                        contact = addContactActivity.filteredContactMEGA[j]
                        if ((contact.megaUser != null && contact.megaUser!!.email == mail)
                            || (contact.contact != null && contact.contact!!.email != null && contact.contact!!.email == mail)
                        ) {
                            addContactActivity.addedContactsMEGA.add(contact)
                            addContactActivity.filteredContactMEGA.remove(contact)
                            break
                        }
                    }
                }
                addContactActivity.adapterMEGAContacts.setContacts(addContactActivity.addedContactsMEGA)
            }
        } else if (addContactActivity.contactType == Constants.CONTACT_TYPE_DEVICE) {
            if (addContactActivity.queryPermissions) {
                addContactActivity.getBothContacts()
                addContactActivity.addedContactsPhone.clear()
                var found: Boolean
                var contactPhone: PhoneContactInfo
                var contactMEGA: MegaContactAdapter?

                if (addContactActivity.filteredContactsPhone != null && !addContactActivity.filteredContactsPhone.isEmpty()) {
                    var i = 0
                    while (i < addContactActivity.filteredContactsPhone.size) {
                        found = false
                        contactPhone = addContactActivity.filteredContactsPhone[i]
                        for (j in addContactActivity.visibleContactsMEGA.indices) {
                            contactMEGA = addContactActivity.visibleContactsMEGA[j]
                            if (contactPhone.email == addContactActivity.getMegaContactMail(
                                    contactMEGA
                                )
                            ) {
                                found = true
                                break
                            }
                        }
                        if (found) {
                            addContactActivity.filteredContactsPhone.remove(contactPhone)
                            i--
                        }
                        i++
                    }
                }
            }
        } else {
            addContactActivity.getVisibleMEGAContacts()
            addContactActivity.addedContactsPhone.clear()
            var contactMEGA: MegaContactAdapter?
            var contact: ShareContactInfo
            addContactActivity.shareContacts.clear()
            addContactActivity.filteredContactsShare.clear()

            if (addContactActivity.filteredContactMEGA != null && !addContactActivity.filteredContactMEGA.isEmpty()) {
                addContactActivity.shareContacts.add(ShareContactInfo(true, true, false))
                for (i in addContactActivity.filteredContactMEGA.indices) {
                    contactMEGA = addContactActivity.filteredContactMEGA[i]
                    contact = ShareContactInfo(null, contactMEGA, null)
                    addContactActivity.shareContacts.add(contact)
                }

                addContactActivity.filteredContactsShare.addAll(addContactActivity.shareContacts)

                if (addContactActivity.queryPermissions) {
                    addContactActivity.filteredContactsShare.add(ShareContactInfo())
                }
            }
        }
        return null
    }

    override fun onPostExecute(avoid: Void?) {
        val addContactActivity = activityRef() ?: return
        Timber.d("onPostExecute GetContactsTask")
        addContactActivity.progressBar.visibility = View.GONE
        if (addContactActivity.searchExpand) {
            if (addContactActivity.isAsyncTaskRunning(addContactActivity.filterContactsTask)) {
                addContactActivity.filterContactsTask.cancel(true)
            }
            addContactActivity.filterContactsTask = FilterContactsTask(addContactActivity)
            addContactActivity.filterContactsTask.execute()
        } else {
            if (addContactActivity.contactType == Constants.CONTACT_TYPE_MEGA) {
                if (addContactActivity.newGroup) {
                    addContactActivity.setAddedAdapterContacts()
                }
                addContactActivity.setMegaAdapterContacts(
                    addContactActivity.filteredContactMEGA,
                    MegaContactsAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT
                )
            } else if (addContactActivity.contactType == Constants.CONTACT_TYPE_DEVICE) {
                addContactActivity.setPhoneAdapterContacts(addContactActivity.filteredContactsPhone)
            } else {
                addContactActivity.setShareAdapterContacts(addContactActivity.filteredContactsShare)
                if (addContactActivity.queryPermissions) {
                    addContactActivity.waitingForPhoneContacts = true
                    addContactActivity.getPhoneContactsTask = GetPhoneContactsTask(
                        addContactActivity
                    )
                    addContactActivity.getPhoneContactsTask.execute()
                }
            }
            addContactActivity.setTitleAB()
            addContactActivity.setRecyclersVisibility()
            addContactActivity.setSendInvitationVisibility()
            addContactActivity.visibilityFastScroller()
            addContactActivity.setSearchVisibility()

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
