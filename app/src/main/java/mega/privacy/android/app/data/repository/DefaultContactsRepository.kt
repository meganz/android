package mega.privacy.android.app.data.repository

import android.content.Context
import com.vdurmont.emoji.EmojiParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
import mega.privacy.android.app.components.twemoji.EmojiUtils
import mega.privacy.android.app.components.twemoji.EmojiUtilsShortcodes
import mega.privacy.android.app.data.extensions.failWithError
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
import mega.privacy.android.domain.repository.ContactsRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaUser
import java.util.Locale
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
                    getAvatarFirstLetter(alias ?: fullName ?: megaUser.email),
                    megaApiGateway.getUserAvatarColor(megaUser),
                    megaApiGateway.areCredentialsVerified(megaUser),
                    status,
                    avatarUri,
                    lastSeen
                )
            }
            .sortList()
    }

    /**
     * Retrieve the first letter of a String.
     *
     * @param text String to obtain the first letter.
     * @return The first letter of the string to be painted in the default avatar.
     */
    private fun getAvatarFirstLetter(text: String): String {
        val unknown = "U"

        if (text.isEmpty()) {
            return unknown
        }

        val result = text.trim { it <= ' ' }
        if (result.length == 1) {
            return result[0].toString().uppercase(Locale.getDefault())
        }

        val resultTitle = EmojiUtilsShortcodes.emojify(result)
        if (resultTitle.isNullOrEmpty()) {
            return unknown
        }

        val emojis = EmojiUtils.emojis(resultTitle)

        if (emojis.size > 0 && emojis[0].start == 0) {
            return resultTitle.substring(emojis[0].start, emojis[0].end)
        }

        val resultEmojiCompat = getEmojiCompatAtFirst(resultTitle)
        if (resultEmojiCompat != null) {
            return resultEmojiCompat
        }

        val resultChar = resultTitle[0].toString().uppercase(Locale.getDefault())
        return if (resultChar.trim { it <= ' ' }
                .isEmpty() || resultChar == "(" || !isRecognizableCharacter(
                resultChar[0])
        ) {
            unknown
        } else resultChar

    }

    /**
     * Gets the first character as an emoji if any.
     *
     * @param text Text to check.
     * @return The emoji if any, null otherwise.
     */
    private fun getEmojiCompatAtFirst(text: String?): String? {
        if (text.isNullOrEmpty()) {
            return null
        }

        val listEmojis = EmojiParser.extractEmojis(text)

        if (listEmojis != null && listEmojis.isNotEmpty()) {
            val substring = text.substring(0, listEmojis[0].length)
            val sublistEmojis = EmojiParser.extractEmojis(substring)
            if (sublistEmojis != null && sublistEmojis.isNotEmpty()) {
                return substring
            }
        }

        return null
    }

    /**
     * Retrieve if a char is recognizable.
     *
     * @param inputChar The char to be examined.
     * @return True if the char is recognizable. Otherwise false.
     */
    private fun isRecognizableCharacter(inputChar: Char): Boolean =
        inputChar.code in 48..57 || inputChar.code in 65..90 || inputChar.code in 97..122


    override suspend fun getContactData(contactItem: ContactItem): ContactData =
        withContext(ioDispatcher) {
            val email = contactItem.email
            val fullName = getFullName(email)
            val alias = getAlias(contactItem.handle)
            val avatarUri = getAvatarUri(email, "${email}.jpg")
            val defaultAvatarContent = getAvatarFirstLetter(alias ?: fullName ?: email)

            contactDataMapper(fullName, alias, avatarUri, defaultAvatarContent)
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
        email_or_handle: String,
        avatarFileName: String,
    ): String? =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaApiGateway.getContactAvatar(email_or_handle,
                    avatarFileName,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = onRequestGetUserAvatarCompleted(continuation)
                    ))
            }
        }

    private suspend fun getUserFullName(email_or_handle: String): String? =
        withContext(ioDispatcher) {
            getUserFirstName(email_or_handle)
            getUserLastName(email_or_handle)
            val userHandle = megaApiGateway.getContact(email_or_handle)?.handle ?: -1
            val fullName = megaChatApiGateway.getUserFullNameFromCache(userHandle)
            if (fullName.isNullOrEmpty()) null else fullName
        }

    private suspend fun getUserFirstName(email_or_handle: String): String? =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaApiGateway.getUserAttribute(email_or_handle,
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

    private suspend fun getUserLastName(email_or_handle: String): String? =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaApiGateway.getUserAttribute(email_or_handle,
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
}