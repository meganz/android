package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.extensions.failWithError
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.app.data.mapper.ContactRequestMapper
import mega.privacy.android.app.data.mapper.MegaChatPeerListMapper
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.listeners.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.repository.ContactsRepository
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

/**
 * Default implementation of [ContactsRepository]
 *
 * @property megaApiGateway         [MegaApiGateway]
 * @property megaChatApiGateway     [MegaChatApiGateway]
 * @property ioDispatcher           [CoroutineDispatcher]
 * @property contactRequestMapper   [ContactRequestMapper]
 * @property megaChatPeerListMapper [MegaChatPeerListMapper]
 */
class DefaultContactsRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val megaChatApiGateway: MegaChatApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val contactRequestMapper: ContactRequestMapper,
    private val megaChatPeerListMapper: MegaChatPeerListMapper,
) : ContactsRepository {

    override fun monitorContactRequestUpdates(): Flow<List<ContactRequest>> =
        megaApiGateway.globalUpdates
            .filterIsInstance<GlobalUpdate.OnContactRequestsUpdate>()
            .mapNotNull { it.requests?.map(contactRequestMapper) }

    override suspend fun startConversation(isGroup: Boolean, userHandles: List<Long>): Long =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                val chat1to1 = megaChatApiGateway.getChatRoomByUser(userHandles[0])
                if (!isGroup && chat1to1 != null) {
                    continuation.resumeWith(Result.success(chat1to1.chatId))
                } else {
                    megaChatApiGateway.createChat(isGroup,
                        megaChatPeerListMapper(userHandles),
                        OptionalMegaChatRequestListenerInterface(
                            onRequestFinish = onRequestCreateChatCompleted(continuation)
                        ))
                }
            }
        }

    private fun onRequestCreateChatCompleted(continuation: Continuation<Long>) =
        { request: MegaChatRequest, error: MegaChatError ->
            if (error.errorCode == MegaChatError.ERROR_OK) {
                continuation.resumeWith(Result.success(request.chatHandle))
            } else {
                continuation.failWithError(error)
            }
        }
}