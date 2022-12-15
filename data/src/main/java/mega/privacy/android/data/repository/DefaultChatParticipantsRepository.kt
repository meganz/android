package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.mapper.userPermission
import mega.privacy.android.data.mapper.userStatus
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.usecase.RequestLastGreen
import nz.mega.sdk.MegaChatRoom
import java.io.File
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
    private val avatarRepository: AvatarRepository,
    private val contactsRepository: ContactsRepository,
    private val requestLastGreen: RequestLastGreen,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ChatParticipantsRepository {

    override suspend fun getAllChatParticipants(chatId: Long): List<ChatParticipant> =
        withContext(ioDispatcher) {
            val list: MutableList<ChatParticipant> = mutableListOf()
            megaChatApiGateway.getChatRoom(chatId)?.let { chatRoom ->
                val myEmail = getMyEmail()
                val myParticipant = ChatParticipant(
                    handle = getMyUserHandle(),
                    data = ContactData(
                        fullName = getMyFullName().ifEmpty { myEmail },
                        alias = null,
                        avatarUri = null),
                    email = myEmail,
                    isMe = true,
                    defaultAvatarColor = avatarRepository.getMyAvatarColor(),
                    privilege = userPermission[chatRoom.ownPrivilege]
                        ?: ChatRoomPermission.Unknown)

                list.add(myParticipant)

                val participantsCount = chatRoom.peerCount
                for (i in 0 until participantsCount) {
                    val participantPrivilege = chatRoom.getPeerPrivilege(i)
                    if (participantPrivilege == MegaChatRoom.PRIV_RM) {
                        continue
                    }

                    val handle = chatRoom.getPeerHandle(i)
                    val alias = megaChatApiGateway.getUserAliasFromCache(handle)
                    val participant = ChatParticipant(
                        handle = handle,
                        data = ContactData(
                            fullName = chatRoom.getPeerFullname(i),
                            alias = alias,
                            avatarUri = null),
                        email = chatRoom.getPeerEmail(i),
                        isMe = false,
                        privilege = userPermission[participantPrivilege]
                            ?: ChatRoomPermission.Unknown,
                        defaultAvatarColor = avatarRepository.getAvatarColor(handle))

                    list.add(participant)
                }

            }
            return@withContext list
        }

    override suspend fun getStatus(participant: ChatParticipant): UserStatus =
        withContext(ioDispatcher) {
            if (participant.handle == getMyUserHandle()) {
                val status = userStatus[megaChatApiGateway.getOnlineStatus()]
                    ?: UserStatus.Invalid

                if (status != UserStatus.Online) {
                    requestLastGreen(participant.handle)
                }
                return@withContext status
            }

            megaApiGateway.getContact(participant.email)?.let {
                val status = userStatus[megaChatApiGateway.getUserOnlineStatus(it.handle)]
                    ?: UserStatus.Invalid
                if (status != UserStatus.Online) {
                    requestLastGreen(it.handle)
                }

                return@withContext status
            }

            return@withContext UserStatus.Invalid
        }

    override suspend fun getAlias(participant: ChatParticipant): String? =
        runCatching { contactsRepository.getUserAlias(participant.handle) }.fold(
            onSuccess = { alias -> alias },
            onFailure = { null }
        )

    override suspend fun getAvatarColor(participant: ChatParticipant): Int {
        if (participant.isMe) {
            return avatarRepository.getMyAvatarColor()
        }

        return avatarRepository.getAvatarColor(participant.handle)
    }

    override suspend fun getAvatarUri(participant: ChatParticipant): File? {
        if (participant.isMe) {
            avatarRepository.getMyAvatarFile()?.let {
                if (it.exists() && it.length() > 0) {
                    return it
                }
            }

            return null
        } else {
            runCatching { avatarRepository.getAvatarFile(userHandle = participant.handle) }.fold(
                onSuccess = { file ->
                    file?.let {
                        if (it.exists() && it.length() > 0) {
                            return it
                        }
                    }
                    return null
                },
                onFailure = {
                    return null
                }
            )
        }
    }

    override suspend fun getPermissions(
        chatId: Long,
        participant: ChatParticipant,
    ): ChatRoomPermission = withContext(ioDispatcher) {

        megaChatApiGateway.getChatRoom(chatId)?.let { chatRoom ->
            val participantPrivilege =
                if (participant.isMe) chatRoom.ownPrivilege else chatRoom.getPeerPrivilegeByHandle(
                    participant.handle)
            return@withContext userPermission[participantPrivilege]
                ?: ChatRoomPermission.Unknown
        }

        return@withContext ChatRoomPermission.Unknown
    }

    override suspend fun areCredentialsVerified(participant: ChatParticipant): Boolean =
        runCatching { contactsRepository.areCredentialsVerified(participant.email) }.fold(
            onSuccess = { cred -> cred },
            onFailure = { false }
        )

    suspend fun getMyUserHandle(): Long =
        withContext(ioDispatcher) {
            megaChatApiGateway.getMyUserHandle()
        }

    suspend fun getMyFullName(): String = withContext(ioDispatcher) {
        return@withContext megaChatApiGateway.getMyFullname()
    }

    suspend fun getMyEmail(): String = withContext(ioDispatcher) {
        megaChatApiGateway.getMyEmail()
    }
}
