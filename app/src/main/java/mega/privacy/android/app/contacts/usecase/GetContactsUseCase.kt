package mega.privacy.android.app.contacts.usecase

import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableEmitter
import io.reactivex.rxjava3.disposables.Disposable
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.contacts.data.ContactItem
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.utils.CacheFolderManager
import nz.mega.sdk.*
import java.io.File
import javax.inject.Inject

class GetContactsUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    private val databaseHandler: DatabaseHandler
) {

    fun get(): Flowable<List<ContactItem>> =
        Flowable.create({ emitter: FlowableEmitter<List<ContactItem>> ->
            val contacts = megaApi.contacts.map { megaUser ->
                val dbUser = databaseHandler.findContactByHandle(megaUser.handle.toString())
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
                    name = dbUser.name,
                    status = userStatus,
                    imageUri = userImageUri,
                    imageColor = userImageColor,
                    lastSeen = "Online"
                )
            }.toMutableList()

            emitter.onNext(contacts)

            val listener = object : MegaRequestListenerInterface {
                override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
                    if (emitter.isCancelled) {
                        megaApi.removeRequestListener(this)
                    }
                }

                override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {
                    if (emitter.isCancelled) {
                        megaApi.removeRequestListener(this)
                        return
                    }

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
                }

                override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
                    if (emitter.isCancelled) {
                        megaApi.removeRequestListener(this)
                        return
                    }

                    if (e.errorCode == MegaError.API_OK) {
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
                    }
                }

                override fun onRequestTemporaryError(
                    api: MegaApiJava,
                    request: MegaRequest,
                    e: MegaError
                ) {
                    megaApi.removeRequestListener(this)
                    emitter.onError(IllegalStateException(e.errorString))
                }
            }

            contacts.forEach { contact ->
                val userImageFile = getUserImageFile(contact.email).absolutePath
                megaApi.getUserAvatar(contact.email, userImageFile, listener)
            }

            emitter.setDisposable(Disposable.fromAction {
                megaApi.removeRequestListener(listener)
            })
        }, BackpressureStrategy.BUFFER)

    private fun getUserImageFile(email: String): File {
        val context = MegaApplication.getInstance().applicationContext
        val fileName = "$email.jpg"
        return CacheFolderManager.buildAvatarFile(context, fileName)
    }
}
