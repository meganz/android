package mega.privacy.android.app.contacts.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.LogUtil.logError
import nz.mega.sdk.*
import nz.mega.sdk.MegaContactRequest.*
import nz.mega.sdk.MegaError.API_OK
import javax.inject.Inject

/**
 * Use case to create group chat from contacts selected
 *
 * @property megaApi            MegaApi required to call the SDK
 * @property megaChatApi        MegaChatApi required to call the MegaChatSDK
 */
class CreateGroupChatUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid
) {

    /**
     * Create and get a group chat room
     *
     * @param contactsData  List of selected contacts
     * @param chatTitle     Title of new chat room
     * @return              Chat ID of the new chat room
     */
    fun getGroupChatRoomCreated(contactsData: ArrayList<String>, chatTitle: String?): Single<Long> =
        Single.create { emitter ->
            val peerList = MegaChatPeerList.createInstance()
            for (i in contactsData.indices) {
                val user = megaApi.getContact(contactsData[i])
                if (user != null) {
                    peerList.addPeer(user.handle, MegaChatPeerList.PRIV_STANDARD)
                }
            }

            megaChatApi.createPublicChat(peerList,
                chatTitle,
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
                        logError(error.toThrowable().stackTraceToString())
                    }
                ))
        }
}