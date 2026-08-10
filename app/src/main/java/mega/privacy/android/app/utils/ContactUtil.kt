package mega.privacy.android.app.utils

import android.content.Context
import android.content.Intent
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_FIRST_NAME
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_LAST_NAME
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_NICKNAME
import mega.privacy.android.app.constants.BroadcastConstants.EXTRA_USER_HANDLE
import mega.privacy.android.domain.entity.Contact
import nz.mega.sdk.MegaUser

object ContactUtil {

    /**
     * To retrieve the contact from cache
     *
     * @param contactHandle The contact's ID
     * @return contact
     */
    @JvmStatic
    @Deprecated("Use GetContactFromCacheByHandleUseCase instead.")
    fun getContactDB(contactHandle: Long): Contact? =
        MegaApplication.getInstance().dbH.findContactByHandle(contactHandle)

    @JvmStatic
    fun getMegaUserNameDB(user: MegaUser?): String? {
        if (user == null) return null
        val nameContact = getContactNameDB(user.handle)
        if (nameContact != null) {
            return nameContact
        }

        return user.email
    }

    @JvmStatic
    fun getContactNameDB(contactDB: Contact?): String? {
        if (contactDB == null) {
            return null
        }

        val nicknameText = contactDB.nickname
        if (nicknameText != null) {
            return nicknameText
        }

        val firstNameText = contactDB.firstName
        val lastNameText = contactDB.lastName
        val emailText = contactDB.email

        return buildFullName(firstNameText, lastNameText, emailText)
    }

    @JvmStatic
    fun getContactNameDB(contactHandle: Long): String? {
        val contactDB = getContactDB(contactHandle)
        if (contactDB != null) {
            return getContactNameDB(contactDB)
        }

        return null
    }

    @JvmStatic
    fun getNicknameContact(contactHandle: Long): String? {
        val contactDB = getContactDB(contactHandle) ?: return null
        return contactDB.nickname
    }

    @JvmStatic
    fun getNicknameContact(email: String?): String? {
        val contactDB = MegaApplication.getInstance().dbH.findContactByEmail(email)
        if (contactDB != null) {
            return contactDB.nickname
        }

        return null
    }

    @JvmStatic
    fun buildFullName(name: String?, lastName: String?, mail: String?): String {
        var fullName = ""
        if (!name.isNullOrBlank()) {
            fullName = name
            if (!lastName.isNullOrBlank()) {
                fullName = "$fullName $lastName"
            }
        } else if (!lastName.isNullOrBlank()) {
            fullName = lastName
        } else if (!mail.isNullOrBlank()) {
            fullName = mail
        }
        return fullName
    }

    @JvmStatic
    fun getFirstNameDB(contactHandle: Long): String {
        val contactDB = getContactDB(contactHandle)
        if (contactDB != null) {
            val nicknameText = contactDB.nickname
            if (nicknameText != null) {
                return nicknameText
            }

            val firstNameText = contactDB.firstName
            if (!firstNameText.isNullOrBlank()) {
                return firstNameText
            }

            val lastNameText = contactDB.lastName
            if (!lastNameText.isNullOrBlank()) {
                return lastNameText
            }

            val emailText = contactDB.email
            if (!emailText.isNullOrBlank()) {
                return emailText
            }
        }
        return ""
    }

    @JvmStatic
    fun notifyNicknameUpdate(context: Context, userHandle: Long) {
        notifyUserNameUpdate(context, ACTION_UPDATE_NICKNAME, userHandle)
    }

    @JvmStatic
    fun notifyFirstNameUpdate(context: Context, userHandle: Long) {
        notifyUserNameUpdate(context, ACTION_UPDATE_FIRST_NAME, userHandle)
    }

    @JvmStatic
    fun notifyLastNameUpdate(context: Context, userHandle: Long) {
        notifyUserNameUpdate(context, ACTION_UPDATE_LAST_NAME, userHandle)
    }

    @JvmStatic
    fun notifyUserNameUpdate(context: Context, action: String, userHandle: Long) {
        val intent = Intent(action)
            .putExtra(EXTRA_USER_HANDLE, userHandle)
            .setPackage(context.applicationContext.packageName)
        context.sendBroadcast(intent)
    }

    /**
     * Gets a contact's email from DB.
     *
     * @param contactHandle contact's identifier
     * @return The contact's email.
     */
    @JvmStatic
    fun getContactEmailDB(contactHandle: Long): String? {
        val contactDB = getContactDB(contactHandle)
        return if (contactDB != null) getContactEmailDB(contactDB) else null
    }

    /**
     * Gets a contact's email from DB.
     *
     * @param contactDB contact's MegaContactDB
     * @return The contact's email.
     */
    @JvmStatic
    fun getContactEmailDB(contactDB: Contact?): String? =
        contactDB?.email
}
