package mega.privacy.android.data.repository


import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.constant.FileConstant
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.findItemByHandle
import mega.privacy.android.data.extensions.getDecodedAliases
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.extensions.replaceIfExists
import mega.privacy.android.data.extensions.sortList
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.ContactRequestMapper
import mega.privacy.android.data.mapper.InviteContactRequestMapper
import mega.privacy.android.data.mapper.UserUpdateMapper
import mega.privacy.android.data.mapper.chat.ChatConnectionStateMapper
import mega.privacy.android.data.mapper.chat.OnlineStatusMapper
import mega.privacy.android.data.mapper.chat.UserLastGreenMapper
import mega.privacy.android.data.mapper.contact.ContactCredentialsMapper
import mega.privacy.android.data.mapper.contact.ContactDataMapper
import mega.privacy.android.data.mapper.contact.ContactItemMapper
import mega.privacy.android.data.mapper.contact.UserChatStatusMapper
import mega.privacy.android.data.model.ChatUpdate
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.data.wrapper.ContactWrapper
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.ContactLink
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.exception.ContactDoesNotExistException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.ContactsRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaStringMap
import nz.mega.sdk.MegaUser
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Default implementation of [ContactsRepository]
 *
 * @property megaApiGateway           [MegaApiGateway]
 * @property megaChatApiGateway       [MegaChatApiGateway]
 * @property ioDispatcher             [CoroutineDispatcher]
 * @property cacheGateway             [CacheGateway]
 * @property contactRequestMapper     [ContactRequestMapper]
 * @property userLastGreenMapper      [UserLastGreenMapper]
 * @property userUpdateMapper         [UserUpdateMapper]
 * @property onlineStatusMapper       [OnlineStatusMapper]
 * @property contactItemMapper        [ContactItemMapper]
 * @property contactDataMapper        [ContactDataMapper]
 * @property contactCredentialsMapper [ContactCredentialsMapper]
 * @property inviteContactRequestMapper [InviteContactRequestMapper]
 * @property localStorageGateway      [MegaLocalStorageGateway]
 */
internal class DefaultContactsRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val megaChatApiGateway: MegaChatApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val cacheGateway: CacheGateway,
    private val contactRequestMapper: ContactRequestMapper,
    private val userLastGreenMapper: UserLastGreenMapper,
    private val userUpdateMapper: UserUpdateMapper,
    private val onlineStatusMapper: OnlineStatusMapper,
    private val chatConnectionStateMapper: ChatConnectionStateMapper,
    private val contactItemMapper: ContactItemMapper,
    private val contactDataMapper: ContactDataMapper,
    private val contactCredentialsMapper: ContactCredentialsMapper,
    private val inviteContactRequestMapper: InviteContactRequestMapper,
    private val localStorageGateway: MegaLocalStorageGateway,
    private val contactWrapper: ContactWrapper,
    private val databaseHandler: DatabaseHandler,
    private val megaLocalRoomGateway: MegaLocalRoomGateway,
    @ApplicationContext private val context: Context,
    private val userChatStatusMapper: UserChatStatusMapper,
) : ContactsRepository {

    override fun monitorContactRequestUpdates(): Flow<List<ContactRequest>> =
        megaApiGateway.globalUpdates
            .filterIsInstance<GlobalUpdate.OnContactRequestsUpdate>()
            .mapNotNull { it.requests?.map(contactRequestMapper) }
            .flowOn(ioDispatcher)

    override fun monitorChatPresenceLastGreenUpdates() = megaChatApiGateway.chatUpdates
        .filterIsInstance<ChatUpdate.OnChatPresenceLastGreen>()
        .map { userLastGreenMapper(it.userHandle, it.lastGreen) }
        .flowOn(ioDispatcher)

    override fun monitorChatOnlineStatusUpdates() = megaChatApiGateway.chatUpdates
        .filterIsInstance<ChatUpdate.OnChatOnlineStatusUpdate>()
        .map { onlineStatusMapper(it.userHandle, it.status, it.inProgress) }
        .flowOn(ioDispatcher)

    override fun monitorMyChatOnlineStatusUpdates() = monitorChatOnlineStatusUpdates()
        .filter { it.userHandle == megaChatApiGateway.getMyUserHandle() }

    override fun monitorChatConnectionStateUpdates() = megaChatApiGateway.chatUpdates
        .filterIsInstance<ChatUpdate.OnChatConnectionStateUpdate>()
        .map { chatConnectionStateMapper(it.chatId, it.newState) }
        .flowOn(ioDispatcher)

    override suspend fun requestLastGreen(userHandle: Long) {
        megaChatApiGateway.requestLastGreen(userHandle)
    }

    override fun monitorContactUpdates(): Flow<UserUpdate> =
        megaApiGateway.globalUpdates
            .filterIsInstance<GlobalUpdate.OnUsersUpdate>()
            .mapNotNull { it.users }
            .map { usersList ->
                userUpdateMapper(usersList.filter { user ->
                    (user.handle != megaApiGateway.myUserHandle &&
                            (user.changes == 0L ||
                                    (user.hasChanged(MegaUser.CHANGE_TYPE_AVATAR.toLong()) && user.isOwnChange == 0) ||
                                    user.hasChanged(MegaUser.CHANGE_TYPE_FIRSTNAME.toLong()) ||
                                    user.hasChanged(MegaUser.CHANGE_TYPE_LASTNAME.toLong()) ||
                                    user.hasChanged(MegaUser.CHANGE_TYPE_EMAIL.toLong())) ||
                            (user.handle == megaApiGateway.myUserHandle &&
                                    (user.hasChanged(MegaUser.CHANGE_TYPE_ALIAS.toLong()) ||
                                            user.hasChanged(MegaUser.CHANGE_TYPE_AUTHRING.toLong()))))
                })
            }
            .filter { it.changes.isNotEmpty() }
            .flowOn(ioDispatcher)

    override suspend fun getVisibleContacts(): List<ContactItem> = withContext(ioDispatcher) {
        megaApiGateway.getContacts()
            .filter { contact -> contact.visibility == MegaUser.VISIBILITY_VISIBLE }
            .map { getContactItem(it, false) }
            .sortList()
    }

    /**
     * Requests last green if the user is not online.
     *
     * @param status User online status.
     * @param userHandle User handle.
     */
    private suspend fun checkLastGreen(status: Int, userHandle: Long) {
        if (status != MegaChatApi.STATUS_ONLINE) {
            megaChatApiGateway.requestLastGreen(userHandle)
        }
    }

    override suspend fun getContactData(contactItem: ContactItem): ContactData =
        withContext(ioDispatcher) {
            val email = contactItem.email
            val fullName = runCatching { getUserFullName(contactItem.handle) }.getOrNull()
            val alias = runCatching { getUserAlias(contactItem.handle) }.getOrNull()
            val avatarUri = getAvatarUri(email)

            contactDataMapper(fullName, alias, avatarUri)
        }

    override suspend fun getContactItem(userId: UserId, skipCache: Boolean): ContactItem? =
        withContext(ioDispatcher) {
            val email = getUserEmail(userId.id)
            megaApiGateway.getContact(email)?.let {
                getContactItem(it, skipCache)
            }
        }

    override suspend fun getUserAlias(handle: Long): String =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaApiGateway.getUserAlias(
                    userHandle = handle,
                    listener = OptionalMegaRequestListenerInterface(
                        onRequestFinish = onRequestGetUserAliasCompleted(continuation)
                    )
                )
            }
        }

    private fun onRequestGetUserAliasCompleted(continuation: Continuation<String>) =
        { request: MegaRequest, error: MegaError ->
            if (error.errorCode == MegaError.API_OK) {
                continuation.resumeWith(Result.success(request.name))
            } else {
                continuation.failWithError(error, "onRequestGetUserAliasCompleted")
            }
        }

    override suspend fun getAvatarUri(email: String): String? =
        runCatching {
            val avatarFile =
                cacheGateway.getCacheFile(
                    CacheFolderConstant.AVATAR_FOLDER,
                    email + FileConstant.JPG_EXTENSION
                )

            getContactAvatar(email, avatarFile?.absolutePath ?: return@runCatching null)
        }.fold(
            onSuccess = { avatar -> avatar },
            onFailure = { null }
        )

    override suspend fun deleteAvatar(email: String) {
        withContext(ioDispatcher) {
            val avatarFile =
                cacheGateway.getCacheFile(
                    CacheFolderConstant.AVATAR_FOLDER,
                    email + FileConstant.JPG_EXTENSION
                )
            avatarFile?.delete()
        }
    }

    private suspend fun getContactAvatar(
        email: String,
        avatarFileName: String,
    ): String? =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaApiGateway.getContactAvatar(email,
                    avatarFileName,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = { request: MegaRequest, error: MegaError ->
                            if (error.errorCode == MegaError.API_OK) {
                                continuation.resumeWith(Result.success(request.file))
                            } else {
                                continuation.failWithError(error, "getContactAvatar")
                            }
                        }
                    ))
            }
        }

    override suspend fun getUserEmail(handle: Long, skipCache: Boolean): String =
        withContext(ioDispatcher) {
            if (!skipCache) {
                val cachedEmail = megaChatApiGateway.getUserEmailFromCache(handle)
                if (!cachedEmail.isNullOrBlank()) {
                    return@withContext cachedEmail
                }
            }
            suspendCoroutine { continuation ->
                megaApiGateway.getUserEmail(handle,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = { request: MegaRequest, error: MegaError ->
                            if (error.errorCode == MegaError.API_OK) {
                                continuation.resume(request.email)
                            } else {
                                continuation.failWithError(error, "getUserEmail")
                            }
                        }
                    ))
            }
        }

    override suspend fun getUserFirstName(
        handle: Long,
        skipCache: Boolean,
        shouldNotify: Boolean,
    ): String =
        withContext(ioDispatcher) {
            if (!skipCache) {
                val cachedName = megaChatApiGateway.getUserFirstnameFromCache(handle)
                if (!cachedName.isNullOrBlank()) {
                    return@withContext cachedName
                }
            }
            suspendCoroutine { continuation ->
                megaApiGateway.getUserAttribute(
                    emailOrHandle = handle.toBase64Handle(),
                    type = MegaApiJava.USER_ATTR_FIRSTNAME,
                    listener = OptionalMegaRequestListenerInterface(
                        onRequestFinish = onRequestGetUserNameCompleted(continuation)
                    )
                )
            }.also { request ->
                megaLocalRoomGateway.updateContactFistNameByHandle(handle, request.text)
                if (shouldNotify) {
                    contactWrapper.notifyFirstNameUpdate(context, handle)
                }
            }.text.orEmpty()
        }

    override suspend fun getUserLastName(
        handle: Long,
        skipCache: Boolean,
        shouldNotify: Boolean,
    ): String =
        withContext(ioDispatcher) {
            if (!skipCache) {
                val cachedName = megaChatApiGateway.getUserLastnameFromCache(handle)
                if (!cachedName.isNullOrBlank()) {
                    return@withContext cachedName
                }
            }
            suspendCoroutine { continuation ->
                megaApiGateway.getUserAttribute(
                    emailOrHandle = handle.toBase64Handle(),
                    type = MegaApiJava.USER_ATTR_LASTNAME,
                    listener = OptionalMegaRequestListenerInterface(
                        onRequestFinish = onRequestGetUserNameCompleted(continuation)
                    )
                )
            }.also { request ->
                megaLocalRoomGateway.updateContactLastNameByHandle(handle, request.text)
                if (shouldNotify) {
                    contactWrapper.notifyLastNameUpdate(context, handle)
                }
            }.text.orEmpty()
        }

    override suspend fun getUserFullName(handle: Long, skipCache: Boolean): String =
        withContext(ioDispatcher) {
            if (!skipCache) {
                val cachedName = megaChatApiGateway.getUserFullNameFromCache(handle)
                if (!cachedName.isNullOrBlank()) {
                    return@withContext cachedName
                }
            }

            getUserFirstName(handle)
            getUserLastName(handle)
            megaChatApiGateway.getUserFullNameFromCache(handle) ?: error("Can't retrieve full name")
        }

    private fun onRequestGetUserNameCompleted(continuation: Continuation<MegaRequest>) =
        { request: MegaRequest, error: MegaError ->
            if (error.errorCode == MegaError.API_OK) {
                continuation.resumeWith(Result.success(request))
            } else {
                continuation.failWithError(error, "onRequestGetUserNameCompleted")
            }
        }

    override suspend fun applyContactUpdates(
        outdatedContactList: List<ContactItem>,
        contactUpdates: UserUpdate,
    ): List<ContactItem> {
        val updatedList = outdatedContactList.toMutableList()

        contactUpdates.changes.forEach { (userId, changes) ->
            var updatedContact = outdatedContactList.findItemByHandle(userId.id)
            val megaUser = megaApiGateway.getContact(userId.id.toBase64Handle())

            if (changes.isEmpty()
                && (megaUser == null || megaUser.visibility != MegaUser.VISIBILITY_VISIBLE)
            ) {
                updatedList.removeAll { (handle) -> handle == userId.id }
            } else if (megaUser != null) {
                if (updatedContact == null && megaUser.visibility == MegaUser.VISIBILITY_VISIBLE) {
                    updatedContact = getContactItem(megaUser, true)
                    updatedList.add(updatedContact)
                }

                if (changes.contains(UserChanges.Alias)) {
                    runCatching { getCurrentUserAliases() }.fold(
                        onSuccess = { aliases -> aliases },
                        onFailure = { null }
                    )?.let { aliases ->
                        outdatedContactList.forEach { (handle, _, contactData) ->
                            val newContactData = contactData.copy(
                                alias = if (aliases.containsKey(handle)) aliases[handle] else null
                            )

                            updatedContact = updatedList.findItemByHandle(handle)
                                ?.copy(contactData = newContactData)
                        }
                    }
                }

                if (changes.contains(UserChanges.Firstname) || changes.contains(UserChanges.Lastname)) {
                    val fullName = runCatching { getUserFullName(megaUser.handle) }.getOrNull()
                    updatedContact?.contactData?.copy(fullName = fullName)?.let { contactData ->
                        updatedContact = updatedContact?.copy(contactData = contactData)
                    }
                }

                if (changes.contains(UserChanges.Email)) {
                    updatedContact = updatedContact?.copy(email = megaUser.email)
                }

                if (changes.contains(UserChanges.Avatar)) {
                    val avatarUri = getAvatarUri(megaUser.email)
                    updatedContact?.contactData?.copy(avatarUri = avatarUri)?.let { contactData ->
                        updatedContact = updatedContact?.copy(contactData = contactData)
                    }
                }

                updatedContact?.let { updatedList.replaceIfExists(it) }
            }
        }

        return updatedList.sortList().toMutableList()
    }

    private suspend fun getContactItem(
        megaUser: MegaUser,
        skipCache: Boolean,
    ): ContactItem {
        val fullName: String?
        val alias: String?
        if (skipCache) {
            fullName = runCatching { getUserFullName(megaUser.handle) }.getOrNull()
            alias = runCatching { getUserAlias(megaUser.handle) }.getOrNull()
        } else {
            fullName = megaChatApiGateway.getUserFullNameFromCache(megaUser.handle)
            alias = megaChatApiGateway.getUserAliasFromCache(megaUser.handle)
        }
        val status = megaChatApiGateway.getUserOnlineStatus(megaUser.handle)
        val avatarUri = if (skipCache) {
            getAvatarUri(megaUser.email)
        } else {
            cacheGateway.getCacheFile(
                folderName = CacheFolderConstant.AVATAR_FOLDER,
                fileName = megaUser.email + FileConstant.JPG_EXTENSION
            )?.takeIf { it.exists() }?.absolutePath
        }

        checkLastGreen(status, megaUser.handle)

        val contactData = contactDataMapper(
            fullName?.ifEmpty { null },
            alias?.ifEmpty { null },
            avatarUri
        )

        return contactItemMapper(
            megaUser,
            contactData,
            megaApiGateway.getUserAvatarColor(megaUser),
            megaApiGateway.areCredentialsVerified(megaUser),
            status,
            null
        )
    }

    override suspend fun getCurrentUserAliases(): Map<Long, String> = withContext(ioDispatcher) {
        suspendCoroutine { continuation ->
            megaApiGateway.myUser?.let {
                megaApiGateway.getUserAttribute(
                    user = it,
                    type = MegaApiJava.USER_ATTR_ALIAS,
                    listener = OptionalMegaRequestListenerInterface(
                        onRequestFinish = onRequestGetAliasesCompleted(continuation)
                    )
                )
            } ?: continuation.resumeWith(Result.failure(NullPointerException("myUser null")))
        }.getDecodedAliases()
            .also {
                updateContactsNickname(megaApiGateway.getContacts(), it)
            }
    }

    private fun onRequestGetAliasesCompleted(continuation: Continuation<MegaStringMap>) =
        { request: MegaRequest, error: MegaError ->
            if (error.errorCode == MegaError.API_OK) {
                continuation.resumeWith(Result.success(request.megaStringMap))
            } else {
                continuation.failWithError(error, "onRequestGetAliasesCompleted")
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
                    updatedList.add(getContactItem(megaUser, true))
                }
            }
        }

        return updatedList.sortList()
    }

    private suspend fun getUserCredentials(user: MegaUser) = withContext(ioDispatcher) {
        suspendCoroutine { continuation ->
            megaApiGateway.getUserCredentials(
                user = user,
                listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = onGetUserCredentialsCompleted(continuation)
                )
            )
        }
    }

    private fun onGetUserCredentialsCompleted(continuation: Continuation<String?>) =
        { request: MegaRequest, error: MegaError ->
            if (error.errorCode == MegaError.API_OK) {
                continuation.resumeWith(Result.success(request.password))
            } else {
                continuation.failWithError(error, "onGetUserCredentialsCompleted")
            }
        }

    override suspend fun areCredentialsVerified(userEmail: String) = withContext(ioDispatcher) {
        megaApiGateway.getContact(userEmail)?.let {
            megaApiGateway.areCredentialsVerified(it)
        } ?: throw ContactDoesNotExistException()
    }

    override suspend fun resetCredentials(userEmail: String) = withContext(ioDispatcher) {
        megaApiGateway.getContact(userEmail)?.let {
            suspendCoroutine { continuation: Continuation<Unit> ->
                megaApiGateway.resetCredentials(
                    user = it,
                    listener = OptionalMegaRequestListenerInterface(
                        onRequestFinish = onResetCredentialsCompleted(continuation)
                    )
                )
            }
        } ?: throw ContactDoesNotExistException()
    }

    private fun onResetCredentialsCompleted(continuation: Continuation<Unit>) =
        { _: MegaRequest, error: MegaError ->
            if (error.errorCode == MegaError.API_OK) {
                continuation.resumeWith(Result.success(Unit))
            } else {
                continuation.failWithError(error, "onResetCredentialsCompleted")
            }
        }

    override suspend fun verifyCredentials(userEmail: String) = withContext(ioDispatcher) {
        megaApiGateway.getContact(userEmail)?.let {
            suspendCoroutine { continuation: Continuation<Unit> ->
                megaApiGateway.verifyCredentials(
                    user = it,
                    listener = OptionalMegaRequestListenerInterface(
                        onRequestFinish = onVerifyCredentialsCompleted(continuation)
                    )
                )
            }
        } ?: throw ContactDoesNotExistException()
    }

    private fun onVerifyCredentialsCompleted(continuation: Continuation<Unit>) =
        { _: MegaRequest, error: MegaError ->
            if (error.errorCode == MegaError.API_OK) {
                continuation.resumeWith(Result.success(Unit))
            } else {
                continuation.failWithError(error, "onVerifyCredentialsCompleted")
            }
        }

    override suspend fun getContactCredentials(userEmail: String) =
        withContext(ioDispatcher) {
            megaApiGateway.getContact(userEmail)?.let { user ->
                val userCredentials = runCatching { getUserCredentials(user) }.getOrNull()
                val name = runCatching { getUserAlias(user.handle) }.getOrNull()
                    ?: runCatching { getUserFullName(user.handle, skipCache = false) }.getOrNull()
                    ?: userEmail

                contactCredentialsMapper(
                    userCredentials,
                    userEmail,
                    name
                )
            }
        }

    override suspend fun getCurrentUserFirstName(forceRefresh: Boolean): String =
        withContext(ioDispatcher) {
            if (forceRefresh) {
                getCurrentUserNameAttribute(MegaApiJava.USER_ATTR_FIRSTNAME)
                    .also { localStorageGateway.saveMyFirstName(it) }
            } else {
                localStorageGateway.getUserCredentials()?.firstName
                    ?: getCurrentUserNameAttribute(MegaApiJava.USER_ATTR_FIRSTNAME)
                        .also { localStorageGateway.saveMyFirstName(it) }
            }
        }

    override suspend fun getCurrentUserLastName(forceRefresh: Boolean): String =
        withContext(ioDispatcher) {
            if (forceRefresh) {
                getCurrentUserNameAttribute(MegaApiJava.USER_ATTR_LASTNAME)
                    .also { localStorageGateway.saveMyLastName(it) }
            } else {
                localStorageGateway.getUserCredentials()?.lastName
                    ?: getCurrentUserNameAttribute(MegaApiJava.USER_ATTR_LASTNAME)
                        .also { localStorageGateway.saveMyLastName(it) }
            }
        }

    override suspend fun inviteContact(
        email: String,
        handle: Long,
        message: String?,
    ): InviteContactRequest = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request: MegaRequest, error: MegaError ->
                    continuation.resumeWith(runCatching {
                        inviteContactRequestMapper(
                            error,
                            request.email,
                            megaApiGateway.outgoingContactRequests()
                        )
                    })
                }
            )
            megaApiGateway.inviteContact(
                email,
                handle,
                message,
                listener
            )

            continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
        }
    }

    private suspend fun getCurrentUserNameAttribute(attribute: Int): String =
        withContext(ioDispatcher) {
            return@withContext suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("getCurrentUserNameAttribute") {
                    it.text.orEmpty()
                }
                megaApiGateway.getUserAttribute(attribute, listener)
                continuation.invokeOnCancellation {
                    megaApiGateway.removeRequestListener(listener)
                }
            }
        }

    override suspend fun updateCurrentUserFirstName(value: String): String = executeNameUpdate(
        MegaApiJava.USER_ATTR_FIRSTNAME,
        "setUserAttribute(MegaApiJava.USER_ATTR_FIRSTNAME)",
        value
    ) { localStorageGateway.saveMyFirstName(it) }

    override suspend fun updateCurrentUserLastName(value: String): String = executeNameUpdate(
        MegaApiJava.USER_ATTR_LASTNAME,
        "setUserAttribute(MegaApiJava.USER_ATTR_LASTNAME)",
        value
    ) { localStorageGateway.saveMyLastName(it) }

    private suspend inline fun executeNameUpdate(
        type: Int,
        methodName: String,
        value: String,
        crossinline block: suspend ((value: String) -> Unit),
    ) = withContext(ioDispatcher) {
        return@withContext suspendCancellableCoroutine<String> { continuation ->
            val listener =
                continuation.getRequestListener(methodName = methodName) { it.text }
            megaApiGateway.setUserAttribute(
                type,
                value,
                listener,
            )
            continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
        }.also {
            block(it)
        }
    }

    override suspend fun getContactEmails(): Map<Long, String> = withContext(ioDispatcher) {
        val contacts = megaApiGateway.getContacts()
        contacts.associate { it.handle to it.email.orEmpty() }
    }

    override suspend fun clearContactDatabase() = withContext(ioDispatcher) {
        Timber.d("clear Database")
        databaseHandler.clearContacts()
    }

    override suspend fun createOrUpdateContact(
        handle: Long,
        email: String,
        firstName: String,
        lastName: String,
        nickname: String?,
    ) = withContext(ioDispatcher) {
        val contact = megaLocalRoomGateway.getContactByHandle(handle)
            ?.copy(email = email, firstName = firstName, lastName = lastName, nickname = nickname)
            ?: Contact(
                userId = handle,
                email = email,
                lastName = lastName,
                firstName = firstName
            )
        megaLocalRoomGateway.insertContact(contact)
    }

    override suspend fun getContactDatabaseSize(): Int = withContext(ioDispatcher) {
        megaLocalRoomGateway.getContactCount()
    }

    override suspend fun getContactEmail(handle: Long): String = withContext(ioDispatcher) {
        suspendCancellableCoroutine<String> { continuation ->
            val listener = continuation.getRequestListener("getContactEmail") {
                return@getRequestListener it.email
            }
            megaApiGateway.getUserEmail(handle, listener)
            continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
        }.also {
            megaLocalRoomGateway.updateContactMailByHandle(handle, it)
        }
    }

    private fun Long.toBase64Handle(): String = megaApiGateway.userHandleToBase64(this)

    override suspend fun getUserOnlineStatusByHandle(handle: Long) = withContext(ioDispatcher) {
        userChatStatusMapper(megaChatApiGateway.getUserOnlineStatus(handle))
    }

    override suspend fun getUserEmailFromChat(handle: Long) =
        withContext(ioDispatcher) {
            val chatRoom = megaChatApiGateway.getChatRoom(handle)
            chatRoom?.getPeerHandle(0)?.toBase64Handle()
        }

    override suspend fun getContactItemFromUserEmail(email: String, skipCache: Boolean) =
        withContext(ioDispatcher) {
            megaApiGateway.getContact(email)?.let {
                getContactItem(it, skipCache)
            }
        }

    override suspend fun setUserAlias(name: String?, userHandle: Long) = withContext(ioDispatcher) {
        suspendCancellableCoroutine<String> { continuation ->
            val listener = continuation.getRequestListener("setUserAlias") {
                return@getRequestListener it.text
            }
            megaApiGateway.setUserAlias(
                userHandle = userHandle,
                name = name,
                listener = listener
            )
            continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
        }.also {
            megaLocalRoomGateway.updateContactNicknameByHandle(userHandle, it)
        }
    }

    override suspend fun removeContact(email: String): Boolean = withContext(ioDispatcher) {
        val contact = megaApiGateway.getContact(email) ?: return@withContext false
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("removeContact") { true }
            megaApiGateway.removeContact(contact, listener)
            continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
        }
    }

    override suspend fun getContactHandleByEmail(email: String) = withContext(ioDispatcher) {
        megaApiGateway.getContact(email)?.handle ?: -1L
    }

    private suspend fun updateContactsNickname(
        contacts: List<MegaUser>?,
        alias: Map<Long, String>,
    ) {
        if (contacts.isNullOrEmpty()) return
        val localContacts = megaLocalRoomGateway.getAllContacts().associateBy { it.userId }
        if (localContacts.isEmpty()) return
        contacts.forEach {
            val oldNickname = localContacts[it.handle]?.nickname
            val newNickname = alias[it.handle]
            if (oldNickname != newNickname) {
                megaLocalRoomGateway.updateContactNicknameByHandle(it.handle, newNickname)
                contactWrapper.notifyNicknameUpdate(context, it.handle)
            }
        }
    }

    override suspend fun getIncomingContactRequests(): List<ContactRequest> =
        withContext(ioDispatcher) {
            megaApiGateway.getIncomingContactRequests()?.map(contactRequestMapper).orEmpty()
        }

    override suspend fun getContactLink(userHandle: Long) = withContext(ioDispatcher) {
        val result = suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request: MegaRequest, error: MegaError ->
                    when (error.errorCode) {
                        MegaError.API_OK -> {
                            databaseHandler.setLastPublicHandle(request.nodeHandle)
                            databaseHandler.setLastPublicHandleTimeStamp()
                            databaseHandler.lastPublicHandleType =
                                MegaApiJava.AFFILIATE_TYPE_CONTACT
                            continuation.resumeWith(
                                Result.success(
                                    ContactLink(
                                        email = request.email,
                                        contactHandle = request.parentHandle,
                                        contactLinkHandle = request.nodeHandle,
                                        fullName = "${request.name} ${request.text}"
                                    )
                                )
                            )
                        }

                        MegaError.API_EEXIST -> continuation.resumeWith(
                            Result.success(ContactLink(isContact = false))
                        )

                        else -> continuation.failWithError(error, "getContactLink")

                    }
                },
            )
            megaApiGateway.getContactLink(userHandle, listener)
            continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
        }
        val isContact = !result.email.isNullOrBlank() && megaApiGateway.getContacts()
            .any { contact -> result.email == contact.email && contact.visibility == MegaUser.VISIBILITY_VISIBLE }
        return@withContext result.copy(isContact = isContact)
    }

    override suspend fun isContactRequestSent(email: String) = withContext(ioDispatcher) {
        megaApiGateway.outgoingContactRequests().any { it.targetEmail == email }
    }
}
