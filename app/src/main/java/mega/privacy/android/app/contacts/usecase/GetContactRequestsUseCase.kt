package mega.privacy.android.app.contacts.usecase

import android.content.Context
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableEmitter
import mega.privacy.android.app.contacts.data.ContactItem
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.MegaUserUtils.getUserImageFile
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.*
import nz.mega.sdk.MegaChatApi.*
import nz.mega.sdk.MegaChatApiAndroid
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

    fun getIncomingRequests(): Flowable<List<ContactItem>> =
        getContactRequest(true)

    fun getOutgoingRequests(): Flowable<List<ContactItem>> =
        getContactRequest(false)

    private fun getContactRequest(isIncoming: Boolean): Flowable<List<ContactItem>> =
        Flowable.create({ emitter: FlowableEmitter<List<ContactItem>> ->
            val contactRequests = if (isIncoming) {
                megaApi.incomingContactRequests
            } else {
                megaApi.outgoingContactRequests
            }

            val contacts = contactRequests.sortedBy { it.modificationTime }.map { request ->
                val userName = megaChatApi.getUserFirstnameFromCache(request.handle)
                val userImageColor = megaApi.getUserAvatarColor(request.handle.toString()).toColorInt()
                val userImageFile = getUserImageFile(context, request.sourceEmail)
                val userImageUri = if (userImageFile.exists()) {
                    userImageFile.toUri()
                } else {
                    null
                }

                ContactItem(
                    handle = request.handle,
                    email = request.sourceEmail,
                    name = userName,
                    imageUri = userImageUri,
                    imageColor = userImageColor
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

            contacts.forEach { contact ->
                val userImageFile = getUserImageFile(context, contact.email).absolutePath
                megaApi.getUserAvatar(contact.email, userImageFile, userAttrsListener)
                megaApi.getUserAttribute(contact.email, USER_ATTR_FIRSTNAME, userAttrsListener)
            }
        }, BackpressureStrategy.BUFFER)
}
