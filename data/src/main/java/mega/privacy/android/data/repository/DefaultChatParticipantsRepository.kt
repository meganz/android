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
import mega.privacy.android.data.mapper.chat.UserStatusToIntMapper
import mega.privacy.android.data.mapper.contact.UserChatStatusMapper
import mega.privacy.android.data.mapper.handles.MegaHandleListMapper
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.exception.ChatRoomDoesNotExistException
import mega.privacy.android.domain.exception.NullMegaHandleListException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.RequestLastGreen
import mega.privacy.android.domain.usecase.account.GetUserAliasUseCase
import mega.privacy.android.domain.usecase.avatar.GetAvatarFileFromEmailUseCase
import mega.privacy.android.domain.usecase.avatar.GetAvatarFileFromHandleUseCase
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.domain.usecase.avatar.GetUserAvatarColorUseCase
import mega.privacy.android.domain.usecase.chat.GetUserPrivilegeUseCase
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import mega.privacy.android.domain.usecase.contact.GetContactEmail
import mega.privacy.android.domain.usecase.contact.GetContactFullNameUseCase
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRoom
import java.io.File
import javax.inject.Inject

/**
 * Default implementation of [ChatParticipantsRepository]
 *
 * @property megaChatApiGateway             [MegaChatApiGateway]
 * @property megaApiGateway                 [MegaApiGateway]
 * @property getMyAvatarColorUseCase        [GetMyAvatarColorUseCase]
 * @property requestLastGreen               [RequestLastGreen]
 * @property getContactEmail                [GetContactEmail]
 * @property getUserAvatarColorUseCase      [GetUserAvatarColorUseCase]
 * @property userStatusToIntMapper          [UserStatusToIntMapper]
 * @property megaHandleListMapper           [MegaHandleListMapper]
 * @property chatPermissionsMapper          [ChatPermissionsMapper]
 * @property getMyAvatarFileUseCase         [GetMyAvatarFileUseCase]
 * @property getAvatarFileFromEmailUseCase  [GetAvatarFileFromEmailUseCase]
 * @property getUserAliasUseCase            [GetUserAliasUseCase]
 * @property getAvatarFileFromHandleUseCase [GetAvatarFileFromHandleUseCase]
 * @property areCredentialsVerifiedUseCase  [AreCredentialsVerifiedUseCase]
 * @property getContactFullNameUseCase      [GetContactFullNameUseCase]
 * @property getUserPrivilegeUseCase        [GetUserPrivilegeUseCase]
 * @property ioDispatcher                   [CoroutineDispatcher]
 */
internal class DefaultChatParticipantsRepository @Inject constructor(
    private val megaChatApiGateway: MegaChatApiGateway,
    private val megaApiGateway: MegaApiGateway,
    private val getMyAvatarColorUseCase: GetMyAvatarColorUseCase,
    private val requestLastGreen: RequestLastGreen,
    private val getContactEmail: GetContactEmail,
    private val getUserAvatarColorUseCase: GetUserAvatarColorUseCase,
    private val chatPermissionsMapper: ChatPermissionsMapper,
    private val megaHandleListMapper: MegaHandleListMapper,
    private val userStatusToIntMapper: UserStatusToIntMapper,
    private val getMyAvatarFileUseCase: GetMyAvatarFileUseCase,
    private val getAvatarFileFromEmailUseCase: GetAvatarFileFromEmailUseCase,
    private val getAvatarFileFromHandleUseCase: GetAvatarFileFromHandleUseCase,
    private val getUserAliasUseCase: GetUserAliasUseCase,
    private val areCredentialsVerifiedUseCase: AreCredentialsVerifiedUseCase,
    private val getContactFullNameUseCase: GetContactFullNameUseCase,
    private val getUserPrivilegeUseCase: GetUserPrivilegeUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val userChatStatusMapper: UserChatStatusMapper,
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
                            defaultAvatarColor = getMyAvatarColorUseCase(),
                            privilege = chatPermissionsMapper(chatRoom.ownPrivilege)
                        )
                    )
                }

                peerHandles.forEach { handle ->
                    val participantPrivilege = chatRoom.getPeerPrivilegeByHandle(handle)
                    if (participantPrivilege != MegaChatRoom.PRIV_RM) {
                        val alias = async { megaChatApiGateway.getUserAliasFromCache(handle) }
                        val fullName = async {
                            getContactFullNameUseCase(handle)
                        }
                        val email = async { getContactEmail(handle) }
                        val privilege = async { chatPermissionsMapper(participantPrivilege) }
                        val avatarColor = async { getUserAvatarColorUseCase(handle) }

                        participants.add(
                            ChatParticipant(
                                handle = handle,
                                data = ContactData(
                                    fullName = fullName.await(),
                                    alias = alias.await(),
                                    avatarUri = null
                                ),
                                email = email.await() ?: "",
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

    override suspend fun getSeveralChatParticipants(
        chatId: Long,
        peerHandles: List<Long>,
        preloadUserAttributes: Boolean,
    ): List<ChatParticipant> =
        withContext(ioDispatcher) {
            val participants = mutableListOf<ChatParticipant>()
            if (preloadUserAttributes) {
                peerHandles.map {
                    async { loadUserAttributes(chatId, listOf(it)) }
                }.awaitAll() // Retrieve User Attributes in parallel
            }

            peerHandles.forEach { handle ->
                val participantPrivilege = async { getUserPrivilegeUseCase(chatId, handle) }
                val alias = async { megaChatApiGateway.getUserAliasFromCache(handle) }
                val fullName = async { getContactFullNameUseCase(handle) }
                val email = async { getContactEmail(handle) }
                val avatarColor = async { getUserAvatarColorUseCase(handle) }
                participants.add(
                    ChatParticipant(
                        handle = handle,
                        data = ContactData(
                            fullName = fullName.await(),
                            alias = alias.await(),
                            avatarUri = null
                        ),
                        email = email.await() ?: "",
                        isMe = false,
                        privilege = participantPrivilege.await(),
                        defaultAvatarColor = avatarColor.await()
                    )
                )
            }
            return@withContext participants
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

    override suspend fun getStatus(participant: ChatParticipant): UserChatStatus {
        if (participant.isMe) {
            val status = getCurrentStatus()
            if (status != UserChatStatus.Online) {
                requestLastGreen(participant.handle)
            }
            return status
        }

        participant.email?.let { email ->
            megaApiGateway.getContact(email)?.let {
                val status = userChatStatusMapper(megaChatApiGateway.getUserOnlineStatus(it.handle))
                if (status != UserChatStatus.Online) {
                    requestLastGreen(it.handle)
                }

                return status
            }

        }

        return UserChatStatus.Invalid
    }

    override suspend fun getCurrentStatus(): UserChatStatus = withContext(ioDispatcher) {
        userChatStatusMapper(megaChatApiGateway.getOnlineStatus())
    }

    override suspend fun getAlias(participant: ChatParticipant): String? =
        runCatching { getUserAliasUseCase(participant.handle) }.fold(
            onSuccess = { alias -> alias },
            onFailure = { null }
        )

    override suspend fun getAvatarColor(participant: ChatParticipant): Int =
        if (participant.isMe) getMyAvatarColorUseCase() else getUserAvatarColorUseCase(participant.handle)

    override suspend fun getAvatarUri(participant: ChatParticipant, skipCache: Boolean): File? =
        runCatching {
            if (participant.isMe) {
                getMyAvatarFileUseCase(false)
            } else {
                participant.email?.let { email ->
                    if (email.isNotBlank()) {
                        getAvatarFileFromEmailUseCase(
                            userEmail = email,
                            skipCache = skipCache
                        )
                    } else {
                        getAvatarFileFromHandleUseCase(
                            userHandle = participant.handle,
                            skipCache = skipCache
                        )
                    }
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

    override suspend fun areCredentialsVerified(participant: ChatParticipant): Boolean {
        val email = participant.email
        if (email == null) {
            return false
        } else {
            runCatching { areCredentialsVerifiedUseCase(email) }.fold(
                onSuccess = { cred -> return cred },
                onFailure = { return false }
            )
        }
    }

    override suspend fun setOnlineStatus(status: UserChatStatus) = withContext(ioDispatcher) {
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
