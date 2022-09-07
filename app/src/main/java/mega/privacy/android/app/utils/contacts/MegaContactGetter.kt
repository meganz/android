package mega.privacy.android.app.utils.contacts

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.di.CoroutineScopesModule
import mega.privacy.android.app.di.CoroutinesDispatchersModule
import mega.privacy.android.app.di.getDbHandler
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.contacts.ContactsUtil.LocalContact
import nz.mega.sdk.*
import timber.log.Timber
import java.util.*

class MegaContactGetter(val context: Context) {

    companion object {
        private const val INDEX_ID = 1
        private const val INDEX_DATA = 0
        const val LAST_SYNC_TIMESTAMP_FILE = "last_sync_timestamp"
        const val LAST_SYNC_TIMESTAMP_KEY = "last_sync_mega_contacts_timestamp"
    }

    private val dbH = getDbHandler()
    val preferences: SharedPreferences by lazy {
        context.getSharedPreferences(LAST_SYNC_TIMESTAMP_FILE, Context.MODE_PRIVATE)
    }
    private var updater: MegaContactUpdater? = null
    private val megaContacts = mutableListOf<MegaContact>()
    private val megaContactsWithEmail = mutableListOf<MegaContact>()
    private var currentContactIndex = 0
    private var requestInProgress = false
    private val coroutineScope = CoroutineScopesModule
        .provideCoroutineScope(CoroutinesDispatchersModule.providesIoDispatcher())

    private suspend fun getLastSyncTimeStamp(): Long =
        withContext(coroutineScope.coroutineContext) {
            return@withContext preferences.getLong(LAST_SYNC_TIMESTAMP_KEY, 0)
        }

    fun setMegaContactUpdater(updater: MegaContactUpdater?) {
        this.updater = updater
    }

    suspend fun clearLastSyncTimeStamp() = withContext(coroutineScope.coroutineContext) {
        preferences.edit().clear().apply()
    }

    private suspend fun updateLastSyncTimestamp() = withContext(coroutineScope.coroutineContext) {
        preferences.edit().putLong(LAST_SYNC_TIMESTAMP_KEY, System.currentTimeMillis()).apply()
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
                        Timber.w("Contact's email is empty!")
                    }
                } else {
                    Timber.e("Get contact's email faild with error code: ${error.errorCode}")
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
        table: MegaStringTable,
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
        val filteredList = list.filterOut(api)
        //when request is successful, update the timestamp.
        coroutineScope.launch {
            updateLastSyncTimestamp()
        }
        updater?.onFinish(filteredList)
    }

    private fun MutableList<MegaContact>.filterOut(api: MegaApiJava): MutableList<MegaContact> {
        val emails = this.map { it.email }
        ContactsFilter.filterOutContacts(api, emails)
        ContactsFilter.filterOutPendingContacts(api, emails)
        ContactsFilter.filterOutMyself(api, emails)

        val iterator = iterator()
        while (iterator.hasNext()) {
            if (!emails.contains(iterator.next().email)) {
                iterator.remove()
            }
        }

        sortedBy { it.localName ?: "" }

        return this
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
            Timber.d("haven't logged in, return")
            return
        }
        coroutineScope.launch {
            if (System.currentTimeMillis() - getLastSyncTimeStamp() > period && !requestInProgress) {
                requestInProgress = true
                Timber.d("getMegaContacts request from server")
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
                                coroutineScope.launch {
                                    updateLastSyncTimestamp()
                                }
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
                                        Timber.w("No mega contacts.")
                                        updater?.noContacts()
                                    }
                                }
                            }
                        } else {
                            Timber.e("Get registered contacts failed with error code: ${error.errorCode}")
                            // API_ETOOMANY: Current account has requested mega contacts too many times and reached the limitation, no need to re-try.
                            // API_EPAYWALL: Need to call "updateLastSyncTimestamp()" to avoid fall in an infinite loop to dismiss the ODQ Paywall warning.
                            if (error.errorCode == MegaError.API_ETOOMANY || error.errorCode == MegaError.API_EPAYWALL) {
                                coroutineScope.launch {
                                    updateLastSyncTimestamp()
                                }
                            }
                            updater?.onException(error.errorCode, request.requestString)
                        }
                    }
                ))
            } else {
                if (!requestInProgress) {
                    Timber.d("getMegaContacts load from database")
                    val list = dbH.megaContacts.apply { this.filterOut(api) }
                    updater?.onFinish(list)
                }
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
        Timber.d("local contacts size is: ${stringMap.size()}")
        return stringMap
    }

    interface MegaContactUpdater {
        /**
         * Get registered contacts successfully.
         *
         * @param megaContacts Registerd mega contacts with all the info needed.
         */
        fun onFinish(megaContacts: MutableList<MegaContact>?)

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
        var normalizedPhoneNumber: String? = null,
    )
}
