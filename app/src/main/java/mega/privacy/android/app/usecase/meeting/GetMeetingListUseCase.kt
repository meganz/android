package mega.privacy.android.app.usecase.meeting

import android.content.Context
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import mega.privacy.android.app.contacts.group.data.ContactGroupUser
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.meeting.list.MeetingItem
import mega.privacy.android.app.usecase.chat.GetChatChangesUseCase
import mega.privacy.android.app.usecase.chat.GetChatChangesUseCase.Result
import mega.privacy.android.app.usecase.exception.toMegaException
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class GetMeetingListUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    private val getChatChangesUseCase: GetChatChangesUseCase,
) {

    fun get(): Flowable<List<MeetingItem>> =
        Flowable.create({ emitter ->
            val changesSubscription = CompositeDisposable()
            val meetings = mutableListOf<MeetingItem>()

            val userAttrsListener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (emitter.isCancelled) return@OptionalMegaRequestListenerInterface

                    if (error.errorCode == MegaError.API_OK) {
                        val index = meetings.indexOfFirst {
                            request.nodeHandle in it.firstUser.handle..it.lastUser.handle
                                    || request.email == it.firstUser.email || request.email == it.lastUser.email
                        }

                        if (index != Constants.INVALID_POSITION) {
                            val currentItem = meetings[index]
                            when (request.paramType) {
                                MegaRequest.TYPE_GET_USER_EMAIL -> {
                                    meetings[index] =
                                        if (request.nodeHandle == currentItem.firstUser.handle) {
                                            currentItem.copy(
                                                firstUser = currentItem.firstUser.copy(email = request.text)
                                            )
                                        } else {
                                            currentItem.copy(
                                                lastUser = currentItem.lastUser.copy(email = request.text)
                                            )
                                        }
                                }
                                MegaApiJava.USER_ATTR_FIRSTNAME -> {
                                    meetings[index] =
                                        if (request.nodeHandle == currentItem.firstUser.handle) {
                                            currentItem.copy(
                                                firstUser = currentItem.firstUser.copy(firstName = request.text)
                                            )
                                        } else {
                                            currentItem.copy(
                                                lastUser = currentItem.lastUser.copy(firstName = request.text)
                                            )
                                        }
                                }
                                MegaApiJava.USER_ATTR_AVATAR -> {
                                    meetings[index] =
                                        if (request.email == currentItem.firstUser.email) {
                                            currentItem.copy(
                                                firstUser = currentItem.firstUser.copy(avatar = File(request.file).toUri())
                                            )
                                        } else {
                                            currentItem.copy(
                                                lastUser = currentItem.lastUser.copy(avatar = File(request.file).toUri())
                                            )
                                        }
                                }
                            }

                            emitter.onNext(meetings.sortedByDescending { it.timeStamp })
                        }
                    } else {
                        Timber.e(error.toMegaException())
                    }
                },
                onRequestTemporaryError = { _, error ->
                    Timber.e(error.toMegaException())
                }
            )

            megaChatApi.getChatRoomsByType(MegaChatApi.CHAT_TYPE_MEETING_ROOM)
                .forEach { chatRoom ->
                    meetings.add(chatRoom.toMeetingItem(userAttrsListener))
                }

            emitter.onNext(meetings.sortedByDescending { it.timeStamp })

            getChatChangesUseCase.get()
                .filter { it is Result.OnChatListItemUpdate && it.item != null }
                .subscribeBy(
                    onNext = { change ->
                        if (emitter.isCancelled) return@subscribeBy

                        when (change) {
                            is Result.OnChatListItemUpdate -> {
                                val chatId = change.item!!.chatId
                                val index = meetings.indexOfFirst { it.chatId == chatId }
                                if (index != Constants.INVALID_POSITION) {
                                    val updatedMeeting = megaChatApi.getChatRoom(chatId).toMeetingItem(userAttrsListener)
                                    meetings[index] = updatedMeeting

                                    emitter.onNext(meetings.sortedByDescending { it.timeStamp })
                                }
                            }
                            else -> {
                                // Nothing to do
                            }
                        }
                    },
                    onError = Timber::e
                ).addTo(changesSubscription)

            emitter.setCancellable {
                changesSubscription.dispose()
            }
        }, BackpressureStrategy.LATEST)

    /**
     * Build MeetingItem given a MegaChatRoom
     *
     * @param userAttrsListener Listener to deliver user attributes
     * @return                  MeetingItem
     */
    private fun MegaChatRoom.toMeetingItem(userAttrsListener: OptionalMegaRequestListenerInterface): MeetingItem {
        val chatListItem = megaChatApi.getChatListItem(chatId)
        return MeetingItem(
            chatId = chatId,
            title = ChatUtil.getTitleChat(this),
            lastMessage = chatListItem.lastMessage,
            timeStamp = chatListItem.lastTimestamp,
            firstUser = getGroupUserFromHandle(getPeerHandle(0), userAttrsListener),
            lastUser = getGroupUserFromHandle(getPeerHandle(peerCount),
                userAttrsListener)
        )
    }

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
            megaApi.getUserAttribute(userHandle.toString(), MegaApiJava.USER_ATTR_FIRSTNAME, listener)
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
}
