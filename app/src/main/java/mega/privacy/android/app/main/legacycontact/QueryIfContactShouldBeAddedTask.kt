package mega.privacy.android.app.main.legacycontact

import mega.privacy.android.shared.resources.R as sharedR
import android.content.DialogInterface
import android.os.AsyncTask
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.R
import mega.privacy.android.app.main.PhoneContactInfo
import mega.privacy.android.app.main.ShareContactInfo
import mega.privacy.android.app.utils.Constants
import timber.log.Timber
import java.lang.ref.WeakReference

class QueryIfContactShouldBeAddedTask(addContactActivity: AddContactActivity) :
    AsyncTask<Boolean, Void, Int>() {
    private val activityReference = WeakReference(addContactActivity)
    private fun activityRef() = activityReference.get().takeUnless { it == null || it.isFinishing }

    private var shareContactInfo: ShareContactInfo? = null
    private var phoneContactInfo: PhoneContactInfo? = null
    private var showDialog: Boolean = false
    private val isShareContact: Int = 1
    private val addContactShare: Int = 2
    private val isPhoneContact: Int = 3
    private val addContactPhone: Int = 4
    private val isAddedContact: Int = 5
    private val isMegaContact: Int = 6

    override fun doInBackground(vararg booleans: Boolean?): Int {
        val addContactActivity = activityRef() ?: return 0
        showDialog = booleans.firstOrNull() ?: false

        if (addContactActivity.contactType == Constants.CONTACT_TYPE_DEVICE) {
            for (i in addContactActivity.addedContactsPhone.indices) {
                if (addContactActivity.addedContactsPhone[i].email == addContactActivity.confirmAddMail) {
                    return isAddedContact
                }
            }
            for (i in addContactActivity.filteredContactsPhone.indices) {
                if (addContactActivity.filteredContactsPhone[i].email == addContactActivity.confirmAddMail) {
                    phoneContactInfo = addContactActivity.filteredContactsPhone[i]
                    return isPhoneContact
                }
            }
            for (i in addContactActivity.visibleContactsMEGA.indices) {
                if (addContactActivity.getMegaContactMail(addContactActivity.visibleContactsMEGA[i]) == addContactActivity.confirmAddMail) {
                    return isMegaContact
                }
            }
            return addContactPhone
        } else if (addContactActivity.contactType == Constants.CONTACT_TYPE_BOTH) {
            for (i in addContactActivity.addedContactsShare.indices) {
                if (addContactActivity.addedContactsShare[i].isMegaContact && !addContactActivity.addedContactsShare[i].isHeader) {
                    if (addContactActivity.getMegaContactMail(addContactActivity.addedContactsShare[i].getMegaContactAdapter()) == addContactActivity.confirmAddMail) {
                        return isAddedContact
                    }
                } else if (addContactActivity.addedContactsShare[i].isPhoneContact && !addContactActivity.addedContactsShare[i].isHeader) {
                    if (addContactActivity.addedContactsShare[i].phoneContactInfo.email == addContactActivity.confirmAddMail) {
                        return isAddedContact
                    }
                } else {
                    if (addContactActivity.addedContactsShare[i].mail == addContactActivity.confirmAddMail) {
                        return isAddedContact
                    }
                }
            }

            for (i in addContactActivity.filteredContactsShare.indices) {
                if (addContactActivity.filteredContactsShare[i].isMegaContact && !addContactActivity.filteredContactsShare[i].isHeader) {
                    if (addContactActivity.getMegaContactMail(addContactActivity.filteredContactsShare[i].getMegaContactAdapter()) == addContactActivity.confirmAddMail) {
                        shareContactInfo = addContactActivity.filteredContactsShare[i]
                        return isShareContact
                    }
                } else if (addContactActivity.filteredContactsShare[i].isPhoneContact && !addContactActivity.filteredContactsShare[i].isHeader) {
                    if (addContactActivity.filteredContactsShare[i].phoneContactInfo.email == addContactActivity.confirmAddMail) {
                        shareContactInfo = addContactActivity.filteredContactsShare[i]
                        return isShareContact
                    }
                }
            }
            return addContactShare
        }
        return 0
    }

    private fun shareContact() {
        val addContactActivity = activityRef() ?: return
        shareContactInfo?.let { addContactActivity.addShareContact(it) }
        if (shareContactInfo?.isMegaContact == true) {
            if (addContactActivity.filteredContactMEGA.size == 1) {
                addContactActivity.filteredContactsShare.removeAt(0)
            }
            addContactActivity.filteredContactMEGA.remove(shareContactInfo?.getMegaContactAdapter())
        } else if (shareContactInfo?.isPhoneContact == true) {
            addContactActivity.filteredContactsPhone.remove(shareContactInfo?.phoneContactInfo)
            if (addContactActivity.filteredContactsPhone.size == 0) {
                addContactActivity.filteredContactsShare.removeAt(addContactActivity.filteredContactsShare.size - 2)
            }
        }
        addContactActivity.filteredContactsShare.remove(shareContactInfo)
        addContactActivity.setShareAdapterContacts(addContactActivity.filteredContactsShare)
    }

    private fun phoneContact() {
        val addContactActivity = activityRef() ?: return
        phoneContactInfo?.let { addContactActivity.addContact(it) }
        addContactActivity.filteredContactsPhone.remove(phoneContactInfo)
        addContactActivity.setPhoneAdapterContacts(addContactActivity.filteredContactsPhone)
    }

    override fun onPostExecute(type: Int) {
        val addContactActivity = activityRef() ?: return
        Timber.d("onPostExecute QueryIfContactSouldBeAddedTask")
        if (showDialog) {
            val builder = MaterialAlertDialogBuilder(
                addContactActivity,
                R.style.ThemeOverlay_Mega_MaterialAlertDialog
            )
            builder.setCancelable(false)

            val dialogClickListener = DialogInterface.OnClickListener { _, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        if (addContactActivity.contactType == Constants.CONTACT_TYPE_DEVICE) {
                            if (type == isPhoneContact) {
                                phoneContact()
                            } else {
                                addContactActivity.addContact(
                                    PhoneContactInfo(
                                        0,
                                        null,
                                        addContactActivity.confirmAddMail,
                                        null
                                    )
                                )
                            }
                        } else if (addContactActivity.contactType == Constants.CONTACT_TYPE_BOTH) {
                            if (type == isShareContact) {
                                shareContact()
                            } else {
                                addContactActivity.addShareContact(
                                    ShareContactInfo(
                                        null,
                                        null,
                                        addContactActivity.confirmAddMail
                                    )
                                )
                            }
                        }
                        addContactActivity.isConfirmAddShown = false
                    }

                    DialogInterface.BUTTON_NEGATIVE -> {
                        //No button clicked
                        addContactActivity.isConfirmAddShown = false
                    }
                }
            }

            when (type) {
                isShareContact, addContactShare -> {
                    builder.setMessage(
                        addContactActivity.getString(
                            R.string.confirmation_share_contact,
                            addContactActivity.confirmAddMail
                        )
                    )

                    builder.setPositiveButton(R.string.menu_add_contact, dialogClickListener)
                        .setNegativeButton(sharedR.string.general_dialog_cancel_button, dialogClickListener).show()
                }

                isPhoneContact, addContactPhone -> {
                    builder.setMessage(
                        addContactActivity.getString(
                            R.string.confirmation_invite_contact,
                            addContactActivity.confirmAddMail
                        )
                    )

                    builder.setPositiveButton(R.string.menu_add_contact, dialogClickListener)
                        .setNegativeButton(sharedR.string.general_dialog_cancel_button, dialogClickListener).show()
                }

                isAddedContact -> {
                    builder.setMessage(
                        addContactActivity.getString(
                            R.string.confirmation_invite_contact_already_added,
                            addContactActivity.confirmAddMail
                        )
                    )

                    builder.setNegativeButton(sharedR.string.general_dialog_cancel_button, dialogClickListener).show()
                }

                isMegaContact -> {
                    builder.setMessage(
                        addContactActivity.getString(
                            R.string.confirmation_not_invite_contact,
                            addContactActivity.confirmAddMail
                        )
                    )

                    builder.setNegativeButton(sharedR.string.general_dialog_cancel_button, dialogClickListener).show()
                }
            }
            addContactActivity.isConfirmAddShown = true
            builder.setOnDismissListener { addContactActivity.isConfirmAddShown = false }
        } else {
            when (type) {
                isShareContact -> {
                    shareContact()
                }

                addContactShare -> {
                    addContactActivity.addShareContact(
                        ShareContactInfo(
                            null,
                            null,
                            addContactActivity.confirmAddMail
                        )
                    )
                }

                isPhoneContact -> {
                    phoneContact()
                }

                addContactPhone -> {
                    addContactActivity.addContact(
                        PhoneContactInfo(
                            0,
                            null,
                            addContactActivity.confirmAddMail,
                            null
                        )
                    )
                }

                isAddedContact -> {
                    addContactActivity.showSnackbar(addContactActivity.getString(R.string.contact_not_added))
                }

                isMegaContact -> {
                    addContactActivity.showSnackbar(
                        addContactActivity.getString(
                            R.string.context_contact_already_exists,
                            addContactActivity.confirmAddMail
                        )
                    )
                }
            }
        }
    }
}
