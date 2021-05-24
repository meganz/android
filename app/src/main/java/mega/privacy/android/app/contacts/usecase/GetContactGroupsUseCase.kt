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
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.MegaUserUtils
import mega.privacy.android.app.utils.view.TextDrawable
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

    fun get(): Flowable<List<ContactGroupItem>> =
        Flowable.create({ emitter: FlowableEmitter<List<ContactGroupItem>> ->
            val groups = mutableListOf<ContactGroupItem>()

            val userAttrsListener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (emitter.isCancelled) return@OptionalMegaRequestListenerInterface

                    if (error.errorCode == MegaError.API_OK) {
                        val index = groups.indexOfFirst { request.email in it.firstUserEmail..it.lastUserEmail }
                        if (index != NOT_FOUND) {
                            val imageFile = File(request.file).toUri()
                            val currentGroup = groups[index]
                            if (request.email == currentGroup.firstUserEmail) {
                                groups[index] = currentGroup.copy(firstUserAvatar = imageFile)
                            } else {
                                groups[index] = currentGroup.copy(lastUserAvatar = imageFile)
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
                    val firstUserName = megaChatApi.getUserFirstnameFromCache(firstUserHandle)
                    val firstUserColor = megaApi.getUserAvatarColor(firstUserHandle.toString()).toColorInt()
                    val firstUserPlaceholder = getImagePlaceholder(firstUserName ?: firstUserEmail, firstUserColor)
                    var firstImage = MegaUserUtils.getUserAvatarFile(context, firstUserEmail)
                    if (firstImage?.exists() == false) {
                        megaApi.getUserAvatar(firstUserEmail, firstImage.absolutePath, userAttrsListener)
                        firstImage = null
                    }

                    val lastUserHandle = chatRoom.getPeerHandle(chatRoom.peerCount - 1)
                    val lastUserEmail = megaChatApi.getContactEmail(lastUserHandle)
                    val lastUserName = megaChatApi.getUserFirstnameFromCache(lastUserHandle)
                    val lastUserColor = megaApi.getUserAvatarColor(lastUserHandle.toString()).toColorInt()
                    val lastUserPlaceholder = getImagePlaceholder(lastUserName ?: lastUserEmail, lastUserColor)
                    var secondImage = MegaUserUtils.getUserAvatarFile(context, lastUserEmail)
                    if (secondImage?.exists() == false) {
                        megaApi.getUserAvatar(lastUserEmail, secondImage.absolutePath, userAttrsListener)
                        secondImage = null
                    }

                    groups.add(
                        ContactGroupItem(
                            chatId = chatRoom.chatId,
                            title = chatRoom.title,
                            firstUserAvatar = firstImage?.toUri(),
                            firstUserEmail = firstUserEmail,
                            firstUserPlaceholder = firstUserPlaceholder,
                            lastUserEmail = lastUserEmail,
                            lastUserAvatar = secondImage?.toUri(),
                            lastUserPlaceholder = lastUserPlaceholder,
                            isPublic = chatRoom.isPublic
                        )
                    )
                }
            }

            emitter.onNext(groups)
        }, BackpressureStrategy.BUFFER)

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
            .buildRound(title.first().toString(), color)
}
