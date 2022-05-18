package mega.privacy.android.app.utils.contacts

import android.content.Context
import android.text.TextUtils
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.contacts.ContactsUtil.LocalContact
import nz.mega.sdk.*
import java.util.*

class MegaContactGetter(context: Context) {

    companion object {
        private const val INDEX_ID = 1
        private const val INDEX_DATA = 0

        const val LAST_SYNC_TIMESTAMP_FILE = "last_sync_timestamp"
        const val LAST_SYNC_TIMESTAMP_KEY = "last_sync_mega_contacts_timestamp"
    }

    private val dbH = DatabaseHandler.getDbHandler(context)
    private val preferences = context.getSharedPreferences(LAST_SYNC_TIMESTAMP_FILE, Context.MODE_PRIVATE)
    private var lastSyncTimestamp = preferences.getLong(LAST_SYNC_TIMESTAMP_KEY, 0)
    private var updater: MegaContactUpdater? = null
    private val megaContacts = mutableListOf<MegaContact>()
    private val megaContactsWithEmail = mutableListOf<MegaContact>()
    private var currentContactIndex = 0
    private var requestInProgress = false

    fun setMegaContactUpdater(updater: MegaContactUpdater?) {
        this.updater = updater
    }

    fun clearLastSyncTimeStamp() {
        preferences.edit().clear().apply()
    }

    private fun updateLastSyncTimestamp() {
        lastSyncTimestamp = System.currentTimeMillis()
        preferences.edit().putLong(LAST_SYNC_TIMESTAMP_KEY, lastSyncTimestamp).apply()
    }

    private fun getUserEmail(userHandle: Long, api: MegaApiJava) {
        api.getUserEmail(userHandle, OptionalMegaRequestListenerInterface(
            onRequestFinish = { request, error ->
                if (error.errorCode == MegaError.API_OK) {
                    val email = request.email
                    val handle = request.nodeHandle
                    if (!TextUtils.isEmpty(email)) {
                        val currentContact = getCurrentContactIndex()
                        // Avoid to set email to wrong user.
                        if (currentContact != null && handle == currentContact.handle) {
                            currentContact.email = email
                            if (currentContact.localName == null) {
                                currentContact.localName = email
                            }
                        }
                    } else {
                        logWarning("Contact's email is empty!")
                    }
                } else {
                    logWarning("Get contact's email faild with error code: " + error.errorCode)
                }
                // Get next contact's email.
                currentContactIndex++
                // All the emails have been gotten.
                if (currentContactIndex >= megaContacts.size) {
                    megaContacts.addAll(megaContactsWithEmail)
                    // Remove the contact who doesn't get email successfully.
                    val iterator = megaContacts.iterator()
                    while (iterator.hasNext()) {
                        val contact = iterator.next()
                        if (contact.email == null) {
                            iterator.remove()
                        }
                    }
                    processFinished(api, megaContacts)
                } else {
                    val nextContact = getCurrentContactIndex()
                    if (nextContact != null) {
                        getUserEmail(nextContact.id!!.toUserHandle(), api)
                    }
                }
            }
        ))
    }

    /**
     * Fill the matched MEGA contacts into two lists:
     * megaContactsWithEmail contains the contacts with email.
     * megaContacts contains the contacts without email, need to ask for email.
     *
     * @param contacts All the unique MEGA contacts, may be empty.
     */
    private fun fillContactsList(contacts: Map<String, MegaContact>) {
        for (megaContact in contacts.values) {
            if (megaContact.email != null) {
                megaContactsWithEmail.add(megaContact)
            } else {
                megaContacts.add(megaContact)
            }
        }
    }

    private fun resetAfterRequest() {
        megaContacts.clear()
        megaContactsWithEmail.clear()
        currentContactIndex = 0
    }

    /**
     * Parse server reponse to get matched and unique MEGA users.
     *
     * @see this.getRequestParameter
     *
     * @see MegaApiJava.getRegisteredContacts
     *
     *
     * @param map The submited data, Key is phone number or email of the local contact, Value is the local name.
     * Here is used to get local name.
     * @param table Server returned infomation, contains matched MEGA users.
     * @return MEGA users. Key is user handle in Base64, value is corresponding MegaContact object.
     */
    private fun parseMatchedContacts(
        map: MegaStringMap,
        table: MegaStringTable
    ): Map<String, MegaContact> {
        val temp: MutableMap<String, MegaContact> = HashMap()
        for (i in 0 until table.size()) {
            // Each MegaStringList is a matched MEGA user.
            val list = table[i]

            // User handle in Base64.
            val id = list[INDEX_ID]
            // Phone number or email.
            val data = list[INDEX_DATA]

            // Check if there's already a MegaContact with same id.
            val ex = temp[id]
            if (ex != null) {
                if (ex.email == null) {
                    ex.email = data
                }
                if (ex.normalizedPhoneNumber == null) {
                    ex.normalizedPhoneNumber = data
                }
            } else {
                val contact = MegaContact()
                contact.id = id
                if (TextUtil.isEmail(data)) {
                    contact.email = data
                } else {
                    contact.normalizedPhoneNumber = data
                }
                contact.handle = contact.id!!.toUserHandle()
                // The data(phone number or email) is the key.
                contact.localName = map[data]
                temp[id] = contact
            }
        }
        return temp
    }

    private fun processFinished(api: MegaApiJava, list: MutableList<MegaContact>) {
        // save to db
        dbH.clearMegaContacts()
        dbH.batchInsertMegaContacts(list)

        // filter out
        val filteredList = filterOut(api, list)
        //when request is successful, update the timestamp.
        updateLastSyncTimestamp()
        updater?.onFinish(filteredList)
    }

    private fun filterOut(api: MegaApiJava, list: MutableList<MegaContact>): MutableList<MegaContact> {
        val emails: MutableList<String?> = mutableListOf()
        for (megaContact in list) {
            emails.add(megaContact.email)
        }
        ContactsFilter.filterOutContacts(api, emails)
        ContactsFilter.filterOutPendingContacts(api, emails)
        ContactsFilter.filterOutMyself(api, emails)
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            if (!emails.contains(iterator.next().email)) {
                iterator.remove()
            }
        }
        Collections.sort(list) { o1: MegaContact, o2: MegaContact ->
            o1.localName!!.compareTo(
                o2.localName!!
            )
        }
        return list
    }

    private fun getCurrentContactIndex(): MegaContact? {
        if (megaContacts.size == 0) {
            return null
        }
        if (currentContactIndex >= megaContacts.size) {
            //get last contact.
            currentContactIndex = megaContacts.size - 1
        }
        return megaContacts[currentContactIndex]
    }

    fun getMegaContacts(api: MegaApiAndroid, period: Long, context: Context) {
        if (api.rootNode == null) {
            logDebug("haven't logged in, return")
            return
        }
        if (System.currentTimeMillis() - lastSyncTimestamp > period && !requestInProgress) {
            requestInProgress = true
            logDebug("getMegaContacts request from server")
            val requestParam = getRequestParameter(ContactsUtil.getLocalContactList(context))
            api.getRegisteredContacts(requestParam, OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    requestInProgress = false
                    resetAfterRequest()
                    if (error.errorCode == MegaError.API_OK) {
                        // The table contains all the mathced users.
                        val table = request.megaStringTable

                        // When there's no matched user, should be considered as successful
                        if (table.size() == 0) {
                            updateLastSyncTimestamp()
                        } else {
                            val map = request.megaStringMap
                            val contacts = parseMatchedContacts(map, table)
                            fillContactsList(contacts)
                            // Need to ask for email from server.
                            if (megaContacts.size > 0) {
                                val firstContact = getCurrentContactIndex()
                                if (firstContact != null) {
                                    getUserEmail(firstContact.handle, api)
                                }
                            } else {
                                // All the contacts are matched by email.
                                if (megaContactsWithEmail.size > 0) {
                                    processFinished(api, megaContactsWithEmail)
                                } else {
                                    logWarning("No mega contacts.")
                                    updater?.noContacts()
                                }
                            }
                        }
                    } else {
                        logWarning("Get registered contacts failed with error code: " + error.errorCode)
                        // API_ETOOMANY: Current account has requested mega contacts too many times and reached the limitation, no need to re-try.
                        // API_EPAYWALL: Need to call "updateLastSyncTimestamp()" to avoid fall in an infinite loop to dismiss the ODQ Paywall warning.
                        if (error.errorCode == MegaError.API_ETOOMANY || error.errorCode == MegaError.API_EPAYWALL) {
                            updateLastSyncTimestamp()
                        }
                        updater?.onException(error.errorCode, request.requestString)
                    }
                }
            ))
        } else {
            if (!requestInProgress) {
                logDebug("getMegaContacts load from database")
                val list = dbH.megaContacts.apply { filterOut(api, this) }
                updater?.onFinish(list)
            }
        }
    }

    private fun String.toUserHandle(): Long =
        MegaApiAndroid.base64ToUserHandle(this)

    private fun getRequestParameter(localContacts: List<LocalContact>): MegaStringMap? {
        val stringMap = MegaStringMap.createInstance() ?: return null
        for (contact in localContacts) {
            val name = contact.name
            val normalizedPhoneNumberSet = contact.normalizedPhoneNumberList
            val emailSet = contact.emailList
            for (phoneNumber in normalizedPhoneNumberSet) {
                stringMap[phoneNumber] = name
            }
            for (email in emailSet) {
                stringMap[email] = name
            }
        }
        logDebug("local contacts size is: " + stringMap.size())
        return stringMap
    }

    interface MegaContactUpdater {
        /**
         * Get registered contacts successfully.
         *
         * @param megaContacts Registerd mega contacts with all the info needed.
         */
        fun onFinish(megaContacts: List<MegaContact>?)

        /**
         * When mega request failed.
         *
         * @param errorCode     Error code.
         * @param requestString What request.
         * @see MegaError
         *
         * @see MegaRequest
         */
        fun onException(errorCode: Int, requestString: String?)

        /**
         * When get no registered mega contacts.
         */
        fun noContacts()
    }

    data class MegaContact constructor(
        var id: String? = null,
        var handle: Long = 0,
        var localName: String? = null,
        var email: String? = null,
        var normalizedPhoneNumber: String? = null
    )
}
