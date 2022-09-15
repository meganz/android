package mega.privacy.android.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
import mega.privacy.android.app.data.extensions.failWithError
import mega.privacy.android.app.data.extensions.findItemByHandle
import mega.privacy.android.app.data.extensions.getDecodedAliases
import mega.privacy.android.app.data.extensions.replaceIfExists
import mega.privacy.android.app.data.extensions.sortList
import mega.privacy.android.app.data.gateway.CacheFolderGateway
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.app.data.mapper.ContactDataMapper
import mega.privacy.android.app.data.mapper.ContactItemMapper
import mega.privacy.android.app.data.mapper.ContactRequestMapper
import mega.privacy.android.app.data.mapper.MegaChatPeerListMapper
import mega.privacy.android.app.data.mapper.OnlineStatusMapper
import mega.privacy.android.app.data.mapper.UserLastGreenMapper
import mega.privacy.android.app.data.mapper.UserUpdateMapper
import mega.privacy.android.app.data.model.ChatUpdate
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.listeners.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.presentation.extensions.getFormattedStringOrDefault
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.repository.ContactsRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaUser
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

/**
 * Default implementation of [ContactsRepository]
 *
 * @property megaApiGateway         [MegaApiGateway]
 * @property megaChatApiGateway     [MegaChatApiGateway]
 * @property ioDispatcher           [CoroutineDispatcher]
 * @property context                [Context]
 * @property cacheFolderGateway     [CacheFolderGateway]
 * @property contactRequestMapper   [ContactRequestMapper]
 * @property userLastGreenMapper    [UserLastGreenMapper]
 * @property userUpdateMapper       [UserUpdateMapper]
 * @property megaChatPeerListMapper [MegaChatPeerListMapper]
 * @property onlineStatusMapper     [OnlineStatusMapper]
 * @property contactItemMapper      [ContactItemMapper]
 * @property contactDataMapper      [ContactDataMapper]
 */
class DefaultContactsRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val megaChatApiGateway: MegaChatApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context,
    private val cacheFolderGateway: CacheFolderGateway,
    private val contactRequestMapper: ContactRequestMapper,
    private val userLastGreenMapper: UserLastGreenMapper,
    private val userUpdateMapper: UserUpdateMapper,
    private val megaChatPeerListMapper: MegaChatPeerListMapper,
    private val onlineStatusMapper: OnlineStatusMapper,
    private val contactItemMapper: ContactItemMapper,
    private val contactDataMapper: ContactDataMapper,
) : ContactsRepository {

    override fun monitorContactRequestUpdates(): Flow<List<ContactRequest>> =
        megaApiGateway.globalUpdates
            .filterIsInstance<GlobalUpdate.OnContactRequestsUpdate>()
            .mapNotNull { it.requests?.map(contactRequestMapper) }

    override fun monitorChatPresenceLastGreenUpdates() = megaChatApiGateway.chatUpdates
        .filterIsInstance<ChatUpdate.OnChatPresenceLastGreen>()
        .map { userLastGreenMapper(context, it.userHandle, it.lastGreen) }

    override suspend fun requestLastGreen(userHandle: Long) {
        megaChatApiGateway.requestLastGreen(userHandle)
    }

    override fun monitorContactUpdates() =
        megaApiGateway.globalUpdates
            .filterIsInstance<GlobalUpdate.OnUsersUpdate>()
            .mapNotNull { it.users }
            .map { usersList ->
                userUpdateMapper(usersList.filter { user ->
                    user.handle != megaApiGateway.myUserHandle
                            && (user.changes == 0
                            || (user.hasChanged(MegaUser.CHANGE_TYPE_AVATAR) && user.isOwnChange == 0)
                            || user.hasChanged(MegaUser.CHANGE_TYPE_FIRSTNAME)
                            || user.hasChanged(MegaUser.CHANGE_TYPE_LASTNAME)
                            || user.hasChanged(MegaUser.CHANGE_TYPE_EMAIL)
                            || user.hasChanged(MegaUser.CHANGE_TYPE_ALIAS))
                })
            }.filter { it.changes.isNotEmpty() }

    override suspend fun startConversation(isGroup: Boolean, userHandles: List<Long>): Long =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                val chat1to1 = megaChatApiGateway.getChatRoomByUser(userHandles[0])
                if (!isGroup && chat1to1 != null) {
                    continuation.resumeWith(Result.success(chat1to1.chatId))
                } else {
                    megaChatApiGateway.createChat(isGroup,
                        megaChatPeerListMapper(userHandles),
                        OptionalMegaChatRequestListenerInterface(
                            onRequestFinish = onRequestCreateChatCompleted(continuation)
                        ))
                }
            }
        }

    private fun onRequestCreateChatCompleted(continuation: Continuation<Long>) =
        { request: MegaChatRequest, error: MegaChatError ->
            if (error.errorCode == MegaChatError.ERROR_OK) {
                continuation.resumeWith(Result.success(request.chatHandle))
            } else {
                continuation.failWithError(error)
            }
        }

    override fun monitorChatOnlineStatusUpdates() = megaChatApiGateway.chatUpdates
        .filterIsInstance<ChatUpdate.OnChatOnlineStatusUpdate>()
        .map { onlineStatusMapper(it.userHandle, it.status, it.inProgress) }

    override suspend fun getVisibleContacts(): List<ContactItem> = withContext(ioDispatcher) {
        megaApiGateway.getContacts()
            .filter { contact -> contact.visibility == MegaUser.VISIBILITY_VISIBLE }
            .map { megaUser ->
                val fullName = megaChatApiGateway.getUserFullNameFromCache(megaUser.handle)
                val alias = megaChatApiGateway.getUserAliasFromCache(megaUser.handle)
                val status = megaChatApiGateway.getUserOnlineStatus(megaUser.handle)
                val avatarUri = cacheFolderGateway.getCacheFile(CacheFolderManager.AVATAR_FOLDER,
                    "${megaUser.email}.jpg")?.absolutePath
                val lastSeen = if (status == MegaChatApi.STATUS_ONLINE) {
                    context.getFormattedStringOrDefault(R.string.online_status)
                } else {
                    megaChatApiGateway.requestLastGreen(megaUser.handle)
                    null
                }

                contactItemMapper(
                    megaUser,
                    fullName?.ifEmpty { null },
                    alias?.ifEmpty { null },
                    megaApiGateway.getUserAvatarColor(megaUser),
                    megaApiGateway.areCredentialsVerified(megaUser),
                    status,
                    avatarUri,
                    lastSeen
                )
            }
            .sortList()
    }

    override suspend fun getContactData(contactItem: ContactItem): ContactData =
        withContext(ioDispatcher) {
            val email = contactItem.email
            val fullName = getFullName(email)
            val alias = getAlias(contactItem.handle)
            val avatarUri = getAvatarUri(email, "${email}.jpg")

            contactDataMapper(fullName, alias, avatarUri)
        }

    private suspend fun getFullName(email: String): String? =
        runCatching { getUserFullName(email) }.fold(
            onSuccess = { fullName -> fullName },
            onFailure = { null }
        )

    private suspend fun getAlias(handle: Long): String? =
        runCatching { getUserAlias(handle) }.fold(
            onSuccess = { alias -> alias },
            onFailure = { null }
        )

    private suspend fun getUserAlias(handle: Long): String? =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaApiGateway.getUserAlias(handle,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = onRequestGetUserAliasCompleted(continuation)
                    ))
            }
        }

    private fun onRequestGetUserAliasCompleted(continuation: Continuation<String?>) =
        { request: MegaRequest, error: MegaError ->
            if (error.errorCode == MegaError.API_OK) {
                continuation.resumeWith(Result.success(request.name))
            } else {
                continuation.failWithError(error)
            }
        }

    private suspend fun getAvatarUri(email: String, avatarFileName: String): String? =
        runCatching {
            val avatarFile =
                cacheFolderGateway.getCacheFile(CacheFolderManager.AVATAR_FOLDER, avatarFileName)

            getContactAvatar(email, avatarFile?.absolutePath ?: return@runCatching null)
        }.fold(
            onSuccess = { avatar -> avatar },
            onFailure = { null }
        )

    private suspend fun getContactAvatar(
        emailOrHandle: String,
        avatarFileName: String,
    ): String? =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaApiGateway.getContactAvatar(emailOrHandle,
                    avatarFileName,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = onRequestGetUserAvatarCompleted(continuation)
                    ))
            }
        }

    private suspend fun getUserFullName(emailOrHandle: String): String? =
        withContext(ioDispatcher) {
            getUserFirstName(emailOrHandle)
            getUserLastName(emailOrHandle)
            val userHandle = megaApiGateway.getContact(emailOrHandle)?.handle ?: -1
            val fullName = megaChatApiGateway.getUserFullNameFromCache(userHandle)
            if (fullName.isNullOrEmpty()) null else fullName
        }

    private suspend fun getUserFirstName(emailOrHandle: String): String? =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaApiGateway.getUserAttribute(emailOrHandle,
                    MegaApiJava.USER_ATTR_FIRSTNAME,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = onRequestGetUserNameCompleted(continuation)
                    ))
            }
        }

    private fun onRequestGetUserAvatarCompleted(continuation: Continuation<String?>) =
        { request: MegaRequest, error: MegaError ->
            if (error.errorCode == MegaError.API_OK) {
                continuation.resumeWith(Result.success(request.file))
            } else {
                continuation.failWithError(error)
            }
        }

    private suspend fun getUserLastName(emailOrHandle: String): String? =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaApiGateway.getUserAttribute(emailOrHandle,
                    MegaApiJava.USER_ATTR_LASTNAME,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = onRequestGetUserNameCompleted(continuation)
                    ))
            }
        }

    private fun onRequestGetUserNameCompleted(continuation: Continuation<String?>) =
        { request: MegaRequest, error: MegaError ->
            if (error.errorCode == MegaError.API_OK) {
                continuation.resumeWith(Result.success(request.text))
            } else {
                continuation.failWithError(error)
            }
        }

    override suspend fun applyContactUpdates(
        outdatedContactList: List<ContactItem>,
        contactUpdates: UserUpdate,
    ): List<ContactItem> {
        val updatedList = outdatedContactList.toMutableList()

        contactUpdates.changes.forEach { (userId, changes) ->
            var updatedContact = outdatedContactList.findItemByHandle(userId.id)
            val megaUser = megaApiGateway.getContact(megaApiGateway.userHandleToBase64(userId.id))

            if (changes.isEmpty()
                && (megaUser == null || megaUser.visibility != MegaUser.VISIBILITY_VISIBLE)
            ) {
                updatedList.removeIf { (handle) -> handle == userId.id }
            } else if (megaUser != null) {
                if (updatedContact == null && megaUser.visibility == MegaUser.VISIBILITY_VISIBLE) {
                    updatedContact = getVisibleContact(megaUser)
                    updatedList.add(updatedContact)
                }

                if (changes.contains(UserChanges.Alias)) {
                    runCatching { getAliases() }.fold(
                        onSuccess = { aliases -> aliases },
                        onFailure = { null }
                    )?.let { aliases ->
                        outdatedContactList.forEach { (userHandle) ->
                            updatedContact = updatedList.findItemByHandle(userHandle)
                                ?.copy(alias = if (aliases.containsKey(userHandle)) aliases[userHandle] else null)
                        }
                    }
                }

                if (changes.contains(UserChanges.Firstname) || changes.contains(UserChanges.Lastname)) {
                    val fullName = getFullName(megaUser.email)
                    updatedContact = updatedContact?.copy(fullName = fullName)
                }

                if (changes.contains(UserChanges.Email)) {
                    updatedContact = updatedContact?.copy(email = megaUser.email)
                }

                if (changes.contains(UserChanges.Avatar)) {
                    val avatarUri = getAvatarUri(megaUser.email, "${megaUser.email}.jpg")
                    updatedContact = updatedContact?.copy(avatarUri = avatarUri)
                }

                updatedContact?.let { updatedList.replaceIfExists(it) }
            }
        }

        return updatedList.sortList().toMutableList()
    }

    private suspend fun getVisibleContact(megaUser: MegaUser): ContactItem {
        val fullName = getFullName(megaUser.email)
        val alias = getAlias(megaUser.handle)
        val status = megaChatApiGateway.getUserOnlineStatus(megaUser.handle)
        val lastSeen = if (status == MegaChatApi.STATUS_ONLINE) {
            context.getFormattedStringOrDefault(R.string.online_status)
        } else {
            megaChatApiGateway.requestLastGreen(megaUser.handle)
            null
        }

        return contactItemMapper(
            megaUser,
            fullName,
            alias,
            megaApiGateway.getUserAvatarColor(megaUser),
            megaApiGateway.areCredentialsVerified(megaUser),
            status,
            getAvatarUri(megaUser.email, "${megaUser.email}.jpg"),
            lastSeen
        )
    }

    private suspend fun getAliases(): Map<Long, String> = withContext(ioDispatcher) {
        suspendCoroutine { continuation ->
            megaApiGateway.myUser?.let {
                megaApiGateway.getUserAttribute(it, MegaApiJava.USER_ATTR_ALIAS,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = onRequestGetAliasesCompleted(continuation)
                    )
                )
            }
        }
    }

    private fun onRequestGetAliasesCompleted(continuation: Continuation<Map<Long, String>>) =
        { request: MegaRequest, error: MegaError ->
            if (error.errorCode == MegaError.API_OK) {
                continuation.resumeWith(Result.success(request.megaStringMap.getDecodedAliases()))
            } else {
                continuation.failWithError(error)
            }
        }

    override suspend fun addNewContacts(
        outdatedContactList: List<ContactItem>,
        newContacts: List<ContactRequest>,
    ): List<ContactItem> {
        val updatedList = outdatedContactList.toMutableList()

        newContacts.forEach { contactRequest ->
            if (updatedList.find { contact -> contact.email == contactRequest.sourceEmail } == null) {
                val megaUser = megaApiGateway.getContact(contactRequest.sourceEmail)
                if (megaUser != null) {
                    updatedList.add(getVisibleContact(megaUser))
                }
            }
        }

        return updatedList.sortList()
    }
}