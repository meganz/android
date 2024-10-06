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
import java.util.Locale

internal class FilterContactsTask(addContactActivity: AddContactActivity) :
    AsyncTask<Void, Void, Void>() {
    private val activityReference = WeakReference(addContactActivity)
    private fun activityRef() = activityReference.get().takeUnless { it == null || it.isFinishing }

    private var queryContactsShare: ArrayList<ShareContactInfo> = ArrayList()

    override fun doInBackground(vararg voids: Void): Void? {
        val addContactActivity = activityRef() ?: return null
        if (addContactActivity.searchExpand) {
            if (addContactActivity.searchAutoComplete != null) {
                addContactActivity.inputString =
                    addContactActivity.searchAutoComplete.text.toString()
            }
        } else {
            addContactActivity.inputString = addContactActivity.typeContactEditText.text.toString()
        }
        if (addContactActivity.inputString != null && addContactActivity.inputString != "") {
            var contactMega: MegaContactAdapter
            var contactPhone: PhoneContactInfo
            var contactShare: ShareContactInfo

            if (addContactActivity.contactType == Constants.CONTACT_TYPE_MEGA) {
                addContactActivity.queryContactMEGA.clear()
                for (i in addContactActivity.filteredContactMEGA.indices) {
                    contactMega = addContactActivity.filteredContactMEGA[i]
                    if (addContactActivity.getMegaContactMail(contactMega)
                            .lowercase(Locale.getDefault()).contains(
                            addContactActivity.inputString.lowercase(Locale.getDefault())
                        )
                        || contactMega.fullName!!.lowercase(Locale.getDefault()).contains(
                            addContactActivity.inputString.lowercase(Locale.getDefault())
                        )
                    ) {
                        addContactActivity.queryContactMEGA.add(contactMega)
                    }
                }
            } else if (addContactActivity.contactType == Constants.CONTACT_TYPE_DEVICE) {
                addContactActivity.queryContactsPhone.clear()
                for (i in addContactActivity.filteredContactsPhone.indices) {
                    contactPhone = addContactActivity.filteredContactsPhone[i]
                    if (contactPhone.email.lowercase(Locale.getDefault()).contains(
                            addContactActivity.inputString.lowercase(Locale.getDefault())
                        )
                        || contactPhone.name.lowercase(Locale.getDefault()).contains(
                            addContactActivity.inputString.lowercase(Locale.getDefault())
                        )
                    ) {
                        addContactActivity.queryContactsPhone.add(contactPhone)
                    }
                }
            } else {
                queryContactsShare.clear()
                var numMega = 0
                var numPhone = 0
                for (i in addContactActivity.filteredContactsShare.indices) {
                    contactShare = addContactActivity.filteredContactsShare[i]
                    if (contactShare.isHeader) {
                        queryContactsShare.add(contactShare)
                    } else {
                        if (contactShare.isMegaContact) {
                            if (addContactActivity.getMegaContactMail(contactShare.getMegaContactAdapter())
                                    .lowercase(
                                        Locale.getDefault()
                                    ).contains(
                                    addContactActivity.inputString.lowercase(Locale.getDefault())
                                )
                                || contactShare.getMegaContactAdapter().fullName!!.lowercase(Locale.getDefault())
                                    .contains(
                                        addContactActivity.inputString.lowercase(Locale.getDefault())
                                    )
                            ) {
                                queryContactsShare.add(contactShare)
                                numMega++
                            }
                        } else if (contactShare.phoneContactInfo != null
                            && ((contactShare.phoneContactInfo.email != null && contactShare.phoneContactInfo.email.lowercase(
                                Locale.getDefault()
                            ).contains(
                                addContactActivity.inputString.lowercase(Locale.getDefault())
                            ))
                                    || (contactShare.phoneContactInfo.name != null && contactShare.phoneContactInfo.name.lowercase(
                                Locale.getDefault()
                            ).contains(
                                addContactActivity.inputString.lowercase(Locale.getDefault())
                            )))
                        ) {
                            queryContactsShare.add(contactShare)
                            numPhone++
                        }
                    }
                }
                if (numMega == 0 && queryContactsShare.size > 0) {
                    val first = queryContactsShare[0]
                    if (first.isHeader && first.isMegaContact) {
                        queryContactsShare.removeAt(0)
                    }
                }
                if (numPhone == 0 && (queryContactsShare.size - 1 >= 0)) {
                    val last = queryContactsShare[queryContactsShare.size - 1]
                    if (last.isHeader && last.isPhoneContact) {
                        queryContactsShare.removeAt(queryContactsShare.size - 1)
                    }
                }
            }
        }
        return null
    }

    override fun onPostExecute(voids: Void?) {
        Timber.d("onPostExecute FilterContactsTask")
        val addContactActivity = activityRef() ?: return
        if (addContactActivity.contactType == Constants.CONTACT_TYPE_MEGA) {
            if (addContactActivity.inputString != null && addContactActivity.inputString != "") {
                addContactActivity.setMegaAdapterContacts(
                    addContactActivity.queryContactMEGA,
                    MegaContactsAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT
                )
            } else {
                addContactActivity.setMegaAdapterContacts(
                    addContactActivity.filteredContactMEGA,
                    MegaContactsAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT
                )
            }
        } else if (addContactActivity.contactType == Constants.CONTACT_TYPE_DEVICE) {
            if (addContactActivity.inputString != null && addContactActivity.inputString != "") {
                addContactActivity.setPhoneAdapterContacts(addContactActivity.queryContactsPhone)
            } else {
                addContactActivity.setPhoneAdapterContacts(addContactActivity.filteredContactsPhone)
            }
        } else {
            if (addContactActivity.inputString != null && addContactActivity.inputString != "") {
                addContactActivity.setShareAdapterContacts(queryContactsShare)
            } else {
                addContactActivity.setShareAdapterContacts(addContactActivity.filteredContactsShare)
            }
        }
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
