package mega.privacy.android.app.contacts.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import nz.mega.sdk.*
import nz.mega.sdk.MegaError.API_OK
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case to create group chat from contacts selected
 *
 * @property megaApi            MegaApi required to call the SDK
 * @property megaChatApi        MegaChatApi required to call the MegaChatSDK
 */
class CreateGroupChatUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
) {

    /**
     * Create and get a group chat room
     *
     * @param contactsData  List of selected contacts
     * @param chatTitle     Title of new chat room
     * @return              Chat ID of the new chat room
     */
    fun getGroupChatRoomCreated(
        contactsData: ArrayList<String>,
        chatTitle: String?,
        allowAddParticipants: Boolean,
    ): Single<Long> =
        Single.create { emitter ->
            val peerList = MegaChatPeerList.createInstance()
            contactsData.forEach { email ->
                val contact = megaApi.getContact(email)
                if (contact != null) {
                    peerList.addPeer(contact.handle, MegaChatPeerList.PRIV_STANDARD)
                }
            }

            if (peerList.size() == 0) {
                emitter.onError(IllegalArgumentException("No contacts"))
                return@create
            }

            megaChatApi.createPublicChat(peerList,
                chatTitle,
                false,
                false,
                allowAddParticipants,
                OptionalMegaChatRequestListenerInterface(
                    onRequestFinish = { request: MegaChatRequest, error: MegaChatError ->
                        if (emitter.isDisposed) return@OptionalMegaChatRequestListenerInterface

                        if (error.errorCode == API_OK) {
                            emitter.onSuccess(request.chatHandle)
                        } else {
                            emitter.onError(error.toThrowable())
                        }
                    },
                    onRequestTemporaryError = { _: MegaChatRequest, error: MegaChatError ->
                        Timber.e(error.toThrowable())
                    }
                ))
        }
}