package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getChatRequestListener
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.data.mapper.chat.ChatPermissionsMapper
import mega.privacy.android.data.mapper.chat.OnlineStatusMapper.Companion.userStatus
import mega.privacy.android.data.mapper.handles.MegaHandleListMapper
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.exception.ChatRoomDoesNotExistException
import mega.privacy.android.domain.exception.NullMegaHandleListException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.usecase.RequestLastGreen
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaHandleList
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
    private val chatPermissionsMapper: ChatPermissionsMapper,
    private val megaHandleListMapper: MegaHandleListMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ChatParticipantsRepository {

    override suspend fun getAllChatParticipants(chatId: Long): List<ChatParticipant> {
        megaChatApiGateway.getChatRoom(chatId)?.let { chatRoom ->
            val list: MutableList<ChatParticipant> = mutableListOf()
            val peerHandles = getChatParticipantsHandles(chatId)

            megaChatApiGateway.getMyEmail()?.let {
                val myName = megaChatApiGateway.getMyFullname()
                val myParticipant = ChatParticipant(
                    handle = megaChatApiGateway.getMyUserHandle(), data = ContactData(
                        fullName = myName?.ifEmpty { it }, alias = null, avatarUri = null
                    ),
                    email = it,
                    isMe = true,
                    defaultAvatarColor = avatarRepository.getMyAvatarColor(),
                    privilege = chatPermissionsMapper(chatRoom.ownPrivilege)
                )
                list.add(myParticipant)
            }

            peerHandles.forEach { handle ->
                val participantPrivilege = chatRoom.getPeerPrivilegeByHandle(handle)
                if (participantPrivilege != MegaChatRoom.PRIV_RM) {
                    val alias = megaChatApiGateway.getUserAliasFromCache(handle)
                    val participant = ChatParticipant(
                        handle = handle,
                        data = ContactData(
                            fullName = contactsRepository.getUserFullName(handle),
                            alias = alias,
                            avatarUri = null
                        ),
                        email = contactsRepository.getUserEmail(handle),
                        isMe = false,
                        privilege = chatPermissionsMapper(participantPrivilege),
                        defaultAvatarColor = avatarRepository.getAvatarColor(handle)
                    )

                    list.add(participant)
                }
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
                return@withContext emptyList()
            }

            val megaHandleList = MegaHandleList.createInstance()
            val peerList = mutableListOf<Long>().apply {
                for (index in 0 until chatRoom.peerCount) {
                    val peerHandle = chatRoom.getPeerHandle(index)
                    add(peerHandle)
                    megaHandleList.addMegaHandle(peerHandle)
                }
            }

            suspendCoroutine { continuation ->
                megaChatApiGateway.loadUserAttributes(
                    chatId,
                    megaHandleList,
                    OptionalMegaChatRequestListenerInterface(
                        onRequestFinish = { _: MegaChatRequest, error: MegaChatError ->
                            if (error.errorCode == MegaError.API_OK) {
                                continuation.resume(peerList)
                            } else {
                                continuation.failWithError(error, "getChatParticipantsHandles")
                            }
                        }
                    ))
            }
        }

    override suspend fun getStatus(participant: ChatParticipant): UserStatus {
        if (participant.isMe) {
            val status = getCurrentStatus()
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

    override suspend fun getCurrentStatus(): UserStatus =
        runCatching { megaChatApiGateway.getOnlineStatus().let { userStatus[it] } }.getOrNull()
            ?: UserStatus.Invalid

    override suspend fun getAlias(participant: ChatParticipant): String? =
        runCatching { contactsRepository.getUserAlias(participant.handle) }.fold(
            onSuccess = { alias -> alias },
            onFailure = { null }
        )

    override suspend fun getAvatarColor(participant: ChatParticipant): Int =
        if (participant.isMe) avatarRepository.getMyAvatarColor() else avatarRepository.getAvatarColor(
            participant.handle
        )

    override suspend fun getAvatarUri(participant: ChatParticipant, skipCache: Boolean): File? =
        runCatching {
            if (participant.isMe) {
                avatarRepository.getMyAvatarFile()
            } else {
                if (participant.email.isNotBlank()) {
                    avatarRepository.getAvatarFile(
                        userEmail = participant.email,
                        skipCache = skipCache
                    )
                } else {
                    avatarRepository.getAvatarFile(
                        userHandle = participant.handle,
                        skipCache = skipCache
                    )
                }
            }
        }.getOrNull()?.takeIf { it.exists() && it.length() > 0 }

    override suspend fun getPermissions(
        chatId: Long,
        participant: ChatParticipant,
    ): ChatRoomPermission {
        megaChatApiGateway.getChatRoom(chatId)?.let { chatRoom ->
            val participantPrivilege =
                if (participant.isMe) chatRoom.ownPrivilege else chatRoom.getPeerPrivilegeByHandle(
                    participant.handle
                )
            return chatPermissionsMapper(participantPrivilege)
        }

        return ChatRoomPermission.Unknown
    }

    override suspend fun loadUserAttributes(chatId: Long, usersHandles: List<Long>) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                megaHandleListMapper(usersHandles)?.let { megaHandleList ->
                    val listener = continuation.getChatRequestListener("loadUserAttributes") {}

                    megaChatApiGateway.loadUserAttributes(chatId, megaHandleList, listener)

                    continuation.invokeOnCancellation {
                        megaChatApiGateway.removeRequestListener(listener)
                    }
                } ?: continuation.resumeWith(Result.failure(NullMegaHandleListException()))
            }
        }

    override suspend fun getUserFullNameFromCache(userHandle: Long): String? =
        withContext(ioDispatcher) {
            megaChatApiGateway.getUserFullNameFromCache(userHandle)
        }

    override suspend fun getUserEmailFromCache(userHandle: Long): String? =
        withContext(ioDispatcher) {
            megaChatApiGateway.getUserEmailFromCache(userHandle)
        }

    override suspend fun areCredentialsVerified(participant: ChatParticipant): Boolean =
        runCatching { contactsRepository.areCredentialsVerified(participant.email) }.fold(
            onSuccess = { cred -> cred },
            onFailure = { false }
        )
}
