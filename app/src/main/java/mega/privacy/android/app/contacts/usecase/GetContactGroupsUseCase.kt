package mega.privacy.android.app.contacts.usecase

import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableEmitter
import mega.privacy.android.app.contacts.group.data.GroupItem
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.MegaUserUtils
import nz.mega.sdk.*
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

    fun get(): Flowable<List<GroupItem>> =
        Flowable.create({ emitter: FlowableEmitter<List<GroupItem>> ->
            val groups = mutableListOf<GroupItem>()

            val userAttrsListener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (emitter.isCancelled) return@OptionalMegaRequestListenerInterface

                    if (error.errorCode == MegaError.API_OK) {
                        val index = groups.indexOfFirst { request.email in it.firstImageEmail..it.secondImageEmail }
                        if (index != NOT_FOUND) {
                            val imageFile = File(request.file).toUri()
                            val currentGroup = groups[index]
                            if (request.email == currentGroup.firstImageEmail) {
                                groups[index] = currentGroup.copy(firstImage = imageFile)
                            } else {
                                groups[index] = currentGroup.copy(secondImage = imageFile)
                            }

                            emitter.onNext(groups)
                        }
                    } else {
                        LogUtil.logError(error.toThrowable().stackTraceToString())
                    }
                },
                onRequestTemporaryError = { _, error ->
                    LogUtil.logError(error.toThrowable().stackTraceToString())
                }
            )

            megaChatApi.chatRooms.sortedByDescending { it.creationTs }.forEach { chatRoom ->
                if (chatRoom.isGroup && chatRoom.peerCount > 0) {
                    val firstUserHandle = chatRoom.getPeerHandle(0)
                    val firstUserEmail = megaChatApi.getContactEmail(firstUserHandle)
                    var firstImage: File? = MegaUserUtils.getUserImageFile(context, firstUserEmail)
                    if (firstImage?.exists() == false) {
                        megaApi.getUserAvatar(firstUserEmail, firstImage.absolutePath, userAttrsListener)
                    } else {
                        firstImage = null
                    }

                    val lastUserHandle = chatRoom.getPeerHandle(chatRoom.peerCount - 1)
                    val lastUserEmail = megaChatApi.getContactEmail(lastUserHandle)
                    var secondImage: File? = MegaUserUtils.getUserImageFile(context, lastUserEmail)
                    if (secondImage?.exists() == false) {
                        megaApi.getUserAvatar(firstUserEmail, secondImage.absolutePath, userAttrsListener)
                    } else {
                        secondImage = null
                    }

                    groups.add(
                        GroupItem(
                            chatId = chatRoom.chatId,
                            title = chatRoom.title,
                            firstImage = firstImage?.toUri(),
                            firstImageEmail = firstUserEmail,
                            secondImageEmail = lastUserEmail,
                            secondImage = secondImage?.toUri(),
                            firstImageColor = firstUserHandle.getUserAvatarColor(),
                            secondImageColor = lastUserHandle.getUserAvatarColor(),
                            isPublic = chatRoom.isPublic
                        )
                    )
                }
            }

            emitter.onNext(groups)
        }, BackpressureStrategy.BUFFER)

    @ColorInt
    private fun Long.getUserAvatarColor(): Int =
        megaApi.getUserAvatarColor(toString()).toColorInt()
}
