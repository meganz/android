package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.mapper.userStatus
import mega.privacy.android.data.mapper.userVisibility
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import mega.privacy.android.domain.repository.ContactsRepository
import nz.mega.sdk.MegaUser
import javax.inject.Inject

/**
 * Default implementation of [ChatParticipantsRepository]
 *
 * @property megaChatApiGateway [MegaChatApiGateway]
 * @property ioDispatcher CoroutineDispatcher
 */
internal class DefaultChatParticipantsRepository @Inject constructor(
    private val megaChatApiGateway: MegaChatApiGateway,
    private val megaApiGateway: MegaApiGateway,
    private val contactsRepository: ContactsRepository,
    private val cacheFolderGateway: CacheFolderGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ChatParticipantsRepository {

    override suspend fun getAllChatParticipants(chatId: Long): List<ChatParticipant> =
        withContext(ioDispatcher) {
            val list: MutableList<ChatParticipant> = mutableListOf()
            return@withContext list
        }

    override suspend fun getMyDefaultAvatarColor(): String =
        withContext(ioDispatcher) {
            val myHandle = getMyUserHandle()
            return@withContext megaApiGateway.getUserAvatarColor(myHandle)
        }

    override suspend fun getMyStatus(): UserStatus =
        withContext(ioDispatcher) {
            val status: Int = megaChatApiGateway.getOnlineStatus()
            return@withContext userStatus[status] ?: UserStatus.Invalid
        }

    override suspend fun getMyUserHandle(): Long =
        withContext(ioDispatcher) {
            megaChatApiGateway.getMyUserHandle()
        }

    override suspend fun getMyFullName(): String = withContext(ioDispatcher) {
        megaChatApiGateway.getMyFullname()
    }

    override suspend fun getMyEmail(): String = withContext(ioDispatcher) {
        megaChatApiGateway.getMyEmail()
    }

    private suspend fun getAlias(handle: Long): String? =
        runCatching { contactsRepository.getUserAlias(handle) }.fold(
            onSuccess = { alias -> alias },
            onFailure = { null }
        )

    private suspend fun getDefaultAvatarColor(megaUser: MegaUser): String =
        withContext(ioDispatcher) {
            return@withContext megaApiGateway.getUserAvatarColor(megaUser)
        }

    private suspend fun areCredentialsVerified(megaUser: MegaUser): Boolean =
        withContext(ioDispatcher) {
            return@withContext megaApiGateway.areCredentialsVerified(megaUser)
        }

    private suspend fun getParticipantStatus(
        participantHandle: Long,
        megaUser: MegaUser?,
    ): UserStatus =
        withContext(ioDispatcher) {
            megaUser?.let {
                val visibility =
                    userVisibility[megaUser.visibility] ?: UserVisibility.Unknown
                val isContact = visibility == UserVisibility.Visible
                if (isContact) {
                    val status: Int = megaChatApiGateway.getUserOnlineStatus(participantHandle)
                    return@withContext userStatus[status] ?: UserStatus.Invalid
                }
            }

            UserStatus.Invalid
        }

    private suspend fun getMegaUser(participantHandle: Long): MegaUser? =
        withContext(ioDispatcher) {
            if (participantHandle != megaApiGateway.getInvalidHandle()) {
                megaApiGateway.userHandleToBase64(participantHandle).let { handleInBase64 ->
                    if (handleInBase64.isNotEmpty()) {
                        return@withContext megaApiGateway.getContact(handleInBase64)
                    }
                }
            }

            return@withContext null
        }
}
