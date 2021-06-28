package mega.privacy.android.app.contacts.usecase

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableEmitter
import mega.privacy.android.app.R
import mega.privacy.android.app.contacts.group.data.ContactGroupItem
import mega.privacy.android.app.contacts.group.data.ContactGroupUser
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.LogUtil.*
import mega.privacy.android.app.utils.MegaUserUtils
import mega.privacy.android.app.utils.view.TextDrawable
import nz.mega.sdk.*
import nz.mega.sdk.MegaApiJava.*
import nz.mega.sdk.MegaRequest.TYPE_GET_USER_EMAIL
import java.io.File
import javax.inject.Inject

class GetContactGroupsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid
) {

    companion object {
        private const val NOT_FOUND = -1
    }

    fun get(): Flowable<List<ContactGroupItem>> =
        Flowable.create({ emitter: FlowableEmitter<List<ContactGroupItem>> ->
            val groups = mutableListOf<ContactGroupItem>()

            val userAttrsListener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (emitter.isCancelled) return@OptionalMegaRequestListenerInterface

                    if (error.errorCode == MegaError.API_OK) {
                        val index = groups.indexOfFirst {
                            request.nodeHandle in it.firstUser.handle..it.lastUser.handle
                                    || request.email == it.firstUser.email || request.email == it.lastUser.email
                        }

                        if (index != NOT_FOUND) {
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
                                    val newPlaceholder = getImagePlaceholder(newFirstname, user.avatarColor)
                                    val newUser = user.copy(
                                        firstName = newFirstname,
                                        placeholder = newPlaceholder
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
                        logError(error.toThrowable().stackTraceToString())
                    }
                },
                onRequestTemporaryError = { _, error ->
                    logError(error.toThrowable().stackTraceToString())
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

    private fun getGroupUserFromHandle(
        userHandle: Long,
        listener: OptionalMegaRequestListenerInterface
    ): ContactGroupUser {
        var userAvatar: File? = null
        val userName = megaChatApi.getUserFirstnameFromCache(userHandle)
        val userEmail = megaChatApi.getUserEmailFromCache(userHandle)
        val userAvatarColor = megaApi.getUserAvatarColor(userHandle.toString()).toColorInt()
        val userPlaceholder = getImagePlaceholder(
            userName ?: userEmail ?: userHandle.toString(),
            userAvatarColor
        )

        if (userName.isNullOrBlank()) {
            megaApi.getUserAttribute(userHandle.toString(), USER_ATTR_FIRSTNAME, listener)
        }

        if (userEmail.isNullOrBlank()) {
            megaApi.getUserEmail(userHandle, listener)
        } else {
            val avatarFile = MegaUserUtils.getUserAvatarFile(context, userEmail)
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
            avatarColor = userAvatarColor,
            placeholder = userPlaceholder
        )
    }

    private fun getImagePlaceholder(title: String, @ColorInt color: Int): Drawable =
        TextDrawable.builder()
            .beginConfig()
            .width(context.resources.getDimensionPixelSize(R.dimen.image_group_size))
            .height(context.resources.getDimensionPixelSize(R.dimen.image_group_size))
            .fontSize(context.resources.getDimensionPixelSize(R.dimen.image_group_text_size))
            .withBorder(context.resources.getDimensionPixelSize(R.dimen.image_group_border_size))
            .borderColor(ContextCompat.getColor(context, R.color.white))
            .bold()
            .toUpperCase()
            .endConfig()
            .buildRound(AvatarUtil.getFirstLetter(title), color)

    private fun MutableList<ContactGroupItem>.sortedAlphabetically(): List<ContactGroupItem> =
        sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, ContactGroupItem::title))
}
