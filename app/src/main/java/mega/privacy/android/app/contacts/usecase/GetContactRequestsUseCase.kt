package mega.privacy.android.app.contacts.usecase

import android.content.Context
import android.net.Uri
import android.text.format.DateUtils.getRelativeTimeSpanString
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableEmitter
import mega.privacy.android.app.contacts.requests.data.ContactRequestItem
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.MegaUserUtils.getUserImageFile
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.*
import nz.mega.sdk.MegaChatApi.*
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaError
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
        Flowable.create({ emitter: FlowableEmitter<List<ContactRequestItem>> ->
            val contactRequests = arrayListOf<MegaContactRequest>().apply {
                addAll(megaApi.incomingContactRequests)
                addAll(megaApi.outgoingContactRequests)
            }

            val contacts = contactRequests.sortedByDescending { it.creationTime }.map { request ->
                var userImageUri: Uri? = null
                var userName: String? = null

                val userEmail = if (request.isOutgoing) request.targetEmail else request.sourceEmail
                val userImageColor = megaApi.getUserAvatarColor(request.handle.toString()).toColorInt()
                val userImageFile = getUserImageFile(context, userEmail)
                if (userImageFile.exists()) {
                    userImageUri = userImageFile.toUri()
                }

                if (!request.isOutgoing) {
                    userName = megaChatApi.getUserFirstnameFromCache(request.handle)
                }

                ContactRequestItem(
                    handle = request.handle,
                    email = userEmail,
                    name = userName,
                    imageUri = userImageUri,
                    imageColor = userImageColor,
                    isOutgoing = request.isOutgoing,
                    createdTime = getRelativeTimeSpanString(request.creationTime * 1000).toString()
                )
            }.toMutableList()

            emitter.onNext(contacts)

            val userAttrsListener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (emitter.isCancelled) return@OptionalMegaRequestListenerInterface

                    if (error.errorCode == MegaError.API_OK) {
                        val index = contacts.indexOfFirst { it.email == request.email }
                        if (index != NOT_FOUND) {
                            val currentContact = contacts[index]

                            when (request.paramType) {
                                USER_ATTR_AVATAR ->
                                    contacts[index] = currentContact.copy(
                                        imageUri = File(request.file).toUri()
                                    )
                                USER_ATTR_FIRSTNAME ->
                                    contacts[index] = currentContact.copy(
                                        name = request.text
                                    )
                            }

                            emitter.onNext(contacts)
                        }
                    } else {
                        logError(error.toThrowable().stackTraceToString())
                    }
                },
                onRequestTemporaryError = { _, error ->
                    logError(error.toThrowable().stackTraceToString())
                }
            )

            contacts.forEach { request ->
                val userImageFile = getUserImageFile(context, request.email!!).absolutePath
                megaApi.getUserAvatar(request.email, userImageFile, userAttrsListener)
                megaApi.getUserAttribute(request.email, USER_ATTR_FIRSTNAME, userAttrsListener)
            }
        }, BackpressureStrategy.BUFFER)
}
