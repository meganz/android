package mega.privacy.android.app.contacts.usecase

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.format.DateUtils.getRelativeTimeSpanString
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.Disposable
import mega.privacy.android.app.R
import mega.privacy.android.app.contacts.requests.data.ContactRequestItem
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaGlobalListenerInterface
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.view.TextDrawable
import nz.mega.sdk.*
import nz.mega.sdk.MegaApiJava.*
import nz.mega.sdk.MegaChatApi.*
import nz.mega.sdk.MegaContactRequest.STATUS_REMINDED
import nz.mega.sdk.MegaContactRequest.STATUS_UNRESOLVED
import java.io.File
import java.util.*
import javax.inject.Inject

class GetContactRequestsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid
) {

    companion object {
        private const val NOT_FOUND = -1
    }

    fun get(): Flowable<List<ContactRequestItem>> =
        Flowable.create({ emitter ->
            val requests = arrayListOf<MegaContactRequest>().apply {
                addAll(megaApi.incomingContactRequests)
                addAll(megaApi.outgoingContactRequests)
            }

            val requestItems = requests
                .sortedByDescending { it.creationTime }
                .map { it.toContactRequestItem() }
                .toMutableList()

            emitter.onNext(requestItems)

            val userAttrsListener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (emitter.isCancelled) return@OptionalMegaRequestListenerInterface

                    if (error.errorCode == MegaError.API_OK) {
                        val index = requestItems.indexOfFirst { it.email == request.email }
                        if (index != NOT_FOUND) {
                            val currentContact = requestItems[index]

                            when (request.paramType) {
                                USER_ATTR_AVATAR ->
                                    requestItems[index] = currentContact.copy(
                                        avatarUri = File(request.file).toUri()
                                    )
                                USER_ATTR_FIRSTNAME ->
                                    requestItems[index] = currentContact.copy(
                                        name = request.text
                                    )
                            }

                            emitter.onNext(requestItems)
                        }
                    } else {
                        logError(error.toThrowable().stackTraceToString())
                    }
                },
                onRequestTemporaryError = { _, error ->
                    logError(error.toThrowable().stackTraceToString())
                }
            )

            val globalListener = OptionalMegaGlobalListenerInterface(
                onContactRequestsUpdate = { updatedRequests ->
                    updatedRequests.forEach { request ->
                        when (request.status) {
                            STATUS_UNRESOLVED -> {
                                if (requestItems.any { it.handle == request.handle }) return@forEach

                                val newRequestItem = request.toContactRequestItem().apply {
                                    val userImageFile = AvatarUtil.getUserAvatarFile(context, email)?.absolutePath
                                    megaApi.getUserAvatar(email, userImageFile, userAttrsListener)
                                    megaApi.getUserAttribute(email, USER_ATTR_FIRSTNAME, userAttrsListener)
                                }

                                requestItems.add(newRequestItem)
                            }
                            STATUS_REMINDED -> {
                                // do nothing
                            }
                            else -> {
                                requestItems.removeIf { it.handle == request.handle }
                            }
                        }
                    }

                    emitter.onNext(requestItems)
                }
            )

            megaApi.addGlobalListener(globalListener)

            requestItems.forEach { request ->
                if (request.avatarUri == null) {
                    val userImageFile = AvatarUtil.getUserAvatarFile(context, request.email)?.absolutePath
                    megaApi.getUserAvatar(request.email, userImageFile, userAttrsListener)
                }

                if (request.name.isNullOrBlank()) {
                    megaApi.getUserAttribute(request.email, USER_ATTR_FIRSTNAME, userAttrsListener)
                }
            }

            emitter.setDisposable(Disposable.fromAction {
                megaApi.removeGlobalListener(globalListener)
            })
        }, BackpressureStrategy.LATEST)

    fun getIncomingRequestsSize(): Flowable<Int> =
        Flowable.create({ emitter ->
            emitter.onNext(megaApi.incomingContactRequests.size)

            val globalListener = OptionalMegaGlobalListenerInterface(
                onContactRequestsUpdate = {
                    emitter.onNext(megaApi.incomingContactRequests.size)
                }
            )

            megaApi.addGlobalListener(globalListener)

            emitter.setDisposable(Disposable.fromAction {
                megaApi.removeGlobalListener(globalListener)
            })
        }, BackpressureStrategy.LATEST)

    private fun MegaContactRequest.toContactRequestItem(): ContactRequestItem {
        var userImageUri: Uri? = null

        val userEmail = if (isOutgoing) targetEmail else sourceEmail
        val userName = megaChatApi.getUserFirstnameFromCache(handle)
        val userImageColor = megaApi.getUserAvatarColor(handle.toString()).toColorInt()
        val placeholder = getImagePlaceholder(userName ?: userEmail, userImageColor)
        val userImageFile = AvatarUtil.getUserAvatarFile(context, userEmail)
        if (userImageFile?.exists() == true) {
            userImageUri = userImageFile.toUri()
        }

        return ContactRequestItem(
            handle = handle,
            email = userEmail,
            name = userName,
            avatarUri = userImageUri,
            placeholder = placeholder,
            isOutgoing = isOutgoing,
            createdTime = getRelativeTimeSpanString(creationTime * 1000).toString()
        )
    }

    private fun getImagePlaceholder(title: String, @ColorInt color: Int): Drawable =
        TextDrawable.builder()
            .beginConfig()
            .width(context.resources.getDimensionPixelSize(R.dimen.image_contact_size))
            .height(context.resources.getDimensionPixelSize(R.dimen.image_contact_size))
            .fontSize(context.resources.getDimensionPixelSize(R.dimen.image_contact_text_size))
            .textColor(ContextCompat.getColor(context, R.color.white))
            .bold()
            .toUpperCase()
            .endConfig()
            .buildRound(AvatarUtil.getFirstLetter(title), color)
}
