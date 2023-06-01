package mega.privacy.android.data.facade

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.database.dao.CompletedTransferDao
import mega.privacy.android.data.database.dao.ContactDao
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.mapper.contact.ContactEntityMapper
import mega.privacy.android.data.mapper.contact.ContactModelMapper
import mega.privacy.android.data.mapper.transfer.completed.CompletedTransferModelMapper
import mega.privacy.android.domain.entity.Contact
import javax.inject.Inject

internal class MegaLocalRoomFacade @Inject constructor(
    private val contactDao: ContactDao,
    private val contactEntityMapper: ContactEntityMapper,
    private val contactModelMapper: ContactModelMapper,
    private val completedTransferDao: CompletedTransferDao,
    private val completedTransferModelMapper: CompletedTransferModelMapper,
    private val encryptData: EncryptData,
) : MegaLocalRoomGateway {
    override suspend fun insertContact(contact: Contact) {
        contactDao.insertOrUpdateContact(contactEntityMapper(contact))
    }

    override suspend fun updateContactNameByEmail(firstName: String?, email: String?) {
        if (email.isNullOrBlank()) return
        contactDao.getContactByEmail(encryptData(email))?.let { entity ->
            contactDao.insertOrUpdateContact(entity.copy(firstName = encryptData(firstName)))
        }
    }

    override suspend fun updateContactLastNameByEmail(lastName: String?, email: String?) {
        if (email.isNullOrBlank()) return
        contactDao.getContactByEmail(encryptData(email))?.let { entity ->
            contactDao.insertOrUpdateContact(entity.copy(lastName = encryptData(lastName)))
        }
    }

    override suspend fun updateContactMailByHandle(handle: Long, email: String?) {
        contactDao.getContactByHandle(encryptData(handle.toString()))?.let { entity ->
            contactDao.insertOrUpdateContact(entity.copy(mail = encryptData(email)))
        }
    }

    override suspend fun updateContactFistNameByHandle(handle: Long, firstName: String?) {
        contactDao.getContactByHandle(encryptData(handle.toString()))?.let { entity ->
            contactDao.insertOrUpdateContact(entity.copy(firstName = encryptData(firstName)))
        }
    }

    override suspend fun updateContactLastNameByHandle(handle: Long, lastName: String?) {
        contactDao.getContactByHandle(encryptData(handle.toString()))?.let { entity ->
            contactDao.insertOrUpdateContact(entity.copy(lastName = encryptData(lastName)))
        }
    }

    override suspend fun updateContactNicknameByHandle(handle: Long, nickname: String?) {
        contactDao.getContactByHandle(encryptData(handle.toString()))?.let { entity ->
            contactDao.insertOrUpdateContact(entity.copy(nickName = encryptData(nickname)))
        }
    }

    override suspend fun getContactByHandle(handle: Long): Contact? =
        contactDao.getContactByHandle(encryptData(handle.toString()))?.let { contactModelMapper(it) }

    override suspend fun getContactByEmail(email: String?): Contact? =
        contactDao.getContactByEmail(encryptData(email))?.let { contactModelMapper(it) }

    override suspend fun deleteAllContacts() = contactDao.deleteAllContact()

    override suspend fun getContactCount() = contactDao.getContactCount()

    override suspend fun getAllContacts(): List<Contact> {
        val entities = contactDao.getAllContact().first()
        return entities.map { contactModelMapper(it) }
    }

    override suspend fun getAllCompletedTransfers(size: Int?) =
        completedTransferDao.getAllCompletedTransfers()
            .map { list ->
                list.map { completedTransferModelMapper(it) }
                    .toMutableList()
                    .apply { sortWith(compareByDescending { it.timestamp }) }
                    .let { if (size != null) it.take(size) else it }
            }
}
