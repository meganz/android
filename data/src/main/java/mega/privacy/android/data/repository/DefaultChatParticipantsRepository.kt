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
import mega.privacy.android.domain.exception.ChatRoomDoesNotExistException
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
 * @property megaChatApiGateway     [MegaChatApiGateway]
 * @property megaApiGateway         [MegaApiGateway]
 * @property avatarRepository       [AvatarRepository]
 * @property contactsRepository     [ContactsRepository]
 * @property requestLastGreen       [RequestLastGreen]
 * @property ioDispatcher           [CoroutineDispatcher]
 */
internal class DefaultChatParticipantsRepository @Inject constructor(
    private val megaChatApiGateway: MegaChatApiGateway,
    private val megaApiGateway: MegaApiGateway,
    private val avatarRepository: AvatarRepository,
    private val contactsRepository: ContactsRepository,
    private val requestLastGreen: RequestLastGreen,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ChatParticipantsRepository {

    override suspend fun getAllChatParticipants(chatId: Long): List<ChatParticipant> {
        megaChatApiGateway.getChatRoom(chatId)?.let { chatRoom ->
            val list: MutableList<ChatParticipant> = mutableListOf()

            val myEmail = megaChatApiGateway.getMyEmail()
            val myName = megaChatApiGateway.getMyFullname()
            val myParticipant = ChatParticipant(
                handle = megaChatApiGateway.getMyUserHandle(),
                data = ContactData(
                    fullName = myName.ifEmpty { myEmail },
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
                        fullName = contactsRepository.getUserFullName(handle),
                        alias = alias,
                        avatarUri = null),
                    email = contactsRepository.getUserEmail(handle),
                    isMe = false,
                    privilege = userPermission[participantPrivilege]
                        ?: ChatRoomPermission.Unknown,
                    defaultAvatarColor = avatarRepository.getAvatarColor(handle))

                list.add(participant)
            }
            return list
        }

        return mutableListOf()
    }

    override suspend fun getChatParticipantsHandles(chatId: Long): List<Long> =
        withContext(ioDispatcher) {
            val chatRoom = megaChatApiGateway.getChatRoom(chatId)
                ?: throw ChatRoomDoesNotExistException()

            if (!chatRoom.isGroup || chatRoom.peerCount == 0L) {
                emptyList()
            } else {
                mutableListOf<Long>().apply {
                    for (index in 0 until chatRoom.peerCount) {
                        add(chatRoom.getPeerHandle(index))
                    }
                }
            }
        }

    override suspend fun getStatus(participant: ChatParticipant): UserStatus {
        if (participant.isMe) {
            val status = userStatus[megaChatApiGateway.getOnlineStatus()]
                ?: UserStatus.Invalid

            if (status != UserStatus.Online) {
                requestLastGreen(participant.handle)
            }
            return status
        }

        megaApiGateway.getContact(participant.email)?.let {
            val status = userStatus[megaChatApiGateway.getUserOnlineStatus(it.handle)]
                ?: UserStatus.Invalid
            if (status != UserStatus.Online) {
                requestLastGreen(it.handle)
            }

            return status
        }

        return UserStatus.Invalid
    }

    override suspend fun getAlias(participant: ChatParticipant): String? =
        runCatching { contactsRepository.getUserAlias(participant.handle) }.fold(
            onSuccess = { alias -> alias },
            onFailure = { null }
        )

    override suspend fun getAvatarColor(participant: ChatParticipant): Int =
        if (participant.isMe) avatarRepository.getMyAvatarColor() else avatarRepository.getAvatarColor(
            participant.handle)

    override suspend fun getAvatarUri(participant: ChatParticipant): File? {
        if (participant.isMe) {
            avatarRepository.getMyAvatarFile()?.let { file ->
                if (file.exists() && file.length() > 0) {
                    return file
                }
            }

            return null
        } else {
            runCatching { avatarRepository.getAvatarFile(userHandle = participant.handle) }.fold(
                onSuccess = { file ->
                    file?.let {
                        if (it.exists() && it.length() > 0) {
                            return file
                        }
                    }
                    return null
                },
                onFailure = { return null }
            )
        }
    }

    override suspend fun getPermissions(
        chatId: Long,
        participant: ChatParticipant,
    ): ChatRoomPermission {
        megaChatApiGateway.getChatRoom(chatId)?.let { chatRoom ->
            val participantPrivilege =
                if (participant.isMe) chatRoom.ownPrivilege else chatRoom.getPeerPrivilegeByHandle(
                    participant.handle)
            return userPermission[participantPrivilege]
                ?: ChatRoomPermission.Unknown
        }

        return ChatRoomPermission.Unknown
    }

    override suspend fun areCredentialsVerified(participant: ChatParticipant): Boolean =
        runCatching { contactsRepository.areCredentialsVerified(participant.email) }.fold(
            onSuccess = { cred -> cred },
            onFailure = { false }
        )
}
