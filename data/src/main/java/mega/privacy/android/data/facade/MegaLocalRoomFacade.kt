package mega.privacy.android.data.facade

import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.database.dao.ContactDao
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.mapper.contact.ContactEntityMapper
import mega.privacy.android.data.mapper.contact.ContactModelMapper
import mega.privacy.android.domain.entity.Contact
import javax.inject.Inject

internal class MegaLocalRoomFacade @Inject constructor(
    private val contactDao: ContactDao,
    private val contactEntityMapper: ContactEntityMapper,
    private val contactModelMapper: ContactModelMapper,
    private val encryptData: EncryptData,
) : MegaLocalRoomGateway {
    override suspend fun saveContact(contact: Contact) {
        contactDao.insertOrUpdate(contactEntityMapper(contact))
    }

    override suspend fun setContactName(firstName: String?, mail: String?) {
        if (mail.isNullOrBlank()) return
        contactDao.getByEmail(encryptData(mail))?.let { entity ->
            contactDao.insertOrUpdate(entity.copy(firstName = encryptData(firstName)))
        }
    }

    override suspend fun setContactLastName(lastName: String?, mail: String?) {
        if (mail.isNullOrBlank()) return
        contactDao.getByEmail(encryptData(mail))?.let { entity ->
            contactDao.insertOrUpdate(entity.copy(lastName = encryptData(lastName)))
        }
    }

    override suspend fun setContactMail(handle: Long, mail: String?) {
        contactDao.getByHandle(encryptData(handle.toString()))?.let { entity ->
            contactDao.insertOrUpdate(entity.copy(mail = encryptData(mail)))
        }
    }

    override suspend fun setContactFistName(handle: Long, firstName: String?) {
        contactDao.getByHandle(encryptData(handle.toString()))?.let { entity ->
            contactDao.insertOrUpdate(entity.copy(firstName = encryptData(firstName)))
        }
    }

    override suspend fun setContactLastName(handle: Long, lastName: String?) {
        contactDao.getByHandle(encryptData(handle.toString()))?.let { entity ->
            contactDao.insertOrUpdate(entity.copy(lastName = encryptData(lastName)))
        }
    }

    override suspend fun setContactNickname(handle: Long, nickname: String?) {
        contactDao.getByHandle(encryptData(handle.toString()))?.let { entity ->
            contactDao.insertOrUpdate(entity.copy(nickName = encryptData(nickname)))
        }
    }

    override suspend fun findContactByHandle(handle: Long): Contact? =
        contactDao.getByHandle(encryptData(handle.toString()))?.let { contactModelMapper(it) }

    override suspend fun findContactByEmail(mail: String?): Contact? =
        contactDao.getByEmail(encryptData(mail))?.let { contactModelMapper(it) }

    override suspend fun clearContacts() = contactDao.deleteAll()

    override suspend fun getContactCount() = contactDao.getCount()
}