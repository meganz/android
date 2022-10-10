package mega.privacy.android.app.contacts.usecase

import android.content.Context
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import mega.privacy.android.app.contacts.group.data.ContactGroupItem
import mega.privacy.android.app.contacts.group.data.ContactGroupUser
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.USER_ATTR_AVATAR
import nz.mega.sdk.MegaApiJava.USER_ATTR_FIRSTNAME
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest.TYPE_GET_USER_EMAIL
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Use Case to retrieve contact groups for current user.
 *
 * @property context        Application context required to get resources
 * @property megaApi        MegaApi required to call the SDK
 * @property megaChatApi    MegaChatApi required to call the SDK
 */
class GetContactGroupsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid
) {

    fun get(): Flowable<List<ContactGroupItem>> =
        Flowable.create({ emitter ->
            val groups = mutableListOf<ContactGroupItem>()

            val userAttrsListener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (emitter.isCancelled) return@OptionalMegaRequestListenerInterface

                    if (error.errorCode == MegaError.API_OK) {
                        val index = groups.indexOfFirst {
                            request.nodeHandle in it.firstUser.handle..it.lastUser.handle
                                    || request.email == it.firstUser.email || request.email == it.lastUser.email
                        }

                        if (index != INVALID_POSITION) {
                            val currentGroup = groups[index]
                            when (request.paramType) {
                                TYPE_GET_USER_EMAIL -> {
                                    groups[index] =
                                        if (request.nodeHandle == currentGroup.firstUser.handle) {
                                            currentGroup.copy(
                                                firstUser = currentGroup.firstUser.copy(email = request.text)
                                            )
                                        } else {
                                            currentGroup.copy(
                                                lastUser = currentGroup.lastUser.copy(email = request.text)
                                            )
                                        }
                                }
                                USER_ATTR_FIRSTNAME -> {
                                    val isFirstUser = request.nodeHandle == currentGroup.firstUser.handle
                                    val user = if (isFirstUser) currentGroup.firstUser else currentGroup.lastUser

                                    val newFirstname = request.text
                                    val newUser = user.copy(
                                        firstName = newFirstname
                                    )

                                    groups[index] = if (isFirstUser) {
                                        currentGroup.copy(firstUser = newUser)
                                    } else {
                                        currentGroup.copy(lastUser = newUser)
                                    }
                                }
                                USER_ATTR_AVATAR -> {
                                    groups[index] =
                                        if (request.email == currentGroup.firstUser.email) {
                                            currentGroup.copy(
                                                firstUser = currentGroup.firstUser.copy(avatar = File(request.file).toUri())
                                            )
                                        } else {
                                            currentGroup.copy(
                                                lastUser = currentGroup.lastUser.copy(avatar = File(request.file).toUri())
                                            )
                                        }
                                }
                            }

                            emitter.onNext(groups.sortedAlphabetically())
                        }
                    } else {
                        Timber.e(error.toThrowable())
                    }
                },
                onRequestTemporaryError = { _, error ->
                    Timber.e(error.toThrowable())
                }
            )

            megaChatApi.chatRooms.forEach { chatRoom ->
                if (chatRoom.isGroup && chatRoom.peerCount > 0) {
                    val firstUserHandle = chatRoom.getPeerHandle(0)
                    val lastUserHandle = chatRoom.getPeerHandle(chatRoom.peerCount - 1)

                    groups.add(
                        ContactGroupItem(
                            chatId = chatRoom.chatId,
                            title = ChatUtil.getTitleChat(chatRoom),
                            firstUser = getGroupUserFromHandle(firstUserHandle, userAttrsListener),
                            lastUser = getGroupUserFromHandle(lastUserHandle, userAttrsListener),
                            isPublic = chatRoom.isPublic
                        )
                    )
                }
            }

            emitter.onNext(groups.sortedAlphabetically())
        }, BackpressureStrategy.LATEST)

    /**
     * Build ContactGroupUser given an User handle
     *
     * @param userHandle    User handle to obtain group
     * @param listener      Listener to deliver user attributes
     * @return              ContactGroupUser
     */
    private fun getGroupUserFromHandle(
        userHandle: Long,
        listener: OptionalMegaRequestListenerInterface
    ): ContactGroupUser {
        var userAvatar: File? = null
        val userName = megaChatApi.getUserFirstnameFromCache(userHandle)
        val userEmail = megaChatApi.getUserEmailFromCache(userHandle)
        val userAvatarColor = megaApi.getUserAvatarColor(userHandle.toString()).toColorInt()

        if (userName.isNullOrBlank()) {
            megaApi.getUserAttribute(userHandle.toString(), USER_ATTR_FIRSTNAME, listener)
        }

        if (userEmail.isNullOrBlank()) {
            megaApi.getUserEmail(userHandle, listener)
        } else {
            val avatarFile = AvatarUtil.getUserAvatarFile(context, userEmail)
            if (avatarFile?.exists() == true) {
                userAvatar = avatarFile
            } else {
                megaApi.getUserAvatar(userEmail, avatarFile?.absolutePath, listener)
            }
        }

        return ContactGroupUser(
            handle = userHandle,
            email = userEmail,
            firstName = userName,
            avatar = userAvatar?.toUri(),
            avatarColor = userAvatarColor
        )
    }

    private fun MutableList<ContactGroupItem>.sortedAlphabetically(): List<ContactGroupItem> =
        sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, ContactGroupItem::title))
}
