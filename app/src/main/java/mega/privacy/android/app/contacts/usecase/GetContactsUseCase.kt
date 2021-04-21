package mega.privacy.android.app.contacts.usecase

import android.content.Context
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableEmitter
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.contacts.data.ContactItem
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.LogUtil.*
import nz.mega.sdk.*
import java.io.File
import javax.inject.Inject

class GetContactsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    private val databaseHandler: DatabaseHandler
) {

    fun get(): Flowable<List<ContactItem>> =
        Flowable.create({ emitter: FlowableEmitter<List<ContactItem>> ->
            val contacts = megaApi.contacts.map { megaUser ->
                val userName = databaseHandler.findContactByHandle(megaUser.handle.toString()).name
                val userStatus = megaChatApi.getUserOnlineStatus(megaUser.handle)
                val userImageColor = megaApi.getUserAvatarColor(megaUser).toColorInt()
                val userImageFile = getUserImageFile(megaUser.email)
                val userImageUri = if (userImageFile.exists()) {
                    userImageFile.toUri()
                } else {
                    null
                }

                ContactItem(
                    handle = megaUser.handle,
                    email = megaUser.email,
                    name = userName,
                    status = userStatus,
                    imageUri = userImageUri,
                    imageColor = userImageColor,
                    lastSeen = "Online"
                )
            }.toMutableList()

            emitter.onNext(contacts)

            val avatarListener = object : MegaRequestListenerInterface {
                override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {}

                override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {}

                override fun onRequestFinish(
                    api: MegaApiJava,
                    request: MegaRequest,
                    error: MegaError
                ) {
                    if (emitter.isCancelled) return

                    if (error.errorCode == MegaError.API_OK) {
                        contacts.forEachIndexed { index, contact ->
                            if (contact.email == request.email) {
                                val userImageUri = File(request.file).toUri()

                                contacts[index] = contact.copy(
                                    imageUri = userImageUri
                                )
                                return@forEachIndexed
                            }
                        }

                        emitter.onNext(contacts.toList())
                    } else {
                        logError(error.toThrowable().stackTraceToString())
                    }
                }

                override fun onRequestTemporaryError(
                    api: MegaApiJava,
                    request: MegaRequest,
                    error: MegaError
                ) {
                    logError(error.toThrowable().stackTraceToString())
                }
            }

            contacts.forEach { contact ->
                val userImageFile = getUserImageFile(contact.email).absolutePath
                megaApi.getUserAvatar(contact.email, userImageFile, avatarListener)
            }
        }, BackpressureStrategy.BUFFER)

    private fun getUserImageFile(userEmail: String): File =
        CacheFolderManager.buildAvatarFile(context, "$userEmail.jpg")
}
