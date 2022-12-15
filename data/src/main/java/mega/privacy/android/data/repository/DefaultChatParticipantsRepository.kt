package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.constant.FileConstant
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.mapper.userPermission
import mega.privacy.android.data.mapper.userStatus
import mega.privacy.android.data.mapper.userVisibility
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import mega.privacy.android.domain.repository.ContactsRepository
import nz.mega.sdk.MegaChatRoom
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
    private val avatarRepository: AvatarRepository,
    private val contactsRepository: ContactsRepository,
    private val cacheFolderGateway: CacheFolderGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ChatParticipantsRepository {

    override suspend fun getAllChatParticipants(chatId: Long): List<ChatParticipant> =
        withContext(ioDispatcher) {
            val list: MutableList<ChatParticipant> = mutableListOf()
            megaChatApiGateway.getChatRoom(chatId)?.let { chatRoom ->
                val myEmail = getMyEmail()
                val handle = getMyUserHandle()
                val myParticipant = ChatParticipant(
                    handle = handle,
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

                    val handleP = chatRoom.getPeerHandle(i)
                    val participant = ChatParticipant(
                        handle = handleP,
                        data = ContactData(
                            fullName = chatRoom.getPeerFullname(i),
                            alias = null,
                            avatarUri = null),
                        email = chatRoom.getPeerEmail(i),
                        isMe = false,
                        privilege = userPermission[participantPrivilege]
                            ?: ChatRoomPermission.Unknown,
                        defaultAvatarColor = avatarRepository.getAvatarColor(handleP))

                    list.add(participant)
                }

            }
            return@withContext list
        }

    override suspend fun getStatus(participant: ChatParticipant): UserStatus =
        withContext(ioDispatcher) {
            if (participant.handle == getMyUserHandle()) {
                return@withContext userStatus[megaChatApiGateway.getOnlineStatus()]
                    ?: UserStatus.Invalid
            }

            getMegaUser(participant.handle)?.let { megaUser ->
                val visibility =
                    userVisibility[megaUser.visibility] ?: UserVisibility.Unknown
                if (visibility == UserVisibility.Visible) {
                    return@withContext userStatus[megaChatApiGateway.getUserOnlineStatus(participant.handle)]
                        ?: UserStatus.Invalid
                }
            }

            return@withContext UserStatus.Invalid
        }

    override suspend fun getAlias(participant: ChatParticipant): String? =
        runCatching { contactsRepository.getUserAlias(participant.handle) }.fold(
            onSuccess = { alias -> alias },
            onFailure = { null }
        )

    override suspend fun getAvatarUri(participant: ChatParticipant): String? =
        withContext(ioDispatcher) {
            if (participant.isMe) {
                val myAvatarFile = avatarRepository.getMyAvatarFile()
                myAvatarFile?.let {
                    if (it.exists() && it.length() > 0) {
                        return@withContext it.toString()
                    }
                }
            } else {
                runCatching { avatarRepository.getAvatarFile(participant.handle) }.fold(
                    onSuccess = { contactAvatarFile ->
                        contactAvatarFile?.let {
                            if (it.exists() && it.length() > 0) {
                                return@withContext it.toString()
                            }
                        }
                    },
                    onFailure = {
                        cacheFolderGateway.buildAvatarFile(participant.email + FileConstant.JPG_EXTENSION)
                            ?.let { contactAvatarFile ->
                                if (contactAvatarFile.exists() && contactAvatarFile.length() > 0) {
                                    return@withContext it.toString()
                                }
                            }

                    }
                )
            }

            return@withContext null
        }

    override suspend fun getPermissions(
        chatId: Long,
        participant: ChatParticipant,
    ): ChatRoomPermission = withContext(ioDispatcher) {
        megaChatApiGateway.getChatRoom(chatId)?.let { chatRoom ->
            val participantPrivilege = chatRoom.getPeerPrivilegeByHandle(participant.handle)
            return@withContext userPermission[participantPrivilege]
                ?: ChatRoomPermission.Unknown
        }

        return@withContext ChatRoomPermission.Unknown
    }

    override suspend fun areCredentialsVerified(participant: ChatParticipant): Boolean =
        withContext(ioDispatcher) {
            getMegaUser(participant.handle)?.let { megaUser ->
                val visibility =
                    userVisibility[megaUser.visibility] ?: UserVisibility.Unknown
                if (visibility == UserVisibility.Visible) {
                    return@withContext megaApiGateway.areCredentialsVerified(megaUser)
                }
            }

            return@withContext false
        }

    override suspend fun updateList(
        chatId: Long,
        currentList: List<ChatParticipant>,
    ): MutableList<ChatParticipant> = withContext(ioDispatcher) {
        val newList = getAllChatParticipants(chatId).toMutableList()
        newList.forEach { participantNewList ->
            val index = newList.indexOf(participantNewList)
            currentList.filter { it.handle == participantNewList.handle }.map {
                newList.set(index, it)
            }
        }
        return@withContext newList
    }

    suspend fun getMyUserHandle(): Long =
        withContext(ioDispatcher) {
            megaChatApiGateway.getMyUserHandle()
        }

    suspend fun getMyFullName(): String = withContext(ioDispatcher) {
        megaChatApiGateway.getMyFullname()
    }

    suspend fun getMyEmail(): String = withContext(ioDispatcher) {
        megaChatApiGateway.getMyEmail()
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
