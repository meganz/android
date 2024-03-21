package mega.privacy.android.data.gateway.contact

import android.content.Context
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.domain.entity.contacts.LocalContact
import timber.log.Timber
import javax.inject.Inject

/**
 * Implementation class of [ContactGateway]
 */
class ContactGatewayImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ContactGateway {

    override suspend fun getLocalContacts(): List<LocalContact> =
        buildList {
            val projection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
            )
            val cursor = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                null,
                null,
                null
            )

            cursor?.use {
                Timber.d("getting local contacts, has %d contacts", it.count)
                while (it.moveToNext()) {
                    val id = it.getLong(0)
                    val name = it.getString(1)
                    add(LocalContact(id, name))
                }
            }
        }

    override suspend fun getLocalContactNumbers(): List<LocalContact> {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
        )
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        val mapOfContactNumbers =
            mutableMapOf<Long, Pair<MutableList<String>, MutableList<String>>>()
        cursor?.use {
            Timber.d("getting local contact's phone numbers")
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val phoneNumber = it.getString(1)
                val normalizedNumber = it.getString(2).orEmpty()

                if (mapOfContactNumbers[id] == null) {
                    mapOfContactNumbers[id] = Pair(
                        mutableListOf(phoneNumber),
                        mutableListOf(normalizedNumber)
                    )
                } else {
                    mapOfContactNumbers[id]?.first?.add(phoneNumber)
                    mapOfContactNumbers[id]?.second?.add(normalizedNumber)
                }
            }
        }

        return mapOfContactNumbers.map {
            LocalContact(
                id = it.key,
                phoneNumbers = it.value.first,
                normalizedPhoneNumbers = it.value.second
            )
        }
    }

    override suspend fun getLocalContactEmailAddresses(): List<LocalContact> {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Email.CONTACT_ID,
            ContactsContract.CommonDataKinds.Email.ADDRESS
        )
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        val mapOfContactEmails = mutableMapOf<Long, MutableList<String>>()
        cursor?.use {
            Timber.d("getting local contact's email addresses")
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val email = it.getString(1)

                if (mapOfContactEmails[id] == null) {
                    mapOfContactEmails[id] = mutableListOf(email)
                } else {
                    mapOfContactEmails[id]?.add(email)
                }
            }
        }

        return mapOfContactEmails.map {
            LocalContact(
                id = it.key,
                emails = it.value
            )
        }
    }
}
