package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getChatRequestListener
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.data.mapper.chat.ChatPermissionsMapper
import mega.privacy.android.data.mapper.chat.OnlineStatusMapper.Companion.userStatus
import mega.privacy.android.data.mapper.chat.UserStatusToIntMapper
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
    private val chatPermissionsMapper: ChatPermissionsMapper,
    private val megaHandleListMapper: MegaHandleListMapper,
    private val userStatusToIntMapper: UserStatusToIntMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ChatParticipantsRepository {

    override suspend fun getAllChatParticipants(
        chatId: Long,
        preloadUserAttributes: Boolean,
    ): List<ChatParticipant> =
        withContext(ioDispatcher) {
            megaChatApiGateway.getChatRoom(chatId)?.let { chatRoom ->
                val participants = mutableListOf<ChatParticipant>()
                val peerHandles = getChatParticipantsHandles(chatId)

                if (preloadUserAttributes) {
                    peerHandles.map {
                        async { loadUserAttributes(chatId, listOf(it)) }
                    }.awaitAll() // Retrieve User Attributes in parallel
                }

                megaChatApiGateway.getMyEmail()?.let { myEmail ->
                    val myName = megaChatApiGateway.getMyFullname()
                    participants.add(
                        ChatParticipant(
                            handle = megaChatApiGateway.getMyUserHandle(),
                            data = ContactData(
                                fullName = myName?.ifEmpty { myEmail },
                                alias = null,
                                avatarUri = null
                            ),
                            email = myEmail,
                            isMe = true,
                            defaultAvatarColor = avatarRepository.getMyAvatarColor(),
                            privilege = chatPermissionsMapper(chatRoom.ownPrivilege)
                        )
                    )
                }

                peerHandles.forEach { handle ->
                    val participantPrivilege = chatRoom.getPeerPrivilegeByHandle(handle)
                    if (participantPrivilege != MegaChatRoom.PRIV_RM) {
                        val alias = async { megaChatApiGateway.getUserAliasFromCache(handle) }
                        val fullName = async { contactsRepository.getUserFullName(handle) }
                        val email = async { contactsRepository.getUserEmail(handle) }
                        val privilege = async { chatPermissionsMapper(participantPrivilege) }
                        val avatarColor = async { avatarRepository.getAvatarColor(handle) }

                        participants.add(
                            ChatParticipant(
                                handle = handle,
                                data = ContactData(
                                    fullName = fullName.await(),
                                    alias = alias.await(),
                                    avatarUri = null
                                ),
                                email = email.await(),
                                isMe = false,
                                privilege = privilege.await(),
                                defaultAvatarColor = avatarColor.await()
                            )
                        )
                    }
                }

                return@withContext participants
            } ?: emptyList()
        }

    override suspend fun getChatParticipantsHandles(chatId: Long, limit: Int): List<Long> =
        withContext(ioDispatcher) {
            val chatRoom = megaChatApiGateway.getChatRoom(chatId)
                ?: throw ChatRoomDoesNotExistException()

            val peerHandles = mutableListOf<Long>()
            if (chatRoom.isGroup && chatRoom.peerCount > 0) {
                for (index in 0 until chatRoom.peerCount) {
                    val peerHandle = chatRoom.getPeerHandle(index)
                    if (peerHandle != megaChatApiGateway.getChatInvalidHandle()) {
                        peerHandles.add(peerHandle)

                        if (limit != -1 && limit == peerHandles.size) break
                    }
                }
            }
            return@withContext peerHandles
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

    override suspend fun getCurrentStatus(): UserStatus = withContext(ioDispatcher) {
        runCatching { megaChatApiGateway.getOnlineStatus().let { userStatus[it] } }.getOrNull()
            ?: UserStatus.Invalid
    }

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

    override suspend fun setOnlineStatus(status: UserStatus) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaChatRequestListenerInterface(
                onRequestFinish = { _, error ->
                    when (error.errorCode) {
                        MegaChatError.ERROR_OK -> {
                            continuation.resumeWith(Result.success(Unit))
                        }

                        MegaChatError.ERROR_ARGS -> {
                            // special case, user select the same status, we consider it is success case
                            continuation.resumeWith(Result.success(Unit))
                        }

                        else -> {
                            continuation.failWithError(error, "setOnlineStatus")
                        }
                    }
                }
            )

            megaChatApiGateway.setOnlineStatus(userStatusToIntMapper(status), listener)

            continuation.invokeOnCancellation {
                megaChatApiGateway.removeRequestListener(listener)
            }
        }
    }
}
