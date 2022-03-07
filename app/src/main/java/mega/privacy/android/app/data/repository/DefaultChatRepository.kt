package mega.privacy.android.app.data.repository

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import mega.privacy.android.app.domain.repository.ChatRepository
import nz.mega.sdk.*
import javax.inject.Inject

class DefaultChatRepository @Inject constructor(
    private val chatApi: MegaChatApiAndroid
) : ChatRepository {
    override fun notifyChatLogout(): Flow<Boolean> {
        return callbackFlow {
            val listener = object : MegaChatRequestListenerInterface{
                override fun onRequestStart(api: MegaChatApiJava?, request: MegaChatRequest?) {}

                override fun onRequestUpdate(api: MegaChatApiJava?, request: MegaChatRequest?) {}

                override fun onRequestFinish(
                    api: MegaChatApiJava?,
                    request: MegaChatRequest?,
                    e: MegaChatError?
                ) {
                    if (request?.type == MegaChatRequest.TYPE_LOGOUT) {
                        if (e?.errorCode == MegaError.API_OK) {
                            trySend(true)
                        }
                    }
                }

                override fun onRequestTemporaryError(
                    api: MegaChatApiJava?,
                    request: MegaChatRequest?,
                    e: MegaChatError?
                ) {}
            }
            chatApi.addChatRequestListener(listener)

            awaitClose{ chatApi.removeChatRequestListener(listener) }
        }
    }
}
